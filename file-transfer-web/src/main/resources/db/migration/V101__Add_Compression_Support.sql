-- Migration to add compression support to file transfer system
-- Version: V101
-- Description: Add compression and decompression capabilities

-- Add compression fields to file_transfer_records table
ALTER TABLE file_transfer_records 
ADD COLUMN compression_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN compression_type ENUM('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD') DEFAULT 'NONE',
ADD COLUMN original_file_size BIGINT,
ADD COLUMN compressed_file_size BIGINT,
ADD COLUMN compression_ratio FLOAT,
ADD COLUMN compression_time_ms BIGINT,
ADD COLUMN decompression_time_ms BIGINT,
ADD COLUMN compressed_file_path VARCHAR(500);

-- Add indexes for compression-related queries
CREATE INDEX idx_file_transfer_compression_enabled ON file_transfer_records(compression_enabled);
CREATE INDEX idx_file_transfer_compression_type ON file_transfer_records(compression_type);
CREATE INDEX idx_file_transfer_tenant_compression ON file_transfer_records(tenant_id, compression_enabled);

-- Create compression configuration table
CREATE TABLE compression_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    compression_enabled BOOLEAN DEFAULT FALSE,
    default_compression_type ENUM('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD') DEFAULT 'GZIP',
    compress_outbound BOOLEAN DEFAULT TRUE,
    decompress_inbound BOOLEAN DEFAULT TRUE,
    compression_threshold_mb INT DEFAULT 1,
    prioritize_speed BOOLEAN DEFAULT FALSE,
    max_compression_time_seconds INT DEFAULT 300,
    auto_select_compression BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_compression_config (tenant_id, service_name, sub_service_name),
    INDEX idx_compression_config_tenant (tenant_id),
    INDEX idx_compression_config_enabled (compression_enabled)
);

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
    FALSE, -- Start with compression disabled by default
    'GZIP',
    TRUE,
    TRUE
FROM service_configurations 
WHERE active = TRUE
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Create compression statistics table
CREATE TABLE compression_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    compression_type ENUM('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD') NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_compression_stats (tenant_id, service_name, compression_type, stat_date),
    INDEX idx_compression_stats_tenant_date (tenant_id, stat_date),
    INDEX idx_compression_stats_type_date (compression_type, stat_date)
);

-- Create compression audit log table
CREATE TABLE compression_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_transfer_id BIGINT NOT NULL,
    operation ENUM('COMPRESS', 'DECOMPRESS', 'TEST') NOT NULL,
    compression_type ENUM('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD') NOT NULL,
    original_size BIGINT,
    processed_size BIGINT,
    processing_time_ms BIGINT,
    compression_ratio FLOAT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    user_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_compression_audit_file_transfer (file_transfer_id),
    INDEX idx_compression_audit_operation (operation),
    INDEX idx_compression_audit_created_at (created_at),
    
    CONSTRAINT fk_compression_audit_file_transfer 
        FOREIGN KEY (file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Create view for compression dashboard metrics
CREATE VIEW compression_dashboard_metrics AS
SELECT 
    tenant_id,
    service_name,
    COUNT(*) as total_files,
    SUM(CASE WHEN compression_enabled = TRUE THEN 1 ELSE 0 END) as compressed_files,
    SUM(CASE WHEN compression_enabled = TRUE THEN original_file_size ELSE 0 END) as total_original_size,
    SUM(CASE WHEN compression_enabled = TRUE THEN compressed_file_size ELSE 0 END) as total_compressed_size,
    SUM(CASE WHEN compression_enabled = TRUE THEN (original_file_size - compressed_file_size) ELSE 0 END) as total_space_saved,
    AVG(CASE WHEN compression_enabled = TRUE THEN compression_ratio ELSE NULL END) as avg_compression_ratio,
    AVG(CASE WHEN compression_enabled = TRUE THEN compression_time_ms ELSE NULL END) as avg_compression_time,
    AVG(CASE WHEN compression_enabled = TRUE THEN decompression_time_ms ELSE NULL END) as avg_decompression_time,
    compression_type as most_used_compression_type
FROM file_transfer_records 
WHERE compression_enabled = TRUE
GROUP BY tenant_id, service_name, compression_type
ORDER BY COUNT(*) DESC;

-- Create stored procedure to update compression statistics
DELIMITER //
CREATE PROCEDURE UpdateCompressionStatistics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_service_name VARCHAR(100);
    DECLARE v_compression_type VARCHAR(20);
    
    DECLARE compression_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, service_name, compression_type
        FROM file_transfer_records 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND compression_enabled = TRUE
        AND DATE(created_at) = target_date;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN compression_cursor;
    
    read_loop: LOOP
        FETCH compression_cursor INTO v_tenant_id, v_service_name, v_compression_type;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO compression_statistics (
            tenant_id, service_name, compression_type, stat_date,
            files_compressed, files_decompressed,
            total_original_size, total_compressed_size,
            total_compression_time_ms, total_decompression_time_ms,
            avg_compression_ratio, max_compression_ratio, min_compression_ratio
        )
        SELECT 
            v_tenant_id,
            v_service_name,
            v_compression_type,
            target_date,
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
        WHERE tenant_id = v_tenant_id 
        AND service_name = v_service_name 
        AND compression_type = v_compression_type
        AND compression_enabled = TRUE
        AND DATE(created_at) = target_date
        ON DUPLICATE KEY UPDATE
            files_compressed = VALUES(files_compressed),
            files_decompressed = VALUES(files_decompressed),
            total_original_size = VALUES(total_original_size),
            total_compressed_size = VALUES(total_compressed_size),
            total_compression_time_ms = VALUES(total_compression_time_ms),
            total_decompression_time_ms = VALUES(total_decompression_time_ms),
            avg_compression_ratio = VALUES(avg_compression_ratio),
            max_compression_ratio = VALUES(max_compression_ratio),
            min_compression_ratio = VALUES(min_compression_ratio),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE compression_cursor;
END//
DELIMITER ;

-- Create event to automatically update compression statistics daily
CREATE EVENT evt_update_compression_statistics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 3 HOUR
DO
    CALL UpdateCompressionStatistics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Add triggers for compression audit logging
DELIMITER //
CREATE TRIGGER tr_compression_audit_insert
    AFTER INSERT ON file_transfer_records
    FOR EACH ROW
BEGIN
    IF NEW.compression_enabled = TRUE THEN
        INSERT INTO compression_audit_log (
            file_transfer_id, operation, compression_type,
            original_size, processed_size, processing_time_ms, 
            compression_ratio, success
        )
        VALUES (
            NEW.id, 'COMPRESS', NEW.compression_type,
            NEW.original_file_size, NEW.compressed_file_size, 
            NEW.compression_time_ms, NEW.compression_ratio, TRUE
        );
    END IF;
END//

CREATE TRIGGER tr_compression_audit_update
    AFTER UPDATE ON file_transfer_records
    FOR EACH ROW
BEGIN
    -- Log compression operation
    IF OLD.compression_enabled = FALSE AND NEW.compression_enabled = TRUE THEN
        INSERT INTO compression_audit_log (
            file_transfer_id, operation, compression_type,
            original_size, processed_size, processing_time_ms, 
            compression_ratio, success
        )
        VALUES (
            NEW.id, 'COMPRESS', NEW.compression_type,
            NEW.original_file_size, NEW.compressed_file_size, 
            NEW.compression_time_ms, NEW.compression_ratio, TRUE
        );
    END IF;
    
    -- Log decompression operation
    IF OLD.decompression_time_ms IS NULL AND NEW.decompression_time_ms IS NOT NULL THEN
        INSERT INTO compression_audit_log (
            file_transfer_id, operation, compression_type,
            original_size, processed_size, processing_time_ms, 
            compression_ratio, success
        )
        VALUES (
            NEW.id, 'DECOMPRESS', NEW.compression_type,
            NEW.compressed_file_size, NEW.original_file_size, 
            NEW.decompression_time_ms, NEW.compression_ratio, TRUE
        );
    END IF;
END//
DELIMITER ;

-- Add compression-related indexes for performance
CREATE INDEX idx_file_transfer_compression_ratio ON file_transfer_records(compression_ratio) WHERE compression_enabled = TRUE;
CREATE INDEX idx_file_transfer_compression_time ON file_transfer_records(compression_time_ms) WHERE compression_enabled = TRUE;
CREATE INDEX idx_file_transfer_original_size ON file_transfer_records(original_file_size);

-- Update existing records to set original_file_size where missing
UPDATE file_transfer_records 
SET original_file_size = file_size 
WHERE original_file_size IS NULL AND file_size IS NOT NULL;

-- Add comments to new columns
ALTER TABLE file_transfer_records 
MODIFY COLUMN compression_enabled BOOLEAN DEFAULT FALSE COMMENT 'Whether compression is enabled for this transfer',
MODIFY COLUMN compression_type ENUM('NONE', 'GZIP', 'ZIP', 'BZIP2', 'XZ', 'LZ4', 'ZSTD') DEFAULT 'NONE' COMMENT 'Type of compression algorithm used',
MODIFY COLUMN original_file_size BIGINT COMMENT 'Original file size before compression',
MODIFY COLUMN compressed_file_size BIGINT COMMENT 'File size after compression',
MODIFY COLUMN compression_ratio FLOAT COMMENT 'Compression ratio (compressed_size / original_size)',
MODIFY COLUMN compression_time_ms BIGINT COMMENT 'Time taken to compress the file in milliseconds',
MODIFY COLUMN decompression_time_ms BIGINT COMMENT 'Time taken to decompress the file in milliseconds',
MODIFY COLUMN compressed_file_path VARCHAR(500) COMMENT 'Path to the compressed file';

-- Add table comments
ALTER TABLE compression_configuration COMMENT = 'Configuration settings for compression behavior per service';
ALTER TABLE compression_statistics COMMENT = 'Daily statistics for compression operations per tenant and service';
ALTER TABLE compression_audit_log COMMENT = 'Audit trail for all compression and decompression operations';