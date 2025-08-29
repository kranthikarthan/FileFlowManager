package com.filetransfer.web.analytics.repository;

import com.filetransfer.web.analytics.model.AnalyticsModels.RealTimeAnalyticsEvent;
import com.filetransfer.web.analytics.model.AnalyticsModels.AnalyticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Real-Time Analytics Events
 * Handles streaming analytics data and event processing
 */
@Repository
public interface RealTimeAnalyticsEventRepository extends JpaRepository<RealTimeAnalyticsEvent, Long> {

    /**
     * Find unprocessed events for batch processing
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.processed = false ORDER BY rtae.eventTimestamp ASC")
    List<RealTimeAnalyticsEvent> findUnprocessedEvents();

    /**
     * Find unprocessed events with limit for batch processing
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.processed = false ORDER BY rtae.eventTimestamp ASC")
    List<RealTimeAnalyticsEvent> findUnprocessedEvents(@Param("limit") int limit);

    /**
     * Find events by tenant and time range
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findByTenantIdAndEventTimestampBetween(@Param("tenantId") String tenantId, 
                                                                       @Param("startTime") LocalDateTime startTime, 
                                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find events by tenant, event type and time range
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventType = :eventType AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findByTenantIdAndEventTypeAndEventTimestampBetween(@Param("tenantId") String tenantId, 
                                                                                   @Param("eventType") AnalyticsEventType eventType, 
                                                                                   @Param("startTime") LocalDateTime startTime, 
                                                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * Count events by tenant and event type after specified time
     */
    @Query("SELECT COUNT(rtae) FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventType = :eventType AND rtae.eventTimestamp > :afterTime")
    long countByTenantIdAndEventTypeAndEventTimestampAfter(@Param("tenantId") String tenantId, 
                                                          @Param("eventType") AnalyticsEventType eventType, 
                                                          @Param("afterTime") LocalDateTime afterTime);

    /**
     * Find recent error events for dashboard
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND (rtae.eventType LIKE '%FAILED%' OR rtae.eventType = 'SYSTEM_ERROR' OR rtae.eventType = 'SECURITY_INCIDENT') ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findRecentErrorsByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    /**
     * Find events by service and time range
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.serviceName = :serviceName AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findByTenantIdAndServiceNameAndEventTimestampBetween(@Param("tenantId") String tenantId, 
                                                                                     @Param("serviceName") String serviceName, 
                                                                                     @Param("startTime") LocalDateTime startTime, 
                                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find events by sub-service and time range
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.serviceName = :serviceName AND rtae.subServiceName = :subServiceName AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findByTenantIdAndSubServiceAndEventTimestampBetween(@Param("tenantId") String tenantId, 
                                                                                    @Param("serviceName") String serviceName, 
                                                                                    @Param("subServiceName") String subServiceName, 
                                                                                    @Param("startTime") LocalDateTime startTime, 
                                                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * Get event counts by type for dashboard
     */
    @Query("SELECT rtae.eventType, COUNT(rtae) FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY rtae.eventType ORDER BY COUNT(rtae) DESC")
    List<Object[]> getEventCountsByType(@Param("tenantId") String tenantId, 
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Get hourly event distribution for trend analysis
     */
    @Query("SELECT HOUR(rtae.eventTimestamp) as hour, COUNT(rtae) as eventCount FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY HOUR(rtae.eventTimestamp) ORDER BY hour ASC")
    List<Object[]> getHourlyEventDistribution(@Param("tenantId") String tenantId, 
                                             @Param("startTime") LocalDateTime startTime, 
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * Get average processing times by service
     */
    @Query("SELECT rtae.serviceName, AVG(rtae.processingTimeMs) as avgProcessingTime, COUNT(rtae) as eventCount FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.processingTimeMs IS NOT NULL AND rtae.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY rtae.serviceName ORDER BY avgProcessingTime DESC")
    List<Object[]> getAverageProcessingTimesByService(@Param("tenantId") String tenantId, 
                                                     @Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find file size distribution
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN rtae.fileSizeBytes < 1024 THEN 'SMALL' " +
           "  WHEN rtae.fileSizeBytes < 1048576 THEN 'MEDIUM' " +
           "  WHEN rtae.fileSizeBytes < 104857600 THEN 'LARGE' " +
           "  ELSE 'XLARGE' " +
           "END as sizeCategory, " +
           "COUNT(rtae) as fileCount, " +
           "AVG(rtae.processingTimeMs) as avgProcessingTime " +
           "FROM RealTimeAnalyticsEvent rtae " +
           "WHERE rtae.tenantId = :tenantId AND rtae.fileSizeBytes IS NOT NULL AND rtae.processingTimeMs IS NOT NULL AND rtae.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY " +
           "CASE " +
           "  WHEN rtae.fileSizeBytes < 1024 THEN 'SMALL' " +
           "  WHEN rtae.fileSizeBytes < 1048576 THEN 'MEDIUM' " +
           "  WHEN rtae.fileSizeBytes < 104857600 THEN 'LARGE' " +
           "  ELSE 'XLARGE' " +
           "END " +
           "ORDER BY fileCount DESC")
    List<Object[]> getFileSizeDistribution(@Param("tenantId") String tenantId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * Find events with performance anomalies (slow processing times)
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.processingTimeMs > :thresholdMs AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.processingTimeMs DESC")
    List<RealTimeAnalyticsEvent> findSlowProcessingEvents(@Param("tenantId") String tenantId, 
                                                         @Param("thresholdMs") Long thresholdMs, 
                                                         @Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime);

    /**
     * Find events with large file sizes for capacity analysis
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.fileSizeBytes > :thresholdBytes AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.fileSizeBytes DESC")
    List<RealTimeAnalyticsEvent> findLargeFileEvents(@Param("tenantId") String tenantId, 
                                                    @Param("thresholdBytes") Long thresholdBytes, 
                                                    @Param("startTime") LocalDateTime startTime, 
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * Get data volume trends by hour
     */
    @Query("SELECT HOUR(rtae.eventTimestamp) as hour, SUM(rtae.fileSizeBytes) as totalDataVolume, COUNT(rtae) as fileCount FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.fileSizeBytes IS NOT NULL AND rtae.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY HOUR(rtae.eventTimestamp) ORDER BY hour ASC")
    List<Object[]> getDataVolumeTrendsByHour(@Param("tenantId") String tenantId, 
                                            @Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * Find error patterns by error code
     */
    @Query("SELECT rtae.errorCode, rtae.serviceName, COUNT(rtae) as errorCount FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.errorCode IS NOT NULL AND rtae.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY rtae.errorCode, rtae.serviceName ORDER BY errorCount DESC")
    List<Object[]> getErrorPatterns(@Param("tenantId") String tenantId, 
                                   @Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * Get success/failure rates by service
     */
    @Query("SELECT " +
           "rtae.serviceName, " +
           "SUM(CASE WHEN rtae.transferStatus = 'COMPLETED' THEN 1 ELSE 0 END) as successCount, " +
           "SUM(CASE WHEN rtae.transferStatus = 'FAILED' THEN 1 ELSE 0 END) as failureCount, " +
           "COUNT(rtae) as totalCount, " +
           "CASE WHEN COUNT(rtae) > 0 THEN (CAST(SUM(CASE WHEN rtae.transferStatus = 'COMPLETED' THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(rtae)) * 100.0 ELSE 0.0 END as successRate " +
           "FROM RealTimeAnalyticsEvent rtae " +
           "WHERE rtae.tenantId = :tenantId AND rtae.transferStatus IS NOT NULL AND rtae.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY rtae.serviceName " +
           "ORDER BY successRate DESC")
    List<Object[]> getSuccessFailureRatesByService(@Param("tenantId") String tenantId, 
                                                  @Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * Find concurrent processing events for load analysis
     */
    @Query("SELECT COUNT(rtae) FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventType = 'FILE_PROCESSING_STARTED' AND rtae.eventTimestamp BETWEEN :startTime AND :endTime")
    long countConcurrentProcessingEvents(@Param("tenantId") String tenantId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * Get throughput metrics (files per minute)
     */
    @Query("SELECT " +
           "DATE_FORMAT(rtae.eventTimestamp, '%Y-%m-%d %H:%i') as minute, " +
           "COUNT(rtae) as filesPerMinute " +
           "FROM RealTimeAnalyticsEvent rtae " +
           "WHERE rtae.tenantId = :tenantId AND rtae.eventType IN ('FILE_UPLOAD_COMPLETED', 'FILE_PROCESSING_COMPLETED') AND rtae.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE_FORMAT(rtae.eventTimestamp, '%Y-%m-%d %H:%i') " +
           "ORDER BY minute ASC")
    List<Object[]> getThroughputMetrics(@Param("tenantId") String tenantId, 
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find security-related events
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.tenantId = :tenantId AND rtae.eventType = 'SECURITY_INCIDENT' AND rtae.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY rtae.eventTimestamp DESC")
    List<RealTimeAnalyticsEvent> findSecurityEvents(@Param("tenantId") String tenantId, 
                                                   @Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * Get system health indicators
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN rtae.eventType LIKE '%COMPLETED%' THEN 1 END) as completedEvents, " +
           "COUNT(CASE WHEN rtae.eventType LIKE '%FAILED%' THEN 1 END) as failedEvents, " +
           "COUNT(CASE WHEN rtae.eventType = 'SYSTEM_ERROR' THEN 1 END) as systemErrors, " +
           "COUNT(CASE WHEN rtae.eventType = 'PERFORMANCE_ANOMALY' THEN 1 END) as performanceAnomalies, " +
           "AVG(rtae.processingTimeMs) as avgProcessingTime " +
           "FROM RealTimeAnalyticsEvent rtae " +
           "WHERE rtae.tenantId = :tenantId AND rtae.eventTimestamp BETWEEN :startTime AND :endTime")
    Object[] getSystemHealthIndicators(@Param("tenantId") String tenantId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * Find events for data retention cleanup
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.processed = true AND rtae.eventTimestamp < :cutoffTime")
    List<RealTimeAnalyticsEvent> findEventsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete processed events older than specified time
     */
    @Query("DELETE FROM RealTimeAnalyticsEvent rtae WHERE rtae.processed = true AND rtae.eventTimestamp < :cutoffTime")
    void deleteOldProcessedEvents(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Get event processing lag (unprocessed events older than threshold)
     */
    @Query("SELECT COUNT(rtae) FROM RealTimeAnalyticsEvent rtae WHERE rtae.processed = false AND rtae.eventTimestamp < :thresholdTime")
    long getEventProcessingLag(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Find events by correlation ID (for tracing)
     */
    @Query("SELECT rtae FROM RealTimeAnalyticsEvent rtae WHERE rtae.metadata LIKE CONCAT('%correlationId%', :correlationId, '%') ORDER BY rtae.eventTimestamp ASC")
    List<RealTimeAnalyticsEvent> findEventsByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Get real-time metrics for monitoring dashboard (last N minutes)
     */
    @Query("SELECT " +
           "COUNT(rtae) as totalEvents, " +
           "COUNT(CASE WHEN rtae.eventType LIKE '%COMPLETED%' THEN 1 END) as completedEvents, " +
           "COUNT(CASE WHEN rtae.eventType LIKE '%FAILED%' THEN 1 END) as failedEvents, " +
           "SUM(rtae.fileSizeBytes) as totalDataVolume, " +
           "AVG(rtae.processingTimeMs) as avgProcessingTime, " +
           "MAX(rtae.processingTimeMs) as maxProcessingTime " +
           "FROM RealTimeAnalyticsEvent rtae " +
           "WHERE rtae.tenantId = :tenantId AND rtae.eventTimestamp > :sinceTime")
    Object[] getRealTimeMetrics(@Param("tenantId") String tenantId, 
                               @Param("sinceTime") LocalDateTime sinceTime);
}