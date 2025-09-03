package com.filetransfer.web.controller;

import com.filetransfer.web.dto.AckNackRecordDto;
import com.filetransfer.web.entity.AckNackStatus;
import com.filetransfer.web.entity.AckNackType;
import com.filetransfer.web.service.AckNackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for ACK/NACK file management
 */
@RestController
@RequestMapping("/api/v1/ack-nack")
@CrossOrigin(origins = "*")
public class AckNackController {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackController.class);
    
    @Autowired
    private AckNackService ackNackService;
    
    /**
     * Get all ACK/NACK records for a tenant
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<List<AckNackRecordDto>> getAllAckNackRecords(@PathVariable String tenantId) {
        try {
            List<AckNackRecordDto> records = ackNackService.getAllAckNackRecords(tenantId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("Error retrieving ACK/NACK records for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get ACK/NACK records by status
     */
    @GetMapping("/{tenantId}/status/{status}")
    public ResponseEntity<List<AckNackRecordDto>> getAckNackRecordsByStatus(
            @PathVariable String tenantId, 
            @PathVariable AckNackStatus status) {
        try {
            List<AckNackRecordDto> records = ackNackService.getAckNackRecordsByStatus(tenantId, status);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("Error retrieving ACK/NACK records by status for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get ACK/NACK records by type
     */
    @GetMapping("/{tenantId}/type/{type}")
    public ResponseEntity<List<AckNackRecordDto>> getAckNackRecordsByType(
            @PathVariable String tenantId, 
            @PathVariable AckNackType type) {
        try {
            List<AckNackRecordDto> records = ackNackService.getAckNackRecordsByType(tenantId, type);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("Error retrieving ACK/NACK records by type for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get ACK/NACK record for a specific file transfer
     */
    @GetMapping("/file-transfer/{fileTransferId}")
    public ResponseEntity<AckNackRecordDto> getAckNackForFileTransfer(@PathVariable Long fileTransferId) {
        try {
            Optional<AckNackRecordDto> record = ackNackService.getAckNackForFileTransfer(fileTransferId);
            return record.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error retrieving ACK/NACK for file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Manually generate ACK for an inbound file transfer
     */
    @PostMapping("/generate-ack/{fileTransferId}")
    public ResponseEntity<Map<String, String>> generateAck(@PathVariable Long fileTransferId) {
        try {
            ackNackService.generateAckForInboundFile(fileTransferId);
            return ResponseEntity.ok(Map.of("message", "ACK generated successfully", "fileTransferId", fileTransferId.toString()));
        } catch (Exception e) {
            logger.error("Error generating ACK for file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Manually generate NACK for an inbound file transfer
     */
    @PostMapping("/generate-nack/{fileTransferId}")
    public ResponseEntity<Map<String, String>> generateNack(
            @PathVariable Long fileTransferId,
            @RequestBody Map<String, String> nackRequest) {
        try {
            String reasonCode = nackRequest.get("reasonCode");
            String reasonDescription = nackRequest.get("reasonDescription");
            
            ackNackService.generateNackForInboundFile(fileTransferId, reasonCode, reasonDescription);
            return ResponseEntity.ok(Map.of("message", "NACK generated successfully", "fileTransferId", fileTransferId.toString()));
        } catch (Exception e) {
            logger.error("Error generating NACK for file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Upload and process received ACK/NACK file
     */
    @PostMapping("/{tenantId}/upload")
    public ResponseEntity<Map<String, String>> uploadAckNackFile(
            @PathVariable String tenantId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".ack") && !fileName.toLowerCase().endsWith(".nack"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be an .ack or .nack file"));
            }
            
            // Save file temporarily and process
            String tempFilePath = "/tmp/" + fileName;
            file.transferTo(new java.io.File(tempFilePath));
            
            ackNackService.processReceivedAckNackFile(tempFilePath, fileName, tenantId);
            
            // Clean up temp file
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFilePath));
            
            return ResponseEntity.ok(Map.of("message", "ACK/NACK file processed successfully", "fileName", fileName));
            
        } catch (Exception e) {
            logger.error("Error processing uploaded ACK/NACK file for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Retry failed ACK/NACK generation or sending
     */
    @PostMapping("/retry/{ackNackId}")
    public ResponseEntity<Map<String, String>> retryAckNack(@PathVariable Long ackNackId) {
        try {
            ackNackService.retryAckNack(ackNackId);
            return ResponseEntity.ok(Map.of("message", "ACK/NACK retry initiated", "ackNackId", ackNackId.toString()));
        } catch (Exception e) {
            logger.error("Error retrying ACK/NACK {}: {}", ackNackId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Send pending ACK/NACK files for a tenant
     */
    @PostMapping("/{tenantId}/send-pending")
    public ResponseEntity<Map<String, String>> sendPendingAckNackFiles(@PathVariable String tenantId) {
        try {
            ackNackService.sendPendingAckNackFiles(tenantId);
            return ResponseEntity.ok(Map.of("message", "Pending ACK/NACK files sent", "tenantId", tenantId));
        } catch (Exception e) {
            logger.error("Error sending pending ACK/NACK files for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get ACK/NACK statistics for a tenant
     */
    @GetMapping("/{tenantId}/statistics")
    public ResponseEntity<Map<String, Object>> getAckNackStatistics(@PathVariable String tenantId) {
        try {
            List<AckNackRecordDto> allRecords = ackNackService.getAllAckNackRecords(tenantId);
            
            long ackCount = allRecords.stream().filter(r -> r.getType() == AckNackType.ACK).count();
            long nackCount = allRecords.stream().filter(r -> r.getType() == AckNackType.NACK).count();
            long pendingCount = allRecords.stream().filter(r -> r.getStatus() == AckNackStatus.PENDING).count();
            long sentCount = allRecords.stream().filter(r -> r.getStatus() == AckNackStatus.SENT).count();
            long failedCount = allRecords.stream().filter(r -> r.getStatus() == AckNackStatus.FAILED).count();
            
            Map<String, Object> stats = Map.of(
                "totalRecords", allRecords.size(),
                "ackCount", ackCount,
                "nackCount", nackCount,
                "pendingCount", pendingCount,
                "sentCount", sentCount,
                "failedCount", failedCount
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving ACK/NACK statistics for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}