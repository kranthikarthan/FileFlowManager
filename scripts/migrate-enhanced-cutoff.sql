-- Migration script for Enhanced Cut-Off Time Management
-- This script safely adds new columns to existing service_configurations table
-- Run this script on existing databases to enable enhanced cut-off time features

-- Check if the new columns already exist before adding them
-- This prevents errors if the script is run multiple times

-- Add cut_off_time_type column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'cut_off_time_type')
BEGIN
    ALTER TABLE service_configurations 
    ADD cut_off_time_type VARCHAR(20) NOT NULL DEFAULT 'DAILY';
    PRINT 'Added cut_off_time_type column';
END
ELSE
BEGIN
    PRINT 'cut_off_time_type column already exists';
END

-- Add weekday_cut_off_time column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'weekday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD weekday_cut_off_time TIME NULL;
    PRINT 'Added weekday_cut_off_time column';
END
ELSE
BEGIN
    PRINT 'weekday_cut_off_time column already exists';
END

-- Add weekend_cut_off_time column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'weekend_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD weekend_cut_off_time TIME NULL;
    PRINT 'Added weekend_cut_off_time column';
END
ELSE
BEGIN
    PRINT 'weekend_cut_off_time column already exists';
END

-- Add individual day columns if they don't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'monday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD monday_cut_off_time TIME NULL;
    PRINT 'Added monday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'tuesday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD tuesday_cut_off_time TIME NULL;
    PRINT 'Added tuesday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'wednesday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD wednesday_cut_off_time TIME NULL;
    PRINT 'Added wednesday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'thursday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD thursday_cut_off_time TIME NULL;
    PRINT 'Added thursday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'friday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD friday_cut_off_time TIME NULL;
    PRINT 'Added friday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'saturday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD saturday_cut_off_time TIME NULL;
    PRINT 'Added saturday_cut_off_time column';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'sunday_cut_off_time')
BEGIN
    ALTER TABLE service_configurations 
    ADD sunday_cut_off_time TIME NULL;
    PRINT 'Added sunday_cut_off_time column';
END

-- Add all_sundays_as_holidays column if it doesn't exist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'service_configurations' 
               AND COLUMN_NAME = 'all_sundays_as_holidays')
BEGIN
    ALTER TABLE service_configurations 
    ADD all_sundays_as_holidays BIT NOT NULL DEFAULT 0;
    PRINT 'Added all_sundays_as_holidays column';
END
ELSE
BEGIN
    PRINT 'all_sundays_as_holidays column already exists';
END

-- Ensure all existing records have the default cut_off_time_type
UPDATE service_configurations 
SET cut_off_time_type = 'DAILY' 
WHERE cut_off_time_type IS NULL OR cut_off_time_type = '';

PRINT 'Migration completed successfully';
PRINT 'All existing services are now configured with DAILY cut-off time type';
PRINT 'Enhanced cut-off time features are now available';

-- Optional: Add check constraints for valid enum values
-- Uncomment the following if you want to enforce enum values at database level

/*
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS 
               WHERE CONSTRAINT_NAME = 'CHK_cut_off_time_type')
BEGIN
    ALTER TABLE service_configurations
    ADD CONSTRAINT CHK_cut_off_time_type 
    CHECK (cut_off_time_type IN ('DAILY', 'WEEKDAY_WEEKEND', 'PER_DAY'));
    PRINT 'Added check constraint for cut_off_time_type';
END
*/

-- Create indexes for better performance on new columns
IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_service_configurations_cut_off_time_type')
BEGIN
    CREATE INDEX IX_service_configurations_cut_off_time_type 
    ON service_configurations(cut_off_time_type);
    PRINT 'Added index on cut_off_time_type';
END

IF NOT EXISTS (SELECT * FROM sys.indexes 
               WHERE name = 'IX_service_configurations_all_sundays_as_holidays')
BEGIN
    CREATE INDEX IX_service_configurations_all_sundays_as_holidays 
    ON service_configurations(all_sundays_as_holidays);
    PRINT 'Added index on all_sundays_as_holidays';
END

PRINT 'Enhanced Cut-Off Time Migration Script Completed Successfully!';