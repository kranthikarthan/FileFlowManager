package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_configurations")
public class AlertConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "service_name")
    private String serviceName;
    
    @Column(name = "sub_service_name")
    private String subServiceName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
    
    @Column(name = "alert_duration_minutes", nullable = false)
    private Integer alertDurationMinutes;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
    
    @Column(name = "email_recipients")
    private String emailRecipients;
    
    @Column(name = "notification_channels", columnDefinition = "JSON")
    private String notificationChannels;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    public enum AlertType {
        CUT_OFF_MISSED,
        EOT_NOT_RECEIVED,
        PROCESSING_FAILED
    }
    
    // Constructors
    public AlertConfiguration() {}
    
    public AlertConfiguration(String tenantId, String serviceName, String subServiceName, 
                            AlertType alertType, Integer alertDurationMinutes) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.alertType = alertType;
        this.alertDurationMinutes = alertDurationMinutes;
        this.enabled = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getSubServiceName() {
        return subServiceName;
    }
    
    public void setSubServiceName(String subServiceName) {
        this.subServiceName = subServiceName;
    }
    
    public AlertType getAlertType() {
        return alertType;
    }
    
    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }
    
    public Integer getAlertDurationMinutes() {
        return alertDurationMinutes;
    }
    
    public void setAlertDurationMinutes(Integer alertDurationMinutes) {
        this.alertDurationMinutes = alertDurationMinutes;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getEmailRecipients() {
        return emailRecipients;
    }
    
    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }
    
    public String getNotificationChannels() {
        return notificationChannels;
    }
    
    public void setNotificationChannels(String notificationChannels) {
        this.notificationChannels = notificationChannels;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}