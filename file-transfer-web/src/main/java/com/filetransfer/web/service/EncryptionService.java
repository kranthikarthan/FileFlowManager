package com.filetransfer.web.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Service for encryption, hashing, and cryptographic operations
 * Implements strong encryption standards for data protection
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    // Encryption constants
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    private static final int AES_KEY_LENGTH = 256; // 256-bit AES key

    // Hashing constants
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SECURE_HASH_ALGORITHM = "SHA-512";

    @Value("${security.encryption.master-key:}")
    private String masterKeyBase64;

    @Value("${security.encryption.key-derivation-salt:FileTransferSalt2024}")
    private String keyDerivationSalt;

    private final SecureRandom secureRandom;
    private final BCryptPasswordEncoder passwordEncoder;

    public EncryptionService() {
        // Add Bouncy Castle provider for enhanced crypto support
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        this.secureRandom = new SecureRandom();
        this.passwordEncoder = new BCryptPasswordEncoder(12); // High strength
    }

    /**
     * Encrypt sensitive data using AES-GCM
     */
    public EncryptionResult encryptData(String plaintext) {
        try {
            SecretKey key = getMasterKey();
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            
            // Encrypt data
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and ciphertext
            byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);
            
            String encodedData = Base64.getEncoder().encodeToString(encryptedData);
            
            logger.debug("Data encrypted successfully, length: {}", encodedData.length());
            return new EncryptionResult(true, encodedData, null);
            
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            return new EncryptionResult(false, null, "Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypt sensitive data using AES-GCM
     */
    public EncryptionResult decryptData(String encryptedData) {
        try {
            SecretKey key = getMasterKey();
            
            // Decode from Base64
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            if (decodedData.length < GCM_IV_LENGTH) {
                return new EncryptionResult(false, null, "Invalid encrypted data format");
            }
            
            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decodedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            
            // Decrypt data
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            String plaintext = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            logger.debug("Data decrypted successfully");
            return new EncryptionResult(true, plaintext, null);
            
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            return new EncryptionResult(false, null, "Decryption failed: " + e.getMessage());
        }
    }

    /**
     * Hash password using BCrypt
     */
    public String hashPassword(String password) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            logger.debug("Password hashed successfully");
            return hashedPassword;
        } catch (Exception e) {
            logger.error("Password hashing failed", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify password against BCrypt hash
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        try {
            boolean matches = passwordEncoder.matches(password, hashedPassword);
            logger.debug("Password verification: {}", matches ? "successful" : "failed");
            return matches;
        } catch (Exception e) {
            logger.error("Password verification failed", e);
            return false;
        }
    }

    /**
     * Generate secure hash of data using SHA-256
     */
    public String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash generation failed", e);
            throw new RuntimeException("Hash generation failed", e);
        }
    }

    /**
     * Generate secure hash with salt using SHA-512
     */
    public String generateSecureHash(String data, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SECURE_HASH_ALGORITHM);
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Secure hash generation failed", e);
            throw new RuntimeException("Secure hash generation failed", e);
        }
    }

    /**
     * Generate cryptographically secure random token
     */
    public String generateSecureToken(int lengthBytes) {
        byte[] tokenBytes = new byte[lengthBytes];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Generate secure API key
     */
    public String generateApiKey() {
        return generateSecureToken(32); // 256-bit API key
    }

    /**
     * Generate secure session token
     */
    public String generateSessionToken() {
        return generateSecureToken(24); // 192-bit session token
    }

    /**
     * Verify data integrity using hash comparison
     */
    public boolean verifyDataIntegrity(String originalData, String expectedHash) {
        try {
            String actualHash = generateHash(originalData);
            return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            logger.error("Data integrity verification failed", e);
            return false;
        }
    }

    /**
     * Encrypt file content
     */
    public EncryptionResult encryptFileContent(byte[] fileContent) {
        try {
            String base64Content = Base64.getEncoder().encodeToString(fileContent);
            return encryptData(base64Content);
        } catch (Exception e) {
            logger.error("File content encryption failed", e);
            return new EncryptionResult(false, null, "File encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypt file content
     */
    public EncryptionResult decryptFileContent(String encryptedContent) {
        try {
            EncryptionResult decryptResult = decryptData(encryptedContent);
            if (!decryptResult.isSuccess()) {
                return decryptResult;
            }
            
            byte[] fileContent = Base64.getDecoder().decode(decryptResult.getData());
            String base64FileContent = Base64.getEncoder().encodeToString(fileContent);
            
            return new EncryptionResult(true, base64FileContent, null);
        } catch (Exception e) {
            logger.error("File content decryption failed", e);
            return new EncryptionResult(false, null, "File decryption failed: " + e.getMessage());
        }
    }

    /**
     * Get or generate master encryption key
     */
    private SecretKey getMasterKey() throws Exception {
        if (masterKeyBase64 != null && !masterKeyBase64.isEmpty()) {
            // Use provided master key
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            return new SecretKeySpec(keyBytes, AES_ALGORITHM);
        } else {
            // Derive key from salt (not recommended for production)
            logger.warn("Using derived key - consider setting security.encryption.master-key property");
            return deriveKeyFromSalt();
        }
    }

    /**
     * Derive encryption key from salt (fallback method)
     */
    private SecretKey deriveKeyFromSalt() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] saltBytes = keyDerivationSalt.getBytes(StandardCharsets.UTF_8);
        
        // Multiple rounds of hashing for key derivation
        byte[] keyBytes = saltBytes;
        for (int i = 0; i < 10000; i++) {
            digest.reset();
            keyBytes = digest.digest(keyBytes);
        }
        
        // Ensure key is exactly 256 bits
        byte[] aesKey = new byte[32];
        System.arraycopy(keyBytes, 0, aesKey, 0, Math.min(keyBytes.length, 32));
        
        return new SecretKeySpec(aesKey, AES_ALGORITHM);
    }

    /**
     * Generate new AES key for data encryption
     */
    public String generateNewEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.error("Key generation failed", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Encryption result container
     */
    public static class EncryptionResult {
        private final boolean success;
        private final String data;
        private final String errorMessage;

        public EncryptionResult(boolean success, String data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}