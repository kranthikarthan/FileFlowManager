package com.filetransfer.web.service;

import com.filetransfer.web.entity.CutOffExtension;
import com.filetransfer.web.entity.SubServiceConfiguration;
import com.filetransfer.web.repository.CutOffExtensionRepository;
import com.filetransfer.web.repository.SubServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CutOffExtensionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CutOffExtensionService.class);
    
    @Autowired
    private CutOffExtensionRepository cutOffExtensionRepository;
    
    @Autowired
    private SubServiceConfigurationRepository subServiceConfigRepository;
    
    @Autowired
    private SecurityContextService securityContextService;
    
    @Autowired
    private AlertService alertService;
    
    /**
     * Request a cut-off time extension
     */
    public CutOffExtension requestExtension(String tenantId, String serviceName, String subServiceName,
                                          LocalDate extensionDate, LocalTime extendedCutOffTime, 
                                          String reason, CutOffExtension.ExtensionPriority priority) {
        
        // Validate sub-service exists
        SubServiceConfiguration subServiceConfig = subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElseThrow(() -> new RuntimeException(
                String.format("Sub-service not found: %s/%s for tenant %s", serviceName, subServiceName, tenantId)));
        
        // Get current cut-off time for the date
        LocalTime originalCutOffTime = getCurrentCutOffTime(subServiceConfig, extensionDate);
        
        // Validate extension is in the future
        if (extendedCutOffTime.isBefore(originalCutOffTime)) {
            throw new IllegalArgumentException("Extended cut-off time must be after the original cut-off time");
        }
        
        // Check if there's already an active or pending extension for this date
        long existingExtensions = cutOffExtensionRepository.countActiveOrPendingExtensionsForServiceAndDate(
            tenantId, serviceName, subServiceName, extensionDate);
        
        if (existingExtensions > 0) {
            throw new IllegalStateException(
                String.format("There is already an active or pending extension for %s/%s on %s", 
                             serviceName, subServiceName, extensionDate));
        }
        
        // Create extension request
        String requestedBy = securityContextService.getCurrentUserId();
        CutOffExtension extension = new CutOffExtension(tenantId, serviceName, subServiceName,
                                                       extensionDate, originalCutOffTime, 
                                                       extendedCutOffTime, reason, requestedBy);
        extension.setPriority(priority);
        
        // Check for auto-approval conditions
        if (shouldAutoApprove(extension, requestedBy)) {
            extension.approve("system", "Auto-approved based on configured rules");
            extension.setAutoApproved(true);
            logger.info("Extension auto-approved for {}/{} on {}", serviceName, subServiceName, extensionDate);
        }
        
        CutOffExtension savedExtension = cutOffExtensionRepository.save(extension);
        
        // Send notifications
        sendExtensionNotifications(savedExtension, "requested");
        
        logger.info("Cut-off extension requested: ID={}, Service={}/{}, Date={}, Status={}", 
                   savedExtension.getId(), serviceName, subServiceName, extensionDate, savedExtension.getStatus());
        
        return savedExtension;
    }
    
    /**
     * Approve a cut-off extension
     */
    public CutOffExtension approveExtension(Long extensionId, String comments) {
        CutOffExtension extension = cutOffExtensionRepository.findById(extensionId)
            .orElseThrow(() -> new RuntimeException("Extension not found with id: " + extensionId));
        
        if (extension.getStatus() != CutOffExtension.ExtensionStatus.PENDING) {
            throw new IllegalStateException("Only pending extensions can be approved");
        }
        
        String approvedBy = securityContextService.getCurrentUserId();
        extension.approve(approvedBy, comments);
        
        CutOffExtension savedExtension = cutOffExtensionRepository.save(extension);
        
        // Send notifications
        sendExtensionNotifications(savedExtension, "approved");
        
        logger.info("Cut-off extension approved: ID={}, ApprovedBy={}", extensionId, approvedBy);
        
        return savedExtension;
    }
    
    /**
     * Reject a cut-off extension
     */
    public CutOffExtension rejectExtension(Long extensionId, String rejectionReason) {
        CutOffExtension extension = cutOffExtensionRepository.findById(extensionId)
            .orElseThrow(() -> new RuntimeException("Extension not found with id: " + extensionId));
        
        if (extension.getStatus() != CutOffExtension.ExtensionStatus.PENDING) {
            throw new IllegalStateException("Only pending extensions can be rejected");
        }
        
        String rejectedBy = securityContextService.getCurrentUserId();
        extension.reject(rejectedBy, rejectionReason);
        
        CutOffExtension savedExtension = cutOffExtensionRepository.save(extension);
        
        // Send notifications
        sendExtensionNotifications(savedExtension, "rejected");
        
        logger.info("Cut-off extension rejected: ID={}, RejectedBy={}, Reason={}", 
                   extensionId, rejectedBy, rejectionReason);
        
        return savedExtension;
    }
    
    /**
     * Get effective cut-off time considering any active extensions
     */
    public LocalTime getEffectiveCutOffTime(String tenantId, String serviceName, String subServiceName, 
                                           LocalDate date, LocalDateTime currentTime) {
        
        // First get the configured cut-off time
        SubServiceConfiguration subServiceConfig = subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElse(null);
        
        if (subServiceConfig == null) {
            return LocalTime.parse("23:59:59"); // Default fallback
        }
        
        LocalTime configuredCutOffTime = getCurrentCutOffTime(subServiceConfig, date);
        
        // Check for active extensions
        Optional<CutOffExtension> activeExtension = cutOffExtensionRepository
            .findActiveExtensionForService(tenantId, serviceName, subServiceName, date, currentTime);
        
        if (activeExtension.isPresent()) {
            LocalTime extendedTime = activeExtension.get().getExtendedCutOffTime();
            logger.debug("Using extended cut-off time {} for {}/{} on {}", 
                        extendedTime, serviceName, subServiceName, date);
            return extendedTime;
        }
        
        return configuredCutOffTime;
    }
    
    /**
     * Get all extensions for a tenant
     */
    public List<CutOffExtension> getExtensionsForTenant(String tenantId) {
        return cutOffExtensionRepository.findByTenantId(tenantId);
    }
    
    /**
     * Get pending extensions for approval
     */
    public List<CutOffExtension> getPendingExtensions() {
        return cutOffExtensionRepository.findPendingExtensionsOrderedByPriority();
    }
    
    /**
     * Activate approved extensions for today
     */
    @Transactional
    public void activateApprovedExtensionsForToday() {
        LocalDate today = LocalDate.now();
        List<CutOffExtension> approvedExtensions = cutOffExtensionRepository.findApprovedExtensionsForToday(today);
        
        for (CutOffExtension extension : approvedExtensions) {
            extension.activate();
            cutOffExtensionRepository.save(extension);
            
            logger.info("Activated cut-off extension: ID={}, Service={}/{}", 
                       extension.getId(), extension.getServiceName(), extension.getSubServiceName());
        }
    }
    
    /**
     * Expire active extensions that have passed their effective time
     */
    @Transactional
    public void expireOldExtensions() {
        LocalDateTime currentTime = LocalDateTime.now();
        List<CutOffExtension> expiredExtensions = cutOffExtensionRepository.findExpiredActiveExtensions(currentTime);
        
        for (CutOffExtension extension : expiredExtensions) {
            extension.expire();
            cutOffExtensionRepository.save(extension);
            
            logger.info("Expired cut-off extension: ID={}, Service={}/{}", 
                       extension.getId(), extension.getServiceName(), extension.getSubServiceName());
        }
    }
    
    private LocalTime getCurrentCutOffTime(SubServiceConfiguration config, LocalDate date) {
        // Use the existing CutOffTimeService logic
        return LocalTime.parse(config.getCutOffTime()); // Simplified for now
    }
    
    private boolean shouldAutoApprove(CutOffExtension extension, String requestedBy) {
        // Auto-approval rules - these could be configurable
        
        // Rule 1: Extensions less than 30 minutes for trusted users
        long extensionMinutes = extension.getExtensionDurationMinutes();
        if (extensionMinutes <= 30) {
            long recentApprovals = cutOffExtensionRepository.countRecentApprovedExtensionsByUser(
                requestedBy, LocalDateTime.now().minusDays(30));
            
            if (recentApprovals >= 5) { // User has good track record
                return true;
            }
        }
        
        // Rule 2: Same-day extensions for critical priority
        if (extension.getPriority() == CutOffExtension.ExtensionPriority.CRITICAL &&
            extension.getExtensionDate().equals(LocalDate.now())) {
            return true;
        }
        
        return false;
    }
    
    private void sendExtensionNotifications(CutOffExtension extension, String action) {
        try {
            // Create alert for extension event
            // This would integrate with your existing AlertService
            logger.info("Extension {} notification sent for ID={}", action, extension.getId());
            
            // TODO: Implement actual notification logic
            // - Email to approvers for new requests
            // - Email to requestor for approvals/rejections
            // - Dashboard notifications
            
        } catch (Exception e) {
            logger.error("Failed to send extension notifications for ID={}: {}", 
                        extension.getId(), e.getMessage());
        }
    }
}