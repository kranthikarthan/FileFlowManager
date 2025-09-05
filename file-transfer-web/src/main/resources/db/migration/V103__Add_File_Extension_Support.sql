-- Migration to add file extension support
-- Version: V103
-- Description: Add optional file extension field for better file categorization and filtering

-- Add file extension field to file_transfer_records table
ALTER TABLE file_transfer_records 
ADD COLUMN file_extension VARCHAR(20);

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
    ELSE SUBSTRING(file_name, LOCATE('.', REVERSE(file_name)) * -1 + 1)
END
WHERE file_name LIKE '%.%' AND file_extension IS NULL;

-- Add indexes for file extension queries
CREATE INDEX idx_file_transfer_file_extension ON file_transfer_records(file_extension);
CREATE INDEX idx_file_transfer_tenant_extension ON file_transfer_records(tenant_id, file_extension);
CREATE INDEX idx_file_transfer_service_extension ON file_transfer_records(tenant_id, service_name, file_extension);

-- Create file extension statistics table
CREATE TABLE file_extension_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    file_extension VARCHAR(20) NOT NULL,
    stat_date DATE NOT NULL,
    file_count BIGINT DEFAULT 0,
    total_file_size BIGINT DEFAULT 0,
    avg_file_size BIGINT DEFAULT 0,
    successful_transfers BIGINT DEFAULT 0,
    failed_transfers BIGINT DEFAULT 0,
    compressed_files BIGINT DEFAULT 0,
    avg_compression_ratio FLOAT DEFAULT 0,
    hsm_validated_files BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_file_extension_stats (tenant_id, service_name, file_extension, stat_date),
    INDEX idx_file_extension_stats_tenant_date (tenant_id, stat_date),
    INDEX idx_file_extension_stats_extension_date (file_extension, stat_date),
    INDEX idx_file_extension_stats_service_date (service_name, stat_date)
);

-- Create file extension configuration table for per-service extension policies
CREATE TABLE file_extension_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    allowed_extensions TEXT, -- JSON array of allowed extensions
    blocked_extensions TEXT, -- JSON array of blocked extensions
    require_extension BOOLEAN DEFAULT FALSE,
    auto_detect_file_type BOOLEAN DEFAULT TRUE,
    default_file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI') DEFAULT 'COBOL_FLAT_FILE',
    extension_validation_enabled BOOLEAN DEFAULT FALSE,
    case_sensitive_extensions BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_file_extension_config (tenant_id, service_name, sub_service_name),
    INDEX idx_file_extension_config_tenant (tenant_id),
    INDEX idx_file_extension_config_service (tenant_id, service_name)
);

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
    TRUE,
    FALSE,
    FALSE
FROM service_configurations 
WHERE active = TRUE
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Create view for file extension analytics
CREATE VIEW file_extension_analytics AS
SELECT 
    tenant_id,
    service_name,
    file_extension,
    COUNT(*) as total_files,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_files,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_files,
    SUM(CASE WHEN compression_enabled = TRUE THEN 1 ELSE 0 END) as compressed_files,
    AVG(CASE WHEN compression_enabled = TRUE THEN compression_ratio END) as avg_compression_ratio,
    AVG(file_size) as avg_file_size,
    MAX(file_size) as max_file_size,
    MIN(file_size) as min_file_size,
    MAX(created_at) as last_file_date
FROM file_transfer_records 
WHERE file_extension IS NOT NULL
GROUP BY tenant_id, service_name, file_extension;

-- Create stored procedure to update file extension statistics
DELIMITER //
CREATE PROCEDURE UpdateFileExtensionStatistics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_service_name VARCHAR(100);
    DECLARE v_file_extension VARCHAR(20);
    
    DECLARE extension_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, service_name, file_extension
        FROM file_transfer_records 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND file_extension IS NOT NULL
        AND DATE(created_at) = target_date;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN extension_cursor;
    
    read_loop: LOOP
        FETCH extension_cursor INTO v_tenant_id, v_service_name, v_file_extension;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO file_extension_statistics (
            tenant_id, service_name, file_extension, stat_date,
            file_count, total_file_size, avg_file_size,
            successful_transfers, failed_transfers,
            compressed_files, avg_compression_ratio, hsm_validated_files
        )
        SELECT 
            v_tenant_id,
            v_service_name,
            v_file_extension,
            target_date,
            COUNT(*) as file_count,
            SUM(COALESCE(file_size, 0)) as total_file_size,
            AVG(COALESCE(file_size, 0)) as avg_file_size,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_transfers,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_transfers,
            SUM(CASE WHEN compression_enabled = TRUE THEN 1 ELSE 0 END) as compressed_files,
            AVG(CASE WHEN compression_enabled = TRUE THEN compression_ratio END) as avg_compression_ratio,
            (SELECT COUNT(*) FROM hsm_validation_records h 
             WHERE h.tenant_id = v_tenant_id 
             AND h.service_name = v_service_name 
             AND h.file_name LIKE CONCAT('%', v_file_extension)
             AND DATE(h.created_at) = target_date) as hsm_validated_files
        FROM file_transfer_records 
        WHERE tenant_id = v_tenant_id 
        AND service_name = v_service_name 
        AND file_extension = v_file_extension
        AND DATE(created_at) = target_date
        ON DUPLICATE KEY UPDATE
            file_count = VALUES(file_count),
            total_file_size = VALUES(total_file_size),
            avg_file_size = VALUES(avg_file_size),
            successful_transfers = VALUES(successful_transfers),
            failed_transfers = VALUES(failed_transfers),
            compressed_files = VALUES(compressed_files),
            avg_compression_ratio = VALUES(avg_compression_ratio),
            hsm_validated_files = VALUES(hsm_validated_files),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE extension_cursor;
END//
DELIMITER ;

-- Create event to automatically update file extension statistics daily
CREATE EVENT evt_update_file_extension_statistics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 5 HOUR
DO
    CALL UpdateFileExtensionStatistics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Add triggers to automatically update file extension when filename changes
DELIMITER //
CREATE TRIGGER tr_file_transfer_extension_update
    BEFORE UPDATE ON file_transfer_records
    FOR EACH ROW
BEGIN
    IF OLD.file_name != NEW.file_name THEN
        SET NEW.file_extension = CASE 
            WHEN NEW.file_name LIKE '%.%' THEN 
                LOWER(SUBSTRING(NEW.file_name, LOCATE('.', REVERSE(NEW.file_name)) * -1 + 1))
            ELSE NULL
        END;
    END IF;
END//
DELIMITER ;

-- Add comments to new columns and tables
ALTER TABLE file_transfer_records 
MODIFY COLUMN file_extension VARCHAR(20) COMMENT 'Optional file extension extracted from filename (e.g., .txt, .csv, .xml)';

ALTER TABLE file_extension_statistics COMMENT = 'Daily statistics for file extensions per tenant and service';
ALTER TABLE file_extension_configuration COMMENT = 'File extension validation and policy configuration per service';

-- Create indexes for better query performance
CREATE INDEX idx_file_extension_stats_tenant_extension ON file_extension_statistics(tenant_id, file_extension);
CREATE INDEX idx_file_extension_config_validation ON file_extension_configuration(extension_validation_enabled) WHERE extension_validation_enabled = TRUE;