package com.filetransfer.web.entity;

/**
 * Enum for business rule types
 */
public enum RuleType {
    
    // Routing Rules
    ROUTING("Route files to specific destinations based on conditions"),
    PARTNER_ROUTING("Route files to specific partners or external systems"),
    SERVICE_ROUTING("Route files between different services or sub-services"),
    
    // Processing Rules
    CONDITIONAL_PROCESSING("Apply different processing logic based on conditions"),
    PRIORITY_PROCESSING("Assign processing priority based on file characteristics"),
    BATCH_PROCESSING("Group files for batch processing based on criteria"),
    
    // Validation Rules
    PRE_PROCESSING_VALIDATION("Validate files before processing"),
    POST_PROCESSING_VALIDATION("Validate files after processing"),
    BUSINESS_VALIDATION("Apply business-specific validation rules"),
    
    // Transformation Rules
    DATA_TRANSFORMATION("Transform file content based on rules"),
    FORMAT_CONVERSION("Convert files between different formats"),
    DATA_ENRICHMENT("Enrich file data with additional information"),
    
    // Notification Rules
    ALERT_NOTIFICATION("Send alerts based on file processing events"),
    STATUS_NOTIFICATION("Notify stakeholders of status changes"),
    ESCALATION_NOTIFICATION("Escalate issues based on conditions"),
    
    // Security Rules
    SECURITY_CLASSIFICATION("Classify files based on security requirements"),
    ACCESS_CONTROL("Control file access based on conditions"),
    ENCRYPTION_RULES("Apply encryption based on file characteristics"),
    
    // Compliance Rules
    RETENTION_RULES("Apply data retention policies"),
    AUDIT_RULES("Apply audit requirements based on file type"),
    REGULATORY_COMPLIANCE("Ensure regulatory compliance based on content"),
    
    // Quality Rules
    QUALITY_ASSESSMENT("Assess file quality and take actions"),
    DATA_CLEANSING("Apply data cleansing rules"),
    QUALITY_ROUTING("Route files based on quality scores"),
    
    // Workflow Rules
    APPROVAL_WORKFLOW("Require approvals for specific files"),
    MULTI_STEP_WORKFLOW("Execute multi-step processing workflows"),
    CONDITIONAL_WORKFLOW("Execute different workflows based on conditions"),
    
    // Integration Rules
    EXTERNAL_SYSTEM_INTEGRATION("Integrate with external systems"),
    API_ORCHESTRATION("Orchestrate API calls based on file events"),
    MESSAGE_ROUTING("Route messages to external queues or topics"),
    
    // Performance Rules
    LOAD_BALANCING("Balance processing load across resources"),
    RESOURCE_OPTIMIZATION("Optimize resource usage based on file characteristics"),
    PERFORMANCE_TUNING("Apply performance optimizations based on conditions"),
    
    // Custom Rules
    CUSTOM_BUSINESS_LOGIC("Execute custom business logic"),
    TENANT_SPECIFIC("Tenant-specific custom rules"),
    INDUSTRY_SPECIFIC("Industry-specific processing rules");
    
    private final String description;
    
    RuleType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get rule types by category
     */
    public static RuleType[] getRoutingRules() {
        return new RuleType[]{ROUTING, PARTNER_ROUTING, SERVICE_ROUTING};
    }
    
    public static RuleType[] getProcessingRules() {
        return new RuleType[]{CONDITIONAL_PROCESSING, PRIORITY_PROCESSING, BATCH_PROCESSING};
    }
    
    public static RuleType[] getValidationRules() {
        return new RuleType[]{PRE_PROCESSING_VALIDATION, POST_PROCESSING_VALIDATION, BUSINESS_VALIDATION};
    }
    
    public static RuleType[] getWorkflowRules() {
        return new RuleType[]{APPROVAL_WORKFLOW, MULTI_STEP_WORKFLOW, CONDITIONAL_WORKFLOW};
    }
    
    public static RuleType[] getSecurityRules() {
        return new RuleType[]{SECURITY_CLASSIFICATION, ACCESS_CONTROL, ENCRYPTION_RULES};
    }
    
    /**
     * Check if rule type requires human intervention
     */
    public boolean requiresHumanIntervention() {
        return this == APPROVAL_WORKFLOW || 
               this == ESCALATION_NOTIFICATION ||
               this == BUSINESS_VALIDATION;
    }
    
    /**
     * Check if rule type is time-sensitive
     */
    public boolean isTimeSensitive() {
        return this == PRIORITY_PROCESSING ||
               this == ALERT_NOTIFICATION ||
               this == ESCALATION_NOTIFICATION ||
               this == REGULATORY_COMPLIANCE;
    }
    
    /**
     * Check if rule type affects routing
     */
    public boolean affectsRouting() {
        return this == ROUTING ||
               this == PARTNER_ROUTING ||
               this == SERVICE_ROUTING ||
               this == QUALITY_ROUTING;
    }
}