package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTransferTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for file transfer tags (many-to-many relationship)
 */
@Repository
public interface FileTransferTagRepository extends JpaRepository<FileTransferTag, Long> {
    
    // Find tags for a specific file transfer
    List<FileTransferTag> findByFileTransferId(Long fileTransferId);
    
    // Find file transfers with a specific tag
    List<FileTransferTag> findByFileTagId(Long fileTagId);
    
    // Find by file transfer and tag
    FileTransferTag findByFileTransferIdAndFileTagId(Long fileTransferId, Long fileTagId);
    
    // Find by tagged user
    List<FileTransferTag> findByTaggedBy(String taggedBy);
    
    // Find auto-tagged entries
    List<FileTransferTag> findByAutoTagged(Boolean autoTagged);
    
    // Find recent tagging activity
    List<FileTransferTag> findByTaggedAtAfter(LocalDateTime since);
    
    // Get tagging statistics for a tag
    @Query("SELECT COUNT(ftt) FROM FileTransferTag ftt WHERE ftt.fileTagId = :tagId")
    Long getTagUsageCount(@Param("tagId") Long tagId);
    
    // Get file transfers by tag name (through join)
    @Query("SELECT ftt.fileTransferId FROM FileTransferTag ftt " +
           "JOIN FileTag ft ON ftt.fileTagId = ft.id " +
           "WHERE ft.tenantId = :tenantId AND ft.tagName = :tagName")
    List<Long> findFileTransferIdsByTagName(@Param("tenantId") String tenantId, @Param("tagName") String tagName);
    
    // Get file transfers by multiple tags (intersection)
    @Query("SELECT ftt1.fileTransferId FROM FileTransferTag ftt1 " +
           "JOIN FileTag ft1 ON ftt1.fileTagId = ft1.id " +
           "WHERE ft1.tenantId = :tenantId AND ft1.tagName IN :tagNames " +
           "GROUP BY ftt1.fileTransferId " +
           "HAVING COUNT(DISTINCT ft1.tagName) = :tagCount")
    List<Long> findFileTransferIdsByAllTags(@Param("tenantId") String tenantId, 
                                           @Param("tagNames") List<String> tagNames, 
                                           @Param("tagCount") Long tagCount);
    
    // Get file transfers by any of the specified tags (union)
    @Query("SELECT DISTINCT ftt.fileTransferId FROM FileTransferTag ftt " +
           "JOIN FileTag ft ON ftt.fileTagId = ft.id " +
           "WHERE ft.tenantId = :tenantId AND ft.tagName IN :tagNames")
    List<Long> findFileTransferIdsByAnyTags(@Param("tenantId") String tenantId, @Param("tagNames") List<String> tagNames);
    
    // Remove tag from file transfer
    @Modifying
    @Transactional
    @Query("DELETE FROM FileTransferTag ftt WHERE ftt.fileTransferId = :fileTransferId AND ftt.fileTagId = :tagId")
    void removeTagFromFileTransfer(@Param("fileTransferId") Long fileTransferId, @Param("tagId") Long tagId);
    
    // Remove all tags from file transfer
    @Modifying
    @Transactional
    void deleteByFileTransferId(Long fileTransferId);
    
    // Remove all file transfers from tag
    @Modifying
    @Transactional
    void deleteByFileTagId(Long fileTagId);
    
    // Get tagging activity by user
    @Query("SELECT ftt.taggedBy, COUNT(ftt) FROM FileTransferTag ftt " +
           "JOIN FileTag ft ON ftt.fileTagId = ft.id " +
           "WHERE ft.tenantId = :tenantId " +
           "GROUP BY ftt.taggedBy " +
           "ORDER BY COUNT(ftt) DESC")
    List<Object[]> getTaggingActivityByUser(@Param("tenantId") String tenantId);
    
    // Get tagging trends over time
    @Query("SELECT DATE(ftt.taggedAt), COUNT(ftt) FROM FileTransferTag ftt " +
           "JOIN FileTag ft ON ftt.fileTagId = ft.id " +
           "WHERE ft.tenantId = :tenantId AND ftt.taggedAt >= :since " +
           "GROUP BY DATE(ftt.taggedAt) " +
           "ORDER BY DATE(ftt.taggedAt)")
    List<Object[]> getTaggingTrends(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);
}