package com.filetransfer.batch.controller;

import com.filetransfer.batch.service.AckNackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller for ACK/NACK batch operations
 */
@RestController
@RequestMapping("/api/v1/batch/ack-nack")
@CrossOrigin(origins = "*")
public class AckNackBatchController {
    
    private static final Logger logger = LoggerFactory.getLogger(AckNackBatchController.class);
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("processAckNackFilesJob")
    private Job processAckNackFilesJob;
    
    @Autowired
    private AckNackService ackNackService;
    
    /**
     * Manually trigger ACK/NACK file processing job
     */
    @PostMapping("/process-files")
    public ResponseEntity<Map<String, String>> processAckNackFiles() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .toJobParameters();
            
            jobLauncher.run(processAckNackFilesJob, jobParameters);
            
            return ResponseEntity.ok(Map.of("message", "ACK/NACK file processing job started successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to start ACK/NACK processing job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start job: " + e.getMessage()));
        }
    }
    
    /**
     * Generate ACK files for all completed inbound transfers without acknowledgment
     */
    @PostMapping("/generate-acks/{tenantId}")
    public ResponseEntity<Map<String, String>> generateAckFiles(@PathVariable String tenantId) {
        try {
            ackNackService.generateAckForCompletedInboundFiles(tenantId);
            return ResponseEntity.ok(Map.of("message", "ACK generation completed for tenant: " + tenantId));
            
        } catch (Exception e) {
            logger.error("Failed to generate ACK files for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Generate NACK files for all failed inbound transfers without acknowledgment
     */
    @PostMapping("/generate-nacks/{tenantId}")
    public ResponseEntity<Map<String, String>> generateNackFiles(@PathVariable String tenantId) {
        try {
            ackNackService.generateNackForFailedInboundFiles(tenantId);
            return ResponseEntity.ok(Map.of("message", "NACK generation completed for tenant: " + tenantId));
            
        } catch (Exception e) {
            logger.error("Failed to generate NACK files for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Send all pending ACK/NACK files
     */
    @PostMapping("/send-pending")
    public ResponseEntity<Map<String, String>> sendPendingFiles() {
        try {
            // This will be handled by the scheduled job, but can be triggered manually
            return ResponseEntity.ok(Map.of("message", "Pending ACK/NACK files will be sent by the scheduled job"));
            
        } catch (Exception e) {
            logger.error("Failed to trigger sending of pending ACK/NACK files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark expired ACK/NACK records
     */
    @PostMapping("/mark-expired")
    public ResponseEntity<Map<String, String>> markExpiredRecords() {
        try {
            ackNackService.markExpiredRecords();
            return ResponseEntity.ok(Map.of("message", "Expired ACK/NACK records marked successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to mark expired ACK/NACK records: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get batch processing status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        try {
            // Return basic status information
            Map<String, Object> status = Map.of(
                "service", "ACK/NACK Batch Processing",
                "status", "RUNNING",
                "timestamp", LocalDateTime.now(),
                "description", "Batch service for processing ACK/NACK files"
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to get batch status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}