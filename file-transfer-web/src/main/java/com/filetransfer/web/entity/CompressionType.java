package com.filetransfer.web.entity;

/**
 * Supported compression algorithms for file transfer operations
 */
public enum CompressionType {
    NONE("No Compression", "Files are transferred without compression", "", 1.0f),
    GZIP("GZIP Compression", "Standard GZIP compression with good compression ratio", ".gz", 0.3f),
    ZIP("ZIP Archive", "ZIP archive format supporting multiple files", ".zip", 0.4f),
    BZIP2("BZIP2 Compression", "BZIP2 compression with high compression ratio", ".bz2", 0.25f),
    XZ("XZ Compression", "XZ compression with excellent compression ratio", ".xz", 0.2f),
    LZ4("LZ4 Fast Compression", "LZ4 compression optimized for speed", ".lz4", 0.6f),
    ZSTD("Zstandard Compression", "Modern compression with good speed/ratio balance", ".zst", 0.35f);
    
    private final String displayName;
    private final String description;
    private final String fileExtension;
    private final float averageCompressionRatio; // Estimated compression ratio (0.0-1.0)
    
    CompressionType(String displayName, String description, String fileExtension, float averageCompressionRatio) {
        this.displayName = displayName;
        this.description = description;
        this.fileExtension = fileExtension;
        this.averageCompressionRatio = averageCompressionRatio;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public float getAverageCompressionRatio() {
        return averageCompressionRatio;
    }
    
    /**
     * Check if this compression type is fast (optimized for speed)
     */
    public boolean isFastCompression() {
        return this == LZ4 || this == ZSTD;
    }
    
    /**
     * Check if this compression type provides high compression ratio
     */
    public boolean isHighCompressionRatio() {
        return this == BZIP2 || this == XZ;
    }
    
    /**
     * Get recommended compression type based on file size and requirements
     */
    public static CompressionType getRecommended(long fileSizeBytes, boolean prioritizeSpeed) {
        if (fileSizeBytes < 1024 * 1024) { // < 1MB
            return NONE; // Small files don't benefit much from compression
        }
        
        if (prioritizeSpeed) {
            return fileSizeBytes > 100 * 1024 * 1024 ? LZ4 : ZSTD; // Use LZ4 for very large files
        } else {
            return fileSizeBytes > 100 * 1024 * 1024 ? GZIP : XZ; // Use XZ for better compression on smaller files
        }
    }
    
    /**
     * Detect compression type from file extension
     */
    public static CompressionType fromFileExtension(String fileName) {
        if (fileName == null) {
            return NONE;
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        for (CompressionType type : values()) {
            if (!type.fileExtension.isEmpty() && lowerFileName.endsWith(type.fileExtension)) {
                return type;
            }
        }
        
        return NONE;
    }
    
    /**
     * Check if file type should be compressed
     */
    public static boolean shouldCompress(FileType fileType) {
        switch (fileType) {
            case BINARY_FILE:
                return false; // Binary files may already be compressed
            case COBOL_FLAT_FILE:
            case CSV:
            case FIXED_WIDTH:
            case DELIMITED:
            case XML:
            case JSON:
            case EDI:
                return true; // Text-based files benefit from compression
            default:
                return true;
        }
    }
}