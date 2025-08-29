package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mapping entity to track which subservices use which shared schemas.
 * Replaces individual schema ID fields with flexible schema usage tracking.
 */
@Entity
@Table(name = "schema_usage_mappings")
public class SchemaUsageMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_service_config_id", nullable = false)
    private SubServiceConfiguration subServiceConfiguration;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_schema_id", nullable = false)
    private SharedSchema sharedSchema;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransferDirection direction;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType = UsageType.VALIDATION;
    
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = true; // Primary schema for this direction/type
    
    @Column(name = "validation_enabled", nullable = false)
    private Boolean validationEnabled = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    // Custom configuration for this usage
    @Column(name = "custom_config", columnDefinition = "TEXT")
    private String customConfig; // JSON configuration specific to this usage
    
    public enum UsageType {
        VALIDATION,     // Schema used for validation
        TRANSFORMATION, // Schema used for data transformation
        DOCUMENTATION,  // Schema used for documentation only
        REFERENCE      // Schema used as reference/template
    }
    
    // Constructors
    public SchemaUsageMapping() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SchemaUsageMapping(SubServiceConfiguration subServiceConfiguration, 
                             SharedSchema sharedSchema, 
                             TransferDirection direction,
                             String createdBy) {
        this();
        this.subServiceConfiguration = subServiceConfiguration;
        this.sharedSchema = sharedSchema;
        this.direction = direction;
        this.createdBy = createdBy;
    }
    
    // Helper methods
    public void recordUsage() {
        this.usageCount = (this.usageCount == null ? 0L : this.usageCount) + 1L;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public String getDisplayName() {
        return String.format("%s - %s (%s)", 
                           sharedSchema.getSchemaName(), 
                           direction, 
                           usageType);
    }
    
    public boolean isActiveUsage() {
        return validationEnabled && 
               sharedSchema.getIsActive() && 
               subServiceConfiguration.getEnabled();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SubServiceConfiguration getSubServiceConfiguration() { return subServiceConfiguration; }
    public void setSubServiceConfiguration(SubServiceConfiguration subServiceConfiguration) { this.subServiceConfiguration = subServiceConfiguration; }
    
    public SharedSchema getSharedSchema() { return sharedSchema; }
    public void setSharedSchema(SharedSchema sharedSchema) { this.sharedSchema = sharedSchema; }
    
    public TransferDirection getDirection() { return direction; }
    public void setDirection(TransferDirection direction) { this.direction = direction; }
    
    public UsageType getUsageType() { return usageType; }
    public void setUsageType(UsageType usageType) { this.usageType = usageType; }
    
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    
    public Boolean getValidationEnabled() { return validationEnabled; }
    public void setValidationEnabled(Boolean validationEnabled) { this.validationEnabled = validationEnabled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public String getCustomConfig() { return customConfig; }
    public void setCustomConfig(String customConfig) { this.customConfig = customConfig; }
}