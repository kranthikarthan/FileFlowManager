-- Schema Management Tables for File Transfer System
-- This script creates tables for managing file schemas and validation rules

-- File Schema Definition Table
CREATE TABLE file_schemas (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    schema_name VARCHAR(200) NOT NULL,
    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    schema_type VARCHAR(50) NOT NULL, -- JSON, XML, CSV, FIXED_WIDTH, etc.
    schema_definition TEXT NOT NULL, -- JSON schema definition
    is_active BIT NOT NULL DEFAULT 1,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by VARCHAR(100),
    updated_at DATETIME2,
    description NVARCHAR(500),
    
    CONSTRAINT UK_file_schemas_tenant_service_version UNIQUE (tenant_id, service_type, schema_version)
);

-- Schema Validation Rules Table
CREATE TABLE schema_validation_rules (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- REGEX, JSON_SCHEMA, XML_SCHEMA, CUSTOM
    rule_definition TEXT NOT NULL,
    rule_order INT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    error_message NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2,
    
    CONSTRAINT FK_schema_validation_rules_schema_id FOREIGN KEY (schema_id) REFERENCES file_schemas(id) ON DELETE CASCADE
);

-- Schema Field Definitions Table
CREATE TABLE schema_fields (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(50) NOT NULL, -- STRING, INTEGER, DECIMAL, DATE, BOOLEAN, etc.
    field_length INT,
    is_required BIT NOT NULL DEFAULT 0,
    is_unique BIT NOT NULL DEFAULT 0,
    default_value NVARCHAR(500),
    validation_regex NVARCHAR(500),
    field_order INT NOT NULL DEFAULT 0,
    description NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2,
    
    CONSTRAINT FK_schema_fields_schema_id FOREIGN KEY (schema_id) REFERENCES file_schemas(id) ON DELETE CASCADE
);

-- Schema Usage Tracking Table
CREATE TABLE schema_usage_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    file_transfer_id BIGINT,
    validation_result VARCHAR(20) NOT NULL, -- PASSED, FAILED, ERROR
    validation_details TEXT,
    validation_timestamp DATETIME2 NOT NULL DEFAULT GETDATE(),
    file_name VARCHAR(500),
    file_size BIGINT,
    
    CONSTRAINT FK_schema_usage_log_schema_id FOREIGN KEY (schema_id) REFERENCES file_schemas(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IX_file_schemas_tenant_service ON file_schemas (tenant_id, service_type);
CREATE INDEX IX_file_schemas_active ON file_schemas (is_active);
CREATE INDEX IX_schema_validation_rules_schema ON schema_validation_rules (schema_id);
CREATE INDEX IX_schema_fields_schema ON schema_fields (schema_id);
CREATE INDEX IX_schema_usage_log_schema ON schema_usage_log (schema_id);
CREATE INDEX IX_schema_usage_log_timestamp ON schema_usage_log (validation_timestamp);

-- Insert sample schemas for common file types
INSERT INTO file_schemas (tenant_id, service_type, schema_name, schema_version, schema_type, schema_definition, created_by, description)
VALUES 
('default', 'service1', 'CSV Data Schema', '1.0', 'CSV', 
'{
  "type": "csv",
  "delimiter": ",",
  "hasHeader": true,
  "fields": [
    {"name": "transaction_id", "type": "STRING", "required": true, "length": 50},
    {"name": "amount", "type": "DECIMAL", "required": true, "precision": 10, "scale": 2},
    {"name": "currency", "type": "STRING", "required": true, "length": 3},
    {"name": "transaction_date", "type": "DATE", "required": true, "format": "yyyy-MM-dd"},
    {"name": "status", "type": "STRING", "required": true, "length": 20}
  ]
}', 'system', 'Standard CSV schema for financial transactions'),

('default', 'service2', 'JSON Data Schema', '1.0', 'JSON', 
'{
  "type": "json",
  "schema": {
    "type": "object",
    "properties": {
      "order_id": {"type": "string", "maxLength": 50},
      "customer_id": {"type": "string", "maxLength": 50},
      "items": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "product_id": {"type": "string"},
            "quantity": {"type": "integer", "minimum": 1},
            "price": {"type": "number", "minimum": 0}
          },
          "required": ["product_id", "quantity", "price"]
        }
      },
      "total_amount": {"type": "number", "minimum": 0}
    },
    "required": ["order_id", "customer_id", "items", "total_amount"]
  }
}', 'system', 'JSON schema for order data'),

('default', 'service3', 'Fixed Width Schema', '1.0', 'FIXED_WIDTH', 
'{
  "type": "fixed_width",
  "fields": [
    {"name": "record_type", "type": "STRING", "start": 1, "length": 2, "required": true},
    {"name": "account_number", "type": "STRING", "start": 3, "length": 12, "required": true},
    {"name": "balance", "type": "DECIMAL", "start": 15, "length": 15, "precision": 10, "scale": 2, "required": true},
    {"name": "last_updated", "type": "DATE", "start": 30, "length": 8, "format": "yyyyMMdd", "required": true}
  ]
}', 'system', 'Fixed width schema for account data'),

('default', 'service4', 'COBOL Copybook Schema', '1.0', 'COBOL_COPYBOOK', 
'{
  "type": "cobol_copybook",
  "recordLength": 80,
  "fields": [
    {"name": "CUSTOMER-ID", "level": "05", "type": "PIC", "picture": "X(10)", "start": 1, "length": 10, "required": true},
    {"name": "CUSTOMER-NAME", "level": "05", "type": "PIC", "picture": "X(30)", "start": 11, "length": 30, "required": true},
    {"name": "ACCOUNT-BALANCE", "level": "05", "type": "PIC", "picture": "9(10)V99", "start": 41, "length": 12, "required": true},
    {"name": "LAST-UPDATE-DATE", "level": "05", "type": "PIC", "picture": "9(8)", "start": 53, "length": 8, "required": true},
    {"name": "STATUS-CODE", "level": "05", "type": "PIC", "picture": "X(1)", "start": 61, "length": 1, "required": true},
    {"name": "FILLER", "level": "05", "type": "PIC", "picture": "X(19)", "start": 62, "length": 19, "required": false}
  ]
}', 'system', 'COBOL copybook schema for customer records');

-- Insert sample validation rules
INSERT INTO schema_validation_rules (schema_id, rule_name, rule_type, rule_definition, rule_order, error_message)
SELECT 
    fs.id,
    'File Size Check',
    'CUSTOM',
    'fileSize <= 10485760', -- 10MB limit
    1,
    'File size exceeds maximum allowed size of 10MB'
FROM file_schemas fs WHERE fs.schema_name = 'CSV Data Schema';

INSERT INTO schema_validation_rules (schema_id, rule_name, rule_type, rule_definition, rule_order, error_message)
SELECT 
    fs.id,
    'Header Validation',
    'REGEX',
    '^transaction_id,amount,currency,transaction_date,status$',
    2,
    'CSV header does not match expected format'
FROM file_schemas fs WHERE fs.schema_name = 'CSV Data Schema';

-- Insert sample fields
INSERT INTO schema_fields (schema_id, field_name, field_type, field_length, is_required, field_order, description)
SELECT 
    fs.id,
    'transaction_id',
    'STRING',
    50,
    1,
    1,
    'Unique transaction identifier'
FROM file_schemas fs WHERE fs.schema_name = 'CSV Data Schema';

INSERT INTO schema_fields (schema_id, field_name, field_type, field_length, is_required, field_order, description)
SELECT 
    fs.id,
    'amount',
    'DECIMAL',
    NULL,
    1,
    2,
    'Transaction amount'
FROM file_schemas fs WHERE fs.schema_name = 'CSV Data Schema';

PRINT 'Schema management tables created successfully!';
PRINT 'Sample schemas and validation rules inserted.';