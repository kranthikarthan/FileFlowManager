package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.TransferStatus;
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
    
    @Scheduled(fixedDelayString = "${file-transfer.poll-interval-seconds:30}000")
    public void processPendingTransfers() {
        if (!fileTransferConfig.isEnabled()) {
            return;
        }
        
        List<FileTransferRecord> pendingTransfers = fileTransferRepository.findByStatus(TransferStatus.PENDING);
        
        for (FileTransferRecord record : pendingTransfers) {
            try {
                processFileTransfer(record);
            } catch (Exception e) {
                logger.error("Error processing file transfer for record ID {}: {}", record.getId(), e.getMessage());
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
            
            // Copy the file
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify the transfer
            if (Files.exists(targetPath) && Files.size(targetPath) == record.getFileSize()) {
                record.setStatus(TransferStatus.COMPLETED);
                logger.info("File transfer completed successfully: {}", record.getFileName());
                
                // Log completion with service details from database
                logTransferCompletion(record);
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
            // Parse service key to extract tenant and service information
            String[] parts = record.getServiceType().split(":");
            if (parts.length >= 2) {
                String tenantId = parts[0];
                String serviceName = parts[1];
                String subServiceName = parts.length > 2 ? parts[2] : null;
                
                ServiceConfiguration config = serviceConfigurationRepository
                    .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName)
                    .orElse(null);
                
                if (config != null) {
                    logger.info("Transfer completed - File: '{}', Service: '{}', Tenant: '{}', Sub-service: '{}', Description: '{}'",
                               record.getFileName(), 
                               config.getServiceName(), 
                               config.getTenantId(),
                               config.getSubServiceName() != null ? config.getSubServiceName() : "N/A",
                               config.getDescription() != null ? config.getDescription() : "N/A");
                } else {
                    logger.warn("Could not find service configuration for completed transfer: {}", record.getServiceType());
                }
            }
        } catch (Exception e) {
            logger.warn("Error logging transfer completion details: {}", e.getMessage());
        }
    }
}