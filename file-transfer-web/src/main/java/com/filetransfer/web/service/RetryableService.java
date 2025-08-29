package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

/**
 * Service providing retry mechanisms for operations that may fail transiently.
 * Uses Spring Retry for automatic retry with exponential backoff.
 */
@Service
public class RetryableService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryableService.class);

    /**
     * Retryable database operation with exponential backoff
     */
    @Retryable(
        value = {SQLException.class, org.springframework.dao.DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public <T> T executeWithRetry(DatabaseOperation<T> operation, String operationName) {
        logger.debug("Executing retryable database operation: {}", operationName);
        try {
            return operation.execute();
        } catch (Exception e) {
            logger.warn("Database operation failed, will retry: {} - {}", operationName, e.getMessage());
            throw e;
        }
    }

    /**
     * Retryable external service call with fixed delay
     */
    @Retryable(
        value = {TimeoutException.class, java.net.ConnectException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000)
    )
    public <T> T callExternalServiceWithRetry(ExternalServiceCall<T> serviceCall, String serviceName) {
        logger.debug("Calling external service with retry: {}", serviceName);
        try {
            return serviceCall.call();
        } catch (Exception e) {
            logger.warn("External service call failed, will retry: {} - {}", serviceName, e.getMessage());
            throw e;
        }
    }

    /**
     * Retryable file processing operation
     */
    @Retryable(
        value = {java.io.IOException.class, org.springframework.dao.TransientDataAccessException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 500, multiplier = 1.5)
    )
    public <T> T processFileWithRetry(FileProcessingOperation<T> operation, String fileName) {
        logger.debug("Processing file with retry: {}", fileName);
        try {
            return operation.process();
        } catch (Exception e) {
            logger.warn("File processing failed, will retry: {} - {}", fileName, e.getMessage());
            throw e;
        }
    }

    /**
     * Recovery method for database operations
     */
    @Recover
    public <T> T recoverDatabaseOperation(SQLException ex, DatabaseOperation<T> operation, String operationName) {
        logger.error("Database operation failed after all retries: {} - {}", operationName, ex.getMessage());
        throw new RuntimeException("Database operation failed permanently: " + operationName, ex);
    }

    /**
     * Recovery method for external service calls
     */
    @Recover
    public <T> T recoverExternalServiceCall(Exception ex, ExternalServiceCall<T> serviceCall, String serviceName) {
        logger.error("External service call failed after all retries: {} - {}", serviceName, ex.getMessage());
        throw new RuntimeException("External service unavailable: " + serviceName, ex);
    }

    /**
     * Recovery method for file processing
     */
    @Recover
    public <T> T recoverFileProcessing(Exception ex, FileProcessingOperation<T> operation, String fileName) {
        logger.error("File processing failed after all retries: {} - {}", fileName, ex.getMessage());
        throw new RuntimeException("File processing failed permanently: " + fileName, ex);
    }

    // Functional interfaces for operations
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws SQLException;
    }

    @FunctionalInterface
    public interface ExternalServiceCall<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    public interface FileProcessingOperation<T> {
        T process() throws Exception;
    }
}