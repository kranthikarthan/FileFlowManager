package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.TransferDirection;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class FileMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);
    
    @Autowired
    private FileTransferConfig fileTransferConfig;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private FileTransferService fileTransferService;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Autowired
    private CutOffTimeService cutOffTimeService;
    
    @Autowired
    private HolidayService holidayService;
    
    @Autowired
    private FileValidationService fileValidationService;
    
    @Scheduled(fixedDelayString = "${file-transfer.poll-interval-seconds:30}000")
    public void monitorInboundFiles() {
        if (!fileTransferConfig.isEnabled()) {
            return;
        }
        
        logger.debug("Starting file monitoring cycle");
        
        // Process all enabled services from database (tenant-aware)
        List<String> activeTenantIds = serviceConfigurationRepository.findAllActiveTenantIds();
        
        for (String tenantId : activeTenantIds) {
            try {
                processTenantServices(tenantId);
            } catch (Exception e) {
                logger.error("Error processing services for tenant: {}", tenantId, e);
            }
        }
    }
    
    /**
     * Process all enabled services for a specific tenant
     */
    private void processTenantServices(String tenantId) {
        List<ServiceConfiguration> enabledServices = serviceConfigurationRepository.findAllEnabledServicesForTenant(tenantId);
        
        for (ServiceConfiguration config : enabledServices) {
            try {
                // Check if processing should be skipped due to holidays
                LocalDate today = LocalDate.now();
                if (holidayService.shouldSkipProcessing(tenantId, today, config.getAllSundaysAsHolidays())) {
                    logger.info("Skipping processing for service '{}' (tenant: {}) - Holiday detected", 
                               config.getServiceName(), tenantId);
                    continue;
                }
                
                // Check cut-off time
                if (!cutOffTimeService.isCurrentTimeBeforeCutOff(config, today)) {
                    logger.info("Skipping processing for service '{}' (tenant: {}) - Past cut-off time", 
                               config.getServiceName(), tenantId);
                    continue;
                }
                
                monitorServiceInboundFiles(config);
            } catch (Exception e) {
                logger.error("Error monitoring files for service: {} (tenant: {})", 
                           config.getServiceName(), tenantId, e);
            }
        }
    }
    
    private void monitorServiceInboundFiles(ServiceConfiguration config) {
        Path inboundPath = Paths.get(config.getInboundPath());
        
        if (!Files.exists(inboundPath)) {
            logger.warn("Inbound path does not exist for service {} (tenant: {}): {}", 
                       config.getServiceName(), config.getTenantId(), inboundPath);
            return;
        }
        
        try {
            // Check for start of transmission files
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(config.getStartMarkerPrefix()))
                .filter(path -> validateFile(path.getFileName().toString(), config))
                .forEach(startMarkerPath -> processStartMarker(config, startMarkerPath));
            
            // Check for end of transmission files
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(config.getEndMarkerPrefix()))
                .filter(path -> validateFile(path.getFileName().toString(), config))
                .forEach(endMarkerPath -> processEndMarker(config, endMarkerPath));
                
        } catch (IOException e) {
            logger.error("Error listing files in inbound directory for service {} (tenant: {}): {}", 
                        config.getServiceName(), config.getTenantId(), e.getMessage());
        }
    }
    
    /**
     * Validate file using the service configuration regex patterns
     */
    private boolean validateFile(String fileName, ServiceConfiguration config) {
        FileValidationService.ValidationResult result = fileValidationService.validateFileWithDetails(fileName, config);
        
        if (!result.isValid()) {
            logger.warn("File '{}' failed validation for service '{}' (tenant: {}): {}", 
                       fileName, config.getServiceName(), config.getTenantId(), result);
            return false;
        }
        
        logger.debug("File '{}' passed validation for service '{}' (tenant: {})", 
                    fileName, config.getServiceName(), config.getTenantId());
        return true;
    }
    
    private void processStartMarker(ServiceConfiguration config, Path startMarkerPath) {
        String startMarkerName = startMarkerPath.getFileName().toString();
        String baseFileName = startMarkerName.substring(config.getStartMarkerPrefix().length());
        
        logger.info("Found start marker for service {} (tenant: {}): {}", 
                   config.getServiceName(), config.getTenantId(), startMarkerName);
        
        // Look for corresponding data files
        Path inboundPath = startMarkerPath.getParent();
        
        try {
            Files.list(inboundPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains(baseFileName))
                .filter(path -> !path.getFileName().toString().startsWith(config.getStartMarkerPrefix()))
                .filter(path -> !path.getFileName().toString().startsWith(config.getEndMarkerPrefix()))
                .filter(path -> validateFile(path.getFileName().toString(), config))
                .forEach(dataFilePath -> {
                    String dataFileName = dataFilePath.getFileName().toString();
                    
                    // Check if this file is already being tracked
                    List<FileTransferRecord> existingRecords = fileTransferRepository.findByFileName(dataFileName);
                    if (existingRecords.isEmpty()) {
                        createFileTransferRecord(config, dataFilePath, TransferDirection.INBOUND);
                    }
                });
                
        } catch (IOException e) {
            logger.error("Error processing start marker for service {} (tenant: {}): {}", 
                        config.getServiceName(), config.getTenantId(), e.getMessage());
        }
    }
    
    private void processEndMarker(ServiceConfiguration config, Path endMarkerPath) {
        String endMarkerName = endMarkerPath.getFileName().toString();
        String baseFileName = endMarkerName.substring(config.getEndMarkerPrefix().length());
        
        logger.info("Found end marker for service {} (tenant: {}): {}", 
                   config.getServiceName(), config.getTenantId(), endMarkerName);
        
        // Find files waiting for end marker for this specific service
        String serviceKey = buildServiceKey(config);
        List<FileTransferRecord> waitingFiles = fileTransferRepository
            .findByServiceTypeAndStatus(serviceKey, TransferStatus.WAITING_FOR_END_MARKER);
        
        waitingFiles.stream()
            .filter(record -> record.getFileName().contains(baseFileName))
            .forEach(record -> {
                record.setStatus(TransferStatus.PENDING);
                fileTransferRepository.save(record);
                logger.info("File {} ready for transfer after end marker (service: {}, tenant: {})", 
                           record.getFileName(), config.getServiceName(), config.getTenantId());
            });
    }
    
    private void createFileTransferRecord(ServiceConfiguration config, Path filePath, TransferDirection direction) {
        try {
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            String checksum = calculateChecksum(filePath);
            
            String serviceKey = buildServiceKey(config);
            
            FileTransferRecord record = new FileTransferRecord(
                fileName, 
                serviceKey, 
                filePath.toString(), 
                Paths.get(config.getOutboundPath(), fileName).toString(),
                direction
            );
            
            record.setFileSize(fileSize);
            record.setChecksum(checksum);
            record.setStatus(TransferStatus.WAITING_FOR_END_MARKER);
            
            fileTransferRepository.save(record);
            logger.info("Created transfer record for file: {} (service: {}, tenant: {})", 
                       fileName, config.getServiceName(), config.getTenantId());
            
        } catch (Exception e) {
            logger.error("Error creating transfer record for file: {} (service: {}, tenant: {})", 
                        filePath, config.getServiceName(), config.getTenantId(), e);
        }
    }
    
    /**
     * Build a unique service key that includes tenant, service, and sub-service
     */
    private String buildServiceKey(ServiceConfiguration config) {
        StringBuilder key = new StringBuilder();
        key.append(config.getTenantId()).append(":");
        key.append(config.getServiceName());
        if (config.getSubServiceName() != null && !config.getSubServiceName().trim().isEmpty()) {
            key.append(":").append(config.getSubServiceName());
        }
        return key.toString();
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