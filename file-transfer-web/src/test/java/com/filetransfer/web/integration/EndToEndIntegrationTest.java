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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for complete file transfer system
 * Tests full user workflows across all components and features
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EndToEndIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Long testTenantId;
    private Long testServiceId;
    private Long testSubServiceId;
    private Long testSchemaId;

    @BeforeAll
    void setUpAll() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-1: Complete Tenant Setup Workflow")
    void testCompleteTenantSetupWorkflow() throws Exception {
        // Step 1: Create tenant
        String tenantRequest = """
            {
                "name": "E2E Test Tenant",
                "description": "End-to-end test tenant",
                "timezone": "America/New_York",
                "contactEmail": "test@example.com",
                "active": true
            }
            """;

        MvcResult tenantResult = mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("E2E Test Tenant")))
                .andExpect(jsonPath("$.timezone", is("America/New_York")))
                .andReturn();

        testTenantId = objectMapper.readTree(tenantResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 2: Configure tenant settings
        String configRequest = """
            {
                "fileRetentionDays": 30,
                "maxFileSize": 104857600,
                "allowedFileTypes": ["XML", "JSON", "COBOL"],
                "enableAuditLogging": true,
                "enableNotifications": true
            }
            """;

        mockMvc.perform(put("/api/v2/tenants/" + testTenantId + "/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(configRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileRetentionDays", is(30)));

        // Step 3: Verify tenant health
        mockMvc.perform(get("/api/v2/tenants/" + testTenantId + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HEALTHY")));
    }

    @Test
    @Order(2)
    @DisplayName("E2E-2: Complete Service Configuration Workflow")
    void testCompleteServiceConfigurationWorkflow() throws Exception {
        // Step 1: Create service type
        String serviceRequest = """
            {
                "tenantId": %d,
                "name": "E2E Test Service",
                "description": "End-to-end test service",
                "active": true
            }
            """.formatted(testTenantId);

        MvcResult serviceResult = mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId", is(testTenantId.intValue())))
                .andReturn();

        testServiceId = objectMapper.readTree(serviceResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 2: Create sub-service
        String subServiceRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "name": "E2E Test Sub-Service",
                "description": "End-to-end test sub-service",
                "active": true
            }
            """.formatted(testTenantId, testServiceId);

        MvcResult subServiceResult = mockMvc.perform(post("/api/subservices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subServiceRequest))
                .andExpect(status().isCreated())
                .andReturn();

        testSubServiceId = objectMapper.readTree(subServiceResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 3: Configure cut-off times
        String cutOffRequest = """
            {
                "tenantId": %d,
                "subServiceId": %d,
                "cutOffTime": "23:59",
                "timezone": "America/New_York",
                "type": "END_OF_DAY",
                "active": true
            }
            """.formatted(testTenantId, testSubServiceId);

        mockMvc.perform(post("/api/cutoff-times")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cutOffRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cutOffTime", is("23:59")));
    }

    @Test
    @Order(3)
    @DisplayName("E2E-3: Schema Management Workflow")
    void testSchemaManagementWorkflow() throws Exception {
        // Step 1: Create file schema
        String schemaRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "subServiceId": %d,
                "direction": "INBOUND",
                "fileType": "JSON",
                "schemaName": "E2E Test Schema",
                "schemaContent": "{\\"type\\": \\"object\\", \\"properties\\": {\\"name\\": {\\"type\\": \\"string\\"}}}",
                "version": "1.0",
                "active": true
            }
            """.formatted(testTenantId, testServiceId, testSubServiceId);

        MvcResult schemaResult = mockMvc.perform(post("/api/schemas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(schemaRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.schemaName", is("E2E Test Schema")))
                .andReturn();

        testSchemaId = objectMapper.readTree(schemaResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 2: Validate schema
        mockMvc.perform(get("/api/schemas/" + testSchemaId + "/validate"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.valid", is(true)));

        // Step 3: Test schema reuse
        String reuseRequest = """
            {
                "sourceSchemaId": %d,
                "targetTenantId": %d,
                "targetServiceId": %d,
                "targetSubServiceId": %d,
                "targetDirection": "OUTBOUND"
            }
            """.formatted(testSchemaId, testTenantId, testServiceId, testSubServiceId);

        mockMvc.perform(post("/api/schemas/reuse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reuseRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.sourceSchemaId", is(testSchemaId.intValue())));
    }

    @Test
    @Order(4)
    @DisplayName("E2E-4: File Processing Workflow")
    void testFileProcessingWorkflow() throws Exception {
        // Step 1: Upload file for processing
        String fileData = """
            {"name": "Test File", "content": "Sample content", "type": "JSON"}
            """;

        mockMvc.perform(multipart("/api/files/upload")
                .file("file", fileData.getBytes())
                .param("tenantId", testTenantId.toString())
                .param("serviceId", testServiceId.toString())
                .param("subServiceId", testSubServiceId.toString())
                .param("direction", "INBOUND"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.fileId", notNullValue()));

        // Step 2: Check file processing status
        mockMvc.perform(get("/api/files/status")
                .param("tenantId", testTenantId.toString()))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.totalFiles", greaterThan(0)));

        // Step 3: Validate file against schema
        mockMvc.perform(post("/api/files/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantId": %d,
                        "schemaId": %d,
                        "fileContent": "%s"
                    }
                    """.formatted(testTenantId, testSchemaId, fileData.replace("\"", "\\\""))))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.valid", is(true)));
    }

    @Test
    @Order(5)
    @DisplayName("E2E-5: EOT Validation Workflow")
    void testEotValidationWorkflow() throws Exception {
        // Step 1: Configure EOT validation
        String eotConfigRequest = """
            {
                "tenantId": %d,
                "subServiceId": %d,
                "direction": "INBOUND",
                "enabled": true,
                "countFieldPath": "fileCount",
                "tolerancePercentage": 5.0,
                "strictMode": false
            }
            """.formatted(testTenantId, testSubServiceId);

        mockMvc.perform(post("/api/eot-validation/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eotConfigRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.enabled", is(true)));

        // Step 2: Submit EOT file
        String eotData = """
            {"fileCount": 5, "processingDate": "2023-12-01", "totalSize": 1048576}
            """;

        mockMvc.perform(post("/api/eot-validation/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantId": %d,
                        "subServiceId": %d,
                        "direction": "INBOUND",
                        "eotContent": %s,
                        "processingDate": "2023-12-01"
                    }
                    """.formatted(testTenantId, testSubServiceId, eotData)))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)));

        // Step 3: Validate EOT against received files
        mockMvc.perform(post("/api/eot-validation/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantId": %d,
                        "subServiceId": %d,
                        "direction": "INBOUND",
                        "processingDate": "2023-12-01",
                        "actualFileCount": 5
                    }
                    """.formatted(testTenantId, testSubServiceId)))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.valid", is(true)))
                .andExpected(jsonPath("$.expectedCount", is(5)))
                .andExpected(jsonPath("$.actualCount", is(5)));
    }

    @Test
    @Order(6)
    @DisplayName("E2E-6: Monitoring and Alerting Workflow")
    void testMonitoringAndAlertingWorkflow() throws Exception {
        // Step 1: Check system health
        mockMvc.perform(get("/actuator/health"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status", is("UP")));

        // Step 2: Get system metrics
        mockMvc.perform(get("/api/monitoring/metrics"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.systemMetrics", notNullValue()))
                .andExpected(jsonPath("$.applicationMetrics", notNullValue()));

        // Step 3: Test alert creation
        String alertRequest = """
            {
                "tenantId": %d,
                "level": "WARNING",
                "message": "E2E test alert",
                "component": "INTEGRATION_TEST",
                "metadata": {
                    "testType": "end-to-end",
                    "timestamp": "%s"
                }
            }
            """.formatted(testTenantId, java.time.LocalDateTime.now());

        mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequest))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.level", is("WARNING")))
                .andExpected(jsonPath("$.message", is("E2E test alert")));

        // Step 4: Verify alert was processed
        mockMvc.perform(get("/api/alerts")
                .param("tenantId", testTenantId.toString())
                .param("level", "WARNING"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @Order(7)
    @DisplayName("E2E-7: Security and Access Control Workflow")
    void testSecurityAndAccessControlWorkflow() throws Exception {
        // Step 1: Test rate limiting
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/tenants"))
                    .andExpected(status().isOk());
        }

        // Step 2: Test input validation
        String invalidTenantRequest = """
            {
                "name": "<script>alert('xss')</script>",
                "description": "Invalid tenant with XSS attempt",
                "timezone": "Invalid/Timezone"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidTenantRequest))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", notNullValue()));

        // Step 3: Test security headers
        mockMvc.perform(get("/api/tenants"))
                .andExpected(header().string("X-Content-Type-Options", "nosniff"))
                .andExpected(header().string("X-Frame-Options", "DENY"))
                .andExpected(header().string("X-XSS-Protection", "1; mode=block"));
    }

    @Test
    @Order(8)
    @DisplayName("E2E-8: Cross-Application Integration Workflow")
    void testCrossApplicationIntegrationWorkflow() throws Exception {
        // Step 1: Test web-to-batch integration
        String batchJobRequest = """
            {
                "tenantId": "%d",
                "inputPath": "/data/input/test",
                "outputPath": "/data/output/test",
                "chunkSize": 100,
                "threadCount": 2
            }
            """.formatted(testTenantId);

        // Note: This would require batch application to be running
        // For now, test the API endpoint availability
        mockMvc.perform(post("/api/batch/integration/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJobRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.integration", is("BATCH_API_AVAILABLE")));

        // Step 2: Test backup coordination
        String backupRequest = """
            {
                "type": "MANUAL",
                "includeWeb": true,
                "includeBatch": true,
                "includeFrontend": true,
                "description": "E2E integration test backup"
            }
            """;

        mockMvc.perform(post("/api/backup/cross-application/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)));
    }

    @Test
    @Order(9)
    @DisplayName("E2E-9: Performance and Scalability Workflow")
    void testPerformanceAndScalabilityWorkflow() throws Exception {
        // Step 1: Test load balancing status
        mockMvc.perform(get("/api/scalability/load-balancing/stats")
                .param("serviceName", "file-transfer-web"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.serviceName", is("file-transfer-web")))
                .andExpected(jsonPath("$.healthyInstances", greaterThanOrEqualTo(0)));

        // Step 2: Test performance metrics
        mockMvc.perform(get("/api/scalability/performance/metrics"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.cpuUsage", notNullValue()))
                .andExpected(jsonPath("$.memoryUsage", notNullValue()))
                .andExpected(jsonPath("$.responseTime", notNullValue()));

        // Step 3: Test async processing
        String asyncRequest = """
            {
                "tenantId": "%d",
                "files": [
                    {"name": "test1.json", "content": "test content 1"},
                    {"name": "test2.json", "content": "test content 2"}
                ],
                "parallel": true
            }
            """.formatted(testTenantId);

        mockMvc.perform(post("/api/scalability/async/process-files-parallel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asyncRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.processedFiles", greaterThan(0)));
    }

    @Test
    @Order(10)
    @DisplayName("E2E-10: Complete Data Lifecycle Workflow")
    void testCompleteDataLifecycleWorkflow() throws Exception {
        // Step 1: File upload and validation
        String uploadRequest = """
            {
                "tenantId": %d,
                "serviceId": %d,
                "subServiceId": %d,
                "direction": "INBOUND",
                "fileType": "JSON",
                "fileName": "e2e-test-file.json",
                "content": "{\\"name\\": \\"E2E Test\\", \\"value\\": 123}"
            }
            """.formatted(testTenantId, testServiceId, testSubServiceId);

        MvcResult uploadResult = mockMvc.perform(post("/api/files/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(uploadRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andReturn();

        String fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("fileId").asText();

        // Step 2: Process file through validation pipeline
        mockMvc.perform(post("/api/files/" + fileId + "/process")
                .param("validateSchema", "true")
                .param("checkNaming", "true"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.processed", is(true)));

        // Step 3: Check processing results
        mockMvc.perform(get("/api/files/" + fileId + "/status"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status", oneOf("PROCESSED", "VALIDATED")))
                .andExpected(jsonPath("$.validationResults", notNullValue()));

        // Step 4: Archive processed file
        mockMvc.perform(post("/api/files/" + fileId + "/archive"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.archived", is(true)));
    }

    @Test
    @Order(11)
    @DisplayName("E2E-11: System Recovery Workflow")
    void testSystemRecoveryWorkflow() throws Exception {
        // Step 1: Create system snapshot before testing recovery
        String snapshotRequest = """
            {
                "type": "RESTORE_POINT",
                "includeDatabase": true,
                "includeFiles": true,
                "includeApplicationState": true,
                "description": "E2E recovery test snapshot"
            }
            """;

        MvcResult snapshotResult = mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(snapshotRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andReturn();

        String snapshotId = objectMapper.readTree(snapshotResult.getResponse().getContentAsString())
                .get("backupId").asText();

        // Step 2: Test disaster recovery plan execution (simulation)
        String drTestRequest = """
            {
                "type": "SIMULATION",
                "reason": "E2E integration test",
                "triggeredBy": "integration-test",
                "triggeredAt": "%s"
            }
            """.formatted(java.time.LocalDateTime.now());

        // Note: This would test DR plan if one exists
        mockMvc.perform(get("/api/backup/disaster-recovery/status"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status.enabled", isA(Boolean.class)));
    }

    @Test
    @Order(12)
    @DisplayName("E2E-12: Complete System Health Verification")
    void testCompleteSystemHealthVerification() throws Exception {
        // Step 1: Verify all core services are healthy
        mockMvc.perform(get("/actuator/health"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status", is("UP")))
                .andExpected(jsonPath("$.components", notNullValue()));

        // Step 2: Verify database connectivity
        mockMvc.perform(get("/actuator/health/db"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status", is("UP")));

        // Step 3: Verify all created resources still exist and are accessible
        mockMvc.perform(get("/api/v2/tenants/" + testTenantId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id", is(testTenantId.intValue())));

        mockMvc.perform(get("/api/services/" + testServiceId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id", is(testServiceId.intValue())));

        mockMvc.perform(get("/api/subservices/" + testSubServiceId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id", is(testSubServiceId.intValue())));

        mockMvc.perform(get("/api/schemas/" + testSchemaId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id", is(testSchemaId.intValue())));
    }

    @AfterAll
    void cleanUp() throws Exception {
        // Clean up test data (optional - test database should be isolated)
        if (testTenantId != null) {
            try {
                mockMvc.perform(delete("/api/v2/tenants/" + testTenantId))
                        .andExpected(status().isNoContent());
            } catch (Exception e) {
                // Ignore cleanup errors
                System.out.println("Cleanup warning: " + e.getMessage());
            }
        }
    }
}