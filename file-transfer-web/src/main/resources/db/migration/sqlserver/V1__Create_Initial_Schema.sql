-- SQL Server Migration Script V1
-- Initial schema creation for Azure SQL Managed Instance

-- Enable snapshot isolation for better concurrency
ALTER DATABASE filetransfer SET ALLOW_SNAPSHOT_ISOLATION ON;
ALTER DATABASE filetransfer SET READ_COMMITTED_SNAPSHOT ON;

-- Create schemas for organization
CREATE SCHEMA [tenant_data];
CREATE SCHEMA [file_processing];
CREATE SCHEMA [configuration];
CREATE SCHEMA [monitoring];
CREATE SCHEMA [security];

-- Tenants table
CREATE TABLE [dbo].[tenants] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL UNIQUE,
    [tenant_name] NVARCHAR(255) NOT NULL,
    [timezone] NVARCHAR(100) NOT NULL DEFAULT 'UTC',
    [enabled] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100)
);

-- Create index for tenant queries
CREATE NONCLUSTERED INDEX IX_tenants_tenant_id ON [dbo].[tenants] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_tenants_enabled ON [dbo].[tenants] ([enabled]) WHERE [enabled] = 1;

-- Service Configuration table
CREATE TABLE [dbo].[service_configurations] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [service_name] NVARCHAR(255) NOT NULL,
    [description] NVARCHAR(MAX),
    [enabled] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_service_configurations_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for service configuration
CREATE NONCLUSTERED INDEX IX_service_configurations_tenant_id ON [dbo].[service_configurations] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_service_configurations_enabled ON [dbo].[service_configurations] ([enabled]) WHERE [enabled] = 1;

-- Sub-Service Configuration table
CREATE TABLE [dbo].[sub_service_configurations] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [service_id] BIGINT NOT NULL,
    [service_name] NVARCHAR(255) NOT NULL,
    [sub_service_name] NVARCHAR(255) NOT NULL,
    [direction] NVARCHAR(50) NOT NULL CHECK ([direction] IN ('INBOUND', 'OUTBOUND', 'BIDIRECTIONAL')),
    [enabled] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_sub_service_configurations_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE,
    CONSTRAINT FK_sub_service_configurations_service FOREIGN KEY ([service_id]) REFERENCES [dbo].[service_configurations]([id]) ON DELETE CASCADE
);

-- Create indexes for sub-service configuration
CREATE NONCLUSTERED INDEX IX_sub_service_configurations_tenant_id ON [dbo].[sub_service_configurations] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_sub_service_configurations_service_id ON [dbo].[sub_service_configurations] ([service_id]);
CREATE NONCLUSTERED INDEX IX_sub_service_configurations_direction ON [dbo].[sub_service_configurations] ([direction]);

-- File Schemas table
CREATE TABLE [dbo].[file_schemas] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [schema_name] NVARCHAR(255) NOT NULL,
    [file_type] NVARCHAR(50) NOT NULL CHECK ([file_type] IN ('JSON', 'XML', 'COBOL', 'CSV', 'BINARY')),
    [schema_content] NVARCHAR(MAX) NOT NULL,
    [version] NVARCHAR(50) NOT NULL DEFAULT '1.0',
    [active] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_file_schemas_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for file schemas
CREATE NONCLUSTERED INDEX IX_file_schemas_tenant_id ON [dbo].[file_schemas] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_file_schemas_file_type ON [dbo].[file_schemas] ([file_type]);
CREATE NONCLUSTERED INDEX IX_file_schemas_active ON [dbo].[file_schemas] ([active]) WHERE [active] = 1;

-- Shared Schemas table (for schema reuse feature)
CREATE TABLE [dbo].[shared_schemas] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [schema_name] NVARCHAR(255) NOT NULL,
    [file_type] NVARCHAR(50) NOT NULL CHECK ([file_type] IN ('JSON', 'XML', 'COBOL', 'CSV', 'BINARY')),
    [schema_content] NVARCHAR(MAX) NOT NULL,
    [version] NVARCHAR(50) NOT NULL DEFAULT '1.0',
    [is_global] BIT NOT NULL DEFAULT 0,
    [supports_count_validation] BIT NOT NULL DEFAULT 0,
    [count_field_path] NVARCHAR(255),
    [active] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_shared_schemas_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for shared schemas
CREATE NONCLUSTERED INDEX IX_shared_schemas_tenant_id ON [dbo].[shared_schemas] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_shared_schemas_file_type ON [dbo].[shared_schemas] ([file_type]);
CREATE NONCLUSTERED INDEX IX_shared_schemas_global ON [dbo].[shared_schemas] ([is_global]) WHERE [is_global] = 1;

-- File Transfer Records table
CREATE TABLE [dbo].[file_transfer_records] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [file_name] NVARCHAR(500) NOT NULL,
    [file_path] NVARCHAR(1000),
    [file_size] BIGINT NOT NULL DEFAULT 0,
    [file_type] NVARCHAR(50) NOT NULL,
    [transfer_direction] NVARCHAR(50) NOT NULL CHECK ([transfer_direction] IN ('INBOUND', 'OUTBOUND')),
    [status] NVARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK ([status] IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'ARCHIVED')),
    [service_name] NVARCHAR(255),
    [sub_service_name] NVARCHAR(255),
    [processing_start_time] DATETIME2(7),
    [processing_end_time] DATETIME2(7),
    [error_message] NVARCHAR(MAX),
    [validation_result] NVARCHAR(MAX),
    [metadata] NVARCHAR(MAX), -- JSON metadata
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    
    CONSTRAINT FK_file_transfer_records_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for file transfer records (optimized for enterprise queries)
CREATE NONCLUSTERED INDEX IX_file_transfer_records_tenant_id ON [dbo].[file_transfer_records] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_file_transfer_records_status ON [dbo].[file_transfer_records] ([status]);
CREATE NONCLUSTERED INDEX IX_file_transfer_records_direction ON [dbo].[file_transfer_records] ([transfer_direction]);
CREATE NONCLUSTERED INDEX IX_file_transfer_records_processing_date ON [dbo].[file_transfer_records] ([created_at]) INCLUDE ([tenant_id], [status]);
CREATE NONCLUSTERED INDEX IX_file_transfer_records_service ON [dbo].[file_transfer_records] ([tenant_id], [service_name], [sub_service_name]);

-- Daily File Count Tracker table (for EOT validation)
CREATE TABLE [dbo].[daily_file_count_tracker] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [service_name] NVARCHAR(255) NOT NULL,
    [sub_service_name] NVARCHAR(255) NOT NULL,
    [direction] NVARCHAR(50) NOT NULL CHECK ([direction] IN ('INBOUND', 'OUTBOUND')),
    [file_type] NVARCHAR(50) NOT NULL,
    [processing_date] DATE NOT NULL,
    [expected_count] INT,
    [actual_count] INT NOT NULL DEFAULT 0,
    [sot_received] BIT NOT NULL DEFAULT 0,
    [eot_received] BIT NOT NULL DEFAULT 0,
    [validation_status] NVARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK ([validation_status] IN ('PENDING', 'VALID', 'DISCREPANCY', 'MISSING_EOT', 'MISSING_SOT', 'ERROR')),
    [discrepancy_count] INT,
    [tolerance_percentage] DECIMAL(5,2) DEFAULT 0.0,
    [validated_at] DATETIME2(7),
    [validation_notes] NVARCHAR(MAX),
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    
    CONSTRAINT FK_daily_file_count_tracker_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for daily file count tracker (optimized for EOT queries)
CREATE NONCLUSTERED INDEX IX_daily_file_count_tracker_tenant_date ON [dbo].[daily_file_count_tracker] ([tenant_id], [processing_date]);
CREATE NONCLUSTERED INDEX IX_daily_file_count_tracker_validation_status ON [dbo].[daily_file_count_tracker] ([validation_status]);
CREATE NONCLUSTERED INDEX IX_daily_file_count_tracker_subservice_date ON [dbo].[daily_file_count_tracker] ([tenant_id], [service_name], [sub_service_name], [processing_date]);

-- Holidays table
CREATE TABLE [dbo].[holidays] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [holiday_name] NVARCHAR(255) NOT NULL,
    [holiday_date] DATE NOT NULL,
    [is_recurring] BIT NOT NULL DEFAULT 0,
    [affects_processing] BIT NOT NULL DEFAULT 1,
    [description] NVARCHAR(MAX),
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_holidays_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for holidays
CREATE NONCLUSTERED INDEX IX_holidays_tenant_id ON [dbo].[holidays] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_holidays_date ON [dbo].[holidays] ([holiday_date]);
CREATE NONCLUSTERED INDEX IX_holidays_affects_processing ON [dbo].[holidays] ([affects_processing]) WHERE [affects_processing] = 1;

-- Cut-off Extensions table
CREATE TABLE [dbo].[cut_off_extensions] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [sub_service_id] BIGINT NOT NULL,
    [extension_date] DATE NOT NULL,
    [original_cut_off_time] TIME NOT NULL,
    [requested_extension_minutes] INT NOT NULL,
    [approved_extension_minutes] INT,
    [new_cut_off_time] TIME,
    [status] NVARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK ([status] IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'COMPLETED')),
    [priority] NVARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK ([priority] IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    [reason] NVARCHAR(MAX) NOT NULL,
    [business_justification] NVARCHAR(MAX),
    [requested_by] NVARCHAR(100) NOT NULL,
    [requested_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [approved_by] NVARCHAR(100),
    [approved_at] DATETIME2(7),
    [approval_comments] NVARCHAR(MAX),
    [completed_at] DATETIME2(7),
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    
    CONSTRAINT FK_cut_off_extensions_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for cut-off extensions
CREATE NONCLUSTERED INDEX IX_cut_off_extensions_tenant_id ON [dbo].[cut_off_extensions] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_cut_off_extensions_status ON [dbo].[cut_off_extensions] ([status]);
CREATE NONCLUSTERED INDEX IX_cut_off_extensions_date ON [dbo].[cut_off_extensions] ([extension_date]);

-- SSO Configuration table
CREATE TABLE [dbo].[sso_configurations] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [provider_type] NVARCHAR(50) NOT NULL CHECK ([provider_type] IN ('SAML', 'OAUTH2', 'OIDC')),
    [provider_name] NVARCHAR(255) NOT NULL,
    [configuration] NVARCHAR(MAX) NOT NULL, -- JSON configuration
    [active] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_sso_configurations_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Create indexes for SSO configurations
CREATE NONCLUSTERED INDEX IX_sso_configurations_tenant_id ON [dbo].[sso_configurations] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_sso_configurations_active ON [dbo].[sso_configurations] ([active]) WHERE [active] = 1;

-- Alert Configurations table
CREATE TABLE [dbo].[alert_configurations] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [alert_name] NVARCHAR(255) NOT NULL,
    [alert_type] NVARCHAR(100) NOT NULL,
    [configuration] NVARCHAR(MAX) NOT NULL, -- JSON configuration
    [enabled] BIT NOT NULL DEFAULT 1,
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [created_by] NVARCHAR(100),
    [updated_by] NVARCHAR(100),
    
    CONSTRAINT FK_alert_configurations_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE
);

-- Alert History table
CREATE TABLE [dbo].[alert_history] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tenant_id] NVARCHAR(100) NOT NULL,
    [alert_configuration_id] BIGINT,
    [alert_level] NVARCHAR(20) NOT NULL CHECK ([alert_level] IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    [message] NVARCHAR(MAX) NOT NULL,
    [component] NVARCHAR(100),
    [metadata] NVARCHAR(MAX), -- JSON metadata
    [acknowledged] BIT NOT NULL DEFAULT 0,
    [acknowledged_by] NVARCHAR(100),
    [acknowledged_at] DATETIME2(7),
    [resolved] BIT NOT NULL DEFAULT 0,
    [resolved_by] NVARCHAR(100),
    [resolved_at] DATETIME2(7),
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    
    CONSTRAINT FK_alert_history_tenant FOREIGN KEY ([tenant_id]) REFERENCES [dbo].[tenants]([tenant_id]) ON DELETE CASCADE,
    CONSTRAINT FK_alert_history_configuration FOREIGN KEY ([alert_configuration_id]) REFERENCES [dbo].[alert_configurations]([id]) ON DELETE SET NULL
);

-- Create indexes for alert history (optimized for time-series queries)
CREATE NONCLUSTERED INDEX IX_alert_history_tenant_id ON [dbo].[alert_history] ([tenant_id]);
CREATE NONCLUSTERED INDEX IX_alert_history_created_at ON [dbo].[alert_history] ([created_at] DESC);
CREATE NONCLUSTERED INDEX IX_alert_history_level ON [dbo].[alert_history] ([alert_level]);
CREATE NONCLUSTERED INDEX IX_alert_history_unresolved ON [dbo].[alert_history] ([resolved]) WHERE [resolved] = 0;

-- Schema Usage Mapping table (for schema reuse tracking)
CREATE TABLE [dbo].[schema_usage_mappings] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [sub_service_configuration_id] BIGINT NOT NULL,
    [shared_schema_id] BIGINT NOT NULL,
    [direction] NVARCHAR(50) NOT NULL CHECK ([direction] IN ('INBOUND', 'OUTBOUND')),
    [is_primary] BIT NOT NULL DEFAULT 0,
    [validation_enabled] BIT NOT NULL DEFAULT 1,
    [usage_count] BIGINT NOT NULL DEFAULT 0,
    [last_used_at] DATETIME2(7),
    [created_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    [updated_at] DATETIME2(7) NOT NULL DEFAULT GETUTCDATE(),
    
    CONSTRAINT FK_schema_usage_mappings_subservice FOREIGN KEY ([sub_service_configuration_id]) REFERENCES [dbo].[sub_service_configurations]([id]) ON DELETE CASCADE,
    CONSTRAINT FK_schema_usage_mappings_schema FOREIGN KEY ([shared_schema_id]) REFERENCES [dbo].[shared_schemas]([id]) ON DELETE CASCADE,
    CONSTRAINT UQ_schema_usage_mappings_unique UNIQUE ([sub_service_configuration_id], [shared_schema_id], [direction])
);

-- Create indexes for schema usage mappings
CREATE NONCLUSTERED INDEX IX_schema_usage_mappings_subservice ON [dbo].[schema_usage_mappings] ([sub_service_configuration_id]);
CREATE NONCLUSTERED INDEX IX_schema_usage_mappings_schema ON [dbo].[schema_usage_mappings] ([shared_schema_id]);
CREATE NONCLUSTERED INDEX IX_schema_usage_mappings_primary ON [dbo].[schema_usage_mappings] ([is_primary]) WHERE [is_primary] = 1;

-- Create triggers for updated_at columns (SQL Server equivalent of PostgreSQL triggers)
-- Trigger for tenants table
CREATE TRIGGER [dbo].[tr_tenants_updated_at]
ON [dbo].[tenants]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [dbo].[tenants] 
    SET [updated_at] = GETUTCDATE() 
    FROM [dbo].[tenants] t
    INNER JOIN inserted i ON t.[id] = i.[id];
END;

-- Trigger for service_configurations table
CREATE TRIGGER [dbo].[tr_service_configurations_updated_at]
ON [dbo].[service_configurations]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [dbo].[service_configurations] 
    SET [updated_at] = GETUTCDATE() 
    FROM [dbo].[service_configurations] sc
    INNER JOIN inserted i ON sc.[id] = i.[id];
END;

-- Trigger for file_transfer_records table
CREATE TRIGGER [dbo].[tr_file_transfer_records_updated_at]
ON [dbo].[file_transfer_records]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [dbo].[file_transfer_records] 
    SET [updated_at] = GETUTCDATE() 
    FROM [dbo].[file_transfer_records] ftr
    INNER JOIN inserted i ON ftr.[id] = i.[id];
END;

-- Create stored procedures for common enterprise operations

-- Stored procedure for tenant statistics
CREATE PROCEDURE [dbo].[GetTenantStatistics]
    @TenantId NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        @TenantId as tenant_id,
        COUNT(DISTINCT sc.id) as total_services,
        COUNT(DISTINCT ssc.id) as total_sub_services,
        COUNT(DISTINCT fs.id) as total_schemas,
        COUNT(ftr.id) as total_file_transfers,
        SUM(CASE WHEN ftr.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_transfers,
        SUM(CASE WHEN ftr.status = 'FAILED' THEN 1 ELSE 0 END) as failed_transfers,
        SUM(ftr.file_size) as total_data_volume,
        AVG(CASE WHEN ftr.processing_end_time IS NOT NULL AND ftr.processing_start_time IS NOT NULL 
                 THEN DATEDIFF(MILLISECOND, ftr.processing_start_time, ftr.processing_end_time) 
                 ELSE NULL END) as avg_processing_time_ms
    FROM [dbo].[tenants] t
    LEFT JOIN [dbo].[service_configurations] sc ON t.tenant_id = sc.tenant_id
    LEFT JOIN [dbo].[sub_service_configurations] ssc ON sc.id = ssc.service_id
    LEFT JOIN [dbo].[file_schemas] fs ON t.tenant_id = fs.tenant_id
    LEFT JOIN [dbo].[file_transfer_records] ftr ON t.tenant_id = ftr.tenant_id
    WHERE t.tenant_id = @TenantId
    GROUP BY t.tenant_id;
END;

-- Stored procedure for EOT validation
CREATE PROCEDURE [dbo].[ValidateEotCounts]
    @TenantId NVARCHAR(100),
    @ProcessingDate DATE
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Update actual counts from file transfer records
    UPDATE dfct
    SET actual_count = ftr_counts.actual_count,
        updated_at = GETUTCDATE()
    FROM [dbo].[daily_file_count_tracker] dfct
    INNER JOIN (
        SELECT 
            tenant_id,
            service_name,
            sub_service_name,
            transfer_direction,
            file_type,
            CAST(created_at AS DATE) as processing_date,
            COUNT(*) as actual_count
        FROM [dbo].[file_transfer_records]
        WHERE tenant_id = @TenantId 
          AND CAST(created_at AS DATE) = @ProcessingDate
          AND status IN ('COMPLETED', 'ARCHIVED')
        GROUP BY tenant_id, service_name, sub_service_name, transfer_direction, file_type, CAST(created_at AS DATE)
    ) ftr_counts ON dfct.tenant_id = ftr_counts.tenant_id
                 AND dfct.service_name = ftr_counts.service_name
                 AND dfct.sub_service_name = ftr_counts.sub_service_name
                 AND dfct.direction = ftr_counts.transfer_direction
                 AND dfct.file_type = ftr_counts.file_type
                 AND dfct.processing_date = ftr_counts.processing_date
    WHERE dfct.tenant_id = @TenantId 
      AND dfct.processing_date = @ProcessingDate;
    
    -- Update validation status based on counts
    UPDATE [dbo].[daily_file_count_tracker]
    SET validation_status = CASE 
        WHEN expected_count IS NULL THEN 'PENDING'
        WHEN ABS(actual_count - expected_count) <= (expected_count * tolerance_percentage / 100.0) THEN 'VALID'
        ELSE 'DISCREPANCY'
    END,
    discrepancy_count = CASE 
        WHEN expected_count IS NOT NULL THEN ABS(actual_count - expected_count)
        ELSE NULL
    END,
    validated_at = GETUTCDATE(),
    updated_at = GETUTCDATE()
    WHERE tenant_id = @TenantId 
      AND processing_date = @ProcessingDate
      AND validation_status = 'PENDING';
END;

-- Create full-text indexes for enterprise search capabilities
CREATE FULLTEXT CATALOG [FileTransferFullTextCatalog];

-- Full-text index on file schemas for content search
CREATE FULLTEXT INDEX ON [dbo].[file_schemas]([schema_content])
KEY INDEX [PK__file_schemas]
ON [FileTransferFullTextCatalog];

-- Full-text index on shared schemas for content search
CREATE FULLTEXT INDEX ON [dbo].[shared_schemas]([schema_content])
KEY INDEX [PK__shared_schemas]
ON [FileTransferFullTextCatalog];

-- Create partitioning function for large tables (enterprise feature)
CREATE PARTITION FUNCTION [DateRangePartitionFunction] (DATE)
AS RANGE RIGHT FOR VALUES (
    '2023-01-01', '2023-04-01', '2023-07-01', '2023-10-01',
    '2024-01-01', '2024-04-01', '2024-07-01', '2024-10-01'
);

CREATE PARTITION SCHEME [DateRangePartitionScheme]
AS PARTITION [DateRangePartitionFunction]
ALL TO ([PRIMARY]);

-- Apply partitioning to file transfer records for enterprise performance
-- Note: This would require recreating the table in a real migration
-- ALTER TABLE [dbo].[file_transfer_records] DROP CONSTRAINT [PK__file_transfer_records];
-- ALTER TABLE [dbo].[file_transfer_records] ADD CONSTRAINT [PK__file_transfer_records] PRIMARY KEY ([id], [created_at]) ON [DateRangePartitionScheme]([created_at]);

-- Insert initial configuration data
INSERT INTO [dbo].[tenants] ([tenant_id], [tenant_name], [timezone], [enabled], [created_by])
VALUES 
    ('system', 'System Tenant', 'UTC', 1, 'system'),
    ('demo', 'Demo Tenant', 'America/New_York', 1, 'system');

-- Create database roles for enterprise security
CREATE ROLE [FileTransferReadOnly];
CREATE ROLE [FileTransferReadWrite]; 
CREATE ROLE [FileTransferAdmin];

-- Grant permissions to roles
-- Read-only role
GRANT SELECT ON SCHEMA::[dbo] TO [FileTransferReadOnly];

-- Read-write role  
GRANT SELECT, INSERT, UPDATE ON SCHEMA::[dbo] TO [FileTransferReadWrite];
GRANT DELETE ON [dbo].[file_transfer_records] TO [FileTransferReadWrite];
GRANT DELETE ON [dbo].[alert_history] TO [FileTransferReadWrite];

-- Admin role
GRANT ALL ON SCHEMA::[dbo] TO [FileTransferAdmin];
GRANT CREATE TABLE TO [FileTransferAdmin];
GRANT CREATE PROCEDURE TO [FileTransferAdmin];

-- Enable Query Store for enterprise performance monitoring
ALTER DATABASE [filetransfer] SET QUERY_STORE = ON;
ALTER DATABASE [filetransfer] SET QUERY_STORE (
    OPERATION_MODE = READ_WRITE,
    CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30),
    DATA_FLUSH_INTERVAL_SECONDS = 900,
    INTERVAL_LENGTH_MINUTES = 60,
    MAX_STORAGE_SIZE_MB = 1000,
    QUERY_CAPTURE_MODE = AUTO,
    SIZE_BASED_CLEANUP_MODE = AUTO
);

-- Enable automatic tuning for enterprise optimization
ALTER DATABASE [filetransfer] SET AUTOMATIC_TUNING (FORCE_LAST_GOOD_PLAN = ON);
ALTER DATABASE [filetransfer] SET AUTOMATIC_TUNING (CREATE_INDEX = ON);
ALTER DATABASE [filetransfer] SET AUTOMATIC_TUNING (DROP_INDEX = ON);