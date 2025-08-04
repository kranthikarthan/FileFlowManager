package com.enterprise.fileflow.batch.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * XML file validator using XSD schema validation
 */
@Component
@Slf4j
public class XmlValidator implements FileValidator {
    
    @Override
    public ValidationResult validate(File file, String schemaContent) {
        try {
            // Create schema factory
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            // Create schema from content
            Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(schemaContent)));
            
            // Create validator
            Validator validator = schema.newValidator();
            
            // Custom error handler to collect errors
            ValidationErrorHandler errorHandler = new ValidationErrorHandler();
            validator.setErrorHandler(errorHandler);
            
            // Validate the file
            validator.validate(new StreamSource(file));
            
            // Return results
            if (errorHandler.hasErrors()) {
                return ValidationResult.failure(errorHandler.getErrors());
            } else {
                ValidationResult result = ValidationResult.success();
                result.setWarnings(errorHandler.getWarnings());
                return result;
            }
            
        } catch (Exception e) {
            log.error("Error validating XML file: {}", file.getName(), e);
            return ValidationResult.failure("XML validation error: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".xml");
    }
    
    /**
     * Custom error handler to collect validation errors and warnings
     */
    private static class ValidationErrorHandler implements ErrorHandler {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            warnings.add("Warning at line " + exception.getLineNumber() + 
                        ", column " + exception.getColumnNumber() + ": " + exception.getMessage());
        }
        
        @Override
        public void error(SAXParseException exception) throws SAXException {
            errors.add("Error at line " + exception.getLineNumber() + 
                      ", column " + exception.getColumnNumber() + ": " + exception.getMessage());
        }
        
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add("Fatal error at line " + exception.getLineNumber() + 
                      ", column " + exception.getColumnNumber() + ": " + exception.getMessage());
            throw exception;
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}