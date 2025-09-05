package com.filetransfer.web.dto;

import com.filetransfer.web.entity.AckNackStatus;
import com.filetransfer.web.entity.AckNackType;
import com.filetransfer.web.entity.TransferDirection;

import java.time.LocalDateTime;

/**
 * DTO for ACK/NACK record data transfer
 */
public class AckNackRecordDto {
    
    private Long id;
    private Long originalFileTransferId;
    private String originalFileName;
    private String ackNackFileName;
    private AckNackType type;
    private AckNackStatus status;
    private TransferDirection direction;
    private String tenantId;
    private String serviceName;
    private String subServiceName;
    private String ackNackFilePath;
    private String partnerPath;
    private String content;
    private String errorMessage;
    private String reasonCode;
    private String reasonDescription;
    private LocalDateTime createdAt;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime expiresAt;
    private Long fileSize;
    private String checksum;
    private String metadata;
    
    // Default constructor
    public AckNackRecordDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOriginalFileTransferId() { return originalFileTransferId; }
    public void setOriginalFileTransferId(Long originalFileTransferId) { this.originalFileTransferId = originalFileTransferId; }
    
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    
    public String getAckNackFileName() { return ackNackFileName; }
    public void setAckNackFileName(String ackNackFileName) { this.ackNackFileName = ackNackFileName; }
    
    public AckNackType getType() { return type; }
    public void setType(AckNackType type) { this.type = type; }
    
    public AckNackStatus getStatus() { return status; }
    public void setStatus(AckNackStatus status) { this.status = status; }
    
    public TransferDirection getDirection() { return direction; }
    public void setDirection(TransferDirection direction) { this.direction = direction; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public String getAckNackFilePath() { return ackNackFilePath; }
    public void setAckNackFilePath(String ackNackFilePath) { this.ackNackFilePath = ackNackFilePath; }
    
    public String getPartnerPath() { return partnerPath; }
    public void setPartnerPath(String partnerPath) { this.partnerPath = partnerPath; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}