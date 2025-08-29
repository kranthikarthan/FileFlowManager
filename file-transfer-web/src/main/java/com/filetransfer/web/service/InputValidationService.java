package com.filetransfer.web.service;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.passay.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for comprehensive input validation and sanitization
 * Implements OWASP security guidelines for input validation
 */
@Service
public class InputValidationService {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationService.class);

    // HTML sanitization policy - very restrictive
    private static final PolicyFactory HTML_SANITIZER = new HtmlPolicyBuilder()
        .allowElements("b", "i", "em", "strong", "br", "p")
        .allowStandardUrlProtocols()
        .requireRelNofollowOnLinks()
        .toFactory();

    // Common regex patterns for validation
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,100}$");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(select|insert|update|delete|drop|create|alter|exec|execute|union|script|javascript|vbscript)", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|vbscript:|onload|onerror|onclick)", 
        Pattern.CASE_INSENSITIVE
    );

    // Email and URL validators
    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    // Password policy
    private static final PasswordValidator PASSWORD_VALIDATOR = new PasswordValidator(Arrays.asList(
        new LengthRule(8, 128),
        new CharacterRule(EnglishCharacterData.UpperCase, 1),
        new CharacterRule(EnglishCharacterData.LowerCase, 1),
        new CharacterRule(EnglishCharacterData.Digit, 1),
        new CharacterRule(EnglishCharacterData.Special, 1),
        new WhitespaceRule(),
        new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false),
        new IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false),
        new IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false),
        new DictionaryRule()
    ));

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String sanitizedValue;
        private final List<String> errors;

        public ValidationResult(boolean valid, String sanitizedValue, List<String> errors) {
            this.valid = valid;
            this.sanitizedValue = sanitizedValue;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public String getSanitizedValue() {
            return sanitizedValue;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Sanitize HTML input to prevent XSS attacks
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return HTML_SANITIZER.sanitize(input);
    }

    /**
     * Validate and sanitize general text input
     */
    public ValidationResult validateText(String input, int maxLength, boolean required) {
        if (input == null) {
            if (required) {
                return new ValidationResult(false, null, List.of("Input is required"));
            }
            return new ValidationResult(true, null, List.of());
        }

        String trimmed = input.trim();
        
        if (required && trimmed.isEmpty()) {
            return new ValidationResult(false, "", List.of("Input cannot be empty"));
        }

        if (trimmed.length() > maxLength) {
            return new ValidationResult(false, trimmed, 
                List.of("Input exceeds maximum length of " + maxLength + " characters"));
        }

        // Check for common attack patterns
        if (containsMaliciousContent(trimmed)) {
            logger.warn("Potentially malicious input detected: {}", trimmed);
            return new ValidationResult(false, trimmed, 
                List.of("Input contains potentially malicious content"));
        }

        // Sanitize HTML
        String sanitized = sanitizeHtml(trimmed);
        
        return new ValidationResult(true, sanitized, List.of());
    }

    /**
     * Validate tenant ID
     */
    public ValidationResult validateTenantId(String tenantId) {
        if (tenantId == null) {
            return new ValidationResult(false, null, List.of("Tenant ID is required"));
        }

        String trimmed = tenantId.trim();
        
        if (!TENANT_ID_PATTERN.matcher(trimmed).matches()) {
            return new ValidationResult(false, trimmed, 
                List.of("Tenant ID must be 3-50 characters and contain only letters, numbers, hyphens, and underscores"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Validate service name
     */
    public ValidationResult validateServiceName(String serviceName) {
        if (serviceName == null) {
            return new ValidationResult(false, null, List.of("Service name is required"));
        }

        String trimmed = serviceName.trim();
        
        if (!SERVICE_NAME_PATTERN.matcher(trimmed).matches()) {
            return new ValidationResult(false, trimmed, 
                List.of("Service name must be 2-100 characters and contain only letters, numbers, hyphens, and underscores"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Validate file name
     */
    public ValidationResult validateFileName(String fileName) {
        if (fileName == null) {
            return new ValidationResult(false, null, List.of("File name is required"));
        }

        String trimmed = fileName.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationResult(false, trimmed, List.of("File name cannot be empty"));
        }

        if (trimmed.length() > 255) {
            return new ValidationResult(false, trimmed, 
                List.of("File name exceeds maximum length of 255 characters"));
        }

        // Check for path traversal attempts
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            return new ValidationResult(false, trimmed, 
                List.of("File name contains invalid characters"));
        }

        // Basic filename pattern validation
        if (!FILENAME_PATTERN.matcher(trimmed).matches()) {
            return new ValidationResult(false, trimmed, 
                List.of("File name can only contain letters, numbers, dots, hyphens, and underscores"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Validate email address
     */
    public ValidationResult validateEmail(String email) {
        if (email == null) {
            return new ValidationResult(false, null, List.of("Email is required"));
        }

        String trimmed = email.trim().toLowerCase();
        
        if (!EMAIL_VALIDATOR.isValid(trimmed)) {
            return new ValidationResult(false, trimmed, List.of("Invalid email format"));
        }

        if (trimmed.length() > 254) {
            return new ValidationResult(false, trimmed, 
                List.of("Email exceeds maximum length of 254 characters"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Validate URL
     */
    public ValidationResult validateUrl(String url) {
        if (url == null) {
            return new ValidationResult(false, null, List.of("URL is required"));
        }

        String trimmed = url.trim();
        
        if (!URL_VALIDATOR.isValid(trimmed)) {
            return new ValidationResult(false, trimmed, List.of("Invalid URL format"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Validate password according to security policy
     */
    public ValidationResult validatePassword(String password) {
        if (password == null) {
            return new ValidationResult(false, null, List.of("Password is required"));
        }

        RuleResult result = PASSWORD_VALIDATOR.validate(new PasswordData(password));
        
        if (result.isValid()) {
            return new ValidationResult(true, password, List.of());
        } else {
            List<String> errors = PASSWORD_VALIDATOR.getMessages(result);
            return new ValidationResult(false, password, errors);
        }
    }

    /**
     * Validate numeric input
     */
    public ValidationResult validateNumeric(String input, long minValue, long maxValue) {
        if (input == null) {
            return new ValidationResult(false, null, List.of("Numeric input is required"));
        }

        String trimmed = input.trim();
        
        try {
            long value = Long.parseLong(trimmed);
            
            if (value < minValue || value > maxValue) {
                return new ValidationResult(false, trimmed, 
                    List.of("Value must be between " + minValue + " and " + maxValue));
            }
            
            return new ValidationResult(true, trimmed, List.of());
        } catch (NumberFormatException e) {
            return new ValidationResult(false, trimmed, List.of("Invalid numeric format"));
        }
    }

    /**
     * Validate alphanumeric input
     */
    public ValidationResult validateAlphanumeric(String input, int maxLength, boolean required) {
        if (input == null) {
            if (required) {
                return new ValidationResult(false, null, List.of("Input is required"));
            }
            return new ValidationResult(true, null, List.of());
        }

        String trimmed = input.trim();
        
        if (required && trimmed.isEmpty()) {
            return new ValidationResult(false, trimmed, List.of("Input cannot be empty"));
        }

        if (trimmed.length() > maxLength) {
            return new ValidationResult(false, trimmed, 
                List.of("Input exceeds maximum length of " + maxLength + " characters"));
        }

        if (!ALPHANUMERIC_PATTERN.matcher(trimmed).matches()) {
            return new ValidationResult(false, trimmed, 
                List.of("Input can only contain letters and numbers"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }

    /**
     * Check for potentially malicious content
     */
    private boolean containsMaliciousContent(String input) {
        if (input == null) {
            return false;
        }

        String lowercaseInput = input.toLowerCase();
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(lowercaseInput).find()) {
            return true;
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(lowercaseInput).find()) {
            return true;
        }

        // Check for common attack strings
        String[] attackStrings = {
            "../", "..\\", "<script", "javascript:", "vbscript:", 
            "onload=", "onerror=", "onclick=", "eval(", "expression(",
            "exec(", "system(", "cmd.exe", "/bin/sh", "union select",
            "drop table", "truncate table"
        };

        for (String attackString : attackStrings) {
            if (lowercaseInput.contains(attackString)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate JSON string
     */
    public ValidationResult validateJsonString(String jsonString) {
        if (jsonString == null) {
            return new ValidationResult(false, null, List.of("JSON string is required"));
        }

        String trimmed = jsonString.trim();
        
        try {
            // Try to parse JSON to validate structure
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readTree(trimmed);
            
            return new ValidationResult(true, trimmed, List.of());
        } catch (Exception e) {
            return new ValidationResult(false, trimmed, List.of("Invalid JSON format: " + e.getMessage()));
        }
    }

    /**
     * Sanitize input for safe database storage
     */
    public String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }

        // Remove null bytes and control characters
        String sanitized = input.replaceAll("\\x00", "")
                               .replaceAll("\\p{Cntrl}", "");

        // Trim and limit length
        sanitized = sanitized.trim();
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }

        return sanitized;
    }

    /**
     * Validate file path for security
     */
    public ValidationResult validateFilePath(String filePath) {
        if (filePath == null) {
            return new ValidationResult(false, null, List.of("File path is required"));
        }

        String trimmed = filePath.trim();

        // Check for path traversal
        if (trimmed.contains("..")) {
            return new ValidationResult(false, trimmed, 
                List.of("File path contains path traversal sequences"));
        }

        // Check for absolute paths (should be relative)
        if (trimmed.startsWith("/") || trimmed.matches("^[A-Za-z]:.*")) {
            return new ValidationResult(false, trimmed, 
                List.of("Absolute file paths are not allowed"));
        }

        return new ValidationResult(true, trimmed, List.of());
    }
}