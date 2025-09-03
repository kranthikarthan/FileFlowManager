package com.filetransfer.web.service;

import com.filetransfer.web.dto.AckNackRecordDto;
import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.AckNackRecordRepository;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import com.filetransfer.web.repository.ServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing ACK/NACK file generation, processing, and monitoring
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
    
    @Value("${file-transfer.ack-nack.timeout-hours:24}")
    private int ackNackTimeoutHours;
    
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
    
    /**
     * Process received ACK/NACK file for outbound data transfers
     */
    public AckNackRecord processReceivedAckNackFile(String filePath, String fileName, String tenantId) {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String content = new String(fileContent);
            
            // Parse the ACK/NACK file to extract information
            AckNackFileInfo ackNackInfo = parseAckNackFile(content, fileName);
            
            // Find the original file transfer record
            Optional<FileTransferRecord> originalTransfer = findOriginalFileTransfer(
                ackNackInfo.getOriginalFileName(), tenantId, ackNackInfo.getServiceName(), ackNackInfo.getSubServiceName());
            
            if (originalTransfer.isEmpty()) {
                throw new RuntimeException("Original file transfer not found for ACK/NACK: " + fileName);
            }
            
            FileTransferRecord fileTransfer = originalTransfer.get();
            
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
            ackNackRecord.setAckNackFilePath(filePath);
            ackNackRecord.setContent(content);
            ackNackRecord.setFileSize((long) fileContent.length);
            ackNackRecord.setChecksum(calculateChecksum(fileContent));
            ackNackRecord.setReceivedAt(LocalDateTime.now());
            ackNackRecord.setStatus(AckNackStatus.RECEIVED);
            
            if (ackNackInfo.getType() == AckNackType.NACK) {
                ackNackRecord.setReasonCode(ackNackInfo.getReasonCode());
                ackNackRecord.setReasonDescription(ackNackInfo.getReasonDescription());
            }
            
            ackNackRepository.save(ackNackRecord);
            
            // Update original file transfer status based on ACK/NACK
            updateFileTransferStatusFromAckNack(fileTransfer, ackNackRecord);
            
            logger.info("Processed received {} file: {}", ackNackInfo.getType(), fileName);
            return ackNackRecord;
            
        } catch (Exception e) {
            logger.error("Failed to process ACK/NACK file {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to process ACK/NACK file", e);
        }
    }
    
    /**
     * Send pending ACK/NACK files to partners
     */
    public void sendPendingAckNackFiles(String tenantId) {
        List<AckNackRecord> pendingRecords = ackNackRepository.findByTenantIdAndStatus(tenantId, AckNackStatus.GENERATED);
        
        for (AckNackRecord record : pendingRecords) {
            try {
                sendAckNackFile(record);
                record.setStatus(AckNackStatus.SENT);
                record.setSentAt(LocalDateTime.now());
                ackNackRepository.save(record);
                
                logger.info("Sent {} file to partner: {}", record.getType(), record.getAckNackFileName());
                
            } catch (Exception e) {
                logger.error("Failed to send {} file {}: {}", record.getType(), record.getAckNackFileName(), e.getMessage());
                record.setStatus(AckNackStatus.FAILED);
                record.setErrorMessage(e.getMessage());
                ackNackRepository.save(record);
            }
        }
    }
    
    /**
     * Get all ACK/NACK records for a tenant
     */
    public List<AckNackRecordDto> getAllAckNackRecords(String tenantId) {
        return ackNackRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get ACK/NACK records by status
     */
    public List<AckNackRecordDto> getAckNackRecordsByStatus(String tenantId, AckNackStatus status) {
        return ackNackRepository.findByTenantIdAndStatus(tenantId, status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get ACK/NACK records by type
     */
    public List<AckNackRecordDto> getAckNackRecordsByType(String tenantId, AckNackType type) {
        return ackNackRepository.findByTenantIdAndType(tenantId, type).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get ACK/NACK record for a specific file transfer
     */
    public Optional<AckNackRecordDto> getAckNackForFileTransfer(Long fileTransferId) {
        return ackNackRepository.findByOriginalFileTransferId(fileTransferId)
                .map(this::convertToDto);
    }
    
    /**
     * Retry failed ACK/NACK generation or sending
     */
    public void retryAckNack(Long ackNackId) {
        AckNackRecord record = ackNackRepository.findById(ackNackId)
            .orElseThrow(() -> new RuntimeException("ACK/NACK record not found: " + ackNackId));
        
        if (record.getStatus() == AckNackStatus.FAILED) {
            record.setStatus(AckNackStatus.PENDING);
            record.setErrorMessage(null);
            ackNackRepository.save(record);
            
            // Re-trigger generation or sending based on current state
            if (record.getGeneratedAt() == null) {
                // Need to regenerate
                regenerateAckNackFile(record);
            } else {
                // Need to resend
                sendAckNackFile(record);
                record.setStatus(AckNackStatus.SENT);
                record.setSentAt(LocalDateTime.now());
                ackNackRepository.save(record);
            }
            
            logger.info("Retried {} file: {}", record.getType(), record.getAckNackFileName());
        } else {
            throw new IllegalArgumentException("Cannot retry ACK/NACK with status: " + record.getStatus());
        }
    }
    
    /**
     * Mark expired ACK/NACK records
     */
    public void markExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        List<AckNackRecord> expiredRecords = ackNackRepository.findExpiredRecords(AckNackStatus.SENT, now);
        
        for (AckNackRecord record : expiredRecords) {
            record.setStatus(AckNackStatus.EXPIRED);
            ackNackRepository.save(record);
            logger.warn("Marked {} file as expired: {}", record.getType(), record.getAckNackFileName());
        }
    }
    
    // Private helper methods
    
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
        
        return String.format("%s/%s/%s/%s_%s_%s.%s",
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
                // Construct partner path based on service configuration
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
        Files.copy(sourcePath, targetPath);
        
        logger.info("Sent {} file to partner: {} -> {}", record.getType(), sourcePath, targetPath);
    }
    
    private void regenerateAckNackFile(AckNackRecord record) throws Exception {
        FileTransferRecord fileTransfer = fileTransferRepository.findById(record.getOriginalFileTransferId())
            .orElseThrow(() -> new RuntimeException("Original file transfer not found"));
        
        String content;
        if (record.getType() == AckNackType.ACK) {
            content = generateAckContent(fileTransfer);
        } else {
            content = generateNackContent(fileTransfer, record.getReasonCode(), record.getReasonDescription());
        }
        
        String filePath = generateAckFilePath(record);
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        
        record.setContent(content);
        record.setAckNackFilePath(filePath);
        record.setFileSize((long) content.getBytes().length);
        record.setChecksum(calculateChecksum(content.getBytes()));
        record.setGeneratedAt(LocalDateTime.now());
        record.setStatus(AckNackStatus.GENERATED);
    }
    
    private AckNackFileInfo parseAckNackFile(String content, String fileName) {
        // Simple pipe-delimited format parsing
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
            // ACK received - mark as acknowledged
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "ack_received", "true"));
        } else {
            // NACK received - mark as rejected
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "nack_received", "true"));
            fileTransfer.setMetadata(updateMetadata(fileTransfer.getMetadata(), "nack_reason", ackNackRecord.getReasonDescription()));
        }
        
        fileTransferRepository.save(fileTransfer);
    }
    
    private String updateMetadata(String existingMetadata, String key, String value) {
        // Simple key-value metadata update (could be enhanced with JSON)
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
    
    private AckNackRecordDto convertToDto(AckNackRecord record) {
        AckNackRecordDto dto = new AckNackRecordDto();
        dto.setId(record.getId());
        dto.setOriginalFileTransferId(record.getOriginalFileTransferId());
        dto.setOriginalFileName(record.getOriginalFileName());
        dto.setAckNackFileName(record.getAckNackFileName());
        dto.setType(record.getType());
        dto.setStatus(record.getStatus());
        dto.setDirection(record.getDirection());
        dto.setTenantId(record.getTenantId());
        dto.setServiceName(record.getServiceName());
        dto.setSubServiceName(record.getSubServiceName());
        dto.setAckNackFilePath(record.getAckNackFilePath());
        dto.setPartnerPath(record.getPartnerPath());
        dto.setContent(record.getContent());
        dto.setErrorMessage(record.getErrorMessage());
        dto.setReasonCode(record.getReasonCode());
        dto.setReasonDescription(record.getReasonDescription());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setGeneratedAt(record.getGeneratedAt());
        dto.setSentAt(record.getSentAt());
        dto.setReceivedAt(record.getReceivedAt());
        dto.setProcessedAt(record.getProcessedAt());
        dto.setExpiresAt(record.getExpiresAt());
        dto.setFileSize(record.getFileSize());
        dto.setChecksum(record.getChecksum());
        dto.setMetadata(record.getMetadata());
        return dto;
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