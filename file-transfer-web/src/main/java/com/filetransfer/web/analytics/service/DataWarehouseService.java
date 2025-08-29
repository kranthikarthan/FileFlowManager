package com.filetransfer.web.analytics.service;

import com.filetransfer.web.analytics.model.AnalyticsModels.*;
import com.filetransfer.web.analytics.repository.FileTransferAnalyticsRepository;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Warehouse Service - Advanced data processing and ETL operations
 * Handles data aggregation, dimensional modeling, and analytical data preparation
 */
@Service
@Transactional
public class DataWarehouseService {

    private static final Logger logger = LoggerFactory.getLogger(DataWarehouseService.class);

    @Autowired
    private FileTransferAnalyticsRepository analyticsRepository;

    @Autowired
    private FileTransferRecordRepository fileTransferRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Perform ETL (Extract, Transform, Load) operations for analytics data warehouse
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    @Transactional
    public void performDailyETL() {
        logger.info("Starting daily ETL process for analytics data warehouse");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            // Extract data from operational systems
            List<FileTransferRecord> rawData = extractDailyData(yesterday);
            
            // Transform data for analytical purposes
            List<FileTransferAnalytics> transformedData = transformDataForAnalytics(rawData, yesterday);
            
            // Load data into analytics warehouse
            loadAnalyticsData(transformedData);
            
            // Perform data quality checks
            performDataQualityChecks(yesterday);
            
            // Update dimensional data
            updateDimensionalData(yesterday);
            
            // Generate summary statistics
            generateDailySummaryStatistics(yesterday);
            
            logger.info("Completed daily ETL process for date: {}", yesterday);
            
        } catch (Exception e) {
            logger.error("Error in daily ETL process", e);
        }
    }

    /**
     * Extract daily data from operational systems
     */
    private List<FileTransferRecord> extractDailyData(LocalDate date) {
        logger.debug("Extracting data for date: {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        return fileTransferRepository.findByCreatedAtBetween(startOfDay, endOfDay);
    }

    /**
     * Transform operational data into analytical format
     */
    private List<FileTransferAnalytics> transformDataForAnalytics(List<FileTransferRecord> rawData, LocalDate date) {
        logger.debug("Transforming {} records for date: {}", rawData.size(), date);
        
        // Group data by tenant, service, sub-service, direction, and file type
        Map<String, List<FileTransferRecord>> groupedData = rawData.stream()
            .collect(Collectors.groupingBy(record -> createGroupingKey(record)));
        
        List<FileTransferAnalytics> analyticsRecords = new ArrayList<>();
        
        for (Map.Entry<String, List<FileTransferRecord>> entry : groupedData.entrySet()) {
            String[] keyParts = entry.getKey().split(":");
            List<FileTransferRecord> records = entry.getValue();
            
            FileTransferAnalytics analytics = createAnalyticsRecord(
                keyParts[0], // tenantId
                keyParts[1], // serviceName
                keyParts[2], // subServiceName
                keyParts[3], // direction
                keyParts[4], // fileType
                date,
                records
            );
            
            analyticsRecords.add(analytics);
        }
        
        return analyticsRecords;
    }

    /**
     * Create grouping key for data aggregation
     */
    private String createGroupingKey(FileTransferRecord record) {
        return String.format("%s:%s:%s:%s:%s",
            record.getTenantId(),
            Optional.ofNullable(record.getServiceName()).orElse("UNKNOWN"),
            Optional.ofNullable(record.getSubServiceName()).orElse("UNKNOWN"),
            Optional.ofNullable(record.getTransferDirection()).orElse("UNKNOWN"),
            Optional.ofNullable(record.getFileType()).orElse("UNKNOWN")
        );
    }

    /**
     * Create analytics record from grouped operational data
     */
    private FileTransferAnalytics createAnalyticsRecord(String tenantId, String serviceName, 
                                                       String subServiceName, String direction, 
                                                       String fileType, LocalDate date,
                                                       List<FileTransferRecord> records) {
        
        FileTransferAnalytics analytics = new FileTransferAnalytics();
        analytics.setTenantId(tenantId);
        analytics.setServiceName("UNKNOWN".equals(serviceName) ? null : serviceName);
        analytics.setSubServiceName("UNKNOWN".equals(subServiceName) ? null : subServiceName);
        analytics.setAnalyticsDate(date);
        
        // Set direction and file type
        try {
            if (!"UNKNOWN".equals(direction)) {
                analytics.setDirection(TransferDirection.valueOf(direction));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid transfer direction: {}", direction);
        }
        
        try {
            if (!"UNKNOWN".equals(fileType)) {
                analytics.setFileType(FileType.valueOf(fileType));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file type: {}", fileType);
        }
        
        // Calculate volume metrics
        analytics.setTotalFiles((long) records.size());
        analytics.setSuccessfulTransfers(records.stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()) || "ARCHIVED".equals(r.getStatus()))
            .count());
        analytics.setFailedTransfers(records.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .count());
        
        // Calculate data volume
        analytics.setTotalDataVolumeBytes(records.stream()
            .mapToLong(r -> Optional.ofNullable(r.getFileSize()).orElse(0L))
            .sum());
        
        // Calculate performance metrics
        calculatePerformanceMetrics(analytics, records);
        
        // Calculate quality metrics
        calculateQualityMetrics(analytics, records);
        
        // Calculate business metrics
        calculateBusinessMetrics(analytics, records);
        
        // Calculate cost metrics
        calculateCostMetrics(analytics, records);
        
        return analytics;
    }

    /**
     * Calculate performance metrics from records
     */
    private void calculatePerformanceMetrics(FileTransferAnalytics analytics, List<FileTransferRecord> records) {
        List<Long> processingTimes = records.stream()
            .filter(r -> r.getProcessingStartTime() != null && r.getProcessingEndTime() != null)
            .map(r -> ChronoUnit.MILLIS.between(r.getProcessingStartTime(), r.getProcessingEndTime()))
            .collect(Collectors.toList());
        
        if (!processingTimes.isEmpty()) {
            analytics.setAvgProcessingTimeMs(processingTimes.stream()
                .mapToLong(Long::longValue).average().orElse(0.0));
            analytics.setMinProcessingTimeMs(processingTimes.stream()
                .mapToLong(Long::longValue).min().orElse(0L));
            analytics.setMaxProcessingTimeMs(processingTimes.stream()
                .mapToLong(Long::longValue).max().orElse(0L));
            
            // Calculate percentiles
            processingTimes.sort(Long::compareTo);
            int size = processingTimes.size();
            analytics.setP95ProcessingTimeMs(processingTimes.get((int) Math.ceil(size * 0.95) - 1));
            analytics.setP99ProcessingTimeMs(processingTimes.get((int) Math.ceil(size * 0.99) - 1));
        }
    }

    /**
     * Calculate quality metrics from records
     */
    private void calculateQualityMetrics(FileTransferAnalytics analytics, List<FileTransferRecord> records) {
        // Count validation errors from error messages
        long validationErrors = records.stream()
            .filter(r -> r.getErrorMessage() != null && 
                        r.getErrorMessage().toLowerCase().contains("validation"))
            .count();
        
        long schemaFailures = records.stream()
            .filter(r -> r.getErrorMessage() != null && 
                        r.getErrorMessage().toLowerCase().contains("schema"))
            .count();
        
        long corruptionIncidents = records.stream()
            .filter(r -> r.getErrorMessage() != null && 
                        (r.getErrorMessage().toLowerCase().contains("corrupt") ||
                         r.getErrorMessage().toLowerCase().contains("integrity")))
            .count();
        
        analytics.setValidationErrors(validationErrors);
        analytics.setSchemaValidationFailures(schemaFailures);
        analytics.setFileCorruptionIncidents(corruptionIncidents);
    }

    /**
     * Calculate business metrics from records
     */
    private void calculateBusinessMetrics(FileTransferAnalytics analytics, List<FileTransferRecord> records) {
        // SLA breaches - files that took longer than expected
        long slaBreaches = records.stream()
            .filter(r -> r.getProcessingStartTime() != null && r.getProcessingEndTime() != null)
            .filter(r -> ChronoUnit.MINUTES.between(r.getProcessingStartTime(), r.getProcessingEndTime()) > 30)
            .count();
        
        analytics.setSlaBreaches(slaBreaches);
        
        // Cut-off extensions would be tracked separately
        analytics.setCutOffExtensions(0L);
        
        // Holiday processing would be determined by date
        analytics.setHolidayProcessing(0L);
    }

    /**
     * Calculate cost metrics from records
     */
    private void calculateCostMetrics(FileTransferAnalytics analytics, List<FileTransferRecord> records) {
        // Simple cost calculation model
        long totalFiles = analytics.getTotalFiles();
        long totalDataBytes = analytics.getTotalDataVolumeBytes();
        
        // Processing cost: $0.001 per file + $0.0001 per MB
        double processingCost = (totalFiles * 0.001) + ((totalDataBytes / 1024.0 / 1024.0) * 0.0001);
        
        // Storage cost: $0.023 per GB per month (daily = monthly / 30)
        double storageCost = (totalDataBytes / 1024.0 / 1024.0 / 1024.0) * 0.023 / 30.0;
        
        // Bandwidth cost: $0.09 per GB
        double bandwidthCost = (totalDataBytes / 1024.0 / 1024.0 / 1024.0) * 0.09;
        
        analytics.setEstimatedProcessingCost(processingCost);
        analytics.setStorageCost(storageCost);
        analytics.setBandwidthCost(bandwidthCost);
    }

    /**
     * Load transformed analytics data into warehouse
     */
    private void loadAnalyticsData(List<FileTransferAnalytics> analyticsData) {
        logger.debug("Loading {} analytics records into warehouse", analyticsData.size());
        
        for (FileTransferAnalytics analytics : analyticsData) {
            // Check if record already exists (upsert logic)
            Optional<FileTransferAnalytics> existing = analyticsRepository
                .findByTenantIdAndServiceNameAndSubServiceNameAndAnalyticsDate(
                    analytics.getTenantId(),
                    analytics.getServiceName(),
                    analytics.getSubServiceName(),
                    analytics.getAnalyticsDate()
                );
            
            if (existing.isPresent()) {
                // Update existing record
                FileTransferAnalytics existingRecord = existing.get();
                updateAnalyticsRecord(existingRecord, analytics);
                analyticsRepository.save(existingRecord);
            } else {
                // Insert new record
                analyticsRepository.save(analytics);
            }
        }
    }

    /**
     * Update existing analytics record with new data
     */
    private void updateAnalyticsRecord(FileTransferAnalytics existing, FileTransferAnalytics newData) {
        existing.setTotalFiles(newData.getTotalFiles());
        existing.setSuccessfulTransfers(newData.getSuccessfulTransfers());
        existing.setFailedTransfers(newData.getFailedTransfers());
        existing.setTotalDataVolumeBytes(newData.getTotalDataVolumeBytes());
        existing.setAvgProcessingTimeMs(newData.getAvgProcessingTimeMs());
        existing.setMinProcessingTimeMs(newData.getMinProcessingTimeMs());
        existing.setMaxProcessingTimeMs(newData.getMaxProcessingTimeMs());
        existing.setP95ProcessingTimeMs(newData.getP95ProcessingTimeMs());
        existing.setP99ProcessingTimeMs(newData.getP99ProcessingTimeMs());
        existing.setValidationErrors(newData.getValidationErrors());
        existing.setSchemaValidationFailures(newData.getSchemaValidationFailures());
        existing.setFileCorruptionIncidents(newData.getFileCorruptionIncidents());
        existing.setSlaBreaches(newData.getSlaBreaches());
        existing.setCutOffExtensions(newData.getCutOffExtensions());
        existing.setHolidayProcessing(newData.getHolidayProcessing());
        existing.setEstimatedProcessingCost(newData.getEstimatedProcessingCost());
        existing.setStorageCost(newData.getStorageCost());
        existing.setBandwidthCost(newData.getBandwidthCost());
        existing.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Perform data quality checks on warehouse data
     */
    private void performDataQualityChecks(LocalDate date) {
        logger.debug("Performing data quality checks for date: {}", date);
        
        List<FileTransferAnalytics> dayData = analyticsRepository
            .findByTenantIdAndDateRange("", date, date); // Get all tenants
        
        // Check for data completeness
        checkDataCompleteness(dayData, date);
        
        // Check for data consistency
        checkDataConsistency(dayData, date);
        
        // Check for data accuracy
        checkDataAccuracy(dayData, date);
        
        // Log data quality issues
        logDataQualityIssues(dayData, date);
    }

    /**
     * Check data completeness
     */
    private void checkDataCompleteness(List<FileTransferAnalytics> data, LocalDate date) {
        // Verify all expected tenants have data
        List<String> activeTenants = fileTransferRepository.findDistinctTenantIds();
        List<String> analyticsTenantsForDate = data.stream()
            .map(FileTransferAnalytics::getTenantId)
            .distinct()
            .collect(Collectors.toList());
        
        List<String> missingTenants = activeTenants.stream()
            .filter(tenant -> !analyticsTenantsForDate.contains(tenant))
            .collect(Collectors.toList());
        
        if (!missingTenants.isEmpty()) {
            logger.warn("Missing analytics data for tenants on {}: {}", date, missingTenants);
        }
    }

    /**
     * Check data consistency
     */
    private void checkDataConsistency(List<FileTransferAnalytics> data, LocalDate date) {
        for (FileTransferAnalytics analytics : data) {
            // Check if successful + failed = total
            long calculatedTotal = analytics.getSuccessfulTransfers() + analytics.getFailedTransfers();
            if (calculatedTotal != analytics.getTotalFiles()) {
                logger.warn("Data inconsistency for tenant {} on {}: total files ({}) != successful ({}) + failed ({})",
                    analytics.getTenantId(), date, analytics.getTotalFiles(), 
                    analytics.getSuccessfulTransfers(), analytics.getFailedTransfers());
            }
            
            // Check for negative values
            if (analytics.getTotalFiles() < 0 || analytics.getSuccessfulTransfers() < 0 || 
                analytics.getFailedTransfers() < 0 || analytics.getTotalDataVolumeBytes() < 0) {
                logger.warn("Negative values detected for tenant {} on {}", analytics.getTenantId(), date);
            }
        }
    }

    /**
     * Check data accuracy by comparing with operational data
     */
    private void checkDataAccuracy(List<FileTransferAnalytics> data, LocalDate date) {
        // Sample check: verify a few records against operational data
        for (FileTransferAnalytics analytics : data.stream().limit(5).collect(Collectors.toList())) {
            List<FileTransferRecord> operationalRecords = fileTransferRepository
                .findByTenantIdAndCreatedAtBetween(
                    analytics.getTenantId(), 
                    date.atStartOfDay(), 
                    date.plusDays(1).atStartOfDay()
                );
            
            long operationalCount = operationalRecords.size();
            if (Math.abs(operationalCount - analytics.getTotalFiles()) > 0) {
                logger.warn("Data accuracy issue for tenant {} on {}: analytics count ({}) vs operational count ({})",
                    analytics.getTenantId(), date, analytics.getTotalFiles(), operationalCount);
            }
        }
    }

    /**
     * Log data quality issues
     */
    private void logDataQualityIssues(List<FileTransferAnalytics> data, LocalDate date) {
        Map<String, Object> qualityReport = new HashMap<>();
        qualityReport.put("date", date);
        qualityReport.put("totalRecords", data.size());
        qualityReport.put("uniqueTenants", data.stream().map(FileTransferAnalytics::getTenantId).distinct().count());
        qualityReport.put("totalFiles", data.stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum());
        qualityReport.put("totalDataVolume", data.stream().mapToLong(FileTransferAnalytics::getTotalDataVolumeBytes).sum());
        
        logger.info("Data quality report for {}: {}", date, qualityReport);
    }

    /**
     * Update dimensional data (reference data)
     */
    private void updateDimensionalData(LocalDate date) {
        logger.debug("Updating dimensional data for date: {}", date);
        
        // Update service dimension
        updateServiceDimension(date);
        
        // Update file type dimension
        updateFileTypeDimension(date);
        
        // Update time dimension
        updateTimeDimension(date);
    }

    /**
     * Generate daily summary statistics
     */
    private void generateDailySummaryStatistics(LocalDate date) {
        logger.debug("Generating daily summary statistics for date: {}", date);
        
        List<FileTransferAnalytics> dayData = analyticsRepository
            .findByTenantIdAndDateRange("", date, date);
        
        Map<String, Object> summaryStats = new HashMap<>();
        summaryStats.put("date", date);
        summaryStats.put("totalRecords", dayData.size());
        summaryStats.put("totalFiles", dayData.stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum());
        summaryStats.put("totalSuccessful", dayData.stream().mapToLong(FileTransferAnalytics::getSuccessfulTransfers).sum());
        summaryStats.put("totalFailed", dayData.stream().mapToLong(FileTransferAnalytics::getFailedTransfers).sum());
        summaryStats.put("totalDataVolume", dayData.stream().mapToLong(FileTransferAnalytics::getTotalDataVolumeBytes).sum());
        summaryStats.put("avgProcessingTime", dayData.stream()
            .filter(a -> a.getAvgProcessingTimeMs() != null)
            .mapToDouble(FileTransferAnalytics::getAvgProcessingTimeMs)
            .average().orElse(0.0));
        
        logger.info("Daily summary statistics: {}", summaryStats);
    }

    /**
     * Clean up old analytics data based on retention policy
     */
    @Scheduled(cron = "0 0 4 * * SUN") // Weekly on Sunday at 4:00 AM
    @Transactional
    public void cleanupOldData() {
        logger.info("Starting cleanup of old analytics data");
        
        try {
            // Keep data for 2 years (configurable)
            LocalDate cutoffDate = LocalDate.now().minusYears(2);
            
            List<FileTransferAnalytics> oldRecords = analyticsRepository.findRecordsForCleanup(cutoffDate);
            
            if (!oldRecords.isEmpty()) {
                logger.info("Deleting {} old analytics records before {}", oldRecords.size(), cutoffDate);
                analyticsRepository.deleteOldRecords(cutoffDate);
            }
            
        } catch (Exception e) {
            logger.error("Error in analytics data cleanup", e);
        }
    }

    // Placeholder methods for dimensional data updates
    private void updateServiceDimension(LocalDate date) {
        // Implementation would update service dimension table
    }

    private void updateFileTypeDimension(LocalDate date) {
        // Implementation would update file type dimension table
    }

    private void updateTimeDimension(LocalDate date) {
        // Implementation would update time dimension table
    }
}