-- Critical Bug Fix: Field Name Inconsistencies
-- Standardize field names across all tables to use serviceName/subServiceName

-- Fix file_transfer_records table
ALTER TABLE file_transfer_records 
ADD COLUMN service_name VARCHAR(255);

ALTER TABLE file_transfer_records 
ADD COLUMN sub_service_name VARCHAR(255);

-- Copy data from old columns to new columns
UPDATE file_transfer_records 
SET service_name = service_type, 
    sub_service_name = sub_service_type;

-- Add NOT NULL constraint to service_name
ALTER TABLE file_transfer_records 
ALTER COLUMN service_name SET NOT NULL;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_file_transfer_records_service_name 
ON file_transfer_records (tenant_id, service_name);

CREATE INDEX IF NOT EXISTS idx_file_transfer_records_sub_service_name 
ON file_transfer_records (tenant_id, service_name, sub_service_name);

CREATE INDEX IF NOT EXISTS idx_file_transfer_records_status_processing 
ON file_transfer_records (tenant_id, service_name, status) 
WHERE status IN ('PENDING', 'PROCESSING', 'PAUSED');

-- Fix file_schemas table
ALTER TABLE file_schemas 
ADD COLUMN service_name VARCHAR(255);

-- Copy data from service_type to service_name
UPDATE file_schemas 
SET service_name = service_type;

-- Add NOT NULL constraint
ALTER TABLE file_schemas 
ALTER COLUMN service_name SET NOT NULL;

-- Add index
CREATE INDEX IF NOT EXISTS idx_file_schemas_service_name 
ON file_schemas (tenant_id, service_name);

-- Add missing columns to file_transfer_records for processing control
ALTER TABLE file_transfer_records 
ADD COLUMN processing_start_time TIMESTAMP;

ALTER TABLE file_transfer_records 
ADD COLUMN processing_end_time TIMESTAMP;

ALTER TABLE file_transfer_records 
ADD COLUMN validation_result TEXT;

ALTER TABLE file_transfer_records 
ADD COLUMN metadata TEXT;

-- Add processing control audit table
CREATE TABLE IF NOT EXISTS processing_control_audit (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(255),
    sub_service_name VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_processing_audit_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Add indexes for audit table
CREATE INDEX idx_processing_audit_tenant_timestamp 
ON processing_control_audit (tenant_id, timestamp DESC);

CREATE INDEX idx_processing_audit_action 
ON processing_control_audit (action);

CREATE INDEX idx_processing_audit_user 
ON processing_control_audit (user_id);

-- Add holiday edge case tracking table
CREATE TABLE IF NOT EXISTS holiday_edge_cases (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    holiday_id BIGINT,
    edge_case_type VARCHAR(50) NOT NULL,
    holiday_date DATE NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    message TEXT,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    
    CONSTRAINT fk_holiday_edge_cases_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    
    CONSTRAINT fk_holiday_edge_cases_holiday 
    FOREIGN KEY (holiday_id) REFERENCES holidays(id) ON DELETE CASCADE
);

-- Add indexes for edge case table
CREATE INDEX idx_holiday_edge_cases_tenant_date 
ON holiday_edge_cases (tenant_id, holiday_date);

CREATE INDEX idx_holiday_edge_cases_type 
ON holiday_edge_cases (edge_case_type);

CREATE INDEX idx_holiday_edge_cases_unresolved 
ON holiday_edge_cases (resolved) WHERE resolved = FALSE;

-- Create function to detect holiday edge cases
CREATE OR REPLACE FUNCTION detect_holiday_edge_cases()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if holiday falls on Sunday
    IF EXTRACT(DOW FROM NEW.holiday_date) = 0 THEN
        INSERT INTO holiday_edge_cases (
            tenant_id, 
            holiday_id, 
            edge_case_type, 
            holiday_date, 
            day_of_week, 
            severity, 
            message
        ) VALUES (
            NEW.tenant_id,
            NEW.id,
            'HOLIDAY_SUNDAY_COLLISION',
            NEW.holiday_date,
            'SUNDAY',
            'WARNING',
            'Holiday "' || NEW.holiday_name || '" falls on Sunday - potential processing conflicts'
        );
    END IF;
    
    -- Check for adjacent holidays (within 1 day)
    IF EXISTS (
        SELECT 1 FROM holidays h 
        WHERE h.tenant_id = NEW.tenant_id 
        AND h.id != NEW.id 
        AND ABS(EXTRACT(EPOCH FROM (h.holiday_date - NEW.holiday_date)) / 86400) <= 1
    ) THEN
        INSERT INTO holiday_edge_cases (
            tenant_id, 
            holiday_id, 
            edge_case_type, 
            holiday_date, 
            day_of_week, 
            severity, 
            message
        ) VALUES (
            NEW.tenant_id,
            NEW.id,
            'ADJACENT_HOLIDAYS',
            NEW.holiday_date,
            TO_CHAR(NEW.holiday_date, 'Day'),
            'INFO',
            'Holiday "' || NEW.holiday_name || '" is adjacent to other holidays'
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for holiday edge case detection
DROP TRIGGER IF EXISTS trigger_detect_holiday_edge_cases ON holidays;
CREATE TRIGGER trigger_detect_holiday_edge_cases
    AFTER INSERT OR UPDATE ON holidays
    FOR EACH ROW
    EXECUTE FUNCTION detect_holiday_edge_cases();

-- Add processing control status tracking
CREATE TABLE IF NOT EXISTS processing_status_tracking (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(255),
    sub_service_name VARCHAR(255),
    processing_level VARCHAR(20) NOT NULL, -- TENANT, SERVICE, SUBSERVICE, FILE
    entity_id VARCHAR(100), -- ID of the entity being controlled
    status VARCHAR(20) NOT NULL, -- ACTIVE, STOPPED, PAUSED, ERROR
    previous_status VARCHAR(20),
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    metadata TEXT, -- JSON metadata
    
    CONSTRAINT fk_processing_status_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id) ON DELETE CASCADE
);

-- Add indexes for processing status tracking
CREATE INDEX idx_processing_status_tenant_level 
ON processing_status_tracking (tenant_id, processing_level);

CREATE INDEX idx_processing_status_current 
ON processing_status_tracking (tenant_id, service_name, sub_service_name, changed_at DESC);

CREATE INDEX idx_processing_status_entity 
ON processing_status_tracking (entity_id, processing_level);

-- Create view for current processing status
CREATE OR REPLACE VIEW current_processing_status AS
SELECT DISTINCT ON (tenant_id, service_name, sub_service_name, processing_level, entity_id)
    tenant_id,
    service_name,
    sub_service_name,
    processing_level,
    entity_id,
    status,
    changed_by,
    changed_at,
    reason
FROM processing_status_tracking
ORDER BY tenant_id, service_name, sub_service_name, processing_level, entity_id, changed_at DESC;

-- Insert initial processing status for existing data
INSERT INTO processing_status_tracking (
    tenant_id, 
    service_name, 
    processing_level, 
    entity_id, 
    status, 
    changed_by, 
    reason
)
SELECT 
    tenant_id,
    service_name,
    'SERVICE',
    id::text,
    CASE WHEN enabled THEN 'ACTIVE' ELSE 'STOPPED' END,
    'system',
    'Initial status from migration'
FROM service_configurations;

INSERT INTO processing_status_tracking (
    tenant_id, 
    service_name, 
    sub_service_name,
    processing_level, 
    entity_id, 
    status, 
    changed_by, 
    reason
)
SELECT 
    tenant_id,
    service_name,
    sub_service_name,
    'SUBSERVICE',
    id::text,
    CASE WHEN enabled THEN 'ACTIVE' ELSE 'STOPPED' END,
    'system',
    'Initial status from migration'
FROM sub_service_configurations;

-- Add comments for documentation
COMMENT ON TABLE processing_control_audit IS 'Audit trail for all processing control actions';
COMMENT ON TABLE holiday_edge_cases IS 'Tracks holiday edge cases like Sunday collisions';
COMMENT ON TABLE processing_status_tracking IS 'Tracks processing status changes at all levels';
COMMENT ON VIEW current_processing_status IS 'Current processing status for all entities';

-- Grant permissions for application roles
GRANT SELECT, INSERT, UPDATE ON processing_control_audit TO file_transfer_app;
GRANT SELECT, INSERT, UPDATE ON holiday_edge_cases TO file_transfer_app;
GRANT SELECT, INSERT, UPDATE ON processing_status_tracking TO file_transfer_app;
GRANT SELECT ON current_processing_status TO file_transfer_app;

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_file_transfer_records_processing_times 
ON file_transfer_records (processing_start_time, processing_end_time) 
WHERE processing_start_time IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_file_transfer_records_validation 
ON file_transfer_records (tenant_id, service_name) 
WHERE validation_result IS NOT NULL;

-- Add database constraints for data integrity
ALTER TABLE file_transfer_records 
ADD CONSTRAINT chk_processing_times 
CHECK (processing_end_time IS NULL OR processing_start_time IS NULL OR processing_end_time >= processing_start_time);

ALTER TABLE holiday_edge_cases 
ADD CONSTRAINT chk_edge_case_type 
CHECK (edge_case_type IN ('HOLIDAY_SUNDAY_COLLISION', 'ADJACENT_HOLIDAYS', 'HOLIDAY_WEEKEND_CONFLICT', 'MULTIPLE_HOLIDAYS_SAME_DATE'));

ALTER TABLE processing_status_tracking 
ADD CONSTRAINT chk_processing_level 
CHECK (processing_level IN ('TENANT', 'SERVICE', 'SUBSERVICE', 'FILE', 'BATCH'));

ALTER TABLE processing_status_tracking 
ADD CONSTRAINT chk_processing_status 
CHECK (status IN ('ACTIVE', 'STOPPED', 'PAUSED', 'ERROR', 'MAINTENANCE', 'DEGRADED'));

-- Update statistics for query optimization
ANALYZE file_transfer_records;
ANALYZE processing_control_audit;
ANALYZE holiday_edge_cases;
ANALYZE processing_status_tracking;