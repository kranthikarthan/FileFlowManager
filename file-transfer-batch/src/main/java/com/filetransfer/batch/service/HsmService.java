package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.*;
import com.filetransfer.batch.repository.HsmValidationRecordRepository;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Batch service for HSM (Hardware Security Module) integration and cryptographic operations
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
    
    @Value("${hsm.batch-processing-enabled:true}")
    private boolean batchProcessingEnabled;
    
    // HSM provider instances cache
    private final Map<HsmProvider, Provider> providerCache = new HashMap<>();
    private final Map<String, KeyStore> keyStoreCache = new HashMap<>();
    
    /**
     * Scheduled job to process pending HSM validations
     */
    @Scheduled(fixedDelayString = "${hsm.processing-interval-seconds:60}000")
    public void processPendingHsmValidations() {
        if (!hsmEnabled || !batchProcessingEnabled) {
            return;
        }
        
        try {
            List<HsmValidationRecord> pendingRecords = hsmValidationRepository.findByStatus(HsmValidationStatus.PENDING);
            
            logger.info("Processing {} pending HSM validations", pendingRecords.size());
            
            for (HsmValidationRecord record : pendingRecords) {
                try {
                    processHsmValidationRecord(record);
                } catch (Exception e) {
                    logger.error("Failed to process HSM validation record {}: {}", record.getId(), e.getMessage());
                    markValidationAsFailed(record, "PROCESSING_ERROR", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing pending HSM validations: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled job to handle HSM validation timeouts
     */
    @Scheduled(fixedDelayString = "${hsm.timeout-check-interval-seconds:300}000")
    public void handleHsmValidationTimeouts() {
        if (!hsmEnabled) {
            return;
        }
        
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(defaultTimeoutSeconds);
            List<HsmValidationRecord> timedOutRecords = hsmValidationRepository
                .findTimedOutRecords(HsmValidationStatus.IN_PROGRESS, timeoutThreshold);
            
            for (HsmValidationRecord record : timedOutRecords) {
                logger.warn("HSM validation timed out for file: {}", record.getFileName());
                markValidationAsFailed(record, "TIMEOUT", "HSM validation timed out");
            }
            
        } catch (Exception e) {
            logger.error("Error handling HSM validation timeouts: {}", e.getMessage());
        }
    }
    
    /**
     * Perform HSM validation for a file transfer (batch processing)
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
        
        // Process immediately or queue for batch processing
        if (shouldProcessImmediately(filePath)) {
            return processHsmValidationRecord(record);
        } else {
            // Will be processed by scheduled job
            logger.info("Queued HSM validation for batch processing: {}", fileName);
            return record;
        }
    }
    
    /**
     * Perform HSM validation asynchronously
     */
    @Async("hsmProcessingExecutor")
    public CompletableFuture<HsmValidationRecord> performHsmValidationAsync(
            Long fileTransferId, String fileName, String tenantId, 
            String serviceName, String subServiceName, TransferDirection direction, Path filePath) {
        
        try {
            HsmValidationRecord result = performHsmValidation(
                fileTransferId, fileName, tenantId, serviceName, subServiceName, direction, filePath);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<HsmValidationRecord> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Batch process multiple HSM validations
     */
    public List<HsmValidationRecord> batchProcessHsmValidations(List<HsmValidationRecord> records) {
        List<HsmValidationRecord> results = new ArrayList<>();
        
        for (HsmValidationRecord record : records) {
            try {
                HsmValidationRecord result = processHsmValidationRecord(record);
                results.add(result);
            } catch (Exception e) {
                logger.error("Failed to process HSM validation in batch for record {}: {}", 
                           record.getId(), e.getMessage());
                markValidationAsFailed(record, "BATCH_PROCESSING_ERROR", e.getMessage());
                results.add(record);
            }
        }
        
        return results;
    }
    
    // Private helper methods
    
    private HsmValidationRecord processHsmValidationRecord(HsmValidationRecord record) {
        try {
            record.setStartedAt(LocalDateTime.now());
            record.setStatus(HsmValidationStatus.IN_PROGRESS);
            hsmValidationRepository.save(record);
            
            ServiceConfiguration serviceConfig = getServiceConfiguration(record);
            if (serviceConfig == null) {
                throw new RuntimeException("Service configuration not found");
            }
            
            // Reconstruct file path (this would be more sophisticated in practice)
            Path filePath = Paths.get("/data/processing/" + record.getFileName());
            
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found for HSM validation: " + filePath);
            }
            
            // Execute HSM operation
            HsmValidationResult result = executeHsmOperation(record, serviceConfig, filePath);
            
            // Update record with results
            updateValidationRecord(record, result);
            
            logger.info("HSM validation completed for file {}: {} - {}", 
                       record.getFileName(), record.getOperation(), record.getStatus());
            
            return record;
            
        } catch (Exception e) {
            logger.error("HSM validation processing failed for file {}: {}", record.getFileName(), e.getMessage());
            markValidationAsFailed(record, "PROCESSING_ERROR", e.getMessage());
            return record;
        }
    }
    
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
    
    private void markValidationAsFailed(HsmValidationRecord record, String errorCode, String errorMessage) {
        record.setStatus(HsmValidationStatus.FAILED);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setCompletedAt(LocalDateTime.now());
        record.setProcessingTimeMs(record.calculateProcessingDuration());
        hsmValidationRepository.save(record);
    }
    
    private boolean shouldProcessImmediately(Path filePath) {
        try {
            // Process small files immediately, queue large files for batch processing
            long fileSize = Files.size(filePath);
            return fileSize < 10 * 1024 * 1024; // 10MB threshold
        } catch (IOException e) {
            logger.warn("Could not determine file size for {}, processing immediately", filePath);
            return true;
        }
    }
    
    private HsmValidationResult executeHsmOperation(HsmValidationRecord record, 
                                                   ServiceConfiguration serviceConfig, Path filePath) {
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
            logger.error("HSM operation execution failed for {}: {}", record.getOperation(), e.getMessage());
            throw new RuntimeException("HSM operation execution failed", e);
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
        
        // For batch processing, signature might be in a companion file or metadata
        String signatureValue = findSignatureForFile(filePath, record);
        if (signatureValue == null) {
            return new HsmValidationResult(HsmValidationStatus.FAILED, null, "SIGNATURE_NOT_FOUND", 
                                         "Signature file not found for verification");
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
    
    private String findSignatureForFile(Path filePath, HsmValidationRecord record) {
        try {
            // Look for companion signature file
            Path signatureFile = filePath.resolveSibling(filePath.getFileName() + ".sig");
            if (Files.exists(signatureFile)) {
                return Files.readString(signatureFile).trim();
            }
            
            // Look in metadata
            if (record.getMetadata() != null) {
                Map<String, Object> metadata = objectMapper.readValue(record.getMetadata(), Map.class);
                return (String) metadata.get("signature");
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Could not find signature for file {}: {}", filePath, e.getMessage());
            return null;
        }
    }
    
    private Provider createAzureKeyVaultProvider(String configProperties) throws Exception {
        // Azure Key Vault HSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically initialize the Azure Key Vault JCE provider
        // with proper configuration for the specific tenant/service
        return new Provider("AzureKeyVault", "1.0", "Azure Key Vault HSM Provider") {};
    }
    
    private Provider createAwsCloudHsmProvider(String configProperties) throws Exception {
        // AWS CloudHSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically initialize the AWS CloudHSM JCE provider
        return new Provider("AWSCloudHSM", "1.0", "AWS CloudHSM Provider") {};
    }
    
    private Provider createThalesLunaProvider(String configProperties) throws Exception {
        // Thales Luna HSM provider implementation
        Map<String, String> config = parseConfigProperties(configProperties);
        
        // This would typically initialize the Thales Luna JCE provider
        return new Provider("LunaProvider", "1.0", "Thales Luna HSM Provider") {};
    }
    
    private Map<String, String> parseConfigProperties(String configProperties) throws IOException {
        if (configProperties == null || configProperties.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        return objectMapper.readValue(configProperties, Map.class);
    }
    
    private char[] getKeyPassword(ServiceConfiguration serviceConfig) {
        // Retrieve password from secure configuration or environment variable
        String password = System.getenv("HSM_KEY_PASSWORD_" + serviceConfig.getTenantId());
        if (password == null) {
            password = System.getProperty("hsm.key.password", "");
        }
        if (password.isEmpty()) {
            logger.warn("HSM key password not configured for tenant: {}", serviceConfig.getTenantId());
            throw new RuntimeException("HSM key password not configured");
        }
        return password.toCharArray();
    }
    
    private char[] getKeyStorePassword(ServiceConfiguration serviceConfig) {
        // Retrieve password from secure configuration or environment variable
        String password = System.getenv("HSM_KEYSTORE_PASSWORD_" + serviceConfig.getTenantId());
        if (password == null) {
            password = System.getProperty("hsm.keystore.password", "");
        }
        if (password.isEmpty()) {
            logger.warn("HSM keystore password not configured for tenant: {}", serviceConfig.getTenantId());
            throw new RuntimeException("HSM keystore password not configured");
        }
        return password.toCharArray();
    }
    
    // Result classes (same as web application)
    
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
}