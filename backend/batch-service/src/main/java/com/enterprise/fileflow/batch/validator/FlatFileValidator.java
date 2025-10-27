package com.enterprise.fileflow.batch.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Flat file validator for fixed format files using YAML configuration
 */
@Component
@Slf4j
public class FlatFileValidator implements FileValidator {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @Override
    public ValidationResult validate(File file, String schemaContent) {
        try {
            // Parse YAML schema configuration
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = yamlMapper.readValue(schemaContent, Map.class);
            
            // Read file content
            List<String> lines = Files.readAllLines(file.toPath());
            
            ValidationResult result = new ValidationResult();
            result.setValid(true);
            
            // Validate structure (header, data, trailer)
            validateStructure(lines, schema, result);
            
            // Validate field formats if structure is valid
            if (result.isValid()) {
                validateFieldFormats(lines, schema, result);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error validating flat file: {}", file.getName(), e);
            return ValidationResult.failure("Flat file validation error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && (fileName.toLowerCase().endsWith(".txt") || 
                                  fileName.toLowerCase().endsWith(".dat") ||
                                  fileName.toLowerCase().endsWith(".csv"));
    }
    
    @SuppressWarnings("unchecked")
    private void validateStructure(List<String> lines, Map<String, Object> schema, ValidationResult result) {
        // Check minimum number of lines (header + at least one data + trailer)
        Integer minLines = (Integer) schema.getOrDefault("minLines", 3);
        if (lines.size() < minLines) {
            result.addError("File must have at least " + minLines + " lines (header, data, trailer)");
            return;
        }
        
        // Validate header
        Map<String, Object> headerConfig = (Map<String, Object>) schema.get("header");
        if (headerConfig != null) {
            validateLine(lines.get(0), headerConfig, "Header", result);
        }
        
        // Validate trailer
        Map<String, Object> trailerConfig = (Map<String, Object>) schema.get("trailer");
        if (trailerConfig != null) {
            validateLine(lines.get(lines.size() - 1), trailerConfig, "Trailer", result);
        }
        
        // Validate data lines
        Map<String, Object> dataConfig = (Map<String, Object>) schema.get("data");
        if (dataConfig != null) {
            for (int i = 1; i < lines.size() - 1; i++) {
                validateLine(lines.get(i), dataConfig, "Data line " + i, result);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void validateLine(String line, Map<String, Object> lineConfig, String lineType, ValidationResult result) {
        // Check line length
        Integer expectedLength = (Integer) lineConfig.get("length");
        if (expectedLength != null && line.length() != expectedLength) {
            result.addError(lineType + " length should be " + expectedLength + " but was " + line.length());
        }
        
        // Check line pattern
        String pattern = (String) lineConfig.get("pattern");
        if (pattern != null && !line.matches(pattern)) {
            result.addError(lineType + " does not match expected pattern: " + pattern);
        }
        
        // Validate fields
        List<Map<String, Object>> fields = (List<Map<String, Object>>) lineConfig.get("fields");
        if (fields != null) {
            validateFields(line, fields, lineType, result);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void validateFields(String line, List<Map<String, Object>> fields, String lineType, ValidationResult result) {
        for (Map<String, Object> field : fields) {
            String fieldName = (String) field.get("name");
            Integer start = (Integer) field.get("start");
            Integer length = (Integer) field.get("length");
            String dataType = (String) field.get("type");
            Boolean required = (Boolean) field.getOrDefault("required", false);
            
            if (start != null && length != null) {
                if (start + length > line.length()) {
                    result.addError(lineType + " field '" + fieldName + "' exceeds line length");
                    continue;
                }
                
                String fieldValue = line.substring(start, start + length).trim();
                
                if (required && fieldValue.isEmpty()) {
                    result.addError(lineType + " field '" + fieldName + "' is required but empty");
                }
                
                if (!fieldValue.isEmpty() && dataType != null) {
                    validateFieldType(fieldValue, dataType, fieldName, lineType, result);
                }
            }
        }
    }
    
    private void validateFieldType(String value, String dataType, String fieldName, String lineType, ValidationResult result) {
        try {
            switch (dataType.toLowerCase()) {
                case "integer":
                case "int":
                    Integer.parseInt(value);
                    break;
                case "decimal":
                case "double":
                    Double.parseDouble(value);
                    break;
                case "date":
                    // Simple date validation - can be enhanced with specific format
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}") && !value.matches("\\d{8}")) {
                        result.addError(lineType + " field '" + fieldName + "' has invalid date format");
                    }
                    break;
                case "string":
                case "text":
                    // String validation passed by default
                    break;
                default:
                    result.addWarning(lineType + " field '" + fieldName + "' has unknown data type: " + dataType);
            }
        } catch (NumberFormatException e) {
            result.addError(lineType + " field '" + fieldName + "' has invalid " + dataType + " value: " + value);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void validateFieldFormats(List<String> lines, Map<String, Object> schema, ValidationResult result) {
        // Additional format validations can be added here
        // This could include business rules, cross-field validations, etc.
        
        Map<String, Object> businessRules = (Map<String, Object>) schema.get("businessRules");
        if (businessRules != null) {
            // Implement business rule validations
            result.addWarning("Business rule validation not yet implemented");
        }
    }
}