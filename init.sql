-- Azure SQL MI compatible schema

CREATE DATABASE filetransfer;
GO
USE filetransfer;
GO

-- Tenants table
CREATE TABLE tenants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
GO

-- File transfer records
CREATE TABLE file_transfer_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    sub_service_type VARCHAR(100) NULL,
    tenant_id VARCHAR(100) NOT NULL,
    source_path VARCHAR(500) NOT NULL,
    target_path VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'WAITING_FOR_END_MARKER')),
    direction VARCHAR(16) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    error_message NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    processed_at DATETIME2 NULL,
    file_size BIGINT,
    checksum VARCHAR(255),
    batch_job_execution_id VARCHAR(255),
    INDEX idx_service_type (service_type),
    INDEX idx_sub_service_type (sub_service_type),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_direction (direction),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
GO

-- Service configurations
CREATE TABLE service_configurations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100) NULL,
    tenant_id VARCHAR(100) NOT NULL,
    inbound_path VARCHAR(500) NOT NULL,
    outbound_path VARCHAR(500) NOT NULL,
    start_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'SOT_',
    end_marker_prefix VARCHAR(50) NOT NULL DEFAULT 'EOT_',
    data_file_pattern VARCHAR(100) NOT NULL DEFAULT '*.*',
    enabled BIT NOT NULL DEFAULT 1,
    max_retries INT NOT NULL DEFAULT 3,
    poll_interval_seconds INT NOT NULL DEFAULT 30,
    cut_off_time TIME NOT NULL DEFAULT '23:59:59',
    sot_file_validation_regex VARCHAR(1000),
    eot_file_validation_regex VARCHAR(1000),
    data_file_validation_regex VARCHAR(1000),
    description NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_service_subservice_tenant UNIQUE (service_name, sub_service_name, tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_enabled (enabled),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
GO

-- Holidays
CREATE TABLE holidays (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_tenant_holiday_date UNIQUE (tenant_id, holiday_date),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_holiday_date (holiday_date),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
GO

-- Alert configurations
CREATE TABLE alert_configurations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type VARCHAR(32) NOT NULL CHECK (alert_type IN ('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED')),
    alert_duration_minutes INT NOT NULL DEFAULT 60,
    enabled BIT NOT NULL DEFAULT 1,
    email_recipients NVARCHAR(MAX),
    notification_channels NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_alert_type (alert_type),
    INDEX idx_enabled (enabled),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
GO

-- Alert history
CREATE TABLE alert_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type VARCHAR(32) NOT NULL CHECK (alert_type IN ('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED')),
    alert_message NVARCHAR(MAX) NOT NULL,
    alert_level VARCHAR(16) NOT NULL CHECK (alert_level IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')) DEFAULT 'WARNING',
    sent_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    acknowledged_at DATETIME2 NULL,
    acknowledged_by VARCHAR(100),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_service_name (service_name),
    INDEX idx_sub_service_name (sub_service_name),
    INDEX idx_alert_type (alert_type),
    INDEX idx_sent_at (sent_at),
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);
GO

-- SSO configurations
CREATE TABLE sso_configurations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    organization_id VARCHAR(100) NOT NULL UNIQUE,
    organization_name VARCHAR(255) NOT NULL,
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('AZURE_AD', 'GOOGLE', 'OKTA', 'KEYCLOAK', 'CUSTOM_OIDC', 'SAML2')),
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    issuer_uri VARCHAR(500),
    authorization_uri VARCHAR(500),
    token_uri VARCHAR(500),
    user_info_uri VARCHAR(500),
    jwk_set_uri VARCHAR(500),
    redirect_uri VARCHAR(2000),
    scopes VARCHAR(1000) DEFAULT 'openid,profile,email',
    enabled BIT NOT NULL DEFAULT 1,
    logo_url VARCHAR(500),
    description NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
GO

-- SSO attributes mapping
CREATE TABLE sso_attributes_mapping (
    sso_config_id BIGINT NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    mapped_field VARCHAR(100) NOT NULL,
    PRIMARY KEY (sso_config_id, attribute_name),
    FOREIGN KEY (sso_config_id) REFERENCES sso_configurations(id)
);
GO