package com.filetransfer.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive backup service for batch application
 * Handles batch job state, configuration, and data backup
 */
@Service
public class BatchBackupService {

    private static final Logger logger = LoggerFactory.getLogger(BatchBackupService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobRepository jobRepository;

    @Value("${batch.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${batch.backup.location:/var/backups/batch}")
    private String backupLocation;

    @Value("${batch.backup.retention.days:30}")
    private int retentionDays;

    @Value("${batch.backup.compression:true}")
    private boolean compressionEnabled;

    @Value("${batch.backup.encryption:true}")
    private boolean encryptionEnabled;

    @Value("${batch.data.path:/app/batch-data}")
    private String batchDataPath;

    @Value("${batch.config.path:/app/config}")
    private String configPath;

    @Value("${batch.backup.remote.enabled:false}")
    private boolean remoteBackupEnabled;

    private final DateTimeFormatter backupTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Scheduled batch backup - runs daily at 3 AM
     */
    @Scheduled(cron = "${batch.backup.schedule:0 0 3 * * *}")
    public void performScheduledBatchBackup() {
        if (!backupEnabled) {
            logger.debug("Batch backup is disabled");
            return;
        }

        logger.info("Starting scheduled batch backup");
        
        try {
            BatchBackupRequest request = BatchBackupRequest.builder()
                .type(BatchBackupType.FULL)
                .includeJobRepository(true)
                .includeJobData(true)
                .includeConfiguration(true)
                .includeBatchFiles(true)
                .compression(compressionEnabled)
                .encryption(encryptionEnabled)
                .remoteSync(remoteBackupEnabled)
                .build();

            BatchBackupResult result = performBatchBackup(request).join();
            
            if (result.isSuccess()) {
                logger.info("Scheduled batch backup completed successfully: {}", result.getBackupId());
                notifyBackupSuccess(result);
            } else {
                logger.error("Scheduled batch backup failed: {}", result.getErrorMessage());
                notifyBackupFailure(result);
            }
            
        } catch (Exception e) {
            logger.error("Scheduled batch backup failed with exception", e);
            notifyBackupException(e);
        }
    }

    /**
     * Perform batch backup asynchronously
     */
    @Async("batchBackupExecutor")
    public CompletableFuture<BatchBackupResult> performBatchBackup(BatchBackupRequest request) {
        String backupId = generateBackupId(request.getType());
        logger.info("Starting batch backup: {} of type: {}", backupId, request.getType());
        
        long startTime = System.currentTimeMillis();
        BatchBackupResult.Builder resultBuilder = BatchBackupResult.builder()
            .backupId(backupId)
            .type(request.getType())
            .startTime(LocalDateTime.now());

        try {
            // Create backup directory
            Path backupDir = createBackupDirectory(backupId);
            
            // Backup Spring Batch job repository
            if (request.isIncludeJobRepository()) {
                JobRepositoryBackupResult jobRepoResult = backupJobRepository(backupDir, request);
                resultBuilder.jobRepositoryBackup(jobRepoResult);
                
                if (!jobRepoResult.isSuccess()) {
                    throw new BatchBackupException("Job repository backup failed: " + jobRepoResult.getErrorMessage());
                }
            }

            // Backup batch job data and files
            if (request.isIncludeJobData()) {
                JobDataBackupResult jobDataResult = backupJobData(backupDir, request);
                resultBuilder.jobDataBackup(jobDataResult);
                
                if (!jobDataResult.isSuccess()) {
                    throw new BatchBackupException("Job data backup failed: " + jobDataResult.getErrorMessage());
                }
            }

            // Backup batch configuration
            if (request.isIncludeConfiguration()) {
                ConfigurationBackupResult configResult = backupConfiguration(backupDir, request);
                resultBuilder.configurationBackup(configResult);
                
                if (!configResult.isSuccess()) {
                    throw new BatchBackupException("Configuration backup failed: " + configResult.getErrorMessage());
                }
            }

            // Backup batch files and processing data
            if (request.isIncludeBatchFiles()) {
                BatchFilesBackupResult filesResult = backupBatchFiles(backupDir, request);
                resultBuilder.batchFilesBackup(filesResult);
                
                if (!filesResult.isSuccess()) {
                    throw new BatchBackupException("Batch files backup failed: " + filesResult.getErrorMessage());
                }
            }

            // Compress backup if requested
            if (request.isCompression()) {
                Path compressedBackup = compressBackup(backupDir);
                resultBuilder.compressedPath(compressedBackup.toString());
                
                // Remove uncompressed directory
                deleteDirectory(backupDir);
            }

            // Encrypt backup if requested
            if (request.isEncryption()) {
                Path encryptedBackup = encryptBackup(backupDir);
                resultBuilder.encryptedPath(encryptedBackup.toString());
            }

            // Sync to remote location if requested
            if (request.isRemoteSync() && remoteBackupEnabled) {
                RemoteSyncResult remoteSyncResult = syncToRemoteLocation(backupDir);
                resultBuilder.remoteSync(remoteSyncResult);
            }

            // Create backup metadata
            BatchBackupMetadata metadata = createBackupMetadata(backupId, request, backupDir);
            saveBackupMetadata(metadata);

            long duration = System.currentTimeMillis() - startTime;
            
            BatchBackupResult result = resultBuilder
                .success(true)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .backupPath(backupDir.toString())
                .size(calculateBackupSize(backupDir))
                .build();

            logger.info("Batch backup completed successfully: {} in {}ms", backupId, duration);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Batch backup failed: {}", backupId, e);
            
            long duration = System.currentTimeMillis() - startTime;
            
            BatchBackupResult result = resultBuilder
                .success(false)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .errorMessage(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Backup Spring Batch job repository
     */
    private JobRepositoryBackupResult backupJobRepository(Path backupDir, BatchBackupRequest request) {
        logger.info("Starting Spring Batch job repository backup");
        
        try {
            Path jobRepoDir = backupDir.resolve("job_repository");
            Files.createDirectories(jobRepoDir);

            // Export job instances
            Path jobInstancesFile = jobRepoDir.resolve("job_instances.json");
            List<JobInstance> jobInstances = getAllJobInstances();
            writeJobInstancesToFile(jobInstances, jobInstancesFile);

            // Export job executions
            Path jobExecutionsFile = jobRepoDir.resolve("job_executions.json");
            List<JobExecution> jobExecutions = getAllJobExecutions();
            writeJobExecutionsToFile(jobExecutions, jobExecutionsFile);

            // Export job parameters
            Path jobParametersFile = jobRepoDir.resolve("job_parameters.json");
            Map<String, Object> jobParameters = extractJobParameters(jobExecutions);
            writeJobParametersToFile(jobParameters, jobParametersFile);

            // Backup job repository database tables if using database repository
            if (isUsingDatabaseJobRepository()) {
                backupJobRepositoryTables(jobRepoDir);
            }

            long size = calculateDirectorySize(jobRepoDir);

            return JobRepositoryBackupResult.builder()
                .success(true)
                .backupPath(jobRepoDir.toString())
                .jobInstanceCount(jobInstances.size())
                .jobExecutionCount(jobExecutions.size())
                .size(size)
                .build();

        } catch (Exception e) {
            logger.error("Job repository backup failed", e);
            
            return JobRepositoryBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Backup batch job data and processing files
     */
    private JobDataBackupResult backupJobData(Path backupDir, BatchBackupRequest request) {
        logger.info("Starting batch job data backup");
        
        try {
            Path jobDataDir = backupDir.resolve("job_data");
            Files.createDirectories(jobDataDir);

            // Backup input files
            Path inputDir = Paths.get(batchDataPath, "input");
            if (Files.exists(inputDir)) {
                Path inputBackupDir = jobDataDir.resolve("input");
                copyDirectory(inputDir, inputBackupDir);
            }

            // Backup output files
            Path outputDir = Paths.get(batchDataPath, "output");
            if (Files.exists(outputDir)) {
                Path outputBackupDir = jobDataDir.resolve("output");
                copyDirectory(outputDir, outputBackupDir);
            }

            // Backup processing files
            Path processingDir = Paths.get(batchDataPath, "processing");
            if (Files.exists(processingDir)) {
                Path processingBackupDir = jobDataDir.resolve("processing");
                copyDirectory(processingDir, processingBackupDir);
            }

            // Backup error files
            Path errorDir = Paths.get(batchDataPath, "error");
            if (Files.exists(errorDir)) {
                Path errorBackupDir = jobDataDir.resolve("error");
                copyDirectory(errorDir, errorBackupDir);
            }

            // Backup archive files
            Path archiveDir = Paths.get(batchDataPath, "archive");
            if (Files.exists(archiveDir)) {
                Path archiveBackupDir = jobDataDir.resolve("archive");
                copyDirectory(archiveDir, archiveBackupDir);
            }

            // Count files and calculate size
            long fileCount = Files.walk(jobDataDir)
                .filter(Files::isRegularFile)
                .count();
            
            long size = calculateDirectorySize(jobDataDir);

            return JobDataBackupResult.builder()
                .success(true)
                .backupPath(jobDataDir.toString())
                .fileCount(fileCount)
                .size(size)
                .build();

        } catch (Exception e) {
            logger.error("Job data backup failed", e);
            
            return JobDataBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Backup batch configuration
     */
    private ConfigurationBackupResult backupConfiguration(Path backupDir, BatchBackupRequest request) {
        logger.info("Starting batch configuration backup");
        
        try {
            Path configBackupDir = backupDir.resolve("configuration");
            Files.createDirectories(configBackupDir);

            // Backup application properties
            Path configSource = Paths.get(configPath);
            if (Files.exists(configSource)) {
                copyDirectory(configSource, configBackupDir);
            }

            // Backup environment-specific configurations
            backupEnvironmentConfigurations(configBackupDir);

            // Backup Spring Batch job configurations
            backupBatchJobConfigurations(configBackupDir);

            // Backup security configurations
            backupSecurityConfigurations(configBackupDir);

            long size = calculateDirectorySize(configBackupDir);

            return ConfigurationBackupResult.builder()
                .success(true)
                .backupPath(configBackupDir.toString())
                .size(size)
                .build();

        } catch (Exception e) {
            logger.error("Configuration backup failed", e);
            
            return ConfigurationBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Backup batch files and processing data
     */
    private BatchFilesBackupResult backupBatchFiles(Path backupDir, BatchBackupRequest request) {
        logger.info("Starting batch files backup");
        
        try {
            Path batchFilesDir = backupDir.resolve("batch_files");
            Files.createDirectories(batchFilesDir);

            // Backup currently processing files
            backupCurrentlyProcessingFiles(batchFilesDir);

            // Backup batch job metadata
            backupBatchJobMetadata(batchFilesDir);

            // Backup batch execution statistics
            backupBatchExecutionStatistics(batchFilesDir);

            // Backup batch monitoring data
            backupBatchMonitoringData(batchFilesDir);

            long size = calculateDirectorySize(batchFilesDir);

            return BatchFilesBackupResult.builder()
                .success(true)
                .backupPath(batchFilesDir.toString())
                .size(size)
                .build();

        } catch (Exception e) {
            logger.error("Batch files backup failed", e);
            
            return BatchFilesBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Restore batch backup
     */
    @Async("batchBackupExecutor")
    public CompletableFuture<BatchRestoreResult> restoreBatchBackup(BatchRestoreRequest request) {
        String restoreId = generateRestoreId();
        logger.info("Starting batch restore: {} from backup: {}", restoreId, request.getBackupId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate backup exists
            BatchBackupMetadata metadata = getBatchBackupMetadata(request.getBackupId());
            if (metadata == null) {
                throw new BatchRestoreException("Batch backup not found: " + request.getBackupId());
            }

            // Create restore point before starting
            if (request.isCreateRestorePoint()) {
                BatchBackupRequest restorePointRequest = BatchBackupRequest.builder()
                    .type(BatchBackupType.RESTORE_POINT)
                    .includeJobRepository(true)
                    .includeJobData(true)
                    .includeConfiguration(true)
                    .build();
                
                performBatchBackup(restorePointRequest).join();
            }

            Path backupPath = Paths.get(metadata.getBackupPath());
            
            // Decrypt backup if encrypted
            if (metadata.isEncrypted()) {
                backupPath = decryptBackup(backupPath);
            }

            // Decompress backup if compressed
            if (metadata.isCompressed()) {
                backupPath = decompressBackup(backupPath);
            }

            BatchRestoreResult.Builder resultBuilder = BatchRestoreResult.builder()
                .restoreId(restoreId)
                .backupId(request.getBackupId())
                .startTime(LocalDateTime.now());

            // Restore job repository
            if (request.isRestoreJobRepository()) {
                JobRepositoryRestoreResult jobRepoResult = restoreJobRepository(backupPath, request);
                resultBuilder.jobRepositoryRestore(jobRepoResult);
                
                if (!jobRepoResult.isSuccess()) {
                    throw new BatchRestoreException("Job repository restore failed: " + jobRepoResult.getErrorMessage());
                }
            }

            // Restore job data
            if (request.isRestoreJobData()) {
                JobDataRestoreResult jobDataResult = restoreJobData(backupPath, request);
                resultBuilder.jobDataRestore(jobDataResult);
                
                if (!jobDataResult.isSuccess()) {
                    throw new BatchRestoreException("Job data restore failed: " + jobDataResult.getErrorMessage());
                }
            }

            // Restore configuration
            if (request.isRestoreConfiguration()) {
                ConfigurationRestoreResult configResult = restoreConfiguration(backupPath, request);
                resultBuilder.configurationRestore(configResult);
                
                if (!configResult.isSuccess()) {
                    throw new BatchRestoreException("Configuration restore failed: " + configResult.getErrorMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            BatchRestoreResult result = resultBuilder
                .success(true)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .build();

            logger.info("Batch restore completed successfully: {} in {}ms", restoreId, duration);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Batch restore failed: {}", restoreId, e);
            
            long duration = System.currentTimeMillis() - startTime;
            
            BatchRestoreResult result = BatchRestoreResult.builder()
                .restoreId(restoreId)
                .backupId(request.getBackupId())
                .success(false)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .errorMessage(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(result);
        }
    }

    // Helper methods and utility functions...
    
    private String generateBackupId(BatchBackupType type) {
        return String.format("batch_%s_%s_%d", 
            type.name().toLowerCase(), 
            LocalDateTime.now().format(backupTimestamp),
            System.nanoTime() % 10000);
    }

    private String generateRestoreId() {
        return String.format("batch_restore_%s_%d", 
            LocalDateTime.now().format(backupTimestamp),
            System.nanoTime() % 10000);
    }

    private Path createBackupDirectory(String backupId) throws IOException {
        Path backupDir = Paths.get(backupLocation, backupId);
        Files.createDirectories(backupDir);
        return backupDir;
    }

    private List<JobInstance> getAllJobInstances() {
        // Implementation to get all job instances from JobExplorer
        List<JobInstance> instances = new ArrayList<>();
        Set<String> jobNames = jobExplorer.getJobNames();
        
        for (String jobName : jobNames) {
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);
            instances.addAll(jobInstances);
        }
        
        return instances;
    }

    private List<JobExecution> getAllJobExecutions() {
        // Implementation to get all job executions
        List<JobExecution> executions = new ArrayList<>();
        List<JobInstance> instances = getAllJobInstances();
        
        for (JobInstance instance : instances) {
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(instance);
            executions.addAll(jobExecutions);
        }
        
        return executions;
    }

    // Additional helper methods would be implemented here...

    // Exception classes
    public static class BatchBackupException extends RuntimeException {
        public BatchBackupException(String message) {
            super(message);
        }
        
        public BatchBackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class BatchRestoreException extends RuntimeException {
        public BatchRestoreException(String message) {
            super(message);
        }
        
        public BatchRestoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}