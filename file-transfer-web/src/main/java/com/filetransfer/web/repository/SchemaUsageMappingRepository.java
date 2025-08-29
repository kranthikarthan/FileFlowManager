package com.filetransfer.web.repository;

import com.filetransfer.web.entity.SchemaUsageMapping;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaUsageMappingRepository extends JpaRepository<SchemaUsageMapping, Long> {
    
    // Find mappings for a specific subservice
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.id = :subServiceConfigId AND sum.validationEnabled = true ORDER BY sum.direction, sum.isPrimary DESC")
    List<SchemaUsageMapping> findBySubServiceConfiguration(@Param("subServiceConfigId") Long subServiceConfigId);
    
    // Find mappings by subservice and direction
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.id = :subServiceConfigId AND sum.direction = :direction AND sum.validationEnabled = true ORDER BY sum.isPrimary DESC")
    List<SchemaUsageMapping> findBySubServiceAndDirection(@Param("subServiceConfigId") Long subServiceConfigId, @Param("direction") TransferDirection direction);
    
    // Find primary schema for subservice and direction
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.id = :subServiceConfigId AND sum.direction = :direction AND sum.isPrimary = true AND sum.validationEnabled = true")
    Optional<SchemaUsageMapping> findPrimaryBySubServiceAndDirection(@Param("subServiceConfigId") Long subServiceConfigId, @Param("direction") TransferDirection direction);
    
    // Find mappings by shared schema ID
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.sharedSchema.id = :sharedSchemaId ORDER BY sum.subServiceConfiguration.serviceName, sum.subServiceConfiguration.subServiceName")
    List<SchemaUsageMapping> findBySharedSchema(@Param("sharedSchemaId") Long sharedSchemaId);
    
    // Find mappings by tenant (through subservice)
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.tenantId = :tenantId AND sum.validationEnabled = true ORDER BY sum.subServiceConfiguration.serviceName, sum.subServiceConfiguration.subServiceName")
    List<SchemaUsageMapping> findByTenant(@Param("tenantId") String tenantId);
    
    // Find active mappings for a schema (to check if schema can be deleted)
    @Query("SELECT COUNT(sum) FROM SchemaUsageMapping sum WHERE sum.sharedSchema.id = :sharedSchemaId AND sum.validationEnabled = true")
    long countActiveUsages(@Param("sharedSchemaId") Long sharedSchemaId);
    
    // Find duplicate primary mappings (for validation)
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.id = :subServiceConfigId AND sum.direction = :direction AND sum.isPrimary = true AND sum.validationEnabled = true AND sum.id != :excludeId")
    List<SchemaUsageMapping> findOtherPrimaryMappings(@Param("subServiceConfigId") Long subServiceConfigId, @Param("direction") TransferDirection direction, @Param("excludeId") Long excludeId);
    
    // Get usage statistics by schema
    @Query("SELECT sum.sharedSchema.id, sum.sharedSchema.schemaName, COUNT(sum) as usageCount, SUM(sum.usageCount) as totalUsages " +
           "FROM SchemaUsageMapping sum " +
           "WHERE sum.sharedSchema.tenantId = :tenantId OR sum.sharedSchema.isGlobal = true " +
           "GROUP BY sum.sharedSchema.id, sum.sharedSchema.schemaName " +
           "ORDER BY totalUsages DESC")
    List<Object[]> getSchemaUsageStatistics(@Param("tenantId") String tenantId);
    
    // Find mappings by file type (through schema)
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.sharedSchema.fileType = :fileType AND sum.validationEnabled = true ORDER BY sum.usageCount DESC")
    List<SchemaUsageMapping> findByFileType(@Param("fileType") FileType fileType);
    
    // Find EOT schemas with count validation
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.tenantId = :tenantId AND sum.sharedSchema.supportsCountValidation = true AND sum.validationEnabled = true ORDER BY sum.subServiceConfiguration.serviceName")
    List<SchemaUsageMapping> findEotCountValidationMappings(@Param("tenantId") String tenantId);
    
    // Check if specific mapping already exists
    @Query("SELECT COUNT(sum) > 0 FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.id = :subServiceConfigId AND sum.sharedSchema.id = :sharedSchemaId AND sum.direction = :direction")
    boolean existsBySubServiceAndSchemaAndDirection(@Param("subServiceConfigId") Long subServiceConfigId, @Param("sharedSchemaId") Long sharedSchemaId, @Param("direction") TransferDirection direction);
    
    // Find most recent mappings for a tenant
    @Query("SELECT sum FROM SchemaUsageMapping sum WHERE sum.subServiceConfiguration.tenantId = :tenantId ORDER BY sum.createdAt DESC")
    List<SchemaUsageMapping> findRecentMappingsByTenant(@Param("tenantId") String tenantId);
}