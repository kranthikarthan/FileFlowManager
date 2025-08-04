package com.filetransfer.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AlertHistoryDto {
    
    private Long id;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    private String serviceName;
    private String subServiceName;
    
    @NotNull(message = "Alert type is required")
    private AlertConfigurationDto.AlertType alertType;
    
    @NotBlank(message = "Alert message is required")
    private String alertMessage;
    
    @NotNull(message = "Alert level is required")
    private AlertLevel alertLevel;
    
    private LocalDateTime sentAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    
    public enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    // Constructors
    public AlertHistoryDto() {}
    
    public AlertHistoryDto(String tenantId, String serviceName, String subServiceName, 
                          AlertConfigurationDto.AlertType alertType, String alertMessage, AlertLevel alertLevel) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.alertType = alertType;
        this.alertMessage = alertMessage;
        this.alertLevel = alertLevel;
        this.sentAt = LocalDateTime.now();
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
    
    public AlertConfigurationDto.AlertType getAlertType() {
        return alertType;
    }
    
    public void setAlertType(AlertConfigurationDto.AlertType alertType) {
        this.alertType = alertType;
    }
    
    public String getAlertMessage() {
        return alertMessage;
    }
    
    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }
    
    public AlertLevel getAlertLevel() {
        return alertLevel;
    }
    
    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }
    
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
    
    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }
    
    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }
}