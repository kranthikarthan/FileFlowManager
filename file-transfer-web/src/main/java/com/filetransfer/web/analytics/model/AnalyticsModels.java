package com.filetransfer.web.analytics.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Analytics data models for business intelligence and reporting
 * Supports real-time analytics, historical analysis, and predictive insights
 */
public class AnalyticsModels {

    /**
     * File Transfer Analytics - Aggregated metrics for BI reporting
     */
    @Entity
    @Table(name = "file_transfer_analytics", 
           indexes = {
               @Index(name = "idx_fta_tenant_date", columnList = "tenant_id, analytics_date"),
               @Index(name = "idx_fta_service_date", columnList = "service_name, analytics_date"),
               @Index(name = "idx_fta_date_range", columnList = "analytics_date, created_at")
           })
    public static class FileTransferAnalytics {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "tenant_id", nullable = false)
        private String tenantId;
        
        @Column(name = "service_name")
        private String serviceName;
        
        @Column(name = "sub_service_name")
        private String subServiceName;
        
        @Column(name = "analytics_date", nullable = false)
        private LocalDate analyticsDate;
        
        @Column(name = "transfer_direction")
        @Enumerated(EnumType.STRING)
        private TransferDirection direction;
        
        @Column(name = "file_type")
        @Enumerated(EnumType.STRING)
        private FileType fileType;
        
        // Volume metrics
        @Column(name = "total_files")
        private Long totalFiles = 0L;
        
        @Column(name = "successful_transfers")
        private Long successfulTransfers = 0L;
        
        @Column(name = "failed_transfers")
        private Long failedTransfers = 0L;
        
        @Column(name = "total_data_volume_bytes")
        private Long totalDataVolumeBytes = 0L;
        
        // Performance metrics
        @Column(name = "avg_processing_time_ms")
        private Double avgProcessingTimeMs;
        
        @Column(name = "min_processing_time_ms")
        private Long minProcessingTimeMs;
        
        @Column(name = "max_processing_time_ms")
        private Long maxProcessingTimeMs;
        
        @Column(name = "p95_processing_time_ms")
        private Long p95ProcessingTimeMs;
        
        @Column(name = "p99_processing_time_ms")
        private Long p99ProcessingTimeMs;
        
        // Quality metrics
        @Column(name = "validation_errors")
        private Long validationErrors = 0L;
        
        @Column(name = "schema_validation_failures")
        private Long schemaValidationFailures = 0L;
        
        @Column(name = "file_corruption_incidents")
        private Long fileCorruptionIncidents = 0L;
        
        // Business metrics
        @Column(name = "sla_breaches")
        private Long slaBreaches = 0L;
        
        @Column(name = "cut_off_extensions")
        private Long cutOffExtensions = 0L;
        
        @Column(name = "holiday_processing")
        private Long holidayProcessing = 0L;
        
        // Cost metrics
        @Column(name = "estimated_processing_cost")
        private Double estimatedProcessingCost;
        
        @Column(name = "storage_cost")
        private Double storageCost;
        
        @Column(name = "bandwidth_cost")
        private Double bandwidthCost;
        
        // Timestamps
        @Column(name = "created_at")
        private LocalDateTime createdAt;
        
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
        
        // Constructors, getters, setters
        public FileTransferAnalytics() {
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
        
        // Getters and setters...
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getSubServiceName() { return subServiceName; }
        public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
        
        public LocalDate getAnalyticsDate() { return analyticsDate; }
        public void setAnalyticsDate(LocalDate analyticsDate) { this.analyticsDate = analyticsDate; }
        
        public TransferDirection getDirection() { return direction; }
        public void setDirection(TransferDirection direction) { this.direction = direction; }
        
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getSuccessfulTransfers() { return successfulTransfers; }
        public void setSuccessfulTransfers(Long successfulTransfers) { this.successfulTransfers = successfulTransfers; }
        
        public Long getFailedTransfers() { return failedTransfers; }
        public void setFailedTransfers(Long failedTransfers) { this.failedTransfers = failedTransfers; }
        
        public Long getTotalDataVolumeBytes() { return totalDataVolumeBytes; }
        public void setTotalDataVolumeBytes(Long totalDataVolumeBytes) { this.totalDataVolumeBytes = totalDataVolumeBytes; }
        
        public Double getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
        public void setAvgProcessingTimeMs(Double avgProcessingTimeMs) { this.avgProcessingTimeMs = avgProcessingTimeMs; }
        
        public Long getMinProcessingTimeMs() { return minProcessingTimeMs; }
        public void setMinProcessingTimeMs(Long minProcessingTimeMs) { this.minProcessingTimeMs = minProcessingTimeMs; }
        
        public Long getMaxProcessingTimeMs() { return maxProcessingTimeMs; }
        public void setMaxProcessingTimeMs(Long maxProcessingTimeMs) { this.maxProcessingTimeMs = maxProcessingTimeMs; }
        
        public Long getP95ProcessingTimeMs() { return p95ProcessingTimeMs; }
        public void setP95ProcessingTimeMs(Long p95ProcessingTimeMs) { this.p95ProcessingTimeMs = p95ProcessingTimeMs; }
        
        public Long getP99ProcessingTimeMs() { return p99ProcessingTimeMs; }
        public void setP99ProcessingTimeMs(Long p99ProcessingTimeMs) { this.p99ProcessingTimeMs = p99ProcessingTimeMs; }
        
        public Long getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Long validationErrors) { this.validationErrors = validationErrors; }
        
        public Long getSchemaValidationFailures() { return schemaValidationFailures; }
        public void setSchemaValidationFailures(Long schemaValidationFailures) { this.schemaValidationFailures = schemaValidationFailures; }
        
        public Long getFileCorruptionIncidents() { return fileCorruptionIncidents; }
        public void setFileCorruptionIncidents(Long fileCorruptionIncidents) { this.fileCorruptionIncidents = fileCorruptionIncidents; }
        
        public Long getSlaBreaches() { return slaBreaches; }
        public void setSlaBreaches(Long slaBreaches) { this.slaBreaches = slaBreaches; }
        
        public Long getCutOffExtensions() { return cutOffExtensions; }
        public void setCutOffExtensions(Long cutOffExtensions) { this.cutOffExtensions = cutOffExtensions; }
        
        public Long getHolidayProcessing() { return holidayProcessing; }
        public void setHolidayProcessing(Long holidayProcessing) { this.holidayProcessing = holidayProcessing; }
        
        public Double getEstimatedProcessingCost() { return estimatedProcessingCost; }
        public void setEstimatedProcessingCost(Double estimatedProcessingCost) { this.estimatedProcessingCost = estimatedProcessingCost; }
        
        public Double getStorageCost() { return storageCost; }
        public void setStorageCost(Double storageCost) { this.storageCost = storageCost; }
        
        public Double getBandwidthCost() { return bandwidthCost; }
        public void setBandwidthCost(Double bandwidthCost) { this.bandwidthCost = bandwidthCost; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * Real-time Analytics Events - Stream processing for live insights
     */
    @Entity
    @Table(name = "real_time_analytics_events",
           indexes = {
               @Index(name = "idx_rtae_timestamp", columnList = "event_timestamp"),
               @Index(name = "idx_rtae_tenant_event", columnList = "tenant_id, event_type"),
               @Index(name = "idx_rtae_processing", columnList = "processed, event_timestamp")
           })
    public static class RealTimeAnalyticsEvent {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "tenant_id", nullable = false)
        private String tenantId;
        
        @Column(name = "event_type", nullable = false)
        @Enumerated(EnumType.STRING)
        private AnalyticsEventType eventType;
        
        @Column(name = "event_timestamp", nullable = false)
        private LocalDateTime eventTimestamp;
        
        @Column(name = "service_name")
        private String serviceName;
        
        @Column(name = "sub_service_name")
        private String subServiceName;
        
        @Column(name = "file_name")
        private String fileName;
        
        @Column(name = "file_size_bytes")
        private Long fileSizeBytes;
        
        @Column(name = "processing_time_ms")
        private Long processingTimeMs;
        
        @Column(name = "transfer_status")
        @Enumerated(EnumType.STRING)
        private TransferStatus transferStatus;
        
        @Column(name = "error_code")
        private String errorCode;
        
        @Column(name = "error_message", columnDefinition = "TEXT")
        private String errorMessage;
        
        @Column(name = "metadata", columnDefinition = "TEXT")
        private String metadata; // JSON metadata
        
        @Column(name = "processed", nullable = false)
        private Boolean processed = false;
        
        @Column(name = "created_at")
        private LocalDateTime createdAt;
        
        // Constructors, getters, setters
        public RealTimeAnalyticsEvent() {
            this.eventTimestamp = LocalDateTime.now();
            this.createdAt = LocalDateTime.now();
        }
        
        public RealTimeAnalyticsEvent(String tenantId, AnalyticsEventType eventType) {
            this();
            this.tenantId = tenantId;
            this.eventType = eventType;
        }
        
        // Getters and setters...
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public AnalyticsEventType getEventType() { return eventType; }
        public void setEventType(AnalyticsEventType eventType) { this.eventType = eventType; }
        
        public LocalDateTime getEventTimestamp() { return eventTimestamp; }
        public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getSubServiceName() { return subServiceName; }
        public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public TransferStatus getTransferStatus() { return transferStatus; }
        public void setTransferStatus(TransferStatus transferStatus) { this.transferStatus = transferStatus; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }
        
        public Boolean getProcessed() { return processed; }
        public void setProcessed(Boolean processed) { this.processed = processed; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Business Intelligence Reports - Pre-computed analytical insights
     */
    @Entity
    @Table(name = "bi_reports",
           indexes = {
               @Index(name = "idx_bi_tenant_type", columnList = "tenant_id, report_type"),
               @Index(name = "idx_bi_generated_at", columnList = "generated_at"),
               @Index(name = "idx_bi_period", columnList = "report_period_start, report_period_end")
           })
    public static class BusinessIntelligenceReport {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "tenant_id", nullable = false)
        private String tenantId;
        
        @Column(name = "report_type", nullable = false)
        @Enumerated(EnumType.STRING)
        private ReportType reportType;
        
        @Column(name = "report_name", nullable = false)
        private String reportName;
        
        @Column(name = "report_period_start", nullable = false)
        private LocalDate reportPeriodStart;
        
        @Column(name = "report_period_end", nullable = false)
        private LocalDate reportPeriodEnd;
        
        @Column(name = "report_data", columnDefinition = "TEXT")
        private String reportData; // JSON report data
        
        @Column(name = "summary_metrics", columnDefinition = "TEXT")
        private String summaryMetrics; // JSON summary
        
        @Column(name = "insights", columnDefinition = "TEXT")
        private String insights; // JSON insights and recommendations
        
        @Column(name = "generated_at", nullable = false)
        private LocalDateTime generatedAt;
        
        @Column(name = "expires_at")
        private LocalDateTime expiresAt;
        
        @Column(name = "report_size_bytes")
        private Long reportSizeBytes;
        
        @Column(name = "generation_time_ms")
        private Long generationTimeMs;
        
        @Column(name = "created_by")
        private String createdBy;
        
        // Constructors, getters, setters
        public BusinessIntelligenceReport() {
            this.generatedAt = LocalDateTime.now();
        }
        
        public BusinessIntelligenceReport(String tenantId, ReportType reportType, String reportName) {
            this();
            this.tenantId = tenantId;
            this.reportType = reportType;
            this.reportName = reportName;
        }
        
        // Getters and setters...
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public ReportType getReportType() { return reportType; }
        public void setReportType(ReportType reportType) { this.reportType = reportType; }
        
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        
        public LocalDate getReportPeriodStart() { return reportPeriodStart; }
        public void setReportPeriodStart(LocalDate reportPeriodStart) { this.reportPeriodStart = reportPeriodStart; }
        
        public LocalDate getReportPeriodEnd() { return reportPeriodEnd; }
        public void setReportPeriodEnd(LocalDate reportPeriodEnd) { this.reportPeriodEnd = reportPeriodEnd; }
        
        public String getReportData() { return reportData; }
        public void setReportData(String reportData) { this.reportData = reportData; }
        
        public String getSummaryMetrics() { return summaryMetrics; }
        public void setSummaryMetrics(String summaryMetrics) { this.summaryMetrics = summaryMetrics; }
        
        public String getInsights() { return insights; }
        public void setInsights(String insights) { this.insights = insights; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        
        public Long getReportSizeBytes() { return reportSizeBytes; }
        public void setReportSizeBytes(Long reportSizeBytes) { this.reportSizeBytes = reportSizeBytes; }
        
        public Long getGenerationTimeMs() { return generationTimeMs; }
        public void setGenerationTimeMs(Long generationTimeMs) { this.generationTimeMs = generationTimeMs; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }

    // Enums for analytics
    public enum AnalyticsEventType {
        FILE_UPLOAD_STARTED,
        FILE_UPLOAD_COMPLETED,
        FILE_UPLOAD_FAILED,
        FILE_PROCESSING_STARTED,
        FILE_PROCESSING_COMPLETED,
        FILE_PROCESSING_FAILED,
        VALIDATION_STARTED,
        VALIDATION_COMPLETED,
        VALIDATION_FAILED,
        SCHEMA_VALIDATION_FAILED,
        CUT_OFF_EXTENSION_REQUESTED,
        CUT_OFF_EXTENSION_APPROVED,
        SLA_BREACH_DETECTED,
        SYSTEM_ERROR,
        PERFORMANCE_ANOMALY,
        SECURITY_INCIDENT
    }

    public enum ReportType {
        DAILY_SUMMARY,
        WEEKLY_SUMMARY,
        MONTHLY_SUMMARY,
        QUARTERLY_SUMMARY,
        ANNUAL_SUMMARY,
        PERFORMANCE_ANALYSIS,
        QUALITY_ANALYSIS,
        COST_ANALYSIS,
        SLA_COMPLIANCE,
        TREND_ANALYSIS,
        PREDICTIVE_ANALYSIS,
        ANOMALY_DETECTION,
        CAPACITY_PLANNING,
        CUSTOM_REPORT
    }

    // Supporting enums (assuming these exist in the main codebase)
    public enum TransferDirection {
        INBOUND, OUTBOUND, BIDIRECTIONAL
    }

    public enum FileType {
        JSON, XML, COBOL, CSV, BINARY
    }

    public enum TransferStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, ARCHIVED
    }
}