package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Service to handle file validation using regex patterns
 */
@Service
public class FileValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);
    
    /**
     * Validate a file name against the service configuration patterns
     */
    public boolean validateFileName(String fileName, ServiceConfiguration config, FileType fileType) {
        if (fileName == null || config == null || fileType == null) {
            return false;
        }
        
        String regex = getRegexForFileType(config, fileType);
        
        if (regex == null || regex.trim().isEmpty()) {
            logger.debug("No validation regex configured for file type: {} in service: {}", fileType, config.getServiceName());
            return true; // No validation configured, allow the file
        }
        
        try {
            Pattern pattern = Pattern.compile(regex);
            boolean isValid = pattern.matcher(fileName).matches();
            
            if (!isValid) {
                logger.warn("File '{}' failed validation for service '{}' with pattern: {}", fileName, config.getServiceName(), regex);
            } else {
                logger.debug("File '{}' passed validation for service '{}'", fileName, config.getServiceName());
            }
            
            return isValid;
        } catch (PatternSyntaxException e) {
            logger.error("Invalid regex pattern for service '{}' and file type '{}': {}", 
                        config.getServiceName(), fileType, regex, e);
            return false; // Invalid regex means file fails validation
        }
    }
    
    /**
     * Get the appropriate regex pattern for the file type
     */
    private String getRegexForFileType(ServiceConfiguration config, FileType fileType) {
        switch (fileType) {
            case SOT:
                return config.getSotFileValidationRegex();
            case EOT:
                return config.getEotFileValidationRegex();
            case DATA:
                return config.getDataFileValidationRegex();
            default:
                return null;
        }
    }
    
    /**
     * Determine file type based on filename and service configuration
     */
    public FileType determineFileType(String fileName, ServiceConfiguration config) {
        if (fileName == null || config == null) {
            return FileType.UNKNOWN;
        }
        
        if (fileName.startsWith(config.getStartMarkerPrefix())) {
            return FileType.SOT;
        } else if (fileName.startsWith(config.getEndMarkerPrefix())) {
            return FileType.EOT;
        } else {
            return FileType.DATA;
        }
    }
    
    /**
     * Validate file name and return validation result with details
     */
    public ValidationResult validateFileWithDetails(String fileName, ServiceConfiguration config) {
        FileType fileType = determineFileType(fileName, config);
        boolean isValid = validateFileName(fileName, config, fileType);
        
        return new ValidationResult(isValid, fileType, getRegexForFileType(config, fileType));
    }
    
    /**
     * File type enumeration
     */
    public enum FileType {
        SOT,    // Start of Transmission
        EOT,    // End of Transmission
        DATA,   // Data file
        UNKNOWN
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final FileType fileType;
        private final String appliedRegex;
        
        public ValidationResult(boolean valid, FileType fileType, String appliedRegex) {
            this.valid = valid;
            this.fileType = fileType;
            this.appliedRegex = appliedRegex;
        }
        
        public boolean isValid() { return valid; }
        public FileType getFileType() { return fileType; }
        public String getAppliedRegex() { return appliedRegex; }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, fileType=%s, regex='%s'}", 
                               valid, fileType, appliedRegex);
        }
    }
}