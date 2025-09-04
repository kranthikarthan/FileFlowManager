package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for storing schema validation results
 */
@Entity
@Table(name = "schema_validation_records")
public class SchemaValidationRecord {
    
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
    
    @Column(name = "schema_path", length = 1000)
    private String schemaPath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;
    
    @Column(name = "validation_passed")
    private Boolean validationPassed = false;
    
    @Column(name = "error_count")
    private Integer errorCount = 0;
    
    @Column(name = "warning_count")
    private Integer warningCount = 0;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors; // JSON array string
    
    @Column(name = "validation_warnings", columnDefinition = "TEXT")
    private String validationWarnings; // JSON array string
    
    @Column(name = "schema_compliance_score")
    private Double schemaComplianceScore;
    
    @Column(name = "validation_duration_ms")
    private Long validationDurationMs;
    
    @Column(name = "validation_engine", length = 50)
    private String validationEngine = "INTERNAL";
    
    @Column(name = "validation_version", length = 20)
    private String validationVersion = "1.0";
    
    @Column(name = "auto_validation")
    private Boolean autoValidation = false;
    
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules; // JSON string
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public SchemaValidationRecord() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SchemaValidationRecord(String tenantId, Long fileTransferId, String fileName, String filePath) {
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
    
    public String getSchemaPath() { return schemaPath; }
    public void setSchemaPath(String schemaPath) { this.schemaPath = schemaPath; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public Boolean getValidationPassed() { return validationPassed; }
    public void setValidationPassed(Boolean validationPassed) { this.validationPassed = validationPassed; }
    
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    
    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
    
    public String getValidationErrors() { return validationErrors; }
    public void setValidationErrors(String validationErrors) { this.validationErrors = validationErrors; }
    
    public String getValidationWarnings() { return validationWarnings; }
    public void setValidationWarnings(String validationWarnings) { this.validationWarnings = validationWarnings; }
    
    public Double getSchemaComplianceScore() { return schemaComplianceScore; }
    public void setSchemaComplianceScore(Double schemaComplianceScore) { this.schemaComplianceScore = schemaComplianceScore; }
    
    public Long getValidationDurationMs() { return validationDurationMs; }
    public void setValidationDurationMs(Long validationDurationMs) { this.validationDurationMs = validationDurationMs; }
    
    public String getValidationEngine() { return validationEngine; }
    public void setValidationEngine(String validationEngine) { this.validationEngine = validationEngine; }
    
    public String getValidationVersion() { return validationVersion; }
    public void setValidationVersion(String validationVersion) { this.validationVersion = validationVersion; }
    
    public Boolean getAutoValidation() { return autoValidation; }
    public void setAutoValidation(Boolean autoValidation) { this.autoValidation = autoValidation; }
    
    public String getValidationRules() { return validationRules; }
    public void setValidationRules(String validationRules) { this.validationRules = validationRules; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}