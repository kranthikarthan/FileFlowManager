package com.filetransfer.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that adds comprehensive security headers to all HTTP responses
 * Implements OWASP security header recommendations
 */
@Component
public class SecurityHeadersFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Add comprehensive security headers
            addSecurityHeaders(httpRequest, httpResponse);

            // Continue with the request
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in security headers filter", e);
            // Continue with request even if header setting fails
            chain.doFilter(request, response);
        }
    }

    /**
     * Add comprehensive security headers to the response
     */
    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        
        // Prevent content type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS Protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Prevent framing (clickjacking protection)
        response.setHeader("X-Frame-Options", "DENY");

        // HTTPS Strict Transport Security
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Content Security Policy
        String csp = buildContentSecurityPolicy(request);
        response.setHeader("Content-Security-Policy", csp);

        // Permissions Policy (formerly Feature Policy)
        response.setHeader("Permissions-Policy", buildPermissionsPolicy());

        // Cache Control for sensitive pages
        if (isSensitivePage(request)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // Server header obfuscation
        response.setHeader("Server", "FileTransfer");

        // Custom security headers
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("X-Download-Options", "noopen");
        response.setHeader("X-DNS-Prefetch-Control", "off");

        // API Version header
        response.setHeader("X-API-Version", "1.0");

        // Security signature
        response.setHeader("X-Security-Policy", "enforced");
    }

    /**
     * Build Content Security Policy based on request type
     */
    private String buildContentSecurityPolicy(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        if (uri.startsWith("/api/")) {
            // Strict CSP for API endpoints
            return "default-src 'none'; " +
                   "connect-src 'self'; " +
                   "frame-ancestors 'none'; " +
                   "base-uri 'none'; " +
                   "form-action 'none'";
        } else if (uri.startsWith("/admin/")) {
            // Very strict CSP for admin pages
            return "default-src 'self'; " +
                   "script-src 'self' 'unsafe-inline'; " +
                   "style-src 'self' 'unsafe-inline'; " +
                   "img-src 'self' data:; " +
                   "font-src 'self'; " +
                   "connect-src 'self'; " +
                   "frame-ancestors 'none'; " +
                   "base-uri 'self'; " +
                   "form-action 'self'";
        } else {
            // Standard CSP for general pages
            return "default-src 'self'; " +
                   "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                   "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                   "img-src 'self' data: https:; " +
                   "font-src 'self' https://fonts.gstatic.com; " +
                   "connect-src 'self' https:; " +
                   "media-src 'self'; " +
                   "object-src 'none'; " +
                   "child-src 'self'; " +
                   "frame-ancestors 'none'; " +
                   "base-uri 'self'; " +
                   "form-action 'self'";
        }
    }

    /**
     * Build Permissions Policy to restrict browser features
     */
    private String buildPermissionsPolicy() {
        return "accelerometer=(), " +
               "ambient-light-sensor=(), " +
               "autoplay=(), " +
               "battery=(), " +
               "camera=(), " +
               "cross-origin-isolated=(), " +
               "display-capture=(), " +
               "document-domain=(), " +
               "encrypted-media=(), " +
               "execution-while-not-rendered=(), " +
               "execution-while-out-of-viewport=(), " +
               "fullscreen=(), " +
               "geolocation=(), " +
               "gyroscope=(), " +
               "keyboard-map=(), " +
               "magnetometer=(), " +
               "microphone=(), " +
               "midi=(), " +
               "navigation-override=(), " +
               "payment=(), " +
               "picture-in-picture=(), " +
               "publickey-credentials-get=(), " +
               "screen-wake-lock=(), " +
               "sync-xhr=(), " +
               "usb=(), " +
               "web-share=(), " +
               "xr-spatial-tracking=()";
    }

    /**
     * Check if the page contains sensitive information that should not be cached
     */
    private boolean isSensitivePage(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        
        return uri.contains("/auth/") ||
               uri.contains("/login") ||
               uri.contains("/admin/") ||
               uri.contains("/api/auth/") ||
               uri.contains("/password") ||
               uri.contains("/token") ||
               uri.contains("/oauth/") ||
               uri.contains("/profile") ||
               uri.contains("/settings");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Security Headers Filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("Security Headers Filter destroyed");
    }
}