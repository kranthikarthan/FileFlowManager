package com.enterprise.fileflow.batch.validator;

import com.enterprise.fileflow.shared.enums.ValidationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory to create appropriate file validators based on validation strategy
 */
@Component
@RequiredArgsConstructor
public class ValidatorFactory {
    
    private final List<FileValidator> validators;
    
    /**
     * Gets the appropriate validator for the given strategy and file name
     * 
     * @param strategy The validation strategy
     * @param fileName The name of the file to validate
     * @return The appropriate validator, or null if no suitable validator found
     */
    public FileValidator getValidator(ValidationStrategy strategy, String fileName) {
        switch (strategy) {
            case XML_XSD:
                return getValidatorByType(XmlValidator.class, fileName);
            case JSON_SCHEMA:
                return getValidatorByType(JsonValidator.class, fileName);
            case FLAT_FILE_YAML:
            case FLAT_FILE_COPYBOOK:
                return getValidatorByType(FlatFileValidator.class, fileName);
            case NONE:
            default:
                return null;
        }
    }
    
    /**
     * Gets a validator by its class type and checks if it supports the file
     */
    private FileValidator getValidatorByType(Class<? extends FileValidator> validatorClass, String fileName) {
        return validators.stream()
            .filter(validator -> validatorClass.isInstance(validator))
            .filter(validator -> validator.supports(fileName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets all available validators that support the given file name
     */
    public List<FileValidator> getSupportedValidators(String fileName) {
        return validators.stream()
            .filter(validator -> validator.supports(fileName))
            .toList();
    }
}