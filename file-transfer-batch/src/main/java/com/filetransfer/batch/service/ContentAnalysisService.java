package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.FileType;
import com.filetransfer.batch.entity.FileTransferRecord;
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
 * Batch service for intelligent file content analysis and classification
 */
@Service
public class ContentAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentAnalysisService.class);
    
    // File content patterns for type detection
    private static final Pattern CSV_PATTERN = Pattern.compile("^[^,]*,[^,]*,.*$", Pattern.MULTILINE);
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*[\\{\\[].*[\\}\\]]\\s*$", Pattern.DOTALL);
    private static final Pattern XML_PATTERN = Pattern.compile("^\\s*<\\?xml.*\\?>.*$|^\\s*<[^>]+>.*</[^>]+>\\s*$", Pattern.DOTALL);
    
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
     * Perform lightweight content analysis during batch processing
     */
    public BatchContentAnalysisResult analyzeForBatch(FileTransferRecord record) {
        try {
            String filePath = record.getSourcePath();
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return new BatchContentAnalysisResult(false, "File not found: " + filePath);
            }
            
            long fileSize = Files.size(path);
            boolean isBinary = isBinaryFile(path);
            FileType detectedType = detectFileType(path, record.getFileName());
            String encoding = detectEncoding(path);
            
            BatchContentAnalysisResult result = new BatchContentAnalysisResult(true, "Batch analysis completed");
            result.setFileSize(fileSize);
            result.setDetectedFileType(detectedType);
            result.setDetectedEncoding(encoding);
            result.setBinaryFile(isBinary);
            result.setRecommendedProcessing(getRecommendedProcessing(detectedType, isBinary, fileSize));
            
            // Quick quality assessment
            if (!isBinary && fileSize < 10 * 1024 * 1024) { // Only for text files under 10MB
                QuickQualityAssessment quality = performQuickQualityAssessment(path);
                result.setQualityAssessment(quality);
            }
            
            logger.debug("Batch content analysis completed for file: {}", record.getFileName());
            return result;
            
        } catch (Exception e) {
            logger.error("Error during batch content analysis for file {}: {}", record.getFileName(), e.getMessage());
            return new BatchContentAnalysisResult(false, "Batch analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced file type detection for batch processing
     */
    public FileType enhancedFileTypeDetection(FileTransferRecord record) {
        try {
            BatchContentAnalysisResult analysis = analyzeForBatch(record);
            
            if (analysis.isSuccess() && analysis.getDetectedFileType() != null) {
                // Update the record with detected type if it's more specific than current type
                if (record.getFileType() == FileType.COBOL_FLAT_FILE && 
                    analysis.getDetectedFileType() != FileType.COBOL_FLAT_FILE) {
                    
                    logger.info("Enhanced file type detection: {} -> {} for file: {}", 
                        record.getFileType(), analysis.getDetectedFileType(), record.getFileName());
                    
                    return analysis.getDetectedFileType();
                }
            }
            
            return record.getFileType(); // Keep original if detection doesn't improve it
            
        } catch (Exception e) {
            logger.error("Error in enhanced file type detection for {}: {}", record.getFileName(), e.getMessage());
            return record.getFileType(); // Fallback to original type
        }
    }
    
    /**
     * Check if file should be processed based on content analysis
     */
    public ProcessingDecision shouldProcessFile(FileTransferRecord record) {
        try {
            BatchContentAnalysisResult analysis = analyzeForBatch(record);
            
            ProcessingDecision decision = new ProcessingDecision();
            decision.setShouldProcess(true); // Default to processing
            
            if (!analysis.isSuccess()) {
                decision.setShouldProcess(false);
                decision.setReason("Content analysis failed");
                decision.setRecommendedAction("Manual review required");
                return decision;
            }
            
            // Check for empty files
            if (analysis.getFileSize() == 0) {
                decision.setShouldProcess(false);
                decision.setReason("File is empty");
                decision.setRecommendedAction("Skip processing or investigate source");
                return decision;
            }
            
            // Check quality assessment
            if (analysis.getQualityAssessment() != null) {
                double qualityScore = analysis.getQualityAssessment().getOverallScore();
                
                if (qualityScore < 30) {
                    decision.setShouldProcess(false);
                    decision.setReason("Quality score too low: " + qualityScore + "%");
                    decision.setRecommendedAction("Data quality review required");
                } else if (qualityScore < 60) {
                    decision.setShouldProcess(true);
                    decision.setReason("Quality score below threshold: " + qualityScore + "%");
                    decision.setRecommendedAction("Process with caution - enable additional validation");
                    decision.setRequiresAdditionalValidation(true);
                }
            }
            
            // Check for binary files in text processing workflows
            if (analysis.isBinaryFile() && isTextProcessingWorkflow(record)) {
                decision.setShouldProcess(false);
                decision.setReason("Binary file in text processing workflow");
                decision.setRecommendedAction("Route to binary processing workflow");
            }
            
            return decision;
            
        } catch (Exception e) {
            logger.error("Error determining processing decision for {}: {}", record.getFileName(), e.getMessage());
            
            ProcessingDecision decision = new ProcessingDecision();
            decision.setShouldProcess(true); // Default to processing on error
            decision.setReason("Analysis error - proceeding with default processing");
            decision.setRecommendedAction("Monitor processing carefully");
            return decision;
        }
    }
    
    /**
     * Get processing optimization recommendations
     */
    public ProcessingOptimization getProcessingOptimization(FileTransferRecord record) {
        try {
            BatchContentAnalysisResult analysis = analyzeForBatch(record);
            
            ProcessingOptimization optimization = new ProcessingOptimization();
            
            if (!analysis.isSuccess()) {
                return optimization; // Return defaults
            }
            
            // Compression recommendations
            if (!analysis.isBinaryFile() && analysis.getFileSize() > 1024 * 1024) { // > 1MB
                optimization.setRecommendCompression(true);
                optimization.setCompressionReason("Large text file - compression will improve transfer performance");
            }
            
            // Batch size recommendations
            if (analysis.getDetectedFileType() == FileType.CSV && analysis.getFileSize() > 50 * 1024 * 1024) {
                optimization.setRecommendedBatchSize(1000);
                optimization.setBatchSizeReason("Large CSV file - process in smaller batches");
            } else if (analysis.getDetectedFileType() == FileType.JSON && analysis.getFileSize() > 20 * 1024 * 1024) {
                optimization.setRecommendedBatchSize(100);
                optimization.setBatchSizeReason("Large JSON file - reduce batch size for memory efficiency");
            }
            
            // Memory recommendations
            if (analysis.getFileSize() > 100 * 1024 * 1024) { // > 100MB
                optimization.setRecommendStreamingProcessing(true);
                optimization.setStreamingReason("Large file - use streaming to reduce memory usage");
            }
            
            // Parallel processing recommendations
            if (analysis.getDetectedFileType() == FileType.CSV && analysis.getFileSize() > 10 * 1024 * 1024) {
                optimization.setRecommendParallelProcessing(true);
                optimization.setParallelProcessingReason("Large structured file - suitable for parallel processing");
            }
            
            return optimization;
            
        } catch (Exception e) {
            logger.error("Error generating processing optimization for {}: {}", record.getFileName(), e.getMessage());
            return new ProcessingOptimization(); // Return defaults
        }
    }
    
    // Private helper methods (same as web service but optimized for batch)
    
    private String detectEncoding(Path filePath) throws IOException {
        byte[] buffer = new byte[1024]; // Smaller buffer for batch processing
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
    
    private FileType detectFileType(Path filePath, String fileName) throws IOException {
        // First check if it's binary
        if (isBinaryFile(filePath)) {
            return FileType.BINARY_FILE;
        }
        
        // Read only first 5 lines for quick detection
        List<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)
                .limit(5)
                .collect(Collectors.toList());
        
        if (lines.isEmpty()) {
            return FileType.COBOL_FLAT_FILE;
        }
        
        String content = String.join("\n", lines);
        
        // Quick pattern matching for common types
        if (JSON_PATTERN.matcher(content).matches()) {
            return FileType.JSON;
        }
        
        if (XML_PATTERN.matcher(content).find()) {
            return FileType.XML;
        }
        
        if (CSV_PATTERN.matcher(content).find()) {
            return FileType.CSV;
        }
        
        // Check for delimited patterns
        if (content.contains("|") && content.split("\\|").length > 2) {
            return FileType.DELIMITED;
        }
        
        // Default to COBOL flat file
        return FileType.COBOL_FLAT_FILE;
    }
    
    private boolean isBinaryFile(Path filePath) throws IOException {
        byte[] buffer = new byte[1024]; // Smaller buffer for performance
        try (InputStream is = Files.newInputStream(filePath)) {
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) return false;
            
            // Quick binary signature check
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
            
            // Quick null byte check (first 512 bytes only)
            int nullBytes = 0;
            int checkBytes = Math.min(bytesRead, 512);
            
            for (int i = 0; i < checkBytes; i++) {
                if (buffer[i] == 0) {
                    nullBytes++;
                }
            }
            
            // If more than 1% null bytes, consider it binary
            return (double) nullBytes / checkBytes > 0.01;
        }
    }
    
    private QuickQualityAssessment performQuickQualityAssessment(Path filePath) throws IOException {
        QuickQualityAssessment assessment = new QuickQualityAssessment();
        
        // Read first 100 lines for quick assessment
        List<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)
                .limit(100)
                .collect(Collectors.toList());
        
        if (lines.isEmpty()) {
            assessment.setOverallScore(0.0);
            assessment.addIssue("File is empty");
            return assessment;
        }
        
        // Quick quality checks
        int emptyLines = 0;
        int totalLength = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                emptyLines++;
            }
            totalLength += line.length();
        }
        
        double emptyLinePercentage = (double) emptyLines / lines.size() * 100;
        double avgLineLength = (double) totalLength / lines.size();
        
        // Calculate quick quality score
        double score = 100.0;
        score -= emptyLinePercentage * 0.5; // Small penalty for empty lines
        
        if (avgLineLength < 10) {
            score -= 20.0; // Penalty for very short lines
            assessment.addIssue("Average line length is very short");
        }
        
        if (emptyLinePercentage > 20) {
            assessment.addIssue("High percentage of empty lines: " + String.format("%.1f%%", emptyLinePercentage));
        }
        
        assessment.setOverallScore(Math.max(0.0, Math.min(100.0, score)));
        assessment.setEmptyLinePercentage(emptyLinePercentage);
        assessment.setAverageLineLength(avgLineLength);
        assessment.setSampleSize(lines.size());
        
        return assessment;
    }
    
    private String getRecommendedProcessing(FileType detectedType, boolean isBinary, long fileSize) {
        if (isBinary) {
            return "BINARY_PROCESSING";
        }
        
        if (fileSize > 100 * 1024 * 1024) { // > 100MB
            return "STREAMING_PROCESSING";
        }
        
        switch (detectedType) {
            case CSV:
            case DELIMITED:
                return fileSize > 10 * 1024 * 1024 ? "BATCH_PROCESSING" : "STANDARD_PROCESSING";
            case JSON:
            case XML:
                return fileSize > 5 * 1024 * 1024 ? "STREAMING_PROCESSING" : "STANDARD_PROCESSING";
            default:
                return "STANDARD_PROCESSING";
        }
    }
    
    private boolean isTextProcessingWorkflow(FileTransferRecord record) {
        // Determine if this is a text processing workflow based on service configuration
        String serviceName = record.getServiceName();
        
        return serviceName != null && (
            serviceName.contains("TEXT") || 
            serviceName.contains("CSV") || 
            serviceName.contains("DATA") ||
            serviceName.contains("REPORT")
        );
    }
    
    // Result classes
    
    public static class BatchContentAnalysisResult {
        private boolean success;
        private String message;
        private long fileSize;
        private FileType detectedFileType;
        private String detectedEncoding;
        private boolean isBinaryFile;
        private String recommendedProcessing;
        private QuickQualityAssessment qualityAssessment;
        private Date analysisTimestamp;
        
        public BatchContentAnalysisResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.analysisTimestamp = new Date();
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public FileType getDetectedFileType() { return detectedFileType; }
        public void setDetectedFileType(FileType detectedFileType) { this.detectedFileType = detectedFileType; }
        
        public String getDetectedEncoding() { return detectedEncoding; }
        public void setDetectedEncoding(String detectedEncoding) { this.detectedEncoding = detectedEncoding; }
        
        public boolean isBinaryFile() { return isBinaryFile; }
        public void setBinaryFile(boolean binaryFile) { this.isBinaryFile = binaryFile; }
        
        public String getRecommendedProcessing() { return recommendedProcessing; }
        public void setRecommendedProcessing(String recommendedProcessing) { this.recommendedProcessing = recommendedProcessing; }
        
        public QuickQualityAssessment getQualityAssessment() { return qualityAssessment; }
        public void setQualityAssessment(QuickQualityAssessment qualityAssessment) { this.qualityAssessment = qualityAssessment; }
        
        public Date getAnalysisTimestamp() { return analysisTimestamp; }
        public void setAnalysisTimestamp(Date analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
    }
    
    public static class QuickQualityAssessment {
        private double overallScore;
        private double emptyLinePercentage;
        private double averageLineLength;
        private int sampleSize;
        private List<String> issues = new ArrayList<>();
        
        public void addIssue(String issue) {
            this.issues.add(issue);
        }
        
        // Getters and Setters
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public double getEmptyLinePercentage() { return emptyLinePercentage; }
        public void setEmptyLinePercentage(double emptyLinePercentage) { this.emptyLinePercentage = emptyLinePercentage; }
        
        public double getAverageLineLength() { return averageLineLength; }
        public void setAverageLineLength(double averageLineLength) { this.averageLineLength = averageLineLength; }
        
        public int getSampleSize() { return sampleSize; }
        public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
        
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
    }
    
    public static class ProcessingDecision {
        private boolean shouldProcess = true;
        private String reason;
        private String recommendedAction;
        private boolean requiresAdditionalValidation = false;
        private boolean requiresManualReview = false;
        
        // Getters and Setters
        public boolean isShouldProcess() { return shouldProcess; }
        public void setShouldProcess(boolean shouldProcess) { this.shouldProcess = shouldProcess; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        
        public boolean isRequiresAdditionalValidation() { return requiresAdditionalValidation; }
        public void setRequiresAdditionalValidation(boolean requiresAdditionalValidation) { this.requiresAdditionalValidation = requiresAdditionalValidation; }
        
        public boolean isRequiresManualReview() { return requiresManualReview; }
        public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }
    }
    
    public static class ProcessingOptimization {
        private boolean recommendCompression = false;
        private String compressionReason;
        private int recommendedBatchSize = 500; // Default batch size
        private String batchSizeReason;
        private boolean recommendStreamingProcessing = false;
        private String streamingReason;
        private boolean recommendParallelProcessing = false;
        private String parallelProcessingReason;
        
        // Getters and Setters
        public boolean isRecommendCompression() { return recommendCompression; }
        public void setRecommendCompression(boolean recommendCompression) { this.recommendCompression = recommendCompression; }
        
        public String getCompressionReason() { return compressionReason; }
        public void setCompressionReason(String compressionReason) { this.compressionReason = compressionReason; }
        
        public int getRecommendedBatchSize() { return recommendedBatchSize; }
        public void setRecommendedBatchSize(int recommendedBatchSize) { this.recommendedBatchSize = recommendedBatchSize; }
        
        public String getBatchSizeReason() { return batchSizeReason; }
        public void setBatchSizeReason(String batchSizeReason) { this.batchSizeReason = batchSizeReason; }
        
        public boolean isRecommendStreamingProcessing() { return recommendStreamingProcessing; }
        public void setRecommendStreamingProcessing(boolean recommendStreamingProcessing) { this.recommendStreamingProcessing = recommendStreamingProcessing; }
        
        public String getStreamingReason() { return streamingReason; }
        public void setStreamingReason(String streamingReason) { this.streamingReason = streamingReason; }
        
        public boolean isRecommendParallelProcessing() { return recommendParallelProcessing; }
        public void setRecommendParallelProcessing(boolean recommendParallelProcessing) { this.recommendParallelProcessing = recommendParallelProcessing; }
        
        public String getParallelProcessingReason() { return parallelProcessingReason; }
        public void setParallelProcessingReason(String parallelProcessingReason) { this.parallelProcessingReason = parallelProcessingReason; }
    }
}