package com.filetransfer.web.service;

import com.filetransfer.web.entity.FileType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for performing file type specific validation rules.
 * This service implements comprehensive validation for different file formats.
 */
@Service
public class FileTypeSpecificValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTypeSpecificValidator.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    // Validation patterns
    private static final Pattern CSV_DELIMITER_PATTERN = Pattern.compile("[,;\\t|]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\+]?[0-9\\-\\(\\)\\s]{7,15}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{8}");
    
    /**
     * Validate file content based on its type
     */
    public FileTypeValidationResult validateFileContent(FileType fileType, byte[] content, String fileName) {
        FileTypeValidationResult result = new FileTypeValidationResult();
        result.setFileType(fileType);
        result.setFileName(fileName);
        
        try {
            switch (fileType) {
                case XML:
                    return validateXmlFile(content, result);
                case JSON:
                    return validateJsonFile(content, result);
                case CSV:
                    return validateCsvFile(content, result);
                case COBOL_FLAT_FILE:
                    return validateCobolFlatFile(content, result);
                case BINARY_FILE:
                    return validateBinaryFile(content, result);
                case TEXT:
                    return validateTextFile(content, result);
                case FIXED_WIDTH:
                    return validateFixedWidthFile(content, result);
                default:
                    result.setValid(true);
                    result.addWarning("No specific validation rules for file type: " + fileType);
                    return result;
            }
        } catch (Exception e) {
            logger.error("Error validating file content for type {}: {}", fileType, e.getMessage(), e);
            result.setValid(false);
            result.addError("Validation error: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Validate XML file
     */
    private FileTypeValidationResult validateXmlFile(byte[] content, FileTypeValidationResult result) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(content));
            
            result.setValid(true);
            result.addInfo("Valid XML document with root element: " + document.getDocumentElement().getNodeName());
            
            // Additional XML validation
            validateXmlStructure(document, result);
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            result.setValid(false);
            result.addError("Invalid XML: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate JSON file
     */
    private FileTypeValidationResult validateJsonFile(byte[] content, FileTypeValidationResult result) {
        try {
            String jsonString = new String(content, StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            result.setValid(true);
            result.addInfo("Valid JSON document");
            
            // Additional JSON validation
            validateJsonStructure(jsonNode, result);
            
        } catch (IOException e) {
            result.setValid(false);
            result.addError("Invalid JSON: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate CSV file
     */
    private FileTypeValidationResult validateCsvFile(byte[] content, FileTypeValidationResult result) {
        try {
            String csvString = new String(content, StandardCharsets.UTF_8);
            String[] lines = csvString.split("\\r?\\n");
            
            if (lines.length == 0) {
                result.setValid(false);
                result.addError("CSV file is empty");
                return result;
            }
            
            // Detect delimiter
            String delimiter = detectCsvDelimiter(lines[0]);
            result.addInfo("Detected CSV delimiter: '" + delimiter + "'");
            
            // Validate CSV structure
            validateCsvStructure(lines, delimiter, result);
            
            result.setValid(true);
            result.addInfo("CSV file has " + lines.length + " lines");
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("CSV validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate COBOL flat file
     */
    private FileTypeValidationResult validateCobolFlatFile(byte[] content, FileTypeValidationResult result) {
        try {
            String fileContent = new String(content, StandardCharsets.UTF_8);
            String[] lines = fileContent.split("\\r?\\n");
            
            if (lines.length == 0) {
                result.setValid(false);
                result.addError("COBOL flat file is empty");
                return result;
            }
            
            // Validate fixed-width structure
            validateFixedWidthStructure(lines, result);
            
            // Check for COBOL-specific patterns
            validateCobolSpecificPatterns(lines, result);
            
            result.setValid(true);
            result.addInfo("COBOL flat file has " + lines.length + " records");
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("COBOL flat file validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate binary file
     */
    private FileTypeValidationResult validateBinaryFile(byte[] content, FileTypeValidationResult result) {
        try {
            // Basic binary file validation
            if (content.length == 0) {
                result.setValid(false);
                result.addError("Binary file is empty");
                return result;
            }
            
            // Check for common binary file signatures
            String fileType = detectBinaryFileType(content);
            result.addInfo("Detected binary file type: " + fileType);
            
            // Validate file integrity based on type
            validateBinaryIntegrity(content, fileType, result);
            
            result.setValid(true);
            result.addInfo("Binary file size: " + content.length + " bytes");
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Binary file validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate text file
     */
    private FileTypeValidationResult validateTextFile(byte[] content, FileTypeValidationResult result) {
        try {
            String textContent = new String(content, StandardCharsets.UTF_8);
            String[] lines = textContent.split("\\r?\\n");
            
            if (lines.length == 0) {
                result.setValid(false);
                result.addError("Text file is empty");
                return result;
            }
            
            // Validate character encoding
            validateTextEncoding(content, result);
            
            // Check for suspicious content
            validateTextContent(lines, result);
            
            result.setValid(true);
            result.addInfo("Text file has " + lines.length + " lines");
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Text file validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate fixed-width file
     */
    private FileTypeValidationResult validateFixedWidthFile(byte[] content, FileTypeValidationResult result) {
        try {
            String fileContent = new String(content, StandardCharsets.UTF_8);
            String[] lines = fileContent.split("\\r?\\n");
            
            if (lines.length == 0) {
                result.setValid(false);
                result.addError("Fixed-width file is empty");
                return result;
            }
            
            // Validate consistent line lengths
            validateFixedWidthStructure(lines, result);
            
            result.setValid(true);
            result.addInfo("Fixed-width file has " + lines.length + " records");
            
        } catch (Exception e) {
            result.setValid(false);
            result.addError("Fixed-width file validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    // Helper methods for specific validations
    
    private void validateXmlStructure(Document document, FileTypeValidationResult result) {
        // Check for common XML issues
        String rootElement = document.getDocumentElement().getNodeName();
        
        if (rootElement.isEmpty()) {
            result.addWarning("XML root element has no name");
        }
        
        // Check for namespace declarations
        if (document.getDocumentElement().getNamespaceURI() != null) {
            result.addInfo("XML uses namespaces");
        }
    }
    
    private void validateJsonStructure(JsonNode jsonNode, FileTypeValidationResult result) {
        // Check JSON structure
        if (jsonNode.isArray()) {
            result.addInfo("JSON is an array with " + jsonNode.size() + " elements");
            
            // Validate array consistency
            if (jsonNode.size() > 1) {
                JsonNode first = jsonNode.get(0);
                JsonNode second = jsonNode.get(1);
                
                if (first.isObject() && second.isObject()) {
                    // Check if objects have similar structure
                    if (!first.fieldNames().hasNext() && !second.fieldNames().hasNext()) {
                        result.addWarning("JSON array contains empty objects");
                    }
                }
            }
        } else if (jsonNode.isObject()) {
            result.addInfo("JSON is an object with " + jsonNode.size() + " fields");
            
            if (jsonNode.size() == 0) {
                result.addWarning("JSON object is empty");
            }
        } else {
            result.addInfo("JSON is a primitive value");
        }
    }
    
    private String detectCsvDelimiter(String headerLine) {
        int commaCount = (int) headerLine.chars().filter(c -> c == ',').count();
        int semicolonCount = (int) headerLine.chars().filter(c -> c == ';').count();
        int tabCount = (int) headerLine.chars().filter(c -> c == '\t').count();
        int pipeCount = (int) headerLine.chars().filter(c -> c == '|').count();
        
        if (commaCount >= semicolonCount && commaCount >= tabCount && commaCount >= pipeCount) {
            return ",";
        } else if (semicolonCount >= tabCount && semicolonCount >= pipeCount) {
            return ";";
        } else if (tabCount >= pipeCount) {
            return "\t";
        } else {
            return "|";
        }
    }
    
    private void validateCsvStructure(String[] lines, String delimiter, FileTypeValidationResult result) {
        if (lines.length < 2) {
            result.addWarning("CSV file has only header row or single line");
            return;
        }
        
        String[] headers = lines[0].split(Pattern.quote(delimiter));
        int expectedColumns = headers.length;
        
        result.addInfo("CSV has " + expectedColumns + " columns");
        
        // Validate data rows
        int inconsistentRows = 0;
        for (int i = 1; i < Math.min(lines.length, 100); i++) { // Check first 100 rows
            String[] columns = lines[i].split(Pattern.quote(delimiter));
            if (columns.length != expectedColumns) {
                inconsistentRows++;
            }
        }
        
        if (inconsistentRows > 0) {
            result.addWarning("Found " + inconsistentRows + " rows with inconsistent column count");
        }
        
        // Validate data types in columns
        validateCsvDataTypes(lines, delimiter, result);
    }
    
    private void validateCsvDataTypes(String[] lines, String delimiter, FileTypeValidationResult result) {
        if (lines.length < 2) return;
        
        String[] headers = lines[0].split(Pattern.quote(delimiter));
        
        // Sample first few data rows to detect data types
        for (int col = 0; col < headers.length && col < 10; col++) { // Check first 10 columns
            String columnName = headers[col];
            
            boolean allNumeric = true;
            boolean allDates = true;
            boolean allEmails = true;
            
            for (int row = 1; row < Math.min(lines.length, 20) && row < 20; row++) { // Check first 20 rows
                String[] columns = lines[row].split(Pattern.quote(delimiter));
                if (col < columns.length) {
                    String value = columns[col].trim();
                    
                    if (!value.isEmpty()) {
                        if (allNumeric && !isNumeric(value)) {
                            allNumeric = false;
                        }
                        if (allDates && !DATE_PATTERN.matcher(value).matches()) {
                            allDates = false;
                        }
                        if (allEmails && !EMAIL_PATTERN.matcher(value).matches()) {
                            allEmails = false;
                        }
                    }
                }
            }
            
            if (allNumeric) {
                result.addInfo("Column '" + columnName + "' appears to be numeric");
            } else if (allDates) {
                result.addInfo("Column '" + columnName + "' appears to contain dates");
            } else if (allEmails) {
                result.addInfo("Column '" + columnName + "' appears to contain email addresses");
            }
        }
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void validateFixedWidthStructure(String[] lines, FileTypeValidationResult result) {
        if (lines.length == 0) return;
        
        int firstLineLength = lines[0].length();
        result.addInfo("Fixed-width record length: " + firstLineLength + " characters");
        
        int inconsistentRows = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].length() != firstLineLength) {
                inconsistentRows++;
            }
        }
        
        if (inconsistentRows > 0) {
            result.addWarning("Found " + inconsistentRows + " rows with inconsistent length");
        }
    }
    
    private void validateCobolSpecificPatterns(String[] lines, FileTypeValidationResult result) {
        // Check for COBOL numeric fields (typically right-justified with leading zeros)
        boolean hasNumericFields = false;
        boolean hasPackedFields = false;
        
        for (String line : lines) {
            if (line.matches(".*\\d{6,}.*")) { // Look for long numeric sequences
                hasNumericFields = true;
            }
            
            // Look for patterns that might indicate packed decimal
            if (line.matches(".*[\\x00-\\x1F\\x7F-\\xFF].*")) {
                hasPackedFields = true;
            }
        }
        
        if (hasNumericFields) {
            result.addInfo("File contains COBOL-style numeric fields");
        }
        
        if (hasPackedFields) {
            result.addWarning("File may contain packed decimal or binary data");
        }
    }
    
    private String detectBinaryFileType(byte[] content) {
        if (content.length < 8) {
            return "Unknown (too small)";
        }
        
        // Check common file signatures
        if (content[0] == (byte) 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "PNG Image";
        } else if (content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) {
            return "JPEG Image";
        } else if (content[0] == 'P' && content[1] == 'K') {
            return "ZIP Archive";
        } else if (content[0] == (byte) 0x50 && content[1] == (byte) 0x4B) {
            return "ZIP/Office Document";
        } else if (content[0] == (byte) 0x25 && content[1] == (byte) 0x50 && content[2] == (byte) 0x44 && content[3] == (byte) 0x46) {
            return "PDF Document";
        } else {
            return "Unknown Binary";
        }
    }
    
    private void validateBinaryIntegrity(byte[] content, String fileType, FileTypeValidationResult result) {
        // Basic integrity checks based on file type
        if (fileType.contains("ZIP")) {
            // Check for ZIP end of central directory signature
            boolean hasValidZipEnd = false;
            for (int i = content.length - 22; i >= Math.max(0, content.length - 65557); i--) {
                if (i >= 0 && content[i] == (byte) 0x50 && i + 3 < content.length &&
                    content[i + 1] == (byte) 0x4B && content[i + 2] == (byte) 0x05 && content[i + 3] == (byte) 0x06) {
                    hasValidZipEnd = true;
                    break;
                }
            }
            
            if (!hasValidZipEnd) {
                result.addWarning("ZIP file may be corrupted - no valid end signature found");
            } else {
                result.addInfo("ZIP file structure appears valid");
            }
        }
    }
    
    private void validateTextEncoding(byte[] content, FileTypeValidationResult result) {
        // Check for BOM (Byte Order Mark)
        if (content.length >= 3 && content[0] == (byte) 0xEF && content[1] == (byte) 0xBB && content[2] == (byte) 0xBF) {
            result.addInfo("Text file has UTF-8 BOM");
        }
        
        // Check for control characters that might indicate binary content
        int controlChars = 0;
        for (byte b : content) {
            if (b < 32 && b != 9 && b != 10 && b != 13) { // Exclude tab, LF, CR
                controlChars++;
            }
        }
        
        if (controlChars > content.length * 0.1) { // More than 10% control characters
            result.addWarning("Text file contains many control characters - may be binary");
        }
    }
    
    private void validateTextContent(String[] lines, FileTypeValidationResult result) {
        int veryLongLines = 0;
        int emptyLines = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                emptyLines++;
            } else if (line.length() > 1000) {
                veryLongLines++;
            }
        }
        
        if (emptyLines > lines.length * 0.5) {
            result.addWarning("Text file contains many empty lines (" + emptyLines + "/" + lines.length + ")");
        }
        
        if (veryLongLines > 0) {
            result.addWarning("Text file contains " + veryLongLines + " very long lines (>1000 chars)");
        }
    }
    
    // Result class
    public static class FileTypeValidationResult {
        private FileType fileType;
        private String fileName;
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            valid = false;
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addInfo(String info) {
            this.info.add(info);
        }
        
        // Getters and setters
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfo() { return info; }
        
        public boolean hasIssues() {
            return !errors.isEmpty() || !warnings.isEmpty();
        }
    }
}