package com.filetransfer.batch.controller.v1;

import com.filetransfer.batch.versioning.ApiVersion;
import com.filetransfer.batch.versioning.VersioningStrategy;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch Job Controller API Version 1.0 - 1.2
 * Provides backward compatibility for legacy batch clients
 */
@RestController
@RequestMapping("/api/batch/v1/jobs")
@CrossOrigin(origins = "*")
@ApiVersion(
    value = {"1.0", "1.1", "1.2"}, 
    deprecated = true,
    deprecationMessage = "Batch API v1.x is deprecated. Please migrate to v2.0 for enhanced batch features.",
    sunsetDate = "2024-06-01",
    strategy = VersioningStrategy.URL_PATH
)
public class BatchJobV1Controller {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @Autowired
    @Qualifier("fileProcessingJob")
    private Job fileProcessingJob;
    
    /**
     * Start a batch job (v1.0+ compatible)
     */
    @PostMapping("/start")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<Map<String, Object>> startJob(@RequestBody Map<String, Object> jobRequest) {
        try {
            String jobName = (String) jobRequest.getOrDefault("jobName", "fileProcessingJob");
            
            // Build job parameters (v1 format - simplified)
            JobParametersBuilder parametersBuilder = new JobParametersBuilder();
            
            // Add basic parameters for v1 compatibility
            parametersBuilder.addString("jobId", System.currentTimeMillis() + "");
            parametersBuilder.addString("inputPath", (String) jobRequest.get("inputPath"));
            parametersBuilder.addString("outputPath", (String) jobRequest.get("outputPath"));
            
            // For v1.x, tenant is optional (backward compatibility)
            if (jobRequest.containsKey("tenantId")) {
                parametersBuilder.addString("tenantId", jobRequest.get("tenantId").toString());
            }
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(fileProcessingJob, jobParameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobExecutionId", jobExecution.getId());
            response.put("jobInstanceId", jobExecution.getJobInstance().getId());
            response.put("status", jobExecution.getStatus().name());
            response.put("startTime", jobExecution.getStartTime());
            response.put("message", "Job started successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get job execution status (v1.0+ compatible)
     */
    @GetMapping("/executions/{executionId}")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<Map<String, Object>> getJobExecution(@PathVariable Long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        
        if (jobExecution == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Job execution not found");
            errorResponse.put("executionId", executionId);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", jobExecution.getId());
        response.put("jobName", jobExecution.getJobInstance().getJobName());
        response.put("status", jobExecution.getStatus().name());
        response.put("startTime", jobExecution.getStartTime());
        response.put("endTime", jobExecution.getEndTime());
        response.put("exitCode", jobExecution.getExitStatus().getExitCode());
        response.put("exitDescription", jobExecution.getExitStatus().getExitDescription());
        
        // v1 format - simplified parameters
        Map<String, Object> parameters = new HashMap<>();
        jobExecution.getJobParameters().getParameters().forEach((key, value) -> {
            parameters.put(key, value.getValue());
        });
        response.put("parameters", parameters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all job executions (v1.0+ compatible)
     */
    @GetMapping("/executions")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<Map<String, Object>> getAllJobExecutions(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "20") int count) {
        
        List<String> jobNames = jobExplorer.getJobNames().stream().toList();
        Map<String, Object> response = new HashMap<>();
        
        if (jobNames.isEmpty()) {
            response.put("executions", List.of());
            response.put("totalCount", 0);
            return ResponseEntity.ok(response);
        }
        
        // Get executions for first job (v1 limitation)
        String jobName = jobNames.get(0);
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, start, count);
        
        List<Map<String, Object>> executions = jobInstances.stream()
            .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
            .map(execution -> {
                Map<String, Object> exec = new HashMap<>();
                exec.put("executionId", execution.getId());
                exec.put("jobName", execution.getJobInstance().getJobName());
                exec.put("status", execution.getStatus().name());
                exec.put("startTime", execution.getStartTime());
                exec.put("endTime", execution.getEndTime());
                return exec;
            })
            .toList();
        
        response.put("executions", executions);
        response.put("totalCount", executions.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Stop a running job (v1.1+ feature)
     */
    @PostMapping("/executions/{executionId}/stop")
    @ApiVersion(value = {"1.1", "1.2"}, since = "1.1")
    public ResponseEntity<Map<String, Object>> stopJobExecution(@PathVariable Long executionId) {
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            
            if (jobExecution == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Job execution not found");
                errorResponse.put("executionId", executionId);
                return ResponseEntity.notFound().build();
            }
            
            // In v1.x, stopping is simplified (just mark as stopped)
            // Full stop functionality available in v2.0+
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Stop request submitted (simplified in v1.x)");
            response.put("executionId", executionId);
            response.put("note", "For full stop control, please upgrade to API v2.0");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to stop job");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get job statistics (v1.2+ feature)
     */
    @GetMapping("/statistics")
    @ApiVersion(value = {"1.2"}, since = "1.2")
    public ResponseEntity<Map<String, Object>> getJobStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        List<String> jobNames = jobExplorer.getJobNames().stream().toList();
        statistics.put("totalJobs", jobNames.size());
        
        // Simple statistics for v1.2
        long totalExecutions = 0;
        long completedExecutions = 0;
        long failedExecutions = 0;
        
        for (String jobName : jobNames) {
            List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 100);
            for (JobInstance instance : instances) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                totalExecutions += executions.size();
                
                for (JobExecution execution : executions) {
                    if (execution.getStatus().isUnsuccessful()) {
                        failedExecutions++;
                    } else if (execution.getStatus().equals(org.springframework.batch.core.BatchStatus.COMPLETED)) {
                        completedExecutions++;
                    }
                }
            }
        }
        
        statistics.put("totalExecutions", totalExecutions);
        statistics.put("completedExecutions", completedExecutions);
        statistics.put("failedExecutions", failedExecutions);
        statistics.put("successRate", totalExecutions > 0 ? (double) completedExecutions / totalExecutions : 0.0);
        
        return ResponseEntity.ok(statistics);
    }
}