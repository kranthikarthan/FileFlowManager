package com.filetransfer.web.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Custom exception for File Processing related errors.
 */
public class FileProcessingException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Map<String, Object> details;

    public FileProcessingException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null, null);
    }

    public FileProcessingException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, null, null);
    }

    public FileProcessingException(String message, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    public FileProcessingException(String message, Throwable cause, HttpStatus httpStatus, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }

    // Static factory methods
    public static FileProcessingException validationFailed(String fileName, String reason) {
        String message = String.format("File validation failed for %s: %s", fileName, reason);
        Map<String, Object> details = Map.of(
            "fileName", fileName,
            "reason", reason
        );
        return new FileProcessingException(message, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", details);
    }

    public static FileProcessingException schemaNotFound(String fileName, String fileType) {
        String message = String.format("No schema found for file %s of type %s", fileName, fileType);
        Map<String, Object> details = Map.of(
            "fileName", fileName,
            "fileType", fileType
        );
        return new FileProcessingException(message, HttpStatus.NOT_FOUND, "SCHEMA_NOT_FOUND", details);
    }

    public static FileProcessingException parsingError(String fileName, String reason, Throwable cause) {
        String message = String.format("Failed to parse file %s: %s", fileName, reason);
        Map<String, Object> details = Map.of(
            "fileName", fileName,
            "parseError", reason
        );
        return new FileProcessingException(message, cause, HttpStatus.UNPROCESSABLE_ENTITY, "PARSING_ERROR", details);
    }

    public static FileProcessingException unsupportedFileType(String fileName, String fileType) {
        String message = String.format("Unsupported file type %s for file %s", fileType, fileName);
        Map<String, Object> details = Map.of(
            "fileName", fileName,
            "fileType", fileType
        );
        return new FileProcessingException(message, HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", details);
    }

    public static FileProcessingException processingTimeout(String fileName, long timeoutMs) {
        String message = String.format("File processing timeout for %s after %d ms", fileName, timeoutMs);
        Map<String, Object> details = Map.of(
            "fileName", fileName,
            "timeoutMs", timeoutMs
        );
        return new FileProcessingException(message, HttpStatus.REQUEST_TIMEOUT, "PROCESSING_TIMEOUT", details);
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