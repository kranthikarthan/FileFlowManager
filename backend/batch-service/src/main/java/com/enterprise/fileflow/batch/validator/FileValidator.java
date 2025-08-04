package com.enterprise.fileflow.batch.validator;

import java.io.File;

/**
 * Strategy interface for file content validation
 */
public interface FileValidator {
    
    /**
     * Validates file content against configured schema/rules
     * 
     * @param file The file to validate
     * @param schemaContent The schema content for validation
     * @return ValidationResult containing validation status and errors
     */
    ValidationResult validate(File file, String schemaContent);
    
    /**
     * Checks if this validator supports the given file type
     * 
     * @param fileName The name of the file
     * @return true if this validator can handle the file type
     */
    boolean supports(String fileName);
}