-- Initialize database for file transfer application
CREATE DATABASE IF NOT EXISTS filetransfer;
USE filetransfer;

-- Create tenants table for multi-tenancy support
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_enabled (enabled)
);

-- Create file_transfer_records table
CREATE TABLE IF NOT EXISTS file_transfer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    sub_service_type VARCHAR(100) NULL,
    tenant_id VARCHAR(100) NOT NULL,
    source_path VARCHAR(500) NOT NULL,
    target_path VARCHAR(500) NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'WAITING_FOR_END_MARKER') NOT NULL,
    direction ENUM('INBOUND', 'OUTBOUND') NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    file_size BIGINT,
    checksum VARCHAR(255),
    batch_job_execution_id VARCHAR(255),
    INDEX idx_service_type (service_type),
    INDEX idx_sub_service_type (sub_service_type),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_direction (direction),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Create service_configurations table
CREATE TABLE IF NOT EXISTS service_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100) NULL,
    tenant_id VARCHAR(100) NOT NULL,
    inbound_path VARCHAR(500) NOT NULL,
    outbound_path VARCHAR(500) NOT NULL,
    start_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'SOT_',
    end_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'EOT_',
    data_file_pattern VARCHAR(100) NOT NULL DEFAULT '*.*',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries INTEGER NOT NULL DEFAULT 3,
    poll_interval_seconds INTEGER NOT NULL DEFAULT 30,
    cut_off_time TIME NOT NULL DEFAULT '23:59:59',
    sot_file_validation_regex VARCHAR(1000),
    eot_file_validation_regex VARCHAR(1000),
    data_file_validation_regex VARCHAR(1000),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE KEY uk_service_subservice_tenant (service_name, sub_service_name, tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_enabled (enabled),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Create holidays table
CREATE TABLE IF NOT EXISTS holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE KEY uk_tenant_holiday_date (tenant_id, holiday_date),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_holiday_date (holiday_date),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Create alert_configurations table
CREATE TABLE IF NOT EXISTS alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type ENUM('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED') NOT NULL,
    alert_duration_minutes INTEGER NOT NULL DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_recipients TEXT,
    notification_channels JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_alert_type (alert_type),
    INDEX idx_enabled (enabled),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Create alert_history table
CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type ENUM('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED') NOT NULL,
    alert_message TEXT NOT NULL,
    alert_level ENUM('INFO', 'WARNING', 'ERROR', 'CRITICAL') NOT NULL DEFAULT 'WARNING',
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL,
    acknowledged_by VARCHAR(100),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_alert_type (alert_type),
    INDEX idx_sent_at (sent_at),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Create sso_configurations table
CREATE TABLE IF NOT EXISTS sso_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id VARCHAR(100) NOT NULL UNIQUE,
    organization_name VARCHAR(255) NOT NULL,
    provider ENUM('AZURE_AD', 'GOOGLE', 'OKTA', 'KEYCLOAK', 'CUSTOM_OIDC', 'SAML2') NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    issuer_uri VARCHAR(500),
    authorization_uri VARCHAR(500),
    token_uri VARCHAR(500),
    user_info_uri VARCHAR(500),
    jwk_set_uri VARCHAR(500),
    redirect_uri VARCHAR(2000),
    scopes VARCHAR(1000) DEFAULT 'openid,profile,email',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    logo_url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_organization_id (organization_id),
    INDEX idx_provider (provider),
    INDEX idx_enabled (enabled)
);

-- Create sso_attributes_mapping table
CREATE TABLE IF NOT EXISTS sso_attributes_mapping (
    sso_config_id BIGINT NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    mapped_field VARCHAR(100) NOT NULL,
    PRIMARY KEY (sso_config_id, attribute_name),
    FOREIGN KEY (sso_config_id) REFERENCES sso_configurations(id) ON DELETE CASCADE
);

-- Insert default tenant
INSERT INTO tenants (tenant_id, tenant_name, timezone, created_by) 
VALUES ('default', 'Default Tenant', 'UTC', 'system');

-- Insert sample service configurations with sub-services
INSERT INTO service_configurations 
(service_name, sub_service_name, tenant_id, inbound_path, outbound_path, start_marker_prefix, end_marker_prefix, data_file_pattern, cut_off_time, description, created_by) 
VALUES 
('service1', NULL, 'default', '/app/data/inbound/service1', '/app/data/outbound/service1', 'SOT_', 'EOT_', '*.dat', '18:00:00', 'Primary data service for DAT files', 'system'),
('service1', 'subservice1', 'default', '/app/data/inbound/service1/subservice1', '/app/data/outbound/service1/subservice1', 'SOT_', 'EOT_', '*.dat', '17:00:00', 'Sub-service 1 for service1', 'system'),
('service1', 'subservice2', 'default', '/app/data/inbound/service1/subservice2', '/app/data/outbound/service1/subservice2', 'SOT_', 'EOT_', '*.dat', '16:00:00', 'Sub-service 2 for service1', 'system'),
('service2', NULL, 'default', '/app/data/inbound/service2', '/app/data/outbound/service2', 'START_', 'END_', '*.xml', '19:00:00', 'XML configuration service', 'system');

-- Insert sample holidays
INSERT INTO holidays (tenant_id, holiday_date, holiday_name, description, created_by)
VALUES 
('default', '2024-01-01', 'New Year Day', 'New Year Day holiday', 'system'),
('default', '2024-12-25', 'Christmas Day', 'Christmas Day holiday', 'system'),
('default', '2024-07-04', 'Independence Day', 'Independence Day holiday', 'system');

-- Insert sample alert configurations
INSERT INTO alert_configurations 
(tenant_id, service_name, sub_service_name, alert_type, alert_duration_minutes, email_recipients, created_by)
VALUES 
('default', 'service1', NULL, 'CUT_OFF_MISSED', 30, 'admin@company.com,ops@company.com', 'system'),
('default', 'service1', 'subservice1', 'CUT_OFF_MISSED', 15, 'subservice1-admin@company.com', 'system'),
('default', 'service1', 'subservice2', 'CUT_OFF_MISSED', 20, 'subservice2-admin@company.com', 'system'),
('default', 'service2', NULL, 'CUT_OFF_MISSED', 45, 'service2-admin@company.com', 'system');

-- Insert sample data for testing
INSERT INTO file_transfer_records 
(file_name, service_type, sub_service_type, tenant_id, source_path, target_path, status, direction, file_size, created_at, processed_at) 
VALUES 
('test_data_001.dat', 'service1', NULL, 'default', '/app/data/inbound/service1/test_data_001.dat', '/app/data/outbound/service1/test_data_001.dat', 'COMPLETED', 'INBOUND', 1024, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
('test_data_002.dat', 'service1', 'subservice1', 'default', '/app/data/inbound/service1/subservice1/test_data_002.dat', '/app/data/outbound/service1/subservice1/test_data_002.dat', 'COMPLETED', 'INBOUND', 2048, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
('test_config.xml', 'service2', NULL, 'default', '/app/data/inbound/service2/test_config.xml', '/app/data/outbound/service2/test_config.xml', 'PENDING', 'INBOUND', 512, NOW() - INTERVAL 4 HOUR, NULL),
('failed_transfer.dat', 'service1', 'subservice2', 'default', '/app/data/inbound/service1/subservice2/failed_transfer.dat', '/app/data/outbound/service1/subservice2/failed_transfer.dat', 'FAILED', 'INBOUND', 4096, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR),
('large_file.xml', 'service2', NULL, 'default', '/app/data/inbound/service2/large_file.xml', '/app/data/outbound/service2/large_file.xml', 'IN_PROGRESS', 'INBOUND', 1048576, NOW() - INTERVAL 1 HOUR, NULL);

-- Insert sample SSO configuration
INSERT INTO sso_configurations 
(organization_id, organization_name, provider, client_id, client_secret, issuer_uri, scopes, description, created_by)
VALUES 
('demo-org', 'Demo Organization', 'AZURE_AD', 'demo-client-id', 'demo-client-secret', 'https://login.microsoftonline.com/demo-tenant-id/v2.0', 'openid,profile,email', 'Demo Azure AD configuration', 'system');

-- Insert sample attribute mapping
INSERT INTO sso_attributes_mapping (sso_config_id, attribute_name, mapped_field)
VALUES 
(1, 'email', 'email'),
(1, 'name', 'name'),
(1, 'firstName', 'given_name'),
(1, 'lastName', 'family_name');