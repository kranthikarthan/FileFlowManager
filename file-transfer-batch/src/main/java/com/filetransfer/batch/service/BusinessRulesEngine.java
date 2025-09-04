package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.FileTransferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight business rules engine for batch processing
 */
@Service
public class BusinessRulesEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessRulesEngine.class);
    
    /**
     * Apply intelligent routing decisions based on file characteristics
     */
    public RoutingDecision determineRouting(FileTransferRecord fileTransfer) {
        RoutingDecision decision = new RoutingDecision();
        decision.setOriginalService(fileTransfer.getServiceName());
        decision.setRecommendedService(fileTransfer.getServiceName()); // Default to current
        
        try {
            // Apply routing rules based on file characteristics
            
            // Rule 1: Large files to high-capacity service
            if (fileTransfer.getFileSize() != null && fileTransfer.getFileSize() > 100 * 1024 * 1024) {
                decision.setRecommendedService("HIGH_CAPACITY_PROCESSING");
                decision.setRoutingReason("Large file requires high-capacity processing");
                decision.setPriority(90);
                decision.addRecommendation("Enable streaming processing for large file");
            }
            
            // Rule 2: Customer data to secure service
            if (fileTransfer.getServiceName() != null && fileTransfer.getServiceName().contains("CUSTOMER")) {
                decision.setRecommendedService("SECURE_CUSTOMER_PROCESSING");
                decision.setRoutingReason("Customer data requires secure processing");
                decision.setPriority(95);
                decision.addRecommendation("Enable HSM validation for sensitive data");
                decision.setRequiresApproval(true);
            }
            
            // Rule 3: Time-sensitive routing based on current time
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            
            if (hour >= 18 || hour <= 6) { // After hours
                decision.setRecommendedService("BATCH_PROCESSING");
                decision.setRoutingReason("After-hours processing routed to batch queue");
                decision.setPriority(30);
                decision.addRecommendation("Schedule for next business day processing");
            }
            
            // Rule 4: Extension-based routing
            if (fileTransfer.getFileExtension() != null) {
                switch (fileTransfer.getFileExtension().toLowerCase()) {
                    case ".log":
                        decision.setRecommendedService("LOG_PROCESSING");
                        decision.setRoutingReason("Log file routed to specialized log processor");
                        break;
                    case ".csv":
                    case ".xlsx":
                        decision.setRecommendedService("STRUCTURED_DATA_PROCESSING");
                        decision.setRoutingReason("Structured data file routed to specialized processor");
                        break;
                    case ".zip":
                    case ".gz":
                        decision.setRecommendedService("ARCHIVE_PROCESSING");
                        decision.setRoutingReason("Archive file routed to specialized extractor");
                        decision.addRecommendation("Extract and validate archive contents");
                        break;
                }
            }
            
            // Rule 5: Failed file recovery routing
            if (fileTransfer.getStatus() == TransferStatus.FAILED) {
                decision.setRecommendedService("ERROR_RECOVERY_PROCESSING");
                decision.setRoutingReason("Failed file routed to error recovery workflow");
                decision.setPriority(80);
                decision.addRecommendation("Analyze failure cause and apply recovery procedures");
            }
            
            logger.debug("Routing decision for file {}: {} -> {} (priority: {})", 
                fileTransfer.getFileName(), decision.getOriginalService(), 
                decision.getRecommendedService(), decision.getPriority());
            
        } catch (Exception e) {
            logger.error("Error in routing decision for file {}: {}", fileTransfer.getFileName(), e.getMessage());
            decision.addRecommendation("Manual review required due to routing error");
        }
        
        return decision;
    }
    
    /**
     * Determine processing priority based on file characteristics
     */
    public ProcessingPriority determinePriority(FileTransferRecord fileTransfer) {
        ProcessingPriority priority = new ProcessingPriority();
        priority.setBasePriority(50); // Default priority
        priority.setCalculatedPriority(50);
        
        try {
            int calculatedPriority = 50;
            List<String> reasons = new ArrayList<>();
            
            // Priority factor 1: Service importance
            if (fileTransfer.getServiceName() != null) {
                if (fileTransfer.getServiceName().contains("CRITICAL") || 
                    fileTransfer.getServiceName().contains("URGENT")) {
                    calculatedPriority += 30;
                    reasons.add("Critical/urgent service designation");
                } else if (fileTransfer.getServiceName().contains("CUSTOMER")) {
                    calculatedPriority += 20;
                    reasons.add("Customer-related service");
                } else if (fileTransfer.getServiceName().contains("BATCH") || 
                          fileTransfer.getServiceName().contains("ARCHIVE")) {
                    calculatedPriority -= 20;
                    reasons.add("Batch/archive service - lower priority");
                }
            }
            
            // Priority factor 2: File size (smaller files get higher priority for quick processing)
            if (fileTransfer.getFileSize() != null) {
                if (fileTransfer.getFileSize() < 1024 * 1024) { // < 1MB
                    calculatedPriority += 10;
                    reasons.add("Small file - quick processing");
                } else if (fileTransfer.getFileSize() > 100 * 1024 * 1024) { // > 100MB
                    calculatedPriority -= 10;
                    reasons.add("Large file - resource intensive");
                }
            }
            
            // Priority factor 3: Time of day
            int hour = LocalDateTime.now().getHour();
            if (hour >= 9 && hour <= 17) { // Business hours
                calculatedPriority += 5;
                reasons.add("Business hours processing");
            }
            
            // Priority factor 4: File age (older files get slightly higher priority)
            if (fileTransfer.getCreatedAt() != null) {
                long hoursOld = java.time.Duration.between(fileTransfer.getCreatedAt(), LocalDateTime.now()).toHours();
                if (hoursOld > 24) {
                    calculatedPriority += 15;
                    reasons.add("File older than 24 hours");
                } else if (hoursOld > 4) {
                    calculatedPriority += 5;
                    reasons.add("File older than 4 hours");
                }
            }
            
            // Priority factor 5: Retry attempts (failed files get higher priority for recovery)
            if (fileTransfer.getStatus() == TransferStatus.FAILED) {
                calculatedPriority += 25;
                reasons.add("Failed file requires recovery");
            }
            
            // Ensure priority stays within bounds
            calculatedPriority = Math.max(1, Math.min(100, calculatedPriority));
            
            priority.setCalculatedPriority(calculatedPriority);
            priority.setPriorityReasons(reasons);
            priority.setQueueName(determineQueueName(calculatedPriority));
            
            logger.debug("Priority calculation for file {}: {} (reasons: {})", 
                fileTransfer.getFileName(), calculatedPriority, String.join(", ", reasons));
            
        } catch (Exception e) {
            logger.error("Error calculating priority for file {}: {}", fileTransfer.getFileName(), e.getMessage());
            priority.addReason("Priority calculation error - using default");
        }
        
        return priority;
    }
    
    /**
     * Determine if file should be processed immediately or queued
     */
    public ProcessingDecision shouldProcessImmediately(FileTransferRecord fileTransfer) {
        ProcessingDecision decision = new ProcessingDecision();
        decision.setShouldProcessImmediately(true); // Default to immediate processing
        
        try {
            // Rule 1: Large files during business hours should be queued
            int hour = LocalDateTime.now().getHour();
            if (fileTransfer.getFileSize() != null && fileTransfer.getFileSize() > 50 * 1024 * 1024 && 
                hour >= 9 && hour <= 17) {
                decision.setShouldProcessImmediately(false);
                decision.setReason("Large file during business hours - queue for off-hours processing");
                decision.setRecommendedProcessingTime(LocalDateTime.now().withHour(18).withMinute(0));
            }
            
            // Rule 2: Files requiring approval should be queued
            if (fileTransfer.getServiceName() != null && 
                (fileTransfer.getServiceName().contains("SENSITIVE") || 
                 fileTransfer.getServiceName().contains("CONFIDENTIAL"))) {
                decision.setShouldProcessImmediately(false);
                decision.setReason("Sensitive file requires approval before processing");
                decision.setRequiresApproval(true);
            }
            
            // Rule 3: High priority files should always be processed immediately
            ProcessingPriority priority = determinePriority(fileTransfer);
            if (priority.getCalculatedPriority() >= 80) {
                decision.setShouldProcessImmediately(true);
                decision.setReason("High priority file - process immediately");
            }
            
            // Rule 4: System overload protection
            // In a real implementation, this would check current system load
            // For now, we'll simulate load-based queuing
            if (isSystemOverloaded()) {
                decision.setShouldProcessImmediately(false);
                decision.setReason("System overloaded - queue for later processing");
                decision.setRecommendedProcessingTime(LocalDateTime.now().plusMinutes(30));
            }
            
        } catch (Exception e) {
            logger.error("Error in processing decision for file {}: {}", fileTransfer.getFileName(), e.getMessage());
            decision.setReason("Decision error - defaulting to immediate processing");
        }
        
        return decision;
    }
    
    /**
     * Apply conditional processing rules
     */
    public ConditionalProcessingResult applyConditionalProcessing(FileTransferRecord fileTransfer) {
        ConditionalProcessingResult result = new ConditionalProcessingResult();
        result.setFileTransferId(fileTransfer.getId());
        
        try {
            List<ProcessingRule> appliedRules = new ArrayList<>();
            
            // Compression rules
            if (shouldEnableCompression(fileTransfer)) {
                ProcessingRule compressionRule = new ProcessingRule();
                compressionRule.setRuleType("COMPRESSION");
                compressionRule.setAction("ENABLE_COMPRESSION");
                compressionRule.setReason("File characteristics indicate compression would be beneficial");
                compressionRule.getParameters().put("compressionType", "GZIP");
                appliedRules.add(compressionRule);
                
                // Apply the rule
                fileTransfer.setCompressionEnabled(true);
                if (fileTransfer.getCompressionType() == null) {
                    fileTransfer.setCompressionType(CompressionType.GZIP);
                }
            }
            
            // HSM validation rules
            if (shouldEnableHsmValidation(fileTransfer)) {
                ProcessingRule hsmRule = new ProcessingRule();
                hsmRule.setRuleType("HSM_VALIDATION");
                hsmRule.setAction("ENABLE_HSM_VALIDATION");
                hsmRule.setReason("Sensitive data detected - HSM validation required");
                appliedRules.add(hsmRule);
            }
            
            // Processing mode rules
            String recommendedMode = determineProcessingMode(fileTransfer);
            if (!"STANDARD".equals(recommendedMode)) {
                ProcessingRule modeRule = new ProcessingRule();
                modeRule.setRuleType("PROCESSING_MODE");
                modeRule.setAction("SET_PROCESSING_MODE");
                modeRule.setReason("File characteristics suggest alternative processing mode");
                modeRule.getParameters().put("mode", recommendedMode);
                appliedRules.add(modeRule);
            }
            
            // Auto-tagging rules
            List<String> suggestedTags = generateAutoTags(fileTransfer);
            if (!suggestedTags.isEmpty()) {
                ProcessingRule taggingRule = new ProcessingRule();
                taggingRule.setRuleType("AUTO_TAGGING");
                taggingRule.setAction("ADD_TAGS");
                taggingRule.setReason("Auto-tags suggested based on file characteristics");
                taggingRule.getParameters().put("tags", suggestedTags);
                appliedRules.add(taggingRule);
            }
            
            result.setAppliedRules(appliedRules);
            result.setSuccess(true);
            result.setMessage("Applied " + appliedRules.size() + " conditional processing rules");
            
            logger.info("Applied {} conditional processing rules for file: {}", 
                appliedRules.size(), fileTransfer.getFileName());
            
        } catch (Exception e) {
            logger.error("Error applying conditional processing for file {}: {}", 
                fileTransfer.getFileName(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage("Conditional processing error: " + e.getMessage());
        }
        
        return result;
    }
    
    // Helper methods
    
    private String determineQueueName(int priority) {
        if (priority >= 90) return "CRITICAL";
        if (priority >= 70) return "HIGH";
        if (priority >= 30) return "NORMAL";
        if (priority >= 10) return "LOW";
        return "BATCH";
    }
    
    private boolean shouldEnableCompression(FileTransferRecord fileTransfer) {
        // Enable compression for large text files
        if (fileTransfer.getFileSize() != null && fileTransfer.getFileSize() > 1024 * 1024) {
            if (fileTransfer.getFileExtension() != null) {
                String ext = fileTransfer.getFileExtension().toLowerCase();
                return ext.equals(".txt") || ext.equals(".csv") || ext.equals(".json") || 
                       ext.equals(".xml") || ext.equals(".log") || ext.equals(".dat");
            }
        }
        return false;
    }
    
    private boolean shouldEnableHsmValidation(FileTransferRecord fileTransfer) {
        // Enable HSM for sensitive services or large files
        if (fileTransfer.getServiceName() != null) {
            String service = fileTransfer.getServiceName().toUpperCase();
            if (service.contains("CUSTOMER") || service.contains("SENSITIVE") || 
                service.contains("CONFIDENTIAL") || service.contains("PERSONAL")) {
                return true;
            }
        }
        
        // Large files also get HSM validation
        return fileTransfer.getFileSize() != null && fileTransfer.getFileSize() > 100 * 1024 * 1024;
    }
    
    private String determineProcessingMode(FileTransferRecord fileTransfer) {
        if (fileTransfer.getFileSize() != null) {
            if (fileTransfer.getFileSize() > 500 * 1024 * 1024) { // > 500MB
                return "STREAMING";
            } else if (fileTransfer.getFileSize() > 50 * 1024 * 1024) { // > 50MB
                return "CHUNKED";
            }
        }
        
        // Check for batch processing based on time
        int hour = LocalDateTime.now().getHour();
        if (hour >= 22 || hour <= 5) { // Late night/early morning
            return "BATCH";
        }
        
        return "STANDARD";
    }
    
    private List<String> generateAutoTags(FileTransferRecord fileTransfer) {
        List<String> tags = new ArrayList<>();
        
        // Size-based tags
        if (fileTransfer.getFileSize() != null) {
            if (fileTransfer.getFileSize() > 100 * 1024 * 1024) {
                tags.add("large-file");
            }
        }
        
        // Service-based tags
        if (fileTransfer.getServiceName() != null) {
            if (fileTransfer.getServiceName().contains("CUSTOMER")) {
                tags.add("customer-data");
            }
            if (fileTransfer.getServiceName().contains("TRANSACTION")) {
                tags.add("transaction-data");
            }
        }
        
        // Extension-based tags
        if (fileTransfer.getFileExtension() != null) {
            switch (fileTransfer.getFileExtension().toLowerCase()) {
                case ".log":
                    tags.add("log-data");
                    break;
                case ".csv":
                case ".xlsx":
                    tags.add("structured-data");
                    break;
                case ".zip":
                case ".gz":
                    tags.add("archive-data");
                    break;
            }
        }
        
        // Status-based tags
        if (fileTransfer.getStatus() == TransferStatus.COMPLETED) {
            tags.add("processed");
        } else if (fileTransfer.getStatus() == TransferStatus.FAILED) {
            tags.add("needs-attention");
        }
        
        return tags;
    }
    
    private boolean isSystemOverloaded() {
        // In a real implementation, this would check actual system metrics
        // For simulation, randomly return true 10% of the time
        return Math.random() < 0.1;
    }
    
    // Result classes
    
    public static class RoutingDecision {
        private String originalService;
        private String recommendedService;
        private String routingReason;
        private int priority = 50;
        private boolean requiresApproval = false;
        private List<String> recommendations = new ArrayList<>();
        
        public void addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
        }
        
        // Getters and Setters
        public String getOriginalService() { return originalService; }
        public void setOriginalService(String originalService) { this.originalService = originalService; }
        
        public String getRecommendedService() { return recommendedService; }
        public void setRecommendedService(String recommendedService) { this.recommendedService = recommendedService; }
        
        public String getRoutingReason() { return routingReason; }
        public void setRoutingReason(String routingReason) { this.routingReason = routingReason; }
        
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        
        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    public static class ProcessingPriority {
        private int basePriority;
        private int calculatedPriority;
        private String queueName;
        private List<String> priorityReasons = new ArrayList<>();
        
        public void addReason(String reason) {
            this.priorityReasons.add(reason);
        }
        
        // Getters and Setters
        public int getBasePriority() { return basePriority; }
        public void setBasePriority(int basePriority) { this.basePriority = basePriority; }
        
        public int getCalculatedPriority() { return calculatedPriority; }
        public void setCalculatedPriority(int calculatedPriority) { this.calculatedPriority = calculatedPriority; }
        
        public String getQueueName() { return queueName; }
        public void setQueueName(String queueName) { this.queueName = queueName; }
        
        public List<String> getPriorityReasons() { return priorityReasons; }
        public void setPriorityReasons(List<String> priorityReasons) { this.priorityReasons = priorityReasons; }
    }
    
    public static class ProcessingDecision {
        private boolean shouldProcessImmediately;
        private String reason;
        private boolean requiresApproval = false;
        private LocalDateTime recommendedProcessingTime;
        
        // Getters and Setters
        public boolean isShouldProcessImmediately() { return shouldProcessImmediately; }
        public void setShouldProcessImmediately(boolean shouldProcessImmediately) { this.shouldProcessImmediately = shouldProcessImmediately; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        
        public LocalDateTime getRecommendedProcessingTime() { return recommendedProcessingTime; }
        public void setRecommendedProcessingTime(LocalDateTime recommendedProcessingTime) { this.recommendedProcessingTime = recommendedProcessingTime; }
    }
    
    public static class ConditionalProcessingResult {
        private Long fileTransferId;
        private boolean success;
        private String message;
        private String errorMessage;
        private List<ProcessingRule> appliedRules = new ArrayList<>();
        
        // Getters and Setters
        public Long getFileTransferId() { return fileTransferId; }
        public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<ProcessingRule> getAppliedRules() { return appliedRules; }
        public void setAppliedRules(List<ProcessingRule> appliedRules) { this.appliedRules = appliedRules; }
    }
    
    public static class ProcessingRule {
        private String ruleType;
        private String action;
        private String reason;
        private Map<String, Object> parameters = new HashMap<>();
        
        // Getters and Setters
        public String getRuleType() { return ruleType; }
        public void setRuleType(String ruleType) { this.ruleType = ruleType; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}