package com.filetransfer.web.versioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service to resolve API version from HTTP requests using various strategies
 */
@Component
public class ApiVersionResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersionResolver.class);
    
    @Value("${api.versioning.default-version:1.0}")
    private String defaultVersion;
    
    @Value("${api.versioning.default-strategy:URL_PATH}")
    private String defaultStrategy;
    
    @Value("${api.versioning.header-name:X-API-Version}")
    private String versionHeaderName;
    
    @Value("${api.versioning.accept-header-name:Accept-Version}")
    private String acceptVersionHeaderName;
    
    @Value("${api.versioning.query-parameter:version}")
    private String versionQueryParameter;
    
    @Value("${api.versioning.media-type-prefix:application/vnd.filetransfer}")
    private String mediaTypePrefix;
    
    @Value("${api.versioning.strict-mode:false}")
    private boolean strictMode;
    
    // Regex patterns for version validation
    private static final Pattern SEMANTIC_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-([a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*))?$");
    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?$");
    private static final Pattern DATE_VERSION_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    /**
     * Resolve API version from HTTP request using specified strategy
     */
    public String resolveVersion(HttpServletRequest request, VersioningStrategy strategy) {
        logger.debug("Resolving API version using strategy: {}", strategy);
        
        String version = null;
        
        switch (strategy) {
            case URL_PATH:
                version = resolveVersionFromUrlPath(request);
                break;
            case HEADER:
                version = resolveVersionFromHeader(request);
                break;
            case QUERY_PARAMETER:
                version = resolveVersionFromQueryParameter(request);
                break;
            case MEDIA_TYPE:
                version = resolveVersionFromMediaType(request);
                break;
            case ACCEPT_HEADER:
                version = resolveVersionFromAcceptHeader(request);
                break;
            case HYBRID:
                version = resolveVersionHybrid(request);
                break;
            case SEMANTIC:
                version = resolveSemanticVersion(request);
                break;
            case DATE_BASED:
                version = resolveDateBasedVersion(request);
                break;
            case NONE:
                version = null;
                break;
            default:
                version = resolveVersionFromUrlPath(request);
                break;
        }
        
        // Fallback to default version if no version found and not in strict mode
        if (!StringUtils.hasText(version) && !strictMode) {
            version = defaultVersion;
            logger.debug("Using default version: {}", version);
        }
        
        // Validate resolved version
        if (StringUtils.hasText(version) && !isValidVersion(version)) {
            logger.warn("Invalid version format: {}", version);
            if (strictMode) {
                throw new InvalidApiVersionException("Invalid version format: " + version);
            }
            version = defaultVersion;
        }
        
        logger.debug("Resolved API version: {}", version);
        return version;
    }
    
    /**
     * Resolve version from URL path (e.g., /api/v1/tenants)
     */
    private String resolveVersionFromUrlPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        logger.debug("Resolving version from URL path: {}", requestUri);
        
        // Pattern: /api/v{version}/...
        Pattern urlVersionPattern = Pattern.compile("/api/v([^/]+)");
        java.util.regex.Matcher matcher = urlVersionPattern.matcher(requestUri);
        
        if (matcher.find()) {
            String version = matcher.group(1);
            logger.debug("Found version in URL path: {}", version);
            return version;
        }
        
        return null;
    }
    
    /**
     * Resolve version from custom header
     */
    private String resolveVersionFromHeader(HttpServletRequest request) {
        String version = request.getHeader(versionHeaderName);
        logger.debug("Resolving version from header {}: {}", versionHeaderName, version);
        return version;
    }
    
    /**
     * Resolve version from query parameter
     */
    private String resolveVersionFromQueryParameter(HttpServletRequest request) {
        String version = request.getParameter(versionQueryParameter);
        logger.debug("Resolving version from query parameter {}: {}", versionQueryParameter, version);
        return version;
    }
    
    /**
     * Resolve version from Accept header media type
     */
    private String resolveVersionFromMediaType(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        logger.debug("Resolving version from Accept header: {}", acceptHeader);
        
        if (!StringUtils.hasText(acceptHeader)) {
            return null;
        }
        
        // Pattern: application/vnd.filetransfer.v1+json
        Pattern mediaTypePattern = Pattern.compile(Pattern.quote(mediaTypePrefix) + "\\.v([^+]+)\\+");
        java.util.regex.Matcher matcher = mediaTypePattern.matcher(acceptHeader);
        
        if (matcher.find()) {
            String version = matcher.group(1);
            logger.debug("Found version in media type: {}", version);
            return version;
        }
        
        return null;
    }
    
    /**
     * Resolve version from Accept-Version header
     */
    private String resolveVersionFromAcceptHeader(HttpServletRequest request) {
        String version = request.getHeader(acceptVersionHeaderName);
        logger.debug("Resolving version from Accept-Version header: {}", version);
        return version;
    }
    
    /**
     * Resolve version using hybrid strategy (try multiple methods in order)
     */
    private String resolveVersionHybrid(HttpServletRequest request) {
        logger.debug("Resolving version using hybrid strategy");
        
        // Priority order: URL Path -> Header -> Accept-Version -> Query Parameter -> Media Type
        String version = resolveVersionFromUrlPath(request);
        if (StringUtils.hasText(version)) return version;
        
        version = resolveVersionFromHeader(request);
        if (StringUtils.hasText(version)) return version;
        
        version = resolveVersionFromAcceptHeader(request);
        if (StringUtils.hasText(version)) return version;
        
        version = resolveVersionFromQueryParameter(request);
        if (StringUtils.hasText(version)) return version;
        
        version = resolveVersionFromMediaType(request);
        if (StringUtils.hasText(version)) return version;
        
        return null;
    }
    
    /**
     * Resolve semantic version with range support
     */
    private String resolveSemanticVersion(HttpServletRequest request) {
        String version = resolveVersionHybrid(request);
        
        if (StringUtils.hasText(version)) {
            // Handle semantic version ranges (e.g., ~1.2.0, ^1.0.0)
            if (version.startsWith("~") || version.startsWith("^")) {
                return resolveSemanticVersionRange(version);
            }
        }
        
        return version;
    }
    
    /**
     * Resolve date-based version
     */
    private String resolveDateBasedVersion(HttpServletRequest request) {
        String version = resolveVersionHybrid(request);
        
        if (StringUtils.hasText(version) && DATE_VERSION_PATTERN.matcher(version).matches()) {
            return version;
        }
        
        return null;
    }
    
    /**
     * Resolve semantic version range to specific version
     */
    private String resolveSemanticVersionRange(String versionRange) {
        // Implementation would resolve version ranges to specific versions
        // This is a simplified implementation
        if (versionRange.startsWith("~")) {
            // ~1.2.0 means >=1.2.0 <1.3.0
            return versionRange.substring(1);
        } else if (versionRange.startsWith("^")) {
            // ^1.0.0 means >=1.0.0 <2.0.0
            return versionRange.substring(1);
        }
        
        return versionRange;
    }
    
    /**
     * Validate version format
     */
    public boolean isValidVersion(String version) {
        if (!StringUtils.hasText(version)) {
            return false;
        }
        
        return SEMANTIC_VERSION_PATTERN.matcher(version).matches() ||
               SIMPLE_VERSION_PATTERN.matcher(version).matches() ||
               DATE_VERSION_PATTERN.matcher(version).matches();
    }
    
    /**
     * Compare two versions
     */
    public int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;
        
        // Handle semantic versions
        if (SEMANTIC_VERSION_PATTERN.matcher(version1).matches() && 
            SEMANTIC_VERSION_PATTERN.matcher(version2).matches()) {
            return compareSemanticVersions(version1, version2);
        }
        
        // Handle simple versions
        if (SIMPLE_VERSION_PATTERN.matcher(version1).matches() && 
            SIMPLE_VERSION_PATTERN.matcher(version2).matches()) {
            return compareSimpleVersions(version1, version2);
        }
        
        // Handle date-based versions
        if (DATE_VERSION_PATTERN.matcher(version1).matches() && 
            DATE_VERSION_PATTERN.matcher(version2).matches()) {
            return version1.compareTo(version2);
        }
        
        // Fallback to string comparison
        return version1.compareTo(version2);
    }
    
    /**
     * Compare semantic versions
     */
    private int compareSemanticVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        // Compare major version
        int major1 = Integer.parseInt(parts1[0]);
        int major2 = Integer.parseInt(parts2[0]);
        if (major1 != major2) return Integer.compare(major1, major2);
        
        // Compare minor version
        if (parts1.length > 1 && parts2.length > 1) {
            int minor1 = Integer.parseInt(parts1[1]);
            int minor2 = Integer.parseInt(parts2[1]);
            if (minor1 != minor2) return Integer.compare(minor1, minor2);
        }
        
        // Compare patch version
        if (parts1.length > 2 && parts2.length > 2) {
            int patch1 = Integer.parseInt(parts1[2]);
            int patch2 = Integer.parseInt(parts2[2]);
            return Integer.compare(patch1, patch2);
        }
        
        return 0;
    }
    
    /**
     * Compare simple versions
     */
    private int compareSimpleVersions(String version1, String version2) {
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
    
    /**
     * Check if version is supported
     */
    public boolean isVersionSupported(String version, String[] supportedVersions) {
        if (supportedVersions == null || supportedVersions.length == 0) {
            return true; // No restrictions
        }
        
        for (String supportedVersion : supportedVersions) {
            if (version.equals(supportedVersion)) {
                return true;
            }
            
            // Handle version ranges
            if (supportedVersion.contains("-")) {
                String[] range = supportedVersion.split("-");
                if (range.length == 2) {
                    String minVersion = range[0].trim();
                    String maxVersion = range[1].trim();
                    
                    if (compareVersions(version, minVersion) >= 0 && 
                        compareVersions(version, maxVersion) <= 0) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get latest supported version
     */
    public String getLatestSupportedVersion(String[] supportedVersions) {
        if (supportedVersions == null || supportedVersions.length == 0) {
            return defaultVersion;
        }
        
        String latest = supportedVersions[0];
        for (String version : supportedVersions) {
            if (compareVersions(version, latest) > 0) {
                latest = version;
            }
        }
        
        return latest;
    }
    
    // Exception class
    public static class InvalidApiVersionException extends RuntimeException {
        public InvalidApiVersionException(String message) {
            super(message);
        }
        
        public InvalidApiVersionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}