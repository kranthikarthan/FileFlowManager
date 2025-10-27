package com.enterprise.fileflow.shared.enums;

/**
 * Enum representing the various states a file can be in during processing
 */
public enum FileStatus {
    RECEIVED("File received and queued for processing"),
    SOT_RECEIVED("Start of Transmission received"),
    DATA_RECEIVED("Data file received"),
    EOT_RECEIVED("End of Transmission received"),
    PROCESSING("File set is being processed"),
    VALIDATED("File content validation completed successfully"),
    VALIDATION_FAILED("File content validation failed"),
    MOVED("File successfully moved to destination"),
    FAILED("File processing failed"),
    SKIPPED("File processing skipped"),
    CLOSED("Service closed for the day"),
    REOPENED("Service reopened for processing");

    private final String description;

    FileStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}