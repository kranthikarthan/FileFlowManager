package com.filetransfer.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for file preview operations
 */
@RestController
@RequestMapping("/api/v1/file-preview")
public class FilePreviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(FilePreviewController.class);
    
    // Maximum file size for preview (10MB)
    private static final long MAX_PREVIEW_SIZE = 10 * 1024 * 1024;
    
    // Maximum lines to preview for text files
    private static final int MAX_PREVIEW_LINES = 1000;
    
    /**
     * Get file preview content
     */
    @GetMapping("/content")
    public ResponseEntity<Map<String, Object>> getFilePreview(
            @RequestParam String filePath,
            @RequestParam(required = false, defaultValue = "100") int maxLines) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            
            long fileSize = Files.size(path);
            if (fileSize > MAX_PREVIEW_SIZE) {
                Map<String, Object> result = new HashMap<>();
                result.put("error", "File too large for preview");
                result.put("fileSize", fileSize);
                result.put("maxSize", MAX_PREVIEW_SIZE);
                return ResponseEntity.badRequest().body(result);
            }
            
            String fileName = path.getFileName().toString();
            String fileExtension = getFileExtension(fileName);
            
            Map<String, Object> preview = new HashMap<>();
            preview.put("fileName", fileName);
            preview.put("fileSize", fileSize);
            preview.put("fileExtension", fileExtension);
            preview.put("previewType", determinePreviewType(fileExtension));
            
            // Check if file is likely binary
            if (isBinaryFile(path)) {
                preview.put("isBinary", true);
                preview.put("content", "Binary file - preview not available");
                preview.put("hexPreview", getHexPreview(path, 256)); // First 256 bytes as hex
            } else {
                preview.put("isBinary", false);
                
                // Read text content
                List<String> lines = Files.lines(path)
                        .limit(Math.min(maxLines, MAX_PREVIEW_LINES))
                        .collect(Collectors.toList());
                
                preview.put("content", lines);
                preview.put("totalLines", lines.size());
                preview.put("truncated", lines.size() >= maxLines);
                
                // Additional analysis for structured files
                if (isStructuredFile(fileExtension)) {
                    preview.put("structureInfo", analyzeStructure(lines, fileExtension));
                }
            }
            
            logger.debug("Generated preview for file: {} (size: {} bytes)", fileName, fileSize);
            return ResponseEntity.ok(preview);
            
        } catch (IOException e) {
            logger.error("Error reading file for preview {}: {}", filePath, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Error generating preview for {}: {}", filePath, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Download file for preview
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFileForPreview(@RequestParam String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            
            long fileSize = Files.size(path);
            if (fileSize > MAX_PREVIEW_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }
            
            byte[] fileContent = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentType(determineMediaType(getFileExtension(fileName)));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
                    
        } catch (IOException e) {
            logger.error("Error downloading file for preview {}: {}", filePath, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file metadata for preview
     */
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@RequestParam String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", path.getFileName().toString());
            metadata.put("fileSize", Files.size(path));
            metadata.put("lastModified", Files.getLastModifiedTime(path).toInstant());
            metadata.put("isReadable", Files.isReadable(path));
            metadata.put("isWritable", Files.isWritable(path));
            metadata.put("fileExtension", getFileExtension(path.getFileName().toString()));
            metadata.put("mimeType", Files.probeContentType(path));
            metadata.put("canPreview", canPreview(path));
            
            return ResponseEntity.ok(metadata);
            
        } catch (IOException e) {
            logger.error("Error getting file metadata {}: {}", filePath, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }
    
    private String determinePreviewType(String extension) {
        switch (extension.toLowerCase()) {
            case ".txt":
            case ".log":
            case ".dat":
                return "text";
            case ".csv":
                return "csv";
            case ".json":
                return "json";
            case ".xml":
                return "xml";
            case ".yaml":
            case ".yml":
                return "yaml";
            case ".html":
                return "html";
            case ".css":
                return "css";
            case ".js":
                return "javascript";
            case ".sql":
                return "sql";
            case ".py":
                return "python";
            case ".java":
                return "java";
            case ".pdf":
                return "pdf";
            case ".jpg":
            case ".jpeg":
            case ".png":
            case ".gif":
                return "image";
            default:
                return "unknown";
        }
    }
    
    private MediaType determineMediaType(String extension) {
        switch (extension.toLowerCase()) {
            case ".txt":
            case ".log":
            case ".dat":
                return MediaType.TEXT_PLAIN;
            case ".csv":
                return MediaType.parseMediaType("text/csv");
            case ".json":
                return MediaType.APPLICATION_JSON;
            case ".xml":
                return MediaType.APPLICATION_XML;
            case ".html":
                return MediaType.TEXT_HTML;
            case ".css":
                return MediaType.parseMediaType("text/css");
            case ".js":
                return MediaType.parseMediaType("application/javascript");
            case ".pdf":
                return MediaType.APPLICATION_PDF;
            case ".jpg":
            case ".jpeg":
                return MediaType.IMAGE_JPEG;
            case ".png":
                return MediaType.IMAGE_PNG;
            case ".gif":
                return MediaType.IMAGE_GIF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
    
    private boolean isBinaryFile(Path filePath) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = Files.newInputStream(filePath).read(buffer);
        
        if (bytesRead <= 0) return false;
        
        // Check for null bytes (strong indicator of binary content)
        for (int i = 0; i < bytesRead; i++) {
            if (buffer[i] == 0) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isStructuredFile(String extension) {
        return extension.matches("\\.(csv|json|xml|yaml|yml)");
    }
    
    private boolean canPreview(Path filePath) {
        try {
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_PREVIEW_SIZE) {
                return false;
            }
            
            String fileName = filePath.getFileName().toString();
            String extension = getFileExtension(fileName);
            
            // Check if it's a supported preview type
            String previewType = determinePreviewType(extension);
            return !"unknown".equals(previewType) && !isBinaryFile(filePath);
            
        } catch (IOException e) {
            return false;
        }
    }
    
    private String getHexPreview(Path filePath, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int bytesRead = Files.newInputStream(filePath).read(buffer);
        
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bytesRead; i++) {
            if (i > 0 && i % 16 == 0) {
                hex.append("\n");
            } else if (i > 0 && i % 8 == 0) {
                hex.append("  ");
            } else if (i > 0) {
                hex.append(" ");
            }
            hex.append(String.format("%02X", buffer[i]));
        }
        
        return hex.toString();
    }
    
    private Map<String, Object> analyzeStructure(List<String> lines, String extension) {
        Map<String, Object> structure = new HashMap<>();
        
        if (lines.isEmpty()) {
            return structure;
        }
        
        switch (extension.toLowerCase()) {
            case ".csv":
                String[] headers = lines.get(0).split(",");
                structure.put("columnCount", headers.length);
                structure.put("hasHeader", true);
                structure.put("headers", List.of(headers));
                structure.put("sampleRowCount", Math.min(lines.size() - 1, 5));
                
                if (lines.size() > 1) {
                    List<String[]> sampleRows = lines.stream()
                            .skip(1)
                            .limit(5)
                            .map(line -> line.split(","))
                            .collect(Collectors.toList());
                    structure.put("sampleData", sampleRows);
                }
                break;
                
            case ".json":
                String content = String.join("\n", lines);
                structure.put("isArray", content.trim().startsWith("["));
                structure.put("isObject", content.trim().startsWith("{"));
                structure.put("estimatedSize", content.length());
                break;
                
            case ".xml":
                long elementCount = lines.stream()
                        .mapToLong(line -> line.chars().filter(ch -> ch == '<').count())
                        .sum() / 2; // Rough estimate
                structure.put("estimatedElements", elementCount);
                structure.put("hasXmlDeclaration", lines.get(0).trim().startsWith("<?xml"));
                break;
        }
        
        return structure;
    }
}