-- Migration to add HSM (Hardware Security Module) support
-- Version: V102
-- Description: Add HSM integration for file integrity validation and cryptographic operations

-- Add HSM fields to service_configurations table
ALTER TABLE service_configurations 
ADD COLUMN hsm_validation_required BOOLEAN DEFAULT FALSE,
ADD COLUMN hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') DEFAULT 'NONE',
ADD COLUMN hsm_operation_outbound ENUM('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION') DEFAULT 'SIGN',
ADD COLUMN hsm_operation_inbound ENUM('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION') DEFAULT 'VERIFY',
ADD COLUMN hsm_key_alias VARCHAR(255),
ADD COLUMN hsm_algorithm VARCHAR(100) DEFAULT 'SHA256withRSA',
ADD COLUMN hsm_timeout_seconds INT DEFAULT 30,
ADD COLUMN hsm_retry_attempts INT DEFAULT 3,
ADD COLUMN hsm_fail_on_error BOOLEAN DEFAULT TRUE,
ADD COLUMN hsm_certificate_path VARCHAR(500),
ADD COLUMN hsm_config_properties TEXT;

-- Add indexes for HSM-related queries
CREATE INDEX idx_service_config_hsm_required ON service_configurations(hsm_validation_required);
CREATE INDEX idx_service_config_hsm_provider ON service_configurations(hsm_provider);
CREATE INDEX idx_service_config_tenant_hsm ON service_configurations(tenant_id, hsm_validation_required);

-- Create hsm_validation_records table
CREATE TABLE hsm_validation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_transfer_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') NOT NULL,
    operation ENUM('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION') NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED', 'ERROR', 'SKIPPED', 'TIMEOUT', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS') NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    hsm_key_id VARCHAR(255),
    hsm_key_alias VARCHAR(255),
    algorithm VARCHAR(100),
    signature_value TEXT,
    original_hash VARCHAR(255),
    hsm_hash VARCHAR(255),
    validation_data TEXT,
    error_message TEXT,
    error_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    processing_time_ms BIGINT,
    hsm_session_id VARCHAR(255),
    certificate_serial VARCHAR(255),
    key_version INT,
    metadata TEXT,
    
    INDEX idx_hsm_validation_file_transfer_id (file_transfer_id),
    INDEX idx_hsm_validation_tenant_id (tenant_id),
    INDEX idx_hsm_validation_status (status),
    INDEX idx_hsm_validation_provider (hsm_provider),
    INDEX idx_hsm_validation_operation (operation),
    INDEX idx_hsm_validation_created_at (created_at),
    INDEX idx_hsm_validation_tenant_service (tenant_id, service_name),
    INDEX idx_hsm_validation_tenant_status (tenant_id, status),
    
    CONSTRAINT fk_hsm_validation_file_transfer 
        FOREIGN KEY (file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Create hsm_configuration table for global HSM settings
CREATE TABLE hsm_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    provider_config TEXT,
    connection_string VARCHAR(500),
    keystore_path VARCHAR(500),
    keystore_password_encrypted VARCHAR(255),
    default_key_alias VARCHAR(255),
    default_algorithm VARCHAR(100) DEFAULT 'SHA256withRSA',
    timeout_seconds INT DEFAULT 30,
    max_concurrent_operations INT DEFAULT 10,
    health_check_interval_seconds INT DEFAULT 300,
    enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_hsm_config_provider (hsm_provider, provider_name),
    INDEX idx_hsm_config_enabled (enabled),
    INDEX idx_hsm_config_provider (hsm_provider)
);

-- Create hsm_key_management table for key lifecycle management
CREATE TABLE hsm_key_management (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') NOT NULL,
    key_alias VARCHAR(255) NOT NULL,
    key_id VARCHAR(255),
    key_type VARCHAR(50) NOT NULL,
    key_algorithm VARCHAR(100) NOT NULL,
    key_size INT,
    key_usage VARCHAR(100),
    certificate_serial VARCHAR(255),
    certificate_subject VARCHAR(500),
    certificate_issuer VARCHAR(500),
    key_created_date TIMESTAMP,
    key_expiry_date TIMESTAMP,
    key_status ENUM('ACTIVE', 'EXPIRED', 'REVOKED', 'SUSPENDED') DEFAULT 'ACTIVE',
    rotation_required BOOLEAN DEFAULT FALSE,
    last_used_at TIMESTAMP,
    usage_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_hsm_key_tenant_service_alias (tenant_id, service_name, sub_service_name, key_alias),
    INDEX idx_hsm_key_tenant (tenant_id),
    INDEX idx_hsm_key_provider (hsm_provider),
    INDEX idx_hsm_key_status (key_status),
    INDEX idx_hsm_key_expiry (key_expiry_date),
    INDEX idx_hsm_key_rotation (rotation_required)
);

-- Create hsm_audit_log table for HSM operations audit trail
CREATE TABLE hsm_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hsm_validation_id BIGINT,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    operation ENUM('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION', 'KEY_ACCESS', 'CONFIG_CHANGE') NOT NULL,
    hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') NOT NULL,
    key_alias VARCHAR(255),
    algorithm VARCHAR(100),
    operation_result ENUM('SUCCESS', 'FAILURE', 'TIMEOUT', 'ERROR') NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    processing_time_ms BIGINT,
    user_id VARCHAR(100),
    client_ip VARCHAR(45),
    session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_hsm_audit_validation_id (hsm_validation_id),
    INDEX idx_hsm_audit_tenant (tenant_id),
    INDEX idx_hsm_audit_operation (operation),
    INDEX idx_hsm_audit_result (operation_result),
    INDEX idx_hsm_audit_created_at (created_at),
    INDEX idx_hsm_audit_tenant_operation (tenant_id, operation),
    
    CONSTRAINT fk_hsm_audit_validation 
        FOREIGN KEY (hsm_validation_id) 
        REFERENCES hsm_validation_records(id) 
        ON DELETE SET NULL
);

-- Create hsm_performance_metrics table for HSM performance tracking
CREATE TABLE hsm_performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') NOT NULL,
    operation ENUM('SIGN', 'VERIFY', 'ENCRYPT', 'DECRYPT', 'HASH', 'MAC', 'KEY_GENERATION', 'KEY_DERIVATION') NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_hsm_metrics (tenant_id, hsm_provider, operation, metric_date),
    INDEX idx_hsm_metrics_tenant_date (tenant_id, metric_date),
    INDEX idx_hsm_metrics_provider_date (hsm_provider, metric_date),
    INDEX idx_hsm_metrics_operation_date (operation, metric_date)
);

-- Create view for HSM dashboard metrics
CREATE VIEW hsm_dashboard_metrics AS
SELECT 
    tenant_id,
    service_name,
    sub_service_name,
    hsm_provider,
    COUNT(*) as total_validations,
    SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END) as passed_validations,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_validations,
    SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) as error_validations,
    SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout_validations,
    SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END) as skipped_validations,
    AVG(CASE WHEN processing_time_ms IS NOT NULL THEN processing_time_ms END) as avg_processing_time_ms,
    MAX(processing_time_ms) as max_processing_time_ms,
    MIN(processing_time_ms) as min_processing_time_ms,
    MAX(created_at) as last_validation_at
FROM hsm_validation_records 
GROUP BY tenant_id, service_name, sub_service_name, hsm_provider;

-- Create stored procedure to update HSM performance metrics
DELIMITER //
CREATE PROCEDURE UpdateHsmPerformanceMetrics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_hsm_provider VARCHAR(50);
    DECLARE v_operation VARCHAR(50);
    
    DECLARE hsm_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, hsm_provider, operation
        FROM hsm_validation_records 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND DATE(created_at) = target_date
        AND hsm_provider != 'NONE';
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN hsm_cursor;
    
    read_loop: LOOP
        FETCH hsm_cursor INTO v_tenant_id, v_hsm_provider, v_operation;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO hsm_performance_metrics (
            tenant_id, hsm_provider, operation, metric_date,
            total_operations, successful_operations, failed_operations,
            avg_processing_time_ms, max_processing_time_ms, min_processing_time_ms,
            total_processing_time_ms, timeout_count, error_count
        )
        SELECT 
            v_tenant_id,
            v_hsm_provider,
            v_operation,
            target_date,
            COUNT(*) as total_operations,
            SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END) as successful_operations,
            SUM(CASE WHEN status IN ('FAILED', 'ERROR', 'TIMEOUT', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS') THEN 1 ELSE 0 END) as failed_operations,
            AVG(CASE WHEN processing_time_ms IS NOT NULL THEN processing_time_ms END) as avg_processing_time_ms,
            MAX(processing_time_ms) as max_processing_time_ms,
            MIN(processing_time_ms) as min_processing_time_ms,
            SUM(CASE WHEN processing_time_ms IS NOT NULL THEN processing_time_ms ELSE 0 END) as total_processing_time_ms,
            SUM(CASE WHEN status = 'TIMEOUT' THEN 1 ELSE 0 END) as timeout_count,
            SUM(CASE WHEN status IN ('ERROR', 'HSM_UNAVAILABLE', 'INVALID_KEY', 'INSUFFICIENT_PERMISSIONS') THEN 1 ELSE 0 END) as error_count
        FROM hsm_validation_records 
        WHERE tenant_id = v_tenant_id 
        AND hsm_provider = v_hsm_provider 
        AND operation = v_operation
        AND DATE(created_at) = target_date
        ON DUPLICATE KEY UPDATE
            total_operations = VALUES(total_operations),
            successful_operations = VALUES(successful_operations),
            failed_operations = VALUES(failed_operations),
            avg_processing_time_ms = VALUES(avg_processing_time_ms),
            max_processing_time_ms = VALUES(max_processing_time_ms),
            min_processing_time_ms = VALUES(min_processing_time_ms),
            total_processing_time_ms = VALUES(total_processing_time_ms),
            timeout_count = VALUES(timeout_count),
            error_count = VALUES(error_count),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE hsm_cursor;
END//
DELIMITER ;

-- Create event to automatically update HSM metrics daily
CREATE EVENT evt_update_hsm_metrics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 4 HOUR
DO
    CALL UpdateHsmPerformanceMetrics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Create triggers for HSM audit logging
DELIMITER //
CREATE TRIGGER tr_hsm_validation_audit_insert
    AFTER INSERT ON hsm_validation_records
    FOR EACH ROW
BEGIN
    INSERT INTO hsm_audit_log (
        hsm_validation_id, tenant_id, service_name, operation, 
        hsm_provider, key_alias, algorithm, operation_result
    )
    VALUES (
        NEW.id, NEW.tenant_id, NEW.service_name, NEW.operation,
        NEW.hsm_provider, NEW.hsm_key_alias, NEW.algorithm, 'SUCCESS'
    );
END//

CREATE TRIGGER tr_hsm_validation_audit_update
    AFTER UPDATE ON hsm_validation_records
    FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO hsm_audit_log (
            hsm_validation_id, tenant_id, service_name, operation,
            hsm_provider, key_alias, algorithm, operation_result,
            error_code, error_message, processing_time_ms
        )
        VALUES (
            NEW.id, NEW.tenant_id, NEW.service_name, NEW.operation,
            NEW.hsm_provider, NEW.hsm_key_alias, NEW.algorithm,
            CASE 
                WHEN NEW.status = 'PASSED' THEN 'SUCCESS'
                WHEN NEW.status = 'TIMEOUT' THEN 'TIMEOUT'
                ELSE 'FAILURE'
            END,
            NEW.error_code, NEW.error_message, NEW.processing_time_ms
        );
    END IF;
END//
DELIMITER ;

-- Insert default HSM configurations for supported providers
INSERT INTO hsm_configuration (hsm_provider, provider_name, enabled, provider_config) VALUES
('AZURE_KEY_VAULT', 'Azure Key Vault HSM', FALSE, '{"vaultUrl":"","clientId":"","clientSecret":"","tenantId":""}'),
('AWS_CLOUD_HSM', 'AWS CloudHSM', FALSE, '{"clusterId":"","region":"","accessKey":"","secretKey":""}'),
('THALES_LUNA', 'Thales Luna Network HSM', FALSE, '{"serverHost":"","serverPort":"1792","clientCert":"","clientKey":""}');

-- Add HSM-related indexes for performance
CREATE INDEX idx_hsm_validation_tenant_provider_status ON hsm_validation_records(tenant_id, hsm_provider, status);
CREATE INDEX idx_hsm_validation_processing_time ON hsm_validation_records(processing_time_ms) WHERE processing_time_ms IS NOT NULL;
CREATE INDEX idx_hsm_validation_timeout_check ON hsm_validation_records(status, started_at) WHERE status = 'IN_PROGRESS';

-- Add comments to new tables and columns
ALTER TABLE service_configurations 
MODIFY COLUMN hsm_validation_required BOOLEAN DEFAULT FALSE COMMENT 'Whether HSM validation is required for this service',
MODIFY COLUMN hsm_provider ENUM('NONE', 'THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'UTIMACO_CRYPTO_SERVER', 'GEMALTO_SAFE_NET', 'NCIPHER_NSHIELD', 'FORTANIX_DSM', 'SECUROSYS_PRIMUS') DEFAULT 'NONE' COMMENT 'HSM provider type for cryptographic operations',
MODIFY COLUMN hsm_key_alias VARCHAR(255) COMMENT 'HSM key alias for cryptographic operations',
MODIFY COLUMN hsm_algorithm VARCHAR(100) DEFAULT 'SHA256withRSA' COMMENT 'Cryptographic algorithm for HSM operations';

ALTER TABLE hsm_validation_records COMMENT = 'Records of HSM validation operations for file transfers';
ALTER TABLE hsm_configuration COMMENT = 'Global HSM provider configurations and settings';
ALTER TABLE hsm_key_management COMMENT = 'HSM key lifecycle management and metadata';
ALTER TABLE hsm_audit_log COMMENT = 'Comprehensive audit trail for all HSM operations';
ALTER TABLE hsm_performance_metrics COMMENT = 'Daily performance metrics for HSM operations';