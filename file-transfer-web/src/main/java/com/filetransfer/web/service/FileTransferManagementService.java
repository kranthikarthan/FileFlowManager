package com.filetransfer.web.service;

import com.filetransfer.web.dto.FileTransferRecordDto;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.entity.CompressionType;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileTransferManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferManagementService.class);
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    @Autowired
    private AckNackService ackNackService;
    
    @Autowired
    private CompressionService compressionService;
    
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
    
    /**
     * Compress a file for transfer
     */
    public void compressFile(Long id, CompressionType compressionType) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        if (record.getCompressionEnabled()) {
            throw new IllegalStateException("File is already compressed");
        }
        
        try {
            Path sourceFile = Paths.get(record.getSourcePath());
            String targetDirectory = sourceFile.getParent().toString() + "/compressed";
            
            long startTime = System.currentTimeMillis();
            CompressionService.CompressionResult result = compressionService.compressFile(
                sourceFile, compressionType, targetDirectory);
            
            // Update record with compression information
            record.setCompressionEnabled(true);
            record.setCompressionType(compressionType);
            record.setOriginalFileSize(Files.size(sourceFile));
            record.setCompressedFileSize(Files.size(result.getCompressedFile()));
            record.setCompressionRatio(result.getCompressionRatio());
            record.setCompressionTimeMs(result.getCompressionTimeMs());
            record.setCompressedFilePath(result.getCompressedFile().toString());
            
            fileTransferRepository.save(record);
            
            logger.info("Compressed file for transfer {}: {} -> {} (ratio: {:.2f})", 
                       id, record.getOriginalFileSize(), record.getCompressedFileSize(), 
                       record.getCompressionRatio());
                       
        } catch (Exception e) {
            logger.error("Failed to compress file for transfer {}: {}", id, e.getMessage());
            throw new RuntimeException("File compression failed", e);
        }
    }
    
    /**
     * Decompress a received file
     */
    public void decompressFile(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        if (!record.getCompressionEnabled() || record.getCompressionType() == CompressionType.NONE) {
            throw new IllegalStateException("File is not compressed");
        }
        
        try {
            Path compressedFile = Paths.get(record.getCompressedFilePath() != null ? 
                                           record.getCompressedFilePath() : record.getSourcePath());
            String targetDirectory = record.getTargetPath();
            
            long startTime = System.currentTimeMillis();
            CompressionService.DecompressionResult result = compressionService.decompressFile(
                compressedFile, targetDirectory);
            
            // Update record with decompression information
            record.setDecompressionTimeMs(result.getDecompressionTimeMs());
            record.setTargetPath(result.getDecompressedFile().toString());
            
            fileTransferRepository.save(record);
            
            logger.info("Decompressed file for transfer {}: {} (time: {}ms)", 
                       id, result.getDecompressedFile(), result.getDecompressionTimeMs());
                       
        } catch (Exception e) {
            logger.error("Failed to decompress file for transfer {}: {}", id, e.getMessage());
            throw new RuntimeException("File decompression failed", e);
        }
    }
    
    /**
     * Get compression recommendations for a file
     */
    public Map<String, Object> getCompressionRecommendations(Long id) {
        FileTransferRecord record = fileTransferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File transfer record not found: " + id));
        
        try {
            Path file = Paths.get(record.getSourcePath());
            FileType fileType = record.getFileType();
            
            // Get recommended compression types
            CompressionType speedOptimized = compressionService.getRecommendedCompression(file, fileType, true);
            CompressionType ratioOptimized = compressionService.getRecommendedCompression(file, fileType, false);
            
            Map<String, Object> recommendations = new HashMap<>();
            recommendations.put("speedOptimized", speedOptimized);
            recommendations.put("ratioOptimized", ratioOptimized);
            recommendations.put("shouldCompress", CompressionType.shouldCompress(fileType));
            recommendations.put("originalFileSize", Files.size(file));
            recommendations.put("estimatedCompressedSize", Math.round(Files.size(file) * speedOptimized.getAverageCompressionRatio()));
            
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Failed to get compression recommendations for transfer {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to get compression recommendations", e);
        }
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
        
        // Compression fields
        dto.setCompressionEnabled(record.getCompressionEnabled());
        dto.setCompressionType(record.getCompressionType());
        dto.setOriginalFileSize(record.getOriginalFileSize());
        dto.setCompressedFileSize(record.getCompressedFileSize());
        dto.setCompressionRatio(record.getCompressionRatio());
        dto.setCompressionTimeMs(record.getCompressionTimeMs());
        dto.setDecompressionTimeMs(record.getDecompressionTimeMs());
        dto.setCompressedFilePath(record.getCompressedFilePath());
        
        return dto;
    }
}