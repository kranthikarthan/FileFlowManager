package com.filetransfer.web.dto;

import com.filetransfer.web.entity.CutOffTimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ServiceConfigurationDto {
    
    private Long id;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String subServiceName;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @NotBlank(message = "Inbound path is required")
    private String inboundPath;
    
    @NotBlank(message = "Outbound path is required")
    private String outboundPath;
    
    private String startMarkerPrefix = "SOT_";
    private String endMarkerPrefix = "EOT_";
    private String dataFilePattern = "*.*";
    
    @NotNull
    private Boolean enabled = true;
    
    private Integer maxRetries = 3;
    private Integer pollIntervalSeconds = 30;
    
    // Enhanced cut-off time configuration
    private String cutOffTime = "23:59:59";
    
    @NotNull
    private CutOffTimeType cutOffTimeType = CutOffTimeType.DAILY;
    
    // Weekday vs Weekend cut-off times
    private String weekdayCutOffTime;
    private String weekendCutOffTime;
    
    // Individual day cut-off times
    private String mondayCutOffTime;
    private String tuesdayCutOffTime;
    private String wednesdayCutOffTime;
    private String thursdayCutOffTime;
    private String fridayCutOffTime;
    private String saturdayCutOffTime;
    private String sundayCutOffTime;
    
    // Holiday configuration
    private Boolean allSundaysAsHolidays = false;
    
    // Validation regex patterns
    private String sotFileValidationRegex;
    private String eotFileValidationRegex;
    private String dataFileValidationRegex;
    
    // Schema validation settings
    private Boolean schemaValidationEnabled = false;
    private Long schemaId;
    private String schemaValidationMode = "STRICT"; // STRICT, LENIENT, WARNING_ONLY
    private Boolean binaryFileBypass = false;
    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Constructors
    public ServiceConfigurationDto() {}
    
    public ServiceConfigurationDto(String serviceName, String subServiceName, String tenantId, 
                                  String inboundPath, String outboundPath) {
        this.serviceName = serviceName;
        this.subServiceName = subServiceName;
        this.tenantId = tenantId;
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
    
    public String getStartMarkerPrefix() { return startMarkerPrefix; }
    public void setStartMarkerPrefix(String startMarkerPrefix) { this.startMarkerPrefix = startMarkerPrefix; }
    
    public String getEndMarkerPrefix() { return endMarkerPrefix; }
    public void setEndMarkerPrefix(String endMarkerPrefix) { this.endMarkerPrefix = endMarkerPrefix; }
    
    public String getDataFilePattern() { return dataFilePattern; }
    public void setDataFilePattern(String dataFilePattern) { this.dataFilePattern = dataFilePattern; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public Integer getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(Integer pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
    
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
    
    public String getSotFileValidationRegex() { return sotFileValidationRegex; }
    public void setSotFileValidationRegex(String sotFileValidationRegex) { this.sotFileValidationRegex = sotFileValidationRegex; }
    
    public String getEotFileValidationRegex() { return eotFileValidationRegex; }
    public void setEotFileValidationRegex(String eotFileValidationRegex) { this.eotFileValidationRegex = eotFileValidationRegex; }
    
    public String getDataFileValidationRegex() { return dataFileValidationRegex; }
    public void setDataFileValidationRegex(String dataFileValidationRegex) { this.dataFileValidationRegex = dataFileValidationRegex; }
    
    public Boolean getSchemaValidationEnabled() { return schemaValidationEnabled; }
    public void setSchemaValidationEnabled(Boolean schemaValidationEnabled) { this.schemaValidationEnabled = schemaValidationEnabled; }
    
    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    
    public String getSchemaValidationMode() { return schemaValidationMode; }
    public void setSchemaValidationMode(String schemaValidationMode) { this.schemaValidationMode = schemaValidationMode; }
    
    public Boolean getBinaryFileBypass() { return binaryFileBypass; }
    public void setBinaryFileBypass(Boolean binaryFileBypass) { this.binaryFileBypass = binaryFileBypass; }
    
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
}