package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a file tag for categorization and organization
 */
@Entity
@Table(name = "file_tags")
public class FileTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;
    
    @Column(name = "tag_color", length = 7) // Hex color code
    private String tagColor = "#2196f3"; // Default blue
    
    @Column(name = "tag_description", length = 500)
    private String tagDescription;
    
    @Column(name = "is_system_tag")
    private Boolean isSystemTag = false;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public FileTag() {
        this.createdAt = LocalDateTime.now();
    }
    
    public FileTag(String tenantId, String tagName, String tagColor, String createdBy) {
        this();
        this.tenantId = tenantId;
        this.tagName = tagName;
        this.tagColor = tagColor;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    
    public String getTagColor() { return tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }
    
    public String getTagDescription() { return tagDescription; }
    public void setTagDescription(String tagDescription) { this.tagDescription = tagDescription; }
    
    public Boolean getIsSystemTag() { return isSystemTag; }
    public void setIsSystemTag(Boolean isSystemTag) { this.isSystemTag = isSystemTag; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}