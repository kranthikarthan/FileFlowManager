package com.enterprise.fileflow.batch.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON file validator using JSON schema validation
 */
@Component
@Slf4j
public class JsonValidator implements FileValidator {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ValidationResult validate(File file, String schemaContent) {
        try {
            // Parse schema
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = objectMapper.readTree(schemaContent);
            JsonSchema jsonSchema = factory.getSchema(schemaNode);
            
            // Parse JSON file
            JsonNode jsonNode = objectMapper.readTree(file);
            
            // Validate
            Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);
            
            if (validationMessages.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure(
                    validationMessages.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.toList())
                );
            }
            
        } catch (Exception e) {
            log.error("Error validating JSON file: {}", file.getName(), e);
            return ValidationResult.failure("JSON validation error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".json");
    }
}