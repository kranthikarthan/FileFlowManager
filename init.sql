-- Initialize database for file transfer application
CREATE DATABASE IF NOT EXISTS filetransfer;
USE filetransfer;

-- Create file_transfer_records table
CREATE TABLE IF NOT EXISTS file_transfer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
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
    INDEX idx_status (status),
    INDEX idx_direction (direction),
    INDEX idx_created_at (created_at)
);

-- Create service_configurations table
CREATE TABLE IF NOT EXISTS service_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    inbound_path VARCHAR(500) NOT NULL,
    outbound_path VARCHAR(500) NOT NULL,
    start_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'SOT_',
    end_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'EOT_',
    data_file_pattern VARCHAR(100) NOT NULL DEFAULT '*.*',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries INTEGER NOT NULL DEFAULT 3,
    poll_interval_seconds INTEGER NOT NULL DEFAULT 30,
    sot_file_validation_regex VARCHAR(1000),
    eot_file_validation_regex VARCHAR(1000),
    data_file_validation_regex VARCHAR(1000),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_service_name (service_name),
    INDEX idx_enabled (enabled)
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

-- Insert sample service configurations
INSERT INTO service_configurations 
(service_name, inbound_path, outbound_path, start_marker_prefix, end_marker_prefix, data_file_pattern, description, created_by) 
VALUES 
('service1', '/app/data/inbound/service1', '/app/data/outbound/service1', 'SOT_', 'EOT_', '*.dat', 'Primary data service for DAT files', 'system'),
('service2', '/app/data/inbound/service2', '/app/data/outbound/service2', 'START_', 'END_', '*.xml', 'XML configuration service', 'system');

-- Insert sample data for testing
INSERT INTO file_transfer_records 
(file_name, service_type, source_path, target_path, status, direction, file_size, created_at, processed_at) 
VALUES 
('test_data_001.dat', 'service1', '/app/data/inbound/service1/test_data_001.dat', '/app/data/outbound/service1/test_data_001.dat', 'COMPLETED', 'INBOUND', 1024, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
('test_data_002.dat', 'service1', '/app/data/inbound/service1/test_data_002.dat', '/app/data/outbound/service1/test_data_002.dat', 'COMPLETED', 'INBOUND', 2048, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
('test_config.xml', 'service2', '/app/data/inbound/service2/test_config.xml', '/app/data/outbound/service2/test_config.xml', 'PENDING', 'INBOUND', 512, NOW() - INTERVAL 4 HOUR, NULL),
('failed_transfer.dat', 'service1', '/app/data/inbound/service1/failed_transfer.dat', '/app/data/outbound/service1/failed_transfer.dat', 'FAILED', 'INBOUND', 4096, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR),
('large_file.xml', 'service2', '/app/data/inbound/service2/large_file.xml', '/app/data/outbound/service2/large_file.xml', 'IN_PROGRESS', 'INBOUND', 1048576, NOW() - INTERVAL 1 HOUR, NULL);

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