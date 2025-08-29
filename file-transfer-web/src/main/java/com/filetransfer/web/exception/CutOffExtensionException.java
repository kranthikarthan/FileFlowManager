package com.filetransfer.web.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Custom exception for Cut-off Extension related errors.
 */
public class CutOffExtensionException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Map<String, Object> details;

    public CutOffExtensionException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null, null);
    }

    public CutOffExtensionException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, null, null);
    }

    public CutOffExtensionException(String message, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    public CutOffExtensionException(String message, Throwable cause, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    // Static factory methods
    public static CutOffExtensionException notFound(Long extensionId) {
        String message = String.format("Cut-off extension not found with ID: %d", extensionId);
        return new CutOffExtensionException(message, HttpStatus.NOT_FOUND, "EXTENSION_NOT_FOUND", null);
    }

    public static CutOffExtensionException invalidStatus(Long extensionId, String currentStatus, String expectedStatus) {
        String message = String.format("Extension %d is in status %s, expected %s", 
                                      extensionId, currentStatus, expectedStatus);
        Map<String, Object> details = Map.of(
            "extensionId", extensionId,
            "currentStatus", currentStatus,
            "expectedStatus", expectedStatus
        );
        return new CutOffExtensionException(message, HttpStatus.CONFLICT, "INVALID_STATUS", details);
    }

    public static CutOffExtensionException pastDate(String date) {
        String message = String.format("Cannot request extension for past date: %s", date);
        return new CutOffExtensionException(message, HttpStatus.BAD_REQUEST, "PAST_DATE", null);
    }

    public static CutOffExtensionException duplicateRequest(String tenantId, String serviceName, String subServiceName, String date) {
        String message = String.format("Extension already exists for %s/%s on %s", serviceName, subServiceName, date);
        Map<String, Object> details = Map.of(
            "tenantId", tenantId,
            "serviceName", serviceName,
            "subServiceName", subServiceName,
            "date", date
        );
        return new CutOffExtensionException(message, HttpStatus.CONFLICT, "DUPLICATE_REQUEST", details);
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