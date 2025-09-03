package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_transfer_records")
public class FileTransferRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "sub_service_name")
    private String subServiceName;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String sourcePath;
    
    @Column(nullable = false)
    private String targetPath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferDirection direction;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType = FileType.COBOL_FLAT_FILE;
    
    @Column
    private Boolean schemaValidationPassed;
    
    @Column
    private String schemaValidationErrors;
    
    @Column
    private Long schemaId;
    
    @Column
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;
    
    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;
    
    @Column
    private Long fileSize;
    
    @Column
    private String checksum;
    
    @Column
    private String batchJobExecutionId;
    
    @Column(name = "validation_result", columnDefinition = "TEXT")
    private String validationResult;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compression_type")
    private CompressionType compressionType = CompressionType.NONE;
    
    @Column(name = "original_file_size")
    private Long originalFileSize;
    
    @Column(name = "compressed_file_size")
    private Long compressedFileSize;
    
    @Column(name = "compression_ratio")
    private Float compressionRatio;
    
    @Column(name = "compression_time_ms")
    private Long compressionTimeMs;
    
    @Column(name = "decompression_time_ms")
    private Long decompressionTimeMs;
    
    @Column(name = "compressed_file_path")
    private String compressedFilePath;
    
    @Column(name = "compression_enabled")
    private Boolean compressionEnabled = false;
    
    // Constructors
    public FileTransferRecord() {
        this.createdAt = LocalDateTime.now();
        this.status = TransferStatus.PENDING;
    }
    
    public FileTransferRecord(String fileName, String serviceName, String subServiceName, String tenantId, 
                            String sourcePath, String targetPath, TransferDirection direction) {
        this();
        this.fileName = fileName;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.tenantId = tenantId;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.direction = direction;
        this.fileType = FileType.detectFromContent(null, fileName); // Auto-detect from filename
    }
    
    public FileTransferRecord(String fileName, String serviceName, String subServiceName, String tenantId, 
                            String sourcePath, String targetPath, TransferDirection direction, FileType fileType) {
        this();
        this.fileName = fileName;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.tenantId = tenantId;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.direction = direction;
        this.fileType = fileType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    
    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
    
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    
    public TransferDirection getDirection() { return direction; }
    public void setDirection(TransferDirection direction) { this.direction = direction; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public Boolean getSchemaValidationPassed() { return schemaValidationPassed; }
    public void setSchemaValidationPassed(Boolean schemaValidationPassed) { this.schemaValidationPassed = schemaValidationPassed; }
    
    public String getSchemaValidationErrors() { return schemaValidationErrors; }
    public void setSchemaValidationErrors(String schemaValidationErrors) { this.schemaValidationErrors = schemaValidationErrors; }
    
    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public LocalDateTime getProcessingStartTime() { return processingStartTime; }
    public void setProcessingStartTime(LocalDateTime processingStartTime) { this.processingStartTime = processingStartTime; }
    
    public LocalDateTime getProcessingEndTime() { return processingEndTime; }
    public void setProcessingEndTime(LocalDateTime processingEndTime) { this.processingEndTime = processingEndTime; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getBatchJobExecutionId() { return batchJobExecutionId; }
    public void setBatchJobExecutionId(String batchJobExecutionId) { this.batchJobExecutionId = batchJobExecutionId; }
    
    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public CompressionType getCompressionType() { return compressionType; }
    public void setCompressionType(CompressionType compressionType) { this.compressionType = compressionType; }
    
    public Long getOriginalFileSize() { return originalFileSize; }
    public void setOriginalFileSize(Long originalFileSize) { this.originalFileSize = originalFileSize; }
    
    public Long getCompressedFileSize() { return compressedFileSize; }
    public void setCompressedFileSize(Long compressedFileSize) { this.compressedFileSize = compressedFileSize; }
    
    public Float getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(Float compressionRatio) { this.compressionRatio = compressionRatio; }
    
    public Long getCompressionTimeMs() { return compressionTimeMs; }
    public void setCompressionTimeMs(Long compressionTimeMs) { this.compressionTimeMs = compressionTimeMs; }
    
    public Long getDecompressionTimeMs() { return decompressionTimeMs; }
    public void setDecompressionTimeMs(Long decompressionTimeMs) { this.decompressionTimeMs = decompressionTimeMs; }
    
    public String getCompressedFilePath() { return compressedFilePath; }
    public void setCompressedFilePath(String compressedFilePath) { this.compressedFilePath = compressedFilePath; }
    
    public Boolean getCompressionEnabled() { return compressionEnabled; }
    public void setCompressionEnabled(Boolean compressionEnabled) { this.compressionEnabled = compressionEnabled; }
}