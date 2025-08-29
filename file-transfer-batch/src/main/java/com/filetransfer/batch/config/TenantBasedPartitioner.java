package com.filetransfer.batch.config;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant-based partitioner for distributing batch processing workload
 * Partitions data by tenant to ensure even distribution and tenant isolation
 */
@Component
public class TenantBasedPartitioner implements Partitioner {

    @Autowired
    private DataSource dataSource;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        try {
            // Get all active tenants with pending files
            List<String> tenants = getActiveTenants();
            
            if (tenants.isEmpty()) {
                // Create a single default partition if no tenants found
                ExecutionContext context = new ExecutionContext();
                context.putString("tenantId", "default");
                context.putLong("minId", 0L);
                context.putLong("maxId", Long.MAX_VALUE);
                partitions.put("partition0", context);
                return partitions;
            }
            
            // Distribute tenants across partitions
            int tenantsPerPartition = Math.max(1, tenants.size() / gridSize);
            int partitionIndex = 0;
            
            for (int i = 0; i < tenants.size(); i += tenantsPerPartition) {
                ExecutionContext context = new ExecutionContext();
                
                // Assign tenants to this partition
                List<String> partitionTenants = new ArrayList<>();
                for (int j = i; j < Math.min(i + tenantsPerPartition, tenants.size()); j++) {
                    partitionTenants.add(tenants.get(j));
                }
                
                // Set partition parameters
                context.putString("tenants", String.join(",", partitionTenants));
                context.putInt("partitionIndex", partitionIndex);
                context.putInt("totalPartitions", gridSize);
                
                // Get ID range for this partition
                IdRange idRange = getIdRangeForTenants(partitionTenants);
                context.putLong("minId", idRange.minId);
                context.putLong("maxId", idRange.maxId);
                
                partitions.put("partition" + partitionIndex, context);
                partitionIndex++;
                
                if (partitionIndex >= gridSize) {
                    break;
                }
            }
            
            // Fill remaining partitions if needed
            while (partitionIndex < gridSize) {
                ExecutionContext context = new ExecutionContext();
                context.putString("tenants", "");
                context.putInt("partitionIndex", partitionIndex);
                context.putLong("minId", 0L);
                context.putLong("maxId", 0L);
                partitions.put("partition" + partitionIndex, context);
                partitionIndex++;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create partitions", e);
        }
        
        return partitions;
    }

    /**
     * Get list of active tenants with pending file processing
     */
    private List<String> getActiveTenants() throws SQLException {
        List<String> tenants = new ArrayList<>();
        
        String sql = """
            SELECT DISTINCT tenant_id 
            FROM file_transfer_records 
            WHERE status IN ('PENDING', 'PROCESSING') 
            ORDER BY tenant_id
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tenants.add(rs.getString("tenant_id"));
            }
        }
        
        // If no pending records, get all tenants with recent activity
        if (tenants.isEmpty()) {
            sql = """
                SELECT DISTINCT tenant_id 
                FROM file_transfer_records 
                WHERE created_at > NOW() - INTERVAL '1 DAY'
                ORDER BY tenant_id
                """;
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    tenants.add(rs.getString("tenant_id"));
                }
            }
        }
        
        return tenants;
    }

    /**
     * Get ID range for a list of tenants
     */
    private IdRange getIdRangeForTenants(List<String> tenants) throws SQLException {
        if (tenants.isEmpty()) {
            return new IdRange(0L, 0L);
        }
        
        StringBuilder sql = new StringBuilder("""
            SELECT MIN(id) as min_id, MAX(id) as max_id 
            FROM file_transfer_records 
            WHERE tenant_id IN (
            """);
        
        for (int i = 0; i < tenants.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < tenants.size(); i++) {
                stmt.setString(i + 1, tenants.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long minId = rs.getLong("min_id");
                    Long maxId = rs.getLong("max_id");
                    return new IdRange(
                        rs.wasNull() ? 0L : minId,
                        rs.wasNull() ? 0L : maxId
                    );
                }
            }
        }
        
        return new IdRange(0L, 0L);
    }

    /**
     * Helper class for ID ranges
     */
    private static class IdRange {
        final long minId;
        final long maxId;
        
        IdRange(long minId, long maxId) {
            this.minId = minId;
            this.maxId = maxId;
        }
    }
}