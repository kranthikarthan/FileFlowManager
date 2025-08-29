package com.filetransfer.web.repository;

import com.filetransfer.web.config.DatabaseConfiguration;
import com.filetransfer.web.entity.Tenant;
import com.filetransfer.web.service.DatabaseAbstractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Database-specific tenant repository that leverages database-specific features
 * Provides optimized queries for both PostgreSQL and Azure SQL MI
 */
@Repository
public class DatabaseSpecificTenantRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseConfiguration.DatabaseTypeDetector databaseTypeDetector;

    @Autowired
    private DatabaseAbstractionService databaseAbstractionService;

    /**
     * Search tenants with database-specific text search
     */
    public List<Map<String, Object>> searchTenants(String searchTerm, String tenantId) {
        return databaseAbstractionService.executeTextSearchQuery(
            "tenants", "tenant_name", searchTerm, tenantId
        );
    }

    /**
     * Get tenant statistics with database-specific optimizations
     */
    public Map<String, Object> getTenantStatistics(String tenantId) {
        if (databaseTypeDetector.isSqlServer()) {
            // Use SQL Server stored procedure for better performance
            return jdbcTemplate.queryForMap("EXEC GetTenantStatistics ?", tenantId);
        } else {
            // Use PostgreSQL query
            String query = """
                SELECT 
                    t.tenant_id,
                    COUNT(DISTINCT sc.id) as total_services,
                    COUNT(DISTINCT ssc.id) as total_sub_services,
                    COUNT(DISTINCT fs.id) as total_schemas,
                    COUNT(ftr.id) as total_file_transfers,
                    SUM(CASE WHEN ftr.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_transfers,
                    SUM(CASE WHEN ftr.status = 'FAILED' THEN 1 ELSE 0 END) as failed_transfers,
                    SUM(ftr.file_size) as total_data_volume,
                    AVG(EXTRACT(EPOCH FROM (ftr.processing_end_time - ftr.processing_start_time)) * 1000) as avg_processing_time_ms
                FROM tenants t
                LEFT JOIN service_configurations sc ON t.tenant_id = sc.tenant_id
                LEFT JOIN sub_service_configurations ssc ON sc.id = ssc.service_id
                LEFT JOIN file_schemas fs ON t.tenant_id = fs.tenant_id
                LEFT JOIN file_transfer_records ftr ON t.tenant_id = ftr.tenant_id
                WHERE t.tenant_id = ?
                GROUP BY t.tenant_id
                """;
            
            return jdbcTemplate.queryForMap(query, tenantId);
        }
    }

    /**
     * Get tenant activity report with date range
     */
    public List<Map<String, Object>> getTenantActivityReport(String tenantId, LocalDate startDate, LocalDate endDate) {
        return databaseAbstractionService.executeDateRangeQuery(
            "file_transfer_records", "created_at", startDate, endDate,
            "tenant_id = '" + tenantId + "'"
        );
    }

    /**
     * Search tenant configurations using JSON metadata
     */
    public List<Map<String, Object>> searchTenantConfigurations(String tenantId, String configKey, String configValue) {
        // This would search in JSON configuration fields
        return databaseAbstractionService.executeJsonQuery(
            "tenant_configurations", "configuration_json", configKey, configValue, tenantId
        );
    }

    /**
     * Get performance metrics aggregated by time period
     */
    public Map<String, Object> getTenantPerformanceMetrics(String tenantId, LocalDate startDate, LocalDate endDate) {
        return databaseAbstractionService.executeAggregationQuery(
            "file_transfer_records", "DATE(created_at)", "file_size", tenantId, startDate, endDate
        );
    }

    /**
     * Execute database-specific maintenance for tenant data
     */
    public void performTenantDataMaintenance(String tenantId) {
        if (databaseTypeDetector.isSqlServer()) {
            // SQL Server-specific maintenance
            jdbcTemplate.execute("EXEC sp_recompile 'dbo.tenants'");
            
            // Update statistics for tenant-related tables
            String updateStatsQuery = """
                UPDATE STATISTICS dbo.tenants;
                UPDATE STATISTICS dbo.service_configurations;
                UPDATE STATISTICS dbo.file_transfer_records;
                """;
            jdbcTemplate.execute(updateStatsQuery);
            
        } else if (databaseTypeDetector.isPostgreSQL()) {
            // PostgreSQL-specific maintenance
            String vacuumQuery = """
                VACUUM ANALYZE tenants;
                VACUUM ANALYZE service_configurations;
                VACUUM ANALYZE file_transfer_records;
                """;
            jdbcTemplate.execute(vacuumQuery);
        }
    }

    /**
     * Get database-specific query execution plan
     */
    public List<Map<String, Object>> getQueryExecutionPlan(String query, Object... parameters) {
        if (databaseTypeDetector.isSqlServer()) {
            // SQL Server execution plan
            jdbcTemplate.execute("SET SHOWPLAN_ALL ON");
            List<Map<String, Object>> plan = jdbcTemplate.queryForList(query, parameters);
            jdbcTemplate.execute("SET SHOWPLAN_ALL OFF");
            return plan;
        } else if (databaseTypeDetector.isPostgreSQL()) {
            // PostgreSQL execution plan
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query;
            return jdbcTemplate.queryForList(explainQuery, parameters);
        }
        
        return List.of();
    }

    /**
     * Check if database-specific features are available
     */
    public boolean hasAdvancedFeatures() {
        if (databaseTypeDetector.isSqlServer()) {
            // Check SQL Server enterprise features
            return checkSqlServerEnterpriseFeatures();
        } else if (databaseTypeDetector.isPostgreSQL()) {
            // Check PostgreSQL extensions
            return checkPostgreSQLExtensions();
        }
        
        return false;
    }

    private boolean checkSqlServerEnterpriseFeatures() {
        try {
            // Check if Query Store is enabled
            Integer queryStoreEnabled = jdbcTemplate.queryForObject(
                "SELECT is_query_store_on FROM sys.databases WHERE name = DB_NAME()", Integer.class);
            
            // Check if automatic tuning is enabled
            Integer autoTuningEnabled = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.database_automatic_tuning_options WHERE desired_state = 1", Integer.class);
            
            return queryStoreEnabled != null && queryStoreEnabled == 1 && 
                   autoTuningEnabled != null && autoTuningEnabled > 0;
                   
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkPostgreSQLExtensions() {
        try {
            // Check for useful PostgreSQL extensions
            Integer extensionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname IN ('pg_stat_statements', 'pg_trgm', 'btree_gin')", 
                Integer.class);
            
            return extensionCount != null && extensionCount > 0;
            
        } catch (Exception e) {
            return false;
        }
    }
}