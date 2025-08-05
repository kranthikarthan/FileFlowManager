package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "schema_fields")
public class SchemaField {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", nullable = false)
    private FileSchema schema;
    
    @NotBlank
    @Column(name = "field_name", nullable = false, length = 200)
    private String fieldName;
    
    @NotBlank
    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // STRING, INTEGER, DECIMAL, DATE, BOOLEAN, etc.
    
    @Column(name = "field_length")
    private Integer fieldLength;
    
    @NotNull
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;
    
    @NotNull
    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;
    
    @Column(name = "default_value", length = 500)
    private String defaultValue;
    
    @Column(name = "validation_regex", length = 500)
    private String validationRegex;
    
    @NotNull
    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder = 0;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public SchemaField() {}
    
    public SchemaField(FileSchema schema, String fieldName, String fieldType, Boolean isRequired, Integer fieldOrder) {
        this.schema = schema;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.isRequired = isRequired;
        this.fieldOrder = fieldOrder;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public FileSchema getSchema() { return schema; }
    public void setSchema(FileSchema schema) { this.schema = schema; }
    
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    
    public Integer getFieldLength() { return fieldLength; }
    public void setFieldLength(Integer fieldLength) { this.fieldLength = fieldLength; }
    
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    
    public Boolean getIsUnique() { return isUnique; }
    public void setIsUnique(Boolean isUnique) { this.isUnique = isUnique; }
    
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    
    public String getValidationRegex() { return validationRegex; }
    public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }
    
    public Integer getFieldOrder() { return fieldOrder; }
    public void setFieldOrder(Integer fieldOrder) { this.fieldOrder = fieldOrder; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}