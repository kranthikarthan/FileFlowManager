-- Database migration script for Schema Reuse and EOT Validation functionality
-- Created: 2024-01-XX
-- Purpose: Add tables for shared schemas, schema usage mappings, and daily file count tracking

-- ============================================================================
-- 1. SHARED SCHEMAS TABLE
-- ============================================================================
CREATE TABLE shared_schemas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(100) NOT NULL,
    schema_name VARCHAR(255) NOT NULL,
    schema_version VARCHAR(50) NOT NULL,
    schema_type VARCHAR(50) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    schema_definition TEXT,
    description VARCHAR(1000),
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    eot_count_field_path VARCHAR(500),
    supports_count_validation BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Indexes
    INDEX idx_shared_schemas_tenant (tenant_id),
    INDEX idx_shared_schemas_active (is_active),
    INDEX idx_shared_schemas_global (is_global),
    INDEX idx_shared_schemas_file_type (file_type),
    INDEX idx_shared_schemas_schema_type (schema_type),
    INDEX idx_shared_schemas_count_validation (supports_count_validation),
    INDEX idx_shared_schemas_usage (usage_count DESC),
    INDEX idx_shared_schemas_name_version (tenant_id, schema_name, schema_version),
    
    -- Constraints
    CONSTRAINT uk_shared_schemas_name_version UNIQUE (tenant_id, schema_name, schema_version)
);

-- ============================================================================
-- 2. SHARED SCHEMA TAGS TABLE
-- ============================================================================
CREATE TABLE shared_schema_tags (
    schema_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    
    -- Indexes
    INDEX idx_schema_tags_schema_id (schema_id),
    INDEX idx_schema_tags_tag (tag),
    
    -- Foreign Keys
    FOREIGN KEY (schema_id) REFERENCES shared_schemas(id) ON DELETE CASCADE,
    
    -- Constraints
    PRIMARY KEY (schema_id, tag)
);

-- ============================================================================
-- 3. SCHEMA USAGE MAPPINGS TABLE
-- ============================================================================
CREATE TABLE schema_usage_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sub_service_config_id BIGINT NOT NULL,
    shared_schema_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    usage_type VARCHAR(20) NOT NULL DEFAULT 'VALIDATION',
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    validation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP NULL,
    custom_config TEXT,
    
    -- Indexes
    INDEX idx_usage_mappings_sub_service (sub_service_config_id),
    INDEX idx_usage_mappings_schema (shared_schema_id),
    INDEX idx_usage_mappings_direction (direction),
    INDEX idx_usage_mappings_primary (is_primary),
    INDEX idx_usage_mappings_enabled (validation_enabled),
    INDEX idx_usage_mappings_sub_service_direction (sub_service_config_id, direction),
    INDEX idx_usage_mappings_usage_count (usage_count DESC),
    
    -- Foreign Keys
    FOREIGN KEY (sub_service_config_id) REFERENCES sub_service_configurations(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_schema_id) REFERENCES shared_schemas(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_usage_mappings_unique UNIQUE (sub_service_config_id, shared_schema_id, direction)
);

-- ============================================================================
-- 4. DAILY FILE COUNT TRACKER TABLE
-- ============================================================================
CREATE TABLE daily_file_count_tracker (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    sub_service_name VARCHAR(255) NOT NULL,
    processing_date DATE NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    expected_count INT,
    actual_count INT NOT NULL DEFAULT 0,
    sot_received BOOLEAN NOT NULL DEFAULT FALSE,
    eot_received BOOLEAN NOT NULL DEFAULT FALSE,
    eot_count_value INT,
    eot_field_path VARCHAR(500),
    validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    validation_message VARCHAR(1000),
    discrepancy_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    validated_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_file_count_tracker_tenant (tenant_id),
    INDEX idx_file_count_tracker_date (processing_date),
    INDEX idx_file_count_tracker_service (service_name, sub_service_name),
    INDEX idx_file_count_tracker_status (validation_status),
    INDEX idx_file_count_tracker_discrepancy (discrepancy_count),
    INDEX idx_file_count_tracker_tenant_date (tenant_id, processing_date),
    INDEX idx_file_count_tracker_validated (validated_at),
    INDEX idx_file_count_tracker_eot_received (eot_received),
    INDEX idx_file_count_tracker_sot_received (sot_received),
    
    -- Constraints
    CONSTRAINT uk_file_count_tracker_unique UNIQUE (tenant_id, service_name, sub_service_name, processing_date, file_type, direction)
);

-- ============================================================================
-- 5. ADD MISSING COLUMNS TO EXISTING TABLES (if needed)
-- ============================================================================

-- Add schema usage tracking columns to sub_service_configurations if they don't exist
ALTER TABLE sub_service_configurations 
ADD COLUMN IF NOT EXISTS start_marker_prefix VARCHAR(50) DEFAULT 'SOT_',
ADD COLUMN IF NOT EXISTS end_marker_prefix VARCHAR(50) DEFAULT 'EOT_';

-- ============================================================================
-- 6. SAMPLE DATA INSERTS
-- ============================================================================

-- Insert sample global schemas
INSERT INTO shared_schemas (
    tenant_id, schema_name, schema_version, schema_type, file_type, 
    schema_definition, description, is_global, supports_count_validation, 
    eot_count_field_path, created_by
) VALUES 
(
    'GLOBAL', 'Standard_JSON_EOT', '1.0', 'JSON_SCHEMA', 'JSON',
    '{"type":"object","properties":{"header":{"type":"object","properties":{"recordCount":{"type":"integer"},"timestamp":{"type":"string"}}},"required":["header"]}}',
    'Standard JSON End-of-Day schema with record count validation',
    TRUE, TRUE, 'header.recordCount', 'SYSTEM'
),
(
    'GLOBAL', 'Standard_XML_EOT', '1.0', 'XML_SCHEMA', 'XML',
    '<?xml version="1.0"?><xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"><xsd:element name="eot"><xsd:complexType><xsd:sequence><xsd:element name="recordCount" type="xsd:int"/><xsd:element name="timestamp" type="xsd:dateTime"/></xsd:sequence></xsd:complexType></xsd:element></xsd:schema>',
    'Standard XML End-of-Day schema with record count validation',
    TRUE, TRUE, '/eot/recordCount', 'SYSTEM'
),
(
    'GLOBAL', 'COBOL_Payment_File', '1.0', 'COBOL_COPYBOOK', 'COBOL_FLAT_FILE',
    '01 PAYMENT-RECORD.\n   05 RECORD-TYPE        PIC X(2).\n   05 PAYMENT-ID         PIC 9(10).\n   05 AMOUNT             PIC 9(8)V99.\n   05 CURRENCY           PIC X(3).\n   05 PAYMENT-DATE       PIC 9(8).\n   05 FILLER             PIC X(50).',
    'Standard COBOL copybook for payment file processing',
    TRUE, FALSE, NULL, 'SYSTEM'
);

-- ============================================================================
-- 7. PERFORMANCE OPTIMIZATION INDEXES
-- ============================================================================

-- Additional composite indexes for common query patterns
CREATE INDEX idx_shared_schemas_tenant_type_active ON shared_schemas (tenant_id, file_type, is_active);
CREATE INDEX idx_shared_schemas_global_type_active ON shared_schemas (is_global, file_type, is_active);
CREATE INDEX idx_usage_mappings_tenant_validation ON schema_usage_mappings sm 
    JOIN sub_service_configurations ssc ON sm.sub_service_config_id = ssc.id (ssc.tenant_id, sm.validation_enabled);
CREATE INDEX idx_file_count_tracker_tenant_status_date ON daily_file_count_tracker (tenant_id, validation_status, processing_date DESC);

-- ============================================================================
-- 8. SAMPLE USAGE MAPPING DATA
-- ============================================================================

-- Note: Actual usage mappings would be created through the application
-- when users configure schemas for their subservices

-- ============================================================================
-- 9. DATA MIGRATION SCRIPT (if upgrading from existing system)
-- ============================================================================

-- Migrate existing file_schemas to shared_schemas (if applicable)
-- INSERT INTO shared_schemas (tenant_id, schema_name, schema_version, schema_type, file_type, schema_definition, description, created_by)
-- SELECT 
--     tenant_id, 
--     schema_name, 
--     '1.0' as schema_version,
--     schema_type,
--     file_type,
--     schema_definition,
--     description,
--     'MIGRATION' as created_by
-- FROM file_schemas 
-- WHERE NOT EXISTS (
--     SELECT 1 FROM shared_schemas ss 
--     WHERE ss.tenant_id = file_schemas.tenant_id 
--     AND ss.schema_name = file_schemas.schema_name
-- );

-- ============================================================================
-- 10. CLEANUP AND OPTIMIZATION
-- ============================================================================

-- Update table statistics for query optimization
ANALYZE TABLE shared_schemas;
ANALYZE TABLE shared_schema_tags;
ANALYZE TABLE schema_usage_mappings;
ANALYZE TABLE daily_file_count_tracker;

-- ============================================================================
-- 11. VALIDATION QUERIES
-- ============================================================================

-- Verify table creation
SELECT 
    table_name, 
    table_rows, 
    data_length, 
    index_length
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name IN ('shared_schemas', 'shared_schema_tags', 'schema_usage_mappings', 'daily_file_count_tracker');

-- Verify indexes
SELECT 
    table_name, 
    index_name, 
    column_name, 
    seq_in_index
FROM information_schema.statistics 
WHERE table_schema = DATABASE() 
AND table_name IN ('shared_schemas', 'shared_schema_tags', 'schema_usage_mappings', 'daily_file_count_tracker')
ORDER BY table_name, index_name, seq_in_index;

COMMIT;