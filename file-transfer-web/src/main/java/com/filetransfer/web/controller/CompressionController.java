package com.filetransfer.web.controller;

import com.filetransfer.web.entity.CompressionType;
import com.filetransfer.web.service.CompressionService;
import com.filetransfer.web.service.FileTransferManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for file compression and decompression operations
 */
@RestController
@RequestMapping("/api/v1/compression")
@CrossOrigin(origins = "*")
public class CompressionController {
    
    private static final Logger logger = LoggerFactory.getLogger(CompressionController.class);
    
    @Autowired
    private CompressionService compressionService;
    
    @Autowired
    private FileTransferManagementService fileTransferManagementService;
    
    /**
     * Get all available compression types
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getCompressionTypes() {
        try {
            List<Map<String, Object>> compressionTypes = Arrays.stream(CompressionType.values())
                .map(type -> Map.of(
                    "type", type.name(),
                    "displayName", type.getDisplayName(),
                    "description", type.getDescription(),
                    "fileExtension", type.getFileExtension(),
                    "averageCompressionRatio", type.getAverageCompressionRatio(),
                    "isFastCompression", type.isFastCompression(),
                    "isHighCompressionRatio", type.isHighCompressionRatio()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(compressionTypes);
        } catch (Exception e) {
            logger.error("Error retrieving compression types: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Compress a file transfer
     */
    @PostMapping("/compress/{fileTransferId}")
    public ResponseEntity<Map<String, String>> compressFileTransfer(
            @PathVariable Long fileTransferId,
            @RequestParam CompressionType compressionType) {
        try {
            fileTransferManagementService.compressFile(fileTransferId, compressionType);
            return ResponseEntity.ok(Map.of(
                "message", "File compressed successfully",
                "fileTransferId", fileTransferId.toString(),
                "compressionType", compressionType.name()
            ));
        } catch (Exception e) {
            logger.error("Error compressing file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Decompress a file transfer
     */
    @PostMapping("/decompress/{fileTransferId}")
    public ResponseEntity<Map<String, String>> decompressFileTransfer(@PathVariable Long fileTransferId) {
        try {
            fileTransferManagementService.decompressFile(fileTransferId);
            return ResponseEntity.ok(Map.of(
                "message", "File decompressed successfully",
                "fileTransferId", fileTransferId.toString()
            ));
        } catch (Exception e) {
            logger.error("Error decompressing file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get compression recommendations for a file transfer
     */
    @GetMapping("/recommendations/{fileTransferId}")
    public ResponseEntity<Map<String, Object>> getCompressionRecommendations(@PathVariable Long fileTransferId) {
        try {
            Map<String, Object> recommendations = fileTransferManagementService.getCompressionRecommendations(fileTransferId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            logger.error("Error getting compression recommendations for file transfer {}: {}", fileTransferId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test compression efficiency for an uploaded file
     */
    @PostMapping("/test-efficiency")
    public ResponseEntity<Map<String, Object>> testCompressionEfficiency(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }
            
            // Save file temporarily
            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFile = Paths.get(tempDir, "compression-test-" + System.currentTimeMillis() + "-" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());
            
            try {
                // Test compression efficiency
                CompressionService.CompressionTestResult testResult = compressionService.testCompressionEfficiency(tempFile);
                
                Map<String, Object> response = Map.of(
                    "originalSize", testResult.getOriginalSize(),
                    "results", testResult.getResults(),
                    "bestForSpeed", testResult.getBestCompressionType(true),
                    "bestForRatio", testResult.getBestCompressionType(false)
                );
                
                return ResponseEntity.ok(response);
                
            } finally {
                // Clean up temp file
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            logger.error("Error testing compression efficiency: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Compress an uploaded file and return download link
     */
    @PostMapping("/compress-file")
    public ResponseEntity<Map<String, Object>> compressUploadedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam CompressionType compressionType) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }
            
            // Save file temporarily
            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFile = Paths.get(tempDir, "upload-" + System.currentTimeMillis() + "-" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());
            
            try {
                // Compress the file
                CompressionService.CompressionResult result = compressionService.compressFile(
                    tempFile, compressionType, tempDir);
                
                // Prepare response
                Map<String, Object> response = Map.of(
                    "originalFileName", file.getOriginalFilename(),
                    "compressedFileName", result.getCompressedFile().getFileName().toString(),
                    "originalSize", Files.size(tempFile),
                    "compressedSize", Files.size(result.getCompressedFile()),
                    "compressionRatio", result.getCompressionRatio(),
                    "compressionTime", result.getCompressionTimeMs(),
                    "compressionType", compressionType.name(),
                    "downloadPath", result.getCompressedFile().toString()
                );
                
                return ResponseEntity.ok(response);
                
            } finally {
                // Clean up original temp file
                Files.deleteIfExists(tempFile);
            }
            
        } catch (Exception e) {
            logger.error("Error compressing uploaded file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get compression statistics for a tenant
     */
    @GetMapping("/statistics/{tenantId}")
    public ResponseEntity<Map<String, Object>> getCompressionStatistics(@PathVariable String tenantId) {
        try {
            // This would typically query the database for compression statistics
            // For now, return basic statistics structure
            Map<String, Object> statistics = Map.of(
                "totalCompressedFiles", 0,
                "totalCompressionSavings", 0L,
                "averageCompressionRatio", 0.0f,
                "mostUsedCompressionType", CompressionType.GZIP.name(),
                "compressionTypes", Arrays.stream(CompressionType.values())
                    .filter(type -> type != CompressionType.NONE)
                    .map(type -> Map.of(
                        "type", type.name(),
                        "displayName", type.getDisplayName(),
                        "usage", 0 // Would be actual usage count from database
                    ))
                    .collect(Collectors.toList())
            );
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving compression statistics for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}