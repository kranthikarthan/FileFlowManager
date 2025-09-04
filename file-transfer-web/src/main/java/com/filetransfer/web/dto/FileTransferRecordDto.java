package com.filetransfer.web.dto;

import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.entity.CompressionType;

import java.time.LocalDateTime;

public class FileTransferRecordDto {
    
    private Long id;
    private String fileName;
    private String serviceName;
    private String subServiceName;
    private String tenantId;
    private String sourcePath;
    private String targetPath;
    private TransferStatus status;
    private TransferDirection direction;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long fileSize;
    private String checksum;
    private String batchJobExecutionId;
    
    // Compression fields
    private Boolean compressionEnabled;
    private CompressionType compressionType;
    private Long originalFileSize;
    private Long compressedFileSize;
    private Float compressionRatio;
    private Long compressionTimeMs;
    private Long decompressionTimeMs;
    private String compressedFilePath;
    
    // Constructors
    public FileTransferRecordDto() {}
    
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
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getBatchJobExecutionId() { return batchJobExecutionId; }
    public void setBatchJobExecutionId(String batchJobExecutionId) { this.batchJobExecutionId = batchJobExecutionId; }
    
    // Compression getters and setters
    public Boolean getCompressionEnabled() { return compressionEnabled; }
    public void setCompressionEnabled(Boolean compressionEnabled) { this.compressionEnabled = compressionEnabled; }
    
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
}