package com.enterprise.fileflow.shared.enums;

/**
 * Enum representing different file validation strategies
 */
public enum ValidationStrategy {
    XML_XSD("XML validation using XSD schema"),
    JSON_SCHEMA("JSON validation using JSON/YAML schema"),
    FLAT_FILE_COPYBOOK("Flat file validation using Copybook rules"),
    FLAT_FILE_YAML("Flat file validation using YAML rules"),
    NONE("No validation required");

    private final String description;

    ValidationStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}