package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "schema_usage_log")
public class SchemaUsageLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", nullable = false)
    private FileSchema schema;
    
    @Column(name = "file_transfer_id")
    private Long fileTransferId;
    
    @NotBlank
    @Column(name = "validation_result", nullable = false, length = 20)
    private String validationResult; // PASSED, FAILED, ERROR
    
    @Column(name = "validation_details", columnDefinition = "TEXT")
    private String validationDetails;
    
    @NotNull
    @Column(name = "validation_timestamp", nullable = false)
    private LocalDateTime validationTimestamp = LocalDateTime.now();
    
    @Column(name = "file_name", length = 500)
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    // Constructors
    public SchemaUsageLog() {}
    
    public SchemaUsageLog(FileSchema schema, String validationResult, String fileName, Long fileSize) {
        this.schema = schema;
        this.validationResult = validationResult;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public FileSchema getSchema() { return schema; }
    public void setSchema(FileSchema schema) { this.schema = schema; }
    
    public Long getFileTransferId() { return fileTransferId; }
    public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
    
    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }
    
    public String getValidationDetails() { return validationDetails; }
    public void setValidationDetails(String validationDetails) { this.validationDetails = validationDetails; }
    
    public LocalDateTime getValidationTimestamp() { return validationTimestamp; }
    public void setValidationTimestamp(LocalDateTime validationTimestamp) { this.validationTimestamp = validationTimestamp; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}