package com.filetransfer.web.versioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage API versions, deprecation, and backward compatibility
 */
@Service
public class ApiVersionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersionManager.class);
    
    @Value("${api.versioning.current-version:2.0}")
    private String currentVersion;
    
    @Value("${api.versioning.minimum-supported-version:1.0}")
    private String minimumSupportedVersion;
    
    @Value("${api.versioning.deprecation-notice-period-days:90}")
    private int deprecationNoticePeriodDays;
    
    @Value("${api.versioning.sunset-period-days:180}")
    private int sunsetPeriodDays;
    
    private final Map<String, ApiVersionInfo> versionRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<String>> compatibilityMatrix = new ConcurrentHashMap<>();
    
    /**
     * Initialize version manager with default versions
     */
    public void initialize() {
        logger.info("Initializing API Version Manager");
        
        // Register supported versions
        registerVersion(createVersionInfo("1.0", "DEPRECATED"));
        registerVersion(createVersionInfo("1.1", "DEPRECATED"));
        registerVersion(createVersionInfo("1.2", "SUNSET"));
        registerVersion(createVersionInfo("2.0", "CURRENT"));
        registerVersion(createVersionInfo("2.1", "BETA"));
        
        // Set up compatibility matrix
        setupCompatibilityMatrix();
        
        logger.info("API Version Manager initialized with {} versions", versionRegistry.size());
    }
    
    /**
     * Register a new API version
     */
    public void registerVersion(ApiVersionInfo versionInfo) {
        versionRegistry.put(versionInfo.getVersion(), versionInfo);
        logger.info("Registered API version: {} (status: {})", 
                   versionInfo.getVersion(), versionInfo.getStatus());
    }
    
    /**
     * Get version information
     */
    public ApiVersionInfo getVersionInfo(String version) {
        return versionRegistry.get(version);
    }
    
    /**
     * Get all supported versions
     */
    public List<ApiVersionInfo> getAllVersions() {
        return new ArrayList<>(versionRegistry.values());
    }
    
    /**
     * Get currently supported versions (not sunset)
     */
    public List<ApiVersionInfo> getSupportedVersions() {
        return versionRegistry.values().stream()
            .filter(v -> !"SUNSET".equals(v.getStatus()))
            .sorted((a, b) -> compareVersions(b.getVersion(), a.getVersion())) // Newest first
            .toList();
    }
    
    /**
     * Get deprecated versions
     */
    public List<ApiVersionInfo> getDeprecatedVersions() {
        return versionRegistry.values().stream()
            .filter(ApiVersionInfo::isDeprecated)
            .sorted((a, b) -> compareVersions(a.getVersion(), b.getVersion())) // Oldest first
            .toList();
    }
    
    /**
     * Check if version is supported
     */
    public boolean isVersionSupported(String version) {
        ApiVersionInfo versionInfo = versionRegistry.get(version);
        return versionInfo != null && !"SUNSET".equals(versionInfo.getStatus());
    }
    
    /**
     * Check if version is deprecated
     */
    public boolean isVersionDeprecated(String version) {
        ApiVersionInfo versionInfo = versionRegistry.get(version);
        return versionInfo != null && versionInfo.isDeprecated();
    }
    
    /**
     * Get deprecation information for version
     */
    public DeprecationInfo getDeprecationInfo(String version) {
        ApiVersionInfo versionInfo = versionRegistry.get(version);
        if (versionInfo == null || !versionInfo.isDeprecated()) {
            return null;
        }
        
        return DeprecationInfo.builder()
            .version(version)
            .deprecated(true)
            .deprecationMessage(versionInfo.getDeprecationMessage())
            .sunsetDate(versionInfo.getSunsetDate())
            .replacementVersion(getReplacementVersion(version))
            .migrationGuide(versionInfo.getMigrationGuide())
            .build();
    }
    
    /**
     * Deprecate a version
     */
    public void deprecateVersion(String version, String reason, LocalDateTime sunsetDate) {
        ApiVersionInfo versionInfo = versionRegistry.get(version);
        if (versionInfo != null) {
            versionInfo.setDeprecated(true);
            versionInfo.setDeprecationMessage(reason);
            versionInfo.setSunsetDate(sunsetDate);
            versionInfo.setStatus("DEPRECATED");
            
            logger.info("Deprecated API version: {} - {} (sunset: {})", version, reason, sunsetDate);
        }
    }
    
    /**
     * Sunset a version (no longer supported)
     */
    public void sunsetVersion(String version) {
        ApiVersionInfo versionInfo = versionRegistry.get(version);
        if (versionInfo != null) {
            versionInfo.setStatus("SUNSET");
            
            logger.info("Sunset API version: {}", version);
        }
    }
    
    /**
     * Get compatible versions for a given version
     */
    public List<String> getCompatibleVersions(String version) {
        return compatibilityMatrix.getOrDefault(version, Collections.emptyList());
    }
    
    /**
     * Check if two versions are compatible
     */
    public boolean areVersionsCompatible(String version1, String version2) {
        if (version1.equals(version2)) {
            return true;
        }
        
        List<String> compatibleVersions = getCompatibleVersions(version1);
        return compatibleVersions.contains(version2);
    }
    
    /**
     * Get the best compatible version for a requested version
     */
    public String getBestCompatibleVersion(String requestedVersion) {
        // If exact version exists and is supported, return it
        if (isVersionSupported(requestedVersion)) {
            return requestedVersion;
        }
        
        // Find the best compatible version
        List<String> compatibleVersions = getCompatibleVersions(requestedVersion);
        for (String version : compatibleVersions) {
            if (isVersionSupported(version)) {
                return version;
            }
        }
        
        // Fallback to current version
        return currentVersion;
    }
    
    /**
     * Get replacement version for deprecated version
     */
    public String getReplacementVersion(String deprecatedVersion) {
        // Simple logic: find the next higher supported version
        List<ApiVersionInfo> supportedVersions = getSupportedVersions();
        
        for (ApiVersionInfo versionInfo : supportedVersions) {
            if (compareVersions(versionInfo.getVersion(), deprecatedVersion) > 0) {
                return versionInfo.getVersion();
            }
        }
        
        return currentVersion;
    }
    
    /**
     * Generate version migration plan
     */
    public VersionMigrationPlan generateMigrationPlan(String fromVersion, String toVersion) {
        VersionMigrationPlan.Builder planBuilder = VersionMigrationPlan.builder()
            .fromVersion(fromVersion)
            .toVersion(toVersion);
        
        // Get version information
        ApiVersionInfo fromInfo = getVersionInfo(fromVersion);
        ApiVersionInfo toInfo = getVersionInfo(toVersion);
        
        if (fromInfo == null || toInfo == null) {
            planBuilder.feasible(false)
                      .reason("Version information not available");
            return planBuilder.build();
        }
        
        // Check if migration is feasible
        if (compareVersions(toVersion, fromVersion) < 0) {
            planBuilder.feasible(false)
                      .reason("Cannot migrate to older version");
            return planBuilder.build();
        }
        
        // Generate migration steps
        List<MigrationStep> steps = generateMigrationSteps(fromVersion, toVersion);
        planBuilder.feasible(true)
                  .steps(steps)
                  .estimatedEffort(calculateMigrationEffort(steps))
                  .breakingChanges(getBreakingChangesBetweenVersions(fromVersion, toVersion));
        
        return planBuilder.build();
    }
    
    /**
     * Get breaking changes between versions
     */
    public List<ApiVersionInfo.BreakingChange> getBreakingChangesBetweenVersions(String fromVersion, String toVersion) {
        List<ApiVersionInfo.BreakingChange> breakingChanges = new ArrayList<>();
        
        // Collect breaking changes from all versions between fromVersion and toVersion
        for (ApiVersionInfo versionInfo : versionRegistry.values()) {
            if (compareVersions(versionInfo.getVersion(), fromVersion) > 0 && 
                compareVersions(versionInfo.getVersion(), toVersion) <= 0) {
                
                if (versionInfo.getBreakingChanges() != null) {
                    breakingChanges.addAll(versionInfo.getBreakingChanges());
                }
            }
        }
        
        return breakingChanges;
    }
    
    /**
     * Check version lifecycle and trigger notifications
     */
    public void checkVersionLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        
        for (ApiVersionInfo versionInfo : versionRegistry.values()) {
            // Check for versions that should be deprecated
            if ("CURRENT".equals(versionInfo.getStatus()) && shouldDeprecateVersion(versionInfo, now)) {
                scheduleDeprecation(versionInfo);
            }
            
            // Check for versions that should be sunset
            if (versionInfo.isDeprecated() && versionInfo.getSunsetDate() != null && 
                now.isAfter(versionInfo.getSunsetDate())) {
                sunsetVersion(versionInfo.getVersion());
            }
        }
    }
    
    // Private helper methods
    
    private ApiVersionInfo createVersionInfo(String version, String status) {
        LocalDateTime releaseDate = LocalDateTime.now().minusDays(
            switch (version) {
                case "1.0" -> 365;
                case "1.1" -> 300;
                case "1.2" -> 180;
                case "2.0" -> 30;
                case "2.1" -> 0;
                default -> 0;
            }
        );
        
        ApiVersionInfo.Builder builder = ApiVersionInfo.builder()
            .version(version)
            .semanticVersion(version + ".0")
            .releaseDate(releaseDate)
            .status(status)
            .documentationUrl("/docs/api/v" + version)
            .changelogUrl("/docs/changelog/v" + version);
        
        // Set deprecation info for deprecated versions
        if ("DEPRECATED".equals(status)) {
            builder.deprecated(true)
                   .deprecationMessage("This version is deprecated. Please migrate to version " + currentVersion)
                   .sunsetDate(LocalDateTime.now().plusDays(sunsetPeriodDays));
        }
        
        // Add breaking changes for newer versions
        if ("2.0".equals(version)) {
            List<ApiVersionInfo.BreakingChange> breakingChanges = Arrays.asList(
                new ApiVersionInfo.BreakingChange(
                    "Tenant ID is now required in all endpoints",
                    "/api/*/tenants/*",
                    "ADDED_REQUIRED"
                ),
                new ApiVersionInfo.BreakingChange(
                    "Date format changed to ISO 8601",
                    "All date fields",
                    "MODIFIED"
                )
            );
            builder.breakingChanges(breakingChanges);
        }
        
        return builder.build();
    }
    
    private void setupCompatibilityMatrix() {
        // Define version compatibility
        compatibilityMatrix.put("1.0", Arrays.asList("1.1", "1.2"));
        compatibilityMatrix.put("1.1", Arrays.asList("1.0", "1.2"));
        compatibilityMatrix.put("1.2", Arrays.asList("1.1", "2.0"));
        compatibilityMatrix.put("2.0", Arrays.asList("1.2", "2.1"));
        compatibilityMatrix.put("2.1", Arrays.asList("2.0"));
    }
    
    private int compareVersions(String version1, String version2) {
        // Simplified version comparison
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int major1 = Integer.parseInt(parts1[0]);
        int major2 = Integer.parseInt(parts2[0]);
        if (major1 != major2) return Integer.compare(major1, major2);
        
        if (parts1.length > 1 && parts2.length > 1) {
            int minor1 = Integer.parseInt(parts1[1]);
            int minor2 = Integer.parseInt(parts2[1]);
            return Integer.compare(minor1, minor2);
        }
        
        return 0;
    }
    
    private List<MigrationStep> generateMigrationSteps(String fromVersion, String toVersion) {
        List<MigrationStep> steps = new ArrayList<>();
        
        // Add common migration steps
        steps.add(new MigrationStep(
            "Update API endpoints",
            "Change base URL from /api/v" + fromVersion + " to /api/v" + toVersion,
            "HIGH"
        ));
        
        steps.add(new MigrationStep(
            "Update request/response models",
            "Review and update data models for compatibility",
            "MEDIUM"
        ));
        
        steps.add(new MigrationStep(
            "Test API integration",
            "Thoroughly test all API endpoints with new version",
            "HIGH"
        ));
        
        return steps;
    }
    
    private String calculateMigrationEffort(List<MigrationStep> steps) {
        long highEffortSteps = steps.stream().filter(s -> "HIGH".equals(s.getEffort())).count();
        long mediumEffortSteps = steps.stream().filter(s -> "MEDIUM".equals(s.getEffort())).count();
        
        if (highEffortSteps > 2) return "HIGH";
        if (highEffortSteps > 0 || mediumEffortSteps > 3) return "MEDIUM";
        return "LOW";
    }
    
    private boolean shouldDeprecateVersion(ApiVersionInfo versionInfo, LocalDateTime now) {
        // Logic to determine if a version should be deprecated
        return false; // Simplified for now
    }
    
    private void scheduleDeprecation(ApiVersionInfo versionInfo) {
        LocalDateTime sunsetDate = LocalDateTime.now().plusDays(sunsetPeriodDays);
        deprecateVersion(versionInfo.getVersion(), "Scheduled deprecation", sunsetDate);
    }
    
    // Inner classes for migration planning
    
    public static class DeprecationInfo {
        private String version;
        private boolean deprecated;
        private String deprecationMessage;
        private LocalDateTime sunsetDate;
        private String replacementVersion;
        private String migrationGuide;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private DeprecationInfo info = new DeprecationInfo();
            
            public Builder version(String version) { info.version = version; return this; }
            public Builder deprecated(boolean deprecated) { info.deprecated = deprecated; return this; }
            public Builder deprecationMessage(String deprecationMessage) { info.deprecationMessage = deprecationMessage; return this; }
            public Builder sunsetDate(LocalDateTime sunsetDate) { info.sunsetDate = sunsetDate; return this; }
            public Builder replacementVersion(String replacementVersion) { info.replacementVersion = replacementVersion; return this; }
            public Builder migrationGuide(String migrationGuide) { info.migrationGuide = migrationGuide; return this; }
            
            public DeprecationInfo build() { return info; }
        }
        
        // Getters and setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public boolean isDeprecated() { return deprecated; }
        public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
        public String getDeprecationMessage() { return deprecationMessage; }
        public void setDeprecationMessage(String deprecationMessage) { this.deprecationMessage = deprecationMessage; }
        public LocalDateTime getSunsetDate() { return sunsetDate; }
        public void setSunsetDate(LocalDateTime sunsetDate) { this.sunsetDate = sunsetDate; }
        public String getReplacementVersion() { return replacementVersion; }
        public void setReplacementVersion(String replacementVersion) { this.replacementVersion = replacementVersion; }
        public String getMigrationGuide() { return migrationGuide; }
        public void setMigrationGuide(String migrationGuide) { this.migrationGuide = migrationGuide; }
    }
    
    public static class VersionMigrationPlan {
        private String fromVersion;
        private String toVersion;
        private boolean feasible;
        private String reason;
        private List<MigrationStep> steps;
        private String estimatedEffort;
        private List<ApiVersionInfo.BreakingChange> breakingChanges;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private VersionMigrationPlan plan = new VersionMigrationPlan();
            
            public Builder fromVersion(String fromVersion) { plan.fromVersion = fromVersion; return this; }
            public Builder toVersion(String toVersion) { plan.toVersion = toVersion; return this; }
            public Builder feasible(boolean feasible) { plan.feasible = feasible; return this; }
            public Builder reason(String reason) { plan.reason = reason; return this; }
            public Builder steps(List<MigrationStep> steps) { plan.steps = steps; return this; }
            public Builder estimatedEffort(String estimatedEffort) { plan.estimatedEffort = estimatedEffort; return this; }
            public Builder breakingChanges(List<ApiVersionInfo.BreakingChange> breakingChanges) { plan.breakingChanges = breakingChanges; return this; }
            
            public VersionMigrationPlan build() { return plan; }
        }
        
        // Getters and setters
        public String getFromVersion() { return fromVersion; }
        public void setFromVersion(String fromVersion) { this.fromVersion = fromVersion; }
        public String getToVersion() { return toVersion; }
        public void setToVersion(String toVersion) { this.toVersion = toVersion; }
        public boolean isFeasible() { return feasible; }
        public void setFeasible(boolean feasible) { this.feasible = feasible; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public List<MigrationStep> getSteps() { return steps; }
        public void setSteps(List<MigrationStep> steps) { this.steps = steps; }
        public String getEstimatedEffort() { return estimatedEffort; }
        public void setEstimatedEffort(String estimatedEffort) { this.estimatedEffort = estimatedEffort; }
        public List<ApiVersionInfo.BreakingChange> getBreakingChanges() { return breakingChanges; }
        public void setBreakingChanges(List<ApiVersionInfo.BreakingChange> breakingChanges) { this.breakingChanges = breakingChanges; }
    }
    
    public static class MigrationStep {
        private String title;
        private String description;
        private String effort; // LOW, MEDIUM, HIGH
        
        public MigrationStep(String title, String description, String effort) {
            this.title = title;
            this.description = description;
            this.effort = effort;
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEffort() { return effort; }
        public void setEffort(String effort) { this.effort = effort; }
    }
}