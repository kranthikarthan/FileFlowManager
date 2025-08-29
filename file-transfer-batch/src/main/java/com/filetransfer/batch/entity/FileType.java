package com.filetransfer.batch.entity;

/**
 * Enum representing different file types supported by the system
 */
public enum FileType {
    COBOL_FLAT_FILE("COBOL Flat File", "Mainframe COBOL fixed-width record files", true, "COBOL_COPYBOOK"),
    BINARY_FILE("Binary File", "Binary files (images, executables, etc.)", false, null),
    XML("XML File", "Extensible Markup Language files", true, "XML_SCHEMA"),
    JSON("JSON File", "JavaScript Object Notation files", true, "JSON_SCHEMA"),
    CSV("CSV File", "Comma Separated Values files", true, "CSV_SCHEMA"),
    FIXED_WIDTH("Fixed Width File", "Fixed-width text files", true, "FIXED_WIDTH_SCHEMA"),
    DELIMITED("Delimited File", "Delimited text files (TSV, pipe-delimited, etc.)", true, "DELIMITED_SCHEMA"),
    EDI("EDI File", "Electronic Data Interchange files", true, "EDI_SCHEMA");
    
    private final String displayName;
    private final String description;
    private final boolean requiresSchemaValidation;
    private final String defaultSchemaType;
    
    FileType(String displayName, String description, boolean requiresSchemaValidation, String defaultSchemaType) {
        this.displayName = displayName;
        this.description = description;
        this.requiresSchemaValidation = requiresSchemaValidation;
        this.defaultSchemaType = defaultSchemaType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresSchemaValidation() {
        return requiresSchemaValidation;
    }
    
    public String getDefaultSchemaType() {
        return defaultSchemaType;
    }
    
    /**
     * Check if this file type is binary
     */
    public boolean isBinary() {
        return this == BINARY_FILE;
    }
    
    /**
     * Check if this file type supports automatic detection
     */
    public boolean supportsAutoDetection() {
        return this == XML || this == JSON || this == CSV;
    }
    
    /**
     * Get file type from file extension
     */
    public static FileType fromFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return COBOL_FLAT_FILE; // Default for mainframe files without extensions
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        switch (extension) {
            case "xml":
                return XML;
            case "json":
                return JSON;
            case "csv":
                return CSV;
            case "txt":
            case "dat":
                return FIXED_WIDTH; // Common for mainframe data files
            case "tsv":
            case "pipe":
            case "psv":
                return DELIMITED;
            case "edi":
                return EDI;
            case "exe":
            case "dll":
            case "bin":
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "pdf":
            case "zip":
            case "rar":
            case "tar":
            case "gz":
                return BINARY_FILE;
            default:
                return COBOL_FLAT_FILE; // Default for unknown extensions
        }
    }
    
    /**
     * Detect file type from content
     */
    public static FileType detectFromContent(String content, String fileName) {
        if (content == null || content.trim().isEmpty()) {
            return fromFileExtension(fileName);
        }
        
        // Check for binary content (non-printable characters)
        if (containsBinaryContent(content)) {
            return BINARY_FILE;
        }
        
        String trimmedContent = content.trim();
        
        // JSON detection
        if ((trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) ||
            (trimmedContent.startsWith("[") && trimmedContent.endsWith("]"))) {
            return JSON;
        }
        
        // XML detection
        if (trimmedContent.startsWith("<?xml") || 
            (trimmedContent.startsWith("<") && trimmedContent.contains(">"))) {
            return XML;
        }
        
        // CSV detection (look for comma-separated values in first line)
        String firstLine = trimmedContent.split("\n")[0];
        if (firstLine.contains(",") && firstLine.split(",").length > 1) {
            return CSV;
        }
        
        // EDI detection (look for EDI segment terminators)
        if (trimmedContent.contains("~") || trimmedContent.matches(".*[A-Z]{2,3}\\*.*")) {
            return EDI;
        }
        
        // Fall back to file extension detection
        return fromFileExtension(fileName);
    }
    
    private static boolean containsBinaryContent(String content) {
        // Check for null bytes or other binary indicators
        return content.contains("\0") || 
               content.chars().anyMatch(c -> c < 32 && c != 9 && c != 10 && c != 13);
    }
}