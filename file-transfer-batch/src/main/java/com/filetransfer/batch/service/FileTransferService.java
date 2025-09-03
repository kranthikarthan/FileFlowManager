package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.entity.TransferDirection;
import com.filetransfer.batch.entity.CompressionType;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileTransferService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private FileTransferConfig fileTransferConfig;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Autowired
    private AckNackService ackNackService;
    
    @Autowired
    private CompressionService compressionService;
    
    @Scheduled(fixedDelayString = "${file-transfer.poll-interval-seconds:30}000")
    public void processPendingTransfers() {
        if (!fileTransferConfig.isEnabled()) {
            return;
        }
        
        // Process pending transfers for all active tenants
        List<String> activeTenantIds = serviceConfigurationRepository.findAllActiveTenantIds();
        
        for (String tenantId : activeTenantIds) {
            try {
                processPendingTransfersForTenant(tenantId);
            } catch (Exception e) {
                logger.error("Error processing pending transfers for tenant: {}", tenantId, e);
            }
        }
    }
    
    /**
     * Process pending transfers for a specific tenant
     */
    private void processPendingTransfersForTenant(String tenantId) {
        List<FileTransferRecord> pendingTransfers = fileTransferRepository.findByTenantIdAndStatus(tenantId, TransferStatus.PENDING);
        
        for (FileTransferRecord record : pendingTransfers) {
            try {
                processFileTransfer(record);
            } catch (Exception e) {
                logger.error("Error processing file transfer for record ID {} (tenant: {}): {}", 
                           record.getId(), tenantId, e.getMessage());
                handleTransferFailure(record, e.getMessage());
            }
        }
    }
    
    public void processFileTransfer(FileTransferRecord record) {
        logger.info("Processing file transfer: {} -> {}", record.getSourcePath(), record.getTargetPath());
        
        record.setStatus(TransferStatus.IN_PROGRESS);
        record.setProcessedAt(LocalDateTime.now());
        fileTransferRepository.save(record);
        
        try {
            Path sourcePath = Paths.get(record.getSourcePath());
            Path targetPath = Paths.get(record.getTargetPath());
            
            // Ensure target directory exists
            Files.createDirectories(targetPath.getParent());
            
            Path actualSourcePath = sourcePath;
            Path actualTargetPath = targetPath;
            
            // Handle compression for outbound files
            if (record.getDirection() == TransferDirection.OUTBOUND && record.getCompressionEnabled()) {
                logger.info("Compressing outbound file: {}", record.getFileName());
                
                CompressionService.CompressionResult compressionResult = compressionService.compressFile(
                    sourcePath, record.getCompressionType(), targetPath.getParent().toString());
                
                // Update record with compression details
                record.setOriginalFileSize(Files.size(sourcePath));
                record.setCompressedFileSize(Files.size(compressionResult.getCompressedFile()));
                record.setCompressionRatio(compressionResult.getCompressionRatio());
                record.setCompressionTimeMs(compressionResult.getCompressionTimeMs());
                record.setCompressedFilePath(compressionResult.getCompressedFile().toString());
                
                actualSourcePath = compressionResult.getCompressedFile();
                actualTargetPath = targetPath.getParent().resolve(compressionResult.getCompressedFile().getFileName());
            }
            
            // Handle decompression for inbound files
            if (record.getDirection() == TransferDirection.INBOUND && 
                CompressionType.fromFileExtension(sourcePath.getFileName().toString()) != CompressionType.NONE) {
                
                logger.info("Decompressing inbound file: {}", record.getFileName());
                
                CompressionService.DecompressionResult decompressionResult = compressionService.decompressFile(
                    sourcePath, targetPath.getParent().toString());
                
                // Update record with decompression details
                record.setCompressionEnabled(true);
                record.setCompressionType(decompressionResult.getCompressionType());
                record.setCompressedFileSize(Files.size(sourcePath));
                record.setOriginalFileSize(Files.size(decompressionResult.getDecompressedFile()));
                record.setDecompressionTimeMs(decompressionResult.getDecompressionTimeMs());
                record.setCompressionRatio((float) record.getCompressedFileSize() / record.getOriginalFileSize());
                
                actualSourcePath = decompressionResult.getDecompressedFile();
                actualTargetPath = targetPath;
            }
            
            // Copy the file (compressed or decompressed as needed)
            if (!actualSourcePath.equals(actualTargetPath)) {
                Files.copy(actualSourcePath, actualTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Verify the transfer
            long expectedSize = record.getDirection() == TransferDirection.OUTBOUND && record.getCompressionEnabled() 
                              ? record.getCompressedFileSize() 
                              : record.getOriginalFileSize() != null ? record.getOriginalFileSize() : record.getFileSize();
            
            if (Files.exists(actualTargetPath) && Files.size(actualTargetPath) == expectedSize) {
                record.setStatus(TransferStatus.COMPLETED);
                logger.info("File transfer completed successfully: {}", record.getFileName());
                
                // Log completion with service details from database
                logTransferCompletion(record);
                
                // Auto-generate ACK for completed inbound files
                if (record.getDirection() == TransferDirection.INBOUND) {
                    try {
                        ackNackService.generateAckForInboundFile(record.getId());
                        logger.info("Auto-generated ACK for completed inbound file: {}", record.getFileName());
                    } catch (Exception e) {
                        logger.error("Failed to auto-generate ACK for file {}: {}", record.getFileName(), e.getMessage());
                    }
                }
            } else {
                throw new IOException("File verification failed after transfer");
            }
            
        } catch (IOException e) {
            logger.error("File transfer failed for {}: {}", record.getFileName(), e.getMessage());
            handleTransferFailure(record, e.getMessage());
        }
        
        fileTransferRepository.save(record);
    }
    
    private void handleTransferFailure(FileTransferRecord record, String errorMessage) {
        record.setStatus(TransferStatus.FAILED);
        record.setErrorMessage(errorMessage);
        record.setProcessedAt(LocalDateTime.now());
        fileTransferRepository.save(record);
        
        // Auto-generate NACK for failed inbound files
        if (record.getDirection() == TransferDirection.INBOUND) {
            try {
                ackNackService.generateNackForInboundFile(record.getId(), "PROCESSING_FAILED", errorMessage);
                logger.info("Auto-generated NACK for failed inbound file: {}", record.getFileName());
            } catch (Exception e) {
                logger.error("Failed to auto-generate NACK for file {}: {}", record.getFileName(), e.getMessage());
            }
        }
    }
    
    public void retryTransfer(Long recordId) {
        FileTransferRecord record = fileTransferRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("File transfer record not found: " + recordId));
        
        if (record.getStatus() == TransferStatus.FAILED || record.getStatus() == TransferStatus.CANCELLED) {
            record.setStatus(TransferStatus.PENDING);
            record.setErrorMessage(null);
            fileTransferRepository.save(record);
            logger.info("Retrying file transfer for record ID: {}", recordId);
        } else {
            throw new RuntimeException("Cannot retry transfer with status: " + record.getStatus());
        }
    }
    
    public void cancelTransfer(Long recordId) {
        FileTransferRecord record = fileTransferRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("File transfer record not found: " + recordId));
        
        if (record.getStatus() == TransferStatus.PENDING || record.getStatus() == TransferStatus.FAILED) {
            record.setStatus(TransferStatus.CANCELLED);
            record.setProcessedAt(LocalDateTime.now());
            fileTransferRepository.save(record);
            logger.info("Cancelled file transfer for record ID: {}", recordId);
        } else {
            throw new RuntimeException("Cannot cancel transfer with status: " + record.getStatus());
        }
    }
    
    /**
     * Log transfer completion with service configuration details
     */
    private void logTransferCompletion(FileTransferRecord record) {
        try {
            // Get service configuration using tenant and service information
            String tenantId = record.getTenantId();
            String serviceName = record.getServiceType();
            String subServiceType = record.getSubServiceType();
            
            ServiceConfiguration config = serviceConfigurationRepository
                .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceType)
                .orElse(null);
            
            if (config != null) {
                logger.info("Transfer completed - File: '{}', Service: '{}', Tenant: '{}', Sub-service: '{}', Description: '{}'",
                           record.getFileName(), 
                           config.getServiceName(), 
                           config.getTenantId(),
                           config.getSubServiceName() != null ? config.getSubServiceName() : "N/A",
                           config.getDescription() != null ? config.getDescription() : "N/A");
            } else {
                logger.warn("Could not find service configuration for completed transfer - Tenant: '{}', Service: '{}', Sub-service: '{}'",
                           tenantId, serviceName, subServiceType);
            }
        } catch (Exception e) {
            logger.warn("Error logging transfer completion details: {}", e.getMessage());
        }
    }
}