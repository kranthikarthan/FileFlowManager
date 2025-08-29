package com.filetransfer.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced Features End-to-End Integration Tests
 * Tests advanced features like holidays, cut-off extensions, SSO, monitoring, and security
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdvancedFeaturesEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Long testTenantId;
    private Long testSubServiceId;
    private Long testHolidayId;
    private Long testExtensionId;

    @BeforeAll
    void setUpAll() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("ADV-E2E-1: Complete holiday management and processing impact workflow")
    void testCompleteHolidayManagementWorkflow() throws Exception {
        // Setup test tenant and sub-service first
        setupTestTenantAndSubService();

        // PHASE 1: Create holiday configuration
        String holidayRequest = """
            {
                "tenantId": %d,
                "name": "E2E Test Holiday",
                "date": "2024-01-01",
                "type": "NATIONAL",
                "recurring": true,
                "recurrencePattern": "ANNUAL",
                "affectsProcessing": true,
                "processingAction": "SKIP_PROCESSING",
                "description": "New Year Day - E2E test holiday"
            }
            """.formatted(testTenantId);

        MvcResult holidayResult = mockMvc.perform(post("/api/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content(holidayRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("E2E Test Holiday")))
                .andExpect(jsonPath("$.affectsProcessing", is(true)))
                .andReturn();

        testHolidayId = objectMapper.readTree(holidayResult.getResponse().getContentAsString())
                .get("id").asLong();

        // PHASE 2: Test holiday impact on cut-off times
        String cutOffImpactRequest = """
            {
                "tenantId": %d,
                "subServiceId": %d,
                "checkDate": "2024-01-01",
                "originalCutOffTime": "23:59"
            }
            """.formatted(testTenantId, testSubServiceId);

        mockMvc.perform(post("/api/cutoff-times/check-holiday-impact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cutOffImpactRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holidayDetected", is(true)))
                .andExpect(jsonPath("$.processingAction", is("SKIP_PROCESSING")))
                .andExpect(jsonPath("$.nextProcessingDate", is("2024-01-02")))
                .andExpect(jsonPath("$.adjustedCutOffTime", notNullValue()));

        // PHASE 3: Test holiday calendar generation
        mockMvc.perform(get("/api/holidays/calendar")
                .param("tenantId", testTenantId.toString())
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holidays", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.holidays[?(@.name == 'E2E Test Holiday')]", hasSize(1)))
                .andExpect(jsonPath("$.processingImpactDays", hasSize(greaterThan(0))));

        // PHASE 4: Test holiday processing workflow
        String processingRequest = """
            {
                "tenantId": %d,
                "subServiceId": %d,
                "processingDate": "2024-01-01",
                "files": [
                    {
                        "fileName": "holiday-test.json",
                        "content": "{\\"holidayTest\\": true}"
                    }
                ],
                "respectHolidays": true
            }
            """.formatted(testTenantId, testSubServiceId);

        mockMvc.perform(post("/api/files/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(processingRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingSkipped", is(true)))
                .andExpect(jsonPath("$.reason", is("HOLIDAY_PROCESSING_DISABLED")))
                .andExpect(jsonPath("$.rescheduledDate", is("2024-01-02")));
    }

    @Test
    @Order(2)
    @DisplayName("ADV-E2E-2: Complete cut-off extension request and approval workflow")
    void testCompleteCutOffExtensionWorkflow() throws Exception {
        // PHASE 1: Request cut-off extension
        String extensionRequest = """
            {
                "tenantId": %d,
                "subServiceId": %d,
                "extensionDate": "2023-12-31",
                "requestedExtensionMinutes": 120,
                "reason": "Year-end processing requires additional time for data reconciliation",
                "businessJustification": "Critical financial year-end closing process",
                "priority": "HIGH",
                "requestedBy": "finance-manager",
                "urgency": "CRITICAL",
                "estimatedCompletionTime": "01:30"
            }
            """.formatted(testTenantId, testSubServiceId);

        MvcResult extensionResult = mockMvc.perform(post("/api/cutoff-extensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extensionRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.requestedExtensionMinutes", is(120)))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andReturn();

        testExtensionId = objectMapper.readTree(extensionResult.getResponse().getContentAsString())
                .get("id").asLong();

        // PHASE 2: Manager review and approval
        String approvalRequest = """
            {
                "approvedBy": "senior-manager",
                "approvalComments": "Approved for critical year-end processing",
                "approvedExtensionMinutes": 120,
                "conditions": [
                    "Must complete by 01:30 AM",
                    "Provide hourly progress updates",
                    "Notify completion immediately"
                ],
                "monitoringRequired": true,
                "escalationRequired": false
            }
            """;

        mockMvc.perform(put("/api/cutoff-extensions/" + testExtensionId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedBy", is("senior-manager")))
                .andExpect(jsonPath("$.newCutOffTime", is("01:59")))
                .andExpect(jsonPath("$.monitoringEnabled", is(true)));

        // PHASE 3: Test extension monitoring
        mockMvc.perform(get("/api/cutoff-extensions/" + testExtensionId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.remainingMinutes", greaterThan(0)))
                .andExpect(jsonPath("$.monitoringActive", is(true)))
                .andExpect(jsonPath("$.alertsConfigured", is(true)));

        // PHASE 4: Test extension completion
        String completionRequest = """
            {
                "completedBy": "operations-team",
                "completionTime": "01:15",
                "actualProcessingTime": 105,
                "completionNotes": "Year-end processing completed successfully",
                "filesProcessed": 1250,
                "successRate": 99.8
            }
            """;

        mockMvc.perform(put("/api/cutoff-extensions/" + testExtensionId + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.actualDuration", is(105)))
                .andExpect(jsonPath("$.completedEarly", is(true)));
    }

    @Test
    @Order(3)
    @DisplayName("ADV-E2E-3: Complete SSO configuration and authentication workflow")
    void testCompleteSsoConfigurationWorkflow() throws Exception {
        // PHASE 1: Configure SAML SSO
        String ssoConfigRequest = """
            {
                "tenantId": %d,
                "providerType": "SAML",
                "providerName": "Corporate SAML",
                "entityId": "https://corp.example.com/saml",
                "ssoUrl": "https://corp.example.com/saml/sso",
                "logoutUrl": "https://corp.example.com/saml/logout",
                "certificate": "-----BEGIN CERTIFICATE-----\\nMIIC...test...certificate\\n-----END CERTIFICATE-----",
                "attributeMapping": {
                    "email": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
                    "firstName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname",
                    "lastName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname",
                    "roles": "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
                },
                "active": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/sso-configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoConfigRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerType", is("SAML")))
                .andExpect(jsonPath("$.active", is(true)));

        // PHASE 2: Test SSO configuration
        String ssoTestRequest = """
            {
                "testType": "CONFIGURATION_TEST",
                "testUser": "test.user@corp.example.com",
                "validateCertificate": true,
                "validateAttributeMapping": true,
                "validateRoleMapping": true
            }
            """;

        mockMvc.perform(post("/api/sso-configurations/test")
                .param("tenantId", testTenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ssoTestRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurationValid", is(true)))
                .andExpect(jsonPath("$.certificateValid", is(true)))
                .andExpect(jsonPath("$.attributeMappingValid", is(true)))
                .andExpect(jsonPath("$.testResults.authenticationTest", is("PASSED")));

        // PHASE 3: Test SSO authentication flow
        String authRequest = """
            {
                "samlResponse": "mock-saml-response",
                "tenantId": %d,
                "relayState": "/dashboard"
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/auth/sso/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.user.email", notNullValue()))
                .andExpect(jsonPath("$.user.roles", notNullValue()))
                .andExpect(jsonPath("$.redirectUrl", is("/dashboard")));

        // PHASE 4: Test SSO logout
        mockMvc.perform(post("/api/auth/sso/logout")
                .param("tenantId", testTenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedOut", is(true)))
                .andExpect(jsonPath("$.ssoLogoutUrl", notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("ADV-E2E-4: Complete monitoring and alerting workflow")
    void testCompleteMonitoringAndAlertingWorkflow() throws Exception {
        // PHASE 1: Configure monitoring
        String monitoringConfigRequest = """
            {
                "tenantId": %d,
                "enableSystemMonitoring": true,
                "enableApplicationMonitoring": true,
                "enablePerformanceMonitoring": true,
                "enableSecurityMonitoring": true,
                "monitoringLevel": "DETAILED",
                "metricsRetentionDays": 30,
                "alertingEnabled": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/monitoring/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(monitoringConfigRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monitoringEnabled", is(true)))
                .andExpect(jsonPath("$.alertingEnabled", is(true)));

        // PHASE 2: Create alert rules
        String alertRuleRequest = """
            {
                "tenantId": %d,
                "ruleName": "High Error Rate Alert",
                "metric": "ERROR_RATE",
                "condition": "GREATER_THAN",
                "threshold": 5.0,
                "timeWindow": 15,
                "severity": "WARNING",
                "notificationChannels": ["EMAIL", "DASHBOARD"],
                "active": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/alerts/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRuleRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active", is(true)));

        // PHASE 3: Simulate alert triggering
        String alertTriggerRequest = """
            {
                "tenantId": %d,
                "level": "WARNING",
                "message": "E2E test alert - high error rate detected",
                "component": "FILE_PROCESSING",
                "metadata": {
                    "errorRate": 7.5,
                    "timeWindow": "15 minutes",
                    "affectedFiles": 15
                }
            }
            """.formatted(testTenantId);

        MvcResult alertResult = mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertTriggerRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level", is("WARNING")))
                .andExpect(jsonPath("$.component", is("FILE_PROCESSING")))
                .andReturn();

        Long alertId = objectMapper.readTree(alertResult.getResponse().getContentAsString())
                .get("id").asLong();

        // PHASE 4: Test alert acknowledgment and resolution
        String acknowledgmentRequest = """
            {
                "acknowledgedBy": "operations-team",
                "acknowledgmentComments": "Investigating high error rate issue",
                "estimatedResolutionTime": 30,
                "assignedTo": "senior-engineer"
            }
            """;

        mockMvc.perform(put("/api/alerts/" + alertId + "/acknowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(acknowledgmentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACKNOWLEDGED")))
                .andExpect(jsonPath("$.acknowledgedBy", is("operations-team")));

        // PHASE 5: Test alert resolution
        String resolutionRequest = """
            {
                "resolvedBy": "senior-engineer",
                "resolution": "Fixed configuration issue causing validation errors",
                "rootCause": "Schema validation rule misconfiguration",
                "preventiveMeasures": "Added automated schema validation checks",
                "resolutionTime": 25
            }
            """;

        mockMvc.perform(put("/api/alerts/" + alertId + "/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resolutionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.resolutionTime", is(25)));
    }

    @Test
    @Order(5)
    @DisplayName("ADV-E2E-5: Complete security monitoring and incident response workflow")
    void testCompleteSecurityMonitoringWorkflow() throws Exception {
        // PHASE 1: Configure security monitoring
        String securityConfigRequest = """
            {
                "tenantId": %d,
                "enableSecurityMonitoring": true,
                "enableThreatDetection": true,
                "enableAuditLogging": true,
                "securityLevel": "HIGH",
                "alertThresholds": {
                    "failedLoginAttempts": 5,
                    "suspiciousApiCalls": 10,
                    "dataAccessViolations": 1,
                    "rateLimitViolations": 20
                },
                "incidentResponseEnabled": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/security/monitoring/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(securityConfigRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.securityMonitoringEnabled", is(true)))
                .andExpect(jsonPath("$.threatDetectionEnabled", is(true)));

        // PHASE 2: Simulate security events
        String[] securityEvents = {
            """
            {
                "eventType": "FAILED_LOGIN_ATTEMPT",
                "tenantId": %d,
                "sourceIp": "192.168.1.100",
                "username": "attacker@malicious.com",
                "timestamp": "%s",
                "severity": "MEDIUM"
            }
            """.formatted(testTenantId, java.time.LocalDateTime.now()),
            
            """
            {
                "eventType": "SUSPICIOUS_API_CALLS",
                "tenantId": %d,
                "sourceIp": "10.0.0.50",
                "endpoint": "/api/v2/tenants",
                "requestCount": 100,
                "timeWindow": "1 minute",
                "severity": "HIGH"
            }
            """.formatted(testTenantId),
            
            """
            {
                "eventType": "DATA_ACCESS_VIOLATION",
                "tenantId": %d,
                "userId": "unauthorized-user",
                "attemptedResource": "/api/admin/sensitive-data",
                "accessDenied": true,
                "severity": "CRITICAL"
            }
            """.formatted(testTenantId)
        };

        for (String eventJson : securityEvents) {
            mockMvc.perform(post("/api/security/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(eventJson))
                    .andExpect(status().isCreated());
        }

        // PHASE 3: Verify security alerts generated
        mockMvc.perform(get("/api/security/alerts")
                .param("tenantId", testTenantId.toString())
                .param("severity", "HIGH,CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.eventType == 'SUSPICIOUS_API_CALLS')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.eventType == 'DATA_ACCESS_VIOLATION')]", hasSize(1)));

        // PHASE 4: Test incident response workflow
        String incidentRequest = """
            {
                "tenantId": %d,
                "incidentType": "SECURITY_BREACH_ATTEMPT",
                "severity": "CRITICAL",
                "description": "Multiple security events detected from same source",
                "affectedSystems": ["WEB_API", "DATABASE"],
                "immediateActions": [
                    "Block source IP addresses",
                    "Reset affected user sessions",
                    "Enable additional monitoring"
                ],
                "assignedTo": "security-team"
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/security/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incidentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.assignedTo", is("security-team")));
    }

    @Test
    @Order(6)
    @DisplayName("ADV-E2E-6: Complete scalability and performance workflow")
    void testCompleteScalabilityAndPerformanceWorkflow() throws Exception {
        // PHASE 1: Configure auto-scaling
        String scalingConfigRequest = """
            {
                "tenantId": %d,
                "enableAutoScaling": true,
                "scalingPolicy": {
                    "scaleUpThreshold": 80,
                    "scaleDownThreshold": 30,
                    "minInstances": 2,
                    "maxInstances": 10,
                    "cooldownPeriod": 300
                },
                "loadBalancing": {
                    "algorithm": "LEAST_CONNECTIONS",
                    "healthCheckInterval": 30,
                    "enableSessionAffinity": false
                }
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/scalability/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scalingConfigRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoScalingEnabled", is(true)))
                .andExpect(jsonPath("$.loadBalancingConfigured", is(true)));

        // PHASE 2: Simulate high load
        String loadTestRequest = """
            {
                "tenantId": %d,
                "simulatedLoad": {
                    "concurrentUsers": 50,
                    "requestsPerUser": 20,
                    "durationSeconds": 60,
                    "rampUpSeconds": 10
                },
                "targetEndpoints": [
                    "/api/v2/tenants",
                    "/api/files/upload",
                    "/api/schemas/validate"
                ],
                "enableAutoScaling": true
            }
            """.formatted(testTenantId);

        MvcResult loadTestResult = mockMvc.perform(post("/api/scalability/simulate-load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loadTestRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadTestStarted", is(true)))
                .andExpect(jsonPath("$.loadTestId", notNullValue()))
                .andReturn();

        String loadTestId = objectMapper.readTree(loadTestResult.getResponse().getContentAsString())
                .get("loadTestId").asText();

        // PHASE 3: Monitor auto-scaling events
        Thread.sleep(30000); // Allow load test to run and trigger scaling

        mockMvc.perform(get("/api/scalability/auto-scaling/events")
                .param("tenantId", testTenantId.toString())
                .param("loadTestId", loadTestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scalingEvents", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.scalingEvents[0].action", oneOf("SCALE_UP", "SCALE_DOWN")))
                .andExpect(jsonPath("$.currentInstances", greaterThanOrEqualTo(2)));

        // PHASE 4: Verify performance metrics
        mockMvc.perform(get("/api/scalability/performance/metrics")
                .param("tenantId", testTenantId.toString())
                .param("loadTestId", loadTestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageResponseTime", lessThan(1000)))
                .andExpect(jsonPath("$.throughput", greaterThan(10.0)))
                .andExpect(jsonPath("$.errorRate", lessThan(5.0)))
                .andExpect(jsonPath("$.cpuUtilization", lessThan(90.0)));
    }

    // Helper method to setup test data
    private void setupTestTenantAndSubService() throws Exception {
        // Create test tenant
        String tenantRequest = """
            {
                "name": "Advanced Features E2E Tenant",
                "description": "Tenant for testing advanced features",
                "timezone": "America/New_York",
                "contactEmail": "advanced@example.com",
                "active": true
            }
            """;

        MvcResult tenantResult = mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantRequest))
                .andExpect(status().isCreated())
                .andReturn();

        testTenantId = objectMapper.readTree(tenantResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Create test service and sub-service
        String serviceRequest = """
            {
                "tenantId": %d,
                "name": "Advanced Features Service",
                "description": "Service for advanced features testing",
                "active": true
            }
            """.formatted(testTenantId);

        MvcResult serviceResult = mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceRequest))
                .andExpect(status().isCreated())
                .andReturn();

        Long serviceId = objectMapper.readTree(serviceResult.getResponse().getContentAsString())
                .get("id").asLong();

        String subServiceRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "name": "Advanced Features Sub-Service",
                "description": "Sub-service for advanced features testing",
                "direction": "INBOUND",
                "active": true
            }
            """.formatted(testTenantId, serviceId);

        MvcResult subServiceResult = mockMvc.perform(post("/api/subservices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subServiceRequest))
                .andExpect(status().isCreated())
                .andReturn();

        testSubServiceId = objectMapper.readTree(subServiceResult.getResponse().getContentAsString())
                .get("id").asLong();
    }

    @AfterAll
    void cleanUp() throws Exception {
        // Clean up test data
        if (testTenantId != null) {
            try {
                mockMvc.perform(delete("/api/v2/tenants/" + testTenantId));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}