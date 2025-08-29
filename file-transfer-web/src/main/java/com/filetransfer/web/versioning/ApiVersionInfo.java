package com.filetransfer.web.versioning;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Information about API version including metadata and compatibility details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiVersionInfo {
    
    private String version;
    private String semanticVersion;
    private LocalDateTime releaseDate;
    private String status; // CURRENT, DEPRECATED, SUNSET, BETA, ALPHA
    private boolean deprecated;
    private String deprecationMessage;
    private LocalDateTime sunsetDate;
    private List<String> supportedVersions;
    private List<String> compatibleVersions;
    private Map<String, Object> features;
    private List<BreakingChange> breakingChanges;
    private String documentationUrl;
    private String changelogUrl;
    private String migrationGuide;
    
    // Constructors
    public ApiVersionInfo() {}
    
    public ApiVersionInfo(String version) {
        this.version = version;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ApiVersionInfo info = new ApiVersionInfo();
        
        public Builder version(String version) {
            info.version = version;
            return this;
        }
        
        public Builder semanticVersion(String semanticVersion) {
            info.semanticVersion = semanticVersion;
            return this;
        }
        
        public Builder releaseDate(LocalDateTime releaseDate) {
            info.releaseDate = releaseDate;
            return this;
        }
        
        public Builder status(String status) {
            info.status = status;
            return this;
        }
        
        public Builder deprecated(boolean deprecated) {
            info.deprecated = deprecated;
            return this;
        }
        
        public Builder deprecationMessage(String deprecationMessage) {
            info.deprecationMessage = deprecationMessage;
            return this;
        }
        
        public Builder sunsetDate(LocalDateTime sunsetDate) {
            info.sunsetDate = sunsetDate;
            return this;
        }
        
        public Builder supportedVersions(List<String> supportedVersions) {
            info.supportedVersions = supportedVersions;
            return this;
        }
        
        public Builder compatibleVersions(List<String> compatibleVersions) {
            info.compatibleVersions = compatibleVersions;
            return this;
        }
        
        public Builder features(Map<String, Object> features) {
            info.features = features;
            return this;
        }
        
        public Builder breakingChanges(List<BreakingChange> breakingChanges) {
            info.breakingChanges = breakingChanges;
            return this;
        }
        
        public Builder documentationUrl(String documentationUrl) {
            info.documentationUrl = documentationUrl;
            return this;
        }
        
        public Builder changelogUrl(String changelogUrl) {
            info.changelogUrl = changelogUrl;
            return this;
        }
        
        public Builder migrationGuide(String migrationGuide) {
            info.migrationGuide = migrationGuide;
            return this;
        }
        
        public ApiVersionInfo build() {
            return info;
        }
    }
    
    // Inner class for breaking changes
    public static class BreakingChange {
        private String description;
        private String endpoint;
        private String changeType; // REMOVED, MODIFIED, ADDED_REQUIRED
        private String mitigation;
        private LocalDateTime effectiveDate;
        
        // Constructors, getters, and setters
        public BreakingChange() {}
        
        public BreakingChange(String description, String endpoint, String changeType) {
            this.description = description;
            this.endpoint = endpoint;
            this.changeType = changeType;
        }
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
        public String getMitigation() { return mitigation; }
        public void setMitigation(String mitigation) { this.mitigation = mitigation; }
        public LocalDateTime getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDateTime effectiveDate) { this.effectiveDate = effectiveDate; }
    }
    
    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSemanticVersion() { return semanticVersion; }
    public void setSemanticVersion(String semanticVersion) { this.semanticVersion = semanticVersion; }
    public LocalDateTime getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDateTime releaseDate) { this.releaseDate = releaseDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDeprecated() { return deprecated; }
    public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
    public String getDeprecationMessage() { return deprecationMessage; }
    public void setDeprecationMessage(String deprecationMessage) { this.deprecationMessage = deprecationMessage; }
    public LocalDateTime getSunsetDate() { return sunsetDate; }
    public void setSunsetDate(LocalDateTime sunsetDate) { this.sunsetDate = sunsetDate; }
    public List<String> getSupportedVersions() { return supportedVersions; }
    public void setSupportedVersions(List<String> supportedVersions) { this.supportedVersions = supportedVersions; }
    public List<String> getCompatibleVersions() { return compatibleVersions; }
    public void setCompatibleVersions(List<String> compatibleVersions) { this.compatibleVersions = compatibleVersions; }
    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }
    public List<BreakingChange> getBreakingChanges() { return breakingChanges; }
    public void setBreakingChanges(List<BreakingChange> breakingChanges) { this.breakingChanges = breakingChanges; }
    public String getDocumentationUrl() { return documentationUrl; }
    public void setDocumentationUrl(String documentationUrl) { this.documentationUrl = documentationUrl; }
    public String getChangelogUrl() { return changelogUrl; }
    public void setChangelogUrl(String changelogUrl) { this.changelogUrl = changelogUrl; }
    public String getMigrationGuide() { return migrationGuide; }
    public void setMigrationGuide(String migrationGuide) { this.migrationGuide = migrationGuide; }
}