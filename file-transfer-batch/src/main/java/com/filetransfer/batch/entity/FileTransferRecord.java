package com.filetransfer.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_transfer_records")
public class FileTransferRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String serviceType;
    
    @Column
    private String subServiceType;
    
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
    
    @Column
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column
    private Long fileSize;
    
    @Column
    private String checksum;
    
    @Column
    private String batchJobExecutionId;
    
    // Constructors
    public FileTransferRecord() {
        this.createdAt = LocalDateTime.now();
        this.status = TransferStatus.PENDING;
    }
    
    public FileTransferRecord(String tenantId, String fileName, String serviceType, String sourcePath, 
                            String targetPath, TransferDirection direction) {
        this();
        this.tenantId = tenantId;
        this.fileName = fileName;
        this.serviceType = serviceType;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.direction = direction;
    }
    
    public FileTransferRecord(String tenantId, String fileName, String serviceType, String subServiceType, 
                            String sourcePath, String targetPath, TransferDirection direction) {
        this();
        this.tenantId = tenantId;
        this.fileName = fileName;
        this.serviceType = serviceType;
        this.subServiceType = subServiceType;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.direction = direction;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getSubServiceType() { return subServiceType; }
    public void setSubServiceType(String subServiceType) { this.subServiceType = subServiceType; }
    
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