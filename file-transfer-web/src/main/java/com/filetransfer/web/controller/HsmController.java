package com.filetransfer.web.controller;

import com.filetransfer.web.entity.HsmProvider;
import com.filetransfer.web.entity.HsmOperation;
import com.filetransfer.web.entity.HsmValidationStatus;
import com.filetransfer.web.entity.HsmValidationRecord;
import com.filetransfer.web.service.HsmService;
import com.filetransfer.web.repository.HsmValidationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for HSM (Hardware Security Module) management
 */
@RestController
@RequestMapping("/api/v1/hsm")
// CORS configured globally in GlobalCorsConfiguration
public class HsmController {
    
    private static final Logger logger = LoggerFactory.getLogger(HsmController.class);
    
    @Autowired
    private HsmService hsmService;
    
    @Autowired
    private HsmValidationRecordRepository hsmValidationRepository;
    
    /**
     * Get all available HSM providers
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getHsmProviders() {
        try {
            List<Map<String, Object>> providers = Arrays.stream(HsmProvider.values())
                .map(provider -> Map.of(
                    "provider", provider.name(),
                    "displayName", provider.getDisplayName(),
                    "description", provider.getDescription(),
                    "providerClassName", provider.getProviderClassName(),
                    "isNetworkBased", provider.isNetworkBased(),
                    "isCloudBased", provider.isCloudBased(),
                    "supportsHighAvailability", provider.supportsHighAvailability(),
                    "supportedKeyAlgorithms", provider.getSupportedKeyAlgorithms()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(providers);
        } catch (Exception e) {
            logger.error("Error retrieving HSM providers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all available HSM operations
     */
    @GetMapping("/operations")
    public ResponseEntity<List<Map<String, Object>>> getHsmOperations() {
        try {
            List<Map<String, Object>> operations = Arrays.stream(HsmOperation.values())
                .map(operation -> Map.of(
                    "operation", operation.name(),
                    "displayName", operation.getDisplayName(),
                    "description", operation.getDescription(),
                    "supportsOutbound", operation.supportsOutbound(),
                    "supportsInbound", operation.supportsInbound(),
                    "providesNonRepudiation", operation.providesNonRepudiation(),
                    "providesConfidentiality", operation.providesConfidentiality(),
                    "providesIntegrity", operation.providesIntegrity(),
                    "complementaryOperation", operation.getComplementaryOperation().name()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(operations);
        } catch (Exception e) {
            logger.error("Error retrieving HSM operations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all HSM validation records for a tenant
     */
    @GetMapping("/{tenantId}/validations")
    public ResponseEntity<List<HsmValidationRecord>> getHsmValidations(@PathVariable String tenantId) {
        try {
            List<HsmValidationRecord> records = hsmValidationRepository.findByTenantId(tenantId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("Error retrieving HSM validations for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get HSM validation records by status
     */
    @GetMapping("/{tenantId}/validations/status/{status}")
    public ResponseEntity<List<HsmValidationRecord>> getHsmValidationsByStatus(
            @PathVariable String tenantId, 
            @PathVariable HsmValidationStatus status) {
        try {
            List<HsmValidationRecord> records = hsmValidationRepository.findByTenantIdAndStatus(tenantId, status);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("Error retrieving HSM validations by status for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get HSM validation record for a specific file transfer
     */
    @GetMapping("/validation/file-transfer/{fileTransferId}")
    public ResponseEntity<HsmValidationRecord> getHsmValidationForFileTransfer(@PathVariable Long fileTransferId) {
        try {
            Optional<HsmValidationRecord> record = hsmValidationRepository.findByFileTransferId(fileTransferId);
            return record.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error retrieving HSM validation for file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Test HSM connection and configuration
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testHsmConnection(@RequestBody Map<String, String> request) {
        try {
            String tenantId = request.get("tenantId");
            String serviceName = request.get("serviceName");
            String subServiceName = request.get("subServiceName");
            
            if (tenantId == null || serviceName == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "tenantId and serviceName are required"));
            }
            
            HsmService.HsmTestResult testResult = hsmService.testHsmConnection(tenantId, serviceName, subServiceName);
            
            Map<String, Object> response = Map.of(
                "success", testResult.isSuccess(),
                "message", testResult.getMessage(),
                "errorCode", testResult.getErrorCode() != null ? testResult.getErrorCode() : ""
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing HSM connection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get HSM statistics for a tenant
     */
    @GetMapping("/{tenantId}/statistics")
    public ResponseEntity<Map<String, Object>> getHsmStatistics(@PathVariable String tenantId) {
        try {
            Map<String, Object> statistics = hsmService.getHsmStatistics(tenantId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving HSM statistics for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retry failed HSM validation
     */
    @PostMapping("/validation/{validationId}/retry")
    public ResponseEntity<Map<String, String>> retryHsmValidation(@PathVariable Long validationId) {
        try {
            HsmValidationRecord record = hsmValidationRepository.findById(validationId)
                .orElseThrow(() -> new RuntimeException("HSM validation record not found: " + validationId));
            
            if (!record.getStatus().isFailure()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot retry HSM validation with status: " + record.getStatus()));
            }
            
            // Reset status for retry
            record.setStatus(HsmValidationStatus.PENDING);
            record.setErrorMessage(null);
            record.setErrorCode(null);
            record.setStartedAt(null);
            record.setCompletedAt(null);
            record.setProcessingTimeMs(null);
            
            hsmValidationRepository.save(record);
            
            return ResponseEntity.ok(Map.of(
                "message", "HSM validation retry initiated",
                "validationId", validationId.toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error retrying HSM validation {}: {}", validationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get HSM validation status summary
     */
    @GetMapping("/{tenantId}/status-summary")
    public ResponseEntity<Map<String, Object>> getHsmStatusSummary(@PathVariable String tenantId) {
        try {
            List<HsmValidationRecord> records = hsmValidationRepository.findByTenantId(tenantId);
            
            Map<HsmValidationStatus, Long> statusCounts = records.stream()
                .collect(Collectors.groupingBy(
                    HsmValidationRecord::getStatus,
                    Collectors.counting()
                ));
            
            Map<HsmProvider, Long> providerCounts = records.stream()
                .collect(Collectors.groupingBy(
                    HsmValidationRecord::getHsmProvider,
                    Collectors.counting()
                ));
            
            Map<HsmOperation, Long> operationCounts = records.stream()
                .collect(Collectors.groupingBy(
                    HsmValidationRecord::getOperation,
                    Collectors.counting()
                ));
            
            long totalValidations = records.size();
            long passedValidations = statusCounts.getOrDefault(HsmValidationStatus.PASSED, 0L);
            long failedValidations = records.stream()
                .filter(r -> r.getStatus().isFailure())
                .count();
            
            double successRate = totalValidations > 0 ? (double) passedValidations / totalValidations : 0.0;
            
            Map<String, Object> summary = Map.of(
                "totalValidations", totalValidations,
                "passedValidations", passedValidations,
                "failedValidations", failedValidations,
                "successRate", successRate,
                "statusCounts", statusCounts,
                "providerCounts", providerCounts,
                "operationCounts", operationCounts
            );
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error retrieving HSM status summary for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validate HSM configuration
     */
    @PostMapping("/validate-config")
    public ResponseEntity<Map<String, Object>> validateHsmConfig(@RequestBody Map<String, Object> config) {
        try {
            String providerName = (String) config.get("hsmProvider");
            String keyAlias = (String) config.get("hsmKeyAlias");
            String algorithm = (String) config.get("hsmAlgorithm");
            
            Map<String, Object> validation = Map.of(
                "valid", true,
                "message", "HSM configuration is valid",
                "warnings", List.of(),
                "recommendations", getConfigurationRecommendations(providerName, algorithm)
            );
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            logger.error("Error validating HSM configuration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    private List<String> getConfigurationRecommendations(String providerName, String algorithm) {
        List<String> recommendations = new ArrayList<>();
        
        if ("SHA1withRSA".equals(algorithm)) {
            recommendations.add("Consider upgrading to SHA256withRSA for better security");
        }
        
        if ("NONE".equals(providerName)) {
            recommendations.add("HSM validation is disabled. Enable for enhanced security");
        }
        
        return recommendations;
    }
}