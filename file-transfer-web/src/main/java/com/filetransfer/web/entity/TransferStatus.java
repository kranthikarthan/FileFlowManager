package com.filetransfer.web.entity;

public enum TransferStatus {
    PENDING,
    PROCESSING,      // Standardized from IN_PROGRESS
    IN_PROGRESS,     // Keep for backward compatibility
    PAUSED,          // NEW: For pause functionality
    COMPLETED,
    FAILED,
    CANCELLED,
    ARCHIVED,        // NEW: For archival status
    WAITING_FOR_END_MARKER
}