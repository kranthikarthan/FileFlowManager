package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive backup service for database, files, and application state
 * Provides automated backups, point-in-time recovery, and cross-region replication
 */
@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private NotificationService notificationService;

    @Value("${backup.database.enabled:true}")
    private boolean databaseBackupEnabled;

    @Value("${backup.files.enabled:true}")
    private boolean fileBackupEnabled;

    @Value("${backup.retention.days:30}")
    private int retentionDays;

    @Value("${backup.location.primary:/var/backups/file-transfer/primary}")
    private String primaryBackupLocation;

    @Value("${backup.location.secondary:/var/backups/file-transfer/secondary}")
    private String secondaryBackupLocation;

    @Value("${backup.remote.enabled:false}")
    private boolean remoteBackupEnabled;

    @Value("${backup.remote.endpoint:}")
    private String remoteBackupEndpoint;

    @Value("${backup.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${backup.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${backup.verification.enabled:true}")
    private boolean verificationEnabled;

    @Value("${file.storage.path:/app/data}")
    private String fileStoragePath;

    private final DateTimeFormatter backupTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Scheduled full backup - runs daily at 2 AM
     */
    @Scheduled(cron = "${backup.schedule.full:0 0 2 * * *}")
    public void performScheduledFullBackup() {
        logger.info("Starting scheduled full backup");
        
        try {
            BackupRequest request = BackupRequest.builder()
                .type(BackupType.FULL)
                .includeDatabase(databaseBackupEnabled)
                .includeFiles(fileBackupEnabled)
                .includeApplicationState(true)
                .compression(compressionEnabled)
                .encryption(encryptionEnabled)
                .verification(verificationEnabled)
                .remoteSync(remoteBackupEnabled)
                .build();

            BackupResult result = performBackup(request).join();
            
            if (result.isSuccess()) {
                logger.info("Scheduled full backup completed successfully: {}", result.getBackupId());
                notifyBackupSuccess(result);
            } else {
                logger.error("Scheduled full backup failed: {}", result.getErrorMessage());
                notifyBackupFailure(result);
            }
            
        } catch (Exception e) {
            logger.error("Scheduled full backup failed with exception", e);
            notifyBackupException(e);
        }
    }

    /**
     * Scheduled incremental backup - runs every 4 hours
     */
    @Scheduled(cron = "${backup.schedule.incremental:0 0 */4 * * *}")
    public void performScheduledIncrementalBackup() {
        logger.info("Starting scheduled incremental backup");
        
        try {
            BackupRequest request = BackupRequest.builder()
                .type(BackupType.INCREMENTAL)
                .includeDatabase(true)
                .includeFiles(true)
                .includeApplicationState(false)
                .compression(compressionEnabled)
                .encryption(encryptionEnabled)
                .verification(false)
                .remoteSync(false)
                .build();

            BackupResult result = performBackup(request).join();
            
            if (result.isSuccess()) {
                logger.info("Scheduled incremental backup completed: {}", result.getBackupId());
            } else {
                logger.error("Scheduled incremental backup failed: {}", result.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("Scheduled incremental backup failed with exception", e);
        }
    }

    /**
     * Perform backup asynchronously
     */
    @Async("backupTaskExecutor")
    public CompletableFuture<BackupResult> performBackup(BackupRequest request) {
        String backupId = generateBackupId(request.getType());
        logger.info("Starting backup: {} of type: {}", backupId, request.getType());
        
        long startTime = System.currentTimeMillis();
        BackupResult.Builder resultBuilder = BackupResult.builder()
            .backupId(backupId)
            .type(request.getType())
            .startTime(LocalDateTime.now());

        try {
            // Create backup directory
            Path backupDir = createBackupDirectory(backupId);
            
            // Perform database backup
            if (request.isIncludeDatabase()) {
                DatabaseBackupResult dbResult = performDatabaseBackup(backupDir, request);
                resultBuilder.databaseBackup(dbResult);
                
                if (!dbResult.isSuccess()) {
                    throw new BackupException("Database backup failed: " + dbResult.getErrorMessage());
                }
            }

            // Perform file backup
            if (request.isIncludeFiles()) {
                FileBackupResult fileResult = performFileBackup(backupDir, request);
                resultBuilder.fileBackup(fileResult);
                
                if (!fileResult.isSuccess()) {
                    throw new BackupException("File backup failed: " + fileResult.getErrorMessage());
                }
            }

            // Perform application state backup
            if (request.isIncludeApplicationState()) {
                ApplicationStateBackupResult stateResult = performApplicationStateBackup(backupDir, request);
                resultBuilder.applicationStateBackup(stateResult);
                
                if (!stateResult.isSuccess()) {
                    throw new BackupException("Application state backup failed: " + stateResult.getErrorMessage());
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

            // Verify backup if requested
            if (request.isVerification()) {
                BackupVerificationResult verificationResult = verifyBackup(backupDir, request);
                resultBuilder.verification(verificationResult);
                
                if (!verificationResult.isSuccess()) {
                    throw new BackupException("Backup verification failed: " + verificationResult.getErrorMessage());
                }
            }

            // Sync to remote location if requested
            if (request.isRemoteSync() && remoteBackupEnabled) {
                RemoteSyncResult remoteSyncResult = syncToRemoteLocation(backupDir);
                resultBuilder.remoteSync(remoteSyncResult);
            }

            // Create backup metadata
            BackupMetadata metadata = createBackupMetadata(backupId, request, backupDir);
            saveBackupMetadata(metadata);

            long duration = System.currentTimeMillis() - startTime;
            
            BackupResult result = resultBuilder
                .success(true)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .backupPath(backupDir.toString())
                .size(calculateBackupSize(backupDir))
                .build();

            logger.info("Backup completed successfully: {} in {}ms", backupId, duration);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Backup failed: {}", backupId, e);
            
            long duration = System.currentTimeMillis() - startTime;
            
            BackupResult result = resultBuilder
                .success(false)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .errorMessage(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Perform database backup
     */
    private DatabaseBackupResult performDatabaseBackup(Path backupDir, BackupRequest request) {
        logger.info("Starting database backup");
        
        try {
            String databaseType = getDatabaseType();
            Path dbBackupFile = backupDir.resolve("database_backup.sql");
            
            ProcessBuilder processBuilder;
            
            switch (databaseType.toLowerCase()) {
                case "postgresql":
                    processBuilder = createPostgreSQLBackupProcess(dbBackupFile);
                    break;
                case "mysql":
                    processBuilder = createMySQLBackupProcess(dbBackupFile);
                    break;
                case "h2":
                    return performH2Backup(dbBackupFile);
                default:
                    throw new BackupException("Unsupported database type: " + databaseType);
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                long size = Files.size(dbBackupFile);
                
                return DatabaseBackupResult.builder()
                    .success(true)
                    .backupFile(dbBackupFile.toString())
                    .size(size)
                    .duration(System.currentTimeMillis())
                    .build();
            } else {
                String error = readProcessError(process);
                throw new BackupException("Database backup process failed with exit code " + exitCode + ": " + error);
            }

        } catch (Exception e) {
            logger.error("Database backup failed", e);
            
            return DatabaseBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Perform file backup
     */
    private FileBackupResult performFileBackup(Path backupDir, BackupRequest request) {
        logger.info("Starting file backup");
        
        try {
            Path sourceDir = Paths.get(fileStoragePath);
            Path targetDir = backupDir.resolve("files");
            
            if (!Files.exists(sourceDir)) {
                logger.warn("Source directory does not exist: {}", sourceDir);
                return FileBackupResult.builder()
                    .success(true)
                    .filesCopied(0)
                    .size(0)
                    .message("No files to backup")
                    .build();
            }

            Files.createDirectories(targetDir);
            
            FileBackupStatistics stats = new FileBackupStatistics();
            
            if (request.getType() == BackupType.INCREMENTAL) {
                // Incremental backup - only copy files modified since last backup
                LocalDateTime lastBackupTime = getLastBackupTime();
                copyModifiedFiles(sourceDir, targetDir, lastBackupTime, stats);
            } else {
                // Full backup - copy all files
                copyAllFiles(sourceDir, targetDir, stats);
            }

            return FileBackupResult.builder()
                .success(true)
                .filesCopied(stats.getFilesCopied())
                .size(stats.getTotalSize())
                .duration(stats.getDuration())
                .backupPath(targetDir.toString())
                .build();

        } catch (Exception e) {
            logger.error("File backup failed", e);
            
            return FileBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Perform application state backup
     */
    private ApplicationStateBackupResult performApplicationStateBackup(Path backupDir, BackupRequest request) {
        logger.info("Starting application state backup");
        
        try {
            Path stateDir = backupDir.resolve("application_state");
            Files.createDirectories(stateDir);

            // Backup application configuration
            backupApplicationConfiguration(stateDir);
            
            // Backup cache state
            backupCacheState(stateDir);
            
            // Backup session data
            backupSessionData(stateDir);
            
            // Backup metrics and logs
            backupMetricsAndLogs(stateDir);

            long size = calculateDirectorySize(stateDir);

            return ApplicationStateBackupResult.builder()
                .success(true)
                .backupPath(stateDir.toString())
                .size(size)
                .build();

        } catch (Exception e) {
            logger.error("Application state backup failed", e);
            
            return ApplicationStateBackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Restore from backup
     */
    @Transactional
    public CompletableFuture<RestoreResult> restoreFromBackup(RestoreRequest request) {
        String restoreId = generateRestoreId();
        logger.info("Starting restore: {} from backup: {}", restoreId, request.getBackupId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate backup exists
            BackupMetadata metadata = getBackupMetadata(request.getBackupId());
            if (metadata == null) {
                throw new RestoreException("Backup not found: " + request.getBackupId());
            }

            // Create restore point before starting
            if (request.isCreateRestorePoint()) {
                BackupRequest restorePointRequest = BackupRequest.builder()
                    .type(BackupType.RESTORE_POINT)
                    .includeDatabase(true)
                    .includeFiles(true)
                    .includeApplicationState(true)
                    .build();
                
                performBackup(restorePointRequest).join();
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

            RestoreResult.Builder resultBuilder = RestoreResult.builder()
                .restoreId(restoreId)
                .backupId(request.getBackupId())
                .startTime(LocalDateTime.now());

            // Restore database
            if (request.isRestoreDatabase()) {
                DatabaseRestoreResult dbResult = restoreDatabase(backupPath, request);
                resultBuilder.databaseRestore(dbResult);
                
                if (!dbResult.isSuccess()) {
                    throw new RestoreException("Database restore failed: " + dbResult.getErrorMessage());
                }
            }

            // Restore files
            if (request.isRestoreFiles()) {
                FileRestoreResult fileResult = restoreFiles(backupPath, request);
                resultBuilder.fileRestore(fileResult);
                
                if (!fileResult.isSuccess()) {
                    throw new RestoreException("File restore failed: " + fileResult.getErrorMessage());
                }
            }

            // Restore application state
            if (request.isRestoreApplicationState()) {
                ApplicationStateRestoreResult stateResult = restoreApplicationState(backupPath, request);
                resultBuilder.applicationStateRestore(stateResult);
                
                if (!stateResult.isSuccess()) {
                    throw new RestoreException("Application state restore failed: " + stateResult.getErrorMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            RestoreResult result = resultBuilder
                .success(true)
                .endTime(LocalDateTime.now())
                .duration(duration)
                .build();

            logger.info("Restore completed successfully: {} in {}ms", restoreId, duration);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            logger.error("Restore failed: {}", restoreId, e);
            
            long duration = System.currentTimeMillis() - startTime;
            
            RestoreResult result = RestoreResult.builder()
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

    /**
     * List available backups
     */
    public List<BackupMetadata> listAvailableBackups() {
        List<BackupMetadata> backups = new ArrayList<>();
        
        try {
            Path metadataDir = Paths.get(primaryBackupLocation, "metadata");
            
            if (Files.exists(metadataDir)) {
                Files.walk(metadataDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            BackupMetadata metadata = loadBackupMetadata(path);
                            if (metadata != null) {
                                backups.add(metadata);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to load backup metadata: {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            logger.error("Failed to list available backups", e);
        }

        // Sort by creation time (newest first)
        backups.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return backups;
    }

    /**
     * Clean up old backups based on retention policy
     */
    @Scheduled(cron = "${backup.cleanup.schedule:0 0 3 * * *}")
    public void cleanupOldBackups() {
        logger.info("Starting backup cleanup");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<BackupMetadata> backups = listAvailableBackups();
            
            int deletedCount = 0;
            long freedSpace = 0;
            
            for (BackupMetadata backup : backups) {
                if (backup.getCreatedAt().isBefore(cutoffDate) && 
                    backup.getType() != BackupType.RESTORE_POINT) {
                    
                    try {
                        long backupSize = deleteBackup(backup);
                        deletedCount++;
                        freedSpace += backupSize;
                        
                        logger.info("Deleted old backup: {} ({})", backup.getBackupId(), backup.getCreatedAt());
                        
                    } catch (Exception e) {
                        logger.error("Failed to delete backup: {}", backup.getBackupId(), e);
                    }
                }
            }
            
            logger.info("Backup cleanup completed. Deleted {} backups, freed {} bytes", 
                       deletedCount, freedSpace);
            
        } catch (Exception e) {
            logger.error("Backup cleanup failed", e);
        }
    }

    // Helper methods and utility functions...
    
    private String generateBackupId(BackupType type) {
        return String.format("%s_%s_%d", 
            type.name().toLowerCase(), 
            LocalDateTime.now().format(backupTimestamp),
            System.nanoTime() % 10000);
    }

    private String generateRestoreId() {
        return String.format("restore_%s_%d", 
            LocalDateTime.now().format(backupTimestamp),
            System.nanoTime() % 10000);
    }

    private Path createBackupDirectory(String backupId) throws IOException {
        Path backupDir = Paths.get(primaryBackupLocation, backupId);
        Files.createDirectories(backupDir);
        return backupDir;
    }

    private String getDatabaseType() {
        // Implementation to detect database type from DataSource
        return "postgresql"; // Default for now
    }

    private ProcessBuilder createPostgreSQLBackupProcess(Path backupFile) {
        return new ProcessBuilder(
            "pg_dump",
            "--host=" + System.getProperty("spring.datasource.host", "localhost"),
            "--port=" + System.getProperty("spring.datasource.port", "5432"),
            "--username=" + System.getProperty("spring.datasource.username", "postgres"),
            "--format=plain",
            "--verbose",
            "--file=" + backupFile.toString(),
            System.getProperty("spring.datasource.database", "filetransfer")
        );
    }

    private ProcessBuilder createMySQLBackupProcess(Path backupFile) {
        return new ProcessBuilder(
            "mysqldump",
            "--host=" + System.getProperty("spring.datasource.host", "localhost"),
            "--port=" + System.getProperty("spring.datasource.port", "3306"),
            "--user=" + System.getProperty("spring.datasource.username", "root"),
            "--password=" + System.getProperty("spring.datasource.password", ""),
            "--result-file=" + backupFile.toString(),
            System.getProperty("spring.datasource.database", "filetransfer")
        );
    }

    // Additional helper methods would be implemented here...
    
    // Exception classes
    public static class BackupException extends RuntimeException {
        public BackupException(String message) {
            super(message);
        }
        
        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class RestoreException extends RuntimeException {
        public RestoreException(String message) {
            super(message);
        }
        
        public RestoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}