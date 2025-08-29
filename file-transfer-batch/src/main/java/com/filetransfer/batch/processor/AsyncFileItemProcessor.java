package com.filetransfer.batch.processor;

import com.filetransfer.batch.config.ScalableBatchConfig.FileProcessingItem;
import com.filetransfer.batch.config.ScalableBatchConfig.ProcessedFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Asynchronous file item processor for non-blocking processing
 * Provides async processing capabilities for better throughput
 */
@Component
public class AsyncFileItemProcessor implements ItemProcessor<FileProcessingItem, ProcessedFileItem> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncFileItemProcessor.class);

    @Autowired
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;

    @Autowired
    private ParallelFileItemProcessor parallelFileItemProcessor;

    @Override
    public ProcessedFileItem process(FileProcessingItem item) throws Exception {
        logger.debug("Starting async processing for file: {}", item.getFileName());
        
        try {
            // Process using the parallel processor asynchronously
            CompletableFuture<ProcessedFileItem> future = 
                parallelFileItemProcessor.processAsync(item);
            
            // Wait for completion (with timeout handling)
            ProcessedFileItem result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            logger.debug("Completed async processing for file: {}", item.getFileName());
            return result;
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Async processing timeout for file: {}", item.getFileName());
            
            ProcessedFileItem timeoutItem = new ProcessedFileItem(
                item.getFileName(), 
                item.getTenantId(), 
                "TIMEOUT"
            );
            timeoutItem.setErrorMessage("Processing timeout after 30 seconds");
            return timeoutItem;
            
        } catch (Exception e) {
            logger.error("Async processing failed for file: {}", item.getFileName(), e);
            
            ProcessedFileItem errorItem = new ProcessedFileItem(
                item.getFileName(), 
                item.getTenantId(), 
                "ERROR"
            );
            errorItem.setErrorMessage("Async processing error: " + e.getMessage());
            return errorItem;
        }
    }

    /**
     * Process multiple items concurrently
     */
    @Async("batchTaskExecutor")
    public CompletableFuture<ProcessedFileItem> processItemAsync(FileProcessingItem item) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return process(item);
            } catch (Exception e) {
                logger.error("Concurrent processing failed for file: {}", item.getFileName(), e);
                
                ProcessedFileItem errorItem = new ProcessedFileItem(
                    item.getFileName(), 
                    item.getTenantId(), 
                    "ERROR"
                );
                errorItem.setErrorMessage(e.getMessage());
                return errorItem;
            }
        }, batchTaskExecutor);
    }

    /**
     * Batch async processing for multiple items
     */
    public CompletableFuture<java.util.List<ProcessedFileItem>> processItemsBatch(
            java.util.List<FileProcessingItem> items) {
        
        logger.info("Starting batch async processing for {} items", items.size());
        
        java.util.List<CompletableFuture<ProcessedFileItem>> futures = items.stream()
            .map(this::processItemAsync)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
}