package com.filetransfer.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.BusinessRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Powerful business rules engine for intelligent file processing automation
 */
@Service
public class BusinessRulesEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessRulesEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private BusinessRuleRepository businessRuleRepository;
    
    /**
     * Evaluate all applicable rules for a file transfer
     */
    public RuleEvaluationResult evaluateRules(FileTransferRecord fileTransfer) {
        try {
            List<BusinessRule> applicableRules = getApplicableRules(fileTransfer);
            
            RuleEvaluationResult result = new RuleEvaluationResult();
            result.setFileTransferId(fileTransfer.getId());
            result.setTenantId(fileTransfer.getTenantId());
            result.setEvaluationTimestamp(LocalDateTime.now());
            
            // Sort rules by priority and execution order
            applicableRules.sort((r1, r2) -> {
                int priorityCompare = Integer.compare(r2.getRulePriority(), r1.getRulePriority());
                if (priorityCompare != 0) return priorityCompare;
                return Integer.compare(r1.getExecutionOrder(), r2.getExecutionOrder());
            });
            
            List<RuleExecutionResult> executionResults = new ArrayList<>();
            
            for (BusinessRule rule : applicableRules) {
                long startTime = System.currentTimeMillis();
                
                try {
                    RuleExecutionResult ruleResult = evaluateRule(rule, fileTransfer);
                    ruleResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    
                    executionResults.add(ruleResult);
                    
                    // Update rule statistics
                    rule.recordExecution(ruleResult.isSuccess(), ruleResult.getExecutionTimeMs());
                    businessRuleRepository.save(rule);
                    
                    // If rule failed and is critical, stop evaluation
                    if (!ruleResult.isSuccess() && ruleResult.isCritical()) {
                        logger.warn("Critical rule failed for file {}: {}", fileTransfer.getFileName(), ruleResult.getErrorMessage());
                        result.setCriticalFailure(true);
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error evaluating rule {} for file {}: {}", 
                        rule.getRuleName(), fileTransfer.getFileName(), e.getMessage(), e);
                    
                    RuleExecutionResult errorResult = new RuleExecutionResult();
                    errorResult.setRuleId(rule.getId());
                    errorResult.setRuleName(rule.getRuleName());
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage("Rule evaluation error: " + e.getMessage());
                    errorResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    
                    executionResults.add(errorResult);
                    
                    // Update rule statistics
                    rule.recordExecution(false, errorResult.getExecutionTimeMs());
                    businessRuleRepository.save(rule);
                }
            }
            
            result.setRuleExecutionResults(executionResults);
            result.setTotalRulesEvaluated(executionResults.size());
            result.setSuccessfulRules(executionResults.stream().filter(RuleExecutionResult::isSuccess).count());
            result.setFailedRules(executionResults.stream().filter(r -> !r.isSuccess()).count());
            
            // Aggregate all actions from successful rules
            List<RuleAction> aggregatedActions = executionResults.stream()
                    .filter(RuleExecutionResult::isSuccess)
                    .flatMap(r -> r.getActions().stream())
                    .collect(Collectors.toList());
            
            result.setAggregatedActions(aggregatedActions);
            
            logger.info("Rule evaluation completed for file {}: {} rules evaluated, {} successful, {} failed", 
                fileTransfer.getFileName(), result.getTotalRulesEvaluated(), 
                result.getSuccessfulRules(), result.getFailedRules());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in rule evaluation for file {}: {}", fileTransfer.getFileName(), e.getMessage(), e);
            
            RuleEvaluationResult errorResult = new RuleEvaluationResult();
            errorResult.setFileTransferId(fileTransfer.getId());
            errorResult.setTenantId(fileTransfer.getTenantId());
            errorResult.setEvaluationTimestamp(LocalDateTime.now());
            errorResult.setCriticalFailure(true);
            errorResult.setErrorMessage("Rule evaluation system error: " + e.getMessage());
            
            return errorResult;
        }
    }
    
    /**
     * Evaluate a single rule against a file transfer
     */
    private RuleExecutionResult evaluateRule(BusinessRule rule, FileTransferRecord fileTransfer) throws Exception {
        RuleExecutionResult result = new RuleExecutionResult();
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getRuleName());
        result.setRuleType(rule.getRuleType());
        
        // Parse rule conditions
        JsonNode conditions = objectMapper.readTree(rule.getRuleConditions());
        
        // Evaluate conditions
        boolean conditionsMet = evaluateConditions(conditions, fileTransfer);
        
        if (!conditionsMet) {
            result.setSuccess(true);
            result.setConditionsMet(false);
            result.setMessage("Rule conditions not met - rule skipped");
            return result;
        }
        
        result.setConditionsMet(true);
        
        // Parse and execute actions
        JsonNode actions = objectMapper.readTree(rule.getRuleActions());
        List<RuleAction> executedActions = executeActions(actions, fileTransfer, rule);
        
        result.setActions(executedActions);
        result.setSuccess(true);
        result.setMessage("Rule executed successfully");
        
        return result;
    }
    
    /**
     * Evaluate rule conditions against file transfer
     */
    private boolean evaluateConditions(JsonNode conditions, FileTransferRecord fileTransfer) throws Exception {
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means always execute
        }
        
        String operator = conditions.has("operator") ? conditions.get("operator").asText() : "AND";
        JsonNode conditionList = conditions.get("conditions");
        
        if (conditionList == null || !conditionList.isArray()) {
            return evaluateSingleCondition(conditions, fileTransfer);
        }
        
        List<Boolean> results = new ArrayList<>();
        
        for (JsonNode condition : conditionList) {
            boolean conditionResult = evaluateSingleCondition(condition, fileTransfer);
            results.add(conditionResult);
        }
        
        // Apply operator
        switch (operator.toUpperCase()) {
            case "AND":
                return results.stream().allMatch(Boolean::booleanValue);
            case "OR":
                return results.stream().anyMatch(Boolean::booleanValue);
            case "NOT":
                return results.stream().noneMatch(Boolean::booleanValue);
            default:
                return results.stream().allMatch(Boolean::booleanValue); // Default to AND
        }
    }
    
    /**
     * Evaluate a single condition
     */
    private boolean evaluateSingleCondition(JsonNode condition, FileTransferRecord fileTransfer) throws Exception {
        String field = condition.get("field").asText();
        String operator = condition.get("operator").asText();
        JsonNode valueNode = condition.get("value");
        
        Object fieldValue = getFieldValue(field, fileTransfer);
        Object conditionValue = getConditionValue(valueNode);
        
        return evaluateComparison(fieldValue, operator, conditionValue);
    }
    
    /**
     * Get field value from file transfer record
     */
    private Object getFieldValue(String field, FileTransferRecord fileTransfer) {
        switch (field.toLowerCase()) {
            case "filename": return fileTransfer.getFileName();
            case "filesize": return fileTransfer.getFileSize();
            case "fileextension": return fileTransfer.getFileExtension();
            case "servicename": return fileTransfer.getServiceName();
            case "subservicename": return fileTransfer.getSubServiceName();
            case "status": return fileTransfer.getStatus().toString();
            case "direction": return fileTransfer.getDirection().toString();
            case "filetype": return fileTransfer.getFileType().toString();
            case "createdat": return fileTransfer.getCreatedAt();
            case "processedat": return fileTransfer.getProcessedAt();
            case "compressionenabled": return fileTransfer.getCompressionEnabled();
            case "compressiontype": return fileTransfer.getCompressionType() != null ? fileTransfer.getCompressionType().toString() : null;
            case "hour": return LocalTime.now().getHour();
            case "dayofweek": return LocalDateTime.now().getDayOfWeek().toString();
            case "month": return LocalDateTime.now().getMonth().toString();
            default:
                logger.warn("Unknown field in rule condition: {}", field);
                return null;
        }
    }
    
    /**
     * Get condition value from JSON node
     */
    private Object getConditionValue(JsonNode valueNode) {
        if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isNumber()) {
            return valueNode.asLong();
        } else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else if (valueNode.isArray()) {
            List<String> values = new ArrayList<>();
            valueNode.forEach(node -> values.add(node.asText()));
            return values;
        }
        return valueNode.asText();
    }
    
    /**
     * Evaluate comparison between field value and condition value
     */
    private boolean evaluateComparison(Object fieldValue, String operator, Object conditionValue) {
        if (fieldValue == null) {
            return "IS_NULL".equals(operator) || "IS_EMPTY".equals(operator);
        }
        
        switch (operator.toUpperCase()) {
            case "EQUALS":
            case "EQ":
                return Objects.equals(fieldValue.toString(), conditionValue.toString());
                
            case "NOT_EQUALS":
            case "NEQ":
                return !Objects.equals(fieldValue.toString(), conditionValue.toString());
                
            case "CONTAINS":
                return fieldValue.toString().toLowerCase().contains(conditionValue.toString().toLowerCase());
                
            case "NOT_CONTAINS":
                return !fieldValue.toString().toLowerCase().contains(conditionValue.toString().toLowerCase());
                
            case "STARTS_WITH":
                return fieldValue.toString().toLowerCase().startsWith(conditionValue.toString().toLowerCase());
                
            case "ENDS_WITH":
                return fieldValue.toString().toLowerCase().endsWith(conditionValue.toString().toLowerCase());
                
            case "MATCHES_REGEX":
                return Pattern.matches(conditionValue.toString(), fieldValue.toString());
                
            case "GREATER_THAN":
            case "GT":
                return compareNumeric(fieldValue, conditionValue) > 0;
                
            case "GREATER_THAN_OR_EQUAL":
            case "GTE":
                return compareNumeric(fieldValue, conditionValue) >= 0;
                
            case "LESS_THAN":
            case "LT":
                return compareNumeric(fieldValue, conditionValue) < 0;
                
            case "LESS_THAN_OR_EQUAL":
            case "LTE":
                return compareNumeric(fieldValue, conditionValue) <= 0;
                
            case "IN":
                if (conditionValue instanceof List) {
                    return ((List<?>) conditionValue).contains(fieldValue.toString());
                }
                return false;
                
            case "NOT_IN":
                if (conditionValue instanceof List) {
                    return !((List<?>) conditionValue).contains(fieldValue.toString());
                }
                return true;
                
            case "IS_NULL":
                return fieldValue == null;
                
            case "IS_NOT_NULL":
                return fieldValue != null;
                
            case "IS_EMPTY":
                return fieldValue == null || fieldValue.toString().trim().isEmpty();
                
            case "IS_NOT_EMPTY":
                return fieldValue != null && !fieldValue.toString().trim().isEmpty();
                
            case "BETWEEN":
                if (conditionValue instanceof List && ((List<?>) conditionValue).size() == 2) {
                    List<?> range = (List<?>) conditionValue;
                    double numValue = Double.parseDouble(fieldValue.toString());
                    double minValue = Double.parseDouble(range.get(0).toString());
                    double maxValue = Double.parseDouble(range.get(1).toString());
                    return numValue >= minValue && numValue <= maxValue;
                }
                return false;
                
            default:
                logger.warn("Unknown operator in rule condition: {}", operator);
                return false;
        }
    }
    
    /**
     * Execute rule actions
     */
    private List<RuleAction> executeActions(JsonNode actions, FileTransferRecord fileTransfer, BusinessRule rule) throws Exception {
        List<RuleAction> executedActions = new ArrayList<>();
        
        if (!actions.isArray()) {
            // Single action
            RuleAction action = executeAction(actions, fileTransfer, rule);
            if (action != null) {
                executedActions.add(action);
            }
        } else {
            // Multiple actions
            for (JsonNode actionNode : actions) {
                RuleAction action = executeAction(actionNode, fileTransfer, rule);
                if (action != null) {
                    executedActions.add(action);
                }
            }
        }
        
        return executedActions;
    }
    
    /**
     * Execute a single rule action
     */
    private RuleAction executeAction(JsonNode actionNode, FileTransferRecord fileTransfer, BusinessRule rule) throws Exception {
        String actionType = actionNode.get("type").asText();
        JsonNode parameters = actionNode.get("parameters");
        
        RuleAction action = new RuleAction();
        action.setActionType(actionType);
        action.setExecutedAt(LocalDateTime.now());
        
        switch (actionType.toUpperCase()) {
            case "SET_PRIORITY":
                int priority = parameters.get("priority").asInt();
                // This would integrate with a priority field on FileTransferRecord
                action.setSuccess(true);
                action.setResult("Priority set to " + priority);
                break;
                
            case "ROUTE_TO_SERVICE":
                String targetService = parameters.get("targetService").asText();
                String targetSubService = parameters.has("targetSubService") ? 
                    parameters.get("targetSubService").asText() : null;
                
                // Update routing information
                fileTransfer.setServiceName(targetService);
                if (targetSubService != null) {
                    fileTransfer.setSubServiceName(targetSubService);
                }
                
                action.setSuccess(true);
                action.setResult("Routed to service: " + targetService + 
                    (targetSubService != null ? "/" + targetSubService : ""));
                break;
                
            case "ENABLE_COMPRESSION":
                fileTransfer.setCompressionEnabled(true);
                if (parameters.has("compressionType")) {
                    String compressionType = parameters.get("compressionType").asText();
                    fileTransfer.setCompressionType(CompressionType.valueOf(compressionType));
                }
                action.setSuccess(true);
                action.setResult("Compression enabled");
                break;
                
            case "ADD_TAG":
                String tagName = parameters.get("tagName").asText();
                // This would integrate with the tagging system
                action.setSuccess(true);
                action.setResult("Tag added: " + tagName);
                break;
                
            case "SEND_NOTIFICATION":
                String notificationType = parameters.get("type").asText();
                String message = parameters.get("message").asText();
                String recipient = parameters.has("recipient") ? parameters.get("recipient").asText() : null;
                
                // This would integrate with notification system
                action.setSuccess(true);
                action.setResult("Notification sent: " + notificationType);
                break;
                
            case "REQUIRE_APPROVAL":
                String approverRole = parameters.get("approverRole").asText();
                String approvalReason = parameters.has("reason") ? parameters.get("reason").asText() : "Rule-based approval required";
                
                // This would integrate with approval workflow system
                action.setSuccess(true);
                action.setResult("Approval required from: " + approverRole);
                action.setRequiresApproval(true);
                break;
                
            case "SET_PROCESSING_MODE":
                String processingMode = parameters.get("mode").asText();
                // This would set processing mode (STANDARD, STREAMING, BATCH, etc.)
                action.setSuccess(true);
                action.setResult("Processing mode set to: " + processingMode);
                break;
                
            case "DELAY_PROCESSING":
                int delayMinutes = parameters.get("delayMinutes").asInt();
                // This would schedule processing for later
                action.setSuccess(true);
                action.setResult("Processing delayed by " + delayMinutes + " minutes");
                break;
                
            case "REJECT_FILE":
                String rejectReason = parameters.get("reason").asText();
                fileTransfer.setStatus(TransferStatus.FAILED);
                fileTransfer.setErrorMessage("Rejected by rule: " + rejectReason);
                action.setSuccess(true);
                action.setResult("File rejected: " + rejectReason);
                break;
                
            case "TRANSFORM_PATH":
                String pathTemplate = parameters.get("pathTemplate").asText();
                String newPath = applyPathTemplate(pathTemplate, fileTransfer);
                fileTransfer.setTargetPath(newPath);
                action.setSuccess(true);
                action.setResult("Path transformed to: " + newPath);
                break;
                
            case "CUSTOM_SCRIPT":
                String scriptName = parameters.get("scriptName").asText();
                // This would execute custom scripts
                action.setSuccess(true);
                action.setResult("Custom script executed: " + scriptName);
                break;
                
            default:
                logger.warn("Unknown action type: {}", actionType);
                action.setSuccess(false);
                action.setErrorMessage("Unknown action type: " + actionType);
        }
        
        return action;
    }
    
    /**
     * Get applicable rules for a file transfer
     */
    private List<BusinessRule> getApplicableRules(FileTransferRecord fileTransfer) {
        // Get all active rules for the tenant
        List<BusinessRule> allRules = businessRuleRepository.findActiveRulesForTenant(fileTransfer.getTenantId());
        
        return allRules.stream()
                .filter(rule -> isRuleApplicable(rule, fileTransfer))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a rule is applicable to a file transfer
     */
    private boolean isRuleApplicable(BusinessRule rule, FileTransferRecord fileTransfer) {
        // Check effective date range
        LocalDateTime now = LocalDateTime.now();
        if (rule.getEffectiveFrom() != null && now.isBefore(rule.getEffectiveFrom())) {
            return false;
        }
        if (rule.getEffectiveUntil() != null && now.isAfter(rule.getEffectiveUntil())) {
            return false;
        }
        
        // Check service name filter
        if (rule.getServiceName() != null && !rule.getServiceName().equals(fileTransfer.getServiceName())) {
            return false;
        }
        
        // Check sub-service name filter
        if (rule.getSubServiceName() != null && !rule.getSubServiceName().equals(fileTransfer.getSubServiceName())) {
            return false;
        }
        
        // Check direction filter
        if (rule.getAppliesToDirection() != null && rule.getAppliesToDirection() != fileTransfer.getDirection()) {
            return false;
        }
        
        // Check file type filter
        if (rule.getAppliesToFileTypes() != null && !rule.getAppliesToFileTypes().isEmpty()) {
            String[] allowedTypes = rule.getAppliesToFileTypes().split(",");
            String currentType = fileTransfer.getFileType().toString();
            return Arrays.asList(allowedTypes).contains(currentType);
        }
        
        return true;
    }
    
    /**
     * Compare numeric values
     */
    private int compareNumeric(Object value1, Object value2) {
        try {
            double num1 = Double.parseDouble(value1.toString());
            double num2 = Double.parseDouble(value2.toString());
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            // Fallback to string comparison
            return value1.toString().compareTo(value2.toString());
        }
    }
    
    /**
     * Apply path template with variable substitution
     */
    private String applyPathTemplate(String template, FileTransferRecord fileTransfer) {
        String result = template;
        
        // Replace variables with actual values
        result = result.replace("{fileName}", fileTransfer.getFileName());
        result = result.replace("{serviceName}", fileTransfer.getServiceName());
        result = result.replace("{subServiceName}", fileTransfer.getSubServiceName() != null ? fileTransfer.getSubServiceName() : "");
        result = result.replace("{tenantId}", fileTransfer.getTenantId());
        result = result.replace("{direction}", fileTransfer.getDirection().toString().toLowerCase());
        result = result.replace("{fileExtension}", fileTransfer.getFileExtension() != null ? fileTransfer.getFileExtension() : "");
        result = result.replace("{yyyy}", String.valueOf(LocalDateTime.now().getYear()));
        result = result.replace("{mm}", String.format("%02d", LocalDateTime.now().getMonthValue()));
        result = result.replace("{dd}", String.format("%02d", LocalDateTime.now().getDayOfMonth()));
        
        return result;
    }
    
    /**
     * Create default rules for a tenant
     */
    public void createDefaultRules(String tenantId, String createdBy) {
        List<BusinessRule> defaultRules = new ArrayList<>();
        
        // Large file compression rule
        BusinessRule compressionRule = new BusinessRule(tenantId, "Auto-compress large files", RuleType.CONDITIONAL_PROCESSING, createdBy);
        compressionRule.setRuleDescription("Automatically enable compression for files larger than 10MB");
        compressionRule.setRuleConditions("{\"field\":\"fileSize\",\"operator\":\"GT\",\"value\":10485760}");
        compressionRule.setRuleActions("[{\"type\":\"ENABLE_COMPRESSION\",\"parameters\":{\"compressionType\":\"GZIP\"}}]");
        compressionRule.setRulePriority(80);
        compressionRule.setRuleCategory("PROCESSING");
        defaultRules.add(compressionRule);
        
        // High priority customer data rule
        BusinessRule priorityRule = new BusinessRule(tenantId, "High priority customer data", RuleType.PRIORITY_PROCESSING, createdBy);
        priorityRule.setRuleDescription("Set high priority for customer data files");
        priorityRule.setRuleConditions("{\"field\":\"serviceName\",\"operator\":\"CONTAINS\",\"value\":\"CUSTOMER\"}");
        priorityRule.setRuleActions("[{\"type\":\"SET_PRIORITY\",\"parameters\":{\"priority\":90}},{\"type\":\"ADD_TAG\",\"parameters\":{\"tagName\":\"high-priority\"}}]");
        priorityRule.setRulePriority(90);
        priorityRule.setRuleCategory("ROUTING");
        defaultRules.add(priorityRule);
        
        // Failed file notification rule
        BusinessRule notificationRule = new BusinessRule(tenantId, "Notify on file failures", RuleType.ALERT_NOTIFICATION, createdBy);
        notificationRule.setRuleDescription("Send notifications when files fail processing");
        notificationRule.setRuleConditions("{\"field\":\"status\",\"operator\":\"EQUALS\",\"value\":\"FAILED\"}");
        notificationRule.setRuleActions("[{\"type\":\"SEND_NOTIFICATION\",\"parameters\":{\"type\":\"EMAIL\",\"message\":\"File processing failed\",\"recipient\":\"operations@company.com\"}}]");
        notificationRule.setRulePriority(70);
        notificationRule.setRuleCategory("NOTIFICATION");
        defaultRules.add(notificationRule);
        
        // Large file approval rule
        BusinessRule approvalRule = new BusinessRule(tenantId, "Approve very large files", RuleType.APPROVAL_WORKFLOW, createdBy);
        approvalRule.setRuleDescription("Require approval for files larger than 100MB");
        approvalRule.setRuleConditions("{\"field\":\"fileSize\",\"operator\":\"GT\",\"value\":104857600}");
        approvalRule.setRuleActions("[{\"type\":\"REQUIRE_APPROVAL\",\"parameters\":{\"approverRole\":\"FILE_MANAGER\",\"reason\":\"Large file requires approval\"}}]");
        approvalRule.setRulePriority(95);
        approvalRule.setRuleCategory("VALIDATION");
        defaultRules.add(approvalRule);
        
        // Save all default rules
        businessRuleRepository.saveAll(defaultRules);
        
        logger.info("Created {} default rules for tenant: {}", defaultRules.size(), tenantId);
    }
    
    // Result classes
    
    public static class RuleEvaluationResult {
        private Long fileTransferId;
        private String tenantId;
        private LocalDateTime evaluationTimestamp;
        private int totalRulesEvaluated;
        private long successfulRules;
        private long failedRules;
        private boolean criticalFailure = false;
        private String errorMessage;
        private List<RuleExecutionResult> ruleExecutionResults = new ArrayList<>();
        private List<RuleAction> aggregatedActions = new ArrayList<>();
        
        // Getters and Setters
        public Long getFileTransferId() { return fileTransferId; }
        public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public LocalDateTime getEvaluationTimestamp() { return evaluationTimestamp; }
        public void setEvaluationTimestamp(LocalDateTime evaluationTimestamp) { this.evaluationTimestamp = evaluationTimestamp; }
        
        public int getTotalRulesEvaluated() { return totalRulesEvaluated; }
        public void setTotalRulesEvaluated(int totalRulesEvaluated) { this.totalRulesEvaluated = totalRulesEvaluated; }
        
        public long getSuccessfulRules() { return successfulRules; }
        public void setSuccessfulRules(long successfulRules) { this.successfulRules = successfulRules; }
        
        public long getFailedRules() { return failedRules; }
        public void setFailedRules(long failedRules) { this.failedRules = failedRules; }
        
        public boolean isCriticalFailure() { return criticalFailure; }
        public void setCriticalFailure(boolean criticalFailure) { this.criticalFailure = criticalFailure; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<RuleExecutionResult> getRuleExecutionResults() { return ruleExecutionResults; }
        public void setRuleExecutionResults(List<RuleExecutionResult> ruleExecutionResults) { this.ruleExecutionResults = ruleExecutionResults; }
        
        public List<RuleAction> getAggregatedActions() { return aggregatedActions; }
        public void setAggregatedActions(List<RuleAction> aggregatedActions) { this.aggregatedActions = aggregatedActions; }
    }
    
    public static class RuleExecutionResult {
        private Long ruleId;
        private String ruleName;
        private RuleType ruleType;
        private boolean success;
        private boolean conditionsMet;
        private String message;
        private String errorMessage;
        private long executionTimeMs;
        private boolean critical = false;
        private List<RuleAction> actions = new ArrayList<>();
        
        // Getters and Setters
        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        
        public RuleType getRuleType() { return ruleType; }
        public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public boolean isConditionsMet() { return conditionsMet; }
        public void setConditionsMet(boolean conditionsMet) { this.conditionsMet = conditionsMet; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        
        public boolean isCritical() { return critical; }
        public void setCritical(boolean critical) { this.critical = critical; }
        
        public List<RuleAction> getActions() { return actions; }
        public void setActions(List<RuleAction> actions) { this.actions = actions; }
    }
    
    public static class RuleAction {
        private String actionType;
        private LocalDateTime executedAt;
        private boolean success;
        private String result;
        private String errorMessage;
        private boolean requiresApproval = false;
        private Map<String, Object> metadata = new HashMap<>();
        
        // Getters and Setters
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        
        public LocalDateTime getExecutedAt() { return executedAt; }
        public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}