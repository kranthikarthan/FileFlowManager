-- Migration to add HSM (Hardware Security Module) support for SQL Server
-- Version: V102
-- Description: Add HSM integration for file integrity validation and cryptographic operations

-- Add HSM fields to service_configurations table
ALTER TABLE service_configurations 
ADD hsm_validation_required BIT DEFAULT 0,
    hsm_provider NVARCHAR(50) DEFAULT 'NONE' CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    hsm_operation_outbound NVARCHAR(20) DEFAULT 'SIGN' CHECK (hsm_operation_outbound IN ('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION')),
    hsm_operation_inbound NVARCHAR(20) DEFAULT 'VERIFY' CHECK (hsm_operation_inbound IN ('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION')),
    hsm_key_alias NVARCHAR(255),
    hsm_algorithm NVARCHAR(100) DEFAULT 'SHA256withRSA',
    hsm_timeout_seconds INT DEFAULT 30,
    hsm_retry_attempts INT DEFAULT 3,
    hsm_fail_on_error BIT DEFAULT 1,
    hsm_certificate_path NVARCHAR(500),
    hsm_config_properties NTEXT;

-- Add indexes for HSM-related queries
CREATE INDEX idx_service_config_hsm_required ON service_configurations(hsm_validation_required);
CREATE INDEX idx_service_config_hsm_provider ON service_configurations(hsm_provider);
CREATE INDEX idx_service_config_tenant_hsm ON service_configurations(tenant_id, hsm_validation_required);

-- Create hsm_validation_records table
CREATE TABLE hsm_validation_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    file_transfer_id BIGINT NOT NULL,
    file_name NVARCHAR(255) NOT NULL,
    hsm_provider NVARCHAR(50) NOT NULL CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    operation NVARCHAR(20) NOT NULL CHECK (operation IN ('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION')),
    status NVARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED', 'ERROR', 'SKIPPED', 'TIMEOUT', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS')),
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    hsm_key_id NVARCHAR(255),
    hsm_key_alias NVARCHAR(255),
    algorithm NVARCHAR(100),
    signature_value NTEXT,
    original_hash NVARCHAR(255),
    hsm_hash NVARCHAR(255),
    validation_data NTEXT,
    error_message NTEXT,
    error_code NVARCHAR(50),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    started_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    processing_time_ms BIGINT,
    hsm_session_id NVARCHAR(255),
    certificate_serial NVARCHAR(255),
    key_version INT,
    metadata NTEXT,
    
    CONSTRAINT fk_hsm_validation_file_transfer 
        FOREIGN KEY (file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for hsm_validation_records
CREATE INDEX idx_hsm_validation_file_transfer_id ON hsm_validation_records(file_transfer_id);
CREATE INDEX idx_hsm_validation_tenant_id ON hsm_validation_records(tenant_id);
CREATE INDEX idx_hsm_validation_status ON hsm_validation_records(status);
CREATE INDEX idx_hsm_validation_provider ON hsm_validation_records(hsm_provider);
CREATE INDEX idx_hsm_validation_operation ON hsm_validation_records(operation);
CREATE INDEX idx_hsm_validation_created_at ON hsm_validation_records(created_at);
CREATE INDEX idx_hsm_validation_tenant_service ON hsm_validation_records(tenant_id, service_name);
CREATE INDEX idx_hsm_validation_tenant_status ON hsm_validation_records(tenant_id, status);

-- Create hsm_configuration table for global HSM settings
CREATE TABLE hsm_configuration (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hsm_provider NVARCHAR(50) NOT NULL CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    provider_name NVARCHAR(100) NOT NULL,
    provider_config NTEXT,
    connection_string NVARCHAR(500),
    keystore_path NVARCHAR(500),
    keystore_password_encrypted NVARCHAR(255),
    default_key_alias NVARCHAR(255),
    default_algorithm NVARCHAR(100) DEFAULT 'SHA256withRSA',
    timeout_seconds INT DEFAULT 30,
    max_concurrent_operations INT DEFAULT 10,
    health_check_interval_seconds INT DEFAULT 300,
    enabled BIT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes for hsm_configuration
ALTER TABLE hsm_configuration 
ADD CONSTRAINT uk_hsm_config_provider UNIQUE (hsm_provider, provider_name);

CREATE INDEX idx_hsm_config_enabled ON hsm_configuration(enabled);
CREATE INDEX idx_hsm_config_provider ON hsm_configuration(hsm_provider);

-- Create hsm_key_management table for key lifecycle management
CREATE TABLE hsm_key_management (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    hsm_provider NVARCHAR(50) NOT NULL CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    key_alias NVARCHAR(255) NOT NULL,
    key_id NVARCHAR(255),
    key_type NVARCHAR(50) NOT NULL,
    key_algorithm NVARCHAR(100) NOT NULL,
    key_size INT,
    key_usage NVARCHAR(100),
    certificate_serial NVARCHAR(255),
    certificate_subject NVARCHAR(500),
    certificate_issuer NVARCHAR(500),
    key_created_date DATETIME2,
    key_expiry_date DATETIME2,
    key_status NVARCHAR(20) DEFAULT 'ACTIVE' CHECK (key_status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'SUSPENDED')),
    rotation_required BIT DEFAULT 0,
    last_used_at DATETIME2,
    usage_count BIGINT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes for hsm_key_management
ALTER TABLE hsm_key_management 
ADD CONSTRAINT uk_hsm_key_tenant_service_alias UNIQUE (tenant_id, service_name, sub_service_name, key_alias);

CREATE INDEX idx_hsm_key_tenant ON hsm_key_management(tenant_id);
CREATE INDEX idx_hsm_key_provider ON hsm_key_management(hsm_provider);
CREATE INDEX idx_hsm_key_status ON hsm_key_management(key_status);
CREATE INDEX idx_hsm_key_expiry ON hsm_key_management(key_expiry_date);
CREATE INDEX idx_hsm_key_rotation ON hsm_key_management(rotation_required);

-- Create hsm_audit_log table for HSM operations audit trail
CREATE TABLE hsm_audit_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hsm_validation_id BIGINT,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    operation NVARCHAR(20) NOT NULL CHECK (operation IN ('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION', 'KEY_ACCESS', 'CONFIG_CHANGE')),
    hsm_provider NVARCHAR(50) NOT NULL CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    key_alias NVARCHAR(255),
    algorithm NVARCHAR(100),
    operation_result NVARCHAR(20) NOT NULL CHECK (operation_result IN ('SUCCESS', 'FAILURE', 'TIMEOUT', 'ERROR')),
    error_code NVARCHAR(50),
    error_message NTEXT,
    processing_time_ms BIGINT,
    user_id NVARCHAR(100),
    client_ip NVARCHAR(45),
    session_id NVARCHAR(255),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT fk_hsm_audit_validation 
        FOREIGN KEY (hsm_validation_id) 
        REFERENCES hsm_validation_records(id) 
        ON DELETE SET NULL
);

-- Add indexes for hsm_audit_log
CREATE INDEX idx_hsm_audit_validation_id ON hsm_audit_log(hsm_validation_id);
CREATE INDEX idx_hsm_audit_tenant ON hsm_audit_log(tenant_id);
CREATE INDEX idx_hsm_audit_operation ON hsm_audit_log(operation);
CREATE INDEX idx_hsm_audit_result ON hsm_audit_log(operation_result);
CREATE INDEX idx_hsm_audit_created_at ON hsm_audit_log(created_at);
CREATE INDEX idx_hsm_audit_tenant_operation ON hsm_audit_log(tenant_id, operation);

-- Create hsm_performance_metrics table for HSM performance tracking
CREATE TABLE hsm_performance_metrics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    hsm_provider NVARCHAR(50) NOT NULL CHECK (hsm_provider IN ('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS')),
    operation NVARCHAR(20) NOT NULL CHECK (operation IN ('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION')),
    metric_date DATE NOT NULL,
    total_operations BIGINT DEFAULT 0,
    successful_operations BIGINT DEFAULT 0,
    failed_operations BIGINT DEFAULT 0,
    avg_processing_time_ms DECIMAL(10,2) DEFAULT 0,
    max_processing_time_ms BIGINT DEFAULT 0,
    min_processing_time_ms BIGINT DEFAULT 0,
    total_processing_time_ms BIGINT DEFAULT 0,
    timeout_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes for hsm_performance_metrics
ALTER TABLE hsm_performance_metrics 
ADD CONSTRAINT uk_hsm_metrics UNIQUE (tenant_id, hsm_provider, operation, metric_date);

CREATE INDEX idx_hsm_metrics_tenant_date ON hsm_performance_metrics(tenant_id, metric_date);
CREATE INDEX idx_hsm_metrics_provider_date ON hsm_performance_metrics(hsm_provider, metric_date);
CREATE INDEX idx_hsm_metrics_operation_date ON hsm_performance_metrics(operation, metric_date);

-- Create triggers for updating updated_at columns
CREATE TRIGGER tr_hsm_config_update
    ON hsm_configuration
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE hsm_configuration 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

CREATE TRIGGER tr_hsm_key_mgmt_update
    ON hsm_key_management
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE hsm_key_management 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

CREATE TRIGGER tr_hsm_perf_metrics_update
    ON hsm_performance_metrics
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE hsm_performance_metrics 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Create stored procedure to update HSM performance metrics for SQL Server
CREATE PROCEDURE UpdateHsmPerformanceMetrics
    @target_date DATE = NULL,
    @target_tenant_id NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @target_date IS NULL
        SET @target_date = CAST(GETDATE() - 1 AS DATE);
    
    -- Update HSM performance metrics
    MERGE hsm_performance_metrics AS target
    USING (
        SELECT 
            tenant_id,
            hsm_provider,
            operation,
            @target_date as metric_date,
            COUNT(*) as total_operations,
            SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END) as successful_operations,
            SUM(CASE WHEN status IN ('FAILED', 'ERROR', 'TIMEOUT', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS') THEN 1 ELSE 0 END) as failed_operations,
            AVG(CASE WHEN processing_time_ms IS NOT NULL THEN CAST(processing_time_ms AS DECIMAL(10,2)) END) as avg_processing_time_ms,
            MAX(processing_time_ms) as max_processing_time_ms,
            MIN(processing_time_ms) as min_processing_time_ms,
            SUM(CASE WHEN processing_time_ms IS NOT NULL THEN processing_time_ms ELSE 0 END) as total_processing_time_ms,
            SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout_count,
            SUM(CASE WHEN status IN ('ERROR', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS') THEN 1 ELSE 0 END) as error_count
        FROM hsm_validation_records 
        WHERE (@target_tenant_id IS NULL OR tenant_id = @target_tenant_id)
        AND CAST(created_at AS DATE) = @target_date
        AND hsm_provider != 'NONE'
        GROUP BY tenant_id, hsm_provider, operation
    ) AS source
    ON (target.tenant_id = source.tenant_id 
        AND target.hsm_provider = source.hsm_provider 
        AND target.operation = source.operation
        AND target.metric_date = source.metric_date)
    WHEN MATCHED THEN
        UPDATE SET
            total_operations = source.total_operations,
            successful_operations = source.successful_operations,
            failed_operations = source.failed_operations,
            avg_processing_time_ms = source.avg_processing_time_ms,
            max_processing_time_ms = source.max_processing_time_ms,
            min_processing_time_ms = source.min_processing_time_ms,
            total_processing_time_ms = source.total_processing_time_ms,
            timeout_count = source.timeout_count,
            error_count = source.error_count,
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (tenant_id, hsm_provider, operation, metric_date, total_operations, 
                successful_operations, failed_operations, avg_processing_time_ms,
                max_processing_time_ms, min_processing_time_ms, total_processing_time_ms,
                timeout_count, error_count)
        VALUES (source.tenant_id, source.hsm_provider, source.operation, source.metric_date,
                source.total_operations, source.successful_operations, source.failed_operations,
                source.avg_processing_time_ms, source.max_processing_time_ms, source.min_processing_time_ms,
                source.total_processing_time_ms, source.timeout_count, source.error_count);
END;

-- Insert default HSM configurations for supported providers
INSERT INTO hsm_configuration (hsm_provider, provider_name, enabled, provider_config) VALUES
('AZURE_KEY_VAULT', 'Azure Key Vault HSM', 0, '{"vaultUrl":"","clientId":"","clientSecret":"","tenantId":""}'),
('AWS_CLOUD_HSM', 'AWS CloudHSM', 0, '{"clusterId":"","region":"","accessKey":"","secretKey":""}'),
('THALES_LUNA', 'Thales Luna Network HSM', 0, '{"serverHost":"","serverPort":"1792","clientCert":"","clientKey":""}');

-- Add additional indexes for performance
CREATE INDEX idx_hsm_validation_tenant_provider_status ON hsm_validation_records(tenant_id, hsm_provider, status);
CREATE INDEX idx_hsm_validation_timeout_check ON hsm_validation_records(status, started_at) WHERE status = 'IN_PROGRESS';