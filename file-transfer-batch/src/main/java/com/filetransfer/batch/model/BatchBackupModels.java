package com.filetransfer.batch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data models for batch application backup operations
 */
public class BatchBackupModels {

    /**
     * Batch backup types
     */
    public enum BatchBackupType {
        FULL,           // Complete backup of all batch data
        INCREMENTAL,    // Only changed data since last backup
        JOB_STATE,      // Only Spring Batch job repository state
        RESTORE_POINT,  // Quick snapshot before major operations
        MANUAL          // User-initiated backup
    }

    /**
     * Batch backup request model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchBackupRequest {
        private BatchBackupType type;
        private boolean includeJobRepository;
        private boolean includeJobData;
        private boolean includeConfiguration;
        private boolean includeBatchFiles;
        private boolean compression;
        private boolean encryption;
        private boolean remoteSync;
        private String description;
        private Map<String, Object> options;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BatchBackupRequest request = new BatchBackupRequest();

            public Builder type(BatchBackupType type) { request.type = type; return this; }
            public Builder includeJobRepository(boolean includeJobRepository) { request.includeJobRepository = includeJobRepository; return this; }
            public Builder includeJobData(boolean includeJobData) { request.includeJobData = includeJobData; return this; }
            public Builder includeConfiguration(boolean includeConfiguration) { request.includeConfiguration = includeConfiguration; return this; }
            public Builder includeBatchFiles(boolean includeBatchFiles) { request.includeBatchFiles = includeBatchFiles; return this; }
            public Builder compression(boolean compression) { request.compression = compression; return this; }
            public Builder encryption(boolean encryption) { request.encryption = encryption; return this; }
            public Builder remoteSync(boolean remoteSync) { request.remoteSync = remoteSync; return this; }
            public Builder description(String description) { request.description = description; return this; }
            public Builder options(Map<String, Object> options) { request.options = options; return this; }

            public BatchBackupRequest build() { return request; }
        }

        // Getters and setters
        public BatchBackupType getType() { return type; }
        public void setType(BatchBackupType type) { this.type = type; }
        public boolean isIncludeJobRepository() { return includeJobRepository; }
        public void setIncludeJobRepository(boolean includeJobRepository) { this.includeJobRepository = includeJobRepository; }
        public boolean isIncludeJobData() { return includeJobData; }
        public void setIncludeJobData(boolean includeJobData) { this.includeJobData = includeJobData; }
        public boolean isIncludeConfiguration() { return includeConfiguration; }
        public void setIncludeConfiguration(boolean includeConfiguration) { this.includeConfiguration = includeConfiguration; }
        public boolean isIncludeBatchFiles() { return includeBatchFiles; }
        public void setIncludeBatchFiles(boolean includeBatchFiles) { this.includeBatchFiles = includeBatchFiles; }
        public boolean isCompression() { return compression; }
        public void setCompression(boolean compression) { this.compression = compression; }
        public boolean isEncryption() { return encryption; }
        public void setEncryption(boolean encryption) { this.encryption = encryption; }
        public boolean isRemoteSync() { return remoteSync; }
        public void setRemoteSync(boolean remoteSync) { this.remoteSync = remoteSync; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * Batch backup result model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchBackupResult {
        private String backupId;
        private BatchBackupType type;
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
        
        private JobRepositoryBackupResult jobRepositoryBackup;
        private JobDataBackupResult jobDataBackup;
        private ConfigurationBackupResult configurationBackup;
        private BatchFilesBackupResult batchFilesBackup;
        private RemoteSyncResult remoteSync;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BatchBackupResult result = new BatchBackupResult();

            public Builder backupId(String backupId) { result.backupId = backupId; return this; }
            public Builder type(BatchBackupType type) { result.type = type; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder compressedPath(String compressedPath) { result.compressedPath = compressedPath; return this; }
            public Builder encryptedPath(String encryptedPath) { result.encryptedPath = encryptedPath; return this; }
            public Builder jobRepositoryBackup(JobRepositoryBackupResult jobRepositoryBackup) { result.jobRepositoryBackup = jobRepositoryBackup; return this; }
            public Builder jobDataBackup(JobDataBackupResult jobDataBackup) { result.jobDataBackup = jobDataBackup; return this; }
            public Builder configurationBackup(ConfigurationBackupResult configurationBackup) { result.configurationBackup = configurationBackup; return this; }
            public Builder batchFilesBackup(BatchFilesBackupResult batchFilesBackup) { result.batchFilesBackup = batchFilesBackup; return this; }
            public Builder remoteSync(RemoteSyncResult remoteSync) { result.remoteSync = remoteSync; return this; }

            public BatchBackupResult build() { return result; }
        }

        // Getters and setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public BatchBackupType getType() { return type; }
        public void setType(BatchBackupType type) { this.type = type; }
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
        public JobRepositoryBackupResult getJobRepositoryBackup() { return jobRepositoryBackup; }
        public void setJobRepositoryBackup(JobRepositoryBackupResult jobRepositoryBackup) { this.jobRepositoryBackup = jobRepositoryBackup; }
        public JobDataBackupResult getJobDataBackup() { return jobDataBackup; }
        public void setJobDataBackup(JobDataBackupResult jobDataBackup) { this.jobDataBackup = jobDataBackup; }
        public ConfigurationBackupResult getConfigurationBackup() { return configurationBackup; }
        public void setConfigurationBackup(ConfigurationBackupResult configurationBackup) { this.configurationBackup = configurationBackup; }
        public BatchFilesBackupResult getBatchFilesBackup() { return batchFilesBackup; }
        public void setBatchFilesBackup(BatchFilesBackupResult batchFilesBackup) { this.batchFilesBackup = batchFilesBackup; }
        public RemoteSyncResult getRemoteSync() { return remoteSync; }
        public void setRemoteSync(RemoteSyncResult remoteSync) { this.remoteSync = remoteSync; }
    }

    /**
     * Job repository backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobRepositoryBackupResult {
        private boolean success;
        private String backupPath;
        private int jobInstanceCount;
        private int jobExecutionCount;
        private long size;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private JobRepositoryBackupResult result = new JobRepositoryBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder jobInstanceCount(int jobInstanceCount) { result.jobInstanceCount = jobInstanceCount; return this; }
            public Builder jobExecutionCount(int jobExecutionCount) { result.jobExecutionCount = jobExecutionCount; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }

            public JobRepositoryBackupResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public int getJobInstanceCount() { return jobInstanceCount; }
        public void setJobInstanceCount(int jobInstanceCount) { this.jobInstanceCount = jobInstanceCount; }
        public int getJobExecutionCount() { return jobExecutionCount; }
        public void setJobExecutionCount(int jobExecutionCount) { this.jobExecutionCount = jobExecutionCount; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Job data backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobDataBackupResult {
        private boolean success;
        private String backupPath;
        private long fileCount;
        private long size;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private JobDataBackupResult result = new JobDataBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder fileCount(long fileCount) { result.fileCount = fileCount; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }

            public JobDataBackupResult build() { return result; }
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        public long getFileCount() { return fileCount; }
        public void setFileCount(long fileCount) { this.fileCount = fileCount; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Configuration backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConfigurationBackupResult {
        private boolean success;
        private String backupPath;
        private long size;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ConfigurationBackupResult result = new ConfigurationBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }

            public ConfigurationBackupResult build() { return result; }
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
    }

    /**
     * Batch files backup result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchFilesBackupResult {
        private boolean success;
        private String backupPath;
        private long size;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BatchFilesBackupResult result = new BatchFilesBackupResult();

            public Builder success(boolean success) { result.success = success; return this; }
            public Builder backupPath(String backupPath) { result.backupPath = backupPath; return this; }
            public Builder size(long size) { result.size = size; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }

            public BatchFilesBackupResult build() { return result; }
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
     * Batch backup metadata
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchBackupMetadata {
        private String backupId;
        private BatchBackupType type;
        private String description;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        private String backupPath;
        private long size;
        private boolean compressed;
        private boolean encrypted;
        private String version;
        private String environment;
        private Map<String, Object> metadata;

        // Constructors, getters, and setters
        public BatchBackupMetadata() {}

        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public BatchBackupType getType() { return type; }
        public void setType(BatchBackupType type) { this.type = type; }
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
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * Batch restore request model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchRestoreRequest {
        private String backupId;
        private boolean restoreJobRepository;
        private boolean restoreJobData;
        private boolean restoreConfiguration;
        private boolean createRestorePoint;
        private String targetLocation;
        private Map<String, Object> options;

        // Constructors, getters, and setters
        public BatchRestoreRequest() {}

        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public boolean isRestoreJobRepository() { return restoreJobRepository; }
        public void setRestoreJobRepository(boolean restoreJobRepository) { this.restoreJobRepository = restoreJobRepository; }
        public boolean isRestoreJobData() { return restoreJobData; }
        public void setRestoreJobData(boolean restoreJobData) { this.restoreJobData = restoreJobData; }
        public boolean isRestoreConfiguration() { return restoreConfiguration; }
        public void setRestoreConfiguration(boolean restoreConfiguration) { this.restoreConfiguration = restoreConfiguration; }
        public boolean isCreateRestorePoint() { return createRestorePoint; }
        public void setCreateRestorePoint(boolean createRestorePoint) { this.createRestorePoint = createRestorePoint; }
        public String getTargetLocation() { return targetLocation; }
        public void setTargetLocation(String targetLocation) { this.targetLocation = targetLocation; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * Batch restore result model
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchRestoreResult {
        private String restoreId;
        private String backupId;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String errorMessage;
        
        private JobRepositoryRestoreResult jobRepositoryRestore;
        private JobDataRestoreResult jobDataRestore;
        private ConfigurationRestoreResult configurationRestore;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BatchRestoreResult result = new BatchRestoreResult();

            public Builder restoreId(String restoreId) { result.restoreId = restoreId; return this; }
            public Builder backupId(String backupId) { result.backupId = backupId; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder jobRepositoryRestore(JobRepositoryRestoreResult jobRepositoryRestore) { result.jobRepositoryRestore = jobRepositoryRestore; return this; }
            public Builder jobDataRestore(JobDataRestoreResult jobDataRestore) { result.jobDataRestore = jobDataRestore; return this; }
            public Builder configurationRestore(ConfigurationRestoreResult configurationRestore) { result.configurationRestore = configurationRestore; return this; }

            public BatchRestoreResult build() { return result; }
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
        public JobRepositoryRestoreResult getJobRepositoryRestore() { return jobRepositoryRestore; }
        public void setJobRepositoryRestore(JobRepositoryRestoreResult jobRepositoryRestore) { this.jobRepositoryRestore = jobRepositoryRestore; }
        public JobDataRestoreResult getJobDataRestore() { return jobDataRestore; }
        public void setJobDataRestore(JobDataRestoreResult jobDataRestore) { this.jobDataRestore = jobDataRestore; }
        public ConfigurationRestoreResult getConfigurationRestore() { return configurationRestore; }
        public void setConfigurationRestore(ConfigurationRestoreResult configurationRestore) { this.configurationRestore = configurationRestore; }
    }

    /**
     * Job repository restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobRepositoryRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private int jobInstancesRestored;
        private int jobExecutionsRestored;

        // Constructors, getters, and setters
        public JobRepositoryRestoreResult() {}

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public int getJobInstancesRestored() { return jobInstancesRestored; }
        public void setJobInstancesRestored(int jobInstancesRestored) { this.jobInstancesRestored = jobInstancesRestored; }
        public int getJobExecutionsRestored() { return jobExecutionsRestored; }
        public void setJobExecutionsRestored(int jobExecutionsRestored) { this.jobExecutionsRestored = jobExecutionsRestored; }
    }

    /**
     * Job data restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobDataRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private long filesRestored;
        private long bytesRestored;

        // Constructors, getters, and setters
        public JobDataRestoreResult() {}

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
     * Configuration restore result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConfigurationRestoreResult {
        private boolean success;
        private String errorMessage;
        private long duration;
        private Map<String, Boolean> componentRestoreStatus;

        // Constructors, getters, and setters
        public ConfigurationRestoreResult() {}

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