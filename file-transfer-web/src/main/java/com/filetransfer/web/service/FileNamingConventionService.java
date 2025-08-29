package com.filetransfer.web.service;

import com.filetransfer.web.entity.SubServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for validating and managing file naming conventions,
 * particularly for Start-of-Day (SOT) and End-of-Day (EOT) files.
 */
@Service
public class FileNamingConventionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileNamingConventionService.class);
    
    // Common date patterns
    private static final Pattern DATE_PATTERN_YYYYMMDD = Pattern.compile("\\d{8}");
    private static final Pattern DATE_PATTERN_YYMMDD = Pattern.compile("\\d{6}");
    private static final Pattern DATE_PATTERN_YYYY_MM_DD = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATE_PATTERN_DD_MM_YYYY = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
    
    // Timestamp patterns
    private static final Pattern TIMESTAMP_PATTERN_HHMMSS = Pattern.compile("\\d{6}");
    private static final Pattern TIMESTAMP_PATTERN_HHMM = Pattern.compile("\\d{4}");
    private static final Pattern TIMESTAMP_PATTERN_HH_MM_SS = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
    
    /**
     * Validate file name against subservice naming conventions
     */
    public NamingValidationResult validateFileName(String fileName, SubServiceConfiguration config, FileNameType fileType) {
        NamingValidationResult result = new NamingValidationResult();
        result.setFileName(fileName);
        result.setFileType(fileType);
        
        try {
            switch (fileType) {
                case SOT:
                    validateSotFileName(fileName, config, result);
                    break;
                case EOT:
                    validateEotFileName(fileName, config, result);
                    break;
                case DATA:
                    validateDataFileName(fileName, config, result);
                    break;
                default:
                    result.setValid(false);
                    result.addError("Unknown file type for validation: " + fileType);
            }
            
        } catch (Exception e) {
            logger.error("Error validating file name {}: {}", fileName, e.getMessage(), e);
            result.setValid(false);
            result.addError("Validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate Start-of-Day (SOT) file name
     */
    private void validateSotFileName(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        String sotPattern = config.getSotFilePattern();
        if (sotPattern == null || sotPattern.isEmpty()) {
            sotPattern = "SOT_*"; // Default pattern
        }
        
        // Check prefix
        String expectedPrefix = config.getStartMarkerPrefix();
        if (!fileName.startsWith(expectedPrefix)) {
            result.addError(String.format("SOT file must start with prefix '%s'", expectedPrefix));
        }
        
        // Validate against pattern
        if (!matchesPattern(fileName, sotPattern)) {
            result.addError(String.format("SOT file name does not match pattern '%s'", sotPattern));
        }
        
        // Extract and validate date
        LocalDate extractedDate = extractDateFromFileName(fileName);
        if (extractedDate == null) {
            result.addWarning("Could not extract date from SOT file name");
        } else {
            result.setExtractedDate(extractedDate);
            validateDateReasonableness(extractedDate, result);
        }
        
        // SOT-specific validations
        validateSotSpecificRules(fileName, config, result);
    }
    
    /**
     * Validate End-of-Day (EOT) file name
     */
    private void validateEotFileName(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        String eotPattern = config.getEotFilePattern();
        if (eotPattern == null || eotPattern.isEmpty()) {
            eotPattern = "EOT_*"; // Default pattern
        }
        
        // Check prefix
        String expectedPrefix = config.getEndMarkerPrefix();
        if (!fileName.startsWith(expectedPrefix)) {
            result.addError(String.format("EOT file must start with prefix '%s'", expectedPrefix));
        }
        
        // Validate against pattern
        if (!matchesPattern(fileName, eotPattern)) {
            result.addError(String.format("EOT file name does not match pattern '%s'", eotPattern));
        }
        
        // Extract and validate date
        LocalDate extractedDate = extractDateFromFileName(fileName);
        if (extractedDate == null) {
            result.addWarning("Could not extract date from EOT file name");
        } else {
            result.setExtractedDate(extractedDate);
            validateDateReasonableness(extractedDate, result);
        }
        
        // EOT-specific validations
        validateEotSpecificRules(fileName, config, result);
    }
    
    /**
     * Validate data file name
     */
    private void validateDataFileName(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        String dataPattern = config.getDataFilePattern();
        if (dataPattern == null || dataPattern.isEmpty()) {
            dataPattern = "*.*"; // Default pattern
        }
        
        // Validate against pattern
        if (!matchesPattern(fileName, dataPattern)) {
            result.addError(String.format("Data file name does not match pattern '%s'", dataPattern));
        }
        
        // Extract and validate date if present
        LocalDate extractedDate = extractDateFromFileName(fileName);
        if (extractedDate != null) {
            result.setExtractedDate(extractedDate);
            validateDateReasonableness(extractedDate, result);
        }
        
        // Data file specific validations
        validateDataFileSpecificRules(fileName, config, result);
    }
    
    /**
     * Check if file name matches pattern (supports wildcards)
     */
    private boolean matchesPattern(String fileName, String pattern) {
        // Convert wildcard pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .replace("[", "\\[")
            .replace("]", "\\]");
        
        return fileName.matches(regex);
    }
    
    /**
     * Extract date from file name using various patterns
     */
    private LocalDate extractDateFromFileName(String fileName) {
        // Try different date patterns
        Matcher matcher;
        
        // YYYYMMDD pattern
        matcher = DATE_PATTERN_YYYYMMDD.matcher(fileName);
        if (matcher.find()) {
            try {
                String dateStr = matcher.group();
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                logger.debug("Failed to parse date {} as yyyyMMdd", matcher.group());
            }
        }
        
        // YYMMDD pattern
        matcher = DATE_PATTERN_YYMMDD.matcher(fileName);
        if (matcher.find()) {
            try {
                String dateStr = matcher.group();
                int year = Integer.parseInt(dateStr.substring(0, 2));
                // Assume 20xx for years 00-50, 19xx for years 51-99
                year += (year <= 50) ? 2000 : 1900;
                String fullDateStr = year + dateStr.substring(2);
                return LocalDate.parse(fullDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                logger.debug("Failed to parse date {} as yyMMdd", matcher.group());
            }
        }
        
        // YYYY-MM-DD pattern
        matcher = DATE_PATTERN_YYYY_MM_DD.matcher(fileName);
        if (matcher.find()) {
            try {
                return LocalDate.parse(matcher.group(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                logger.debug("Failed to parse date {} as yyyy-MM-dd", matcher.group());
            }
        }
        
        // DD-MM-YYYY pattern
        matcher = DATE_PATTERN_DD_MM_YYYY.matcher(fileName);
        if (matcher.find()) {
            try {
                return LocalDate.parse(matcher.group(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception e) {
                logger.debug("Failed to parse date {} as dd-MM-yyyy", matcher.group());
            }
        }
        
        return null;
    }
    
    /**
     * Validate if extracted date is reasonable
     */
    private void validateDateReasonableness(LocalDate date, NamingValidationResult result) {
        LocalDate today = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);
        LocalDate oneYearAhead = today.plusYears(1);
        
        if (date.isBefore(oneYearAgo)) {
            result.addWarning(String.format("File date %s is more than a year old", date));
        } else if (date.isAfter(oneYearAhead)) {
            result.addWarning(String.format("File date %s is more than a year in the future", date));
        }
        
        if (date.isAfter(today)) {
            result.addWarning(String.format("File date %s is in the future", date));
        }
    }
    
    /**
     * SOT-specific validation rules
     */
    private void validateSotSpecificRules(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        // SOT files should typically be the first files of the day
        // Check for sequence numbers or timestamps if required
        
        // Example: SOT files should not have data sequence numbers
        if (fileName.matches(".*_\\d{3,}_.*")) {
            result.addWarning("SOT file contains sequence numbers, which is unusual");
        }
        
        // Check for proper extension
        if (!fileName.toLowerCase().endsWith(".txt") && !fileName.toLowerCase().endsWith(".dat")) {
            result.addWarning("SOT file should typically have .txt or .dat extension");
        }
        
        // Validate timestamp if present
        validateTimestampInFileName(fileName, "SOT", result);
    }
    
    /**
     * EOT-specific validation rules
     */
    private void validateEotSpecificRules(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        // EOT files should typically be the last files of the day
        
        // Check for proper extension
        if (!fileName.toLowerCase().endsWith(".txt") && !fileName.toLowerCase().endsWith(".dat")) {
            result.addWarning("EOT file should typically have .txt or .dat extension");
        }
        
        // EOT files might contain summary information in the name
        if (fileName.toLowerCase().contains("summary") || fileName.toLowerCase().contains("total")) {
            result.addInfo("EOT file appears to contain summary information");
        }
        
        // Validate timestamp if present
        validateTimestampInFileName(fileName, "EOT", result);
    }
    
    /**
     * Data file specific validation rules
     */
    private void validateDataFileSpecificRules(String fileName, SubServiceConfiguration config, NamingValidationResult result) {
        // Data files often have sequence numbers
        Pattern sequencePattern = Pattern.compile(".*_(\\d+)_.*|.*\\.(\\d+)\\.");
        Matcher matcher = sequencePattern.matcher(fileName);
        if (matcher.find()) {
            String sequence = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            result.addInfo("Data file contains sequence number: " + sequence);
            
            // Validate sequence number reasonableness
            try {
                int seqNum = Integer.parseInt(sequence);
                if (seqNum > 9999) {
                    result.addWarning("Sequence number " + seqNum + " is unusually high");
                }
            } catch (NumberFormatException e) {
                // Ignore if not a number
            }
        }
        
        // Check for proper extension based on file type
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".xml")) {
            result.addInfo("XML data file detected");
        } else if (lowerFileName.endsWith(".json")) {
            result.addInfo("JSON data file detected");
        } else if (lowerFileName.endsWith(".csv")) {
            result.addInfo("CSV data file detected");
        } else if (lowerFileName.endsWith(".dat") || lowerFileName.endsWith(".txt")) {
            result.addInfo("Fixed-width/COBOL data file detected");
        }
    }
    
    /**
     * Validate timestamp in file name
     */
    private void validateTimestampInFileName(String fileName, String fileType, NamingValidationResult result) {
        // Look for timestamp patterns
        Matcher matcher;
        
        // HHMMSS pattern
        matcher = TIMESTAMP_PATTERN_HHMMSS.matcher(fileName);
        if (matcher.find()) {
            String timeStr = matcher.group();
            try {
                int hours = Integer.parseInt(timeStr.substring(0, 2));
                int minutes = Integer.parseInt(timeStr.substring(2, 4));
                int seconds = Integer.parseInt(timeStr.substring(4, 6));
                
                if (hours > 23 || minutes > 59 || seconds > 59) {
                    result.addError("Invalid timestamp in file name: " + timeStr);
                } else {
                    result.addInfo(String.format("%s file timestamp: %02d:%02d:%02d", fileType, hours, minutes, seconds));
                }
            } catch (Exception e) {
                result.addWarning("Could not parse timestamp: " + timeStr);
            }
        }
        
        // HH:MM:SS pattern
        matcher = TIMESTAMP_PATTERN_HH_MM_SS.matcher(fileName);
        if (matcher.find()) {
            result.addInfo(fileType + " file contains formatted timestamp: " + matcher.group());
        }
    }
    
    /**
     * Generate suggested file names based on conventions
     */
    public List<String> generateSuggestedFileNames(SubServiceConfiguration config, FileNameType fileType, LocalDate date) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String shortDateStr = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
            
            switch (fileType) {
                case SOT:
                    suggestions.add(config.getStartMarkerPrefix() + dateStr + ".dat");
                    suggestions.add(config.getStartMarkerPrefix() + shortDateStr + ".txt");
                    suggestions.add(config.getStartMarkerPrefix() + config.getServiceName() + "_" + dateStr + ".dat");
                    break;
                    
                case EOT:
                    suggestions.add(config.getEndMarkerPrefix() + dateStr + ".dat");
                    suggestions.add(config.getEndMarkerPrefix() + shortDateStr + ".txt");
                    suggestions.add(config.getEndMarkerPrefix() + config.getServiceName() + "_" + dateStr + ".dat");
                    break;
                    
                case DATA:
                    suggestions.add(config.getServiceName() + "_" + dateStr + "_001.dat");
                    suggestions.add("DATA_" + config.getSubServiceName() + "_" + dateStr + ".txt");
                    suggestions.add(config.getServiceName().toLowerCase() + dateStr + ".csv");
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Error generating file name suggestions: {}", e.getMessage());
        }
        
        return suggestions;
    }
    
    // Enums and Data Classes
    
    public enum FileNameType {
        SOT,    // Start of Day
        EOT,    // End of Day
        DATA    // Data file
    }
    
    public static class NamingValidationResult {
        private String fileName;
        private FileNameType fileType;
        private boolean valid = true;
        private LocalDate extractedDate;
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
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public FileNameType getFileType() { return fileType; }
        public void setFileType(FileNameType fileType) { this.fileType = fileType; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public LocalDate getExtractedDate() { return extractedDate; }
        public void setExtractedDate(LocalDate extractedDate) { this.extractedDate = extractedDate; }
        
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfo() { return info; }
        
        public boolean hasIssues() {
            return !errors.isEmpty() || !warnings.isEmpty();
        }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Errors: ").append(String.join("; ", errors)).append(" ");
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings: ").append(String.join("; ", warnings)).append(" ");
            }
            if (!info.isEmpty()) {
                sb.append("Info: ").append(String.join("; ", info));
            }
            return sb.toString().trim();
        }
    }
}