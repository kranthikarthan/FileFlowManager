package com.filetransfer.web.model.backup;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data models for disaster recovery operations
 */
public class DisasterRecoveryModels {

    /**
     * Recovery plan status
     */
    public enum RecoveryPlanStatus {
        DRAFT,      // Plan is being created
        ACTIVE,     // Plan is active and ready
        TESTING,    // Plan is being tested
        SUSPENDED,  // Plan is temporarily suspended
        ARCHIVED    // Plan is archived
    }

    /**
     * Recovery trigger types
     */
    public enum RecoveryTriggerType {
        DISASTER,           // Actual disaster or failure
        PLANNED_MAINTENANCE, // Planned maintenance window
        TESTING,            // Recovery testing
        MANUAL,             // Manual trigger by admin
        AUTOMATIC           // Automatic trigger by monitoring
    }

    /**
     * Recovery test types
     */
    public enum RecoveryTestType {
        WALKTHROUGH,    // Document review and walkthrough
        TABLETOP,       // Tabletop discussion exercise
        SIMULATION,     // Simulated recovery without actual changes
        FULL_TEST       // Full recovery test with actual systems
    }

    /**
     * Health status levels
     */
    public enum HealthStatus {
        HEALTHY,    // System operating normally
        WARNING,    // Minor issues detected
        DEGRADED,   // Performance or functionality impacted
        CRITICAL,   // Major issues requiring immediate attention
        DOWN        // System is down or unreachable
    }

    /**
     * Recovery plan priority levels
     */
    public enum RecoveryPriority {
        CRITICAL,   // Business critical - immediate recovery required
        HIGH,       // High priority - recovery within hours
        MEDIUM,     // Medium priority - recovery within days
        LOW         // Low priority - recovery as resources permit
    }

    /**
     * Disaster recovery plan
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisasterRecoveryPlan {
        private String planId;
        private String planName;
        private String description;
        private RecoveryPriority priority;
        private int rto; // Recovery Time Objective in minutes
        private int rpo; // Recovery Point Objective in minutes
        
        private List<String> applicationComponents;
        private List<String> dataComponents;
        private List<RecoveryStep> recoverySteps;
        private String testingSchedule;
        private List<Contact> contacts;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastTested;
        
        private RecoveryPlanStatus status;
        private Map<String, Object> metadata;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DisasterRecoveryPlan plan = new DisasterRecoveryPlan();

            public Builder planId(String planId) { plan.planId = planId; return this; }
            public Builder planName(String planName) { plan.planName = planName; return this; }
            public Builder description(String description) { plan.description = description; return this; }
            public Builder priority(RecoveryPriority priority) { plan.priority = priority; return this; }
            public Builder rto(int rto) { plan.rto = rto; return this; }
            public Builder rpo(int rpo) { plan.rpo = rpo; return this; }
            public Builder applicationComponents(List<String> applicationComponents) { plan.applicationComponents = applicationComponents; return this; }
            public Builder dataComponents(List<String> dataComponents) { plan.dataComponents = dataComponents; return this; }
            public Builder recoverySteps(List<RecoveryStep> recoverySteps) { plan.recoverySteps = recoverySteps; return this; }
            public Builder testingSchedule(String testingSchedule) { plan.testingSchedule = testingSchedule; return this; }
            public Builder contacts(List<Contact> contacts) { plan.contacts = contacts; return this; }
            public Builder createdAt(LocalDateTime createdAt) { plan.createdAt = createdAt; return this; }
            public Builder lastUpdated(LocalDateTime lastUpdated) { plan.lastUpdated = lastUpdated; return this; }
            public Builder lastTested(LocalDateTime lastTested) { plan.lastTested = lastTested; return this; }
            public Builder status(RecoveryPlanStatus status) { plan.status = status; return this; }
            public Builder metadata(Map<String, Object> metadata) { plan.metadata = metadata; return this; }

            public DisasterRecoveryPlan build() { return plan; }
        }

        // Getters and setters
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public RecoveryPriority getPriority() { return priority; }
        public void setPriority(RecoveryPriority priority) { this.priority = priority; }
        public int getRto() { return rto; }
        public void setRto(int rto) { this.rto = rto; }
        public int getRpo() { return rpo; }
        public void setRpo(int rpo) { this.rpo = rpo; }
        public List<String> getApplicationComponents() { return applicationComponents; }
        public void setApplicationComponents(List<String> applicationComponents) { this.applicationComponents = applicationComponents; }
        public List<String> getDataComponents() { return dataComponents; }
        public void setDataComponents(List<String> dataComponents) { this.dataComponents = dataComponents; }
        public List<RecoveryStep> getRecoverySteps() { return recoverySteps; }
        public void setRecoverySteps(List<RecoveryStep> recoverySteps) { this.recoverySteps = recoverySteps; }
        public String getTestingSchedule() { return testingSchedule; }
        public void setTestingSchedule(String testingSchedule) { this.testingSchedule = testingSchedule; }
        public List<Contact> getContacts() { return contacts; }
        public void setContacts(List<Contact> contacts) { this.contacts = contacts; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        public LocalDateTime getLastTested() { return lastTested; }
        public void setLastTested(LocalDateTime lastTested) { this.lastTested = lastTested; }
        public RecoveryPlanStatus getStatus() { return status; }
        public void setStatus(RecoveryPlanStatus status) { this.status = status; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * Recovery step definition
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecoveryStep {
        private int order;
        private String name;
        private String description;
        private String type; // MANUAL, AUTOMATED, SCRIPT
        private String command;
        private int estimatedDuration; // in minutes
        private List<String> dependencies;
        private String owner;
        private boolean critical;

        // Constructors, getters, and setters
        public RecoveryStep() {}

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public int getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public boolean isCritical() { return critical; }
        public void setCritical(boolean critical) { this.critical = critical; }
    }

    /**
     * Contact information
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Contact {
        private String name;
        private String role;
        private String email;
        private String phone;
        private boolean primary;

        // Constructors, getters, and setters
        public Contact() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public boolean isPrimary() { return primary; }
        public void setPrimary(boolean primary) { this.primary = primary; }
    }

    /**
     * Recovery trigger
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecoveryTrigger {
        private RecoveryTriggerType type;
        private String reason;
        private String triggeredBy;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime triggeredAt;
        
        private Map<String, Object> context;

        // Constructors, getters, and setters
        public RecoveryTrigger() {}

        public RecoveryTriggerType getType() { return type; }
        public void setType(RecoveryTriggerType type) { this.type = type; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getTriggeredBy() { return triggeredBy; }
        public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
        public LocalDateTime getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    /**
     * Recovery execution result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecoveryExecutionResult {
        private String executionId;
        private String planId;
        private RecoveryTrigger trigger;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String errorMessage;
        private List<StepResult> stepResults;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RecoveryExecutionResult result = new RecoveryExecutionResult();

            public Builder executionId(String executionId) { result.executionId = executionId; return this; }
            public Builder planId(String planId) { result.planId = planId; return this; }
            public Builder trigger(RecoveryTrigger trigger) { result.trigger = trigger; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder stepResults(List<StepResult> stepResults) { result.stepResults = stepResults; return this; }

            public RecoveryExecutionResult build() { return result; }
        }

        // Getters and setters
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public RecoveryTrigger getTrigger() { return trigger; }
        public void setTrigger(RecoveryTrigger trigger) { this.trigger = trigger; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<StepResult> getStepResults() { return stepResults; }
        public void setStepResults(List<StepResult> stepResults) { this.stepResults = stepResults; }
    }

    /**
     * Step execution result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StepResult {
        private int stepOrder;
        private String stepName;
        private boolean success;
        private long duration;
        private String errorMessage;
        private String output;

        // Constructors, getters, and setters
        public StepResult() {}

        public int getStepOrder() { return stepOrder; }
        public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
    }

    /**
     * System health status
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SystemHealthStatus {
        private String region;
        private HealthStatus overallHealth;
        private HealthStatus databaseHealth;
        private HealthStatus applicationHealth;
        private HealthStatus networkHealth;
        private HealthStatus storageHealth;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastChecked;
        
        private Map<String, Object> details;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SystemHealthStatus status = new SystemHealthStatus();

            public Builder region(String region) { status.region = region; return this; }
            public Builder overallHealth(HealthStatus overallHealth) { status.overallHealth = overallHealth; return this; }
            public Builder databaseHealth(HealthStatus databaseHealth) { status.databaseHealth = databaseHealth; return this; }
            public Builder applicationHealth(HealthStatus applicationHealth) { status.applicationHealth = applicationHealth; return this; }
            public Builder networkHealth(HealthStatus networkHealth) { status.networkHealth = networkHealth; return this; }
            public Builder storageHealth(HealthStatus storageHealth) { status.storageHealth = storageHealth; return this; }
            public Builder lastChecked(LocalDateTime lastChecked) { status.lastChecked = lastChecked; return this; }
            public Builder details(Map<String, Object> details) { status.details = details; return this; }

            public SystemHealthStatus build() { return status; }
        }

        // Getters and setters
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public HealthStatus getOverallHealth() { return overallHealth; }
        public void setOverallHealth(HealthStatus overallHealth) { this.overallHealth = overallHealth; }
        public HealthStatus getDatabaseHealth() { return databaseHealth; }
        public void setDatabaseHealth(HealthStatus databaseHealth) { this.databaseHealth = databaseHealth; }
        public HealthStatus getApplicationHealth() { return applicationHealth; }
        public void setApplicationHealth(HealthStatus applicationHealth) { this.applicationHealth = applicationHealth; }
        public HealthStatus getNetworkHealth() { return networkHealth; }
        public void setNetworkHealth(HealthStatus networkHealth) { this.networkHealth = networkHealth; }
        public HealthStatus getStorageHealth() { return storageHealth; }
        public void setStorageHealth(HealthStatus storageHealth) { this.storageHealth = storageHealth; }
        public LocalDateTime getLastChecked() { return lastChecked; }
        public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    /**
     * Failover request
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailoverRequest {
        private String sourceRegion;
        private String targetRegion;
        private boolean automatic;
        private String reason;
        private Map<String, Object> options;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FailoverRequest request = new FailoverRequest();

            public Builder sourceRegion(String sourceRegion) { request.sourceRegion = sourceRegion; return this; }
            public Builder targetRegion(String targetRegion) { request.targetRegion = targetRegion; return this; }
            public Builder automatic(boolean automatic) { request.automatic = automatic; return this; }
            public Builder reason(String reason) { request.reason = reason; return this; }
            public Builder options(Map<String, Object> options) { request.options = options; return this; }

            public FailoverRequest build() { return request; }
        }

        // Getters and setters
        public String getSourceRegion() { return sourceRegion; }
        public void setSourceRegion(String sourceRegion) { this.sourceRegion = sourceRegion; }
        public String getTargetRegion() { return targetRegion; }
        public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }
        public boolean isAutomatic() { return automatic; }
        public void setAutomatic(boolean automatic) { this.automatic = automatic; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * Failover result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailoverResult {
        private String failoverId;
        private String sourceRegion;
        private String targetRegion;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String errorMessage;
        private List<FailoverStep> steps;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FailoverResult result = new FailoverResult();

            public Builder failoverId(String failoverId) { result.failoverId = failoverId; return this; }
            public Builder sourceRegion(String sourceRegion) { result.sourceRegion = sourceRegion; return this; }
            public Builder targetRegion(String targetRegion) { result.targetRegion = targetRegion; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder steps(List<FailoverStep> steps) { result.steps = steps; return this; }

            public FailoverResult build() { return result; }
        }

        // Getters and setters
        public String getFailoverId() { return failoverId; }
        public void setFailoverId(String failoverId) { this.failoverId = failoverId; }
        public String getSourceRegion() { return sourceRegion; }
        public void setSourceRegion(String sourceRegion) { this.sourceRegion = sourceRegion; }
        public String getTargetRegion() { return targetRegion; }
        public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<FailoverStep> getSteps() { return steps; }
        public void setSteps(List<FailoverStep> steps) { this.steps = steps; }
    }

    /**
     * Failover step
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailoverStep {
        private String name;
        private String description;
        private boolean success;
        private long duration;
        private String errorMessage;

        // Constructors, getters, and setters
        public FailoverStep() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Recovery test result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecoveryTestResult {
        private String testId;
        private String planId;
        private RecoveryTestType testType;
        private boolean success;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private long duration;
        private String errorMessage;
        private List<TestStepResult> testResults;
        private String summary;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RecoveryTestResult result = new RecoveryTestResult();

            public Builder testId(String testId) { result.testId = testId; return this; }
            public Builder planId(String planId) { result.planId = planId; return this; }
            public Builder testType(RecoveryTestType testType) { result.testType = testType; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder startTime(LocalDateTime startTime) { result.startTime = startTime; return this; }
            public Builder endTime(LocalDateTime endTime) { result.endTime = endTime; return this; }
            public Builder duration(long duration) { result.duration = duration; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public Builder testResults(List<TestStepResult> testResults) { result.testResults = testResults; return this; }
            public Builder summary(String summary) { result.summary = summary; return this; }

            public RecoveryTestResult build() { return result; }
        }

        // Getters and setters
        public String getTestId() { return testId; }
        public void setTestId(String testId) { this.testId = testId; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public RecoveryTestType getTestType() { return testType; }
        public void setTestType(RecoveryTestType testType) { this.testType = testType; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<TestStepResult> getTestResults() { return testResults; }
        public void setTestResults(List<TestStepResult> testResults) { this.testResults = testResults; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    /**
     * Test step result
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestStepResult {
        private String stepName;
        private boolean passed;
        private String notes;
        private String recommendation;

        // Constructors, getters, and setters
        public TestStepResult() {}

        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    /**
     * Disaster recovery status
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DisasterRecoveryStatus {
        private boolean enabled;
        private String currentActiveRegion;
        private String primaryRegion;
        private String secondaryRegion;
        private boolean failoverInProgress;
        private boolean replicationEnabled;
        private boolean automaticFailover;
        private int rto;
        private int rpo;
        private int planCount;
        private SystemHealthStatus systemHealth;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastHealthCheck;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DisasterRecoveryStatus status = new DisasterRecoveryStatus();

            public Builder enabled(boolean enabled) { status.enabled = enabled; return this; }
            public Builder currentActiveRegion(String currentActiveRegion) { status.currentActiveRegion = currentActiveRegion; return this; }
            public Builder primaryRegion(String primaryRegion) { status.primaryRegion = primaryRegion; return this; }
            public Builder secondaryRegion(String secondaryRegion) { status.secondaryRegion = secondaryRegion; return this; }
            public Builder failoverInProgress(boolean failoverInProgress) { status.failoverInProgress = failoverInProgress; return this; }
            public Builder replicationEnabled(boolean replicationEnabled) { status.replicationEnabled = replicationEnabled; return this; }
            public Builder automaticFailover(boolean automaticFailover) { status.automaticFailover = automaticFailover; return this; }
            public Builder rto(int rto) { status.rto = rto; return this; }
            public Builder rpo(int rpo) { status.rpo = rpo; return this; }
            public Builder planCount(int planCount) { status.planCount = planCount; return this; }
            public Builder systemHealth(SystemHealthStatus systemHealth) { status.systemHealth = systemHealth; return this; }
            public Builder lastHealthCheck(LocalDateTime lastHealthCheck) { status.lastHealthCheck = lastHealthCheck; return this; }

            public DisasterRecoveryStatus build() { return status; }
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCurrentActiveRegion() { return currentActiveRegion; }
        public void setCurrentActiveRegion(String currentActiveRegion) { this.currentActiveRegion = currentActiveRegion; }
        public String getPrimaryRegion() { return primaryRegion; }
        public void setPrimaryRegion(String primaryRegion) { this.primaryRegion = primaryRegion; }
        public String getSecondaryRegion() { return secondaryRegion; }
        public void setSecondaryRegion(String secondaryRegion) { this.secondaryRegion = secondaryRegion; }
        public boolean isFailoverInProgress() { return failoverInProgress; }
        public void setFailoverInProgress(boolean failoverInProgress) { this.failoverInProgress = failoverInProgress; }
        public boolean isReplicationEnabled() { return replicationEnabled; }
        public void setReplicationEnabled(boolean replicationEnabled) { this.replicationEnabled = replicationEnabled; }
        public boolean isAutomaticFailover() { return automaticFailover; }
        public void setAutomaticFailover(boolean automaticFailover) { this.automaticFailover = automaticFailover; }
        public int getRto() { return rto; }
        public void setRto(int rto) { this.rto = rto; }
        public int getRpo() { return rpo; }
        public void setRpo(int rpo) { this.rpo = rpo; }
        public int getPlanCount() { return planCount; }
        public void setPlanCount(int planCount) { this.planCount = planCount; }
        public SystemHealthStatus getSystemHealth() { return systemHealth; }
        public void setSystemHealth(SystemHealthStatus systemHealth) { this.systemHealth = systemHealth; }
        public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
    }

    /**
     * Create recovery plan request
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateRecoveryPlanRequest {
        private String planName;
        private String description;
        private RecoveryPriority priority;
        private Integer rto;
        private Integer rpo;
        private List<String> applicationComponents;
        private List<String> dataComponents;
        private List<RecoveryStep> recoverySteps;
        private String testingSchedule;
        private List<Contact> contacts;

        // Constructors, getters, and setters
        public CreateRecoveryPlanRequest() {}

        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public RecoveryPriority getPriority() { return priority; }
        public void setPriority(RecoveryPriority priority) { this.priority = priority; }
        public Integer getRto() { return rto; }
        public void setRto(Integer rto) { this.rto = rto; }
        public Integer getRpo() { return rpo; }
        public void setRpo(Integer rpo) { this.rpo = rpo; }
        public List<String> getApplicationComponents() { return applicationComponents; }
        public void setApplicationComponents(List<String> applicationComponents) { this.applicationComponents = applicationComponents; }
        public List<String> getDataComponents() { return dataComponents; }
        public void setDataComponents(List<String> dataComponents) { this.dataComponents = dataComponents; }
        public List<RecoveryStep> getRecoverySteps() { return recoverySteps; }
        public void setRecoverySteps(List<RecoveryStep> recoverySteps) { this.recoverySteps = recoverySteps; }
        public String getTestingSchedule() { return testingSchedule; }
        public void setTestingSchedule(String testingSchedule) { this.testingSchedule = testingSchedule; }
        public List<Contact> getContacts() { return contacts; }
        public void setContacts(List<Contact> contacts) { this.contacts = contacts; }
    }
}