package com.filetransfer.web.analytics.controller;

import com.filetransfer.web.analytics.model.AnalyticsModels.*;
import com.filetransfer.web.analytics.service.AnalyticsDataService;
import com.filetransfer.web.analytics.service.BusinessIntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Analytics Controller - REST API for analytics and business intelligence
 * Provides endpoints for real-time analytics, historical data, and BI reports
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Analytics and Business Intelligence API")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsDataService analyticsDataService;

    @Autowired
    private BusinessIntelligenceService businessIntelligenceService;

    /**
     * Get analytics summary for a tenant and date range
     */
    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary", description = "Retrieve analytics summary for specified tenant and date range")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting analytics summary for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        try {
            Map<String, Object> summary = analyticsDataService.getAnalyticsSummary(tenantId, startDate, endDate);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting analytics summary for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get trending analytics data
     */
    @GetMapping("/trends")
    @Operation(summary = "Get trending analytics", description = "Retrieve trending analytics data for visualization")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTrendingAnalytics(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Granularity (daily, weekly, monthly)") @RequestParam(defaultValue = "daily") String granularity) {
        
        logger.info("Getting trending analytics for tenant: {} from {} to {} with granularity: {}", 
                   tenantId, startDate, endDate, granularity);
        
        try {
            List<Map<String, Object>> trends = analyticsDataService.getTrendingAnalytics(tenantId, startDate, endDate, granularity);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.error("Error getting trending analytics for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service-level analytics breakdown
     */
    @GetMapping("/services")
    @Operation(summary = "Get service analytics", description = "Retrieve service-level analytics breakdown")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getServiceAnalytics(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting service analytics for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        try {
            List<Map<String, Object>> serviceAnalytics = analyticsDataService.getServiceAnalytics(tenantId, startDate, endDate);
            return ResponseEntity.ok(serviceAnalytics);
        } catch (Exception e) {
            logger.error("Error getting service analytics for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get real-time dashboard data
     */
    @GetMapping("/realtime/dashboard")
    @Operation(summary = "Get real-time dashboard", description = "Retrieve real-time analytics dashboard data")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRealTimeDashboard(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId) {
        
        logger.info("Getting real-time dashboard for tenant: {}", tenantId);
        
        try {
            Map<String, Object> dashboard = analyticsDataService.getRealTimeDashboard(tenantId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Error getting real-time dashboard for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Record analytics event (for real-time processing)
     */
    @PostMapping("/events")
    @Operation(summary = "Record analytics event", description = "Record a real-time analytics event")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> recordAnalyticsEvent(@RequestBody Map<String, Object> eventData) {
        
        try {
            String tenantId = (String) eventData.get("tenantId");
            AnalyticsEventType eventType = AnalyticsEventType.valueOf((String) eventData.get("eventType"));
            String serviceName = (String) eventData.get("serviceName");
            String subServiceName = (String) eventData.get("subServiceName");
            String fileName = (String) eventData.get("fileName");
            Long fileSizeBytes = eventData.get("fileSizeBytes") != null ? 
                Long.valueOf(eventData.get("fileSizeBytes").toString()) : null;
            Long processingTimeMs = eventData.get("processingTimeMs") != null ? 
                Long.valueOf(eventData.get("processingTimeMs").toString()) : null;
            TransferStatus status = eventData.get("transferStatus") != null ? 
                TransferStatus.valueOf((String) eventData.get("transferStatus")) : null;
            String errorCode = (String) eventData.get("errorCode");
            String errorMessage = (String) eventData.get("errorMessage");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
            
            analyticsDataService.recordAnalyticsEvent(tenantId, eventType, serviceName, subServiceName,
                fileName, fileSizeBytes, processingTimeMs, status, errorCode, errorMessage, metadata);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error recording analytics event", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate business intelligence report
     */
    @PostMapping("/reports/generate")
    @Operation(summary = "Generate BI report", description = "Generate a business intelligence report")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<BusinessIntelligenceReport>> generateReport(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Report type") @RequestParam ReportType reportType,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Created by") @RequestParam String createdBy) {
        
        logger.info("Generating BI report: {} for tenant: {} from {} to {}", reportType, tenantId, startDate, endDate);
        
        try {
            CompletableFuture<BusinessIntelligenceReport> reportFuture = 
                businessIntelligenceService.generateReport(tenantId, reportType, startDate, endDate, createdBy);
            
            return ResponseEntity.accepted().body(reportFuture);
        } catch (Exception e) {
            logger.error("Error generating BI report for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get or generate business intelligence report
     */
    @GetMapping("/reports")
    @Operation(summary = "Get BI report", description = "Get existing or generate new business intelligence report")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BusinessIntelligenceReport> getOrGenerateReport(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Report type") @RequestParam ReportType reportType,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Created by") @RequestParam String createdBy) {
        
        logger.info("Getting or generating BI report: {} for tenant: {} from {} to {}", 
                   reportType, tenantId, startDate, endDate);
        
        try {
            BusinessIntelligenceReport report = businessIntelligenceService
                .getOrGenerateReport(tenantId, reportType, startDate, endDate, createdBy);
            
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting BI report for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get predictive analytics
     */
    @GetMapping("/predictive")
    @Operation(summary = "Get predictive analytics", description = "Generate predictive analytics and forecasting")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPredictiveAnalytics(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Forecast days") @RequestParam(defaultValue = "30") int forecastDays) {
        
        logger.info("Getting predictive analytics for tenant: {} with {} days forecast", tenantId, forecastDays);
        
        try {
            Map<String, Object> predictions = businessIntelligenceService
                .generatePredictiveAnalytics(tenantId, forecastDays);
            
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("Error getting predictive analytics for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get capacity planning report
     */
    @GetMapping("/capacity-planning")
    @Operation(summary = "Get capacity planning", description = "Generate capacity planning analysis")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCapacityPlanningReport(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Planning months") @RequestParam(defaultValue = "6") int planningMonths) {
        
        logger.info("Getting capacity planning report for tenant: {} for {} months", tenantId, planningMonths);
        
        try {
            Map<String, Object> capacityPlan = businessIntelligenceService
                .generateCapacityPlanningReport(tenantId, planningMonths);
            
            return ResponseEntity.ok(capacityPlan);
        } catch (Exception e) {
            logger.error("Error getting capacity planning report for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get SLA compliance report
     */
    @GetMapping("/sla-compliance")
    @Operation(summary = "Get SLA compliance", description = "Generate SLA compliance analysis")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSlaComplianceReport(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting SLA compliance report for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        try {
            Map<String, Object> slaReport = businessIntelligenceService
                .generateSlaComplianceReport(tenantId, startDate, endDate);
            
            return ResponseEntity.ok(slaReport);
        } catch (Exception e) {
            logger.error("Error getting SLA compliance report for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get cost analysis report
     */
    @GetMapping("/cost-analysis")
    @Operation(summary = "Get cost analysis", description = "Generate cost analysis and optimization recommendations")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCostAnalysisReport(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting cost analysis report for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        try {
            Map<String, Object> costReport = businessIntelligenceService
                .generateCostAnalysisReport(tenantId, startDate, endDate);
            
            return ResponseEntity.ok(costReport);
        } catch (Exception e) {
            logger.error("Error getting cost analysis report for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get analytics data for specific date range
     */
    @GetMapping("/data")
    @Operation(summary = "Get analytics data", description = "Retrieve raw analytics data for specified date range")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FileTransferAnalytics>> getAnalyticsData(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting analytics data for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        try {
            List<FileTransferAnalytics> analyticsData = analyticsDataService.getAnalytics(tenantId, startDate, endDate);
            return ResponseEntity.ok(analyticsData);
        } catch (Exception e) {
            logger.error("Error getting analytics data for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get system health metrics
     */
    @GetMapping("/health")
    @Operation(summary = "Get system health", description = "Retrieve system health metrics and indicators")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemHealthMetrics(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId) {
        
        logger.info("Getting system health metrics for tenant: {}", tenantId);
        
        try {
            Map<String, Object> healthMetrics = analyticsDataService.getRealTimeDashboard(tenantId);
            
            // Add additional health indicators
            healthMetrics.put("systemStatus", determineSystemStatus(healthMetrics));
            healthMetrics.put("healthScore", calculateHealthScore(healthMetrics));
            
            return ResponseEntity.ok(healthMetrics);
        } catch (Exception e) {
            logger.error("Error getting system health metrics for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get analytics export data (CSV format)
     */
    @GetMapping("/export")
    @Operation(summary = "Export analytics data", description = "Export analytics data in CSV format")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> exportAnalyticsData(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Export format") @RequestParam(defaultValue = "csv") String format) {
        
        logger.info("Exporting analytics data for tenant: {} from {} to {} in format: {}", 
                   tenantId, startDate, endDate, format);
        
        try {
            List<FileTransferAnalytics> analyticsData = analyticsDataService.getAnalytics(tenantId, startDate, endDate);
            
            if ("csv".equalsIgnoreCase(format)) {
                String csvData = convertToCSV(analyticsData);
                return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=analytics_" + tenantId + "_" + startDate + "_to_" + endDate + ".csv")
                    .body(csvData);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Error exporting analytics data for tenant: {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Private helper methods

    private String determineSystemStatus(Map<String, Object> healthMetrics) {
        // Logic to determine system status based on health metrics
        Integer lastHourFailures = (Integer) healthMetrics.get("lastHourFailures");
        Integer lastHourEvents = (Integer) healthMetrics.get("lastHourEvents");
        
        if (lastHourFailures == null || lastHourEvents == null) {
            return "UNKNOWN";
        }
        
        if (lastHourEvents == 0) {
            return "IDLE";
        }
        
        double failureRate = lastHourEvents > 0 ? (double) lastHourFailures / lastHourEvents : 0.0;
        
        if (failureRate > 0.1) { // More than 10% failures
            return "DEGRADED";
        } else if (failureRate > 0.05) { // More than 5% failures
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    private double calculateHealthScore(Map<String, Object> healthMetrics) {
        // Logic to calculate overall health score (0-100)
        Integer lastHourEvents = (Integer) healthMetrics.get("lastHourEvents");
        Integer lastHourFailures = (Integer) healthMetrics.get("lastHourFailures");
        
        if (lastHourEvents == null || lastHourEvents == 0) {
            return 100.0; // No activity = healthy
        }
        
        if (lastHourFailures == null) {
            lastHourFailures = 0;
        }
        
        double successRate = 1.0 - ((double) lastHourFailures / lastHourEvents);
        return Math.max(0.0, Math.min(100.0, successRate * 100.0));
    }

    private String convertToCSV(List<FileTransferAnalytics> analyticsData) {
        StringBuilder csv = new StringBuilder();
        
        // CSV Header
        csv.append("Date,Tenant ID,Service Name,Sub-Service Name,Direction,File Type,Total Files,Successful Transfers,Failed Transfers,Total Data Volume (Bytes),Avg Processing Time (ms),Validation Errors,SLA Breaches,Processing Cost,Storage Cost,Bandwidth Cost\n");
        
        // CSV Data
        for (FileTransferAnalytics analytics : analyticsData) {
            csv.append(analytics.getAnalyticsDate()).append(",")
               .append(analytics.getTenantId()).append(",")
               .append(analytics.getServiceName() != null ? analytics.getServiceName() : "").append(",")
               .append(analytics.getSubServiceName() != null ? analytics.getSubServiceName() : "").append(",")
               .append(analytics.getDirection() != null ? analytics.getDirection() : "").append(",")
               .append(analytics.getFileType() != null ? analytics.getFileType() : "").append(",")
               .append(analytics.getTotalFiles()).append(",")
               .append(analytics.getSuccessfulTransfers()).append(",")
               .append(analytics.getFailedTransfers()).append(",")
               .append(analytics.getTotalDataVolumeBytes()).append(",")
               .append(analytics.getAvgProcessingTimeMs() != null ? analytics.getAvgProcessingTimeMs() : "").append(",")
               .append(analytics.getValidationErrors()).append(",")
               .append(analytics.getSlaBreaches()).append(",")
               .append(analytics.getEstimatedProcessingCost() != null ? analytics.getEstimatedProcessingCost() : "").append(",")
               .append(analytics.getStorageCost() != null ? analytics.getStorageCost() : "").append(",")
               .append(analytics.getBandwidthCost() != null ? analytics.getBandwidthCost() : "").append("\n");
        }
        
        return csv.toString();
    }
}