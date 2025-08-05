package com.filetransfer.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class FileSchemaDto {
    
    private Long id;
    
    @NotBlank
    private String tenantId;
    
    @NotBlank
    private String serviceType;
    
    @NotBlank
    private String schemaName;
    
    @NotBlank
    private String schemaVersion = "1.0";
    
    @NotBlank
    private String schemaType; // JSON, XML, CSV, FIXED_WIDTH, etc.
    
    @NotBlank
    private String schemaDefinition;
    
    @NotNull
    private Boolean isActive = true;
    
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String description;
    
    // Related data
    private List<SchemaValidationRuleDto> validationRules;
    private List<SchemaFieldDto> fields;
    
    // Constructors
    public FileSchemaDto() {}
    
    public FileSchemaDto(String tenantId, String serviceType, String schemaName, String schemaType, String schemaDefinition) {
        this.tenantId = tenantId;
        this.serviceType = serviceType;
        this.schemaName = schemaName;
        this.schemaType = schemaType;
        this.schemaDefinition = schemaDefinition;
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
    
    public List<SchemaValidationRuleDto> getValidationRules() { return validationRules; }
    public void setValidationRules(List<SchemaValidationRuleDto> validationRules) { this.validationRules = validationRules; }
    
    public List<SchemaFieldDto> getFields() { return fields; }
    public void setFields(List<SchemaFieldDto> fields) { this.fields = fields; }
}