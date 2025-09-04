package com.filetransfer.web.repository;

import com.filetransfer.web.entity.DataProfileRecord;
import com.filetransfer.web.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for data profile records
 */
@Repository
public interface DataProfileRecordRepository extends JpaRepository<DataProfileRecord, Long> {
    
    // Find by file transfer ID
    Optional<DataProfileRecord> findByFileTransferId(Long fileTransferId);
    
    // Find by tenant and file transfer ID
    Optional<DataProfileRecord> findByTenantIdAndFileTransferId(String tenantId, Long fileTransferId);
    
    // Find by tenant ID
    List<DataProfileRecord> findByTenantId(String tenantId);
    
    // Find by file type
    List<DataProfileRecord> findByTenantIdAndFileType(String tenantId, FileType fileType);
    
    // Find by quality score range
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.overallQualityScore BETWEEN :minScore AND :maxScore")
    List<DataProfileRecord> findByQualityScoreRange(@Param("tenantId") String tenantId, 
                                                    @Param("minScore") Double minScore, 
                                                    @Param("maxScore") Double maxScore);
    
    // Find files with poor quality
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.overallQualityScore < :threshold")
    List<DataProfileRecord> findFilesWithPoorQuality(@Param("tenantId") String tenantId, 
                                                     @Param("threshold") Double threshold);
    
    // Find by record count range
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.recordCount BETWEEN :minRecords AND :maxRecords")
    List<DataProfileRecord> findByRecordCountRange(@Param("tenantId") String tenantId, 
                                                   @Param("minRecords") Integer minRecords, 
                                                   @Param("maxRecords") Integer maxRecords);
    
    // Find by column count
    List<DataProfileRecord> findByTenantIdAndColumnCount(String tenantId, Integer columnCount);
    
    // Find large files
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.fileSize > :sizeThreshold")
    List<DataProfileRecord> findLargeFiles(@Param("tenantId") String tenantId, @Param("sizeThreshold") Long sizeThreshold);
    
    // Find recent profiles
    List<DataProfileRecord> findByTenantIdAndCreatedAtAfter(String tenantId, LocalDateTime since);
    
    // Find auto-profiled files
    List<DataProfileRecord> findByTenantIdAndAutoProfiling(String tenantId, Boolean autoProfiling);
    
    // Find by profiling depth
    List<DataProfileRecord> findByTenantIdAndProfilingDepth(String tenantId, String profilingDepth);
    
    // Get quality statistics
    @Query("SELECT AVG(d.overallQualityScore), MIN(d.overallQualityScore), MAX(d.overallQualityScore) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getQualityStatistics(@Param("tenantId") String tenantId);
    
    // Get completeness statistics
    @Query("SELECT AVG(d.dataCompletenessScore), MIN(d.dataCompletenessScore), MAX(d.dataCompletenessScore) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getCompletenessStatistics(@Param("tenantId") String tenantId);
    
    // Get uniqueness statistics
    @Query("SELECT AVG(d.dataUniquenessScore), MIN(d.dataUniquenessScore), MAX(d.dataUniquenessScore) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getUniquenessStatistics(@Param("tenantId") String tenantId);
    
    // Get consistency statistics
    @Query("SELECT AVG(d.dataConsistencyScore), MIN(d.dataConsistencyScore), MAX(d.dataConsistencyScore) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getConsistencyStatistics(@Param("tenantId") String tenantId);
    
    // Get file size statistics
    @Query("SELECT AVG(d.fileSize), MIN(d.fileSize), MAX(d.fileSize) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getFileSizeStatistics(@Param("tenantId") String tenantId);
    
    // Get record count statistics
    @Query("SELECT AVG(d.recordCount), MIN(d.recordCount), MAX(d.recordCount) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getRecordCountStatistics(@Param("tenantId") String tenantId);
    
    // Get column count statistics
    @Query("SELECT AVG(d.columnCount), MIN(d.columnCount), MAX(d.columnCount) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getColumnCountStatistics(@Param("tenantId") String tenantId);
    
    // Get profiling performance statistics
    @Query("SELECT AVG(d.profilingDurationMs), MIN(d.profilingDurationMs), MAX(d.profilingDurationMs) FROM DataProfileRecord d WHERE d.tenantId = :tenantId")
    Object[] getProfilingPerformanceStatistics(@Param("tenantId") String tenantId);
    
    // Find files by file type and quality
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.fileType = :fileType AND d.overallQualityScore >= :minQuality")
    List<DataProfileRecord> findHighQualityFilesByType(@Param("tenantId") String tenantId, 
                                                       @Param("fileType") FileType fileType, 
                                                       @Param("minQuality") Double minQuality);
    
    // Find files needing attention (low quality or high error count)
    @Query("SELECT d FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND (d.overallQualityScore < :qualityThreshold OR d.recordCount = 0)")
    List<DataProfileRecord> findFilesNeedingAttention(@Param("tenantId") String tenantId, 
                                                      @Param("qualityThreshold") Double qualityThreshold);
    
    // Get profiling trends over time
    @Query("SELECT DATE(d.createdAt), COUNT(d), AVG(d.overallQualityScore) FROM DataProfileRecord d WHERE d.tenantId = :tenantId AND d.createdAt >= :since GROUP BY DATE(d.createdAt) ORDER BY DATE(d.createdAt)")
    List<Object[]> getProfilingTrends(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);
    
    // Find files needing reprofile (old profiling version)
    @Query("SELECT d FROM DataProfileRecord d WHERE d.profilingVersion != :currentVersion")
    List<DataProfileRecord> findFilesNeedingReprofile(@Param("currentVersion") String currentVersion);
}