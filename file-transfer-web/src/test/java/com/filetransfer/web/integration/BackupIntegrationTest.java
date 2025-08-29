package com.filetransfer.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.FileTransferWebApplication;
import com.filetransfer.web.model.backup.BackupModels.*;
import com.filetransfer.web.service.BackupService;
import com.filetransfer.web.service.DisasterRecoveryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for backup and disaster recovery functionality
 * Tests end-to-end backup operations, restore procedures, and DR scenarios
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class BackupIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BackupService backupService;

    @Autowired
    private DisasterRecoveryService disasterRecoveryService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("1. Create Full Backup - Should create complete system backup")
    void testCreateFullBackup() throws Exception {
        String backupRequest = """
            {
                "type": "FULL",
                "includeDatabase": true,
                "includeFiles": true,
                "includeApplicationState": true,
                "compression": true,
                "encryption": true,
                "verification": true,
                "description": "Integration test full backup"
            }
            """;

        mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.backupId", notNullValue()))
                .andExpect(jsonPath("$.type", is("FULL")))
                .andExpect(jsonPath("$.duration", greaterThan(0)))
                .andExpect(jsonPath("$.size", greaterThan(0)))
                .andExpect(jsonPath("$.message", is("Backup created successfully")));
    }

    @Test
    @DisplayName("2. Create Incremental Backup - Should create incremental backup")
    void testCreateIncrementalBackup() throws Exception {
        String backupRequest = """
            {
                "type": "INCREMENTAL",
                "includeDatabase": true,
                "includeFiles": true,
                "compression": true,
                "description": "Integration test incremental backup"
            }
            """;

        mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.type", is("INCREMENTAL")));
    }

    @Test
    @DisplayName("3. List Backups - Should return available backups")
    void testListBackups() throws Exception {
        mockMvc.perform(get("/api/backup/list"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.backups", isA(Object.class)))
                .andExpected(jsonPath("$.totalCount", greaterThanOrEqualTo(0)))
                .andExpected(jsonPath("$.page", is(0)))
                .andExpected(jsonPath("$.size", is(20)));

        // Test with filters
        mockMvc.perform(get("/api/backup/list")
                .param("type", "FULL")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.page", is(0)))
                .andExpected(jsonPath("$.size", is(10)));
    }

    @Test
    @DisplayName("4. Backup Status - Should return backup system status")
    void testBackupStatus() throws Exception {
        mockMvc.perform(get("/api/backup/status"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.totalBackups", greaterThanOrEqualTo(0)))
                .andExpected(jsonPath("$.totalSize", greaterThanOrEqualTo(0)))
                .andExpected(jsonPath("$.backupLocation", notNullValue()))
                .andExpected(jsonPath("$.retentionDays", is(30)));
    }

    @Test
    @DisplayName("5. Disaster Recovery Status - Should return DR system status")
    void testDisasterRecoveryStatus() throws Exception {
        mockMvc.perform(get("/api/backup/disaster-recovery/status"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status.enabled", isA(Boolean.class)))
                .andExpected(jsonPath("$.status.currentActiveRegion", notNullValue()))
                .andExpected(jsonPath("$.status.primaryRegion", notNullValue()))
                .andExpected(jsonPath("$.status.secondaryRegion", notNullValue()));
    }

    @Test
    @DisplayName("6. Backup Health Check - Should return backup system health")
    void testBackupHealthCheck() throws Exception {
        mockMvc.perform(get("/api/backup/health"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status", is("UP")))
                .andExpected(jsonPath("$.backupService", is("OPERATIONAL")))
                .andExpected(jsonPath("$.disasterRecovery", is("OPERATIONAL")))
                .andExpected(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("7. Backup Service Integration - Should integrate with actual service")
    void testBackupServiceIntegration() throws Exception {
        // Create backup request
        BackupRequest request = BackupRequest.builder()
                .type(BackupType.MANUAL)
                .includeDatabase(true)
                .includeFiles(true)
                .compression(false) // Disable for faster testing
                .encryption(false)  // Disable for faster testing
                .description("Integration test backup")
                .build();

        // Execute backup
        CompletableFuture<BackupResult> future = backupService.performBackup(request);
        BackupResult result = future.join();

        // Verify backup result
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Backup should succeed");
        assertNotNull(result.getBackupId(), "Backup ID should be generated");
        assertNotNull(result.getBackupPath(), "Backup path should be set");
        assertTrue(result.getDuration() > 0, "Backup duration should be positive");
    }

    @Test
    @DisplayName("8. Cross-Application Backup - Should coordinate backups across apps")
    void testCrossApplicationBackup() throws Exception {
        String crossAppRequest = """
            {
                "type": "FULL",
                "includeWeb": true,
                "includeBatch": true,
                "includeFrontend": true,
                "synchronizeBackups": true,
                "createSnapshot": true,
                "description": "Cross-application integration test"
            }
            """;

        mockMvc.perform(post("/api/backup/cross-application/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(crossAppRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.coordinationId", notNullValue()))
                .andExpected(jsonPath("$.applications", hasSize(3)));
    }

    @Test
    @DisplayName("9. Backup Error Handling - Should handle backup failures gracefully")
    void testBackupErrorHandling() throws Exception {
        // Test backup with invalid configuration
        String invalidRequest = """
            {
                "type": "INVALID_TYPE",
                "includeDatabase": true
            }
            """;

        mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("10. Backup Performance - Should complete within acceptable time")
    void testBackupPerformance() throws Exception {
        long startTime = System.currentTimeMillis();

        String backupRequest = """
            {
                "type": "MANUAL",
                "includeDatabase": true,
                "includeFiles": false,
                "compression": false,
                "encryption": false,
                "description": "Performance test backup"
            }
            """;

        var response = mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpected(status().isOk())
                .andReturn();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Backup should complete within 30 seconds for test data
        assertTrue(duration < 30000, "Backup should complete within 30 seconds");

        String responseBody = response.getResponse().getContentAsString();
        var result = objectMapper.readTree(responseBody);
        assertTrue(result.get("success").asBoolean(), "Backup should succeed");
    }
}