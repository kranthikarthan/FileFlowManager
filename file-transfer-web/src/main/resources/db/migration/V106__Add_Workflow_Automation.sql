-- Migration to add workflow automation and business rules
-- Version: V106
-- Description: Add comprehensive workflow automation, business rules engine, and intelligent processing

-- Create business_rules table
CREATE TABLE business_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    rule_description VARCHAR(1000),
    rule_type ENUM('ROUTING', 'PARTNER_ROUTING', 'SERVICE_ROUTING', 'CONDITIONAL_PROCESSING', 
                   'PRIORITY_PROCESSING', 'BATCH_PROCESSING', 'PRE_PROCESSING_VALIDATION',
                   'POST_PROCESSING_VALIDATION', 'BUSINESS_VALIDATION', 'DATA_TRANSFORMATION',
                   'FORMAT_CONVERSION', 'DATA_ENRICHMENT', 'ALERT_NOTIFICATION', 'STATUS_NOTIFICATION',
                   'ESCALATION_NOTIFICATION', 'SECURITY_CLASSIFICATION', 'ACCESS_CONTROL',
                   'ENCRYPTION_RULES', 'RETENTION_RULES', 'AUDIT_RULES', 'REGULATORY_COMPLIANCE',
                   'QUALITY_ASSESSMENT', 'DATA_CLEANSING', 'QUALITY_ROUTING', 'APPROVAL_WORKFLOW',
                   'MULTI_STEP_WORKFLOW', 'CONDITIONAL_WORKFLOW', 'EXTERNAL_SYSTEM_INTEGRATION',
                   'API_ORCHESTRATION', 'MESSAGE_ROUTING', 'LOAD_BALANCING', 'RESOURCE_OPTIMIZATION',
                   'PERFORMANCE_TUNING', 'CUSTOM_BUSINESS_LOGIC', 'TENANT_SPECIFIC', 'INDUSTRY_SPECIFIC') NOT NULL,
    service_name VARCHAR(100),
    sub_service_name VARCHAR(100),
    rule_conditions TEXT, -- JSON string defining conditions
    rule_actions TEXT, -- JSON string defining actions
    rule_priority INT DEFAULT 50, -- 1-100, higher = more priority
    is_active BOOLEAN DEFAULT TRUE,
    execution_order INT DEFAULT 100,
    rule_category VARCHAR(50), -- ROUTING, VALIDATION, PROCESSING, NOTIFICATION
    applies_to_direction ENUM('INBOUND', 'OUTBOUND'),
    applies_to_file_types VARCHAR(500), -- Comma-separated list
    execution_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    last_executed_at TIMESTAMP NULL,
    average_execution_time_ms BIGINT,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    effective_from TIMESTAMP NULL,
    effective_until TIMESTAMP NULL,
    
    UNIQUE KEY uk_business_rules (tenant_id, rule_name),
    INDEX idx_business_rules_tenant (tenant_id),
    INDEX idx_business_rules_active (tenant_id, is_active),
    INDEX idx_business_rules_type (tenant_id, rule_type),
    INDEX idx_business_rules_service (tenant_id, service_name),
    INDEX idx_business_rules_priority (tenant_id, rule_priority DESC),
    INDEX idx_business_rules_category (tenant_id, rule_category),
    INDEX idx_business_rules_direction (tenant_id, applies_to_direction),
    INDEX idx_business_rules_execution (tenant_id, execution_count DESC),
    INDEX idx_business_rules_effective (effective_from, effective_until)
);

-- Create workflow_definitions table
CREATE TABLE workflow_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    workflow_name VARCHAR(200) NOT NULL,
    workflow_description VARCHAR(1000),
    workflow_version VARCHAR(20) DEFAULT '1.0',
    workflow_type ENUM('LINEAR_SEQUENTIAL', 'CONDITIONAL_SEQUENTIAL', 'PARALLEL_EXECUTION',
                       'FORK_JOIN', 'EVENT_TRIGGERED', 'TIME_TRIGGERED', 'FILE_TRIGGERED',
                       'SINGLE_APPROVAL', 'MULTI_LEVEL_APPROVAL', 'CONDITIONAL_APPROVAL',
                       'DATA_PROCESSING', 'VALIDATION_PROCESSING', 'INTEGRATION_PROCESSING',
                       'ERROR_RECOVERY', 'ESCALATION', 'RETRY_LOGIC', 'CUSTOMER_ONBOARDING',
                       'TRANSACTION_PROCESSING', 'REPORT_GENERATION', 'DATA_MIGRATION',
                       'AUDIT_WORKFLOW', 'RETENTION_WORKFLOW', 'SECURITY_WORKFLOW',
                       'API_ORCHESTRATION', 'MESSAGE_ROUTING', 'EXTERNAL_NOTIFICATION',
                       'TEMPLATE_BASIC', 'TEMPLATE_ADVANCED', 'TEMPLATE_CUSTOM'),
    service_name VARCHAR(100),
    sub_service_name VARCHAR(100),
    workflow_steps TEXT, -- JSON array of workflow steps
    trigger_conditions TEXT, -- JSON conditions that trigger this workflow
    workflow_variables TEXT, -- JSON object of workflow variables
    error_handling TEXT, -- JSON error handling configuration
    timeout_minutes INT DEFAULT 60,
    max_retries INT DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    is_template BOOLEAN DEFAULT FALSE,
    parent_template_id BIGINT,
    execution_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    average_duration_minutes DOUBLE,
    last_executed_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_template_id) REFERENCES workflow_definitions(id) ON DELETE SET NULL,
    UNIQUE KEY uk_workflow_definitions (tenant_id, workflow_name, workflow_version),
    INDEX idx_workflow_definitions_tenant (tenant_id),
    INDEX idx_workflow_definitions_active (tenant_id, is_active),
    INDEX idx_workflow_definitions_type (tenant_id, workflow_type),
    INDEX idx_workflow_definitions_service (tenant_id, service_name),
    INDEX idx_workflow_definitions_template (tenant_id, is_template),
    INDEX idx_workflow_definitions_execution (tenant_id, execution_count DESC)
);

-- Create workflow_executions table
CREATE TABLE workflow_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    workflow_definition_id BIGINT NOT NULL,
    file_transfer_id BIGINT,
    execution_id VARCHAR(100) NOT NULL UNIQUE,
    execution_status ENUM('PENDING', 'RUNNING', 'PAUSED', 'WAITING_FOR_APPROVAL',
                          'WAITING_FOR_EVENT', 'WAITING_FOR_RETRY', 'COMPLETED',
                          'FAILED', 'CANCELLED', 'TIMEOUT', 'SKIPPED') DEFAULT 'PENDING',
    current_step INT DEFAULT 0,
    total_steps INT,
    execution_context TEXT, -- JSON context variables and state
    step_results TEXT, -- JSON array of step execution results
    error_details TEXT, -- JSON error information
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_minutes DOUBLE,
    triggered_by VARCHAR(100),
    trigger_event VARCHAR(200),
    priority INT DEFAULT 50,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP NULL,
    timeout_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    FOREIGN KEY (file_transfer_id) REFERENCES file_transfer_records(id) ON DELETE CASCADE,
    INDEX idx_workflow_executions_tenant (tenant_id),
    INDEX idx_workflow_executions_status (tenant_id, execution_status),
    INDEX idx_workflow_executions_workflow (workflow_definition_id),
    INDEX idx_workflow_executions_file (file_transfer_id),
    INDEX idx_workflow_executions_priority (tenant_id, priority DESC),
    INDEX idx_workflow_executions_retry (next_retry_at),
    INDEX idx_workflow_executions_timeout (timeout_at),
    INDEX idx_workflow_executions_created (tenant_id, created_at DESC)
);

-- Create workflow_step_templates table
CREATE TABLE workflow_step_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    template_description VARCHAR(1000),
    step_type VARCHAR(50) NOT NULL,
    step_configuration TEXT, -- JSON configuration for the step
    parameter_schema TEXT, -- JSON schema for step parameters
    is_system_template BOOLEAN DEFAULT FALSE,
    usage_count BIGINT DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_workflow_step_templates (tenant_id, template_name),
    INDEX idx_workflow_step_templates_tenant (tenant_id),
    INDEX idx_workflow_step_templates_type (step_type),
    INDEX idx_workflow_step_templates_system (tenant_id, is_system_template),
    INDEX idx_workflow_step_templates_usage (tenant_id, usage_count DESC)
);

-- Create workflow_execution_logs table for detailed step logging
CREATE TABLE workflow_execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    workflow_execution_id BIGINT NOT NULL,
    step_number INT NOT NULL,
    step_name VARCHAR(200),
    step_type VARCHAR(50),
    log_level ENUM('DEBUG', 'INFO', 'WARN', 'ERROR') DEFAULT 'INFO',
    log_message TEXT,
    log_details TEXT, -- JSON additional details
    execution_time_ms BIGINT,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE,
    INDEX idx_workflow_logs_execution (workflow_execution_id),
    INDEX idx_workflow_logs_tenant (tenant_id),
    INDEX idx_workflow_logs_level (tenant_id, log_level),
    INDEX idx_workflow_logs_time (logged_at)
);

-- Add workflow-related fields to file_transfer_records
ALTER TABLE file_transfer_records 
ADD COLUMN workflow_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN active_workflows INT DEFAULT 0,
ADD COLUMN completed_workflows INT DEFAULT 0,
ADD COLUMN failed_workflows INT DEFAULT 0,
ADD COLUMN last_workflow_execution_at TIMESTAMP NULL,
ADD COLUMN workflow_status ENUM('NONE', 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'WAITING') DEFAULT 'NONE',
ADD COLUMN processing_priority INT DEFAULT 50,
ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE,
ADD COLUMN approved_by VARCHAR(100),
ADD COLUMN approved_at TIMESTAMP NULL,
ADD COLUMN approval_comments TEXT;

-- Add indexes for workflow integration
CREATE INDEX idx_file_transfer_workflow_enabled ON file_transfer_records(tenant_id, workflow_enabled);
CREATE INDEX idx_file_transfer_workflow_status ON file_transfer_records(tenant_id, workflow_status);
CREATE INDEX idx_file_transfer_priority ON file_transfer_records(tenant_id, processing_priority DESC);
CREATE INDEX idx_file_transfer_approval ON file_transfer_records(tenant_id, requires_approval);

-- Insert default business rules
INSERT INTO business_rules (
    tenant_id, rule_name, rule_description, rule_type, rule_conditions, rule_actions,
    rule_priority, rule_category, created_by
) VALUES 
(
    'default', 'Auto-compress large files', 'Automatically enable compression for files larger than 10MB',
    'CONDITIONAL_PROCESSING',
    '{"field":"fileSize","operator":"GT","value":10485760}',
    '[{"type":"ENABLE_COMPRESSION","parameters":{"compressionType":"GZIP"}}]',
    80, 'PROCESSING', 'SYSTEM'
),
(
    'default', 'High priority customer data', 'Set high priority for customer data files',
    'PRIORITY_PROCESSING',
    '{"field":"serviceName","operator":"CONTAINS","value":"CUSTOMER"}',
    '[{"type":"SET_PRIORITY","parameters":{"priority":90}},{"type":"ADD_TAG","parameters":{"tagName":"high-priority"}}]',
    90, 'ROUTING', 'SYSTEM'
),
(
    'default', 'Notify on failures', 'Send notifications when files fail processing',
    'ALERT_NOTIFICATION',
    '{"field":"status","operator":"EQUALS","value":"FAILED"}',
    '[{"type":"SEND_NOTIFICATION","parameters":{"type":"EMAIL","message":"File processing failed","recipient":"operations@company.com"}}]',
    70, 'NOTIFICATION', 'SYSTEM'
),
(
    'default', 'Approve large files', 'Require approval for files larger than 100MB',
    'APPROVAL_WORKFLOW',
    '{"field":"fileSize","operator":"GT","value":104857600}',
    '[{"type":"REQUIRE_APPROVAL","parameters":{"approverRole":"FILE_MANAGER","reason":"Large file requires approval"}}]',
    95, 'VALIDATION', 'SYSTEM'
),
(
    'default', 'Route sensitive data', 'Route files with sensitive data to secure processing',
    'SECURITY_CLASSIFICATION',
    '{"operator":"OR","conditions":[{"field":"serviceName","operator":"CONTAINS","value":"CUSTOMER"},{"field":"serviceName","operator":"CONTAINS","value":"PERSONAL"}]}',
    '[{"type":"ROUTE_TO_SERVICE","parameters":{"targetService":"SECURE_PROCESSING"}},{"type":"ADD_TAG","parameters":{"tagName":"sensitive-data"}}]',
    85, 'ROUTING', 'SYSTEM'
),
(
    'default', 'Archive old files', 'Automatically archive completed files older than 30 days',
    'RETENTION_RULES',
    '{"operator":"AND","conditions":[{"field":"status","operator":"EQUALS","value":"COMPLETED"},{"field":"createdat","operator":"LT","value":"30_DAYS_AGO"}]}',
    '[{"type":"ADD_TAG","parameters":{"tagName":"archived"}},{"type":"MOVE_TO_ARCHIVE","parameters":{"archivePath":"/archive/{yyyy}/{mm}"}}]',
    60, 'PROCESSING', 'SYSTEM'
);

-- Insert default workflow definitions
INSERT INTO workflow_definitions (
    tenant_id, workflow_name, workflow_description, workflow_type, workflow_steps,
    trigger_conditions, timeout_minutes, is_template, created_by
) VALUES 
(
    'default', 'Standard File Processing', 'Standard workflow for regular file processing',
    'LINEAR_SEQUENTIAL',
    '[
        {"type":"VALIDATE_FILE","name":"File Validation","parameters":{"type":"FILE_EXISTS"}},
        {"type":"APPLY_BUSINESS_RULES","name":"Apply Business Rules","parameters":{}},
        {"type":"COMPRESS_FILE","name":"Conditional Compression","parameters":{"type":"AUTO"}},
        {"type":"LOG_EVENT","name":"Log Processing Start","parameters":{"level":"INFO","message":"Processing file {fileName}"}}
    ]',
    '{"events":["FILE_RECEIVED","FILE_UPLOADED"],"conditions":{"field":"status","operator":"EQUALS","value":"PENDING"}}',
    30, TRUE, 'SYSTEM'
),
(
    'default', 'Customer Data Processing', 'Specialized workflow for customer data files',
    'CONDITIONAL_SEQUENTIAL',
    '[
        {"type":"VALIDATE_FILE","name":"Customer Data Validation","parameters":{"type":"FILE_SIZE","minSize":1,"maxSize":104857600}},
        {"type":"REQUIRE_APPROVAL","name":"Customer Data Approval","parameters":{"approverRole":"DATA_STEWARD","reason":"Customer data requires approval"}},
        {"type":"ROUTE_FILE","name":"Route to Secure Processing","parameters":{"type":"SERVICE_ROUTING","targetService":"SECURE_CUSTOMER_PROCESSING"}},
        {"type":"SEND_NOTIFICATION","name":"Notify Data Team","parameters":{"type":"EMAIL","message":"Customer data file {fileName} processed","recipient":"data-team@company.com"}}
    ]',
    '{"events":["FILE_RECEIVED"],"conditions":{"field":"serviceName","operator":"CONTAINS","value":"CUSTOMER"}}',
    120, TRUE, 'SYSTEM'
),
(
    'default', 'Error Recovery Workflow', 'Automated error recovery and escalation',
    'ERROR_RECOVERY',
    '[
        {"type":"LOG_EVENT","name":"Log Error","parameters":{"level":"ERROR","message":"Processing failed for {fileName}: {errorMessage}"}},
        {"type":"DELAY","name":"Wait Before Retry","parameters":{"delayMinutes":5}},
        {"type":"VALIDATE_FILE","name":"Re-validate File","parameters":{"type":"FILE_EXISTS"}},
        {"type":"SEND_NOTIFICATION","name":"Escalate Error","parameters":{"type":"EMAIL","message":"File processing failed after retry: {fileName}","recipient":"operations@company.com"}}
    ]',
    '{"events":["FILE_PROCESSING_FAILED"],"conditions":{"field":"status","operator":"EQUALS","value":"FAILED"}}',
    60, TRUE, 'SYSTEM'
),
(
    'default', 'Large File Workflow', 'Specialized handling for large files',
    'PARALLEL_EXECUTION',
    '[
        {"type":"VALIDATE_FILE","name":"Large File Validation","parameters":{"type":"FILE_SIZE","minSize":104857600}},
        {"type":"REQUIRE_APPROVAL","name":"Large File Approval","parameters":{"approverRole":"FILE_MANAGER","reason":"Large file processing approval"}},
        {"type":"PARALLEL_EXECUTION","name":"Parallel Processing","parameters":{"steps":[
            {"type":"COMPRESS_FILE","parameters":{"type":"GZIP"}},
            {"type":"SEND_NOTIFICATION","parameters":{"type":"SLACK","message":"Large file processing started: {fileName}"}}
        ]}},
        {"type":"LOG_EVENT","name":"Log Completion","parameters":{"level":"INFO","message":"Large file {fileName} processed successfully"}}
    ]',
    '{"events":["FILE_RECEIVED"],"conditions":{"field":"fileSize","operator":"GT","value":104857600}}',
    180, TRUE, 'SYSTEM'
);

-- Insert default workflow step templates
INSERT INTO workflow_step_templates (
    tenant_id, template_name, template_description, step_type, step_configuration,
    parameter_schema, is_system_template, created_by
) VALUES 
(
    'default', 'File Size Validation', 'Validate file size within specified range',
    'VALIDATE_FILE',
    '{"type":"FILE_SIZE","minSize":1,"maxSize":104857600}',
    '{"type":"object","properties":{"minSize":{"type":"integer"},"maxSize":{"type":"integer"}}}',
    TRUE, 'SYSTEM'
),
(
    'default', 'Email Notification', 'Send email notification with custom message',
    'SEND_NOTIFICATION',
    '{"type":"EMAIL","message":"Workflow notification: {fileName}","recipient":"admin@company.com"}',
    '{"type":"object","properties":{"message":{"type":"string"},"recipient":{"type":"string"}}}',
    TRUE, 'SYSTEM'
),
(
    'default', 'Manager Approval', 'Require approval from manager role',
    'REQUIRE_APPROVAL',
    '{"approverRole":"MANAGER","reason":"File requires manager approval"}',
    '{"type":"object","properties":{"approverRole":{"type":"string"},"reason":{"type":"string"}}}',
    TRUE, 'SYSTEM'
),
(
    'default', 'Auto Compression', 'Automatically compress file with specified algorithm',
    'COMPRESS_FILE',
    '{"type":"GZIP","onlyIfBeneficial":true}',
    '{"type":"object","properties":{"type":{"type":"string","enum":["GZIP","ZIP","BZIP2"]}}}',
    TRUE, 'SYSTEM'
),
(
    'default', 'Service Routing', 'Route file to specific service',
    'ROUTE_FILE',
    '{"type":"SERVICE_ROUTING","targetService":"TARGET_SERVICE"}',
    '{"type":"object","properties":{"targetService":{"type":"string"},"targetSubService":{"type":"string"}}}',
    TRUE, 'SYSTEM'
),
(
    'default', 'Conditional Branch', 'Execute different steps based on condition',
    'CONDITIONAL_BRANCH',
    '{"condition":{"field":"fileSize","operator":"GT","value":1048576},"trueBranch":[{"type":"LOG_EVENT","parameters":{"message":"Large file detected"}}],"falseBranch":[{"type":"LOG_EVENT","parameters":{"message":"Regular file detected"}}]}',
    '{"type":"object","properties":{"condition":{"type":"object"},"trueBranch":{"type":"array"},"falseBranch":{"type":"array"}}}',
    TRUE, 'SYSTEM'
);

-- Create workflow_statistics table for analytics
CREATE TABLE workflow_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    workflow_definition_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    executions_started BIGINT DEFAULT 0,
    executions_completed BIGINT DEFAULT 0,
    executions_failed BIGINT DEFAULT 0,
    executions_timeout BIGINT DEFAULT 0,
    executions_cancelled BIGINT DEFAULT 0,
    avg_duration_minutes DOUBLE DEFAULT 0,
    avg_steps_completed DOUBLE DEFAULT 0,
    files_processed BIGINT DEFAULT 0,
    approval_requests BIGINT DEFAULT 0,
    notifications_sent BIGINT DEFAULT 0,
    errors_recovered BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_workflow_statistics (tenant_id, workflow_definition_id, stat_date),
    INDEX idx_workflow_statistics_tenant_date (tenant_id, stat_date),
    INDEX idx_workflow_statistics_workflow_date (workflow_definition_id, stat_date)
);

-- Create priority_processing_queues table
CREATE TABLE priority_processing_queues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    queue_name VARCHAR(100) NOT NULL,
    priority_level INT NOT NULL, -- 1-100, higher = more priority
    max_concurrent_processing INT DEFAULT 5,
    current_processing_count INT DEFAULT 0,
    queue_description VARCHAR(500),
    processing_rules TEXT, -- JSON rules for queue assignment
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_priority_queues (tenant_id, queue_name),
    INDEX idx_priority_queues_tenant (tenant_id),
    INDEX idx_priority_queues_priority (tenant_id, priority_level DESC),
    INDEX idx_priority_queues_active (tenant_id, is_active)
);

-- Insert default priority queues
INSERT INTO priority_processing_queues (
    tenant_id, queue_name, priority_level, max_concurrent_processing, 
    queue_description, processing_rules, created_by
) VALUES 
(
    'default', 'CRITICAL', 100, 10, 'Critical files requiring immediate processing',
    '{"conditions":[{"field":"serviceName","operator":"CONTAINS","value":"CRITICAL"},{"field":"fileSize","operator":"GT","value":104857600}]}', 'SYSTEM'
),
(
    'default', 'HIGH', 80, 8, 'High priority files',
    '{"conditions":[{"field":"serviceName","operator":"CONTAINS","value":"CUSTOMER"},{"field":"processingPriority","operator":"GTE","value":80}]}', 'SYSTEM'
),
(
    'default', 'NORMAL', 50, 5, 'Normal priority files',
    '{"conditions":[{"field":"processingPriority","operator":"BETWEEN","value":[30,79]}]}', 'SYSTEM'
),
(
    'default', 'LOW', 20, 3, 'Low priority files',
    '{"conditions":[{"field":"processingPriority","operator":"LT","value":30}]}', 'SYSTEM'
),
(
    'default', 'BATCH', 10, 2, 'Batch processing queue for non-urgent files',
    '{"conditions":[{"field":"direction","operator":"EQUALS","value":"OUTBOUND"},{"field":"fileSize","operator":"LT","value":1048576}]}', 'SYSTEM'
);

-- Create stored procedure for workflow statistics updates
DELIMITER //
CREATE PROCEDURE UpdateWorkflowStatistics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_workflow_id BIGINT;
    
    DECLARE workflow_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, workflow_definition_id
        FROM workflow_executions 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND DATE(created_at) = target_date;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN workflow_cursor;
    
    read_loop: LOOP
        FETCH workflow_cursor INTO v_tenant_id, v_workflow_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO workflow_statistics (
            tenant_id, workflow_definition_id, stat_date,
            executions_started, executions_completed, executions_failed,
            executions_timeout, executions_cancelled, avg_duration_minutes,
            avg_steps_completed, files_processed, approval_requests
        )
        SELECT 
            v_tenant_id,
            v_workflow_id,
            target_date,
            COUNT(*) as executions_started,
            SUM(CASE WHEN execution_status = 'COMPLETED' THEN 1 ELSE 0 END) as executions_completed,
            SUM(CASE WHEN execution_status = 'FAILED' THEN 1 ELSE 0 END) as executions_failed,
            SUM(CASE WHEN execution_status = 'TIMEOUT' THEN 1 ELSE 0 END) as executions_timeout,
            SUM(CASE WHEN execution_status = 'CANCELLED' THEN 1 ELSE 0 END) as executions_cancelled,
            AVG(COALESCE(duration_minutes, 0)) as avg_duration_minutes,
            AVG(COALESCE(current_step, 0)) as avg_steps_completed,
            COUNT(DISTINCT file_transfer_id) as files_processed,
            SUM(CASE WHEN execution_status = 'WAITING_FOR_APPROVAL' THEN 1 ELSE 0 END) as approval_requests
        FROM workflow_executions 
        WHERE tenant_id = v_tenant_id 
        AND workflow_definition_id = v_workflow_id 
        AND DATE(created_at) = target_date
        ON DUPLICATE KEY UPDATE
            executions_started = VALUES(executions_started),
            executions_completed = VALUES(executions_completed),
            executions_failed = VALUES(executions_failed),
            executions_timeout = VALUES(executions_timeout),
            executions_cancelled = VALUES(executions_cancelled),
            avg_duration_minutes = VALUES(avg_duration_minutes),
            avg_steps_completed = VALUES(avg_steps_completed),
            files_processed = VALUES(files_processed),
            approval_requests = VALUES(approval_requests),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE workflow_cursor;
END//
DELIMITER ;

-- Create triggers for workflow execution tracking
DELIMITER //
CREATE TRIGGER tr_workflow_execution_update
    AFTER UPDATE ON workflow_executions
    FOR EACH ROW
BEGIN
    -- Update file transfer workflow status
    IF OLD.execution_status != NEW.execution_status THEN
        UPDATE file_transfer_records 
        SET workflow_status = CASE 
            WHEN NEW.execution_status = 'COMPLETED' THEN 'COMPLETED'
            WHEN NEW.execution_status = 'FAILED' THEN 'FAILED'
            WHEN NEW.execution_status IN ('RUNNING', 'WAITING_FOR_APPROVAL', 'WAITING_FOR_EVENT') THEN 'RUNNING'
            ELSE 'PENDING'
        END,
        last_workflow_execution_at = NEW.updated_at,
        requires_approval = CASE WHEN NEW.execution_status = 'WAITING_FOR_APPROVAL' THEN TRUE ELSE requires_approval END
        WHERE id = NEW.file_transfer_id;
    END IF;
END//

CREATE TRIGGER tr_workflow_execution_insert
    AFTER INSERT ON workflow_executions
    FOR EACH ROW
BEGIN
    -- Update file transfer workflow counts
    UPDATE file_transfer_records 
    SET workflow_enabled = TRUE,
        active_workflows = active_workflows + 1,
        workflow_status = 'PENDING'
    WHERE id = NEW.file_transfer_id;
END//

CREATE TRIGGER tr_workflow_execution_complete
    AFTER UPDATE ON workflow_executions
    FOR EACH ROW
BEGIN
    -- Update workflow completion counts
    IF OLD.execution_status != 'COMPLETED' AND NEW.execution_status = 'COMPLETED' THEN
        UPDATE file_transfer_records 
        SET completed_workflows = completed_workflows + 1,
            active_workflows = GREATEST(0, active_workflows - 1)
        WHERE id = NEW.file_transfer_id;
    ELSEIF OLD.execution_status != 'FAILED' AND NEW.execution_status = 'FAILED' THEN
        UPDATE file_transfer_records 
        SET failed_workflows = failed_workflows + 1,
            active_workflows = GREATEST(0, active_workflows - 1)
        WHERE id = NEW.file_transfer_id;
    END IF;
END//
DELIMITER ;

-- Create events for workflow maintenance
CREATE EVENT evt_update_workflow_statistics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 8 HOUR
DO
    CALL UpdateWorkflowStatistics(CURDATE() - INTERVAL 1 DAY, NULL);

CREATE EVENT evt_cleanup_old_workflow_executions
ON SCHEDULE EVERY 1 WEEK
DO
    DELETE FROM workflow_executions 
    WHERE execution_status IN ('COMPLETED', 'FAILED', 'CANCELLED') 
    AND completed_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

CREATE EVENT evt_timeout_stale_workflows
ON SCHEDULE EVERY 1 HOUR
DO
    UPDATE workflow_executions 
    SET execution_status = 'TIMEOUT',
        completed_at = NOW(),
        error_details = 'Workflow timed out'
    WHERE execution_status IN ('RUNNING', 'WAITING_FOR_EVENT') 
    AND timeout_at <= NOW();

-- Create views for workflow monitoring
CREATE VIEW workflow_execution_summary AS
SELECT 
    we.tenant_id,
    wd.workflow_name,
    wd.workflow_type,
    we.execution_status,
    COUNT(*) as execution_count,
    AVG(we.duration_minutes) as avg_duration,
    MAX(we.created_at) as last_execution,
    SUM(CASE WHEN we.execution_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count,
    SUM(CASE WHEN we.execution_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count
FROM workflow_executions we
JOIN workflow_definitions wd ON we.workflow_definition_id = wd.id
GROUP BY we.tenant_id, wd.workflow_name, wd.workflow_type, we.execution_status;

CREATE VIEW active_workflow_dashboard AS
SELECT 
    we.tenant_id,
    COUNT(CASE WHEN we.execution_status = 'RUNNING' THEN 1 END) as running_workflows,
    COUNT(CASE WHEN we.execution_status = 'WAITING_FOR_APPROVAL' THEN 1 END) as waiting_approval,
    COUNT(CASE WHEN we.execution_status = 'WAITING_FOR_EVENT' THEN 1 END) as waiting_events,
    COUNT(CASE WHEN we.execution_status = 'WAITING_FOR_RETRY' THEN 1 END) as waiting_retry,
    COUNT(CASE WHEN we.execution_status = 'FAILED' THEN 1 END) as failed_today,
    AVG(we.current_step * 100.0 / NULLIF(we.total_steps, 0)) as avg_progress_percent
FROM workflow_executions we
WHERE DATE(we.created_at) = CURDATE()
GROUP BY we.tenant_id;

-- Add comments to tables and columns
ALTER TABLE business_rules COMMENT = 'Business rules for intelligent file processing automation';
ALTER TABLE workflow_definitions COMMENT = 'Workflow definitions for multi-step file processing';
ALTER TABLE workflow_executions COMMENT = 'Workflow execution instances and their current state';
ALTER TABLE workflow_step_templates COMMENT = 'Reusable workflow step templates';
ALTER TABLE workflow_execution_logs COMMENT = 'Detailed logs for workflow step execution';
ALTER TABLE priority_processing_queues COMMENT = 'Priority-based processing queues for file transfers';
ALTER TABLE workflow_statistics COMMENT = 'Daily statistics for workflow execution and performance';

-- Create indexes for performance
CREATE INDEX idx_business_rules_performance ON business_rules(tenant_id, is_active, rule_priority DESC, execution_order ASC);
CREATE INDEX idx_workflow_executions_monitoring ON workflow_executions(tenant_id, execution_status, priority DESC, created_at DESC);
CREATE INDEX idx_workflow_statistics_reporting ON workflow_statistics(tenant_id, stat_date DESC, executions_completed DESC);