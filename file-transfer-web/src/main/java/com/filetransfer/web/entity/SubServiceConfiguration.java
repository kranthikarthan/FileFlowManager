package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sub_service_configurations")
public class SubServiceConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    @Column(nullable = false)
    @NotBlank(message = "Sub-service name is required")
    private String subServiceName;
    
    @Column(nullable = false)
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @Column(nullable = false)
    @NotBlank(message = "Inbound path is required")
    private String inboundPath;
    
    @Column(nullable = false)
    @NotBlank(message = "Outbound path is required")
    private String outboundPath;
    
    @Column(nullable = false)
    @NotNull
    private Boolean enabled = true;
    
    // Cut-off time configuration (moved from ServiceConfiguration)
    @Column(nullable = false)
    private String cutOffTime = "23:59:59";
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CutOffTimeType cutOffTimeType = CutOffTimeType.DAILY;
    
    // Weekday vs Weekend cut-off times
    @Column
    private String weekdayCutOffTime;
    
    @Column
    private String weekendCutOffTime;
    
    // Individual day cut-off times
    @Column
    private String mondayCutOffTime;
    
    @Column
    private String tuesdayCutOffTime;
    
    @Column
    private String wednesdayCutOffTime;
    
    @Column
    private String thursdayCutOffTime;
    
    @Column
    private String fridayCutOffTime;
    
    @Column
    private String saturdayCutOffTime;
    
    @Column
    private String sundayCutOffTime;
    
    // Holiday configuration
    @Column(nullable = false)
    private Boolean allSundaysAsHolidays = false;
    
    // File type and direction specific schema configurations
    @OneToMany(mappedBy = "subServiceConfiguration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileTypeSchemaMapping> fileTypeSchemaMappings;
    
    @ElementCollection
    @CollectionTable(name = "sub_service_direction_configs", 
                    joinColumns = @JoinColumn(name = "sub_service_config_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "direction")
    @Column(name = "config_json", columnDefinition = "TEXT")
    private Map<TransferDirection, String> directionConfigs;
    
    // SOT/EOT marker configuration
    @Column(nullable = false)
    private String startMarkerPrefix = "SOT_";
    
    @Column(nullable = false)
    private String endMarkerPrefix = "EOT_";
    
    // File naming patterns
    @Column(nullable = false)
    private String dataFilePattern = "*.*";
    
    @Column
    private String sotFilePattern = "SOT_*";
    
    @Column
    private String eotFilePattern = "EOT_*";
    
    // Validation settings
    @Column(nullable = false)
    private Boolean schemaValidationEnabled = true;
    
    @Column(nullable = false)
    private Boolean binaryFileBypass = true;
    
    @Column
    private String schemaValidationMode = "STRICT"; // STRICT, LENIENT, WARNING_ONLY
    
    // Processing settings
    @Column(nullable = false)
    private Integer maxRetries = 3;
    
    @Column(nullable = false)
    private Integer pollIntervalSeconds = 30;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    // Constructors
    public SubServiceConfiguration() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SubServiceConfiguration(String tenantId, String serviceName, String subServiceName, 
                                 String inboundPath, String outboundPath) {
        this();
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.inboundPath = inboundPath;
        this.outboundPath = outboundPath;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getSubServiceName() { return subServiceName; }
    public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getInboundPath() { return inboundPath; }
    public void setInboundPath(String inboundPath) { this.inboundPath = inboundPath; }
    
    public String getOutboundPath() { return outboundPath; }
    public void setOutboundPath(String outboundPath) { this.outboundPath = outboundPath; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getCutOffTime() { return cutOffTime; }
    public void setCutOffTime(String cutOffTime) { this.cutOffTime = cutOffTime; }
    
    public CutOffTimeType getCutOffTimeType() { return cutOffTimeType; }
    public void setCutOffTimeType(CutOffTimeType cutOffTimeType) { this.cutOffTimeType = cutOffTimeType; }
    
    public String getWeekdayCutOffTime() { return weekdayCutOffTime; }
    public void setWeekdayCutOffTime(String weekdayCutOffTime) { this.weekdayCutOffTime = weekdayCutOffTime; }
    
    public String getWeekendCutOffTime() { return weekendCutOffTime; }
    public void setWeekendCutOffTime(String weekendCutOffTime) { this.weekendCutOffTime = weekendCutOffTime; }
    
    public String getMondayCutOffTime() { return mondayCutOffTime; }
    public void setMondayCutOffTime(String mondayCutOffTime) { this.mondayCutOffTime = mondayCutOffTime; }
    
    public String getTuesdayCutOffTime() { return tuesdayCutOffTime; }
    public void setTuesdayCutOffTime(String tuesdayCutOffTime) { this.tuesdayCutOffTime = tuesdayCutOffTime; }
    
    public String getWednesdayCutOffTime() { return wednesdayCutOffTime; }
    public void setWednesdayCutOffTime(String wednesdayCutOffTime) { this.wednesdayCutOffTime = wednesdayCutOffTime; }
    
    public String getThursdayCutOffTime() { return thursdayCutOffTime; }
    public void setThursdayCutOffTime(String thursdayCutOffTime) { this.thursdayCutOffTime = thursdayCutOffTime; }
    
    public String getFridayCutOffTime() { return fridayCutOffTime; }
    public void setFridayCutOffTime(String fridayCutOffTime) { this.fridayCutOffTime = fridayCutOffTime; }
    
    public String getSaturdayCutOffTime() { return saturdayCutOffTime; }
    public void setSaturdayCutOffTime(String saturdayCutOffTime) { this.saturdayCutOffTime = saturdayCutOffTime; }
    
    public String getSundayCutOffTime() { return sundayCutOffTime; }
    public void setSundayCutOffTime(String sundayCutOffTime) { this.sundayCutOffTime = sundayCutOffTime; }
    
    public Boolean getAllSundaysAsHolidays() { return allSundaysAsHolidays; }
    public void setAllSundaysAsHolidays(Boolean allSundaysAsHolidays) { this.allSundaysAsHolidays = allSundaysAsHolidays; }
    
    public List<FileTypeSchemaMapping> getFileTypeSchemaMappings() { return fileTypeSchemaMappings; }
    public void setFileTypeSchemaMappings(List<FileTypeSchemaMapping> fileTypeSchemaMappings) { this.fileTypeSchemaMappings = fileTypeSchemaMappings; }
    
    public Map<TransferDirection, String> getDirectionConfigs() { return directionConfigs; }
    public void setDirectionConfigs(Map<TransferDirection, String> directionConfigs) { this.directionConfigs = directionConfigs; }
    
    public String getStartMarkerPrefix() { return startMarkerPrefix; }
    public void setStartMarkerPrefix(String startMarkerPrefix) { this.startMarkerPrefix = startMarkerPrefix; }
    
    public String getEndMarkerPrefix() { return endMarkerPrefix; }
    public void setEndMarkerPrefix(String endMarkerPrefix) { this.endMarkerPrefix = endMarkerPrefix; }
    
    public String getDataFilePattern() { return dataFilePattern; }
    public void setDataFilePattern(String dataFilePattern) { this.dataFilePattern = dataFilePattern; }
    
    public String getSotFilePattern() { return sotFilePattern; }
    public void setSotFilePattern(String sotFilePattern) { this.sotFilePattern = sotFilePattern; }
    
    public String getEotFilePattern() { return eotFilePattern; }
    public void setEotFilePattern(String eotFilePattern) { this.eotFilePattern = eotFilePattern; }
    
    public Boolean getSchemaValidationEnabled() { return schemaValidationEnabled; }
    public void setSchemaValidationEnabled(Boolean schemaValidationEnabled) { this.schemaValidationEnabled = schemaValidationEnabled; }
    
    public Boolean getBinaryFileBypass() { return binaryFileBypass; }
    public void setBinaryFileBypass(Boolean binaryFileBypass) { this.binaryFileBypass = binaryFileBypass; }
    
    public String getSchemaValidationMode() { return schemaValidationMode; }
    public void setSchemaValidationMode(String schemaValidationMode) { this.schemaValidationMode = schemaValidationMode; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public Integer getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(Integer pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    /**
     * Get schema ID for specific file type and direction
     */
    public Long getSchemaIdForFileType(FileType fileType, TransferDirection direction) {
        if (fileType == FileType.BINARY_FILE && binaryFileBypass) {
            return null; // No schema validation for binary files when bypass is enabled
        }
        
        if (fileTypeSchemaMappings == null) {
            return null;
        }
        
        return fileTypeSchemaMappings.stream()
            .filter(mapping -> mapping.getFileType() == fileType)
            .findFirst()
            .map(mapping -> mapping.getSchemaIdForDirection(direction))
            .orElse(null);
    }
    
    /**
     * Check if schema validation is required for file type
     */
    public boolean requiresSchemaValidation(FileType fileType) {
        if (fileType == FileType.BINARY_FILE && binaryFileBypass) {
            return false;
        }
        
        return schemaValidationEnabled && fileType.requiresSchemaValidation();
    }
    
    /**
     * Check if schema validation is required for file type and direction
     */
    public boolean requiresSchemaValidation(FileType fileType, TransferDirection direction) {
        if (fileType == FileType.BINARY_FILE && binaryFileBypass) {
            return false;
        }
        
        if (!schemaValidationEnabled || !fileType.requiresSchemaValidation()) {
            return false;
        }
        
        if (fileTypeSchemaMappings == null) {
            return false;
        }
        
        return fileTypeSchemaMappings.stream()
            .filter(mapping -> mapping.getFileType() == fileType)
            .findFirst()
            .map(mapping -> mapping.isValidationEnabledForDirection(direction))
            .orElse(false);
    }
    
    /**
     * Add or update file type schema mapping
     */
    public void addFileTypeSchemaMapping(FileType fileType, Long inboundSchemaId, Long outboundSchemaId) {
        if (fileTypeSchemaMappings == null) {
            fileTypeSchemaMappings = new java.util.ArrayList<>();
        }
        
        // Remove existing mapping for this file type
        fileTypeSchemaMappings.removeIf(mapping -> mapping.getFileType() == fileType);
        
        // Add new mapping
        FileTypeSchemaMapping mapping = new FileTypeSchemaMapping(this, fileType, inboundSchemaId, outboundSchemaId);
        fileTypeSchemaMappings.add(mapping);
    }
}