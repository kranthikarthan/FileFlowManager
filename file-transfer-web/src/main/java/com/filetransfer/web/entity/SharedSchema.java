package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared schema entity that can be reused across multiple subservices and directions.
 * Extends the existing FileSchema concept to support reusability.
 */
@Entity
@Table(name = "shared_schemas")
public class SharedSchema {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @Column(name = "schema_name", nullable = false)
    @NotBlank(message = "Schema name is required")
    private String schemaName;
    
    @Column(name = "schema_version", nullable = false)
    @NotBlank(message = "Schema version is required")
    private String schemaVersion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schema_type", nullable = false)
    @NotNull(message = "Schema type is required")
    private SchemaType schemaType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    @NotNull(message = "File type is required")
    private FileType fileType;
    
    @Column(name = "schema_definition", columnDefinition = "TEXT")
    private String schemaDefinition;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false; // Can be used across tenants
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Tags for categorization and search
    @ElementCollection
    @CollectionTable(name = "shared_schema_tags", joinColumns = @JoinColumn(name = "schema_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
    
    // EOT-specific fields for file count validation
    @Column(name = "eot_count_field_path")
    private String eotCountFieldPath; // JSON path or field name for count validation
    
    @Column(name = "supports_count_validation")
    private Boolean supportsCountValidation = false;
    
    public enum SchemaType {
        JSON_SCHEMA,
        XML_SCHEMA,
        COBOL_COPYBOOK,
        CSV_DEFINITION,
        FIXED_WIDTH_DEFINITION,
        CUSTOM
    }
    
    // Constructors
    public SharedSchema() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SharedSchema(String tenantId, String schemaName, SchemaType schemaType, FileType fileType) {
        this();
        this.tenantId = tenantId;
        this.schemaName = schemaName;
        this.schemaType = schemaType;
        this.fileType = fileType;
    }
    
    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
    
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.add(tag.toLowerCase().trim());
    }
    
    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag.toLowerCase().trim());
        }
    }
    
    public boolean hasTag(String tag) {
        return this.tags != null && this.tags.contains(tag.toLowerCase().trim());
    }
    
    public String getFullName() {
        return String.format("%s v%s (%s)", schemaName, schemaVersion, fileType);
    }
    
    public boolean canBeUsedByTenant(String requestingTenantId) {
        return isGlobal || tenantId.equals(requestingTenantId);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public SchemaType getSchemaType() { return schemaType; }
    public void setSchemaType(SchemaType schemaType) { this.schemaType = schemaType; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public String getSchemaDefinition() { return schemaDefinition; }
    public void setSchemaDefinition(String schemaDefinition) { this.schemaDefinition = schemaDefinition; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    
    public String getEotCountFieldPath() { return eotCountFieldPath; }
    public void setEotCountFieldPath(String eotCountFieldPath) { this.eotCountFieldPath = eotCountFieldPath; }
    
    public Boolean getSupportsCountValidation() { return supportsCountValidation; }
    public void setSupportsCountValidation(Boolean supportsCountValidation) { this.supportsCountValidation = supportsCountValidation; }
}