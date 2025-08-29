package com.filetransfer.web.controller;

import com.filetransfer.web.service.ProcessingControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Processing Control Controller - Manage processing at all levels
 * Provides endpoints to start/stop/pause/resume processing from tenant to individual file level
 */
@RestController
@RequestMapping("/api/v1/processing-control")
@Tag(name = "Processing Control", description = "Processing Control Management API")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProcessingControlController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingControlController.class);

    @Autowired
    private ProcessingControlService processingControlService;

    // ===== TENANT LEVEL PROCESSING CONTROL =====

    /**
     * Start/Stop processing for entire tenant
     */
    @PostMapping("/tenant/{tenantId}/start")
    @Operation(summary = "Start tenant processing", description = "Start all processing for a tenant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> startTenantProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Starting processing for tenant: {} by user: {}", tenantId, userId);
        
        try {
            Map<String, Object> result = processingControlService.startTenantProcessing(tenantId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting tenant processing: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start tenant processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/stop")
    @Operation(summary = "Stop tenant processing", description = "Stop all processing for a tenant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> stopTenantProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Graceful shutdown") @RequestParam(defaultValue = "true") boolean graceful) {
        
        logger.info("Stopping processing for tenant: {} by user: {} (graceful: {})", tenantId, userId, graceful);
        
        try {
            Map<String, Object> result = processingControlService.stopTenantProcessing(tenantId, userId, graceful);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error stopping tenant processing: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop tenant processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/pause")
    @Operation(summary = "Pause tenant processing", description = "Pause all processing for a tenant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> pauseTenantProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Pausing processing for tenant: {} by user: {}", tenantId, userId);
        
        try {
            Map<String, Object> result = processingControlService.pauseTenantProcessing(tenantId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error pausing tenant processing: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to pause tenant processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/resume")
    @Operation(summary = "Resume tenant processing", description = "Resume all processing for a tenant")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> resumeTenantProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Resuming processing for tenant: {} by user: {}", tenantId, userId);
        
        try {
            Map<String, Object> result = processingControlService.resumeTenantProcessing(tenantId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resuming tenant processing: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to resume tenant processing: " + e.getMessage()));
        }
    }

    // ===== SERVICE LEVEL PROCESSING CONTROL =====

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/start")
    @Operation(summary = "Start service processing", description = "Start processing for a specific service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> startServiceProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Starting processing for service: {} in tenant: {} by user: {}", serviceName, tenantId, userId);
        
        try {
            Map<String, Object> result = processingControlService.startServiceProcessing(tenantId, serviceName, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting service processing: {} in tenant: {}", serviceName, tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start service processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/stop")
    @Operation(summary = "Stop service processing", description = "Stop processing for a specific service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> stopServiceProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Graceful shutdown") @RequestParam(defaultValue = "true") boolean graceful) {
        
        logger.info("Stopping processing for service: {} in tenant: {} by user: {} (graceful: {})", 
                   serviceName, tenantId, userId, graceful);
        
        try {
            Map<String, Object> result = processingControlService.stopServiceProcessing(tenantId, serviceName, userId, graceful);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error stopping service processing: {} in tenant: {}", serviceName, tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop service processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/reset")
    @Operation(summary = "Reset service for the day", description = "Reset service processing state for the current day")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> resetServiceForDay(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Reset date") @RequestParam(required = false) LocalDate resetDate) {
        
        LocalDate dateToReset = resetDate != null ? resetDate : LocalDate.now();
        logger.info("Resetting service: {} in tenant: {} for date: {} by user: {}", 
                   serviceName, tenantId, dateToReset, userId);
        
        try {
            Map<String, Object> result = processingControlService.resetServiceForDay(tenantId, serviceName, dateToReset, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resetting service: {} in tenant: {} for date: {}", serviceName, tenantId, dateToReset, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to reset service for day: " + e.getMessage()));
        }
    }

    // ===== SUB-SERVICE LEVEL PROCESSING CONTROL =====

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/start")
    @Operation(summary = "Start sub-service processing", description = "Start processing for a specific sub-service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> startSubServiceProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "Sub-Service Name") @PathVariable String subServiceName,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Starting processing for sub-service: {}/{} in tenant: {} by user: {}", 
                   serviceName, subServiceName, tenantId, userId);
        
        try {
            Map<String, Object> result = processingControlService.startSubServiceProcessing(
                tenantId, serviceName, subServiceName, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting sub-service processing: {}/{} in tenant: {}", 
                        serviceName, subServiceName, tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start sub-service processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/stop")
    @Operation(summary = "Stop sub-service processing", description = "Stop processing for a specific sub-service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> stopSubServiceProcessing(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "Sub-Service Name") @PathVariable String subServiceName,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Graceful shutdown") @RequestParam(defaultValue = "true") boolean graceful) {
        
        logger.info("Stopping processing for sub-service: {}/{} in tenant: {} by user: {} (graceful: {})", 
                   serviceName, subServiceName, tenantId, userId, graceful);
        
        try {
            Map<String, Object> result = processingControlService.stopSubServiceProcessing(
                tenantId, serviceName, subServiceName, userId, graceful);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error stopping sub-service processing: {}/{} in tenant: {}", 
                        serviceName, subServiceName, tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop sub-service processing: " + e.getMessage()));
        }
    }

    @PostMapping("/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/reset")
    @Operation(summary = "Reset sub-service for the day", description = "Reset sub-service processing state for the current day")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TENANT_ADMIN') or hasRole('SERVICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> resetSubServiceForDay(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName,
            @Parameter(description = "Sub-Service Name") @PathVariable String subServiceName,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Reset date") @RequestParam(required = false) LocalDate resetDate) {
        
        LocalDate dateToReset = resetDate != null ? resetDate : LocalDate.now();
        logger.info("Resetting sub-service: {}/{} in tenant: {} for date: {} by user: {}", 
                   serviceName, subServiceName, tenantId, dateToReset, userId);
        
        try {
            Map<String, Object> result = processingControlService.resetSubServiceForDay(
                tenantId, serviceName, subServiceName, dateToReset, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resetting sub-service: {}/{} in tenant: {} for date: {}", 
                        serviceName, subServiceName, tenantId, dateToReset, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to reset sub-service for day: " + e.getMessage()));
        }
    }

    // ===== FILE LEVEL PROCESSING CONTROL =====

    @PostMapping("/file/{fileId}/start")
    @Operation(summary = "Start file processing", description = "Start processing for a specific file")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> startFileProcessing(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Starting processing for file: {} by user: {}", fileId, userId);
        
        try {
            Map<String, Object> result = processingControlService.startFileProcessing(fileId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting file processing: {}", fileId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start file processing: " + e.getMessage()));
        }
    }

    @PostMapping("/file/{fileId}/stop")
    @Operation(summary = "Stop file processing", description = "Stop processing for a specific file")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stopFileProcessing(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Force stop") @RequestParam(defaultValue = "false") boolean force) {
        
        logger.info("Stopping processing for file: {} by user: {} (force: {})", fileId, userId, force);
        
        try {
            Map<String, Object> result = processingControlService.stopFileProcessing(fileId, userId, force);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error stopping file processing: {}", fileId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop file processing: " + e.getMessage()));
        }
    }

    @PostMapping("/file/{fileId}/pause")
    @Operation(summary = "Pause file processing", description = "Pause processing for a specific file")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> pauseFileProcessing(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Pausing processing for file: {} by user: {}", fileId, userId);
        
        try {
            Map<String, Object> result = processingControlService.pauseFileProcessing(fileId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error pausing file processing: {}", fileId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to pause file processing: " + e.getMessage()));
        }
    }

    @PostMapping("/file/{fileId}/resume")
    @Operation(summary = "Resume file processing", description = "Resume processing for a specific file")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resumeFileProcessing(
            @Parameter(description = "File ID") @PathVariable Long fileId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Resuming processing for file: {} by user: {}", fileId, userId);
        
        try {
            Map<String, Object> result = processingControlService.resumeFileProcessing(fileId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resuming file processing: {}", fileId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to resume file processing: " + e.getMessage()));
        }
    }

    // ===== BATCH PROCESSING CONTROL =====

    @PostMapping("/batch/start")
    @Operation(summary = "Start batch processing", description = "Start batch processing for specified criteria")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> startBatchProcessing(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Service Name") @RequestParam(required = false) String serviceName,
            @Parameter(description = "Sub-Service Name") @RequestParam(required = false) String subServiceName,
            @Parameter(description = "Processing Date") @RequestParam(required = false) LocalDate processingDate,
            @Parameter(description = "User ID") @RequestParam String userId) {
        
        logger.info("Starting batch processing for tenant: {}, service: {}, sub-service: {}, date: {} by user: {}", 
                   tenantId, serviceName, subServiceName, processingDate, userId);
        
        try {
            Map<String, Object> result = processingControlService.startBatchProcessing(
                tenantId, serviceName, subServiceName, processingDate, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting batch processing", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start batch processing: " + e.getMessage()));
        }
    }

    @PostMapping("/batch/stop")
    @Operation(summary = "Stop batch processing", description = "Stop batch processing for specified criteria")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> stopBatchProcessing(
            @Parameter(description = "Job Execution ID") @RequestParam(required = false) Long jobExecutionId,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Force stop") @RequestParam(defaultValue = "false") boolean force) {
        
        logger.info("Stopping batch processing - Job ID: {}, User: {}, Force: {}", jobExecutionId, userId, force);
        
        try {
            Map<String, Object> result = processingControlService.stopBatchProcessing(jobExecutionId, userId, force);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error stopping batch processing", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to stop batch processing: " + e.getMessage()));
        }
    }

    // ===== PROCESSING STATUS QUERIES =====

    @GetMapping("/tenant/{tenantId}/status")
    @Operation(summary = "Get tenant processing status", description = "Get current processing status for a tenant")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTenantProcessingStatus(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId) {
        
        try {
            Map<String, Object> status = processingControlService.getTenantProcessingStatus(tenantId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting tenant processing status: {}", tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get tenant processing status: " + e.getMessage()));
        }
    }

    @GetMapping("/tenant/{tenantId}/service/{serviceName}/status")
    @Operation(summary = "Get service processing status", description = "Get current processing status for a service")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getServiceProcessingStatus(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Service Name") @PathVariable String serviceName) {
        
        try {
            Map<String, Object> status = processingControlService.getServiceProcessingStatus(tenantId, serviceName);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting service processing status: {} in tenant: {}", serviceName, tenantId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get service processing status: " + e.getMessage()));
        }
    }

    @GetMapping("/file/{fileId}/status")
    @Operation(summary = "Get file processing status", description = "Get current processing status for a file")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFileProcessingStatus(
            @Parameter(description = "File ID") @PathVariable Long fileId) {
        
        try {
            Map<String, Object> status = processingControlService.getFileProcessingStatus(fileId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting file processing status: {}", fileId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get file processing status: " + e.getMessage()));
        }
    }

    @GetMapping("/batch/status")
    @Operation(summary = "Get batch processing status", description = "Get current batch processing status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBatchProcessingStatus(
            @Parameter(description = "Tenant ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "Job Execution ID") @RequestParam(required = false) Long jobExecutionId) {
        
        try {
            Map<String, Object> status = processingControlService.getBatchProcessingStatus(tenantId, jobExecutionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting batch processing status", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get batch processing status: " + e.getMessage()));
        }
    }
}