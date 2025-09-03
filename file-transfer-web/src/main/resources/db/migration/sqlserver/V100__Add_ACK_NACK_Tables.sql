-- Migration to add ACK/NACK tables and related functionality for SQL Server
-- Version: V100
-- Description: Add ACK/NACK record tracking tables

-- Create ack_nack_records table
CREATE TABLE ack_nack_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    original_file_transfer_id BIGINT NOT NULL,
    original_file_name NVARCHAR(255) NOT NULL,
    ack_nack_file_name NVARCHAR(255) NOT NULL,
    type NVARCHAR(10) NOT NULL CHECK (type IN ('ACK', 'NACK')),
    status NVARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED')),
    direction NVARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    ack_nack_file_path NVARCHAR(500),
    partner_path NVARCHAR(500),
    content NTEXT,
    error_message NTEXT,
    reason_code NVARCHAR(50),
    reason_description NTEXT,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    generated_at DATETIME2 NULL,
    sent_at DATETIME2 NULL,
    received_at DATETIME2 NULL,
    processed_at DATETIME2 NULL,
    expires_at DATETIME2 NULL,
    file_size BIGINT,
    checksum NVARCHAR(255),
    metadata NTEXT,
    
    CONSTRAINT fk_ack_nack_file_transfer 
        FOREIGN KEY (original_file_transfer_id) 
        REFERENCES file_transfer_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for performance
CREATE INDEX idx_ack_nack_original_file_transfer_id ON ack_nack_records(original_file_transfer_id);
CREATE INDEX idx_ack_nack_tenant_id ON ack_nack_records(tenant_id);
CREATE INDEX idx_ack_nack_status ON ack_nack_records(status);
CREATE INDEX idx_ack_nack_type ON ack_nack_records(type);
CREATE INDEX idx_ack_nack_direction ON ack_nack_records(direction);
CREATE INDEX idx_ack_nack_service ON ack_nack_records(tenant_id, service_name);
CREATE INDEX idx_ack_nack_created_at ON ack_nack_records(created_at);
CREATE INDEX idx_ack_nack_expires_at ON ack_nack_records(expires_at);
CREATE INDEX idx_ack_nack_tenant_service_status ON ack_nack_records(tenant_id, service_name, status);
CREATE INDEX idx_ack_nack_tenant_type_status ON ack_nack_records(tenant_id, type, status);

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
    AVG(DATEDIFF(MINUTE, created_at, COALESCE(processed_at, sent_at))) as avg_processing_time_minutes
FROM ack_nack_records 
GROUP BY tenant_id, service_name, sub_service_name;

-- Add configuration table for ACK/NACK settings per service
CREATE TABLE ack_nack_configuration (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    auto_generate_ack BIT DEFAULT 1,
    auto_generate_nack BIT DEFAULT 1,
    auto_send_ack_nack BIT DEFAULT 1,
    ack_nack_timeout_hours INT DEFAULT 24,
    ack_nack_format NVARCHAR(20) DEFAULT 'PIPE_DELIMITED' CHECK (ack_nack_format IN ('PIPE_DELIMITED', 'JSON', 'XML', 'CUSTOM')),
    custom_ack_template NTEXT,
    custom_nack_template NTEXT,
    partner_ack_path NVARCHAR(500),
    partner_nack_path NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint
ALTER TABLE ack_nack_configuration 
ADD CONSTRAINT uk_ack_nack_config UNIQUE (tenant_id, service_name, sub_service_name);

-- Add indexes
CREATE INDEX idx_ack_nack_config_tenant ON ack_nack_configuration(tenant_id);
CREATE INDEX idx_ack_nack_config_service ON ack_nack_configuration(tenant_id, service_name);

-- Insert default configurations for existing services
INSERT INTO ack_nack_configuration (tenant_id, service_name, sub_service_name, auto_generate_ack, auto_generate_nack, auto_send_ack_nack)
SELECT DISTINCT 
    tenant_id, 
    service_name, 
    sub_service_name,
    1,
    1,
    1
FROM service_configurations 
WHERE active = 1;

-- Add audit trail for ACK/NACK operations
CREATE TABLE ack_nack_audit_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ack_nack_record_id BIGINT NOT NULL,
    action NVARCHAR(50) NOT NULL,
    old_status NVARCHAR(20) CHECK (old_status IN ('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED')),
    new_status NVARCHAR(20) CHECK (new_status IN ('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED')),
    user_id NVARCHAR(100),
    details NTEXT,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT fk_ack_nack_audit_record 
        FOREIGN KEY (ack_nack_record_id) 
        REFERENCES ack_nack_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for audit log
CREATE INDEX idx_ack_nack_audit_record_id ON ack_nack_audit_log(ack_nack_record_id);
CREATE INDEX idx_ack_nack_audit_created_at ON ack_nack_audit_log(created_at);

-- Create trigger for ACK/NACK status change auditing
CREATE TRIGGER tr_ack_nack_status_audit
    ON ack_nack_records
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    INSERT INTO ack_nack_audit_log (ack_nack_record_id, action, old_status, new_status, details)
    SELECT 
        i.id,
        'STATUS_CHANGE',
        d.status,
        i.status,
        CONCAT('Status changed from ', d.status, ' to ', i.status)
    FROM inserted i
    INNER JOIN deleted d ON i.id = d.id
    WHERE i.status != d.status;
END;

-- Create trigger for updating updated_at in ack_nack_configuration
CREATE TRIGGER tr_ack_nack_config_update
    ON ack_nack_configuration
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE ack_nack_configuration 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;

-- Add indexes for better query performance on file_transfer_records for ACK/NACK lookups
CREATE INDEX idx_file_transfer_direction_status ON file_transfer_records(direction, status);
CREATE INDEX idx_file_transfer_tenant_direction_status ON file_transfer_records(tenant_id, direction, status);
CREATE INDEX idx_file_transfer_service_direction ON file_transfer_records(service_name, direction);

-- Create table for ACK/NACK dashboard metrics
CREATE TABLE ack_nack_dashboard_metrics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
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
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Create unique constraint and indexes
ALTER TABLE ack_nack_dashboard_metrics 
ADD CONSTRAINT uk_ack_nack_metrics UNIQUE (tenant_id, service_name, metric_date);

CREATE INDEX idx_ack_nack_metrics_date ON ack_nack_dashboard_metrics(metric_date);
CREATE INDEX idx_ack_nack_metrics_tenant_date ON ack_nack_dashboard_metrics(tenant_id, metric_date);

-- Create stored procedure to refresh ACK/NACK metrics
CREATE PROCEDURE RefreshAckNackMetrics
    @target_date DATE = NULL,
    @target_tenant_id NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @target_date IS NULL
        SET @target_date = CAST(GETDATE() - 1 AS DATE);
    
    -- Refresh metrics for all tenant/service combinations
    MERGE ack_nack_dashboard_metrics AS target
    USING (
        SELECT 
            sc.tenant_id,
            sc.service_name,
            @target_date as metric_date,
            ISNULL(sent.count, 0) as total_files_sent,
            ISNULL(received.count, 0) as total_files_received,
            ISNULL(ack_gen.count, 0) as ack_files_generated,
            ISNULL(nack_gen.count, 0) as nack_files_generated,
            ISNULL(ack_recv.count, 0) as ack_files_received,
            ISNULL(nack_recv.count, 0) as nack_files_received,
            ISNULL(pending.count, 0) as pending_ack_nack,
            ISNULL(failed.count, 0) as failed_ack_nack,
            ISNULL(expired.count, 0) as expired_ack_nack,
            ISNULL(avg_time.avg_hours, 0) as avg_ack_response_time_hours
        FROM (
            SELECT DISTINCT tenant_id, service_name 
            FROM service_configurations 
            WHERE (@target_tenant_id IS NULL OR tenant_id = @target_tenant_id)
            AND active = 1
        ) sc
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM file_transfer_records 
            WHERE direction = 'OUTBOUND' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) sent ON sc.tenant_id = sent.tenant_id AND sc.service_name = sent.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM file_transfer_records 
            WHERE direction = 'INBOUND' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) received ON sc.tenant_id = received.tenant_id AND sc.service_name = received.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE type = 'ACK' AND direction = 'OUTBOUND' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) ack_gen ON sc.tenant_id = ack_gen.tenant_id AND sc.service_name = ack_gen.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE type = 'NACK' AND direction = 'OUTBOUND' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) nack_gen ON sc.tenant_id = nack_gen.tenant_id AND sc.service_name = nack_gen.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE type = 'ACK' AND direction = 'INBOUND' AND CAST(received_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) ack_recv ON sc.tenant_id = ack_recv.tenant_id AND sc.service_name = ack_recv.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE type = 'NACK' AND direction = 'INBOUND' AND CAST(received_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) nack_recv ON sc.tenant_id = nack_recv.tenant_id AND sc.service_name = nack_recv.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE status = 'PENDING' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) pending ON sc.tenant_id = pending.tenant_id AND sc.service_name = pending.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE status = 'FAILED' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) failed ON sc.tenant_id = failed.tenant_id AND sc.service_name = failed.service_name
        LEFT JOIN (
            SELECT tenant_id, service_name, COUNT(*) as count 
            FROM ack_nack_records 
            WHERE status = 'EXPIRED' AND CAST(created_at AS DATE) = @target_date
            GROUP BY tenant_id, service_name
        ) expired ON sc.tenant_id = expired.tenant_id AND sc.service_name = expired.service_name
        LEFT JOIN (
            SELECT 
                tenant_id, 
                service_name, 
                AVG(CAST(DATEDIFF(MINUTE, created_at, ISNULL(received_at, processed_at)) AS FLOAT) / 60.0) as avg_hours
            FROM ack_nack_records 
            WHERE direction = 'OUTBOUND' AND CAST(created_at AS DATE) = @target_date
            AND received_at IS NOT NULL
            GROUP BY tenant_id, service_name
        ) avg_time ON sc.tenant_id = avg_time.tenant_id AND sc.service_name = avg_time.service_name
    ) AS source
    ON (target.tenant_id = source.tenant_id 
        AND target.service_name = source.service_name 
        AND target.metric_date = source.metric_date)
    WHEN MATCHED THEN
        UPDATE SET
            total_files_sent = source.total_files_sent,
            total_files_received = source.total_files_received,
            ack_files_generated = source.ack_files_generated,
            nack_files_generated = source.nack_files_generated,
            ack_files_received = source.ack_files_received,
            nack_files_received = source.nack_files_received,
            pending_ack_nack = source.pending_ack_nack,
            failed_ack_nack = source.failed_ack_nack,
            expired_ack_nack = source.expired_ack_nack,
            avg_ack_response_time_hours = source.avg_ack_response_time_hours,
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (tenant_id, service_name, metric_date, total_files_sent, total_files_received,
                ack_files_generated, nack_files_generated, ack_files_received, nack_files_received,
                pending_ack_nack, failed_ack_nack, expired_ack_nack, avg_ack_response_time_hours)
        VALUES (source.tenant_id, source.service_name, source.metric_date, source.total_files_sent,
                source.total_files_received, source.ack_files_generated, source.nack_files_generated,
                source.ack_files_received, source.nack_files_received, source.pending_ack_nack,
                source.failed_ack_nack, source.expired_ack_nack, source.avg_ack_response_time_hours);
END;

-- Add configuration table for ACK/NACK settings per service
CREATE TABLE ack_nack_configuration (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id NVARCHAR(100) NOT NULL,
    service_name NVARCHAR(100) NOT NULL,
    sub_service_name NVARCHAR(100),
    auto_generate_ack BIT DEFAULT 1,
    auto_generate_nack BIT DEFAULT 1,
    auto_send_ack_nack BIT DEFAULT 1,
    ack_nack_timeout_hours INT DEFAULT 24,
    ack_nack_format NVARCHAR(20) DEFAULT 'PIPE_DELIMITED' CHECK (ack_nack_format IN ('PIPE_DELIMITED', 'JSON', 'XML', 'CUSTOM')),
    custom_ack_template NTEXT,
    custom_nack_template NTEXT,
    partner_ack_path NVARCHAR(500),
    partner_nack_path NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Add indexes
CREATE INDEX idx_ack_nack_config_tenant ON ack_nack_configuration(tenant_id);
CREATE INDEX idx_ack_nack_config_service ON ack_nack_configuration(tenant_id, service_name);

-- Add audit trail for ACK/NACK operations
CREATE TABLE ack_nack_audit_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ack_nack_record_id BIGINT NOT NULL,
    action NVARCHAR(50) NOT NULL,
    old_status NVARCHAR(20) CHECK (old_status IN ('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED')),
    new_status NVARCHAR(20) CHECK (new_status IN ('PENDING', 'GENERATED', 'SENT', 'RECEIVED', 'PROCESSED', 'FAILED', 'EXPIRED')),
    user_id NVARCHAR(100),
    details NTEXT,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    CONSTRAINT fk_ack_nack_audit_record 
        FOREIGN KEY (ack_nack_record_id) 
        REFERENCES ack_nack_records(id) 
        ON DELETE CASCADE
);

-- Add indexes for audit log
CREATE INDEX idx_ack_nack_audit_record_id ON ack_nack_audit_log(ack_nack_record_id);
CREATE INDEX idx_ack_nack_audit_created_at ON ack_nack_audit_log(created_at);

-- Create trigger for ACK/NACK status change auditing
CREATE TRIGGER tr_ack_nack_status_audit
    ON ack_nack_records
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    INSERT INTO ack_nack_audit_log (ack_nack_record_id, action, old_status, new_status, details)
    SELECT 
        i.id,
        'STATUS_CHANGE',
        d.status,
        i.status,
        CONCAT('Status changed from ', d.status, ' to ', i.status)
    FROM inserted i
    INNER JOIN deleted d ON i.id = d.id
    WHERE i.status != d.status;
END;

-- Create trigger for updating updated_at in ack_nack_configuration
CREATE TRIGGER tr_ack_nack_config_update
    ON ack_nack_configuration
    AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE ack_nack_configuration 
    SET updated_at = GETDATE()
    WHERE id IN (SELECT id FROM inserted);
END;