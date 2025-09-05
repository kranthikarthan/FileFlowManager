package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a workflow definition for multi-step file processing
 */
@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "workflow_name", nullable = false, length = 200)
    private String workflowName;
    
    @Column(name = "workflow_description", length = 1000)
    private String workflowDescription;
    
    @Column(name = "workflow_version", length = 20)
    private String workflowVersion = "1.0";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_type")
    private WorkflowType workflowType;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(name = "sub_service_name", length = 100)
    private String subServiceName;
    
    @Column(name = "workflow_steps", columnDefinition = "TEXT")
    private String workflowSteps; // JSON array of workflow steps
    
    @Column(name = "trigger_conditions", columnDefinition = "TEXT")
    private String triggerConditions; // JSON conditions that trigger this workflow
    
    @Column(name = "workflow_variables", columnDefinition = "TEXT")
    private String workflowVariables; // JSON object of workflow variables
    
    @Column(name = "error_handling", columnDefinition = "TEXT")
    private String errorHandling; // JSON error handling configuration
    
    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes = 60; // Default 1 hour timeout
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_template")
    private Boolean isTemplate = false;
    
    @Column(name = "parent_template_id")
    private Long parentTemplateId; // Reference to template if created from one
    
    @Column(name = "execution_count")
    private Long executionCount = 0L;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "average_duration_minutes")
    private Double averageDurationMinutes;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WorkflowDefinition() {
        this.createdAt = LocalDateTime.now();
    }
    
    public WorkflowDefinition(String tenantId, String workflowName, WorkflowType workflowType, String createdBy) {
        this();
        this.tenantId = tenantId;
        this.workflowName = workflowName;
        this.workflowType = workflowType;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    
    public String getWorkflowDescription() { return workflowDescription; }
    public void setWorkflowDescription(String workflowDescription) { this.workflowDescription = workflowDescription; }
    
    public String getWorkflowVersion() { return workflowVersion; }
    public void setWorkflowVersion(String workflowVersion) { this.workflowVersion = workflowVersion; }
    
    public WorkflowType getWorkflowType() { return workflowType; }
    public void setWorkflowType(WorkflowType workflowType) { this.workflowType = workflowType; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public String getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(String workflowSteps) { this.workflowSteps = workflowSteps; }
    
    public String getTriggerConditions() { return triggerConditions; }
    public void setTriggerConditions(String triggerConditions) { this.triggerConditions = triggerConditions; }
    
    public String getWorkflowVariables() { return workflowVariables; }
    public void setWorkflowVariables(String workflowVariables) { this.workflowVariables = workflowVariables; }
    
    public String getErrorHandling() { return errorHandling; }
    public void setErrorHandling(String errorHandling) { this.errorHandling = errorHandling; }
    
    public Integer getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(Integer timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Boolean isTemplate) { this.isTemplate = isTemplate; }
    
    public Long getParentTemplateId() { return parentTemplateId; }
    public void setParentTemplateId(Long parentTemplateId) { this.parentTemplateId = parentTemplateId; }
    
    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
    
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    
    public Double getAverageDurationMinutes() { return averageDurationMinutes; }
    public void setAverageDurationMinutes(Double averageDurationMinutes) { this.averageDurationMinutes = averageDurationMinutes; }
    
    public LocalDateTime getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record workflow execution statistics
     */
    public void recordExecution(boolean success, double durationMinutes) {
        this.executionCount++;
        if (success) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        this.lastExecutedAt = LocalDateTime.now();
        
        // Update average duration
        if (this.averageDurationMinutes == null) {
            this.averageDurationMinutes = durationMinutes;
        } else {
            this.averageDurationMinutes = 
                (this.averageDurationMinutes * (this.executionCount - 1) + durationMinutes) / this.executionCount;
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