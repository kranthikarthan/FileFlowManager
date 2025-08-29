package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "cutoff_extensions")
public class CutOffExtension {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @Column(nullable = false)
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    @Column(nullable = false)
    @NotBlank(message = "Sub-service name is required")
    private String subServiceName;
    
    @Column(nullable = false)
    @NotNull(message = "Extension date is required")
    private LocalDate extensionDate;
    
    @Column(nullable = false)
    @NotNull(message = "Original cut-off time is required")
    private LocalTime originalCutOffTime;
    
    @Column(nullable = false)
    @NotNull(message = "Extended cut-off time is required")
    private LocalTime extendedCutOffTime;
    
    @Column(nullable = false)
    @NotBlank(message = "Reason is required")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionStatus status = ExtensionStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionPriority priority = ExtensionPriority.NORMAL;
    
    @Column(nullable = false)
    private String requestedBy;
    
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    
    @Column
    private String approvedBy;
    
    @Column
    private LocalDateTime approvedAt;
    
    @Column
    private String rejectedBy;
    
    @Column
    private LocalDateTime rejectedAt;
    
    @Column
    private String rejectionReason;
    
    @Column
    private LocalDateTime effectiveFrom;
    
    @Column
    private LocalDateTime effectiveUntil;
    
    @Column(nullable = false)
    private Boolean autoApproved = false;
    
    @Column
    private String approvalComments;
    
    @Column
    private String notificationsSent;
    
    public enum ExtensionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        ACTIVE,
        EXPIRED,
        CANCELLED
    }
    
    public enum ExtensionPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    // Constructors
    public CutOffExtension() {
        this.requestedAt = LocalDateTime.now();
    }
    
    public CutOffExtension(String tenantId, String serviceName, String subServiceName,
                          LocalDate extensionDate, LocalTime originalCutOffTime, 
                          LocalTime extendedCutOffTime, String reason, String requestedBy) {
        this();
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.extensionDate = extensionDate;
        this.originalCutOffTime = originalCutOffTime;
        this.extendedCutOffTime = extendedCutOffTime;
        this.reason = reason;
        this.requestedBy = requestedBy;
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
    
    public LocalDate getExtensionDate() { return extensionDate; }
    public void setExtensionDate(LocalDate extensionDate) { this.extensionDate = extensionDate; }
    
    public LocalTime getOriginalCutOffTime() { return originalCutOffTime; }
    public void setOriginalCutOffTime(LocalTime originalCutOffTime) { this.originalCutOffTime = originalCutOffTime; }
    
    public LocalTime getExtendedCutOffTime() { return extendedCutOffTime; }
    public void setExtendedCutOffTime(LocalTime extendedCutOffTime) { this.extendedCutOffTime = extendedCutOffTime; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public ExtensionStatus getStatus() { return status; }
    public void setStatus(ExtensionStatus status) { this.status = status; }
    
    public ExtensionPriority getPriority() { return priority; }
    public void setPriority(ExtensionPriority priority) { this.priority = priority; }
    
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }
    
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    
    public Boolean getAutoApproved() { return autoApproved; }
    public void setAutoApproved(Boolean autoApproved) { this.autoApproved = autoApproved; }
    
    public String getApprovalComments() { return approvalComments; }
    public void setApprovalComments(String approvalComments) { this.approvalComments = approvalComments; }
    
    public String getNotificationsSent() { return notificationsSent; }
    public void setNotificationsSent(String notificationsSent) { this.notificationsSent = notificationsSent; }
    
    // Business methods
    public void approve(String approvedBy, String comments) {
        this.status = ExtensionStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.approvalComments = comments;
        
        // Set effective times
        this.effectiveFrom = this.extensionDate.atTime(this.originalCutOffTime);
        this.effectiveUntil = this.extensionDate.atTime(this.extendedCutOffTime);
    }
    
    public void reject(String rejectedBy, String rejectionReason) {
        this.status = ExtensionStatus.REJECTED;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }
    
    public void activate() {
        if (this.status == ExtensionStatus.APPROVED) {
            this.status = ExtensionStatus.ACTIVE;
        }
    }
    
    public void expire() {
        if (this.status == ExtensionStatus.ACTIVE) {
            this.status = ExtensionStatus.EXPIRED;
        }
    }
    
    public void cancel() {
        if (this.status == ExtensionStatus.PENDING || this.status == ExtensionStatus.APPROVED) {
            this.status = ExtensionStatus.CANCELLED;
        }
    }
    
    public boolean isActive() {
        return status == ExtensionStatus.ACTIVE && 
               LocalDateTime.now().isAfter(effectiveFrom) && 
               LocalDateTime.now().isBefore(effectiveUntil);
    }
    
    public boolean isEffectiveForDateTime(LocalDateTime dateTime) {
        return status == ExtensionStatus.ACTIVE &&
               effectiveFrom != null && effectiveUntil != null &&
               dateTime.isAfter(effectiveFrom) && dateTime.isBefore(effectiveUntil);
    }
    
    public long getExtensionDurationMinutes() {
        if (originalCutOffTime != null && extendedCutOffTime != null) {
            return java.time.Duration.between(originalCutOffTime, extendedCutOffTime).toMinutes();
        }
        return 0;
    }
}