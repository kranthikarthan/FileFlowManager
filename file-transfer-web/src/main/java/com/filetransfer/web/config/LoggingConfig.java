package com.filetransfer.web.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Configuration for structured logging
 * Adds correlation IDs and request context to logs
 */
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String TENANT_ID_MDC_KEY = "tenantId";

    /**
     * Logging interceptor to add correlation IDs to MDC
     */
    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor());
    }

    /**
     * Interceptor that adds correlation context to logs
     */
    public static class LoggingInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // Get or generate correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Generate unique request ID
            String requestId = UUID.randomUUID().toString();

            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(REQUEST_ID_MDC_KEY, requestId);

            // Extract user context if available
            String userId = extractUserId(request);
            String tenantId = extractTenantId(request);

            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }
            if (tenantId != null) {
                MDC.put(TENANT_ID_MDC_KEY, tenantId);
            }

            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                  Object handler, Exception ex) {
            // Clear MDC after request completion
            MDC.clear();
        }

        /**
         * Extract user ID from request (from JWT token, session, etc.)
         */
        private String extractUserId(HttpServletRequest request) {
            // Extract from Authorization header, JWT token, or session
            // Implementation depends on your authentication mechanism
            try {
                // Example: Extract from JWT token
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // Parse JWT token to extract user ID
                    // This is a placeholder - implement based on your JWT structure
                    return "user-from-jwt";
                }
                
                // Example: Extract from request parameter
                String userParam = request.getParameter("userId");
                if (userParam != null) {
                    return userParam;
                }
            } catch (Exception e) {
                // Log error but don't fail the request
            }
            return null;
        }

        /**
         * Extract tenant ID from request
         */
        private String extractTenantId(HttpServletRequest request) {
            // Extract from header, path parameter, or JWT token
            try {
                // Check X-Tenant-ID header
                String tenantHeader = request.getHeader("X-Tenant-ID");
                if (tenantHeader != null) {
                    return tenantHeader;
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

                // Extract from request parameter
                String tenantParam = request.getParameter("tenantId");
                if (tenantParam != null) {
                    return tenantParam;
                }
            } catch (Exception e) {
                // Log error but don't fail the request
            }
            return null;
        }
    }
}