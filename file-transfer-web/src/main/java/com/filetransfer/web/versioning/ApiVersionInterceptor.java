package com.filetransfer.web.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor to handle API versioning for incoming requests
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersionInterceptor.class);
    
    @Autowired
    private ApiVersionResolver versionResolver;
    
    @Autowired
    private ApiVersionManager versionManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Request attribute keys
    public static final String API_VERSION_ATTRIBUTE = "apiVersion";
    public static final String API_VERSION_INFO_ATTRIBUTE = "apiVersionInfo";
    public static final String API_DEPRECATION_INFO_ATTRIBUTE = "apiDeprecationInfo";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("Processing API version for request: {} {}", request.getMethod(), request.getRequestURI());
        
        // Only process for controller methods
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        // Check if method or class has ApiVersion annotation
        ApiVersion apiVersion = getApiVersionAnnotation(method, handlerMethod.getBeanType());
        
        if (apiVersion == null) {
            logger.debug("No API version annotation found, skipping version processing");
            return true;
        }
        
        try {
            // Resolve version from request
            String requestedVersion = resolveRequestedVersion(request, apiVersion);
            logger.debug("Resolved requested version: {}", requestedVersion);
            
            // Validate version
            String validatedVersion = validateAndNormalizeVersion(requestedVersion, apiVersion, request, response);
            if (validatedVersion == null) {
                return false; // Response already sent
            }
            
            // Store version information in request attributes
            request.setAttribute(API_VERSION_ATTRIBUTE, validatedVersion);
            
            ApiVersionInfo versionInfo = versionManager.getVersionInfo(validatedVersion);
            if (versionInfo != null) {
                request.setAttribute(API_VERSION_INFO_ATTRIBUTE, versionInfo);
            }
            
            // Handle deprecation
            handleDeprecation(validatedVersion, request, response);
            
            // Add version headers to response
            addVersionHeaders(response, validatedVersion, apiVersion);
            
            logger.debug("API version processing completed for version: {}", validatedVersion);
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing API version", e);
            
            // Send error response
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid API version");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Add final version information to response headers
        String version = (String) request.getAttribute(API_VERSION_ATTRIBUTE);
        if (version != null) {
            response.setHeader("X-API-Version-Used", version);
        }
        
        // Add deprecation warnings if applicable
        ApiVersionManager.DeprecationInfo deprecationInfo = 
            (ApiVersionManager.DeprecationInfo) request.getAttribute(API_DEPRECATION_INFO_ATTRIBUTE);
        
        if (deprecationInfo != null) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", deprecationInfo.getSunsetDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            response.setHeader("Link", String.format("<%s>; rel=\"successor-version\"", 
                             "/api/v" + deprecationInfo.getReplacementVersion()));
        }
    }
    
    /**
     * Get ApiVersion annotation from method or class
     */
    private ApiVersion getApiVersionAnnotation(Method method, Class<?> controllerClass) {
        // Check method first
        ApiVersion methodAnnotation = method.getAnnotation(ApiVersion.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        
        // Check class
        return controllerClass.getAnnotation(ApiVersion.class);
    }
    
    /**
     * Resolve requested version from request
     */
    private String resolveRequestedVersion(HttpServletRequest request, ApiVersion apiVersion) {
        VersioningStrategy strategy = apiVersion.strategy();
        if (strategy == VersioningStrategy.DEFAULT) {
            strategy = VersioningStrategy.URL_PATH; // Default strategy
        }
        
        return versionResolver.resolveVersion(request, strategy);
    }
    
    /**
     * Validate and normalize the requested version
     */
    private String validateAndNormalizeVersion(String requestedVersion, ApiVersion apiVersion, 
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        // Check if version is required
        if (apiVersion.requiresVersion() && requestedVersion == null) {
            throw new ApiVersionResolver.InvalidApiVersionException("API version is required for this endpoint");
        }
        
        // Use default version if none specified
        if (requestedVersion == null) {
            requestedVersion = getDefaultVersionForAnnotation(apiVersion);
        }
        
        // Validate version format
        if (!versionResolver.isValidVersion(requestedVersion)) {
            throw new ApiVersionResolver.InvalidApiVersionException("Invalid version format: " + requestedVersion);
        }
        
        // Check if version is supported by this endpoint
        if (!versionResolver.isVersionSupported(requestedVersion, apiVersion.value())) {
            // Try to find compatible version
            String compatibleVersion = findCompatibleVersion(requestedVersion, apiVersion);
            
            if (compatibleVersion != null) {
                logger.info("Using compatible version {} for requested version {}", 
                           compatibleVersion, requestedVersion);
                return compatibleVersion;
            }
            
            // Send version not supported error
            sendVersionNotSupportedError(response, requestedVersion, apiVersion.value());
            return null;
        }
        
        // Check version range constraints
        if (!isVersionInRange(requestedVersion, apiVersion)) {
            throw new ApiVersionResolver.InvalidApiVersionException(
                String.format("Version %s is not in supported range [%s - %s]", 
                            requestedVersion, apiVersion.since(), apiVersion.until()));
        }
        
        return requestedVersion;
    }
    
    /**
     * Handle version deprecation
     */
    private void handleDeprecation(String version, HttpServletRequest request, HttpServletResponse response) {
        if (versionManager.isVersionDeprecated(version)) {
            ApiVersionManager.DeprecationInfo deprecationInfo = versionManager.getDeprecationInfo(version);
            if (deprecationInfo != null) {
                request.setAttribute(API_DEPRECATION_INFO_ATTRIBUTE, deprecationInfo);
                
                logger.warn("Using deprecated API version: {} - {}", 
                           version, deprecationInfo.getDeprecationMessage());
            }
        }
    }
    
    /**
     * Add version-related headers to response
     */
    private void addVersionHeaders(HttpServletResponse response, String version, ApiVersion apiVersion) {
        if (apiVersion.includeVersionHeaders()) {
            response.setHeader("X-API-Version", version);
            response.setHeader("X-API-Supported-Versions", String.join(",", apiVersion.value()));
            
            // Add version metadata
            response.setHeader("X-API-Version-Strategy", apiVersion.strategy().name());
            
            if (apiVersion.deprecated()) {
                response.setHeader("X-API-Deprecated", "true");
                if (!apiVersion.deprecationMessage().isEmpty()) {
                    response.setHeader("X-API-Deprecation-Message", apiVersion.deprecationMessage());
                }
            }
        }
    }
    
    /**
     * Get default version for annotation
     */
    private String getDefaultVersionForAnnotation(ApiVersion apiVersion) {
        if (apiVersion.value().length > 0) {
            // Return the latest supported version
            String latestVersion = apiVersion.value()[0];
            for (String version : apiVersion.value()) {
                if (versionResolver.compareVersions(version, latestVersion) > 0) {
                    latestVersion = version;
                }
            }
            return latestVersion;
        }
        
        return "1.0"; // Fallback default
    }
    
    /**
     * Find compatible version for requested version
     */
    private String findCompatibleVersion(String requestedVersion, ApiVersion apiVersion) {
        // Check if any supported version is compatible with requested version
        for (String supportedVersion : apiVersion.value()) {
            if (versionManager.areVersionsCompatible(requestedVersion, supportedVersion)) {
                return supportedVersion;
            }
        }
        
        // Try to find best compatible version using version manager
        return versionManager.getBestCompatibleVersion(requestedVersion);
    }
    
    /**
     * Check if version is within specified range
     */
    private boolean isVersionInRange(String version, ApiVersion apiVersion) {
        // Check since constraint
        if (!apiVersion.since().isEmpty()) {
            if (versionResolver.compareVersions(version, apiVersion.since()) < 0) {
                return false;
            }
        }
        
        // Check until constraint
        if (!apiVersion.until().isEmpty()) {
            if (versionResolver.compareVersions(version, apiVersion.until()) > 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Send version not supported error response
     */
    private void sendVersionNotSupportedError(HttpServletResponse response, String requestedVersion, String[] supportedVersions) throws Exception {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unsupported API version");
        errorResponse.put("message", String.format("Version %s is not supported", requestedVersion));
        errorResponse.put("requestedVersion", requestedVersion);
        errorResponse.put("supportedVersions", supportedVersions);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        // Add suggestions for migration
        String replacementVersion = versionManager.getReplacementVersion(requestedVersion);
        if (replacementVersion != null) {
            errorResponse.put("suggestedVersion", replacementVersion);
            
            ApiVersionManager.VersionMigrationPlan migrationPlan = 
                versionManager.generateMigrationPlan(requestedVersion, replacementVersion);
            if (migrationPlan.isFeasible()) {
                errorResponse.put("migrationPlan", migrationPlan);
            }
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}