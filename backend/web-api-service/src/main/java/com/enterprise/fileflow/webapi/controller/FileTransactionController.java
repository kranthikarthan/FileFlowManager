package com.enterprise.fileflow.webapi.controller;

import com.enterprise.fileflow.shared.dto.FileTransactionDto;
import com.enterprise.fileflow.shared.enums.FileStatus;
import com.enterprise.fileflow.webapi.service.FileTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for File Transaction enquiry and operations
 */
@RestController
@RequestMapping("/v1/file-transactions")
@RequiredArgsConstructor
@Tag(name = "File Transactions", description = "File transaction enquiry and operations")
public class FileTransactionController {

    private final FileTransactionService fileTransactionService;

    @GetMapping
    @Operation(summary = "Get file transactions", description = "Retrieve paginated list of file transactions with filters")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<FileTransactionDto>> getFileTransactions(
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String subServiceCode,
            @RequestParam(required = false) FileStatus status,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        
        return ResponseEntity.ok(fileTransactionService.getFileTransactions(
            serviceCode, subServiceCode, status, fileType, fromDate, toDate, pageable));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get file transaction by ID", description = "Retrieve file transaction details by transaction ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileTransactionDto> getFileTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(fileTransactionService.getFileTransaction(transactionId));
    }

    @PostMapping("/{transactionId}/resend")
    @Operation(summary = "Resend file", description = "Mark file for reprocessing")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> resendFile(@PathVariable String transactionId) {
        fileTransactionService.resendFile(transactionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get files by batch ID", description = "Retrieve all files in a batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<FileTransactionDto>> getFilesByBatch(
            @PathVariable String batchId,
            Pageable pageable) {
        return ResponseEntity.ok(fileTransactionService.getFilesByBatch(batchId, pageable));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve file processing statistics for dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getDashboardStats(
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date) {
        // Return dashboard statistics - simplified for now
        return ResponseEntity.ok("Dashboard stats endpoint - to be implemented");
    }
}