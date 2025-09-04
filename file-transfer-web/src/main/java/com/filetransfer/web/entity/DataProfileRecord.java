package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing data profiling results
 */
@Entity
@Table(name = "data_profile_records")
public class DataProfileRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "file_transfer_id", nullable = false)
    private Long fileTransferId;
    
    @Column(name = "content_analysis_id")
    private Long contentAnalysisId;
    
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "record_count")
    private Integer recordCount;
    
    @Column(name = "column_count")
    private Integer columnCount;
    
    @Column(name = "data_statistics", columnDefinition = "TEXT")
    private String dataStatistics; // JSON string
    
    @Column(name = "column_profiles", columnDefinition = "TEXT")
    private String columnProfiles; // JSON array string
    
    @Column(name = "quality_insights", columnDefinition = "TEXT")
    private String qualityInsights; // JSON array string
    
    @Column(name = "processing_recommendations", columnDefinition = "TEXT")
    private String processingRecommendations; // JSON array string
    
    @Column(name = "data_completeness_score")
    private Double dataCompletenessScore;
    
    @Column(name = "data_uniqueness_score")
    private Double dataUniquenessScore;
    
    @Column(name = "data_consistency_score")
    private Double dataConsistencyScore;
    
    @Column(name = "overall_quality_score")
    private Double overallQualityScore;
    
    @Column(name = "profiling_duration_ms")
    private Long profilingDurationMs;
    
    @Column(name = "profiling_version", length = 20)
    private String profilingVersion = "1.0";
    
    @Column(name = "auto_profiling")
    private Boolean autoProfiling = false;
    
    @Column(name = "profiling_depth", length = 20)
    private String profilingDepth = "STANDARD"; // BASIC, STANDARD, DEEP
    
    @Column(name = "sample_size")
    private Integer sampleSize;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public DataProfileRecord() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DataProfileRecord(String tenantId, Long fileTransferId, String fileName, String filePath) {
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
    
    public Long getContentAnalysisId() { return contentAnalysisId; }
    public void setContentAnalysisId(Long contentAnalysisId) { this.contentAnalysisId = contentAnalysisId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    
    public Integer getColumnCount() { return columnCount; }
    public void setColumnCount(Integer columnCount) { this.columnCount = columnCount; }
    
    public String getDataStatistics() { return dataStatistics; }
    public void setDataStatistics(String dataStatistics) { this.dataStatistics = dataStatistics; }
    
    public String getColumnProfiles() { return columnProfiles; }
    public void setColumnProfiles(String columnProfiles) { this.columnProfiles = columnProfiles; }
    
    public String getQualityInsights() { return qualityInsights; }
    public void setQualityInsights(String qualityInsights) { this.qualityInsights = qualityInsights; }
    
    public String getProcessingRecommendations() { return processingRecommendations; }
    public void setProcessingRecommendations(String processingRecommendations) { this.processingRecommendations = processingRecommendations; }
    
    public Double getDataCompletenessScore() { return dataCompletenessScore; }
    public void setDataCompletenessScore(Double dataCompletenessScore) { this.dataCompletenessScore = dataCompletenessScore; }
    
    public Double getDataUniquenessScore() { return dataUniquenessScore; }
    public void setDataUniquenessScore(Double dataUniquenessScore) { this.dataUniquenessScore = dataUniquenessScore; }
    
    public Double getDataConsistencyScore() { return dataConsistencyScore; }
    public void setDataConsistencyScore(Double dataConsistencyScore) { this.dataConsistencyScore = dataConsistencyScore; }
    
    public Double getOverallQualityScore() { return overallQualityScore; }
    public void setOverallQualityScore(Double overallQualityScore) { this.overallQualityScore = overallQualityScore; }
    
    public Long getProfilingDurationMs() { return profilingDurationMs; }
    public void setProfilingDurationMs(Long profilingDurationMs) { this.profilingDurationMs = profilingDurationMs; }
    
    public String getProfilingVersion() { return profilingVersion; }
    public void setProfilingVersion(String profilingVersion) { this.profilingVersion = profilingVersion; }
    
    public Boolean getAutoProfiling() { return autoProfiling; }
    public void setAutoProfiling(Boolean autoProfiling) { this.autoProfiling = autoProfiling; }
    
    public String getProfilingDepth() { return profilingDepth; }
    public void setProfilingDepth(String profilingDepth) { this.profilingDepth = profilingDepth; }
    
    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer sampleSize) { this.sampleSize = sampleSize; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}