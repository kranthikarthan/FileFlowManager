package com.filetransfer.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for API versioning functionality
 * Tests version resolution, compatibility, and deprecation handling
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ApiVersioningIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("1. URL Path Versioning - Should route to correct version")
    void testUrlPathVersioning() throws Exception {
        // Test v1 endpoint (deprecated)
        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "1.0"))
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().exists("Sunset"));

        // Test v2 endpoint (current)
        mockMvc.perform(get("/api/v2/tenants"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "2.0"))
                .andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    @DisplayName("2. Header Versioning - Should resolve version from headers")
    void testHeaderVersioning() throws Exception {
        // Test with version header
        mockMvc.perform(get("/api/tenants")
                .header("X-API-Version", "2.0"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "2.0"));

        // Test with deprecated version header
        mockMvc.perform(get("/api/tenants")
                .header("X-API-Version", "1.2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "1.2"))
                .andExpect(header().string("Deprecation", "true"));
    }

    @Test
    @DisplayName("3. Query Parameter Versioning - Should resolve version from query params")
    void testQueryParameterVersioning() throws Exception {
        // Test with version query parameter
        mockMvc.perform(get("/api/tenants?version=2.0"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "2.0"));

        // Test with deprecated version
        mockMvc.perform(get("/api/tenants?version=1.1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "1.1"))
                .andExpect(header().string("Deprecation", "true"));
    }

    @Test
    @DisplayName("4. Media Type Versioning - Should resolve version from Accept header")
    void testMediaTypeVersioning() throws Exception {
        // Test with media type version
        mockMvc.perform(get("/api/tenants")
                .header("Accept", "application/vnd.filetransfer.v2+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "2.0"));

        // Test with deprecated media type
        mockMvc.perform(get("/api/tenants")
                .header("Accept", "application/vnd.filetransfer.v1+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", "1.0"))
                .andExpect(header().string("Deprecation", "true"));
    }

    @Test
    @DisplayName("5. Version Compatibility - Should handle compatible versions")
    void testVersionCompatibility() throws Exception {
        // Test compatible version resolution
        mockMvc.perform(get("/api/v2/tenants")
                .header("X-API-Version", "1.2")) // Compatible with 2.0
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version-Used", anyOf(is("1.2"), is("2.0"))));
    }

    @Test
    @DisplayName("6. Invalid Version Handling - Should return proper error")
    void testInvalidVersionHandling() throws Exception {
        // Test unsupported version
        mockMvc.perform(get("/api/tenants")
                .header("X-API-Version", "3.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Unsupported API version")))
                .andExpect(jsonPath("$.requestedVersion", is("3.0")))
                .andExpect(jsonPath("$.supportedVersions", isA(Object.class)));

        // Test invalid version format
        mockMvc.perform(get("/api/tenants")
                .header("X-API-Version", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid API version")));
    }

    @Test
    @DisplayName("7. Version Information API - Should return version details")
    void testVersionInformationApi() throws Exception {
        // Get all versions
        mockMvc.perform(get("/api/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVersions", greaterThan(0)))
                .andExpect(jsonPath("$.supportedVersions", isA(Object.class)))
                .andExpect(jsonPath("$.deprecatedVersions", isA(Object.class)));

        // Get specific version info
        mockMvc.perform(get("/api/versions/2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionInfo.version", is("2.0")))
                .andExpect(jsonPath("$.supported", is(true)))
                .andExpect(jsonPath("$.deprecated", is(false)));

        // Get deprecated version info
        mockMvc.perform(get("/api/versions/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionInfo.version", is("1.0")))
                .andExpect(jsonPath("$.deprecated", is(true)))
                .andExpect(jsonPath("$.deprecationInfo", isA(Object.class)));
    }

    @Test
    @DisplayName("8. Migration Planning - Should generate migration plans")
    void testMigrationPlanning() throws Exception {
        // Get migration plan
        mockMvc.perform(get("/api/versions/migration-plan")
                .param("fromVersion", "1.2")
                .param("toVersion", "2.0"))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.migrationPlan.feasible", is(true)))
                .andExpect(jsonPath("$.migrationPlan.fromVersion", is("1.2")))
                .andExpect(jsonPath("$.migrationPlan.toVersion", is("2.0")))
                .andExpected(jsonPath("$.migrationPlan.steps", isA(Object.class)));

        // Get breaking changes
        mockMvc.perform(get("/api/versions/breaking-changes")
                .param("fromVersion", "1.2")
                .param("toVersion", "2.0"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.hasBreakingChanges", is(true)))
                .andExpected(jsonPath("$.breakingChanges", isA(Object.class)));
    }

    @Test
    @DisplayName("9. V1 vs V2 Feature Differences - Should show correct capabilities")
    void testVersionFeatureDifferences() throws Exception {
        // Test v1 tenant creation (timezone optional)
        String tenantV1 = """
            {
                "name": "Test Tenant V1",
                "description": "Test tenant for v1 API"
            }
            """;

        mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantV1))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.timezone", is("UTC"))); // Auto-set in v1

        // Test v2 tenant creation (timezone required)
        String tenantV2 = """
            {
                "name": "Test Tenant V2",
                "description": "Test tenant for v2 API",
                "timezone": "America/New_York"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantV2))
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.timezone", is("America/New_York")));

        // Test v2 with missing timezone (should fail)
        String tenantV2Invalid = """
            {
                "name": "Test Tenant V2 Invalid",
                "description": "Test tenant without timezone"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantV2Invalid))
                .andExpected(status().isBadRequest());
    }

    @Test
    @DisplayName("10. Cross-Version Compatibility - Should handle version mismatches")
    void testCrossVersionCompatibility() throws Exception {
        // Create tenant with v1 API
        String tenant = """
            {
                "name": "Cross Version Test",
                "description": "Test cross-version compatibility"
            }
            """;

        var createResponse = mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenant))
                .andExpected(status().isCreated())
                .andReturn();

        String responseBody = createResponse.getResponse().getContentAsString();
        Long tenantId = objectMapper.readTree(responseBody).get("id").asLong();

        // Access same tenant with v2 API
        mockMvc.perform(get("/api/v2/tenants/" + tenantId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id", is(tenantId.intValue())))
                .andExpected(jsonPath("$.timezone", is("UTC"))); // Should have default from v1
    }
}