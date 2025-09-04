package com.filetransfer.web.controller;

import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.service.ContentAnalysisService;
import com.filetransfer.web.service.DataProfilingService;
import com.filetransfer.web.service.SchemaValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * REST Controller for content analysis operations
 */
@RestController
@RequestMapping("/api/v1/content-analysis")
public class ContentAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentAnalysisController.class);
    
    @Autowired
    private ContentAnalysisService contentAnalysisService;
    
    @Autowired
    private SchemaValidationService schemaValidationService;
    
    @Autowired
    private DataProfilingService dataProfilingService;
    
    /**
     * Analyze file content
     */
    @PostMapping("/analyze")
    public ResponseEntity<ContentAnalysisService.ContentAnalysisResult> analyzeFile(
            @RequestParam String filePath,
            @RequestParam String fileName) {
        try {
            ContentAnalysisService.ContentAnalysisResult result = 
                contentAnalysisService.analyzeFileContent(filePath, fileName);
            
            if (result.isSuccess()) {
                logger.info("Content analysis completed for file: {}", fileName);
                return ResponseEntity.ok(result);
            } else {
                logger.warn("Content analysis failed for file: {}", fileName);
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("Error during content analysis for file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload and analyze file
     */
    @PostMapping("/upload-and-analyze")
    public ResponseEntity<ContentAnalysisService.ContentAnalysisResult> uploadAndAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tenantId) {
        try {
            // Save uploaded file temporarily
            Path tempFile = Files.createTempFile("analysis_", "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Analyze the file
            ContentAnalysisService.ContentAnalysisResult result = 
                contentAnalysisService.analyzeFileContent(tempFile.toString(), file.getOriginalFilename());
            
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
            
            logger.info("Upload and analysis completed for file: {}", file.getOriginalFilename());
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("Error during file upload and analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validate file against schema
     */
    @PostMapping("/validate-schema")
    public ResponseEntity<SchemaValidationService.SchemaValidationResult> validateSchema(
            @RequestParam String filePath,
            @RequestParam(required = false) String schemaPath,
            @RequestParam FileType fileType) {
        try {
            SchemaValidationService.SchemaValidationResult result = 
                schemaValidationService.validateFileSchema(filePath, schemaPath, fileType);
            
            if (result.isSuccess()) {
                logger.info("Schema validation completed for file: {}", filePath);
                return ResponseEntity.ok(result);
            } else {
                logger.warn("Schema validation failed for file: {}", filePath);
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("Error during schema validation for file {}: {}", filePath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate data profile for file
     */
    @PostMapping("/profile")
    public ResponseEntity<DataProfilingService.DataProfile> generateDataProfile(
            @RequestParam String filePath,
            @RequestParam FileType fileType) {
        try {
            DataProfilingService.DataProfile profile = 
                dataProfilingService.generateDataProfile(filePath, fileType);
            
            if (profile.isSuccess()) {
                logger.info("Data profiling completed for file: {}", filePath);
                return ResponseEntity.ok(profile);
            } else {
                logger.warn("Data profiling failed for file: {}", filePath);
                return ResponseEntity.badRequest().body(profile);
            }
        } catch (Exception e) {
            logger.error("Error during data profiling for file {}: {}", filePath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Comprehensive file analysis (content + schema + profiling)
     */
    @PostMapping("/analyze-comprehensive")
    public ResponseEntity<Map<String, Object>> analyzeComprehensive(
            @RequestParam String filePath,
            @RequestParam String fileName,
            @RequestParam(required = false) String schemaPath,
            @RequestParam(required = false) FileType fileType) {
        try {
            Map<String, Object> comprehensiveResults = new HashMap<>();
            
            // Step 1: Content Analysis
            ContentAnalysisService.ContentAnalysisResult contentAnalysis = 
                contentAnalysisService.analyzeFileContent(filePath, fileName);
            comprehensiveResults.put("contentAnalysis", contentAnalysis);
            
            if (contentAnalysis.isSuccess()) {
                FileType detectedType = fileType != null ? fileType : contentAnalysis.getDetectedFileType();
                
                // Step 2: Schema Validation (if applicable)
                if (detectedType != null && !contentAnalysis.isBinaryFile()) {
                    SchemaValidationService.SchemaValidationResult schemaValidation = 
                        schemaValidationService.validateFileSchema(filePath, schemaPath, detectedType);
                    comprehensiveResults.put("schemaValidation", schemaValidation);
                }
                
                // Step 3: Data Profiling (if applicable)
                if (detectedType != null && !contentAnalysis.isBinaryFile() && !contentAnalysis.isEmpty()) {
                    DataProfilingService.DataProfile dataProfile = 
                        dataProfilingService.generateDataProfile(filePath, detectedType);
                    comprehensiveResults.put("dataProfile", dataProfile);
                }
            }
            
            logger.info("Comprehensive analysis completed for file: {}", fileName);
            return ResponseEntity.ok(comprehensiveResults);
            
        } catch (Exception e) {
            logger.error("Error during comprehensive analysis for file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get content analysis statistics for tenant
     */
    @GetMapping("/statistics/content")
    public ResponseEntity<Map<String, Object>> getContentAnalysisStatistics(
            @RequestParam String tenantId) {
        try {
            // This would integrate with the repository to get actual statistics
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("message", "Content analysis statistics endpoint - implementation pending");
            statistics.put("tenantId", tenantId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving content analysis statistics for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get schema validation statistics for tenant
     */
    @GetMapping("/statistics/validation")
    public ResponseEntity<Map<String, Object>> getSchemaValidationStatistics(
            @RequestParam String tenantId) {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("message", "Schema validation statistics endpoint - implementation pending");
            statistics.put("tenantId", tenantId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving schema validation statistics for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get data profiling statistics for tenant
     */
    @GetMapping("/statistics/profiling")
    public ResponseEntity<Map<String, Object>> getDataProfilingStatistics(
            @RequestParam String tenantId) {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("message", "Data profiling statistics endpoint - implementation pending");
            statistics.put("tenantId", tenantId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving data profiling statistics for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file type detection recommendations
     */
    @PostMapping("/detect-file-type")
    public ResponseEntity<Map<String, Object>> detectFileType(
            @RequestParam String filePath,
            @RequestParam String fileName) {
        try {
            ContentAnalysisService.ContentAnalysisResult analysis = 
                contentAnalysisService.analyzeFileContent(filePath, fileName);
            
            Map<String, Object> result = new HashMap<>();
            if (analysis.isSuccess()) {
                result.put("detectedFileType", analysis.getDetectedFileType());
                result.put("detectedEncoding", analysis.getDetectedEncoding());
                result.put("isBinaryFile", analysis.isBinaryFile());
                result.put("confidence", calculateDetectionConfidence(analysis));
                result.put("recommendations", generateFileTypeRecommendations(analysis));
            } else {
                result.put("error", analysis.getMessage());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error detecting file type for {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get data quality recommendations for file
     */
    @PostMapping("/quality-recommendations")
    public ResponseEntity<Map<String, Object>> getQualityRecommendations(
            @RequestParam String filePath,
            @RequestParam FileType fileType) {
        try {
            DataProfilingService.DataProfile profile = 
                dataProfilingService.generateDataProfile(filePath, fileType);
            
            Map<String, Object> result = new HashMap<>();
            if (profile.isSuccess()) {
                result.put("qualityInsights", profile.getQualityInsights());
                result.put("processingRecommendations", profile.getProcessingRecommendations());
                result.put("overallQuality", calculateOverallQuality(profile));
                result.put("actionItems", generateActionItems(profile));
            } else {
                result.put("error", profile.getMessage());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error generating quality recommendations for {}: {}", filePath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Compare two files for similarity
     */
    @PostMapping("/compare-files")
    public ResponseEntity<Map<String, Object>> compareFiles(
            @RequestParam String filePath1,
            @RequestParam String fileName1,
            @RequestParam String filePath2,
            @RequestParam String fileName2) {
        try {
            // Analyze both files
            ContentAnalysisService.ContentAnalysisResult analysis1 = 
                contentAnalysisService.analyzeFileContent(filePath1, fileName1);
            ContentAnalysisService.ContentAnalysisResult analysis2 = 
                contentAnalysisService.analyzeFileContent(filePath2, fileName2);
            
            Map<String, Object> comparison = new HashMap<>();
            
            if (analysis1.isSuccess() && analysis2.isSuccess()) {
                comparison.put("structuralSimilarity", calculateStructuralSimilarity(analysis1, analysis2));
                comparison.put("sizeDifference", Math.abs(analysis1.getFileSize() - analysis2.getFileSize()));
                comparison.put("typesMatch", analysis1.getDetectedFileType() == analysis2.getDetectedFileType());
                comparison.put("encodingsMatch", Objects.equals(analysis1.getDetectedEncoding(), analysis2.getDetectedEncoding()));
                comparison.put("qualityDifference", Math.abs(
                    (analysis1.getQualityAssessment() != null ? analysis1.getQualityAssessment().getQualityScore() : 0) -
                    (analysis2.getQualityAssessment() != null ? analysis2.getQualityAssessment().getQualityScore() : 0)
                ));
                comparison.put("recommendations", generateComparisonRecommendations(analysis1, analysis2));
            } else {
                comparison.put("error", "One or both file analyses failed");
            }
            
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            logger.error("Error comparing files {} and {}: {}", fileName1, fileName2, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods
    
    private double calculateDetectionConfidence(ContentAnalysisService.ContentAnalysisResult analysis) {
        double confidence = 50.0; // Base confidence
        
        // Increase confidence based on clear indicators
        if (analysis.isBinaryFile()) {
            confidence += 30.0; // Binary detection is usually reliable
        }
        
        if (analysis.getDetectedFileType() != null) {
            switch (analysis.getDetectedFileType()) {
                case JSON:
                case XML:
                    confidence += 25.0; // These have clear structure markers
                    break;
                case CSV:
                    confidence += 20.0; // CSV detection is fairly reliable
                    break;
                case DELIMITED:
                    confidence += 15.0; // Delimiter detection is good but not perfect
                    break;
                default:
                    confidence += 10.0; // Other types have moderate confidence
            }
        }
        
        // Adjust based on file size (larger files give more confidence)
        if (analysis.getFileSize() > 1024) { // > 1KB
            confidence += 10.0;
        }
        
        return Math.min(95.0, confidence); // Cap at 95%
    }
    
    private List<String> generateFileTypeRecommendations(ContentAnalysisService.ContentAnalysisResult analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.isBinaryFile()) {
            recommendations.add("Binary file detected - use binary processing mode");
            recommendations.add("Consider compression settings for binary files");
        } else {
            recommendations.add("Text file detected - suitable for content analysis");
            
            if (analysis.getDetectedFileType() != null) {
                switch (analysis.getDetectedFileType()) {
                    case CSV:
                        recommendations.add("CSV format detected - enable column validation");
                        recommendations.add("Consider schema validation for data quality");
                        break;
                    case JSON:
                        recommendations.add("JSON format detected - enable JSON schema validation");
                        recommendations.add("Consider pretty-printing for readability");
                        break;
                    case XML:
                        recommendations.add("XML format detected - enable XSD validation");
                        recommendations.add("Consider XML namespace validation");
                        break;
                }
            }
        }
        
        if (analysis.getQualityAssessment() != null && 
            analysis.getQualityAssessment().getQualityScore() < 80) {
            recommendations.add("Quality issues detected - review before processing");
        }
        
        return recommendations;
    }
    
    private Map<String, Object> calculateOverallQuality(DataProfilingService.DataProfile profile) {
        Map<String, Object> quality = new HashMap<>();
        
        if (profile.getDataStatistics() != null) {
            Double avgQuality = (Double) profile.getDataStatistics().get("averageQualityScore");
            Double avgCompleteness = (Double) profile.getDataStatistics().get("averageCompleteness");
            Double avgUniqueness = (Double) profile.getDataStatistics().get("averageUniqueness");
            
            quality.put("overallScore", avgQuality != null ? avgQuality : 0.0);
            quality.put("completeness", avgCompleteness != null ? avgCompleteness : 0.0);
            quality.put("uniqueness", avgUniqueness != null ? avgUniqueness : 0.0);
            
            // Calculate quality grade
            double score = avgQuality != null ? avgQuality : 0.0;
            String grade;
            if (score >= 90) grade = "Excellent";
            else if (score >= 80) grade = "Good";
            else if (score >= 70) grade = "Fair";
            else if (score >= 60) grade = "Poor";
            else grade = "Critical";
            
            quality.put("grade", grade);
        }
        
        return quality;
    }
    
    private List<String> generateActionItems(DataProfilingService.DataProfile profile) {
        List<String> actionItems = new ArrayList<>();
        
        if (profile.getProcessingRecommendations() != null) {
            actionItems.addAll(profile.getProcessingRecommendations());
        }
        
        if (profile.getQualityInsights() != null) {
            for (String insight : profile.getQualityInsights()) {
                if (insight.toLowerCase().contains("poor") || insight.toLowerCase().contains("critical")) {
                    actionItems.add("URGENT: " + insight);
                }
            }
        }
        
        return actionItems;
    }
    
    private double calculateStructuralSimilarity(ContentAnalysisService.ContentAnalysisResult analysis1, 
                                               ContentAnalysisService.ContentAnalysisResult analysis2) {
        double similarity = 0.0;
        
        // File type similarity
        if (analysis1.getDetectedFileType() == analysis2.getDetectedFileType()) {
            similarity += 30.0;
        }
        
        // Size similarity (within 10% is considered similar)
        double sizeRatio = Math.min(analysis1.getFileSize(), analysis2.getFileSize()) / 
                          (double) Math.max(analysis1.getFileSize(), analysis2.getFileSize());
        if (sizeRatio > 0.9) {
            similarity += 20.0;
        } else if (sizeRatio > 0.5) {
            similarity += 10.0;
        }
        
        // Line count similarity
        if (analysis1.getLineCount() > 0 && analysis2.getLineCount() > 0) {
            double lineRatio = Math.min(analysis1.getLineCount(), analysis2.getLineCount()) / 
                              (double) Math.max(analysis1.getLineCount(), analysis2.getLineCount());
            if (lineRatio > 0.9) {
                similarity += 20.0;
            } else if (lineRatio > 0.5) {
                similarity += 10.0;
            }
        }
        
        // Encoding similarity
        if (Objects.equals(analysis1.getDetectedEncoding(), analysis2.getDetectedEncoding())) {
            similarity += 15.0;
        }
        
        // Binary file similarity
        if (analysis1.isBinaryFile() == analysis2.isBinaryFile()) {
            similarity += 15.0;
        }
        
        return Math.min(100.0, similarity);
    }
    
    private List<String> generateComparisonRecommendations(ContentAnalysisService.ContentAnalysisResult analysis1,
                                                          ContentAnalysisService.ContentAnalysisResult analysis2) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis1.getDetectedFileType() != analysis2.getDetectedFileType()) {
            recommendations.add("Files have different types - may require different processing approaches");
        }
        
        if (!Objects.equals(analysis1.getDetectedEncoding(), analysis2.getDetectedEncoding())) {
            recommendations.add("Files have different encodings - ensure consistent encoding handling");
        }
        
        double sizeDifference = Math.abs(analysis1.getFileSize() - analysis2.getFileSize());
        if (sizeDifference > analysis1.getFileSize() * 0.5) {
            recommendations.add("Significant size difference detected - verify files are related");
        }
        
        if (analysis1.isBinaryFile() != analysis2.isBinaryFile()) {
            recommendations.add("One file is binary, other is text - ensure appropriate processing modes");
        }
        
        return recommendations;
    }
}