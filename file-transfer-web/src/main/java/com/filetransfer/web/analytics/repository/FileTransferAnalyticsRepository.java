package com.filetransfer.web.analytics.repository;

import com.filetransfer.web.analytics.model.AnalyticsModels.FileTransferAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for File Transfer Analytics data
 * Provides optimized queries for business intelligence and reporting
 */
@Repository
public interface FileTransferAnalyticsRepository extends JpaRepository<FileTransferAnalytics, Long> {

    /**
     * Find analytics by tenant and date range
     */
    @Query("SELECT fta FROM FileTransferAnalytics fta WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate ORDER BY fta.analyticsDate ASC")
    List<FileTransferAnalytics> findByTenantIdAndDateRange(@Param("tenantId") String tenantId, 
                                                          @Param("startDate") LocalDate startDate, 
                                                          @Param("endDate") LocalDate endDate);

    /**
     * Find analytics by tenant, service, sub-service and date
     */
    @Query("SELECT fta FROM FileTransferAnalytics fta WHERE fta.tenantId = :tenantId AND fta.serviceName = :serviceName AND fta.subServiceName = :subServiceName AND fta.analyticsDate = :analyticsDate")
    Optional<FileTransferAnalytics> findByTenantIdAndServiceNameAndSubServiceNameAndAnalyticsDate(@Param("tenantId") String tenantId, 
                                                                                                  @Param("serviceName") String serviceName, 
                                                                                                  @Param("subServiceName") String subServiceName, 
                                                                                                  @Param("analyticsDate") LocalDate analyticsDate);

    /**
     * Find analytics by tenant and service for date range
     */
    @Query("SELECT fta FROM FileTransferAnalytics fta WHERE fta.tenantId = :tenantId AND fta.serviceName = :serviceName AND fta.analyticsDate BETWEEN :startDate AND :endDate ORDER BY fta.analyticsDate ASC")
    List<FileTransferAnalytics> findByTenantIdAndServiceNameAndDateRange(@Param("tenantId") String tenantId, 
                                                                        @Param("serviceName") String serviceName, 
                                                                        @Param("startDate") LocalDate startDate, 
                                                                        @Param("endDate") LocalDate endDate);

    /**
     * Find analytics by tenant and sub-service for date range
     */
    @Query("SELECT fta FROM FileTransferAnalytics fta WHERE fta.tenantId = :tenantId AND fta.serviceName = :serviceName AND fta.subServiceName = :subServiceName AND fta.analyticsDate BETWEEN :startDate AND :endDate ORDER BY fta.analyticsDate ASC")
    List<FileTransferAnalytics> findByTenantIdAndSubServiceNameAndDateRange(@Param("tenantId") String tenantId, 
                                                                           @Param("serviceName") String serviceName, 
                                                                           @Param("subServiceName") String subServiceName, 
                                                                           @Param("startDate") LocalDate startDate, 
                                                                           @Param("endDate") LocalDate endDate);

    /**
     * Get aggregated metrics for tenant and date range
     */
    @Query("SELECT " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.successfulTransfers) as successfulTransfers, " +
           "SUM(fta.failedTransfers) as failedTransfers, " +
           "SUM(fta.totalDataVolumeBytes) as totalDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as avgProcessingTime, " +
           "SUM(fta.validationErrors) as validationErrors, " +
           "SUM(fta.schemaValidationFailures) as schemaFailures, " +
           "SUM(fta.slaBreaches) as slaBreaches, " +
           "SUM(fta.estimatedProcessingCost) as processingCost, " +
           "SUM(fta.storageCost) as storageCost, " +
           "SUM(fta.bandwidthCost) as bandwidthCost " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getAggregatedMetrics(@Param("tenantId") String tenantId, 
                                 @Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);

    /**
     * Get service-level aggregated metrics
     */
    @Query("SELECT " +
           "fta.serviceName, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.successfulTransfers) as successfulTransfers, " +
           "SUM(fta.failedTransfers) as failedTransfers, " +
           "SUM(fta.totalDataVolumeBytes) as totalDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as avgProcessingTime, " +
           "SUM(fta.validationErrors) as validationErrors, " +
           "SUM(fta.slaBreaches) as slaBreaches " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.serviceName " +
           "ORDER BY SUM(fta.totalFiles) DESC")
    List<Object[]> getServiceLevelMetrics(@Param("tenantId") String tenantId, 
                                         @Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    /**
     * Get sub-service level aggregated metrics
     */
    @Query("SELECT " +
           "fta.serviceName, " +
           "fta.subServiceName, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.successfulTransfers) as successfulTransfers, " +
           "SUM(fta.failedTransfers) as failedTransfers, " +
           "SUM(fta.totalDataVolumeBytes) as totalDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as avgProcessingTime " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.serviceName, fta.subServiceName " +
           "ORDER BY SUM(fta.totalFiles) DESC")
    List<Object[]> getSubServiceLevelMetrics(@Param("tenantId") String tenantId, 
                                            @Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * Get daily trending data for visualization
     */
    @Query("SELECT " +
           "fta.analyticsDate, " +
           "SUM(fta.totalFiles) as dailyFiles, " +
           "SUM(fta.successfulTransfers) as dailySuccessful, " +
           "SUM(fta.failedTransfers) as dailyFailed, " +
           "SUM(fta.totalDataVolumeBytes) as dailyDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as dailyAvgProcessingTime " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.analyticsDate " +
           "ORDER BY fta.analyticsDate ASC")
    List<Object[]> getDailyTrendingData(@Param("tenantId") String tenantId, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    /**
     * Get performance percentiles for SLA analysis
     */
    @Query("SELECT " +
           "AVG(fta.avgProcessingTimeMs) as avgProcessingTime, " +
           "AVG(fta.p95ProcessingTimeMs) as p95ProcessingTime, " +
           "AVG(fta.p99ProcessingTimeMs) as p99ProcessingTime, " +
           "MIN(fta.minProcessingTimeMs) as minProcessingTime, " +
           "MAX(fta.maxProcessingTimeMs) as maxProcessingTime " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getPerformancePercentiles(@Param("tenantId") String tenantId, 
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    /**
     * Get quality metrics summary
     */
    @Query("SELECT " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.validationErrors) as totalValidationErrors, " +
           "SUM(fta.schemaValidationFailures) as totalSchemaFailures, " +
           "SUM(fta.fileCorruptionIncidents) as totalCorruption, " +
           "AVG(CASE WHEN fta.totalFiles > 0 THEN (1.0 - (CAST(fta.validationErrors + fta.schemaValidationFailures + fta.fileCorruptionIncidents AS DOUBLE) / fta.totalFiles)) * 100.0 ELSE 100.0 END) as avgQualityScore " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getQualityMetrics(@Param("tenantId") String tenantId, 
                              @Param("startDate") LocalDate startDate, 
                              @Param("endDate") LocalDate endDate);

    /**
     * Get cost breakdown
     */
    @Query("SELECT " +
           "SUM(fta.estimatedProcessingCost) as totalProcessingCost, " +
           "SUM(fta.storageCost) as totalStorageCost, " +
           "SUM(fta.bandwidthCost) as totalBandwidthCost, " +
           "SUM(fta.estimatedProcessingCost + fta.storageCost + fta.bandwidthCost) as totalCost, " +
           "AVG((fta.estimatedProcessingCost + fta.storageCost + fta.bandwidthCost) / NULLIF(fta.totalFiles, 0)) as avgCostPerFile, " +
           "AVG((fta.estimatedProcessingCost + fta.storageCost + fta.bandwidthCost) / NULLIF(fta.totalDataVolumeBytes, 0) * 1073741824) as avgCostPerGB " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getCostBreakdown(@Param("tenantId") String tenantId, 
                             @Param("startDate") LocalDate startDate, 
                             @Param("endDate") LocalDate endDate);

    /**
     * Get SLA compliance metrics
     */
    @Query("SELECT " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.slaBreaches) as totalSlaBreaches, " +
           "AVG(CASE WHEN fta.totalFiles > 0 THEN (1.0 - (CAST(fta.slaBreaches AS DOUBLE) / fta.totalFiles)) * 100.0 ELSE 100.0 END) as avgComplianceRate, " +
           "SUM(fta.cutOffExtensions) as totalCutOffExtensions, " +
           "SUM(fta.holidayProcessing) as totalHolidayProcessing " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate")
    Object[] getSlaComplianceMetrics(@Param("tenantId") String tenantId, 
                                    @Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate);

    /**
     * Find top performing services by success rate
     */
    @Query("SELECT " +
           "fta.serviceName, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.successfulTransfers) as successfulTransfers, " +
           "CASE WHEN SUM(fta.totalFiles) > 0 THEN (CAST(SUM(fta.successfulTransfers) AS DOUBLE) / SUM(fta.totalFiles)) * 100.0 ELSE 0.0 END as successRate " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.serviceName " +
           "HAVING SUM(fta.totalFiles) >= :minFiles " +
           "ORDER BY successRate DESC")
    List<Object[]> getTopPerformingServices(@Param("tenantId") String tenantId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate, 
                                           @Param("minFiles") Long minFiles);

    /**
     * Find services with highest error rates
     */
    @Query("SELECT " +
           "fta.serviceName, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.failedTransfers) as failedTransfers, " +
           "SUM(fta.validationErrors) as validationErrors, " +
           "SUM(fta.schemaValidationFailures) as schemaFailures, " +
           "CASE WHEN SUM(fta.totalFiles) > 0 THEN (CAST(SUM(fta.failedTransfers + fta.validationErrors + fta.schemaValidationFailures) AS DOUBLE) / SUM(fta.totalFiles)) * 100.0 ELSE 0.0 END as errorRate " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.serviceName " +
           "HAVING SUM(fta.totalFiles) >= :minFiles " +
           "ORDER BY errorRate DESC")
    List<Object[]> getServicesWithHighestErrorRates(@Param("tenantId") String tenantId, 
                                                    @Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate, 
                                                    @Param("minFiles") Long minFiles);

    /**
     * Find data volume trends by file type
     */
    @Query("SELECT " +
           "fta.fileType, " +
           "fta.analyticsDate, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.totalDataVolumeBytes) as totalDataVolume " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.fileType, fta.analyticsDate " +
           "ORDER BY fta.analyticsDate ASC, fta.fileType ASC")
    List<Object[]> getDataVolumeTrendsByFileType(@Param("tenantId") String tenantId, 
                                                @Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);

    /**
     * Get transfer direction analysis
     */
    @Query("SELECT " +
           "fta.direction, " +
           "SUM(fta.totalFiles) as totalFiles, " +
           "SUM(fta.successfulTransfers) as successfulTransfers, " +
           "SUM(fta.totalDataVolumeBytes) as totalDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as avgProcessingTime " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.direction " +
           "ORDER BY SUM(fta.totalFiles) DESC")
    List<Object[]> getTransferDirectionAnalysis(@Param("tenantId") String tenantId, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);

    /**
     * Find tenants with activity on a specific date (for scheduled report generation)
     */
    @Query("SELECT DISTINCT fta.tenantId FROM FileTransferAnalytics fta WHERE fta.analyticsDate = :date AND fta.totalFiles > 0")
    List<String> findActiveTenants(@Param("date") LocalDate date);

    /**
     * Get monthly aggregated data for trend analysis
     */
    @Query("SELECT " +
           "YEAR(fta.analyticsDate) as year, " +
           "MONTH(fta.analyticsDate) as month, " +
           "SUM(fta.totalFiles) as monthlyFiles, " +
           "SUM(fta.successfulTransfers) as monthlySuccessful, " +
           "SUM(fta.totalDataVolumeBytes) as monthlyDataVolume, " +
           "AVG(fta.avgProcessingTimeMs) as monthlyAvgProcessingTime " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(fta.analyticsDate), MONTH(fta.analyticsDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> getMonthlyTrendingData(@Param("tenantId") String tenantId, 
                                         @Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    /**
     * Get analytics data for capacity planning (growth analysis)
     */
    @Query("SELECT " +
           "fta.analyticsDate, " +
           "SUM(fta.totalFiles) as dailyFiles, " +
           "SUM(fta.totalDataVolumeBytes) as dailyDataVolume, " +
           "MAX(fta.maxProcessingTimeMs) as peakProcessingTime, " +
           "COUNT(DISTINCT fta.serviceName) as activeServices " +
           "FROM FileTransferAnalytics fta " +
           "WHERE fta.tenantId = :tenantId AND fta.analyticsDate BETWEEN :startDate AND :endDate " +
           "GROUP BY fta.analyticsDate " +
           "ORDER BY fta.analyticsDate ASC")
    List<Object[]> getCapacityPlanningData(@Param("tenantId") String tenantId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    /**
     * Find analytics records for cleanup (old data beyond retention period)
     */
    @Query("SELECT fta FROM FileTransferAnalytics fta WHERE fta.analyticsDate < :cutoffDate")
    List<FileTransferAnalytics> findRecordsForCleanup(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Delete analytics records older than specified date
     */
    @Query("DELETE FROM FileTransferAnalytics fta WHERE fta.analyticsDate < :cutoffDate")
    void deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
}