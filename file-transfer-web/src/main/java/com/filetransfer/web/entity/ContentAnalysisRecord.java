package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing file content analysis results
 */
@Entity
@Table(name = "content_analysis_records")
public class ContentAnalysisRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "file_transfer_id", nullable = false)
    private Long fileTransferId;
    
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_file_type")
    private FileType detectedFileType;
    
    @Column(name = "detected_encoding", length = 50)
    private String detectedEncoding;
    
    @Column(name = "is_binary_file")
    private Boolean isBinaryFile = false;
    
    @Column(name = "is_empty_file")
    private Boolean isEmptyFile = false;
    
    @Column(name = "line_count")
    private Integer lineCount;
    
    @Column(name = "total_characters")
    private Integer totalCharacters;
    
    @Column(name = "average_line_length")
    private Double averageLineLength;
    
    @Column(name = "max_line_length")
    private Integer maxLineLength;
    
    @Column(name = "min_line_length")
    private Integer minLineLength;
    
    @Column(name = "record_count")
    private Integer recordCount;
    
    @Column(name = "column_count")
    private Integer columnCount;
    
    @Column(name = "structure_analysis", columnDefinition = "TEXT")
    private String structureAnalysis; // JSON string
    
    @Column(name = "quality_score")
    private Double qualityScore;
    
    @Column(name = "quality_issues", columnDefinition = "TEXT")
    private String qualityIssues; // JSON array string
    
    @Column(name = "quality_recommendations", columnDefinition = "TEXT")
    private String qualityRecommendations; // JSON array string
    
    @Column(name = "processing_recommendations", columnDefinition = "TEXT")
    private String processingRecommendations; // JSON array string
    
    @Column(name = "analysis_duration_ms")
    private Long analysisDurationMs;
    
    @Column(name = "analysis_version", length = 20)
    private String analysisVersion = "1.0";
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ContentAnalysisRecord() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ContentAnalysisRecord(String tenantId, Long fileTransferId, String fileName, String filePath) {
        this();
        this.tenantId = tenantId;
        this.fileTransferId = fileTransferId;
        this.fileName = fileName;
        this.filePath = filePath;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public Long getFileTransferId() { return fileTransferId; }
    public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public FileType getDetectedFileType() { return detectedFileType; }
    public void setDetectedFileType(FileType detectedFileType) { this.detectedFileType = detectedFileType; }
    
    public String getDetectedEncoding() { return detectedEncoding; }
    public void setDetectedEncoding(String detectedEncoding) { this.detectedEncoding = detectedEncoding; }
    
    public Boolean getIsBinaryFile() { return isBinaryFile; }
    public void setIsBinaryFile(Boolean isBinaryFile) { this.isBinaryFile = isBinaryFile; }
    
    public Boolean getIsEmptyFile() { return isEmptyFile; }
    public void setIsEmptyFile(Boolean isEmptyFile) { this.isEmptyFile = isEmptyFile; }
    
    public Integer getLineCount() { return lineCount; }
    public void setLineCount(Integer lineCount) { this.lineCount = lineCount; }
    
    public Integer getTotalCharacters() { return totalCharacters; }
    public void setTotalCharacters(Integer totalCharacters) { this.totalCharacters = totalCharacters; }
    
    public Double getAverageLineLength() { return averageLineLength; }
    public void setAverageLineLength(Double averageLineLength) { this.averageLineLength = averageLineLength; }
    
    public Integer getMaxLineLength() { return maxLineLength; }
    public void setMaxLineLength(Integer maxLineLength) { this.maxLineLength = maxLineLength; }
    
    public Integer getMinLineLength() { return minLineLength; }
    public void setMinLineLength(Integer minLineLength) { this.minLineLength = minLineLength; }
    
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    
    public Integer getColumnCount() { return columnCount; }
    public void setColumnCount(Integer columnCount) { this.columnCount = columnCount; }
    
    public String getStructureAnalysis() { return structureAnalysis; }
    public void setStructureAnalysis(String structureAnalysis) { this.structureAnalysis = structureAnalysis; }
    
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    
    public String getQualityIssues() { return qualityIssues; }
    public void setQualityIssues(String qualityIssues) { this.qualityIssues = qualityIssues; }
    
    public String getQualityRecommendations() { return qualityRecommendations; }
    public void setQualityRecommendations(String qualityRecommendations) { this.qualityRecommendations = qualityRecommendations; }
    
    public String getProcessingRecommendations() { return processingRecommendations; }
    public void setProcessingRecommendations(String processingRecommendations) { this.processingRecommendations = processingRecommendations; }
    
    public Long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(Long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; }
    
    public String getAnalysisVersion() { return analysisVersion; }
    public void setAnalysisVersion(String analysisVersion) { this.analysisVersion = analysisVersion; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}