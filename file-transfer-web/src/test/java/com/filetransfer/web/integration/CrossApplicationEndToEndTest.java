package com.filetransfer.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-Application End-to-End Integration Tests
 * Tests complete workflows that span across web, batch, and frontend applications
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CrossApplicationEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Long testTenantId;
    private Long testServiceId;
    private Long testSubServiceId;
    private String testBackupId;
    private String testJobExecutionId;

    @BeforeAll
    void setUpAll() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("CROSS-E2E-1: Complete system setup and configuration workflow")
    void testCompleteSystemSetupWorkflow() throws Exception {
        // PHASE 1: Create tenant through web API
        String tenantRequest = """
            {
                "name": "Cross-App E2E Tenant",
                "description": "End-to-end cross-application test tenant",
                "timezone": "America/New_York",
                "contactEmail": "crossapp@example.com",
                "type": "ENTERPRISE",
                "region": "US-EAST",
                "currency": "USD",
                "language": "en"
            }
            """;

        MvcResult tenantResult = mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.name", is("Cross-App E2E Tenant")))
                .andReturn();

        testTenantId = objectMapper.readTree(tenantResult.getResponse().getContentAsString())
                .get("id").asLong();

        // PHASE 2: Configure tenant settings
        String configRequest = """
            {
                "fileRetentionDays": 90,
                "maxFileSize": 209715200,
                "allowedFileTypes": ["JSON", "XML", "COBOL"],
                "enableAuditLogging": true,
                "enableNotifications": true,
                "enableBatchProcessing": true,
                "batchProcessingSchedule": "0 */2 * * * *",
                "monitoringEnabled": true,
                "alertingEnabled": true
            }
            """;

        mockMvc.perform(put("/api/v2/tenants/" + testTenantId + "/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(configRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.enableBatchProcessing", is(true)));

        // PHASE 3: Create service hierarchy
        createCompleteServiceHierarchy();

        // PHASE 4: Configure monitoring and alerting
        configureMonitoringAndAlerting();

        // PHASE 5: Set up backup strategy
        configureBackupStrategy();

        // PHASE 6: Verify complete system configuration
        verifySystemConfiguration();
    }

    @Test
    @Order(2)
    @DisplayName("CROSS-E2E-2: Web-triggered batch processing workflow")
    void testWebTriggeredBatchProcessingWorkflow() throws Exception {
        // PHASE 1: Upload files through web API
        String fileUploadRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "subServiceId": %d,
                "direction": "INBOUND",
                "files": [
                    {
                        "fileName": "batch-test-1.json",
                        "content": "{\\"id\\": \\"BATCH001\\", \\"data\\": \\"test data 1\\"}",
                        "fileType": "JSON"
                    },
                    {
                        "fileName": "batch-test-2.json", 
                        "content": "{\\"id\\": \\"BATCH002\\", \\"data\\": \\"test data 2\\"}",
                        "fileType": "JSON"
                    }
                ],
                "processingOptions": {
                    "validateSchema": true,
                    "enableBatchProcessing": true,
                    "priority": "HIGH",
                    "notifyOnCompletion": true
                }
            }
            """.formatted(testTenantId, testServiceId, testSubServiceId);

        MvcResult uploadResult = mockMvc.perform(post("/api/files/bulk-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fileUploadRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.filesUploaded", is(2)))
                .andExpected(jsonPath("$.batchJobTriggered", is(true)))
                .andReturn();

        String batchJobId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("batchJobId").asText();

        // PHASE 2: Monitor batch job through web API
        boolean jobCompleted = false;
        int maxAttempts = 30;
        int attempts = 0;

        while (!jobCompleted && attempts < maxAttempts) {
            MvcResult statusResult = mockMvc.perform(get("/api/batch/integration/job-status/" + batchJobId))
                    .andExpected(status().isOk())
                    .andReturn();

            String status = objectMapper.readTree(statusResult.getResponse().getContentAsString())
                    .get("status").asText();

            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                jobCompleted = true;
                assertEquals("COMPLETED", status, "Batch job should complete successfully");
            } else {
                Thread.sleep(2000); // Wait 2 seconds
                attempts++;
            }
        }

        assertTrue(jobCompleted, "Batch job should complete within 60 seconds");

        // PHASE 3: Verify batch processing results through web API
        mockMvc.perform(get("/api/files/processing-results")
                .param("tenantId", testTenantId.toString())
                .param("batchJobId", batchJobId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.totalFiles", is(2)))
                .andExpected(jsonPath("$.processedFiles", is(2)))
                .andExpected(jsonPath("$.failedFiles", is(0)))
                .andExpected(jsonPath("$.successRate", is(100.0)));

        // PHASE 4: Verify notifications were sent
        mockMvc.perform(get("/api/notifications")
                .param("tenantId", testTenantId.toString())
                .param("type", "BATCH_JOB_COMPLETION"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$", hasSize(greaterThan(0))))
                .andExpected(jsonPath("$[0].message", containsString("batch job completed")));
    }

    @Test
    @Order(3)
    @DisplayName("CROSS-E2E-3: Complete backup and disaster recovery workflow")
    void testCompleteBackupAndDisasterRecoveryWorkflow() throws Exception {
        // PHASE 1: Create cross-application backup
        String backupRequest = """
            {
                "type": "FULL",
                "includeWeb": true,
                "includeBatch": true,
                "includeFrontend": true,
                "synchronizeBackups": true,
                "createSnapshot": true,
                "compression": true,
                "encryption": true,
                "verification": true,
                "description": "Cross-application E2E test backup"
            }
            """;

        MvcResult backupResult = mockMvc.perform(post("/api/backup/cross-application/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.coordinationId", notNullValue()))
                .andReturn();

        String coordinationId = objectMapper.readTree(backupResult.getResponse().getContentAsString())
                .get("coordinationId").asText();

        // PHASE 2: Monitor backup progress
        boolean backupCompleted = false;
        int maxAttempts = 30;
        int attempts = 0;

        while (!backupCompleted && attempts < maxAttempts) {
            MvcResult statusResult = mockMvc.perform(get("/api/backup/cross-application/status/" + coordinationId))
                    .andExpected(status().isOk())
                    .andReturn();

            String status = objectMapper.readTree(statusResult.getResponse().getContentAsString())
                    .get("status").asText();

            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                backupCompleted = true;
                assertEquals("COMPLETED", status, "Cross-application backup should complete successfully");
            } else {
                Thread.sleep(2000);
                attempts++;
            }
        }

        assertTrue(backupCompleted, "Cross-application backup should complete within 60 seconds");

        // PHASE 3: Verify backup components
        mockMvc.perform(get("/api/backup/cross-application/details/" + coordinationId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.webBackup.success", is(true)))
                .andExpected(jsonPath("$.batchBackup.success", is(true)))
                .andExpected(jsonPath("$.frontendBackup.success", is(true)))
                .andExpected(jsonPath("$.verification.success", is(true)));

        // PHASE 4: Test disaster recovery simulation
        String drTestRequest = """
            {
                "coordinationId": "%s",
                "testType": "SIMULATION",
                "scope": "CROSS_APPLICATION",
                "simulateFailure": {
                    "component": "PRIMARY_REGION",
                    "type": "NETWORK_PARTITION"
                }
            }
            """.formatted(coordinationId);

        mockMvc.perform(post("/api/backup/disaster-recovery/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(drTestRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.testResult.success", is(true)))
                .andExpected(jsonPath("$.testResult.applicationsTestedCount", is(3)))
                .andExpected(jsonPath("$.testResult.recoveryTimeSeconds", lessThan(300)));
    }

    @Test
    @Order(4)
    @DisplayName("CROSS-E2E-4: API version compatibility across all applications")
    void testApiVersionCompatibilityAcrossApplications() throws Exception {
        // PHASE 1: Test cross-application version negotiation
        String versionTestRequest = """
            {
                "requestedVersions": {
                    "web": "2.0",
                    "batch": "2.0",
                    "frontend": "2.0"
                },
                "fallbackStrategy": "LATEST_COMPATIBLE",
                "strictMode": false
            }
            """;

        mockMvc.perform(post("/api/versions/cross-application/negotiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionTestRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.negotiatedVersions.web", is("2.0")))
                .andExpected(jsonPath("$.negotiatedVersions.batch", is("2.0")))
                .andExpected(jsonPath("$.negotiatedVersions.frontend", is("2.0")))
                .andExpected(jsonPath("$.compatible", is(true)));

        // PHASE 2: Test mixed version compatibility
        String mixedVersionRequest = """
            {
                "requestedVersions": {
                    "web": "1.2",
                    "batch": "2.0", 
                    "frontend": "2.0"
                },
                "fallbackStrategy": "BEST_COMPATIBLE"
            }
            """;

        mockMvc.perform(post("/api/versions/cross-application/negotiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mixedVersionRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.negotiatedVersions.web", anyOf(is("1.2"), is("2.0"))))
                .andExpected(jsonPath("$.negotiatedVersions.batch", is("2.0")))
                .andExpected(jsonPath("$.compatible", is(true)))
                .andExpected(jsonPath("$.warnings", notNullValue()));

        // PHASE 3: Test cross-application feature compatibility
        mockMvc.perform(get("/api/versions/cross-application/features")
                .param("webVersion", "2.0")
                .param("batchVersion", "2.0")
                .param("frontendVersion", "2.0"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.commonFeatures", hasSize(greaterThan(0))))
                .andExpected(jsonPath("$.enhancedFeatures.pagination", is(true)))
                .andExpected(jsonPath("$.enhancedFeatures.bulkOperations", is(true)))
                .andExpected(jsonPath("$.enhancedFeatures.advancedMonitoring", is(true)));
    }

    @Test
    @Order(5)
    @DisplayName("CROSS-E2E-5: Monitoring and observability across applications")
    void testMonitoringAndObservabilityAcrossApplications() throws Exception {
        // PHASE 1: Check system-wide health
        mockMvc.perform(get("/api/monitoring/system-health"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.overallHealth", is("UP")))
                .andExpected(jsonPath("$.applications.web.status", is("UP")))
                .andExpected(jsonPath("$.applications.batch.status", is("UP")))
                .andExpected(jsonPath("$.applications.frontend.status", is("UP")));

        // PHASE 2: Test distributed tracing
        String tracingRequest = """
            {
                "tenantId": %d,
                "operation": "CROSS_APP_FILE_PROCESSING",
                "traceId": "e2e-trace-001",
                "enableDistributedTracing": true
            }
            """.formatted(testTenantId);

        MvcResult tracingResult = mockMvc.perform(post("/api/monitoring/start-trace")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tracingRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.traceId", is("e2e-trace-001")))
                .andReturn();

        // PHASE 3: Execute operation that spans all applications
        String crossAppOperation = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "operation": "COMPLETE_FILE_PROCESSING",
                "traceId": "e2e-trace-001",
                "files": [
                    {
                        "fileName": "cross-app-test.json",
                        "content": "{\\"crossAppTest\\": true, \\"traceId\\": \\"e2e-trace-001\\"}",
                        "triggerBatchProcessing": true,
                        "updateFrontendDashboard": true
                    }
                ]
            }
            """.formatted(testTenantId, testServiceId);

        mockMvc.perform(post("/api/operations/cross-application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(crossAppOperation))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.operationId", notNullValue()))
                .andExpected(jsonPath("$.applicationsInvolved", hasSize(3)));

        // PHASE 4: Verify distributed trace
        Thread.sleep(5000); // Allow time for trace propagation

        mockMvc.perform(get("/api/monitoring/trace/e2e-trace-001"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.traceId", is("e2e-trace-001")))
                .andExpected(jsonPath("$.spans", hasSize(greaterThan(2))))
                .andExpected(jsonPath("$.applications", hasSize(3)))
                .andExpected(jsonPath("$.totalDuration", greaterThan(0)));
    }

    @Test
    @Order(6)
    @DisplayName("CROSS-E2E-6: Security and access control across applications")
    void testSecurityAndAccessControlAcrossApplications() throws Exception {
        // PHASE 1: Test cross-application authentication
        String authRequest = """
            {
                "username": "crossapp-user",
                "password": "SecureP@ssw0rd123!",
                "requestedScopes": ["web:read", "web:write", "batch:read", "batch:execute"],
                "crossApplicationAccess": true
            }
            """;

        MvcResult authResult = mockMvc.perform(post("/api/auth/cross-application/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.token", notNullValue()))
                .andExpected(jsonPath("$.scopes", hasSize(4)))
                .andReturn();

        String token = objectMapper.readTree(authResult.getResponse().getContentAsString())
                .get("token").asText();

        // PHASE 2: Test token validation across applications
        mockMvc.perform(get("/api/v2/tenants")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        mockMvc.perform(get("/api/batch/v2/jobs/executions")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        // PHASE 3: Test cross-application authorization
        String restrictedRequest = """
            {
                "tenantId": %d,
                "operation": "ADMIN_OPERATION",
                "targetApplications": ["web", "batch", "frontend"]
            }
            """.formatted(testTenantId);

        // Should succeed with proper token
        mockMvc.perform(post("/api/admin/cross-application/operation")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(restrictedRequest))
                .andExpected(status().isOk());

        // Should fail without token
        mockMvc.perform(post("/api/admin/cross-application/operation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restrictedRequest))
                .andExpected(status().isUnauthorized());

        // PHASE 4: Test rate limiting across applications
        for (int i = 0; i < 25; i++) {
            mockMvc.perform(get("/api/v2/tenants")
                    .header("Authorization", "Bearer " + token))
                    .andExpected(status().isOk());
        }

        // Should trigger rate limiting
        mockMvc.perform(get("/api/v2/tenants")
                .header("Authorization", "Bearer " + token))
                .andExpected(anyOf(status().isOk(), status().isTooManyRequests()));
    }

    @Test
    @Order(7)
    @DisplayName("CROSS-E2E-7: Performance and scalability across applications")
    void testPerformanceAndScalabilityAcrossApplications() throws Exception {
        // PHASE 1: Test load balancing configuration
        mockMvc.perform(get("/api/scalability/load-balancing/configuration"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.webInstances", greaterThan(0)))
                .andExpected(jsonPath("$.batchInstances", greaterThan(0)))
                .andExpected(jsonPath("$.loadBalancingEnabled", is(true)));

        // PHASE 2: Test auto-scaling triggers
        String loadTestRequest = """
            {
                "tenantId": %d,
                "simulatedLoad": {
                    "concurrentUsers": 50,
                    "requestsPerUser": 10,
                    "durationSeconds": 30
                },
                "targetApplications": ["web", "batch"],
                "enableAutoScaling": true,
                "scalingThreshold": 80
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/scalability/simulate-load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loadTestRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.loadTestId", notNullValue()))
                .andExpected(jsonPath("$.autoScalingTriggered", isA(Boolean.class)));

        // PHASE 3: Monitor performance across applications
        Thread.sleep(10000); // Allow load test to run

        mockMvc.perform(get("/api/scalability/performance/cross-application"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.webApplication.responseTime", lessThan(1000)))
                .andExpected(jsonPath("$.batchApplication.throughput", greaterThan(0)))
                .andExpected(jsonPath("$.overallSystemHealth", is("HEALTHY")));
    }

    @Test
    @Order(8)
    @DisplayName("CROSS-E2E-8: Data consistency and integrity across applications")
    void testDataConsistencyAndIntegrityAcrossApplications() throws Exception {
        // PHASE 1: Create data in web application
        String dataCreationRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "operation": "CREATE_TEST_DATA",
                "data": {
                    "records": 100,
                    "dataType": "FINANCIAL_TRANSACTIONS",
                    "enableCrossAppValidation": true
                }
            }
            """.formatted(testTenantId, testServiceId);

        MvcResult dataResult = mockMvc.perform(post("/api/data/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dataCreationRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.recordsCreated", is(100)))
                .andReturn();

        String dataSetId = objectMapper.readTree(dataResult.getResponse().getContentAsString())
                .get("dataSetId").asText();

        // PHASE 2: Process data through batch application
        String batchProcessingRequest = """
            {
                "tenantId": %d,
                "dataSetId": "%s",
                "processingType": "VALIDATION_AND_TRANSFORMATION",
                "enableIntegrityChecks": true,
                "enableCrossAppConsistency": true
            }
            """.formatted(testTenantId, dataSetId);

        mockMvc.perform(post("/api/batch/integration/process-dataset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchProcessingRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.batchJobStarted", is(true)))
                .andExpected(jsonPath("$.integrityChecksEnabled", is(true)));

        // PHASE 3: Wait for processing and verify consistency
        Thread.sleep(15000); // Allow batch processing to complete

        mockMvc.perform(get("/api/data/integrity-check/" + dataSetId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.consistencyCheck.passed", is(true)))
                .andExpected(jsonPath("$.webApplicationData.recordCount", is(100)))
                .andExpected(jsonPath("$.batchApplicationData.processedCount", is(100)))
                .andExpected(jsonPath("$.dataIntegrityScore", greaterThan(95.0)));

        // PHASE 4: Verify frontend dashboard reflects accurate data
        mockMvc.perform(get("/api/dashboard/data-consistency")
                .param("tenantId", testTenantId.toString())
                .param("dataSetId", dataSetId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.frontendDisplayData.recordCount", is(100)))
                .andExpected(jsonPath("$.realTimeSync", is(true)))
                .andExpected(jsonPath("$.lastSyncTimestamp", notNullValue()));
    }

    // Helper methods

    private void createCompleteServiceHierarchy() throws Exception {
        // Create service
        String serviceRequest = """
            {
                "tenantId": %d,
                "name": "E2E Cross-App Service",
                "description": "Cross-application end-to-end test service",
                "serviceCode": "CROSS001",
                "active": true,
                "enableBatchProcessing": true,
                "enableMonitoring": true
            }
            """.formatted(testTenantId);

        MvcResult serviceResult = mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceRequest))
                .andExpected(status().isCreated())
                .andReturn();

        testServiceId = objectMapper.readTree(serviceResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Create sub-service
        String subServiceRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "name": "E2E Cross-App Sub-Service",
                "description": "Cross-application sub-service",
                "direction": "INBOUND",
                "active": true,
                "enableBatchProcessing": true
            }
            """.formatted(testTenantId, testServiceId);

        MvcResult subServiceResult = mockMvc.perform(post("/api/subservices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subServiceRequest))
                .andExpected(status().isCreated())
                .andReturn();

        testSubServiceId = objectMapper.readTree(subServiceResult.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private void configureMonitoringAndAlerting() throws Exception {
        String monitoringRequest = """
            {
                "tenantId": %d,
                "enableCrossAppMonitoring": true,
                "monitoringLevel": "DETAILED",
                "alertThresholds": {
                    "errorRate": 5.0,
                    "responseTime": 1000,
                    "queueDepth": 100
                },
                "notificationChannels": ["EMAIL", "DASHBOARD"]
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/monitoring/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(monitoringRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.crossAppMonitoringEnabled", is(true)));
    }

    private void configureBackupStrategy() throws Exception {
        String backupStrategyRequest = """
            {
                "tenantId": %d,
                "strategy": "CROSS_APPLICATION",
                "schedule": {
                    "fullBackup": "0 2 * * *",
                    "incrementalBackup": "0 */4 * * *"
                },
                "retention": {
                    "days": 90,
                    "versions": 12
                },
                "crossAppCoordination": true,
                "verificationEnabled": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/backup/strategy/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupStrategyRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.crossAppCoordination", is(true)));
    }

    private void verifySystemConfiguration() throws Exception {
        // Verify tenant configuration
        mockMvc.perform(get("/api/v2/tenants/" + testTenantId + "/configuration"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.enableBatchProcessing", is(true)))
                .andExpected(jsonPath("$.monitoringEnabled", is(true)));

        // Verify service configuration
        mockMvc.perform(get("/api/services/" + testServiceId + "/configuration"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.enableBatchProcessing", is(true)))
                .andExpected(jsonPath("$.enableMonitoring", is(true)));

        // Verify cross-application integration
        mockMvc.perform(get("/api/integration/cross-application/status")
                .param("tenantId", testTenantId.toString()))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.webIntegration", is("ACTIVE")))
                .andExpected(jsonPath("$.batchIntegration", is("ACTIVE")))
                .andExpected(jsonPath("$.frontendIntegration", is("ACTIVE")));
    }

    @AfterAll
    void cleanUp() throws Exception {
        // Clean up cross-application test data
        if (testTenantId != null) {
            try {
                mockMvc.perform(delete("/api/v2/tenants/" + testTenantId))
                        .andExpected(status().isNoContent());
            } catch (Exception e) {
                System.out.println("Cleanup warning: " + e.getMessage());
            }
        }
    }
}