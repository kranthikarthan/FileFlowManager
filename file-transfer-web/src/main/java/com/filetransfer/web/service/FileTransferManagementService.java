package com.filetransfer.web.service;

import com.filetransfer.web.dto.FileTransferRecordDto;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileTransferManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferManagementService.class);
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private AckNackService ackNackService;
    
    public List<FileTransferRecordDto> getAllFileTransfers(String tenantId) {
        return fileTransferRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByService(String tenantId, String serviceType) {
        return fileTransferRepository.findByTenantIdAndServiceType(tenantId, serviceType).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByStatus(String tenantId, TransferStatus status) {
        return fileTransferRepository.findByTenantIdAndStatus(tenantId, status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByDirection(String tenantId, TransferDirection direction) {
        return fileTransferRepository.findByTenantIdAndDirection(tenantId, direction).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByServiceAndStatus(String tenantId, String serviceType, TransferStatus status) {
        return fileTransferRepository.findByTenantIdAndServiceTypeAndStatus(tenantId, serviceType, status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByDateRange(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        return fileTransferRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByServiceAndDateRange(String tenantId, String serviceType, 
                                                                          LocalDateTime startDate, 
                                                                          LocalDateTime endDate) {
        return fileTransferRepository.findByTenantIdAndServiceTypeAndDateRange(tenantId, serviceType, startDate, endDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public FileTransferRecordDto getFileTransferById(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        return convertToDto(record);
    }
    
    public void retryTransfer(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        if (record.getStatus() == TransferStatus.FAILED || record.getStatus() == TransferStatus.CANCELLED) {
            record.setStatus(TransferStatus.PENDING);
            record.setErrorMessage(null);
            fileTransferRepository.save(record);
        } else {
            throw new RuntimeException("Cannot retry transfer with status: " + record.getStatus());
        }
    }
    
    public void cancelTransfer(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        if (record.getStatus() == TransferStatus.PENDING || record.getStatus() == TransferStatus.FAILED) {
            record.setStatus(TransferStatus.CANCELLED);
            record.setProcessedAt(LocalDateTime.now());
            fileTransferRepository.save(record);
        } else {
            throw new RuntimeException("Cannot cancel transfer with status: " + record.getStatus());
        }
    }
    
    public List<String> getDistinctServiceTypes(String tenantId) {
        return fileTransferRepository.findDistinctServiceTypesForTenant(tenantId);
    }
    
    public List<String> getDistinctSubServiceTypes(String tenantId, String serviceType) {
        return fileTransferRepository.findDistinctSubServiceTypesForService(tenantId, serviceType);
    }
    
    public List<FileTransferRecordDto> getFileTransfersByFileName(String tenantId, String fileName) {
        return fileTransferRepository.findByTenantIdAndFileName(tenantId, fileName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Complete file transfer and trigger ACK generation for inbound files
     */
    public void completeFileTransfer(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        record.setStatus(TransferStatus.COMPLETED);
        record.setProcessedAt(LocalDateTime.now());
        fileTransferRepository.save(record);
        
        // Auto-generate ACK for completed inbound files
        if (record.getDirection() == TransferDirection.INBOUND) {
            try {
                ackNackService.generateAckForInboundFile(id);
                logger.info("Auto-generated ACK for completed inbound file: {}", record.getFileName());
            } catch (Exception e) {
                logger.error("Failed to auto-generate ACK for file {}: {}", record.getFileName(), e.getMessage());
            }
        }
    }
    
    /**
     * Mark file transfer as failed and trigger NACK generation for inbound files
     */
    public void failFileTransfer(Long id, String errorMessage) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        record.setStatus(TransferStatus.FAILED);
        record.setErrorMessage(errorMessage);
        record.setProcessedAt(LocalDateTime.now());
        fileTransferRepository.save(record);
        
        // Auto-generate NACK for failed inbound files
        if (record.getDirection() == TransferDirection.INBOUND) {
            try {
                ackNackService.generateNackForInboundFile(id, "PROCESSING_FAILED", errorMessage);
                logger.info("Auto-generated NACK for failed inbound file: {}", record.getFileName());
            } catch (Exception e) {
                logger.error("Failed to auto-generate NACK for file {}: {}", record.getFileName(), e.getMessage());
            }
        }
    }
    
    /**
     * Generate ACK for a specific file transfer
     */
    public void generateAckForFile(Long id) {
        ackNackService.generateAckForInboundFile(id);
    }
    
    /**
     * Generate NACK for a specific file transfer
     */
    public void generateNackForFile(Long id, String reasonCode, String reasonDescription) {
        ackNackService.generateNackForInboundFile(id, reasonCode, reasonDescription);
    }
    
    private FileTransferRecordDto convertToDto(FileTransferRecord record) {
        FileTransferRecordDto dto = new FileTransferRecordDto();
        dto.setId(record.getId());
        dto.setFileName(record.getFileName());
        dto.setServiceType(record.getServiceType());
        dto.setSourcePath(record.getSourcePath());
        dto.setTargetPath(record.getTargetPath());
        dto.setStatus(record.getStatus());
        dto.setDirection(record.getDirection());
        dto.setErrorMessage(record.getErrorMessage());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setProcessedAt(record.getProcessedAt());
        dto.setFileSize(record.getFileSize());
        dto.setChecksum(record.getChecksum());
        dto.setBatchJobExecutionId(record.getBatchJobExecutionId());
        return dto;
    }
}