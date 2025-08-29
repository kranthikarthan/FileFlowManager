package com.filetransfer.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing and orchestrating batch jobs
 * Provides job execution, monitoring, and coordination capabilities
 */
@Service
public class BatchJobService {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobService.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("fileProcessingJob")
    private Job fileProcessingJob;

    @Autowired
    @Qualifier("remoteChunkingJob")
    private Job remoteChunkingJob;

    @Autowired
    @Qualifier("parallelProcessingJob")
    private Job parallelProcessingJob;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Execute file processing job synchronously
     */
    public JobExecution executeFileProcessingJob(String tenantId) 
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
                   JobRestartException, JobInstanceAlreadyCompleteException {
        
        logger.info("Starting file processing job for tenant: {}", tenantId);
        
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("tenantId", tenantId)
            .addDate("startTime", new Date())
            .addString("jobId", generateJobId("file-processing", tenantId))
            .toJobParameters();
        
        JobExecution execution = jobLauncher.run(fileProcessingJob, jobParameters);
        
        logger.info("File processing job started with execution ID: {} for tenant: {}", 
                   execution.getId(), tenantId);
        
        return execution;
    }

    /**
     * Execute file processing job asynchronously
     */
    @Async("batchTaskExecutor")
    public CompletableFuture<JobExecution> executeFileProcessingJobAsync(String tenantId) {
        try {
            JobExecution execution = executeFileProcessingJob(tenantId);
            return CompletableFuture.completedFuture(execution);
        } catch (Exception e) {
            logger.error("Failed to execute file processing job for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Execute remote chunking job for distributed processing
     */
    public JobExecution executeRemoteChunkingJob(String tenantId, int chunkSize) 
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
                   JobRestartException, JobInstanceAlreadyCompleteException {
        
        logger.info("Starting remote chunking job for tenant: {} with chunk size: {}", 
                   tenantId, chunkSize);
        
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("tenantId", tenantId)
            .addLong("chunkSize", (long) chunkSize)
            .addDate("startTime", new Date())
            .addString("jobId", generateJobId("remote-chunking", tenantId))
            .toJobParameters();
        
        JobExecution execution = jobLauncher.run(remoteChunkingJob, jobParameters);
        
        logger.info("Remote chunking job started with execution ID: {} for tenant: {}", 
                   execution.getId(), tenantId);
        
        return execution;
    }

    /**
     * Execute parallel processing job for CPU-intensive tasks
     */
    public JobExecution executeParallelProcessingJob(String tenantId, int threadCount) 
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
                   JobRestartException, JobInstanceAlreadyCompleteException {
        
        logger.info("Starting parallel processing job for tenant: {} with {} threads", 
                   tenantId, threadCount);
        
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("tenantId", tenantId)
            .addLong("threadCount", (long) threadCount)
            .addDate("startTime", new Date())
            .addString("jobId", generateJobId("parallel-processing", tenantId))
            .toJobParameters();
        
        JobExecution execution = jobLauncher.run(parallelProcessingJob, jobParameters);
        
        logger.info("Parallel processing job started with execution ID: {} for tenant: {}", 
                   execution.getId(), tenantId);
        
        return execution;
    }

    /**
     * Execute batch job based on processing type
     */
    public JobExecution executeBatchJob(BatchJobRequest request) 
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
                   JobRestartException, JobInstanceAlreadyCompleteException {
        
        return switch (request.getJobType()) {
            case FILE_PROCESSING -> executeFileProcessingJob(request.getTenantId());
            case REMOTE_CHUNKING -> executeRemoteChunkingJob(request.getTenantId(), request.getChunkSize());
            case PARALLEL_PROCESSING -> executeParallelProcessingJob(request.getTenantId(), request.getThreadCount());
        };
    }

    /**
     * Get job execution status
     */
    @Cacheable(value = "jobExecutionCache", key = "#executionId")
    public JobExecutionStatus getJobExecutionStatus(Long executionId) {
        // In a real implementation, this would query the JobRepository
        // For now, we'll create a mock status
        return new JobExecutionStatus(
            executionId,
            BatchStatus.UNKNOWN,
            ExitStatus.UNKNOWN,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0L,
            0L,
            0L
        );
    }

    /**
     * Stop running job
     */
    public boolean stopJob(Long executionId) {
        try {
            // In a real implementation, this would interact with JobOperator
            logger.info("Stopping job execution: {}", executionId);
            
            // Store stop request in Redis for distributed coordination
            if (redisTemplate != null) {
                String key = "job:stop:" + executionId;
                redisTemplate.opsForValue().set(key, "STOP_REQUESTED", 300, TimeUnit.SECONDS);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop job execution: {}", executionId, e);
            return false;
        }
    }

    /**
     * Restart failed job
     */
    public JobExecution restartJob(Long executionId) 
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
                   JobRestartException, JobInstanceAlreadyCompleteException {
        
        logger.info("Restarting job execution: {}", executionId);
        
        // In a real implementation, this would:
        // 1. Get the original job parameters
        // 2. Create new parameters with restart flag
        // 3. Launch the job with restart parameters
        
        // For now, return a mock execution
        return new JobExecution(executionId);
    }

    /**
     * Get active job executions for a tenant
     */
    public List<JobExecutionStatus> getActiveJobsForTenant(String tenantId) {
        // In a real implementation, this would query the JobRepository
        // for active jobs filtered by tenant
        return List.of();
    }

    /**
     * Get job execution statistics
     */
    public BatchJobStatistics getJobStatistics(String tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        // In a real implementation, this would aggregate statistics from JobRepository
        return new BatchJobStatistics(
            tenantId,
            0L, 0L, 0L, 0L,
            fromDate, toDate
        );
    }

    /**
     * Schedule batch job for later execution
     */
    public void scheduleBatchJob(BatchJobRequest request, LocalDateTime scheduledTime) {
        logger.info("Scheduling batch job {} for tenant {} at {}", 
                   request.getJobType(), request.getTenantId(), scheduledTime);
        
        // Store scheduled job in Redis
        if (redisTemplate != null) {
            String key = "job:scheduled:" + generateJobId(request.getJobType().name(), request.getTenantId());
            ScheduledBatchJob scheduledJob = new ScheduledBatchJob(request, scheduledTime);
            
            long delaySeconds = java.time.Duration.between(LocalDateTime.now(), scheduledTime).getSeconds();
            redisTemplate.opsForValue().set(key, scheduledJob, Math.max(delaySeconds, 60), TimeUnit.SECONDS);
        }
    }

    /**
     * Generate unique job ID
     */
    private String generateJobId(String jobType, String tenantId) {
        return String.format("%s_%s_%d", jobType, tenantId, System.currentTimeMillis());
    }

    // Data classes

    /**
     * Batch job request
     */
    public static class BatchJobRequest {
        private String tenantId;
        private JobType jobType;
        private int chunkSize = 100;
        private int threadCount = 5;

        public BatchJobRequest() {}

        public BatchJobRequest(String tenantId, JobType jobType) {
            this.tenantId = tenantId;
            this.jobType = jobType;
        }

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public JobType getJobType() { return jobType; }
        public void setJobType(JobType jobType) { this.jobType = jobType; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    }

    /**
     * Job types
     */
    public enum JobType {
        FILE_PROCESSING,
        REMOTE_CHUNKING,
        PARALLEL_PROCESSING
    }

    /**
     * Job execution status
     */
    public static class JobExecutionStatus {
        private final Long executionId;
        private final BatchStatus batchStatus;
        private final ExitStatus exitStatus;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Long readCount;
        private final Long writeCount;
        private final Long skipCount;

        public JobExecutionStatus(Long executionId, BatchStatus batchStatus, ExitStatus exitStatus,
                                 LocalDateTime startTime, LocalDateTime endTime,
                                 Long readCount, Long writeCount, Long skipCount) {
            this.executionId = executionId;
            this.batchStatus = batchStatus;
            this.exitStatus = exitStatus;
            this.startTime = startTime;
            this.endTime = endTime;
            this.readCount = readCount;
            this.writeCount = writeCount;
            this.skipCount = skipCount;
        }

        // Getters
        public Long getExecutionId() { return executionId; }
        public BatchStatus getBatchStatus() { return batchStatus; }
        public ExitStatus getExitStatus() { return exitStatus; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Long getReadCount() { return readCount; }
        public Long getWriteCount() { return writeCount; }
        public Long getSkipCount() { return skipCount; }
    }

    /**
     * Batch job statistics
     */
    public static class BatchJobStatistics {
        private final String tenantId;
        private final Long totalJobs;
        private final Long completedJobs;
        private final Long failedJobs;
        private final Long runningJobs;
        private final LocalDateTime fromDate;
        private final LocalDateTime toDate;

        public BatchJobStatistics(String tenantId, Long totalJobs, Long completedJobs,
                                 Long failedJobs, Long runningJobs,
                                 LocalDateTime fromDate, LocalDateTime toDate) {
            this.tenantId = tenantId;
            this.totalJobs = totalJobs;
            this.completedJobs = completedJobs;
            this.failedJobs = failedJobs;
            this.runningJobs = runningJobs;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        // Getters
        public String getTenantId() { return tenantId; }
        public Long getTotalJobs() { return totalJobs; }
        public Long getCompletedJobs() { return completedJobs; }
        public Long getFailedJobs() { return failedJobs; }
        public Long getRunningJobs() { return runningJobs; }
        public LocalDateTime getFromDate() { return fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        
        public double getSuccessRate() {
            return totalJobs > 0 ? (double) completedJobs / totalJobs * 100.0 : 0.0;
        }
    }

    /**
     * Scheduled batch job
     */
    public static class ScheduledBatchJob {
        private final BatchJobRequest request;
        private final LocalDateTime scheduledTime;
        private final LocalDateTime createdAt;

        public ScheduledBatchJob(BatchJobRequest request, LocalDateTime scheduledTime) {
            this.request = request;
            this.scheduledTime = scheduledTime;
            this.createdAt = LocalDateTime.now();
        }

        // Getters
        public BatchJobRequest getRequest() { return request; }
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}