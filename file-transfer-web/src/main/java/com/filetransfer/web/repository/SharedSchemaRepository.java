package com.filetransfer.web.repository;

import com.filetransfer.web.entity.SharedSchema;
import com.filetransfer.web.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedSchemaRepository extends JpaRepository<SharedSchema, Long> {
    
    // Find schemas available for a tenant (tenant-specific + global)
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true ORDER BY ss.schemaName, ss.schemaVersion")
    List<SharedSchema> findAvailableSchemasForTenant(@Param("tenantId") String tenantId);
    
    // Find schemas by file type for a tenant
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.fileType = :fileType AND ss.isActive = true ORDER BY ss.usageCount DESC, ss.schemaName")
    List<SharedSchema> findByTenantAndFileType(@Param("tenantId") String tenantId, @Param("fileType") FileType fileType);
    
    // Find schemas by schema type for a tenant
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.schemaType = :schemaType AND ss.isActive = true ORDER BY ss.schemaName")
    List<SharedSchema> findByTenantAndSchemaType(@Param("tenantId") String tenantId, @Param("schemaType") SharedSchema.SchemaType schemaType);
    
    // Find most popular schemas (by usage count)
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true ORDER BY ss.usageCount DESC")
    List<SharedSchema> findMostPopularSchemas(@Param("tenantId") String tenantId);
    
    // Search schemas by name or description
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true AND " +
           "(LOWER(ss.schemaName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ss.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY ss.schemaName")
    List<SharedSchema> searchSchemas(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    // Find schemas with specific tags
    @Query("SELECT DISTINCT ss FROM SharedSchema ss JOIN ss.tags t WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true AND t IN :tags ORDER BY ss.schemaName")
    List<SharedSchema> findByTenantAndTags(@Param("tenantId") String tenantId, @Param("tags") List<String> tags);
    
    // Find schemas that support count validation (for EOT processing)
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true AND ss.supportsCountValidation = true ORDER BY ss.schemaName")
    List<SharedSchema> findCountValidationSchemas(@Param("tenantId") String tenantId);
    
    // Find schemas by exact name and version
    @Query("SELECT ss FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.schemaName = :schemaName AND ss.schemaVersion = :schemaVersion AND ss.isActive = true")
    Optional<SharedSchema> findByTenantAndNameAndVersion(@Param("tenantId") String tenantId, @Param("schemaName") String schemaName, @Param("schemaVersion") String schemaVersion);
    
    // Check if schema name exists for tenant
    @Query("SELECT COUNT(ss) > 0 FROM SharedSchema ss WHERE ss.tenantId = :tenantId AND ss.schemaName = :schemaName AND (:excludeId IS NULL OR ss.id != :excludeId)")
    boolean existsByTenantAndSchemaName(@Param("tenantId") String tenantId, @Param("schemaName") String schemaName, @Param("excludeId") Long excludeId);
    
    // Get usage statistics
    @Query("SELECT ss.fileType, COUNT(ss) as count, SUM(ss.usageCount) as totalUsage FROM SharedSchema ss WHERE (ss.tenantId = :tenantId OR ss.isGlobal = true) AND ss.isActive = true GROUP BY ss.fileType")
    List<Object[]> getUsageStatisticsByFileType(@Param("tenantId") String tenantId);
    
    // Find unused schemas (no recent usage)
    @Query("SELECT ss FROM SharedSchema ss WHERE ss.tenantId = :tenantId AND ss.usageCount = 0 AND ss.isActive = true ORDER BY ss.createdAt DESC")
    List<SharedSchema> findUnusedSchemas(@Param("tenantId") String tenantId);
    
    // Find global schemas (for admin management)
    @Query("SELECT ss FROM SharedSchema ss WHERE ss.isGlobal = true AND ss.isActive = true ORDER BY ss.schemaName")
    List<SharedSchema> findGlobalSchemas();
}