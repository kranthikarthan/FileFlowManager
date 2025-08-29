package com.filetransfer.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Database configuration for multi-database support
 * Supports PostgreSQL, Azure SQL MI, and H2 databases with automatic detection
 */
@Configuration
public class DatabaseConfiguration {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.jpa.database-platform:}")
    private String databasePlatform;

    /**
     * Database type detection bean
     */
    @Bean
    @Primary
    public DatabaseTypeDetector databaseTypeDetector() {
        return new DatabaseTypeDetector(datasourceUrl, databasePlatform);
    }

    /**
     * Database-specific query provider
     */
    @Bean
    public DatabaseQueryProvider databaseQueryProvider(DatabaseTypeDetector detector) {
        return new DatabaseQueryProvider(detector.getDatabaseType());
    }

    /**
     * Database-specific configuration for PostgreSQL
     */
    @Configuration
    @ConditionalOnProperty(name = "database.type", havingValue = "postgresql", matchIfMissing = true)
    @Profile("!azure-sqlmi")
    static class PostgreSQLConfiguration {
        
        @Bean
        public PostgreSQLSpecificService postgreSQLSpecificService(JdbcTemplate jdbcTemplate) {
            return new PostgreSQLSpecificService(jdbcTemplate);
        }
    }

    /**
     * Database-specific configuration for Azure SQL MI
     */
    @Configuration
    @ConditionalOnProperty(name = "database.type", havingValue = "sqlserver")
    @Profile("azure-sqlmi")
    static class AzureSqlMiConfiguration {
        
        @Bean
        public AzureSqlMiSpecificService azureSqlMiSpecificService(JdbcTemplate jdbcTemplate) {
            return new AzureSqlMiSpecificService(jdbcTemplate);
        }
    }

    /**
     * Database type detector utility
     */
    public static class DatabaseTypeDetector {
        
        private final DatabaseType databaseType;
        
        public DatabaseTypeDetector(String datasourceUrl, String databasePlatform) {
            this.databaseType = detectDatabaseType(datasourceUrl, databasePlatform);
        }
        
        private DatabaseType detectDatabaseType(String url, String platform) {
            if (url.contains("sqlserver") || platform.contains("SQLServer")) {
                return DatabaseType.SQL_SERVER;
            } else if (url.contains("postgresql") || platform.contains("PostgreSQL")) {
                return DatabaseType.POSTGRESQL;
            } else if (url.contains("h2")) {
                return DatabaseType.H2;
            } else {
                return DatabaseType.UNKNOWN;
            }
        }
        
        public DatabaseType getDatabaseType() {
            return databaseType;
        }
        
        public boolean isSqlServer() {
            return databaseType == DatabaseType.SQL_SERVER;
        }
        
        public boolean isPostgreSQL() {
            return databaseType == DatabaseType.POSTGRESQL;
        }
        
        public boolean isH2() {
            return databaseType == DatabaseType.H2;
        }
    }

    /**
     * Database-agnostic query provider
     */
    public static class DatabaseQueryProvider {
        
        private final DatabaseType databaseType;
        private final Map<String, String> queries;
        
        public DatabaseQueryProvider(DatabaseType databaseType) {
            this.databaseType = databaseType;
            this.queries = initializeQueries();
        }
        
        private Map<String, String> initializeQueries() {
            Map<String, String> queryMap = new HashMap<>();
            
            // Pagination queries
            if (databaseType == DatabaseType.SQL_SERVER) {
                queryMap.put("LIMIT_OFFSET", "ORDER BY {orderBy} OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY");
                queryMap.put("TOP_N", "SELECT TOP {limit} * FROM {table}");
            } else {
                queryMap.put("LIMIT_OFFSET", "LIMIT {limit} OFFSET {offset}");
                queryMap.put("TOP_N", "SELECT * FROM {table} LIMIT {limit}");
            }
            
            // Date/time functions
            if (databaseType == DatabaseType.SQL_SERVER) {
                queryMap.put("CURRENT_TIMESTAMP", "GETUTCDATE()");
                queryMap.put("DATE_TRUNC_DAY", "CAST({column} AS DATE)");
                queryMap.put("DATE_PART_HOUR", "DATEPART(HOUR, {column})");
                queryMap.put("INTERVAL_DAYS", "DATEADD(DAY, {days}, {date})");
            } else {
                queryMap.put("CURRENT_TIMESTAMP", "NOW()");
                queryMap.put("DATE_TRUNC_DAY", "DATE_TRUNC('day', {column})");
                queryMap.put("DATE_PART_HOUR", "EXTRACT(HOUR FROM {column})");
                queryMap.put("INTERVAL_DAYS", "{date} + INTERVAL '{days} days'");
            }
            
            // String functions
            if (databaseType == DatabaseType.SQL_SERVER) {
                queryMap.put("ILIKE", "LOWER({column}) LIKE LOWER({pattern})");
                queryMap.put("CONCAT", "CONCAT({str1}, {str2})");
                queryMap.put("LENGTH", "LEN({column})");
            } else {
                queryMap.put("ILIKE", "{column} ILIKE {pattern}");
                queryMap.put("CONCAT", "{str1} || {str2}");
                queryMap.put("LENGTH", "LENGTH({column})");
            }
            
            // Boolean handling
            if (databaseType == DatabaseType.SQL_SERVER) {
                queryMap.put("TRUE", "1");
                queryMap.put("FALSE", "0");
                queryMap.put("BOOLEAN_TYPE", "BIT");
            } else {
                queryMap.put("TRUE", "true");
                queryMap.put("FALSE", "false");
                queryMap.put("BOOLEAN_TYPE", "BOOLEAN");
            }
            
            // JSON functions
            if (databaseType == DatabaseType.SQL_SERVER) {
                queryMap.put("JSON_EXTRACT", "JSON_VALUE({column}, '$.{path}')");
                queryMap.put("JSON_EXTRACT_ARRAY", "JSON_QUERY({column}, '$.{path}')");
                queryMap.put("JSON_VALID", "ISJSON({column}) = 1");
            } else {
                queryMap.put("JSON_EXTRACT", "{column}->'{path}'");
                queryMap.put("JSON_EXTRACT_ARRAY", "{column}->>'{path}'");
                queryMap.put("JSON_VALID", "{column}::json IS NOT NULL");
            }
            
            return queryMap;
        }
        
        public String getQuery(String queryName) {
            return queries.getOrDefault(queryName, "");
        }
        
        public String formatQuery(String queryName, Map<String, String> parameters) {
            String query = getQuery(queryName);
            
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                query = query.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            return query;
        }
        
        public DatabaseType getDatabaseType() {
            return databaseType;
        }
    }

    /**
     * PostgreSQL-specific service
     */
    public static class PostgreSQLSpecificService {
        
        private final JdbcTemplate jdbcTemplate;
        
        public PostgreSQLSpecificService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }
        
        public void optimizeForPostgreSQL() {
            // PostgreSQL-specific optimizations
            jdbcTemplate.execute("SET work_mem = '256MB'");
            jdbcTemplate.execute("SET maintenance_work_mem = '1GB'");
            jdbcTemplate.execute("SET random_page_cost = 1.1");
        }
        
        public void analyzeDatabase() {
            // Run ANALYZE on all tables for better query planning
            jdbcTemplate.execute("ANALYZE");
        }
    }

    /**
     * Azure SQL MI-specific service
     */
    public static class AzureSqlMiSpecificService {
        
        private final JdbcTemplate jdbcTemplate;
        
        public AzureSqlMiSpecificService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }
        
        public void optimizeForSqlServer() {
            // SQL Server-specific optimizations
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTO_UPDATE_STATISTICS ON");
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTO_CREATE_STATISTICS ON");
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTO_SHRINK OFF");
        }
        
        public void updateStatistics() {
            // Update statistics for better query performance
            jdbcTemplate.execute("EXEC sp_updatestats");
        }
        
        public void enableQueryStore() {
            // Enable Query Store for performance monitoring
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET QUERY_STORE = ON");
        }
        
        public void enableAutomaticTuning() {
            // Enable automatic tuning features
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTOMATIC_TUNING (FORCE_LAST_GOOD_PLAN = ON)");
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTOMATIC_TUNING (CREATE_INDEX = ON)");
            jdbcTemplate.execute("ALTER DATABASE CURRENT SET AUTOMATIC_TUNING (DROP_INDEX = ON)");
        }
    }

    /**
     * Database type enumeration
     */
    public enum DatabaseType {
        POSTGRESQL,
        SQL_SERVER,
        H2,
        UNKNOWN
    }
}