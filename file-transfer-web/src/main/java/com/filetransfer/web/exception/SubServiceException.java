package com.filetransfer.web.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Custom exception for SubService related errors.
 * Provides structured error information for better debugging and user feedback.
 */
public class SubServiceException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Map<String, Object> details;

    public SubServiceException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null, null);
    }

    public SubServiceException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, null, null);
    }

    public SubServiceException(String message, String errorCode) {
        this(message, HttpStatus.BAD_REQUEST, errorCode, null);
    }

    public SubServiceException(String message, HttpStatus httpStatus, String errorCode) {
        this(message, httpStatus, errorCode, null);
    }

    public SubServiceException(String message, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    public SubServiceException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
    }

    public SubServiceException(String message, Throwable cause, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    // Static factory methods for common scenarios
    public static SubServiceException notFound(String tenantId, String serviceName, String subServiceName) {
        String message = String.format("SubService not found: %s/%s for tenant %s", 
                                      serviceName, subServiceName, tenantId);
        return new SubServiceException(message, HttpStatus.NOT_FOUND, "SUBSERVICE_NOT_FOUND");
    }

    public static SubServiceException alreadyExists(String tenantId, String serviceName, String subServiceName) {
        String message = String.format("SubService already exists: %s/%s for tenant %s", 
                                      serviceName, subServiceName, tenantId);
        return new SubServiceException(message, HttpStatus.CONFLICT, "SUBSERVICE_ALREADY_EXISTS");
    }

    public static SubServiceException disabled(String tenantId, String serviceName, String subServiceName) {
        String message = String.format("SubService is disabled: %s/%s for tenant %s", 
                                      serviceName, subServiceName, tenantId);
        return new SubServiceException(message, HttpStatus.FORBIDDEN, "SUBSERVICE_DISABLED");
    }

    public static SubServiceException configurationError(String message, Map<String, Object> details) {
        return new SubServiceException(message, HttpStatus.BAD_REQUEST, "CONFIGURATION_ERROR", details);
    }

    public static SubServiceException validationError(String message, Map<String, Object> details) {
        return new SubServiceException(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details);
    }

    public static SubServiceException processingError(String message, Throwable cause) {
        return new SubServiceException(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "PROCESSING_ERROR", null);
    }

    // Getters
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}