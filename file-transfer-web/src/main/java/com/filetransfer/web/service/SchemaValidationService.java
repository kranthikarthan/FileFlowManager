package com.filetransfer.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.entity.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for validating file content against predefined schemas
 */
@Service
public class SchemaValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Common CSV patterns for validation
    private static final Map<String, Pattern> CSV_PATTERNS = Map.of(
        "EMAIL", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"),
        "PHONE", Pattern.compile("^\\+?[1-9]\\d{1,14}$|^\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$"),
        "DATE_ISO", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"),
        "DATE_US", Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$"),
        "CURRENCY", Pattern.compile("^\\$?[0-9,]+\\.?\\d{0,2}$"),
        "SSN", Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$"),
        "ZIP_CODE", Pattern.compile("^\\d{5}(-\\d{4})?$")
    );
    
    /**
     * Validate file content against schema
     */
    public SchemaValidationResult validateFileSchema(String filePath, String schemaPath, FileType fileType) {
        try {
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                return new SchemaValidationResult(false, "File not found: " + filePath);
            }
            
            SchemaValidationResult result = new SchemaValidationResult(true, "Schema validation completed");
            result.setFilePath(filePath);
            result.setSchemaPath(schemaPath);
            result.setFileType(fileType);
            
            switch (fileType) {
                case CSV:
                    return validateCsvSchema(file, schemaPath, result);
                case JSON:
                    return validateJsonSchema(file, schemaPath, result);
                case XML:
                    return validateXmlSchema(file, schemaPath, result);
                case DELIMITED:
                    return validateDelimitedSchema(file, schemaPath, result);
                case FIXED_WIDTH:
                    return validateFixedWidthSchema(file, schemaPath, result);
                default:
                    result.setSuccess(false);
                    result.setMessage("Schema validation not supported for file type: " + fileType);
                    return result;
            }
            
        } catch (Exception e) {
            logger.error("Error validating schema for file {}: {}", filePath, e.getMessage(), e);
            return new SchemaValidationResult(false, "Schema validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Validate CSV file against schema
     */
    private SchemaValidationResult validateCsvSchema(Path filePath, String schemaPath, SchemaValidationResult result) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("CSV file is empty");
            return result;
        }
        
        // Load CSV schema definition
        CsvSchema schema = loadCsvSchema(schemaPath);
        if (schema == null) {
            result.setSuccess(false);
            result.setMessage("Could not load CSV schema from: " + schemaPath);
            return result;
        }
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate header
        String headerLine = lines.get(0);
        String[] headers = headerLine.split(",");
        
        if (schema.getExpectedColumns() != null && headers.length != schema.getExpectedColumns().size()) {
            errors.add(new ValidationError(1, "HEADER", 
                "Expected " + schema.getExpectedColumns().size() + " columns, found " + headers.length));
        }
        
        // Validate column names if specified
        if (schema.getExpectedColumns() != null) {
            for (int i = 0; i < Math.min(headers.length, schema.getExpectedColumns().size()); i++) {
                CsvColumnSchema columnSchema = schema.getExpectedColumns().get(i);
                if (!columnSchema.getName().equalsIgnoreCase(headers[i].trim())) {
                    errors.add(new ValidationError(1, "HEADER", 
                        "Expected column '" + columnSchema.getName() + "' at position " + (i + 1) + 
                        ", found '" + headers[i].trim() + "'"));
                }
            }
        }
        
        // Validate data rows
        for (int lineNum = 2; lineNum <= Math.min(lines.size(), 1000); lineNum++) { // Validate first 1000 rows
            String line = lines.get(lineNum - 1);
            String[] values = line.split(",");
            
            if (values.length != headers.length) {
                errors.add(new ValidationError(lineNum, "STRUCTURE", 
                    "Expected " + headers.length + " columns, found " + values.length));
                continue;
            }
            
            // Validate individual fields
            if (schema.getExpectedColumns() != null) {
                for (int i = 0; i < values.length && i < schema.getExpectedColumns().size(); i++) {
                    CsvColumnSchema columnSchema = schema.getExpectedColumns().get(i);
                    String value = values[i].trim();
                    
                    ValidationError fieldError = validateCsvField(value, columnSchema, lineNum, i + 1);
                    if (fieldError != null) {
                        errors.add(fieldError);
                    }
                }
            }
        }
        
        result.setValidationErrors(errors);
        result.setSuccess(errors.isEmpty());
        if (!errors.isEmpty()) {
            result.setMessage("CSV validation failed with " + errors.size() + " errors");
        }
        
        return result;
    }
    
    /**
     * Validate JSON file against schema
     */
    private SchemaValidationResult validateJsonSchema(Path filePath, String schemaPath, SchemaValidationResult result) throws IOException {
        String content = Files.readString(filePath);
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(content);
            
            // Load JSON schema if provided
            if (schemaPath != null && !schemaPath.isEmpty()) {
                JsonNode schemaNode = objectMapper.readTree(new File(schemaPath));
                
                // Basic JSON schema validation
                errors.addAll(validateJsonNode(jsonNode, schemaNode, "$"));
            } else {
                // Basic JSON structure validation
                if (!jsonNode.isObject() && !jsonNode.isArray()) {
                    errors.add(new ValidationError(1, "STRUCTURE", "JSON must be object or array at root level"));
                }
            }
            
        } catch (Exception e) {
            errors.add(new ValidationError(1, "PARSE", "JSON parsing failed: " + e.getMessage()));
        }
        
        result.setValidationErrors(errors);
        result.setSuccess(errors.isEmpty());
        if (!errors.isEmpty()) {
            result.setMessage("JSON validation failed with " + errors.size() + " errors");
        }
        
        return result;
    }
    
    /**
     * Validate XML file against XSD schema
     */
    private SchemaValidationResult validateXmlSchema(Path filePath, String schemaPath, SchemaValidationResult result) throws IOException {
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            if (schemaPath != null && !schemaPath.isEmpty()) {
                // Validate against XSD schema
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(new File(schemaPath));
                Validator validator = schema.newValidator();
                
                validator.validate(new StreamSource(filePath.toFile()));
            } else {
                // Basic XML well-formedness check
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(filePath.toFile());
                doc.getDocumentElement().normalize();
            }
            
        } catch (SAXException e) {
            errors.add(new ValidationError(1, "XML_VALIDATION", "XML validation failed: " + e.getMessage()));
        } catch (Exception e) {
            errors.add(new ValidationError(1, "PARSE", "XML parsing failed: " + e.getMessage()));
        }
        
        result.setValidationErrors(errors);
        result.setSuccess(errors.isEmpty());
        if (!errors.isEmpty()) {
            result.setMessage("XML validation failed with " + errors.size() + " errors");
        }
        
        return result;
    }
    
    /**
     * Validate delimited file schema
     */
    private SchemaValidationResult validateDelimitedSchema(Path filePath, String schemaPath, SchemaValidationResult result) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<ValidationError> errors = new ArrayList<>();
        
        if (lines.isEmpty()) {
            errors.add(new ValidationError(1, "EMPTY", "Delimited file is empty"));
        } else {
            // Detect delimiter
            String delimiter = detectDelimiter(lines.get(0));
            if (delimiter == null) {
                errors.add(new ValidationError(1, "DELIMITER", "Could not detect delimiter in file"));
            } else {
                // Validate structure consistency
                String[] expectedColumns = lines.get(0).split(Pattern.quote(delimiter));
                
                for (int i = 1; i < Math.min(lines.size(), 1000); i++) {
                    String[] actualColumns = lines.get(i).split(Pattern.quote(delimiter));
                    if (actualColumns.length != expectedColumns.length) {
                        errors.add(new ValidationError(i + 1, "STRUCTURE", 
                            "Expected " + expectedColumns.length + " columns, found " + actualColumns.length));
                    }
                }
            }
        }
        
        result.setValidationErrors(errors);
        result.setSuccess(errors.isEmpty());
        if (!errors.isEmpty()) {
            result.setMessage("Delimited file validation failed with " + errors.size() + " errors");
        }
        
        return result;
    }
    
    /**
     * Validate fixed-width file schema
     */
    private SchemaValidationResult validateFixedWidthSchema(Path filePath, String schemaPath, SchemaValidationResult result) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<ValidationError> errors = new ArrayList<>();
        
        if (lines.isEmpty()) {
            errors.add(new ValidationError(1, "EMPTY", "Fixed-width file is empty"));
        } else {
            // Check record length consistency
            int expectedLength = lines.get(0).length();
            
            for (int i = 1; i < Math.min(lines.size(), 1000); i++) {
                if (lines.get(i).length() != expectedLength) {
                    errors.add(new ValidationError(i + 1, "LENGTH", 
                        "Expected record length " + expectedLength + ", found " + lines.get(i).length()));
                }
            }
            
            // Load fixed-width schema if provided
            if (schemaPath != null && !schemaPath.isEmpty()) {
                FixedWidthSchema schema = loadFixedWidthSchema(schemaPath);
                if (schema != null) {
                    validateFixedWidthFields(lines, schema, errors);
                }
            }
        }
        
        result.setValidationErrors(errors);
        result.setSuccess(errors.isEmpty());
        if (!errors.isEmpty()) {
            result.setMessage("Fixed-width validation failed with " + errors.size() + " errors");
        }
        
        return result;
    }
    
    /**
     * Validate CSV field against column schema
     */
    private ValidationError validateCsvField(String value, CsvColumnSchema columnSchema, int lineNum, int columnNum) {
        // Check required fields
        if (columnSchema.isRequired() && (value == null || value.trim().isEmpty())) {
            return new ValidationError(lineNum, "REQUIRED", 
                "Required field '" + columnSchema.getName() + "' is empty at column " + columnNum);
        }
        
        if (value == null || value.trim().isEmpty()) {
            return null; // Empty values are OK for non-required fields
        }
        
        // Check data type
        if (columnSchema.getDataType() != null) {
            switch (columnSchema.getDataType().toUpperCase()) {
                case "INTEGER":
                    if (!isValidInteger(value)) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid integer value '" + value + "' in column " + columnNum);
                    }
                    break;
                case "DECIMAL":
                    if (!isValidDecimal(value)) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid decimal value '" + value + "' in column " + columnNum);
                    }
                    break;
                case "DATE":
                    if (!isValidDate(value)) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid date value '" + value + "' in column " + columnNum);
                    }
                    break;
                case "EMAIL":
                    if (!CSV_PATTERNS.get("EMAIL").matcher(value).matches()) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid email value '" + value + "' in column " + columnNum);
                    }
                    break;
            }
        }
        
        // Check length constraints
        if (columnSchema.getMaxLength() != null && value.length() > columnSchema.getMaxLength()) {
            return new ValidationError(lineNum, "LENGTH", 
                "Value '" + value + "' exceeds maximum length " + columnSchema.getMaxLength() + " in column " + columnNum);
        }
        
        if (columnSchema.getMinLength() != null && value.length() < columnSchema.getMinLength()) {
            return new ValidationError(lineNum, "LENGTH", 
                "Value '" + value + "' is shorter than minimum length " + columnSchema.getMinLength() + " in column " + columnNum);
        }
        
        // Check pattern validation
        if (columnSchema.getPattern() != null) {
            Pattern pattern = Pattern.compile(columnSchema.getPattern());
            if (!pattern.matcher(value).matches()) {
                return new ValidationError(lineNum, "PATTERN", 
                    "Value '" + value + "' does not match required pattern in column " + columnNum);
            }
        }
        
        // Check allowed values
        if (columnSchema.getAllowedValues() != null && !columnSchema.getAllowedValues().contains(value)) {
            return new ValidationError(lineNum, "ALLOWED_VALUES", 
                "Value '" + value + "' is not in allowed values list for column " + columnNum);
        }
        
        return null; // No validation errors
    }
    
    /**
     * Validate JSON node against schema
     */
    private List<ValidationError> validateJsonNode(JsonNode data, JsonNode schema, String path) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Basic JSON Schema validation (simplified implementation)
        if (schema.has("type")) {
            String expectedType = schema.get("type").asText();
            String actualType = getJsonNodeType(data);
            
            if (!expectedType.equals(actualType)) {
                errors.add(new ValidationError(1, "TYPE", 
                    "Expected type '" + expectedType + "' but found '" + actualType + "' at " + path));
            }
        }
        
        // Validate required properties for objects
        if (data.isObject() && schema.has("required")) {
            JsonNode required = schema.get("required");
            if (required.isArray()) {
                for (JsonNode requiredField : required) {
                    String fieldName = requiredField.asText();
                    if (!data.has(fieldName)) {
                        errors.add(new ValidationError(1, "REQUIRED", 
                            "Required property '" + fieldName + "' is missing at " + path));
                    }
                }
            }
        }
        
        // Validate properties
        if (data.isObject() && schema.has("properties")) {
            JsonNode properties = schema.get("properties");
            data.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                
                if (properties.has(fieldName)) {
                    JsonNode fieldSchema = properties.get(fieldName);
                    errors.addAll(validateJsonNode(fieldValue, fieldSchema, path + "." + fieldName));
                }
            });
        }
        
        // Validate array items
        if (data.isArray() && schema.has("items")) {
            JsonNode itemSchema = schema.get("items");
            for (int i = 0; i < data.size(); i++) {
                errors.addAll(validateJsonNode(data.get(i), itemSchema, path + "[" + i + "]"));
            }
        }
        
        return errors;
    }
    
    /**
     * Validate fixed-width fields against schema
     */
    private void validateFixedWidthFields(List<String> lines, FixedWidthSchema schema, List<ValidationError> errors) {
        for (int lineNum = 1; lineNum <= Math.min(lines.size(), 1000); lineNum++) {
            String line = lines.get(lineNum - 1);
            
            for (FixedWidthFieldSchema fieldSchema : schema.getFields()) {
                int startPos = fieldSchema.getStartPosition() - 1; // Convert to 0-based
                int endPos = startPos + fieldSchema.getLength();
                
                if (startPos < 0 || endPos > line.length()) {
                    errors.add(new ValidationError(lineNum, "FIELD_BOUNDARY", 
                        "Field '" + fieldSchema.getName() + "' boundary exceeds line length"));
                    continue;
                }
                
                String fieldValue = line.substring(startPos, endPos).trim();
                
                // Validate field based on its schema
                ValidationError fieldError = validateFixedWidthField(fieldValue, fieldSchema, lineNum);
                if (fieldError != null) {
                    errors.add(fieldError);
                }
            }
        }
    }
    
    /**
     * Validate individual fixed-width field
     */
    private ValidationError validateFixedWidthField(String value, FixedWidthFieldSchema fieldSchema, int lineNum) {
        // Check required fields
        if (fieldSchema.isRequired() && value.isEmpty()) {
            return new ValidationError(lineNum, "REQUIRED", 
                "Required field '" + fieldSchema.getName() + "' is empty");
        }
        
        if (value.isEmpty()) {
            return null; // Empty values are OK for non-required fields
        }
        
        // Check data type
        if (fieldSchema.getDataType() != null) {
            switch (fieldSchema.getDataType().toUpperCase()) {
                case "NUMERIC":
                    if (!isValidDecimal(value)) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid numeric value '" + value + "' in field '" + fieldSchema.getName() + "'");
                    }
                    break;
                case "ALPHANUMERIC":
                    if (!value.matches("^[a-zA-Z0-9\\s]*$")) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid alphanumeric value '" + value + "' in field '" + fieldSchema.getName() + "'");
                    }
                    break;
                case "DATE":
                    if (!isValidDate(value)) {
                        return new ValidationError(lineNum, "DATA_TYPE", 
                            "Invalid date value '" + value + "' in field '" + fieldSchema.getName() + "'");
                    }
                    break;
            }
        }
        
        return null;
    }
    
    /**
     * Load CSV schema from file or create default
     */
    private CsvSchema loadCsvSchema(String schemaPath) {
        if (schemaPath == null || schemaPath.isEmpty()) {
            return createDefaultCsvSchema();
        }
        
        try {
            // Load schema from JSON file
            JsonNode schemaNode = objectMapper.readTree(new File(schemaPath));
            return parseCsvSchema(schemaNode);
        } catch (Exception e) {
            logger.warn("Could not load CSV schema from {}, using default: {}", schemaPath, e.getMessage());
            return createDefaultCsvSchema();
        }
    }
    
    /**
     * Load fixed-width schema from file
     */
    private FixedWidthSchema loadFixedWidthSchema(String schemaPath) {
        if (schemaPath == null || schemaPath.isEmpty()) {
            return null;
        }
        
        try {
            JsonNode schemaNode = objectMapper.readTree(new File(schemaPath));
            return parseFixedWidthSchema(schemaNode);
        } catch (Exception e) {
            logger.warn("Could not load fixed-width schema from {}: {}", schemaPath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Create default CSV schema for basic validation
     */
    private CsvSchema createDefaultCsvSchema() {
        CsvSchema schema = new CsvSchema();
        schema.setHasHeader(true);
        schema.setDelimiter(",");
        // No specific column definitions - will only validate basic structure
        return schema;
    }
    
    /**
     * Parse CSV schema from JSON
     */
    private CsvSchema parseCsvSchema(JsonNode schemaNode) {
        CsvSchema schema = new CsvSchema();
        
        if (schemaNode.has("hasHeader")) {
            schema.setHasHeader(schemaNode.get("hasHeader").asBoolean());
        }
        
        if (schemaNode.has("delimiter")) {
            schema.setDelimiter(schemaNode.get("delimiter").asText());
        }
        
        if (schemaNode.has("columns")) {
            List<CsvColumnSchema> columns = new ArrayList<>();
            JsonNode columnsNode = schemaNode.get("columns");
            
            for (JsonNode columnNode : columnsNode) {
                CsvColumnSchema columnSchema = new CsvColumnSchema();
                columnSchema.setName(columnNode.get("name").asText());
                
                if (columnNode.has("dataType")) {
                    columnSchema.setDataType(columnNode.get("dataType").asText());
                }
                if (columnNode.has("required")) {
                    columnSchema.setRequired(columnNode.get("required").asBoolean());
                }
                if (columnNode.has("maxLength")) {
                    columnSchema.setMaxLength(columnNode.get("maxLength").asInt());
                }
                if (columnNode.has("pattern")) {
                    columnSchema.setPattern(columnNode.get("pattern").asText());
                }
                
                columns.add(columnSchema);
            }
            
            schema.setExpectedColumns(columns);
        }
        
        return schema;
    }
    
    /**
     * Parse fixed-width schema from JSON
     */
    private FixedWidthSchema parseFixedWidthSchema(JsonNode schemaNode) {
        FixedWidthSchema schema = new FixedWidthSchema();
        
        if (schemaNode.has("recordLength")) {
            schema.setRecordLength(schemaNode.get("recordLength").asInt());
        }
        
        if (schemaNode.has("fields")) {
            List<FixedWidthFieldSchema> fields = new ArrayList<>();
            JsonNode fieldsNode = schemaNode.get("fields");
            
            for (JsonNode fieldNode : fieldsNode) {
                FixedWidthFieldSchema fieldSchema = new FixedWidthFieldSchema();
                fieldSchema.setName(fieldNode.get("name").asText());
                fieldSchema.setStartPosition(fieldNode.get("startPosition").asInt());
                fieldSchema.setLength(fieldNode.get("length").asInt());
                
                if (fieldNode.has("dataType")) {
                    fieldSchema.setDataType(fieldNode.get("dataType").asText());
                }
                if (fieldNode.has("required")) {
                    fieldSchema.setRequired(fieldNode.get("required").asBoolean());
                }
                
                fields.add(fieldSchema);
            }
            
            schema.setFields(fields);
        }
        
        return schema;
    }
    
    // Utility methods
    private String detectDelimiter(String line) {
        String[] delimiters = {"|", "\t", ";", ":", "~"};
        for (String delimiter : delimiters) {
            if (line.contains(delimiter)) {
                return delimiter;
            }
        }
        return null;
    }
    
    private String getJsonNodeType(JsonNode node) {
        if (node.isObject()) return "object";
        if (node.isArray()) return "array";
        if (node.isTextual()) return "string";
        if (node.isIntegralNumber()) return "integer";
        if (node.isFloatingPointNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isNull()) return "null";
        return "unknown";
    }
    
    private boolean isValidInteger(String value) {
        try {
            Long.parseLong(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isValidDecimal(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isValidDate(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}") || 
               value.matches("\\d{2}/\\d{2}/\\d{4}") ||
               value.matches("\\d{2}-\\d{2}-\\d{4}") ||
               value.matches("\\d{8}"); // YYYYMMDD
    }
    
    // Schema classes
    
    public static class CsvSchema {
        private boolean hasHeader = true;
        private String delimiter = ",";
        private List<CsvColumnSchema> expectedColumns;
        
        // Getters and Setters
        public boolean isHasHeader() { return hasHeader; }
        public void setHasHeader(boolean hasHeader) { this.hasHeader = hasHeader; }
        
        public String getDelimiter() { return delimiter; }
        public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
        
        public List<CsvColumnSchema> getExpectedColumns() { return expectedColumns; }
        public void setExpectedColumns(List<CsvColumnSchema> expectedColumns) { this.expectedColumns = expectedColumns; }
    }
    
    public static class CsvColumnSchema {
        private String name;
        private String dataType;
        private boolean required = false;
        private Integer maxLength;
        private Integer minLength;
        private String pattern;
        private List<String> allowedValues;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        
        public Integer getMinLength() { return minLength; }
        public void setMinLength(Integer minLength) { this.minLength = minLength; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public List<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
    }
    
    public static class FixedWidthSchema {
        private int recordLength;
        private List<FixedWidthFieldSchema> fields;
        
        // Getters and Setters
        public int getRecordLength() { return recordLength; }
        public void setRecordLength(int recordLength) { this.recordLength = recordLength; }
        
        public List<FixedWidthFieldSchema> getFields() { return fields; }
        public void setFields(List<FixedWidthFieldSchema> fields) { this.fields = fields; }
    }
    
    public static class FixedWidthFieldSchema {
        private String name;
        private int startPosition;
        private int length;
        private String dataType;
        private boolean required = false;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getStartPosition() { return startPosition; }
        public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
        
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }
    
    public static class SchemaValidationResult {
        private boolean success;
        private String message;
        private String filePath;
        private String schemaPath;
        private FileType fileType;
        private List<ValidationError> validationErrors = new ArrayList<>();
        private Date validationTimestamp;
        
        public SchemaValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.validationTimestamp = new Date();
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getSchemaPath() { return schemaPath; }
        public void setSchemaPath(String schemaPath) { this.schemaPath = schemaPath; }
        
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
        
        public List<ValidationError> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<ValidationError> validationErrors) { this.validationErrors = validationErrors; }
        
        public Date getValidationTimestamp() { return validationTimestamp; }
        public void setValidationTimestamp(Date validationTimestamp) { this.validationTimestamp = validationTimestamp; }
    }
    
    public static class ValidationError {
        private int lineNumber;
        private String errorType;
        private String message;
        
        public ValidationError(int lineNumber, String errorType, String message) {
            this.lineNumber = lineNumber;
            this.errorType = errorType;
            this.message = message;
        }
        
        // Getters and Setters
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}