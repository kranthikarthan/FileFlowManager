package com.filetransfer.batch.controller.v2;

import com.filetransfer.batch.versioning.ApiVersion;
import com.filetransfer.batch.versioning.VersioningStrategy;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Batch Job Controller API Version 2.0+
 * Enhanced version with advanced batch management, monitoring, and control
 */
@RestController
@RequestMapping("/api/batch/v2/jobs")
@CrossOrigin(origins = "*")
@Validated
@ApiVersion(
    value = {"2.0", "2.1"}, 
    strategy = VersioningStrategy.URL_PATH,
    requiresVersion = true
)
public class BatchJobV2Controller {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @Autowired
    private JobOperator jobOperator;
    
    @Autowired
    @Qualifier("fileProcessingJob")
    private Job fileProcessingJob;
    
    /**
     * Start a batch job with enhanced parameters (v2.0+)
     */
    @PostMapping("/start")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> startJob(@Valid @RequestBody BatchJobV2Request jobRequest) {
        try {
            // Enhanced parameter building for v2.0+
            JobParametersBuilder parametersBuilder = new JobParametersBuilder();
            
            // Required parameters in v2.0+
            parametersBuilder.addString("jobId", UUID.randomUUID().toString());
            parametersBuilder.addString("tenantId", jobRequest.getTenantId()); // Required in v2.0+
            parametersBuilder.addString("inputPath", jobRequest.getInputPath());
            parametersBuilder.addString("outputPath", jobRequest.getOutputPath());
            parametersBuilder.addLong("timestamp", System.currentTimeMillis());
            
            // Enhanced parameters
            if (jobRequest.getChunkSize() != null) {
                parametersBuilder.addLong("chunkSize", jobRequest.getChunkSize());
            }
            if (jobRequest.getThreadCount() != null) {
                parametersBuilder.addLong("threadCount", jobRequest.getThreadCount());
            }
            if (jobRequest.getRetryLimit() != null) {
                parametersBuilder.addLong("retryLimit", jobRequest.getRetryLimit());
            }
            if (jobRequest.getSkipLimit() != null) {
                parametersBuilder.addLong("skipLimit", jobRequest.getSkipLimit());
            }
            
            // Custom parameters
            if (jobRequest.getCustomParameters() != null) {
                jobRequest.getCustomParameters().forEach((key, value) -> {
                    if (value instanceof String) {
                        parametersBuilder.addString(key, (String) value);
                    } else if (value instanceof Long) {
                        parametersBuilder.addLong(key, (Long) value);
                    } else if (value instanceof Double) {
                        parametersBuilder.addDouble(key, (Double) value);
                    }
                });
            }
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(fileProcessingJob, jobParameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobExecutionId", jobExecution.getId());
            response.put("jobInstanceId", jobExecution.getJobInstance().getId());
            response.put("jobName", jobExecution.getJobInstance().getJobName());
            response.put("status", jobExecution.getStatus().name());
            response.put("startTime", jobExecution.getStartTime());
            response.put("createTime", jobExecution.getCreateTime());
            response.put("version", jobExecution.getVersion());
            response.put("message", "Job started successfully");
            
            // Enhanced response for v2.0+
            response.put("parameters", convertJobParameters(jobExecution.getJobParameters()));
            response.put("executionContext", jobExecution.getExecutionContext().entrySet());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start job");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get job execution with detailed information (v2.0+)
     */
    @GetMapping("/executions/{executionId}")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getJobExecution(@PathVariable Long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        
        if (jobExecution == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Job execution not found");
            errorResponse.put("executionId", executionId);
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", jobExecution.getId());
        response.put("jobInstanceId", jobExecution.getJobInstance().getId());
        response.put("jobName", jobExecution.getJobInstance().getJobName());
        response.put("status", jobExecution.getStatus().name());
        response.put("startTime", jobExecution.getStartTime());
        response.put("createTime", jobExecution.getCreateTime());
        response.put("endTime", jobExecution.getEndTime());
        response.put("lastUpdated", jobExecution.getLastUpdated());
        response.put("version", jobExecution.getVersion());
        response.put("exitCode", jobExecution.getExitStatus().getExitCode());
        response.put("exitDescription", jobExecution.getExitStatus().getExitDescription());
        
        // Enhanced details for v2.0+
        response.put("parameters", convertJobParameters(jobExecution.getJobParameters()));
        response.put("executionContext", jobExecution.getExecutionContext().entrySet());
        
        // Step executions
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        List<Map<String, Object>> steps = stepExecutions.stream()
            .map(this::convertStepExecution)
            .collect(Collectors.toList());
        response.put("stepExecutions", steps);
        
        // Performance metrics
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            long duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            response.put("durationMs", duration);
            response.put("durationFormatted", formatDuration(duration));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get job executions with pagination and filtering (v2.0+)
     */
    @GetMapping("/executions")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Page<Map<String, Object>>> getJobExecutions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId) {
        
        Pageable pageable = PageRequest.of(page, size);
        List<Map<String, Object>> executions = new ArrayList<>();
        
        List<String> jobNames = jobName != null ? 
            List.of(jobName) : jobExplorer.getJobNames().stream().toList();
        
        for (String name : jobNames) {
            List<JobInstance> instances = jobExplorer.getJobInstances(name, 0, 1000);
            
            for (JobInstance instance : instances) {
                List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(instance);
                
                for (JobExecution execution : jobExecutions) {
                    // Filter by status if specified
                    if (status != null && !execution.getStatus().name().equalsIgnoreCase(status)) {
                        continue;
                    }
                    
                    // Filter by tenant if specified
                    if (tenantId != null) {
                        String executionTenantId = execution.getJobParameters().getString("tenantId");
                        if (!tenantId.equals(executionTenantId)) {
                            continue;
                        }
                    }
                    
                    Map<String, Object> executionData = new HashMap<>();
                    executionData.put("executionId", execution.getId());
                    executionData.put("jobInstanceId", execution.getJobInstance().getId());
                    executionData.put("jobName", execution.getJobInstance().getJobName());
                    executionData.put("status", execution.getStatus().name());
                    executionData.put("startTime", execution.getStartTime());
                    executionData.put("endTime", execution.getEndTime());
                    executionData.put("exitCode", execution.getExitStatus().getExitCode());
                    
                    // Add tenant information
                    String execTenantId = execution.getJobParameters().getString("tenantId");
                    if (execTenantId != null) {
                        executionData.put("tenantId", execTenantId);
                    }
                    
                    executions.add(executionData);
                }
            }
        }
        
        // Sort by start time (newest first)
        executions.sort((a, b) -> {
            Date dateA = (Date) a.get("startTime");
            Date dateB = (Date) b.get("startTime");
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), executions.size());
        List<Map<String, Object>> pageContent = executions.subList(start, end);
        
        Page<Map<String, Object>> result = new PageImpl<>(pageContent, pageable, executions.size());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Stop/abandon a running job with enhanced control (v2.0+)
     */
    @PostMapping("/executions/{executionId}/stop")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> stopJobExecution(
            @PathVariable Long executionId,
            @RequestParam(defaultValue = "false") boolean abandon) {
        
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            
            if (jobExecution == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Job execution not found");
                errorResponse.put("executionId", executionId);
                return ResponseEntity.notFound().build();
            }
            
            if (abandon) {
                // Abandon the job execution
                jobOperator.abandon(executionId);
            } else {
                // Stop the job execution
                jobOperator.stop(executionId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", abandon ? "Job execution abandoned" : "Job execution stopped");
            response.put("executionId", executionId);
            response.put("action", abandon ? "ABANDON" : "STOP");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to stop job execution");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("executionId", executionId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Restart a failed job execution (v2.0+)
     */
    @PostMapping("/executions/{executionId}/restart")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> restartJobExecution(@PathVariable Long executionId) {
        try {
            Long newExecutionId = jobOperator.restart(executionId);
            JobExecution newExecution = jobExplorer.getJobExecution(newExecutionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job execution restarted");
            response.put("originalExecutionId", executionId);
            response.put("newExecutionId", newExecutionId);
            response.put("status", newExecution.getStatus().name());
            response.put("startTime", newExecution.getStartTime());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to restart job execution");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("executionId", executionId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get comprehensive job statistics and metrics (v2.0+)
     */
    @GetMapping("/statistics")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getJobStatistics(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String period) {
        
        Map<String, Object> statistics = new HashMap<>();
        List<String> jobNames = jobExplorer.getJobNames().stream().toList();
        
        statistics.put("totalJobs", jobNames.size());
        
        // Detailed statistics
        long totalExecutions = 0;
        long completedExecutions = 0;
        long failedExecutions = 0;
        long runningExecutions = 0;
        long stoppedExecutions = 0;
        
        Map<String, Long> jobExecutionCounts = new HashMap<>();
        Map<String, Long> tenantExecutionCounts = new HashMap<>();
        
        for (String jobName : jobNames) {
            List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1000);
            long jobExecutionCount = 0;
            
            for (JobInstance instance : instances) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                jobExecutionCount += executions.size();
                totalExecutions += executions.size();
                
                for (JobExecution execution : executions) {
                    // Filter by tenant if specified
                    String execTenantId = execution.getJobParameters().getString("tenantId");
                    if (tenantId != null && !tenantId.equals(execTenantId)) {
                        continue;
                    }
                    
                    // Count by tenant
                    if (execTenantId != null) {
                        tenantExecutionCounts.merge(execTenantId, 1L, Long::sum);
                    }
                    
                    // Count by status
                    BatchStatus status = execution.getStatus();
                    if (status.equals(BatchStatus.COMPLETED)) {
                        completedExecutions++;
                    } else if (status.equals(BatchStatus.FAILED)) {
                        failedExecutions++;
                    } else if (status.equals(BatchStatus.STARTED) || status.equals(BatchStatus.STARTING)) {
                        runningExecutions++;
                    } else if (status.equals(BatchStatus.STOPPED) || status.equals(BatchStatus.STOPPING)) {
                        stoppedExecutions++;
                    }
                }
            }
            
            jobExecutionCounts.put(jobName, jobExecutionCount);
        }
        
        statistics.put("totalExecutions", totalExecutions);
        statistics.put("completedExecutions", completedExecutions);
        statistics.put("failedExecutions", failedExecutions);
        statistics.put("runningExecutions", runningExecutions);
        statistics.put("stoppedExecutions", stoppedExecutions);
        statistics.put("successRate", totalExecutions > 0 ? (double) completedExecutions / totalExecutions : 0.0);
        
        statistics.put("jobExecutionCounts", jobExecutionCounts);
        statistics.put("tenantExecutionCounts", tenantExecutionCounts);
        
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Get job execution metrics and performance data (v2.1+ feature)
     */
    @GetMapping("/executions/{executionId}/metrics")
    @ApiVersion(value = {"2.1"}, since = "2.1")
    public ResponseEntity<Map<String, Object>> getJobExecutionMetrics(@PathVariable Long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        
        if (jobExecution == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Performance metrics
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            long duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
            metrics.put("durationMs", duration);
            metrics.put("durationFormatted", formatDuration(duration));
        }
        
        // Step metrics
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        List<Map<String, Object>> stepMetrics = stepExecutions.stream()
            .map(step -> {
                Map<String, Object> stepData = new HashMap<>();
                stepData.put("stepName", step.getStepName());
                stepData.put("readCount", step.getReadCount());
                stepData.put("writeCount", step.getWriteCount());
                stepData.put("commitCount", step.getCommitCount());
                stepData.put("rollbackCount", step.getRollbackCount());
                stepData.put("readSkipCount", step.getReadSkipCount());
                stepData.put("processSkipCount", step.getProcessSkipCount());
                stepData.put("writeSkipCount", step.getWriteSkipCount());
                stepData.put("filterCount", step.getFilterCount());
                
                if (step.getStartTime() != null && step.getEndTime() != null) {
                    long stepDuration = step.getEndTime().getTime() - step.getStartTime().getTime();
                    stepData.put("durationMs", stepDuration);
                    
                    // Calculate throughput
                    if (stepDuration > 0) {
                        double throughput = (double) step.getReadCount() / (stepDuration / 1000.0);
                        stepData.put("throughputPerSecond", throughput);
                    }
                }
                
                return stepData;
            })
            .collect(Collectors.toList());
        
        metrics.put("stepMetrics", stepMetrics);
        
        return ResponseEntity.ok(metrics);
    }
    
    // Helper methods
    private Map<String, Object> convertJobParameters(JobParameters jobParameters) {
        Map<String, Object> parameters = new HashMap<>();
        jobParameters.getParameters().forEach((key, value) -> {
            parameters.put(key, value.getValue());
        });
        return parameters;
    }
    
    private Map<String, Object> convertStepExecution(StepExecution stepExecution) {
        Map<String, Object> step = new HashMap<>();
        step.put("stepName", stepExecution.getStepName());
        step.put("status", stepExecution.getStatus().name());
        step.put("startTime", stepExecution.getStartTime());
        step.put("endTime", stepExecution.getEndTime());
        step.put("readCount", stepExecution.getReadCount());
        step.put("writeCount", stepExecution.getWriteCount());
        step.put("commitCount", stepExecution.getCommitCount());
        step.put("rollbackCount", stepExecution.getRollbackCount());
        step.put("exitCode", stepExecution.getExitStatus().getExitCode());
        step.put("exitDescription", stepExecution.getExitStatus().getExitDescription());
        return step;
    }
    
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Request DTO for v2.0+
    public static class BatchJobV2Request {
        private String tenantId; // Required in v2.0+
        private String inputPath;
        private String outputPath;
        private Long chunkSize;
        private Long threadCount;
        private Long retryLimit;
        private Long skipLimit;
        private Map<String, Object> customParameters;
        
        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getInputPath() { return inputPath; }
        public void setInputPath(String inputPath) { this.inputPath = inputPath; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        public Long getChunkSize() { return chunkSize; }
        public void setChunkSize(Long chunkSize) { this.chunkSize = chunkSize; }
        public Long getThreadCount() { return threadCount; }
        public void setThreadCount(Long threadCount) { this.threadCount = threadCount; }
        public Long getRetryLimit() { return retryLimit; }
        public void setRetryLimit(Long retryLimit) { this.retryLimit = retryLimit; }
        public Long getSkipLimit() { return skipLimit; }
        public void setSkipLimit(Long skipLimit) { this.skipLimit = skipLimit; }
        public Map<String, Object> getCustomParameters() { return customParameters; }
        public void setCustomParameters(Map<String, Object> customParameters) { this.customParameters = customParameters; }
    }
}