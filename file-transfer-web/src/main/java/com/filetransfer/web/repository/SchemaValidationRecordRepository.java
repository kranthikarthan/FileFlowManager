package com.filetransfer.web.repository;

import com.filetransfer.web.entity.SchemaValidationRecord;
import com.filetransfer.web.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for schema validation records
 */
@Repository
public interface SchemaValidationRecordRepository extends JpaRepository<SchemaValidationRecord, Long> {
    
    // Find by file transfer ID
    Optional<SchemaValidationRecord> findByFileTransferId(Long fileTransferId);
    
    // Find by tenant and file transfer ID
    Optional<SchemaValidationRecord> findByTenantIdAndFileTransferId(String tenantId, Long fileTransferId);
    
    // Find by tenant ID
    List<SchemaValidationRecord> findByTenantId(String tenantId);
    
    // Find by validation status
    List<SchemaValidationRecord> findByTenantIdAndValidationPassed(String tenantId, Boolean validationPassed);
    
    // Find by file type
    List<SchemaValidationRecord> findByTenantIdAndFileType(String tenantId, FileType fileType);
    
    // Find by error count range
    @Query("SELECT s FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId AND s.errorCount BETWEEN :minErrors AND :maxErrors")
    List<SchemaValidationRecord> findByErrorCountRange(@Param("tenantId") String tenantId, 
                                                       @Param("minErrors") Integer minErrors, 
                                                       @Param("maxErrors") Integer maxErrors);
    
    // Find files with validation errors
    @Query("SELECT s FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId AND s.errorCount > 0")
    List<SchemaValidationRecord> findFilesWithValidationErrors(@Param("tenantId") String tenantId);
    
    // Find files with warnings
    @Query("SELECT s FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId AND s.warningCount > 0")
    List<SchemaValidationRecord> findFilesWithWarnings(@Param("tenantId") String tenantId);
    
    // Find by compliance score range
    @Query("SELECT s FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId AND s.schemaComplianceScore BETWEEN :minScore AND :maxScore")
    List<SchemaValidationRecord> findByComplianceScoreRange(@Param("tenantId") String tenantId, 
                                                            @Param("minScore") Double minScore, 
                                                            @Param("maxScore") Double maxScore);
    
    // Find recent validations
    List<SchemaValidationRecord> findByTenantIdAndCreatedAtAfter(String tenantId, LocalDateTime since);
    
    // Find auto-validated files
    List<SchemaValidationRecord> findByTenantIdAndAutoValidation(String tenantId, Boolean autoValidation);
    
    // Get validation statistics
    @Query("SELECT COUNT(s), SUM(CASE WHEN s.validationPassed = true THEN 1 ELSE 0 END) FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId")
    Object[] getValidationStatistics(@Param("tenantId") String tenantId);
    
    // Get validation performance statistics
    @Query("SELECT AVG(s.validationDurationMs), MIN(s.validationDurationMs), MAX(s.validationDurationMs) FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId")
    Object[] getValidationPerformanceStatistics(@Param("tenantId") String tenantId);
    
    // Get error statistics by file type
    @Query("SELECT s.fileType, AVG(s.errorCount) FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId GROUP BY s.fileType")
    List<Object[]> getErrorStatisticsByFileType(@Param("tenantId") String tenantId);
    
    // Get compliance score statistics
    @Query("SELECT AVG(s.schemaComplianceScore), MIN(s.schemaComplianceScore), MAX(s.schemaComplianceScore) FROM SchemaValidationRecord s WHERE s.tenantId = :tenantId")
    Object[] getComplianceScoreStatistics(@Param("tenantId") String tenantId);
    
    // Find files using specific schema
    List<SchemaValidationRecord> findByTenantIdAndSchemaPath(String tenantId, String schemaPath);
    
    // Find files by validation engine
    List<SchemaValidationRecord> findByTenantIdAndValidationEngine(String tenantId, String validationEngine);
    
    // Find files needing revalidation (old validation version)
    @Query("SELECT s FROM SchemaValidationRecord s WHERE s.validationVersion != :currentVersion")
    List<SchemaValidationRecord> findFilesNeedingRevalidation(@Param("currentVersion") String currentVersion);
}