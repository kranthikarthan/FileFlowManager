package com.filetransfer.web.service;

import com.filetransfer.web.entity.ServiceConfiguration;
import com.filetransfer.web.repository.ServiceConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@Transactional
public class ServiceConfigurationService {
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigRepository;
    
    public List<ServiceConfiguration> getAllServices() {
        return serviceConfigRepository.findAll();
    }
    
    public List<ServiceConfiguration> getEnabledServices() {
        return serviceConfigRepository.findAllEnabledServices();
    }
    
    public Optional<ServiceConfiguration> getServiceById(Long id) {
        return serviceConfigRepository.findById(id);
    }
    
    public Optional<ServiceConfiguration> getServiceByName(String serviceName) {
        return serviceConfigRepository.findByServiceName(serviceName);
    }
    
    public ServiceConfiguration createService(ServiceConfiguration serviceConfig) {
        validateServiceConfiguration(serviceConfig);
        
        if (serviceConfigRepository.existsByServiceName(serviceConfig.getServiceName())) {
            throw new RuntimeException("Service with name '" + serviceConfig.getServiceName() + "' already exists");
        }
        
        return serviceConfigRepository.save(serviceConfig);
    }
    
    public ServiceConfiguration updateService(Long id, ServiceConfiguration serviceConfig) {
        ServiceConfiguration existingService = serviceConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        
        // Check if name is being changed and if new name already exists
        if (!existingService.getServiceName().equals(serviceConfig.getServiceName())) {
            if (serviceConfigRepository.existsByServiceName(serviceConfig.getServiceName())) {
                throw new RuntimeException("Service with name '" + serviceConfig.getServiceName() + "' already exists");
            }
        }
        
        validateServiceConfiguration(serviceConfig);
        
        // Update fields
        existingService.setServiceName(serviceConfig.getServiceName());
        existingService.setInboundPath(serviceConfig.getInboundPath());
        existingService.setOutboundPath(serviceConfig.getOutboundPath());
        existingService.setStartMarkerPrefix(serviceConfig.getStartMarkerPrefix());
        existingService.setEndMarkerPrefix(serviceConfig.getEndMarkerPrefix());
        existingService.setDataFilePattern(serviceConfig.getDataFilePattern());
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
    
    public long getEnabledServicesCount() {
        return serviceConfigRepository.countEnabledServices();
    }
}