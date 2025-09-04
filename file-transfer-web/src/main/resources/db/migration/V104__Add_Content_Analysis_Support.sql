-- Migration to add content analysis and data profiling support
-- Version: V104
-- Description: Add intelligent file content analysis, schema validation, and data profiling capabilities

-- Create content analysis records table
CREATE TABLE content_analysis_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    file_transfer_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    detected_file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI'),
    detected_encoding VARCHAR(50),
    is_binary_file BOOLEAN DEFAULT FALSE,
    is_empty_file BOOLEAN DEFAULT FALSE,
    line_count INT,
    total_characters INT,
    average_line_length DOUBLE,
    max_line_length INT,
    min_line_length INT,
    record_count INT,
    column_count INT,
    structure_analysis TEXT, -- JSON string containing detailed structure analysis
    quality_score DOUBLE,
    quality_issues TEXT, -- JSON array of quality issues
    quality_recommendations TEXT, -- JSON array of recommendations
    processing_recommendations TEXT, -- JSON array of processing recommendations
    analysis_duration_ms BIGINT,
    analysis_version VARCHAR(20) DEFAULT '1.0',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (file_transfer_id) REFERENCES file_transfer_records(id) ON DELETE CASCADE,
    INDEX idx_content_analysis_tenant (tenant_id),
    INDEX idx_content_analysis_file_transfer (file_transfer_id),
    INDEX idx_content_analysis_file_type (tenant_id, detected_file_type),
    INDEX idx_content_analysis_quality (tenant_id, quality_score),
    INDEX idx_content_analysis_binary (tenant_id, is_binary_file),
    INDEX idx_content_analysis_created (tenant_id, created_at),
    UNIQUE KEY uk_content_analysis_file_transfer (file_transfer_id)
);

-- Create schema validation records table
CREATE TABLE schema_validation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    file_transfer_id BIGINT NOT NULL,
    content_analysis_id BIGINT,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    schema_path VARCHAR(1000),
    file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI'),
    validation_passed BOOLEAN DEFAULT FALSE,
    error_count INT DEFAULT 0,
    warning_count INT DEFAULT 0,
    validation_errors TEXT, -- JSON array of validation errors
    validation_warnings TEXT, -- JSON array of validation warnings
    schema_compliance_score DOUBLE,
    validation_duration_ms BIGINT,
    validation_engine VARCHAR(50) DEFAULT 'INTERNAL',
    validation_version VARCHAR(20) DEFAULT '1.0',
    auto_validation BOOLEAN DEFAULT FALSE,
    validation_rules TEXT, -- JSON string containing validation rules used
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (file_transfer_id) REFERENCES file_transfer_records(id) ON DELETE CASCADE,
    FOREIGN KEY (content_analysis_id) REFERENCES content_analysis_records(id) ON DELETE SET NULL,
    INDEX idx_schema_validation_tenant (tenant_id),
    INDEX idx_schema_validation_file_transfer (file_transfer_id),
    INDEX idx_schema_validation_content_analysis (content_analysis_id),
    INDEX idx_schema_validation_passed (tenant_id, validation_passed),
    INDEX idx_schema_validation_file_type (tenant_id, file_type),
    INDEX idx_schema_validation_errors (tenant_id, error_count),
    INDEX idx_schema_validation_compliance (tenant_id, schema_compliance_score),
    INDEX idx_schema_validation_created (tenant_id, created_at),
    UNIQUE KEY uk_schema_validation_file_transfer (file_transfer_id)
);

-- Create data profile records table
CREATE TABLE data_profile_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    file_transfer_id BIGINT NOT NULL,
    content_analysis_id BIGINT,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI'),
    file_size BIGINT,
    record_count INT,
    column_count INT,
    data_statistics TEXT, -- JSON string containing data statistics
    column_profiles TEXT, -- JSON array of column profiles
    quality_insights TEXT, -- JSON array of quality insights
    processing_recommendations TEXT, -- JSON array of processing recommendations
    data_completeness_score DOUBLE,
    data_uniqueness_score DOUBLE,
    data_consistency_score DOUBLE,
    overall_quality_score DOUBLE,
    profiling_duration_ms BIGINT,
    profiling_version VARCHAR(20) DEFAULT '1.0',
    auto_profiling BOOLEAN DEFAULT FALSE,
    profiling_depth VARCHAR(20) DEFAULT 'STANDARD', -- BASIC, STANDARD, DEEP
    sample_size INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (file_transfer_id) REFERENCES file_transfer_records(id) ON DELETE CASCADE,
    FOREIGN KEY (content_analysis_id) REFERENCES content_analysis_records(id) ON DELETE SET NULL,
    INDEX idx_data_profile_tenant (tenant_id),
    INDEX idx_data_profile_file_transfer (file_transfer_id),
    INDEX idx_data_profile_content_analysis (content_analysis_id),
    INDEX idx_data_profile_file_type (tenant_id, file_type),
    INDEX idx_data_profile_quality (tenant_id, overall_quality_score),
    INDEX idx_data_profile_completeness (tenant_id, data_completeness_score),
    INDEX idx_data_profile_record_count (tenant_id, record_count),
    INDEX idx_data_profile_column_count (tenant_id, column_count),
    INDEX idx_data_profile_file_size (tenant_id, file_size),
    INDEX idx_data_profile_created (tenant_id, created_at),
    UNIQUE KEY uk_data_profile_file_transfer (file_transfer_id)
);

-- Create file schema definitions table
CREATE TABLE file_schema_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    schema_name VARCHAR(200) NOT NULL,
    schema_version VARCHAR(20) DEFAULT '1.0',
    file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI') NOT NULL,
    schema_definition TEXT NOT NULL, -- JSON string containing schema definition
    schema_file_path VARCHAR(1000), -- Path to external schema file (XSD, JSON Schema, etc.)
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    validation_enabled BOOLEAN DEFAULT TRUE,
    strict_validation BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_file_schema_tenant (tenant_id),
    INDEX idx_file_schema_service (tenant_id, service_name),
    INDEX idx_file_schema_sub_service (tenant_id, service_name, sub_service_name),
    INDEX idx_file_schema_type (tenant_id, file_type),
    INDEX idx_file_schema_active (tenant_id, is_active),
    INDEX idx_file_schema_default (tenant_id, service_name, is_default),
    UNIQUE KEY uk_file_schema (tenant_id, service_name, sub_service_name, schema_name, schema_version)
);

-- Create content analysis statistics table
CREATE TABLE content_analysis_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    stat_date DATE NOT NULL,
    total_files_analyzed BIGINT DEFAULT 0,
    binary_files BIGINT DEFAULT 0,
    text_files BIGINT DEFAULT 0,
    empty_files BIGINT DEFAULT 0,
    avg_quality_score DOUBLE DEFAULT 0,
    avg_file_size BIGINT DEFAULT 0,
    avg_record_count BIGINT DEFAULT 0,
    avg_column_count DOUBLE DEFAULT 0,
    files_with_issues BIGINT DEFAULT 0,
    avg_analysis_duration_ms BIGINT DEFAULT 0,
    file_type_distribution TEXT, -- JSON object with file type counts
    encoding_distribution TEXT, -- JSON object with encoding counts
    quality_distribution TEXT, -- JSON object with quality score ranges
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_content_analysis_stats (tenant_id, service_name, stat_date),
    INDEX idx_content_analysis_stats_tenant_date (tenant_id, stat_date),
    INDEX idx_content_analysis_stats_service_date (service_name, stat_date),
    INDEX idx_content_analysis_stats_quality (tenant_id, avg_quality_score)
);

-- Create data quality rules table
CREATE TABLE data_quality_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    sub_service_name VARCHAR(100),
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- COMPLETENESS, UNIQUENESS, CONSISTENCY, FORMAT, RANGE
    file_type ENUM('COBOL_FLAT_FILE', 'BINARY_FILE', 'XML', 'JSON', 'CSV', 'FIXED_WIDTH', 'DELIMITED', 'EDI'),
    column_name VARCHAR(200), -- Specific column for column-level rules
    rule_definition TEXT NOT NULL, -- JSON string containing rule parameters
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT TRUE,
    auto_apply BOOLEAN DEFAULT FALSE,
    threshold_value DOUBLE,
    error_message TEXT,
    recommendation TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_data_quality_rules_tenant (tenant_id),
    INDEX idx_data_quality_rules_service (tenant_id, service_name),
    INDEX idx_data_quality_rules_type (rule_type),
    INDEX idx_data_quality_rules_severity (severity),
    INDEX idx_data_quality_rules_active (tenant_id, is_active),
    UNIQUE KEY uk_data_quality_rules (tenant_id, service_name, sub_service_name, rule_name)
);

-- Update file_transfer_records table to link to content analysis
ALTER TABLE file_transfer_records 
ADD COLUMN content_analysis_completed BOOLEAN DEFAULT FALSE,
ADD COLUMN schema_validation_completed BOOLEAN DEFAULT FALSE,
ADD COLUMN data_profiling_completed BOOLEAN DEFAULT FALSE,
ADD COLUMN content_quality_score DOUBLE,
ADD COLUMN schema_compliance_score DOUBLE,
ADD COLUMN data_quality_score DOUBLE;

-- Add indexes for content analysis integration
CREATE INDEX idx_file_transfer_content_analysis ON file_transfer_records(tenant_id, content_analysis_completed);
CREATE INDEX idx_file_transfer_schema_validation ON file_transfer_records(tenant_id, schema_validation_completed);
CREATE INDEX idx_file_transfer_data_profiling ON file_transfer_records(tenant_id, data_profiling_completed);
CREATE INDEX idx_file_transfer_quality_scores ON file_transfer_records(tenant_id, content_quality_score, data_quality_score);

-- Create view for comprehensive file analysis
CREATE VIEW file_analysis_summary AS
SELECT 
    ftr.id as file_transfer_id,
    ftr.tenant_id,
    ftr.service_name,
    ftr.sub_service_name,
    ftr.file_name,
    ftr.file_extension,
    ftr.file_type as declared_file_type,
    ftr.file_size,
    ftr.status as transfer_status,
    ftr.direction,
    ftr.created_at as transfer_created_at,
    
    -- Content Analysis
    car.detected_file_type,
    car.detected_encoding,
    car.is_binary_file,
    car.is_empty_file,
    car.line_count,
    car.record_count as analyzed_record_count,
    car.column_count as analyzed_column_count,
    car.quality_score as content_quality_score,
    car.analysis_duration_ms,
    
    -- Schema Validation
    svr.validation_passed,
    svr.error_count as validation_error_count,
    svr.warning_count as validation_warning_count,
    svr.schema_compliance_score,
    svr.validation_duration_ms,
    
    -- Data Profiling
    dpr.overall_quality_score as data_quality_score,
    dpr.data_completeness_score,
    dpr.data_uniqueness_score,
    dpr.data_consistency_score,
    dpr.profiling_duration_ms,
    dpr.profiling_depth,
    
    -- Analysis Status
    ftr.content_analysis_completed,
    ftr.schema_validation_completed,
    ftr.data_profiling_completed,
    
    -- Overall Scores
    CASE 
        WHEN ftr.content_analysis_completed AND ftr.data_profiling_completed THEN
            (COALESCE(car.quality_score, 0) + COALESCE(dpr.overall_quality_score, 0)) / 2
        WHEN ftr.content_analysis_completed THEN car.quality_score
        WHEN ftr.data_profiling_completed THEN dpr.overall_quality_score
        ELSE NULL
    END as combined_quality_score
    
FROM file_transfer_records ftr
LEFT JOIN content_analysis_records car ON ftr.id = car.file_transfer_id
LEFT JOIN schema_validation_records svr ON ftr.id = svr.file_transfer_id
LEFT JOIN data_profile_records dpr ON ftr.id = dpr.file_transfer_id;

-- Create stored procedure to update content analysis statistics
DELIMITER //
CREATE PROCEDURE UpdateContentAnalysisStatistics(IN target_date DATE, IN target_tenant_id VARCHAR(100))
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_service_name VARCHAR(100);
    
    DECLARE service_cursor CURSOR FOR
        SELECT DISTINCT tenant_id, service_name
        FROM file_transfer_records 
        WHERE (target_tenant_id IS NULL OR tenant_id = target_tenant_id)
        AND DATE(created_at) = target_date;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN service_cursor;
    
    read_loop: LOOP
        FETCH service_cursor INTO v_tenant_id, v_service_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        INSERT INTO content_analysis_statistics (
            tenant_id, service_name, stat_date,
            total_files_analyzed, binary_files, text_files, empty_files,
            avg_quality_score, avg_file_size, avg_record_count, avg_column_count,
            files_with_issues, avg_analysis_duration_ms,
            file_type_distribution, encoding_distribution, quality_distribution
        )
        SELECT 
            v_tenant_id,
            v_service_name,
            target_date,
            COUNT(*) as total_files_analyzed,
            SUM(CASE WHEN car.is_binary_file = TRUE THEN 1 ELSE 0 END) as binary_files,
            SUM(CASE WHEN car.is_binary_file = FALSE THEN 1 ELSE 0 END) as text_files,
            SUM(CASE WHEN car.is_empty_file = TRUE THEN 1 ELSE 0 END) as empty_files,
            AVG(COALESCE(car.quality_score, 0)) as avg_quality_score,
            AVG(COALESCE(car.file_size, 0)) as avg_file_size,
            AVG(COALESCE(car.record_count, 0)) as avg_record_count,
            AVG(COALESCE(car.column_count, 0)) as avg_column_count,
            SUM(CASE WHEN car.quality_score < 70 THEN 1 ELSE 0 END) as files_with_issues,
            AVG(COALESCE(car.analysis_duration_ms, 0)) as avg_analysis_duration_ms,
            CONCAT('{',
                GROUP_CONCAT(
                    DISTINCT CONCAT('"', car.detected_file_type, '":',
                    (SELECT COUNT(*) FROM content_analysis_records car2 
                     WHERE car2.tenant_id = v_tenant_id 
                     AND car2.detected_file_type = car.detected_file_type
                     AND DATE(car2.created_at) = target_date))
                ),
            '}') as file_type_distribution,
            CONCAT('{',
                GROUP_CONCAT(
                    DISTINCT CONCAT('"', COALESCE(car.detected_encoding, 'UNKNOWN'), '":',
                    (SELECT COUNT(*) FROM content_analysis_records car3 
                     WHERE car3.tenant_id = v_tenant_id 
                     AND car3.detected_encoding = car.detected_encoding
                     AND DATE(car3.created_at) = target_date))
                ),
            '}') as encoding_distribution,
            '{"excellent":0,"good":0,"fair":0,"poor":0}' as quality_distribution -- Simplified for now
        FROM file_transfer_records ftr
        INNER JOIN content_analysis_records car ON ftr.id = car.file_transfer_id
        WHERE ftr.tenant_id = v_tenant_id 
        AND ftr.service_name = v_service_name 
        AND DATE(ftr.created_at) = target_date
        GROUP BY v_tenant_id, v_service_name
        ON DUPLICATE KEY UPDATE
            total_files_analyzed = VALUES(total_files_analyzed),
            binary_files = VALUES(binary_files),
            text_files = VALUES(text_files),
            empty_files = VALUES(empty_files),
            avg_quality_score = VALUES(avg_quality_score),
            avg_file_size = VALUES(avg_file_size),
            avg_record_count = VALUES(avg_record_count),
            avg_column_count = VALUES(avg_column_count),
            files_with_issues = VALUES(files_with_issues),
            avg_analysis_duration_ms = VALUES(avg_analysis_duration_ms),
            file_type_distribution = VALUES(file_type_distribution),
            encoding_distribution = VALUES(encoding_distribution),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE service_cursor;
END//
DELIMITER ;

-- Create event to automatically update content analysis statistics daily
CREATE EVENT evt_update_content_analysis_statistics
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 6 HOUR
DO
    CALL UpdateContentAnalysisStatistics(CURDATE() - INTERVAL 1 DAY, NULL);

-- Insert default schema definitions for common file types
INSERT INTO file_schema_definitions (
    tenant_id, service_name, schema_name, file_type, schema_definition, is_default, description
) VALUES 
(
    'default', 'CUSTOMER_DATA', 'Standard CSV Schema', 'CSV',
    '{
        "hasHeader": true,
        "delimiter": ",",
        "columns": [
            {"name": "customer_id", "dataType": "INTEGER", "required": true},
            {"name": "customer_name", "dataType": "STRING", "required": true, "maxLength": 100},
            {"name": "email", "dataType": "EMAIL", "required": false},
            {"name": "phone", "dataType": "PHONE", "required": false},
            {"name": "created_date", "dataType": "DATE", "required": true}
        ]
    }',
    TRUE,
    'Standard schema for customer data CSV files'
),
(
    'default', 'TRANSACTION_DATA', 'Transaction JSON Schema', 'JSON',
    '{
        "type": "object",
        "required": ["transaction_id", "amount", "timestamp"],
        "properties": {
            "transaction_id": {"type": "string"},
            "amount": {"type": "number", "minimum": 0},
            "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
            "timestamp": {"type": "string", "format": "date-time"}
        }
    }',
    TRUE,
    'Standard schema for transaction JSON files'
),
(
    'default', 'LOG_DATA', 'Log File Schema', 'COBOL_FLAT_FILE',
    '{
        "recordLength": 200,
        "fields": [
            {"name": "timestamp", "startPosition": 1, "length": 19, "dataType": "DATETIME", "required": true},
            {"name": "log_level", "startPosition": 21, "length": 10, "dataType": "ALPHANUMERIC", "required": true},
            {"name": "message", "startPosition": 32, "length": 168, "dataType": "ALPHANUMERIC", "required": false}
        ]
    }',
    TRUE,
    'Standard schema for log files in fixed-width format'
);

-- Insert default data quality rules
INSERT INTO data_quality_rules (
    tenant_id, service_name, rule_name, rule_type, rule_definition, severity, auto_apply, error_message, recommendation
) VALUES 
(
    'default', 'CUSTOMER_DATA', 'Email Format Validation', 'FORMAT',
    '{"pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$", "column": "email"}',
    'MEDIUM', TRUE,
    'Invalid email format detected',
    'Ensure email addresses follow standard format (user@domain.com)'
),
(
    'default', 'CUSTOMER_DATA', 'Completeness Check', 'COMPLETENESS',
    '{"minCompleteness": 95.0, "requiredColumns": ["customer_id", "customer_name"]}',
    'HIGH', TRUE,
    'Required fields have missing values',
    'Ensure all required fields are populated'
),
(
    'default', 'TRANSACTION_DATA', 'Amount Range Check', 'RANGE',
    '{"column": "amount", "minValue": 0.01, "maxValue": 1000000.00}',
    'HIGH', TRUE,
    'Transaction amount outside valid range',
    'Verify transaction amounts are within business rules'
),
(
    'default', 'ALL', 'File Size Check', 'RANGE',
    '{"maxFileSize": 104857600, "unit": "bytes"}',
    'MEDIUM', FALSE,
    'File size exceeds recommended limit',
    'Consider splitting large files for better processing performance'
);

-- Create triggers for automatic content analysis tracking
DELIMITER //
CREATE TRIGGER tr_file_transfer_content_analysis_update
    AFTER UPDATE ON file_transfer_records
    FOR EACH ROW
BEGIN
    -- Update content analysis completion status when file processing completes
    IF OLD.status != 'COMPLETED' AND NEW.status = 'COMPLETED' THEN
        -- Mark for content analysis if not already done
        IF NEW.content_analysis_completed = FALSE THEN
            INSERT INTO content_analysis_records (
                tenant_id, file_transfer_id, file_name, file_path, file_size
            ) VALUES (
                NEW.tenant_id, NEW.id, NEW.file_name, NEW.source_path, NEW.file_size
            )
            ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
        END IF;
    END IF;
END//
DELIMITER ;

-- Add comments to new tables and columns
ALTER TABLE content_analysis_records COMMENT = 'Stores intelligent file content analysis results including type detection and quality assessment';
ALTER TABLE schema_validation_records COMMENT = 'Stores schema validation results for structured files (CSV, JSON, XML)';
ALTER TABLE data_profile_records COMMENT = 'Stores comprehensive data profiling and statistical analysis results';
ALTER TABLE file_schema_definitions COMMENT = 'Defines validation schemas for different file types and services';
ALTER TABLE content_analysis_statistics COMMENT = 'Daily aggregated statistics for content analysis operations';
ALTER TABLE data_quality_rules COMMENT = 'Configurable data quality rules for automated validation';

-- Add column comments
ALTER TABLE file_transfer_records 
MODIFY COLUMN content_analysis_completed BOOLEAN DEFAULT FALSE COMMENT 'Indicates if content analysis has been completed for this file',
MODIFY COLUMN schema_validation_completed BOOLEAN DEFAULT FALSE COMMENT 'Indicates if schema validation has been completed for this file',
MODIFY COLUMN data_profiling_completed BOOLEAN DEFAULT FALSE COMMENT 'Indicates if data profiling has been completed for this file',
MODIFY COLUMN content_quality_score DOUBLE COMMENT 'Overall content quality score from analysis (0-100)',
MODIFY COLUMN schema_compliance_score DOUBLE COMMENT 'Schema compliance score from validation (0-100)',
MODIFY COLUMN data_quality_score DOUBLE COMMENT 'Data quality score from profiling (0-100)';

-- Create indexes for better performance
CREATE INDEX idx_content_analysis_quality_score ON content_analysis_records(tenant_id, quality_score DESC);
CREATE INDEX idx_schema_validation_compliance_score ON schema_validation_records(tenant_id, schema_compliance_score DESC);
CREATE INDEX idx_data_profile_quality_score ON data_profile_records(tenant_id, overall_quality_score DESC);

-- Create materialized view for analysis dashboard (if supported)
CREATE TABLE analysis_dashboard_cache AS
SELECT 
    tenant_id,
    COUNT(*) as total_files,
    SUM(CASE WHEN content_analysis_completed = TRUE THEN 1 ELSE 0 END) as analyzed_files,
    SUM(CASE WHEN schema_validation_completed = TRUE THEN 1 ELSE 0 END) as validated_files,
    SUM(CASE WHEN data_profiling_completed = TRUE THEN 1 ELSE 0 END) as profiled_files,
    AVG(content_quality_score) as avg_content_quality,
    AVG(schema_compliance_score) as avg_schema_compliance,
    AVG(data_quality_score) as avg_data_quality,
    DATE(created_at) as analysis_date
FROM file_transfer_records 
WHERE created_at >= CURDATE() - INTERVAL 30 DAY
GROUP BY tenant_id, DATE(created_at);

-- Add primary key and indexes to cache table
ALTER TABLE analysis_dashboard_cache 
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST,
ADD INDEX idx_analysis_cache_tenant_date (tenant_id, analysis_date),
ADD INDEX idx_analysis_cache_date (analysis_date);

-- Create procedure to refresh dashboard cache
DELIMITER //
CREATE PROCEDURE RefreshAnalysisDashboardCache()
BEGIN
    TRUNCATE TABLE analysis_dashboard_cache;
    
    INSERT INTO analysis_dashboard_cache (
        tenant_id, total_files, analyzed_files, validated_files, profiled_files,
        avg_content_quality, avg_schema_compliance, avg_data_quality, analysis_date
    )
    SELECT 
        tenant_id,
        COUNT(*) as total_files,
        SUM(CASE WHEN content_analysis_completed = TRUE THEN 1 ELSE 0 END) as analyzed_files,
        SUM(CASE WHEN schema_validation_completed = TRUE THEN 1 ELSE 0 END) as validated_files,
        SUM(CASE WHEN data_profiling_completed = TRUE THEN 1 ELSE 0 END) as profiled_files,
        AVG(content_quality_score) as avg_content_quality,
        AVG(schema_compliance_score) as avg_schema_compliance,
        AVG(data_quality_score) as avg_data_quality,
        DATE(created_at) as analysis_date
    FROM file_transfer_records 
    WHERE created_at >= CURDATE() - INTERVAL 30 DAY
    GROUP BY tenant_id, DATE(created_at);
END//
DELIMITER ;

-- Create event to refresh dashboard cache every hour
CREATE EVENT evt_refresh_analysis_dashboard_cache
ON SCHEDULE EVERY 1 HOUR
DO
    CALL RefreshAnalysisDashboardCache();