package com.filetransfer.web.controller;

import com.filetransfer.web.entity.ServiceConfiguration;
import com.filetransfer.web.service.ServiceConfigurationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:3000")
public class ServiceConfigurationController {
    
    @Autowired
    private ServiceConfigurationService serviceConfigService;
    
    @GetMapping
    public ResponseEntity<List<ServiceConfiguration>> getAllServices() {
        return ResponseEntity.ok(serviceConfigService.getAllServices());
    }
    
    @GetMapping("/enabled")
    public ResponseEntity<List<ServiceConfiguration>> getEnabledServices() {
        return ResponseEntity.ok(serviceConfigService.getEnabledServices());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ServiceConfiguration> getServiceById(@PathVariable Long id) {
        return serviceConfigService.getServiceById(id)
            .map(service -> ResponseEntity.ok(service))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/name/{serviceName}")
    public ResponseEntity<ServiceConfiguration> getServiceByName(@PathVariable String serviceName) {
        return serviceConfigService.getServiceByName(serviceName)
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
    
    @PostMapping("/validate-file")
    public ResponseEntity<?> validateFile(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            String serviceType = request.get("serviceType");
            String fileType = request.get("fileType");
            
            if (fileName == null || serviceType == null || fileType == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "fileName, serviceType, and fileType are required"));
            }
            
            boolean isValid = serviceConfigService.validateFileAgainstService(fileName, serviceType, fileType);
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "fileName", fileName,
                "serviceType", serviceType,
                "fileType", fileType
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getServiceStats() {
        long enabledCount = serviceConfigService.getEnabledServicesCount();
        long totalCount = serviceConfigService.getAllServices().size();
        
        return ResponseEntity.ok(Map.of(
            "totalServices", totalCount,
            "enabledServices", enabledCount,
            "disabledServices", totalCount - enabledCount
        ));
    }
}