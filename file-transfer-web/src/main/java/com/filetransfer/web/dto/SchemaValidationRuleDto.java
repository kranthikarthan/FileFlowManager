package com.filetransfer.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class SchemaValidationRuleDto {
    
    private Long id;
    private Long schemaId;
    
    @NotBlank
    private String ruleName;
    
    @NotBlank
    private String ruleType; // REGEX, JSON_SCHEMA, XML_SCHEMA, CUSTOM
    
    @NotBlank
    private String ruleDefinition;
    
    @NotNull
    private Integer ruleOrder = 0;
    
    @NotNull
    private Boolean isActive = true;
    
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public SchemaValidationRuleDto() {}
    
    public SchemaValidationRuleDto(String ruleName, String ruleType, String ruleDefinition, String errorMessage) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.ruleDefinition = ruleDefinition;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    
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
}