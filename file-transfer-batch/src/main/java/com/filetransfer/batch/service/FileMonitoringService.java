package com.filetransfer.batch.service;

import com.filetransfer.batch.config.FileTransferConfig;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.SubServiceConfiguration;
import com.filetransfer.batch.entity.TransferDirection;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.entity.FileType;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
import com.filetransfer.batch.repository.SubServiceConfigurationRepository;
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
    private SubServiceConfigurationRepository subServiceConfigurationRepository;
    
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
                processTenantSubServices(tenantId); // Add subservice processing
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
                    
                    // Check if this file is already being tracked for this tenant
                    List<FileTransferRecord> existingRecords = fileTransferRepository.findByTenantIdAndFileName(config.getTenantId(), dataFileName);
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
        
        // Find files waiting for end marker for this specific tenant and service
        List<FileTransferRecord> waitingFiles = fileTransferRepository
            .findByTenantIdAndServiceTypeAndStatus(config.getTenantId(), config.getServiceName(), TransferStatus.WAITING_FOR_END_MARKER);
        
        waitingFiles.stream()
            .filter(record -> record.getFileName().contains(baseFileName))
            .forEach(record -> {
                record.setStatus(TransferStatus.PENDING);
                fileTransferRepository.save(record);
                logger.info("File {} ready for transfer after end marker (service: {}, tenant: {})", 
                           record.getFileName(), config.getServiceName(), config.getTenantId());
                
                // Trigger immediate file transfer processing
                try {
                    fileTransferService.processFileTransfer(record);
                } catch (Exception e) {
                    logger.error("Error processing file transfer for record {}: {}", 
                               record.getId(), e.getMessage());
                }
            });
    }
    
    private void createFileTransferRecord(ServiceConfiguration config, Path filePath, TransferDirection direction) {
        try {
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            String checksum = calculateChecksum(filePath);
            
            FileTransferRecord record = new FileTransferRecord(
                config.getTenantId(),
                fileName, 
                config.getServiceName(), 
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
    
    /**
     * Process all enabled subservices for a specific tenant
     */
    private void processTenantSubServices(String tenantId) {
        List<SubServiceConfiguration> enabledSubServices = subServiceConfigurationRepository.findByTenantIdAndEnabled(tenantId, true);
        
        for (SubServiceConfiguration config : enabledSubServices) {
            try {
                // Check if processing should be skipped due to holidays
                LocalDate today = LocalDate.now();
                if (holidayService.shouldSkipProcessing(tenantId, today, config.getAllSundaysAsHolidays())) {
                    logger.debug("Skipping processing for subservice {}/{} on {} due to holiday", 
                               config.getServiceName(), config.getSubServiceName(), today);
                    continue;
                }
                
                // Check cut-off time for this subservice
                LocalTime cutOffTime = cutOffTimeService.getCutOffTimeForDate(config, today);
                LocalTime currentTime = LocalTime.now();
                
                if (currentTime.isBefore(cutOffTime)) {
                    // Normal processing window
                    processSubServiceInboundFiles(config);
                } else {
                    logger.debug("Current time {} is after cut-off time {} for subservice {}/{}", 
                               currentTime, cutOffTime, config.getServiceName(), config.getSubServiceName());
                }
                
            } catch (Exception e) {
                logger.error("Error processing subservice {}/{} for tenant {}: {}", 
                           config.getServiceName(), config.getSubServiceName(), tenantId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Process inbound files for a specific subservice
     */
    private void processSubServiceInboundFiles(SubServiceConfiguration config) {
        String inboundPath = config.getInboundPath();
        Path directoryPath = Paths.get(inboundPath);
        
        if (!Files.exists(directoryPath)) {
            logger.warn("Inbound directory does not exist for subservice {}/{}: {}", 
                       config.getServiceName(), config.getSubServiceName(), inboundPath);
            return;
        }
        
        try {
            List<Path> files = Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().startsWith("."))
                .collect(java.util.stream.Collectors.toList());
            
            for (Path file : files) {
                processSubServiceFile(config, file);
            }
            
        } catch (IOException e) {
            logger.error("Error reading inbound directory for subservice {}/{}: {}", 
                        config.getServiceName(), config.getSubServiceName(), e.getMessage(), e);
        }
    }
    
    /**
     * Process a single file for a subservice
     */
    private void processSubServiceFile(SubServiceConfiguration config, Path filePath) {
        String fileName = filePath.getFileName().toString();
        
        try {
            // Detect file type
            byte[] content = Files.readAllBytes(filePath);
            FileType fileType = FileType.detectFromContent(new String(content), fileName);
            
            // Create file transfer record
            FileTransferRecord record = new FileTransferRecord(
                fileName,
                config.getServiceName(),
                config.getSubServiceName(),
                config.getTenantId(),
                filePath.toString(),
                config.getOutboundPath() + "/" + fileName,
                TransferDirection.INBOUND,
                fileType
            );
            
            // Set file metadata
            record.setFileSize(Files.size(filePath));
            record.setChecksum(calculateChecksum(filePath.toString()));
            
            // Validate file based on type and configuration
            if (config.getSchemaValidationEnabled() && fileType.requiresSchemaValidation()) {
                boolean validationPassed = validateSubServiceFile(config, fileName, content, fileType);
                record.setSchemaValidationPassed(validationPassed);
                
                if (!validationPassed && "STRICT".equals(config.getSchemaValidationMode())) {
                    record.setStatus(TransferStatus.FAILED);
                    record.setErrorMessage("Schema validation failed in STRICT mode");
                }
            }
            
            // Save record
            fileTransferRepository.save(record);
            
            // Transfer file if validation passed
            if (record.getStatus() != TransferStatus.FAILED) {
                fileTransferService.transferFile(record);
            }
            
            logger.info("Processed file {} for subservice {}/{}", fileName, 
                       config.getServiceName(), config.getSubServiceName());
            
        } catch (Exception e) {
            logger.error("Error processing file {} for subservice {}/{}: {}", 
                        fileName, config.getServiceName(), config.getSubServiceName(), e.getMessage(), e);
            
            // Create failed record
            FileTransferRecord failedRecord = new FileTransferRecord(
                fileName,
                config.getServiceName(), 
                config.getSubServiceName(),
                config.getTenantId(),
                filePath.toString(),
                config.getOutboundPath() + "/" + fileName,
                TransferDirection.INBOUND
            );
            failedRecord.setStatus(TransferStatus.FAILED);
            failedRecord.setErrorMessage(e.getMessage());
            fileTransferRepository.save(failedRecord);
        }
    }
    
    /**
     * Validate file for subservice
     */
    private boolean validateSubServiceFile(SubServiceConfiguration config, String fileName, 
                                         byte[] content, FileType fileType) {
        try {
            // Binary file bypass
            if (fileType == FileType.BINARY_FILE && config.getBinaryFileBypass()) {
                return true;
            }
            
            // File pattern validation
            if (!fileName.matches(config.getDataFilePattern().replace("*", ".*"))) {
                logger.warn("File {} does not match pattern {} for subservice {}/{}", 
                           fileName, config.getDataFilePattern(), 
                           config.getServiceName(), config.getSubServiceName());
                return false;
            }
            
            // TODO: Add schema validation once FileTypeSchemaMapping integration is complete
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating file {} for subservice {}/{}: {}", 
                        fileName, config.getServiceName(), config.getSubServiceName(), e.getMessage());
            return false;
        }
    }
}