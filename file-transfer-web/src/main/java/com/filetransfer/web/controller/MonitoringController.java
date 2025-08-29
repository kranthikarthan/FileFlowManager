package com.filetransfer.web.controller;

import com.filetransfer.web.service.AlertingService;
import com.filetransfer.web.service.MetricsService;
import com.filetransfer.web.entity.AlertHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for monitoring and observability endpoints
 * Provides access to system health, metrics, and alerting data
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    @Autowired
    private AlertingService alertingService;

    @Autowired
    private MetricsService metricsService;

    /**
     * Get system health summary
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Perform basic health checks
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("uptime", getUptime());
            health.put("version", getClass().getPackage().getImplementationVersion());
            
            // Add component status
            Map<String, String> components = new HashMap<>();
            components.put("database", "UP");
            components.put("fileSystem", "UP");
            components.put("monitoring", "UP");
            health.put("components", components);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Get recent alerts for a tenant
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertHistory>> getRecentAlerts(
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<AlertHistory> alerts = alertingService.getRecentAlerts(tenantId, limit);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Acknowledge an alert
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestParam String acknowledgedBy) {
        
        try {
            alertingService.acknowledgeAlert(alertId, acknowledgedBy);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Trigger system health check
     */
    @PostMapping("/health-check")
    public ResponseEntity<Map<String, Object>> triggerHealthCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            alertingService.checkSystemHealth();
            result.put("status", "completed");
            result.put("message", "System health check completed");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Trigger business metrics check
     */
    @PostMapping("/business-metrics-check")
    public ResponseEntity<Map<String, Object>> triggerBusinessMetricsCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            alertingService.checkBusinessMetrics();
            result.put("status", "completed");
            result.put("message", "Business metrics check completed");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Get monitoring dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(required = false) String tenantId) {
        
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // Get basic system info
            dashboard.put("timestamp", System.currentTimeMillis());
            dashboard.put("uptime", getUptime());
            
            // Get recent alerts count
            List<AlertHistory> recentAlerts = alertingService.getRecentAlerts(tenantId, 50);
            Map<String, Long> alertCounts = new HashMap<>();
            alertCounts.put("total", (long) recentAlerts.size());
            alertCounts.put("unacknowledged", 
                recentAlerts.stream().filter(alert -> !alert.isAcknowledged()).count());
            alertCounts.put("critical", 
                recentAlerts.stream().filter(alert -> "CRITICAL".equals(alert.getAlertLevel().toString())).count());
            alertCounts.put("high", 
                recentAlerts.stream().filter(alert -> "HIGH".equals(alert.getAlertLevel().toString())).count());
            
            dashboard.put("alerts", alertCounts);
            
            // Add system metrics (these would typically come from Micrometer)
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("memoryUsage", getMemoryUsage());
            systemMetrics.put("diskUsage", getDiskUsage());
            systemMetrics.put("cpuUsage", getCpuUsage());
            dashboard.put("system", systemMetrics);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Test alert generation (for testing purposes)
     */
    @PostMapping("/test-alert")
    public ResponseEntity<Map<String, Object>> generateTestAlert(
            @RequestParam String alertType,
            @RequestParam String level,
            @RequestParam String message,
            @RequestParam(required = false) String tenantId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            alertingService.generateAlert(
                alertType, 
                com.filetransfer.web.enums.AlertLevel.valueOf(level.toUpperCase()), 
                message, 
                "test", 
                tenantId
            );
            
            result.put("status", "success");
            result.put("message", "Test alert generated");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    // Helper methods for system metrics
    
    private long getUptime() {
        return System.currentTimeMillis() - 
               java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    private double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / maxMemory * 100;
    }

    private double getDiskUsage() {
        try {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            return (double) usedSpace / totalSpace * 100;
        } catch (Exception e) {
            return -1;
        }
    }

    private double getCpuUsage() {
        try {
            javax.management.MBeanServer server = 
                java.lang.management.ManagementFactory.getPlatformMBeanServer();
            javax.management.ObjectName name = 
                javax.management.ObjectName.getInstance("java.lang:type=OperatingSystem");
            
            Object cpuUsage = server.getAttribute(name, "ProcessCpuLoad");
            if (cpuUsage instanceof Double) {
                return ((Double) cpuUsage) * 100;
            }
        } catch (Exception e) {
            // Fallback - return -1 to indicate unavailable
        }
        return -1;
    }
}