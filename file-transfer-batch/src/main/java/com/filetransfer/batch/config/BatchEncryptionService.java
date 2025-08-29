package com.filetransfer.batch.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Encryption service for batch application
 * Provides AES-GCM encryption, hashing, and secure token generation
 */
@Service
public class BatchEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;

    private final SecretKey masterKey;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    static {
        // Add BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public BatchEncryptionService(String masterKeyString) {
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        this.secureRandom = new SecureRandom();
        this.masterKey = deriveKeyFromString(masterKeyString);
    }

    /**
     * Encrypt sensitive data using AES-GCM
     */
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null) {
            return null;
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

        byte[] encryptedData = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV and encrypted data
        byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    /**
     * Decrypt sensitive data using AES-GCM
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null) {
            return null;
        }

        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
        System.arraycopy(encryptedWithIv, iv.length, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, "UTF-8");
    }

    /**
     * Hash password using BCrypt
     */
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * Verify password against hash
     */
    public boolean verifyPassword(String password, String hash) {
        return passwordEncoder.matches(password, hash);
    }

    /**
     * Generate secure random token
     */
    public String generateSecureToken(int length) {
        byte[] token = new byte[length];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Generate API key
     */
    public String generateApiKey() {
        return "batch_" + generateSecureToken(32);
    }

    /**
     * Generate session token
     */
    public String generateSessionToken() {
        return generateSecureToken(48);
    }

    /**
     * Hash sensitive data for storage
     */
    public String hashSensitiveData(String data) {
        return passwordEncoder.encode(data);
    }

    /**
     * Encrypt job parameters
     */
    public String encryptJobParameters(String parameters) throws Exception {
        return encrypt(parameters);
    }

    /**
     * Decrypt job parameters
     */
    public String decryptJobParameters(String encryptedParameters) throws Exception {
        return decrypt(encryptedParameters);
    }

    /**
     * Encrypt tenant configuration
     */
    public String encryptTenantConfig(String config) throws Exception {
        return encrypt(config);
    }

    /**
     * Decrypt tenant configuration
     */
    public String decryptTenantConfig(String encryptedConfig) throws Exception {
        return decrypt(encryptedConfig);
    }

    /**
     * Generate secure job ID
     */
    public String generateSecureJobId() {
        return "job_" + System.currentTimeMillis() + "_" + generateSecureToken(16);
    }

    /**
     * Derive encryption key from string
     */
    private SecretKey deriveKeyFromString(String keyString) {
        try {
            // Use SHA-256 to derive a consistent key from the string
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyString.getBytes("UTF-8"));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    /**
     * Generate new AES key
     */
    public SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(AES_KEY_LENGTH);
        return keyGenerator.generateKey();
    }

    /**
     * Secure data wiping
     */
    public void secureWipe(byte[] data) {
        if (data != null) {
            secureRandom.nextBytes(data);
            java.util.Arrays.fill(data, (byte) 0);
        }
    }

    /**
     * Secure string wiping (best effort)
     */
    public void secureWipe(char[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (char) secureRandom.nextInt(Character.MAX_VALUE);
            }
            java.util.Arrays.fill(data, '\0');
        }
    }
}