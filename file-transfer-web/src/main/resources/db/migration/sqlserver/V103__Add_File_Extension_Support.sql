-- Migration to add file extension support for SQL Server
-- Version: V103
-- Description: Add optional file extension field for better file categorization and filtering

-- Add file extension field to file_transfer_records table
ALTER TABLE file_transfer_records 
ADD file_extension NVARCHAR(20);

-- Update existing records with file extension extracted from filename
UPDATE file_transfer_records 
SET file_extension = CASE 
    WHEN file_name LIKE '%.txt' THEN '.txt'
    WHEN file_name LIKE '%.csv' THEN '.csv'
    WHEN file_name LIKE '%.json' THEN '.json'
    WHEN file_name LIKE '%.xml' THEN '.xml'
    WHEN file_name LIKE '%.dat' THEN '.dat'
    WHEN file_name LIKE '%.log' THEN '.log'
    WHEN file_name LIKE '%.yaml' THEN '.yaml'
    WHEN file_name LIKE '%.yml' THEN '.yml'
    WHEN file_name LIKE '%.properties' THEN '.properties'
    WHEN file_name LIKE '%.zip' THEN '.zip'
    WHEN file_name LIKE '%.gz' THEN '.gz'
    WHEN file_name LIKE '%.bz2' THEN '.bz2'
    WHEN file_name LIKE '%.xz' THEN '.xz'
    WHEN file_name LIKE '%.tar' THEN '.tar'
    WHEN file_name LIKE '%.pdf' THEN '.pdf'
    WHEN file_name LIKE '%.doc' THEN '.doc'
    WHEN file_name LIKE '%.docx' THEN '.docx'
    WHEN file_name LIKE '%.xls' THEN '.xls'
    WHEN file_name LIKE '%.xlsx' THEN '.xlsx'
    WHEN file_name LIKE '%.sql' THEN '.sql'
    WHEN file_name LIKE '%.sh' THEN '.sh'
    WHEN file_name LIKE '%.bat' THEN '.bat'
    WHEN file_name LIKE '%.py' THEN '.py'
    WHEN file_name LIKE '%.java' THEN '.java'
    WHEN file_name LIKE '%.js' THEN '.js'
    WHEN file_name LIKE '%.html' THEN '.html'
    WHEN file_name LIKE '%.css' THEN '.css'
    ELSE 
        CASE 
            WHEN CHARINDEX('.', file_name) > 0 THEN 
                LOWER(SUBSTRING(file_name, LEN(file_name) - CHARINDEX('.', REVERSE(file_name)) + 1, LEN(file_name)))
            ELSE NULL
        END
END
WHERE CHARINDEX('.', file_name) > 0 AND file_extension IS NULL;

-- Add indexes for file extension queries
CREATE INDEX idx_file_transfer_file_extension ON file_transfer_records(file_extension);
CREATE INDEX idx_file_transfer_tenant_extension ON file_transfer_records(tenant_id, file_extension);
CREATE INDEX idx_file_transfer_service_extension ON file_transfer_records(tenant_id, service_name, file_extension);

-- Create file extension statistics table
CREATE TABLE file_extension_statistics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    file_extension NVARCHAR(20) NOT NULL,
    stat_date DATE NOT NULL,
    file_count BIGINT DEFAULT 0,
    total_file_size BIGINT DEFAULT 0,
    avg_file_size BIGINT DEFAULT 0,
    successful_transfers BIGINT DEFAULT 0,
    failed_transfers BIGINT DEFAULT 0,
    compressed_files BIGINT DEFAULT 0,
    avg_compression_ratio FLOAT DEFAULT 0,
    hsm_validated_files BIGINT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes for file_extension_statistics
ALTER TABLE file_extension_statistics 
ADD CONSTRAINT uk_file_extension_stats UNIQUE (tenant_id, service_name, file_extension, stat_date);

CREATE INDEX idx_file_extension_stats_tenant_date ON file_extension_statistics(tenant_id, stat_date);
CREATE INDEX idx_file_extension_stats_extension_date ON file_extension_statistics(file_extension, stat_date);
CREATE INDEX idx_file_extension_stats_service_date ON file_extension_statistics(service_name, stat_date);

-- Create file extension configuration table for per-service extension policies
CREATE TABLE file_extension_configuration (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    allowed_extensions NTEXT, -- JSON array of allowed extensions
    blocked_extensions NTEXT, -- JSON array of blocked extensions
    require_extension BIT DEFAULT 0,
    auto_detect_file_type BIT DEFAULT 1,
    default_file_type NVARCHAR(20) DEFAULT 'COBOL_FLAT_FILE' CHECK (default_file_type IN ('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI')),
    extension_validation_enabled BIT DEFAULT 0,
    case_sensitive_extensions BIT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes for file_extension_configuration
ALTER TABLE file_extension_configuration 
ADD CONSTRAINT uk_file_extension_config UNIQUE (tenant_id, service_name, sub_service_name);

CREATE INDEX idx_file_extension_config_tenant ON file_extension_configuration(tenant_id);
CREATE INDEX idx_file_extension_config_service ON file_extension_configuration(tenant_id, service_name);

-- Insert default file extension configurations for existing services
INSERT INTO file_extension_configuration (
    tenant_id, 
    service_name, 
    sub_service_name,
    auto_detect_file_type,
    extension_validation_enabled,
    require_extension
)
SELECT DISTINCT 
    tenant_id, 
    service_name, 
    sub_service_name,
    1,
    0,
    0
FROM service_configurations 
WHERE active = 1;

-- Create trigger for updating updated_at in file_extension_configuration
CREATE TRIGGER tr_file_extension_config_update
    ON file_extension_configuration
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE file_extension_configuration 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Create trigger for updating updated_at in file_extension_statistics
CREATE TRIGGER tr_file_extension_stats_update
    ON file_extension_statistics
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE file_extension_statistics 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Create stored procedure to update file extension statistics for SQL Server
CREATE PROCEDURE UpdateFileExtensionStatistics
    @target_date DATE = NULL,
    @target_tenant_id NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @target_date IS NULL
        SET @target_date = CAST(GETDATE() - 1 AS DATE);
    
    -- Update file extension statistics
    MERGE file_extension_statistics AS target
    USING (
        SELECT 
            tenant_id,
            service_name,
            file_extension,
            @target_date as stat_date,
            COUNT(*) as file_count,
            SUM(ISNULL(file_size, 0)) as total_file_size,
            AVG(ISNULL(file_size, 0)) as avg_file_size,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_transfers,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_transfers,
            SUM(CASE WHEN compression_enabled = 1 THEN 1 ELSE 0 END) as compressed_files,
            AVG(CASE WHEN compression_enabled = 1 THEN compression_ratio END) as avg_compression_ratio,
            (SELECT COUNT(*) FROM hsm_validation_records h 
             WHERE h.tenant_id = f.tenant_id 
             AND h.service_name = f.service_name 
             AND h.file_name LIKE '%' + f.file_extension
             AND CAST(h.created_at AS DATE) = @target_date) as hsm_validated_files
        FROM file_transfer_records f
        WHERE (@target_tenant_id IS NULL OR tenant_id = @target_tenant_id)
        AND file_extension IS NOT NULL
        AND CAST(created_at AS DATE) = @target_date
        GROUP BY tenant_id, service_name, file_extension
    ) AS source
    ON (target.tenant_id = source.tenant_id 
        AND target.service_name = source.service_name 
        AND target.file_extension = source.file_extension
        AND target.stat_date = source.stat_date)
    WHEN MATCHED THEN
        UPDATE SET
            file_count = source.file_count,
            total_file_size = source.total_file_size,
            avg_file_size = source.avg_file_size,
            successful_transfers = source.successful_transfers,
            failed_transfers = source.failed_transfers,
            compressed_files = source.compressed_files,
            avg_compression_ratio = source.avg_compression_ratio,
            hsm_validated_files = source.hsm_validated_files,
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (tenant_id, service_name, file_extension, stat_date, file_count, 
                total_file_size, avg_file_size, successful_transfers, failed_transfers,
                compressed_files, avg_compression_ratio, hsm_validated_files)
        VALUES (source.tenant_id, source.service_name, source.file_extension, source.stat_date,
                source.file_count, source.total_file_size, source.avg_file_size,
                source.successful_transfers, source.failed_transfers, source.compressed_files,
                source.avg_compression_ratio, source.hsm_validated_files);
END;

-- Add additional indexes for performance
CREATE INDEX idx_file_extension_stats_tenant_extension ON file_extension_statistics(tenant_id, file_extension);

-- Create trigger to automatically update file extension when filename changes
CREATE TRIGGER tr_file_transfer_extension_update
    ON file_transfer_records
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE file_transfer_records 
    SET file_extension = CASE 
        WHEN CHARINDEX('.', i.file_name) > 0 THEN 
            LOWER(SUBSTRING(i.file_name, LEN(i.file_name) - CHARINDEX('.', REVERSE(i.file_name)) + 1, LEN(i.file_name)))
        ELSE NULL
    END
    FROM file_transfer_records ftr
    INNER JOIN inserted i ON ftr.id = i.id
    INNER JOIN deleted d ON ftr.id = d.id
    WHERE i.file_name != d.file_name;
END;