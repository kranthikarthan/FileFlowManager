package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.*;
import com.filetransfer.batch.repository.AckNackRecordRepository;
import com.filetransfer.batch.repository.FileTransferRecordRepository;
import com.filetransfer.batch.repository.ServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Batch service for processing ACK/NACK files and automated acknowledgment handling
 */
@Service
@Transactional
public class AckNackService {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Autowired
    private AckNackRecordRepository ackNackRepository;
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;
    
    @Value("${file-transfer.ack-nack.base-path:/data/ack-nack}")
    private String ackNackBasePath;
    
    @Value("${file-transfer.ack-nack.incoming-path:/data/incoming/ack-nack}")
    private String incomingAckNackPath;
    
    @Value("${file-transfer.ack-nack.timeout-hours:24}")
    private int ackNackTimeoutHours;
    
    @Value("${file-transfer.ack-nack.auto-generate:true}")
    private boolean autoGenerateAckNack;
    
    /**
     * Scheduled job to process incoming ACK/NACK files
     */
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.poll-interval-seconds:60}000")
    public void processIncomingAckNackFiles() {
        try {
            Path incomingDir = Paths.get(incomingAckNackPath);
            if (!Files.exists(incomingDir)) {
                Files.createDirectories(incomingDir);
                return;
            }
            
            try (Stream<Path> files = Files.walk(incomingDir, 1)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> isAckNackFile(path.getFileName().toString()))
                     .forEach(this::processIncomingAckNackFile);
            }
            
        } catch (Exception e) {
            logger.error("Error processing incoming ACK/NACK files: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled job to generate ACK files for completed inbound transfers
     */
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.generation-interval-seconds:300}000")
    public void generatePendingAckFiles() {
        if (!autoGenerateAckNack) {
            return;
        }
        
        try {
            // Find completed inbound transfers without ACK/NACK
            List<FileTransferRecord> completedInboundTransfers = fileTransferRepository
                .findCompletedInboundTransfersWithoutAck();
            
            for (FileTransferRecord transfer : completedInboundTransfers) {
                try {
                    generateAckForInboundFile(transfer.getId());
                } catch (Exception e) {
                    logger.error("Failed to auto-generate ACK for transfer {}: {}", transfer.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in auto ACK generation: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled job to send pending ACK/NACK files
     */
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.send-interval-seconds:120}000")
    public void sendPendingAckNackFiles() {
        try {
            List<AckNackRecord> pendingRecords = ackNackRepository.findByStatus(AckNackStatus.GENERATED);
            
            for (AckNackRecord record : pendingRecords) {
                try {
                    sendAckNackFile(record);
                    record.setStatus(AckNackStatus.SENT);
                    record.setSentAt(LocalDateTime.now());
                    ackNackRepository.save(record);
                    
                    logger.info("Sent {} file: {}", record.getType(), record.getAckNackFileName());
                    
                } catch (Exception e) {
                    logger.error("Failed to send {} file {}: {}", record.getType(), record.getAckNackFileName(), e.getMessage());
                    record.setStatus(AckNackStatus.FAILED);
                    record.setErrorMessage(e.getMessage());
                    ackNackRepository.save(record);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error sending pending ACK/NACK files: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled job to mark expired ACK/NACK records
     */
    @Scheduled(fixedDelayString = "${file-transfer.ack-nack.cleanup-interval-seconds:3600}000")
    public void markExpiredRecords() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AckNackRecord> expiredRecords = ackNackRepository.findExpiredRecords(AckNackStatus.SENT, now);
            
            for (AckNackRecord record : expiredRecords) {
                record.setStatus(AckNackStatus.EXPIRED);
                ackNackRepository.save(record);
                logger.warn("Marked {} file as expired: {}", record.getType(), record.getAckNackFileName());
            }
            
        } catch (Exception e) {
            logger.error("Error marking expired ACK/NACK records: {}", e.getMessage());
        }
    }
    
    /**
     * Generate ACK file for a successfully processed inbound file
     */
    public AckNackRecord generateAckForInboundFile(Long fileTransferId) {
        FileTransferRecord fileTransfer = fileTransferRepository.findById(fileTransferId)
            .orElseThrow(() -> new RuntimeException("File transfer record not found: " + fileTransferId));
        
        if (fileTransfer.getDirection() != TransferDirection.INBOUND) {
            throw new IllegalArgumentException("ACK can only be generated for inbound files");
        }
        
        if (fileTransfer.getStatus() != TransferStatus.COMPLETED) {
            throw new IllegalArgumentException("ACK can only be generated for completed file transfers");
        }
        
        // Check if ACK already exists
        Optional<AckNackRecord> existingAck = ackNackRepository.findByOriginalFileTransferId(fileTransferId);
        if (existingAck.isPresent()) {
            logger.warn("ACK already exists for file transfer ID: {}", fileTransferId);
            return existingAck.get();
        }
        
        AckNackRecord ackRecord = new AckNackRecord(
            fileTransferId,
            fileTransfer.getFileName(),
            fileTransfer.getTenantId(),
            fileTransfer.getServiceName(),
            fileTransfer.getSubServiceName(),
            AckNackType.ACK,
            TransferDirection.OUTBOUND // ACK is outbound relative to our system
        );
        
        try {
            String ackContent = generateAckContent(fileTransfer);
            String ackFilePath = generateAckFilePath(ackRecord);
            
            // Write ACK file to filesystem
            Path ackPath = Paths.get(ackFilePath);
            Files.createDirectories(ackPath.getParent());
            Files.write(ackPath, ackContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            
            // Update record with file details
            ackRecord.setContent(ackContent);
            ackRecord.setAckNackFilePath(ackFilePath);
            ackRecord.setFileSize((long) ackContent.getBytes().length);
            ackRecord.setChecksum(calculateChecksum(ackContent.getBytes()));
            ackRecord.setGeneratedAt(LocalDateTime.now());
            ackRecord.setExpiresAt(LocalDateTime.now().plusHours(ackNackTimeoutHours));
            ackRecord.setStatus(AckNackStatus.GENERATED);
            
            // Set partner path based on service configuration
            setPartnerPath(ackRecord);
            
            ackNackRepository.save(ackRecord);
            logger.info("Generated ACK file for {}: {}", fileTransfer.getFileName(), ackFilePath);
            
            return ackRecord;
            
        } catch (Exception e) {
            logger.error("Failed to generate ACK for file transfer ID {}: {}", fileTransferId, e.getMessage());
            ackRecord.setStatus(AckNackStatus.FAILED);
            ackRecord.setErrorMessage(e.getMessage());
            ackNackRepository.save(ackRecord);
            throw new RuntimeException("Failed to generate ACK file", e);
        }
    }
    
    /**
     * Generate NACK file for a failed inbound file
     */
    public AckNackRecord generateNackForInboundFile(Long fileTransferId, String reasonCode, String reasonDescription) {
        FileTransferRecord fileTransfer = fileTransferRepository.findById(fileTransferId)
            .orElseThrow(() -> new RuntimeException("File transfer record not found: " + fileTransferId));
        
        if (fileTransfer.getDirection() != TransferDirection.INBOUND) {
            throw new IllegalArgumentException("NACK can only be generated for inbound files");
        }
        
        // Check if NACK already exists
        Optional<AckNackRecord> existingNack = ackNackRepository.findByOriginalFileTransferId(fileTransferId);
        if (existingNack.isPresent()) {
            logger.warn("NACK already exists for file transfer ID: {}", fileTransferId);
            return existingNack.get();
        }
        
        AckNackRecord nackRecord = new AckNackRecord(
            fileTransferId,
            fileTransfer.getFileName(),
            fileTransfer.getTenantId(),
            fileTransfer.getServiceName(),
            fileTransfer.getSubServiceName(),
            AckNackType.NACK,
            TransferDirection.OUTBOUND // NACK is outbound relative to our system
        );
        
        nackRecord.setReasonCode(reasonCode);
        nackRecord.setReasonDescription(reasonDescription);
        
        try {
            String nackContent = generateNackContent(fileTransfer, reasonCode, reasonDescription);
            String nackFilePath = generateAckFilePath(nackRecord);
            
            // Write NACK file to filesystem
            Path nackPath = Paths.get(nackFilePath);
            Files.createDirectories(nackPath.getParent());
            Files.write(nackPath, nackContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            
            // Update record with file details
            nackRecord.setContent(nackContent);
            nackRecord.setAckNackFilePath(nackFilePath);
            nackRecord.setFileSize((long) nackContent.getBytes().length);
            nackRecord.setChecksum(calculateChecksum(nackContent.getBytes()));
            nackRecord.setGeneratedAt(LocalDateTime.now());
            nackRecord.setExpiresAt(LocalDateTime.now().plusHours(ackNackTimeoutHours));
            nackRecord.setStatus(AckNackStatus.GENERATED);
            
            // Set partner path based on service configuration
            setPartnerPath(nackRecord);
            
            ackNackRepository.save(nackRecord);
            logger.info("Generated NACK file for {}: {}", fileTransfer.getFileName(), nackFilePath);
            
            return nackRecord;
            
        } catch (Exception e) {
            logger.error("Failed to generate NACK for file transfer ID {}: {}", fileTransferId, e.getMessage());
            nackRecord.setStatus(AckNackStatus.FAILED);
            nackRecord.setErrorMessage(e.getMessage());
            ackNackRepository.save(nackRecord);
            throw new RuntimeException("Failed to generate NACK file", e);
        }
    }
    
    // Private helper methods
    
    private void processIncomingAckNackFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String content = Files.readString(filePath);
            
            logger.info("Processing incoming ACK/NACK file: {}", fileName);
            
            // Parse the ACK/NACK file
            AckNackFileInfo ackNackInfo = parseAckNackFile(content, fileName);
            
            // Determine tenant from file path or content
            String tenantId = extractTenantFromPath(filePath);
            
            // Find the original file transfer record
            Optional<FileTransferRecord> originalTransfer = findOriginalFileTransfer(
                ackNackInfo.getOriginalFileName(), tenantId, ackNackInfo.getServiceName(), ackNackInfo.getSubServiceName());
            
            if (originalTransfer.isEmpty()) {
                logger.warn("Original file transfer not found for ACK/NACK: {}", fileName);
                moveToErrorDirectory(filePath, "Original file transfer not found");
                return;
            }
            
            FileTransferRecord fileTransfer = originalTransfer.get();
            
            // Check if ACK/NACK already processed
            Optional<AckNackRecord> existingRecord = ackNackRepository.findByOriginalFileTransferId(fileTransfer.getId());
            if (existingRecord.isPresent() && existingRecord.get().getStatus() == AckNackStatus.PROCESSED) {
                logger.warn("ACK/NACK already processed for file transfer ID: {}", fileTransfer.getId());
                moveToProcessedDirectory(filePath);
                return;
            }
            
            AckNackRecord ackNackRecord = new AckNackRecord(
                fileTransfer.getId(),
                fileTransfer.getFileName(),
                tenantId,
                fileTransfer.getServiceName(),
                fileTransfer.getSubServiceName(),
                ackNackInfo.getType(),
                TransferDirection.INBOUND // Received ACK/NACK is inbound
            );
            
            ackNackRecord.setAckNackFileName(fileName);
            ackNackRecord.setAckNackFilePath(filePath.toString());
            ackNackRecord.setContent(content);
            ackNackRecord.setFileSize(Files.size(filePath));
            ackNackRecord.setChecksum(calculateChecksum(Files.readAllBytes(filePath)));
            ackNackRecord.setReceivedAt(LocalDateTime.now());
            ackNackRecord.setProcessedAt(LocalDateTime.now());
            ackNackRecord.setStatus(AckNackStatus.PROCESSED);
            
            if (ackNackInfo.getType() == AckNackType.NACK) {
                ackNackRecord.setReasonCode(ackNackInfo.getReasonCode());
                ackNackRecord.setReasonDescription(ackNackInfo.getReasonDescription());
            }
            
            ackNackRepository.save(ackNackRecord);
            
            // Update original file transfer status
            updateFileTransferStatusFromAckNack(fileTransfer, ackNackRecord);
            
            // Move processed file to processed directory
            moveToProcessedDirectory(filePath);
            
            logger.info("Successfully processed {} file: {}", ackNackInfo.getType(), fileName);
            
        } catch (Exception e) {
            logger.error("Failed to process ACK/NACK file {}: {}", filePath, e.getMessage());
            try {
                moveToErrorDirectory(filePath, e.getMessage());
            } catch (IOException ioException) {
                logger.error("Failed to move error file: {}", ioException.getMessage());
            }
        }
    }
    
    /**
     * Generate ACK files for all completed inbound transfers without acknowledgment
     */
    public void generateAckForCompletedInboundFiles(String tenantId) {
        List<FileTransferRecord> completedTransfers = fileTransferRepository
            .findByTenantIdAndDirectionAndStatus(tenantId, TransferDirection.INBOUND, TransferStatus.COMPLETED);
        
        for (FileTransferRecord transfer : completedTransfers) {
            try {
                // Check if ACK already exists
                Optional<AckNackRecord> existingAck = ackNackRepository.findByOriginalFileTransferId(transfer.getId());
                if (existingAck.isEmpty()) {
                    generateAckForInboundFile(transfer.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to generate ACK for transfer {}: {}", transfer.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Generate NACK files for all failed inbound transfers without acknowledgment
     */
    public void generateNackForFailedInboundFiles(String tenantId) {
        List<FileTransferRecord> failedTransfers = fileTransferRepository
            .findByTenantIdAndDirectionAndStatus(tenantId, TransferDirection.INBOUND, TransferStatus.FAILED);
        
        for (FileTransferRecord transfer : failedTransfers) {
            try {
                // Check if NACK already exists
                Optional<AckNackRecord> existingNack = ackNackRepository.findByOriginalFileTransferId(transfer.getId());
                if (existingNack.isEmpty()) {
                    generateNackForInboundFile(transfer.getId(), "PROCESSING_FAILED", 
                        transfer.getErrorMessage() != null ? transfer.getErrorMessage() : "File processing failed");
                }
            } catch (Exception e) {
                logger.error("Failed to generate NACK for transfer {}: {}", transfer.getId(), e.getMessage());
            }
        }
    }
    
    // Private helper methods
    
    private boolean isAckNackFile(String fileName) {
        return fileName.toLowerCase().endsWith(".ack") || fileName.toLowerCase().endsWith(".nack");
    }
    
    private String extractTenantFromPath(Path filePath) {
        try {
            // Extract tenant from file path structure: /data/incoming/ack-nack/{tenantId}/...
            Path incomingPath = Paths.get(incomingAckNackPath);
            Path relativePath = incomingPath.relativize(filePath);
            if (relativePath.getNameCount() > 0) {
                return relativePath.getName(0).toString();
            }
        } catch (Exception e) {
            logger.warn("Could not extract tenant from path {}: {}", filePath, e.getMessage());
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
            .filter(record -> record.getServiceName().equals(serviceName))
            .filter(record -> (subServiceName == null && record.getSubServiceName() == null) || 
                             (subServiceName != null && subServiceName.equals(record.getSubServiceName())))
            .filter(record -> record.getDirection() == TransferDirection.OUTBOUND)
            .findFirst();
    }
    
    private void updateFileTransferStatusFromAckNack(FileTransferRecord fileTransfer, AckNackRecord ackNackRecord) {
        if (ackNackRecord.getType() == AckNackType.ACK) {
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "ack_received", "true"));
        } else {
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "nack_received", "true"));
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "nack_reason", ackNackRecord.getReasonDescription()));
        }
        
        fileTransferRepository.save(fileTransfer);
    }
    
    private void moveToProcessedDirectory(Path filePath) throws IOException {
        Path processedDir = Paths.get(incomingAckNackPath, "processed");
        Files.createDirectories(processedDir);
        Path targetPath = processedDir.resolve(filePath.getFileName());
        Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private void moveToErrorDirectory(Path filePath, String errorMessage) throws IOException {
        Path errorDir = Paths.get(incomingAckNackPath, "error");
        Files.createDirectories(errorDir);
        Path targetPath = errorDir.resolve(filePath.getFileName());
        Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Write error details to a companion file
        Path errorDetailsPath = errorDir.resolve(filePath.getFileName() + ".error");
        String errorDetails = "Error: " + errorMessage + "\nTimestamp: " + LocalDateTime.now();
        Files.write(errorDetailsPath, errorDetails.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
    
    private String generateAckContent(FileTransferRecord fileTransfer) {
        StringBuilder content = new StringBuilder();
        content.append("ACK|").append(fileTransfer.getFileName()).append("|");
        content.append(fileTransfer.getServiceName()).append("|");
        content.append(fileTransfer.getSubServiceName() != null ? fileTransfer.getSubServiceName() : "").append("|");
        content.append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("|");
        content.append("SUCCESS|");
        content.append(fileTransfer.getFileSize() != null ? fileTransfer.getFileSize() : "0").append("|");
        content.append(fileTransfer.getChecksum() != null ? fileTransfer.getChecksum() : "").append("\n");
        
        return content.toString();
    }
    
    private String generateNackContent(FileTransferRecord fileTransfer, String reasonCode, String reasonDescription) {
        StringBuilder content = new StringBuilder();
        content.append("NACK|").append(fileTransfer.getFileName()).append("|");
        content.append(fileTransfer.getServiceName()).append("|");
        content.append(fileTransfer.getSubServiceName() != null ? fileTransfer.getSubServiceName() : "").append("|");
        content.append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("|");
        content.append("FAILED|");
        content.append(reasonCode != null ? reasonCode : "PROCESSING_ERROR").append("|");
        content.append(reasonDescription != null ? reasonDescription : "File processing failed").append("\n");
        
        return content.toString();
    }
    
    private String generateAckFilePath(AckNackRecord record) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = record.getOriginalFileName();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        return String.format("%s/%s/%s/outbound/%s_%s_%s.%s",
            ackNackBasePath,
            record.getTenantId(),
            record.getServiceName(),
            baseName,
            record.getType().name().toLowerCase(),
            timestamp,
            record.getType().name().toLowerCase());
    }
    
    private void setPartnerPath(AckNackRecord record) {
        try {
            ServiceConfiguration config = serviceConfigurationRepository
                .findByTenantIdAndServiceNameAndSubServiceName(
                    record.getTenantId(), 
                    record.getServiceName(), 
                    record.getSubServiceName())
                .orElse(null);
            
            if (config != null && config.getTargetPath() != null) {
                String partnerPath = config.getTargetPath() + "/ack-nack/" + record.getAckNackFileName();
                record.setPartnerPath(partnerPath);
            }
        } catch (Exception e) {
            logger.warn("Could not set partner path for ACK/NACK record {}: {}", record.getId(), e.getMessage());
        }
    }
    
    private void sendAckNackFile(AckNackRecord record) throws IOException {
        if (record.getPartnerPath() == null) {
            throw new IOException("Partner path not configured for ACK/NACK file");
        }
        
        Path sourcePath = Paths.get(record.getAckNackFilePath());
        Path targetPath = Paths.get(record.getPartnerPath());
        
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("Sent {} file to partner: {} -> {}", record.getType(), sourcePath, targetPath);
    }
    
    private String updateMetadata(String existingMetadata, String key, String value) {
        if (existingMetadata == null) {
            existingMetadata = "";
        }
        return existingMetadata + (existingMetadata.isEmpty() ? "" : ";") + key + "=" + value;
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
    private static class AckNackFileInfo {
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