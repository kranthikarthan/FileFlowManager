# File Transfer Batch Application - Low Level Design (LLD)

## 1. Document Overview

### 1.1 Purpose
This document provides detailed low-level design specifications for the File Transfer Batch Application, including batch job configurations, processing pipelines, scheduling mechanisms, and implementation details.

### 1.2 Scope
- Spring Batch job configurations and processing pipelines
- Scheduled job implementations and timing
- File monitoring and processing algorithms
- ACK/NACK batch processing workflows
- Performance optimization for high-volume processing

## 2. Application Structure

### 2.1 Package Structure
```
com.filetransfer.batch/
├── FileTransferBatchApplication.java   # Main application class
├── config/                             # Configuration Classes
│   ├── BatchConfig.java
│   ├── AckNackBatchConfig.java
│   ├── ScalableBatchConfig.java
│   ├── BatchMonitoringConfig.java
│   ├── FileTransferConfig.java
│   ├── TenantBasedPartitioner.java
│   ├── BatchInputValidationFilter.java
│   ├── BatchSecurityHeadersFilter.java
│   ├── BatchRateLimitingFilter.java
│   ├── BatchInputValidationService.java
│   └── BatchEncryptionService.java
├── reader/                             # Batch Readers
│   ├── ScalableFileItemReader.java
│   └── AckNackFileReader.java
├── processor/                          # Batch Processors
│   ├── AsyncFileItemProcessor.java
│   ├── ParallelFileItemProcessor.java
│   └── AckNackFileProcessor.java
├── writer/                             # Batch Writers
│   ├── BatchFileItemWriter.java
│   ├── RemoteChunkItemWriter.java
│   └── AckNackFileWriter.java
├── service/                            # Business Logic Services
│   ├── FileTransferService.java
│   ├── AckNackService.java
│   ├── FileMonitoringService.java
│   ├── FileProcessingService.java
│   ├── FileValidationService.java
│   ├── BatchJobService.java
│   ├── BatchBackupService.java
│   ├── SchemaValidationService.java
│   ├── CutOffTimeService.java
│   └── HolidayService.java
├── controller/                         # REST Controllers
│   ├── v1/
│   │   └── BatchJobV1Controller.java
│   ├── v2/
│   │   └── BatchJobV2Controller.java
│   ├── BatchScalabilityController.java
│   └── AckNackBatchController.java
├── entity/                             # JPA Entities
│   ├── FileTransferRecord.java
│   ├── AckNackRecord.java
│   ├── ServiceConfiguration.java
│   ├── SubServiceConfiguration.java
│   ├── Holiday.java
│   ├── TransferStatus.java (Enum)
│   ├── TransferDirection.java (Enum)
│   ├── FileType.java (Enum)
│   ├── AckNackStatus.java (Enum)
│   ├── AckNackType.java (Enum)
│   └── CutOffTimeType.java (Enum)
├── repository/                         # Data Access Layer
│   ├── FileTransferRecordRepository.java
│   ├── AckNackRecordRepository.java
│   ├── ServiceConfigurationRepository.java
│   ├── SubServiceConfigurationRepository.java
│   └── HolidayRepository.java
├── model/                              # Domain Models
│   └── BatchBackupModels.java
└── versioning/                         # API Versioning
    ├── ApiVersion.java (Enum)
    └── VersioningStrategy.java (Enum)
```

## 3. Spring Batch Architecture

### 3.1 Batch Processing Pipeline

#### File Processing Job Configuration
```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    @Bean
    public Job fileProcessingJob(JobRepository jobRepository, 
                                Step fileProcessingStep) {
        return new JobBuilder("fileProcessingJob", jobRepository)
            .start(fileProcessingStep)
            .build();
    }
    
    @Bean
    public Step fileProcessingStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  ItemReader<FileItem> fileReader,
                                  ItemProcessor<FileItem, ProcessedFile> fileProcessor,
                                  ItemWriter<ProcessedFile> fileWriter) {
        return new StepBuilder("fileProcessingStep", jobRepository)
            .<FileItem, ProcessedFile>chunk(10, transactionManager)
            .reader(fileReader)
            .processor(fileProcessor)
            .writer(fileWriter)
            .faultTolerant()
            .retryLimit(3)
            .retry(Exception.class)
            .skipLimit(5)
            .skip(FileProcessingException.class)
            .listener(new FileProcessingStepListener())
            .build();
    }
}
```

#### ACK/NACK Processing Job Configuration
```java
@Configuration
public class AckNackBatchConfig {
    
    @Bean
    public Job processAckNackFilesJob(JobRepository jobRepository,
                                     Step processAckNackFilesStep) {
        return new JobBuilder("processAckNackFilesJob", jobRepository)
            .start(processAckNackFilesStep)
            .build();
    }
    
    @Bean
    public Step processAckNackFilesStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ItemReader<Path> ackNackFileReader,
                                       ItemProcessor<Path, AckNackRecord> ackNackFileProcessor,
                                       ItemWriter<AckNackRecord> ackNackFileWriter) {
        return new StepBuilder("processAckNackFilesStep", jobRepository)
            .<Path, AckNackRecord>chunk(10, transactionManager)
            .reader(ackNackFileReader)
            .processor(ackNackFileProcessor)
            .writer(ackNackFileWriter)
            .faultTolerant()
            .retryLimit(3)
            .retry(IOException.class)
            .skipLimit(10)
            .skip(FileFormatException.class)
            .build();
    }
}
```

### 3.2 Batch Components Design

#### Scalable File Item Reader
```java
@Component
@StepScope
public class ScalableFileItemReader implements ItemReader<FileItem> {
    
    @Value("#{jobParameters['inputPath']}")
    private String inputPath;
    
    @Value("#{jobParameters['tenantId']}")
    private String tenantId;
    
    private Iterator<Path> fileIterator;
    private boolean initialized = false;
    
    @Override
    public FileItem read() throws Exception {
        if (!initialized) {
            initialize();
        }
        
        if (fileIterator != null && fileIterator.hasNext()) {
            Path nextFile = fileIterator.next();
            return new FileItem(nextFile, tenantId);
        }
        
        return null; // End of data
    }
    
    private void initialize() throws IOException {
        Path inputDir = Paths.get(inputPath);
        
        if (Files.exists(inputDir)) {
            try (Stream<Path> fileStream = Files.walk(inputDir)) {
                fileIterator = fileStream
                    .filter(Files::isRegularFile)
                    .filter(this::isValidFileForProcessing)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a)
                                   .compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .iterator();
            }
        }
        
        initialized = true;
    }
    
    private boolean isValidFileForProcessing(Path file) {
        String fileName = file.getFileName().toString();
        return !fileName.startsWith(".") && 
               !fileName.endsWith(".tmp") &&
               !fileName.endsWith(".processing");
    }
}
```

#### Parallel File Item Processor
```java
@Component
@StepScope
public class ParallelFileItemProcessor implements ItemProcessor<FileItem, ProcessedFile> {
    
    @Autowired
    private FileValidationService validationService;
    
    @Autowired
    private SchemaValidationService schemaValidationService;
    
    @Autowired
    private FileProcessingService fileProcessingService;
    
    @Override
    public ProcessedFile process(FileItem fileItem) throws Exception {
        
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setOriginalPath(fileItem.getPath());
        processedFile.setTenantId(fileItem.getTenantId());
        processedFile.setProcessingStartTime(LocalDateTime.now());
        
        try {
            // Step 1: Basic file validation
            ValidationResult basicValidation = validationService.validateFile(fileItem);
            if (!basicValidation.isValid()) {
                throw new FileValidationException("Basic validation failed: " + basicValidation.getErrorMessage());
            }
            
            // Step 2: Determine file type
            FileType fileType = FileType.detectFromContent(
                Files.readString(fileItem.getPath()), 
                fileItem.getPath().getFileName().toString()
            );
            processedFile.setFileType(fileType);
            
            // Step 3: Schema validation (if required)
            if (fileType.requiresSchemaValidation()) {
                SchemaValidationResult schemaResult = schemaValidationService.validateAgainstSchema(
                    fileItem, fileType);
                processedFile.setSchemaValidationResult(schemaResult);
                
                if (!schemaResult.isValid()) {
                    throw new SchemaValidationException("Schema validation failed: " + schemaResult.getErrors());
                }
            }
            
            // Step 4: Business logic processing
            ProcessingResult businessResult = fileProcessingService.processFile(fileItem, fileType);
            processedFile.setBusinessProcessingResult(businessResult);
            
            // Step 5: Mark as successful
            processedFile.setStatus(ProcessingStatus.COMPLETED);
            processedFile.setProcessingEndTime(LocalDateTime.now());
            
            return processedFile;
            
        } catch (Exception e) {
            processedFile.setStatus(ProcessingStatus.FAILED);
            processedFile.setErrorMessage(e.getMessage());
            processedFile.setProcessingEndTime(LocalDateTime.now());
            
            logger.error("File processing failed for {}: {}", fileItem.getPath(), e.getMessage());
            return processedFile; // Return failed item for error handling
        }
    }
}
```

#### Batch File Item Writer
```java
@Component
@StepScope
public class BatchFileItemWriter implements ItemWriter<ProcessedFile> {
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private AckNackService ackNackService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Override
    public void write(Chunk<? extends ProcessedFile> chunk) throws Exception {
        
        for (ProcessedFile processedFile : chunk) {
            try {
                // Create file transfer record
                FileTransferRecord record = createFileTransferRecord(processedFile);
                record = fileTransferRepository.save(record);
                
                // Update metrics
                metricsService.recordFileProcessing(
                    processedFile.getTenantId(), 
                    processedFile.getStatus().name(),
                    Duration.between(processedFile.getProcessingStartTime(), 
                                   processedFile.getProcessingEndTime())
                );
                
                // Generate ACK/NACK if needed
                if (processedFile.getStatus() == ProcessingStatus.COMPLETED && 
                    record.getDirection() == TransferDirection.INBOUND) {
                    ackNackService.generateAckForInboundFile(record.getId());
                } else if (processedFile.getStatus() == ProcessingStatus.FAILED && 
                          record.getDirection() == TransferDirection.INBOUND) {
                    ackNackService.generateNackForInboundFile(
                        record.getId(), 
                        "PROCESSING_FAILED", 
                        processedFile.getErrorMessage()
                    );
                }
                
                // Move processed file
                moveProcessedFile(processedFile);
                
            } catch (Exception e) {
                logger.error("Failed to write processed file {}: {}", 
                           processedFile.getOriginalPath(), e.getMessage());
                throw e; // Re-throw to trigger batch retry logic
            }
        }
    }
    
    private FileTransferRecord createFileTransferRecord(ProcessedFile processedFile) {
        FileTransferRecord record = new FileTransferRecord();
        record.setFileName(processedFile.getOriginalPath().getFileName().toString());
        record.setTenantId(processedFile.getTenantId());
        record.setServiceType(processedFile.getServiceType());
        record.setSubServiceType(processedFile.getSubServiceType());
        record.setSourcePath(processedFile.getOriginalPath().toString());
        record.setTargetPath(processedFile.getTargetPath().toString());
        record.setDirection(processedFile.getDirection());
        record.setFileType(processedFile.getFileType());
        record.setStatus(mapProcessingStatusToTransferStatus(processedFile.getStatus()));
        record.setFileSize(processedFile.getFileSize());
        record.setChecksum(processedFile.getChecksum());
        record.setErrorMessage(processedFile.getErrorMessage());
        record.setProcessingStartTime(processedFile.getProcessingStartTime());
        record.setProcessingEndTime(processedFile.getProcessingEndTime());
        
        return record;
    }
}
```

## 4. Scheduled Job Implementation

### 4.1 File Monitoring Service

#### Core File Monitoring Logic
```java
@Service
public class FileMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Autowired
    private FileTransferService fileTransferService;
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("fileProcessingJob")
    private Job fileProcessingJob;
    
    @Scheduled(fixedDelayString = "${file-transfer.poll-interval-seconds:30}000")
    public void monitorDirectories() {
        if (!fileTransferConfig.isEnabled()) {
            return;
        }
        
        List<String> activeTenantIds = serviceConfigurationRepository.findAllActiveTenantIds();
        
        for (String tenantId : activeTenantIds) {
            try {
                monitorTenantDirectories(tenantId);
            } catch (Exception e) {
                logger.error("Error monitoring directories for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }
    
    private void monitorTenantDirectories(String tenantId) {
        List<ServiceConfiguration> activeServices = 
            serviceConfigurationRepository.findByTenantIdAndActive(tenantId, true);
        
        for (ServiceConfiguration service : activeServices) {
            try {
                monitorServiceDirectory(service);
            } catch (Exception e) {
                logger.error("Error monitoring service {} for tenant {}: {}", 
                           service.getServiceName(), tenantId, e.getMessage());
            }
        }
    }
    
    private void monitorServiceDirectory(ServiceConfiguration service) throws Exception {
        Path sourceDir = Paths.get(service.getSourcePath());
        
        if (!Files.exists(sourceDir)) {
            logger.warn("Source directory does not exist: {}", sourceDir);
            return;
        }
        
        try (Stream<Path> files = Files.walk(sourceDir, 1)) {
            List<Path> newFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> isNewFile(path, service))
                .collect(Collectors.toList());
            
            for (Path file : newFiles) {
                processNewFile(file, service);
            }
        }
    }
    
    private void processNewFile(Path file, ServiceConfiguration service) {
        try {
            // Determine file type (SOT, Data, EOT)
            FileProcessingType type = determineFileType(file, service);
            
            switch (type) {
                case SOT:
                    processSotFile(file, service);
                    break;
                case DATA:
                    processDataFile(file, service);
                    break;
                case EOT:
                    processEotFile(file, service);
                    break;
                default:
                    logger.warn("Unknown file type for {}", file);
            }
            
        } catch (Exception e) {
            logger.error("Error processing new file {}: {}", file, e.getMessage());
            moveToErrorDirectory(file, e.getMessage());
        }
    }
}
```

### 4.2 ACK/NACK Scheduled Processing

#### ACK/NACK Service with Scheduling
```java
@Service
public class AckNackService {
    
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.poll-interval-seconds:60}000")
    public void processIncomingAckNackFiles() {
        try {
            Path incomingDir = Paths.get(incomingAckNackPath);
            if (!Files.exists(incomingDir)) {
                Files.createDirectories(incomingDir);
                return;
            }
            
            try (Stream<Path> files = Files.walk(incomingDir)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> isAckNackFile(path.getFileName().toString()))
                     .forEach(this::processIncomingAckNackFile);
            }
            
        } catch (Exception e) {
            logger.error("Error processing incoming ACK/NACK files: {}", e.getMessage());
        }
    }
    
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.generation-interval-seconds:300}000")
    public void generatePendingAckFiles() {
        if (!autoGenerateAckNack) {
            return;
        }
        
        try {
            List<FileTransferRecord> completedInboundTransfers = 
                fileTransferRepository.findCompletedInboundTransfersWithoutAck();
            
            for (FileTransferRecord transfer : completedInboundTransfers) {
                try {
                    generateAckForInboundFile(transfer.getId());
                } catch (Exception e) {
                    logger.error("Failed to auto-generate ACK for transfer {}: {}", 
                               transfer.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in auto ACK generation: {}", e.getMessage());
        }
    }
    
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.send-interval-seconds:120}000")
    public void sendPendingAckNackFiles() {
        try {
            List<AckNackRecord> pendingRecords = ackNackRepository.findByStatus(AckNackStatus.GENERATED);
            
            for (AckNackRecord record : pendingRecords) {
                try {
                    sendAckNackFile(record);
                    record.setStatus(AckNackStatus.SENT);
                    record.setSentAt(LocalDateTime.now());
                    ackNackRepository.save(record);
                    
                } catch (Exception e) {
                    logger.error("Failed to send {} file {}: {}", 
                               record.getType(), record.getAckNackFileName(), e.getMessage());
                    record.setStatus(AckNackStatus.FAILED);
                    record.setErrorMessage(e.getMessage());
                    ackNackRepository.save(record);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error sending pending ACK/NACK files: {}", e.getMessage());
        }
    }
    
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.cleanup-interval-seconds:3600}000")
    public void markExpiredRecords() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AckNackRecord> expiredRecords = 
                ackNackRepository.findExpiredRecords(AckNackStatus.SENT, now);
            
            for (AckNackRecord record : expiredRecords) {
                record.setStatus(AckNackStatus.EXPIRED);
                ackNackRepository.save(record);
                logger.warn("Marked {} file as expired: {}", record.getType(), record.getAckNackFileName());
            }
            
        } catch (Exception e) {
            logger.error("Error marking expired ACK/NACK records: {}", e.getMessage());
        }
    }
}
```

## 5. File Processing Algorithms

### 5.1 File Type Detection Algorithm

```java
public class FileTypeDetectionAlgorithm {
    
    public FileType detectFileType(Path filePath, String content, ServiceConfiguration service) {
        String fileName = filePath.getFileName().toString();
        
        // Step 1: Check against service-specific patterns
        if (service.getSotFilePattern() != null && 
            fileName.matches(service.getSotFilePattern())) {
            return FileType.SOT_MARKER;
        }
        
        if (service.getEotFilePattern() != null && 
            fileName.matches(service.getEotFilePattern())) {
            return FileType.EOT_MARKER;
        }
        
        if (service.getDataFilePattern() != null && 
            fileName.matches(service.getDataFilePattern())) {
            // Further detect data file type
            return detectDataFileType(content, fileName);
        }
        
        // Step 2: Content-based detection
        return FileType.detectFromContent(content, fileName);
    }
    
    private FileType detectDataFileType(String content, String fileName) {
        // Binary content detection
        if (containsBinaryContent(content)) {
            return FileType.BINARY_FILE;
        }
        
        // JSON detection
        String trimmed = content.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return FileType.JSON;
        }
        
        // XML detection
        if (trimmed.startsWith("<?xml") || 
            (trimmed.startsWith("<") && trimmed.contains(">"))) {
            return FileType.XML;
        }
        
        // CSV detection
        String firstLine = trimmed.split("\n")[0];
        if (firstLine.contains(",") && firstLine.split(",").length > 1) {
            return FileType.CSV;
        }
        
        // Default to COBOL flat file
        return FileType.COBOL_FLAT_FILE;
    }
}
```

### 5.2 EOT Validation Algorithm

```java
public class EotValidationAlgorithm {
    
    public EotValidationResult validateEotFile(Path eotFile, ServiceConfiguration service, 
                                             List<FileTransferRecord> dataFiles) {
        try {
            String content = Files.readString(eotFile);
            
            // Extract expected count from EOT file
            int expectedCount = extractExpectedCount(content, service);
            
            // Count actual data files for this transmission
            int actualCount = countDataFilesForTransmission(dataFiles, service);
            
            EotValidationResult result = new EotValidationResult();
            result.setExpectedCount(expectedCount);
            result.setActualCount(actualCount);
            result.setValid(expectedCount == actualCount);
            
            if (!result.isValid()) {
                result.setErrorMessage(String.format(
                    "Count mismatch: expected %d files, received %d files", 
                    expectedCount, actualCount));
            }
            
            return result;
            
        } catch (Exception e) {
            EotValidationResult result = new EotValidationResult();
            result.setValid(false);
            result.setErrorMessage("Failed to validate EOT file: " + e.getMessage());
            return result;
        }
    }
    
    private int extractExpectedCount(String content, ServiceConfiguration service) {
        // Parse EOT content based on service configuration
        // Support multiple EOT formats: pipe-delimited, JSON, XML, fixed-width
        
        if (service.getEotFilePattern().contains("JSON")) {
            return extractCountFromJson(content);
        } else if (service.getEotFilePattern().contains("XML")) {
            return extractCountFromXml(content);
        } else {
            return extractCountFromDelimited(content);
        }
    }
    
    private int extractCountFromDelimited(String content) {
        // Default pipe-delimited format: SERVICE|COUNT|TIMESTAMP
        String[] parts = content.trim().split("\\|");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new EotValidationException("Invalid count format in EOT file");
            }
        }
        throw new EotValidationException("Invalid EOT file format");
    }
}
```

## 6. Performance Optimization

### 6.1 Parallel Processing Configuration

#### Thread Pool Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileProcessing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "ackNackProcessingExecutor")
    public Executor ackNackProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("AckNackProcessing-");
        executor.initialize();
        return executor;
    }
}
```

#### Async Processing Implementation
```java
@Service
public class AsyncFileItemProcessor {
    
    @Async("fileProcessingExecutor")
    public CompletableFuture<ProcessingResult> processFileAsync(FileItem fileItem) {
        try {
            ProcessingResult result = processFile(fileItem);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<ProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    @Async("ackNackProcessingExecutor")
    public CompletableFuture<Void> generateAckAsync(Long fileTransferId) {
        try {
            ackNackService.generateAckForInboundFile(fileTransferId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
```

### 6.2 Database Optimization

#### Batch Insert Optimization
```java
@Repository
public class OptimizedFileTransferRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public void batchInsertFileTransfers(List<FileTransferRecord> records) {
        final int batchSize = 50;
        
        for (int i = 0; i < records.size(); i++) {
            entityManager.persist(records.get(i));
            
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        entityManager.flush();
        entityManager.clear();
    }
}
```

#### Connection Pool Optimization
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000
      pool-name: BatchApplicationCP
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
        generate_statistics: false
```

## 7. Error Handling and Recovery

### 7.1 Batch Job Error Handling

#### Job Execution Listener
```java
@Component
public class FileProcessingJobListener implements JobExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingJobListener.class);
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Starting batch job: {} with parameters: {}", 
                   jobExecution.getJobInstance().getJobName(),
                   jobExecution.getJobParameters());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();
        
        if (status == BatchStatus.COMPLETED) {
            logger.info("Batch job completed successfully: {}", 
                       jobExecution.getJobInstance().getJobName());
        } else if (status == BatchStatus.FAILED) {
            logger.error("Batch job failed: {} - {}", 
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getAllFailureExceptions());
            
            // Send alert for job failure
            alertService.sendJobFailureAlert(jobExecution);
        }
        
        // Update job metrics
        metricsService.recordJobExecution(jobExecution);
    }
}
```

#### Step Execution Listener
```java
@Component
public class FileProcessingStepListener implements StepExecutionListener {
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Starting step: {}", stepExecution.getStepName());
        stepExecution.getExecutionContext().put("startTime", System.currentTimeMillis());
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long startTime = stepExecution.getExecutionContext().getLong("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Step {} completed in {} ms. Read: {}, Written: {}, Skipped: {}",
                   stepExecution.getStepName(), duration,
                   stepExecution.getReadCount(),
                   stepExecution.getWriteCount(),
                   stepExecution.getSkipCount());
        
        if (stepExecution.getSkipCount() > 0) {
            logger.warn("Step {} had {} skipped items", 
                       stepExecution.getStepName(), stepExecution.getSkipCount());
        }
        
        return stepExecution.getExitStatus();
    }
}
```

### 7.2 Retry and Recovery Mechanisms

#### Retry Configuration
```java
@Configuration
public class RetryConfig {
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}

@Service
public class RetryableFileProcessingService {
    
    @Autowired
    private RetryTemplate retryTemplate;
    
    public ProcessingResult processWithRetry(FileItem fileItem) {
        return retryTemplate.execute(context -> {
            logger.info("Processing file (attempt {}): {}", 
                       context.getRetryCount() + 1, fileItem.getPath());
            return processFile(fileItem);
        }, context -> {
            logger.error("All retry attempts failed for file: {}", fileItem.getPath());
            return createFailedResult(fileItem, "Max retry attempts exceeded");
        });
    }
}
```

## 8. Monitoring and Metrics

### 8.1 Custom Metrics Implementation

#### File Processing Metrics
```java
@Component
public class BatchMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter filesProcessedCounter;
    private final Counter filesFailedCounter;
    private final Timer fileProcessingTimer;
    private final Gauge pendingFilesGauge;
    
    public BatchMetricsCollector(MeterRegistry meterRegistry,
                                FileTransferRecordRepository repository) {
        this.meterRegistry = meterRegistry;
        
        this.filesProcessedCounter = Counter.builder("batch.files.processed")
            .description("Number of files processed successfully")
            .register(meterRegistry);
            
        this.filesFailedCounter = Counter.builder("batch.files.failed")
            .description("Number of files that failed processing")
            .register(meterRegistry);
            
        this.fileProcessingTimer = Timer.builder("batch.file.processing.duration")
            .description("Time taken to process files")
            .register(meterRegistry);
            
        this.pendingFilesGauge = Gauge.builder("batch.files.pending")
            .description("Number of files pending processing")
            .register(meterRegistry, this, BatchMetricsCollector::getPendingFileCount);
    }
    
    public void recordFileProcessed(String tenantId, String serviceType, Duration duration) {
        filesProcessedCounter.increment(
            Tags.of("tenant", tenantId, "service", serviceType)
        );
        fileProcessingTimer.record(duration);
    }
    
    public void recordFileFailed(String tenantId, String serviceType, String errorType) {
        filesFailedCounter.increment(
            Tags.of("tenant", tenantId, "service", serviceType, "error", errorType)
        );
    }
    
    private double getPendingFileCount() {
        return repository.countByStatus(TransferStatus.PENDING);
    }
}
```

### 8.2 Health Checks

#### Batch Application Health Indicators
```java
@Component
public class BatchJobHealthIndicator implements HealthIndicator {
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @Override
    public Health health() {
        try {
            // Check recent job executions
            List<JobInstance> recentJobs = jobExplorer.findJobInstancesByJobName("fileProcessingJob", 0, 5);
            
            for (JobInstance jobInstance : recentJobs) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
                JobExecution latestExecution = executions.get(0);
                
                if (latestExecution.getStatus() == BatchStatus.FAILED &&
                    latestExecution.getEndTime().isAfter(LocalDateTime.now().minusHours(1))) {
                    return Health.down()
                        .withDetail("reason", "Recent job execution failed")
                        .withDetail("jobName", jobInstance.getJobName())
                        .withDetail("failureTime", latestExecution.getEndTime())
                        .build();
                }
            }
            
            return Health.up()
                .withDetail("jobsRunning", countRunningJobs())
                .withDetail("lastSuccessfulRun", getLastSuccessfulRunTime())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("reason", "Error checking job status")
                .withException(e)
                .build();
        }
    }
}
```

## 9. Configuration and Deployment

### 9.1 Application Configuration

#### Batch-Specific Configuration
```java
@ConfigurationProperties(prefix = "batch")
@Component
public class BatchApplicationConfig {
    
    private JobConfig job = new JobConfig();
    private ProcessingConfig processing = new ProcessingConfig();
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    public static class JobConfig {
        private int chunkSize = 10;
        private int threadPoolSize = 5;
        private int maxRetryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(30);
        
        // Getters and setters
    }
    
    public static class ProcessingConfig {
        private long maxFileSizeBytes = 100 * 1024 * 1024; // 100MB
        private Duration fileProcessingTimeout = Duration.ofMinutes(30);
        private boolean archiveProcessedFiles = true;
        private int archiveRetentionDays = 30;
        
        // Getters and setters
    }
    
    public static class MonitoringConfig {
        private boolean enabled = true;
        private Duration healthCheckInterval = Duration.ofMinutes(5);
        private int maxPendingThreshold = 1000;
        private int maxFailedThreshold = 100;
        
        // Getters and setters
    }
}
```

### 9.2 Scheduler Configuration

#### Custom Scheduler Configuration
```java
@Configuration
@EnableScheduling
public class SchedulerConfig {
    
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("batch-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
    
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(5, 
            new ThreadFactoryBuilder()
                .setNameFormat("batch-scheduled-%d")
                .setDaemon(true)
                .build());
    }
}
```

## 10. Testing Strategy

### 10.1 Batch Job Testing

#### Integration Testing for Batch Jobs
```java
@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "file-transfer.enabled=false"
})
class FileProcessingJobIntegrationTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    private FileTransferRecordRepository repository;
    
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }
    
    @Test
    void testFileProcessingJob() throws Exception {
        // Given
        setupTestFiles();
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputPath", "/test/input")
            .addString("tenantId", "test-tenant")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        List<FileTransferRecord> processedRecords = repository.findByTenantId("test-tenant");
        assertFalse(processedRecords.isEmpty());
        
        // Verify ACK generation for inbound files
        long ackCount = processedRecords.stream()
            .filter(r -> r.getDirection() == TransferDirection.INBOUND)
            .filter(r -> r.getStatus() == TransferStatus.COMPLETED)
            .count();
        
        // Should have corresponding ACK records
        assertEquals(ackCount, ackNackRepository.countByTenantIdAndTypeAndStatus(
            "test-tenant", AckNackType.ACK, AckNackStatus.GENERATED));
    }
}
```

### 10.2 Performance Testing

#### Load Testing Configuration
```java
@TestConfiguration
public class LoadTestConfig {
    
    @Bean
    @Primary
    public DataSource loadTestDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:loadtest;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        return new HikariDataSource(config);
    }
}

@SpringBootTest
@Import(LoadTestConfig.class)
class BatchPerformanceTest {
    
    @Test
    void testHighVolumeFileProcessing() throws Exception {
        // Create 10,000 test files
        List<FileItem> testFiles = createLargeFileSet(10000);
        
        long startTime = System.currentTimeMillis();
        
        // Process files in parallel
        CompletableFuture<Void> processingFuture = CompletableFuture.allOf(
            testFiles.stream()
                .map(file -> asyncFileProcessor.processFileAsync(file))
                .toArray(CompletableFuture[]::new)
        );
        
        processingFuture.get(10, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify performance requirements
        double filesPerSecond = (testFiles.size() * 1000.0) / duration;
        assertTrue(filesPerSecond > 100, "Processing rate should be > 100 files/second");
        
        // Verify all files were processed
        assertEquals(testFiles.size(), 
                    repository.countByTenantId("load-test-tenant"));
    }
}
```

## 11. Deployment and Operations

### 11.1 Docker Configuration

#### Optimized Dockerfile for Batch Application
```dockerfile
# Multi-stage build for batch application
FROM maven:3.9-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Download dependencies in separate layer for caching
RUN mvn dependency:go-offline -B

# Build application
RUN mvn clean package -DskipTests

# Runtime stage with minimal base image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install required tools for file operations
RUN apk add --no-cache curl bash

# Create application user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create directories for file processing
RUN mkdir -p /data/incoming /data/processed /data/error /data/ack-nack && \
    chown -R appuser:appgroup /data

# Copy application
COPY --from=builder /app/target/file-transfer-batch-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/heapdumps/"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 11.2 Kubernetes Deployment

#### StatefulSet for Batch Application
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: file-transfer-batch
spec:
  serviceName: file-transfer-batch
  replicas: 1
  selector:
    matchLabels:
      app: file-transfer-batch
  template:
    metadata:
      labels:
        app: file-transfer-batch
    spec:
      containers:
      - name: batch-app
        image: file-transfer-batch:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes,production,ack-nack"
        - name: AZURE_SQL_MI_SERVER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: server
        volumeMounts:
        - name: file-storage
          mountPath: /data
        - name: config-volume
          mountPath: /app/config
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 120
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 15
  volumeClaimTemplates:
  - metadata:
      name: file-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 100Gi
```

## 12. Security Implementation

### 12.1 File System Security

#### Secure File Operations
```java
@Service
public class SecureFileOperations {
    
    @Value("${file-transfer.allowed-base-paths}")
    private List<String> allowedBasePaths;
    
    public boolean isPathAllowed(String path, String tenantId) {
        Path normalizedPath = Paths.get(path).normalize();
        
        // Check if path is within allowed base paths
        boolean withinAllowedPaths = allowedBasePaths.stream()
            .anyMatch(basePath -> normalizedPath.startsWith(Paths.get(basePath)));
        
        // Check if path is within tenant's directory
        boolean withinTenantPath = normalizedPath.toString().contains("/" + tenantId + "/");
        
        // Prevent directory traversal
        boolean noTraversal = !normalizedPath.toString().contains("..");
        
        return withinAllowedPaths && withinTenantPath && noTraversal;
    }
    
    public void secureFileCopy(Path source, Path target, String tenantId) throws IOException {
        if (!isPathAllowed(source.toString(), tenantId) || 
            !isPathAllowed(target.toString(), tenantId)) {
            throw new SecurityException("Path not allowed for tenant: " + tenantId);
        }
        
        // Ensure target directory exists and has correct permissions
        Files.createDirectories(target.getParent());
        
        // Copy with atomic operation
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, 
                  StandardCopyOption.ATOMIC_MOVE);
        
        // Set appropriate file permissions
        Set<PosixFilePermission> permissions = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ
        );
        
        try {
            Files.setPosixFilePermissions(target, permissions);
        } catch (UnsupportedOperationException e) {
            // Windows doesn't support POSIX permissions
            logger.debug("POSIX permissions not supported on this platform");
        }
    }
}
```

## 13. Data Processing Patterns

### 13.1 File Processing State Machine

```java
public enum ProcessingState {
    DETECTED,
    QUEUED,
    VALIDATING,
    PROCESSING,
    COMPLETED,
    FAILED,
    ARCHIVED;
    
    public boolean canTransitionTo(ProcessingState newState) {
        switch (this) {
            case DETECTED:
                return newState == QUEUED || newState == FAILED;
            case QUEUED:
                return newState == VALIDATING || newState == FAILED;
            case VALIDATING:
                return newState == PROCESSING || newState == FAILED;
            case PROCESSING:
                return newState == COMPLETED || newState == FAILED;
            case COMPLETED:
                return newState == ARCHIVED;
            case FAILED:
                return newState == QUEUED || newState == ARCHIVED; // Allow retry
            case ARCHIVED:
                return false; // Terminal state
            default:
                return false;
        }
    }
}

@Service
public class FileProcessingStateMachine {
    
    public void transitionState(FileTransferRecord record, ProcessingState newState) {
        ProcessingState currentState = ProcessingState.valueOf(record.getStatus().name());
        
        if (!currentState.canTransitionTo(newState)) {
            throw new IllegalStateTransitionException(
                String.format("Cannot transition from %s to %s", currentState, newState));
        }
        
        record.setStatus(TransferStatus.valueOf(newState.name()));
        fileTransferRepository.save(record);
        
        // Emit state transition event
        eventPublisher.publishEvent(new StateTransitionEvent(record, currentState, newState));
    }
}
```

### 13.2 Tenant-Based Partitioning

#### Tenant Partitioner Implementation
```java
@Component
public class TenantBasedPartitioner implements Partitioner {
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitionMap = new HashMap<>();
        
        List<String> activeTenantIds = serviceConfigurationRepository.findAllActiveTenantIds();
        
        for (int i = 0; i < activeTenantIds.size(); i++) {
            String tenantId = activeTenantIds.get(i);
            
            ExecutionContext context = new ExecutionContext();
            context.putString("tenantId", tenantId);
            context.putString("partitionName", "tenant-" + tenantId);
            
            partitionMap.put("partition" + i, context);
        }
        
        return partitionMap;
    }
}

@Configuration
public class PartitionedBatchConfig {
    
    @Bean
    public Step partitionedFileProcessingStep(JobRepository jobRepository,
                                            PlatformTransactionManager transactionManager,
                                            TenantBasedPartitioner partitioner,
                                            Step fileProcessingWorkerStep) {
        return new StepBuilder("partitionedFileProcessingStep", jobRepository)
            .partitioner("fileProcessingWorkerStep", partitioner)
            .step(fileProcessingWorkerStep)
            .gridSize(10)
            .taskExecutor(taskExecutor())
            .build();
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-partition-");
        executor.initialize();
        return executor;
    }
}
```

## 14. Backup and Recovery

### 14.1 Backup Service Implementation

#### Automated Backup Processing
```java
@Service
public class BatchBackupService {
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performDailyBackup() {
        try {
            logger.info("Starting daily backup process");
            
            // Backup database
            BackupResult dbBackup = backupDatabase();
            
            // Backup file storage
            BackupResult fileBackup = backupFileStorage();
            
            // Backup configuration
            BackupResult configBackup = backupConfiguration();
            
            // Consolidate backup results
            BackupSummary summary = new BackupSummary(dbBackup, fileBackup, configBackup);
            
            if (summary.isSuccessful()) {
                logger.info("Daily backup completed successfully");
                notificationService.sendBackupSuccessNotification(summary);
            } else {
                logger.error("Daily backup completed with errors: {}", summary.getErrors());
                alertService.sendBackupFailureAlert(summary);
            }
            
        } catch (Exception e) {
            logger.error("Daily backup failed: {}", e.getMessage());
            alertService.sendBackupFailureAlert(e);
        }
    }
    
    private BackupResult backupDatabase() {
        try {
            String backupPath = generateBackupPath("database");
            
            // Execute database backup command
            ProcessBuilder pb = new ProcessBuilder(
                "mysqldump", 
                "--single-transaction",
                "--routines",
                "--triggers",
                "--all-databases",
                "--result-file=" + backupPath
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return BackupResult.success(backupPath);
            } else {
                return BackupResult.failure("Database backup failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            return BackupResult.failure("Database backup exception: " + e.getMessage());
        }
    }
    
    private BackupResult backupFileStorage() {
        try {
            String backupPath = generateBackupPath("files");
            Path sourceDir = Paths.get("/data");
            Path targetDir = Paths.get(backupPath);
            
            // Use rsync for efficient file backup
            ProcessBuilder pb = new ProcessBuilder(
                "rsync", "-av", "--delete", 
                sourceDir.toString() + "/", 
                targetDir.toString() + "/"
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return BackupResult.success(backupPath);
            } else {
                return BackupResult.failure("File backup failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            return BackupResult.failure("File backup exception: " + e.getMessage());
        }
    }
}
```

## 15. Monitoring and Alerting

### 15.1 Batch Job Monitoring

#### Job Execution Monitoring
```java
@Component
public class BatchJobMonitor {
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void handleJobExecution(JobExecutionEvent event) {
        JobExecution jobExecution = event.getJobExecution();
        String jobName = jobExecution.getJobInstance().getJobName();
        
        // Record job metrics
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("batch.job.duration")
            .tag("job", jobName)
            .tag("status", jobExecution.getStatus().name())
            .register(meterRegistry));
        
        // Record job status
        Counter.builder("batch.job.executions")
            .tag("job", jobName)
            .tag("status", jobExecution.getStatus().name())
            .register(meterRegistry)
            .increment();
        
        // Alert on job failures
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            alertService.sendJobFailureAlert(jobExecution);
        }
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorJobHealth() {
        try {
            // Check for stuck jobs
            List<JobExecution> runningJobs = jobExplorer.findRunningJobExecutions("fileProcessingJob");
            
            for (JobExecution execution : runningJobs) {
                Duration runningTime = Duration.between(
                    execution.getStartTime().toInstant(), 
                    Instant.now()
                );
                
                if (runningTime.toHours() > 2) { // Jobs shouldn't run more than 2 hours
                    logger.warn("Long-running job detected: {} running for {} hours", 
                               execution.getId(), runningTime.toHours());
                    alertService.sendLongRunningJobAlert(execution);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error monitoring job health: {}", e.getMessage());
        }
    }
}
```

### 15.2 File Processing Metrics

#### Custom Metrics Collection
```java
@Component
public class FileProcessingMetrics {
    
    private final Counter filesProcessedCounter;
    private final Counter ackNackGeneratedCounter;
    private final Timer fileProcessingTimer;
    private final DistributionSummary fileSizeDistribution;
    
    public FileProcessingMetrics(MeterRegistry meterRegistry) {
        this.filesProcessedCounter = Counter.builder("batch.files.processed.total")
            .description("Total files processed")
            .register(meterRegistry);
            
        this.ackNackGeneratedCounter = Counter.builder("batch.ack.nack.generated.total")
            .description("Total ACK/NACK files generated")
            .register(meterRegistry);
            
        this.fileProcessingTimer = Timer.builder("batch.file.processing.time")
            .description("File processing duration")
            .register(meterRegistry);
            
        this.fileSizeDistribution = DistributionSummary.builder("batch.file.size.bytes")
            .description("Distribution of file sizes processed")
            .register(meterRegistry);
    }
    
    public void recordFileProcessed(String tenantId, String serviceType, 
                                   FileType fileType, Duration processingTime, long fileSize) {
        filesProcessedCounter.increment(
            Tags.of("tenant", tenantId, "service", serviceType, "type", fileType.name())
        );
        fileProcessingTimer.record(processingTime);
        fileSizeDistribution.record(fileSize);
    }
    
    public void recordAckNackGenerated(String tenantId, AckNackType type, AckNackStatus status) {
        ackNackGeneratedCounter.increment(
            Tags.of("tenant", tenantId, "type", type.name(), "status", status.name())
        );
    }
}
```

## 16. Configuration Management

### 16.1 Environment-Specific Configuration

#### Production Configuration
```yaml
# application-production.yml
spring:
  batch:
    job:
      enabled: false # Prevent auto-start of jobs
    jdbc:
      initialize-schema: never # Use Flyway for production
      
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000

file-transfer:
  enabled: true
  poll-interval-seconds: 30
  
  ack-nack:
    enabled: true
    base-path: /mnt/ack-nack
    incoming-path: /mnt/incoming/ack-nack
    auto-generate: true
    timeout-hours: 24
    
    batch:
      chunk-size: 20
      thread-pool-size: 10
      max-retry-attempts: 3

batch:
  job:
    chunk-size: 50
    thread-pool-size: 10
  processing:
    max-file-size-bytes: 1073741824 # 1GB
    file-processing-timeout: PT30M
    archive-processed-files: true
    archive-retention-days: 90

logging:
  level:
    com.filetransfer.batch: INFO
    org.springframework.batch: WARN
    org.springframework.scheduling: INFO
  file:
    name: /app/logs/batch-application.log
    max-size: 100MB
    max-history: 30
```

### 16.2 Dynamic Configuration

#### Configuration Refresh Mechanism
```java
@Component
@RefreshScope
public class DynamicBatchConfig {
    
    @Value("${file-transfer.poll-interval-seconds:30}")
    private int pollIntervalSeconds;
    
    @Value("${batch.job.chunk-size:10}")
    private int chunkSize;
    
    @EventListener
    public void handleConfigurationChangeEvent(ConfigurationChangeEvent event) {
        if (event.getPropertyName().startsWith("file-transfer.") ||
            event.getPropertyName().startsWith("batch.")) {
            
            logger.info("Configuration changed: {} = {}", 
                       event.getPropertyName(), event.getNewValue());
            
            // Refresh configuration
            refreshConfiguration();
            
            // Restart schedulers if needed
            if (event.getPropertyName().equals("file-transfer.poll-interval-seconds")) {
                restartFileMonitoringScheduler();
            }
        }
    }
    
    @RefreshScope
    public void refreshConfiguration() {
        // Reload configuration from external sources
        configurationService.reloadConfiguration();
    }
}
```

## 17. Testing Framework

### 17.1 Batch Testing Utilities

#### Batch Test Configuration
```java
@TestConfiguration
public class BatchTestConfig {
    
    @Bean
    @Primary
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
    
    @Bean
    public JobRepositoryTestUtils jobRepositoryTestUtils(JobRepository jobRepository) {
        return new JobRepositoryTestUtils(jobRepository);
    }
}

@SpringBatchTest
@SpringBootTest
@Import(BatchTestConfig.class)
class BaseBatchIntegrationTest {
    
    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    protected JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    protected TestEntityManager testEntityManager;
    
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }
    
    protected JobParameters createJobParameters(String tenantId, String inputPath) {
        return new JobParametersBuilder()
            .addString("tenantId", tenantId)
            .addString("inputPath", inputPath)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
    }
    
    protected void createTestFile(String path, String content) throws IOException {
        Path filePath = Paths.get(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content.getBytes());
    }
}
```

### 17.2 Performance Testing Framework

#### Load Testing for Batch Operations
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "logging.level.org.springframework.batch=WARN"
})
class BatchLoadTest extends BaseBatchIntegrationTest {
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testHighVolumeFileProcessing() throws Exception {
        // Setup
        int fileCount = 10000;
        String testTenant = "load-test-tenant";
        String inputPath = "/tmp/load-test/input";
        
        // Create test files
        createLargeFileSet(inputPath, fileCount);
        
        // Execute batch job
        JobParameters jobParameters = createJobParameters(testTenant, inputPath);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Verify results
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(fileCount, stepExecution.getReadCount());
        assertEquals(fileCount, stepExecution.getWriteCount());
        assertTrue(stepExecution.getSkipCount() < fileCount * 0.01); // < 1% skip rate
        
        // Verify performance
        long executionTime = jobExecution.getEndTime().getTime() - 
                            jobExecution.getStartTime().getTime();
        double filesPerSecond = (fileCount * 1000.0) / executionTime;
        
        assertTrue(filesPerSecond > 100, 
                  "Processing rate should be > 100 files/second, actual: " + filesPerSecond);
    }
    
    private void createLargeFileSet(String basePath, int count) throws IOException {
        Path baseDir = Paths.get(basePath);
        Files.createDirectories(baseDir);
        
        for (int i = 0; i < count; i++) {
            String fileName = String.format("test-file-%06d.dat", i);
            String content = generateTestFileContent(i);
            createTestFile(baseDir.resolve(fileName).toString(), content);
        }
    }
}
```

## 18. Error Recovery and Resilience

### 18.1 Circuit Breaker Implementation

#### External Service Circuit Breaker
```java
@Component
public class ExternalServiceClient {
    
    @CircuitBreaker(name = "fileStorageService", fallbackMethod = "fallbackFileOperation")
    @Retry(name = "fileStorageService")
    @TimeLimiter(name = "fileStorageService")
    public CompletableFuture<String> performFileOperation(String operation, String path) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate external file storage operation
            return externalFileStorageService.performOperation(operation, path);
        });
    }
    
    public CompletableFuture<String> fallbackFileOperation(String operation, String path, Exception ex) {
        logger.warn("File operation fallback triggered for {}: {}", path, ex.getMessage());
        
        // Implement fallback logic
        return CompletableFuture.completedFuture("FALLBACK_RESULT");
    }
}
```

### 18.2 Dead Letter Queue Pattern

#### Failed File Handling
```java
@Service
public class FailedFileHandler {
    
    @Value("${file-transfer.dead-letter-path:/data/dead-letter}")
    private String deadLetterPath;
    
    @Value("${file-transfer.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    public void handleFailedFile(FileItem fileItem, Exception exception) {
        try {
            FailedFileRecord failedRecord = new FailedFileRecord();
            failedRecord.setOriginalPath(fileItem.getPath().toString());
            failedRecord.setTenantId(fileItem.getTenantId());
            failedRecord.setFailureReason(exception.getMessage());
            failedRecord.setFailureTime(LocalDateTime.now());
            failedRecord.setRetryCount(0);
            
            // Move file to dead letter directory
            Path deadLetterDir = Paths.get(deadLetterPath, fileItem.getTenantId());
            Files.createDirectories(deadLetterDir);
            
            Path targetPath = deadLetterDir.resolve(fileItem.getPath().getFileName());
            Files.move(fileItem.getPath(), targetPath);
            
            failedRecord.setDeadLetterPath(targetPath.toString());
            
            // Save failure record
            failedFileRepository.save(failedRecord);
            
            logger.error("Moved failed file to dead letter queue: {}", targetPath);
            
        } catch (Exception e) {
            logger.error("Failed to handle failed file {}: {}", fileItem.getPath(), e.getMessage());
        }
    }
    
    @Scheduled(fixedRate = 3600000) // Every hour
    public void retryFailedFiles() {
        List<FailedFileRecord> retryableFiles = failedFileRepository
            .findByRetryCountLessThanAndFailureTimeBefore(
                maxRetryAttempts, 
                LocalDateTime.now().minusHours(1)
            );
        
        for (FailedFileRecord failedRecord : retryableFiles) {
            try {
                retryFailedFile(failedRecord);
            } catch (Exception e) {
                logger.error("Retry failed for file {}: {}", 
                           failedRecord.getOriginalPath(), e.getMessage());
                
                failedRecord.incrementRetryCount();
                failedRecord.setLastRetryTime(LocalDateTime.now());
                failedFileRepository.save(failedRecord);
            }
        }
    }
}
```

This Low Level Design document provides comprehensive implementation details for the Batch Application component, covering all aspects from batch job configuration to performance optimization and error handling strategies.