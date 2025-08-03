package com.filetransfer.web.service;

import com.filetransfer.web.dto.FileTransferRecordDto;
import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.repository.FileTransferRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileTransferManagementService {
    
    @Autowired
    private FileTransferRecordRepository fileTransferRepository;
    
    public List<FileTransferRecordDto> getAllFileTransfers() {
        return fileTransferRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByService(String serviceType) {
        return fileTransferRepository.findByServiceType(serviceType).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByStatus(TransferStatus status) {
        return fileTransferRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByDirection(TransferDirection direction) {
        return fileTransferRepository.findByDirection(direction).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByServiceAndStatus(String serviceType, TransferStatus status) {
        return fileTransferRepository.findByServiceTypeAndStatus(serviceType, status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return fileTransferRepository.findByDateRange(startDate, endDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<FileTransferRecordDto> getFileTransfersByServiceAndDateRange(String serviceType, 
                                                                          LocalDateTime startDate, 
                                                                          LocalDateTime endDate) {
        return fileTransferRepository.findByServiceTypeAndDateRange(serviceType, startDate, endDate).stream()
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
    
    public List<String> getDistinctServiceTypes() {
        return fileTransferRepository.findDistinctServiceTypes();
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