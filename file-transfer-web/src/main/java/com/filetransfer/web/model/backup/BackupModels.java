package com.filetransfer.web.model.backup;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data models for backup and disaster recovery operations
 */
public class BackupModels {

    /**
     * Backup types
     */
    public enum BackupType {
        FULL,           // Complete backup of all data
        INCREMENTAL,    // Only changed data since last backup
        DIFFERENTIAL,   // Changed data since last full backup
        RESTORE_POINT,  // Quick snapshot before major operations
        MANUAL          // User-initiated backup
    }

    /**
     * Backup request model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BackupRequest {
        private BackupType type;
        private boolean includeDatabase;
        private boolean includeFiles;
        private boolean includeApplicationState;
        private boolean compression;
        private boolean encryption;
        private boolean verification;
        private boolean remoteSync;
        private String description;
        private Map<String, Object> options;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BackupRequest request = new BackupRequest();

            public Builder type(BackupType type) {
                request.type = type;
                return this;
            }

            public Builder includeDatabase(boolean includeDatabase) {
                request.includeDatabase = includeDatabase;
                return this;
            }

            public Builder includeFiles(boolean includeFiles) {
                request.includeFiles = includeFiles;
                return this;
            }

            public Builder includeApplicationState(boolean includeApplicationState) {
                request.includeApplicationState = includeApplicationState;
                return this;
            }

            public Builder compression(boolean compression) {
                request.compression = compression;
                return this;
            }

            public Builder encryption(boolean encryption) {
                request.encryption = encryption;
                return this;
            }

            public Builder verification(boolean verification) {
                request.verification = verification;
                return this;
            }

            public Builder remoteSync(boolean remoteSync) {
                request.remoteSync = remoteSync;
                return this;
            }

            public Builder description(String description) {
                request.description = description;
                return this;
            }

            public Builder options(Map<String, Object> options) {
                request.options = options;
                return this;
            }

            public BackupRequest build() {
                return request;
            }
        }

        // Getters and setters
        public BackupType getType() { return type; }
        public void setType(BackupType type) { this.type = type; }
        public boolean isIncludeDatabase() { return includeDatabase; }
        public void setIncludeDatabase(boolean includeDatabase) { this.includeDatabase = includeDatabase; }
        public boolean isIncludeFiles() { return includeFiles; }
        public void setIncludeFiles(boolean includeFiles) { this.includeFiles = includeFiles; }
        public boolean isIncludeApplicationState() { return includeApplicationState; }
        public void setIncludeApplicationState(boolean includeApplicationState) { this.includeApplicationState = includeApplicationState; }
        public boolean isCompression() { return compression; }
        public void setCompression(boolean compression) { this.compression = compression; }
        public boolean isEncryption() { return encryption; }
        public void setEncryption(boolean encryption) { this.encryption = encryption; }
        public boolean isVerification() { return verification; }
        public void setVerification(boolean verification) { this.verification = verification; }
        public boolean isRemoteSync() { return remoteSync; }
        public void setRemoteSync(boolean remoteSync) { this.remoteSync = remoteSync; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * Backup result model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BackupResult {
        private String backupId;
        private BackupType type;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String backupPath;
        private long size;
        private String errorMessage;
        private String compressedPath;
        private String encryptedPath;
        
        private DatabaseBackupResult databaseBackup;
        private FileBackupResult fileBackup;
        private ApplicationStateBackupResult applicationStateBackup;
        private BackupVerificationResult verification;
        private RemoteSyncResult remoteSync;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BackupResult result = new BackupResult();

            public Builder backupId(String backupId) {
                result.backupId = backupId;
                return this;
            }

            public Builder type(BackupType type) {
                result.type = type;
                return this;
            }

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder startTime(LocalDateTime startTime) {
                result.startTime = startTime;
                return this;
            }

            public Builder endTime(LocalDateTime endTime) {
                result.endTime = endTime;
                return this;
            }

            public Builder duration(long duration) {
                result.duration = duration;
                return this;
            }

            public Builder backupPath(String backupPath) {
                result.backupPath = backupPath;
                return this;
            }

            public Builder size(long size) {
                result.size = size;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }

            public Builder compressedPath(String compressedPath) {
                result.compressedPath = compressedPath;
                return this;
            }

            public Builder encryptedPath(String encryptedPath) {
                result.encryptedPath = encryptedPath;
                return this;
            }

            public Builder databaseBackup(DatabaseBackupResult databaseBackup) {
                result.databaseBackup = databaseBackup;
                return this;
            }

            public Builder fileBackup(FileBackupResult fileBackup) {
                result.fileBackup = fileBackup;
                return this;
            }

            public Builder applicationStateBackup(ApplicationStateBackupResult applicationStateBackup) {
                result.applicationStateBackup = applicationStateBackup;
                return this;
            }

            public Builder verification(BackupVerificationResult verification) {
                result.verification = verification;
                return this;
            }

            public Builder remoteSync(RemoteSyncResult remoteSync) {
                result.remoteSync = remoteSync;
                return this;
            }

            public BackupResult build() {
                return result;
            }
        }

        // Getters and setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public BackupType getType() { return type; }
        public void setType(BackupType type) { this.type = type; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getCompressedPath() { return compressedPath; }
        public void setCompressedPath(String compressedPath) { this.compressedPath = compressedPath; }
        public String getEncryptedPath() { return encryptedPath; }
        public void setEncryptedPath(String encryptedPath) { this.encryptedPath = encryptedPath; }
        public DatabaseBackupResult getDatabaseBackup() { return databaseBackup; }
        public void setDatabaseBackup(DatabaseBackupResult databaseBackup) { this.databaseBackup = databaseBackup; }
        public FileBackupResult getFileBackup() { return fileBackup; }
        public void setFileBackup(FileBackupResult fileBackup) { this.fileBackup = fileBackup; }
        public ApplicationStateBackupResult getApplicationStateBackup() { return applicationStateBackup; }
        public void setApplicationStateBackup(ApplicationStateBackupResult applicationStateBackup) { this.applicationStateBackup = applicationStateBackup; }
        public BackupVerificationResult getVerification() { return verification; }
        public void setVerification(BackupVerificationResult verification) { this.verification = verification; }
        public RemoteSyncResult getRemoteSync() { return remoteSync; }
        public void setRemoteSync(RemoteSyncResult remoteSync) { this.remoteSync = remoteSync; }
    }

    /**
     * Database backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DatabaseBackupResult {
        private boolean success;
        private String backupFile;
        private long size;
        private long duration;
        private String errorMessage;
        private String databaseType;
        private int tableCount;
        private long recordCount;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DatabaseBackupResult result = new DatabaseBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupFile(String backupFile) { result.backupFile = backupFile; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder databaseType(String databaseType) { result.databaseType = databaseType; return this; }
            public Builder tableCount(int tableCount) { result.tableCount = tableCount; return this; }
            public Builder recordCount(long recordCount) { result.recordCount = recordCount; return this; }

            public DatabaseBackupResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupFile() { return backupFile; }
        public void setBackupFile(String backupFile) { this.backupFile = backupFile; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getDatabaseType() { return databaseType; }
        public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }
        public long getRecordCount() { return recordCount; }
        public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
    }

    /**
     * File backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileBackupResult {
        private boolean success;
        private String backupPath;
        private long filesCopied;
        private long size;
        private long duration;
        private String errorMessage;
        private String message;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FileBackupResult result = new FileBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder filesCopied(long filesCopied) { result.filesCopied = filesCopied; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder message(String message) { result.message = message; return this; }

            public FileBackupResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public long getFilesCopied() { return filesCopied; }
        public void setFilesCopied(long filesCopied) { this.filesCopied = filesCopied; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Application state backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicationStateBackupResult {
        private boolean success;
        private String backupPath;
        private long size;
        private String errorMessage;
        private Map<String, Object> components;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ApplicationStateBackupResult result = new ApplicationStateBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder components(Map<String, Object> components) { result.components = components; return this; }

            public ApplicationStateBackupResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, Object> getComponents() { return components; }
        public void setComponents(Map<String, Object> components) { this.components = components; }
    }

    /**
     * Backup verification result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BackupVerificationResult {
        private boolean success;
        private String errorMessage;
        private Map<String, Boolean> componentVerification;
        private String checksumVerification;
        private boolean integrityCheck;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BackupVerificationResult result = new BackupVerificationResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder componentVerification(Map<String, Boolean> componentVerification) { result.componentVerification = componentVerification; return this; }
            public Builder checksumVerification(String checksumVerification) { result.checksumVerification = checksumVerification; return this; }
            public Builder integrityCheck(boolean integrityCheck) { result.integrityCheck = integrityCheck; return this; }

            public BackupVerificationResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, Boolean> getComponentVerification() { return componentVerification; }
        public void setComponentVerification(Map<String, Boolean> componentVerification) { this.componentVerification = componentVerification; }
        public String getChecksumVerification() { return checksumVerification; }
        public void setChecksumVerification(String checksumVerification) { this.checksumVerification = checksumVerification; }
        public boolean isIntegrityCheck() { return integrityCheck; }
        public void setIntegrityCheck(boolean integrityCheck) { this.integrityCheck = integrityCheck; }
    }

    /**
     * Remote sync result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RemoteSyncResult {
        private boolean success;
        private String remoteLocation;
        private long syncedSize;
        private long duration;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RemoteSyncResult result = new RemoteSyncResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder remoteLocation(String remoteLocation) { result.remoteLocation = remoteLocation; return this; }
            public Builder syncedSize(long syncedSize) { result.syncedSize = syncedSize; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }

            public RemoteSyncResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getRemoteLocation() { return remoteLocation; }
        public void setRemoteLocation(String remoteLocation) { this.remoteLocation = remoteLocation; }
        public long getSyncedSize() { return syncedSize; }
        public void setSyncedSize(long syncedSize) { this.syncedSize = syncedSize; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Backup metadata
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BackupMetadata {
        private String backupId;
        private BackupType type;
        private String description;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        private String backupPath;
        private long size;
        private boolean compressed;
        private boolean encrypted;
        private boolean verified;
        private String version;
        private String environment;
        private Map<String, Object> metadata;

        // Constructors, getters, and setters
        public BackupMetadata() {}

        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public BackupType getType() { return type; }
        public void setType(BackupType type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public boolean isCompressed() { return compressed; }
        public void setCompressed(boolean compressed) { this.compressed = compressed; }
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * Restore request model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RestoreRequest {
        private String backupId;
        private boolean restoreDatabase;
        private boolean restoreFiles;
        private boolean restoreApplicationState;
        private boolean createRestorePoint;
        private String targetLocation;
        private Map<String, Object> options;

        // Constructors, getters, and setters
        public RestoreRequest() {}

        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public boolean isRestoreDatabase() { return restoreDatabase; }
        public void setRestoreDatabase(boolean restoreDatabase) { this.restoreDatabase = restoreDatabase; }
        public boolean isRestoreFiles() { return restoreFiles; }
        public void setRestoreFiles(boolean restoreFiles) { this.restoreFiles = restoreFiles; }
        public boolean isRestoreApplicationState() { return restoreApplicationState; }
        public void setRestoreApplicationState(boolean restoreApplicationState) { this.restoreApplicationState = restoreApplicationState; }
        public boolean isCreateRestorePoint() { return createRestorePoint; }
        public void setCreateRestorePoint(boolean createRestorePoint) { this.createRestorePoint = createRestorePoint; }
        public String getTargetLocation() { return targetLocation; }
        public void setTargetLocation(String targetLocation) { this.targetLocation = targetLocation; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * Restore result model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RestoreResult {
        private String restoreId;
        private String backupId;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String errorMessage;
        
        private DatabaseRestoreResult databaseRestore;
        private FileRestoreResult fileRestore;
        private ApplicationStateRestoreResult applicationStateRestore;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RestoreResult result = new RestoreResult();

            public Builder restoreId(String restoreId) { result.restoreId = restoreId; return this; }
            public Builder backupId(String backupId) { result.backupId = backupId; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder databaseRestore(DatabaseRestoreResult databaseRestore) { result.databaseRestore = databaseRestore; return this; }
            public Builder fileRestore(FileRestoreResult fileRestore) { result.fileRestore = fileRestore; return this; }
            public Builder applicationStateRestore(ApplicationStateRestoreResult applicationStateRestore) { result.applicationStateRestore = applicationStateRestore; return this; }

            public RestoreResult build() { return result; }
        }

        // Getters and setters
        public String getRestoreId() { return restoreId; }
        public void setRestoreId(String restoreId) { this.restoreId = restoreId; }
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public DatabaseRestoreResult getDatabaseRestore() { return databaseRestore; }
        public void setDatabaseRestore(DatabaseRestoreResult databaseRestore) { this.databaseRestore = databaseRestore; }
        public FileRestoreResult getFileRestore() { return fileRestore; }
        public void setFileRestore(FileRestoreResult fileRestore) { this.fileRestore = fileRestore; }
        public ApplicationStateRestoreResult getApplicationStateRestore() { return applicationStateRestore; }
        public void setApplicationStateRestore(ApplicationStateRestoreResult applicationStateRestore) { this.applicationStateRestore = applicationStateRestore; }
    }

    /**
     * Database restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DatabaseRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private int tablesRestored;
        private long recordsRestored;

        // Constructors, getters, and setters
        public DatabaseRestoreResult() {}

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public int getTablesRestored() { return tablesRestored; }
        public void setTablesRestored(int tablesRestored) { this.tablesRestored = tablesRestored; }
        public long getRecordsRestored() { return recordsRestored; }
        public void setRecordsRestored(long recordsRestored) { this.recordsRestored = recordsRestored; }
    }

    /**
     * File restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private long filesRestored;
        private long bytesRestored;

        // Constructors, getters, and setters
        public FileRestoreResult() {}

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public long getFilesRestored() { return filesRestored; }
        public void setFilesRestored(long filesRestored) { this.filesRestored = filesRestored; }
        public long getBytesRestored() { return bytesRestored; }
        public void setBytesRestored(long bytesRestored) { this.bytesRestored = bytesRestored; }
    }

    /**
     * Application state restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicationStateRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private Map<String, Boolean> componentRestoreStatus;

        // Constructors, getters, and setters
        public ApplicationStateRestoreResult() {}

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public Map<String, Boolean> getComponentRestoreStatus() { return componentRestoreStatus; }
        public void setComponentRestoreStatus(Map<String, Boolean> componentRestoreStatus) { this.componentRestoreStatus = componentRestoreStatus; }
    }
}