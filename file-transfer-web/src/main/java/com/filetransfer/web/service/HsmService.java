package com.filetransfer.web.service;

import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.HsmValidationRecordRepository;
import com.filetransfer.web.repository.ServiceConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for HSM (Hardware Security Module) integration and cryptographic operations
 */
@Service
@Transactional
public class HsmService {
    
    private static final Logger logger = LoggerFactory.getLogger(HsmService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private HsmValidationRecordRepository hsmValidationRepository;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Value("${hsm.enabled:false}")
    private boolean hsmEnabled;
    
    @Value("${hsm.default-timeout-seconds:30}")
    private int defaultTimeoutSeconds;
    
    @Value("${hsm.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    // HSM provider instances cache
    private final Map<HsmProvider, Provider> providerCache = new HashMap<>();
    private final Map<String, KeyStore> keyStoreCache = new HashMap<>();
    
    /**
     * Perform HSM validation for a file transfer
     */
    public HsmValidationRecord performHsmValidation(Long fileTransferId, String fileName, 
                                                   String tenantId, String serviceName, String subServiceName,
                                                   TransferDirection direction, Path filePath) {
        
        if (!hsmEnabled) {
            logger.debug("HSM validation disabled globally, skipping for file: {}", fileName);
            return createSkippedValidationRecord(fileTransferId, fileName, tenantId, serviceName, subServiceName);
        }
        
        // Get service configuration for HSM settings
        ServiceConfiguration serviceConfig = serviceConfigurationRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
            .orElse(null);
        
        if (serviceConfig == null || !serviceConfig.getHsmValidationRequired()) {
            logger.debug("HSM validation not required for service {}/{}, skipping", serviceName, subServiceName);
            return createSkippedValidationRecord(fileTransferId, fileName, tenantId, serviceName, subServiceName);
        }
        
        // Determine HSM operation based on direction
        HsmOperation operation = direction == TransferDirection.OUTBOUND 
                               ? serviceConfig.getHsmOperationOutbound()
                               : serviceConfig.getHsmOperationInbound();
        
        HsmValidationRecord record = new HsmValidationRecord(
            fileTransferId, fileName, tenantId, serviceName, subServiceName,
            serviceConfig.getHsmProvider(), operation
        );
        
        record.setHsmKeyAlias(serviceConfig.getHsmKeyAlias());
        record.setAlgorithm(serviceConfig.getHsmAlgorithm());
        record = hsmValidationRepository.save(record);
        
        try {
            // Perform the actual HSM operation
            HsmValidationResult result = executeHsmOperation(record, serviceConfig, filePath);
            
            // Update record with results
            updateValidationRecord(record, result);
            
            logger.info("HSM validation completed for file {}: {} - {}", 
                       fileName, operation, record.getStatus());
            
            return record;
            
        } catch (Exception e) {
            logger.error("HSM validation failed for file {}: {}", fileName, e.getMessage());
            record.setStatus(HsmValidationStatus.ERROR);
            record.setErrorMessage(e.getMessage());
            record.setCompletedAt(LocalDateTime.now());
            record.setProcessingTimeMs(record.calculateProcessingDuration());
            return hsmValidationRepository.save(record);
        }
    }
    
    /**
     * Perform HSM validation asynchronously for large files
     */
    public CompletableFuture<HsmValidationRecord> performHsmValidationAsync(
            Long fileTransferId, String fileName, String tenantId, 
            String serviceName, String subServiceName, TransferDirection direction, Path filePath) {
        
        return CompletableFuture.supplyAsync(() -> 
            performHsmValidation(fileTransferId, fileName, tenantId, serviceName, subServiceName, direction, filePath)
        );
    }
    
    /**
     * Verify HSM signature for inbound files
     */
    public boolean verifyHsmSignature(HsmValidationRecord record, Path filePath, String signatureValue) {
        try {
            ServiceConfiguration serviceConfig = getServiceConfiguration(record);
            if (serviceConfig == null) {
                logger.error("Service configuration not found for HSM verification");
                return false;
            }
            
            Provider hsmProvider = getHsmProvider(serviceConfig.getHsmProvider(), serviceConfig.getHsmConfigProperties());
            KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
            
            // Get public key for verification
            Certificate certificate = keyStore.getCertificate(record.getHsmKeyAlias());
            if (certificate == null) {
                logger.error("Certificate not found for alias: {}", record.getHsmKeyAlias());
                return false;
            }
            
            PublicKey publicKey = certificate.getPublicKey();
            
            // Verify signature
            Signature signature = Signature.getInstance(record.getAlgorithm(), hsmProvider);
            signature.initVerify(publicKey);
            
            byte[] fileData = Files.readAllBytes(filePath);
            signature.update(fileData);
            
            boolean isValid = signature.verify(Base64.getDecoder().decode(signatureValue));
            
            logger.info("HSM signature verification for file {}: {}", record.getFileName(), isValid ? "PASSED" : "FAILED");
            return isValid;
            
        } catch (Exception e) {
            logger.error("HSM signature verification failed for file {}: {}", record.getFileName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate HSM signature for outbound files
     */
    public String generateHsmSignature(HsmValidationRecord record, Path filePath) {
        try {
            ServiceConfiguration serviceConfig = getServiceConfiguration(record);
            if (serviceConfig == null) {
                throw new RuntimeException("Service configuration not found for HSM signature generation");
            }
            
            Provider hsmProvider = getHsmProvider(serviceConfig.getHsmProvider(), serviceConfig.getHsmConfigProperties());
            KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
            
            // Get private key for signing
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(record.getHsmKeyAlias(), getKeyPassword(serviceConfig));
            if (privateKey == null) {
                throw new RuntimeException("Private key not found for alias: " + record.getHsmKeyAlias());
            }
            
            // Generate signature
            Signature signature = Signature.getInstance(record.getAlgorithm(), hsmProvider);
            signature.initSign(privateKey);
            
            byte[] fileData = Files.readAllBytes(filePath);
            signature.update(fileData);
            
            byte[] signatureBytes = signature.sign();
            String signatureValue = Base64.getEncoder().encodeToString(signatureBytes);
            
            logger.info("HSM signature generated for file {}: {} bytes", record.getFileName(), signatureBytes.length);
            return signatureValue;
            
        } catch (Exception e) {
            logger.error("HSM signature generation failed for file {}: {}", record.getFileName(), e.getMessage());
            throw new RuntimeException("HSM signature generation failed", e);
        }
    }
    
    /**
     * Test HSM connectivity and key availability
     */
    public HsmTestResult testHsmConnection(String tenantId, String serviceName, String subServiceName) {
        try {
            ServiceConfiguration serviceConfig = serviceConfigurationRepository
                .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
                .orElseThrow(() -> new RuntimeException("Service configuration not found"));
            
            if (!serviceConfig.getHsmValidationRequired()) {
                return new HsmTestResult(true, "HSM validation not required for this service", null);
            }
            
            Provider hsmProvider = getHsmProvider(serviceConfig.getHsmProvider(), serviceConfig.getHsmConfigProperties());
            KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
            
            // Test key availability
            String keyAlias = serviceConfig.getHsmKeyAlias();
            if (keyAlias == null || !keyStore.containsAlias(keyAlias)) {
                return new HsmTestResult(false, "HSM key not found: " + keyAlias, "KEY_NOT_FOUND");
            }
            
            // Test key access
            Certificate certificate = keyStore.getCertificate(keyAlias);
            if (certificate == null) {
                return new HsmTestResult(false, "Certificate not found for key: " + keyAlias, "CERTIFICATE_NOT_FOUND");
            }
            
            // Test algorithm support
            try {
                Signature.getInstance(serviceConfig.getHsmAlgorithm(), hsmProvider);
            } catch (NoSuchAlgorithmException e) {
                return new HsmTestResult(false, "Algorithm not supported: " + serviceConfig.getHsmAlgorithm(), "ALGORITHM_NOT_SUPPORTED");
            }
            
            return new HsmTestResult(true, "HSM connection and key validation successful", null);
            
        } catch (Exception e) {
            logger.error("HSM connection test failed for {}/{}: {}", serviceName, subServiceName, e.getMessage());
            return new HsmTestResult(false, "HSM connection test failed: " + e.getMessage(), "CONNECTION_FAILED");
        }
    }
    
    /**
     * Get HSM validation statistics for a tenant
     */
    public Map<String, Object> getHsmStatistics(String tenantId) {
        try {
            List<HsmValidationRecord> records = hsmValidationRepository.findByTenantId(tenantId);
            
            long totalValidations = records.size();
            long passedValidations = records.stream().mapToLong(r -> r.getStatus().isSuccess() ? 1 : 0).sum();
            long failedValidations = records.stream().mapToLong(r -> r.getStatus().isFailure() ? 1 : 0).sum();
            
            Map<HsmProvider, Long> providerUsage = new HashMap<>();
            Map<HsmOperation, Long> operationUsage = new HashMap<>();
            
            for (HsmValidationRecord record : records) {
                providerUsage.merge(record.getHsmProvider(), 1L, Long::sum);
                operationUsage.merge(record.getOperation(), 1L, Long::sum);
            }
            
            double averageProcessingTime = records.stream()
                .filter(r -> r.getProcessingTimeMs() != null)
                .mapToLong(HsmValidationRecord::getProcessingTimeMs)
                .average()
                .orElse(0.0);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalValidations", totalValidations);
            statistics.put("passedValidations", passedValidations);
            statistics.put("failedValidations", failedValidations);
            statistics.put("successRate", totalValidations > 0 ? (double) passedValidations / totalValidations : 0.0);
            statistics.put("providerUsage", providerUsage);
            statistics.put("operationUsage", operationUsage);
            statistics.put("averageProcessingTimeMs", averageProcessingTime);
            
            return statistics;
            
        } catch (Exception e) {
            logger.error("Failed to get HSM statistics for tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Failed to get HSM statistics", e);
        }
    }
    
    // Private helper methods
    
    private HsmValidationRecord createSkippedValidationRecord(Long fileTransferId, String fileName, 
                                                            String tenantId, String serviceName, String subServiceName) {
        HsmValidationRecord record = new HsmValidationRecord(
            fileTransferId, fileName, tenantId, serviceName, subServiceName,
            HsmProvider.NONE, HsmOperation.HASH
        );
        record.setStatus(HsmValidationStatus.SKIPPED);
        record.setCompletedAt(LocalDateTime.now());
        return hsmValidationRepository.save(record);
    }
    
    private HsmValidationResult executeHsmOperation(HsmValidationRecord record, 
                                                   ServiceConfiguration serviceConfig, Path filePath) {
        record.setStartedAt(LocalDateTime.now());
        record.setStatus(HsmValidationStatus.IN_PROGRESS);
        hsmValidationRepository.save(record);
        
        try {
            Provider hsmProvider = getHsmProvider(serviceConfig.getHsmProvider(), serviceConfig.getHsmConfigProperties());
            
            switch (record.getOperation()) {
                case SIGN:
                    return performSignOperation(record, serviceConfig, hsmProvider, filePath);
                case VERIFY:
                    return performVerifyOperation(record, serviceConfig, hsmProvider, filePath);
                case HASH:
                    return performHashOperation(record, serviceConfig, hsmProvider, filePath);
                case MAC:
                    return performMacOperation(record, serviceConfig, hsmProvider, filePath);
                default:
                    throw new UnsupportedOperationException("HSM operation not supported: " + record.getOperation());
            }
            
        } catch (Exception e) {
            logger.error("HSM operation failed for {}: {}", record.getOperation(), e.getMessage());
            throw new RuntimeException("HSM operation failed", e);
        }
    }
    
    private HsmValidationResult performSignOperation(HsmValidationRecord record, ServiceConfiguration serviceConfig,
                                                   Provider hsmProvider, Path filePath) throws Exception {
        
        KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(record.getHsmKeyAlias(), getKeyPassword(serviceConfig));
        
        Signature signature = Signature.getInstance(record.getAlgorithm(), hsmProvider);
        signature.initSign(privateKey);
        
        byte[] fileData = Files.readAllBytes(filePath);
        signature.update(fileData);
        
        byte[] signatureBytes = signature.sign();
        String signatureValue = Base64.getEncoder().encodeToString(signatureBytes);
        
        return new HsmValidationResult(HsmValidationStatus.PASSED, signatureValue, null, null);
    }
    
    private HsmValidationResult performVerifyOperation(HsmValidationRecord record, ServiceConfiguration serviceConfig,
                                                     Provider hsmProvider, Path filePath) throws Exception {
        
        // For verification, we need the signature value (could be from metadata or separate file)
        String signatureValue = extractSignatureFromMetadata(record);
        if (signatureValue == null) {
            return new HsmValidationResult(HsmValidationStatus.FAILED, null, "SIGNATURE_NOT_FOUND", 
                                         "Signature value not found for verification");
        }
        
        KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
        Certificate certificate = keyStore.getCertificate(record.getHsmKeyAlias());
        
        Signature signature = Signature.getInstance(record.getAlgorithm(), hsmProvider);
        signature.initVerify(certificate.getPublicKey());
        
        byte[] fileData = Files.readAllBytes(filePath);
        signature.update(fileData);
        
        boolean isValid = signature.verify(Base64.getDecoder().decode(signatureValue));
        
        HsmValidationStatus status = isValid ? HsmValidationStatus.PASSED : HsmValidationStatus.FAILED;
        return new HsmValidationResult(status, signatureValue, null, isValid ? null : "Signature verification failed");
    }
    
    private HsmValidationResult performHashOperation(HsmValidationRecord record, ServiceConfiguration serviceConfig,
                                                   Provider hsmProvider, Path filePath) throws Exception {
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256", hsmProvider);
        byte[] fileData = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileData);
        String hashValue = Base64.getEncoder().encodeToString(hashBytes);
        
        return new HsmValidationResult(HsmValidationStatus.PASSED, hashValue, hashValue, null);
    }
    
    private HsmValidationResult performMacOperation(HsmValidationRecord record, ServiceConfiguration serviceConfig,
                                                  Provider hsmProvider, Path filePath) throws Exception {
        
        KeyStore keyStore = getKeyStore(serviceConfig, hsmProvider);
        Key macKey = keyStore.getKey(record.getHsmKeyAlias(), getKeyPassword(serviceConfig));
        
        Mac mac = Mac.getInstance("HmacSHA256", hsmProvider);
        mac.init(macKey);
        
        byte[] fileData = Files.readAllBytes(filePath);
        byte[] macBytes = mac.doFinal(fileData);
        String macValue = Base64.getEncoder().encodeToString(macBytes);
        
        return new HsmValidationResult(HsmValidationStatus.PASSED, macValue, macValue, null);
    }
    
    private Provider getHsmProvider(HsmProvider providerType, String configProperties) throws Exception {
        if (providerCache.containsKey(providerType)) {
            return providerCache.get(providerType);
        }
        
        Provider provider;
        
        switch (providerType) {
            case AZURE_KEY_VAULT:
                provider = createAzureKeyVaultProvider(configProperties);
                break;
            case AWS_CLOUD_HSM:
                provider = createAwsCloudHsmProvider(configProperties);
                break;
            case THALES_LUNA:
                provider = createThalesLunaProvider(configProperties);
                break;
            default:
                throw new UnsupportedOperationException("HSM provider not implemented: " + providerType);
        }
        
        Security.addProvider(provider);
        providerCache.put(providerType, provider);
        
        return provider;
    }
    
    private KeyStore getKeyStore(ServiceConfiguration serviceConfig, Provider hsmProvider) throws Exception {
        String cacheKey = serviceConfig.getTenantId() + "_" + serviceConfig.getServiceName() + "_" + serviceConfig.getSubServiceName();
        
        if (keyStoreCache.containsKey(cacheKey)) {
            return keyStoreCache.get(cacheKey);
        }
        
        KeyStore keyStore = KeyStore.getInstance("PKCS11", hsmProvider);
        keyStore.load(null, getKeyStorePassword(serviceConfig));
        
        keyStoreCache.put(cacheKey, keyStore);
        return keyStore;
    }
    
    private void updateValidationRecord(HsmValidationRecord record, HsmValidationResult result) {
        record.setStatus(result.getStatus());
        record.setSignatureValue(result.getSignatureValue());
        record.setHsmHash(result.getHashValue());
        record.setErrorMessage(result.getErrorMessage());
        record.setErrorCode(result.getErrorCode());
        record.setCompletedAt(LocalDateTime.now());
        record.setProcessingTimeMs(record.calculateProcessingDuration());
        
        hsmValidationRepository.save(record);
    }
    
    private ServiceConfiguration getServiceConfiguration(HsmValidationRecord record) {
        return serviceConfigurationRepository
            .findByTenantIdAndServiceNameAndSubServiceName(
                record.getTenantId(), record.getServiceName(), record.getSubServiceName())
            .orElse(null);
    }
    
    private Provider createAzureKeyVaultProvider(String configProperties) throws Exception {
        // Azure Key Vault HSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically load the Azure Key Vault JCE provider
        // For now, return a mock implementation
        return new Provider("AzureKeyVault", "1.0", "Azure Key Vault HSM Provider") {};
    }
    
    private Provider createAwsCloudHsmProvider(String configProperties) throws Exception {
        // AWS CloudHSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically load the AWS CloudHSM JCE provider
        return new Provider("AWSCloudHSM", "1.0", "AWS CloudHSM Provider") {};
    }
    
    private Provider createThalesLunaProvider(String configProperties) throws Exception {
        // Thales Luna HSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically load the Thales Luna JCE provider
        return new Provider("LunaProvider", "1.0", "Thales Luna HSM Provider") {};
    }
    
    private Map<String, String> parseConfigProperties(String configProperties) throws IOException {
        if (configProperties == null || configProperties.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        return objectMapper.readValue(configProperties, Map.class);
    }
    
    private char[] getKeyPassword(ServiceConfiguration serviceConfig) {
        // In production, this would retrieve the password from a secure location
        return "changeit".toCharArray();
    }
    
    private char[] getKeyStorePassword(ServiceConfiguration serviceConfig) {
        // In production, this would retrieve the password from a secure location
        return "changeit".toCharArray();
    }
    
    private String extractSignatureFromMetadata(HsmValidationRecord record) {
        // Extract signature from file metadata or companion signature file
        // This is a simplified implementation
        return record.getSignatureValue();
    }
    
    // Result classes
    
    public static class HsmValidationResult {
        private final HsmValidationStatus status;
        private final String signatureValue;
        private final String hashValue;
        private final String errorMessage;
        private final String errorCode;
        
        public HsmValidationResult(HsmValidationStatus status, String signatureValue, String hashValue, String errorMessage) {
            this.status = status;
            this.signatureValue = signatureValue;
            this.hashValue = hashValue;
            this.errorMessage = errorMessage;
            this.errorCode = null;
        }
        
        public HsmValidationResult(HsmValidationStatus status, String signatureValue, String errorCode, String errorMessage) {
            this.status = status;
            this.signatureValue = signatureValue;
            this.hashValue = null;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }
        
        // Getters
        public HsmValidationStatus getStatus() { return status; }
        public String getSignatureValue() { return signatureValue; }
        public String getHashValue() { return hashValue; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }
    }
    
    public static class HsmTestResult {
        private final boolean success;
        private final String message;
        private final String errorCode;
        
        public HsmTestResult(boolean success, String message, String errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
    }
}