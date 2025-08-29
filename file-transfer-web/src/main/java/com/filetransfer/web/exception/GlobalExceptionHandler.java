package com.filetransfer.web.exception;

import com.filetransfer.web.service.SecurityContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for comprehensive error handling and logging.
 * Provides consistent error responses and detailed logging for debugging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private SecurityContextService securityContextService;

    /**
     * Handle SubService specific exceptions
     */
    @ExceptionHandler(SubServiceException.class)
    public ResponseEntity<ErrorResponse> handleSubServiceException(SubServiceException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(ex.getHttpStatus().value())
            .error(ex.getHttpStatus().getReasonPhrase())
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("SUBSERVICE_ERROR")
            .details(ex.getDetails())
            .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle Cut-off Extension specific exceptions
     */
    @ExceptionHandler(CutOffExtensionException.class)
    public ResponseEntity<ErrorResponse> handleCutOffExtensionException(CutOffExtensionException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(ex.getHttpStatus().value())
            .error(ex.getHttpStatus().getReasonPhrase())
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("CUTOFF_EXTENSION_ERROR")
            .details(ex.getDetails())
            .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle File Processing specific exceptions
     */
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessingException(FileProcessingException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(ex.getHttpStatus().value())
            .error(ex.getHttpStatus().getReasonPhrase())
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("FILE_PROCESSING_ERROR")
            .details(ex.getDetails())
            .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationExceptions(Exception ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        Map<String, String> validationErrors = new HashMap<>();
        
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validationEx = (MethodArgumentNotValidException) ex;
            validationEx.getBindingResult().getFieldErrors().forEach(error -> 
                validationErrors.put(error.getField(), error.getDefaultMessage()));
        } else if (ex instanceof BindException) {
            BindException bindEx = (BindException) ex;
            bindEx.getBindingResult().getFieldErrors().forEach(error -> 
                validationErrors.put(error.getField(), error.getDefaultMessage()));
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .path(getPath(request))
            .errorType("VALIDATION_ERROR")
            .details(validationErrors)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            violations.put(propertyPath, message);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Data constraint violation")
            .path(getPath(request))
            .errorType("CONSTRAINT_VIOLATION")
            .details(violations)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        String message = "Data integrity violation";
        Map<String, String> details = new HashMap<>();
        
        // Extract meaningful information from the exception
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        
        if (rootCauseMessage.contains("unique constraint") || rootCauseMessage.contains("duplicate")) {
            message = "Duplicate record detected";
            details.put("constraint", "UNIQUE_VIOLATION");
        } else if (rootCauseMessage.contains("foreign key")) {
            message = "Referenced record not found";
            details.put("constraint", "FOREIGN_KEY_VIOLATION");
        } else if (rootCauseMessage.contains("not null")) {
            message = "Required field is missing";
            details.put("constraint", "NOT_NULL_VIOLATION");
        }
        
        details.put("technical_details", rootCauseMessage);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Data Integrity Violation")
            .message(message)
            .path(getPath(request))
            .errorType("DATA_INTEGRITY_ERROR")
            .details(details)
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("ENTITY_NOT_FOUND")
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Illegal State")
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("ILLEGAL_STATE")
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .path(getPath(request))
            .errorType("ILLEGAL_ARGUMENT")
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String errorId = generateErrorId();
        logError(errorId, ex, request);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(getPath(request))
            .errorType("INTERNAL_ERROR")
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Log error with context information
     */
    private void logError(String errorId, Exception ex, WebRequest request) {
        try {
            // Add context to MDC for structured logging
            MDC.put("errorId", errorId);
            MDC.put("userId", securityContextService.getCurrentUserId());
            MDC.put("requestPath", getPath(request));
            MDC.put("userAgent", request.getHeader("User-Agent"));
            MDC.put("remoteAddr", request.getHeader("X-Forwarded-For"));

            if (ex instanceof SubServiceException || 
                ex instanceof CutOffExtensionException || 
                ex instanceof FileProcessingException) {
                // Log business exceptions at WARN level
                logger.warn("Business exception occurred: {}", ex.getMessage(), ex);
            } else if (ex instanceof MethodArgumentNotValidException || 
                      ex instanceof BindException || 
                      ex instanceof ConstraintViolationException ||
                      ex instanceof IllegalArgumentException) {
                // Log validation errors at INFO level
                logger.info("Validation error occurred: {}", ex.getMessage());
            } else if (ex instanceof DataIntegrityViolationException ||
                      ex instanceof EntityNotFoundException ||
                      ex instanceof IllegalStateException) {
                // Log data/state errors at WARN level
                logger.warn("Data/State error occurred: {}", ex.getMessage(), ex);
            } else {
                // Log unexpected exceptions at ERROR level
                logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
            }

        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    /**
     * Generate unique error ID for tracking
     */
    private String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Extract request path from WebRequest
     */
    private String getPath(WebRequest request) {
        String path = request.getDescription(false);
        return path.startsWith("uri=") ? path.substring(4) : path;
    }

    /**
     * Error response structure
     */
    public static class ErrorResponse {
        private String errorId;
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String errorType;
        private Object details;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ErrorResponse response = new ErrorResponse();

            public Builder errorId(String errorId) {
                response.errorId = errorId;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                response.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                response.status = status;
                return this;
            }

            public Builder error(String error) {
                response.error = error;
                return this;
            }

            public Builder message(String message) {
                response.message = message;
                return this;
            }

            public Builder path(String path) {
                response.path = path;
                return this;
            }

            public Builder errorType(String errorType) {
                response.errorType = errorType;
                return this;
            }

            public Builder details(Object details) {
                response.details = details;
                return this;
            }

            public ErrorResponse build() {
                return response;
            }
        }

        // Getters and setters
        public String getErrorId() { return errorId; }
        public void setErrorId(String errorId) { this.errorId = errorId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }

        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
    }
}