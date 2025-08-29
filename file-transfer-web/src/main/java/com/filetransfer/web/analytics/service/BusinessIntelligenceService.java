package com.filetransfer.web.analytics.service;

import com.filetransfer.web.analytics.model.AnalyticsModels.*;
import com.filetransfer.web.analytics.repository.BusinessIntelligenceReportRepository;
import com.filetransfer.web.analytics.repository.FileTransferAnalyticsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Business Intelligence Service - Advanced analytics and predictive insights
 * Generates comprehensive reports, trend analysis, and business recommendations
 */
@Service
@Transactional
public class BusinessIntelligenceService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessIntelligenceService.class);

    @Autowired
    private BusinessIntelligenceReportRepository reportRepository;

    @Autowired
    private FileTransferAnalyticsRepository analyticsRepository;

    @Autowired
    private AnalyticsDataService analyticsDataService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Generate comprehensive business intelligence report
     */
    @Async
    public CompletableFuture<BusinessIntelligenceReport> generateReport(String tenantId, ReportType reportType,
                                                                       LocalDate startDate, LocalDate endDate,
                                                                       String createdBy) {
        logger.info("Generating BI report: {} for tenant: {} from {} to {}", 
                   reportType, tenantId, startDate, endDate);
        
        long startTime = System.currentTimeMillis();
        
        try {
            BusinessIntelligenceReport report = new BusinessIntelligenceReport(
                tenantId, reportType, generateReportName(reportType, startDate, endDate));
            report.setReportPeriodStart(startDate);
            report.setReportPeriodEnd(endDate);
            report.setCreatedBy(createdBy);
            
            // Set expiration based on report type
            report.setExpiresAt(calculateExpirationDate(reportType));
            
            // Generate report data based on type
            Map<String, Object> reportData = generateReportData(tenantId, reportType, startDate, endDate);
            Map<String, Object> summaryMetrics = generateSummaryMetrics(reportData);
            Map<String, Object> insights = generateInsights(tenantId, reportType, reportData, summaryMetrics);
            
            // Serialize to JSON
            report.setReportData(objectMapper.writeValueAsString(reportData));
            report.setSummaryMetrics(objectMapper.writeValueAsString(summaryMetrics));
            report.setInsights(objectMapper.writeValueAsString(insights));
            
            // Calculate metadata
            long generationTime = System.currentTimeMillis() - startTime;
            report.setGenerationTimeMs(generationTime);
            report.setReportSizeBytes((long) report.getReportData().length());
            
            // Save report
            BusinessIntelligenceReport savedReport = reportRepository.save(report);
            
            logger.info("Generated BI report: {} in {}ms, size: {} bytes", 
                       reportType, generationTime, savedReport.getReportSizeBytes());
            
            return CompletableFuture.completedFuture(savedReport);
            
        } catch (Exception e) {
            logger.error("Failed to generate BI report: {} for tenant: {}", reportType, tenantId, e);
            throw new RuntimeException("Report generation failed", e);
        }
    }

    /**
     * Get existing report or generate new one if not exists
     */
    public BusinessIntelligenceReport getOrGenerateReport(String tenantId, ReportType reportType,
                                                         LocalDate startDate, LocalDate endDate,
                                                         String createdBy) {
        // Check for existing recent report
        Optional<BusinessIntelligenceReport> existingReport = reportRepository
            .findRecentReport(tenantId, reportType, startDate, endDate, LocalDateTime.now().minusHours(6));
        
        if (existingReport.isPresent() && !isReportExpired(existingReport.get())) {
            logger.debug("Using existing BI report: {} for tenant: {}", reportType, tenantId);
            return existingReport.get();
        }
        
        // Generate new report
        try {
            return generateReport(tenantId, reportType, startDate, endDate, createdBy).get();
        } catch (Exception e) {
            logger.error("Failed to generate BI report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }

    /**
     * Generate predictive analytics report
     */
    public Map<String, Object> generatePredictiveAnalytics(String tenantId, int forecastDays) {
        logger.info("Generating predictive analytics for tenant: {} with {} days forecast", tenantId, forecastDays);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90); // Use 90 days of historical data
        
        List<FileTransferAnalytics> historicalData = analyticsRepository
            .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        Map<String, Object> predictions = new HashMap<>();
        
        // Volume predictions
        Map<String, Object> volumePredictions = predictVolumeTrends(historicalData, forecastDays);
        predictions.put("volumePredictions", volumePredictions);
        
        // Performance predictions
        Map<String, Object> performancePredictions = predictPerformanceTrends(historicalData, forecastDays);
        predictions.put("performancePredictions", performancePredictions);
        
        // Quality predictions
        Map<String, Object> qualityPredictions = predictQualityTrends(historicalData, forecastDays);
        predictions.put("qualityPredictions", qualityPredictions);
        
        // Cost predictions
        Map<String, Object> costPredictions = predictCostTrends(historicalData, forecastDays);
        predictions.put("costPredictions", costPredictions);
        
        // Anomaly detection
        List<Map<String, Object>> anomalies = detectAnomalies(historicalData);
        predictions.put("detectedAnomalies", anomalies);
        
        // Recommendations
        List<Map<String, Object>> recommendations = generateRecommendations(historicalData, predictions);
        predictions.put("recommendations", recommendations);
        
        predictions.put("forecastPeriod", forecastDays);
        predictions.put("historicalPeriodDays", ChronoUnit.DAYS.between(startDate, endDate));
        predictions.put("generatedAt", LocalDateTime.now());
        
        return predictions;
    }

    /**
     * Generate capacity planning report
     */
    public Map<String, Object> generateCapacityPlanningReport(String tenantId, int planningMonths) {
        logger.info("Generating capacity planning report for tenant: {} for {} months", tenantId, planningMonths);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6); // Use 6 months of historical data
        
        List<FileTransferAnalytics> historicalData = analyticsRepository
            .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        Map<String, Object> capacityPlan = new HashMap<>();
        
        // Current capacity utilization
        Map<String, Object> currentCapacity = calculateCurrentCapacity(historicalData);
        capacityPlan.put("currentCapacity", currentCapacity);
        
        // Growth trends
        Map<String, Object> growthTrends = calculateGrowthTrends(historicalData);
        capacityPlan.put("growthTrends", growthTrends);
        
        // Projected capacity needs
        Map<String, Object> projectedNeeds = projectCapacityNeeds(historicalData, planningMonths);
        capacityPlan.put("projectedNeeds", projectedNeeds);
        
        // Resource recommendations
        List<Map<String, Object>> resourceRecommendations = generateResourceRecommendations(
            currentCapacity, growthTrends, projectedNeeds);
        capacityPlan.put("resourceRecommendations", resourceRecommendations);
        
        // Cost implications
        Map<String, Object> costImplications = calculateCapacityCostImplications(
            currentCapacity, projectedNeeds, planningMonths);
        capacityPlan.put("costImplications", costImplications);
        
        capacityPlan.put("planningPeriodMonths", planningMonths);
        capacityPlan.put("generatedAt", LocalDateTime.now());
        
        return capacityPlan;
    }

    /**
     * Generate SLA compliance report
     */
    public Map<String, Object> generateSlaComplianceReport(String tenantId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating SLA compliance report for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        List<FileTransferAnalytics> analyticsData = analyticsRepository
            .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        Map<String, Object> slaReport = new HashMap<>();
        
        // Overall SLA metrics
        Map<String, Object> overallMetrics = calculateOverallSlaMetrics(analyticsData);
        slaReport.put("overallMetrics", overallMetrics);
        
        // Service-level SLA breakdown
        Map<String, Object> serviceLevelMetrics = calculateServiceLevelSlaMetrics(analyticsData);
        slaReport.put("serviceLevelMetrics", serviceLevelMetrics);
        
        // SLA trend analysis
        List<Map<String, Object>> slasTrends = calculateSlaTrends(analyticsData);
        slaReport.put("slaTrends", slasTrends);
        
        // SLA breach analysis
        Map<String, Object> breachAnalysis = analyzeSlaBreaches(analyticsData);
        slaReport.put("breachAnalysis", breachAnalysis);
        
        // Improvement recommendations
        List<Map<String, Object>> improvements = generateSlaImprovementRecommendations(analyticsData);
        slaReport.put("improvementRecommendations", improvements);
        
        slaReport.put("reportPeriod", Map.of("start", startDate, "end", endDate));
        slaReport.put("generatedAt", LocalDateTime.now());
        
        return slaReport;
    }

    /**
     * Generate cost analysis report
     */
    public Map<String, Object> generateCostAnalysisReport(String tenantId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating cost analysis report for tenant: {} from {} to {}", tenantId, startDate, endDate);
        
        List<FileTransferAnalytics> analyticsData = analyticsRepository
            .findByTenantIdAndDateRange(tenantId, startDate, endDate);
        
        Map<String, Object> costReport = new HashMap<>();
        
        // Total cost breakdown
        Map<String, Object> totalCosts = calculateTotalCosts(analyticsData);
        costReport.put("totalCosts", totalCosts);
        
        // Cost per service
        Map<String, Object> serviceCosts = calculateServiceCosts(analyticsData);
        costReport.put("serviceCosts", serviceCosts);
        
        // Cost efficiency metrics
        Map<String, Object> efficiencyMetrics = calculateCostEfficiencyMetrics(analyticsData);
        costReport.put("efficiencyMetrics", efficiencyMetrics);
        
        // Cost trends
        List<Map<String, Object>> costTrends = calculateCostTrends(analyticsData);
        costReport.put("costTrends", costTrends);
        
        // Cost optimization recommendations
        List<Map<String, Object>> optimizations = generateCostOptimizationRecommendations(analyticsData);
        costReport.put("optimizationRecommendations", optimizations);
        
        costReport.put("reportPeriod", Map.of("start", startDate, "end", endDate));
        costReport.put("generatedAt", LocalDateTime.now());
        
        return costReport;
    }

    /**
     * Scheduled job to generate daily reports
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    public void generateScheduledReports() {
        logger.info("Starting scheduled BI report generation");
        
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // Get all tenants that had activity yesterday
            List<String> activeTenants = analyticsRepository.findActiveTenants(yesterday);
            
            for (String tenantId : activeTenants) {
                try {
                    // Generate daily summary report
                    generateReport(tenantId, ReportType.DAILY_SUMMARY, yesterday, yesterday, "system");
                    
                    // Generate weekly report on Sundays
                    if (yesterday.getDayOfWeek().getValue() == 7) {
                        LocalDate weekStart = yesterday.minusDays(6);
                        generateReport(tenantId, ReportType.WEEKLY_SUMMARY, weekStart, yesterday, "system");
                    }
                    
                    // Generate monthly report on last day of month
                    if (yesterday.equals(yesterday.withDayOfMonth(yesterday.lengthOfMonth()))) {
                        LocalDate monthStart = yesterday.withDayOfMonth(1);
                        generateReport(tenantId, ReportType.MONTHLY_SUMMARY, monthStart, yesterday, "system");
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to generate scheduled reports for tenant: {}", tenantId, e);
                }
            }
            
            logger.info("Completed scheduled BI report generation for {} tenants", activeTenants.size());
            
        } catch (Exception e) {
            logger.error("Error in scheduled BI report generation", e);
        }
    }

    /**
     * Clean up expired reports
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3:00 AM
    public void cleanupExpiredReports() {
        logger.info("Starting cleanup of expired BI reports");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<BusinessIntelligenceReport> expiredReports = reportRepository.findExpiredReports(now);
            
            if (!expiredReports.isEmpty()) {
                reportRepository.deleteAll(expiredReports);
                logger.info("Deleted {} expired BI reports", expiredReports.size());
            } else {
                logger.debug("No expired BI reports found");
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired BI reports", e);
        }
    }

    // Private helper methods for report generation

    private String generateReportName(ReportType reportType, LocalDate startDate, LocalDate endDate) {
        return String.format("%s_%s_to_%s", 
            reportType.name().toLowerCase(), 
            startDate.toString(), 
            endDate.toString());
    }

    private LocalDateTime calculateExpirationDate(ReportType reportType) {
        return switch (reportType) {
            case DAILY_SUMMARY -> LocalDateTime.now().plusDays(7);
            case WEEKLY_SUMMARY -> LocalDateTime.now().plusDays(30);
            case MONTHLY_SUMMARY -> LocalDateTime.now().plusDays(90);
            case QUARTERLY_SUMMARY -> LocalDateTime.now().plusDays(365);
            case ANNUAL_SUMMARY -> LocalDateTime.now().plusYears(5);
            default -> LocalDateTime.now().plusDays(30);
        };
    }

    private boolean isReportExpired(BusinessIntelligenceReport report) {
        return report.getExpiresAt() != null && 
               LocalDateTime.now().isAfter(report.getExpiresAt());
    }

    private Map<String, Object> generateReportData(String tenantId, ReportType reportType, 
                                                  LocalDate startDate, LocalDate endDate) {
        Map<String, Object> reportData = new HashMap<>();
        
        // Get analytics summary
        Map<String, Object> summary = analyticsDataService.getAnalyticsSummary(tenantId, startDate, endDate);
        reportData.put("summary", summary);
        
        // Get trending data
        List<Map<String, Object>> trends = analyticsDataService.getTrendingAnalytics(tenantId, startDate, endDate, "daily");
        reportData.put("trends", trends);
        
        // Get service breakdown
        List<Map<String, Object>> serviceAnalytics = analyticsDataService.getServiceAnalytics(tenantId, startDate, endDate);
        reportData.put("serviceAnalytics", serviceAnalytics);
        
        // Add report-type specific data
        switch (reportType) {
            case PERFORMANCE_ANALYSIS -> {
                Map<String, Object> performanceData = generatePerformanceAnalysisData(tenantId, startDate, endDate);
                reportData.put("performanceAnalysis", performanceData);
            }
            case QUALITY_ANALYSIS -> {
                Map<String, Object> qualityData = generateQualityAnalysisData(tenantId, startDate, endDate);
                reportData.put("qualityAnalysis", qualityData);
            }
            case COST_ANALYSIS -> {
                Map<String, Object> costData = generateCostAnalysisReport(tenantId, startDate, endDate);
                reportData.put("costAnalysis", costData);
            }
            case PREDICTIVE_ANALYSIS -> {
                Map<String, Object> predictiveData = generatePredictiveAnalytics(tenantId, 30);
                reportData.put("predictiveAnalysis", predictiveData);
            }
            case CAPACITY_PLANNING -> {
                Map<String, Object> capacityData = generateCapacityPlanningReport(tenantId, 6);
                reportData.put("capacityPlanning", capacityData);
            }
        }
        
        return reportData;
    }

    private Map<String, Object> generateSummaryMetrics(Map<String, Object> reportData) {
        Map<String, Object> summary = (Map<String, Object>) reportData.get("summary");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalFiles", summary.get("totalFiles"));
        metrics.put("successRate", summary.get("successRate"));
        metrics.put("avgProcessingTime", summary.get("avgProcessingTimeSeconds"));
        metrics.put("qualityScore", summary.get("qualityScore"));
        metrics.put("slaComplianceRate", summary.get("slaComplianceRate"));
        metrics.put("totalCost", summary.get("totalEstimatedCost"));
        
        return metrics;
    }

    private Map<String, Object> generateInsights(String tenantId, ReportType reportType, 
                                                Map<String, Object> reportData, 
                                                Map<String, Object> summaryMetrics) {
        Map<String, Object> insights = new HashMap<>();
        
        List<String> keyInsights = new ArrayList<>();
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Analyze success rate
        double successRate = (Double) summaryMetrics.get("successRate");
        if (successRate < 95.0) {
            keyInsights.add("Success rate is below 95% threshold, indicating potential system issues");
            recommendations.add(Map.of(
                "priority", "HIGH",
                "category", "RELIABILITY",
                "recommendation", "Investigate failed transfers and implement retry mechanisms",
                "estimatedImpact", "10-15% improvement in success rate"
            ));
        }
        
        // Analyze performance
        double avgProcessingTime = (Double) summaryMetrics.get("avgProcessingTime");
        if (avgProcessingTime > 30.0) {
            keyInsights.add("Average processing time exceeds 30 seconds, impacting user experience");
            recommendations.add(Map.of(
                "priority", "MEDIUM",
                "category", "PERFORMANCE",
                "recommendation", "Optimize file processing pipeline and consider parallel processing",
                "estimatedImpact", "20-30% reduction in processing time"
            ));
        }
        
        // Analyze quality
        double qualityScore = (Double) summaryMetrics.get("qualityScore");
        if (qualityScore < 90.0) {
            keyInsights.add("Data quality score is below 90%, indicating validation issues");
            recommendations.add(Map.of(
                "priority", "HIGH",
                "category", "QUALITY",
                "recommendation", "Review schema validation rules and implement data quality checks",
                "estimatedImpact", "5-10% improvement in quality score"
            ));
        }
        
        // Analyze SLA compliance
        double slaCompliance = (Double) summaryMetrics.get("slaComplianceRate");
        if (slaCompliance < 98.0) {
            keyInsights.add("SLA compliance is below 98% target, risking customer satisfaction");
            recommendations.add(Map.of(
                "priority", "HIGH",
                "category", "SLA",
                "recommendation", "Implement proactive monitoring and automated scaling",
                "estimatedImpact", "2-5% improvement in SLA compliance"
            ));
        }
        
        insights.put("keyInsights", keyInsights);
        insights.put("recommendations", recommendations);
        insights.put("overallHealth", calculateOverallHealth(summaryMetrics));
        insights.put("riskLevel", calculateRiskLevel(summaryMetrics));
        
        return insights;
    }

    // Additional helper methods would be implemented here for specific analysis types
    // (Performance, Quality, Cost analysis, Predictive analytics, etc.)
    
    private String calculateOverallHealth(Map<String, Object> metrics) {
        double successRate = (Double) metrics.get("successRate");
        double qualityScore = (Double) metrics.get("qualityScore");
        double slaCompliance = (Double) metrics.get("slaComplianceRate");
        
        double overallScore = (successRate + qualityScore + slaCompliance) / 3.0;
        
        if (overallScore >= 95.0) return "EXCELLENT";
        if (overallScore >= 90.0) return "GOOD";
        if (overallScore >= 80.0) return "FAIR";
        return "POOR";
    }

    private String calculateRiskLevel(Map<String, Object> metrics) {
        double successRate = (Double) metrics.get("successRate");
        double slaCompliance = (Double) metrics.get("slaComplianceRate");
        
        if (successRate < 90.0 || slaCompliance < 95.0) return "HIGH";
        if (successRate < 95.0 || slaCompliance < 98.0) return "MEDIUM";
        return "LOW";
    }

    // Placeholder methods for detailed analysis (would be fully implemented)
    private Map<String, Object> generatePerformanceAnalysisData(String tenantId, LocalDate startDate, LocalDate endDate) {
        return new HashMap<>();
    }

    private Map<String, Object> generateQualityAnalysisData(String tenantId, LocalDate startDate, LocalDate endDate) {
        return new HashMap<>();
    }

    private Map<String, Object> predictVolumeTrends(List<FileTransferAnalytics> data, int days) {
        return new HashMap<>();
    }

    private Map<String, Object> predictPerformanceTrends(List<FileTransferAnalytics> data, int days) {
        return new HashMap<>();
    }

    private Map<String, Object> predictQualityTrends(List<FileTransferAnalytics> data, int days) {
        return new HashMap<>();
    }

    private Map<String, Object> predictCostTrends(List<FileTransferAnalytics> data, int days) {
        return new HashMap<>();
    }

    private List<Map<String, Object>> detectAnomalies(List<FileTransferAnalytics> data) {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> generateRecommendations(List<FileTransferAnalytics> data, Map<String, Object> predictions) {
        return new ArrayList<>();
    }

    private Map<String, Object> calculateCurrentCapacity(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private Map<String, Object> calculateGrowthTrends(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private Map<String, Object> projectCapacityNeeds(List<FileTransferAnalytics> data, int months) {
        return new HashMap<>();
    }

    private List<Map<String, Object>> generateResourceRecommendations(Map<String, Object> current, 
                                                                    Map<String, Object> trends, 
                                                                    Map<String, Object> projected) {
        return new ArrayList<>();
    }

    private Map<String, Object> calculateCapacityCostImplications(Map<String, Object> current, 
                                                                 Map<String, Object> projected, 
                                                                 int months) {
        return new HashMap<>();
    }

    private Map<String, Object> calculateOverallSlaMetrics(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private Map<String, Object> calculateServiceLevelSlaMetrics(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private List<Map<String, Object>> calculateSlaTrends(List<FileTransferAnalytics> data) {
        return new ArrayList<>();
    }

    private Map<String, Object> analyzeSlaBreaches(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private List<Map<String, Object>> generateSlaImprovementRecommendations(List<FileTransferAnalytics> data) {
        return new ArrayList<>();
    }

    private Map<String, Object> calculateTotalCosts(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private Map<String, Object> calculateServiceCosts(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private Map<String, Object> calculateCostEfficiencyMetrics(List<FileTransferAnalytics> data) {
        return new HashMap<>();
    }

    private List<Map<String, Object>> calculateCostTrends(List<FileTransferAnalytics> data) {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> generateCostOptimizationRecommendations(List<FileTransferAnalytics> data) {
        return new ArrayList<>();
    }
}