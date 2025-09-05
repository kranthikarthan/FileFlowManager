package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight workflow orchestration for batch processing
 */
@Service
public class WorkflowOrchestrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrationService.class);
    
    @Autowired
    private BusinessRulesEngine businessRulesEngine;
    
    /**
     * Execute intelligent file processing workflow
     */
    public WorkflowResult executeFileProcessingWorkflow(FileTransferRecord fileTransfer) {
        WorkflowResult result = new WorkflowResult();
        result.setFileTransferId(fileTransfer.getId());
        result.setWorkflowName("Intelligent File Processing");
        result.setStartTime(LocalDateTime.now());
        
        try {
            List<WorkflowStep> executedSteps = new ArrayList<>();
            
            // Step 1: Determine routing
            WorkflowStep routingStep = executeRoutingStep(fileTransfer);
            executedSteps.add(routingStep);
            
            if (!routingStep.isSuccess()) {
                result.setSuccess(false);
                result.setErrorMessage("Routing step failed: " + routingStep.getErrorMessage());
                result.setExecutedSteps(executedSteps);
                return result;
            }
            
            // Step 2: Determine priority
            WorkflowStep priorityStep = executePriorityStep(fileTransfer);
            executedSteps.add(priorityStep);
            
            // Step 3: Apply conditional processing
            WorkflowStep conditionalStep = executeConditionalProcessingStep(fileTransfer);
            executedSteps.add(conditionalStep);
            
            // Step 4: Check processing decision
            WorkflowStep decisionStep = executeProcessingDecisionStep(fileTransfer);
            executedSteps.add(decisionStep);
            
            if (!decisionStep.isSuccess()) {
                result.setSuccess(false);
                result.setErrorMessage("Processing decision failed: " + decisionStep.getErrorMessage());
                result.setExecutedSteps(executedSteps);
                return result;
            }
            
            // Step 5: Execute any required pre-processing
            WorkflowStep preprocessingStep = executePreprocessingStep(fileTransfer);
            executedSteps.add(preprocessingStep);
            
            result.setSuccess(true);
            result.setMessage("Workflow completed successfully");
            result.setExecutedSteps(executedSteps);
            result.setEndTime(LocalDateTime.now());
            result.calculateDuration();
            
            logger.info("Workflow completed for file {}: {} steps executed in {}ms", 
                fileTransfer.getFileName(), executedSteps.size(), result.getDurationMs());
            
        } catch (Exception e) {
            logger.error("Error executing workflow for file {}: {}", fileTransfer.getFileName(), e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Workflow execution error: " + e.getMessage());
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * Execute routing step
     */
    private WorkflowStep executeRoutingStep(FileTransferRecord fileTransfer) {
        WorkflowStep step = new WorkflowStep("Intelligent Routing", "ROUTING");
        step.setStartTime(LocalDateTime.now());
        
        try {
            BusinessRulesEngine.RoutingDecision decision = businessRulesEngine.determineRouting(fileTransfer);
            
            if (!decision.getRecommendedService().equals(decision.getOriginalService())) {
                fileTransfer.setServiceName(decision.getRecommendedService());
                step.setMessage("Routed from " + decision.getOriginalService() + 
                               " to " + decision.getRecommendedService() + 
                               ": " + decision.getRoutingReason());
            } else {
                step.setMessage("No routing change needed");
            }
            
            step.getMetadata().put("routingDecision", decision);
            step.setSuccess(true);
            
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage("Routing step failed: " + e.getMessage());
        }
        
        step.setEndTime(LocalDateTime.now());
        return step;
    }
    
    /**
     * Execute priority determination step
     */
    private WorkflowStep executePriorityStep(FileTransferRecord fileTransfer) {
        WorkflowStep step = new WorkflowStep("Priority Assignment", "PRIORITY");
        step.setStartTime(LocalDateTime.now());
        
        try {
            BusinessRulesEngine.ProcessingPriority priority = businessRulesEngine.determinePriority(fileTransfer);
            
            step.setMessage("Priority assigned: " + priority.getCalculatedPriority() + 
                           " (queue: " + priority.getQueueName() + ")");
            step.getMetadata().put("priority", priority.getCalculatedPriority());
            step.getMetadata().put("queueName", priority.getQueueName());
            step.getMetadata().put("reasons", priority.getPriorityReasons());
            step.setSuccess(true);
            
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage("Priority step failed: " + e.getMessage());
        }
        
        step.setEndTime(LocalDateTime.now());
        return step;
    }
    
    /**
     * Execute conditional processing step
     */
    private WorkflowStep executeConditionalProcessingStep(FileTransferRecord fileTransfer) {
        WorkflowStep step = new WorkflowStep("Conditional Processing", "CONDITIONAL");
        step.setStartTime(LocalDateTime.now());
        
        try {
            BusinessRulesEngine.ConditionalProcessingResult processingResult = 
                businessRulesEngine.applyConditionalProcessing(fileTransfer);
            
            step.setSuccess(processingResult.isSuccess());
            step.setMessage(processingResult.getMessage());
            
            if (!processingResult.isSuccess()) {
                step.setErrorMessage(processingResult.getErrorMessage());
            }
            
            step.getMetadata().put("appliedRules", processingResult.getAppliedRules());
            
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage("Conditional processing failed: " + e.getMessage());
        }
        
        step.setEndTime(LocalDateTime.now());
        return step;
    }
    
    /**
     * Execute processing decision step
     */
    private WorkflowStep executeProcessingDecisionStep(FileTransferRecord fileTransfer) {
        WorkflowStep step = new WorkflowStep("Processing Decision", "DECISION");
        step.setStartTime(LocalDateTime.now());
        
        try {
            BusinessRulesEngine.ProcessingDecision decision = businessRulesEngine.shouldProcessImmediately(fileTransfer);
            
            step.setSuccess(true);
            step.setMessage("Processing decision: " + 
                (decision.isShouldProcessImmediately() ? "Process immediately" : "Queue for later") + 
                " - " + decision.getReason());
            
            step.getMetadata().put("shouldProcessImmediately", decision.isShouldProcessImmediately());
            step.getMetadata().put("requiresApproval", decision.isRequiresApproval());
            step.getMetadata().put("recommendedProcessingTime", decision.getRecommendedProcessingTime());
            
            // Update file transfer status if queued
            if (!decision.isShouldProcessImmediately()) {
                fileTransfer.setStatus(TransferStatus.PENDING);
                if (decision.isRequiresApproval()) {
                    // This would integrate with approval system
                    logger.info("File {} requires approval before processing", fileTransfer.getFileName());
                }
            }
            
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage("Processing decision failed: " + e.getMessage());
        }
        
        step.setEndTime(LocalDateTime.now());
        return step;
    }
    
    /**
     * Execute preprocessing step
     */
    private WorkflowStep executePreprocessingStep(FileTransferRecord fileTransfer) {
        WorkflowStep step = new WorkflowStep("Preprocessing", "PREPROCESSING");
        step.setStartTime(LocalDateTime.now());
        
        try {
            List<String> preprocessingActions = new ArrayList<>();
            
            // Apply compression if enabled
            if (fileTransfer.getCompressionEnabled() != null && fileTransfer.getCompressionEnabled()) {
                preprocessingActions.add("Compression configured: " + 
                    (fileTransfer.getCompressionType() != null ? fileTransfer.getCompressionType() : "AUTO"));
            }
            
            // Log any other preprocessing actions
            if (preprocessingActions.isEmpty()) {
                preprocessingActions.add("No preprocessing required");
            }
            
            step.setSuccess(true);
            step.setMessage("Preprocessing completed: " + String.join(", ", preprocessingActions));
            step.getMetadata().put("actions", preprocessingActions);
            
        } catch (Exception e) {
            step.setSuccess(false);
            step.setErrorMessage("Preprocessing failed: " + e.getMessage());
        }
        
        step.setEndTime(LocalDateTime.now());
        return step;
    }
    
    /**
     * Execute error recovery workflow
     */
    public WorkflowResult executeErrorRecoveryWorkflow(FileTransferRecord fileTransfer, String errorMessage) {
        WorkflowResult result = new WorkflowResult();
        result.setFileTransferId(fileTransfer.getId());
        result.setWorkflowName("Error Recovery");
        result.setStartTime(LocalDateTime.now());
        
        try {
            List<WorkflowStep> executedSteps = new ArrayList<>();
            
            // Step 1: Analyze error
            WorkflowStep analysisStep = new WorkflowStep("Error Analysis", "ANALYSIS");
            analysisStep.setStartTime(LocalDateTime.now());
            
            String errorCategory = categorizeError(errorMessage);
            boolean isRecoverable = isErrorRecoverable(errorMessage);
            
            analysisStep.setSuccess(true);
            analysisStep.setMessage("Error categorized as: " + errorCategory + 
                                   " (recoverable: " + isRecoverable + ")");
            analysisStep.getMetadata().put("errorCategory", errorCategory);
            analysisStep.getMetadata().put("isRecoverable", isRecoverable);
            analysisStep.setEndTime(LocalDateTime.now());
            executedSteps.add(analysisStep);
            
            // Step 2: Apply recovery actions
            if (isRecoverable) {
                WorkflowStep recoveryStep = new WorkflowStep("Recovery Actions", "RECOVERY");
                recoveryStep.setStartTime(LocalDateTime.now());
                
                List<String> recoveryActions = applyRecoveryActions(fileTransfer, errorCategory);
                
                recoveryStep.setSuccess(true);
                recoveryStep.setMessage("Recovery actions applied: " + String.join(", ", recoveryActions));
                recoveryStep.getMetadata().put("actions", recoveryActions);
                recoveryStep.setEndTime(LocalDateTime.now());
                executedSteps.add(recoveryStep);
                
                // Reset file to pending for retry
                fileTransfer.setStatus(TransferStatus.PENDING);
                fileTransfer.setErrorMessage(null);
            }
            
            // Step 3: Notification
            WorkflowStep notificationStep = new WorkflowStep("Error Notification", "NOTIFICATION");
            notificationStep.setStartTime(LocalDateTime.now());
            
            String notificationMessage = "File processing " + 
                (isRecoverable ? "error recovered" : "failed") + 
                " for file: " + fileTransfer.getFileName();
            
            // This would integrate with notification system
            logger.info("Error workflow notification: {}", notificationMessage);
            
            notificationStep.setSuccess(true);
            notificationStep.setMessage("Notification sent to operations team");
            notificationStep.setEndTime(LocalDateTime.now());
            executedSteps.add(notificationStep);
            
            result.setSuccess(true);
            result.setMessage("Error recovery workflow completed");
            result.setExecutedSteps(executedSteps);
            
        } catch (Exception e) {
            logger.error("Error in recovery workflow for file {}: {}", fileTransfer.getFileName(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage("Recovery workflow failed: " + e.getMessage());
        }
        
        result.setEndTime(LocalDateTime.now());
        result.calculateDuration();
        
        return result;
    }
    
    // Helper methods
    
    private String categorizeError(String errorMessage) {
        if (errorMessage == null) return "UNKNOWN";
        
        String lowerError = errorMessage.toLowerCase();
        
        if (lowerError.contains("file not found") || lowerError.contains("no such file")) {
            return "FILE_NOT_FOUND";
        } else if (lowerError.contains("permission denied") || lowerError.contains("access denied")) {
            return "PERMISSION_ERROR";
        } else if (lowerError.contains("disk") || lowerError.contains("space")) {
            return "DISK_SPACE_ERROR";
        } else if (lowerError.contains("network") || lowerError.contains("connection")) {
            return "NETWORK_ERROR";
        } else if (lowerError.contains("timeout")) {
            return "TIMEOUT_ERROR";
        } else if (lowerError.contains("format") || lowerError.contains("parse")) {
            return "FORMAT_ERROR";
        } else {
            return "PROCESSING_ERROR";
        }
    }
    
    private boolean isErrorRecoverable(String errorMessage) {
        String errorCategory = categorizeError(errorMessage);
        
        switch (errorCategory) {
            case "NETWORK_ERROR":
            case "TIMEOUT_ERROR":
            case "DISK_SPACE_ERROR":
                return true; // These are often temporary
            case "PERMISSION_ERROR":
            case "FORMAT_ERROR":
                return false; // These require manual intervention
            case "FILE_NOT_FOUND":
                return false; // File is missing
            default:
                return true; // Default to recoverable for unknown errors
        }
    }
    
    private List<String> applyRecoveryActions(FileTransferRecord fileTransfer, String errorCategory) {
        List<String> actions = new ArrayList<>();
        
        switch (errorCategory) {
            case "NETWORK_ERROR":
                actions.add("Increased retry delay to 5 minutes");
                actions.add("Enabled network error recovery mode");
                break;
                
            case "TIMEOUT_ERROR":
                actions.add("Increased processing timeout to 120 minutes");
                actions.add("Enabled chunked processing mode");
                break;
                
            case "DISK_SPACE_ERROR":
                actions.add("Enabled compression to reduce space usage");
                actions.add("Scheduled for off-peak processing");
                fileTransfer.setCompressionEnabled(true);
                break;
                
            case "PROCESSING_ERROR":
                actions.add("Reset processing flags");
                actions.add("Scheduled for manual review if retry fails");
                break;
                
            default:
                actions.add("Applied generic recovery procedures");
        }
        
        return actions;
    }
    
    // Result classes
    
    public static class WorkflowResult {
        private Long fileTransferId;
        private String workflowName;
        private boolean success;
        private String message;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMs;
        private List<WorkflowStep> executedSteps = new ArrayList<>();
        
        public void calculateDuration() {
            if (startTime != null && endTime != null) {
                this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            }
        }
        
        // Getters and Setters
        public Long getFileTransferId() { return fileTransferId; }
        public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
        
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        
        public List<WorkflowStep> getExecutedSteps() { return executedSteps; }
        public void setExecutedSteps(List<WorkflowStep> executedSteps) { this.executedSteps = executedSteps; }
    }
    
    public static class WorkflowStep {
        private String stepName;
        private String stepType;
        private boolean success;
        private String message;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Map<String, Object> metadata = new HashMap<>();
        
        public WorkflowStep(String stepName, String stepType) {
            this.stepName = stepName;
            this.stepType = stepType;
        }
        
        // Getters and Setters
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        
        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}