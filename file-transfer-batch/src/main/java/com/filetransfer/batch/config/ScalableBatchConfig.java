package com.filetransfer.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Scalable batch configuration with partitioning and parallel processing
 * Implements distributed batch processing for high-throughput scenarios
 */
@Configuration
@EnableBatchProcessing
public class ScalableBatchConfig {

    @Value("${batch.processing.chunk-size:100}")
    private int chunkSize;

    @Value("${batch.processing.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${batch.processing.partition-size:4}")
    private int partitionSize;

    @Value("${batch.processing.max-threads:20}")
    private int maxThreads;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Main file processing job with partitioning
     */
    @Bean
    public Job fileProcessingJob() {
        return new JobBuilder("fileProcessingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(masterStep())
            .build();
    }

    /**
     * Master step that coordinates partitioned processing
     */
    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
            .partitioner("slaveStep", partitioner())
            .step(slaveStep())
            .gridSize(partitionSize)
            .taskExecutor(batchTaskExecutor())
            .build();
    }

    /**
     * Slave step for processing individual partitions
     */
    @Bean
    public Step slaveStep() {
        return new StepBuilder("slaveStep", jobRepository)
            .<FileProcessingItem, ProcessedFileItem>chunk(chunkSize, transactionManager)
            .reader(fileItemReader())
            .processor(fileItemProcessor())
            .writer(fileItemWriter())
            .taskExecutor(chunkTaskExecutor())
            .throttleLimit(maxThreads)
            .build();
    }

    /**
     * Partitioner for dividing work across multiple threads/instances
     */
    @Bean
    public Partitioner partitioner() {
        return new TenantBasedPartitioner();
    }

    /**
     * Task executor for batch processing
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("Batch-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for chunk processing within steps
     */
    @Bean
    public TaskExecutor chunkTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("Chunk-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Reader for file processing items
     */
    @Bean
    public ItemReader<FileProcessingItem> fileItemReader() {
        return new ScalableFileItemReader();
    }

    /**
     * Processor for file processing items
     */
    @Bean
    public ItemProcessor<FileProcessingItem, ProcessedFileItem> fileItemProcessor() {
        return new ParallelFileItemProcessor();
    }

    /**
     * Writer for processed file items
     */
    @Bean
    public ItemWriter<ProcessedFileItem> fileItemWriter() {
        return new BatchFileItemWriter();
    }

    /**
     * Remote chunking job for distributed processing
     */
    @Bean
    public Job remoteChunkingJob() {
        return new JobBuilder("remoteChunkingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(masterChunkStep())
            .build();
    }

    /**
     * Master step for remote chunking
     */
    @Bean
    public Step masterChunkStep() {
        return new StepBuilder("masterChunkStep", jobRepository)
            .<FileProcessingItem, ProcessedFileItem>chunk(chunkSize, transactionManager)
            .reader(fileItemReader())
            .writer(remoteChunkWriter())
            .build();
    }

    /**
     * Remote chunk writer for distributed processing
     */
    @Bean
    public ItemWriter<ProcessedFileItem> remoteChunkWriter() {
        return new RemoteChunkItemWriter();
    }

    /**
     * Parallel processing job for CPU-intensive tasks
     */
    @Bean
    public Job parallelProcessingJob() {
        return new JobBuilder("parallelProcessingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(parallelStep())
            .build();
    }

    /**
     * Parallel step with multi-threading
     */
    @Bean
    public Step parallelStep() {
        return new StepBuilder("parallelStep", jobRepository)
            .<FileProcessingItem, ProcessedFileItem>chunk(chunkSize, transactionManager)
            .reader(fileItemReader())
            .processor(asyncFileProcessor())
            .writer(fileItemWriter())
            .taskExecutor(batchTaskExecutor())
            .throttleLimit(maxThreads)
            .build();
    }

    /**
     * Async processor for non-blocking processing
     */
    @Bean
    public ItemProcessor<FileProcessingItem, ProcessedFileItem> asyncFileProcessor() {
        return new AsyncFileItemProcessor();
    }

    // Data classes for batch processing

    /**
     * Input item for file processing
     */
    public static class FileProcessingItem {
        private String fileName;
        private String tenantId;
        private String filePath;
        private long fileSize;
        private String fileType;

        // Constructors, getters, and setters
        public FileProcessingItem() {}

        public FileProcessingItem(String fileName, String tenantId, String filePath, long fileSize, String fileType) {
            this.fileName = fileName;
            this.tenantId = tenantId;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.fileType = fileType;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
    }

    /**
     * Output item for processed files
     */
    public static class ProcessedFileItem {
        private String fileName;
        private String tenantId;
        private String status;
        private String errorMessage;
        private long processingTime;
        private java.time.LocalDateTime processedAt;

        // Constructors, getters, and setters
        public ProcessedFileItem() {}

        public ProcessedFileItem(String fileName, String tenantId, String status) {
            this.fileName = fileName;
            this.tenantId = tenantId;
            this.status = status;
            this.processedAt = java.time.LocalDateTime.now();
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public java.time.LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(java.time.LocalDateTime processedAt) { this.processedAt = processedAt; }
    }
}