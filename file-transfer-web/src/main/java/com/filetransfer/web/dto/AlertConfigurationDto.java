package com.filetransfer.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public class AlertConfigurationDto {
    
    private Long id;
    
    @NotNull(message = "Tenant ID is required")
    private String tenantId;
    
    private String serviceName;
    private String subServiceName;
    
    @NotNull(message = "Alert type is required")
    private AlertType alertType;
    
    @NotNull(message = "Alert duration is required")
    @Min(value = 1, message = "Alert duration must be at least 1 minute")
    private Integer alertDurationMinutes;
    
    @NotNull(message = "Enabled status is required")
    private Boolean enabled;
    
    private String emailRecipients;
    private String notificationChannels;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    public enum AlertType {
        CUT_OFF_MISSED,
        EOT_NOT_RECEIVED,
        PROCESSING_FAILED
    }
    
    // Constructors
    public AlertConfigurationDto() {}
    
    public AlertConfigurationDto(String tenantId, String serviceName, String subServiceName, 
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