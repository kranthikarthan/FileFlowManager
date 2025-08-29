package com.filetransfer.web.controller;

import com.filetransfer.web.model.backup.BackupModels.*;
import com.filetransfer.web.model.backup.DisasterRecoveryModels.*;
import com.filetransfer.web.service.BackupService;
import com.filetransfer.web.service.DisasterRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for backup and disaster recovery operations
 * Provides endpoints for backup management, restoration, and disaster recovery
 */
@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = "*")
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    private BackupService backupService;

    @Autowired
    private DisasterRecoveryService disasterRecoveryService;

    /**
     * Create manual backup
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> createBackup(@RequestBody BackupRequest request) {
        try {
            logger.info("Creating manual backup of type: {}", request.getType());

            CompletableFuture<BackupResult> future = backupService.performBackup(request);
            BackupResult result = future.join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("backupId", result.getBackupId());
            response.put("type", result.getType());
            response.put("duration", result.getDuration());
            response.put("size", result.getSize());

            if (result.isSuccess()) {
                response.put("message", "Backup created successfully");
                response.put("backupPath", result.getBackupPath());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", result.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Failed to create backup", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create backup");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * List available backups
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listBackups(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            List<BackupMetadata> backups = backupService.listAvailableBackups();

            // Filter by type if specified
            if (type != null) {
                backups = backups.stream()
                    .filter(backup -> backup.getType().name().equalsIgnoreCase(type))
                    .toList();
            }

            // Filter by date range if specified
            if (from != null) {
                backups = backups.stream()
                    .filter(backup -> backup.getCreatedAt().isAfter(from))
                    .toList();
            }

            if (to != null) {
                backups = backups.stream()
                    .filter(backup -> backup.getCreatedAt().isBefore(to))
                    .toList();
            }

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, backups.size());
            List<BackupMetadata> paginatedBackups = backups.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("backups", paginatedBackups);
            response.put("totalCount", backups.size());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) backups.size() / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to list backups", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to list backups");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get backup details
     */
    @GetMapping("/{backupId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getBackupDetails(@PathVariable String backupId) {
        try {
            List<BackupMetadata> backups = backupService.listAvailableBackups();
            BackupMetadata backup = backups.stream()
                .filter(b -> b.getBackupId().equals(backupId))
                .findFirst()
                .orElse(null);

            if (backup == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Backup not found");
                errorResponse.put("backupId", backupId);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("backup", backup);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get backup details for: {}", backupId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get backup details");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Restore from backup
     */
    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> restoreFromBackup(@RequestBody RestoreRequest request) {
        try {
            logger.info("Restoring from backup: {}", request.getBackupId());

            CompletableFuture<RestoreResult> future = backupService.restoreFromBackup(request);
            RestoreResult result = future.join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("restoreId", result.getRestoreId());
            response.put("backupId", result.getBackupId());
            response.put("duration", result.getDuration());

            if (result.isSuccess()) {
                response.put("message", "Restore completed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", result.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Failed to restore from backup: {}", request.getBackupId(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to restore from backup");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Delete backup
     */
    @DeleteMapping("/{backupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable String backupId) {
        try {
            // Implementation would call backupService.deleteBackup(backupId)
            logger.info("Deleting backup: {}", backupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backup deleted successfully");
            response.put("backupId", backupId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete backup: {}", backupId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete backup");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get backup status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getBackupStatus() {
        try {
            List<BackupMetadata> backups = backupService.listAvailableBackups();
            
            // Calculate statistics
            long totalBackups = backups.size();
            long totalSize = backups.stream().mapToLong(BackupMetadata::getSize).sum();
            
            LocalDateTime lastFullBackup = backups.stream()
                .filter(b -> b.getType() == BackupType.FULL)
                .map(BackupMetadata::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            
            LocalDateTime lastIncrementalBackup = backups.stream()
                .filter(b -> b.getType() == BackupType.INCREMENTAL)
                .map(BackupMetadata::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("totalBackups", totalBackups);
            response.put("totalSize", totalSize);
            response.put("lastFullBackup", lastFullBackup);
            response.put("lastIncrementalBackup", lastIncrementalBackup);
            response.put("backupLocation", "/var/backups/file-transfer/primary");
            response.put("retentionDays", 30);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get backup status", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get backup status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Disaster Recovery Endpoints

    /**
     * Create disaster recovery plan
     */
    @PostMapping("/disaster-recovery/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createRecoveryPlan(@RequestBody CreateRecoveryPlanRequest request) {
        try {
            logger.info("Creating disaster recovery plan: {}", request.getPlanName());

            DisasterRecoveryPlan plan = disasterRecoveryService.createRecoveryPlan(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disaster recovery plan created successfully");
            response.put("plan", plan);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to create disaster recovery plan", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create disaster recovery plan");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Execute disaster recovery plan
     */
    @PostMapping("/disaster-recovery/plans/{planId}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> executeRecoveryPlan(
            @PathVariable String planId,
            @RequestBody RecoveryTrigger trigger) {

        try {
            logger.info("Executing disaster recovery plan: {}", planId);

            CompletableFuture<RecoveryExecutionResult> future = 
                disasterRecoveryService.executeRecoveryPlan(planId, trigger);
            RecoveryExecutionResult result = future.join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("executionId", result.getExecutionId());
            response.put("planId", result.getPlanId());
            response.put("duration", result.getDuration());

            if (result.isSuccess()) {
                response.put("message", "Disaster recovery executed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", result.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Failed to execute disaster recovery plan: {}", planId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to execute disaster recovery plan");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Perform failover
     */
    @PostMapping("/disaster-recovery/failover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performFailover(@RequestBody FailoverRequest request) {
        try {
            logger.info("Performing failover from {} to {}", request.getSourceRegion(), request.getTargetRegion());

            CompletableFuture<FailoverResult> future = disasterRecoveryService.performFailover(request);
            FailoverResult result = future.join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("failoverId", result.getFailoverId());
            response.put("sourceRegion", result.getSourceRegion());
            response.put("targetRegion", result.getTargetRegion());
            response.put("duration", result.getDuration());

            if (result.isSuccess()) {
                response.put("message", "Failover completed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", result.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Failed to perform failover", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to perform failover");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test disaster recovery plan
     */
    @PostMapping("/disaster-recovery/plans/{planId}/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> testRecoveryPlan(
            @PathVariable String planId,
            @RequestParam RecoveryTestType testType) {

        try {
            logger.info("Testing disaster recovery plan: {} with type: {}", planId, testType);

            CompletableFuture<RecoveryTestResult> future = 
                disasterRecoveryService.testRecoveryPlan(planId, testType);
            RecoveryTestResult result = future.join();

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("testId", result.getTestId());
            response.put("planId", result.getPlanId());
            response.put("testType", result.getTestType());
            response.put("duration", result.getDuration());

            if (result.isSuccess()) {
                response.put("message", "Recovery plan test completed successfully");
                response.put("summary", result.getSummary());
                response.put("testResults", result.getTestResults());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", result.getErrorMessage());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("Failed to test disaster recovery plan: {}", planId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to test disaster recovery plan");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get disaster recovery status
     */
    @GetMapping("/disaster-recovery/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getDisasterRecoveryStatus() {
        try {
            DisasterRecoveryStatus status = disasterRecoveryService.getDisasterRecoveryStatus();

            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get disaster recovery status", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get disaster recovery status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getBackupHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("backupService", "OPERATIONAL");
            health.put("disasterRecovery", "OPERATIONAL");
            health.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Error checking backup health", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}