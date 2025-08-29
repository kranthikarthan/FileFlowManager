package com.filetransfer.web.service;

import com.filetransfer.web.entity.SubServiceConfiguration;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.CutOffTimeType;
import com.filetransfer.web.repository.SubServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SubServiceConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubServiceConfigurationService.class);
    
    @Autowired
    private SubServiceConfigurationRepository subServiceConfigRepository;
    
    @Autowired
    private SecurityContextService securityContextService;
    
    @Autowired
    private CutOffExtensionService cutOffExtensionService;
    
    @Autowired
    private HolidayService holidayService;
    
    @Autowired
    private TenantTimeZoneService tenantTimeZoneService;
    
    /**
     * Get all sub-services for a tenant
     */
    public List<SubServiceConfiguration> getSubServicesForTenant(String tenantId) {
        return subServiceConfigRepository.findByTenantId(tenantId);
    }
    
    /**
     * Get enabled sub-services for a tenant
     */
    public List<SubServiceConfiguration> getEnabledSubServicesForTenant(String tenantId) {
        return subServiceConfigRepository.findByTenantIdAndEnabled(tenantId, true);
    }
    
    /**
     * Get sub-services for a specific service
     */
    public List<SubServiceConfiguration> getSubServicesForService(String tenantId, String serviceName) {
        return subServiceConfigRepository.findByTenantIdAndServiceName(tenantId, serviceName);
    }
    
    /**
     * Get a specific sub-service configuration
     */
    public Optional<SubServiceConfiguration> getSubServiceConfiguration(String tenantId, String serviceName, String subServiceName) {
        return subServiceConfigRepository.findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName);
    }
    
    /**
     * Create a new sub-service configuration
     */
    public SubServiceConfiguration createSubService(SubServiceConfiguration subServiceConfig) {
        // Validate unique constraint
        if (subServiceConfigRepository.existsByTenantIdAndServiceNameAndSubServiceName(
                subServiceConfig.getTenantId(), 
                subServiceConfig.getServiceName(), 
                subServiceConfig.getSubServiceName())) {
            throw new IllegalArgumentException(
                String.format("Sub-service %s/%s already exists for tenant %s", 
                             subServiceConfig.getServiceName(), 
                             subServiceConfig.getSubServiceName(), 
                             subServiceConfig.getTenantId()));
        }
        
        // Set audit fields
        subServiceConfig.setCreatedAt(LocalDateTime.now());
        subServiceConfig.setCreatedBy(securityContextService.getCurrentUserId());
        
        SubServiceConfiguration savedConfig = subServiceConfigRepository.save(subServiceConfig);
        
        logger.info("Created sub-service configuration: {}/{} for tenant {}", 
                   savedConfig.getServiceName(), savedConfig.getSubServiceName(), savedConfig.getTenantId());
        
        return savedConfig;
    }
    
    /**
     * Update a sub-service configuration
     */
    public SubServiceConfiguration updateSubService(Long id, SubServiceConfiguration subServiceConfig) {
        SubServiceConfiguration existingConfig = subServiceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sub-service configuration not found with id: " + id));
        
        // Check for unique constraint if name fields are changing
        if (!existingConfig.getServiceName().equals(subServiceConfig.getServiceName()) ||
            !existingConfig.getSubServiceName().equals(subServiceConfig.getSubServiceName())) {
            
            if (subServiceConfigRepository.existsByTenantIdAndServiceNameAndSubServiceNameAndIdNot(
                    subServiceConfig.getTenantId(),
                    subServiceConfig.getServiceName(),
                    subServiceConfig.getSubServiceName(),
                    id)) {
                throw new IllegalArgumentException(
                    String.format("Sub-service %s/%s already exists for tenant %s", 
                                 subServiceConfig.getServiceName(), 
                                 subServiceConfig.getSubServiceName(), 
                                 subServiceConfig.getTenantId()));
            }
        }
        
        // Update fields
        existingConfig.setServiceName(subServiceConfig.getServiceName());
        existingConfig.setSubServiceName(subServiceConfig.getSubServiceName());
        existingConfig.setInboundPath(subServiceConfig.getInboundPath());
        existingConfig.setOutboundPath(subServiceConfig.getOutboundPath());
        existingConfig.setEnabled(subServiceConfig.getEnabled());
        
        // Cut-off time configuration
        existingConfig.setCutOffTime(subServiceConfig.getCutOffTime());
        existingConfig.setCutOffTimeType(subServiceConfig.getCutOffTimeType());
        existingConfig.setWeekdayCutOffTime(subServiceConfig.getWeekdayCutOffTime());
        existingConfig.setWeekendCutOffTime(subServiceConfig.getWeekendCutOffTime());
        existingConfig.setMondayCutOffTime(subServiceConfig.getMondayCutOffTime());
        existingConfig.setTuesdayCutOffTime(subServiceConfig.getTuesdayCutOffTime());
        existingConfig.setWednesdayCutOffTime(subServiceConfig.getWednesdayCutOffTime());
        existingConfig.setThursdayCutOffTime(subServiceConfig.getThursdayCutOffTime());
        existingConfig.setFridayCutOffTime(subServiceConfig.getFridayCutOffTime());
        existingConfig.setSaturdayCutOffTime(subServiceConfig.getSaturdayCutOffTime());
        existingConfig.setSundayCutOffTime(subServiceConfig.getSundayCutOffTime());
        
        // File type and schema configuration
        existingConfig.setFileTypeSchemaMap(subServiceConfig.getFileTypeSchemaMap());
        existingConfig.setDirectionConfigs(subServiceConfig.getDirectionConfigs());
        
        // File patterns
        existingConfig.setStartMarkerPrefix(subServiceConfig.getStartMarkerPrefix());
        existingConfig.setEndMarkerPrefix(subServiceConfig.getEndMarkerPrefix());
        existingConfig.setDataFilePattern(subServiceConfig.getDataFilePattern());
        existingConfig.setSotFilePattern(subServiceConfig.getSotFilePattern());
        existingConfig.setEotFilePattern(subServiceConfig.getEotFilePattern());
        
        // Validation settings
        existingConfig.setSchemaValidationEnabled(subServiceConfig.getSchemaValidationEnabled());
        existingConfig.setBinaryFileBypass(subServiceConfig.getBinaryFileBypass());
        existingConfig.setSchemaValidationMode(subServiceConfig.getSchemaValidationMode());
        
        // Processing settings
        existingConfig.setMaxRetries(subServiceConfig.getMaxRetries());
        existingConfig.setPollIntervalSeconds(subServiceConfig.getPollIntervalSeconds());
        
        existingConfig.setDescription(subServiceConfig.getDescription());
        existingConfig.setUpdatedAt(LocalDateTime.now());
        existingConfig.setUpdatedBy(securityContextService.getCurrentUserId());
        
        SubServiceConfiguration savedConfig = subServiceConfigRepository.save(existingConfig);
        
        logger.info("Updated sub-service configuration: {}/{} for tenant {}", 
                   savedConfig.getServiceName(), savedConfig.getSubServiceName(), savedConfig.getTenantId());
        
        return savedConfig;
    }
    
    /**
     * Delete a sub-service configuration
     */
    public void deleteSubService(Long id) {
        SubServiceConfiguration subService = subServiceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sub-service configuration not found with id: " + id));
        
        // TODO: Add safety check for active file transfers (similar to ServiceConfigurationService)
        
        subServiceConfigRepository.delete(subService);
        
        logger.info("Deleted sub-service configuration: {}/{} for tenant {}", 
                   subService.getServiceName(), subService.getSubServiceName(), subService.getTenantId());
    }
    
    /**
     * Get effective cut-off time for a sub-service on a specific date
     */
    public LocalTime getEffectiveCutOffTime(String tenantId, String serviceName, String subServiceName, LocalDate date) {
        SubServiceConfiguration config = subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElseThrow(() -> new RuntimeException(
                String.format("Sub-service not found: %s/%s for tenant %s", serviceName, subServiceName, tenantId)));
        
        // Get the date in tenant timezone if not specified
        LocalDate tenantDate = date != null ? date : tenantTimeZoneService.getCurrentTenantDate(tenantId);
        
        // First check for any active extensions (timezone-aware)
        LocalDateTime currentTenantTime = tenantTimeZoneService.getCurrentTenantTime(tenantId);
        LocalTime effectiveTime = cutOffExtensionService.getEffectiveCutOffTime(
            tenantId, serviceName, subServiceName, tenantDate, currentTenantTime);
        
        // If no extension, use configured cut-off time
        if (effectiveTime.equals(getConfiguredCutOffTime(config, tenantDate))) {
            // Check if today is a holiday in tenant timezone
            boolean isHoliday = holidayService.isHoliday(tenantId, tenantDate);
            if (isHoliday) {
                // Use holiday cut-off time logic if configured
                // For now, extend by 2 hours as default holiday extension
                effectiveTime = effectiveTime.plusHours(2);
            }
        }
        
        return effectiveTime;
    }
    
    /**
     * Get configured cut-off time based on sub-service configuration
     */
    public LocalTime getConfiguredCutOffTime(SubServiceConfiguration config, LocalDate date) {
        if (config == null || date == null) {
            return LocalTime.parse("23:59:59");
        }
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        switch (config.getCutOffTimeType()) {
            case DAILY:
                return parseTime(config.getCutOffTime());
                
            case WEEKDAY_WEEKEND:
                return getCutOffTimeForWeekdayWeekend(config, dayOfWeek);
                
            case PER_DAY:
                return getCutOffTimeForSpecificDay(config, dayOfWeek);
                
            default:
                return parseTime(config.getCutOffTime());
        }
    }
    
    /**
     * Configure file type to schema mapping for a sub-service
     */
    public SubServiceConfiguration configureFileTypeSchemas(Long subServiceId, Map<FileType, Long> fileTypeSchemaMap) {
        SubServiceConfiguration config = subServiceConfigRepository.findById(subServiceId)
            .orElseThrow(() -> new RuntimeException("Sub-service configuration not found with id: " + subServiceId));
        
        config.setFileTypeSchemaMap(fileTypeSchemaMap);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(securityContextService.getCurrentUserId());
        
        return subServiceConfigRepository.save(config);
    }
    
    /**
     * Configure direction-specific settings for a sub-service
     */
    public SubServiceConfiguration configureDirectionSettings(Long subServiceId, Map<TransferDirection, String> directionConfigs) {
        SubServiceConfiguration config = subServiceConfigRepository.findById(subServiceId)
            .orElseThrow(() -> new RuntimeException("Sub-service configuration not found with id: " + subServiceId));
        
        config.setDirectionConfigs(directionConfigs);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(securityContextService.getCurrentUserId());
        
        return subServiceConfigRepository.save(config);
    }
    
    /**
     * Get schema ID for specific file type and direction
     */
    public Long getSchemaIdForFileType(String tenantId, String serviceName, String subServiceName, 
                                      FileType fileType, TransferDirection direction) {
        return subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .map(config -> config.getSchemaIdForFileType(fileType, direction))
            .orElse(null);
    }
    
    /**
     * Check if schema validation is required
     */
    public boolean requiresSchemaValidation(String tenantId, String serviceName, String subServiceName, FileType fileType) {
        return subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .map(config -> config.requiresSchemaValidation(fileType))
            .orElse(false);
    }
    
    private LocalTime getCutOffTimeForWeekdayWeekend(SubServiceConfiguration config, DayOfWeek dayOfWeek) {
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        if (isWeekend && config.getWeekendCutOffTime() != null) {
            return parseTime(config.getWeekendCutOffTime());
        } else if (!isWeekend && config.getWeekdayCutOffTime() != null) {
            return parseTime(config.getWeekdayCutOffTime());
        }
        
        return parseTime(config.getCutOffTime());
    }
    
    private LocalTime getCutOffTimeForSpecificDay(SubServiceConfiguration config, DayOfWeek dayOfWeek) {
        String daySpecificTime = null;
        
        switch (dayOfWeek) {
            case MONDAY:
                daySpecificTime = config.getMondayCutOffTime();
                break;
            case TUESDAY:
                daySpecificTime = config.getTuesdayCutOffTime();
                break;
            case WEDNESDAY:
                daySpecificTime = config.getWednesdayCutOffTime();
                break;
            case THURSDAY:
                daySpecificTime = config.getThursdayCutOffTime();
                break;
            case FRIDAY:
                daySpecificTime = config.getFridayCutOffTime();
                break;
            case SATURDAY:
                daySpecificTime = config.getSaturdayCutOffTime();
                break;
            case SUNDAY:
                daySpecificTime = config.getSundayCutOffTime();
                break;
        }
        
        if (daySpecificTime != null) {
            return parseTime(daySpecificTime);
        }
        
        return parseTime(config.getCutOffTime());
    }
    
    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            logger.warn("Failed to parse time '{}', using default 23:59:59", timeStr);
            return LocalTime.parse("23:59:59");
        }
    }
    
    /**
     * Check if current time is within processing window (timezone-aware)
     */
    public boolean isInProcessingWindow(String tenantId, String serviceName, String subServiceName) {
        return isInProcessingWindow(tenantId, serviceName, subServiceName, null);
    }
    
    /**
     * Check if current time is within processing window for specific date (timezone-aware)
     */
    public boolean isInProcessingWindow(String tenantId, String serviceName, String subServiceName, LocalDate date) {
        try {
            LocalDate targetDate = date != null ? date : tenantTimeZoneService.getCurrentTenantDate(tenantId);
            LocalTime cutOffTime = getEffectiveCutOffTime(tenantId, serviceName, subServiceName, targetDate);
            
            return tenantTimeZoneService.isInProcessingWindow(tenantId, targetDate, cutOffTime);
            
        } catch (Exception e) {
            logger.error("Error checking processing window for {}/{}: {}", serviceName, subServiceName, e.getMessage());
            return false; // Fail safe - don't process if we can't determine
        }
    }
    
    /**
     * Get time until cut-off in tenant timezone
     */
    public java.time.Duration getTimeUntilCutOff(String tenantId, String serviceName, String subServiceName) {
        return getTimeUntilCutOff(tenantId, serviceName, subServiceName, null);
    }
    
    /**
     * Get time until cut-off for specific date in tenant timezone
     */
    public java.time.Duration getTimeUntilCutOff(String tenantId, String serviceName, String subServiceName, LocalDate date) {
        try {
            LocalDate targetDate = date != null ? date : tenantTimeZoneService.getCurrentTenantDate(tenantId);
            LocalTime cutOffTime = getEffectiveCutOffTime(tenantId, serviceName, subServiceName, targetDate);
            
            return tenantTimeZoneService.getTimeUntilCutOff(tenantId, targetDate, cutOffTime);
            
        } catch (Exception e) {
            logger.error("Error calculating time until cut-off for {}/{}: {}", serviceName, subServiceName, e.getMessage());
            return java.time.Duration.ZERO;
        }
    }
}