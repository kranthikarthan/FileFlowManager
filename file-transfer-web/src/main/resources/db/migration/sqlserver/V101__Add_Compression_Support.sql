-- Migration to add compression support to file transfer system for SQL Server
-- Version: V101
-- Description: Add compression and decompression capabilities

-- Add compression fields to file_transfer_records table
ALTER TABLE file_transfer_records 
ADD compression_enabled BIT DEFAULT 0,
    compression_type NVARCHAR(20) DEFAULT 'NONE' CHECK (compression_type IN ('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD')),
    original_file_size BIGINT,
    compressed_file_size BIGINT,
    compression_ratio FLOAT,
    compression_time_ms BIGINT,
    decompression_time_ms BIGINT,
    compressed_file_path NVARCHAR(500);

-- Add indexes for compression-related queries
CREATE INDEX idx_file_transfer_compression_enabled ON file_transfer_records(compression_enabled);
CREATE INDEX idx_file_transfer_compression_type ON file_transfer_records(compression_type);
CREATE INDEX idx_file_transfer_tenant_compression ON file_transfer_records(tenant_id, compression_enabled);

-- Create compression configuration table
CREATE TABLE compression_configuration (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    compression_enabled BIT DEFAULT 0,
    default_compression_type NVARCHAR(20) DEFAULT 'GZIP' CHECK (default_compression_type IN ('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD')),
    compress_outbound BIT DEFAULT 1,
    decompress_inbound BIT DEFAULT 1,
    compression_threshold_mb INT DEFAULT 1,
    prioritize_speed BIT DEFAULT 0,
    max_compression_time_seconds INT DEFAULT 300,
    auto_select_compression BIT DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes
ALTER TABLE compression_configuration 
ADD CONSTRAINT uk_compression_config UNIQUE (tenant_id, service_name, sub_service_name);

CREATE INDEX idx_compression_config_tenant ON compression_configuration(tenant_id);
CREATE INDEX idx_compression_config_enabled ON compression_configuration(compression_enabled);

-- Insert default compression configurations for existing services
INSERT INTO compression_configuration (
    tenant_id, 
    service_name, 
    sub_service_name, 
    compression_enabled, 
    default_compression_type,
    compress_outbound,
    decompress_inbound
)
SELECT DISTINCT 
    tenant_id, 
    service_name, 
    sub_service_name,
    0, -- Start with compression disabled by default
    'GZIP',
    1,
    1
FROM service_configurations 
WHERE active = 1;

-- Create compression statistics table
CREATE TABLE compression_statistics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    compression_type NVARCHAR(20) NOT NULL CHECK (compression_type IN ('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD')),
    stat_date DATE NOT NULL,
    files_compressed INT DEFAULT 0,
    files_decompressed INT DEFAULT 0,
    total_original_size BIGINT DEFAULT 0,
    total_compressed_size BIGINT DEFAULT 0,
    total_compression_time_ms BIGINT DEFAULT 0,
    total_decompression_time_ms BIGINT DEFAULT 0,
    avg_compression_ratio FLOAT DEFAULT 0,
    max_compression_ratio FLOAT DEFAULT 0,
    min_compression_ratio FLOAT DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes
ALTER TABLE compression_statistics 
ADD CONSTRAINT uk_compression_stats UNIQUE (tenant_id, service_name, compression_type, stat_date);

CREATE INDEX idx_compression_stats_tenant_date ON compression_statistics(tenant_id, stat_date);
CREATE INDEX idx_compression_stats_type_date ON compression_statistics(compression_type, stat_date);

-- Create compression audit log table
CREATE TABLE compression_audit_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    file_transfer_id BIGINT NOT NULL,
    operation NVARCHAR(20) NOT NULL CHECK (operation IN ('COMPRESS', 'DECOMPRESS', 'TEST')),
    compression_type NVARCHAR(20) NOT NULL CHECK (compression_type IN ('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD')),
    original_size BIGINT,
    processed_size BIGINT,
    processing_time_ms BIGINT,
    compression_ratio FLOAT,
    success BIT NOT NULL,
    error_message NTEXT,
    user_id NVARCHAR(100),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT fk_compression_audit_file_transfer 
        FOREIGN KEY (file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for audit log
CREATE INDEX idx_compression_audit_file_transfer ON compression_audit_log(file_transfer_id);
CREATE INDEX idx_compression_audit_operation ON compression_audit_log(operation);
CREATE INDEX idx_compression_audit_created_at ON compression_audit_log(created_at);

-- Create trigger for updating updated_at in compression_configuration
CREATE TRIGGER tr_compression_config_update
    ON compression_configuration
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE compression_configuration 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Create trigger for updating updated_at in compression_statistics
CREATE TRIGGER tr_compression_stats_update
    ON compression_statistics
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE compression_statistics 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Create stored procedure to update compression statistics for SQL Server
CREATE PROCEDURE UpdateCompressionStatistics
    @target_date DATE = NULL,
    @target_tenant_id NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @target_date IS NULL
        SET @target_date = CAST(GETDATE() - 1 AS DATE);
    
    -- Update compression statistics
    MERGE compression_statistics AS target
    USING (
        SELECT 
            tenant_id,
            service_name,
            compression_type,
            @target_date as stat_date,
            COUNT(*) as files_compressed,
            COUNT(CASE WHEN decompression_time_ms IS NOT NULL THEN 1 END) as files_decompressed,
            SUM(original_file_size) as total_original_size,
            SUM(compressed_file_size) as total_compressed_size,
            SUM(compression_time_ms) as total_compression_time_ms,
            SUM(decompression_time_ms) as total_decompression_time_ms,
            AVG(compression_ratio) as avg_compression_ratio,
            MAX(compression_ratio) as max_compression_ratio,
            MIN(compression_ratio) as min_compression_ratio
        FROM file_transfer_records 
        WHERE (@target_tenant_id IS NULL OR tenant_id = @target_tenant_id)
        AND compression_enabled = 1
        AND CAST(created_at AS DATE) = @target_date
        GROUP BY tenant_id, service_name, compression_type
    ) AS source
    ON (target.tenant_id = source.tenant_id 
        AND target.service_name = source.service_name 
        AND target.compression_type = source.compression_type
        AND target.stat_date = source.stat_date)
    WHEN MATCHED THEN
        UPDATE SET
            files_compressed = source.files_compressed,
            files_decompressed = source.files_decompressed,
            total_original_size = source.total_original_size,
            total_compressed_size = source.total_compressed_size,
            total_compression_time_ms = source.total_compression_time_ms,
            total_decompression_time_ms = source.total_decompression_time_ms,
            avg_compression_ratio = source.avg_compression_ratio,
            max_compression_ratio = source.max_compression_ratio,
            min_compression_ratio = source.min_compression_ratio,
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (tenant_id, service_name, compression_type, stat_date,
                files_compressed, files_decompressed, total_original_size, total_compressed_size,
                total_compression_time_ms, total_decompression_time_ms,
                avg_compression_ratio, max_compression_ratio, min_compression_ratio)
        VALUES (source.tenant_id, source.service_name, source.compression_type, source.stat_date,
                source.files_compressed, source.files_decompressed, source.total_original_size, source.total_compressed_size,
                source.total_compression_time_ms, source.total_decompression_time_ms,
                source.avg_compression_ratio, source.max_compression_ratio, source.min_compression_ratio);
END;

-- Update existing records to set original_file_size where missing
UPDATE file_transfer_records 
SET original_file_size = file_size 
WHERE original_file_size IS NULL AND file_size IS NOT NULL;