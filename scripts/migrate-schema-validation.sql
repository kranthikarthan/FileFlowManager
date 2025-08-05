-- Migration script for Schema Validation Feature
-- This script adds schema validation columns to existing service_configurations table
-- Run this script on existing databases to enable schema validation features

-- Check if the new columns already exist before adding them
-- This prevents errors if the script is run multiple times

-- Add schema_validation_enabled column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'schema_validation_enabled')
BEGIN
    ALTER TABLE service_configurations 
    ADD schema_validation_enabled BIT NOT NULL DEFAULT 0;
    PRINT 'Added schema_validation_enabled column';
END
ELSE
BEGIN
    PRINT 'schema_validation_enabled column already exists';
END

-- Add schema_id column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'schema_id')
BEGIN
    ALTER TABLE service_configurations 
    ADD schema_id BIGINT NULL;
    PRINT 'Added schema_id column';
END
ELSE
BEGIN
    PRINT 'schema_id column already exists';
END

-- Add schema_validation_mode column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'schema_validation_mode')
BEGIN
    ALTER TABLE service_configurations 
    ADD schema_validation_mode VARCHAR(20) NOT NULL DEFAULT 'STRICT';
    PRINT 'Added schema_validation_mode column';
END
ELSE
BEGIN
    PRINT 'schema_validation_mode column already exists';
END

-- Add binary_file_bypass column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'binary_file_bypass')
BEGIN
    ALTER TABLE service_configurations 
    ADD binary_file_bypass BIT NOT NULL DEFAULT 0;
    PRINT 'Added binary_file_bypass column';
END
ELSE
BEGIN
    PRINT 'binary_file_bypass column already exists';
END

-- Add foreign key constraint for schema_id if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'schema_id' 
               AND CONSTRAINT_NAME LIKE '%FK%')
BEGIN
    ALTER TABLE service_configurations 
    ADD CONSTRAINT FK_service_configurations_schema_id 
    FOREIGN KEY (schema_id) REFERENCES file_schemas(id);
    PRINT 'Added foreign key constraint for schema_id';
END
ELSE
BEGIN
    PRINT 'Foreign key constraint for schema_id already exists';
END

-- Create index for schema validation queries
IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_service_configurations_schema_validation' 
               AND object_id = OBJECT_ID('service_configurations'))
BEGIN
    CREATE INDEX IX_service_configurations_schema_validation 
    ON service_configurations (schema_validation_enabled, schema_id);
    PRINT 'Created index for schema validation queries';
END
ELSE
BEGIN
    PRINT 'Index for schema validation queries already exists';
END

-- Update existing service configurations to have schema validation disabled by default
UPDATE service_configurations 
SET schema_validation_enabled = 0, 
    schema_validation_mode = 'STRICT'
WHERE schema_validation_enabled IS NULL;

PRINT 'Schema validation migration completed successfully!';
PRINT 'Schema validation is now available as an optional feature per service configuration.';