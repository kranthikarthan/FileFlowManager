package com.enterprise.fileflow.batch.monitor;

import com.enterprise.fileflow.shared.entity.Service;
import com.enterprise.fileflow.shared.entity.SubService;
import com.enterprise.fileflow.shared.entity.FileTransaction;
import com.enterprise.fileflow.shared.enums.FileStatus;
import com.enterprise.fileflow.batch.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Handles file events detected by the file monitoring service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileEventHandler {
    
    private final FileProcessingService fileProcessingService;
    
    /**
     * Handle a file event asynchronously
     */
    @Async
    public void handleFileEvent(Path filePath, Service service, SubService subService) {
        try {
            String fileName = filePath.getFileName().toString();
            log.info("Processing file event: {} for service: {}", fileName, service.getCode());
            
            // Determine file type
            String fileType = determineFileType(fileName, service, subService);
            
            // Create file transaction record
            FileTransaction transaction = createFileTransaction(filePath, service, subService, fileType);
            
            // Process the file based on type
            fileProcessingService.processFile(transaction);
            
        } catch (Exception e) {
            log.error("Error handling file event for file: {}", filePath.getFileName(), e);
        }
    }
    
    private String determineFileType(String fileName, Service service, SubService subService) {
        String sotPattern = subService != null ? subService.getEffectiveSotPattern() : service.getSotPattern();
        String eotPattern = subService != null ? subService.getEffectiveEotPattern() : service.getEotPattern();
        String dataFilePattern = subService != null ? subService.getEffectiveDataFilePattern() : service.getDataFilePattern();
        
        if (matchesPattern(fileName, sotPattern)) {
            return "SOT";
        } else if (matchesPattern(fileName, eotPattern)) {
            return "EOT";
        } else if (matchesPattern(fileName, dataFilePattern)) {
            return "DATA";
        } else {
            return "UNKNOWN";
        }
    }
    
    private boolean matchesPattern(String fileName, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }
        
        try {
            return Pattern.matches(pattern, fileName);
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {} for file: {}", pattern, fileName);
            return false;
        }
    }
    
    private FileTransaction createFileTransaction(Path filePath, Service service, SubService subService, String fileType) {
        FileTransaction transaction = new FileTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setService(service);
        transaction.setSubService(subService);
        transaction.setFileName(filePath.getFileName().toString());
        transaction.setFilePath(filePath.toString());
        transaction.setFileType(fileType);
        transaction.setStatus(FileStatus.RECEIVED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCreatedBy("SYSTEM");
        
        try {
            transaction.setFileSize(java.nio.file.Files.size(filePath));
        } catch (Exception e) {
            log.warn("Could not determine file size for: {}", filePath);
        }
        
        return transaction;
    }
}