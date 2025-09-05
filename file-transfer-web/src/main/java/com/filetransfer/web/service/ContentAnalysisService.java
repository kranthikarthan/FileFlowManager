package com.filetransfer.web.service;

import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.FileTransferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for intelligent file content analysis and classification
 */
@Service
public class ContentAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentAnalysisService.class);
    
    // File content patterns for type detection
    private static final Pattern CSV_PATTERN = Pattern.compile("^[^,]*,[^,]*,.*$", Pattern.MULTILINE);
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*[\\{\\[].*[\\}\\]]\\s*$", Pattern.DOTALL);
    private static final Pattern XML_PATTERN = Pattern.compile("^\\s*<\\?xml.*\\?>.*$|^\\s*<[^>]+>.*</[^>]+>\\s*$", Pattern.DOTALL);
    private static final Pattern YAML_PATTERN = Pattern.compile("^\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*:.*$", Pattern.MULTILINE);
    private static final Pattern FIXED_WIDTH_PATTERN = Pattern.compile("^.{10,}$", Pattern.MULTILINE);
    private static final Pattern DELIMITED_PATTERN = Pattern.compile("^[^|]*\\|[^|]*\\|.*$", Pattern.MULTILINE);
    
    // Binary file signatures (magic numbers)
    private static final Map<String, byte[]> BINARY_SIGNATURES = Map.of(
        "PDF", new byte[]{0x25, 0x50, 0x44, 0x46}, // %PDF
        "ZIP", new byte[]{0x50, 0x4B, 0x03, 0x04}, // PK..
        "GZIP", new byte[]{0x1F, (byte) 0x8B}, // ..
        "JPEG", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // ...
        "PNG", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, // .PNG
        "EXE", new byte[]{0x4D, 0x5A} // MZ
    );
    
    /**
     * Analyze file content and return comprehensive analysis results
     */
    public ContentAnalysisResult analyzeFileContent(String filePath, String fileName) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new ContentAnalysisResult(false, "File not found: " + filePath);
            }
            
            long fileSize = Files.size(path);
            String detectedEncoding = detectEncoding(path);
            FileType detectedType = detectFileType(path, fileName);
            boolean isBinary = isBinaryFile(path);
            
            ContentAnalysisResult result = new ContentAnalysisResult(true, "Analysis completed successfully");
            result.setFileSize(fileSize);
            result.setDetectedEncoding(detectedEncoding);
            result.setDetectedFileType(detectedType);
            result.setBinaryFile(isBinary);
            result.setFileName(fileName);
            result.setFilePath(filePath);
            
            if (!isBinary && fileSize < 50 * 1024 * 1024) { // Only analyze text files under 50MB
                analyzeTextContent(path, result);
            } else if (isBinary) {
                analyzeBinaryContent(path, result);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing file content for {}: {}", filePath, e.getMessage(), e);
            return new ContentAnalysisResult(false, "Analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Detect file encoding
     */
    private String detectEncoding(Path filePath) throws IOException {
        byte[] buffer = new byte[4096];
        try (InputStream is = Files.newInputStream(filePath)) {
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) return "UNKNOWN";
            
            // Check for BOM markers
            if (bytesRead >= 3 && buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB && buffer[2] == (byte) 0xBF) {
                return "UTF-8";
            }
            if (bytesRead >= 2 && buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) {
                return "UTF-16LE";
            }
            if (bytesRead >= 2 && buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }
            
            // Simple heuristic for UTF-8 vs ASCII
            boolean hasHighBits = false;
            for (int i = 0; i < bytesRead; i++) {
                if ((buffer[i] & 0x80) != 0) {
                    hasHighBits = true;
                    break;
                }
            }
            
            return hasHighBits ? "UTF-8" : "ASCII";
        }
    }
    
    /**
     * Detect file type based on content analysis
     */
    private FileType detectFileType(Path filePath, String fileName) throws IOException {
        // First check if it's binary
        if (isBinaryFile(filePath)) {
            return FileType.BINARY_FILE;
        }
        
        // Read first few lines for pattern matching
        List<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)
                .limit(10)
                .collect(Collectors.toList());
        
        if (lines.isEmpty()) {
            return FileType.COBOL_FLAT_FILE;
        }
        
        String content = String.join("\n", lines);
        
        // JSON detection
        if (JSON_PATTERN.matcher(content).matches()) {
            return FileType.JSON;
        }
        
        // XML detection
        if (XML_PATTERN.matcher(content).find()) {
            return FileType.XML;
        }
        
        // CSV detection (check for comma-separated values)
        if (CSV_PATTERN.matcher(content).find()) {
            return FileType.CSV;
        }
        
        // Delimited detection (pipe-separated)
        if (DELIMITED_PATTERN.matcher(content).find()) {
            return FileType.DELIMITED;
        }
        
        // Fixed width detection (consistent line lengths)
        if (lines.size() > 2) {
            int firstLineLength = lines.get(0).length();
            boolean isFixedWidth = lines.stream()
                    .skip(1)
                    .limit(5)
                    .allMatch(line -> Math.abs(line.length() - firstLineLength) <= 2);
            
            if (isFixedWidth && firstLineLength > 20) {
                return FileType.FIXED_WIDTH;
            }
        }
        
        // Default to COBOL flat file
        return FileType.COBOL_FLAT_FILE;
    }
    
    /**
     * Check if file is binary based on content
     */
    private boolean isBinaryFile(Path filePath) throws IOException {
        byte[] buffer = new byte[8192];
        try (InputStream is = Files.newInputStream(filePath)) {
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) return false;
            
            // Check for binary signatures
            for (Map.Entry<String, byte[]> entry : BINARY_SIGNATURES.entrySet()) {
                if (bytesRead >= entry.getValue().length) {
                    boolean matches = true;
                    for (int i = 0; i < entry.getValue().length; i++) {
                        if (buffer[i] != entry.getValue()[i]) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) return true;
                }
            }
            
            // Check for null bytes (strong indicator of binary content)
            int nullBytes = 0;
            int totalBytes = Math.min(bytesRead, 1024); // Check first 1KB
            
            for (int i = 0; i < totalBytes; i++) {
                if (buffer[i] == 0) {
                    nullBytes++;
                }
            }
            
            // If more than 1% null bytes, consider it binary
            return (double) nullBytes / totalBytes > 0.01;
        }
    }
    
    /**
     * Analyze text file content for detailed insights
     */
    private void analyzeTextContent(Path filePath, ContentAnalysisResult result) throws IOException {
        List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        
        result.setLineCount(allLines.size());
        
        if (allLines.isEmpty()) {
            result.setEmpty(true);
            return;
        }
        
        // Basic text statistics
        int totalCharacters = allLines.stream().mapToInt(String::length).sum();
        double avgLineLength = (double) totalCharacters / allLines.size();
        int maxLineLength = allLines.stream().mapToInt(String::length).max().orElse(0);
        int minLineLength = allLines.stream().mapToInt(String::length).min().orElse(0);
        
        result.setTotalCharacters(totalCharacters);
        result.setAverageLineLength(avgLineLength);
        result.setMaxLineLength(maxLineLength);
        result.setMinLineLength(minLineLength);
        
        // Analyze structure based on detected file type
        if (result.getDetectedFileType() == FileType.CSV) {
            analyzeCsvContent(allLines, result);
        } else if (result.getDetectedFileType() == FileType.JSON) {
            analyzeJsonContent(allLines, result);
        } else if (result.getDetectedFileType() == FileType.XML) {
            analyzeXmlContent(allLines, result);
        } else if (result.getDetectedFileType() == FileType.DELIMITED) {
            analyzeDelimitedContent(allLines, result);
        } else if (result.getDetectedFileType() == FileType.FIXED_WIDTH) {
            analyzeFixedWidthContent(allLines, result);
        }
        
        // Content quality assessment
        assessContentQuality(allLines, result);
    }
    
    /**
     * Analyze CSV file structure
     */
    private void analyzeCsvContent(List<String> lines, ContentAnalysisResult result) {
        if (lines.isEmpty()) return;
        
        String headerLine = lines.get(0);
        String[] headers = headerLine.split(",");
        
        Map<String, Object> csvAnalysis = new HashMap<>();
        csvAnalysis.put("columnCount", headers.length);
        csvAnalysis.put("hasHeader", true);
        csvAnalysis.put("headers", Arrays.asList(headers));
        csvAnalysis.put("dataRows", lines.size() - 1);
        
        // Analyze data consistency
        if (lines.size() > 1) {
            boolean consistentColumns = lines.stream()
                    .skip(1)
                    .allMatch(line -> line.split(",").length == headers.length);
            csvAnalysis.put("consistentColumns", consistentColumns);
            
            // Sample data types from first few rows
            if (lines.size() > 2) {
                List<String> sampleDataTypes = detectColumnDataTypes(lines.subList(1, Math.min(6, lines.size())), headers.length);
                csvAnalysis.put("columnDataTypes", sampleDataTypes);
            }
        }
        
        result.setStructureAnalysis(csvAnalysis);
    }
    
    /**
     * Analyze JSON file structure
     */
    private void analyzeJsonContent(List<String> lines, ContentAnalysisResult result) {
        String content = String.join("\n", lines);
        
        Map<String, Object> jsonAnalysis = new HashMap<>();
        
        try {
            // Basic JSON validation
            if (content.trim().startsWith("{")) {
                jsonAnalysis.put("type", "object");
                jsonAnalysis.put("structure", "single_object");
            } else if (content.trim().startsWith("[")) {
                jsonAnalysis.put("type", "array");
                // Count objects in array by counting opening braces
                long objectCount = content.chars().filter(ch -> ch == '{').count();
                jsonAnalysis.put("estimatedObjects", objectCount);
                jsonAnalysis.put("structure", "array_of_objects");
            }
            
            // Estimate nesting depth
            int maxNesting = 0;
            int currentNesting = 0;
            for (char c : content.toCharArray()) {
                if (c == '{' || c == '[') {
                    currentNesting++;
                    maxNesting = Math.max(maxNesting, currentNesting);
                } else if (c == '}' || c == ']') {
                    currentNesting--;
                }
            }
            jsonAnalysis.put("maxNestingDepth", maxNesting);
            
            // Basic validation
            boolean wellFormed = isWellFormedJson(content);
            jsonAnalysis.put("wellFormed", wellFormed);
            
        } catch (Exception e) {
            jsonAnalysis.put("error", "JSON analysis failed: " + e.getMessage());
        }
        
        result.setStructureAnalysis(jsonAnalysis);
    }
    
    /**
     * Analyze XML file structure
     */
    private void analyzeXmlContent(List<String> lines, ContentAnalysisResult result) {
        String content = String.join("\n", lines);
        
        Map<String, Object> xmlAnalysis = new HashMap<>();
        
        try {
            // Check for XML declaration
            boolean hasXmlDeclaration = content.trim().startsWith("<?xml");
            xmlAnalysis.put("hasXmlDeclaration", hasXmlDeclaration);
            
            // Count elements
            long elementCount = content.chars().filter(ch -> ch == '<').count() / 2; // Rough estimate
            xmlAnalysis.put("estimatedElements", elementCount);
            
            // Check for namespaces
            boolean hasNamespaces = content.contains("xmlns:");
            xmlAnalysis.put("hasNamespaces", hasNamespaces);
            
            // Estimate nesting depth
            int maxNesting = 0;
            int currentNesting = 0;
            boolean inTag = false;
            boolean isClosingTag = false;
            
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '<') {
                    inTag = true;
                    isClosingTag = i + 1 < content.length() && content.charAt(i + 1) == '/';
                } else if (c == '>') {
                    if (inTag) {
                        if (isClosingTag) {
                            currentNesting--;
                        } else if (i > 0 && content.charAt(i - 1) != '/') {
                            currentNesting++;
                            maxNesting = Math.max(maxNesting, currentNesting);
                        }
                    }
                    inTag = false;
                    isClosingTag = false;
                }
            }
            xmlAnalysis.put("maxNestingDepth", maxNesting);
            
        } catch (Exception e) {
            xmlAnalysis.put("error", "XML analysis failed: " + e.getMessage());
        }
        
        result.setStructureAnalysis(xmlAnalysis);
    }
    
    /**
     * Analyze delimited file content
     */
    private void analyzeDelimitedContent(List<String> lines, ContentAnalysisResult result) {
        if (lines.isEmpty()) return;
        
        Map<String, Object> delimitedAnalysis = new HashMap<>();
        
        // Detect delimiter
        String firstLine = lines.get(0);
        String delimiter = detectDelimiter(firstLine);
        delimitedAnalysis.put("delimiter", delimiter);
        
        if (delimiter != null) {
            String[] headers = firstLine.split(Pattern.quote(delimiter));
            delimitedAnalysis.put("columnCount", headers.length);
            delimitedAnalysis.put("hasHeader", true);
            delimitedAnalysis.put("headers", Arrays.asList(headers));
            delimitedAnalysis.put("dataRows", lines.size() - 1);
            
            // Check consistency
            boolean consistentColumns = lines.stream()
                    .skip(1)
                    .allMatch(line -> line.split(Pattern.quote(delimiter)).length == headers.length);
            delimitedAnalysis.put("consistentColumns", consistentColumns);
        }
        
        result.setStructureAnalysis(delimitedAnalysis);
    }
    
    /**
     * Analyze fixed-width file content
     */
    private void analyzeFixedWidthContent(List<String> lines, ContentAnalysisResult result) {
        if (lines.isEmpty()) return;
        
        Map<String, Object> fixedWidthAnalysis = new HashMap<>();
        
        int recordLength = lines.get(0).length();
        fixedWidthAnalysis.put("recordLength", recordLength);
        fixedWidthAnalysis.put("totalRecords", lines.size());
        
        // Check for consistent record length
        boolean consistentLength = lines.stream()
                .allMatch(line -> line.length() == recordLength);
        fixedWidthAnalysis.put("consistentLength", consistentLength);
        
        // Analyze field patterns (detect potential field boundaries)
        if (recordLength > 0) {
            List<Integer> potentialBoundaries = detectFieldBoundaries(lines, recordLength);
            fixedWidthAnalysis.put("potentialFieldBoundaries", potentialBoundaries);
            fixedWidthAnalysis.put("estimatedFields", potentialBoundaries.size());
        }
        
        result.setStructureAnalysis(fixedWidthAnalysis);
    }
    
    /**
     * Analyze binary file content
     */
    private void analyzeBinaryContent(Path filePath, ContentAnalysisResult result) throws IOException {
        Map<String, Object> binaryAnalysis = new HashMap<>();
        
        byte[] header = new byte[64];
        try (InputStream is = Files.newInputStream(filePath)) {
            int bytesRead = is.read(header);
            
            // Detect binary file type
            String detectedBinaryType = detectBinaryType(header, bytesRead);
            binaryAnalysis.put("binaryType", detectedBinaryType);
            
            // Calculate entropy (measure of randomness)
            double entropy = calculateEntropy(header, bytesRead);
            binaryAnalysis.put("entropy", entropy);
            
            // Check if file might be compressed
            boolean likelyCompressed = entropy > 7.5; // High entropy suggests compression
            binaryAnalysis.put("likelyCompressed", likelyCompressed);
        }
        
        result.setStructureAnalysis(binaryAnalysis);
    }
    
    /**
     * Assess overall content quality
     */
    private void assessContentQuality(List<String> lines, ContentAnalysisResult result) {
        ContentQualityAssessment quality = new ContentQualityAssessment();
        
        // Basic quality metrics
        int emptyLines = 0;
        int linesWithSpecialChars = 0;
        int totalLength = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                emptyLines++;
            }
            if (line.matches(".*[^\\p{Print}\\p{Space}].*")) {
                linesWithSpecialChars++;
            }
            totalLength += line.length();
        }
        
        quality.setEmptyLinePercentage((double) emptyLines / lines.size() * 100);
        quality.setSpecialCharPercentage((double) linesWithSpecialChars / lines.size() * 100);
        quality.setAverageLineLength((double) totalLength / lines.size());
        
        // Quality score calculation
        double qualityScore = calculateQualityScore(quality, result);
        quality.setQualityScore(qualityScore);
        
        // Quality recommendations
        List<String> recommendations = generateQualityRecommendations(quality, result);
        quality.setRecommendations(recommendations);
        
        result.setQualityAssessment(quality);
    }
    
    /**
     * Detect delimiter in delimited files
     */
    private String detectDelimiter(String line) {
        String[] possibleDelimiters = {"|", "\t", ";", ":", "~"};
        
        for (String delimiter : possibleDelimiters) {
            if (line.contains(delimiter)) {
                return delimiter;
            }
        }
        
        return null;
    }
    
    /**
     * Detect column data types from sample data
     */
    private List<String> detectColumnDataTypes(List<String> dataLines, int columnCount) {
        List<String> dataTypes = new ArrayList<>();
        
        for (int col = 0; col < columnCount; col++) {
            String detectedType = "STRING"; // Default
            
            // Collect values for this column
            List<String> columnValues = new ArrayList<>();
            for (String line : dataLines) {
                String[] values = line.split(",");
                if (values.length > col) {
                    columnValues.add(values[col].trim());
                }
            }
            
            // Analyze column values
            if (columnValues.stream().allMatch(this::isInteger)) {
                detectedType = "INTEGER";
            } else if (columnValues.stream().allMatch(this::isDecimal)) {
                detectedType = "DECIMAL";
            } else if (columnValues.stream().allMatch(this::isDate)) {
                detectedType = "DATE";
            } else if (columnValues.stream().allMatch(this::isBoolean)) {
                detectedType = "BOOLEAN";
            }
            
            dataTypes.add(detectedType);
        }
        
        return dataTypes;
    }
    
    /**
     * Detect potential field boundaries in fixed-width files
     */
    private List<Integer> detectFieldBoundaries(List<String> lines, int recordLength) {
        List<Integer> boundaries = new ArrayList<>();
        
        // Look for columns of spaces or consistent patterns
        for (int pos = 1; pos < recordLength - 1; pos++) {
            boolean isPotentialBoundary = true;
            
            // Check if this position consistently has spaces or delimiters
            for (String line : lines.subList(0, Math.min(10, lines.size()))) {
                if (pos < line.length()) {
                    char c = line.charAt(pos);
                    if (c != ' ' && c != '\t') {
                        isPotentialBoundary = false;
                        break;
                    }
                }
            }
            
            if (isPotentialBoundary) {
                boundaries.add(pos);
            }
        }
        
        return boundaries;
    }
    
    /**
     * Detect binary file type from header bytes
     */
    private String detectBinaryType(byte[] header, int length) {
        for (Map.Entry<String, byte[]> entry : BINARY_SIGNATURES.entrySet()) {
            if (length >= entry.getValue().length) {
                boolean matches = true;
                for (int i = 0; i < entry.getValue().length; i++) {
                    if (header[i] != entry.getValue()[i]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return entry.getKey();
                }
            }
        }
        
        return "UNKNOWN_BINARY";
    }
    
    /**
     * Calculate file entropy (measure of randomness/compression)
     */
    private double calculateEntropy(byte[] data, int length) {
        int[] frequency = new int[256];
        
        // Count byte frequencies
        for (int i = 0; i < length; i++) {
            frequency[data[i] & 0xFF]++;
        }
        
        // Calculate entropy
        double entropy = 0.0;
        for (int freq : frequency) {
            if (freq > 0) {
                double probability = (double) freq / length;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }
        
        return entropy;
    }
    
    /**
     * Check if file is well-formed JSON
     */
    private boolean isWellFormedJson(String content) {
        try {
            // Simple bracket/brace matching
            int braces = 0;
            int brackets = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (char c : content.toCharArray()) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                if (c == '\\' && inString) {
                    escaped = true;
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    if (c == '{') braces++;
                    else if (c == '}') braces--;
                    else if (c == '[') brackets++;
                    else if (c == ']') brackets--;
                }
            }
            
            return braces == 0 && brackets == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculate content quality score
     */
    private double calculateQualityScore(ContentQualityAssessment quality, ContentAnalysisResult result) {
        double score = 100.0;
        
        // Deduct points for quality issues
        score -= quality.getEmptyLinePercentage() * 0.5; // Small penalty for empty lines
        score -= quality.getSpecialCharPercentage() * 2.0; // Larger penalty for special chars
        
        // Bonus points for good structure
        if (result.getStructureAnalysis() != null) {
            Map<String, Object> structure = result.getStructureAnalysis();
            
            if (Boolean.TRUE.equals(structure.get("consistentColumns"))) {
                score += 10.0;
            }
            if (Boolean.TRUE.equals(structure.get("wellFormed"))) {
                score += 10.0;
            }
        }
        
        // Ensure score is between 0 and 100
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Generate quality improvement recommendations
     */
    private List<String> generateQualityRecommendations(ContentQualityAssessment quality, ContentAnalysisResult result) {
        List<String> recommendations = new ArrayList<>();
        
        if (quality.getEmptyLinePercentage() > 10) {
            recommendations.add("Consider removing excessive empty lines to improve processing efficiency");
        }
        
        if (quality.getSpecialCharPercentage() > 5) {
            recommendations.add("File contains special characters that may cause encoding issues");
        }
        
        if (result.getStructureAnalysis() != null) {
            Map<String, Object> structure = result.getStructureAnalysis();
            
            if (Boolean.FALSE.equals(structure.get("consistentColumns"))) {
                recommendations.add("Inconsistent column count detected - verify data integrity");
            }
            
            if (Boolean.FALSE.equals(structure.get("wellFormed"))) {
                recommendations.add("File structure validation failed - check file format compliance");
            }
        }
        
        if (quality.getQualityScore() < 70) {
            recommendations.add("Overall file quality is below recommended threshold - manual review suggested");
        }
        
        return recommendations;
    }
    
    // Utility methods for data type detection
    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isDecimal(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isDate(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}") || 
               value.matches("\\d{2}/\\d{2}/\\d{4}") ||
               value.matches("\\d{2}-\\d{2}-\\d{4}");
    }
    
    private boolean isBoolean(String value) {
        String lower = value.toLowerCase().trim();
        return "true".equals(lower) || "false".equals(lower) || 
               "yes".equals(lower) || "no".equals(lower) ||
               "1".equals(lower) || "0".equals(lower);
    }
    
    // Result classes
    
    public static class ContentAnalysisResult {
        private boolean success;
        private String message;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String detectedEncoding;
        private FileType detectedFileType;
        private boolean isBinaryFile;
        private boolean isEmpty;
        private int lineCount;
        private int totalCharacters;
        private double averageLineLength;
        private int maxLineLength;
        private int minLineLength;
        private Map<String, Object> structureAnalysis;
        private ContentQualityAssessment qualityAssessment;
        private Date analysisTimestamp;
        
        public ContentAnalysisResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.analysisTimestamp = new Date();
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getDetectedEncoding() { return detectedEncoding; }
        public void setDetectedEncoding(String detectedEncoding) { this.detectedEncoding = detectedEncoding; }
        
        public FileType getDetectedFileType() { return detectedFileType; }
        public void setDetectedFileType(FileType detectedFileType) { this.detectedFileType = detectedFileType; }
        
        public boolean isBinaryFile() { return isBinaryFile; }
        public void setBinaryFile(boolean binaryFile) { this.isBinaryFile = binaryFile; }
        
        public boolean isEmpty() { return isEmpty; }
        public void setEmpty(boolean empty) { this.isEmpty = empty; }
        
        public int getLineCount() { return lineCount; }
        public void setLineCount(int lineCount) { this.lineCount = lineCount; }
        
        public int getTotalCharacters() { return totalCharacters; }
        public void setTotalCharacters(int totalCharacters) { this.totalCharacters = totalCharacters; }
        
        public double getAverageLineLength() { return averageLineLength; }
        public void setAverageLineLength(double averageLineLength) { this.averageLineLength = averageLineLength; }
        
        public int getMaxLineLength() { return maxLineLength; }
        public void setMaxLineLength(int maxLineLength) { this.maxLineLength = maxLineLength; }
        
        public int getMinLineLength() { return minLineLength; }
        public void setMinLineLength(int minLineLength) { this.minLineLength = minLineLength; }
        
        public Map<String, Object> getStructureAnalysis() { return structureAnalysis; }
        public void setStructureAnalysis(Map<String, Object> structureAnalysis) { this.structureAnalysis = structureAnalysis; }
        
        public ContentQualityAssessment getQualityAssessment() { return qualityAssessment; }
        public void setQualityAssessment(ContentQualityAssessment qualityAssessment) { this.qualityAssessment = qualityAssessment; }
        
        public Date getAnalysisTimestamp() { return analysisTimestamp; }
        public void setAnalysisTimestamp(Date analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
    }
    
    public static class ContentQualityAssessment {
        private double emptyLinePercentage;
        private double specialCharPercentage;
        private double averageLineLength;
        private double qualityScore;
        private List<String> recommendations = new ArrayList<>();
        
        // Getters and Setters
        public double getEmptyLinePercentage() { return emptyLinePercentage; }
        public void setEmptyLinePercentage(double emptyLinePercentage) { this.emptyLinePercentage = emptyLinePercentage; }
        
        public double getSpecialCharPercentage() { return specialCharPercentage; }
        public void setSpecialCharPercentage(double specialCharPercentage) { this.specialCharPercentage = specialCharPercentage; }
        
        public double getAverageLineLength() { return averageLineLength; }
        public void setAverageLineLength(double averageLineLength) { this.averageLineLength = averageLineLength; }
        
        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}