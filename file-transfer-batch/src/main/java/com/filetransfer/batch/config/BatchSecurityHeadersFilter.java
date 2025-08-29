package com.filetransfer.batch.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter for batch application
 * Adds comprehensive security headers to all responses
 */
@Component
public class BatchSecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        // Content Security Policy
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' https:; " +
            "connect-src 'self' https:; " +
            "media-src 'self'; " +
            "object-src 'none'; " +
            "frame-src 'none'; " +
            "worker-src 'self'; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'; " +
            "base-uri 'self'");

        // HTTP Strict Transport Security
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");

        // X-Content-Type-Options
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options
        response.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), " +
            "magnetometer=(), gyroscope=(), speaker=(), vibrate=(), fullscreen=(self)");

        // Cache Control for sensitive endpoints
        if (isSensitiveEndpoint(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // Server header removal (security through obscurity)
        response.setHeader("Server", "BatchProcessor");

        // Custom security headers
        response.setHeader("X-Batch-Security", "enabled");
        response.setHeader("X-Request-ID", generateRequestId(request));

        filterChain.doFilter(request, response);
    }

    /**
     * Check if endpoint contains sensitive information
     */
    private boolean isSensitiveEndpoint(String uri) {
        return uri.contains("/jobs/") || 
               uri.contains("/statistics/") || 
               uri.contains("/performance/") ||
               uri.contains("/actuator/");
    }

    /**
     * Generate unique request ID for tracing
     */
    private String generateRequestId(HttpServletRequest request) {
        String existingId = request.getHeader("X-Request-ID");
        if (existingId != null) {
            return existingId;
        }
        
        return "batch-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(request.hashCode());
    }
}