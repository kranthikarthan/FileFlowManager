package com.filetransfer.web.entity;

/**
 * Supported HSM (Hardware Security Module) providers for cryptographic operations
 */
public enum HsmProvider {
    NONE("No HSM", "No HSM integration", ""),
    THALES_LUNA("Thales Luna HSM", "Thales Luna Network HSM", "com.safenetinc.luna.provider.LunaProvider"),
    AWS_CLOUD_HSM("AWS CloudHSM", "Amazon Web Services CloudHSM", "com.amazon.cloudhsm.jce.provider.CloudHsmProvider"),
    AZURE_KEY_VAULT("Azure Key Vault", "Microsoft Azure Key Vault HSM", "com.azure.security.keyvault.jce.KeyVaultJceProvider"),
    UTIMACO_CRYPTO_SERVER("Utimaco CryptoServer", "Utimaco CryptoServer HSM", "CryptoServerJCE"),
    GEMALTO_SAFE_NET("Gemalto SafeNet", "Gemalto SafeNet HSM", "com.gemalto.provider.kmip.KMIPProvider"),
    NCIPHER_NSHIELD("nCipher nShield", "nCipher nShield HSM", "com.ncipher.provider.km.nCipherKM"),
    FORTANIX_DSM("Fortanix DSM", "Fortanix Data Security Manager", "com.fortanix.sdkms.jce.provider.SdkmsProvider"),
    SECUROSYS_PRIMUS("Securosys Primus", "Securosys Primus HSM", "ch.securosys.jce.provider.SecurosysProvider");
    
    private final String displayName;
    private final String description;
    private final String providerClassName;
    
    HsmProvider(String displayName, String description, String providerClassName) {
        this.displayName = displayName;
        this.description = description;
        this.providerClassName = providerClassName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getProviderClassName() {
        return providerClassName;
    }
    
    /**
     * Check if this provider requires network connectivity
     */
    public boolean isNetworkBased() {
        return this == THALES_LUNA || this == AWS_CLOUD_HSM || this == AZURE_KEY_VAULT || this == FORTANIX_DSM;
    }
    
    /**
     * Check if this provider is cloud-based
     */
    public boolean isCloudBased() {
        return this == AWS_CLOUD_HSM || this == AZURE_KEY_VAULT || this == FORTANIX_DSM;
    }
    
    /**
     * Check if this provider supports high availability
     */
    public boolean supportsHighAvailability() {
        return this != NONE;
    }
    
    /**
     * Get default key algorithms supported by this provider
     */
    public String[] getSupportedKeyAlgorithms() {
        switch (this) {
            case AWS_CLOUD_HSM:
            case AZURE_KEY_VAULT:
                return new String[]{"RSA", "EC", "AES"};
            case THALES_LUNA:
            case UTIMACO_CRYPTO_SERVER:
            case GEMALTO_SAFE_NET:
            case NCIPHER_NSHIELD:
                return new String[]{"RSA", "EC", "AES", "DES", "3DES"};
            case FORTANIX_DSM:
            case SECUROSYS_PRIMUS:
                return new String[]{"RSA", "EC", "AES", "ChaCha20"};
            default:
                return new String[]{};
        }
    }
}