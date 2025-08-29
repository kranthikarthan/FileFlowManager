package com.filetransfer.batch.writer;

import com.filetransfer.batch.config.ScalableBatchConfig.ProcessedFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Remote chunk item writer for distributed batch processing
 * Sends processed items to remote workers via messaging
 */
@Component
public class RemoteChunkItemWriter implements ItemWriter<ProcessedFileItem> {

    private static final Logger logger = LoggerFactory.getLogger(RemoteChunkItemWriter.class);

    @Autowired(required = false)
    private StreamBridge streamBridge;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${batch.processing.remote.enabled:false}")
    private boolean remoteProcessingEnabled;

    @Value("${batch.processing.remote.queue-name:file-processing-queue}")
    private String queueName;

    @Value("${batch.processing.remote.result-ttl:3600}")
    private int resultTtlSeconds;

    @Override
    public void write(Chunk<? extends ProcessedFileItem> chunk) throws Exception {
        List<? extends ProcessedFileItem> items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        logger.debug("Writing {} items to remote processing queue", items.size());

        if (!remoteProcessingEnabled) {
            logger.warn("Remote processing is disabled, items will be processed locally");
            return;
        }

        try {
            // Send items to message queue for remote processing
            if (streamBridge != null) {
                sendToMessageQueue(items);
            }

            // Store results in Redis for coordination
            if (redisTemplate != null) {
                storeResultsInRedis(items);
            }

            logger.debug("Successfully sent {} items to remote processing", items.size());

        } catch (Exception e) {
            logger.error("Failed to send items to remote processing", e);
            throw e;
        }
    }

    /**
     * Send items to message queue for remote processing
     */
    private void sendToMessageQueue(List<? extends ProcessedFileItem> items) {
        try {
            for (ProcessedFileItem item : items) {
                RemoteProcessingMessage message = new RemoteProcessingMessage(
                    item.getFileName(),
                    item.getTenantId(),
                    item.getStatus(),
                    item.getProcessingTime(),
                    item.getErrorMessage(),
                    System.currentTimeMillis()
                );

                // Send to message broker
                boolean sent = streamBridge.send(queueName, message);
                
                if (!sent) {
                    logger.warn("Failed to send message for file: {}", item.getFileName());
                } else {
                    logger.debug("Sent processing message for file: {}", item.getFileName());
                }
            }
        } catch (Exception e) {
            logger.error("Error sending items to message queue", e);
            throw new RuntimeException("Failed to send to message queue", e);
        }
    }

    /**
     * Store processing results in Redis for coordination
     */
    private void storeResultsInRedis(List<? extends ProcessedFileItem> items) {
        try {
            for (ProcessedFileItem item : items) {
                String key = String.format("batch:result:%s:%s", 
                                         item.getTenantId(), item.getFileName());
                
                RemoteProcessingResult result = new RemoteProcessingResult(
                    item.getFileName(),
                    item.getTenantId(),
                    item.getStatus(),
                    item.getProcessingTime(),
                    item.getErrorMessage(),
                    item.getProcessedAt(),
                    System.currentTimeMillis()
                );

                // Store in Redis with TTL
                redisTemplate.opsForValue().set(key, result, resultTtlSeconds, TimeUnit.SECONDS);
                
                logger.debug("Stored result in Redis for file: {} with key: {}", 
                           item.getFileName(), key);
            }
        } catch (Exception e) {
            logger.error("Error storing results in Redis", e);
            throw new RuntimeException("Failed to store results in Redis", e);
        }
    }

    /**
     * Send processing completion notification
     */
    public void sendCompletionNotification(String batchId, int itemCount, String status) {
        if (!remoteProcessingEnabled || streamBridge == null) {
            return;
        }

        try {
            BatchCompletionMessage message = new BatchCompletionMessage(
                batchId,
                itemCount,
                status,
                System.currentTimeMillis()
            );

            streamBridge.send("batch-completion", message);
            
            logger.info("Sent batch completion notification for batch: {} with {} items", 
                       batchId, itemCount);

        } catch (Exception e) {
            logger.error("Failed to send batch completion notification for batch: {}", batchId, e);
        }
    }

    /**
     * Get processing status from Redis
     */
    public RemoteProcessingResult getProcessingResult(String tenantId, String fileName) {
        if (redisTemplate == null) {
            return null;
        }

        try {
            String key = String.format("batch:result:%s:%s", tenantId, fileName);
            return (RemoteProcessingResult) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Failed to get processing result for file: {}", fileName, e);
            return null;
        }
    }

    /**
     * Clear processing results from Redis
     */
    public void clearProcessingResults(String tenantId, String fileName) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String key = String.format("batch:result:%s:%s", tenantId, fileName);
            redisTemplate.delete(key);
            
            logger.debug("Cleared processing result for file: {}", fileName);
        } catch (Exception e) {
            logger.error("Failed to clear processing result for file: {}", fileName, e);
        }
    }

    // Message classes for remote processing

    /**
     * Message for remote processing
     */
    public static class RemoteProcessingMessage {
        private String fileName;
        private String tenantId;
        private String status;
        private long processingTime;
        private String errorMessage;
        private long timestamp;

        public RemoteProcessingMessage() {}

        public RemoteProcessingMessage(String fileName, String tenantId, String status, 
                                     long processingTime, String errorMessage, long timestamp) {
            this.fileName = fileName;
            this.tenantId = tenantId;
            this.status = status;
            this.processingTime = processingTime;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Processing result stored in Redis
     */
    public static class RemoteProcessingResult {
        private String fileName;
        private String tenantId;
        private String status;
        private long processingTime;
        private String errorMessage;
        private java.time.LocalDateTime processedAt;
        private long storedAt;

        public RemoteProcessingResult() {}

        public RemoteProcessingResult(String fileName, String tenantId, String status, 
                                    long processingTime, String errorMessage, 
                                    java.time.LocalDateTime processedAt, long storedAt) {
            this.fileName = fileName;
            this.tenantId = tenantId;
            this.status = status;
            this.processingTime = processingTime;
            this.errorMessage = errorMessage;
            this.processedAt = processedAt;
            this.storedAt = storedAt;
        }

        // Getters and setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public java.time.LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(java.time.LocalDateTime processedAt) { this.processedAt = processedAt; }
        public long getStoredAt() { return storedAt; }
        public void setStoredAt(long storedAt) { this.storedAt = storedAt; }
    }

    /**
     * Batch completion message
     */
    public static class BatchCompletionMessage {
        private String batchId;
        private int itemCount;
        private String status;
        private long timestamp;

        public BatchCompletionMessage() {}

        public BatchCompletionMessage(String batchId, int itemCount, String status, long timestamp) {
            this.batchId = batchId;
            this.itemCount = itemCount;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}