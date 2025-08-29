package com.filetransfer.web.controller;

import com.filetransfer.web.versioning.ApiVersion;
import com.filetransfer.web.versioning.ApiVersionInfo;
import com.filetransfer.web.versioning.ApiVersionManager;
import com.filetransfer.web.versioning.VersioningStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for API version management and information
 * Provides endpoints for version discovery, migration planning, and version lifecycle management
 */
@RestController
@RequestMapping("/api/versions")
@CrossOrigin(origins = "*")
@ApiVersion(
    value = {"1.0", "2.0", "2.1"}, 
    strategy = VersioningStrategy.HYBRID
)
public class ApiVersionController {
    
    @Autowired
    private ApiVersionManager versionManager;
    
    /**
     * Get all supported API versions
     */
    @GetMapping
    @ApiVersion({"1.0", "2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getAllVersions() {
        List<ApiVersionInfo> allVersions = versionManager.getAllVersions();
        List<ApiVersionInfo> supportedVersions = versionManager.getSupportedVersions();
        List<ApiVersionInfo> deprecatedVersions = versionManager.getDeprecatedVersions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("allVersions", allVersions);
        response.put("supportedVersions", supportedVersions);
        response.put("deprecatedVersions", deprecatedVersions);
        response.put("totalVersions", allVersions.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get information about a specific version
     */
    @GetMapping("/{version}")
    @ApiVersion({"1.0", "2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getVersionInfo(@PathVariable String version) {
        ApiVersionInfo versionInfo = versionManager.getVersionInfo(version);
        
        if (versionInfo == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Version not found");
            errorResponse.put("version", version);
            errorResponse.put("availableVersions", versionManager.getSupportedVersions());
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("versionInfo", versionInfo);
        response.put("supported", versionManager.isVersionSupported(version));
        response.put("deprecated", versionManager.isVersionDeprecated(version));
        response.put("compatibleVersions", versionManager.getCompatibleVersions(version));
        
        // Add deprecation info if applicable
        if (versionManager.isVersionDeprecated(version)) {
            response.put("deprecationInfo", versionManager.getDeprecationInfo(version));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get version compatibility matrix
     */
    @GetMapping("/compatibility")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getVersionCompatibility() {
        List<ApiVersionInfo> supportedVersions = versionManager.getSupportedVersions();
        Map<String, List<String>> compatibilityMatrix = new HashMap<>();
        
        for (ApiVersionInfo versionInfo : supportedVersions) {
            String version = versionInfo.getVersion();
            compatibilityMatrix.put(version, versionManager.getCompatibleVersions(version));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("compatibilityMatrix", compatibilityMatrix);
        response.put("supportedVersions", supportedVersions.stream().map(ApiVersionInfo::getVersion).toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get migration plan between versions
     */
    @GetMapping("/migration-plan")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getMigrationPlan(
            @RequestParam String fromVersion,
            @RequestParam String toVersion) {
        
        ApiVersionManager.VersionMigrationPlan migrationPlan = 
            versionManager.generateMigrationPlan(fromVersion, toVersion);
        
        Map<String, Object> response = new HashMap<>();
        response.put("migrationPlan", migrationPlan);
        response.put("fromVersion", fromVersion);
        response.put("toVersion", toVersion);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get breaking changes between versions
     */
    @GetMapping("/breaking-changes")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getBreakingChanges(
            @RequestParam String fromVersion,
            @RequestParam String toVersion) {
        
        List<ApiVersionInfo.BreakingChange> breakingChanges = 
            versionManager.getBreakingChangesBetweenVersions(fromVersion, toVersion);
        
        Map<String, Object> response = new HashMap<>();
        response.put("breakingChanges", breakingChanges);
        response.put("fromVersion", fromVersion);
        response.put("toVersion", toVersion);
        response.put("hasBreakingChanges", !breakingChanges.isEmpty());
        response.put("changeCount", breakingChanges.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current version information from request
     */
    @GetMapping("/current")
    @ApiVersion({"1.0", "2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getCurrentVersion(HttpServletRequest request) {
        String currentVersion = (String) request.getAttribute("apiVersion");
        ApiVersionInfo versionInfo = (ApiVersionInfo) request.getAttribute("apiVersionInfo");
        ApiVersionManager.DeprecationInfo deprecationInfo = 
            (ApiVersionManager.DeprecationInfo) request.getAttribute("apiDeprecationInfo");
        
        Map<String, Object> response = new HashMap<>();
        response.put("currentVersion", currentVersion);
        response.put("versionInfo", versionInfo);
        response.put("deprecated", deprecationInfo != null);
        
        if (deprecationInfo != null) {
            response.put("deprecationInfo", deprecationInfo);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get version statistics and usage metrics
     */
    @GetMapping("/statistics")
    @ApiVersion({"2.0", "2.1"})
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getVersionStatistics() {
        List<ApiVersionInfo> allVersions = versionManager.getAllVersions();
        List<ApiVersionInfo> supportedVersions = versionManager.getSupportedVersions();
        List<ApiVersionInfo> deprecatedVersions = versionManager.getDeprecatedVersions();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalVersions", allVersions.size());
        statistics.put("supportedVersions", supportedVersions.size());
        statistics.put("deprecatedVersions", deprecatedVersions.size());
        statistics.put("sunsetVersions", allVersions.stream()
            .filter(v -> "SUNSET".equals(v.getStatus())).count());
        statistics.put("betaVersions", allVersions.stream()
            .filter(v -> "BETA".equals(v.getStatus())).count());
        
        // Version distribution
        Map<String, Object> versionDistribution = new HashMap<>();
        for (ApiVersionInfo version : allVersions) {
            versionDistribution.put(version.getVersion(), Map.of(
                "status", version.getStatus(),
                "deprecated", version.isDeprecated(),
                "releaseDate", version.getReleaseDate()
            ));
        }
        statistics.put("versionDistribution", versionDistribution);
        
        Map<String, Object> response = new HashMap<>();
        response.put("statistics", statistics);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deprecate a version (Admin only)
     */
    @PostMapping("/{version}/deprecate")
    @ApiVersion({"2.0", "2.1"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deprecateVersion(
            @PathVariable String version,
            @RequestBody Map<String, Object> deprecationRequest) {
        
        String reason = (String) deprecationRequest.get("reason");
        String sunsetDateStr = (String) deprecationRequest.get("sunsetDate");
        
        LocalDateTime sunsetDate = sunsetDateStr != null ? 
            LocalDateTime.parse(sunsetDateStr) : LocalDateTime.now().plusDays(180);
        
        versionManager.deprecateVersion(version, reason, sunsetDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Version deprecated successfully");
        response.put("version", version);
        response.put("reason", reason);
        response.put("sunsetDate", sunsetDate);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Sunset a version (Admin only)
     */
    @PostMapping("/{version}/sunset")
    @ApiVersion({"2.0", "2.1"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sunsetVersion(@PathVariable String version) {
        if (!versionManager.isVersionSupported(version)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Version not found or already sunset");
            errorResponse.put("version", version);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        versionManager.sunsetVersion(version);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Version sunset successfully");
        response.put("version", version);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Register a new version (Admin only)
     */
    @PostMapping
    @ApiVersion({"2.1"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerVersion(@RequestBody ApiVersionInfo versionInfo) {
        versionManager.registerVersion(versionInfo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Version registered successfully");
        response.put("versionInfo", versionInfo);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check for version management system
     */
    @GetMapping("/health")
    @ApiVersion({"1.0", "2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getVersionHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("versionManagerStatus", "OPERATIONAL");
        health.put("supportedVersions", versionManager.getSupportedVersions().size());
        health.put("deprecatedVersions", versionManager.getDeprecatedVersions().size());
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(health);
    }
}