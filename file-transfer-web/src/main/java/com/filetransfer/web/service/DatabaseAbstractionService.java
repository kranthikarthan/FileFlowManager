package com.filetransfer.web.service;

import com.filetransfer.web.config.DatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database abstraction service to handle database-specific operations
 * Provides unified interface for PostgreSQL and Azure SQL MI operations
 */
@Service
public class DatabaseAbstractionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseAbstractionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseConfiguration.DatabaseTypeDetector databaseTypeDetector;

    @Autowired
    private DatabaseConfiguration.DatabaseQueryProvider queryProvider;

    /**
     * Execute paginated query with database-specific pagination
     */
    public <T> Page<T> executePaginatedQuery(String baseQuery, Pageable pageable, 
                                           Class<T> resultType, Object... parameters) {
        
        String countQuery = "SELECT COUNT(*) FROM (" + baseQuery + ") AS count_query";
        Long totalElements = jdbcTemplate.queryForObject(countQuery, Long.class, parameters);
        
        if (totalElements == null || totalElements == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Build paginated query based on database type
        String paginatedQuery = buildPaginatedQuery(baseQuery, pageable);
        
        List<T> results = jdbcTemplate.query(paginatedQuery, 
            (rs, rowNum) -> mapResultSet(rs, resultType), parameters);
        
        return new PageImpl<>(results, pageable, totalElements);
    }

    /**
     * Build database-specific paginated query
     */
    private String buildPaginatedQuery(String baseQuery, Pageable pageable) {
        Map<String, String> params = new HashMap<>();
        params.put("offset", String.valueOf(pageable.getOffset()));
        params.put("limit", String.valueOf(pageable.getPageSize()));
        
        if (pageable.getSort().isSorted()) {
            String orderBy = pageable.getSort().stream()
                .map(order -> order.getProperty() + " " + order.getDirection().name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("id");
            params.put("orderBy", orderBy);
        } else {
            params.put("orderBy", "id");
        }
        
        if (databaseTypeDetector.isSqlServer()) {
            // SQL Server uses OFFSET...FETCH
            return baseQuery + " " + queryProvider.formatQuery("LIMIT_OFFSET", params);
        } else {
            // PostgreSQL uses LIMIT...OFFSET
            return baseQuery + " ORDER BY " + params.get("orderBy") + " " + 
                   queryProvider.formatQuery("LIMIT_OFFSET", params);
        }
    }

    /**
     * Execute database-specific date range query
     */
    public List<Map<String, Object>> executeDateRangeQuery(String tableName, String dateColumn, 
                                                          LocalDate startDate, LocalDate endDate, 
                                                          String additionalConditions) {
        
        String query;
        if (databaseTypeDetector.isSqlServer()) {
            query = String.format(
                "SELECT * FROM [dbo].[%s] WHERE %s >= ? AND %s <= ? %s ORDER BY %s DESC",
                tableName, dateColumn, dateColumn, 
                additionalConditions != null ? "AND " + additionalConditions : "",
                dateColumn
            );
        } else {
            query = String.format(
                "SELECT * FROM %s WHERE %s >= ? AND %s <= ? %s ORDER BY %s DESC",
                tableName, dateColumn, dateColumn,
                additionalConditions != null ? "AND " + additionalConditions : "",
                dateColumn
            );
        }
        
        return jdbcTemplate.queryForList(query, startDate, endDate);
    }

    /**
     * Execute database-specific text search
     */
    public List<Map<String, Object>> executeTextSearchQuery(String tableName, String searchColumn, 
                                                           String searchTerm, String tenantId) {
        
        String query;
        if (databaseTypeDetector.isSqlServer()) {
            // Use SQL Server full-text search or LIKE
            if (isFullTextEnabled(tableName, searchColumn)) {
                query = String.format(
                    "SELECT * FROM [dbo].[%s] WHERE CONTAINS(%s, ?) AND tenant_id = ? ORDER BY id DESC",
                    tableName, searchColumn
                );
            } else {
                query = String.format(
                    "SELECT * FROM [dbo].[%s] WHERE %s LIKE ? AND tenant_id = ? ORDER BY id DESC",
                    tableName, searchColumn
                );
                searchTerm = "%" + searchTerm + "%";
            }
        } else {
            // Use PostgreSQL ILIKE for case-insensitive search
            query = String.format(
                "SELECT * FROM %s WHERE %s ILIKE ? AND tenant_id = ? ORDER BY id DESC",
                tableName, searchColumn
            );
            searchTerm = "%" + searchTerm + "%";
        }
        
        return jdbcTemplate.queryForList(query, searchTerm, tenantId);
    }

    /**
     * Execute database-specific JSON query
     */
    public List<Map<String, Object>> executeJsonQuery(String tableName, String jsonColumn, 
                                                     String jsonPath, String expectedValue, 
                                                     String tenantId) {
        
        String query;
        if (databaseTypeDetector.isSqlServer()) {
            // SQL Server JSON functions
            query = String.format(
                "SELECT * FROM [dbo].[%s] WHERE JSON_VALUE(%s, '$.%s') = ? AND tenant_id = ? ORDER BY id DESC",
                tableName, jsonColumn, jsonPath
            );
        } else {
            // PostgreSQL JSON operators
            query = String.format(
                "SELECT * FROM %s WHERE %s->>'%s' = ? AND tenant_id = ? ORDER BY id DESC",
                tableName, jsonColumn, jsonPath
            );
        }
        
        return jdbcTemplate.queryForList(query, expectedValue, tenantId);
    }

    /**
     * Execute database-specific aggregation query
     */
    public Map<String, Object> executeAggregationQuery(String tableName, String groupByColumn, 
                                                      String aggregateColumn, String tenantId, 
                                                      LocalDate startDate, LocalDate endDate) {
        
        String query;
        if (databaseTypeDetector.isSqlServer()) {
            query = String.format(
                "SELECT %s, COUNT(*) as record_count, SUM(%s) as total_value, AVG(CAST(%s AS FLOAT)) as avg_value " +
                "FROM [dbo].[%s] " +
                "WHERE tenant_id = ? AND CAST(created_at AS DATE) BETWEEN ? AND ? " +
                "GROUP BY %s ORDER BY %s",
                groupByColumn, aggregateColumn, aggregateColumn, tableName, groupByColumn, groupByColumn
            );
        } else {
            query = String.format(
                "SELECT %s, COUNT(*) as record_count, SUM(%s) as total_value, AVG(%s::numeric) as avg_value " +
                "FROM %s " +
                "WHERE tenant_id = ? AND DATE(created_at) BETWEEN ? AND ? " +
                "GROUP BY %s ORDER BY %s",
                groupByColumn, aggregateColumn, aggregateColumn, tableName, groupByColumn, groupByColumn
            );
        }
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, tenantId, startDate, endDate);
        
        // Aggregate the results
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGroups", results.size());
        summary.put("groupData", results);
        
        return summary;
    }

    /**
     * Execute database-specific bulk insert
     */
    public void executeBulkInsert(String tableName, List<Map<String, Object>> records) {
        
        if (records.isEmpty()) {
            return;
        }
        
        if (databaseTypeDetector.isSqlServer()) {
            // Use SQL Server MERGE or bulk insert
            executeSqlServerBulkInsert(tableName, records);
        } else {
            // Use PostgreSQL bulk insert
            executePostgreSQLBulkInsert(tableName, records);
        }
    }

    /**
     * Get database-specific performance statistics
     */
    public Map<String, Object> getDatabasePerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        if (databaseTypeDetector.isSqlServer()) {
            // SQL Server performance stats
            stats.putAll(getSqlServerPerformanceStats());
        } else if (databaseTypeDetector.isPostgreSQL()) {
            // PostgreSQL performance stats
            stats.putAll(getPostgreSQLPerformanceStats());
        }
        
        return stats;
    }

    /**
     * Execute database-specific maintenance operations
     */
    public void performDatabaseMaintenance() {
        logger.info("Performing database maintenance for {}", databaseTypeDetector.getDatabaseType());
        
        if (databaseTypeDetector.isSqlServer()) {
            performSqlServerMaintenance();
        } else if (databaseTypeDetector.isPostgreSQL()) {
            performPostgreSQLMaintenance();
        }
    }

    // Private helper methods

    private boolean isFullTextEnabled(String tableName, String columnName) {
        if (!databaseTypeDetector.isSqlServer()) {
            return false;
        }
        
        try {
            String checkQuery = """
                SELECT COUNT(*) FROM sys.fulltext_indexes fi
                INNER JOIN sys.tables t ON fi.object_id = t.object_id
                INNER JOIN sys.fulltext_index_columns fic ON fi.object_id = fic.object_id
                INNER JOIN sys.columns c ON fic.object_id = c.object_id AND fic.column_id = c.column_id
                WHERE t.name = ? AND c.name = ?
                """;
            
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.debug("Could not check full-text index status", e);
            return false;
        }
    }

    private void executeSqlServerBulkInsert(String tableName, List<Map<String, Object>> records) {
        // SQL Server bulk insert implementation
        Map<String, Object> firstRecord = records.get(0);
        String columns = String.join(", ", firstRecord.keySet());
        String placeholders = firstRecord.keySet().stream()
            .map(k -> "?")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        
        String sql = String.format("INSERT INTO [dbo].[%s] (%s) VALUES (%s)", 
                                  tableName, columns, placeholders);
        
        List<Object[]> batchArgs = records.stream()
            .map(record -> record.values().toArray())
            .toList();
        
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void executePostgreSQLBulkInsert(String tableName, List<Map<String, Object>> records) {
        // PostgreSQL bulk insert implementation
        Map<String, Object> firstRecord = records.get(0);
        String columns = String.join(", ", firstRecord.keySet());
        String placeholders = firstRecord.keySet().stream()
            .map(k -> "?")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
                                  tableName, columns, placeholders);
        
        List<Object[]> batchArgs = records.stream()
            .map(record -> record.values().toArray())
            .toList();
        
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private Map<String, Object> getSqlServerPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get SQL Server performance counters
            String query = """
                SELECT 
                    counter_name,
                    cntr_value,
                    cntr_type
                FROM sys.dm_os_performance_counters 
                WHERE object_name LIKE '%:Databases%'
                AND instance_name = DB_NAME()
                AND counter_name IN ('Transactions/sec', 'Batch Requests/sec', 'Page reads/sec', 'Page writes/sec')
                """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            for (Map<String, Object> result : results) {
                stats.put((String) result.get("counter_name"), result.get("cntr_value"));
            }
            
            // Get connection count
            Integer connectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.dm_exec_sessions WHERE is_user_process = 1", Integer.class);
            stats.put("active_connections", connectionCount);
            
            // Get database size
            Long databaseSize = jdbcTemplate.queryForObject(
                "SELECT SUM(CAST(FILEPROPERTY(name, 'SpaceUsed') AS bigint) * 8192) FROM sys.database_files", Long.class);
            stats.put("database_size_bytes", databaseSize);
            
        } catch (Exception e) {
            logger.warn("Could not retrieve SQL Server performance stats", e);
        }
        
        return stats;
    }

    private Map<String, Object> getPostgreSQLPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get PostgreSQL statistics
            String query = """
                SELECT 
                    numbackends as active_connections,
                    xact_commit as transactions_committed,
                    xact_rollback as transactions_rolled_back,
                    blks_read as blocks_read,
                    blks_hit as blocks_hit,
                    tup_returned as tuples_returned,
                    tup_fetched as tuples_fetched,
                    tup_inserted as tuples_inserted,
                    tup_updated as tuples_updated,
                    tup_deleted as tuples_deleted
                FROM pg_stat_database 
                WHERE datname = current_database()
                """;
            
            Map<String, Object> result = jdbcTemplate.queryForMap(query);
            stats.putAll(result);
            
            // Get database size
            Long databaseSize = jdbcTemplate.queryForObject(
                "SELECT pg_database_size(current_database())", Long.class);
            stats.put("database_size_bytes", databaseSize);
            
        } catch (Exception e) {
            logger.warn("Could not retrieve PostgreSQL performance stats", e);
        }
        
        return stats;
    }

    private void performSqlServerMaintenance() {
        try {
            // Update statistics
            jdbcTemplate.execute("EXEC sp_updatestats");
            
            // Reorganize indexes with low fragmentation
            String reorganizeQuery = """
                DECLARE @sql NVARCHAR(MAX) = '';
                SELECT @sql = @sql + 'ALTER INDEX ' + QUOTENAME(i.name) + ' ON ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(t.name) + ' REORGANIZE;' + CHAR(13)
                FROM sys.indexes i
                INNER JOIN sys.tables t ON i.object_id = t.object_id
                INNER JOIN sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips ON i.object_id = ips.object_id AND i.index_id = ips.index_id
                WHERE ips.avg_fragmentation_in_percent > 10 AND ips.avg_fragmentation_in_percent < 30
                AND i.type_desc IN ('CLUSTERED', 'NONCLUSTERED');
                EXEC sp_executesql @sql;
                """;
            
            jdbcTemplate.execute(reorganizeQuery);
            
            // Rebuild heavily fragmented indexes
            String rebuildQuery = """
                DECLARE @sql NVARCHAR(MAX) = '';
                SELECT @sql = @sql + 'ALTER INDEX ' + QUOTENAME(i.name) + ' ON ' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(t.name) + ' REBUILD;' + CHAR(13)
                FROM sys.indexes i
                INNER JOIN sys.tables t ON i.object_id = t.object_id
                INNER JOIN sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') ips ON i.object_id = ips.object_id AND i.index_id = ips.index_id
                WHERE ips.avg_fragmentation_in_percent >= 30
                AND i.type_desc IN ('CLUSTERED', 'NONCLUSTERED');
                EXEC sp_executesql @sql;
                """;
            
            jdbcTemplate.execute(rebuildQuery);
            
            logger.info("SQL Server maintenance completed");
            
        } catch (Exception e) {
            logger.error("SQL Server maintenance failed", e);
        }
    }

    private void performPostgreSQLMaintenance() {
        try {
            // Vacuum and analyze tables
            jdbcTemplate.execute("VACUUM ANALYZE");
            
            // Reindex if needed
            jdbcTemplate.execute("REINDEX DATABASE CONCURRENTLY " + getCurrentDatabase());
            
            logger.info("PostgreSQL maintenance completed");
            
        } catch (Exception e) {
            logger.error("PostgreSQL maintenance failed", e);
        }
    }

    private String getCurrentDatabase() {
        return jdbcTemplate.queryForObject("SELECT current_database()", String.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T mapResultSet(java.sql.ResultSet rs, Class<T> resultType) throws java.sql.SQLException {
        // Simple mapping - in real implementation, use proper ORM mapping
        if (resultType == Map.class) {
            Map<String, Object> result = new HashMap<>();
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                result.put(columnName, value);
            }
            
            return (T) result;
        }
        
        // For other types, would need proper mapping logic
        throw new UnsupportedOperationException("Result type mapping not implemented: " + resultType);
    }

    /**
     * Check if database supports specific features
     */
    public boolean supportsFeature(DatabaseFeature feature) {
        return switch (feature) {
            case FULL_TEXT_SEARCH -> databaseTypeDetector.isSqlServer() || databaseTypeDetector.isPostgreSQL();
            case JSON_FUNCTIONS -> true; // Both databases support JSON
            case WINDOW_FUNCTIONS -> true; // Both databases support window functions
            case CTE -> true; // Both databases support Common Table Expressions
            case PARTITIONING -> true; // Both databases support table partitioning
            case MATERIALIZED_VIEWS -> databaseTypeDetector.isPostgreSQL() || databaseTypeDetector.isSqlServer();
            case QUERY_STORE -> databaseTypeDetector.isSqlServer();
            case AUTOMATIC_TUNING -> databaseTypeDetector.isSqlServer();
            default -> false;
        };
    }

    /**
     * Database feature enumeration
     */
    public enum DatabaseFeature {
        FULL_TEXT_SEARCH,
        JSON_FUNCTIONS,
        WINDOW_FUNCTIONS,
        CTE,
        PARTITIONING,
        MATERIALIZED_VIEWS,
        QUERY_STORE,
        AUTOMATIC_TUNING
    }
}