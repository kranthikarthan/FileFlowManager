package com.filetransfer.web.service;

import com.filetransfer.web.entity.CompressionType;
import com.filetransfer.web.entity.FileType;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for handling file compression and decompression operations
 */
@Service
public class CompressionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompressionService.class);
    private static final int BUFFER_SIZE = 8192;
    
    @Value("${file-transfer.compression.temp-dir:/tmp/compression}")
    private String tempCompressionDir;
    
    @Value("${file-transfer.compression.max-file-size-mb:1024}")
    private long maxFileSizeMB;
    
    @Value("${file-transfer.compression.default-type:GZIP}")
    private CompressionType defaultCompressionType;
    
    /**
     * Compress a file using the specified compression type
     */
    public CompressionResult compressFile(Path sourceFile, CompressionType compressionType, String targetDirectory) {
        if (compressionType == CompressionType.NONE) {
            return new CompressionResult(sourceFile, sourceFile, CompressionType.NONE, 0L, 1.0f);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate file size
            long fileSize = Files.size(sourceFile);
            if (fileSize > maxFileSizeMB * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds maximum allowed size: " + maxFileSizeMB + "MB");
            }
            
            // Create target file path
            String originalFileName = sourceFile.getFileName().toString();
            String compressedFileName = originalFileName + compressionType.getFileExtension();
            Path targetFile = Paths.get(targetDirectory, compressedFileName);
            
            // Ensure target directory exists
            Files.createDirectories(targetFile.getParent());
            
            // Perform compression
            long compressedSize = performCompression(sourceFile, targetFile, compressionType);
            
            long compressionTime = System.currentTimeMillis() - startTime;
            float compressionRatio = (float) compressedSize / fileSize;
            
            logger.info("Compressed file {} using {}: {} bytes -> {} bytes (ratio: {:.2f}, time: {}ms)",
                       originalFileName, compressionType, fileSize, compressedSize, compressionRatio, compressionTime);
            
            return new CompressionResult(sourceFile, targetFile, compressionType, compressionTime, compressionRatio);
            
        } catch (Exception e) {
            logger.error("Failed to compress file {} using {}: {}", sourceFile, compressionType, e.getMessage());
            throw new RuntimeException("Compression failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decompress a file using the detected compression type
     */
    public DecompressionResult decompressFile(Path compressedFile, String targetDirectory) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Detect compression type from file extension
            CompressionType compressionType = CompressionType.fromFileExtension(compressedFile.getFileName().toString());
            
            if (compressionType == CompressionType.NONE) {
                // File is not compressed, just copy it
                String fileName = compressedFile.getFileName().toString();
                Path targetFile = Paths.get(targetDirectory, fileName);
                Files.copy(compressedFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return new DecompressionResult(compressedFile, targetFile, CompressionType.NONE, 0L);
            }
            
            // Create target file path (remove compression extension)
            String compressedFileName = compressedFile.getFileName().toString();
            String originalFileName = compressedFileName.substring(0, 
                compressedFileName.length() - compressionType.getFileExtension().length());
            Path targetFile = Paths.get(targetDirectory, originalFileName);
            
            // Ensure target directory exists
            Files.createDirectories(targetFile.getParent());
            
            // Perform decompression
            performDecompression(compressedFile, targetFile, compressionType);
            
            long decompressionTime = System.currentTimeMillis() - startTime;
            
            logger.info("Decompressed file {} using {}: time: {}ms", 
                       compressedFileName, compressionType, decompressionTime);
            
            return new DecompressionResult(compressedFile, targetFile, compressionType, decompressionTime);
            
        } catch (Exception e) {
            logger.error("Failed to decompress file {}: {}", compressedFile, e.getMessage());
            throw new RuntimeException("Decompression failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get recommended compression type for a file
     */
    public CompressionType getRecommendedCompression(Path file, FileType fileType, boolean prioritizeSpeed) {
        try {
            long fileSize = Files.size(file);
            
            // Don't compress if file type doesn't benefit from compression
            if (!CompressionType.shouldCompress(fileType)) {
                return CompressionType.NONE;
            }
            
            return CompressionType.getRecommended(fileSize, prioritizeSpeed);
            
        } catch (IOException e) {
            logger.warn("Could not determine file size for {}, using default compression", file);
            return defaultCompressionType;
        }
    }
    
    /**
     * Test compression efficiency for a file with different algorithms
     */
    public CompressionTestResult testCompressionEfficiency(Path file) {
        CompressionTestResult result = new CompressionTestResult();
        result.setOriginalSize(getFileSize(file));
        
        for (CompressionType type : CompressionType.values()) {
            if (type == CompressionType.NONE) continue;
            
            try {
                String tempDir = tempCompressionDir + "/test";
                CompressionResult compressionResult = compressFile(file, type, tempDir);
                
                result.addTestResult(type, compressionResult.getCompressionRatio(), 
                                   compressionResult.getCompressionTimeMs());
                
                // Clean up test file
                Files.deleteIfExists(compressionResult.getCompressedFile());
                
            } catch (Exception e) {
                logger.warn("Failed to test compression with {}: {}", type, e.getMessage());
                result.addTestResult(type, 1.0f, -1L); // Mark as failed
            }
        }
        
        return result;
    }
    
    // Private helper methods
    
    private long performCompression(Path sourceFile, Path targetFile, CompressionType compressionType) 
            throws IOException {
        
        try (InputStream inputStream = Files.newInputStream(sourceFile);
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            
            OutputStream compressorStream = createCompressorOutputStream(outputStream, compressionType);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                compressorStream.write(buffer, 0, bytesRead);
            }
            
            compressorStream.close();
        }
        
        return Files.size(targetFile);
    }
    
    private void performDecompression(Path compressedFile, Path targetFile, CompressionType compressionType) 
            throws IOException {
        
        try (InputStream inputStream = Files.newInputStream(compressedFile);
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            
            InputStream decompressorStream = createDecompressorInputStream(inputStream, compressionType);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = decompressorStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            decompressorStream.close();
        }
    }
    
    private OutputStream createCompressorOutputStream(OutputStream outputStream, CompressionType type) 
            throws IOException {
        switch (type) {
            case GZIP:
                return new GZIPOutputStream(outputStream);
            case ZIP:
                ZipOutputStream zipOut = new ZipOutputStream(outputStream);
                zipOut.putNextEntry(new ZipEntry("data"));
                return zipOut;
            case BZIP2:
                return new BZip2CompressorOutputStream(outputStream);
            case XZ:
                return new XZCompressorOutputStream(outputStream);
            case LZ4:
                return new FramedLZ4CompressorOutputStream(outputStream);
            case ZSTD:
                return new ZstdCompressorOutputStream(outputStream);
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + type);
        }
    }
    
    private InputStream createDecompressorInputStream(InputStream inputStream, CompressionType type) 
            throws IOException {
        switch (type) {
            case GZIP:
                return new GZIPInputStream(inputStream);
            case ZIP:
                ZipInputStream zipIn = new ZipInputStream(inputStream);
                zipIn.getNextEntry(); // Read the entry
                return zipIn;
            case BZIP2:
                return new BZip2CompressorInputStream(inputStream);
            case XZ:
                return new XZCompressorInputStream(inputStream);
            case LZ4:
                return new FramedLZ4CompressorInputStream(inputStream);
            case ZSTD:
                return new ZstdCompressorInputStream(inputStream);
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + type);
        }
    }
    
    private long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            logger.warn("Could not get file size for {}: {}", file, e.getMessage());
            return 0L;
        }
    }
    
    // Public Result classes for external access
    
    public static class CompressionResult {
        private final Path originalFile;
        private final Path compressedFile;
        private final CompressionType compressionType;
        private final long compressionTimeMs;
        private final float compressionRatio;
        
        public CompressionResult(Path originalFile, Path compressedFile, CompressionType compressionType, 
                               long compressionTimeMs, float compressionRatio) {
            this.originalFile = originalFile;
            this.compressedFile = compressedFile;
            this.compressionType = compressionType;
            this.compressionTimeMs = compressionTimeMs;
            this.compressionRatio = compressionRatio;
        }
        
        // Getters
        public Path getOriginalFile() { return originalFile; }
        public Path getCompressedFile() { return compressedFile; }
        public CompressionType getCompressionType() { return compressionType; }
        public long getCompressionTimeMs() { return compressionTimeMs; }
        public float getCompressionRatio() { return compressionRatio; }
    }
    
    public static class DecompressionResult {
        private final Path compressedFile;
        private final Path decompressedFile;
        private final CompressionType compressionType;
        private final long decompressionTimeMs;
        
        public DecompressionResult(Path compressedFile, Path decompressedFile, 
                                 CompressionType compressionType, long decompressionTimeMs) {
            this.compressedFile = compressedFile;
            this.decompressedFile = decompressedFile;
            this.compressionType = compressionType;
            this.decompressionTimeMs = decompressionTimeMs;
        }
        
        // Getters
        public Path getCompressedFile() { return compressedFile; }
        public Path getDecompressedFile() { return decompressedFile; }
        public CompressionType getCompressionType() { return compressionType; }
        public long getDecompressionTimeMs() { return decompressionTimeMs; }
    }
    
    public static class CompressionTestResult {
        private long originalSize;
        private Map<CompressionType, TestResult> results = new HashMap<>();
        
        public void setOriginalSize(long originalSize) {
            this.originalSize = originalSize;
        }
        
        public void addTestResult(CompressionType type, float ratio, long timeMs) {
            results.put(type, new TestResult(ratio, timeMs));
        }
        
        public CompressionType getBestCompressionType(boolean prioritizeSpeed) {
            return results.entrySet().stream()
                .filter(entry -> entry.getValue().timeMs > 0) // Exclude failed tests
                .min((a, b) -> {
                    if (prioritizeSpeed) {
                        return Long.compare(a.getValue().timeMs, b.getValue().timeMs);
                    } else {
                        return Float.compare(a.getValue().ratio, b.getValue().ratio);
                    }
                })
                .map(Map.Entry::getKey)
                .orElse(CompressionType.GZIP);
        }
        
        // Getters
        public long getOriginalSize() { return originalSize; }
        public Map<CompressionType, TestResult> getResults() { return results; }
        
        public static class TestResult {
            private final float ratio;
            private final long timeMs;
            
            public TestResult(float ratio, long timeMs) {
                this.ratio = ratio;
                this.timeMs = timeMs;
            }
            
            public float getRatio() { return ratio; }
            public long getTimeMs() { return timeMs; }
        }
    }
}