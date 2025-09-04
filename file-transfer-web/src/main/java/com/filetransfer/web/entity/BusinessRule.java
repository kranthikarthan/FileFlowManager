package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a business rule for file processing automation
 */
@Entity
@Table(name = "business_rules")
public class BusinessRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;
    
    @Column(name = "rule_description", length = 1000)
    private String ruleDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;
    
    @Column(name = "service_name", length = 100)
    private String serviceName; // Apply to specific service or null for all
    
    @Column(name = "sub_service_name", length = 100)
    private String subServiceName; // Apply to specific sub-service or null for all
    
    @Column(name = "rule_conditions", columnDefinition = "TEXT")
    private String ruleConditions; // JSON string defining conditions
    
    @Column(name = "rule_actions", columnDefinition = "TEXT")
    private String ruleActions; // JSON string defining actions
    
    @Column(name = "rule_priority")
    private Integer rulePriority = 50; // 1-100, higher = more priority
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "execution_order")
    private Integer executionOrder = 100;
    
    @Column(name = "rule_category", length = 50)
    private String ruleCategory; // ROUTING, VALIDATION, PROCESSING, NOTIFICATION
    
    @Column(name = "applies_to_direction")
    @Enumerated(EnumType.STRING)
    private TransferDirection appliesToDirection; // INBOUND, OUTBOUND, or null for both
    
    @Column(name = "applies_to_file_types", length = 500)
    private String appliesToFileTypes; // Comma-separated list of file types
    
    @Column(name = "execution_count")
    private Long executionCount = 0L;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "average_execution_time_ms")
    private Long averageExecutionTimeMs;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;
    
    // Constructors
    public BusinessRule() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BusinessRule(String tenantId, String ruleName, RuleType ruleType, String createdBy) {
        this();
        this.tenantId = tenantId;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
    public String getRuleDescription() { return ruleDescription; }
    public void setRuleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; }
    
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public String getRuleConditions() { return ruleConditions; }
    public void setRuleConditions(String ruleConditions) { this.ruleConditions = ruleConditions; }
    
    public String getRuleActions() { return ruleActions; }
    public void setRuleActions(String ruleActions) { this.ruleActions = ruleActions; }
    
    public Integer getRulePriority() { return rulePriority; }
    public void setRulePriority(Integer rulePriority) { this.rulePriority = rulePriority; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
    
    public String getRuleCategory() { return ruleCategory; }
    public void setRuleCategory(String ruleCategory) { this.ruleCategory = ruleCategory; }
    
    public TransferDirection getAppliesToDirection() { return appliesToDirection; }
    public void setAppliesToDirection(TransferDirection appliesToDirection) { this.appliesToDirection = appliesToDirection; }
    
    public String getAppliesToFileTypes() { return appliesToFileTypes; }
    public void setAppliesToFileTypes(String appliesToFileTypes) { this.appliesToFileTypes = appliesToFileTypes; }
    
    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
    
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    
    public LocalDateTime getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
    
    public Long getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
    public void setAverageExecutionTimeMs(Long averageExecutionTimeMs) { this.averageExecutionTimeMs = averageExecutionTimeMs; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Increment execution statistics
     */
    public void recordExecution(boolean success, long executionTimeMs) {
        this.executionCount++;
        if (success) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        this.lastExecutedAt = LocalDateTime.now();
        
        // Update average execution time
        if (this.averageExecutionTimeMs == null) {
            this.averageExecutionTimeMs = executionTimeMs;
        } else {
            this.averageExecutionTimeMs = 
                (this.averageExecutionTimeMs * (this.executionCount - 1) + executionTimeMs) / this.executionCount;
        }
    }
    
    /**
     * Calculate success rate
     */
    public double getSuccessRate() {
        if (executionCount == 0) return 0.0;
        return (double) successCount / executionCount * 100.0;
    }
}