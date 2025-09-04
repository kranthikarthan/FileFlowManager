package com.filetransfer.web.entity;

/**
 * Enum for workflow execution status
 */
public enum WorkflowExecutionStatus {
    
    PENDING("Workflow is pending execution"),
    RUNNING("Workflow is currently executing"),
    PAUSED("Workflow execution is paused"),
    WAITING_FOR_APPROVAL("Workflow is waiting for human approval"),
    WAITING_FOR_EVENT("Workflow is waiting for an external event"),
    WAITING_FOR_RETRY("Workflow is waiting for retry after failure"),
    COMPLETED("Workflow completed successfully"),
    FAILED("Workflow failed and cannot continue"),
    CANCELLED("Workflow was cancelled by user or system"),
    TIMEOUT("Workflow timed out during execution"),
    SKIPPED("Workflow was skipped due to conditions");
    
    private final String description;
    
    WorkflowExecutionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if status indicates workflow is still active
     */
    public boolean isActive() {
        return this == PENDING || 
               this == RUNNING || 
               this == PAUSED ||
               this == WAITING_FOR_APPROVAL ||
               this == WAITING_FOR_EVENT ||
               this == WAITING_FOR_RETRY;
    }
    
    /**
     * Check if status indicates workflow is completed (success or failure)
     */
    public boolean isTerminal() {
        return this == COMPLETED ||
               this == FAILED ||
               this == CANCELLED ||
               this == TIMEOUT ||
               this == SKIPPED;
    }
    
    /**
     * Check if status indicates workflow needs attention
     */
    public boolean needsAttention() {
        return this == FAILED ||
               this == TIMEOUT ||
               this == WAITING_FOR_APPROVAL;
    }
    
    /**
     * Check if workflow can be retried
     */
    public boolean canRetry() {
        return this == FAILED ||
               this == TIMEOUT ||
               this == WAITING_FOR_RETRY;
    }
    
    /**
     * Check if workflow can be cancelled
     */
    public boolean canCancel() {
        return this == PENDING ||
               this == RUNNING ||
               this == PAUSED ||
               this == WAITING_FOR_APPROVAL ||
               this == WAITING_FOR_EVENT ||
               this == WAITING_FOR_RETRY;
    }
    
    /**
     * Get status color for UI display
     */
    public String getDisplayColor() {
        switch (this) {
            case PENDING:
            case WAITING_FOR_EVENT:
            case WAITING_FOR_RETRY:
                return "warning";
            case RUNNING:
                return "primary";
            case PAUSED:
            case WAITING_FOR_APPROVAL:
                return "info";
            case COMPLETED:
                return "success";
            case FAILED:
            case TIMEOUT:
                return "error";
            case CANCELLED:
            case SKIPPED:
                return "default";
            default:
                return "default";
        }
    }
    
    /**
     * Get next possible statuses
     */
    public WorkflowExecutionStatus[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING:
                return new WorkflowExecutionStatus[]{RUNNING, CANCELLED, SKIPPED};
            case RUNNING:
                return new WorkflowExecutionStatus[]{COMPLETED, FAILED, PAUSED, CANCELLED, 
                                                   WAITING_FOR_APPROVAL, WAITING_FOR_EVENT, TIMEOUT};
            case PAUSED:
                return new WorkflowExecutionStatus[]{RUNNING, CANCELLED};
            case WAITING_FOR_APPROVAL:
                return new WorkflowExecutionStatus[]{RUNNING, CANCELLED, TIMEOUT};
            case WAITING_FOR_EVENT:
                return new WorkflowExecutionStatus[]{RUNNING, CANCELLED, TIMEOUT};
            case WAITING_FOR_RETRY:
                return new WorkflowExecutionStatus[]{RUNNING, CANCELLED, FAILED};
            case FAILED:
                return new WorkflowExecutionStatus[]{WAITING_FOR_RETRY};
            default:
                return new WorkflowExecutionStatus[]{};
        }
    }
}