package com.filetransfer.web.entity;

/**
 * Enum for workflow types
 */
public enum WorkflowType {
    
    // Sequential Workflows
    LINEAR_SEQUENTIAL("Execute steps in linear sequence"),
    CONDITIONAL_SEQUENTIAL("Execute steps in sequence with conditional branching"),
    
    // Parallel Workflows  
    PARALLEL_EXECUTION("Execute multiple steps in parallel"),
    FORK_JOIN("Fork into parallel paths and join results"),
    
    // Event-Driven Workflows
    EVENT_TRIGGERED("Triggered by specific events"),
    TIME_TRIGGERED("Triggered by time-based conditions"),
    FILE_TRIGGERED("Triggered by file characteristics"),
    
    // Approval Workflows
    SINGLE_APPROVAL("Require single approval step"),
    MULTI_LEVEL_APPROVAL("Require multiple approval levels"),
    CONDITIONAL_APPROVAL("Approval based on conditions"),
    
    // Processing Workflows
    DATA_PROCESSING("Data transformation and processing"),
    VALIDATION_PROCESSING("Validation and quality checking"),
    INTEGRATION_PROCESSING("External system integration"),
    
    // Error Handling Workflows
    ERROR_RECOVERY("Automated error recovery procedures"),
    ESCALATION("Error escalation and notification"),
    RETRY_LOGIC("Automated retry with backoff"),
    
    // Business Process Workflows
    CUSTOMER_ONBOARDING("Customer data onboarding process"),
    TRANSACTION_PROCESSING("Transaction file processing"),
    REPORT_GENERATION("Automated report generation"),
    DATA_MIGRATION("Data migration workflows"),
    
    // Compliance Workflows
    AUDIT_WORKFLOW("Audit and compliance checking"),
    RETENTION_WORKFLOW("Data retention and archival"),
    SECURITY_WORKFLOW("Security validation and encryption"),
    
    // Integration Workflows
    API_ORCHESTRATION("API call orchestration"),
    MESSAGE_ROUTING("Message queue routing"),
    EXTERNAL_NOTIFICATION("External system notifications"),
    
    // Template Workflows
    TEMPLATE_BASIC("Basic workflow template"),
    TEMPLATE_ADVANCED("Advanced workflow template with all features"),
    TEMPLATE_CUSTOM("Custom workflow template");
    
    private final String description;
    
    WorkflowType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get workflow types by category
     */
    public static WorkflowType[] getSequentialWorkflows() {
        return new WorkflowType[]{LINEAR_SEQUENTIAL, CONDITIONAL_SEQUENTIAL};
    }
    
    public static WorkflowType[] getParallelWorkflows() {
        return new WorkflowType[]{PARALLEL_EXECUTION, FORK_JOIN};
    }
    
    public static WorkflowType[] getApprovalWorkflows() {
        return new WorkflowType[]{SINGLE_APPROVAL, MULTI_LEVEL_APPROVAL, CONDITIONAL_APPROVAL};
    }
    
    public static WorkflowType[] getBusinessWorkflows() {
        return new WorkflowType[]{CUSTOMER_ONBOARDING, TRANSACTION_PROCESSING, REPORT_GENERATION, DATA_MIGRATION};
    }
    
    /**
     * Check if workflow type requires human intervention
     */
    public boolean requiresHumanIntervention() {
        return this == SINGLE_APPROVAL || 
               this == MULTI_LEVEL_APPROVAL ||
               this == CONDITIONAL_APPROVAL ||
               this == ESCALATION;
    }
    
    /**
     * Check if workflow type supports parallel execution
     */
    public boolean supportsParallelExecution() {
        return this == PARALLEL_EXECUTION ||
               this == FORK_JOIN ||
               this == DATA_PROCESSING;
    }
    
    /**
     * Check if workflow type is time-sensitive
     */
    public boolean isTimeSensitive() {
        return this == TIME_TRIGGERED ||
               this == ESCALATION ||
               this == AUDIT_WORKFLOW ||
               this == RETENTION_WORKFLOW;
    }
}