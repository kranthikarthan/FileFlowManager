package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
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
                
                // Optionally delete source file (based on configuration)
                FileTransferConfig.ServiceConfig serviceConfig = 
                    fileTransferConfig.getServices().get(record.getServiceType());
                if (serviceConfig != null) {
                    // For now, we'll keep the source file for safety
                    logger.debug("Source file retained: {}", sourcePath);
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
}