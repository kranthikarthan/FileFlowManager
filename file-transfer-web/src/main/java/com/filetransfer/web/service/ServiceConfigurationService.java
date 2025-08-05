package com.filetransfer.web.service;

import com.filetransfer.web.entity.ServiceConfiguration;
import com.filetransfer.web.repository.ServiceConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@Transactional
public class ServiceConfigurationService {
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigRepository;
    
    @Autowired
    private CutOffTimeService cutOffTimeService;
    
    public List<ServiceConfiguration> getServicesForTenant(String tenantId) {
        return serviceConfigRepository.findByTenantId(tenantId);
    }
    
    public List<ServiceConfiguration> getEnabledServicesForTenant(String tenantId) {
        return serviceConfigRepository.findAllEnabledServicesForTenant(tenantId);
    }
    
    public List<ServiceConfiguration> getServicesByNameForTenant(String tenantId, String serviceName) {
        return serviceConfigRepository.findByTenantIdAndServiceName(tenantId, serviceName);
    }
    
    public List<String> getSubServicesForService(String tenantId, String serviceName) {
        return serviceConfigRepository.findSubServicesForService(tenantId, serviceName);
    }
    
    public Optional<ServiceConfiguration> getServiceById(Long id) {
        return serviceConfigRepository.findById(id);
    }
    
    public Optional<ServiceConfiguration> getServiceByName(String serviceName) {
        return serviceConfigRepository.findByServiceName(serviceName);
    }
    
    public ServiceConfiguration createService(ServiceConfiguration serviceConfig) {
        validateServiceConfiguration(serviceConfig);
        
        if (serviceConfigRepository.existsByServiceNameAndSubServiceNameAndTenantId(
                serviceConfig.getServiceName(), serviceConfig.getSubServiceName(), serviceConfig.getTenantId())) {
            throw new RuntimeException("Service with name '" + serviceConfig.getServiceName() + 
                                     "' and sub-service '" + serviceConfig.getSubServiceName() + 
                                     "' already exists for tenant '" + serviceConfig.getTenantId() + "'");
        }
        
        return serviceConfigRepository.save(serviceConfig);
    }
    
    public ServiceConfiguration updateService(Long id, ServiceConfiguration serviceConfig) {
        ServiceConfiguration existingService = serviceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        
        // Check if service/sub-service combination is being changed and if new combination already exists
        if (!existingService.getServiceName().equals(serviceConfig.getServiceName()) || 
            !java.util.Objects.equals(existingService.getSubServiceName(), serviceConfig.getSubServiceName()) ||
            !existingService.getTenantId().equals(serviceConfig.getTenantId())) {
            if (serviceConfigRepository.existsByServiceNameAndSubServiceNameAndTenantId(
                    serviceConfig.getServiceName(), serviceConfig.getSubServiceName(), serviceConfig.getTenantId())) {
                throw new RuntimeException("Service with name '" + serviceConfig.getServiceName() + 
                                         "' and sub-service '" + serviceConfig.getSubServiceName() + 
                                         "' already exists for tenant '" + serviceConfig.getTenantId() + "'");
            }
        }
        
        validateServiceConfiguration(serviceConfig);
        
        // Update fields
        existingService.setServiceName(serviceConfig.getServiceName());
        existingService.setSubServiceName(serviceConfig.getSubServiceName());
        existingService.setTenantId(serviceConfig.getTenantId());
        existingService.setInboundPath(serviceConfig.getInboundPath());
        existingService.setOutboundPath(serviceConfig.getOutboundPath());
        existingService.setStartMarkerPrefix(serviceConfig.getStartMarkerPrefix());
        existingService.setEndMarkerPrefix(serviceConfig.getEndMarkerPrefix());
        existingService.setDataFilePattern(serviceConfig.getDataFilePattern());
        existingService.setCutOffTime(serviceConfig.getCutOffTime());
        existingService.setCutOffTimeType(serviceConfig.getCutOffTimeType());
        existingService.setWeekdayCutOffTime(serviceConfig.getWeekdayCutOffTime());
        existingService.setWeekendCutOffTime(serviceConfig.getWeekendCutOffTime());
        existingService.setMondayCutOffTime(serviceConfig.getMondayCutOffTime());
        existingService.setTuesdayCutOffTime(serviceConfig.getTuesdayCutOffTime());
        existingService.setWednesdayCutOffTime(serviceConfig.getWednesdayCutOffTime());
        existingService.setThursdayCutOffTime(serviceConfig.getThursdayCutOffTime());
        existingService.setFridayCutOffTime(serviceConfig.getFridayCutOffTime());
        existingService.setSaturdayCutOffTime(serviceConfig.getSaturdayCutOffTime());
        existingService.setSundayCutOffTime(serviceConfig.getSundayCutOffTime());
        existingService.setAllSundaysAsHolidays(serviceConfig.getAllSundaysAsHolidays());
        existingService.setEnabled(serviceConfig.getEnabled());
        existingService.setMaxRetries(serviceConfig.getMaxRetries());
        existingService.setPollIntervalSeconds(serviceConfig.getPollIntervalSeconds());
        existingService.setSotFileValidationRegex(serviceConfig.getSotFileValidationRegex());
        existingService.setEotFileValidationRegex(serviceConfig.getEotFileValidationRegex());
        existingService.setDataFileValidationRegex(serviceConfig.getDataFileValidationRegex());
        existingService.setDescription(serviceConfig.getDescription());
        existingService.setUpdatedBy(serviceConfig.getUpdatedBy());
        
        return serviceConfigRepository.save(existingService);
    }
    
    public void deleteService(Long id) {
        ServiceConfiguration service = serviceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        
        // TODO: Check if service has any active file transfers before deletion
        serviceConfigRepository.delete(service);
    }
    
    public ServiceConfiguration toggleServiceStatus(Long id) {
        ServiceConfiguration service = serviceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        
        service.setEnabled(!service.getEnabled());
        return serviceConfigRepository.save(service);
    }
    
    public boolean validateFileAgainstService(String fileName, String serviceType, String fileType) {
        Optional<ServiceConfiguration> serviceOpt = serviceConfigRepository.findByServiceName(serviceType);
        if (serviceOpt.isEmpty()) {
            return false;
        }
        
        ServiceConfiguration service = serviceOpt.get();
        String regex = null;
        
        switch (fileType.toLowerCase()) {
            case "sot":
                regex = service.getSotFileValidationRegex();
                break;
            case "eot":
                regex = service.getEotFileValidationRegex();
                break;
            case "data":
                regex = service.getDataFileValidationRegex();
                break;
            default:
                return true; // No validation if type is unknown
        }
        
        if (regex == null || regex.trim().isEmpty()) {
            return true; // No validation configured
        }
        
        try {
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(fileName).matches();
        } catch (PatternSyntaxException e) {
            // Log error and return false for invalid regex
            return false;
        }
    }
    
    private void validateServiceConfiguration(ServiceConfiguration serviceConfig) {
        // Validate regex patterns if provided
        validateRegexPattern(serviceConfig.getSotFileValidationRegex(), "SOT file validation regex");
        validateRegexPattern(serviceConfig.getEotFileValidationRegex(), "EOT file validation regex");
        validateRegexPattern(serviceConfig.getDataFileValidationRegex(), "Data file validation regex");
        
        // Validate cut-off time configuration
        cutOffTimeService.validateCutOffTimeConfiguration(serviceConfig);
        
        // Validate numeric fields
        if (serviceConfig.getMaxRetries() != null && serviceConfig.getMaxRetries() < 0) {
            throw new RuntimeException("Max retries must be a non-negative number");
        }
        
        if (serviceConfig.getPollIntervalSeconds() != null && serviceConfig.getPollIntervalSeconds() < 1) {
            throw new RuntimeException("Poll interval must be at least 1 second");
        }
        
        // Validate paths
        if (serviceConfig.getInboundPath() != null && serviceConfig.getInboundPath().trim().isEmpty()) {
            throw new RuntimeException("Inbound path cannot be empty");
        }
        
        if (serviceConfig.getOutboundPath() != null && serviceConfig.getOutboundPath().trim().isEmpty()) {
            throw new RuntimeException("Outbound path cannot be empty");
        }
    }
    
    private void validateRegexPattern(String regex, String fieldName) {
        if (regex != null && !regex.trim().isEmpty()) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new RuntimeException("Invalid " + fieldName + ": " + e.getMessage());
            }
        }
    }
    
    public long getEnabledServicesCountForTenant(String tenantId) {
        return serviceConfigRepository.countEnabledServicesForTenant(tenantId);
    }
    
    /**
     * Get the effective cut-off time for a service on a specific date
     */
    public LocalTime getEffectiveCutOffTime(Long serviceId, LocalDate date) {
        Optional<ServiceConfiguration> serviceOpt = getServiceById(serviceId);
        if (serviceOpt.isEmpty()) {
            return LocalTime.parse("23:59:59");
        }
        
        return cutOffTimeService.getCutOffTimeForDate(serviceOpt.get(), date);
    }
    
    /**
     * Get the effective cut-off time for a service by name on a specific date
     */
    public LocalTime getEffectiveCutOffTime(String tenantId, String serviceName, LocalDate date) {
        List<ServiceConfiguration> services = getServicesByNameForTenant(tenantId, serviceName);
        if (services.isEmpty()) {
            return LocalTime.parse("23:59:59");
        }
        
        // Use the first service configuration (or you could add logic to handle multiple)
        return cutOffTimeService.getCutOffTimeForDate(services.get(0), date);
    }
    
    /**
     * Check if a time is before the cut-off for a service on a specific date
     */
    public boolean isBeforeCutOffTime(Long serviceId, LocalDate date, LocalTime time) {
        Optional<ServiceConfiguration> serviceOpt = getServiceById(serviceId);
        if (serviceOpt.isEmpty()) {
            return true; // Default to allowing if service not found
        }
        
        return cutOffTimeService.isBeforeCutOffTime(serviceOpt.get(), date, time);
    }
    
    /**
     * Get a description of the cut-off time configuration for a service
     */
    public String getCutOffTimeDescription(Long serviceId) {
        Optional<ServiceConfiguration> serviceOpt = getServiceById(serviceId);
        if (serviceOpt.isEmpty()) {
            return "Service not found";
        }
        
        return cutOffTimeService.getCutOffTimeDescription(serviceOpt.get());
    }
}