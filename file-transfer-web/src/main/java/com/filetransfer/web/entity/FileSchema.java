package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file_schemas")
public class FileSchema {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
    
    @NotBlank
    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;
    
    @NotBlank
    @Column(name = "schema_name", nullable = false, length = 200)
    private String schemaName;
    
    @NotBlank
    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion = "1.0";
    
    @NotBlank
    @Column(name = "schema_type", nullable = false, length = 50)
    private String schemaType; // JSON, XML, CSV, FIXED_WIDTH, etc.
    
    @NotBlank
    @Column(name = "schema_definition", nullable = false, columnDefinition = "TEXT")
    private String schemaDefinition;
    
    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @NotBlank
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "description", length = 500)
    private String description;
    
    // Relationships
    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchemaValidationRule> validationRules;
    
    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchemaField> fields;
    
    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchemaUsageLog> usageLogs;
    
    // Constructors
    public FileSchema() {}
    
    public FileSchema(String tenantId, String serviceType, String schemaName, String schemaType, String schemaDefinition, String createdBy) {
        this.tenantId = tenantId;
        this.serviceType = serviceType;
        this.schemaName = schemaName;
        this.schemaType = schemaType;
        this.schemaDefinition = schemaDefinition;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    
    public String getSchemaType() { return schemaType; }
    public void setSchemaType(String schemaType) { this.schemaType = schemaType; }
    
    public String getSchemaDefinition() { return schemaDefinition; }
    public void setSchemaDefinition(String schemaDefinition) { this.schemaDefinition = schemaDefinition; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<SchemaValidationRule> getValidationRules() { return validationRules; }
    public void setValidationRules(List<SchemaValidationRule> validationRules) { this.validationRules = validationRules; }
    
    public List<SchemaField> getFields() { return fields; }
    public void setFields(List<SchemaField> fields) { this.fields = fields; }
    
    public List<SchemaUsageLog> getUsageLogs() { return usageLogs; }
    public void setUsageLogs(List<SchemaUsageLog> usageLogs) { this.usageLogs = usageLogs; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}