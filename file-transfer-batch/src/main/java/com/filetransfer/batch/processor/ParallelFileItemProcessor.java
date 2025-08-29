package com.filetransfer.batch.processor;

import com.filetransfer.batch.config.ScalableBatchConfig.FileProcessingItem;
import com.filetransfer.batch.config.ScalableBatchConfig.ProcessedFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Parallel file item processor with caching and retry capabilities
 * Processes files with optimized performance and error handling
 */
@Component
public class ParallelFileItemProcessor implements ItemProcessor<FileProcessingItem, ProcessedFileItem> {

    private static final Logger logger = LoggerFactory.getLogger(ParallelFileItemProcessor.class);

    @Value("${batch.processing.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${batch.processing.simulation.enabled:false}")
    private boolean simulationMode;

    @Value("${batch.processing.max-file-size:104857600}") // 100MB
    private long maxFileSize;

    @Autowired(required = false)
    private FileValidationService fileValidationService;

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ProcessedFileItem process(FileProcessingItem item) throws Exception {
        logger.debug("Processing file: {} for tenant: {}", item.getFileName(), item.getTenantId());
        
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Validate input
            validateFileItem(item);
            
            // Process file based on type
            ProcessingResult result = processFileByType(item);
            
            // Calculate processing time
            long processingTime = Duration.between(startTime, LocalDateTime.now()).toMillis();
            
            // Create processed item
            ProcessedFileItem processedItem = new ProcessedFileItem(
                item.getFileName(), 
                item.getTenantId(), 
                result.isSuccess() ? "COMPLETED" : "FAILED"
            );
            
            processedItem.setProcessingTime(processingTime);
            
            if (!result.isSuccess()) {
                processedItem.setErrorMessage(result.getErrorMessage());
            }
            
            logger.debug("Completed processing file: {} in {}ms", 
                        item.getFileName(), processingTime);
            
            return processedItem;
            
        } catch (Exception e) {
            logger.error("Failed to process file: {} for tenant: {}", 
                        item.getFileName(), item.getTenantId(), e);
            
            long processingTime = Duration.between(startTime, LocalDateTime.now()).toMillis();
            
            ProcessedFileItem errorItem = new ProcessedFileItem(
                item.getFileName(), 
                item.getTenantId(), 
                "ERROR"
            );
            errorItem.setProcessingTime(processingTime);
            errorItem.setErrorMessage(e.getMessage());
            
            return errorItem;
        }
    }

    /**
     * Validate file item before processing
     */
    private void validateFileItem(FileProcessingItem item) throws ValidationException {
        if (item.getFileName() == null || item.getFileName().trim().isEmpty()) {
            throw new ValidationException("File name is required");
        }
        
        if (item.getTenantId() == null || item.getTenantId().trim().isEmpty()) {
            throw new ValidationException("Tenant ID is required");
        }
        
        if (item.getFileSize() > maxFileSize) {
            throw new ValidationException("File size exceeds maximum allowed size: " + maxFileSize);
        }
        
        if (item.getFileSize() <= 0) {
            throw new ValidationException("Invalid file size: " + item.getFileSize());
        }
    }

    /**
     * Process file based on its type
     */
    private ProcessingResult processFileByType(FileProcessingItem item) throws IOException {
        if (simulationMode) {
            return simulateFileProcessing(item);
        }
        
        return switch (item.getFileType().toUpperCase()) {
            case "CSV" -> processCsvFile(item);
            case "XML" -> processXmlFile(item);
            case "JSON" -> processJsonFile(item);
            case "COBOL" -> processCobolFile(item);
            case "BINARY" -> processBinaryFile(item);
            default -> processGenericFile(item);
        };
    }

    /**
     * Process CSV file
     */
    @Cacheable(value = "csvValidationCache", key = "#item.fileName + '_' + #item.fileSize")
    private ProcessingResult processCsvFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing CSV file: {}", item.getFileName());
        
        // Simulate CSV processing
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
            
            if (validationEnabled && fileValidationService != null) {
                boolean isValid = fileValidationService.validateCsvFile(item.getFilePath());
                if (!isValid) {
                    return ProcessingResult.failure("CSV validation failed");
                }
            }
            
            // Process CSV content
            processFileContent(item);
            
            return ProcessingResult.success("CSV file processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process XML file
     */
    @Cacheable(value = "xmlValidationCache", key = "#item.fileName + '_' + #item.fileSize")
    private ProcessingResult processXmlFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing XML file: {}", item.getFileName());
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(150, 600));
            
            if (validationEnabled && fileValidationService != null) {
                boolean isValid = fileValidationService.validateXmlFile(item.getFilePath());
                if (!isValid) {
                    return ProcessingResult.failure("XML validation failed");
                }
            }
            
            processFileContent(item);
            
            return ProcessingResult.success("XML file processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process JSON file
     */
    @Cacheable(value = "jsonValidationCache", key = "#item.fileName + '_' + #item.fileSize")
    private ProcessingResult processJsonFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing JSON file: {}", item.getFileName());
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(80, 400));
            
            if (validationEnabled && fileValidationService != null) {
                boolean isValid = fileValidationService.validateJsonFile(item.getFilePath());
                if (!isValid) {
                    return ProcessingResult.failure("JSON validation failed");
                }
            }
            
            processFileContent(item);
            
            return ProcessingResult.success("JSON file processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process COBOL file
     */
    private ProcessingResult processCobolFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing COBOL file: {}", item.getFileName());
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 800));
            
            if (validationEnabled && fileValidationService != null) {
                boolean isValid = fileValidationService.validateCobolFile(item.getFilePath());
                if (!isValid) {
                    return ProcessingResult.failure("COBOL validation failed");
                }
            }
            
            processFileContent(item);
            
            return ProcessingResult.success("COBOL file processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process binary file
     */
    private ProcessingResult processBinaryFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing binary file: {}", item.getFileName());
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(300, 1000));
            
            // Binary files typically don't require schema validation
            processFileContent(item);
            
            return ProcessingResult.success("Binary file processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process generic file
     */
    private ProcessingResult processGenericFile(FileProcessingItem item) throws IOException {
        logger.debug("Processing generic file: {}", item.getFileName());
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 400));
            
            processFileContent(item);
            
            return ProcessingResult.success("File processed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Simulate file processing for testing
     */
    private ProcessingResult simulateFileProcessing(FileProcessingItem item) {
        try {
            // Simulate variable processing time
            int processingTime = ThreadLocalRandom.current().nextInt(50, 500);
            Thread.sleep(processingTime);
            
            // Simulate 95% success rate
            if (ThreadLocalRandom.current().nextDouble() > 0.05) {
                return ProcessingResult.success("Simulated processing completed");
            } else {
                return ProcessingResult.failure("Simulated processing failure");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessingResult.failure("Processing interrupted");
        }
    }

    /**
     * Process file content (common logic)
     */
    private void processFileContent(FileProcessingItem item) throws IOException {
        if (item.getFilePath() != null) {
            Path filePath = Paths.get(item.getFilePath());
            if (Files.exists(filePath)) {
                // Process actual file content
                logger.debug("Processing file content from: {}", item.getFilePath());
                // Implement actual file processing logic here
            }
        }
    }

    /**
     * Async processing method for non-blocking operations
     */
    public CompletableFuture<ProcessedFileItem> processAsync(FileProcessingItem item) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return process(item);
            } catch (Exception e) {
                logger.error("Async processing failed for file: {}", item.getFileName(), e);
                
                ProcessedFileItem errorItem = new ProcessedFileItem(
                    item.getFileName(), 
                    item.getTenantId(), 
                    "ERROR"
                );
                errorItem.setErrorMessage(e.getMessage());
                return errorItem;
            }
        });
    }

    // Helper classes

    /**
     * Processing result container
     */
    private static class ProcessingResult {
        private final boolean success;
        private final String message;
        private final String errorMessage;

        private ProcessingResult(boolean success, String message, String errorMessage) {
            this.success = success;
            this.message = message;
            this.errorMessage = errorMessage;
        }

        public static ProcessingResult success(String message) {
            return new ProcessingResult(true, message, null);
        }

        public static ProcessingResult failure(String errorMessage) {
            return new ProcessingResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Validation exception
     */
    private static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * File validation service interface
     */
    public interface FileValidationService {
        boolean validateCsvFile(String filePath);
        boolean validateXmlFile(String filePath);
        boolean validateJsonFile(String filePath);
        boolean validateCobolFile(String filePath);
    }
}