package com.enterprise.fileflow.shared.enums;

/**
 * Enum representing the operational status of a service
 */
public enum ServiceStatus {
    ACTIVE("Service is active and processing files"),
    CLOSED("Service is closed for the day"),
    SUSPENDED("Service is temporarily suspended"),
    MAINTENANCE("Service is under maintenance");

    private final String description;

    ServiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}