package com.filetransfer.web.service;

import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.DailyFileCountTrackerRepository;
import com.filetransfer.web.repository.SchemaUsageMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for EOT (End of Day) file validation and count matching.
 * Validates the number of data files received against the count specified in EOT files.
 */
@Service
@Transactional
public class EotValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EotValidationService.class);
    
    @Autowired
    private DailyFileCountTrackerRepository dailyFileCountRepository;
    
    @Autowired
    private SchemaUsageMappingRepository schemaUsageMappingRepository;
    
    @Autowired
    private CobolCopybookParser cobolCopybookParser;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    
    /**
     * Record a data file for count tracking
     */
    public void recordDataFile(String tenantId, String serviceName, String subServiceName, 
                              LocalDate processingDate, FileType fileType, TransferDirection direction) {
        
        DailyFileCountTracker tracker = getOrCreateTracker(tenantId, serviceName, subServiceName, 
                                                          processingDate, fileType, direction);
        
        tracker.incrementActualCount();
        dailyFileCountRepository.save(tracker);
        
        logger.debug("Recorded data file for {}/{} on {}: count now {}", 
                    serviceName, subServiceName, processingDate, tracker.getActualCount());
    }
    
    /**
     * Record SOT file received
     */
    public void recordSotFile(String tenantId, String serviceName, String subServiceName, 
                             LocalDate processingDate, FileType fileType, TransferDirection direction) {
        
        DailyFileCountTracker tracker = getOrCreateTracker(tenantId, serviceName, subServiceName, 
                                                          processingDate, fileType, direction);
        
        tracker.recordSotReceived();
        dailyFileCountRepository.save(tracker);
        
        logger.info("Recorded SOT file for {}/{} on {}", serviceName, subServiceName, processingDate);
    }
    
    /**
     * Process EOT file and extract count for validation
     */
    public void processEotFile(String tenantId, String serviceName, String subServiceName, 
                              LocalDate processingDate, FileType fileType, TransferDirection direction,
                              byte[] fileContent, String fileName) {
        
        try {
            DailyFileCountTracker tracker = getOrCreateTracker(tenantId, serviceName, subServiceName, 
                                                              processingDate, fileType, direction);
            
            // Get the schema mapping for this EOT file to know which field contains the count
            Optional<SchemaUsageMapping> eotSchemaMapping = findEotSchemaMapping(tenantId, serviceName, subServiceName, direction);
            
            if (eotSchemaMapping.isEmpty()) {
                logger.warn("No EOT schema mapping found for {}/{}, skipping count validation", serviceName, subServiceName);
                tracker.recordEotReceived(null, "No schema mapping configured");
                dailyFileCountRepository.save(tracker);
                return;
            }
            
            SharedSchema eotSchema = eotSchemaMapping.get().getSharedSchema();
            
            if (!eotSchema.getSupportsCountValidation() || eotSchema.getEotCountFieldPath() == null) {
                logger.warn("EOT schema for {}/{} does not support count validation", serviceName, subServiceName);
                tracker.recordEotReceived(null, "Schema does not support count validation");
                dailyFileCountRepository.save(tracker);
                return;
            }
            
            // Extract count from EOT file based on file type and field path
            Integer extractedCount = extractCountFromEotFile(fileContent, eotSchema, fileName);
            
            tracker.recordEotReceived(extractedCount, eotSchema.getEotCountFieldPath());
            dailyFileCountRepository.save(tracker);
            
            logger.info("Processed EOT file for {}/{} on {}: extracted count = {}, actual count = {}", 
                       serviceName, subServiceName, processingDate, extractedCount, tracker.getActualCount());
            
            // Check for discrepancies and alert if needed
            if (tracker.hasDiscrepancy()) {
                alertDiscrepancy(tracker);
            }
            
        } catch (Exception e) {
            logger.error("Error processing EOT file for {}/{}: {}", serviceName, subServiceName, e.getMessage(), e);
            
            DailyFileCountTracker tracker = getOrCreateTracker(tenantId, serviceName, subServiceName, 
                                                              processingDate, fileType, direction);
            tracker.recordEotReceived(null, "Error processing EOT file: " + e.getMessage());
            dailyFileCountRepository.save(tracker);
        }
    }
    
    /**
     * Extract count value from EOT file based on schema field path
     */
    private Integer extractCountFromEotFile(byte[] fileContent, SharedSchema eotSchema, String fileName) {
        String fieldPath = eotSchema.getEotCountFieldPath();
        String contentString = new String(fileContent);
        
        try {
            switch (eotSchema.getFileType()) {
                case JSON:
                    return extractCountFromJson(contentString, fieldPath);
                    
                case XML:
                    return extractCountFromXml(fileContent, fieldPath);
                    
                case COBOL_FLAT_FILE:
                    return extractCountFromCobol(contentString, eotSchema.getSchemaDefinition(), fieldPath);
                    
                case CSV:
                    return extractCountFromCsv(contentString, fieldPath);
                    
                case TEXT:
                case FIXED_WIDTH:
                    return extractCountFromText(contentString, fieldPath);
                    
                default:
                    logger.warn("Unsupported file type for count extraction: {}", eotSchema.getFileType());
                    return null;
            }
            
        } catch (Exception e) {
            logger.error("Error extracting count from {} file {}: {}", eotSchema.getFileType(), fileName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract count from JSON using JSONPath
     */
    private Integer extractCountFromJson(String jsonContent, String fieldPath) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        
        // Simple JSONPath implementation (supports basic paths like "count", "header.recordCount", etc.)
        String[] pathParts = fieldPath.split("\\.");
        JsonNode currentNode = rootNode;
        
        for (String part : pathParts) {
            if (currentNode.has(part)) {
                currentNode = currentNode.get(part);
            } else {
                throw new IllegalArgumentException("Field path not found: " + fieldPath);
            }
        }
        
        if (currentNode.isNumber()) {
            return currentNode.asInt();
        } else {
            throw new IllegalArgumentException("Field is not numeric: " + fieldPath);
        }
    }
    
    /**
     * Extract count from XML using XPath
     */
    private Integer extractCountFromXml(byte[] xmlContent, String fieldPath) throws Exception {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent));
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expression = xpath.compile(fieldPath);
        
        Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
        if (node != null) {
            String textContent = node.getTextContent();
            return Integer.parseInt(textContent.trim());
        } else {
            throw new IllegalArgumentException("XPath not found: " + fieldPath);
        }
    }
    
    /**
     * Extract count from COBOL file using copybook field definition
     */
    private Integer extractCountFromCobol(String cobolContent, String copybookDefinition, String fieldName) throws Exception {
        CobolCopybookParser.CobolSchema schema = cobolCopybookParser.parseCopybook(copybookDefinition);
        
        // Find the field in the schema
        Optional<CobolCopybookParser.CobolField> targetField = schema.getFields().stream()
            .filter(field -> field.getName().equalsIgnoreCase(fieldName))
            .findFirst();
        
        if (targetField.isEmpty()) {
            throw new IllegalArgumentException("COBOL field not found: " + fieldName);
        }
        
        CobolCopybookParser.CobolField field = targetField.get();
        
        // Extract the field value from the file content
        String[] lines = cobolContent.split("\\r?\\n");
        if (lines.length > 0) {
            String firstLine = lines[0];
            if (firstLine.length() >= field.getStartPosition() + field.getLength()) {
                String fieldValue = firstLine.substring(field.getStartPosition(), 
                                                       field.getStartPosition() + field.getLength()).trim();
                return Integer.parseInt(fieldValue);
            }
        }
        
        throw new IllegalArgumentException("Could not extract COBOL field value: " + fieldName);
    }
    
    /**
     * Extract count from CSV file
     */
    private Integer extractCountFromCsv(String csvContent, String fieldPath) throws Exception {
        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("CSV file does not have enough lines");
        }
        
        String[] headers = lines[0].split(",");
        String[] values = lines[1].split(","); // Assume count is in first data row
        
        // Find column index
        int columnIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(fieldPath.trim())) {
                columnIndex = i;
                break;
            }
        }
        
        if (columnIndex == -1) {
            throw new IllegalArgumentException("CSV column not found: " + fieldPath);
        }
        
        if (columnIndex >= values.length) {
            throw new IllegalArgumentException("CSV row does not have enough columns");
        }
        
        return Integer.parseInt(values[columnIndex].trim());
    }
    
    /**
     * Extract count from text file using regex or position
     */
    private Integer extractCountFromText(String textContent, String fieldPath) throws Exception {
        // Support different extraction methods:
        // 1. Regex pattern: "regex:Count:(\\d+)"
        // 2. Position: "pos:10-15"
        // 3. Line and position: "line:2,pos:5-10"
        
        if (fieldPath.startsWith("regex:")) {
            String pattern = fieldPath.substring(6);
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(textContent);
            if (matcher.find() && matcher.groupCount() > 0) {
                return Integer.parseInt(matcher.group(1));
            } else {
                throw new IllegalArgumentException("Regex pattern did not match: " + pattern);
            }
            
        } else if (fieldPath.startsWith("pos:")) {
            String posRange = fieldPath.substring(4);
            String[] parts = posRange.split("-");
            int startPos = Integer.parseInt(parts[0]);
            int endPos = Integer.parseInt(parts[1]);
            
            if (endPos <= textContent.length()) {
                String value = textContent.substring(startPos, endPos).trim();
                return Integer.parseInt(value);
            } else {
                throw new IllegalArgumentException("Position range exceeds content length");
            }
            
        } else if (fieldPath.startsWith("line:")) {
            String[] parts = fieldPath.split(",");
            int lineNum = Integer.parseInt(parts[0].substring(5)) - 1; // Convert to 0-based
            String posRange = parts[1].substring(4);
            
            String[] lines = textContent.split("\\r?\\n");
            if (lineNum < lines.length) {
                String line = lines[lineNum];
                String[] posParts = posRange.split("-");
                int startPos = Integer.parseInt(posParts[0]);
                int endPos = Integer.parseInt(posParts[1]);
                
                if (endPos <= line.length()) {
                    String value = line.substring(startPos, endPos).trim();
                    return Integer.parseInt(value);
                } else {
                    throw new IllegalArgumentException("Position range exceeds line length");
                }
            } else {
                throw new IllegalArgumentException("Line number exceeds file length");
            }
            
        } else {
            throw new IllegalArgumentException("Unsupported field path format: " + fieldPath);
        }
    }
    
    /**
     * Find EOT schema mapping for a subservice
     */
    private Optional<SchemaUsageMapping> findEotSchemaMapping(String tenantId, String serviceName, String subServiceName, TransferDirection direction) {
        List<SchemaUsageMapping> eotMappings = schemaUsageMappingRepository.findEotCountValidationMappings(tenantId);
        
        return eotMappings.stream()
            .filter(mapping -> mapping.getSubServiceConfiguration().getServiceName().equals(serviceName))
            .filter(mapping -> mapping.getSubServiceConfiguration().getSubServiceName().equals(subServiceName))
            .filter(mapping -> mapping.getDirection() == direction)
            .filter(mapping -> mapping.getSharedSchema().getSupportsCountValidation())
            .findFirst();
    }
    
    /**
     * Get or create daily file count tracker
     */
    private DailyFileCountTracker getOrCreateTracker(String tenantId, String serviceName, String subServiceName, 
                                                    LocalDate processingDate, FileType fileType, TransferDirection direction) {
        
        Optional<DailyFileCountTracker> existingTracker = dailyFileCountRepository.findBySubServiceAndDate(
            tenantId, serviceName, subServiceName, processingDate, fileType, direction);
        
        if (existingTracker.isPresent()) {
            return existingTracker.get();
        } else {
            return new DailyFileCountTracker(tenantId, serviceName, subServiceName, processingDate, fileType, direction);
        }
    }
    
    /**
     * Alert about count discrepancy
     */
    private void alertDiscrepancy(DailyFileCountTracker tracker) {
        logger.warn("COUNT DISCREPANCY: {}/{} on {} - EOT says {}, actual {}, difference: {}", 
                   tracker.getServiceName(), tracker.getSubServiceName(), tracker.getProcessingDate(),
                   tracker.getEotCountValue(), tracker.getActualCount(), tracker.getDiscrepancyCount());
        
        // TODO: Send alert notification to monitoring system or email
        // alertService.sendDiscrepancyAlert(tracker);
    }
    
    /**
     * Get validation results for a date range
     */
    public List<DailyFileCountTracker> getValidationResults(String tenantId, LocalDate startDate, LocalDate endDate) {
        return dailyFileCountRepository.findByDateRange(tenantId, startDate, endDate);
    }
    
    /**
     * Get pending validations (missing EOT files)
     */
    public List<DailyFileCountTracker> getPendingValidations(String tenantId, LocalDate cutoffDate) {
        return dailyFileCountRepository.findPendingValidation(tenantId, cutoffDate);
    }
    
    /**
     * Get discrepancies for a tenant
     */
    public List<DailyFileCountTracker> getDiscrepancies(String tenantId) {
        return dailyFileCountRepository.findDiscrepanciesByTenant(tenantId);
    }
    
    /**
     * Get validation statistics
     */
    public ValidationStatistics getValidationStatistics(String tenantId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> stats = dailyFileCountRepository.getValidationStatistics(tenantId, startDate, endDate);
        
        ValidationStatistics result = new ValidationStatistics();
        for (Object[] stat : stats) {
            DailyFileCountTracker.ValidationStatus status = (DailyFileCountTracker.ValidationStatus) stat[0];
            Long count = ((Number) stat[1]).longValue();
            
            switch (status) {
                case MATCHED:
                    result.setMatchedCount(count);
                    break;
                case DISCREPANCY:
                    result.setDiscrepancyCount(count);
                    break;
                case PENDING:
                    result.setPendingCount(count);
                    break;
                case MISSING_EOT:
                    result.setMissingEotCount(count);
                    break;
                case MISSING_SOT:
                    result.setMissingSotCount(count);
                    break;
                case ERROR:
                    result.setErrorCount(count);
                    break;
            }
        }
        
        return result;
    }
    
    /**
     * Statistics class for validation results
     */
    public static class ValidationStatistics {
        private Long matchedCount = 0L;
        private Long discrepancyCount = 0L;
        private Long pendingCount = 0L;
        private Long missingEotCount = 0L;
        private Long missingSotCount = 0L;
        private Long errorCount = 0L;
        
        // Getters and setters
        public Long getMatchedCount() { return matchedCount; }
        public void setMatchedCount(Long matchedCount) { this.matchedCount = matchedCount; }
        
        public Long getDiscrepancyCount() { return discrepancyCount; }
        public void setDiscrepancyCount(Long discrepancyCount) { this.discrepancyCount = discrepancyCount; }
        
        public Long getPendingCount() { return pendingCount; }
        public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }
        
        public Long getMissingEotCount() { return missingEotCount; }
        public void setMissingEotCount(Long missingEotCount) { this.missingEotCount = missingEotCount; }
        
        public Long getMissingSotCount() { return missingSotCount; }
        public void setMissingSotCount(Long missingSotCount) { this.missingSotCount = missingSotCount; }
        
        public Long getErrorCount() { return errorCount; }
        public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }
        
        public Long getTotalCount() {
            return matchedCount + discrepancyCount + pendingCount + missingEotCount + missingSotCount + errorCount;
        }
        
        public double getMatchPercentage() {
            Long total = getTotalCount();
            return total > 0 ? (matchedCount.doubleValue() / total.doubleValue()) * 100.0 : 0.0;
        }
    }
}