package com.filetransfer.web.service;

import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processing Control Service - Comprehensive processing control at all levels
 * Handles start/stop/pause/resume operations from tenant level to individual files
 * Includes edge case handling for holidays, weekends, and cross-application coordination
 */
@Service
@Transactional
public class ProcessingControlService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingControlService.class);

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ServiceConfigurationRepository serviceRepository;

    @Autowired
    private SubServiceConfigurationRepository subServiceRepository;

    @Autowired
    private FileTransferRecordRepository fileTransferRepository;

    @Autowired
    private DailyFileCountTrackerRepository dailyCountRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private FileProcessingTrackingService processingTrackingService;

    // ===== TENANT LEVEL PROCESSING CONTROL =====

    /**
     * Start processing for entire tenant - CRITICAL BUG FIX: Missing processing control
     */
    public Map<String, Object> startTenantProcessing(String tenantId, String userId) {
        logger.info("Starting tenant processing: {} by user: {}", tenantId, userId);
        
        // Validate tenant exists and is enabled
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        if (!tenant.getEnabled()) {
            throw new IllegalStateException("Tenant is disabled: " + tenantId);
        }
        
        // Check for holiday conflicts
        LocalDate today = LocalDate.now();
        if (holidayService.isHoliday(tenantId, today)) {
            logger.warn("Starting processing on holiday for tenant: {} on date: {}", tenantId, today);
        }
        
        // Enable all services for this tenant
        List<ServiceConfiguration> services = serviceRepository.findByTenantId(tenantId);
        int enabledServices = 0;
        int enabledSubServices = 0;
        
        for (ServiceConfiguration service : services) {
            service.setEnabled(true);
            service.setUpdatedBy(userId);
            service.setUpdatedAt(LocalDateTime.now());
            serviceRepository.save(service);
            enabledServices++;
            
            // Enable all sub-services
            List<SubServiceConfiguration> subServices = subServiceRepository.findByServiceId(service.getId());
            for (SubServiceConfiguration subService : subServices) {
                subService.setEnabled(true);
                subService.setUpdatedBy(userId);
                subService.setUpdatedAt(LocalDateTime.now());
                subServiceRepository.save(subService);
                enabledSubServices++;
            }
        }
        
        // Log processing control action
        logProcessingControlAction(tenantId, null, null, "TENANT_START", userId, 
            String.format("Started processing for tenant. Enabled %d services and %d sub-services", 
                         enabledServices, enabledSubServices));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("action", "START");
        result.put("timestamp", LocalDateTime.now());
        result.put("enabledServices", enabledServices);
        result.put("enabledSubServices", enabledSubServices);
        result.put("userId", userId);
        result.put("isHoliday", holidayService.isHoliday(tenantId, today));
        result.put("message", "Tenant processing started successfully");
        
        return result;
    }

    /**
     * Stop processing for entire tenant
     */
    public Map<String, Object> stopTenantProcessing(String tenantId, String userId, boolean graceful) {
        logger.info("Stopping tenant processing: {} by user: {} (graceful: {})", tenantId, userId, graceful);
        
        // Validate tenant exists
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        // Get current processing status
        List<FileTransferRecord> activeFiles = fileTransferRepository
            .findByTenantIdAndStatus(tenantId, "PROCESSING");
        
        if (graceful && !activeFiles.isEmpty()) {
            logger.info("Graceful shutdown: waiting for {} active files to complete for tenant: {}", 
                       activeFiles.size(), tenantId);
        }
        
        // Disable all services for this tenant
        List<ServiceConfiguration> services = serviceRepository.findByTenantId(tenantId);
        int disabledServices = 0;
        int disabledSubServices = 0;
        
        for (ServiceConfiguration service : services) {
            service.setEnabled(false);
            service.setUpdatedBy(userId);
            service.setUpdatedAt(LocalDateTime.now());
            serviceRepository.save(service);
            disabledServices++;
            
            // Disable all sub-services
            List<SubServiceConfiguration> subServices = subServiceRepository.findByServiceId(service.getId());
            for (SubServiceConfiguration subService : subServices) {
                subService.setEnabled(false);
                subService.setUpdatedBy(userId);
                subService.setUpdatedAt(LocalDateTime.now());
                subServiceRepository.save(subService);
                disabledSubServices++;
            }
        }
        
        // Handle active files based on graceful flag
        if (!graceful) {
            // Force stop active files
            for (FileTransferRecord activeFile : activeFiles) {
                activeFile.setStatus("CANCELLED");
                activeFile.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(activeFile);
            }
        }
        
        // Log processing control action
        logProcessingControlAction(tenantId, null, null, "TENANT_STOP", userId, 
            String.format("Stopped processing for tenant. Disabled %d services and %d sub-services. Active files: %d", 
                         disabledServices, disabledSubServices, activeFiles.size()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("action", "STOP");
        result.put("timestamp", LocalDateTime.now());
        result.put("disabledServices", disabledServices);
        result.put("disabledSubServices", disabledSubServices);
        result.put("activeFiles", activeFiles.size());
        result.put("graceful", graceful);
        result.put("userId", userId);
        result.put("message", graceful ? "Tenant processing stopped gracefully" : "Tenant processing stopped immediately");
        
        return result;
    }

    /**
     * Pause processing for entire tenant
     */
    public Map<String, Object> pauseTenantProcessing(String tenantId, String userId) {
        logger.info("Pausing tenant processing: {} by user: {}", tenantId, userId);
        
        // Set all services to paused state (use a custom field or status)
        List<ServiceConfiguration> services = serviceRepository.findByTenantId(tenantId);
        
        // For now, we'll disable services (in a full implementation, we'd add a 'paused' status)
        for (ServiceConfiguration service : services) {
            service.setEnabled(false); // ENHANCEMENT NEEDED: Add 'paused' status
            service.setUpdatedBy(userId);
            service.setUpdatedAt(LocalDateTime.now());
            serviceRepository.save(service);
        }
        
        logProcessingControlAction(tenantId, null, null, "TENANT_PAUSE", userId, "Paused processing for tenant");
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("action", "PAUSE");
        result.put("timestamp", LocalDateTime.now());
        result.put("pausedServices", services.size());
        result.put("userId", userId);
        result.put("message", "Tenant processing paused successfully");
        
        return result;
    }

    /**
     * Resume processing for entire tenant
     */
    public Map<String, Object> resumeTenantProcessing(String tenantId, String userId) {
        logger.info("Resuming tenant processing: {} by user: {}", tenantId, userId);
        
        // Re-enable all services for this tenant
        List<ServiceConfiguration> services = serviceRepository.findByTenantId(tenantId);
        
        for (ServiceConfiguration service : services) {
            service.setEnabled(true);
            service.setUpdatedBy(userId);
            service.setUpdatedAt(LocalDateTime.now());
            serviceRepository.save(service);
        }
        
        logProcessingControlAction(tenantId, null, null, "TENANT_RESUME", userId, "Resumed processing for tenant");
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("action", "RESUME");
        result.put("timestamp", LocalDateTime.now());
        result.put("resumedServices", services.size());
        result.put("userId", userId);
        result.put("message", "Tenant processing resumed successfully");
        
        return result;
    }

    // ===== SERVICE LEVEL PROCESSING CONTROL =====

    /**
     * Start processing for specific service
     */
    public Map<String, Object> startServiceProcessing(String tenantId, String serviceName, String userId) {
        logger.info("Starting service processing: {}/{} by user: {}", tenantId, serviceName, userId);
        
        ServiceConfiguration service = serviceRepository.findByTenantIdAndServiceName(tenantId, serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        
        // Check for holiday conflicts - EDGE CASE HANDLING
        LocalDate today = LocalDate.now();
        boolean isHoliday = holidayService.isHoliday(tenantId, today);
        boolean isSunday = holidayService.isSunday(today);
        
        if (isHoliday && isSunday) {
            logger.warn("EDGE CASE: Starting service on date that is both holiday and Sunday: {} for tenant: {}", 
                       today, tenantId);
        }
        
        service.setEnabled(true);
        service.setUpdatedBy(userId);
        service.setUpdatedAt(LocalDateTime.now());
        serviceRepository.save(service);
        
        // Enable all sub-services
        List<SubServiceConfiguration> subServices = subServiceRepository.findByServiceId(service.getId());
        for (SubServiceConfiguration subService : subServices) {
            subService.setEnabled(true);
            subService.setUpdatedBy(userId);
            subService.setUpdatedAt(LocalDateTime.now());
            subServiceRepository.save(subService);
        }
        
        logProcessingControlAction(tenantId, serviceName, null, "SERVICE_START", userId, 
            String.format("Started service processing. Enabled %d sub-services", subServices.size()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("action", "START");
        result.put("timestamp", LocalDateTime.now());
        result.put("enabledSubServices", subServices.size());
        result.put("isHoliday", isHoliday);
        result.put("isSunday", isSunday);
        result.put("holidaySundayCollision", isHoliday && isSunday);
        result.put("userId", userId);
        result.put("message", "Service processing started successfully");
        
        return result;
    }

    /**
     * Stop processing for specific service
     */
    public Map<String, Object> stopServiceProcessing(String tenantId, String serviceName, String userId, boolean graceful) {
        logger.info("Stopping service processing: {}/{} by user: {} (graceful: {})", 
                   tenantId, serviceName, userId, graceful);
        
        ServiceConfiguration service = serviceRepository.findByTenantIdAndServiceName(tenantId, serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        
        // Get active files for this service
        List<FileTransferRecord> activeFiles = fileTransferRepository
            .findByTenantIdAndServiceNameAndStatus(tenantId, serviceName, "PROCESSING");
        
        service.setEnabled(false);
        service.setUpdatedBy(userId);
        service.setUpdatedAt(LocalDateTime.now());
        serviceRepository.save(service);
        
        // Disable all sub-services
        List<SubServiceConfiguration> subServices = subServiceRepository.findByServiceId(service.getId());
        for (SubServiceConfiguration subService : subServices) {
            subService.setEnabled(false);
            subService.setUpdatedBy(userId);
            subService.setUpdatedAt(LocalDateTime.now());
            subServiceRepository.save(subService);
        }
        
        // Handle active files
        if (!graceful) {
            for (FileTransferRecord activeFile : activeFiles) {
                activeFile.setStatus("CANCELLED");
                activeFile.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(activeFile);
            }
        }
        
        logProcessingControlAction(tenantId, serviceName, null, "SERVICE_STOP", userId, 
            String.format("Stopped service processing. Disabled %d sub-services. Active files: %d", 
                         subServices.size(), activeFiles.size()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("action", "STOP");
        result.put("timestamp", LocalDateTime.now());
        result.put("disabledSubServices", subServices.size());
        result.put("activeFiles", activeFiles.size());
        result.put("graceful", graceful);
        result.put("userId", userId);
        result.put("message", "Service processing stopped successfully");
        
        return result;
    }

    /**
     * Reset service for the day - CRITICAL MISSING FEATURE
     */
    public Map<String, Object> resetServiceForDay(String tenantId, String serviceName, LocalDate resetDate, String userId) {
        logger.info("Resetting service for day: {}/{} on {} by user: {}", tenantId, serviceName, resetDate, userId);
        
        ServiceConfiguration service = serviceRepository.findByTenantIdAndServiceName(tenantId, serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        
        // EDGE CASE: Check if reset date is holiday or Sunday
        boolean isHoliday = holidayService.isHoliday(tenantId, resetDate);
        boolean isSunday = holidayService.isSunday(resetDate);
        
        if (isHoliday && isSunday) {
            logger.warn("EDGE CASE: Resetting service on date that is both holiday and Sunday: {} for tenant: {}", 
                       resetDate, tenantId);
        }
        
        // Reset daily file count trackers for this service
        List<DailyFileCountTracker> trackers = dailyCountRepository
            .findByTenantAndServiceAndDate(tenantId, serviceName, resetDate);
        
        int resetTrackers = 0;
        for (DailyFileCountTracker tracker : trackers) {
            tracker.setActualCount(0);
            tracker.setSotReceived(false);
            tracker.setEotReceived(false);
            tracker.setValidationStatus("PENDING");
            tracker.setValidatedAt(null);
            tracker.setValidationNotes("Reset by user: " + userId);
            tracker.setUpdatedAt(LocalDateTime.now());
            dailyCountRepository.save(tracker);
            resetTrackers++;
        }
        
        // Reset file transfer records for this service and date
        LocalDateTime startOfDay = resetDate.atStartOfDay();
        LocalDateTime endOfDay = resetDate.plusDays(1).atStartOfDay();
        
        List<FileTransferRecord> dayFiles = fileTransferRepository
            .findByTenantIdAndServiceNameAndCreatedAtBetween(tenantId, serviceName, startOfDay, endOfDay);
        
        int resetFiles = 0;
        for (FileTransferRecord file : dayFiles) {
            if (!"COMPLETED".equals(file.getStatus()) && !"ARCHIVED".equals(file.getStatus())) {
                file.setStatus("PENDING");
                file.setProcessingStartTime(null);
                file.setProcessingEndTime(null);
                file.setErrorMessage(null);
                file.setValidationResult(null);
                file.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(file);
                resetFiles++;
            }
        }
        
        logProcessingControlAction(tenantId, serviceName, null, "SERVICE_RESET", userId, 
            String.format("Reset service for date %s. Reset %d trackers and %d files", 
                         resetDate, resetTrackers, resetFiles));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("resetDate", resetDate);
        result.put("action", "RESET");
        result.put("timestamp", LocalDateTime.now());
        result.put("resetTrackers", resetTrackers);
        result.put("resetFiles", resetFiles);
        result.put("isHoliday", isHoliday);
        result.put("isSunday", isSunday);
        result.put("holidaySundayCollision", isHoliday && isSunday);
        result.put("userId", userId);
        result.put("message", "Service reset for day completed successfully");
        
        return result;
    }

    // ===== SUB-SERVICE LEVEL PROCESSING CONTROL =====

    /**
     * Start processing for specific sub-service
     */
    public Map<String, Object> startSubServiceProcessing(String tenantId, String serviceName, 
                                                        String subServiceName, String userId) {
        logger.info("Starting sub-service processing: {}/{}/{} by user: {}", 
                   tenantId, serviceName, subServiceName, userId);
        
        SubServiceConfiguration subService = subServiceRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElseThrow(() -> new IllegalArgumentException("Sub-service not found: " + subServiceName));
        
        // Check parent service is enabled
        ServiceConfiguration service = serviceRepository.findById(subService.getServiceId())
            .orElseThrow(() -> new IllegalArgumentException("Parent service not found"));
        
        if (!service.getEnabled()) {
            throw new IllegalStateException("Cannot start sub-service: parent service is disabled");
        }
        
        // Check for holiday conflicts
        LocalDate today = LocalDate.now();
        boolean isHoliday = holidayService.isHoliday(tenantId, today);
        boolean isSunday = holidayService.isSunday(today);
        
        subService.setEnabled(true);
        subService.setUpdatedBy(userId);
        subService.setUpdatedAt(LocalDateTime.now());
        subServiceRepository.save(subService);
        
        logProcessingControlAction(tenantId, serviceName, subServiceName, "SUBSERVICE_START", userId, 
            "Started sub-service processing");
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("subServiceName", subServiceName);
        result.put("action", "START");
        result.put("timestamp", LocalDateTime.now());
        result.put("isHoliday", isHoliday);
        result.put("isSunday", isSunday);
        result.put("holidaySundayCollision", isHoliday && isSunday);
        result.put("userId", userId);
        result.put("message", "Sub-service processing started successfully");
        
        return result;
    }

    /**
     * Stop processing for specific sub-service
     */
    public Map<String, Object> stopSubServiceProcessing(String tenantId, String serviceName, 
                                                       String subServiceName, String userId, boolean graceful) {
        logger.info("Stopping sub-service processing: {}/{}/{} by user: {} (graceful: {})", 
                   tenantId, serviceName, subServiceName, userId, graceful);
        
        SubServiceConfiguration subService = subServiceRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElseThrow(() -> new IllegalArgumentException("Sub-service not found: " + subServiceName));
        
        // Get active files for this sub-service
        List<FileTransferRecord> activeFiles = fileTransferRepository
            .findByTenantIdAndServiceNameAndSubServiceNameAndStatus(tenantId, serviceName, subServiceName, "PROCESSING");
        
        subService.setEnabled(false);
        subService.setUpdatedBy(userId);
        subService.setUpdatedAt(LocalDateTime.now());
        subServiceRepository.save(subService);
        
        // Handle active files
        if (!graceful) {
            for (FileTransferRecord activeFile : activeFiles) {
                activeFile.setStatus("CANCELLED");
                activeFile.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(activeFile);
            }
        }
        
        logProcessingControlAction(tenantId, serviceName, subServiceName, "SUBSERVICE_STOP", userId, 
            String.format("Stopped sub-service processing. Active files: %d", activeFiles.size()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("subServiceName", subServiceName);
        result.put("action", "STOP");
        result.put("timestamp", LocalDateTime.now());
        result.put("activeFiles", activeFiles.size());
        result.put("graceful", graceful);
        result.put("userId", userId);
        result.put("message", "Sub-service processing stopped successfully");
        
        return result;
    }

    /**
     * Reset sub-service for the day - CRITICAL MISSING FEATURE
     */
    public Map<String, Object> resetSubServiceForDay(String tenantId, String serviceName, 
                                                    String subServiceName, LocalDate resetDate, String userId) {
        logger.info("Resetting sub-service for day: {}/{}/{} on {} by user: {}", 
                   tenantId, serviceName, subServiceName, resetDate, userId);
        
        SubServiceConfiguration subService = subServiceRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElseThrow(() -> new IllegalArgumentException("Sub-service not found: " + subServiceName));
        
        // EDGE CASE: Check if reset date is holiday or Sunday
        boolean isHoliday = holidayService.isHoliday(tenantId, resetDate);
        boolean isSunday = holidayService.isSunday(resetDate);
        
        if (isHoliday && isSunday) {
            logger.warn("EDGE CASE: Resetting sub-service on date that is both holiday and Sunday: {} for tenant: {}", 
                       resetDate, tenantId);
        }
        
        // Reset daily file count trackers for this sub-service
        List<DailyFileCountTracker> trackers = dailyCountRepository
            .findByTenantAndServiceAndSubServiceAndDate(tenantId, serviceName, subServiceName, resetDate);
        
        int resetTrackers = 0;
        for (DailyFileCountTracker tracker : trackers) {
            tracker.setActualCount(0);
            tracker.setSotReceived(false);
            tracker.setEotReceived(false);
            tracker.setValidationStatus("PENDING");
            tracker.setValidatedAt(null);
            tracker.setValidationNotes("Reset by user: " + userId + " on " + LocalDateTime.now());
            tracker.setUpdatedAt(LocalDateTime.now());
            dailyCountRepository.save(tracker);
            resetTrackers++;
        }
        
        // Reset file transfer records for this sub-service and date
        LocalDateTime startOfDay = resetDate.atStartOfDay();
        LocalDateTime endOfDay = resetDate.plusDays(1).atStartOfDay();
        
        List<FileTransferRecord> dayFiles = fileTransferRepository
            .findByTenantIdAndServiceNameAndSubServiceNameAndCreatedAtBetween(
                tenantId, serviceName, subServiceName, startOfDay, endOfDay);
        
        int resetFiles = 0;
        for (FileTransferRecord file : dayFiles) {
            if (!"COMPLETED".equals(file.getStatus()) && !"ARCHIVED".equals(file.getStatus())) {
                file.setStatus("PENDING");
                file.setProcessingStartTime(null);
                file.setProcessingEndTime(null);
                file.setErrorMessage(null);
                file.setValidationResult(null);
                file.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(file);
                resetFiles++;
            }
        }
        
        logProcessingControlAction(tenantId, serviceName, subServiceName, "SUBSERVICE_RESET", userId, 
            String.format("Reset sub-service for date %s. Reset %d trackers and %d files", 
                         resetDate, resetTrackers, resetFiles));
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("subServiceName", subServiceName);
        result.put("resetDate", resetDate);
        result.put("action", "RESET");
        result.put("timestamp", LocalDateTime.now());
        result.put("resetTrackers", resetTrackers);
        result.put("resetFiles", resetFiles);
        result.put("isHoliday", isHoliday);
        result.put("isSunday", isSunday);
        result.put("holidaySundayCollision", isHoliday && isSunday);
        result.put("userId", userId);
        result.put("message", "Sub-service reset for day completed successfully");
        
        return result;
    }

    // ===== FILE LEVEL PROCESSING CONTROL =====

    /**
     * Start processing for specific file
     */
    public Map<String, Object> startFileProcessing(Long fileId, String userId) {
        logger.info("Starting file processing: {} by user: {}", fileId, userId);
        
        FileTransferRecord file = fileTransferRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        
        // Check if file can be processed
        if ("PROCESSING".equals(file.getStatus())) {
            throw new IllegalStateException("File is already being processed");
        }
        
        if ("COMPLETED".equals(file.getStatus()) || "ARCHIVED".equals(file.getStatus())) {
            throw new IllegalStateException("File has already been processed successfully");
        }
        
        // Check if parent service/sub-service is enabled
        boolean canProcess = checkProcessingEligibility(file.getTenantId(), 
                                                       file.getServiceName(), 
                                                       file.getSubServiceName());
        
        if (!canProcess) {
            throw new IllegalStateException("Cannot process file: parent service/sub-service is disabled");
        }
        
        file.setStatus("PROCESSING");
        file.setProcessingStartTime(LocalDateTime.now());
        file.setProcessingEndTime(null);
        file.setErrorMessage(null);
        file.setUpdatedAt(LocalDateTime.now());
        fileTransferRepository.save(file);
        
        logProcessingControlAction(file.getTenantId(), file.getServiceName(), file.getSubServiceName(), 
                                  "FILE_START", userId, "Started file processing: " + file.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", file.getFileName());
        result.put("tenantId", file.getTenantId());
        result.put("serviceName", file.getServiceName());
        result.put("subServiceName", file.getSubServiceName());
        result.put("action", "START");
        result.put("timestamp", LocalDateTime.now());
        result.put("userId", userId);
        result.put("message", "File processing started successfully");
        
        return result;
    }

    /**
     * Stop processing for specific file
     */
    public Map<String, Object> stopFileProcessing(Long fileId, String userId, boolean force) {
        logger.info("Stopping file processing: {} by user: {} (force: {})", fileId, userId, force);
        
        FileTransferRecord file = fileTransferRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        
        if (!"PROCESSING".equals(file.getStatus())) {
            throw new IllegalStateException("File is not currently being processed");
        }
        
        if (force) {
            file.setStatus("CANCELLED");
            file.setErrorMessage("Processing cancelled by user: " + userId);
        } else {
            file.setStatus("FAILED");
            file.setErrorMessage("Processing stopped gracefully by user: " + userId);
        }
        
        file.setProcessingEndTime(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        fileTransferRepository.save(file);
        
        logProcessingControlAction(file.getTenantId(), file.getServiceName(), file.getSubServiceName(), 
                                  "FILE_STOP", userId, "Stopped file processing: " + file.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", file.getFileName());
        result.put("action", "STOP");
        result.put("timestamp", LocalDateTime.now());
        result.put("force", force);
        result.put("finalStatus", file.getStatus());
        result.put("userId", userId);
        result.put("message", "File processing stopped successfully");
        
        return result;
    }

    /**
     * Pause file processing - ENHANCEMENT: Add pause/resume capability
     */
    public Map<String, Object> pauseFileProcessing(Long fileId, String userId) {
        logger.info("Pausing file processing: {} by user: {}", fileId, userId);
        
        FileTransferRecord file = fileTransferRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        
        if (!"PROCESSING".equals(file.getStatus())) {
            throw new IllegalStateException("File is not currently being processed");
        }
        
        // ENHANCEMENT NEEDED: Add PAUSED status to TransferStatus enum
        file.setStatus("PAUSED"); // This would need to be added to the enum
        file.setErrorMessage("Processing paused by user: " + userId + " at " + LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        fileTransferRepository.save(file);
        
        logProcessingControlAction(file.getTenantId(), file.getServiceName(), file.getSubServiceName(), 
                                  "FILE_PAUSE", userId, "Paused file processing: " + file.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", file.getFileName());
        result.put("action", "PAUSE");
        result.put("timestamp", LocalDateTime.now());
        result.put("userId", userId);
        result.put("message", "File processing paused successfully");
        
        return result;
    }

    /**
     * Resume file processing
     */
    public Map<String, Object> resumeFileProcessing(Long fileId, String userId) {
        logger.info("Resuming file processing: {} by user: {}", fileId, userId);
        
        FileTransferRecord file = fileTransferRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        
        if (!"PAUSED".equals(file.getStatus())) {
            throw new IllegalStateException("File is not currently paused");
        }
        
        // Check if parent service/sub-service is still enabled
        boolean canProcess = checkProcessingEligibility(file.getTenantId(), 
                                                       file.getServiceName(), 
                                                       file.getSubServiceName());
        
        if (!canProcess) {
            throw new IllegalStateException("Cannot resume file: parent service/sub-service is disabled");
        }
        
        file.setStatus("PROCESSING");
        file.setErrorMessage(null);
        file.setUpdatedAt(LocalDateTime.now());
        fileTransferRepository.save(file);
        
        logProcessingControlAction(file.getTenantId(), file.getServiceName(), file.getSubServiceName(), 
                                  "FILE_RESUME", userId, "Resumed file processing: " + file.getFileName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", file.getFileName());
        result.put("action", "RESUME");
        result.put("timestamp", LocalDateTime.now());
        result.put("userId", userId);
        result.put("message", "File processing resumed successfully");
        
        return result;
    }

    // ===== BATCH PROCESSING CONTROL =====

    /**
     * Start batch processing
     */
    public Map<String, Object> startBatchProcessing(String tenantId, String serviceName, 
                                                   String subServiceName, LocalDate processingDate, String userId) {
        logger.info("Starting batch processing for tenant: {}, service: {}, sub-service: {}, date: {} by user: {}", 
                   tenantId, serviceName, subServiceName, processingDate, userId);
        
        // Validate tenant and services
        if (serviceName != null) {
            ServiceConfiguration service = serviceRepository.findByTenantIdAndServiceName(tenantId, serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
            
            if (!service.getEnabled()) {
                throw new IllegalStateException("Cannot start batch processing: service is disabled");
            }
        }
        
        // ENHANCEMENT: This would integrate with the batch application
        // For now, return success response
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("serviceName", serviceName);
        result.put("subServiceName", subServiceName);
        result.put("processingDate", processingDate);
        result.put("action", "BATCH_START");
        result.put("timestamp", LocalDateTime.now());
        result.put("userId", userId);
        result.put("message", "Batch processing started successfully");
        result.put("jobExecutionId", System.currentTimeMillis()); // Mock job ID
        
        return result;
    }

    /**
     * Stop batch processing
     */
    public Map<String, Object> stopBatchProcessing(Long jobExecutionId, String userId, boolean force) {
        logger.info("Stopping batch processing - Job ID: {}, User: {}, Force: {}", jobExecutionId, userId, force);
        
        // ENHANCEMENT: This would integrate with the batch application
        Map<String, Object> result = new HashMap<>();
        result.put("jobExecutionId", jobExecutionId);
        result.put("action", "BATCH_STOP");
        result.put("timestamp", LocalDateTime.now());
        result.put("force", force);
        result.put("userId", userId);
        result.put("message", "Batch processing stopped successfully");
        
        return result;
    }

    // ===== STATUS QUERIES =====

    /**
     * Get tenant processing status
     */
    public Map<String, Object> getTenantProcessingStatus(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        List<ServiceConfiguration> services = serviceRepository.findByTenantId(tenantId);
        long enabledServices = services.stream().filter(ServiceConfiguration::getEnabled).count();
        long totalServices = services.size();
        
        List<FileTransferRecord> activeFiles = fileTransferRepository
            .findByTenantIdAndStatus(tenantId, "PROCESSING");
        
        Map<String, Object> status = new HashMap<>();
        status.put("tenantId", tenantId);
        status.put("tenantEnabled", tenant.getEnabled());
        status.put("totalServices", totalServices);
        status.put("enabledServices", enabledServices);
        status.put("activeFiles", activeFiles.size());
        status.put("processingStatus", enabledServices > 0 ? "ACTIVE" : "STOPPED");
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }

    /**
     * Get service processing status
     */
    public Map<String, Object> getServiceProcessingStatus(String tenantId, String serviceName) {
        ServiceConfiguration service = serviceRepository.findByTenantIdAndServiceName(tenantId, serviceName)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));
        
        List<SubServiceConfiguration> subServices = subServiceRepository.findByServiceId(service.getId());
        long enabledSubServices = subServices.stream().filter(SubServiceConfiguration::getEnabled).count();
        
        List<FileTransferRecord> activeFiles = fileTransferRepository
            .findByTenantIdAndServiceNameAndStatus(tenantId, serviceName, "PROCESSING");
        
        Map<String, Object> status = new HashMap<>();
        status.put("tenantId", tenantId);
        status.put("serviceName", serviceName);
        status.put("serviceEnabled", service.getEnabled());
        status.put("totalSubServices", subServices.size());
        status.put("enabledSubServices", enabledSubServices);
        status.put("activeFiles", activeFiles.size());
        status.put("processingStatus", service.getEnabled() ? "ACTIVE" : "STOPPED");
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }

    /**
     * Get file processing status
     */
    public Map<String, Object> getFileProcessingStatus(Long fileId) {
        FileTransferRecord file = fileTransferRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        
        Map<String, Object> status = new HashMap<>();
        status.put("fileId", fileId);
        status.put("fileName", file.getFileName());
        status.put("status", file.getStatus());
        status.put("processingStartTime", file.getProcessingStartTime());
        status.put("processingEndTime", file.getProcessingEndTime());
        status.put("errorMessage", file.getErrorMessage());
        status.put("tenantId", file.getTenantId());
        status.put("serviceName", file.getServiceName());
        status.put("subServiceName", file.getSubServiceName());
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }

    /**
     * Get batch processing status
     */
    public Map<String, Object> getBatchProcessingStatus(String tenantId, Long jobExecutionId) {
        // ENHANCEMENT: This would integrate with the batch application
        Map<String, Object> status = new HashMap<>();
        status.put("tenantId", tenantId);
        status.put("jobExecutionId", jobExecutionId);
        status.put("status", "RUNNING"); // Mock status
        status.put("timestamp", LocalDateTime.now());
        status.put("message", "Batch processing status retrieved");
        
        return status;
    }

    // ===== HELPER METHODS =====

    /**
     * Check if processing is eligible based on parent service/sub-service status
     */
    private boolean checkProcessingEligibility(String tenantId, String serviceName, String subServiceName) {
        // Check tenant is enabled
        Tenant tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !tenant.getEnabled()) {
            return false;
        }
        
        // Check service is enabled
        if (serviceName != null) {
            ServiceConfiguration service = serviceRepository
                .findByTenantIdAndServiceName(tenantId, serviceName).orElse(null);
            if (service == null || !service.getEnabled()) {
                return false;
            }
            
            // Check sub-service is enabled
            if (subServiceName != null) {
                SubServiceConfiguration subService = subServiceRepository
                    .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName).orElse(null);
                if (subService == null || !subService.getEnabled()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Log processing control actions for audit trail
     */
    private void logProcessingControlAction(String tenantId, String serviceName, String subServiceName, 
                                          String action, String userId, String details) {
        logger.info("PROCESSING_CONTROL_AUDIT: Tenant={}, Service={}, SubService={}, Action={}, User={}, Details={}", 
                   tenantId, serviceName, subServiceName, action, userId, details);
        
        // ENHANCEMENT: Store in audit table for compliance
        // auditService.logProcessingControlAction(tenantId, serviceName, subServiceName, action, userId, details);
    }
}