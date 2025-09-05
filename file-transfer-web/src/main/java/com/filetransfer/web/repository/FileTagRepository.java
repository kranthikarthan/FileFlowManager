package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for file tags
 */
@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {
    
    // Find by tenant
    List<FileTag> findByTenantId(String tenantId);
    
    // Find by tenant and tag name
    Optional<FileTag> findByTenantIdAndTagName(String tenantId, String tagName);
    
    // Find system tags
    List<FileTag> findByTenantIdAndIsSystemTag(String tenantId, Boolean isSystemTag);
    
    // Find user-created tags
    List<FileTag> findByTenantIdAndIsSystemTagFalse(String tenantId);
    
    // Find by creator
    List<FileTag> findByTenantIdAndCreatedBy(String tenantId, String createdBy);
    
    // Find most used tags
    @Query("SELECT t FROM FileTag t WHERE t.tenantId = :tenantId ORDER BY t.usageCount DESC")
    List<FileTag> findMostUsedTags(@Param("tenantId") String tenantId);
    
    // Find tags by usage count range
    @Query("SELECT t FROM FileTag t WHERE t.tenantId = :tenantId AND t.usageCount BETWEEN :minUsage AND :maxUsage")
    List<FileTag> findByUsageCountRange(@Param("tenantId") String tenantId, 
                                       @Param("minUsage") Integer minUsage, 
                                       @Param("maxUsage") Integer maxUsage);
    
    // Search tags by name pattern
    @Query("SELECT t FROM FileTag t WHERE t.tenantId = :tenantId AND LOWER(t.tagName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<FileTag> searchByTagName(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    // Get tag statistics
    @Query("SELECT COUNT(t), SUM(t.usageCount) FROM FileTag t WHERE t.tenantId = :tenantId")
    Object[] getTagStatistics(@Param("tenantId") String tenantId);
    
    // Find unused tags
    @Query("SELECT t FROM FileTag t WHERE t.tenantId = :tenantId AND t.usageCount = 0")
    List<FileTag> findUnusedTags(@Param("tenantId") String tenantId);
    
    // Find tags by color
    List<FileTag> findByTenantIdAndTagColor(String tenantId, String tagColor);
}