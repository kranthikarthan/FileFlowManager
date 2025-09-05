package com.filetransfer.web.controller;

import com.filetransfer.web.dto.FileTransferRecordDto;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.service.FileTransferManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/file-transfers")
@CrossOrigin(origins = "http://localhost:3000") // Allow React frontend
public class FileTransferController {
    
    @Autowired
    private FileTransferManagementService fileTransferService;
    
    @GetMapping
    public ResponseEntity<List<FileTransferRecordDto>> getAllFileTransfers(@RequestParam String tenantId) {
        return ResponseEntity.ok(fileTransferService.getAllFileTransfers(tenantId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FileTransferRecordDto> getFileTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(fileTransferService.getFileTransferById(id));
    }
    
    @GetMapping("/service/{serviceType}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByService(
            @RequestParam String tenantId, 
            @PathVariable String serviceType) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByService(tenantId, serviceType));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByStatus(
            @RequestParam String tenantId, 
            @PathVariable TransferStatus status) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByStatus(tenantId, status));
    }
    
    @GetMapping("/direction/{direction}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByDirection(
            @RequestParam String tenantId, 
            @PathVariable TransferDirection direction) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByDirection(tenantId, direction));
    }
    
    @GetMapping("/service/{serviceType}/status/{status}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByServiceAndStatus(
            @RequestParam String tenantId,
            @PathVariable String serviceType, 
            @PathVariable TransferStatus status) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByServiceAndStatus(tenantId, serviceType, status));
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByDateRange(
            @RequestParam String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByDateRange(tenantId, startDate, endDate));
    }
    
    @GetMapping("/service/{serviceType}/date-range")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByServiceAndDateRange(
            @RequestParam String tenantId,
            @PathVariable String serviceType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByServiceAndDateRange(tenantId, serviceType, startDate, endDate));
    }
    
    @PostMapping("/{id}/retry")
    public ResponseEntity<String> retryTransfer(@PathVariable Long id) {
        try {
            fileTransferService.retryTransfer(id);
            return ResponseEntity.ok("Transfer retry initiated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelTransfer(@PathVariable Long id) {
        try {
            fileTransferService.cancelTransfer(id);
            return ResponseEntity.ok("Transfer cancelled successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/services")
    public ResponseEntity<List<String>> getDistinctServiceTypes(@RequestParam String tenantId) {
        return ResponseEntity.ok(fileTransferService.getDistinctServiceTypes(tenantId));
    }
    
    @GetMapping("/services/{serviceType}/sub-services")
    public ResponseEntity<List<String>> getDistinctSubServiceTypes(
            @RequestParam String tenantId, 
            @PathVariable String serviceType) {
        return ResponseEntity.ok(fileTransferService.getDistinctSubServiceTypes(tenantId, serviceType));
    }
    
    @GetMapping("/file/{fileName}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByFileName(
            @RequestParam String tenantId, 
            @PathVariable String fileName) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByFileName(tenantId, fileName));
    }
    
    /**
     * Get file transfers by file extension
     */
    @GetMapping("/extension/{extension}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByExtension(
            @RequestParam String tenantId,
            @PathVariable String extension) {
        try {
            // Ensure extension starts with dot
            String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;
            
            List<FileTransferRecordDto> transfers = fileTransferService.getFileTransfersByExtension(tenantId, normalizedExtension);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            logger.error("Error retrieving file transfers by extension for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get distinct file extensions for a tenant
     */
    @GetMapping("/extensions")
    public ResponseEntity<List<String>> getDistinctFileExtensions(@RequestParam String tenantId) {
        try {
            List<String> extensions = fileTransferService.getDistinctFileExtensions(tenantId);
            return ResponseEntity.ok(extensions);
        } catch (Exception e) {
            logger.error("Error retrieving distinct file extensions for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file extension statistics for a tenant
     */
    @GetMapping("/statistics/extensions")
    public ResponseEntity<Map<String, Long>> getFileExtensionStatistics(@RequestParam String tenantId) {
        try {
            Map<String, Long> statistics = fileTransferService.getFileExtensionStatistics(tenantId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving file extension statistics for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get recent files for a tenant
     */
    @GetMapping("/recent")
    public ResponseEntity<List<FileTransferRecordDto>> getRecentFiles(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<FileTransferRecordDto> recentFiles = fileTransferService.getRecentFiles(tenantId, limit);
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            logger.error("Error retrieving recent files for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get recently processed files
     */
    @GetMapping("/recent/processed")
    public ResponseEntity<List<FileTransferRecordDto>> getRecentlyProcessedFiles(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FileTransferRecordDto> recentFiles = fileTransferService.getRecentlyProcessedFiles(tenantId, limit);
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            logger.error("Error retrieving recently processed files for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get recent failed files
     */
    @GetMapping("/recent/failed")
    public ResponseEntity<List<FileTransferRecordDto>> getRecentFailedFiles(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FileTransferRecordDto> recentFiles = fileTransferService.getRecentFailedFiles(tenantId, limit);
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            logger.error("Error retrieving recent failed files for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}