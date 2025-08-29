package com.filetransfer.web.config;

import com.filetransfer.web.service.InputValidationService;
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
 * Filter that validates and sanitizes HTTP request parameters and headers
 * Provides protection against common injection attacks
 */
@Component
public class InputValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationFilter.class);

    @Autowired
    private InputValidationService inputValidationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Skip validation for certain endpoints
            if (shouldSkipValidation(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            // Validate request parameters
            ValidationResult paramValidation = validateRequestParameters(httpRequest);
            if (!paramValidation.isValid()) {
                handleValidationFailure(httpResponse, paramValidation);
                return;
            }

            // Validate request headers
            ValidationResult headerValidation = validateRequestHeaders(httpRequest);
            if (!headerValidation.isValid()) {
                handleValidationFailure(httpResponse, headerValidation);
                return;
            }

            // Validate path parameters
            ValidationResult pathValidation = validatePathParameters(httpRequest);
            if (!pathValidation.isValid()) {
                handleValidationFailure(httpResponse, pathValidation);
                return;
            }

            // Continue with the request
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in input validation filter", e);
            // Continue with request if validation fails
            chain.doFilter(request, response);
        }
    }

    /**
     * Check if validation should be skipped for this request
     */
    private boolean shouldSkipValidation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // Skip validation for certain endpoints
        return uri.startsWith("/actuator") ||
               uri.startsWith("/health") ||
               uri.startsWith("/static") ||
               uri.startsWith("/public") ||
               uri.equals("/") ||
               uri.equals("/index.html");
    }

    /**
     * Validate all request parameters
     */
    private ValidationResult validateRequestParameters(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            
            for (String paramValue : paramValues) {
                ValidationResult result = validateParameter(paramName, paramValue);
                if (!result.isValid()) {
                    logger.warn("Invalid parameter detected: {} = {}", paramName, paramValue);
                    return result;
                }
            }
        }
        
        return new ValidationResult(true, "Parameters validated successfully");
    }

    /**
     * Validate a single parameter
     */
    private ValidationResult validateParameter(String paramName, String paramValue) {
        if (paramValue == null) {
            return new ValidationResult(true, "Parameter is null");
        }

        // Check parameter length
        if (paramValue.length() > 1000) {
            return new ValidationResult(false, 
                "Parameter '" + paramName + "' exceeds maximum length");
        }

        // Validate based on parameter name
        return switch (paramName.toLowerCase()) {
            case "tenantid", "tenant_id" -> {
                var result = inputValidationService.validateTenantId(paramValue);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid tenant ID" : "Invalid tenant ID");
            }
            case "servicename", "service_name" -> {
                var result = inputValidationService.validateServiceName(paramValue);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid service name" : "Invalid service name");
            }
            case "filename", "file_name" -> {
                var result = inputValidationService.validateFileName(paramValue);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid file name" : "Invalid file name");
            }
            case "email" -> {
                var result = inputValidationService.validateEmail(paramValue);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid email" : "Invalid email");
            }
            case "url", "endpoint" -> {
                var result = inputValidationService.validateUrl(paramValue);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid URL" : "Invalid URL");
            }
            default -> {
                var result = inputValidationService.validateText(paramValue, 500, false);
                yield new ValidationResult(result.isValid(), 
                    result.isValid() ? "Valid text" : "Invalid text content");
            }
        };
    }

    /**
     * Validate request headers
     */
    private ValidationResult validateRequestHeaders(HttpServletRequest request) {
        // Validate critical headers
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null) {
            var result = inputValidationService.validateTenantId(tenantId);
            if (!result.isValid()) {
                logger.warn("Invalid X-Tenant-ID header: {}", tenantId);
                return new ValidationResult(false, "Invalid X-Tenant-ID header");
            }
        }

        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null) {
            var result = inputValidationService.validateAlphanumeric(
                correlationId.replaceAll("-", ""), 50, false);
            if (!result.isValid()) {
                logger.warn("Invalid X-Correlation-ID header: {}", correlationId);
                return new ValidationResult(false, "Invalid X-Correlation-ID header");
            }
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            logger.warn("Excessively long User-Agent header: {}", userAgent.substring(0, 100) + "...");
            return new ValidationResult(false, "User-Agent header too long");
        }

        // Check for potentially malicious headers
        String[] suspiciousHeaders = {"X-Forwarded-Host", "X-Original-URL", "X-Rewrite-URL"};
        for (String headerName : suspiciousHeaders) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && containsSuspiciousContent(headerValue)) {
                logger.warn("Suspicious header detected: {} = {}", headerName, headerValue);
                return new ValidationResult(false, "Suspicious header content detected");
            }
        }

        return new ValidationResult(true, "Headers validated successfully");
    }

    /**
     * Validate path parameters extracted from URL
     */
    private ValidationResult validatePathParameters(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // Check for path traversal attempts
        if (requestURI.contains("../") || requestURI.contains("..\\")) {
            logger.warn("Path traversal attempt detected: {}", requestURI);
            return new ValidationResult(false, "Path traversal attempt detected");
        }

        // Check for encoded path traversal
        if (requestURI.contains("%2e%2e") || requestURI.contains("%2E%2E")) {
            logger.warn("Encoded path traversal attempt detected: {}", requestURI);
            return new ValidationResult(false, "Encoded path traversal attempt detected");
        }

        // Validate individual path segments
        String[] pathSegments = requestURI.split("/");
        for (String segment : pathSegments) {
            if (!segment.isEmpty() && containsSuspiciousContent(segment)) {
                logger.warn("Suspicious path segment detected: {}", segment);
                return new ValidationResult(false, "Suspicious path content detected");
            }
        }

        return new ValidationResult(true, "Path validated successfully");
    }

    /**
     * Check if content contains suspicious patterns
     */
    private boolean containsSuspiciousContent(String content) {
        if (content == null) {
            return false;
        }

        String lowercaseContent = content.toLowerCase();
        
        // Common attack patterns
        String[] suspiciousPatterns = {
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "eval(", "exec(", "system(", "../", "..\\",
            "union select", "drop table", "insert into", "delete from",
            "cmd.exe", "/bin/sh", "nc -", "wget ", "curl ",
            "${", "#{", "%{", "{{", "<%", "%>"
        };

        for (String pattern : suspiciousPatterns) {
            if (lowercaseContent.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handle validation failure by sending appropriate error response
     */
    private void handleValidationFailure(HttpServletResponse response, ValidationResult validationResult) 
            throws IOException {
        
        // Log security event
        logger.warn("Input validation failed: {}", validationResult.getMessage());

        // Set response headers
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Input Validation Failed");
        errorResponse.put("message", "Request contains invalid or potentially malicious content");
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("timestamp", System.currentTimeMillis());

        // Write response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Simple validation result container
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Input Validation Filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("Input Validation Filter destroyed");
    }
}