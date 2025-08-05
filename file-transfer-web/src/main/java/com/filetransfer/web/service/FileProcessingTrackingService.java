package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class FileProcessingTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingTrackingService.class);
    
    // In-memory tracking for file counts per service per day
    // In production, this should be stored in database
    private final ConcurrentHashMap<String, AtomicInteger> dailyFileCounts = new ConcurrentHashMap<>();
    
    @Autowired
    private AlertService alertService;
    
    /**
     * Track a processed data file
     */
    public void trackDataFile(String tenantId, String serviceType, String fileName) {
        String key = generateKey(tenantId, serviceType);
        dailyFileCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Log the tracking
        logger.info("Tracked data file: {} for service: {} (Total today: {})", 
                   fileName, serviceType, getDailyFileCount(tenantId, serviceType));
    }
    
    /**
     * Get the total number of data files processed today for a service
     */
    public int getDailyFileCount(String tenantId, String serviceType) {
        String key = generateKey(tenantId, serviceType);
        AtomicInteger counter = dailyFileCounts.get(key);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Validate EOT file against expected data file count
     */
    public EotValidationResult validateEotFile(String tenantId, String serviceType, 
                                             String eotContent, String totalFilesField) {
        try {
            // Parse EOT file to extract total files count
            int expectedCount = extractTotalFilesFromEot(eotContent, totalFilesField);
            int actualCount = getDailyFileCount(tenantId, serviceType);
            
            EotValidationResult result = new EotValidationResult();
            result.setValid(expectedCount == actualCount);
            result.setExpectedCount(expectedCount);
            result.setActualCount(actualCount);
            
            if (expectedCount != actualCount) {
                result.setMessage(String.format("EOT file mismatch: Expected %d files, but processed %d files", 
                                              expectedCount, actualCount));
                
                // Generate alert for mismatch
                generateMismatchAlert(tenantId, serviceType, expectedCount, actualCount);
            } else {
                result.setMessage("EOT file validation passed - file count matches");
            }
            
            return result;
            
        } catch (Exception e) {
            EotValidationResult result = new EotValidationResult();
            result.setValid(false);
            result.setMessage("Error validating EOT file: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Extract total files count from EOT file content
     */
    private int extractTotalFilesFromEot(String eotContent, String totalFilesField) {
        // This is a simplified implementation
        // In production, you would parse the EOT file based on its schema
        
        if (eotContent == null || totalFilesField == null) {
            throw new IllegalArgumentException("EOT content or total files field is null");
        }
        
        // Simple CSV parsing (assuming EOT is CSV format)
        String[] lines = eotContent.split("\n");
        if (lines.length > 0) {
            String headerLine = lines[0];
            String[] headers = headerLine.split(",");
            
            // Find the column index for total files field
            int totalFilesIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(totalFilesField.trim())) {
                    totalFilesIndex = i;
                    break;
                }
            }
            
            if (totalFilesIndex == -1) {
                throw new IllegalArgumentException("Total files field '" + totalFilesField + "' not found in EOT file");
            }
            
            // Parse the first data line (assuming single line EOT file)
            if (lines.length > 1) {
                String dataLine = lines[1];
                String[] values = dataLine.split(",");
                
                if (totalFilesIndex < values.length) {
                    try {
                        return Integer.parseInt(values[totalFilesIndex].trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format in total files field");
                    }
                }
            }
        }
        
        throw new IllegalArgumentException("Could not extract total files count from EOT file");
    }
    
    /**
     * Generate alert for EOT validation mismatch
     */
    private void generateMismatchAlert(String tenantId, String serviceType, int expectedCount, int actualCount) {
        String alertMessage = String.format(
            "EOT Validation Mismatch - Service: %s, Tenant: %s, Expected: %d, Actual: %d, Difference: %d",
            serviceType, tenantId, expectedCount, actualCount, Math.abs(expectedCount - actualCount)
        );
        
        // Create alert
        AlertDto alert = new AlertDto();
        alert.setTenantId(tenantId);
        alert.setServiceType(serviceType);
        alert.setAlertType("EOT_VALIDATION_MISMATCH");
        alert.setSeverity("WARNING");
        alert.setMessage(alertMessage);
        alert.setCreatedAt(LocalDateTime.now());
        
        alertService.createAlert(alert);
        
        logger.info("Alert generated: {}", alertMessage);
    }
    
    /**
     * Reset daily file count (called at start of new day)
     */
    public void resetDailyFileCount(String tenantId, String serviceType) {
        String key = generateKey(tenantId, serviceType);
        dailyFileCounts.remove(key);
        logger.info("Reset daily file count for service: {} (tenant: {})", serviceType, tenantId);
    }
    
    /**
     * Generate unique key for tracking
     */
    private String generateKey(String tenantId, String serviceType) {
        return tenantId + ":" + serviceType + ":" + LocalDate.now();
    }
    
    /**
     * EOT validation result
     */
    public static class EotValidationResult {
        private boolean valid;
        private String message;
        private int expectedCount;
        private int actualCount;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getExpectedCount() { return expectedCount; }
        public void setExpectedCount(int expectedCount) { this.expectedCount = expectedCount; }
        
        public int getActualCount() { return actualCount; }
        public void setActualCount(int actualCount) { this.actualCount = actualCount; }
    }
    
    // Simple AlertDto class for alert generation
    public static class AlertDto {
        private String tenantId;
        private String serviceType;
        private String alertType;
        private String severity;
        private String message;
        private LocalDateTime createdAt;
        
        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}