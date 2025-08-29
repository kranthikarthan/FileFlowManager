package com.filetransfer.web.dto.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Tenant DTO for API v2.0+
 * Includes additional fields, validation, and metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantV2Dto {
    
    private Long id;
    
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Description is required in API v2.0+")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotBlank(message = "Timezone is required in API v2.0+")
    @Pattern(regexp = "^[A-Za-z_/]+$", message = "Invalid timezone format")
    private String timezone;
    
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String contactPhone;
    
    private Boolean active = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
    
    // Enhanced fields for v2.0+
    private TenantStatus status;
    private TenantType type;
    private String region;
    private String currency;
    private String language;
    
    // Configuration and settings
    private Map<String, Object> configuration;
    private Map<String, Object> features;
    private Map<String, Object> limits;
    
    // Relationships and references
    private Long parentTenantId;
    private List<Long> childTenantIds;
    private List<String> tags;
    
    // Metrics and statistics (read-only)
    private TenantMetrics metrics;
    
    // Security and compliance
    private String dataClassification;
    private String complianceLevel;
    private List<String> certifications;
    
    // API version metadata
    private String apiVersion;
    private LocalDateTime lastApiAccess;
    
    /**
     * Tenant status enumeration
     */
    public enum TenantStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_ACTIVATION,
        ARCHIVED
    }
    
    /**
     * Tenant type enumeration
     */
    public enum TenantType {
        ENTERPRISE,
        STANDARD,
        TRIAL,
        PARTNER,
        INTERNAL
    }
    
    /**
     * Tenant metrics (read-only)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TenantMetrics {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalServices;
        private Long activeServices;
        private Long totalFileTransfers;
        private Long totalDataVolume;
        private Double successRate;
        private LocalDateTime lastActivity;
        
        // Constructors, getters, and setters
        public TenantMetrics() {}
        
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        public Long getTotalServices() { return totalServices; }
        public void setTotalServices(Long totalServices) { this.totalServices = totalServices; }
        public Long getActiveServices() { return activeServices; }
        public void setActiveServices(Long activeServices) { this.activeServices = activeServices; }
        public Long getTotalFileTransfers() { return totalFileTransfers; }
        public void setTotalFileTransfers(Long totalFileTransfers) { this.totalFileTransfers = totalFileTransfers; }
        public Long getTotalDataVolume() { return totalDataVolume; }
        public void setTotalDataVolume(Long totalDataVolume) { this.totalDataVolume = totalDataVolume; }
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }
    
    // Constructors
    public TenantV2Dto() {}
    
    public TenantV2Dto(String name, String description, String timezone) {
        this.name = name;
        this.description = description;
        this.timezone = timezone;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    
    public TenantType getType() { return type; }
    public void setType(TenantType type) { this.type = type; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    
    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }
    
    public Map<String, Object> getLimits() { return limits; }
    public void setLimits(Map<String, Object> limits) { this.limits = limits; }
    
    public Long getParentTenantId() { return parentTenantId; }
    public void setParentTenantId(Long parentTenantId) { this.parentTenantId = parentTenantId; }
    
    public List<Long> getChildTenantIds() { return childTenantIds; }
    public void setChildTenantIds(List<Long> childTenantIds) { this.childTenantIds = childTenantIds; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public TenantMetrics getMetrics() { return metrics; }
    public void setMetrics(TenantMetrics metrics) { this.metrics = metrics; }
    
    public String getDataClassification() { return dataClassification; }
    public void setDataClassification(String dataClassification) { this.dataClassification = dataClassification; }
    
    public String getComplianceLevel() { return complianceLevel; }
    public void setComplianceLevel(String complianceLevel) { this.complianceLevel = complianceLevel; }
    
    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }
    
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    
    public LocalDateTime getLastApiAccess() { return lastApiAccess; }
    public void setLastApiAccess(LocalDateTime lastApiAccess) { this.lastApiAccess = lastApiAccess; }
}