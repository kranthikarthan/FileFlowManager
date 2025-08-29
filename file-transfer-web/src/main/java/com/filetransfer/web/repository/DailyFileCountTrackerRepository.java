package com.filetransfer.web.repository;

import com.filetransfer.web.entity.DailyFileCountTracker;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyFileCountTrackerRepository extends JpaRepository<DailyFileCountTracker, Long> {
    
    // Find tracker for specific day and subservice
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.serviceName = :serviceName AND dfc.subServiceName = :subServiceName AND dfc.processingDate = :processingDate AND dfc.fileType = :fileType AND dfc.direction = :direction")
    Optional<DailyFileCountTracker> findBySubServiceAndDate(@Param("tenantId") String tenantId, 
                                                           @Param("serviceName") String serviceName, 
                                                           @Param("subServiceName") String subServiceName, 
                                                           @Param("processingDate") LocalDate processingDate,
                                                           @Param("fileType") FileType fileType,
                                                           @Param("direction") TransferDirection direction);
    
    // Find all trackers for a specific date
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.processingDate = :processingDate ORDER BY dfc.serviceName, dfc.subServiceName")
    List<DailyFileCountTracker> findByTenantAndDate(@Param("tenantId") String tenantId, @Param("processingDate") LocalDate processingDate);
    
    // Find trackers with discrepancies
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.validationStatus = 'DISCREPANCY' ORDER BY dfc.processingDate DESC, dfc.discrepancyCount DESC")
    List<DailyFileCountTracker> findDiscrepanciesByTenant(@Param("tenantId") String tenantId);
    
    // Find trackers pending EOT validation
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.validationStatus = 'PENDING' AND dfc.processingDate <= :cutoffDate ORDER BY dfc.processingDate ASC")
    List<DailyFileCountTracker> findPendingValidation(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDate cutoffDate);
    
    // Find trackers by date range
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.processingDate BETWEEN :startDate AND :endDate ORDER BY dfc.processingDate DESC, dfc.serviceName")
    List<DailyFileCountTracker> findByDateRange(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Find trackers for specific subservice and date range
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.serviceName = :serviceName AND dfc.subServiceName = :subServiceName AND dfc.processingDate BETWEEN :startDate AND :endDate ORDER BY dfc.processingDate DESC")
    List<DailyFileCountTracker> findBySubServiceAndDateRange(@Param("tenantId") String tenantId, 
                                                            @Param("serviceName") String serviceName, 
                                                            @Param("subServiceName") String subServiceName, 
                                                            @Param("startDate") LocalDate startDate, 
                                                            @Param("endDate") LocalDate endDate);
    
    // Find trackers missing SOT files
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.sotReceived = false AND dfc.processingDate <= :cutoffDate ORDER BY dfc.processingDate ASC")
    List<DailyFileCountTracker> findMissingSot(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDate cutoffDate);
    
    // Find trackers missing EOT files
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.eotReceived = false AND dfc.processingDate <= :cutoffDate ORDER BY dfc.processingDate ASC")
    List<DailyFileCountTracker> findMissingEot(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDate cutoffDate);
    
    // Get validation statistics
    @Query("SELECT dfc.validationStatus, COUNT(dfc) FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.processingDate BETWEEN :startDate AND :endDate GROUP BY dfc.validationStatus")
    List<Object[]> getValidationStatistics(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Get count summary by subservice
    @Query("SELECT dfc.serviceName, dfc.subServiceName, COUNT(dfc), SUM(dfc.actualCount), SUM(dfc.discrepancyCount) " +
           "FROM DailyFileCountTracker dfc " +
           "WHERE dfc.tenantId = :tenantId AND dfc.processingDate BETWEEN :startDate AND :endDate " +
           "GROUP BY dfc.serviceName, dfc.subServiceName " +
           "ORDER BY dfc.serviceName, dfc.subServiceName")
    List<Object[]> getCountSummaryBySubService(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Find recent validation errors
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.validationStatus = 'ERROR' ORDER BY dfc.validatedAt DESC")
    List<DailyFileCountTracker> findRecentValidationErrors(@Param("tenantId") String tenantId);
    
    // Clean up old tracking records (for maintenance)
    @Query("DELETE FROM DailyFileCountTracker dfc WHERE dfc.processingDate < :cutoffDate")
    void deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
    
    // Find trackers that need attention (discrepancies, missing files, errors)
    @Query("SELECT dfc FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND " +
           "(dfc.validationStatus IN ('DISCREPANCY', 'MISSING_EOT', 'MISSING_SOT', 'ERROR') OR " +
           "(dfc.validationStatus = 'PENDING' AND dfc.processingDate <= :alertDate)) " +
           "ORDER BY dfc.processingDate DESC, dfc.validationStatus")
    List<DailyFileCountTracker> findTrackersNeedingAttention(@Param("tenantId") String tenantId, @Param("alertDate") LocalDate alertDate);
    
    // Count total discrepancies for a period
    @Query("SELECT SUM(dfc.discrepancyCount) FROM DailyFileCountTracker dfc WHERE dfc.tenantId = :tenantId AND dfc.processingDate BETWEEN :startDate AND :endDate AND dfc.discrepancyCount IS NOT NULL")
    Long sumDiscrepanciesForPeriod(@Param("tenantId") String tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}