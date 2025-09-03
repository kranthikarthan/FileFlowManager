import { api } from './api';

/**
 * Service for file compression and decompression operations
 */
export const compressionService = {
    
    /**
     * Get all available compression types
     */
    async getCompressionTypes() {
        try {
            const response = await api.get('/api/v1/compression/types');
            return response.data;
        } catch (error) {
            console.error('Error fetching compression types:', error);
            throw error;
        }
    },
    
    /**
     * Compress a file transfer
     */
    async compressFileTransfer(fileTransferId, compressionType) {
        try {
            const response = await api.post(`/api/v1/compression/compress/${fileTransferId}`, null, {
                params: { compressionType }
            });
            return response.data;
        } catch (error) {
            console.error('Error compressing file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Decompress a file transfer
     */
    async decompressFileTransfer(fileTransferId) {
        try {
            const response = await api.post(`/api/v1/compression/decompress/${fileTransferId}`);
            return response.data;
        } catch (error) {
            console.error('Error decompressing file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Get compression recommendations for a file transfer
     */
    async getCompressionRecommendations(fileTransferId) {
        try {
            const response = await api.get(`/api/v1/compression/recommendations/${fileTransferId}`);
            return response.data;
        } catch (error) {
            console.error('Error getting compression recommendations:', error);
            throw error;
        }
    },
    
    /**
     * Test compression efficiency for an uploaded file
     */
    async testCompressionEfficiency(file) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            
            const response = await api.post('/api/v1/compression/test-efficiency', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            return response.data;
        } catch (error) {
            console.error('Error testing compression efficiency:', error);
            throw error;
        }
    },
    
    /**
     * Compress an uploaded file
     */
    async compressUploadedFile(file, compressionType) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            
            const response = await api.post('/api/v1/compression/compress-file', formData, {
                params: { compressionType },
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            return response.data;
        } catch (error) {
            console.error('Error compressing uploaded file:', error);
            throw error;
        }
    },
    
    /**
     * Get compression statistics for a tenant
     */
    async getCompressionStatistics(tenantId) {
        try {
            const response = await api.get(`/api/v1/compression/statistics/${tenantId}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching compression statistics:', error);
            throw error;
        }
    },
    
    /**
     * Format file size for display
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    
    /**
     * Calculate compression savings
     */
    calculateSavings(originalSize, compressedSize) {
        if (!originalSize || !compressedSize) return 0;
        
        const savings = originalSize - compressedSize;
        const percentage = (savings / originalSize) * 100;
        
        return {
            bytes: savings,
            percentage: Math.round(percentage * 100) / 100,
            formatted: this.formatFileSize(savings)
        };
    },
    
    /**
     * Get compression type color for UI display
     */
    getCompressionTypeColor(compressionType) {
        const colorMap = {
            'NONE': 'default',
            'GZIP': 'primary',
            'ZIP': 'secondary',
            'BZIP2': 'success',
            'XZ': 'info',
            'LZ4': 'warning',
            'ZSTD': 'error'
        };
        return colorMap[compressionType] || 'default';
    },
    
    /**
     * Get compression type icon
     */
    getCompressionTypeIcon(compressionType) {
        const iconMap = {
            'NONE': 'description',
            'GZIP': 'compress',
            'ZIP': 'folder_zip',
            'BZIP2': 'compress',
            'XZ': 'compress',
            'LZ4': 'speed',
            'ZSTD': 'balance'
        };
        return iconMap[compressionType] || 'compress';
    },
    
    /**
     * Validate file for compression
     */
    validateFileForCompression(file, maxSizeMB = 1024) {
        const errors = [];
        
        if (!file) {
            errors.push('No file selected');
            return { isValid: false, errors };
        }
        
        if (file.size === 0) {
            errors.push('File is empty');
        }
        
        if (file.size > maxSizeMB * 1024 * 1024) {
            errors.push(`File size exceeds maximum allowed size (${maxSizeMB}MB)`);
        }
        
        // Check if file is already compressed
        const fileName = file.name.toLowerCase();
        const compressedExtensions = ['.gz', '.zip', '.bz2', '.xz', '.lz4', '.zst'];
        if (compressedExtensions.some(ext => fileName.endsWith(ext))) {
            errors.push('File appears to already be compressed');
        }
        
        return {
            isValid: errors.length === 0,
            errors
        };
    },
    
    /**
     * Get recommended compression type based on file characteristics
     */
    getRecommendedCompressionType(file, prioritizeSpeed = false) {
        const fileSizeMB = file.size / (1024 * 1024);
        const fileName = file.name.toLowerCase();
        
        // Check file type
        const textExtensions = ['.txt', '.csv', '.json', '.xml', '.dat', '.log'];
        const isTextFile = textExtensions.some(ext => fileName.endsWith(ext));
        
        if (fileSizeMB < 1) {
            return 'NONE'; // Small files don't benefit much from compression
        }
        
        if (!isTextFile) {
            return 'NONE'; // Binary files may already be compressed
        }
        
        if (prioritizeSpeed) {
            return fileSizeMB > 100 ? 'LZ4' : 'ZSTD';
        } else {
            return fileSizeMB > 100 ? 'GZIP' : 'XZ';
        }
    },
    
    /**
     * Estimate compression time based on file size and type
     */
    estimateCompressionTime(fileSizeBytes, compressionType) {
        const fileSizeMB = fileSizeBytes / (1024 * 1024);
        
        // Rough estimates in milliseconds per MB
        const timePerMB = {
            'GZIP': 100,
            'ZIP': 120,
            'BZIP2': 300,
            'XZ': 500,
            'LZ4': 50,
            'ZSTD': 80
        };
        
        const baseTime = timePerMB[compressionType] || 100;
        return Math.round(fileSizeMB * baseTime);
    }
};

export default compressionService;