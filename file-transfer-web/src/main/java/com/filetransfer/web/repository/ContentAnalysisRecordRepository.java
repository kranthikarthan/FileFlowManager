package com.filetransfer.web.repository;

import com.filetransfer.web.entity.ContentAnalysisRecord;
import com.filetransfer.web.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for content analysis records
 */
@Repository
public interface ContentAnalysisRecordRepository extends JpaRepository<ContentAnalysisRecord, Long> {
    
    // Find by file transfer ID
    Optional<ContentAnalysisRecord> findByFileTransferId(Long fileTransferId);
    
    // Find by tenant and file transfer ID
    Optional<ContentAnalysisRecord> findByTenantIdAndFileTransferId(String tenantId, Long fileTransferId);
    
    // Find by tenant ID
    List<ContentAnalysisRecord> findByTenantId(String tenantId);
    
    // Find by file type
    List<ContentAnalysisRecord> findByTenantIdAndDetectedFileType(String tenantId, FileType fileType);
    
    // Find binary files
    List<ContentAnalysisRecord> findByTenantIdAndIsBinaryFile(String tenantId, Boolean isBinaryFile);
    
    // Find empty files
    List<ContentAnalysisRecord> findByTenantIdAndIsEmptyFile(String tenantId, Boolean isEmptyFile);
    
    // Find by quality score range
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId AND c.qualityScore BETWEEN :minScore AND :maxScore")
    List<ContentAnalysisRecord> findByQualityScoreRange(@Param("tenantId") String tenantId, 
                                                        @Param("minScore") Double minScore, 
                                                        @Param("maxScore") Double maxScore);
    
    // Find files with quality issues
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId AND c.qualityScore < :threshold")
    List<ContentAnalysisRecord> findFilesWithQualityIssues(@Param("tenantId") String tenantId, 
                                                           @Param("threshold") Double threshold);
    
    // Find recent analyses
    List<ContentAnalysisRecord> findByTenantIdAndCreatedAtAfter(String tenantId, LocalDateTime since);
    
    // Get file type statistics
    @Query("SELECT c.detectedFileType, COUNT(c) FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId GROUP BY c.detectedFileType")
    List<Object[]> getFileTypeStatistics(@Param("tenantId") String tenantId);
    
    // Get quality statistics
    @Query("SELECT AVG(c.qualityScore), MIN(c.qualityScore), MAX(c.qualityScore) FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId")
    Object[] getQualityStatistics(@Param("tenantId") String tenantId);
    
    // Get encoding statistics
    @Query("SELECT c.detectedEncoding, COUNT(c) FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId GROUP BY c.detectedEncoding")
    List<Object[]> getEncodingStatistics(@Param("tenantId") String tenantId);
    
    // Find large files
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId AND c.fileSize > :sizeThreshold")
    List<ContentAnalysisRecord> findLargeFiles(@Param("tenantId") String tenantId, @Param("sizeThreshold") Long sizeThreshold);
    
    // Find files by record count range
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId AND c.recordCount BETWEEN :minRecords AND :maxRecords")
    List<ContentAnalysisRecord> findByRecordCountRange(@Param("tenantId") String tenantId, 
                                                       @Param("minRecords") Integer minRecords, 
                                                       @Param("maxRecords") Integer maxRecords);
    
    // Find files by column count
    List<ContentAnalysisRecord> findByTenantIdAndColumnCount(String tenantId, Integer columnCount);
    
    // Find files needing reanalysis (old analysis version)
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.analysisVersion != :currentVersion")
    List<ContentAnalysisRecord> findFilesNeedingReanalysis(@Param("currentVersion") String currentVersion);
    
    // Get analysis performance statistics
    @Query("SELECT AVG(c.analysisDurationMs), MIN(c.analysisDurationMs), MAX(c.analysisDurationMs) FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId")
    Object[] getAnalysisPerformanceStatistics(@Param("tenantId") String tenantId);
    
    // Find similar files (same structure)
    @Query("SELECT c FROM ContentAnalysisRecord c WHERE c.tenantId = :tenantId AND c.detectedFileType = :fileType AND c.columnCount = :columnCount AND c.id != :excludeId")
    List<ContentAnalysisRecord> findSimilarFiles(@Param("tenantId") String tenantId, 
                                                 @Param("fileType") FileType fileType, 
                                                 @Param("columnCount") Integer columnCount, 
                                                 @Param("excludeId") Long excludeId);
}