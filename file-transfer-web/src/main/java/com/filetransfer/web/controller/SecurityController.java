package com.filetransfer.web.controller;

import com.filetransfer.web.service.RateLimitingService;
import com.filetransfer.web.service.RateLimitingService.RateLimitType;
import com.filetransfer.web.service.RateLimitingService.RateLimitInfo;
import com.filetransfer.web.service.EncryptionService;
import com.filetransfer.web.service.InputValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for security management operations
 * Provides endpoints for rate limiting, encryption, and security monitoring
 */
@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*")
public class SecurityController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private InputValidationService inputValidationService;

    /**
     * Get rate limiting status for a specific identifier
     */
    @GetMapping("/rate-limit/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @RequestParam String identifier,
            @RequestParam String type) {
        
        try {
            RateLimitType limitType = RateLimitType.valueOf(type.toUpperCase());
            RateLimitInfo info = rateLimitingService.getRateLimitInfo(identifier, limitType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("identifier", identifier);
            response.put("type", type);
            response.put("availableTokens", info.getAvailableTokens());
            response.put("capacity", info.getCapacity());
            response.put("isLimited", info.isLimited());
            response.put("usagePercentage", info.getUsagePercentage());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid rate limit type");
            errorResponse.put("message", "Type must be one of: API_GENERAL, LOGIN_ATTEMPTS, FILE_UPLOAD, TENANT_SPECIFIC, ADMIN_OPERATIONS, BULK_OPERATIONS");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error getting rate limit status", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Reset rate limit for a specific identifier (admin only)
     */
    @PostMapping("/rate-limit/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetRateLimit(
            @RequestParam String identifier,
            @RequestParam String type) {
        
        try {
            RateLimitType limitType = RateLimitType.valueOf(type.toUpperCase());
            rateLimitingService.resetRateLimit(identifier, limitType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rate limit reset successfully");
            response.put("identifier", identifier);
            response.put("type", type);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Rate limit reset by admin for identifier: {} type: {}", identifier, type);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid rate limit type");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error resetting rate limit", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get overall rate limiting statistics
     */
    @GetMapping("/rate-limit/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRateLimitStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // Get statistics for each rate limit type
            for (RateLimitType type : RateLimitType.values()) {
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("name", type.name());
                typeStats.put("refillPeriod", type.getRefillPeriod().toString());
                // Add more statistics as needed
                statistics.put(type.name().toLowerCase(), typeStats);
            }
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Validate input data using validation service
     */
    @PostMapping("/validate/input")
    public ResponseEntity<Map<String, Object>> validateInput(@RequestBody Map<String, Object> request) {
        try {
            String inputType = (String) request.get("type");
            String inputValue = (String) request.get("value");
            
            if (inputType == null || inputValue == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters");
                errorResponse.put("message", "Both 'type' and 'value' are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            InputValidationService.ValidationResult result;
            
            switch (inputType.toLowerCase()) {
                case "tenant_id" -> result = inputValidationService.validateTenantId(inputValue);
                case "service_name" -> result = inputValidationService.validateServiceName(inputValue);
                case "file_name" -> result = inputValidationService.validateFileName(inputValue);
                case "email" -> result = inputValidationService.validateEmail(inputValue);
                case "url" -> result = inputValidationService.validateUrl(inputValue);
                case "password" -> result = inputValidationService.validatePassword(inputValue);
                case "text" -> {
                    Integer maxLength = (Integer) request.getOrDefault("maxLength", 1000);
                    Boolean required = (Boolean) request.getOrDefault("required", false);
                    result = inputValidationService.validateText(inputValue, maxLength, required);
                }
                default -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid input type");
                    errorResponse.put("message", "Supported types: tenant_id, service_name, file_name, email, url, password, text");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("sanitizedValue", result.getSanitizedValue());
            response.put("errors", result.getErrors());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating input", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Generate secure tokens (admin only)
     */
    @PostMapping("/tokens/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateSecureToken(@RequestBody Map<String, Object> request) {
        try {
            String tokenType = (String) request.get("type");
            
            if (tokenType == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing token type");
                errorResponse.put("message", "Supported types: api_key, session_token, custom");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String token;
            switch (tokenType.toLowerCase()) {
                case "api_key" -> token = encryptionService.generateApiKey();
                case "session_token" -> token = encryptionService.generateSessionToken();
                case "custom" -> {
                    Integer length = (Integer) request.getOrDefault("length", 32);
                    token = encryptionService.generateSecureToken(length);
                }
                default -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid token type");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", tokenType);
            response.put("generatedAt", System.currentTimeMillis());
            
            logger.info("Secure token generated: type={}", tokenType);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating secure token", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Hash data using secure algorithms
     */
    @PostMapping("/hash")
    public ResponseEntity<Map<String, Object>> hashData(@RequestBody Map<String, Object> request) {
        try {
            String data = (String) request.get("data");
            String algorithm = (String) request.getOrDefault("algorithm", "sha256");
            String salt = (String) request.get("salt");
            
            if (data == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing data to hash");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String hash;
            switch (algorithm.toLowerCase()) {
                case "sha256" -> hash = encryptionService.generateHash(data);
                case "sha512" -> {
                    if (salt == null) {
                        salt = "default-salt";
                    }
                    hash = encryptionService.generateSecureHash(data, salt);
                }
                case "bcrypt" -> hash = encryptionService.hashPassword(data);
                default -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Unsupported algorithm");
                    errorResponse.put("message", "Supported algorithms: sha256, sha512, bcrypt");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hash", hash);
            response.put("algorithm", algorithm);
            if (salt != null) {
                response.put("salt", salt);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error hashing data", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Verify data integrity
     */
    @PostMapping("/verify/integrity")
    public ResponseEntity<Map<String, Object>> verifyDataIntegrity(@RequestBody Map<String, Object> request) {
        try {
            String data = (String) request.get("data");
            String expectedHash = (String) request.get("expectedHash");
            
            if (data == null || expectedHash == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters");
                errorResponse.put("message", "Both 'data' and 'expectedHash' are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            boolean isValid = encryptionService.verifyDataIntegrity(data, expectedHash);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("message", isValid ? "Data integrity verified" : "Data integrity check failed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying data integrity", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get security configuration (admin only)
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecurityConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Rate limiting configuration
            Map<String, Object> rateLimitConfig = new HashMap<>();
            rateLimitConfig.put("enabled", true);
            rateLimitConfig.put("types", RateLimitType.values());
            config.put("rateLimiting", rateLimitConfig);
            
            // Encryption configuration
            Map<String, Object> encryptionConfig = new HashMap<>();
            encryptionConfig.put("algorithm", "AES-GCM");
            encryptionConfig.put("keyLength", "256-bit");
            encryptionConfig.put("hashAlgorithm", "SHA-256");
            config.put("encryption", encryptionConfig);
            
            // Security headers configuration
            Map<String, Object> headersConfig = new HashMap<>();
            headersConfig.put("frameOptions", "DENY");
            headersConfig.put("contentTypeOptions", "nosniff");
            headersConfig.put("xssProtection", "1; mode=block");
            headersConfig.put("hstsEnabled", true);
            config.put("headers", headersConfig);
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("Error getting security configuration", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Security health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSecurityHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("rateLimiting", "OPERATIONAL");
            health.put("encryption", "OPERATIONAL");
            health.put("inputValidation", "OPERATIONAL");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error checking security health", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}