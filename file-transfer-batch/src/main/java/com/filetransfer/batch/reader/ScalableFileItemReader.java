package com.filetransfer.batch.reader;

import com.filetransfer.batch.config.ScalableBatchConfig.FileProcessingItem;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Scalable file item reader with pagination and partitioning support
 * Reads file processing items from database with optimized queries
 */
@Component
@StepScope
public class ScalableFileItemReader extends JdbcPagingItemReader<FileProcessingItem> {

    @Autowired
    private DataSource dataSource;

    @Value("#{stepExecutionContext['tenants']}")
    private String tenants;

    @Value("#{stepExecutionContext['minId']}")
    private Long minId;

    @Value("#{stepExecutionContext['maxId']}")
    private Long maxId;

    @Value("${batch.processing.page-size:1000}")
    private int pageSize;

    public ScalableFileItemReader() {
        setName("scalableFileItemReader");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Set data source
        setDataSource(dataSource);
        
        // Set page size for pagination
        setPageSize(pageSize);
        
        // Set row mapper
        setRowMapper(new FileProcessingItemRowMapper());
        
        // Create and configure query provider
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id, file_name, tenant_id, file_path, file_size, file_type, status, created_at");
        queryProvider.setFromClause("FROM file_transfer_records");
        queryProvider.setWhereClause(buildWhereClause());
        
        // Set sort keys for pagination
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);
        
        setQueryProvider(queryProvider);
        
        // Set parameters
        setParameterValues(buildParameterValues());
        
        super.afterPropertiesSet();
    }

    /**
     * Build WHERE clause based on partition parameters
     */
    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("status IN ('PENDING', 'PROCESSING')");
        
        // Add tenant filter if specified
        if (tenants != null && !tenants.trim().isEmpty() && !tenants.equals("")) {
            String[] tenantArray = tenants.split(",");
            if (tenantArray.length == 1) {
                whereClause.append(" AND tenant_id = :tenantId");
            } else {
                whereClause.append(" AND tenant_id IN (");
                for (int i = 0; i < tenantArray.length; i++) {
                    if (i > 0) whereClause.append(", ");
                    whereClause.append(":tenant").append(i);
                }
                whereClause.append(")");
            }
        }
        
        // Add ID range filter if specified
        if (minId != null && minId > 0) {
            whereClause.append(" AND id >= :minId");
        }
        if (maxId != null && maxId > 0) {
            whereClause.append(" AND id <= :maxId");
        }
        
        return whereClause.toString();
    }

    /**
     * Build parameter values for the query
     */
    private Map<String, Object> buildParameterValues() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Add tenant parameters
        if (tenants != null && !tenants.trim().isEmpty() && !tenants.equals("")) {
            String[] tenantArray = tenants.split(",");
            if (tenantArray.length == 1) {
                parameters.put("tenantId", tenantArray[0].trim());
            } else {
                for (int i = 0; i < tenantArray.length; i++) {
                    parameters.put("tenant" + i, tenantArray[i].trim());
                }
            }
        }
        
        // Add ID range parameters
        if (minId != null && minId > 0) {
            parameters.put("minId", minId);
        }
        if (maxId != null && maxId > 0) {
            parameters.put("maxId", maxId);
        }
        
        return parameters;
    }

    /**
     * Row mapper for FileProcessingItem
     */
    private static class FileProcessingItemRowMapper implements RowMapper<FileProcessingItem> {
        @Override
        public FileProcessingItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            FileProcessingItem item = new FileProcessingItem();
            item.setFileName(rs.getString("file_name"));
            item.setTenantId(rs.getString("tenant_id"));
            item.setFilePath(rs.getString("file_path"));
            item.setFileSize(rs.getLong("file_size"));
            item.setFileType(rs.getString("file_type"));
            return item;
        }
    }
}