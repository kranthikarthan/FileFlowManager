package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
public class AlertHistory {
    
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
    private AlertConfiguration.AlertType alertType;
    
    @Column(name = "alert_message", nullable = false)
    private String alertMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false)
    private AlertLevel alertLevel;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "acknowledged_by")
    private String acknowledgedBy;
    
    public enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    // Constructors
    public AlertHistory() {}
    
    public AlertHistory(String tenantId, String serviceName, String subServiceName, 
                       AlertConfiguration.AlertType alertType, String alertMessage, AlertLevel alertLevel) {
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
    
    public AlertConfiguration.AlertType getAlertType() {
        return alertType;
    }
    
    public void setAlertType(AlertConfiguration.AlertType alertType) {
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