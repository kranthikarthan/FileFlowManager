package com.filetransfer.batch.processor;

import com.filetransfer.batch.entity.*;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Batch processor for ACK/NACK files
 */
@Component
public class AckNackFileProcessor implements ItemProcessor<Path, AckNackRecord> {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackFileProcessor.class);
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Value("${file-transfer.ack-nack.incoming-path:/data/incoming/ack-nack}")
    private String incomingAckNackPath;
    
    @Override
    public AckNackRecord process(Path filePath) throws Exception {
        try {
            String fileName = filePath.getFileName().toString();
            String content = Files.readString(filePath);
            
            logger.info("Processing ACK/NACK file: {}", fileName);
            
            // Parse the ACK/NACK file
            AckNackFileInfo ackNackInfo = parseAckNackFile(content, fileName);
            
            // Determine tenant from file path
            String tenantId = extractTenantFromPath(filePath);
            
            // Find the original file transfer record
            Optional<FileTransferRecord> originalTransfer = findOriginalFileTransfer(
                ackNackInfo.getOriginalFileName(), tenantId, ackNackInfo.getServiceName(), ackNackInfo.getSubServiceName());
            
            if (originalTransfer.isEmpty()) {
                logger.warn("Original file transfer not found for ACK/NACK: {}", fileName);
                throw new RuntimeException("Original file transfer not found for ACK/NACK: " + fileName);
            }
            
            FileTransferRecord fileTransfer = originalTransfer.get();
            
            AckNackRecord ackNackRecord = new AckNackRecord(
                fileTransfer.getId(),
                fileTransfer.getFileName(),
                tenantId,
                fileTransfer.getServiceType(),
                fileTransfer.getSubServiceType(),
                ackNackInfo.getType(),
                TransferDirection.INBOUND // Received ACK/NACK is inbound
            );
            
            ackNackRecord.setAckNackFileName(fileName);
            ackNackRecord.setAckNackFilePath(filePath.toString());
            ackNackRecord.setContent(content);
            ackNackRecord.setFileSize(Files.size(filePath));
            ackNackRecord.setChecksum(calculateChecksum(Files.readAllBytes(filePath)));
            ackNackRecord.setReceivedAt(LocalDateTime.now());
            ackNackRecord.setStatus(AckNackStatus.RECEIVED);
            
            if (ackNackInfo.getType() == AckNackType.NACK) {
                ackNackRecord.setReasonCode(ackNackInfo.getReasonCode());
                ackNackRecord.setReasonDescription(ackNackInfo.getReasonDescription());
            }
            
            logger.info("Successfully processed {} file: {}", ackNackInfo.getType(), fileName);
            return ackNackRecord;
            
        } catch (Exception e) {
            logger.error("Failed to process ACK/NACK file {}: {}", filePath, e.getMessage());
            throw e; // Re-throw to let batch framework handle the error
        }
    }
    
    private String extractTenantFromPath(Path filePath) {
        // Extract tenant from file path structure: /data/incoming/ack-nack/{tenantId}/...
        Path incomingPath = Path.of(incomingAckNackPath);
        Path relativePath = incomingPath.relativize(filePath);
        if (relativePath.getNameCount() > 0) {
            return relativePath.getName(0).toString();
        }
        return "default"; // Fallback tenant
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
    
    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.warn("Failed to calculate checksum: {}", e.getMessage());
            return null;
        }
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