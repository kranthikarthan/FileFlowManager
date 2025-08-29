package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "sub_service_file_type_configs")
public class FileTypeSchemaMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_service_config_id", nullable = false)
    private SubServiceConfiguration subServiceConfiguration;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    @NotNull
    private FileType fileType;
    
    @Column(name = "inbound_schema_id")
    private Long inboundSchemaId;
    
    @Column(name = "outbound_schema_id")
    private Long outboundSchemaId;
    
    @Column(name = "validation_enabled")
    private Boolean validationEnabled = true;
    
    @Column(name = "binary_bypass_enabled")
    private Boolean binaryBypassEnabled = false;
    
    // Constructors
    public FileTypeSchemaMapping() {}
    
    public FileTypeSchemaMapping(SubServiceConfiguration subServiceConfiguration, 
                               FileType fileType, Long inboundSchemaId, Long outboundSchemaId) {
        this.subServiceConfiguration = subServiceConfiguration;
        this.fileType = fileType;
        this.inboundSchemaId = inboundSchemaId;
        this.outboundSchemaId = outboundSchemaId;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SubServiceConfiguration getSubServiceConfiguration() { return subServiceConfiguration; }
    public void setSubServiceConfiguration(SubServiceConfiguration subServiceConfiguration) { this.subServiceConfiguration = subServiceConfiguration; }
    
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    
    public Long getInboundSchemaId() { return inboundSchemaId; }
    public void setInboundSchemaId(Long inboundSchemaId) { this.inboundSchemaId = inboundSchemaId; }
    
    public Long getOutboundSchemaId() { return outboundSchemaId; }
    public void setOutboundSchemaId(Long outboundSchemaId) { this.outboundSchemaId = outboundSchemaId; }
    
    public Boolean getValidationEnabled() { return validationEnabled; }
    public void setValidationEnabled(Boolean validationEnabled) { this.validationEnabled = validationEnabled; }
    
    public Boolean getBinaryBypassEnabled() { return binaryBypassEnabled; }
    public void setBinaryBypassEnabled(Boolean binaryBypassEnabled) { this.binaryBypassEnabled = binaryBypassEnabled; }
    
    /**
     * Get schema ID for specific direction
     */
    public Long getSchemaIdForDirection(TransferDirection direction) {
        if (fileType == FileType.BINARY_FILE && binaryBypassEnabled) {
            return null; // No schema validation for binary files when bypass is enabled
        }
        
        switch (direction) {
            case INBOUND:
                return inboundSchemaId;
            case OUTBOUND:
                return outboundSchemaId;
            default:
                return inboundSchemaId; // Default to inbound
        }
    }
    
    /**
     * Check if validation is enabled for this file type and direction
     */
    public boolean isValidationEnabledForDirection(TransferDirection direction) {
        if (fileType == FileType.BINARY_FILE && binaryBypassEnabled) {
            return false;
        }
        
        if (!validationEnabled) {
            return false;
        }
        
        Long schemaId = getSchemaIdForDirection(direction);
        return schemaId != null;
    }
}