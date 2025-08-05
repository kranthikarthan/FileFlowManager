package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "schema_validation_rules")
public class SchemaValidationRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", nullable = false)
    private FileSchema schema;
    
    @NotBlank
    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;
    
    @NotBlank
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // REGEX, JSON_SCHEMA, XML_SCHEMA, CUSTOM
    
    @NotBlank
    @Column(name = "rule_definition", nullable = false, columnDefinition = "TEXT")
    private String ruleDefinition;
    
    @NotNull
    @Column(name = "rule_order", nullable = false)
    private Integer ruleOrder = 0;
    
    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public SchemaValidationRule() {}
    
    public SchemaValidationRule(FileSchema schema, String ruleName, String ruleType, String ruleDefinition, String errorMessage) {
        this.schema = schema;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.ruleDefinition = ruleDefinition;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public FileSchema getSchema() { return schema; }
    public void setSchema(FileSchema schema) { this.schema = schema; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    
    public String getRuleDefinition() { return ruleDefinition; }
    public void setRuleDefinition(String ruleDefinition) { this.ruleDefinition = ruleDefinition; }
    
    public Integer getRuleOrder() { return ruleOrder; }
    public void setRuleOrder(Integer ruleOrder) { this.ruleOrder = ruleOrder; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}