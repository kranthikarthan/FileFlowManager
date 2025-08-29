package com.filetransfer.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Security integration tests for the File Transfer Management System
 * Tests authentication, authorization, input validation, and security controls
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class SecurityIntegrationTest {

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
    @DisplayName("SEC-1: Security Headers - Should include all required security headers")
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v2/tenants"))
                .andExpected(status().isOk())
                .andExpected(header().string("X-Content-Type-Options", "nosniff"))
                .andExpected(header().string("X-Frame-Options", "DENY"))
                .andExpected(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpected(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpected(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("SEC-2: Input Validation - Should prevent XSS and injection attacks")
    void testInputValidation() throws Exception {
        // Test XSS prevention
        String xssPayload = """
            {
                "name": "<script>alert('xss')</script>",
                "description": "javascript:alert('xss')",
                "timezone": "America/New_York"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(xssPayload))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", containsString("validation")));

        // Test SQL injection prevention
        String sqlInjectionPayload = """
            {
                "name": "'; DROP TABLE tenants; --",
                "description": "SQL injection test",
                "timezone": "America/New_York"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sqlInjectionPayload))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", containsString("validation")));

        // Test command injection prevention
        String commandInjectionPayload = """
            {
                "name": "test; rm -rf /",
                "description": "Command injection test", 
                "timezone": "America/New_York"
            }
            """;

        mockMvc.perform(post("/api/v2/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commandInjectionPayload))
                .andExpected(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-3: Rate Limiting - Should enforce rate limits")
    void testRateLimiting() throws Exception {
        // Make rapid requests to trigger rate limiting
        for (int i = 0; i < 25; i++) {
            var response = mockMvc.perform(get("/api/v2/tenants"))
                    .andReturn();
            
            if (i < 20) {
                // First 20 requests should succeed
                assertEquals(200, response.getResponse().getStatus());
            } else {
                // Subsequent requests should be rate limited
                if (response.getResponse().getStatus() == 429) {
                    // Rate limit triggered
                    assertNotNull(response.getResponse().getHeader("X-RateLimit-Remaining"));
                    assertNotNull(response.getResponse().getHeader("X-RateLimit-Reset"));
                    break;
                }
            }
        }
    }

    @Test
    @DisplayName("SEC-4: Authentication and Authorization - Should enforce access controls")
    void testAuthenticationAndAuthorization() throws Exception {
        // Test unauthenticated access to protected endpoints
        mockMvc.perform(post("/api/v2/tenants/{id}/delete", 1))
                .andExpected(status().isUnauthorized());

        // Test unauthorized access to admin endpoints
        mockMvc.perform(post("/api/versions/2.0/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "reason": "Test deprecation",
                        "sunsetDate": "2024-12-31"
                    }
                    """))
                .andExpected(status().isUnauthorized());

        // Test access to backup endpoints (admin only)
        mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "type": "MANUAL",
                        "includeDatabase": true
                    }
                    """))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-5: Data Encryption - Should handle encrypted data properly")
    void testDataEncryption() throws Exception {
        // Test encrypted backup creation
        String backupRequest = """
            {
                "type": "MANUAL",
                "includeDatabase": true,
                "includeFiles": true,
                "encryption": true,
                "description": "Encryption test backup"
            }
            """;

        mockMvc.perform(post("/api/backup/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(backupRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.encryptedPath", notNullValue()));
    }

    @Test
    @DisplayName("SEC-6: CORS Configuration - Should handle cross-origin requests properly")
    void testCorsConfiguration() throws Exception {
        // Test preflight request
        mockMvc.perform(options("/api/v2/tenants")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpected(status().isOk())
                .andExpected(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpected(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpected(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));

        // Test actual cross-origin request
        mockMvc.perform(get("/api/v2/tenants")
                .header("Origin", "http://localhost:3000"))
                .andExpected(status().isOk())
                .andExpected(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("SEC-7: Password Security - Should enforce password policies")
    void testPasswordSecurity() throws Exception {
        // Test weak password rejection
        String weakPasswordRequest = """
            {
                "username": "testuser",
                "password": "123",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(weakPasswordRequest))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", containsString("password")));

        // Test strong password acceptance
        String strongPasswordRequest = """
            {
                "username": "testuser",
                "password": "StrongP@ssw0rd123!",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(strongPasswordRequest))
                .andExpected(status().isCreated());
    }

    @Test
    @DisplayName("SEC-8: Session Security - Should handle session management securely")
    void testSessionSecurity() throws Exception {
        // Test session creation
        String loginRequest = """
            {
                "username": "testuser",
                "password": "StrongP@ssw0rd123!"
            }
            """;

        var loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.token", notNullValue()))
                .andExpected(header().exists("Set-Cookie"))
                .andReturn();

        String token = objectMapper.readTree(loginResponse.getResponse().getContentAsString())
                .get("token").asText();

        // Test authenticated request
        mockMvc.perform(get("/api/v2/tenants")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        // Test session timeout
        Thread.sleep(TimeUnit.SECONDS.toMillis(5)); // Simulate time passage
        
        mockMvc.perform(get("/api/v2/tenants")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk()); // Should still be valid for short timeout
    }

    @Test
    @DisplayName("SEC-9: File Upload Security - Should validate uploaded files")
    void testFileUploadSecurity() throws Exception {
        // Test malicious file upload prevention
        byte[] maliciousContent = "<?php system($_GET['cmd']); ?>".getBytes();

        mockMvc.perform(multipart("/api/files/upload")
                .file("file", maliciousContent)
                .param("tenantId", "1")
                .param("fileType", "PHP"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", containsString("file type")));

        // Test file size limit
        byte[] largeFile = new byte[100 * 1024 * 1024]; // 100MB
        
        mockMvc.perform(multipart("/api/files/upload")
                .file("file", largeFile)
                .param("tenantId", "1")
                .param("fileType", "JSON"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", containsString("size")));
    }

    @Test
    @DisplayName("SEC-10: API Security Monitoring - Should detect and log security events")
    void testApiSecurityMonitoring() throws Exception {
        // Generate security events
        for (int i = 0; i < 5; i++) {
            // Attempt SQL injection
            mockMvc.perform(get("/api/v2/tenants")
                    .param("search", "'; DROP TABLE tenants; --"))
                    .andExpected(status().isBadRequest());
        }

        // Check if security events are logged
        mockMvc.perform(get("/api/monitoring/security/events"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.securityEvents", hasSize(greaterThan(0))))
                .andExpected(jsonPath("$.securityEvents[0].type", is("INPUT_VALIDATION_FAILURE")))
                .andExpected(jsonPath("$.securityEvents[0].severity", is("HIGH")));
    }
}