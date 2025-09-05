package com.filetransfer.web.service;

import com.filetransfer.web.entity.FileTag;
import com.filetransfer.web.entity.FileTransferTag;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.repository.FileTagRepository;
import com.filetransfer.web.repository.FileTransferTagRepository;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing file tags and tagging operations
 */
@Service
public class FileTaggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTaggingService.class);
    
    @Autowired
    private FileTagRepository fileTagRepository;
    
    @Autowired
    private FileTransferTagRepository fileTransferTagRepository;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRecordRepository;
    
    // Predefined system tags
    private static final Map<String, String> SYSTEM_TAGS = Map.of(
        "high-priority", "#f44336",
        "reviewed", "#4caf50", 
        "needs-attention", "#ff9800",
        "archived", "#9e9e9e",
        "sensitive-data", "#e91e63",
        "large-file", "#3f51b5",
        "quality-issues", "#ff5722",
        "processed", "#2196f3"
    );
    
    /**
     * Create a new tag
     */
    @Transactional
    public FileTag createTag(String tenantId, String tagName, String tagColor, String tagDescription, String createdBy) {
        // Check if tag already exists
        Optional<FileTag> existingTag = fileTagRepository.findByTenantIdAndTagName(tenantId, tagName);
        if (existingTag.isPresent()) {
            throw new RuntimeException("Tag already exists: " + tagName);
        }
        
        FileTag tag = new FileTag(tenantId, tagName, tagColor, createdBy);
        tag.setTagDescription(tagDescription);
        
        FileTag savedTag = fileTagRepository.save(tag);
        logger.info("Created new tag: {} for tenant: {}", tagName, tenantId);
        
        return savedTag;
    }
    
    /**
     * Create system tags for a tenant
     */
    @Transactional
    public void createSystemTags(String tenantId) {
        for (Map.Entry<String, String> entry : SYSTEM_TAGS.entrySet()) {
            String tagName = entry.getKey();
            String tagColor = entry.getValue();
            
            // Check if system tag already exists
            Optional<FileTag> existingTag = fileTagRepository.findByTenantIdAndTagName(tenantId, tagName);
            if (existingTag.isEmpty()) {
                FileTag systemTag = new FileTag(tenantId, tagName, tagColor, "SYSTEM");
                systemTag.setIsSystemTag(true);
                systemTag.setTagDescription("System-generated tag for " + tagName.replace("-", " "));
                
                fileTagRepository.save(systemTag);
                logger.info("Created system tag: {} for tenant: {}", tagName, tenantId);
            }
        }
    }
    
    /**
     * Add tag to file transfer
     */
    @Transactional
    public void addTagToFileTransfer(Long fileTransferId, Long tagId, String taggedBy, String reason) {
        // Check if tag relationship already exists
        FileTransferTag existingRelation = fileTransferTagRepository.findByFileTransferIdAndFileTagId(fileTransferId, tagId);
        if (existingRelation != null) {
            logger.warn("Tag relationship already exists for file {} and tag {}", fileTransferId, tagId);
            return;
        }
        
        // Verify file transfer exists
        Optional<FileTransferRecord> fileTransfer = fileTransferRecordRepository.findById(fileTransferId);
        if (fileTransfer.isEmpty()) {
            throw new RuntimeException("File transfer not found: " + fileTransferId);
        }
        
        // Verify tag exists
        Optional<FileTag> tag = fileTagRepository.findById(tagId);
        if (tag.isEmpty()) {
            throw new RuntimeException("Tag not found: " + tagId);
        }
        
        // Create tag relationship
        FileTransferTag transferTag = new FileTransferTag(fileTransferId, tagId, taggedBy);
        transferTag.setTagReason(reason);
        transferTag.setAutoTagged(false);
        
        fileTransferTagRepository.save(transferTag);
        
        // Update tag usage count
        FileTag tagEntity = tag.get();
        tagEntity.setUsageCount(tagEntity.getUsageCount() + 1);
        fileTagRepository.save(tagEntity);
        
        logger.info("Added tag '{}' to file transfer {} by user {}", tagEntity.getTagName(), fileTransferId, taggedBy);
    }
    
    /**
     * Add tag by name to file transfer
     */
    @Transactional
    public void addTagToFileTransfer(Long fileTransferId, String tenantId, String tagName, String taggedBy, String reason) {
        Optional<FileTag> tag = fileTagRepository.findByTenantIdAndTagName(tenantId, tagName);
        if (tag.isEmpty()) {
            throw new RuntimeException("Tag not found: " + tagName);
        }
        
        addTagToFileTransfer(fileTransferId, tag.get().getId(), taggedBy, reason);
    }
    
    /**
     * Remove tag from file transfer
     */
    @Transactional
    public void removeTagFromFileTransfer(Long fileTransferId, Long tagId) {
        fileTransferTagRepository.removeTagFromFileTransfer(fileTransferId, tagId);
        
        // Update tag usage count
        Optional<FileTag> tag = fileTagRepository.findById(tagId);
        if (tag.isPresent()) {
            FileTag tagEntity = tag.get();
            tagEntity.setUsageCount(Math.max(0, tagEntity.getUsageCount() - 1));
            fileTagRepository.save(tagEntity);
        }
        
        logger.info("Removed tag {} from file transfer {}", tagId, fileTransferId);
    }
    
    /**
     * Get all tags for a file transfer
     */
    public List<FileTag> getTagsForFileTransfer(Long fileTransferId) {
        List<FileTransferTag> transferTags = fileTransferTagRepository.findByFileTransferId(fileTransferId);
        
        return transferTags.stream()
                .map(transferTag -> fileTagRepository.findById(transferTag.getFileTagId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get file transfers by tag
     */
    public List<Long> getFileTransfersByTag(String tenantId, String tagName) {
        return fileTransferTagRepository.findFileTransferIdsByTagName(tenantId, tagName);
    }
    
    /**
     * Get file transfers by multiple tags (AND operation)
     */
    public List<Long> getFileTransfersByAllTags(String tenantId, List<String> tagNames) {
        if (tagNames.isEmpty()) return new ArrayList<>();
        
        return fileTransferTagRepository.findFileTransferIdsByAllTags(tenantId, tagNames, (long) tagNames.size());
    }
    
    /**
     * Get file transfers by any of the tags (OR operation)
     */
    public List<Long> getFileTransfersByAnyTags(String tenantId, List<String> tagNames) {
        if (tagNames.isEmpty()) return new ArrayList<>();
        
        return fileTransferTagRepository.findFileTransferIdsByAnyTags(tenantId, tagNames);
    }
    
    /**
     * Auto-tag file based on content analysis
     */
    @Transactional
    public void autoTagFile(Long fileTransferId, String tenantId) {
        try {
            Optional<FileTransferRecord> recordOpt = fileTransferRecordRepository.findById(fileTransferId);
            if (recordOpt.isEmpty()) {
                logger.warn("File transfer not found for auto-tagging: {}", fileTransferId);
                return;
            }
            
            FileTransferRecord record = recordOpt.get();
            List<String> autoTags = generateAutoTags(record);
            
            for (String tagName : autoTags) {
                try {
                    Optional<FileTag> tag = fileTagRepository.findByTenantIdAndTagName(tenantId, tagName);
                    if (tag.isPresent()) {
                        // Check if tag is not already applied
                        FileTransferTag existingRelation = fileTransferTagRepository
                                .findByFileTransferIdAndFileTagId(fileTransferId, tag.get().getId());
                        
                        if (existingRelation == null) {
                            FileTransferTag autoTag = new FileTransferTag(fileTransferId, tag.get().getId(), "SYSTEM");
                            autoTag.setAutoTagged(true);
                            autoTag.setTagReason("Auto-tagged based on file characteristics");
                            
                            fileTransferTagRepository.save(autoTag);
                            
                            // Update usage count
                            FileTag tagEntity = tag.get();
                            tagEntity.setUsageCount(tagEntity.getUsageCount() + 1);
                            fileTagRepository.save(tagEntity);
                            
                            logger.info("Auto-tagged file {} with tag: {}", record.getFileName(), tagName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error applying auto-tag '{}' to file {}: {}", tagName, fileTransferId, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in auto-tagging for file {}: {}", fileTransferId, e.getMessage());
        }
    }
    
    /**
     * Generate automatic tags based on file characteristics
     */
    private List<String> generateAutoTags(FileTransferRecord record) {
        List<String> autoTags = new ArrayList<>();
        
        // Size-based tags
        if (record.getFileSize() != null && record.getFileSize() > 100 * 1024 * 1024) { // > 100MB
            autoTags.add("large-file");
        }
        
        // Status-based tags
        if (record.getStatus() != null) {
            switch (record.getStatus()) {
                case COMPLETED:
                    autoTags.add("processed");
                    break;
                case FAILED:
                    autoTags.add("needs-attention");
                    break;
            }
        }
        
        // Extension-based tags
        if (record.getFileExtension() != null) {
            switch (record.getFileExtension().toLowerCase()) {
                case ".csv":
                case ".xlsx":
                case ".xls":
                    // These might contain sensitive customer data
                    if (record.getServiceName() != null && record.getServiceName().contains("CUSTOMER")) {
                        autoTags.add("sensitive-data");
                    }
                    break;
                case ".log":
                    // Archive log files after processing
                    if (record.getStatus() == TransferStatus.COMPLETED) {
                        autoTags.add("archived");
                    }
                    break;
            }
        }
        
        // Service-based tags
        if (record.getServiceName() != null) {
            if (record.getServiceName().contains("PRIORITY") || record.getServiceName().contains("URGENT")) {
                autoTags.add("high-priority");
            }
        }
        
        return autoTags;
    }
    
    /**
     * Update tag
     */
    @Transactional
    public FileTag updateTag(Long tagId, String tagName, String tagColor, String tagDescription) {
        Optional<FileTag> tagOpt = fileTagRepository.findById(tagId);
        if (tagOpt.isEmpty()) {
            throw new RuntimeException("Tag not found: " + tagId);
        }
        
        FileTag tag = tagOpt.get();
        
        if (tag.getIsSystemTag()) {
            throw new RuntimeException("Cannot modify system tag: " + tag.getTagName());
        }
        
        tag.setTagName(tagName);
        tag.setTagColor(tagColor);
        tag.setTagDescription(tagDescription);
        
        FileTag savedTag = fileTagRepository.save(tag);
        logger.info("Updated tag: {} for tenant: {}", tagName, tag.getTenantId());
        
        return savedTag;
    }
    
    /**
     * Delete tag
     */
    @Transactional
    public void deleteTag(Long tagId) {
        Optional<FileTag> tagOpt = fileTagRepository.findById(tagId);
        if (tagOpt.isEmpty()) {
            throw new RuntimeException("Tag not found: " + tagId);
        }
        
        FileTag tag = tagOpt.get();
        
        if (tag.getIsSystemTag()) {
            throw new RuntimeException("Cannot delete system tag: " + tag.getTagName());
        }
        
        // Remove all tag relationships
        fileTransferTagRepository.deleteByFileTagId(tagId);
        
        // Delete the tag
        fileTagRepository.deleteById(tagId);
        
        logger.info("Deleted tag: {} for tenant: {}", tag.getTagName(), tag.getTenantId());
    }
    
    /**
     * Get all tags for tenant
     */
    public List<FileTag> getAllTags(String tenantId) {
        return fileTagRepository.findByTenantId(tenantId);
    }
    
    /**
     * Get user-created tags for tenant
     */
    public List<FileTag> getUserTags(String tenantId) {
        return fileTagRepository.findByTenantIdAndIsSystemTagFalse(tenantId);
    }
    
    /**
     * Get system tags for tenant
     */
    public List<FileTag> getSystemTags(String tenantId) {
        return fileTagRepository.findByTenantIdAndIsSystemTag(tenantId, true);
    }
    
    /**
     * Get most used tags
     */
    public List<FileTag> getMostUsedTags(String tenantId, int limit) {
        List<FileTag> allTags = fileTagRepository.findMostUsedTags(tenantId);
        return allTags.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Search tags by name
     */
    public List<FileTag> searchTags(String tenantId, String searchTerm) {
        return fileTagRepository.searchByTagName(tenantId, searchTerm);
    }
    
    /**
     * Get tag statistics
     */
    public Map<String, Object> getTagStatistics(String tenantId) {
        Object[] stats = fileTagRepository.getTagStatistics(tenantId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalTags", stats[0]);
        statistics.put("totalUsages", stats[1]);
        
        // Additional statistics
        List<FileTag> allTags = fileTagRepository.findByTenantId(tenantId);
        long systemTags = allTags.stream().filter(FileTag::getIsSystemTag).count();
        long userTags = allTags.size() - systemTags;
        
        statistics.put("systemTags", systemTags);
        statistics.put("userTags", userTags);
        statistics.put("averageUsagePerTag", allTags.isEmpty() ? 0 : 
            allTags.stream().mapToInt(FileTag::getUsageCount).average().orElse(0.0));
        
        return statistics;
    }
    
    /**
     * Get tagging activity statistics
     */
    public Map<String, Object> getTaggingActivity(String tenantId) {
        List<Object[]> activityByUser = fileTransferTagRepository.getTaggingActivityByUser(tenantId);
        List<Object[]> trendData = fileTransferTagRepository.getTaggingTrends(tenantId, LocalDateTime.now().minusDays(30));
        
        Map<String, Object> activity = new HashMap<>();
        activity.put("activityByUser", activityByUser.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                )));
        
        activity.put("dailyTrends", trendData.stream()
                .collect(Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1],
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                )));
        
        return activity;
    }
    
    /**
     * Bulk tag operations
     */
    @Transactional
    public BulkTagResult bulkAddTags(List<Long> fileTransferIds, List<Long> tagIds, String taggedBy, String reason) {
        BulkTagResult result = new BulkTagResult();
        
        for (Long fileTransferId : fileTransferIds) {
            for (Long tagId : tagIds) {
                try {
                    addTagToFileTransfer(fileTransferId, tagId, taggedBy, reason);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    result.incrementErrorCount();
                    result.addError("Failed to tag file " + fileTransferId + " with tag " + tagId + ": " + e.getMessage());
                    logger.error("Error in bulk tagging file {} with tag {}: {}", fileTransferId, tagId, e.getMessage());
                }
            }
        }
        
        logger.info("Bulk tagging completed: {} successes, {} errors", result.getSuccessCount(), result.getErrorCount());
        return result;
    }
    
    /**
     * Bulk remove tags
     */
    @Transactional
    public BulkTagResult bulkRemoveTags(List<Long> fileTransferIds, List<Long> tagIds) {
        BulkTagResult result = new BulkTagResult();
        
        for (Long fileTransferId : fileTransferIds) {
            for (Long tagId : tagIds) {
                try {
                    removeTagFromFileTransfer(fileTransferId, tagId);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    result.incrementErrorCount();
                    result.addError("Failed to remove tag " + tagId + " from file " + fileTransferId + ": " + e.getMessage());
                    logger.error("Error in bulk removing tag {} from file {}: {}", tagId, fileTransferId, e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Auto-tag multiple files based on criteria
     */
    @Transactional
    public void autoTagFiles(String tenantId, AutoTaggingCriteria criteria) {
        List<FileTransferRecord> filesToTag = findFilesMatchingCriteria(tenantId, criteria);
        
        for (FileTransferRecord record : filesToTag) {
            autoTagFile(record.getId(), tenantId);
        }
        
        logger.info("Auto-tagged {} files for tenant {} based on criteria", filesToTag.size(), tenantId);
    }
    
    /**
     * Find files matching auto-tagging criteria
     */
    private List<FileTransferRecord> findFilesMatchingCriteria(String tenantId, AutoTaggingCriteria criteria) {
        // This would use the repository to find files based on criteria
        // For now, return recent completed files as an example
        return fileTransferRecordRepository.findByTenantIdAndStatus(tenantId, TransferStatus.COMPLETED)
                .stream()
                .filter(record -> record.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .collect(Collectors.toList());
    }
    
    /**
     * Clean up unused tags
     */
    @Transactional
    public int cleanupUnusedTags(String tenantId) {
        List<FileTag> unusedTags = fileTagRepository.findUnusedTags(tenantId);
        
        // Only remove user-created tags, not system tags
        List<FileTag> tagsToDelete = unusedTags.stream()
                .filter(tag -> !tag.getIsSystemTag())
                .collect(Collectors.toList());
        
        for (FileTag tag : tagsToDelete) {
            fileTagRepository.delete(tag);
        }
        
        logger.info("Cleaned up {} unused tags for tenant {}", tagsToDelete.size(), tenantId);
        return tagsToDelete.size();
    }
    
    // Result and criteria classes
    
    public static class BulkTagResult {
        private int successCount = 0;
        private int errorCount = 0;
        private List<String> errors = new ArrayList<>();
        
        public void incrementSuccessCount() { this.successCount++; }
        public void incrementErrorCount() { this.errorCount++; }
        public void addError(String error) { this.errors.add(error); }
        
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return errorCount > 0; }
    }
    
    public static class AutoTaggingCriteria {
        private String serviceName;
        private String fileExtension;
        private Long minFileSize;
        private Long maxFileSize;
        private TransferStatus status;
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;
        
        // Getters and Setters
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
        
        public Long getMinFileSize() { return minFileSize; }
        public void setMinFileSize(Long minFileSize) { this.minFileSize = minFileSize; }
        
        public Long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(Long maxFileSize) { this.maxFileSize = maxFileSize; }
        
        public TransferStatus getStatus() { return status; }
        public void setStatus(TransferStatus status) { this.status = status; }
        
        public LocalDateTime getCreatedAfter() { return createdAfter; }
        public void setCreatedAfter(LocalDateTime createdAfter) { this.createdAfter = createdAfter; }
        
        public LocalDateTime getCreatedBefore() { return createdBefore; }
        public void setCreatedBefore(LocalDateTime createdBefore) { this.createdBefore = createdBefore; }
    }
}