package com.filetransfer.web.analytics.service;

import com.filetransfer.web.analytics.model.AnalyticsModels.*;
import com.filetransfer.web.analytics.repository.FileTransferAnalyticsRepository;
import com.filetransfer.web.analytics.repository.RealTimeAnalyticsEventRepository;
import com.filetransfer.web.analytics.repository.BusinessIntelligenceReportRepository;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Analytics Data Service - Core analytics processing and data aggregation
 * Handles real-time event processing, historical data analysis, and metric computation
 */
@Service
@Transactional
public class AnalyticsDataService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsDataService.class);

    @Autowired
    private FileTransferAnalyticsRepository analyticsRepository;

    @Autowired
    private RealTimeAnalyticsEventRepository eventRepository;

    @Autowired
    private BusinessIntelligenceReportRepository reportRepository;

    @Autowired
    private FileTransferRecordRepository fileTransferRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Record real-time analytics event
     */
    @Async
    public CompletableFuture<Void> recordAnalyticsEvent(String tenantId, AnalyticsEventType eventType, 
                                                       String serviceName, String subServiceName,
                                                       String fileName, Long fileSizeBytes, 
                                                       Long processingTimeMs, TransferStatus status,
                                                       String errorCode, String errorMessage,
                                                       Map<String, Object> metadata) {
        try {
            RealTimeAnalyticsEvent event = new RealTimeAnalyticsEvent(tenantId, eventType);
            event.setServiceName(serviceName);
            event.setSubServiceName(subServiceName);
            event.setFileName(fileName);
            event.setFileSizeBytes(fileSizeBytes);
            event.setProcessingTimeMs(processingTimeMs);
            event.setTransferStatus(status);
            event.setErrorCode(errorCode);
            event.setErrorMessage(errorMessage);
            
            if (metadata != null && !metadata.isEmpty()) {
                event.setMetadata(objectMapper.writeValueAsString(metadata));
            }
            
            eventRepository.save(event);
            
            logger.debug("Recorded analytics event: {} for tenant: {}", eventType, tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to record analytics event", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process real-time events into aggregated analytics (scheduled job)
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void processRealTimeEvents() {
        logger.info("Starting real-time analytics event processing");
        
        try {
            List<RealTimeAnalyticsEvent> unprocessedEvents = eventRepository.findUnprocessedEvents();
            
            if (unprocessedEvents.isEmpty()) {
                logger.debug("No unprocessed analytics events found");
                return;
            }
            
            logger.info("Processing {} unprocessed analytics events", unprocessedEvents.size());
            
            // Group events by tenant, service, date for aggregation
            Map<String, List<RealTimeAnalyticsEvent>> groupedEvents = unprocessedEvents.stream()
                .collect(Collectors.groupingBy(this::createEventGroupKey));
            
            for (Map.Entry<String, List<RealTimeAnalyticsEvent>> entry : groupedEvents.entrySet()) {
                processEventGroup(entry.getValue());
            }
            
            // Mark events as processed
            unprocessedEvents.forEach(event -> event.setProcessed(true));
            eventRepository.saveAll(unprocessedEvents);
            
            logger.info("Completed processing {} analytics events", unprocessedEvents.size());
            
        } catch (Exception e) {
            logger.error("Error processing real-time analytics events", e);
        }
    }

    /**
     * Generate daily analytics aggregations (scheduled job)
     */
    @Scheduled(cron = "0 30 1 * * *") // Daily at 1:30 AM
    @Transactional
    public void generateDailyAnalytics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.info("Generating daily analytics for date: {}", yesterday);
        
        try {
            List<String> tenants = fileTransferRepository.findDistinctTenantIds();
            
            for (String tenantId : tenants) {
                generateDailyAnalyticsForTenant(tenantId, yesterday);
            }
            
            logger.info("Completed daily analytics generation for {} tenants", tenants.size());
            
        } catch (Exception e) {
            logger.error("Error generating daily analytics", e);
        }
    }

    /**
     * Get analytics data for a specific tenant and date range
     */
    public List<FileTransferAnalytics> getAnalytics(String tenantId, LocalDate startDate, LocalDate endDate) {
        return analyticsRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate);
    }

    /**
     * Get analytics summary for a tenant
     */
    public Map<String, Object> getAnalyticsSummary(String tenantId, LocalDate startDate, LocalDate endDate) {
        List<FileTransferAnalytics> analytics = getAnalytics(tenantId, startDate, endDate);
        
        Map<String, Object> summary = new HashMap<>();
        
        // Volume metrics
        long totalFiles = analytics.stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum();
        long successfulTransfers = analytics.stream().mapToLong(FileTransferAnalytics::getSuccessfulTransfers).sum();
        long failedTransfers = analytics.stream().mapToLong(FileTransferAnalytics::getFailedTransfers).sum();
        long totalDataVolume = analytics.stream().mapToLong(FileTransferAnalytics::getTotalDataVolumeBytes).sum();
        
        summary.put("totalFiles", totalFiles);
        summary.put("successfulTransfers", successfulTransfers);
        summary.put("failedTransfers", failedTransfers);
        summary.put("successRate", totalFiles > 0 ? (double) successfulTransfers / totalFiles * 100 : 0.0);
        summary.put("totalDataVolumeBytes", totalDataVolume);
        summary.put("totalDataVolumeGB", totalDataVolume / (1024.0 * 1024.0 * 1024.0));
        
        // Performance metrics
        OptionalDouble avgProcessingTime = analytics.stream()
            .filter(a -> a.getAvgProcessingTimeMs() != null)
            .mapToDouble(FileTransferAnalytics::getAvgProcessingTimeMs)
            .average();
        
        summary.put("avgProcessingTimeMs", avgProcessingTime.orElse(0.0));
        summary.put("avgProcessingTimeSeconds", avgProcessingTime.orElse(0.0) / 1000.0);
        
        // Quality metrics
        long validationErrors = analytics.stream().mapToLong(FileTransferAnalytics::getValidationErrors).sum();
        long schemaFailures = analytics.stream().mapToLong(FileTransferAnalytics::getSchemaValidationFailures).sum();
        long corruptionIncidents = analytics.stream().mapToLong(FileTransferAnalytics::getFileCorruptionIncidents).sum();
        
        summary.put("validationErrors", validationErrors);
        summary.put("schemaValidationFailures", schemaFailures);
        summary.put("fileCorruptionIncidents", corruptionIncidents);
        summary.put("qualityScore", calculateQualityScore(totalFiles, validationErrors, schemaFailures, corruptionIncidents));
        
        // Business metrics
        long slaBreaches = analytics.stream().mapToLong(FileTransferAnalytics::getSlaBreaches).sum();
        long cutOffExtensions = analytics.stream().mapToLong(FileTransferAnalytics::getCutOffExtensions).sum();
        
        summary.put("slaBreaches", slaBreaches);
        summary.put("cutOffExtensions", cutOffExtensions);
        summary.put("slaComplianceRate", totalFiles > 0 ? (double) (totalFiles - slaBreaches) / totalFiles * 100 : 100.0);
        
        // Cost metrics
        double totalProcessingCost = analytics.stream()
            .filter(a -> a.getEstimatedProcessingCost() != null)
            .mapToDouble(FileTransferAnalytics::getEstimatedProcessingCost)
            .sum();
        
        double totalStorageCost = analytics.stream()
            .filter(a -> a.getStorageCost() != null)
            .mapToDouble(FileTransferAnalytics::getStorageCost)
            .sum();
        
        double totalBandwidthCost = analytics.stream()
            .filter(a -> a.getBandwidthCost() != null)
            .mapToDouble(FileTransferAnalytics::getBandwidthCost)
            .sum();
        
        summary.put("estimatedProcessingCost", totalProcessingCost);
        summary.put("storageCost", totalStorageCost);
        summary.put("bandwidthCost", totalBandwidthCost);
        summary.put("totalEstimatedCost", totalProcessingCost + totalStorageCost + totalBandwidthCost);
        
        // Time period
        summary.put("periodStart", startDate);
        summary.put("periodEnd", endDate);
        summary.put("periodDays", startDate.until(endDate).getDays() + 1);
        
        return summary;
    }

    /**
     * Get trending analytics data
     */
    public List<Map<String, Object>> getTrendingAnalytics(String tenantId, LocalDate startDate, 
                                                         LocalDate endDate, String granularity) {
        List<FileTransferAnalytics> analytics = getAnalytics(tenantId, startDate, endDate);
        
        // Group by date or other granularity
        Map<LocalDate, List<FileTransferAnalytics>> groupedData = analytics.stream()
            .collect(Collectors.groupingBy(FileTransferAnalytics::getAnalyticsDate));
        
        return groupedData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                LocalDate date = entry.getKey();
                List<FileTransferAnalytics> dayData = entry.getValue();
                
                Map<String, Object> trendPoint = new HashMap<>();
                trendPoint.put("date", date);
                trendPoint.put("totalFiles", dayData.stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum());
                trendPoint.put("successfulTransfers", dayData.stream().mapToLong(FileTransferAnalytics::getSuccessfulTransfers).sum());
                trendPoint.put("failedTransfers", dayData.stream().mapToLong(FileTransferAnalytics::getFailedTransfers).sum());
                trendPoint.put("totalDataVolumeBytes", dayData.stream().mapToLong(FileTransferAnalytics::getTotalDataVolumeBytes).sum());
                
                OptionalDouble avgProcessingTime = dayData.stream()
                    .filter(a -> a.getAvgProcessingTimeMs() != null)
                    .mapToDouble(FileTransferAnalytics::getAvgProcessingTimeMs)
                    .average();
                
                trendPoint.put("avgProcessingTimeMs", avgProcessingTime.orElse(0.0));
                
                return trendPoint;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get service-level analytics breakdown
     */
    public List<Map<String, Object>> getServiceAnalytics(String tenantId, LocalDate startDate, LocalDate endDate) {
        List<FileTransferAnalytics> analytics = getAnalytics(tenantId, startDate, endDate);
        
        // Group by service
        Map<String, List<FileTransferAnalytics>> serviceGroups = analytics.stream()
            .collect(Collectors.groupingBy(a -> 
                Optional.ofNullable(a.getServiceName()).orElse("Unknown Service")));
        
        return serviceGroups.entrySet().stream()
            .map(entry -> {
                String serviceName = entry.getKey();
                List<FileTransferAnalytics> serviceData = entry.getValue();
                
                Map<String, Object> serviceMetrics = new HashMap<>();
                serviceMetrics.put("serviceName", serviceName);
                serviceMetrics.put("totalFiles", serviceData.stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum());
                serviceMetrics.put("successfulTransfers", serviceData.stream().mapToLong(FileTransferAnalytics::getSuccessfulTransfers).sum());
                serviceMetrics.put("failedTransfers", serviceData.stream().mapToLong(FileTransferAnalytics::getFailedTransfers).sum());
                serviceMetrics.put("totalDataVolumeBytes", serviceData.stream().mapToLong(FileTransferAnalytics::getTotalDataVolumeBytes).sum());
                serviceMetrics.put("validationErrors", serviceData.stream().mapToLong(FileTransferAnalytics::getValidationErrors).sum());
                serviceMetrics.put("slaBreaches", serviceData.stream().mapToLong(FileTransferAnalytics::getSlaBreaches).sum());
                
                OptionalDouble avgProcessingTime = serviceData.stream()
                    .filter(a -> a.getAvgProcessingTimeMs() != null)
                    .mapToDouble(FileTransferAnalytics::getAvgProcessingTimeMs)
                    .average();
                
                serviceMetrics.put("avgProcessingTimeMs", avgProcessingTime.orElse(0.0));
                
                // Sub-services breakdown
                Map<String, List<FileTransferAnalytics>> subServiceGroups = serviceData.stream()
                    .collect(Collectors.groupingBy(a -> 
                        Optional.ofNullable(a.getSubServiceName()).orElse("Unknown SubService")));
                
                List<Map<String, Object>> subServices = subServiceGroups.entrySet().stream()
                    .map(subEntry -> {
                        Map<String, Object> subServiceMetrics = new HashMap<>();
                        subServiceMetrics.put("subServiceName", subEntry.getKey());
                        subServiceMetrics.put("totalFiles", subEntry.getValue().stream().mapToLong(FileTransferAnalytics::getTotalFiles).sum());
                        subServiceMetrics.put("successfulTransfers", subEntry.getValue().stream().mapToLong(FileTransferAnalytics::getSuccessfulTransfers).sum());
                        subServiceMetrics.put("failedTransfers", subEntry.getValue().stream().mapToLong(FileTransferAnalytics::getFailedTransfers).sum());
                        return subServiceMetrics;
                    })
                    .collect(Collectors.toList());
                
                serviceMetrics.put("subServices", subServices);
                
                return serviceMetrics;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("totalFiles"), (Long) a.get("totalFiles")))
            .collect(Collectors.toList());
    }

    /**
     * Get real-time analytics dashboard data
     */
    public Map<String, Object> getRealTimeDashboard(String tenantId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourAgo = now.minusHours(1);
        LocalDateTime dayAgo = now.minusDays(1);
        
        // Last hour metrics
        List<RealTimeAnalyticsEvent> lastHourEvents = eventRepository
            .findByTenantIdAndEventTimestampBetween(tenantId, hourAgo, now);
        
        dashboard.put("lastHourEvents", lastHourEvents.size());
        dashboard.put("lastHourUploads", lastHourEvents.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.FILE_UPLOAD_COMPLETED)
            .count());
        dashboard.put("lastHourFailures", lastHourEvents.stream()
            .filter(e -> e.getEventType().name().contains("FAILED"))
            .count());
        
        // Last 24 hours metrics
        List<RealTimeAnalyticsEvent> last24HourEvents = eventRepository
            .findByTenantIdAndEventTimestampBetween(tenantId, dayAgo, now);
        
        dashboard.put("last24HourEvents", last24HourEvents.size());
        dashboard.put("last24HourDataVolume", last24HourEvents.stream()
            .filter(e -> e.getFileSizeBytes() != null)
            .mapToLong(RealTimeAnalyticsEvent::getFileSizeBytes)
            .sum());
        
        // Current processing status
        long currentProcessing = eventRepository.countByTenantIdAndEventTypeAndEventTimestampAfter(
            tenantId, AnalyticsEventType.FILE_PROCESSING_STARTED, hourAgo);
        long completedProcessing = eventRepository.countByTenantIdAndEventTypeAndEventTimestampAfter(
            tenantId, AnalyticsEventType.FILE_PROCESSING_COMPLETED, hourAgo);
        
        dashboard.put("currentlyProcessing", Math.max(0, currentProcessing - completedProcessing));
        dashboard.put("processingQueueLength", currentProcessing - completedProcessing);
        
        // Recent errors
        List<RealTimeAnalyticsEvent> recentErrors = eventRepository
            .findRecentErrorsByTenant(tenantId, 10);
        
        dashboard.put("recentErrors", recentErrors.stream()
            .map(event -> {
                Map<String, Object> error = new HashMap<>();
                error.put("eventType", event.getEventType());
                error.put("timestamp", event.getEventTimestamp());
                error.put("serviceName", event.getServiceName());
                error.put("fileName", event.getFileName());
                error.put("errorCode", event.getErrorCode());
                error.put("errorMessage", event.getErrorMessage());
                return error;
            })
            .collect(Collectors.toList()));
        
        dashboard.put("generatedAt", now);
        
        return dashboard;
    }

    // Private helper methods

    private String createEventGroupKey(RealTimeAnalyticsEvent event) {
        return String.format("%s:%s:%s:%s", 
            event.getTenantId(),
            Optional.ofNullable(event.getServiceName()).orElse("unknown"),
            Optional.ofNullable(event.getSubServiceName()).orElse("unknown"),
            event.getEventTimestamp().toLocalDate().toString());
    }

    private void processEventGroup(List<RealTimeAnalyticsEvent> events) {
        if (events.isEmpty()) return;
        
        RealTimeAnalyticsEvent firstEvent = events.get(0);
        String tenantId = firstEvent.getTenantId();
        String serviceName = firstEvent.getServiceName();
        String subServiceName = firstEvent.getSubServiceName();
        LocalDate analyticsDate = firstEvent.getEventTimestamp().toLocalDate();
        
        // Find or create analytics record
        Optional<FileTransferAnalytics> existingAnalytics = analyticsRepository
            .findByTenantIdAndServiceNameAndSubServiceNameAndAnalyticsDate(
                tenantId, serviceName, subServiceName, analyticsDate);
        
        FileTransferAnalytics analytics = existingAnalytics.orElse(new FileTransferAnalytics());
        analytics.setTenantId(tenantId);
        analytics.setServiceName(serviceName);
        analytics.setSubServiceName(subServiceName);
        analytics.setAnalyticsDate(analyticsDate);
        
        // Aggregate metrics from events
        updateAnalyticsFromEvents(analytics, events);
        
        analyticsRepository.save(analytics);
    }

    private void updateAnalyticsFromEvents(FileTransferAnalytics analytics, List<RealTimeAnalyticsEvent> events) {
        // Volume metrics
        long uploads = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.FILE_UPLOAD_COMPLETED)
            .count();
        long successfulTransfers = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.FILE_PROCESSING_COMPLETED)
            .count();
        long failedTransfers = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.FILE_PROCESSING_FAILED)
            .count();
        
        analytics.setTotalFiles(analytics.getTotalFiles() + uploads);
        analytics.setSuccessfulTransfers(analytics.getSuccessfulTransfers() + successfulTransfers);
        analytics.setFailedTransfers(analytics.getFailedTransfers() + failedTransfers);
        
        // Data volume
        long dataVolume = events.stream()
            .filter(e -> e.getFileSizeBytes() != null)
            .mapToLong(RealTimeAnalyticsEvent::getFileSizeBytes)
            .sum();
        analytics.setTotalDataVolumeBytes(analytics.getTotalDataVolumeBytes() + dataVolume);
        
        // Performance metrics
        List<Long> processingTimes = events.stream()
            .filter(e -> e.getProcessingTimeMs() != null && e.getProcessingTimeMs() > 0)
            .map(RealTimeAnalyticsEvent::getProcessingTimeMs)
            .collect(Collectors.toList());
        
        if (!processingTimes.isEmpty()) {
            double avgTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long minTime = processingTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
            long maxTime = processingTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
            
            analytics.setAvgProcessingTimeMs(avgTime);
            analytics.setMinProcessingTimeMs(minTime);
            analytics.setMaxProcessingTimeMs(maxTime);
            
            // Calculate percentiles
            processingTimes.sort(Long::compareTo);
            int p95Index = (int) Math.ceil(processingTimes.size() * 0.95) - 1;
            int p99Index = (int) Math.ceil(processingTimes.size() * 0.99) - 1;
            
            analytics.setP95ProcessingTimeMs(processingTimes.get(Math.max(0, p95Index)));
            analytics.setP99ProcessingTimeMs(processingTimes.get(Math.max(0, p99Index)));
        }
        
        // Quality metrics
        long validationErrors = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.VALIDATION_FAILED)
            .count();
        long schemaFailures = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.SCHEMA_VALIDATION_FAILED)
            .count();
        
        analytics.setValidationErrors(analytics.getValidationErrors() + validationErrors);
        analytics.setSchemaValidationFailures(analytics.getSchemaValidationFailures() + schemaFailures);
        
        // Business metrics
        long slaBreaches = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.SLA_BREACH_DETECTED)
            .count();
        long cutOffExtensions = events.stream()
            .filter(e -> e.getEventType() == AnalyticsEventType.CUT_OFF_EXTENSION_REQUESTED)
            .count();
        
        analytics.setSlaBreaches(analytics.getSlaBreaches() + slaBreaches);
        analytics.setCutOffExtensions(analytics.getCutOffExtensions() + cutOffExtensions);
        
        analytics.setUpdatedAt(LocalDateTime.now());
    }

    private void generateDailyAnalyticsForTenant(String tenantId, LocalDate date) {
        // This method would generate comprehensive daily analytics from file transfer records
        // Implementation would involve complex queries and calculations
        logger.debug("Generating daily analytics for tenant: {} on date: {}", tenantId, date);
        
        // Get all file transfer records for the tenant on the specified date
        List<FileTransferRecord> records = fileTransferRepository
            .findByTenantIdAndCreatedAtBetween(tenantId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        
        if (records.isEmpty()) {
            logger.debug("No file transfer records found for tenant: {} on date: {}", tenantId, date);
            return;
        }
        
        // Group by service and sub-service
        Map<String, Map<String, List<FileTransferRecord>>> serviceGroups = records.stream()
            .collect(Collectors.groupingBy(
                record -> Optional.ofNullable(record.getServiceName()).orElse("Unknown"),
                Collectors.groupingBy(record -> Optional.ofNullable(record.getSubServiceName()).orElse("Unknown"))
            ));
        
        for (Map.Entry<String, Map<String, List<FileTransferRecord>>> serviceEntry : serviceGroups.entrySet()) {
            String serviceName = serviceEntry.getKey();
            
            for (Map.Entry<String, List<FileTransferRecord>> subServiceEntry : serviceEntry.getValue().entrySet()) {
                String subServiceName = subServiceEntry.getKey();
                List<FileTransferRecord> serviceRecords = subServiceEntry.getValue();
                
                generateAnalyticsFromRecords(tenantId, serviceName, subServiceName, date, serviceRecords);
            }
        }
    }

    private void generateAnalyticsFromRecords(String tenantId, String serviceName, String subServiceName,
                                            LocalDate date, List<FileTransferRecord> records) {
        // Create or update analytics record
        Optional<FileTransferAnalytics> existingAnalytics = analyticsRepository
            .findByTenantIdAndServiceNameAndSubServiceNameAndAnalyticsDate(
                tenantId, serviceName, subServiceName, date);
        
        FileTransferAnalytics analytics = existingAnalytics.orElse(new FileTransferAnalytics());
        analytics.setTenantId(tenantId);
        analytics.setServiceName(serviceName);
        analytics.setSubServiceName(subServiceName);
        analytics.setAnalyticsDate(date);
        
        // Calculate metrics from records
        analytics.setTotalFiles((long) records.size());
        analytics.setSuccessfulTransfers(records.stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()) || "ARCHIVED".equals(r.getStatus()))
            .count());
        analytics.setFailedTransfers(records.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .count());
        
        analytics.setTotalDataVolumeBytes(records.stream()
            .mapToLong(r -> Optional.ofNullable(r.getFileSize()).orElse(0L))
            .sum());
        
        // Calculate processing times
        List<Long> processingTimes = records.stream()
            .filter(r -> r.getProcessingStartTime() != null && r.getProcessingEndTime() != null)
            .map(r -> java.time.Duration.between(r.getProcessingStartTime(), r.getProcessingEndTime()).toMillis())
            .collect(Collectors.toList());
        
        if (!processingTimes.isEmpty()) {
            analytics.setAvgProcessingTimeMs(processingTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
            analytics.setMinProcessingTimeMs(processingTimes.stream().mapToLong(Long::longValue).min().orElse(0L));
            analytics.setMaxProcessingTimeMs(processingTimes.stream().mapToLong(Long::longValue).max().orElse(0L));
            
            processingTimes.sort(Long::compareTo);
            int p95Index = (int) Math.ceil(processingTimes.size() * 0.95) - 1;
            int p99Index = (int) Math.ceil(processingTimes.size() * 0.99) - 1;
            
            analytics.setP95ProcessingTimeMs(processingTimes.get(Math.max(0, p95Index)));
            analytics.setP99ProcessingTimeMs(processingTimes.get(Math.max(0, p99Index)));
        }
        
        // Estimate costs (simplified calculation)
        double processingCost = records.size() * 0.001; // $0.001 per file
        double storageCost = analytics.getTotalDataVolumeBytes() * 0.000000023; // $0.023 per GB
        double bandwidthCost = analytics.getTotalDataVolumeBytes() * 0.00000009; // $0.09 per GB
        
        analytics.setEstimatedProcessingCost(processingCost);
        analytics.setStorageCost(storageCost);
        analytics.setBandwidthCost(bandwidthCost);
        
        analyticsRepository.save(analytics);
        
        logger.debug("Generated daily analytics for tenant: {}, service: {}, sub-service: {}, date: {}", 
                    tenantId, serviceName, subServiceName, date);
    }

    private double calculateQualityScore(long totalFiles, long validationErrors, 
                                       long schemaFailures, long corruptionIncidents) {
        if (totalFiles == 0) return 100.0;
        
        long totalIssues = validationErrors + schemaFailures + corruptionIncidents;
        double errorRate = (double) totalIssues / totalFiles;
        
        // Quality score: 100% - error rate percentage, minimum 0%
        return Math.max(0.0, (1.0 - errorRate) * 100.0);
    }
}