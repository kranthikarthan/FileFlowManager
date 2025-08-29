package com.filetransfer.web.service;

import com.filetransfer.web.entity.AlertHistory;
import com.filetransfer.web.enums.AlertLevel;
import com.filetransfer.web.repository.AlertHistoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring system health and generating alerts
 * Provides real-time alerting based on metrics and thresholds
 */
@Service
public class AlertingService {

    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);

    private final AlertHistoryRepository alertHistoryRepository;
    private final MetricsService metricsService;
    private final MeterRegistry meterRegistry;
    private final ScheduledExecutorService alertExecutor;
    
    // Alert thresholds (configurable via properties)
    @Value("${monitoring.alerts.file-transfer-failure-rate:0.1}")
    private double fileTransferFailureRateThreshold;
    
    @Value("${monitoring.alerts.validation-failure-rate:0.05}")
    private double validationFailureRateThreshold;
    
    @Value("${monitoring.alerts.response-time-ms:5000}")
    private double responseTimeThreshold;
    
    @Value("${monitoring.alerts.disk-usage-percent:90}")
    private double diskUsageThreshold;
    
    @Value("${monitoring.alerts.memory-usage-percent:85}")
    private double memoryUsageThreshold;
    
    // Alert suppression to prevent spam
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();
    private final int alertSuppressionMinutes = 15;

    @Autowired
    public AlertingService(AlertHistoryRepository alertHistoryRepository, 
                          MetricsService metricsService,
                          MeterRegistry meterRegistry) {
        this.alertHistoryRepository = alertHistoryRepository;
        this.metricsService = metricsService;
        this.meterRegistry = meterRegistry;
        this.alertExecutor = Executors.newScheduledThreadPool(2);
        
        // Start periodic health checks
        startPeriodicHealthChecks();
    }

    /**
     * Start periodic health monitoring
     */
    private void startPeriodicHealthChecks() {
        // Check system health every minute
        alertExecutor.scheduleAtFixedRate(this::checkSystemHealth, 1, 1, TimeUnit.MINUTES);
        
        // Check business metrics every 5 minutes
        alertExecutor.scheduleAtFixedRate(this::checkBusinessMetrics, 2, 5, TimeUnit.MINUTES);
        
        logger.info("Started periodic health checks for alerting");
    }

    /**
     * Check overall system health metrics
     */
    public void checkSystemHealth() {
        try {
            checkMemoryUsage();
            checkDiskSpace();
            checkResponseTimes();
            checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("Error during system health check", e);
            generateAlert("SYSTEM_HEALTH_CHECK_FAILED", AlertLevel.CRITICAL, 
                         "System health check failed: " + e.getMessage(), "system", null);
        }
    }

    /**
     * Check business-specific metrics
     */
    public void checkBusinessMetrics() {
        try {
            checkFileTransferMetrics();
            checkValidationMetrics();
            checkCutoffTimeViolations();
        } catch (Exception e) {
            logger.error("Error during business metrics check", e);
            generateAlert("BUSINESS_METRICS_CHECK_FAILED", AlertLevel.HIGH, 
                         "Business metrics check failed: " + e.getMessage(), "system", null);
        }
    }

    /**
     * Check memory usage
     */
    private void checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // Record metric
            meterRegistry.gauge("system.memory.usage.percent", memoryUsagePercent);
            
            if (memoryUsagePercent > memoryUsageThreshold) {
                String message = String.format("High memory usage: %.2f%% (threshold: %.2f%%)", 
                                             memoryUsagePercent, memoryUsageThreshold);
                generateAlert("HIGH_MEMORY_USAGE", AlertLevel.HIGH, message, "system", null);
            }
        } catch (Exception e) {
            logger.error("Error checking memory usage", e);
        }
    }

    /**
     * Check disk space
     */
    private void checkDiskSpace() {
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double diskUsagePercent = (double) usedSpace / totalSpace * 100;
            
            // Record metric
            meterRegistry.gauge("system.disk.usage.percent", diskUsagePercent);
            
            if (diskUsagePercent > diskUsageThreshold) {
                String message = String.format("High disk usage: %.2f%% (threshold: %.2f%%)", 
                                             diskUsagePercent, diskUsageThreshold);
                generateAlert("HIGH_DISK_USAGE", AlertLevel.HIGH, message, "system", null);
            }
        } catch (Exception e) {
            logger.error("Error checking disk space", e);
        }
    }

    /**
     * Check response times
     */
    private void checkResponseTimes() {
        try {
            // Get average response time from Micrometer
            double avgResponseTime = meterRegistry.find("http.server.requests")
                .timer()
                .map(timer -> timer.mean(TimeUnit.MILLISECONDS))
                .orElse(0.0);
                
            if (avgResponseTime > responseTimeThreshold) {
                String message = String.format("High response time: %.2f ms (threshold: %.2f ms)", 
                                             avgResponseTime, responseTimeThreshold);
                generateAlert("HIGH_RESPONSE_TIME", AlertLevel.MEDIUM, message, "system", null);
            }
        } catch (Exception e) {
            logger.error("Error checking response times", e);
        }
    }

    /**
     * Check database health
     */
    private void checkDatabaseHealth() {
        try {
            // This would typically check database connection pool metrics
            // For now, we'll use a simple check
            double dbConnectionTime = meterRegistry.find("database.query.time")
                .timer()
                .map(timer -> timer.mean(TimeUnit.MILLISECONDS))
                .orElse(0.0);
                
            if (dbConnectionTime > 1000) { // 1 second threshold
                String message = String.format("Slow database queries: %.2f ms average", dbConnectionTime);
                generateAlert("SLOW_DATABASE_QUERIES", AlertLevel.MEDIUM, message, "system", null);
            }
        } catch (Exception e) {
            logger.error("Error checking database health", e);
        }
    }

    /**
     * Check file transfer metrics for anomalies
     */
    private void checkFileTransferMetrics() {
        try {
            // Get file transfer success/failure counts
            double successCount = meterRegistry.find("file.transfer.successes")
                .counter()
                .map(counter -> counter.count())
                .orElse(0.0);
                
            double failureCount = meterRegistry.find("file.transfer.failures")
                .counter()
                .map(counter -> counter.count())
                .orElse(0.0);
                
            double totalCount = successCount + failureCount;
            
            if (totalCount > 0) {
                double failureRate = failureCount / totalCount;
                
                if (failureRate > fileTransferFailureRateThreshold) {
                    String message = String.format("High file transfer failure rate: %.2f%% (threshold: %.2f%%)", 
                                                 failureRate * 100, fileTransferFailureRateThreshold * 100);
                    generateAlert("HIGH_FILE_TRANSFER_FAILURE_RATE", AlertLevel.HIGH, message, "business", null);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking file transfer metrics", e);
        }
    }

    /**
     * Check validation metrics for anomalies
     */
    private void checkValidationMetrics() {
        try {
            double validationSuccesses = meterRegistry.find("file.validation.successes")
                .counter()
                .map(counter -> counter.count())
                .orElse(0.0);
                
            double validationFailures = meterRegistry.find("file.validation.failures")
                .counter()
                .map(counter -> counter.count())
                .orElse(0.0);
                
            double totalValidations = validationSuccesses + validationFailures;
            
            if (totalValidations > 0) {
                double failureRate = validationFailures / totalValidations;
                
                if (failureRate > validationFailureRateThreshold) {
                    String message = String.format("High validation failure rate: %.2f%% (threshold: %.2f%%)", 
                                                 failureRate * 100, validationFailureRateThreshold * 100);
                    generateAlert("HIGH_VALIDATION_FAILURE_RATE", AlertLevel.MEDIUM, message, "business", null);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking validation metrics", e);
        }
    }

    /**
     * Check for cutoff time violations
     */
    private void checkCutoffTimeViolations() {
        try {
            // This would check for pending file transfers past cutoff times
            // Implementation would depend on your specific business logic
            logger.debug("Checking cutoff time violations");
        } catch (Exception e) {
            logger.error("Error checking cutoff time violations", e);
        }
    }

    /**
     * Generate an alert with suppression logic
     */
    public void generateAlert(String alertType, AlertLevel level, String message, 
                            String category, String tenantId) {
        
        String alertKey = alertType + "_" + (tenantId != null ? tenantId : "system");
        
        // Check if this alert was recently sent (suppression)
        LocalDateTime lastAlert = lastAlertTime.get(alertKey);
        LocalDateTime now = LocalDateTime.now();
        
        if (lastAlert != null && 
            lastAlert.plusMinutes(alertSuppressionMinutes).isAfter(now)) {
            logger.debug("Alert suppressed: {} (last sent: {})", alertType, lastAlert);
            return;
        }
        
        try {
            // Create alert history record
            AlertHistory alert = new AlertHistory();
            alert.setTenantId(tenantId != null ? tenantId : "system");
            alert.setAlertLevel(level);
            alert.setAlertType(alertType);
            alert.setMessage(message);
            alert.setCategory(category);
            alert.setGeneratedAt(now);
            alert.setAcknowledged(false);
            
            alertHistoryRepository.save(alert);
            
            // Record metrics
            metricsService.recordAlertGenerated(
                tenantId != null ? tenantId : "system", 
                level.toString(), 
                alertType
            );
            
            // Update suppression tracking
            lastAlertTime.put(alertKey, now);
            
            // Send notifications (implement based on your notification requirements)
            sendAlertNotification(alert);
            
            logger.warn("Generated alert: {} - {} (Level: {})", alertType, message, level);
            
        } catch (Exception e) {
            logger.error("Failed to generate alert: " + alertType, e);
        }
    }

    /**
     * Send alert notification (email, Slack, PagerDuty, etc.)
     */
    private void sendAlertNotification(AlertHistory alert) {
        // Implement notification logic based on your requirements
        // Examples:
        // - Send email for CRITICAL alerts
        // - Send Slack message for HIGH/MEDIUM alerts
        // - Create PagerDuty incident for CRITICAL alerts
        // - Send SMS for critical system alerts
        
        logger.info("Alert notification would be sent: {} - {}", 
                   alert.getAlertType(), alert.getMessage());
    }

    /**
     * Get recent alerts for dashboard display
     */
    public List<AlertHistory> getRecentAlerts(String tenantId, int limit) {
        if (tenantId != null) {
            return alertHistoryRepository.findTop10ByTenantIdOrderByGeneratedAtDesc(tenantId);
        } else {
            return alertHistoryRepository.findTop10ByOrderByGeneratedAtDesc();
        }
    }

    /**
     * Acknowledge an alert
     */
    public void acknowledgeAlert(Long alertId, String acknowledgedBy) {
        try {
            AlertHistory alert = alertHistoryRepository.findById(alertId).orElse(null);
            if (alert != null) {
                alert.setAcknowledged(true);
                alert.setAcknowledgedBy(acknowledgedBy);
                alert.setAcknowledgedAt(LocalDateTime.now());
                alertHistoryRepository.save(alert);
                
                logger.info("Alert acknowledged: {} by {}", alertId, acknowledgedBy);
            }
        } catch (Exception e) {
            logger.error("Failed to acknowledge alert: " + alertId, e);
        }
    }

    /**
     * Shutdown the alerting service
     */
    public void shutdown() {
        if (alertExecutor != null && !alertExecutor.isShutdown()) {
            alertExecutor.shutdown();
            try {
                if (!alertExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    alertExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                alertExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}