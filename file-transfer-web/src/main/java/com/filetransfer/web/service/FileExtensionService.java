package com.filetransfer.web.service;

import com.filetransfer.web.entity.FileType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for file extension validation, categorization, and utility operations
 */
@Service
public class FileExtensionService {
    
    // Common file extension categories
    private static final Map<String, Set<String>> EXTENSION_CATEGORIES = Map.of(
        "Text Files", Set.of(".txt", ".dat", ".log", ".csv", ".tsv"),
        "Data Files", Set.of(".json", ".xml", ".yaml", ".yml", ".properties"),
        "Archive Files", Set.of(".zip", ".gz", ".tar", ".bz2", ".xz", ".7z"),
        "Document Files", Set.of(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"),
        "Image Files", Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".svg"),
        "Binary Files", Set.of(".exe", ".dll", ".so", ".dylib", ".bin"),
        "Database Files", Set.of(".db", ".sqlite", ".mdb", ".accdb"),
        "Script Files", Set.of(".sh", ".bat", ".ps1", ".py", ".js", ".sql")
    );
    
    // Extensions that typically benefit from compression
    private static final Set<String> COMPRESSIBLE_EXTENSIONS = Set.of(
        ".txt", ".dat", ".log", ".csv", ".tsv", ".json", ".xml", 
        ".yaml", ".yml", ".sql", ".html", ".css", ".js", ".py"
    );
    
    // Extensions that are typically already compressed
    private static final Set<String> ALREADY_COMPRESSED_EXTENSIONS = Set.of(
        ".zip", ".gz", ".bz2", ".xz", ".7z", ".rar", ".jpg", ".jpeg", 
        ".png", ".gif", ".mp3", ".mp4", ".avi", ".mov", ".pdf"
    );
    
    // Extensions that require special handling
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
        ".exe", ".dll", ".so", ".dylib", ".bin", ".obj", ".lib", 
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".ico"
    );
    
    /**
     * Validate file extension format
     */
    public boolean isValidExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false; // Extension is optional, but if provided must be valid
        }
        
        // Must start with dot and have at least one character after
        return extension.matches("^\\.[a-zA-Z0-9]+$") && extension.length() >= 2 && extension.length() <= 10;
    }
    
    /**
     * Normalize file extension (ensure lowercase and starts with dot)
     */
    public String normalizeExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return null;
        }
        
        extension = extension.trim().toLowerCase();
        
        // Add dot if missing
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        
        return extension;
    }
    
    /**
     * Extract file extension from filename
     */
    public String extractExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }
        
        return null; // No extension found
    }
    
    /**
     * Get file category based on extension
     */
    public String getFileCategory(String extension) {
        if (extension == null) {
            return "Unknown";
        }
        
        extension = normalizeExtension(extension);
        
        for (Map.Entry<String, Set<String>> category : EXTENSION_CATEGORIES.entrySet()) {
            if (category.getValue().contains(extension)) {
                return category.getKey();
            }
        }
        
        return "Other";
    }
    
    /**
     * Check if file extension typically benefits from compression
     */
    public boolean shouldCompress(String extension) {
        if (extension == null) {
            return false;
        }
        
        extension = normalizeExtension(extension);
        return COMPRESSIBLE_EXTENSIONS.contains(extension) && 
               !ALREADY_COMPRESSED_EXTENSIONS.contains(extension);
    }
    
    /**
     * Check if file extension indicates binary content
     */
    public boolean isBinaryExtension(String extension) {
        if (extension == null) {
            return false;
        }
        
        extension = normalizeExtension(extension);
        return BINARY_EXTENSIONS.contains(extension);
    }
    
    /**
     * Get recommended FileType based on extension
     */
    public FileType getRecommendedFileType(String extension) {
        if (extension == null) {
            return FileType.COBOL_FLAT_FILE; // Default
        }
        
        extension = normalizeExtension(extension);
        
        switch (extension) {
            case ".csv":
                return FileType.CSV;
            case ".json":
                return FileType.JSON;
            case ".xml":
                return FileType.XML;
            case ".txt":
            case ".dat":
                return FileType.FIXED_WIDTH;
            case ".tsv":
            case ".pipe":
                return FileType.DELIMITED;
            case ".edi":
                return FileType.EDI;
            default:
                if (isBinaryExtension(extension)) {
                    return FileType.BINARY_FILE;
                } else {
                    return FileType.COBOL_FLAT_FILE;
                }
        }
    }
    
    /**
     * Get all supported file extensions grouped by category
     */
    public Map<String, List<String>> getSupportedExtensionsByCategory() {
        return EXTENSION_CATEGORIES.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new ArrayList<>(entry.getValue())
                ));
    }
    
    /**
     * Get file extension statistics with categorization
     */
    public Map<String, Object> analyzeExtensions(List<String> extensions) {
        Map<String, Long> categoryCounts = new HashMap<>();
        Map<String, Long> extensionCounts = new HashMap<>();
        
        for (String extension : extensions) {
            if (extension != null) {
                String normalized = normalizeExtension(extension);
                String category = getFileCategory(normalized);
                
                categoryCounts.merge(category, 1L, Long::sum);
                extensionCounts.merge(normalized, 1L, Long::sum);
            }
        }
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalFiles", extensions.size());
        analysis.put("filesWithExtensions", extensions.stream().filter(Objects::nonNull).count());
        analysis.put("filesWithoutExtensions", extensions.stream().filter(Objects::isNull).count());
        analysis.put("categoryCounts", categoryCounts);
        analysis.put("extensionCounts", extensionCounts);
        analysis.put("mostCommonExtension", getMostCommonExtension(extensionCounts));
        analysis.put("compressibleFiles", extensions.stream().filter(this::shouldCompress).count());
        analysis.put("binaryFiles", extensions.stream().filter(this::isBinaryExtension).count());
        
        return analysis;
    }
    
    /**
     * Validate file extension against allowed extensions for a service
     */
    public ValidationResult validateExtensionForService(String extension, Set<String> allowedExtensions, 
                                                       Set<String> blockedExtensions) {
        if (extension == null) {
            return new ValidationResult(true, "No extension provided (optional)");
        }
        
        extension = normalizeExtension(extension);
        
        // Check if extension is blocked
        if (blockedExtensions != null && blockedExtensions.contains(extension)) {
            return new ValidationResult(false, "File extension is blocked: " + extension);
        }
        
        // Check if extension is allowed (if allowlist is defined)
        if (allowedExtensions != null && !allowedExtensions.isEmpty() && 
            !allowedExtensions.contains(extension)) {
            return new ValidationResult(false, "File extension is not allowed: " + extension);
        }
        
        return new ValidationResult(true, "File extension is valid");
    }
    
    private String getMostCommonExtension(Map<String, Long> extensionCounts) {
        return extensionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    // Result classes
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}