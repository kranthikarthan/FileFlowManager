package com.filetransfer.web.entity;

/**
 * Types of HSM cryptographic operations for file integrity checks
 */
public enum HsmOperation {
    SIGN("Digital Signature", "Create digital signature for file integrity", true, false),
    VERIFY("Signature Verification", "Verify digital signature of received file", false, true),
    ENCRYPT("File Encryption", "Encrypt file content using HSM keys", true, false),
    DECRYPT("File Decryption", "Decrypt file content using HSM keys", false, true),
    HASH("Cryptographic Hash", "Generate cryptographic hash using HSM", true, true),
    MAC("Message Authentication Code", "Generate/verify MAC using HSM keys", true, true),
    KEY_GENERATION("Key Generation", "Generate cryptographic keys in HSM", true, false),
    KEY_DERIVATION("Key Derivation", "Derive keys from master keys in HSM", true, true);
    
    private final String displayName;
    private final String description;
    private final boolean supportsOutbound;
    private final boolean supportsInbound;
    
    HsmOperation(String displayName, String description, boolean supportsOutbound, boolean supportsInbound) {
        this.displayName = displayName;
        this.description = description;
        this.supportsOutbound = supportsOutbound;
        this.supportsInbound = supportsInbound;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean supportsOutbound() {
        return supportsOutbound;
    }
    
    public boolean supportsInbound() {
        return supportsInbound;
    }
    
    /**
     * Check if this operation provides non-repudiation
     */
    public boolean providesNonRepudiation() {
        return this == SIGN || this == VERIFY;
    }
    
    /**
     * Check if this operation provides confidentiality
     */
    public boolean providesConfidentiality() {
        return this == ENCRYPT || this == DECRYPT;
    }
    
    /**
     * Check if this operation provides integrity
     */
    public boolean providesIntegrity() {
        return this == SIGN || this == VERIFY || this == HASH || this == MAC;
    }
    
    /**
     * Get complementary operation (e.g., SIGN -> VERIFY)
     */
    public HsmOperation getComplementaryOperation() {
        switch (this) {
            case SIGN:
                return VERIFY;
            case VERIFY:
                return SIGN;
            case ENCRYPT:
                return DECRYPT;
            case DECRYPT:
                return ENCRYPT;
            case HASH:
                return HASH; // Hash is self-complementary
            case MAC:
                return MAC; // MAC is self-complementary
            default:
                return this;
        }
    }
}