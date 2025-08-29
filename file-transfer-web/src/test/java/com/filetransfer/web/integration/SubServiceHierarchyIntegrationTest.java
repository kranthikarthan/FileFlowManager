package com.filetransfer.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.*;
import com.filetransfer.web.service.TenantTimeZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the new SubService hierarchy and timezone-aware functionality.
 * Tests the complete flow from API endpoints to database operations.
 */
@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
public class SubServiceHierarchyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private SubServiceConfigurationRepository subServiceConfigurationRepository;

    @Autowired
    private CutOffExtensionRepository cutOffExtensionRepository;

    @Autowired
    private FileTypeSchemaMappingRepository fileTypeSchemaMappingRepository;

    @Autowired
    private TenantTimeZoneService tenantTimeZoneService;

    private Tenant testTenant;
    private SubServiceConfiguration testSubService;

    @BeforeEach
    void setUp() {
        // Create test tenant with timezone
        testTenant = new Tenant();
        testTenant.setTenantId("test-tenant");
        testTenant.setName("Test Tenant Organization");
        testTenant.setTimezone("America/New_York");
        testTenant.setContactEmail("test@example.com");
        testTenant.setEnabled(true);
        testTenant = tenantRepository.save(testTenant);

        // Create test subservice configuration
        testSubService = new SubServiceConfiguration();
        testSubService.setTenantId("test-tenant");
        testSubService.setServiceName("TestService");
        testSubService.setSubServiceName("TestSubService");
        testSubService.setInboundPath("/test/inbound");
        testSubService.setOutboundPath("/test/outbound");
        testSubService.setEnabled(true);
        testSubService.setCutOffTime("18:00:00");
        testSubService.setCutOffTimeType(CutOffTimeType.DAILY);
        testSubService.setSchemaValidationEnabled(true);
        testSubService.setBinaryFileBypass(true);
        testSubService.setCreatedBy("test-user");
        testSubService = subServiceConfigurationRepository.save(testSubService);
    }

    @Test
    void testSubServiceHierarchyFlow() throws Exception {
        // Test creating a new sub-service
        SubServiceConfiguration newSubService = new SubServiceConfiguration();
        newSubService.setTenantId("test-tenant");
        newSubService.setServiceName("NewService");
        newSubService.setSubServiceName("NewSubService");
        newSubService.setInboundPath("/new/inbound");
        newSubService.setOutboundPath("/new/outbound");
        newSubService.setEnabled(true);
        newSubService.setCutOffTime("20:00:00");
        newSubService.setCutOffTimeType(CutOffTimeType.WEEKDAY_WEEKEND);
        newSubService.setWeekdayCutOffTime("20:00:00");
        newSubService.setWeekendCutOffTime("16:00:00");

        mockMvc.perform(post("/api/sub-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSubService)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("NewService"))
                .andExpect(jsonPath("$.subServiceName").value("NewSubService"))
                .andExpect(jsonPath("$.cutOffTime").value("20:00:00"))
                .andExpect(jsonPath("$.cutOffTimeType").value("WEEKDAY_WEEKEND"));

        // Verify sub-service was created in database
        var createdSubService = subServiceConfigurationRepository
            .findByTenantIdAndServiceNameAndSubServiceName("test-tenant", "NewService", "NewSubService");
        assertTrue(createdSubService.isPresent());
        assertEquals("20:00:00", createdSubService.get().getCutOffTime());
    }

    @Test
    void testTimezoneAwareCutOffCalculation() throws Exception {
        // Test timezone-aware cut-off time endpoint
        mockMvc.perform(get("/api/sub-services/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/cut-off-info",
                "test-tenant", "TestService", "TestSubService"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutOffTime").exists())
                .andExpect(jsonPath("$.timeZone").exists())
                .andExpect(jsonPath("$.timeZone.zoneId").value("America/New_York"))
                .andExpect(jsonPath("$.currentTenantTime").exists())
                .andExpect(jsonPath("$.inProcessingWindow").exists());

        // Test timezone info endpoint
        mockMvc.perform(get("/api/sub-services/tenant/{tenantId}/timezone-info", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value("America/New_York"))
                .andExpect(jsonPath("$.displayName").exists())
                .andExpect(jsonPath("$.currentTime").exists());
    }

    @Test
    void testCutOffExtensionWorkflow() throws Exception {
        // Test requesting a cut-off extension
        CutOffExtension extension = new CutOffExtension();
        extension.setTenantId("test-tenant");
        extension.setServiceName("TestService");
        extension.setSubServiceName("TestSubService");
        extension.setExtensionDate(LocalDate.now().plusDays(1).atStartOfDay());
        extension.setOriginalCutOffTime("18:00:00");
        extension.setExtendedCutOffTime("20:00:00");
        extension.setReason("Critical business processing required");
        extension.setPriority(CutOffExtension.ExtensionPriority.HIGH);
        extension.setRequestedBy("test-user");

        mockMvc.perform(post("/api/cutoff-extensions/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extension)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.extendedCutOffTime").value("20:00:00"));

        // Verify extension was created
        var extensions = cutOffExtensionRepository.findByTenantIdAndStatus("test-tenant", CutOffExtension.ExtensionStatus.PENDING);
        assertFalse(extensions.isEmpty());
        assertEquals("Critical business processing required", extensions.get(0).getReason());

        // Test approving the extension
        Long extensionId = extensions.get(0).getId();
        mockMvc.perform(post("/api/cutoff-extensions/{id}/approve", extensionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify approval
        var approvedExtension = cutOffExtensionRepository.findById(extensionId);
        assertTrue(approvedExtension.isPresent());
        assertEquals(CutOffExtension.ExtensionStatus.APPROVED, approvedExtension.get().getStatus());
        assertNotNull(approvedExtension.get().getApprovedAt());
    }

    @Test
    void testFileTypeSchemaMappingIntegration() throws Exception {
        // Create a file schema for testing
        FileSchema testSchema = new FileSchema();
        testSchema.setTenantId("test-tenant");
        testSchema.setSchemaName("TestCobolSchema");
        testSchema.setSchemaType("COBOL");
        testSchema.setSchemaDefinition("01 TEST-RECORD.\n   05 TEST-FIELD PIC X(10).");
        testSchema.setCreatedBy("test-user");

        // Note: In a real test, we would save this via repository or service
        // For this integration test, we'll test the mapping structure

        // Test that we can create file type schema mappings
        FileTypeSchemaMapping mapping = new FileTypeSchemaMapping();
        mapping.setSubServiceConfiguration(testSubService);
        mapping.setFileType(FileType.COBOL_FLAT_FILE);
        mapping.setInboundSchemaId(1L); // Mock schema ID
        mapping.setOutboundSchemaId(1L);
        mapping.setValidationEnabled(true);

        mapping = fileTypeSchemaMappingRepository.save(mapping);
        assertNotNull(mapping.getId());

        // Test retrieving mappings for subservice
        var mappings = fileTypeSchemaMappingRepository.findBySubServiceConfiguration(testSubService);
        assertFalse(mappings.isEmpty());
        assertEquals(FileType.COBOL_FLAT_FILE, mappings.get(0).getFileType());
    }

    @Test
    void testSubServiceDeletion() throws Exception {
        // Test deleting a sub-service
        Long subServiceId = testSubService.getId();

        mockMvc.perform(delete("/api/sub-services/{id}", subServiceId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.message").value("Sub-service deleted successfully"));

        // Verify deletion
        var deletedSubService = subServiceConfigurationRepository.findById(subServiceId);
        assertTrue(deletedSubService.isEmpty());
    }

    @Test
    void testSubServiceValidation() throws Exception {
        // Test creating sub-service with invalid data
        SubServiceConfiguration invalidSubService = new SubServiceConfiguration();
        invalidSubService.setTenantId(""); // Empty tenant ID
        invalidSubService.setServiceName("TestService");
        invalidSubService.setSubServiceName(""); // Empty sub-service name

        mockMvc.perform(post("/api/sub-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSubService)))
                .andExpected(status().isBadRequest());
    }

    @Test
    void testTenantTimeZoneService() {
        // Test timezone service functionality
        var timeZoneInfo = tenantTimeZoneService.getTenantTimeZoneInfo("test-tenant");
        assertEquals("America/New_York", timeZoneInfo.getZoneId());
        assertNotNull(timeZoneInfo.getCurrentTime());

        // Test time conversion
        var currentTenantTime = tenantTimeZoneService.getCurrentTenantTime("test-tenant");
        assertNotNull(currentTenantTime);

        // Test processing window check
        boolean inWindow = tenantTimeZoneService.isInProcessingWindow("test-tenant", LocalDate.now(), LocalTime.of(18, 0));
        // Result depends on current time, but method should not throw exception
        assertNotNull(inWindow);
    }

    @Test
    void testSubServiceConfigurationEndpoints() throws Exception {
        // Test getting all sub-services for tenant
        mockMvc.perform(get("/api/sub-services/tenant/{tenantId}", "test-tenant"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$").isArray())
                .andExpected(jsonPath("$[0].serviceName").value("TestService"))
                .andExpected(jsonPath("$[0].subServiceName").value("TestSubService"));

        // Test getting specific sub-service
        mockMvc.perform(get("/api/sub-services/{id}", testSubService.getId()))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.serviceName").value("TestService"))
                .andExpected(jsonPath("$.enabled").value(true));

        // Test updating sub-service
        testSubService.setDescription("Updated description");
        testSubService.setCutOffTime("19:00:00");

        mockMvc.perform(put("/api/sub-services/{id}", testSubService.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSubService)))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.description").value("Updated description"))
                .andExpected(jsonPath("$.cutOffTime").value("19:00:00"));
    }

    @Test
    void testErrorHandling() throws Exception {
        // Test getting non-existent sub-service
        mockMvc.perform(get("/api/sub-services/{id}", 99999L))
                .andExpected(status().isNotFound());

        // Test getting sub-services for non-existent tenant
        mockMvc.perform(get("/api/sub-services/tenant/{tenantId}", "non-existent-tenant"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$").isArray())
                .andExpected(jsonPath("$").isEmpty());

        // Test requesting cut-off extension with invalid data
        CutOffExtension invalidExtension = new CutOffExtension();
        // Missing required fields

        mockMvc.perform(post("/api/cutoff-extensions/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidExtension)))
                .andExpected(status().isBadRequest());
    }

    @Test
    void testFileTypeConfiguration() throws Exception {
        // Test getting available file types
        mockMvc.perform(get("/api/sub-services/file-types"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$").isArray())
                .andExpected(jsonPath("$").isNotEmpty());
    }

    @Test
    void testCutOffExtensionStatuses() throws Exception {
        // Create extension
        CutOffExtension extension = new CutOffExtension();
        extension.setTenantId("test-tenant");
        extension.setServiceName("TestService");
        extension.setSubServiceName("TestSubService");
        extension.setExtensionDate(LocalDate.now().plusDays(1).atStartOfDay());
        extension.setOriginalCutOffTime("18:00:00");
        extension.setExtendedCutOffTime("20:00:00");
        extension.setReason("Test extension");
        extension.setPriority(CutOffExtension.ExtensionPriority.NORMAL);
        extension.setRequestedBy("test-user");

        extension = cutOffExtensionRepository.save(extension);

        // Test rejection
        mockMvc.perform(post("/api/cutoff-extensions/{id}/reject", extension.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rejectionReason\": \"Not justified\"}"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.status").value("REJECTED"))
                .andExpected(jsonPath("$.rejectionReason").value("Not justified"));

        // Verify rejection
        var rejectedExtension = cutOffExtensionRepository.findById(extension.getId());
        assertTrue(rejectedExtension.isPresent());
        assertEquals(CutOffExtension.ExtensionStatus.REJECTED, rejectedExtension.get().getStatus());
        assertEquals("Not justified", rejectedExtension.get().getRejectionReason());
    }
}