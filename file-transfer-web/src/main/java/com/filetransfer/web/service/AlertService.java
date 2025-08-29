package com.filetransfer.web.service;

import com.filetransfer.web.dto.AlertConfigurationDto;
import com.filetransfer.web.dto.AlertHistoryDto;
import com.filetransfer.web.entity.AlertConfiguration;
import com.filetransfer.web.entity.AlertHistory;
import com.filetransfer.web.repository.AlertConfigurationRepository;
import com.filetransfer.web.repository.AlertHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Autowired
    private AlertConfigurationRepository alertConfigurationRepository;
    
    @Autowired
    private AlertHistoryRepository alertHistoryRepository;
    
    @Autowired
    private SecurityContextService securityContextService;
    
    // Alert Configuration Methods
    public List<AlertConfigurationDto> getAlertConfigurationsForTenant(String tenantId) {
        return alertConfigurationRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertConfigurationDto> getActiveAlertConfigurationsForTenant(String tenantId) {
        return alertConfigurationRepository.findActiveAlertsForTenant(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertConfigurationDto> getAlertConfigurationsForService(String tenantId, String serviceName, String subServiceName) {
        return alertConfigurationRepository.findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<AlertConfigurationDto> getAlertConfigurationById(Long id) {
        return alertConfigurationRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public AlertConfigurationDto createAlertConfiguration(AlertConfigurationDto alertConfigDto) {
        AlertConfiguration alertConfig = convertToEntity(alertConfigDto);
        alertConfig.setCreatedAt(LocalDateTime.now());
        alertConfig.setCreatedBy(securityContextService.getCurrentUserId());
        
        AlertConfiguration savedAlertConfig = alertConfigurationRepository.save(alertConfig);
        return convertToDto(savedAlertConfig);
    }
    
    public AlertConfigurationDto updateAlertConfiguration(Long id, AlertConfigurationDto alertConfigDto) {
        AlertConfiguration existingAlertConfig = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert configuration not found with id: " + id));
        
        existingAlertConfig.setTenantId(alertConfigDto.getTenantId());
        existingAlertConfig.setServiceName(alertConfigDto.getServiceName());
        existingAlertConfig.setSubServiceName(alertConfigDto.getSubServiceName());
        existingAlertConfig.setAlertType(convertToEntityAlertType(alertConfigDto.getAlertType()));
        existingAlertConfig.setAlertDurationMinutes(alertConfigDto.getAlertDurationMinutes());
        existingAlertConfig.setEnabled(alertConfigDto.getEnabled());
        existingAlertConfig.setEmailRecipients(alertConfigDto.getEmailRecipients());
        existingAlertConfig.setNotificationChannels(alertConfigDto.getNotificationChannels());
        existingAlertConfig.setUpdatedAt(LocalDateTime.now());
        existingAlertConfig.setUpdatedBy(securityContextService.getCurrentUserId());
        
        AlertConfiguration savedAlertConfig = alertConfigurationRepository.save(existingAlertConfig);
        return convertToDto(savedAlertConfig);
    }
    
    public void deleteAlertConfiguration(Long id) {
        if (!alertConfigurationRepository.existsById(id)) {
            throw new IllegalArgumentException("Alert configuration not found with id: " + id);
        }
        alertConfigurationRepository.deleteById(id);
    }
    
    // Alert History Methods
    public List<AlertHistoryDto> getAlertHistoryForTenant(String tenantId) {
        return alertHistoryRepository.findByTenantIdOrderBySentAtDesc(tenantId).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertHistoryDto> getAlertHistoryForService(String tenantId, String serviceName, String subServiceName) {
        return alertHistoryRepository.findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertHistoryDto> getUnacknowledgedAlerts(String tenantId) {
        return alertHistoryRepository.findUnacknowledgedAlerts(tenantId).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public AlertHistoryDto createAlertHistory(AlertHistoryDto alertHistoryDto) {
        AlertHistory alertHistory = convertToHistoryEntity(alertHistoryDto);
        alertHistory.setSentAt(LocalDateTime.now());
        
        AlertHistory savedAlertHistory = alertHistoryRepository.save(alertHistory);
        return convertToHistoryDto(savedAlertHistory);
    }
    
    public AlertHistoryDto acknowledgeAlert(Long id, String acknowledgedBy) {
        AlertHistory alertHistory = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert history not found with id: " + id));
        
        alertHistory.setAcknowledgedAt(LocalDateTime.now());
        alertHistory.setAcknowledgedBy(acknowledgedBy);
        
        AlertHistory savedAlertHistory = alertHistoryRepository.save(alertHistory);
        return convertToHistoryDto(savedAlertHistory);
    }
    
    public void sendCutOffAlert(String tenantId, String serviceName, String subServiceName, String message) {
        List<AlertConfiguration> alertConfigs = alertConfigurationRepository.findActiveAlertsForService(
                tenantId, serviceName, subServiceName, AlertConfiguration.AlertType.CUT_OFF_MISSED);
        
        for (AlertConfiguration alertConfig : alertConfigs) {
            AlertHistoryDto alertHistoryDto = new AlertHistoryDto(
                    tenantId, serviceName, subServiceName, 
                    convertToDtoAlertType(alertConfig.getAlertType()), 
                    message, AlertHistoryDto.AlertLevel.WARNING);
            
            createAlertHistory(alertHistoryDto);
        }
    }
    
    // Conversion methods
    private AlertConfigurationDto convertToDto(AlertConfiguration alertConfig) {
        AlertConfigurationDto dto = new AlertConfigurationDto();
        dto.setId(alertConfig.getId());
        dto.setTenantId(alertConfig.getTenantId());
        dto.setServiceName(alertConfig.getServiceName());
        dto.setSubServiceName(alertConfig.getSubServiceName());
        dto.setAlertType(convertToDtoAlertType(alertConfig.getAlertType()));
        dto.setAlertDurationMinutes(alertConfig.getAlertDurationMinutes());
        dto.setEnabled(alertConfig.getEnabled());
        dto.setEmailRecipients(alertConfig.getEmailRecipients());
        dto.setNotificationChannels(alertConfig.getNotificationChannels());
        dto.setCreatedAt(alertConfig.getCreatedAt());
        dto.setUpdatedAt(alertConfig.getUpdatedAt());
        dto.setCreatedBy(alertConfig.getCreatedBy());
        dto.setUpdatedBy(alertConfig.getUpdatedBy());
        return dto;
    }
    
    private AlertConfiguration convertToEntity(AlertConfigurationDto dto) {
        AlertConfiguration alertConfig = new AlertConfiguration();
        alertConfig.setId(dto.getId());
        alertConfig.setTenantId(dto.getTenantId());
        alertConfig.setServiceName(dto.getServiceName());
        alertConfig.setSubServiceName(dto.getSubServiceName());
        alertConfig.setAlertType(convertToEntityAlertType(dto.getAlertType()));
        alertConfig.setAlertDurationMinutes(dto.getAlertDurationMinutes());
        alertConfig.setEnabled(dto.getEnabled());
        alertConfig.setEmailRecipients(dto.getEmailRecipients());
        alertConfig.setNotificationChannels(dto.getNotificationChannels());
        alertConfig.setCreatedAt(dto.getCreatedAt());
        alertConfig.setUpdatedAt(dto.getUpdatedAt());
        alertConfig.setCreatedBy(dto.getCreatedBy());
        alertConfig.setUpdatedBy(dto.getUpdatedBy());
        return alertConfig;
    }
    
    private AlertHistoryDto convertToHistoryDto(AlertHistory alertHistory) {
        AlertHistoryDto dto = new AlertHistoryDto();
        dto.setId(alertHistory.getId());
        dto.setTenantId(alertHistory.getTenantId());
        dto.setServiceName(alertHistory.getServiceName());
        dto.setSubServiceName(alertHistory.getSubServiceName());
        dto.setAlertType(convertToDtoAlertType(alertHistory.getAlertType()));
        dto.setAlertMessage(alertHistory.getAlertMessage());
        dto.setAlertLevel(convertToDtoAlertLevel(alertHistory.getAlertLevel()));
        dto.setSentAt(alertHistory.getSentAt());
        dto.setAcknowledgedAt(alertHistory.getAcknowledgedAt());
        dto.setAcknowledgedBy(alertHistory.getAcknowledgedBy());
        return dto;
    }
    
    private AlertHistory convertToHistoryEntity(AlertHistoryDto dto) {
        AlertHistory alertHistory = new AlertHistory();
        alertHistory.setId(dto.getId());
        alertHistory.setTenantId(dto.getTenantId());
        alertHistory.setServiceName(dto.getServiceName());
        alertHistory.setSubServiceName(dto.getSubServiceName());
        alertHistory.setAlertType(convertToEntityAlertType(dto.getAlertType()));
        alertHistory.setAlertMessage(dto.getAlertMessage());
        alertHistory.setAlertLevel(convertToEntityAlertLevel(dto.getAlertLevel()));
        alertHistory.setSentAt(dto.getSentAt());
        alertHistory.setAcknowledgedAt(dto.getAcknowledgedAt());
        alertHistory.setAcknowledgedBy(dto.getAcknowledgedBy());
        return alertHistory;
    }
    
    private AlertConfigurationDto.AlertType convertToDtoAlertType(AlertConfiguration.AlertType alertType) {
        return AlertConfigurationDto.AlertType.valueOf(alertType.name());
    }
    
    private AlertConfiguration.AlertType convertToEntityAlertType(AlertConfigurationDto.AlertType alertType) {
        return AlertConfiguration.AlertType.valueOf(alertType.name());
    }
    
    private AlertHistoryDto.AlertLevel convertToDtoAlertLevel(AlertHistory.AlertLevel alertLevel) {
        return AlertHistoryDto.AlertLevel.valueOf(alertLevel.name());
    }
    
    private AlertHistory.AlertLevel convertToEntityAlertLevel(AlertHistoryDto.AlertLevel alertLevel) {
        return AlertHistory.AlertLevel.valueOf(alertLevel.name());
    }

    /**
     * Create an alert for EOT validation mismatch
     */
    public void createAlert(FileProcessingTrackingService.AlertDto alert) {
        try {
            // Convert AlertDto to AlertHistory entity and save to database
            AlertHistory alertHistory = new AlertHistory();
            alertHistory.setTenantId(alert.getTenantId());
            alertHistory.setServiceName(alert.getServiceType());
            // alertHistory.setSubServiceName(null); // Could be extracted from serviceType if needed
            
            // Map string alert type to enum if needed
            AlertConfiguration.AlertType alertTypeEnum = mapStringToAlertType(alert.getAlertType());
            alertHistory.setAlertType(alertTypeEnum);
            
            alertHistory.setAlertMessage(alert.getMessage());
            alertHistory.setSentAt(alert.getCreatedAt());
            alertHistory.setAcknowledgedAt(null); // Not acknowledged initially
            
            // Map severity to AlertLevel enum
            alertHistory.setAlertLevel(mapSeverityToAlertLevel(alert.getSeverity()));
            
            // Save to database
            AlertHistory savedAlert = alertHistoryRepository.save(alertHistory);
            logger.info("Alert stored in database with ID: {}", savedAlert.getId());
            
            // Send notifications based on alert configuration
            sendAlertNotifications(savedAlert);
            
            // Log for immediate visibility
            logger.warn("ALERT [{}] {} - {} at {}", 
                       alert.getSeverity(), alert.getAlertType(), alert.getMessage(), alert.getCreatedAt());
                       
        } catch (Exception e) {
            logger.error("Failed to create and store alert: {}", e.getMessage(), e);
            // Fallback to logging only if storage fails
            logger.error("FALLBACK ALERT [{}] {} - {} at {}", 
                        alert.getSeverity(), alert.getAlertType(), alert.getMessage(), alert.getCreatedAt());
        }
    }
    
    /**
     * Send alert notifications based on configuration
     */
    private void sendAlertNotifications(AlertHistory alert) {
        try {
            // Find alert configurations for this tenant and service type
            List<AlertConfiguration> configs = alertConfigurationRepository
                .findByTenantIdAndServiceTypeAndEnabled(alert.getTenantId(), alert.getServiceName(), true);
            
            for (AlertConfiguration config : configs) {
                if (shouldTriggerAlert(alert, config)) {
                    sendNotificationForConfig(alert, config);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to send alert notifications for alert {}: {}", alert.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Check if alert should trigger based on configuration
     */
    private boolean shouldTriggerAlert(AlertHistory alert, AlertConfiguration config) {
        // Check if alert type matches
        if (!config.getAlertType().equals(alert.getAlertType()) && 
            !"ALL".equals(config.getAlertType())) {
            return false;
        }
        
        // Check alert level threshold
        AlertHistory.AlertLevel alertLevel = alert.getAlertLevel();
        AlertConfiguration.AlertLevel configLevel = config.getAlertLevel();
        
        // Only trigger if alert level is at or above configured threshold
        return isAlertLevelAtOrAboveThreshold(alertLevel, configLevel);
    }
    
    private boolean isAlertLevelAtOrAboveThreshold(AlertHistory.AlertLevel alertLevel, AlertConfiguration.AlertLevel threshold) {
        // Define severity order: INFO < WARNING < ERROR < CRITICAL
        int alertSeverity = getSeverityValue(alertLevel);
        int thresholdSeverity = getSeverityValue(threshold);
        
        return alertSeverity >= thresholdSeverity;
    }
    
    private int getSeverityValue(Enum<?> level) {
        String levelName = level.name();
        switch (levelName) {
            case "INFO": return 1;
            case "WARNING": return 2;
            case "ERROR": return 3;
            case "CRITICAL": return 4;
            default: return 2; // Default to WARNING
        }
    }
    
    /**
     * Send notification for specific configuration
     */
    private void sendNotificationForConfig(AlertHistory alert, AlertConfiguration config) {
        try {
            String notificationChannels = config.getNotificationChannels();
            
            if (notificationChannels != null && !notificationChannels.isEmpty()) {
                String[] channels = notificationChannels.split(",");
                
                for (String channel : channels) {
                    channel = channel.trim().toUpperCase();
                    
                    switch (channel) {
                        case "EMAIL":
                            sendEmailNotification(alert, config);
                            break;
                        case "SMS":
                            sendSmsNotification(alert, config);
                            break;
                        case "WEBHOOK":
                            sendWebhookNotification(alert, config);
                            break;
                        case "DASHBOARD":
                            updateDashboardNotification(alert, config);
                            break;
                        default:
                            logger.warn("Unknown notification channel: {}", channel);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to send notification for config {}: {}", config.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Send email notification
     */
    private void sendEmailNotification(AlertHistory alert, AlertConfiguration config) {
        try {
            String emailRecipients = config.getEmailRecipients();
            if (emailRecipients == null || emailRecipients.isEmpty()) {
                logger.warn("No email recipients configured for alert config {}", config.getId());
                return;
            }
            
            String subject = String.format("[%s] %s Alert - %s", 
                                         alert.getTenantId(), alert.getAlertLevel(), alert.getAlertType());
            
            String body = buildEmailBody(alert, config);
            
            // Log email details (in production, this would use actual email service)
            logger.info("EMAIL NOTIFICATION - To: {}, Subject: {}, Body: {}", 
                       emailRecipients, subject, body);
            
            // TODO: Integrate with actual email service (e.g., SendGrid, AWS SES, SMTP)
            // emailService.sendEmail(emailRecipients.split(","), subject, body);
            
        } catch (Exception e) {
            logger.error("Failed to send email notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send SMS notification
     */
    private void sendSmsNotification(AlertHistory alert, AlertConfiguration config) {
        try {
            // SMS implementation would require phone numbers in config
            String message = String.format("ALERT [%s]: %s - %s", 
                                          alert.getAlertLevel(), alert.getAlertType(), alert.getMessage());
            
            logger.info("SMS NOTIFICATION - Message: {}", message);
            
            // TODO: Integrate with SMS service (e.g., Twilio, AWS SNS)
            // smsService.sendSms(phoneNumbers, message);
            
        } catch (Exception e) {
            logger.error("Failed to send SMS notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send webhook notification
     */
    private void sendWebhookNotification(AlertHistory alert, AlertConfiguration config) {
        try {
            // Webhook URL would be stored in config or environment
            String webhookUrl = System.getProperty("alert.webhook.url", "http://localhost:8080/api/webhooks/alerts");
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("alertId", alert.getId());
            payload.put("tenantId", alert.getTenantId());
            payload.put("serviceType", alert.getServiceType());
            payload.put("alertType", alert.getAlertType());
            payload.put("severity", alert.getAlertLevel().toString());
            payload.put("message", alert.getMessage());
            payload.put("createdAt", alert.getCreatedAt().toString());
            
            logger.info("WEBHOOK NOTIFICATION - URL: {}, Payload: {}", webhookUrl, payload);
            
            // TODO: Use RestTemplate to send HTTP POST to webhook URL
            // restTemplate.postForEntity(webhookUrl, payload, String.class);
            
        } catch (Exception e) {
            logger.error("Failed to send webhook notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update dashboard notification
     */
    private void updateDashboardNotification(AlertHistory alert, AlertConfiguration config) {
        try {
            // Dashboard update would use WebSocket or Server-Sent Events
            logger.info("DASHBOARD NOTIFICATION - Alert ID: {}, Type: {}, Severity: {}", 
                       alert.getId(), alert.getAlertType(), alert.getAlertLevel());
            
            // TODO: Implement WebSocket/SSE for real-time dashboard updates
            // dashboardService.broadcastAlert(alert);
            
        } catch (Exception e) {
            logger.error("Failed to update dashboard notification: {}", e.getMessage(), e);
        }
    }
    
    private String buildEmailBody(AlertHistory alert, AlertConfiguration config) {
        StringBuilder body = new StringBuilder();
        body.append("An alert has been triggered in the File Transfer Management System.\n\n");
        body.append("Alert Details:\n");
        body.append("- Alert ID: ").append(alert.getId()).append("\n");
        body.append("- Tenant: ").append(alert.getTenantId()).append("\n");
        body.append("- Service: ").append(alert.getServiceType()).append("\n");
        body.append("- Alert Type: ").append(alert.getAlertType()).append("\n");
        body.append("- Severity: ").append(alert.getAlertLevel()).append("\n");
        body.append("- Message: ").append(alert.getMessage()).append("\n");
        body.append("- Created At: ").append(alert.getCreatedAt()).append("\n\n");
        
        body.append("Configuration Details:\n");
        body.append("- Configuration Name: ").append(config.getConfigName()).append("\n");
        body.append("- Description: ").append(config.getDescription()).append("\n\n");
        
        body.append("Please review this alert and take appropriate action if necessary.\n");
        body.append("You can view more details in the File Transfer Management Dashboard.");
        
        return body.toString();
    }
    
    private AlertHistory.AlertLevel mapSeverityToAlertLevel(String severity) {
        if (severity == null) {
            return AlertHistory.AlertLevel.WARNING;
        }
        
        try {
            return AlertHistory.AlertLevel.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown severity level: {}, defaulting to WARNING", severity);
            return AlertHistory.AlertLevel.WARNING;
        }
    }
    
    /**
     * Mark an alert as resolved
     */
    public AlertHistoryDto acknowledgeAlert(Long alertId, String acknowledgmentNote) {
        AlertHistory alert = alertHistoryRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        
        if (alert.getAcknowledgedAt() != null) {
            throw new IllegalStateException("Alert is already acknowledged");
        }
        
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(securityContextService.getCurrentUserId());
        
        if (acknowledgmentNote != null && !acknowledgmentNote.trim().isEmpty()) {
            // You could add an acknowledgmentNote field to AlertHistory entity if needed
            logger.info("Alert {} acknowledged with note: {}", alertId, acknowledgmentNote);
        }
        
        AlertHistory savedAlert = alertHistoryRepository.save(alert);
        logger.info("Alert {} acknowledged by {}", alertId, alert.getAcknowledgedBy());
        
        return convertAlertHistoryToDto(savedAlert);
    }
    
    /**
     * Get unacknowledged alerts for tenant
     */
    public List<AlertHistoryDto> getUnacknowledgedAlertsForTenant(String tenantId) {
        return alertHistoryRepository.findByTenantIdAndAcknowledgedAtIsNull(tenantId).stream()
                .map(this::convertAlertHistoryToDto)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get alerts by severity for tenant
     */
    public List<AlertHistoryDto> getAlertsBySeverityForTenant(String tenantId, String severity) {
        AlertHistory.AlertLevel alertLevel;
        try {
            alertLevel = AlertHistory.AlertLevel.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid severity level: " + severity);
        }
        
        return alertHistoryRepository.findByTenantIdAndAlertLevel(tenantId, alertLevel).stream()
                .map(this::convertAlertHistoryToDto)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get alert statistics for tenant
     */
    public Map<String, Object> getAlertStatisticsForTenant(String tenantId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total alerts
        long totalAlerts = alertHistoryRepository.countByTenantId(tenantId);
        stats.put("totalAlerts", totalAlerts);
        
        // Unacknowledged alerts
        long unacknowledgedAlerts = alertHistoryRepository.countByTenantIdAndAcknowledgedAtIsNull(tenantId);
        stats.put("unacknowledgedAlerts", unacknowledgedAlerts);
        
        // Alerts by severity
        for (AlertHistory.AlertLevel level : AlertHistory.AlertLevel.values()) {
            long count = alertHistoryRepository.countByTenantIdAndAlertLevel(tenantId, level);
            stats.put(level.name().toLowerCase() + "Alerts", count);
        }
        
        // Recent alerts (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentAlerts = alertHistoryRepository.countByTenantIdAndSentAtAfter(tenantId, yesterday);
        stats.put("recentAlerts", recentAlerts);
        
        return stats;
    }
    
    private AlertConfiguration.AlertType mapStringToAlertType(String alertType) {
        if (alertType == null) {
            return AlertConfiguration.AlertType.SYSTEM; // Default fallback
        }
        
        try {
            // Handle common alert type mappings
            switch (alertType.toUpperCase()) {
                case "EOT_VALIDATION_MISMATCH":
                case "FILE_VALIDATION":
                case "VALIDATION":
                    return AlertConfiguration.AlertType.FILE_VALIDATION;
                case "FILE_TRANSFER":
                case "TRANSFER":
                    return AlertConfiguration.AlertType.FILE_TRANSFER;
                case "SYSTEM":
                case "SYSTEM_ERROR":
                    return AlertConfiguration.AlertType.SYSTEM;
                case "PERFORMANCE":
                case "PERFORMANCE_ISSUE":
                    return AlertConfiguration.AlertType.PERFORMANCE;
                default:
                    // Try direct enum mapping
                    return AlertConfiguration.AlertType.valueOf(alertType.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown alert type: {}, defaulting to SYSTEM", alertType);
            return AlertConfiguration.AlertType.SYSTEM;
        }
    }
}