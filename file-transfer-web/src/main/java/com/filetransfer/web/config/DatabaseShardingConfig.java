package com.filetransfer.web.config;

import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * Database sharding configuration using Apache ShardingSphere
 * Implements horizontal partitioning for large-scale data handling
 */
@Configuration
@Profile({"sharding", "production", "scale"})
public class DatabaseShardingConfig {

    @Value("${sharding.enabled:false}")
    private boolean shardingEnabled;

    @Value("${sharding.database.count:2}")
    private int databaseCount;

    @Value("${sharding.table.count:4}")
    private int tableCount;

    // Database connection configuration
    @Value("${spring.datasource.primary.url}")
    private String primaryDatabaseUrl;

    @Value("${spring.datasource.primary.username}")
    private String primaryUsername;

    @Value("${spring.datasource.primary.password}")
    private String primaryPassword;

    @Value("${spring.datasource.secondary.url:}")
    private String secondaryDatabaseUrl;

    @Value("${spring.datasource.secondary.username:}")
    private String secondaryUsername;

    @Value("${spring.datasource.secondary.password:}")
    private String secondaryPassword;

    /**
     * Create sharded data source
     */
    @Bean
    @Primary
    public DataSource shardedDataSource() throws SQLException {
        if (!shardingEnabled) {
            return createSingleDataSource();
        }

        // Create data source map
        Map<String, DataSource> dataSourceMap = createDataSourceMap();

        // Create sharding rule configuration
        ShardingRuleConfiguration shardingRuleConfig = createShardingRuleConfiguration();

        // Create properties
        Properties props = createShardingSphereProperties();

        // Create sharded data source
        return ShardingSphereDataSourceFactory.createDataSource(
            dataSourceMap, 
            Collections.singleton(shardingRuleConfig), 
            props
        );
    }

    /**
     * Create data source map for sharding
     */
    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // Primary database instances
        for (int i = 0; i < databaseCount; i++) {
            String dataSourceName = "ds" + i;
            DataSource dataSource = createHikariDataSource(
                getDatabaseUrl(i),
                getDatabaseUsername(i),
                getDatabasePassword(i)
            );
            dataSourceMap.put(dataSourceName, dataSource);
        }

        return dataSourceMap;
    }

    /**
     * Create HikariCP data source with optimized settings
     */
    private DataSource createHikariDataSource(String url, String username, String password) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.h2.Driver"); // Adjust based on your database
        
        // Performance optimizations
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // Connection properties for performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * Create sharding rule configuration
     */
    private ShardingRuleConfiguration createShardingRuleConfiguration() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        // Configure sharding tables
        shardingRuleConfig.getTables().add(createFileTransferRecordTableRule());
        shardingRuleConfig.getTables().add(createAlertHistoryTableRule());
        shardingRuleConfig.getTables().add(createDailyFileCountTrackerTableRule());

        // Configure sharding algorithms
        shardingRuleConfig.getShardingAlgorithms().putAll(createShardingAlgorithms());

        // Configure default database strategy
        shardingRuleConfig.setDefaultDatabaseShardingStrategy(
            new StandardShardingStrategyConfiguration("tenant_id", "database-inline")
        );

        return shardingRuleConfig;
    }

    /**
     * Configure file transfer record table sharding
     */
    private ShardingTableRuleConfiguration createFileTransferRecordTableRule() {
        ShardingTableRuleConfiguration config = new ShardingTableRuleConfiguration(
            "file_transfer_records", 
            createActualDataNodes("file_transfer_records")
        );

        // Database sharding strategy (by tenant_id)
        config.setDatabaseShardingStrategy(
            new StandardShardingStrategyConfiguration("tenant_id", "database-inline")
        );

        // Table sharding strategy (by id for even distribution)
        config.setTableShardingStrategy(
            new StandardShardingStrategyConfiguration("id", "table-inline")
        );

        return config;
    }

    /**
     * Configure alert history table sharding
     */
    private ShardingTableRuleConfiguration createAlertHistoryTableRule() {
        ShardingTableRuleConfiguration config = new ShardingTableRuleConfiguration(
            "alert_history", 
            createActualDataNodes("alert_history")
        );

        // Database sharding strategy (by tenant_id)
        config.setDatabaseShardingStrategy(
            new StandardShardingStrategyConfiguration("tenant_id", "database-inline")
        );

        // Table sharding strategy (by generated_at for time-based partitioning)
        config.setTableShardingStrategy(
            new StandardShardingStrategyConfiguration("generated_at", "table-time-based")
        );

        return config;
    }

    /**
     * Configure daily file count tracker table sharding
     */
    private ShardingTableRuleConfiguration createDailyFileCountTrackerTableRule() {
        ShardingTableRuleConfiguration config = new ShardingTableRuleConfiguration(
            "daily_file_count_tracker", 
            createActualDataNodes("daily_file_count_tracker")
        );

        // Database sharding strategy (by tenant_id)
        config.setDatabaseShardingStrategy(
            new StandardShardingStrategyConfiguration("tenant_id", "database-inline")
        );

        // Table sharding strategy (by processing_date for date-based partitioning)
        config.setTableShardingStrategy(
            new StandardShardingStrategyConfiguration("processing_date", "table-date-based")
        );

        return config;
    }

    /**
     * Create actual data nodes configuration
     */
    private String createActualDataNodes(String tableName) {
        StringBuilder sb = new StringBuilder();
        
        for (int dbIndex = 0; dbIndex < databaseCount; dbIndex++) {
            for (int tableIndex = 0; tableIndex < tableCount; tableIndex++) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(String.format("ds%d.%s_%d", dbIndex, tableName, tableIndex));
            }
        }
        
        return sb.toString();
    }

    /**
     * Create sharding algorithms
     */
    private Map<String, AlgorithmConfiguration> createShardingAlgorithms() {
        Map<String, AlgorithmConfiguration> algorithms = new HashMap<>();

        // Database sharding algorithm (by tenant_id hash)
        Properties dbProps = new Properties();
        dbProps.setProperty("algorithm-expression", "ds${tenant_id.hashCode() % " + databaseCount + "}");
        algorithms.put("database-inline", new AlgorithmConfiguration("INLINE", dbProps));

        // Table sharding algorithm (by id modulo)
        Properties tableProps = new Properties();
        tableProps.setProperty("algorithm-expression", "${table_name}_${id % " + tableCount + "}");
        algorithms.put("table-inline", new AlgorithmConfiguration("INLINE", tableProps));

        // Time-based table sharding (for alert_history)
        Properties timeProps = new Properties();
        timeProps.setProperty("algorithm-expression", "${table_name}_${generated_at.format('yyyy_MM')}");
        algorithms.put("table-time-based", new AlgorithmConfiguration("INLINE", timeProps));

        // Date-based table sharding (for daily_file_count_tracker)
        Properties dateProps = new Properties();
        dateProps.setProperty("algorithm-expression", "${table_name}_${processing_date.format('yyyy_MM')}");
        algorithms.put("table-date-based", new AlgorithmConfiguration("INLINE", dateProps));

        return algorithms;
    }

    /**
     * Create ShardingSphere properties
     */
    private Properties createShardingSphereProperties() {
        Properties props = new Properties();
        
        // Enable SQL show for debugging (disable in production)
        props.setProperty("sql-show", "false");
        
        // Enable SQL comment parsing
        props.setProperty("sql-comment-parse-enabled", "true");
        
        // Check table metadata compatibility
        props.setProperty("check-table-metadata-enabled", "false");
        
        // Connection pool configuration
        props.setProperty("max-connections-size-per-query", "10");
        
        return props;
    }

    /**
     * Get database URL for sharding instance
     */
    private String getDatabaseUrl(int index) {
        if (index == 0) {
            return primaryDatabaseUrl;
        } else if (index == 1 && secondaryDatabaseUrl != null && !secondaryDatabaseUrl.isEmpty()) {
            return secondaryDatabaseUrl;
        } else {
            // Generate additional database URLs if needed
            return primaryDatabaseUrl.replace("filetransfer", "filetransfer_shard_" + index);
        }
    }

    /**
     * Get database username for sharding instance
     */
    private String getDatabaseUsername(int index) {
        if (index == 0) {
            return primaryUsername;
        } else if (index == 1 && secondaryUsername != null && !secondaryUsername.isEmpty()) {
            return secondaryUsername;
        } else {
            return primaryUsername;
        }
    }

    /**
     * Get database password for sharding instance
     */
    private String getDatabasePassword(int index) {
        if (index == 0) {
            return primaryPassword;
        } else if (index == 1 && secondaryPassword != null && !secondaryPassword.isEmpty()) {
            return secondaryPassword;
        } else {
            return primaryPassword;
        }
    }

    /**
     * Create single data source for non-sharded mode
     */
    private DataSource createSingleDataSource() {
        return createHikariDataSource(primaryDatabaseUrl, primaryUsername, primaryPassword);
    }
}