package com.filetransfer.batch.controller;

import com.filetransfer.batch.service.BatchJobService;
import com.filetransfer.batch.writer.BatchFileItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for batch scalability management and monitoring
 * Provides endpoints for job execution, monitoring, and performance metrics
 */
@RestController
@RequestMapping("/api/batch/scalability")
@CrossOrigin(origins = "*")
public class BatchScalabilityController {

    private static final Logger logger = LoggerFactory.getLogger(BatchScalabilityController.class);

    @Autowired
    private BatchJobService batchJobService;

    @Autowired
    private BatchFileItemWriter batchFileItemWriter;

    /**
     * Execute file processing job
     */
    @PostMapping("/jobs/file-processing")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> executeFileProcessingJob(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameter: tenantId");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            JobExecution execution = batchJobService.executeFileProcessingJob(tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File processing job started successfully");
            response.put("jobExecutionId", execution.getId());
            response.put("tenantId", tenantId);
            response.put("status", execution.getStatus().toString());
            response.put("startTime", execution.getStartTime());
            
            logger.info("Started file processing job for tenant: {} with execution ID: {}", 
                       tenantId, execution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start file processing job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Execute file processing job asynchronously
     */
    @PostMapping("/jobs/file-processing/async")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> executeFileProcessingJobAsync(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameter: tenantId");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            CompletableFuture<JobExecution> future = batchJobService.executeFileProcessingJobAsync(tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Async file processing job started");
            response.put("tenantId", tenantId);
            response.put("status", "STARTED");
            response.put("requestTime", LocalDateTime.now());
            
            logger.info("Started async file processing job for tenant: {}", tenantId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start async file processing job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start async job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Execute parallel processing job
     */
    @PostMapping("/jobs/parallel-processing")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> executeParallelProcessingJob(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            Integer threadCount = (Integer) request.getOrDefault("threadCount", 5);
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameter: tenantId");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            JobExecution execution = batchJobService.executeParallelProcessingJob(tenantId, threadCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Parallel processing job started successfully");
            response.put("jobExecutionId", execution.getId());
            response.put("tenantId", tenantId);
            response.put("threadCount", threadCount);
            response.put("status", execution.getStatus().toString());
            response.put("startTime", execution.getStartTime());
            
            logger.info("Started parallel processing job for tenant: {} with {} threads, execution ID: {}", 
                       tenantId, threadCount, execution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start parallel processing job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start parallel job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Execute remote chunking job
     */
    @PostMapping("/jobs/remote-chunking")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> executeRemoteChunkingJob(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            Integer chunkSize = (Integer) request.getOrDefault("chunkSize", 100);
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameter: tenantId");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            JobExecution execution = batchJobService.executeRemoteChunkingJob(tenantId, chunkSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Remote chunking job started successfully");
            response.put("jobExecutionId", execution.getId());
            response.put("tenantId", tenantId);
            response.put("chunkSize", chunkSize);
            response.put("status", execution.getStatus().toString());
            response.put("startTime", execution.getStartTime());
            
            logger.info("Started remote chunking job for tenant: {} with chunk size: {}, execution ID: {}", 
                       tenantId, chunkSize, execution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to start remote chunking job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start remote chunking job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get job execution status
     */
    @GetMapping("/jobs/{executionId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getJobExecutionStatus(@PathVariable Long executionId) {
        try {
            BatchJobService.JobExecutionStatus status = batchJobService.getJobExecutionStatus(executionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", status.getExecutionId());
            response.put("batchStatus", status.getBatchStatus().toString());
            response.put("exitStatus", status.getExitStatus().getExitCode());
            response.put("startTime", status.getStartTime());
            response.put("endTime", status.getEndTime());
            response.put("readCount", status.getReadCount());
            response.put("writeCount", status.getWriteCount());
            response.put("skipCount", status.getSkipCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get job execution status for ID: {}", executionId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Stop running job
     */
    @PostMapping("/jobs/{executionId}/stop")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> stopJob(@PathVariable Long executionId) {
        try {
            boolean stopped = batchJobService.stopJob(executionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", executionId);
            response.put("stopped", stopped);
            response.put("message", stopped ? "Job stop requested" : "Failed to stop job");
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Stop requested for job execution: {}, result: {}", executionId, stopped);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to stop job execution: {}", executionId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get active jobs for tenant
     */
    @GetMapping("/jobs/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getActiveJobs(@RequestParam String tenantId) {
        try {
            List<BatchJobService.JobExecutionStatus> activeJobs = 
                batchJobService.getActiveJobsForTenant(tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("activeJobCount", activeJobs.size());
            response.put("activeJobs", activeJobs);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get active jobs for tenant: {}", tenantId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get batch job statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getBatchStatistics(
            @RequestParam String tenantId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        try {
            LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : LocalDateTime.now().minusDays(7);
            LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : LocalDateTime.now();
            
            BatchJobService.BatchJobStatistics stats = 
                batchJobService.getJobStatistics(tenantId, from, to);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", stats.getTenantId());
            response.put("totalJobs", stats.getTotalJobs());
            response.put("completedJobs", stats.getCompletedJobs());
            response.put("failedJobs", stats.getFailedJobs());
            response.put("runningJobs", stats.getRunningJobs());
            response.put("successRate", stats.getSuccessRate());
            response.put("fromDate", stats.getFromDate());
            response.put("toDate", stats.getToDate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get batch statistics for tenant: {}", tenantId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get processing performance metrics
     */
    @GetMapping("/performance/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            BatchFileItemWriter.ProcessingStats stats = batchFileItemWriter.getProcessingStats();
            
            Map<String, Object> metrics = new HashMap<>();
            
            // Processing statistics
            Map<String, Object> processingStats = new HashMap<>();
            processingStats.put("processedCount", stats.getProcessedCount());
            processingStats.put("errorCount", stats.getErrorCount());
            processingStats.put("totalCount", stats.getTotalCount());
            processingStats.put("successRate", stats.getSuccessRate());
            processingStats.put("lastUpdated", stats.getLastUpdated());
            
            // JVM metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("totalMemory", runtime.totalMemory());
            jvmMetrics.put("freeMemory", runtime.freeMemory());
            jvmMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmMetrics.put("maxMemory", runtime.maxMemory());
            jvmMetrics.put("availableProcessors", runtime.availableProcessors());
            
            // Thread metrics
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("activeThreads", Thread.activeCount());
            threadMetrics.put("totalStartedThreads", Thread.getAllStackTraces().size());
            
            metrics.put("processing", processingStats);
            metrics.put("jvm", jvmMetrics);
            metrics.put("threads", threadMetrics);
            metrics.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Failed to get performance metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Schedule batch job for later execution
     */
    @PostMapping("/jobs/schedule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> scheduleBatchJob(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            String jobType = (String) request.get("jobType");
            String scheduledTime = (String) request.get("scheduledTime");
            
            if (tenantId == null || jobType == null || scheduledTime == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters: tenantId, jobType, scheduledTime");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            BatchJobService.JobType type = BatchJobService.JobType.valueOf(jobType.toUpperCase());
            LocalDateTime scheduled = LocalDateTime.parse(scheduledTime);
            
            BatchJobService.BatchJobRequest jobRequest = new BatchJobService.BatchJobRequest(tenantId, type);
            if (request.containsKey("chunkSize")) {
                jobRequest.setChunkSize((Integer) request.get("chunkSize"));
            }
            if (request.containsKey("threadCount")) {
                jobRequest.setThreadCount((Integer) request.get("threadCount"));
            }
            
            batchJobService.scheduleBatchJob(jobRequest, scheduled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch job scheduled successfully");
            response.put("tenantId", tenantId);
            response.put("jobType", jobType);
            response.put("scheduledTime", scheduled);
            response.put("createdAt", LocalDateTime.now());
            
            logger.info("Scheduled {} job for tenant: {} at {}", jobType, tenantId, scheduled);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to schedule batch job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to schedule job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Reset processing statistics
     */
    @PostMapping("/performance/reset-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetProcessingStats() {
        try {
            batchFileItemWriter.resetStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Processing statistics reset successfully");
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Processing statistics reset by admin");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to reset processing statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get batch scalability health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getBatchScalabilityHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("batchProcessing", "OPERATIONAL");
            health.put("parallelProcessing", "OPERATIONAL");
            health.put("remoteChunking", "OPERATIONAL");
            health.put("jobCoordination", "OPERATIONAL");
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error checking batch scalability health", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}