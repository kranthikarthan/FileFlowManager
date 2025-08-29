package com.filetransfer.web.service;

import com.filetransfer.web.entity.SsoConfiguration;
import com.filetransfer.web.entity.SsoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class SsoTestingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SsoTestingService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Tests SSO configuration by validating endpoints and authentication flow
     */
    public Map<String, Object> testSsoConfiguration(SsoConfiguration ssoConfig) {
        Map<String, Object> result = new HashMap<>();
        result.put("configurationId", ssoConfig.getId());
        result.put("organizationId", ssoConfig.getOrganizationId());
        result.put("provider", ssoConfig.getProvider().toString());
        
        try {
            // Step 1: Validate configuration completeness
            Map<String, Object> configValidation = validateConfiguration(ssoConfig);
            result.put("configValidation", configValidation);
            
            if (!(Boolean) configValidation.get("valid")) {
                result.put("success", false);
                result.put("message", "Configuration validation failed");
                return result;
            }
            
            // Step 2: Test endpoint connectivity
            Map<String, Object> connectivityTest = testEndpointConnectivity(ssoConfig);
            result.put("connectivityTest", connectivityTest);
            
            // Step 3: Test OIDC discovery (if applicable)
            Map<String, Object> discoveryTest = testOidcDiscovery(ssoConfig);
            result.put("discoveryTest", discoveryTest);
            
            // Step 4: Test authorization endpoint
            Map<String, Object> authTest = testAuthorizationEndpoint(ssoConfig);
            result.put("authorizationTest", authTest);
            
            // Overall success determination
            boolean overallSuccess = (Boolean) configValidation.get("valid") &&
                                   (Boolean) connectivityTest.get("success") &&
                                   (Boolean) authTest.get("success");
            
            result.put("success", overallSuccess);
            result.put("message", overallSuccess ? "SSO configuration test successful" : 
                      "SSO configuration test completed with some issues");
            
        } catch (Exception e) {
            logger.error("Error testing SSO configuration for organization {}: {}", 
                        ssoConfig.getOrganizationId(), e.getMessage(), e);
            result.put("success", false);
            result.put("message", "SSO test failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    private Map<String, Object> validateConfiguration(SsoConfiguration config) {
        Map<String, Object> validation = new HashMap<>();
        validation.put("valid", true);
        Map<String, String> issues = new HashMap<>();
        
        // Check required fields
        if (config.getClientId() == null || config.getClientId().trim().isEmpty()) {
            issues.put("clientId", "Client ID is required");
            validation.put("valid", false);
        }
        
        if (config.getClientSecret() == null || config.getClientSecret().trim().isEmpty()) {
            issues.put("clientSecret", "Client Secret is required");
            validation.put("valid", false);
        }
        
        // Provider-specific validation
        switch (config.getProvider()) {
            case AZURE_AD:
                validateAzureAdConfig(config, issues, validation);
                break;
            case GOOGLE:
                validateGoogleConfig(config, issues, validation);
                break;
            case OKTA:
                validateOktaConfig(config, issues, validation);
                break;
            case KEYCLOAK:
                validateKeycloakConfig(config, issues, validation);
                break;
            case CUSTOM_OIDC:
                validateCustomOidcConfig(config, issues, validation);
                break;
            case SAML2:
                validateSaml2Config(config, issues, validation);
                break;
        }
        
        validation.put("issues", issues);
        return validation;
    }
    
    private void validateAzureAdConfig(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        if (config.getIssuerUri() == null || !config.getIssuerUri().contains("login.microsoftonline.com")) {
            issues.put("issuerUri", "Azure AD issuer URI should contain login.microsoftonline.com");
            validation.put("valid", false);
        }
    }
    
    private void validateGoogleConfig(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        if (config.getIssuerUri() == null || !config.getIssuerUri().contains("accounts.google.com")) {
            issues.put("issuerUri", "Google issuer URI should be https://accounts.google.com");
            validation.put("valid", false);
        }
    }
    
    private void validateOktaConfig(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        if (config.getIssuerUri() == null || !config.getIssuerUri().contains(".okta.com")) {
            issues.put("issuerUri", "Okta issuer URI should contain your okta domain");
            validation.put("valid", false);
        }
    }
    
    private void validateKeycloakConfig(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        if (config.getIssuerUri() == null || !config.getIssuerUri().contains("/realms/")) {
            issues.put("issuerUri", "Keycloak issuer URI should contain /realms/ path");
            validation.put("valid", false);
        }
    }
    
    private void validateCustomOidcConfig(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        if (config.getAuthorizationUri() == null || config.getAuthorizationUri().trim().isEmpty()) {
            issues.put("authorizationUri", "Authorization URI is required for custom OIDC");
            validation.put("valid", false);
        }
        if (config.getTokenUri() == null || config.getTokenUri().trim().isEmpty()) {
            issues.put("tokenUri", "Token URI is required for custom OIDC");
            validation.put("valid", false);
        }
    }
    
    private void validateSaml2Config(SsoConfiguration config, Map<String, String> issues, Map<String, Object> validation) {
        // SAML2 has different requirements - would need metadata URL or certificate
        if (config.getIssuerUri() == null || config.getIssuerUri().trim().isEmpty()) {
            issues.put("issuerUri", "SAML2 metadata URL or issuer URI is required");
            validation.put("valid", false);
        }
    }
    
    private Map<String, Object> testEndpointConnectivity(SsoConfiguration config) {
        Map<String, Object> connectivity = new HashMap<>();
        connectivity.put("success", true);
        Map<String, Object> endpoints = new HashMap<>();
        
        try {
            // Test issuer URI
            if (config.getIssuerUri() != null) {
                boolean issuerReachable = testEndpoint(config.getIssuerUri());
                endpoints.put("issuer", Map.of("url", config.getIssuerUri(), "reachable", issuerReachable));
                if (!issuerReachable) {
                    connectivity.put("success", false);
                }
            }
            
            // Test authorization URI
            if (config.getAuthorizationUri() != null) {
                boolean authReachable = testEndpoint(config.getAuthorizationUri());
                endpoints.put("authorization", Map.of("url", config.getAuthorizationUri(), "reachable", authReachable));
            }
            
            // Test token URI
            if (config.getTokenUri() != null) {
                boolean tokenReachable = testEndpoint(config.getTokenUri());
                endpoints.put("token", Map.of("url", config.getTokenUri(), "reachable", tokenReachable));
            }
            
            connectivity.put("endpoints", endpoints);
            
        } catch (Exception e) {
            logger.warn("Error testing endpoint connectivity: {}", e.getMessage());
            connectivity.put("success", false);
            connectivity.put("error", e.getMessage());
        }
        
        return connectivity;
    }
    
    private boolean testEndpoint(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FileTransfer-SSO-Test/1.0");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            return response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is4xxClientError(); // 4xx means endpoint exists but may require auth
                   
        } catch (Exception e) {
            logger.debug("Endpoint {} not reachable: {}", url, e.getMessage());
            return false;
        }
    }
    
    private Map<String, Object> testOidcDiscovery(SsoConfiguration config) {
        Map<String, Object> discovery = new HashMap<>();
        discovery.put("supported", false);
        discovery.put("attempted", false);
        
        if (config.getProvider() == SsoProvider.SAML2) {
            discovery.put("message", "OIDC discovery not applicable for SAML2");
            return discovery;
        }
        
        try {
            String discoveryUrl = getOidcDiscoveryUrl(config);
            if (discoveryUrl != null) {
                discovery.put("attempted", true);
                discovery.put("discoveryUrl", discoveryUrl);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");
                HttpEntity<?> entity = new HttpEntity<>(headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    discoveryUrl, HttpMethod.GET, entity, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> discoveryDoc = response.getBody();
                    discovery.put("supported", true);
                    discovery.put("authorizationEndpoint", discoveryDoc.get("authorization_endpoint"));
                    discovery.put("tokenEndpoint", discoveryDoc.get("token_endpoint"));
                    discovery.put("userInfoEndpoint", discoveryDoc.get("userinfo_endpoint"));
                    discovery.put("issuer", discoveryDoc.get("issuer"));
                }
            }
            
        } catch (Exception e) {
            logger.debug("OIDC discovery failed for {}: {}", config.getOrganizationId(), e.getMessage());
            discovery.put("error", e.getMessage());
        }
        
        return discovery;
    }
    
    private String getOidcDiscoveryUrl(SsoConfiguration config) {
        if (config.getIssuerUri() == null) {
            return null;
        }
        
        String issuerUri = config.getIssuerUri();
        if (!issuerUri.endsWith("/")) {
            issuerUri += "/";
        }
        
        return issuerUri + ".well-known/openid-configuration";
    }
    
    private Map<String, Object> testAuthorizationEndpoint(SsoConfiguration config) {
        Map<String, Object> authTest = new HashMap<>();
        authTest.put("success", false);
        
        try {
            String authUrl = config.getAuthorizationUri();
            if (authUrl == null && config.getIssuerUri() != null) {
                // Try to construct from issuer URI for common providers
                authUrl = constructAuthorizationUrl(config);
            }
            
            if (authUrl != null) {
                authTest.put("authorizationUrl", authUrl);
                
                // Build a test authorization URL with proper parameters
                String testUrl = UriComponentsBuilder.fromUriString(authUrl)
                    .queryParam("client_id", config.getClientId())
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid profile email")
                    .queryParam("redirect_uri", "http://localhost:8080/test-callback")
                    .queryParam("state", "test-state-123")
                    .build()
                    .toUriString();
                
                authTest.put("testUrl", testUrl);
                
                // Test if the authorization endpoint responds appropriately
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "FileTransfer-SSO-Test/1.0");
                HttpEntity<?> entity = new HttpEntity<>(headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    testUrl, HttpMethod.GET, entity, String.class);
                
                // For OAuth2, we expect either:
                // - 302 redirect to login page (success)
                // - 400 with error message (endpoint working but invalid params)
                // - 200 with login form (success)
                boolean isWorking = response.getStatusCode().is2xxSuccessful() ||
                                   response.getStatusCode().is3xxRedirection() ||
                                   (response.getStatusCode().is4xxClientError() && 
                                    response.getBody() != null && 
                                    response.getBody().contains("error"));
                
                authTest.put("success", isWorking);
                authTest.put("statusCode", response.getStatusCode().value());
                authTest.put("message", isWorking ? "Authorization endpoint is responding correctly" :
                           "Authorization endpoint test failed");
                
            } else {
                authTest.put("success", false);
                authTest.put("message", "No authorization URL available to test");
            }
            
        } catch (Exception e) {
            logger.debug("Authorization endpoint test failed for {}: {}", config.getOrganizationId(), e.getMessage());
            authTest.put("success", false);
            authTest.put("error", e.getMessage());
            authTest.put("message", "Authorization endpoint test failed: " + e.getMessage());
        }
        
        return authTest;
    }
    
    private String constructAuthorizationUrl(SsoConfiguration config) {
        String issuerUri = config.getIssuerUri();
        
        switch (config.getProvider()) {
            case AZURE_AD:
                return issuerUri + "/oauth2/v2.0/authorize";
            case GOOGLE:
                return "https://accounts.google.com/o/oauth2/v2/auth";
            case OKTA:
                return issuerUri + "/oauth2/v1/authorize";
            case KEYCLOAK:
                return issuerUri + "/protocol/openid-connect/auth";
            default:
                return null;
        }
    }
}