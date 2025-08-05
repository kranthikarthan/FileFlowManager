package com.filetransfer.web.service;

import com.filetransfer.web.dto.FileSchemaDto;
import com.filetransfer.web.dto.SchemaValidationRuleDto;
import com.filetransfer.web.dto.SchemaFieldDto;
import com.filetransfer.web.entity.FileSchema;
import com.filetransfer.web.entity.SchemaValidationRule;
import com.filetransfer.web.entity.SchemaField;
import com.filetransfer.web.entity.SchemaUsageLog;
import com.filetransfer.web.repository.FileSchemaRepository;
import com.filetransfer.web.repository.SchemaValidationRuleRepository;
import com.filetransfer.web.repository.SchemaFieldRepository;
import com.filetransfer.web.repository.SchemaUsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileSchemaService {
    
    @Autowired
    private FileSchemaRepository fileSchemaRepository;
    
    @Autowired
    private SchemaValidationRuleRepository validationRuleRepository;
    
    @Autowired
    private SchemaFieldRepository fieldRepository;
    
    @Autowired
    private SchemaUsageLogRepository usageLogRepository;
    
    // Schema Management
    public FileSchemaDto createSchema(FileSchemaDto schemaDto, String createdBy) {
        FileSchema schema = new FileSchema();
        schema.setTenantId(schemaDto.getTenantId());
        schema.setServiceType(schemaDto.getServiceType());
        schema.setSchemaName(schemaDto.getSchemaName());
        schema.setSchemaVersion(schemaDto.getSchemaVersion());
        schema.setSchemaType(schemaDto.getSchemaType());
        schema.setSchemaDefinition(schemaDto.getSchemaDefinition());
        schema.setDescription(schemaDto.getDescription());
        schema.setCreatedBy(createdBy);
        
        FileSchema savedSchema = fileSchemaRepository.save(schema);
        return convertToDto(savedSchema);
    }
    
    public FileSchemaDto updateSchema(Long schemaId, FileSchemaDto schemaDto, String updatedBy) {
        FileSchema schema = fileSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found"));
        
        schema.setSchemaName(schemaDto.getSchemaName());
        schema.setSchemaVersion(schemaDto.getSchemaVersion());
        schema.setSchemaType(schemaDto.getSchemaType());
        schema.setSchemaDefinition(schemaDto.getSchemaDefinition());
        schema.setDescription(schemaDto.getDescription());
        schema.setIsActive(schemaDto.getIsActive());
        schema.setUpdatedBy(updatedBy);
        
        FileSchema savedSchema = fileSchemaRepository.save(schema);
        return convertToDto(savedSchema);
    }
    
    public void deleteSchema(Long schemaId) {
        fileSchemaRepository.deleteById(schemaId);
    }
    
    public FileSchemaDto getSchemaById(Long schemaId) {
        FileSchema schema = fileSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found"));
        return convertToDto(schema);
    }
    
    public List<FileSchemaDto> getSchemasByServiceType(String tenantId, String serviceType) {
        List<FileSchema> schemas = fileSchemaRepository.findByTenantIdAndServiceTypeAndIsActiveTrue(tenantId, serviceType);
        return schemas.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public List<FileSchemaDto> getAllSchemas(String tenantId) {
        List<FileSchema> schemas = fileSchemaRepository.findByTenantIdAndIsActiveTrue(tenantId);
        return schemas.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    // Validation Rule Management
    public SchemaValidationRuleDto addValidationRule(Long schemaId, SchemaValidationRuleDto ruleDto) {
        FileSchema schema = fileSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found"));
        
        SchemaValidationRule rule = new SchemaValidationRule();
        rule.setSchema(schema);
        rule.setRuleName(ruleDto.getRuleName());
        rule.setRuleType(ruleDto.getRuleType());
        rule.setRuleDefinition(ruleDto.getRuleDefinition());
        rule.setRuleOrder(ruleDto.getRuleOrder());
        rule.setErrorMessage(ruleDto.getErrorMessage());
        
        SchemaValidationRule savedRule = validationRuleRepository.save(rule);
        return convertToDto(savedRule);
    }
    
    public void deleteValidationRule(Long ruleId) {
        validationRuleRepository.deleteById(ruleId);
    }
    
    // Field Management
    public SchemaFieldDto addField(Long schemaId, SchemaFieldDto fieldDto) {
        FileSchema schema = fileSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found"));
        
        SchemaField field = new SchemaField();
        field.setSchema(schema);
        field.setFieldName(fieldDto.getFieldName());
        field.setFieldType(fieldDto.getFieldType());
        field.setFieldLength(fieldDto.getFieldLength());
        field.setIsRequired(fieldDto.getIsRequired());
        field.setIsUnique(fieldDto.getIsUnique());
        field.setDefaultValue(fieldDto.getDefaultValue());
        field.setValidationRegex(fieldDto.getValidationRegex());
        field.setFieldOrder(fieldDto.getFieldOrder());
        field.setDescription(fieldDto.getDescription());
        
        SchemaField savedField = fieldRepository.save(field);
        return convertToDto(savedField);
    }
    
    public void deleteField(Long fieldId) {
        fieldRepository.deleteById(fieldId);
    }
    
    // File Validation
    public ValidationResult validateFile(String tenantId, String serviceType, String fileName, InputStream fileContent, Long fileSize) {
        List<FileSchema> schemas = fileSchemaRepository.findByTenantIdAndServiceTypeAndIsActiveTrue(tenantId, serviceType);
        
        if (schemas.isEmpty()) {
            return new ValidationResult(false, "No active schema found for service type: " + serviceType);
        }
        
        // Use the first active schema (you might want to implement version selection logic)
        FileSchema schema = schemas.get(0);
        List<SchemaValidationRule> rules = validationRuleRepository.findBySchemaAndIsActiveTrueOrderByRuleOrder(schema);
        
        ValidationResult result = new ValidationResult(true, "Validation passed");
        
        try {
            // Read file content for validation
            String content = readFileContent(fileContent);
            
            // Apply validation rules
            for (SchemaValidationRule rule : rules) {
                if (!validateRule(rule, content, fileName, fileSize)) {
                    result.setValid(false);
                    result.setMessage(rule.getErrorMessage());
                    break;
                }
            }
            
            // Log validation result
            logValidationResult(schema, result, fileName, fileSize);
            
        } catch (Exception e) {
            result.setValid(false);
            result.setMessage("Error during validation: " + e.getMessage());
            logValidationResult(schema, result, fileName, fileSize);
        }
        
        return result;
    }
    
    private boolean validateRule(SchemaValidationRule rule, String content, String fileName, Long fileSize) {
        switch (rule.getRuleType()) {
            case "REGEX":
                return validateRegexRule(rule, content);
            case "CUSTOM":
                return validateCustomRule(rule, content, fileName, fileSize);
            case "JSON_SCHEMA":
                return validateJsonSchemaRule(rule, content);
            case "XML_SCHEMA":
                return validateXmlSchemaRule(rule, content);
            case "COBOL_COPYBOOK":
                return validateCobolCopybookRule(rule, content);
            default:
                return true; // Unknown rule type, skip
        }
    }
    
    private boolean validateRegexRule(SchemaValidationRule rule, String content) {
        try {
            Pattern pattern = Pattern.compile(rule.getRuleDefinition());
            return pattern.matcher(content).matches();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateCustomRule(SchemaValidationRule rule, String content, String fileName, Long fileSize) {
        // Simple custom rule evaluation (you might want to use a more sophisticated expression evaluator)
        String definition = rule.getRuleDefinition();
        
        if (definition.contains("fileSize")) {
            definition = definition.replace("fileSize", String.valueOf(fileSize));
        }
        
        if (definition.contains("fileName")) {
            definition = definition.replace("fileName", fileName);
        }
        
        if (definition.contains("contentLength")) {
            definition = definition.replace("contentLength", String.valueOf(content.length()));
        }
        
        // Simple evaluation - in production, use a proper expression evaluator
        try {
            // This is a simplified example - you should use a proper expression evaluator
            return evaluateSimpleExpression(definition);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateJsonSchemaRule(SchemaValidationRule rule, String content) {
        // Implement JSON schema validation
        // You might want to use a library like json-schema-validator
        return true; // Placeholder
    }
    
    private boolean validateXmlSchemaRule(SchemaValidationRule rule, String content) {
        // Implement XML schema validation
        return true; // Placeholder
    }
    
    private boolean validateCobolCopybookRule(SchemaValidationRule rule, String content) {
        try {
            // Parse COBOL copybook definition from rule
            String copybookDefinition = rule.getRuleDefinition();
            
            // Basic COBOL copybook validation
            // This is a simplified implementation - in production, use a proper COBOL parser
            
            // Check if content matches COBOL record structure
            String[] lines = content.split("\n");
            
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    // Validate record length
                    if (line.length() != 80) { // Standard COBOL record length
                        return false;
                    }
                    
                    // Validate field positions and formats
                    if (!validateCobolRecord(line, copybookDefinition)) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateCobolRecord(String record, String copybookDefinition) {
        try {
            // Parse copybook definition (simplified)
            // In production, use a proper COBOL copybook parser
            
            // Basic validation for common COBOL field types
            // X(n) - alphanumeric fields
            // 9(n) - numeric fields
            // 9(n)V99 - decimal fields
            
            // Example validation for a simple copybook structure
            if (record.length() >= 80) {
                // Validate first 10 characters as alphanumeric (CUSTOMER-ID)
                String customerId = record.substring(0, 10);
                if (!customerId.matches("^[A-Za-z0-9\\s]+$")) {
                    return false;
                }
                
                // Validate next 30 characters as alphanumeric (CUSTOMER-NAME)
                String customerName = record.substring(10, 40);
                if (!customerName.matches("^[A-Za-z0-9\\s]+$")) {
                    return false;
                }
                
                // Validate next 12 characters as numeric (ACCOUNT-BALANCE)
                String accountBalance = record.substring(40, 52);
                if (!accountBalance.matches("^\\d{10}\\.\\d{2}$")) {
                    return false;
                }
                
                // Validate next 8 characters as numeric (LAST-UPDATE-DATE)
                String lastUpdateDate = record.substring(52, 60);
                if (!lastUpdateDate.matches("^\\d{8}$")) {
                    return false;
                }
                
                // Validate next 1 character as alphanumeric (STATUS-CODE)
                String statusCode = record.substring(60, 61);
                if (!statusCode.matches("^[A-Za-z0-9]$")) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluateSimpleExpression(String expression) {
        // Simple expression evaluator for basic comparisons
        // In production, use a proper expression evaluator library
        if (expression.contains("<=")) {
            String[] parts = expression.split("<=");
            if (parts.length == 2) {
                try {
                    long left = Long.parseLong(parts[0].trim());
                    long right = Long.parseLong(parts[1].trim());
                    return left <= right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return true; // Default to true for unknown expressions
    }
    
    private String readFileContent(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    private void logValidationResult(FileSchema schema, ValidationResult result, String fileName, Long fileSize) {
        SchemaUsageLog log = new SchemaUsageLog();
        log.setSchema(schema);
        log.setValidationResult(result.isValid() ? "PASSED" : "FAILED");
        log.setValidationDetails(result.getMessage());
        log.setFileName(fileName);
        log.setFileSize(fileSize);
        usageLogRepository.save(log);
    }
    
    // Conversion methods
    private FileSchemaDto convertToDto(FileSchema schema) {
        FileSchemaDto dto = new FileSchemaDto();
        dto.setId(schema.getId());
        dto.setTenantId(schema.getTenantId());
        dto.setServiceType(schema.getServiceType());
        dto.setSchemaName(schema.getSchemaName());
        dto.setSchemaVersion(schema.getSchemaVersion());
        dto.setSchemaType(schema.getSchemaType());
        dto.setSchemaDefinition(schema.getSchemaDefinition());
        dto.setIsActive(schema.getIsActive());
        dto.setCreatedBy(schema.getCreatedBy());
        dto.setCreatedAt(schema.getCreatedAt());
        dto.setUpdatedBy(schema.getUpdatedBy());
        dto.setUpdatedAt(schema.getUpdatedAt());
        dto.setDescription(schema.getDescription());
        return dto;
    }
    
    private SchemaValidationRuleDto convertToDto(SchemaValidationRule rule) {
        SchemaValidationRuleDto dto = new SchemaValidationRuleDto();
        dto.setId(rule.getId());
        dto.setSchemaId(rule.getSchema().getId());
        dto.setRuleName(rule.getRuleName());
        dto.setRuleType(rule.getRuleType());
        dto.setRuleDefinition(rule.getRuleDefinition());
        dto.setRuleOrder(rule.getRuleOrder());
        dto.setIsActive(rule.getIsActive());
        dto.setErrorMessage(rule.getErrorMessage());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }
    
    private SchemaFieldDto convertToDto(SchemaField field) {
        SchemaFieldDto dto = new SchemaFieldDto();
        dto.setId(field.getId());
        dto.setSchemaId(field.getSchema().getId());
        dto.setFieldName(field.getFieldName());
        dto.setFieldType(field.getFieldType());
        dto.setFieldLength(field.getFieldLength());
        dto.setIsRequired(field.getIsRequired());
        dto.setIsUnique(field.getIsUnique());
        dto.setDefaultValue(field.getDefaultValue());
        dto.setValidationRegex(field.getValidationRegex());
        dto.setFieldOrder(field.getFieldOrder());
        dto.setDescription(field.getDescription());
        dto.setCreatedAt(field.getCreatedAt());
        dto.setUpdatedAt(field.getUpdatedAt());
        return dto;
    }
    
    // Validation result class
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}