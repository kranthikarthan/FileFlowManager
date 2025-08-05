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
}