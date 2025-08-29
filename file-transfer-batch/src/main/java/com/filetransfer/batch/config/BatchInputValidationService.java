package com.filetransfer.batch.config;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Input validation and sanitization service for batch application
 */
@Service
public class BatchInputValidationService {

    private final PolicyFactory htmlSanitizer;
    
    // Common validation patterns
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");

    public BatchInputValidationService() {
        // Create HTML sanitizer policy
        this.htmlSanitizer = new HtmlPolicyBuilder()
            .allowElements("b", "i", "em", "strong", "p", "br")
            .allowTextNodes()
            .toFactory();
    }

    /**
     * Sanitize general input
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potential script tags and dangerous characters
        String sanitized = input
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<iframe[^>]*>.*?</iframe>", "")
            .replaceAll("javascript:", "")
            .replaceAll("vbscript:", "")
            .replaceAll("on\\w+\\s*=", "")
            .replaceAll("\\x00", "")
            .trim();
        
        // Use OWASP HTML sanitizer for additional protection
        return htmlSanitizer.sanitize(sanitized);
    }

    /**
     * Validate tenant ID
     */
    public boolean isValidTenantId(String tenantId) {
        return tenantId != null && TENANT_ID_PATTERN.matcher(tenantId).matches();
    }

    /**
     * Validate job name
     */
    public boolean isValidJobName(String jobName) {
        return jobName != null && JOB_NAME_PATTERN.matcher(jobName).matches();
    }

    /**
     * Validate email address
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate alphanumeric string
     */
    public boolean isAlphanumeric(String input) {
        return input != null && ALPHANUMERIC_PATTERN.matcher(input).matches();
    }

    /**
     * Validate integer range
     */
    public boolean isValidIntegerRange(String input, int min, int max) {
        try {
            int value = Integer.parseInt(input);
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate string length
     */
    public boolean isValidLength(String input, int maxLength) {
        return input != null && input.length() <= maxLength;
    }

    /**
     * Sanitize file path
     */
    public String sanitizeFilePath(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        // Remove path traversal attempts
        return filePath
            .replaceAll("\\.\\./", "")
            .replaceAll("\\.\\.\\\\", "")
            .replaceAll("\\x00", "")
            .trim();
    }

    /**
     * Validate and sanitize JSON input
     */
    public String sanitizeJsonInput(String jsonInput) {
        if (jsonInput == null) {
            return null;
        }
        
        // Basic JSON sanitization
        return jsonInput
            .replaceAll("javascript:", "")
            .replaceAll("<script[^>]*>.*?</script>", "")
            .trim();
    }
}