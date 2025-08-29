package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Service for asynchronous processing of file operations
 * Provides non-blocking operations for improved scalability
 */
@Service
public class AsyncProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingService.class);

    @Autowired
    @Qualifier("fileProcessingExecutor")
    private Executor fileProcessingExecutor;

    @Autowired
    @Qualifier("validationExecutor")
    private Executor validationExecutor;

    @Autowired
    @Qualifier("databaseExecutor")
    private Executor databaseExecutor;

    @Autowired
    private MetricsService metricsService;

    /**
     * Process file asynchronously
     */
    @Async("fileProcessingExecutor")
    public CompletableFuture<FileProcessingResult> processFileAsync(String fileName, byte[] content, String tenantId) {
        logger.info("Starting async file processing for: {} (tenant: {})", fileName, tenantId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate file processing
            FileProcessingResult result = processFile(fileName, content, tenantId);
            
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordFileProcessingTime(
                metricsService.startFileProcessingTimer(), 
                tenantId, 
                "async_processing"
            );
            
            logger.info("Completed async file processing for: {} in {}ms", fileName, duration);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Failed async file processing for: {}", fileName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Validate file asynchronously
     */
    @Async("validationExecutor")
    public CompletableFuture<ValidationResult> validateFileAsync(String fileName, String schemaType, String content, String tenantId) {
        logger.debug("Starting async file validation for: {} (schema: {})", fileName, schemaType);
        
        try {
            ValidationResult result = validateFile(fileName, schemaType, content, tenantId);
            
            logger.debug("Completed async file validation for: {}", fileName);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Failed async file validation for: {}", fileName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process multiple files in parallel
     */
    public CompletableFuture<List<FileProcessingResult>> processFilesInParallel(List<FileProcessingRequest> requests) {
        logger.info("Starting parallel processing of {} files", requests.size());
        
        List<CompletableFuture<FileProcessingResult>> futures = requests.stream()
            .map(request -> processFileAsync(request.getFileName(), request.getContent(), request.getTenantId()))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Save data asynchronously to database
     */
    @Async("databaseExecutor")
    public CompletableFuture<Void> saveDataAsync(Object entity, String tenantId) {
        logger.debug("Starting async database save for tenant: {}", tenantId);
        
        try {
            // Simulate database save operation
            saveToDatabase(entity);
            
            logger.debug("Completed async database save for tenant: {}", tenantId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed async database save for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Batch process files asynchronously
     */
    @Async("fileProcessingExecutor")
    public CompletableFuture<BatchProcessingResult> processBatchAsync(List<FileProcessingRequest> batch, String tenantId) {
        logger.info("Starting async batch processing of {} files for tenant: {}", batch.size(), tenantId);
        
        long startTime = System.currentTimeMillis();
        BatchProcessingResult result = new BatchProcessingResult();
        
        try {
            for (FileProcessingRequest request : batch) {
                try {
                    FileProcessingResult fileResult = processFile(
                        request.getFileName(), 
                        request.getContent(), 
                        request.getTenantId()
                    );
                    
                    if (fileResult.isSuccess()) {
                        result.addSuccess(fileResult);
                    } else {
                        result.addFailure(fileResult);
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to process file in batch: {}", request.getFileName(), e);
                    result.addFailure(new FileProcessingResult(
                        request.getFileName(), 
                        false, 
                        "Processing failed: " + e.getMessage()
                    ));
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            result.setProcessingTime(duration);
            
            logger.info("Completed async batch processing for tenant: {} in {}ms (success: {}, failures: {})", 
                       tenantId, duration, result.getSuccessCount(), result.getFailureCount());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Failed async batch processing for tenant: {}", tenantId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process file with retry logic
     */
    @Async("fileProcessingExecutor")
    public CompletableFuture<FileProcessingResult> processFileWithRetry(String fileName, byte[] content, String tenantId, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    logger.debug("Processing file: {} (attempt {}/{})", fileName, attempt, maxRetries);
                    
                    FileProcessingResult result = processFile(fileName, content, tenantId);
                    
                    if (result.isSuccess()) {
                        logger.info("Successfully processed file: {} on attempt {}", fileName, attempt);
                        return result;
                    } else {
                        lastException = new RuntimeException(result.getErrorMessage());
                    }
                    
                } catch (Exception e) {
                    lastException = e;
                    logger.warn("File processing attempt {} failed for: {}", attempt, fileName, e);
                    
                    if (attempt < maxRetries) {
                        try {
                            // Exponential backoff
                            Thread.sleep(1000 * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            logger.error("Failed to process file: {} after {} attempts", fileName, maxRetries);
            throw new RuntimeException("Processing failed after " + maxRetries + " attempts", lastException);
            
        }, fileProcessingExecutor);
    }

    /**
     * Generate reports asynchronously
     */
    @Async("databaseExecutor")
    public CompletableFuture<ReportGenerationResult> generateReportAsync(String reportType, String tenantId, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        logger.info("Starting async report generation: {} for tenant: {} (period: {} to {})", 
                   reportType, tenantId, fromDate, toDate);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate report generation
            ReportGenerationResult result = generateReport(reportType, tenantId, fromDate, toDate);
            
            long duration = System.currentTimeMillis() - startTime;
            result.setGenerationTime(duration);
            
            logger.info("Completed async report generation: {} in {}ms", reportType, duration);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Failed async report generation: {}", reportType, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // Private helper methods

    private FileProcessingResult processFile(String fileName, byte[] content, String tenantId) {
        // Simulate file processing logic
        try {
            Thread.sleep(100 + (int)(Math.random() * 200)); // Simulate processing time
            
            if (Math.random() > 0.9) { // 10% failure rate for simulation
                return new FileProcessingResult(fileName, false, "Simulated processing failure");
            }
            
            return new FileProcessingResult(fileName, true, "Processing completed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new FileProcessingResult(fileName, false, "Processing interrupted");
        }
    }

    private ValidationResult validateFile(String fileName, String schemaType, String content, String tenantId) {
        // Simulate file validation logic
        try {
            Thread.sleep(50 + (int)(Math.random() * 100)); // Simulate validation time
            
            if (Math.random() > 0.95) { // 5% failure rate for simulation
                return new ValidationResult(false, "Simulated validation failure");
            }
            
            return new ValidationResult(true, "Validation passed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ValidationResult(false, "Validation interrupted");
        }
    }

    private void saveToDatabase(Object entity) {
        // Simulate database save operation
        try {
            Thread.sleep(20 + (int)(Math.random() * 30)); // Simulate DB save time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Database save interrupted", e);
        }
    }

    private ReportGenerationResult generateReport(String reportType, String tenantId, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        // Simulate report generation
        try {
            Thread.sleep(1000 + (int)(Math.random() * 2000)); // Simulate report generation time
            
            return new ReportGenerationResult(
                reportType, 
                tenantId, 
                fromDate, 
                toDate, 
                true, 
                "Report generated successfully"
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }
    }

    // Result classes

    public static class FileProcessingRequest {
        private final String fileName;
        private final byte[] content;
        private final String tenantId;

        public FileProcessingRequest(String fileName, byte[] content, String tenantId) {
            this.fileName = fileName;
            this.content = content;
            this.tenantId = tenantId;
        }

        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
        public String getTenantId() { return tenantId; }
    }

    public static class FileProcessingResult {
        private final String fileName;
        private final boolean success;
        private final String message;
        private final String errorMessage;

        public FileProcessingResult(String fileName, boolean success, String message) {
            this.fileName = fileName;
            this.success = success;
            this.message = message;
            this.errorMessage = success ? null : message;
        }

        public String getFileName() { return fileName; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class BatchProcessingResult {
        private final java.util.List<FileProcessingResult> successes = new java.util.ArrayList<>();
        private final java.util.List<FileProcessingResult> failures = new java.util.ArrayList<>();
        private long processingTime;

        public void addSuccess(FileProcessingResult result) {
            successes.add(result);
        }

        public void addFailure(FileProcessingResult result) {
            failures.add(result);
        }

        public java.util.List<FileProcessingResult> getSuccesses() { return successes; }
        public java.util.List<FileProcessingResult> getFailures() { return failures; }
        public int getSuccessCount() { return successes.size(); }
        public int getFailureCount() { return failures.size(); }
        public int getTotalCount() { return getSuccessCount() + getFailureCount(); }
        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        
        public double getSuccessRate() {
            int total = getTotalCount();
            return total > 0 ? (double) getSuccessCount() / total * 100 : 0.0;
        }
    }

    public static class ReportGenerationResult {
        private final String reportType;
        private final String tenantId;
        private final java.time.LocalDate fromDate;
        private final java.time.LocalDate toDate;
        private final boolean success;
        private final String message;
        private long generationTime;

        public ReportGenerationResult(String reportType, String tenantId, java.time.LocalDate fromDate, 
                                    java.time.LocalDate toDate, boolean success, String message) {
            this.reportType = reportType;
            this.tenantId = tenantId;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.success = success;
            this.message = message;
        }

        public String getReportType() { return reportType; }
        public String getTenantId() { return tenantId; }
        public java.time.LocalDate getFromDate() { return fromDate; }
        public java.time.LocalDate getToDate() { return toDate; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getGenerationTime() { return generationTime; }
        public void setGenerationTime(long generationTime) { this.generationTime = generationTime; }
    }
}