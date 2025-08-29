package com.filetransfer.batch.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Input validation filter for batch application
 * Validates and sanitizes incoming requests to prevent security vulnerabilities
 */
@Component
public class BatchInputValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BatchInputValidationFilter.class);

    @Autowired
    private BatchInputValidationService validationService;

    // Security patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror).*"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|javascript:|vbscript:|onload|onerror|onclick|onmouseover|onfocus|onblur).*"
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\.[\\\\/]|[\\\\/]\\.\\.[\\\\/]|\\.\\.[\\\\/]|[\\\\/]\\.\\.).*"
    );

    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
        ".*[\\(\\)\\*\\\\\\x00].*"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        try {
            // Skip validation for health checks and static resources
            String requestURI = request.getRequestURI();
            if (isExcludedPath(requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate request parameters
            if (!validateRequestParameters(request)) {
                handleValidationFailure(request, response, "Invalid request parameters detected");
                return;
            }

            // Validate headers
            if (!validateHeaders(request)) {
                handleValidationFailure(request, response, "Invalid headers detected");
                return;
            }

            // Validate request body size
            if (!validateRequestSize(request)) {
                handleValidationFailure(request, response, "Request size exceeds maximum allowed limit");
                return;
            }

            // Create sanitized request wrapper
            BatchSanitizedRequestWrapper sanitizedRequest = new BatchSanitizedRequestWrapper(request, validationService);
            
            filterChain.doFilter(sanitizedRequest, response);

        } catch (Exception e) {
            logger.error("Error in input validation filter", e);
            handleValidationFailure(request, response, "Request validation failed");
        }
    }

    /**
     * Check if path should be excluded from validation
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.contains("/static/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico");
    }

    /**
     * Validate request parameters
     */
    private boolean validateRequestParameters(HttpServletRequest request) {
        // Check query parameters
        if (request.getQueryString() != null) {
            String queryString = request.getQueryString();
            if (containsMaliciousContent(queryString)) {
                logger.warn("Malicious query string detected: {}", queryString);
                return false;
            }
        }

        // Check individual parameters
        if (request.getParameterMap() != null) {
            for (String paramName : request.getParameterMap().keySet()) {
                if (containsMaliciousContent(paramName)) {
                    logger.warn("Malicious parameter name detected: {}", paramName);
                    return false;
                }
                
                String[] paramValues = request.getParameterValues(paramName);
                if (paramValues != null) {
                    for (String paramValue : paramValues) {
                        if (containsMaliciousContent(paramValue)) {
                            logger.warn("Malicious parameter value detected for {}: {}", paramName, paramValue);
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validate request headers
     */
    private boolean validateHeaders(HttpServletRequest request) {
        // Check for suspicious headers
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && containsMaliciousContent(userAgent)) {
            logger.warn("Malicious User-Agent detected: {}", userAgent);
            return false;
        }

        String referer = request.getHeader("Referer");
        if (referer != null && containsMaliciousContent(referer)) {
            logger.warn("Malicious Referer detected: {}", referer);
            return false;
        }

        // Check Content-Type for POST/PUT requests
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            String contentType = request.getContentType();
            if (contentType != null && !isValidContentType(contentType)) {
                logger.warn("Invalid Content-Type detected: {}", contentType);
                return false;
            }
        }

        return true;
    }

    /**
     * Validate request size
     */
    private boolean validateRequestSize(HttpServletRequest request) {
        // Check content length
        int contentLength = request.getContentLength();
        if (contentLength > 10 * 1024 * 1024) { // 10MB limit
            logger.warn("Request size too large: {} bytes", contentLength);
            return false;
        }

        return true;
    }

    /**
     * Check if content contains malicious patterns
     */
    private boolean containsMaliciousContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // Check for SQL injection
        if (SQL_INJECTION_PATTERN.matcher(content).matches()) {
            return true;
        }

        // Check for XSS
        if (XSS_PATTERN.matcher(content).matches()) {
            return true;
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(content).matches()) {
            return true;
        }

        // Check for LDAP injection
        if (LDAP_INJECTION_PATTERN.matcher(content).matches()) {
            return true;
        }

        // Check for command injection
        if (content.contains("$(") || content.contains("`") || content.contains("&&") || content.contains("||")) {
            return true;
        }

        return false;
    }

    /**
     * Check if content type is valid
     */
    private boolean isValidContentType(String contentType) {
        return contentType.startsWith("application/json") ||
               contentType.startsWith("application/x-www-form-urlencoded") ||
               contentType.startsWith("multipart/form-data") ||
               contentType.startsWith("text/plain");
    }

    /**
     * Handle validation failure
     */
    private void handleValidationFailure(HttpServletRequest request, HttpServletResponse response, 
                                       String message) throws IOException {
        
        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        logger.warn("Input validation failed for client: {} on endpoint: {} - {}", 
                   clientIp, endpoint, message);
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setHeader("Content-Type", "application/json");
        
        String jsonResponse = String.format("""
            {
                "error": "Invalid request",
                "message": "%s",
                "timestamp": "%s"
            }
            """, message, java.time.Instant.now().toString());
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}