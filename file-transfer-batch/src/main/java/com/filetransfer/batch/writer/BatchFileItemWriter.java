package com.filetransfer.batch.writer;

import com.filetransfer.batch.config.ScalableBatchConfig.ProcessedFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batch file item writer with optimized database operations
 * Implements batch writing for high-performance data persistence
 */
@Component
public class BatchFileItemWriter implements ItemWriter<ProcessedFileItem> {

    private static final Logger logger = LoggerFactory.getLogger(BatchFileItemWriter.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${batch.processing.batch-insert-size:100}")
    private int batchInsertSize;

    @Value("${batch.processing.enable-metrics:true}")
    private boolean enableMetrics;

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public void write(Chunk<? extends ProcessedFileItem> chunk) throws Exception {
        List<? extends ProcessedFileItem> items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        logger.debug("Writing batch of {} processed file items", items.size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Update file transfer records status
            updateFileTransferRecords(items);
            
            // Insert processing results
            insertProcessingResults(items);
            
            // Update metrics
            if (enableMetrics) {
                updateMetrics(items);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Successfully wrote batch of {} items in {}ms", items.size(), duration);
            
        } catch (Exception e) {
            logger.error("Failed to write batch of {} items", items.size(), e);
            errorCount.addAndGet(items.size());
            throw e;
        }
    }

    /**
     * Update file transfer records with processing status
     */
    private void updateFileTransferRecords(List<? extends ProcessedFileItem> items) {
        String sql = """
            UPDATE file_transfer_records 
            SET status = ?, 
                updated_at = ?, 
                processing_time_ms = ?,
                error_message = ?
            WHERE file_name = ? AND tenant_id = ?
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ProcessedFileItem item = items.get(i);
                ps.setString(1, item.getStatus());
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(3, item.getProcessingTime());
                ps.setString(4, item.getErrorMessage());
                ps.setString(5, item.getFileName());
                ps.setString(6, item.getTenantId());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        logger.debug("Updated {} file transfer records", items.size());
    }

    /**
     * Insert processing results into batch processing log
     */
    private void insertProcessingResults(List<? extends ProcessedFileItem> items) {
        String sql = """
            INSERT INTO batch_processing_log 
            (file_name, tenant_id, status, processing_time_ms, error_message, processed_at, batch_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String batchId = generateBatchId();

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ProcessedFileItem item = items.get(i);
                ps.setString(1, item.getFileName());
                ps.setString(2, item.getTenantId());
                ps.setString(3, item.getStatus());
                ps.setLong(4, item.getProcessingTime());
                ps.setString(5, item.getErrorMessage());
                ps.setTimestamp(6, Timestamp.valueOf(item.getProcessedAt()));
                ps.setString(7, batchId);
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        logger.debug("Inserted {} processing results with batch ID: {}", items.size(), batchId);
    }

    /**
     * Update processing metrics
     */
    private void updateMetrics(List<? extends ProcessedFileItem> items) {
        long successCount = items.stream()
            .mapToLong(item -> "COMPLETED".equals(item.getStatus()) ? 1 : 0)
            .sum();
        
        long failureCount = items.size() - successCount;
        
        processedCount.addAndGet(successCount);
        errorCount.addAndGet(failureCount);
        
        // Update metrics in database
        String sql = """
            INSERT INTO batch_metrics 
            (metric_name, metric_value, recorded_at, tenant_id)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
            metric_value = metric_value + VALUES(metric_value),
            recorded_at = VALUES(recorded_at)
            """;

        // Group by tenant for metrics
        items.stream()
            .collect(java.util.stream.Collectors.groupingBy(ProcessedFileItem::getTenantId))
            .forEach((tenantId, tenantItems) -> {
                long tenantSuccessCount = tenantItems.stream()
                    .mapToLong(item -> "COMPLETED".equals(item.getStatus()) ? 1 : 0)
                    .sum();
                
                long tenantFailureCount = tenantItems.size() - tenantSuccessCount;
                
                if (tenantSuccessCount > 0) {
                    jdbcTemplate.update(sql, "files_processed_success", tenantSuccessCount, 
                                      Timestamp.valueOf(LocalDateTime.now()), tenantId);
                }
                
                if (tenantFailureCount > 0) {
                    jdbcTemplate.update(sql, "files_processed_error", tenantFailureCount, 
                                      Timestamp.valueOf(LocalDateTime.now()), tenantId);
                }
            });
    }

    /**
     * Generate unique batch ID
     */
    private String generateBatchId() {
        return "BATCH_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId();
    }

    /**
     * Get processing statistics
     */
    public ProcessingStats getProcessingStats() {
        return new ProcessingStats(
            processedCount.get(),
            errorCount.get(),
            LocalDateTime.now()
        );
    }

    /**
     * Reset processing statistics
     */
    public void resetStats() {
        processedCount.set(0);
        errorCount.set(0);
    }

    /**
     * Processing statistics container
     */
    public static class ProcessingStats {
        private final long processedCount;
        private final long errorCount;
        private final LocalDateTime lastUpdated;

        public ProcessingStats(long processedCount, long errorCount, LocalDateTime lastUpdated) {
            this.processedCount = processedCount;
            this.errorCount = errorCount;
            this.lastUpdated = lastUpdated;
        }

        public long getProcessedCount() { return processedCount; }
        public long getErrorCount() { return errorCount; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public long getTotalCount() { return processedCount + errorCount; }
        
        public double getSuccessRate() {
            long total = getTotalCount();
            return total > 0 ? (double) processedCount / total * 100.0 : 0.0;
        }
    }
}