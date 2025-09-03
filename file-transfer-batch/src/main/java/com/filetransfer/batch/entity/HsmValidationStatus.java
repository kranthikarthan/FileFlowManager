package com.filetransfer.batch.entity;

/**
 * Status of HSM validation operations
 */
public enum HsmValidationStatus {
    PENDING("Pending", "HSM validation is queued for processing"),
    IN_PROGRESS("In Progress", "HSM validation is currently being performed"),
    PASSED("Passed", "HSM validation completed successfully"),
    FAILED("Failed", "HSM validation failed"),
    ERROR("Error", "HSM validation encountered an error"),
    SKIPPED("Skipped", "HSM validation was skipped (not required)"),
    TIMEOUT("Timeout", "HSM validation timed out"),
    HSM_UNAVAILABLE("HSM Unavailable", "HSM service is not available"),
    INVALID_KEY("Invalid Key", "HSM key is invalid or not found"),
    INSUFFICIENT_PERMISSIONS("Insufficient Permissions", "Insufficient permissions for HSM operation");
    
    private final String displayName;
    private final String description;
    
    HsmValidationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this status indicates a successful validation
     */
    public boolean isSuccess() {
        return this == PASSED || this == SKIPPED;
    }
    
    /**
     * Check if this status indicates a failure
     */
    public boolean isFailure() {
        return this == FAILED || this == ERROR || this == TIMEOUT || 
               this == HSM_UNAVAILABLE || this == INVALID_KEY || this == INSUFFICIENT_PERMISSIONS;
    }
    
    /**
     * Check if this status indicates processing is ongoing
     */
    public boolean isProcessing() {
        return this == PENDING || this == IN_PROGRESS;
    }
    
    /**
     * Check if this status is terminal (no further processing needed)
     */
    public boolean isTerminal() {
        return isSuccess() || isFailure();
    }
    
    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (isSuccess()) {
            return "success";
        } else if (isFailure()) {
            return "error";
        } else if (isProcessing()) {
            return "warning";
        } else {
            return "default";
        }
    }
}