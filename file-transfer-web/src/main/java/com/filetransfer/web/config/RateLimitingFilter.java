package com.filetransfer.web.config;

import com.filetransfer.web.service.RateLimitingService;
import com.filetransfer.web.service.RateLimitingService.RateLimitType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet filter that implements rate limiting for HTTP requests
 * Uses different rate limiting strategies based on request type and endpoint
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for actuator endpoints
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.startsWith("/actuator") || requestURI.startsWith("/health")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Determine rate limiting strategy
            RateLimitStrategy strategy = determineRateLimitStrategy(httpRequest);
            
            if (strategy == null) {
                // No rate limiting required
                chain.doFilter(request, response);
                return;
            }

            // Check rate limit
            boolean allowed = rateLimitingService.isAllowed(
                strategy.identifier, 
                strategy.type, 
                strategy.tokens
            );

            if (!allowed) {
                handleRateLimitExceeded(httpRequest, httpResponse, strategy);
                return;
            }

            // Add rate limit headers to response
            addRateLimitHeaders(httpResponse, strategy);

            // Continue with the request
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            // Continue with request if rate limiting fails
            chain.doFilter(request, response);
        }
    }

    /**
     * Determine rate limiting strategy based on request
     */
    private RateLimitStrategy determineRateLimitStrategy(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIP = getClientIP(request);
        String tenantId = extractTenantId(request);
        String userId = extractUserId(request);

        // Determine identifier (prefer user ID, fallback to tenant, then IP)
        String identifier = userId != null ? "user:" + userId : 
                           tenantId != null ? "tenant:" + tenantId : 
                           "ip:" + clientIP;

        // Login attempts - strict rate limiting
        if (requestURI.contains("/auth/login") || requestURI.contains("/oauth/token")) {
            return new RateLimitStrategy(
                "ip:" + clientIP, 
                RateLimitType.LOGIN_ATTEMPTS, 
                1
            );
        }

        // File upload endpoints - moderate rate limiting
        if (requestURI.contains("/files/upload") || 
            (requestURI.contains("/api/files") && "POST".equals(method))) {
            return new RateLimitStrategy(
                identifier, 
                RateLimitType.FILE_UPLOAD, 
                getFileUploadTokens(request)
            );
        }

        // Admin operations - strict rate limiting
        if (requestURI.contains("/admin/") || requestURI.contains("/management/")) {
            return new RateLimitStrategy(
                identifier, 
                RateLimitType.ADMIN_OPERATIONS, 
                1
            );
        }

        // Bulk operations - very strict rate limiting
        if (requestURI.contains("/bulk/") || 
            requestURI.contains("/batch/") ||
            request.getHeader("X-Bulk-Operation") != null) {
            return new RateLimitStrategy(
                identifier, 
                RateLimitType.BULK_OPERATIONS, 
                getBulkOperationTokens(request)
            );
        }

        // Tenant-specific API endpoints
        if (tenantId != null && requestURI.startsWith("/api/")) {
            return new RateLimitStrategy(
                "tenant:" + tenantId, 
                RateLimitType.TENANT_SPECIFIC, 
                1
            );
        }

        // General API rate limiting
        if (requestURI.startsWith("/api/")) {
            return new RateLimitStrategy(
                identifier, 
                RateLimitType.API_GENERAL, 
                1
            );
        }

        // No rate limiting for other requests
        return null;
    }

    /**
     * Handle rate limit exceeded response
     */
    private void handleRateLimitExceeded(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       RateLimitStrategy strategy) throws IOException {
        
        // Log rate limit violation
        logger.warn("Rate limit exceeded for identifier: {} type: {} URI: {} IP: {}", 
                   strategy.identifier, strategy.type, request.getRequestURI(), getClientIP(request));

        // Set response headers
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-RateLimit-Limit", String.valueOf(getRateLimitCapacity(strategy.type)));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)); // 1 minute
        response.setHeader("Retry-After", "60"); // Retry after 60 seconds

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());

        // Write response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Add rate limit headers to successful responses
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitStrategy strategy) {
        try {
            RateLimitingService.RateLimitInfo info = rateLimitingService.getRateLimitInfo(
                strategy.identifier, strategy.type);
                
            response.setHeader("X-RateLimit-Limit", String.valueOf(info.getCapacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getAvailableTokens()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000));
        } catch (Exception e) {
            logger.debug("Failed to add rate limit headers", e);
        }
    }

    /**
     * Extract client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Extract tenant ID from request
     */
    private String extractTenantId(HttpServletRequest request) {
        // Check header
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null) {
            return tenantId;
        }

        // Check path parameter
        String uri = request.getRequestURI();
        if (uri.contains("/tenant/")) {
            String[] pathParts = uri.split("/");
            for (int i = 0; i < pathParts.length - 1; i++) {
                if ("tenant".equals(pathParts[i]) && i + 1 < pathParts.length) {
                    return pathParts[i + 1];
                }
            }
        }

        // Check request parameter
        return request.getParameter("tenantId");
    }

    /**
     * Extract user ID from request (from JWT token, session, etc.)
     */
    private String extractUserId(HttpServletRequest request) {
        // This would typically extract from JWT token or session
        // For now, return null - implement based on your authentication mechanism
        return null;
    }

    /**
     * Calculate tokens needed for file upload based on file size
     */
    private int getFileUploadTokens(HttpServletRequest request) {
        try {
            String contentLength = request.getHeader("Content-Length");
            if (contentLength != null) {
                long fileSize = Long.parseLong(contentLength);
                // 1 token per MB, minimum 1 token
                return Math.max(1, (int) (fileSize / (1024 * 1024)));
            }
        } catch (NumberFormatException e) {
            logger.debug("Invalid Content-Length header", e);
        }
        return 1; // Default to 1 token
    }

    /**
     * Calculate tokens needed for bulk operations
     */
    private int getBulkOperationTokens(HttpServletRequest request) {
        try {
            String batchSize = request.getHeader("X-Batch-Size");
            if (batchSize != null) {
                int size = Integer.parseInt(batchSize);
                // Scale tokens based on batch size
                return Math.max(1, size / 10);
            }
        } catch (NumberFormatException e) {
            logger.debug("Invalid X-Batch-Size header", e);
        }
        return 3; // Default to 3 tokens for bulk operations
    }

    /**
     * Get rate limit capacity for a given type
     */
    private long getRateLimitCapacity(RateLimitType type) {
        return switch (type) {
            case API_GENERAL -> 60;
            case LOGIN_ATTEMPTS -> 10;
            case FILE_UPLOAD -> 10;
            case TENANT_SPECIFIC -> 1000;
            case ADMIN_OPERATIONS -> 20;
            case BULK_OPERATIONS -> 5;
        };
    }

    /**
     * Rate limiting strategy container
     */
    private static class RateLimitStrategy {
        final String identifier;
        final RateLimitType type;
        final int tokens;

        RateLimitStrategy(String identifier, RateLimitType type, int tokens) {
            this.identifier = identifier;
            this.type = type;
            this.tokens = tokens;
        }
    }
}