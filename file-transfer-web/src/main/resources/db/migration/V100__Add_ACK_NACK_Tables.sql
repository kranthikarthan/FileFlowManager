-- Migration to add ACK/NACK tables and related functionality
-- Version: V100
-- Description: Add ACK/NACK record tracking tables

-- Create ack_nack_records table
CREATE TABLE ack_nack_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_file_transfer_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    ack_nack_file_name VARCHAR(255) NOT NULL,
    type ENUM('ACK', 'NACK') NOT NULL,
    status ENUM('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED') NOT NULL,
    direction ENUM('INBOUND', 'OUTBOUND') NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    ack_nack_file_path VARCHAR(500),
    partner_path VARCHAR(500),
    content TEXT,
    error_message TEXT,
    reason_code VARCHAR(50),
    reason_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    file_size BIGINT,
    checksum VARCHAR(255),
    metadata TEXT,
    
    INDEX idx_ack_nack_original_file_transfer_id (original_file_transfer_id),
    INDEX idx_ack_nack_tenant_id (tenant_id),
    INDEX idx_ack_nack_status (status),
    INDEX idx_ack_nack_type (type),
    INDEX idx_ack_nack_direction (direction),
    INDEX idx_ack_nack_service (tenant_id, service_name),
    INDEX idx_ack_nack_created_at (created_at),
    INDEX idx_ack_nack_expires_at (expires_at),
    
    CONSTRAINT fk_ack_nack_file_transfer 
        FOREIGN KEY (original_file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for performance
CREATE INDEX idx_ack_nack_tenant_service_status ON ack_nack_records(tenant_id, service_name, status);
CREATE INDEX idx_ack_nack_tenant_type_status ON ack_nack_records(tenant_id, type, status);
CREATE INDEX idx_ack_nack_pending_expired ON ack_nack_records(status, expires_at) WHERE status IN ('PENDING', 'GENERATED', 'SENT');

-- Create view for ACK/NACK summary statistics
CREATE VIEW ack_nack_statistics AS
SELECT 
    tenant_id,
    service_name,
    sub_service_name,
    COUNT(*) as total_records,
    SUM(CASE WHEN type = 'ACK' THEN 1 ELSE 0 END) as ack_count,
    SUM(CASE WHEN type = 'NACK' THEN 1 ELSE 0 END) as nack_count,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_count,
    SUM(CASE WHEN status = 'GENERATED' THEN 1 ELSE 0 END) as generated_count,
    SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END) as sent_count,
    SUM(CASE WHEN status = 'RECEIVED' THEN 1 ELSE 0 END) as received_count,
    SUM(CASE WHEN status = 'PROCESSED' THEN 1 ELSE 0 END) as processed_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) as expired_count,
    MAX(created_at) as last_ack_nack_created,
    AVG(TIMESTAMPDIFF(MINUTE, created_at, COALESCE(processed_at, sent_at))) as avg_processing_time_minutes
FROM ack_nack_records 
GROUP BY tenant_id, service_name, sub_service_name;

-- Add configuration table for ACK/NACK settings per service
CREATE TABLE ack_nack_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    auto_generate_ack BOOLEAN DEFAULT TRUE,
    auto_generate_nack BOOLEAN DEFAULT TRUE,
    auto_send_ack_nack BOOLEAN DEFAULT TRUE,
    ack_nack_timeout_hours INT DEFAULT 24,
    ack_nack_format ENUM('PIPE_DELIMITED', 'JSON', 'XML', 'CUSTOM') DEFAULT 'PIPE_DELIMITED',
    custom_ack_template TEXT,
    custom_nack_template TEXT,
    partner_ack_path VARCHAR(500),
    partner_nack_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_ack_nack_config (tenant_id, service_name, sub_service_name),
    INDEX idx_ack_nack_config_tenant (tenant_id),
    INDEX idx_ack_nack_config_service (tenant_id, service_name)
);

-- Insert default configurations for existing services
INSERT INTO ack_nack_configuration (tenant_id, service_name, sub_service_name, auto_generate_ack, auto_generate_nack, auto_send_ack_nack)
SELECT DISTINCT 
    tenant_id, 
    service_name, 
    sub_service_name,
    TRUE,
    TRUE,
    TRUE
FROM service_configurations 
WHERE active = TRUE
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Add triggers to automatically create ACK/NACK configuration when new services are added
DELIMITER //
CREATE TRIGGER tr_service_config_ack_nack_insert
    AFTER INSERT ON service_configurations
    FOR EACH ROW
BEGIN
    INSERT IGNORE INTO ack_nack_configuration (tenant_id, service_name, sub_service_name)
    VALUES (NEW.tenant_id, NEW.service_name, NEW.sub_service_name);
END//
DELIMITER ;

-- Add audit trail for ACK/NACK operations
CREATE TABLE ack_nack_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ack_nack_record_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status ENUM('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED'),
    new_status ENUM('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED'),
    user_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_ack_nack_audit_record_id (ack_nack_record_id),
    INDEX idx_ack_nack_audit_created_at (created_at),
    
    CONSTRAINT fk_ack_nack_audit_record 
        FOREIGN KEY (ack_nack_record_id) 
        REFERENCES ack_nack_records(id) 
        ON DELETE CASCADE
);

-- Create trigger for ACK/NACK status change auditing
DELIMITER //
CREATE TRIGGER tr_ack_nack_status_audit
    AFTER UPDATE ON ack_nack_records
    FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO ack_nack_audit_log (ack_nack_record_id, action, old_status, new_status, details)
        VALUES (NEW.id, 'STATUS_CHANGE', OLD.status, NEW.status, 
                CONCAT('Status changed from ', OLD.status, ' to ', NEW.status));
    END IF;
END//
DELIMITER ;

-- Add indexes for better query performance on file_transfer_records for ACK/NACK lookups
CREATE INDEX idx_file_transfer_direction_status ON file_transfer_records(direction, status);
CREATE INDEX idx_file_transfer_tenant_direction_status ON file_transfer_records(tenant_id, direction, status);
CREATE INDEX idx_file_transfer_service_direction ON file_transfer_records(service_name, direction);

-- Create materialized view for ACK/NACK dashboard metrics (MySQL doesn't support materialized views, so we'll use a table)
CREATE TABLE ack_nack_dashboard_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    metric_date DATE NOT NULL,
    total_files_sent BIGINT DEFAULT 0,
    total_files_received BIGINT DEFAULT 0,
    ack_files_generated BIGINT DEFAULT 0,
    nack_files_generated BIGINT DEFAULT 0,
    ack_files_received BIGINT DEFAULT 0,
    nack_files_received BIGINT DEFAULT 0,
    pending_ack_nack BIGINT DEFAULT 0,
    failed_ack_nack BIGINT DEFAULT 0,
    expired_ack_nack BIGINT DEFAULT 0,
    avg_ack_response_time_hours DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_ack_nack_metrics (tenant_id, service_name, metric_date),
    INDEX idx_ack_nack_metrics_date (metric_date),
    INDEX idx_ack_nack_metrics_tenant_date (tenant_id, metric_date)
);

-- Create stored procedure to refresh ACK/NACK metrics
DELIMITER //
CREATE PROCEDURE RefreshAckNackMetrics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_service_name VARCHAR(100);
    
    DECLARE tenant_service_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, service_name 
        FROM service_configurations 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND active = TRUE;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN tenant_service_cursor;
    
    read_loop: LOOP
        FETCH tenant_service_cursor INTO v_tenant_id, v_service_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO ack_nack_dashboard_metrics (
            tenant_id, service_name, metric_date,
            total_files_sent, total_files_received,
            ack_files_generated, nack_files_generated,
            ack_files_received, nack_files_received,
            pending_ack_nack, failed_ack_nack, expired_ack_nack,
            avg_ack_response_time_hours
        )
        SELECT 
            v_tenant_id,
            v_service_name,
            target_date,
            COALESCE(sent.count, 0) as total_files_sent,
            COALESCE(received.count, 0) as total_files_received,
            COALESCE(ack_gen.count, 0) as ack_files_generated,
            COALESCE(nack_gen.count, 0) as nack_files_generated,
            COALESCE(ack_recv.count, 0) as ack_files_received,
            COALESCE(nack_recv.count, 0) as nack_files_received,
            COALESCE(pending.count, 0) as pending_ack_nack,
            COALESCE(failed.count, 0) as failed_ack_nack,
            COALESCE(expired.count, 0) as expired_ack_nack,
            COALESCE(avg_time.avg_hours, 0) as avg_ack_response_time_hours
        FROM (SELECT 1) as dummy
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM file_transfer_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND direction = 'OUTBOUND' AND DATE(created_at) = target_date
        ) as sent ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM file_transfer_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND direction = 'INBOUND' AND DATE(created_at) = target_date
        ) as received ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND type = 'ACK' AND direction = 'OUTBOUND' AND DATE(created_at) = target_date
        ) as ack_gen ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND type = 'NACK' AND direction = 'OUTBOUND' AND DATE(created_at) = target_date
        ) as nack_gen ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND type = 'ACK' AND direction = 'INBOUND' AND DATE(received_at) = target_date
        ) as ack_recv ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND type = 'NACK' AND direction = 'INBOUND' AND DATE(received_at) = target_date
        ) as nack_recv ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND status = 'PENDING' AND DATE(created_at) = target_date
        ) as pending ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND status = 'FAILED' AND DATE(created_at) = target_date
        ) as failed ON 1=1
        LEFT JOIN (
            SELECT COUNT(*) as count 
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND status = 'EXPIRED' AND DATE(created_at) = target_date
        ) as expired ON 1=1
        LEFT JOIN (
            SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, COALESCE(received_at, processed_at))) as avg_hours
            FROM ack_nack_records 
            WHERE tenant_id = v_tenant_id AND service_name = v_service_name 
            AND direction = 'OUTBOUND' AND DATE(created_at) = target_date
            AND received_at IS NOT NULL
        ) as avg_time ON 1=1
        ON DUPLICATE KEY UPDATE
            total_files_sent = VALUES(total_files_sent),
            total_files_received = VALUES(total_files_received),
            ack_files_generated = VALUES(ack_files_generated),
            nack_files_generated = VALUES(nack_files_generated),
            ack_files_received = VALUES(ack_files_received),
            nack_files_received = VALUES(nack_files_received),
            pending_ack_nack = VALUES(pending_ack_nack),
            failed_ack_nack = VALUES(failed_ack_nack),
            expired_ack_nack = VALUES(expired_ack_nack),
            avg_ack_response_time_hours = VALUES(avg_ack_response_time_hours),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE tenant_service_cursor;
END//
DELIMITER ;

-- Create event to automatically refresh metrics daily
SET GLOBAL event_scheduler = ON;

CREATE EVENT evt_refresh_ack_nack_metrics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 2 HOUR
DO
    CALL RefreshAckNackMetrics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Add comments to tables
ALTER TABLE ack_nack_records COMMENT = 'Table for tracking ACK/NACK file generation, sending, and receiving';
ALTER TABLE ack_nack_configuration COMMENT = 'Configuration settings for ACK/NACK behavior per service';
ALTER TABLE ack_nack_audit_log COMMENT = 'Audit trail for ACK/NACK operations and status changes';
ALTER TABLE ack_nack_dashboard_metrics COMMENT = 'Daily metrics for ACK/NACK operations per tenant and service';