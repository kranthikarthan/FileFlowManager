-- Migration script for SubService hierarchy implementation
-- This script creates the new tables and migrates existing data

-- Create sub_service_configurations table
CREATE TABLE sub_service_configurations (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100) NOT NULL,
    tenant_id NVARCHAR(50) NOT NULL,
    inbound_path NVARCHAR(500) NOT NULL,
    outbound_path NVARCHAR(500) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    
    -- Cut-off time configuration (moved from service level)
    cut_off_time NVARCHAR(10) NOT NULL DEFAULT '23:59:59',
    cut_off_time_type NVARCHAR(20) NOT NULL DEFAULT 'DAILY',
    weekday_cut_off_time NVARCHAR(10),
    weekend_cut_off_time NVARCHAR(10),
    monday_cut_off_time NVARCHAR(10),
    tuesday_cut_off_time NVARCHAR(10),
    wednesday_cut_off_time NVARCHAR(10),
    thursday_cut_off_time NVARCHAR(10),
    friday_cut_off_time NVARCHAR(10),
    saturday_cut_off_time NVARCHAR(10),
    sunday_cut_off_time NVARCHAR(10),
    
    -- Holiday configuration
    all_sundays_as_holidays BIT NOT NULL DEFAULT 0,
    
    -- SOT/EOT marker configuration
    start_marker_prefix NVARCHAR(20) NOT NULL DEFAULT 'SOT_',
    end_marker_prefix NVARCHAR(20) NOT NULL DEFAULT 'EOT_',
    
    -- File naming patterns
    data_file_pattern NVARCHAR(100) NOT NULL DEFAULT '*.*',
    sot_file_pattern NVARCHAR(100) DEFAULT 'SOT_*',
    eot_file_pattern NVARCHAR(100) DEFAULT 'EOT_*',
    
    -- Validation settings
    schema_validation_enabled BIT NOT NULL DEFAULT 1,
    binary_file_bypass BIT NOT NULL DEFAULT 1,
    schema_validation_mode NVARCHAR(20) DEFAULT 'STRICT',
    
    -- Processing settings
    max_retries INT NOT NULL DEFAULT 3,
    poll_interval_seconds INT NOT NULL DEFAULT 30,
    
    description NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    
    CONSTRAINT UK_SubService_Tenant_Service_SubService UNIQUE (tenant_id, service_name, sub_service_name)
);

-- Create indexes for sub_service_configurations
CREATE INDEX IDX_SubServiceConfigurations_TenantId ON sub_service_configurations(tenant_id);
CREATE INDEX IDX_SubServiceConfigurations_ServiceName ON sub_service_configurations(tenant_id, service_name);
CREATE INDEX IDX_SubServiceConfigurations_Enabled ON sub_service_configurations(enabled);

-- Create sub_service_file_type_configs table for FileType to Schema mapping
CREATE TABLE sub_service_file_type_configs (
    sub_service_config_id BIGINT NOT NULL,
    file_type NVARCHAR(50) NOT NULL,
    inbound_schema_id BIGINT,
    outbound_schema_id BIGINT,
    
    PRIMARY KEY (sub_service_config_id, file_type),
    FOREIGN KEY (sub_service_config_id) REFERENCES sub_service_configurations(id) ON DELETE CASCADE,
    FOREIGN KEY (inbound_schema_id) REFERENCES file_schemas(id),
    FOREIGN KEY (outbound_schema_id) REFERENCES file_schemas(id)
);

-- Create sub_service_direction_configs table for direction-specific settings
CREATE TABLE sub_service_direction_configs (
    sub_service_config_id BIGINT NOT NULL,
    direction NVARCHAR(20) NOT NULL,
    config_json NTEXT,
    
    PRIMARY KEY (sub_service_config_id, direction),
    FOREIGN KEY (sub_service_config_id) REFERENCES sub_service_configurations(id) ON DELETE CASCADE
);

-- Create cutoff_extensions table
CREATE TABLE cutoff_extensions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    tenant_id NVARCHAR(50) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100) NOT NULL,
    extension_date DATE NOT NULL,
    original_cut_off_time TIME NOT NULL,
    extended_cut_off_time TIME NOT NULL,
    reason NVARCHAR(1000) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority NVARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    
    requested_by NVARCHAR(100) NOT NULL,
    requested_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    approved_by NVARCHAR(100),
    approved_at DATETIME2,
    rejected_by NVARCHAR(100),
    rejected_at DATETIME2,
    rejection_reason NVARCHAR(1000),
    
    effective_from DATETIME2,
    effective_until DATETIME2,
    auto_approved BIT NOT NULL DEFAULT 0,
    approval_comments NVARCHAR(1000),
    notifications_sent NVARCHAR(500)
);

-- Create indexes for cutoff_extensions
CREATE INDEX IDX_CutoffExtensions_TenantId ON cutoff_extensions(tenant_id);
CREATE INDEX IDX_CutoffExtensions_Status ON cutoff_extensions(status);
CREATE INDEX IDX_CutoffExtensions_ExtensionDate ON cutoff_extensions(extension_date);
CREATE INDEX IDX_CutoffExtensions_Service ON cutoff_extensions(tenant_id, service_name, sub_service_name);
CREATE INDEX IDX_CutoffExtensions_RequestedBy ON cutoff_extensions(requested_by);

-- Add FileType column to file_transfer_records if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('file_transfer_records') AND name = 'file_type')
BEGIN
    ALTER TABLE file_transfer_records ADD file_type NVARCHAR(50) NOT NULL DEFAULT 'COBOL_FLAT_FILE';
END

-- Add schema validation columns to file_transfer_records if they don't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('file_transfer_records') AND name = 'schema_validation_passed')
BEGIN
    ALTER TABLE file_transfer_records ADD schema_validation_passed BIT;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('file_transfer_records') AND name = 'schema_validation_errors')
BEGIN
    ALTER TABLE file_transfer_records ADD schema_validation_errors NVARCHAR(2000);
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('file_transfer_records') AND name = 'schema_id')
BEGIN
    ALTER TABLE file_transfer_records ADD schema_id BIGINT;
END

-- Migrate existing service_configurations to sub_service_configurations
INSERT INTO sub_service_configurations (
    service_name,
    sub_service_name,
    tenant_id,
    inbound_path,
    outbound_path,
    enabled,
    cut_off_time,
    cut_off_time_type,
    weekday_cut_off_time,
    weekend_cut_off_time,
    monday_cut_off_time,
    tuesday_cut_off_time,
    wednesday_cut_off_time,
    thursday_cut_off_time,
    friday_cut_off_time,
    saturday_cut_off_time,
    sunday_cut_off_time,
    all_sundays_as_holidays,
    start_marker_prefix,
    end_marker_prefix,
    data_file_pattern,
    schema_validation_enabled,
    binary_file_bypass,
    max_retries,
    poll_interval_seconds,
    description,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT 
    service_name,
    COALESCE(sub_service_name, 'default') as sub_service_name,
    tenant_id,
    inbound_path,
    outbound_path,
    enabled,
    cut_off_time,
    cut_off_time_type,
    weekday_cut_off_time,
    weekend_cut_off_time,
    monday_cut_off_time,
    tuesday_cut_off_time,
    wednesday_cut_off_time,
    thursday_cut_off_time,
    friday_cut_off_time,
    saturday_cut_off_time,
    sunday_cut_off_time,
    all_sundays_as_holidays,
    start_marker_prefix,
    end_marker_prefix,
    data_file_pattern,
    COALESCE(schema_validation_enabled, 1),
    COALESCE(binary_file_bypass, 1),
    max_retries,
    poll_interval_seconds,
    description,
    created_at,
    updated_at,
    created_by,
    updated_by
FROM service_configurations;

-- Migrate file type schemas from service configurations to sub-service configurations
-- This maps schemas to different file types based on existing schema configurations
INSERT INTO sub_service_file_type_configs (sub_service_config_id, file_type, inbound_schema_id, outbound_schema_id)
SELECT 
    ssc.id,
    'COBOL_FLAT_FILE' as file_type,
    sc.data_schema_id as inbound_schema_id,
    sc.data_schema_id as outbound_schema_id
FROM sub_service_configurations ssc
JOIN service_configurations sc ON sc.tenant_id = ssc.tenant_id 
    AND sc.service_name = ssc.service_name 
    AND COALESCE(sc.sub_service_name, 'default') = ssc.sub_service_name
WHERE sc.data_schema_id IS NOT NULL;

-- Add XML file type mappings where applicable
INSERT INTO sub_service_file_type_configs (sub_service_config_id, file_type, inbound_schema_id, outbound_schema_id)
SELECT 
    ssc.id,
    'XML' as file_type,
    sc.data_schema_id as inbound_schema_id,
    sc.data_schema_id as outbound_schema_id
FROM sub_service_configurations ssc
JOIN service_configurations sc ON sc.tenant_id = ssc.tenant_id 
    AND sc.service_name = ssc.service_name 
    AND COALESCE(sc.sub_service_name, 'default') = ssc.sub_service_name
JOIN file_schemas fs ON fs.id = sc.data_schema_id
WHERE sc.data_schema_id IS NOT NULL AND fs.schema_type = 'XML';

-- Add JSON file type mappings where applicable
INSERT INTO sub_service_file_type_configs (sub_service_config_id, file_type, inbound_schema_id, outbound_schema_id)
SELECT 
    ssc.id,
    'JSON' as file_type,
    sc.data_schema_id as inbound_schema_id,
    sc.data_schema_id as outbound_schema_id
FROM sub_service_configurations ssc
JOIN service_configurations sc ON sc.tenant_id = ssc.tenant_id 
    AND sc.service_name = ssc.service_name 
    AND COALESCE(sc.sub_service_name, 'default') = ssc.sub_service_name
JOIN file_schemas fs ON fs.id = sc.data_schema_id
WHERE sc.data_schema_id IS NOT NULL AND fs.schema_type = 'JSON';

-- Update file_transfer_records to detect file types based on file names
UPDATE file_transfer_records 
SET file_type = CASE 
    WHEN file_name LIKE '%.xml' THEN 'XML'
    WHEN file_name LIKE '%.json' THEN 'JSON'
    WHEN file_name LIKE '%.csv' THEN 'CSV'
    WHEN file_name LIKE '%.exe' OR file_name LIKE '%.dll' OR file_name LIKE '%.bin' 
         OR file_name LIKE '%.jpg' OR file_name LIKE '%.png' OR file_name LIKE '%.pdf' THEN 'BINARY_FILE'
    ELSE 'COBOL_FLAT_FILE'
END
WHERE file_type = 'COBOL_FLAT_FILE';

PRINT 'SubService hierarchy migration completed successfully';
PRINT 'Tables created: sub_service_configurations, sub_service_file_type_configs, sub_service_direction_configs, cutoff_extensions';
PRINT 'Data migrated from service_configurations to sub_service_configurations';
PRINT 'File types detected and updated in file_transfer_records';