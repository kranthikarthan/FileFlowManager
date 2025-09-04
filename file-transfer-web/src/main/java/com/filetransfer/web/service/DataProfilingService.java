package com.filetransfer.web.service;

import com.filetransfer.web.entity.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for statistical analysis and profiling of file data
 */
@Service
public class DataProfilingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataProfilingService.class);
    
    // Common data patterns for profiling
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$|^\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$");
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("^\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}$");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    
    /**
     * Generate comprehensive data profile for a file
     */
    public DataProfile generateDataProfile(String filePath, FileType fileType) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new DataProfile(false, "File not found: " + filePath);
            }
            
            DataProfile profile = new DataProfile(true, "Data profiling completed successfully");
            profile.setFilePath(filePath);
            profile.setFileType(fileType);
            profile.setProfileTimestamp(LocalDateTime.now());
            
            // Basic file statistics
            long fileSize = Files.size(path);
            profile.setFileSize(fileSize);
            
            // Profile based on file type
            switch (fileType) {
                case CSV:
                    profileCsvData(path, profile);
                    break;
                case JSON:
                    profileJsonData(path, profile);
                    break;
                case XML:
                    profileXmlData(path, profile);
                    break;
                case DELIMITED:
                    profileDelimitedData(path, profile);
                    break;
                case FIXED_WIDTH:
                    profileFixedWidthData(path, profile);
                    break;
                default:
                    profileTextData(path, profile);
                    break;
            }
            
            // Generate data quality insights
            generateDataQualityInsights(profile);
            
            // Generate processing recommendations
            generateProcessingRecommendations(profile);
            
            return profile;
            
        } catch (Exception e) {
            logger.error("Error generating data profile for {}: {}", filePath, e.getMessage(), e);
            return new DataProfile(false, "Data profiling failed: " + e.getMessage());
        }
    }
    
    /**
     * Profile CSV file data
     */
    private void profileCsvData(Path filePath, DataProfile profile) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            profile.setRecordCount(0);
            return;
        }
        
        String[] headers = lines.get(0).split(",");
        profile.setColumnCount(headers.length);
        profile.setRecordCount(lines.size() - 1); // Exclude header
        
        List<ColumnProfile> columnProfiles = new ArrayList<>();
        
        // Profile each column
        for (int col = 0; col < headers.length; col++) {
            ColumnProfile columnProfile = new ColumnProfile();
            columnProfile.setColumnName(headers[col].trim());
            columnProfile.setColumnIndex(col);
            
            List<String> columnValues = extractColumnValues(lines, col);
            profileColumn(columnValues, columnProfile);
            
            columnProfiles.add(columnProfile);
        }
        
        profile.setColumnProfiles(columnProfiles);
        
        // Overall data statistics
        profile.setDataStatistics(generateDataStatistics(columnProfiles, lines.size() - 1));
    }
    
    /**
     * Profile JSON file data
     */
    private void profileJsonData(Path filePath, DataProfile profile) throws IOException {
        String content = Files.readString(filePath);
        
        try {
            // Parse JSON and analyze structure
            Map<String, Object> jsonStats = new HashMap<>();
            
            // Count different JSON elements
            int objectCount = countOccurrences(content, '{');
            int arrayCount = countOccurrences(content, '[');
            int stringCount = countOccurrences(content, '"') / 2; // Rough estimate
            
            jsonStats.put("estimatedObjects", objectCount);
            jsonStats.put("estimatedArrays", arrayCount);
            jsonStats.put("estimatedStrings", stringCount);
            
            // Estimate nesting depth
            int maxNesting = calculateMaxNesting(content);
            jsonStats.put("maxNestingDepth", maxNesting);
            
            // File complexity assessment
            double complexity = calculateJsonComplexity(objectCount, arrayCount, maxNesting);
            jsonStats.put("complexityScore", complexity);
            
            profile.setDataStatistics(jsonStats);
            
        } catch (Exception e) {
            logger.error("Error profiling JSON data: {}", e.getMessage());
            profile.setSuccess(false);
            profile.setMessage("JSON profiling failed: " + e.getMessage());
        }
    }
    
    /**
     * Profile XML file data
     */
    private void profileXmlData(Path filePath, DataProfile profile) throws IOException {
        String content = Files.readString(filePath);
        
        Map<String, Object> xmlStats = new HashMap<>();
        
        // Count XML elements
        int elementCount = countOccurrences(content, '<') / 2; // Rough estimate
        int attributeCount = countOccurrences(content, '=');
        
        xmlStats.put("estimatedElements", elementCount);
        xmlStats.put("estimatedAttributes", attributeCount);
        
        // Check for namespaces
        boolean hasNamespaces = content.contains("xmlns:");
        xmlStats.put("hasNamespaces", hasNamespaces);
        
        // Estimate file complexity
        int maxNesting = calculateMaxXmlNesting(content);
        xmlStats.put("maxNestingDepth", maxNesting);
        
        double complexity = calculateXmlComplexity(elementCount, attributeCount, maxNesting);
        xmlStats.put("complexityScore", complexity);
        
        profile.setDataStatistics(xmlStats);
    }
    
    /**
     * Profile delimited file data
     */
    private void profileDelimitedData(Path filePath, DataProfile profile) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            profile.setRecordCount(0);
            return;
        }
        
        // Detect delimiter
        String delimiter = detectDelimiter(lines.get(0));
        if (delimiter == null) {
            profile.setSuccess(false);
            profile.setMessage("Could not detect delimiter in delimited file");
            return;
        }
        
        String[] headers = lines.get(0).split(Pattern.quote(delimiter));
        profile.setColumnCount(headers.length);
        profile.setRecordCount(lines.size() - 1);
        
        List<ColumnProfile> columnProfiles = new ArrayList<>();
        
        // Profile each column
        for (int col = 0; col < headers.length; col++) {
            ColumnProfile columnProfile = new ColumnProfile();
            columnProfile.setColumnName(headers[col].trim());
            columnProfile.setColumnIndex(col);
            
            List<String> columnValues = extractDelimitedColumnValues(lines, col, delimiter);
            profileColumn(columnValues, columnProfile);
            
            columnProfiles.add(columnProfile);
        }
        
        profile.setColumnProfiles(columnProfiles);
        profile.setDataStatistics(generateDataStatistics(columnProfiles, lines.size() - 1));
    }
    
    /**
     * Profile fixed-width file data
     */
    private void profileFixedWidthData(Path filePath, DataProfile profile) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            profile.setRecordCount(0);
            return;
        }
        
        int recordLength = lines.get(0).length();
        profile.setRecordCount(lines.size());
        
        Map<String, Object> fixedWidthStats = new HashMap<>();
        fixedWidthStats.put("recordLength", recordLength);
        fixedWidthStats.put("totalRecords", lines.size());
        
        // Check record length consistency
        boolean consistentLength = lines.stream()
                .allMatch(line -> line.length() == recordLength);
        fixedWidthStats.put("consistentLength", consistentLength);
        
        // Analyze character distribution by position
        Map<Integer, Map<Character, Integer>> positionCharDistribution = new HashMap<>();
        
        for (String line : lines.subList(0, Math.min(1000, lines.size()))) {
            for (int pos = 0; pos < line.length(); pos++) {
                char c = line.charAt(pos);
                positionCharDistribution
                    .computeIfAbsent(pos, k -> new HashMap<>())
                    .merge(c, 1, Integer::sum);
            }
        }
        
        // Detect potential field boundaries
        List<Integer> fieldBoundaries = detectFieldBoundaries(positionCharDistribution, recordLength);
        fixedWidthStats.put("detectedFieldBoundaries", fieldBoundaries);
        fixedWidthStats.put("estimatedFieldCount", fieldBoundaries.size());
        
        profile.setDataStatistics(fixedWidthStats);
    }
    
    /**
     * Profile general text file data
     */
    private void profileTextData(Path filePath, DataProfile profile) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        
        Map<String, Object> textStats = new HashMap<>();
        textStats.put("totalLines", lines.size());
        
        if (!lines.isEmpty()) {
            // Line length statistics
            IntSummaryStatistics lengthStats = lines.stream()
                    .mapToInt(String::length)
                    .summaryStatistics();
            
            textStats.put("averageLineLength", lengthStats.getAverage());
            textStats.put("maxLineLength", lengthStats.getMax());
            textStats.put("minLineLength", lengthStats.getMin());
            
            // Character distribution
            Map<Character, Integer> charDistribution = new HashMap<>();
            for (String line : lines) {
                for (char c : line.toCharArray()) {
                    charDistribution.merge(c, 1, Integer::sum);
                }
            }
            
            textStats.put("uniqueCharacters", charDistribution.size());
            textStats.put("mostCommonCharacter", getMostFrequent(charDistribution));
            
            // Content patterns
            int emptyLines = (int) lines.stream().filter(String::isEmpty).count();
            textStats.put("emptyLines", emptyLines);
            textStats.put("emptyLinePercentage", (double) emptyLines / lines.size() * 100);
        }
        
        profile.setDataStatistics(textStats);
    }
    
    /**
     * Profile individual column data
     */
    private void profileColumn(List<String> values, ColumnProfile columnProfile) {
        if (values.isEmpty()) {
            columnProfile.setDataType("UNKNOWN");
            return;
        }
        
        // Remove header and filter out empty values for analysis
        List<String> dataValues = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.toList());
        
        columnProfile.setTotalValues(values.size());
        columnProfile.setNonEmptyValues(dataValues.size());
        columnProfile.setEmptyValues(values.size() - dataValues.size());
        columnProfile.setNullPercentage((double) (values.size() - dataValues.size()) / values.size() * 100);
        
        if (dataValues.isEmpty()) {
            columnProfile.setDataType("EMPTY");
            return;
        }
        
        // Detect data type
        String detectedType = detectColumnDataType(dataValues);
        columnProfile.setDataType(detectedType);
        
        // Calculate unique values
        Set<String> uniqueValues = new HashSet<>(dataValues);
        columnProfile.setUniqueValues(uniqueValues.size());
        columnProfile.setDuplicateValues(dataValues.size() - uniqueValues.size());
        columnProfile.setUniquenessPercentage((double) uniqueValues.size() / dataValues.size() * 100);
        
        // Length statistics
        IntSummaryStatistics lengthStats = dataValues.stream()
                .mapToInt(String::length)
                .summaryStatistics();
        
        columnProfile.setMinLength(lengthStats.getMin());
        columnProfile.setMaxLength(lengthStats.getMax());
        columnProfile.setAverageLength(lengthStats.getAverage());
        
        // Most common values
        Map<String, Long> valueFrequency = dataValues.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        
        List<Map.Entry<String, Long>> topValues = valueFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        columnProfile.setTopValues(topValues.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
        
        // Numeric analysis if applicable
        if ("NUMERIC".equals(detectedType) || "INTEGER".equals(detectedType) || "DECIMAL".equals(detectedType)) {
            profileNumericColumn(dataValues, columnProfile);
        }
        
        // Pattern analysis
        profileColumnPatterns(dataValues, columnProfile);
        
        // Data quality assessment
        assessColumnQuality(dataValues, columnProfile);
    }
    
    /**
     * Profile numeric column data
     */
    private void profileNumericColumn(List<String> values, ColumnProfile columnProfile) {
        List<Double> numericValues = values.stream()
                .filter(this::isValidDecimal)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
        
        if (numericValues.isEmpty()) return;
        
        DoubleSummaryStatistics stats = numericValues.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        
        Map<String, Object> numericStats = new HashMap<>();
        numericStats.put("min", stats.getMin());
        numericStats.put("max", stats.getMax());
        numericStats.put("average", stats.getAverage());
        numericStats.put("sum", stats.getSum());
        numericStats.put("count", stats.getCount());
        
        // Calculate additional statistics
        Collections.sort(numericValues);
        int size = numericValues.size();
        
        // Median
        double median = size % 2 == 0 ? 
            (numericValues.get(size / 2 - 1) + numericValues.get(size / 2)) / 2.0 :
            numericValues.get(size / 2);
        numericStats.put("median", median);
        
        // Quartiles
        double q1 = numericValues.get(size / 4);
        double q3 = numericValues.get(3 * size / 4);
        numericStats.put("q1", q1);
        numericStats.put("q3", q3);
        numericStats.put("iqr", q3 - q1);
        
        // Standard deviation
        double mean = stats.getAverage();
        double variance = numericValues.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        numericStats.put("standardDeviation", Math.sqrt(variance));
        numericStats.put("variance", variance);
        
        // Outlier detection (values beyond 1.5 * IQR)
        double lowerBound = q1 - 1.5 * (q3 - q1);
        double upperBound = q3 + 1.5 * (q3 - q1);
        long outliers = numericValues.stream()
                .filter(v -> v < lowerBound || v > upperBound)
                .count();
        numericStats.put("outliers", outliers);
        numericStats.put("outlierPercentage", (double) outliers / size * 100);
        
        columnProfile.setNumericStatistics(numericStats);
    }
    
    /**
     * Profile column patterns and data types
     */
    private void profileColumnPatterns(List<String> values, ColumnProfile columnProfile) {
        Map<String, Integer> patternMatches = new HashMap<>();
        
        for (String value : values) {
            if (EMAIL_PATTERN.matcher(value).matches()) {
                patternMatches.merge("EMAIL", 1, Integer::sum);
            }
            if (PHONE_PATTERN.matcher(value).matches()) {
                patternMatches.merge("PHONE", 1, Integer::sum);
            }
            if (SSN_PATTERN.matcher(value).matches()) {
                patternMatches.merge("SSN", 1, Integer::sum);
            }
            if (CREDIT_CARD_PATTERN.matcher(value).matches()) {
                patternMatches.merge("CREDIT_CARD", 1, Integer::sum);
            }
            if (IP_ADDRESS_PATTERN.matcher(value).matches()) {
                patternMatches.merge("IP_ADDRESS", 1, Integer::sum);
            }
        }
        
        // Calculate pattern percentages
        Map<String, Double> patternPercentages = new HashMap<>();
        int totalValues = values.size();
        
        for (Map.Entry<String, Integer> entry : patternMatches.entrySet()) {
            double percentage = (double) entry.getValue() / totalValues * 100;
            if (percentage > 5.0) { // Only include patterns that match >5% of values
                patternPercentages.put(entry.getKey(), percentage);
            }
        }
        
        columnProfile.setDetectedPatterns(patternPercentages);
        
        // Suggest column purpose based on patterns
        String suggestedPurpose = suggestColumnPurpose(patternPercentages, columnProfile.getColumnName());
        columnProfile.setSuggestedPurpose(suggestedPurpose);
    }
    
    /**
     * Assess column data quality
     */
    private void assessColumnQuality(List<String> values, ColumnProfile columnProfile) {
        List<String> qualityIssues = new ArrayList<>();
        List<String> qualityRecommendations = new ArrayList<>();
        
        // Check for quality issues
        if (columnProfile.getNullPercentage() > 20) {
            qualityIssues.add("High null percentage (" + String.format("%.1f", columnProfile.getNullPercentage()) + "%)");
            qualityRecommendations.add("Consider data cleansing to reduce missing values");
        }
        
        if (columnProfile.getUniquenessPercentage() < 10 && values.size() > 100) {
            qualityIssues.add("Low data variety (only " + String.format("%.1f", columnProfile.getUniquenessPercentage()) + "% unique values)");
            qualityRecommendations.add("Investigate data source for potential data quality issues");
        }
        
        if (columnProfile.getNumericStatistics() != null) {
            Map<String, Object> numStats = columnProfile.getNumericStatistics();
            Double outlierPercentage = (Double) numStats.get("outlierPercentage");
            if (outlierPercentage != null && outlierPercentage > 5.0) {
                qualityIssues.add("High outlier percentage (" + String.format("%.1f", outlierPercentage) + "%)");
                qualityRecommendations.add("Review outlier values for data accuracy");
            }
        }
        
        // Check for inconsistent formatting
        Set<Integer> lengths = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(String::length)
                .collect(Collectors.toSet());
        
        if (lengths.size() > values.size() * 0.1) { // More than 10% different lengths
            qualityIssues.add("Inconsistent value formatting detected");
            qualityRecommendations.add("Consider standardizing value format");
        }
        
        // Calculate overall quality score
        double qualityScore = calculateColumnQualityScore(columnProfile, qualityIssues.size());
        
        columnProfile.setQualityScore(qualityScore);
        columnProfile.setQualityIssues(qualityIssues);
        columnProfile.setQualityRecommendations(qualityRecommendations);
    }
    
    /**
     * Generate overall data statistics
     */
    private Map<String, Object> generateDataStatistics(List<ColumnProfile> columnProfiles, int recordCount) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRecords", recordCount);
        stats.put("totalColumns", columnProfiles.size());
        
        // Data type distribution
        Map<String, Long> dataTypeDistribution = columnProfiles.stream()
                .collect(Collectors.groupingBy(ColumnProfile::getDataType, Collectors.counting()));
        stats.put("dataTypeDistribution", dataTypeDistribution);
        
        // Quality statistics
        double averageQuality = columnProfiles.stream()
                .mapToDouble(ColumnProfile::getQualityScore)
                .average()
                .orElse(0.0);
        stats.put("averageQualityScore", averageQuality);
        
        long columnsWithIssues = columnProfiles.stream()
                .filter(cp -> !cp.getQualityIssues().isEmpty())
                .count();
        stats.put("columnsWithQualityIssues", columnsWithIssues);
        
        // Completeness statistics
        double averageCompleteness = columnProfiles.stream()
                .mapToDouble(cp -> 100.0 - cp.getNullPercentage())
                .average()
                .orElse(0.0);
        stats.put("averageCompleteness", averageCompleteness);
        
        // Uniqueness statistics
        double averageUniqueness = columnProfiles.stream()
                .mapToDouble(ColumnProfile::getUniquenessPercentage)
                .average()
                .orElse(0.0);
        stats.put("averageUniqueness", averageUniqueness);
        
        return stats;
    }
    
    /**
     * Generate data quality insights
     */
    private void generateDataQualityInsights(DataProfile profile) {
        List<String> insights = new ArrayList<>();
        
        Map<String, Object> stats = profile.getDataStatistics();
        if (stats == null) return;
        
        // Overall quality assessment
        Double avgQuality = (Double) stats.get("averageQualityScore");
        if (avgQuality != null) {
            if (avgQuality >= 90) {
                insights.add("Excellent data quality - ready for production processing");
            } else if (avgQuality >= 75) {
                insights.add("Good data quality - minor issues detected");
            } else if (avgQuality >= 60) {
                insights.add("Moderate data quality - review recommended before processing");
            } else {
                insights.add("Poor data quality - significant issues require attention");
            }
        }
        
        // Completeness insights
        Double avgCompleteness = (Double) stats.get("averageCompleteness");
        if (avgCompleteness != null && avgCompleteness < 85) {
            insights.add("Data completeness below recommended threshold (" + 
                String.format("%.1f", avgCompleteness) + "% complete)");
        }
        
        // Uniqueness insights
        Double avgUniqueness = (Double) stats.get("averageUniqueness");
        if (avgUniqueness != null && avgUniqueness < 20) {
            insights.add("Low data variety detected - possible data quality issues");
        }
        
        // Column-specific insights
        if (profile.getColumnProfiles() != null) {
            long problematicColumns = profile.getColumnProfiles().stream()
                    .filter(cp -> cp.getQualityScore() < 70)
                    .count();
            
            if (problematicColumns > 0) {
                insights.add(problematicColumns + " columns have quality scores below 70%");
            }
        }
        
        profile.setQualityInsights(insights);
    }
    
    /**
     * Generate processing recommendations
     */
    private void generateProcessingRecommendations(DataProfile profile) {
        List<String> recommendations = new ArrayList<>();
        
        // File size recommendations
        if (profile.getFileSize() > 100 * 1024 * 1024) { // > 100MB
            recommendations.add("Large file detected - consider streaming processing");
            recommendations.add("Enable compression for better transfer performance");
        }
        
        // Record count recommendations
        if (profile.getRecordCount() > 1000000) { // > 1M records
            recommendations.add("High record count - consider batch processing");
            recommendations.add("Implement progress tracking for user feedback");
        }
        
        // Column-specific recommendations
        if (profile.getColumnProfiles() != null) {
            for (ColumnProfile columnProfile : profile.getColumnProfiles()) {
                if (!columnProfile.getQualityRecommendations().isEmpty()) {
                    recommendations.add("Column '" + columnProfile.getColumnName() + "': " + 
                        String.join(", ", columnProfile.getQualityRecommendations()));
                }
            }
        }
        
        // Performance recommendations
        Map<String, Object> stats = profile.getDataStatistics();
        if (stats != null) {
            Double avgQuality = (Double) stats.get("averageQualityScore");
            if (avgQuality != null && avgQuality < 80) {
                recommendations.add("Consider data validation and cleansing before processing");
            }
        }
        
        profile.setProcessingRecommendations(recommendations);
    }
    
    // Utility methods
    
    private List<String> extractColumnValues(List<String> lines, int columnIndex) {
        return lines.stream()
                .skip(1) // Skip header
                .map(line -> {
                    String[] values = line.split(",");
                    return values.length > columnIndex ? values[columnIndex].trim() : "";
                })
                .collect(Collectors.toList());
    }
    
    private List<String> extractDelimitedColumnValues(List<String> lines, int columnIndex, String delimiter) {
        return lines.stream()
                .skip(1) // Skip header
                .map(line -> {
                    String[] values = line.split(Pattern.quote(delimiter));
                    return values.length > columnIndex ? values[columnIndex].trim() : "";
                })
                .collect(Collectors.toList());
    }
    
    private String detectColumnDataType(List<String> values) {
        if (values.isEmpty()) return "UNKNOWN";
        
        // Sample values for type detection
        List<String> sampleValues = values.stream()
                .limit(Math.min(100, values.size()))
                .collect(Collectors.toList());
        
        // Check if all values are integers
        if (sampleValues.stream().allMatch(this::isValidInteger)) {
            return "INTEGER";
        }
        
        // Check if all values are decimals
        if (sampleValues.stream().allMatch(this::isValidDecimal)) {
            return "DECIMAL";
        }
        
        // Check if all values are dates
        if (sampleValues.stream().allMatch(this::isValidDate)) {
            return "DATE";
        }
        
        // Check if all values are booleans
        if (sampleValues.stream().allMatch(this::isValidBoolean)) {
            return "BOOLEAN";
        }
        
        // Check for specific patterns
        long emailCount = sampleValues.stream().filter(v -> EMAIL_PATTERN.matcher(v).matches()).count();
        if (emailCount > sampleValues.size() * 0.8) {
            return "EMAIL";
        }
        
        long phoneCount = sampleValues.stream().filter(v -> PHONE_PATTERN.matcher(v).matches()).count();
        if (phoneCount > sampleValues.size() * 0.8) {
            return "PHONE";
        }
        
        // Default to string
        return "STRING";
    }
    
    private String suggestColumnPurpose(Map<String, Double> patterns, String columnName) {
        String lowerName = columnName.toLowerCase();
        
        // Pattern-based suggestions
        for (Map.Entry<String, Double> entry : patterns.entrySet()) {
            if (entry.getValue() > 80.0) { // >80% match
                return entry.getKey().toLowerCase() + "_field";
            }
        }
        
        // Name-based suggestions
        if (lowerName.contains("email") || lowerName.contains("mail")) {
            return "email_address";
        }
        if (lowerName.contains("phone") || lowerName.contains("tel")) {
            return "phone_number";
        }
        if (lowerName.contains("id") || lowerName.contains("key")) {
            return "identifier";
        }
        if (lowerName.contains("name")) {
            return "name_field";
        }
        if (lowerName.contains("date") || lowerName.contains("time")) {
            return "date_time";
        }
        if (lowerName.contains("amount") || lowerName.contains("price") || lowerName.contains("cost")) {
            return "monetary_value";
        }
        
        return "general_data";
    }
    
    private double calculateColumnQualityScore(ColumnProfile columnProfile, int issueCount) {
        double score = 100.0;
        
        // Deduct for quality issues
        score -= issueCount * 10.0;
        
        // Deduct for high null percentage
        score -= columnProfile.getNullPercentage() * 0.5;
        
        // Bonus for high uniqueness (if appropriate)
        if (columnProfile.getUniquenessPercentage() > 80) {
            score += 5.0;
        }
        
        // Bonus for consistent formatting
        if (columnProfile.getMaxLength() - columnProfile.getMinLength() <= 2) {
            score += 5.0;
        }
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    private int countOccurrences(String text, char target) {
        return (int) text.chars().filter(ch -> ch == target).count();
    }
    
    private int calculateMaxNesting(String content) {
        int maxNesting = 0;
        int currentNesting = 0;
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
                if (c == '{' || c == '[') {
                    currentNesting++;
                    maxNesting = Math.max(maxNesting, currentNesting);
                } else if (c == '}' || c == ']') {
                    currentNesting--;
                }
            }
        }
        
        return maxNesting;
    }
    
    private int calculateMaxXmlNesting(String content) {
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
        
        return maxNesting;
    }
    
    private double calculateJsonComplexity(int objects, int arrays, int nesting) {
        return (objects * 1.0 + arrays * 1.5 + nesting * 2.0) / 10.0;
    }
    
    private double calculateXmlComplexity(int elements, int attributes, int nesting) {
        return (elements * 1.0 + attributes * 0.5 + nesting * 2.0) / 10.0;
    }
    
    private List<Integer> detectFieldBoundaries(Map<Integer, Map<Character, Integer>> charDistribution, int recordLength) {
        List<Integer> boundaries = new ArrayList<>();
        
        for (int pos = 1; pos < recordLength - 1; pos++) {
            Map<Character, Integer> posChars = charDistribution.get(pos);
            if (posChars != null) {
                // If this position is mostly spaces, it's likely a field boundary
                int spaceCount = posChars.getOrDefault(' ', 0);
                int totalCount = posChars.values().stream().mapToInt(Integer::intValue).sum();
                
                if (spaceCount > totalCount * 0.8) { // >80% spaces
                    boundaries.add(pos);
                }
            }
        }
        
        return boundaries;
    }
    
    private String detectDelimiter(String line) {
        String[] delimiters = {"|", "\t", ";", ":", "~"};
        for (String delimiter : delimiters) {
            if (line.contains(delimiter)) {
                return delimiter;
            }
        }
        return null;
    }
    
    private <T> T getMostFrequent(Map<T, Integer> frequency) {
        return frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
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
               value.matches("\\d{8}");
    }
    
    private boolean isValidBoolean(String value) {
        String lower = value.toLowerCase().trim();
        return "true".equals(lower) || "false".equals(lower) || 
               "yes".equals(lower) || "no".equals(lower) ||
               "1".equals(lower) || "0".equals(lower);
    }
    
    // Result classes
    
    public static class DataProfile {
        private boolean success;
        private String message;
        private String filePath;
        private FileType fileType;
        private LocalDateTime profileTimestamp;
        private long fileSize;
        private int recordCount;
        private int columnCount;
        private List<ColumnProfile> columnProfiles;
        private Map<String, Object> dataStatistics;
        private List<String> qualityInsights;
        private List<String> processingRecommendations;
        
        public DataProfile(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
        
        public LocalDateTime getProfileTimestamp() { return profileTimestamp; }
        public void setProfileTimestamp(LocalDateTime profileTimestamp) { this.profileTimestamp = profileTimestamp; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        
        public int getColumnCount() { return columnCount; }
        public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
        
        public List<ColumnProfile> getColumnProfiles() { return columnProfiles; }
        public void setColumnProfiles(List<ColumnProfile> columnProfiles) { this.columnProfiles = columnProfiles; }
        
        public Map<String, Object> getDataStatistics() { return dataStatistics; }
        public void setDataStatistics(Map<String, Object> dataStatistics) { this.dataStatistics = dataStatistics; }
        
        public List<String> getQualityInsights() { return qualityInsights; }
        public void setQualityInsights(List<String> qualityInsights) { this.qualityInsights = qualityInsights; }
        
        public List<String> getProcessingRecommendations() { return processingRecommendations; }
        public void setProcessingRecommendations(List<String> processingRecommendations) { this.processingRecommendations = processingRecommendations; }
    }
    
    public static class ColumnProfile {
        private String columnName;
        private int columnIndex;
        private String dataType;
        private int totalValues;
        private int nonEmptyValues;
        private int emptyValues;
        private double nullPercentage;
        private int uniqueValues;
        private int duplicateValues;
        private double uniquenessPercentage;
        private int minLength;
        private int maxLength;
        private double averageLength;
        private Map<String, Long> topValues;
        private Map<String, Object> numericStatistics;
        private Map<String, Double> detectedPatterns;
        private String suggestedPurpose;
        private double qualityScore;
        private List<String> qualityIssues = new ArrayList<>();
        private List<String> qualityRecommendations = new ArrayList<>();
        
        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public int getColumnIndex() { return columnIndex; }
        public void setColumnIndex(int columnIndex) { this.columnIndex = columnIndex; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public int getTotalValues() { return totalValues; }
        public void setTotalValues(int totalValues) { this.totalValues = totalValues; }
        
        public int getNonEmptyValues() { return nonEmptyValues; }
        public void setNonEmptyValues(int nonEmptyValues) { this.nonEmptyValues = nonEmptyValues; }
        
        public int getEmptyValues() { return emptyValues; }
        public void setEmptyValues(int emptyValues) { this.emptyValues = emptyValues; }
        
        public double getNullPercentage() { return nullPercentage; }
        public void setNullPercentage(double nullPercentage) { this.nullPercentage = nullPercentage; }
        
        public int getUniqueValues() { return uniqueValues; }
        public void setUniqueValues(int uniqueValues) { this.uniqueValues = uniqueValues; }
        
        public int getDuplicateValues() { return duplicateValues; }
        public void setDuplicateValues(int duplicateValues) { this.duplicateValues = duplicateValues; }
        
        public double getUniquenessPercentage() { return uniquenessPercentage; }
        public void setUniquenessPercentage(double uniquenessPercentage) { this.uniquenessPercentage = uniquenessPercentage; }
        
        public int getMinLength() { return minLength; }
        public void setMinLength(int minLength) { this.minLength = minLength; }
        
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        
        public double getAverageLength() { return averageLength; }
        public void setAverageLength(double averageLength) { this.averageLength = averageLength; }
        
        public Map<String, Long> getTopValues() { return topValues; }
        public void setTopValues(Map<String, Long> topValues) { this.topValues = topValues; }
        
        public Map<String, Object> getNumericStatistics() { return numericStatistics; }
        public void setNumericStatistics(Map<String, Object> numericStatistics) { this.numericStatistics = numericStatistics; }
        
        public Map<String, Double> getDetectedPatterns() { return detectedPatterns; }
        public void setDetectedPatterns(Map<String, Double> detectedPatterns) { this.detectedPatterns = detectedPatterns; }
        
        public String getSuggestedPurpose() { return suggestedPurpose; }
        public void setSuggestedPurpose(String suggestedPurpose) { this.suggestedPurpose = suggestedPurpose; }
        
        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
        
        public List<String> getQualityIssues() { return qualityIssues; }
        public void setQualityIssues(List<String> qualityIssues) { this.qualityIssues = qualityIssues; }
        
        public List<String> getQualityRecommendations() { return qualityRecommendations; }
        public void setQualityRecommendations(List<String> qualityRecommendations) { this.qualityRecommendations = qualityRecommendations; }
    }
}