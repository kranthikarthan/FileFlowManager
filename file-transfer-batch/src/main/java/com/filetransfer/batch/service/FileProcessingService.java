package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import com.filetransfer.batch.service.FileProcessingService.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FileProcessingService {
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRecordRepository;
    
    @Autowired
    private SchemaValidationService schemaValidationService;
    
    public void processFiles() {
        List<ServiceConfiguration> activeServices = serviceConfigurationRepository.findByEnabledTrue();
        
        for (ServiceConfiguration service : activeServices) {
            processServiceFiles(service);
        }
    }
    
    private void processServiceFiles(ServiceConfiguration service) {
        try {
            Path inboundPath = Paths.get(service.getInboundPath());
            
            if (!Files.exists(inboundPath)) {
                return;
            }
            
            // Process SOT files
            processFileType(service, "SOT", service.getStartMarkerPrefix());
            
            // Process EOT files
            processFileType(service, "EOT", service.getEndMarkerPrefix());
            
            // Process data files
            processFileType(service, "DATA", service.getDataFilePattern());
            
        } catch (Exception e) {
            // Log error and continue with other services
            System.err.println("Error processing service " + service.getServiceName() + ": " + e.getMessage());
        }
    }
    
    private void processFileType(ServiceConfiguration service, String fileType, String filePattern) {
        try {
            Path inboundPath = Paths.get(service.getInboundPath());
            Path outboundPath = Paths.get(service.getOutboundPath());
            
            // Create outbound directory if it doesn't exist
            Files.createDirectories(outboundPath);
            
            // Find files matching the pattern
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inboundPath, filePattern)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        processFile(service, file, fileType);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing " + fileType + " files for service " + service.getServiceName() + ": " + e.getMessage());
        }
    }
    
    private void processFile(ServiceConfiguration service, Path file, String fileType) {
        FileTransferRecord record = new FileTransferRecord();
        record.setFileName(file.getFileName().toString());
        record.setServiceType(service.getServiceName());
        record.setSourcePath(file.toString());
        record.setStatus("IN_PROGRESS");
        record.setDirection("INBOUND");
        record.setCreatedAt(LocalDateTime.now());
        
        try {
            // Check file size
            long fileSize = Files.size(file);
            record.setFileSize(fileSize);
            
            // Calculate checksum
            String checksum = calculateChecksum(file);
            record.setChecksum(checksum);
            
            // Schema validation (if enabled)
            if (service.getSchemaValidationEnabled()) {
                ValidationResult validationResult = schemaValidationService.validateFile(
                    service.getTenantId(),
                    service.getServiceName(),
                    file.getFileName().toString(),
                    Files.newInputStream(file),
                    fileSize,
                    service.getBinaryFileBypass(),
                    fileType
                );
                
                if (!validationResult.isValid()) {
                    record.setStatus("FAILED");
                    record.setErrorMessage("Schema validation failed: " + validationResult.getMessage());
                    fileTransferRecordRepository.save(record);
                    return;
                }
            }
            
            // Track data files for EOT validation
            if ("DATA".equals(fileType)) {
                trackDataFile(service.getTenantId(), service.getServiceName(), file.getFileName().toString());
            }
            
            // Special handling for EOT files
            if ("EOT".equals(fileType) && service.getEotValidationEnabled()) {
                ValidationResult eotValidationResult = validateEotFile(service, file);
                if (!eotValidationResult.isValid()) {
                    record.setStatus("FAILED");
                    record.setErrorMessage("EOT validation failed: " + eotValidationResult.getMessage());
                    fileTransferRecordRepository.save(record);
                    return;
                }
            }
            
            // Move file to outbound directory
            Path targetPath = Paths.get(service.getOutboundPath(), file.getFileName().toString());
            Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            record.setTargetPath(targetPath.toString());
            record.setStatus("COMPLETED");
            record.setProcessedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMessage("Processing failed: " + e.getMessage());
        }
        
        fileTransferRecordRepository.save(record);
    }
    
    /**
     * Track data file for EOT validation
     */
    private void trackDataFile(String tenantId, String serviceType, String fileName) {
        // This would typically call the tracking service
        System.out.println("Tracking data file: " + fileName + " for EOT validation");
    }
    
    /**
     * Validate EOT file against expected data file count
     */
    private ValidationResult validateEotFile(ServiceConfiguration service, Path eotFile) {
        try {
            // Read EOT file content
            String eotContent = new String(Files.readAllBytes(eotFile));
            
            // Call web API for EOT validation
            ValidationResult result = schemaValidationService.validateEotFile(
                service.getTenantId(),
                service.getServiceName(),
                eotContent,
                service.getEotTotalFilesField()
            );
            
            return result;
            
        } catch (Exception e) {
            ValidationResult result = new ValidationResult(false, "Error validating EOT file: " + e.getMessage());
            return result;
        }
    }
    
    private String calculateChecksum(Path file) throws IOException {
        // Simple checksum calculation - in production, use a proper checksum algorithm
        try (InputStream is = Files.newInputStream(file)) {
            int checksum = 0;
            int b;
            while ((b = is.read()) != -1) {
                checksum = (checksum + b) & 0xFF;
            }
            return String.format("%08x", checksum);
        }
    }
    
    // Validation result class
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}