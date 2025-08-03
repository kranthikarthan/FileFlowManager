package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.TransferDirection;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FileMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);
    
    @Autowired
    private FileTransferConfig fileTransferConfig;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private FileTransferService fileTransferService;
    
    @Scheduled(fixedDelayString = "${file-transfer.poll-interval-seconds:30}000")
    public void monitorInboundFiles() {
        if (!fileTransferConfig.isEnabled()) {
            return;
        }
        
        logger.debug("Starting file monitoring cycle");
        
        for (Map.Entry<String, FileTransferConfig.ServiceConfig> entry : 
             fileTransferConfig.getServices().entrySet()) {
            
            String serviceType = entry.getKey();
            FileTransferConfig.ServiceConfig config = entry.getValue();
            
            if (!config.isEnabled()) {
                continue;
            }
            
            try {
                monitorServiceInboundFiles(serviceType, config);
            } catch (Exception e) {
                logger.error("Error monitoring files for service: {}", serviceType, e);
            }
        }
    }
    
    private void monitorServiceInboundFiles(String serviceType, FileTransferConfig.ServiceConfig config) {
        Path inboundPath = Paths.get(config.getInboundPath());
        
        if (!Files.exists(inboundPath)) {
            logger.warn("Inbound path does not exist for service {}: {}", serviceType, inboundPath);
            return;
        }
        
        try {
            // Check for start of transmission files
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(config.getStartMarkerPrefix()))
                .forEach(startMarkerPath -> processStartMarker(serviceType, config, startMarkerPath));
            
            // Check for end of transmission files
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(config.getEndMarkerPrefix()))
                .forEach(endMarkerPath -> processEndMarker(serviceType, config, endMarkerPath));
                
        } catch (IOException e) {
            logger.error("Error listing files in inbound directory for service {}: {}", serviceType, e.getMessage());
        }
    }
    
    private void processStartMarker(String serviceType, FileTransferConfig.ServiceConfig config, Path startMarkerPath) {
        String startMarkerName = startMarkerPath.getFileName().toString();
        String baseFileName = startMarkerName.substring(config.getStartMarkerPrefix().length());
        
        logger.info("Found start marker for service {}: {}", serviceType, startMarkerName);
        
        // Look for corresponding data files
        Path inboundPath = startMarkerPath.getParent();
        
        try {
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains(baseFileName))
                .filter(path -> !path.getFileName().toString().startsWith(config.getStartMarkerPrefix()))
                .filter(path -> !path.getFileName().toString().startsWith(config.getEndMarkerPrefix()))
                .forEach(dataFilePath -> {
                    String dataFileName = dataFilePath.getFileName().toString();
                    
                    // Check if this file is already being tracked
                    List<FileTransferRecord> existingRecords = fileTransferRepository.findByFileName(dataFileName);
                    if (existingRecords.isEmpty()) {
                        createFileTransferRecord(serviceType, config, dataFilePath, TransferDirection.INBOUND);
                    }
                });
                
        } catch (IOException e) {
            logger.error("Error processing start marker for service {}: {}", serviceType, e.getMessage());
        }
    }
    
    private void processEndMarker(String serviceType, FileTransferConfig.ServiceConfig config, Path endMarkerPath) {
        String endMarkerName = endMarkerPath.getFileName().toString();
        String baseFileName = endMarkerName.substring(config.getEndMarkerPrefix().length());
        
        logger.info("Found end marker for service {}: {}", serviceType, endMarkerName);
        
        // Find files waiting for end marker
        List<FileTransferRecord> waitingFiles = fileTransferRepository
            .findByServiceTypeAndStatus(serviceType, TransferStatus.WAITING_FOR_END_MARKER);
        
        waitingFiles.stream()
            .filter(record -> record.getFileName().contains(baseFileName))
            .forEach(record -> {
                record.setStatus(TransferStatus.PENDING);
                fileTransferRepository.save(record);
                logger.info("File {} ready for transfer after end marker", record.getFileName());
            });
    }
    
    private void createFileTransferRecord(String serviceType, FileTransferConfig.ServiceConfig config, 
                                        Path filePath, TransferDirection direction) {
        try {
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            String checksum = calculateChecksum(filePath);
            
            FileTransferRecord record = new FileTransferRecord(
                fileName, 
                serviceType, 
                filePath.toString(), 
                Paths.get(config.getOutboundPath(), fileName).toString(),
                direction
            );
            
            record.setFileSize(fileSize);
            record.setChecksum(checksum);
            record.setStatus(TransferStatus.WAITING_FOR_END_MARKER);
            
            fileTransferRepository.save(record);
            logger.info("Created transfer record for file: {} (service: {})", fileName, serviceType);
            
        } catch (Exception e) {
            logger.error("Error creating transfer record for file: {}", filePath, e);
        }
    }
    
    private String calculateChecksum(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] digest = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Error calculating checksum for file: {}", filePath, e);
            return null;
        }
    }
}