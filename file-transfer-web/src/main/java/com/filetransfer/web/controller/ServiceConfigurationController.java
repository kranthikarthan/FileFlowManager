package com.filetransfer.web.controller;

import com.filetransfer.web.entity.ServiceConfiguration;
import com.filetransfer.web.service.ServiceConfigurationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:3000")
public class ServiceConfigurationController {
    
    @Autowired
    private ServiceConfigurationService serviceConfigService;
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<ServiceConfiguration>> getServicesForTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(serviceConfigService.getServicesForTenant(tenantId));
    }
    
    @GetMapping("/tenant/{tenantId}/enabled")
    public ResponseEntity<List<ServiceConfiguration>> getEnabledServicesForTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(serviceConfigService.getEnabledServicesForTenant(tenantId));
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}")
    public ResponseEntity<List<ServiceConfiguration>> getServicesByNameForTenant(
            @PathVariable String tenantId, 
            @PathVariable String serviceName) {
        return ResponseEntity.ok(serviceConfigService.getServicesByNameForTenant(tenantId, serviceName));
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/subservices")
    public ResponseEntity<List<String>> getSubServicesForService(
            @PathVariable String tenantId, 
            @PathVariable String serviceName) {
        return ResponseEntity.ok(serviceConfigService.getSubServicesForService(tenantId, serviceName));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ServiceConfiguration> getServiceById(@PathVariable Long id) {
        return serviceConfigService.getServiceById(id)
            .map(service -> ResponseEntity.ok(service))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/tenant/{tenantId}/name/{serviceName}")
    public ResponseEntity<ServiceConfiguration> getServiceByName(@PathVariable String tenantId, @PathVariable String serviceName) {
        return serviceConfigService.getServiceByName(tenantId, serviceName)
            .map(service -> ResponseEntity.ok(service))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createService(@Valid @RequestBody ServiceConfiguration serviceConfig) {
        try {
            ServiceConfiguration createdService = serviceConfigService.createService(serviceConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, 
                                         @Valid @RequestBody ServiceConfiguration serviceConfig) {
        try {
            ServiceConfiguration updatedService = serviceConfigService.updateService(id, serviceConfig);
            return ResponseEntity.ok(updatedService);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        try {
            serviceConfigService.deleteService(id);
            return ResponseEntity.ok(Map.of("message", "Service deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleServiceStatus(@PathVariable Long id) {
        try {
            ServiceConfiguration updatedService = serviceConfigService.toggleServiceStatus(id);
            return ResponseEntity.ok(updatedService);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/tenant/{tenantId}/validate-file")
    public ResponseEntity<?> validateFile(@PathVariable String tenantId, @RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            String serviceType = request.get("serviceType");
            String fileType = request.get("fileType");
            
            if (fileName == null || serviceType == null || fileType == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "fileName, serviceType, and fileType are required"));
            }
            
            boolean isValid = serviceConfigService.validateFileAgainstService(tenantId, fileName, serviceType, fileType);
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "fileName", fileName,
                "serviceType", serviceType,
                "fileType", fileType,
                "tenantId", tenantId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/stats")
    public ResponseEntity<Map<String, Object>> getServiceStatsForTenant(@PathVariable String tenantId) {
        long enabledCount = serviceConfigService.getEnabledServicesCountForTenant(tenantId);
        long totalCount = serviceConfigService.getServicesForTenant(tenantId).size();
        
        return ResponseEntity.ok(Map.of(
            "totalServices", totalCount,
            "enabledServices", enabledCount,
            "disabledServices", totalCount - enabledCount,
            "tenantId", tenantId
        ));
    }
    
    @GetMapping("/{id}/cutoff-time/{date}")
    public ResponseEntity<Map<String, Object>> getEffectiveCutOffTime(
            @PathVariable Long id, 
            @PathVariable LocalDate date) {
        try {
            LocalTime cutOffTime = serviceConfigService.getEffectiveCutOffTime(id, date);
            String description = serviceConfigService.getCutOffTimeDescription(id);
            
            return ResponseEntity.ok(Map.of(
                "serviceId", id,
                "date", date,
                "cutOffTime", cutOffTime.toString(),
                "description", description
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/cutoff-time/{date}")
    public ResponseEntity<Map<String, Object>> getEffectiveCutOffTimeByName(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable LocalDate date) {
        try {
            LocalTime cutOffTime = serviceConfigService.getEffectiveCutOffTime(tenantId, serviceName, date);
            
            return ResponseEntity.ok(Map.of(
                "tenantId", tenantId,
                "serviceName", serviceName,
                "date", date,
                "cutOffTime", cutOffTime.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/check-cutoff")
    public ResponseEntity<Map<String, Object>> checkCutOffTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            LocalDate date = LocalDate.parse(request.get("date"));
            LocalTime time = LocalTime.parse(request.get("time"));
            
            boolean isBeforeCutOff = serviceConfigService.isBeforeCutOffTime(id, date, time);
            LocalTime cutOffTime = serviceConfigService.getEffectiveCutOffTime(id, date);
            
            return ResponseEntity.ok(Map.of(
                "serviceId", id,
                "date", date,
                "time", time.toString(),
                "cutOffTime", cutOffTime.toString(),
                "isBeforeCutOff", isBeforeCutOff,
                "message", isBeforeCutOff ? "Within cut-off time" : "Past cut-off time"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}