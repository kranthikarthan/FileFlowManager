package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a workflow execution instance
 */
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "workflow_definition_id", nullable = false)
    private Long workflowDefinitionId;
    
    @Column(name = "file_transfer_id")
    private Long fileTransferId; // File that triggered this workflow
    
    @Column(name = "execution_id", unique = true, length = 100)
    private String executionId; // Unique identifier for this execution
    
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status")
    private WorkflowExecutionStatus executionStatus = WorkflowExecutionStatus.PENDING;
    
    @Column(name = "current_step")
    private Integer currentStep = 0;
    
    @Column(name = "total_steps")
    private Integer totalSteps;
    
    @Column(name = "execution_context", columnDefinition = "TEXT")
    private String executionContext; // JSON context variables and state
    
    @Column(name = "step_results", columnDefinition = "TEXT")
    private String stepResults; // JSON array of step execution results
    
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails; // JSON error information
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "duration_minutes")
    private Double durationMinutes;
    
    @Column(name = "triggered_by", length = 100)
    private String triggeredBy; // User or system that triggered the workflow
    
    @Column(name = "trigger_event", length = 200)
    private String triggerEvent; // Event that triggered the workflow
    
    @Column(name = "priority")
    private Integer priority = 50; // 1-100, higher = more priority
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "timeout_at")
    private LocalDateTime timeoutAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", insertable = false, updatable = false)
    private WorkflowDefinition workflowDefinition;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_transfer_id", insertable = false, updatable = false)
    private FileTransferRecord fileTransfer;
    
    // Constructors
    public WorkflowExecution() {
        this.createdAt = LocalDateTime.now();
        this.executionId = generateExecutionId();
    }
    
    public WorkflowExecution(String tenantId, Long workflowDefinitionId, Long fileTransferId, String triggeredBy) {
        this();
        this.tenantId = tenantId;
        this.workflowDefinitionId = workflowDefinitionId;
        this.fileTransferId = fileTransferId;
        this.triggeredBy = triggeredBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public Long getWorkflowDefinitionId() { return workflowDefinitionId; }
    public void setWorkflowDefinitionId(Long workflowDefinitionId) { this.workflowDefinitionId = workflowDefinitionId; }
    
    public Long getFileTransferId() { return fileTransferId; }
    public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
    
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public WorkflowExecutionStatus getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(WorkflowExecutionStatus executionStatus) { this.executionStatus = executionStatus; }
    
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    
    public Integer getTotalSteps() { return totalSteps; }
    public void setTotalSteps(Integer totalSteps) { this.totalSteps = totalSteps; }
    
    public String getExecutionContext() { return executionContext; }
    public void setExecutionContext(String executionContext) { this.executionContext = executionContext; }
    
    public String getStepResults() { return stepResults; }
    public void setStepResults(String stepResults) { this.stepResults = stepResults; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public Double getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Double durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    
    public LocalDateTime getTimeoutAt() { return timeoutAt; }
    public void setTimeoutAt(LocalDateTime timeoutAt) { this.timeoutAt = timeoutAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public WorkflowDefinition getWorkflowDefinition() { return workflowDefinition; }
    public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) { this.workflowDefinition = workflowDefinition; }
    
    public FileTransferRecord getFileTransfer() { return fileTransfer; }
    public void setFileTransfer(FileTransferRecord fileTransfer) { this.fileTransfer = fileTransfer; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Start workflow execution
     */
    public void start() {
        this.executionStatus = WorkflowExecutionStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.currentStep = 1;
    }
    
    /**
     * Complete workflow execution
     */
    public void complete() {
        this.executionStatus = WorkflowExecutionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.durationMinutes = java.time.Duration.between(this.startedAt, this.completedAt).toMinutes();
        }
    }
    
    /**
     * Fail workflow execution
     */
    public void fail(String errorMessage) {
        this.executionStatus = WorkflowExecutionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorDetails = errorMessage;
        if (this.startedAt != null) {
            this.durationMinutes = java.time.Duration.between(this.startedAt, this.completedAt).toMinutes();
        }
    }
    
    /**
     * Advance to next step
     */
    public void advanceStep() {
        this.currentStep++;
    }
    
    /**
     * Check if workflow is complete
     */
    public boolean isComplete() {
        return this.executionStatus == WorkflowExecutionStatus.COMPLETED ||
               this.executionStatus == WorkflowExecutionStatus.FAILED ||
               this.executionStatus == WorkflowExecutionStatus.CANCELLED;
    }
    
    /**
     * Calculate progress percentage
     */
    public double getProgressPercentage() {
        if (totalSteps == null || totalSteps == 0) return 0.0;
        return (double) (currentStep - 1) / totalSteps * 100.0;
    }
    
    /**
     * Generate unique execution ID
     */
    private String generateExecutionId() {
        return "WF-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}