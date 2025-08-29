package com.filetransfer.web.service;

import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.FileSchemaRepository;
import com.filetransfer.web.repository.SubServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class FileTypeValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTypeValidationService.class);
    
    @Autowired
    private SubServiceConfigurationRepository subServiceConfigRepository;
    
    @Autowired
    private FileSchemaRepository fileSchemaRepository;
    
    @Autowired
    private FileSchemaService fileSchemaService;
    
    /**
     * Detect file type from content and filename
     */
    public FileType detectFileType(String fileName, byte[] content) {
        try {
            // First check for binary content
            if (isBinaryContent(content)) {
                return FileType.BINARY_FILE;
            }
            
            // Convert to string for text-based detection
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Use the FileType enum's detection logic
            return FileType.detectFromContent(textContent, fileName);
            
        } catch (Exception e) {
            logger.warn("Error detecting file type for {}, falling back to extension-based detection: {}", 
                       fileName, e.getMessage());
            return FileType.fromFileExtension(fileName);
        }
    }
    
    /**
     * Validate file against configured schema based on file type and direction
     */
    public ValidationResult validateFile(String tenantId, String serviceName, String subServiceName,
                                       String fileName, byte[] content, FileType fileType, 
                                       TransferDirection direction) {
        
        ValidationResult result = new ValidationResult();
        result.setFileName(fileName);
        result.setFileType(fileType);
        result.setDirection(direction);
        
        try {
            // Get sub-service configuration
            Optional<SubServiceConfiguration> configOpt = subServiceConfigRepository
                .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName);
            
            if (configOpt.isEmpty()) {
                result.setValid(false);
                result.addError("Sub-service configuration not found");
                return result;
            }
            
            SubServiceConfiguration config = configOpt.get();
            
            // Check if validation is required for this file type
            if (!config.requiresSchemaValidation(fileType)) {
                result.setValid(true);
                result.setSkipped(true);
                result.setSkipReason("Schema validation not required for " + fileType);
                return result;
            }
            
            // Get schema ID for this file type
            Long schemaId = config.getSchemaIdForFileType(fileType, direction);
            if (schemaId == null) {
                // Check if schema is required
                if (config.getSchemaValidationEnabled()) {
                    result.setValid(false);
                    result.addError("No schema configured for file type " + fileType + " and direction " + direction);
                } else {
                    result.setValid(true);
                    result.setSkipped(true);
                    result.setSkipReason("Schema validation disabled");
                }
                return result;
            }
            
            // Get schema
            Optional<FileSchema> schemaOpt = fileSchemaRepository.findById(schemaId);
            if (schemaOpt.isEmpty()) {
                result.setValid(false);
                result.addError("Schema not found with ID: " + schemaId);
                return result;
            }
            
            FileSchema schema = schemaOpt.get();
            result.setSchemaId(schemaId);
            result.setSchemaName(schema.getSchemaName());
            
            // Perform file type specific validation
            switch (fileType) {
                case BINARY_FILE:
                    result = validateBinaryFile(fileName, content, config, result);
                    break;
                case COBOL_FLAT_FILE:
                    result = validateCobolFlatFile(fileName, content, schema, result);
                    break;
                case XML:
                    result = validateXmlFile(fileName, content, schema, result);
                    break;
                case JSON:
                    result = validateJsonFile(fileName, content, schema, result);
                    break;
                case CSV:
                    result = validateCsvFile(fileName, content, schema, result);
                    break;
                case FIXED_WIDTH:
                    result = validateFixedWidthFile(fileName, content, schema, result);
                    break;
                case DELIMITED:
                    result = validateDelimitedFile(fileName, content, schema, result);
                    break;
                case EDI:
                    result = validateEdiFile(fileName, content, schema, result);
                    break;
                default:
                    result.setValid(false);
                    result.addError("Unsupported file type: " + fileType);
            }
            
        } catch (Exception e) {
            logger.error("Error validating file {}: {}", fileName, e.getMessage(), e);
            result.setValid(false);
            result.addError("Validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Check if content is binary
     */
    private boolean isBinaryContent(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        
        // Check first 1024 bytes for binary indicators
        int checkLength = Math.min(content.length, 1024);
        int nullBytes = 0;
        int nonPrintableBytes = 0;
        
        for (int i = 0; i < checkLength; i++) {
            byte b = content[i];
            
            // Check for null bytes (strong indicator of binary)
            if (b == 0) {
                nullBytes++;
                if (nullBytes > 1) {
                    return true;
                }
            }
            
            // Check for non-printable characters (excluding common whitespace)
            if (b < 32 && b != 9 && b != 10 && b != 13) {
                nonPrintableBytes++;
            }
        }
        
        // If more than 5% non-printable characters, consider it binary
        double nonPrintableRatio = (double) nonPrintableBytes / checkLength;
        return nonPrintableRatio > 0.05;
    }
    
    private ValidationResult validateBinaryFile(String fileName, byte[] content, 
                                              SubServiceConfiguration config, ValidationResult result) {
        if (config.getBinaryFileBypass()) {
            result.setValid(true);
            result.setSkipped(true);
            result.setSkipReason("Binary file bypass enabled");
        } else {
            result.setValid(false);
            result.addError("Binary files not allowed - bypass is disabled");
        }
        return result;
    }
    
    private ValidationResult validateCobolFlatFile(String fileName, byte[] content, 
                                                 FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Use existing FileSchemaService validation
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("COBOL copybook validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("COBOL validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateXmlFile(String fileName, byte[] content, 
                                           FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Basic XML structure validation
            if (!textContent.trim().startsWith("<?xml") && !textContent.trim().startsWith("<")) {
                result.setValid(false);
                result.addError("Invalid XML format - missing XML declaration or root element");
                return result;
            }
            
            // Use schema validation if available
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("XML schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("XML validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateJsonFile(String fileName, byte[] content, 
                                            FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8).trim();
            
            // Basic JSON structure validation
            if (!(textContent.startsWith("{") && textContent.endsWith("}")) &&
                !(textContent.startsWith("[") && textContent.endsWith("]"))) {
                result.setValid(false);
                result.addError("Invalid JSON format - must start with { or [ and end with } or ]");
                return result;
            }
            
            // Use schema validation if available
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("JSON schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("JSON validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateCsvFile(String fileName, byte[] content, 
                                           FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Basic CSV validation - check for consistent column count
            String[] lines = textContent.split("\n");
            if (lines.length == 0) {
                result.setValid(false);
                result.addError("Empty CSV file");
                return result;
            }
            
            int expectedColumns = lines[0].split(",").length;
            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    int actualColumns = lines[i].split(",").length;
                    if (actualColumns != expectedColumns) {
                        result.setValid(false);
                        result.addError(String.format("Inconsistent column count at line %d: expected %d, got %d", 
                                                     i + 1, expectedColumns, actualColumns));
                        return result;
                    }
                }
            }
            
            // Use schema validation if available
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("CSV schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("CSV validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateFixedWidthFile(String fileName, byte[] content, 
                                                  FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Use schema validation
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("Fixed width schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Fixed width validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateDelimitedFile(String fileName, byte[] content, 
                                                 FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Use schema validation
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("Delimited file schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Delimited file validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    private ValidationResult validateEdiFile(String fileName, byte[] content, 
                                           FileSchema schema, ValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            
            // Basic EDI structure validation
            if (!textContent.contains("~") && !textContent.matches(".*[A-Z]{2,3}\\*.*")) {
                result.setValid(false);
                result.addError("Invalid EDI format - missing segment terminators or standard structure");
                return result;
            }
            
            // Use schema validation
            boolean isValid = fileSchemaService.validateFileAgainstSchema(
                schema.getId(), fileName, textContent, content.length);
            
            result.setValid(isValid);
            if (!isValid) {
                result.addError("EDI schema validation failed");
            }
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("EDI validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private String fileName;
        private FileType fileType;
        private TransferDirection direction;
        private boolean valid;
        private boolean skipped;
        private String skipReason;
        private Long schemaId;
        private String schemaName;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        private java.util.List<String> warnings = new java.util.ArrayList<>();
        
        // Getters and setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
        
        public TransferDirection getDirection() { return direction; }
        public void setDirection(TransferDirection direction) { this.direction = direction; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public boolean isSkipped() { return skipped; }
        public void setSkipped(boolean skipped) { this.skipped = skipped; }
        
        public String getSkipReason() { return skipReason; }
        public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
        
        public Long getSchemaId() { return schemaId; }
        public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
        
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        
        public java.util.List<String> getErrors() { return errors; }
        public void setErrors(java.util.List<String> errors) { this.errors = errors; }
        
        public java.util.List<String> getWarnings() { return warnings; }
        public void setWarnings(java.util.List<String> warnings) { this.warnings = warnings; }
        
        public void addError(String error) { this.errors.add(error); }
        public void addWarning(String warning) { this.warnings.add(warning); }
    }
}