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
    public ResponseEntity<List<FileTransferRecordDto>> getAllFileTransfers() {
        return ResponseEntity.ok(fileTransferService.getAllFileTransfers());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FileTransferRecordDto> getFileTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(fileTransferService.getFileTransferById(id));
    }
    
    @GetMapping("/service/{serviceType}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByService(@PathVariable String serviceType) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByService(serviceType));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByStatus(@PathVariable TransferStatus status) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByStatus(status));
    }
    
    @GetMapping("/direction/{direction}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByDirection(@PathVariable TransferDirection direction) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByDirection(direction));
    }
    
    @GetMapping("/service/{serviceType}/status/{status}")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByServiceAndStatus(
            @PathVariable String serviceType, 
            @PathVariable TransferStatus status) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByServiceAndStatus(serviceType, status));
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByDateRange(startDate, endDate));
    }
    
    @GetMapping("/service/{serviceType}/date-range")
    public ResponseEntity<List<FileTransferRecordDto>> getFileTransfersByServiceAndDateRange(
            @PathVariable String serviceType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(fileTransferService.getFileTransfersByServiceAndDateRange(serviceType, startDate, endDate));
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
    public ResponseEntity<List<String>> getDistinctServiceTypes() {
        return ResponseEntity.ok(fileTransferService.getDistinctServiceTypes());
    }
}