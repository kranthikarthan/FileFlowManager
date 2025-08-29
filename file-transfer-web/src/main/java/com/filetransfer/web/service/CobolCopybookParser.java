package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * COBOL copybook parser for schema validation.
 * This service parses COBOL copybook definitions and validates flat file data against them.
 */
@Service
public class CobolCopybookParser {
    
    private static final Logger logger = LoggerFactory.getLogger(CobolCopybookParser.class);
    
    // COBOL field definition patterns
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "\\s*(\\d{2})\\s+(\\w+)\\s+PIC\\s+(\\S+)(?:\\s+OCCURS\\s+(\\d+))?(?:\\s+TIMES)?\\s*\\.?\\s*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PIC_PATTERN = Pattern.compile(
        "([9AXS]+)(\\((\\d+)\\))?(?:V(9+)(?:\\((\\d+)\\))?)?",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parse COBOL copybook schema definition
     */
    public CobolSchema parseCopybook(String copybookContent) {
        CobolSchema schema = new CobolSchema();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(copybookContent))) {
            String line;
            int currentPosition = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("*") || line.startsWith("//")) {
                    continue;
                }
                
                // Parse field definition
                CobolField field = parseFieldDefinition(line, currentPosition);
                if (field != null) {
                    schema.addField(field);
                    currentPosition += field.getTotalLength();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error parsing COBOL copybook: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse COBOL copybook: " + e.getMessage());
        }
        
        return schema;
    }
    
    /**
     * Parse individual field definition
     */
    private CobolField parseFieldDefinition(String line, int startPosition) {
        Matcher matcher = FIELD_PATTERN.matcher(line);
        
        if (!matcher.find()) {
            return null; // Not a field definition
        }
        
        String level = matcher.group(1);
        String fieldName = matcher.group(2);
        String picClause = matcher.group(3);
        String occursCount = matcher.group(4);
        
        // Parse PIC clause
        PictureClause picture = parsePictureClause(picClause);
        if (picture == null) {
            logger.warn("Could not parse PIC clause: {} for field: {}", picClause, fieldName);
            return null;
        }
        
        // Handle OCCURS clause
        int occurs = 1;
        if (occursCount != null) {
            occurs = Integer.parseInt(occursCount);
        }
        
        CobolField field = new CobolField();
        field.setLevel(Integer.parseInt(level));
        field.setName(fieldName);
        field.setPicture(picture);
        field.setOccurs(occurs);
        field.setStartPosition(startPosition);
        field.setLength(picture.getLength());
        field.setTotalLength(picture.getLength() * occurs);
        
        return field;
    }
    
    /**
     * Parse COBOL PIC clause
     */
    private PictureClause parsePictureClause(String picClause) {
        Matcher matcher = PIC_PATTERN.matcher(picClause);
        
        if (!matcher.find()) {
            return null;
        }
        
        String mainPart = matcher.group(1);
        String mainRepeat = matcher.group(3);
        String decimalPart = matcher.group(4);
        String decimalRepeat = matcher.group(5);
        
        PictureClause picture = new PictureClause();
        
        // Determine data type
        if (mainPart.contains("9")) {
            picture.setDataType(DataType.NUMERIC);
        } else if (mainPart.contains("X")) {
            picture.setDataType(DataType.ALPHANUMERIC);
        } else if (mainPart.contains("A")) {
            picture.setDataType(DataType.ALPHABETIC);
        } else if (mainPart.contains("S")) {
            picture.setDataType(DataType.SIGNED_NUMERIC);
        } else {
            picture.setDataType(DataType.ALPHANUMERIC);
        }
        
        // Calculate integer length
        int integerLength = calculateLength(mainPart, mainRepeat);
        picture.setIntegerLength(integerLength);
        
        // Calculate decimal length
        int decimalLength = 0;
        if (decimalPart != null) {
            decimalLength = calculateLength(decimalPart, decimalRepeat);
        }
        picture.setDecimalLength(decimalLength);
        
        // Total length
        picture.setLength(integerLength + decimalLength);
        
        return picture;
    }
    
    /**
     * Calculate field length from PIC clause part
     */
    private int calculateLength(String part, String repeat) {
        if (repeat != null) {
            return Integer.parseInt(repeat);
        } else {
            return part.length();
        }
    }
    
    /**
     * Validate flat file data against COBOL schema
     */
    public ValidationResult validateData(CobolSchema schema, String data) {
        ValidationResult result = new ValidationResult();
        
        try {
            // Split data into lines
            String[] lines = data.split("\\r?\\n");
            
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                ValidationResult lineResult = validateLine(schema, line, lineNum + 1);
                result.merge(lineResult);
            }
            
        } catch (Exception e) {
            result.addError("General validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate single line against schema
     */
    private ValidationResult validateLine(CobolSchema schema, String line, int lineNumber) {
        ValidationResult result = new ValidationResult();
        
        int expectedLength = schema.getTotalLength();
        if (line.length() != expectedLength) {
            result.addError(String.format("Line %d: Expected length %d, but got %d", 
                lineNumber, expectedLength, line.length()));
            return result;
        }
        
        // Validate each field
        for (CobolField field : schema.getFields()) {
            try {
                validateField(field, line, lineNumber, result);
            } catch (Exception e) {
                result.addError(String.format("Line %d, Field %s: %s", 
                    lineNumber, field.getName(), e.getMessage()));
            }
        }
        
        result.incrementValidLines();
        return result;
    }
    
    /**
     * Validate individual field
     */
    private void validateField(CobolField field, String line, int lineNumber, ValidationResult result) {
        for (int occur = 0; occur < field.getOccurs(); occur++) {
            int startPos = field.getStartPosition() + (occur * field.getLength());
            int endPos = startPos + field.getLength();
            
            if (endPos > line.length()) {
                result.addError(String.format("Line %d, Field %s[%d]: Field extends beyond line length", 
                    lineNumber, field.getName(), occur));
                continue;
            }
            
            String fieldValue = line.substring(startPos, endPos);
            
            // Validate based on data type
            switch (field.getPicture().getDataType()) {
                case NUMERIC:
                case SIGNED_NUMERIC:
                    validateNumericField(field, fieldValue, lineNumber, occur, result);
                    break;
                case ALPHABETIC:
                    validateAlphabeticField(field, fieldValue, lineNumber, occur, result);
                    break;
                case ALPHANUMERIC:
                    // Alphanumeric accepts any characters
                    break;
            }
        }
    }
    
    /**
     * Validate numeric field
     */
    private void validateNumericField(CobolField field, String value, int lineNumber, int occur, ValidationResult result) {
        // Remove leading/trailing spaces
        String trimmed = value.trim();
        
        if (trimmed.isEmpty()) {
            // Empty numeric field might be valid (spaces)
            return;
        }
        
        // Check if all characters are numeric (and sign for signed fields)
        Pattern numericPattern = field.getPicture().getDataType() == DataType.SIGNED_NUMERIC ?
            Pattern.compile("^[+\\-]?\\d*$") :
            Pattern.compile("^\\d*$");
        
        if (!numericPattern.matcher(trimmed).matches()) {
            result.addError(String.format("Line %d, Field %s[%d]: Invalid numeric value '%s'", 
                lineNumber, field.getName(), occur, value));
        }
        
        // Validate decimal places if applicable
        if (field.getPicture().getDecimalLength() > 0) {
            // For packed decimal or assumed decimal point validation
            // This is a simplified validation - real COBOL might handle this differently
        }
    }
    
    /**
     * Validate alphabetic field
     */
    private void validateAlphabeticField(CobolField field, String value, int lineNumber, int occur, ValidationResult result) {
        if (!value.matches("^[A-Za-z\\s]*$")) {
            result.addError(String.format("Line %d, Field %s[%d]: Invalid alphabetic value '%s'", 
                lineNumber, field.getName(), occur, value));
        }
    }
    
    // Inner classes for data structures
    
    public static class CobolSchema {
        private List<CobolField> fields = new ArrayList<>();
        private int totalLength = 0;
        
        public void addField(CobolField field) {
            fields.add(field);
            totalLength += field.getTotalLength();
        }
        
        public List<CobolField> getFields() { return fields; }
        public int getTotalLength() { return totalLength; }
    }
    
    public static class CobolField {
        private int level;
        private String name;
        private PictureClause picture;
        private int occurs = 1;
        private int startPosition;
        private int length;
        private int totalLength;
        
        // Getters and setters
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public PictureClause getPicture() { return picture; }
        public void setPicture(PictureClause picture) { this.picture = picture; }
        
        public int getOccurs() { return occurs; }
        public void setOccurs(int occurs) { this.occurs = occurs; }
        
        public int getStartPosition() { return startPosition; }
        public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
        
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        
        public int getTotalLength() { return totalLength; }
        public void setTotalLength(int totalLength) { this.totalLength = totalLength; }
    }
    
    public static class PictureClause {
        private DataType dataType;
        private int integerLength;
        private int decimalLength;
        private int length;
        
        // Getters and setters
        public DataType getDataType() { return dataType; }
        public void setDataType(DataType dataType) { this.dataType = dataType; }
        
        public int getIntegerLength() { return integerLength; }
        public void setIntegerLength(int integerLength) { this.integerLength = integerLength; }
        
        public int getDecimalLength() { return decimalLength; }
        public void setDecimalLength(int decimalLength) { this.decimalLength = decimalLength; }
        
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
    }
    
    public enum DataType {
        NUMERIC,
        SIGNED_NUMERIC,
        ALPHABETIC,
        ALPHANUMERIC
    }
    
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private int validLines = 0;
        private int totalLines = 0;
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void incrementValidLines() {
            validLines++;
        }
        
        public void merge(ValidationResult other) {
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
            this.validLines += other.validLines;
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        // Getters
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public int getValidLines() { return validLines; }
        public int getTotalLines() { return totalLines; }
        
        public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    }
}