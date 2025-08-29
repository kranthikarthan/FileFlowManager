package com.filetransfer.batch.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitoring configuration for batch processing
 * Provides comprehensive metrics, tracing, and observability
 */
@Configuration
public class BatchMonitoringConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Job execution listener for monitoring
     */
    @Bean
    public BatchJobExecutionListener jobExecutionListener() {
        return new BatchJobExecutionListener(meterRegistry);
    }

    /**
     * Step execution listener for monitoring
     */
    @Bean
    public BatchStepExecutionListener stepExecutionListener() {
        return new BatchStepExecutionListener(meterRegistry);
    }

    /**
     * Batch metrics service
     */
    @Bean
    public BatchMetricsService batchMetricsService() {
        return new BatchMetricsService(meterRegistry);
    }

    /**
     * Job execution listener implementation
     */
    @Component
    public static class BatchJobExecutionListener implements JobExecutionListener {
        
        private final MeterRegistry meterRegistry;
        private final Timer jobTimer;
        private final Counter jobStartCounter;
        private final Counter jobCompletedCounter;
        private final Counter jobFailedCounter;
        
        public BatchJobExecutionListener(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.jobTimer = Timer.builder("batch.job.duration")
                .description("Job execution duration")
                .register(meterRegistry);
            this.jobStartCounter = Counter.builder("batch.job.started")
                .description("Number of jobs started")
                .register(meterRegistry);
            this.jobCompletedCounter = Counter.builder("batch.job.completed")
                .description("Number of jobs completed successfully")
                .register(meterRegistry);
            this.jobFailedCounter = Counter.builder("batch.job.failed")
                .description("Number of jobs failed")
                .register(meterRegistry);
        }

        @Override
        public void beforeJob(JobExecution jobExecution) {
            jobStartCounter.increment();
            
            // Add job name and tenant ID tags
            String jobName = jobExecution.getJobInstance().getJobName();
            String tenantId = jobExecution.getJobParameters().getString("tenantId", "unknown");
            
            meterRegistry.counter("batch.job.started", 
                "job.name", jobName, 
                "tenant.id", tenantId).increment();
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String tenantId = jobExecution.getJobParameters().getString("tenantId", "unknown");
            String status = jobExecution.getStatus().toString();
            
            // Record job duration
            Duration duration = Duration.between(
                jobExecution.getStartTime().toInstant(),
                jobExecution.getEndTime().toInstant()
            );
            
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("batch.job.duration")
                .tag("job.name", jobName)
                .tag("tenant.id", tenantId)
                .tag("status", status)
                .register(meterRegistry));
            
            // Record completion metrics
            if (jobExecution.getStatus().isUnsuccessful()) {
                jobFailedCounter.increment();
                meterRegistry.counter("batch.job.failed", 
                    "job.name", jobName, 
                    "tenant.id", tenantId,
                    "exit.code", jobExecution.getExitStatus().getExitCode()).increment();
            } else {
                jobCompletedCounter.increment();
                meterRegistry.counter("batch.job.completed", 
                    "job.name", jobName, 
                    "tenant.id", tenantId).increment();
            }
            
            // Record read/write counts
            long totalReadCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getReadCount)
                .sum();
            long totalWriteCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
            long totalSkipCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getSkipCount)
                .sum();
            
            meterRegistry.counter("batch.items.processed", 
                "job.name", jobName, 
                "tenant.id", tenantId,
                "type", "read").increment(totalReadCount);
            meterRegistry.counter("batch.items.processed", 
                "job.name", jobName, 
                "tenant.id", tenantId,
                "type", "write").increment(totalWriteCount);
            meterRegistry.counter("batch.items.processed", 
                "job.name", jobName, 
                "tenant.id", tenantId,
                "type", "skip").increment(totalSkipCount);
        }
    }

    /**
     * Step execution listener implementation
     */
    @Component
    public static class BatchStepExecutionListener implements StepExecutionListener {
        
        private final MeterRegistry meterRegistry;
        private final Timer stepTimer;
        
        public BatchStepExecutionListener(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.stepTimer = Timer.builder("batch.step.duration")
                .description("Step execution duration")
                .register(meterRegistry);
        }

        @Override
        public void beforeStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
            
            meterRegistry.counter("batch.step.started", 
                "step.name", stepName, 
                "job.name", jobName).increment();
        }

        @Override
        public void afterStep(StepExecution stepExecution) {
            String stepName = stepExecution.getStepName();
            String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
            String status = stepExecution.getStatus().toString();
            
            // Record step duration
            Duration duration = Duration.between(
                stepExecution.getStartTime().toInstant(),
                stepExecution.getEndTime().toInstant()
            );
            
            stepTimer.record(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            Timer.builder("batch.step.duration")
                .tag("step.name", stepName)
                .tag("job.name", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .record(duration);
            
            // Record step metrics
            meterRegistry.counter("batch.step.completed", 
                "step.name", stepName, 
                "job.name", jobName,
                "status", status).increment();
            
            // Record processing counts
            meterRegistry.gauge("batch.step.read.count", 
                stepExecution, StepExecution::getReadCount);
            meterRegistry.gauge("batch.step.write.count", 
                stepExecution, StepExecution::getWriteCount);
            meterRegistry.gauge("batch.step.skip.count", 
                stepExecution, StepExecution::getSkipCount);
        }
    }

    /**
     * Batch metrics service for custom metrics
     */
    @Component
    public static class BatchMetricsService {
        
        private final MeterRegistry meterRegistry;
        private final AtomicLong activeJobs = new AtomicLong(0);
        private final AtomicLong queuedJobs = new AtomicLong(0);
        private final AtomicLong totalThroughput = new AtomicLong(0);
        
        public BatchMetricsService(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Register gauges
            Gauge.builder("batch.jobs.active")
                .description("Number of currently active jobs")
                .register(meterRegistry, this, metrics -> metrics.activeJobs.get());
            
            Gauge.builder("batch.jobs.queued")
                .description("Number of queued jobs")
                .register(meterRegistry, this, metrics -> metrics.queuedJobs.get());
            
            Gauge.builder("batch.throughput.total")
                .description("Total items processed per minute")
                .register(meterRegistry, this, metrics -> metrics.totalThroughput.get());
        }
        
        public void incrementActiveJobs() {
            activeJobs.incrementAndGet();
        }
        
        public void decrementActiveJobs() {
            activeJobs.decrementAndGet();
        }
        
        public void setQueuedJobs(long count) {
            queuedJobs.set(count);
        }
        
        public void recordThroughput(long itemsProcessed) {
            totalThroughput.addAndGet(itemsProcessed);
        }
        
        public void recordFileProcessingLatency(String tenantId, String fileType, long durationMs) {
            Timer.builder("batch.file.processing.latency")
                .tag("tenant.id", tenantId)
                .tag("file.type", fileType)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        public void recordDatabaseOperationTime(String operation, long durationMs) {
            Timer.builder("batch.database.operation.duration")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        public void recordCacheHitRate(String cacheType, boolean hit) {
            meterRegistry.counter("batch.cache.access", 
                "cache.type", cacheType,
                "result", hit ? "hit" : "miss").increment();
        }
        
        public void recordMessageQueueMetrics(String queueName, long messageCount, long consumerCount) {
            meterRegistry.gauge("batch.queue.message.count", 
                meterRegistry.gauge("batch.queue.message.count")
                    .tag("queue.name", queueName), messageCount);
            meterRegistry.gauge("batch.queue.consumer.count", 
                meterRegistry.gauge("batch.queue.consumer.count")
                    .tag("queue.name", queueName), consumerCount);
        }
        
        public Timer.Sample startFileProcessingTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordFileProcessingTime(Timer.Sample sample, String tenantId, String processingType) {
            sample.stop(Timer.builder("batch.file.processing.time")
                .tag("tenant.id", tenantId)
                .tag("processing.type", processingType)
                .register(meterRegistry));
        }
    }
}