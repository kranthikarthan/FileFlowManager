package com.filetransfer.batch.writer;

import com.filetransfer.batch.entity.AckNackRecord;
import com.filetransfer.batch.entity.AckNackStatus;
import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.repository.AckNackRecordRepository;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

/**
 * Batch writer for processed ACK/NACK records
 */
@Component
public class AckNackFileWriter implements ItemWriter<AckNackRecord> {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackFileWriter.class);
    
    @Autowired
    private AckNackRecordRepository ackNackRepository;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Value("${file-transfer.ack-nack.incoming-path:/data/incoming/ack-nack}")
    private String incomingAckNackPath;
    
    @Override
    public void write(Chunk<? extends AckNackRecord> chunk) throws Exception {
        for (AckNackRecord ackNackRecord : chunk) {
            try {
                // Save the ACK/NACK record
                ackNackRecord.setProcessedAt(LocalDateTime.now());
                ackNackRecord.setStatus(AckNackStatus.PROCESSED);
                ackNackRepository.save(ackNackRecord);
                
                // Update the original file transfer record with ACK/NACK information
                updateOriginalFileTransfer(ackNackRecord);
                
                // Move the processed file to processed directory
                moveProcessedFile(ackNackRecord);
                
                logger.info("Successfully wrote ACK/NACK record for file: {}", ackNackRecord.getAckNackFileName());
                
            } catch (Exception e) {
                logger.error("Failed to write ACK/NACK record for file {}: {}", 
                           ackNackRecord.getAckNackFileName(), e.getMessage());
                
                // Mark as failed and save
                ackNackRecord.setStatus(AckNackStatus.FAILED);
                ackNackRecord.setErrorMessage(e.getMessage());
                ackNackRecord.setProcessedAt(LocalDateTime.now());
                ackNackRepository.save(ackNackRecord);
                
                // Move to error directory
                moveErrorFile(ackNackRecord, e.getMessage());
                
                throw e; // Re-throw to let batch framework handle
            }
        }
    }
    
    private void updateOriginalFileTransfer(AckNackRecord ackNackRecord) {
        try {
            FileTransferRecord fileTransfer = fileTransferRepository
                .findById(ackNackRecord.getOriginalFileTransferId())
                .orElse(null);
            
            if (fileTransfer != null) {
                String metadata = fileTransfer.getMetadata();
                
                if (ackNackRecord.getType() == AckNackType.ACK) {
                    metadata = updateMetadata(metadata, "ack_received", "true");
                    metadata = updateMetadata(metadata, "ack_received_at", LocalDateTime.now().toString());
                } else {
                    metadata = updateMetadata(metadata, "nack_received", "true");
                    metadata = updateMetadata(metadata, "nack_received_at", LocalDateTime.now().toString());
                    metadata = updateMetadata(metadata, "nack_reason", ackNackRecord.getReasonDescription());
                }
                
                fileTransfer.setMetadata(metadata);
                fileTransferRepository.save(fileTransfer);
                
                logger.info("Updated original file transfer {} with {} information", 
                           fileTransfer.getId(), ackNackRecord.getType());
            }
            
        } catch (Exception e) {
            logger.error("Failed to update original file transfer for ACK/NACK {}: {}", 
                        ackNackRecord.getId(), e.getMessage());
        }
    }
    
    private void moveProcessedFile(AckNackRecord ackNackRecord) throws IOException {
        Path sourcePath = Paths.get(ackNackRecord.getAckNackFilePath());
        if (!Files.exists(sourcePath)) {
            logger.warn("ACK/NACK file not found for moving: {}", sourcePath);
            return;
        }
        
        Path processedDir = Paths.get(incomingAckNackPath, "processed");
        Files.createDirectories(processedDir);
        
        Path targetPath = processedDir.resolve(sourcePath.getFileName());
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Update the path in the record
        ackNackRecord.setAckNackFilePath(targetPath.toString());
        
        logger.info("Moved processed ACK/NACK file: {} -> {}", sourcePath, targetPath);
    }
    
    private void moveErrorFile(AckNackRecord ackNackRecord, String errorMessage) {
        try {
            Path sourcePath = Paths.get(ackNackRecord.getAckNackFilePath());
            if (!Files.exists(sourcePath)) {
                return;
            }
            
            Path errorDir = Paths.get(incomingAckNackPath, "error");
            Files.createDirectories(errorDir);
            
            Path targetPath = errorDir.resolve(sourcePath.getFileName());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Write error details to a companion file
            Path errorDetailsPath = errorDir.resolve(sourcePath.getFileName() + ".error");
            String errorDetails = "Error: " + errorMessage + "\nTimestamp: " + LocalDateTime.now();
            Files.write(errorDetailsPath, errorDetails.getBytes(), 
                       StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            
            logger.info("Moved error ACK/NACK file: {} -> {}", sourcePath, targetPath);
            
        } catch (IOException e) {
            logger.error("Failed to move error ACK/NACK file: {}", e.getMessage());
        }
    }
    
    private String updateMetadata(String existingMetadata, String key, String value) {
        if (existingMetadata == null) {
            existingMetadata = "";
        }
        return existingMetadata + (existingMetadata.isEmpty() ? "" : ";") + key + "=" + value;
    }
    
    private AckNackFileInfo parseAckNackFile(String content, String fileName) {
        String[] parts = content.trim().split("\\|");
        
        if (parts.length < 6) {
            throw new RuntimeException("Invalid ACK/NACK file format: " + fileName);
        }
        
        AckNackFileInfo info = new AckNackFileInfo();
        info.setType(AckNackType.valueOf(parts[0]));
        info.setOriginalFileName(parts[1]);
        info.setServiceName(parts[2]);
        info.setSubServiceName(parts[3].isEmpty() ? null : parts[3]);
        info.setTimestamp(parts[4]);
        info.setStatus(parts[5]);
        
        if (parts.length > 6 && info.getType() == AckNackType.NACK) {
            info.setReasonCode(parts[6]);
            if (parts.length > 7) {
                info.setReasonDescription(parts[7]);
            }
        }
        
        return info;
    }
    
    private Optional<FileTransferRecord> findOriginalFileTransfer(String fileName, String tenantId, String serviceName, String subServiceName) {
        List<FileTransferRecord> candidates = fileTransferRepository.findByTenantIdAndFileName(tenantId, fileName);
        
        return candidates.stream()
            .filter(record -> record.getServiceType().equals(serviceName))
            .filter(record -> (subServiceName == null && record.getSubServiceType() == null) || 
                             (subServiceName != null && subServiceName.equals(record.getSubServiceType())))
            .filter(record -> record.getDirection() == TransferDirection.OUTBOUND)
            .findFirst();
    }
    
    // Helper class for parsing ACK/NACK files
    public static class AckNackFileInfo {
        private AckNackType type;
        private String originalFileName;
        private String serviceName;
        private String subServiceName;
        private String timestamp;
        private String status;
        private String reasonCode;
        private String reasonDescription;
        
        // Getters and setters
        public AckNackType getType() { return type; }
        public void setType(AckNackType type) { this.type = type; }
        
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getSubServiceName() { return subServiceName; }
        public void setSubServiceName(String subServiceName) { this.subServiceName = subServiceName; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
        
        public String getReasonDescription() { return reasonDescription; }
        public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    }
}