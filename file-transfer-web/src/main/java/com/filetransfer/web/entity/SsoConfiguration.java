package com.filetransfer.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sso_configurations")
public class SsoConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Organization ID is required")
    private String organizationId;
    
    @Column(nullable = false)
    @NotBlank(message = "Organization name is required")
    private String organizationName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private SsoProvider provider;
    
    @Column(nullable = false)
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    @Column(nullable = false)
    @NotBlank(message = "Client secret is required")
    private String clientSecret;
    
    @Column
    private String issuerUri;
    
    @Column
    private String authorizationUri;
    
    @Column
    private String tokenUri;
    
    @Column
    private String userInfoUri;
    
    @Column
    private String jwkSetUri;
    
    @Column(length = 2000)
    private String redirectUri;
    
    @Column(length = 1000)
    private String scopes = "openid,profile,email";
    
    @ElementCollection
    @CollectionTable(name = "sso_attributes_mapping", 
                    joinColumns = @JoinColumn(name = "sso_config_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "mapped_field")
    private Map<String, String> attributesMapping;
    
    @Column(nullable = false)
    @NotNull
    private Boolean enabled = true;
    
    @Column
    private String logoUrl;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private String createdBy;
    
    @Column
    private String updatedBy;
    
    // Constructors
    public SsoConfiguration() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SsoConfiguration(String organizationId, String organizationName, SsoProvider provider) {
        this();
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.provider = provider;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public SsoProvider getProvider() { return provider; }
    public void setProvider(SsoProvider provider) { this.provider = provider; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    
    public String getIssuerUri() { return issuerUri; }
    public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    
    public String getAuthorizationUri() { return authorizationUri; }
    public void setAuthorizationUri(String authorizationUri) { this.authorizationUri = authorizationUri; }
    
    public String getTokenUri() { return tokenUri; }
    public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }
    
    public String getUserInfoUri() { return userInfoUri; }
    public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }
    
    public String getJwkSetUri() { return jwkSetUri; }
    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    
    public Map<String, String> getAttributesMapping() { return attributesMapping; }
    public void setAttributesMapping(Map<String, String> attributesMapping) { this.attributesMapping = attributesMapping; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}