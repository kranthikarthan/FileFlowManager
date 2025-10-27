package com.enterprise.fileflow.batch.validator;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates file validation results
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String summary;
    
    public static ValidationResult success() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setSummary("Validation passed successfully");
        return result;
    }
    
    public static ValidationResult failure(String error) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.getErrors().add(error);
        result.setSummary("Validation failed");
        return result;
    }
    
    public static ValidationResult failure(List<String> errors) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setErrors(errors);
        result.setSummary("Validation failed with " + errors.size() + " errors");
        return result;
    }
    
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}