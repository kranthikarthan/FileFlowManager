package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing ACK/NACK file records for tracking acknowledgments
 */
@Entity
@Table(name = "ack_nack_records")
public class AckNackRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long originalFileTransferId; // Reference to the original file transfer
    
    @Column(nullable = false)
    private String originalFileName; // Name of the original data/SOT/EOT file
    
    @Column(nullable = false)
    private String ackNackFileName; // Name of the ACK/NACK file
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AckNackType type; // ACK or NACK
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AckNackStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferDirection direction; // INBOUND or OUTBOUND (relative to original file)
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Column
    private String subServiceName;
    
    @Column
    private String ackNackFilePath; // Path where ACK/NACK file is stored
    
    @Column
    private String partnerPath; // Path where partner expects/sends ACK/NACK files
    
    @Column(columnDefinition = "TEXT")
    private String content; // Content of the ACK/NACK file
    
    @Column
    private String errorMessage; // Error message if processing failed
    
    @Column
    private String reasonCode; // Reason code for NACK files
    
    @Column
    private String reasonDescription; // Human-readable reason for NACK
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime generatedAt; // When ACK/NACK was generated
    
    @Column
    private LocalDateTime sentAt; // When ACK/NACK was sent
    
    @Column
    private LocalDateTime receivedAt; // When ACK/NACK was received
    
    @Column
    private LocalDateTime processedAt; // When ACK/NACK was processed
    
    @Column
    private LocalDateTime expiresAt; // When this ACK/NACK expires
    
    @Column
    private Long fileSize;
    
    @Column
    private String checksum;
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // Additional metadata as JSON
    
    // Constructors
    public AckNackRecord() {
        this.createdAt = LocalDateTime.now();
        this.status = AckNackStatus.PENDING;
    }
    
    public AckNackRecord(Long originalFileTransferId, String originalFileName, String tenantId, 
                        String serviceName, String subServiceName, AckNackType type, 
                        TransferDirection direction) {
        this();
        this.originalFileTransferId = originalFileTransferId;
        this.originalFileName = originalFileName;
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.type = type;
        this.direction = direction;
        this.ackNackFileName = generateAckNackFileName(originalFileName, type);
    }
    
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
    
    /**
     * Generate ACK/NACK file name based on original file name
     */
    private String generateAckNackFileName(String originalFileName, AckNackType type) {
        if (originalFileName == null) {
            return null;
        }
        
        String baseName = originalFileName;
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
        }
        
        return baseName + "." + type.name().toLowerCase();
    }
    
    /**
     * Check if this ACK/NACK record has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}