package com.filetransfer.web.controller;

import com.filetransfer.web.entity.SubServiceConfiguration;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.service.SubServiceConfigurationService;
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
@RequestMapping("/api/sub-services")
@CrossOrigin(origins = "http://localhost:3000")
public class SubServiceConfigurationController {
    
    @Autowired
    private SubServiceConfigurationService subServiceConfigService;
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<SubServiceConfiguration>> getSubServicesForTenant(@PathVariable String tenantId) {
        List<SubServiceConfiguration> subServices = subServiceConfigService.getSubServicesForTenant(tenantId);
        return ResponseEntity.ok(subServices);
    }
    
    @GetMapping("/tenant/{tenantId}/enabled")
    public ResponseEntity<List<SubServiceConfiguration>> getEnabledSubServicesForTenant(@PathVariable String tenantId) {
        List<SubServiceConfiguration> subServices = subServiceConfigService.getEnabledSubServicesForTenant(tenantId);
        return ResponseEntity.ok(subServices);
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}")
    public ResponseEntity<List<SubServiceConfiguration>> getSubServicesForService(
            @PathVariable String tenantId, 
            @PathVariable String serviceName) {
        List<SubServiceConfiguration> subServices = subServiceConfigService.getSubServicesForService(tenantId, serviceName);
        return ResponseEntity.ok(subServices);
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/sub-service/{subServiceName}")
    public ResponseEntity<SubServiceConfiguration> getSubServiceConfiguration(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName) {
        return subServiceConfigService.getSubServiceConfiguration(tenantId, serviceName, subServiceName)
            .map(config -> ResponseEntity.ok(config))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createSubService(@Valid @RequestBody SubServiceConfiguration subServiceConfig) {
        try {
            SubServiceConfiguration createdConfig = subServiceConfigService.createSubService(subServiceConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create sub-service: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubService(
            @PathVariable Long id, 
            @Valid @RequestBody SubServiceConfiguration subServiceConfig) {
        try {
            SubServiceConfiguration updatedConfig = subServiceConfigService.updateSubService(id, subServiceConfig);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update sub-service: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubService(@PathVariable Long id) {
        try {
            subServiceConfigService.deleteSubService(id);
            return ResponseEntity.ok(Map.of("message", "Sub-service deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete sub-service: " + e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/sub-service/{subServiceName}/cutoff-time")
    public ResponseEntity<?> getEffectiveCutOffTime(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName,
            @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            LocalTime cutOffTime = subServiceConfigService.getEffectiveCutOffTime(
                tenantId, serviceName, subServiceName, targetDate);
            
            return ResponseEntity.ok(Map.of(
                "cutOffTime", cutOffTime.toString(),
                "date", targetDate.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/file-type-schemas")
    public ResponseEntity<?> configureFileTypeSchemas(
            @PathVariable Long id,
            @RequestBody Map<FileType, Long> fileTypeSchemaMap) {
        try {
            SubServiceConfiguration updatedConfig = subServiceConfigService.configureFileTypeSchemas(id, fileTypeSchemaMap);
            return ResponseEntity.ok(updatedConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to configure file type schemas: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/direction-configs")
    public ResponseEntity<?> configureDirectionSettings(
            @PathVariable Long id,
            @RequestBody Map<TransferDirection, String> directionConfigs) {
        try {
            SubServiceConfiguration updatedConfig = subServiceConfigService.configureDirectionSettings(id, directionConfigs);
            return ResponseEntity.ok(updatedConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to configure direction settings: " + e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/sub-service/{subServiceName}/schema")
    public ResponseEntity<?> getSchemaForFileType(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName,
            @RequestParam FileType fileType,
            @RequestParam TransferDirection direction) {
        try {
            Long schemaId = subServiceConfigService.getSchemaIdForFileType(
                tenantId, serviceName, subServiceName, fileType, direction);
            
            if (schemaId != null) {
                return ResponseEntity.ok(Map.of("schemaId", schemaId));
            } else {
                return ResponseEntity.ok(Map.of("schemaId", null, "message", "No schema configured for this file type"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/sub-service/{subServiceName}/validation-required")
    public ResponseEntity<?> checkValidationRequired(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName,
            @RequestParam FileType fileType) {
        try {
            boolean required = subServiceConfigService.requiresSchemaValidation(
                tenantId, serviceName, subServiceName, fileType);
            
            return ResponseEntity.ok(Map.of(
                "validationRequired", required,
                "fileType", fileType.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/file-types")
    public ResponseEntity<Map<String, Object>> getFileTypes() {
        Map<String, Object> fileTypes = Map.of(
            "fileTypes", FileType.values(),
            "descriptions", Map.of(
                "COBOL_FLAT_FILE", FileType.COBOL_FLAT_FILE.getDescription(),
                "BINARY_FILE", FileType.BINARY_FILE.getDescription(),
                "XML", FileType.XML.getDescription(),
                "JSON", FileType.JSON.getDescription(),
                "CSV", FileType.CSV.getDescription(),
                "FIXED_WIDTH", FileType.FIXED_WIDTH.getDescription(),
                "DELIMITED", FileType.DELIMITED.getDescription(),
                "EDI", FileType.EDI.getDescription()
            )
        );
        return ResponseEntity.ok(fileTypes);
    }
}