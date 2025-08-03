package com.filetransfer.web.service;

import com.filetransfer.web.entity.SsoConfiguration;
import com.filetransfer.web.entity.SsoProvider;
import com.filetransfer.web.repository.SsoConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SsoConfigurationService {
    
    @Autowired
    private SsoConfigurationRepository ssoConfigRepository;
    
    public List<SsoConfiguration> getAllConfigurations() {
        return ssoConfigRepository.findAll();
    }
    
    public List<SsoConfiguration> getEnabledConfigurations() {
        return ssoConfigRepository.findAllEnabledConfigurations();
    }
    
    public Optional<SsoConfiguration> getConfigurationById(Long id) {
        return ssoConfigRepository.findById(id);
    }
    
    public Optional<SsoConfiguration> getConfigurationByOrganizationId(String organizationId) {
        return ssoConfigRepository.findByOrganizationId(organizationId);
    }
    
    public List<SsoConfiguration> getConfigurationsByProvider(SsoProvider provider) {
        return ssoConfigRepository.findByProvider(provider);
    }
    
    public SsoConfiguration createConfiguration(SsoConfiguration ssoConfig) {
        validateSsoConfiguration(ssoConfig);
        
        if (ssoConfigRepository.existsByOrganizationId(ssoConfig.getOrganizationId())) {
            throw new RuntimeException("SSO configuration for organization '" + 
                ssoConfig.getOrganizationId() + "' already exists");
        }
        
        return ssoConfigRepository.save(ssoConfig);
    }
    
    public SsoConfiguration updateConfiguration(Long id, SsoConfiguration ssoConfig) {
        SsoConfiguration existingConfig = ssoConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("SSO configuration not found with id: " + id));
        
        // Check if organization ID is being changed and if new ID already exists
        if (!existingConfig.getOrganizationId().equals(ssoConfig.getOrganizationId())) {
            if (ssoConfigRepository.existsByOrganizationId(ssoConfig.getOrganizationId())) {
                throw new RuntimeException("SSO configuration for organization '" + 
                    ssoConfig.getOrganizationId() + "' already exists");
            }
        }
        
        validateSsoConfiguration(ssoConfig);
        
        // Update fields
        existingConfig.setOrganizationId(ssoConfig.getOrganizationId());
        existingConfig.setOrganizationName(ssoConfig.getOrganizationName());
        existingConfig.setProvider(ssoConfig.getProvider());
        existingConfig.setClientId(ssoConfig.getClientId());
        existingConfig.setClientSecret(ssoConfig.getClientSecret());
        existingConfig.setIssuerUri(ssoConfig.getIssuerUri());
        existingConfig.setAuthorizationUri(ssoConfig.getAuthorizationUri());
        existingConfig.setTokenUri(ssoConfig.getTokenUri());
        existingConfig.setUserInfoUri(ssoConfig.getUserInfoUri());
        existingConfig.setJwkSetUri(ssoConfig.getJwkSetUri());
        existingConfig.setRedirectUri(ssoConfig.getRedirectUri());
        existingConfig.setScopes(ssoConfig.getScopes());
        existingConfig.setAttributesMapping(ssoConfig.getAttributesMapping());
        existingConfig.setEnabled(ssoConfig.getEnabled());
        existingConfig.setLogoUrl(ssoConfig.getLogoUrl());
        existingConfig.setDescription(ssoConfig.getDescription());
        existingConfig.setUpdatedBy(ssoConfig.getUpdatedBy());
        
        return ssoConfigRepository.save(existingConfig);
    }
    
    public void deleteConfiguration(Long id) {
        SsoConfiguration config = ssoConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("SSO configuration not found with id: " + id));
        
        ssoConfigRepository.delete(config);
    }
    
    public SsoConfiguration toggleConfigurationStatus(Long id) {
        SsoConfiguration config = ssoConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("SSO configuration not found with id: " + id));
        
        config.setEnabled(!config.getEnabled());
        return ssoConfigRepository.save(config);
    }
    
    private void validateSsoConfiguration(SsoConfiguration ssoConfig) {
        // Validate required fields based on provider
        switch (ssoConfig.getProvider()) {
            case AZURE_AD:
                validateAzureAdConfig(ssoConfig);
                break;
            case GOOGLE:
                validateGoogleConfig(ssoConfig);
                break;
            case OKTA:
                validateOktaConfig(ssoConfig);
                break;
            case KEYCLOAK:
                validateKeycloakConfig(ssoConfig);
                break;
            case CUSTOM_OIDC:
                validateCustomOidcConfig(ssoConfig);
                break;
            case SAML2:
                validateSaml2Config(ssoConfig);
                break;
        }
        
        // Validate redirect URI format
        if (ssoConfig.getRedirectUri() != null && !ssoConfig.getRedirectUri().isEmpty()) {
            if (!ssoConfig.getRedirectUri().startsWith("http://") && 
                !ssoConfig.getRedirectUri().startsWith("https://")) {
                throw new RuntimeException("Redirect URI must start with http:// or https://");
            }
        }
    }
    
    private void validateAzureAdConfig(SsoConfiguration config) {
        if (config.getIssuerUri() == null || config.getIssuerUri().isEmpty()) {
            throw new RuntimeException("Issuer URI is required for Azure AD");
        }
        if (!config.getIssuerUri().contains("login.microsoftonline.com")) {
            throw new RuntimeException("Invalid Azure AD issuer URI format");
        }
    }
    
    private void validateGoogleConfig(SsoConfiguration config) {
        if (config.getIssuerUri() == null || config.getIssuerUri().isEmpty()) {
            config.setIssuerUri("https://accounts.google.com");
        }
    }
    
    private void validateOktaConfig(SsoConfiguration config) {
        if (config.getIssuerUri() == null || config.getIssuerUri().isEmpty()) {
            throw new RuntimeException("Issuer URI is required for Okta");
        }
        if (!config.getIssuerUri().contains("okta.com")) {
            throw new RuntimeException("Invalid Okta issuer URI format");
        }
    }
    
    private void validateKeycloakConfig(SsoConfiguration config) {
        if (config.getIssuerUri() == null || config.getIssuerUri().isEmpty()) {
            throw new RuntimeException("Issuer URI is required for Keycloak");
        }
    }
    
    private void validateCustomOidcConfig(SsoConfiguration config) {
        if (config.getIssuerUri() == null || config.getIssuerUri().isEmpty()) {
            throw new RuntimeException("Issuer URI is required for Custom OIDC");
        }
        if (config.getAuthorizationUri() == null || config.getAuthorizationUri().isEmpty()) {
            throw new RuntimeException("Authorization URI is required for Custom OIDC");
        }
        if (config.getTokenUri() == null || config.getTokenUri().isEmpty()) {
            throw new RuntimeException("Token URI is required for Custom OIDC");
        }
    }
    
    private void validateSaml2Config(SsoConfiguration config) {
        // SAML2 specific validation
        throw new RuntimeException("SAML2 configuration not yet implemented");
    }
}