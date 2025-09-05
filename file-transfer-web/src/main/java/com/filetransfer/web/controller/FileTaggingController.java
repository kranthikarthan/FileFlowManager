package com.filetransfer.web.controller;

import com.filetransfer.web.entity.FileTag;
import com.filetransfer.web.service.FileTaggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for file tagging operations
 */
@RestController
@RequestMapping("/api/v1/file-tags")
public class FileTaggingController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTaggingController.class);
    
    @Autowired
    private FileTaggingService fileTaggingService;
    
    /**
     * Get all tags for a tenant
     */
    @GetMapping
    public ResponseEntity<List<FileTag>> getAllTags(@RequestParam String tenantId) {
        try {
            List<FileTag> tags = fileTaggingService.getAllTags(tenantId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user-created tags for a tenant
     */
    @GetMapping("/user-tags")
    public ResponseEntity<List<FileTag>> getUserTags(@RequestParam String tenantId) {
        try {
            List<FileTag> tags = fileTaggingService.getUserTags(tenantId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving user tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get system tags for a tenant
     */
    @GetMapping("/system-tags")
    public ResponseEntity<List<FileTag>> getSystemTags(@RequestParam String tenantId) {
        try {
            List<FileTag> tags = fileTaggingService.getSystemTags(tenantId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving system tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create a new tag
     */
    @PostMapping
    public ResponseEntity<FileTag> createTag(
            @RequestParam String tenantId,
            @RequestParam String tagName,
            @RequestParam(defaultValue = "#2196f3") String tagColor,
            @RequestParam(required = false) String tagDescription,
            @RequestParam String createdBy) {
        try {
            FileTag tag = fileTaggingService.createTag(tenantId, tagName, tagColor, tagDescription, createdBy);
            logger.info("Created tag '{}' for tenant: {}", tagName, tenantId);
            return ResponseEntity.ok(tag);
        } catch (RuntimeException e) {
            logger.warn("Failed to create tag '{}' for tenant {}: {}", tagName, tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating tag '{}' for tenant {}: {}", tagName, tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update an existing tag
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<FileTag> updateTag(
            @PathVariable Long tagId,
            @RequestParam String tagName,
            @RequestParam String tagColor,
            @RequestParam(required = false) String tagDescription) {
        try {
            FileTag tag = fileTaggingService.updateTag(tagId, tagName, tagColor, tagDescription);
            logger.info("Updated tag ID: {}", tagId);
            return ResponseEntity.ok(tag);
        } catch (RuntimeException e) {
            logger.warn("Failed to update tag {}: {}", tagId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating tag {}: {}", tagId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a tag
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<String> deleteTag(@PathVariable Long tagId) {
        try {
            fileTaggingService.deleteTag(tagId);
            logger.info("Deleted tag ID: {}", tagId);
            return ResponseEntity.ok("Tag deleted successfully");
        } catch (RuntimeException e) {
            logger.warn("Failed to delete tag {}: {}", tagId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting tag {}: {}", tagId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Add tag to file transfer
     */
    @PostMapping("/file-transfers/{fileTransferId}/tags/{tagId}")
    public ResponseEntity<String> addTagToFileTransfer(
            @PathVariable Long fileTransferId,
            @PathVariable Long tagId,
            @RequestParam String taggedBy,
            @RequestParam(required = false) String reason) {
        try {
            fileTaggingService.addTagToFileTransfer(fileTransferId, tagId, taggedBy, reason);
            logger.info("Added tag {} to file transfer {} by {}", tagId, fileTransferId, taggedBy);
            return ResponseEntity.ok("Tag added successfully");
        } catch (RuntimeException e) {
            logger.warn("Failed to add tag {} to file transfer {}: {}", tagId, fileTransferId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error adding tag {} to file transfer {}: {}", tagId, fileTransferId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Remove tag from file transfer
     */
    @DeleteMapping("/file-transfers/{fileTransferId}/tags/{tagId}")
    public ResponseEntity<String> removeTagFromFileTransfer(
            @PathVariable Long fileTransferId,
            @PathVariable Long tagId) {
        try {
            fileTaggingService.removeTagFromFileTransfer(fileTransferId, tagId);
            logger.info("Removed tag {} from file transfer {}", tagId, fileTransferId);
            return ResponseEntity.ok("Tag removed successfully");
        } catch (Exception e) {
            logger.error("Error removing tag {} from file transfer {}: {}", tagId, fileTransferId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get tags for a file transfer
     */
    @GetMapping("/file-transfers/{fileTransferId}/tags")
    public ResponseEntity<List<FileTag>> getTagsForFileTransfer(@PathVariable Long fileTransferId) {
        try {
            List<FileTag> tags = fileTaggingService.getTagsForFileTransfer(fileTransferId);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving tags for file transfer {}: {}", fileTransferId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file transfers by tag
     */
    @GetMapping("/tag/{tagName}/file-transfers")
    public ResponseEntity<List<Long>> getFileTransfersByTag(
            @RequestParam String tenantId,
            @PathVariable String tagName) {
        try {
            List<Long> fileTransferIds = fileTaggingService.getFileTransfersByTag(tenantId, tagName);
            return ResponseEntity.ok(fileTransferIds);
        } catch (Exception e) {
            logger.error("Error retrieving file transfers for tag '{}' in tenant {}: {}", tagName, tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Bulk add tags to multiple file transfers
     */
    @PostMapping("/bulk/add")
    public ResponseEntity<FileTaggingService.BulkTagResult> bulkAddTags(
            @RequestBody BulkTagRequest request) {
        try {
            FileTaggingService.BulkTagResult result = fileTaggingService.bulkAddTags(
                request.getFileTransferIds(), 
                request.getTagIds(), 
                request.getTaggedBy(), 
                request.getReason()
            );
            
            logger.info("Bulk tag operation completed: {} successes, {} errors", 
                result.getSuccessCount(), result.getErrorCount());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in bulk tag operation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Bulk remove tags from multiple file transfers
     */
    @PostMapping("/bulk/remove")
    public ResponseEntity<FileTaggingService.BulkTagResult> bulkRemoveTags(
            @RequestBody BulkTagRequest request) {
        try {
            FileTaggingService.BulkTagResult result = fileTaggingService.bulkRemoveTags(
                request.getFileTransferIds(), 
                request.getTagIds()
            );
            
            logger.info("Bulk tag removal completed: {} successes, {} errors", 
                result.getSuccessCount(), result.getErrorCount());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in bulk tag removal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search tags by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<FileTag>> searchTags(
            @RequestParam String tenantId,
            @RequestParam String searchTerm) {
        try {
            List<FileTag> tags = fileTaggingService.searchTags(tenantId, searchTerm);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error searching tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get tag statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTagStatistics(@RequestParam String tenantId) {
        try {
            Map<String, Object> statistics = fileTaggingService.getTagStatistics(tenantId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving tag statistics for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get most used tags
     */
    @GetMapping("/most-used")
    public ResponseEntity<List<FileTag>> getMostUsedTags(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FileTag> tags = fileTaggingService.getMostUsedTags(tenantId, limit);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving most used tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Initialize system tags for a tenant
     */
    @PostMapping("/system-tags/initialize")
    public ResponseEntity<String> initializeSystemTags(@RequestParam String tenantId) {
        try {
            fileTaggingService.createSystemTags(tenantId);
            logger.info("Initialized system tags for tenant: {}", tenantId);
            return ResponseEntity.ok("System tags initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing system tags for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Auto-tag a file transfer
     */
    @PostMapping("/file-transfers/{fileTransferId}/auto-tag")
    public ResponseEntity<String> autoTagFileTransfer(
            @PathVariable Long fileTransferId,
            @RequestParam String tenantId) {
        try {
            fileTaggingService.autoTagFile(fileTransferId, tenantId);
            logger.info("Auto-tagged file transfer: {}", fileTransferId);
            return ResponseEntity.ok("Auto-tagging completed successfully");
        } catch (Exception e) {
            logger.error("Error auto-tagging file transfer {}: {}", fileTransferId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request classes
    
    public static class BulkTagRequest {
        private List<Long> fileTransferIds;
        private List<Long> tagIds;
        private String taggedBy;
        private String reason;
        
        // Getters and Setters
        public List<Long> getFileTransferIds() { return fileTransferIds; }
        public void setFileTransferIds(List<Long> fileTransferIds) { this.fileTransferIds = fileTransferIds; }
        
        public List<Long> getTagIds() { return tagIds; }
        public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }
        
        public String getTaggedBy() { return taggedBy; }
        public void setTaggedBy(String taggedBy) { this.taggedBy = taggedBy; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}