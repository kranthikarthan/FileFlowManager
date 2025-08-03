package com.filetransfer.web.dto;

import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;

import java.time.LocalDateTime;

public class FileTransferRecordDto {
    
    private Long id;
    private String fileName;
    private String serviceType;
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
    
    // Constructors
    public FileTransferRecordDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
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
}