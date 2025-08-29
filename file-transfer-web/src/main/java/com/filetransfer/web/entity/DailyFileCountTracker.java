package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity to track daily file counts for EOT validation.
 * Records the number of data files received each day for each subservice.
 */
@Entity
@Table(name = "daily_file_count_tracker")
public class DailyFileCountTracker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "sub_service_name", nullable = false)
    private String subServiceName;
    
    @Column(name = "processing_date", nullable = false)
    private LocalDate processingDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransferDirection direction;
    
    // Count tracking
    @Column(name = "expected_count")
    private Integer expectedCount; // From EOT file or configuration
    
    @Column(name = "actual_count", nullable = false)
    private Integer actualCount = 0;
    
    @Column(name = "sot_received", nullable = false)
    private Boolean sotReceived = false;
    
    @Column(name = "eot_received", nullable = false)
    private Boolean eotReceived = false;
    
    @Column(name = "eot_count_value")
    private Integer eotCountValue; // Count value extracted from EOT file
    
    @Column(name = "eot_field_path")
    private String eotFieldPath; // Field path used to extract count
    
    // Status tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;
    
    @Column(name = "validation_message", length = 1000)
    private String validationMessage;
    
    @Column(name = "discrepancy_count")
    private Integer discrepancyCount; // Difference between expected and actual
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    public enum ValidationStatus {
        PENDING,        // Waiting for EOT file
        MATCHED,        // Counts match perfectly
        DISCREPANCY,    // Counts don't match
        MISSING_EOT,    // EOT file not received
        MISSING_SOT,    // SOT file not received
        ERROR          // Validation error occurred
    }
    
    // Constructors
    public DailyFileCountTracker() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DailyFileCountTracker(String tenantId, String serviceName, String subServiceName, 
                                LocalDate processingDate, FileType fileType, TransferDirection direction) {
        this();
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.processingDate = processingDate;
        this.fileType = fileType;
        this.direction = direction;
    }
    
    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void incrementActualCount() {
        this.actualCount = (this.actualCount == null ? 0 : this.actualCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void recordSotReceived() {
        this.sotReceived = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void recordEotReceived(Integer countValue, String fieldPath) {
        this.eotReceived = true;
        this.eotCountValue = countValue;
        this.eotFieldPath = fieldPath;
        this.updatedAt = LocalDateTime.now();
        
        // Perform validation
        validateCounts();
    }
    
    public void validateCounts() {
        if (!eotReceived) {
            this.validationStatus = ValidationStatus.PENDING;
            this.validationMessage = "Waiting for EOT file";
            return;
        }
        
        if (!sotReceived) {
            this.validationStatus = ValidationStatus.MISSING_SOT;
            this.validationMessage = "SOT file not received";
            this.validatedAt = LocalDateTime.now();
            return;
        }
        
        if (eotCountValue == null) {
            this.validationStatus = ValidationStatus.ERROR;
            this.validationMessage = "Could not extract count from EOT file";
            this.validatedAt = LocalDateTime.now();
            return;
        }
        
        this.discrepancyCount = Math.abs(eotCountValue - actualCount);
        
        if (eotCountValue.equals(actualCount)) {
            this.validationStatus = ValidationStatus.MATCHED;
            this.validationMessage = String.format("Count validation passed: %d files", actualCount);
        } else {
            this.validationStatus = ValidationStatus.DISCREPANCY;
            this.validationMessage = String.format("Count mismatch: EOT says %d, actual %d (difference: %d)", 
                                                  eotCountValue, actualCount, discrepancyCount);
        }
        
        this.validatedAt = LocalDateTime.now();
    }
    
    public boolean hasDiscrepancy() {
        return validationStatus == ValidationStatus.DISCREPANCY && discrepancyCount != null && discrepancyCount > 0;
    }
    
    public boolean isValidationComplete() {
        return validatedAt != null && 
               (validationStatus == ValidationStatus.MATCHED || 
                validationStatus == ValidationStatus.DISCREPANCY ||
                validationStatus == ValidationStatus.MISSING_SOT ||
                validationStatus == ValidationStatus.ERROR);
    }
    
    public String getTrackingKey() {
        return String.format("%s_%s_%s_%s_%s_%s", 
                           tenantId, serviceName, subServiceName, 
                           processingDate.toString(), fileType, direction);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public LocalDate getProcessingDate() { return processingDate; }
    public void setProcessingDate(LocalDate processingDate) { this.processingDate = processingDate; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public TransferDirection getDirection() { return direction; }
    public void setDirection(TransferDirection direction) { this.direction = direction; }
    
    public Integer getExpectedCount() { return expectedCount; }
    public void setExpectedCount(Integer expectedCount) { this.expectedCount = expectedCount; }
    
    public Integer getActualCount() { return actualCount; }
    public void setActualCount(Integer actualCount) { this.actualCount = actualCount; }
    
    public Boolean getSotReceived() { return sotReceived; }
    public void setSotReceived(Boolean sotReceived) { this.sotReceived = sotReceived; }
    
    public Boolean getEotReceived() { return eotReceived; }
    public void setEotReceived(Boolean eotReceived) { this.eotReceived = eotReceived; }
    
    public Integer getEotCountValue() { return eotCountValue; }
    public void setEotCountValue(Integer eotCountValue) { this.eotCountValue = eotCountValue; }
    
    public String getEotFieldPath() { return eotFieldPath; }
    public void setEotFieldPath(String eotFieldPath) { this.eotFieldPath = eotFieldPath; }
    
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }
    
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    
    public Integer getDiscrepancyCount() { return discrepancyCount; }
    public void setDiscrepancyCount(Integer discrepancyCount) { this.discrepancyCount = discrepancyCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}