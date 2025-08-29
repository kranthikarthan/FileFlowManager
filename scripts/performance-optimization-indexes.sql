-- Performance Optimization: Database Indexes for SubService Hierarchy
-- This script creates optimized indexes for frequently used queries

-- =======================
-- SubService Configuration Indexes
-- =======================

-- Primary lookup index for tenant + service + subservice (most frequent query)
CREATE INDEX IF NOT EXISTS IDX_SubServiceConfig_TenantServiceSub 
ON sub_service_configurations(tenant_id, service_name, sub_service_name);

-- Index for enabled subservices lookup (used by batch processing)
CREATE INDEX IF NOT EXISTS IDX_SubServiceConfig_TenantEnabled 
ON sub_service_configurations(tenant_id, enabled) 
WHERE enabled = true;

-- Index for service-level lookups
CREATE INDEX IF NOT EXISTS IDX_SubServiceConfig_TenantService 
ON sub_service_configurations(tenant_id, service_name);

-- Composite index for cut-off time queries
CREATE INDEX IF NOT EXISTS IDX_SubServiceConfig_CutOffLookup 
ON sub_service_configurations(tenant_id, service_name, sub_service_name, cut_off_time, enabled);

-- =======================
-- File Transfer Record Indexes
-- =======================

-- Enhanced index for file transfer lookups with new fields
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_Enhanced 
ON file_transfer_records(tenant_id, service_type, sub_service_type, status, created_at);

-- Index for file type and direction queries
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_FileType 
ON file_transfer_records(tenant_id, service_type, sub_service_type, file_type, direction);

-- Index for schema validation lookups
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_SchemaValidation 
ON file_transfer_records(tenant_id, schema_id, schema_validation_passed);

-- =======================
-- Cut-off Extension Indexes
-- =======================

-- Index for pending extensions by tenant
CREATE INDEX IF NOT EXISTS IDX_CutOffExtension_TenantStatus 
ON cutoff_extensions(tenant_id, status);

-- Index for active extensions lookup
CREATE INDEX IF NOT EXISTS IDX_CutOffExtension_ActiveLookup 
ON cutoff_extensions(tenant_id, service_name, sub_service_name, extension_date, status)
WHERE status IN ('APPROVED', 'ACTIVE');

-- Index for extension date queries
CREATE INDEX IF NOT EXISTS IDX_CutOffExtension_ExtensionDate 
ON cutoff_extensions(extension_date, status);

-- =======================
-- File Type Schema Mapping Indexes
-- =======================

-- Primary lookup index for schema mappings
CREATE INDEX IF NOT EXISTS IDX_FileTypeSchemaMapping_SubServiceFileType 
ON sub_service_file_type_configs(sub_service_config_id, file_type);

-- Index for schema usage tracking
CREATE INDEX IF NOT EXISTS IDX_FileTypeSchemaMapping_SchemaIds 
ON sub_service_file_type_configs(inbound_schema_id, outbound_schema_id);

-- =======================
-- Tenant and Holiday Indexes
-- =======================

-- Index for tenant timezone lookups (if not already exists)
CREATE INDEX IF NOT EXISTS IDX_Tenant_TenantId_Timezone 
ON tenants(tenant_id, timezone);

-- Enhanced holiday lookup index
CREATE INDEX IF NOT EXISTS IDX_Holiday_TenantDate 
ON holidays(tenant_id, holiday_date, is_recurring);

-- =======================
-- Alert and Monitoring Indexes
-- =======================

-- Index for unacknowledged alerts
CREATE INDEX IF NOT EXISTS IDX_AlertHistory_Unacknowledged 
ON alert_history(tenant_id, acknowledged_at) 
WHERE acknowledged_at IS NULL;

-- Index for alert statistics queries
CREATE INDEX IF NOT EXISTS IDX_AlertHistory_Statistics 
ON alert_history(tenant_id, alert_level, sent_at);

-- =======================
-- Schema and Configuration Indexes
-- =======================

-- Index for file schema lookups by tenant and type
CREATE INDEX IF NOT EXISTS IDX_FileSchema_TenantType 
ON file_schemas(tenant_id, schema_type, enabled);

-- Index for service configuration migration queries
CREATE INDEX IF NOT EXISTS IDX_ServiceConfig_Migration 
ON service_configurations(tenant_id, service_name, sub_service_name);

-- =======================
-- Partial Indexes for Performance
-- =======================

-- Partial index for active file transfers only
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_Active 
ON file_transfer_records(tenant_id, service_type, sub_service_type, created_at)
WHERE status IN ('PENDING', 'IN_PROGRESS', 'PROCESSING');

-- Partial index for failed transfers only (for error analysis)
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_Failed 
ON file_transfer_records(tenant_id, service_type, error_message, created_at)
WHERE status = 'FAILED';

-- Partial index for recent transfers (last 30 days)
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_Recent 
ON file_transfer_records(tenant_id, created_at, status)
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days';

-- =======================
-- Covering Indexes for Frequent Queries
-- =======================

-- Covering index for subservice configuration dashboard queries
CREATE INDEX IF NOT EXISTS IDX_SubServiceConfig_Dashboard 
ON sub_service_configurations(tenant_id, enabled, service_name, sub_service_name, cut_off_time, description);

-- Covering index for file transfer summary queries
CREATE INDEX IF NOT EXISTS IDX_FileTransferRecord_Summary 
ON file_transfer_records(tenant_id, service_type, status, created_at, file_size, file_name);

-- =======================
-- Foreign Key Constraint Indexes (if not auto-created)
-- =======================

-- Ensure foreign key indexes exist for optimal join performance
CREATE INDEX IF NOT EXISTS IDX_FileTypeSchemaMapping_SubServiceFK 
ON sub_service_file_type_configs(sub_service_config_id);

CREATE INDEX IF NOT EXISTS IDX_FileTypeSchemaMapping_InboundSchemaFK 
ON sub_service_file_type_configs(inbound_schema_id);

CREATE INDEX IF NOT EXISTS IDX_FileTypeSchemaMapping_OutboundSchemaFK 
ON sub_service_file_type_configs(outbound_schema_id);

-- =======================
-- Statistics and Maintenance
-- =======================

-- Update table statistics for better query planning
-- (Syntax varies by database - this is PostgreSQL style)
-- ANALYZE sub_service_configurations;
-- ANALYZE file_transfer_records;
-- ANALYZE cutoff_extensions;
-- ANALYZE sub_service_file_type_configs;

-- For SQL Server, use:
-- UPDATE STATISTICS sub_service_configurations;
-- UPDATE STATISTICS file_transfer_records;
-- UPDATE STATISTICS cutoff_extensions;
-- UPDATE STATISTICS sub_service_file_type_configs;

PRINT 'Performance optimization indexes created successfully';
PRINT 'Database query performance should be significantly improved';
PRINT 'Monitor query execution plans to validate index usage';

-- =======================
-- Index Usage Monitoring Queries
-- =======================

-- Query to monitor index usage (PostgreSQL)
/*
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
*/

-- Query to find unused indexes (PostgreSQL)
/*
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes 
WHERE idx_scan = 0 
    AND schemaname = 'public';
*/