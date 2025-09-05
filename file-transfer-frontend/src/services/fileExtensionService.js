import { api } from './api';

/**
 * Service for file extension operations and utilities
 */
export const fileExtensionService = {
    
    /**
     * Get file transfers by file extension
     */
    async getFileTransfersByExtension(tenantId, extension) {
        try {
            const response = await api.get(`/api/v1/file-transfers/extension/${extension}`, {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching file transfers by extension:', error);
            throw error;
        }
    },
    
    /**
     * Get distinct file extensions for a tenant
     */
    async getDistinctFileExtensions(tenantId) {
        try {
            const response = await api.get('/api/v1/file-transfers/extensions', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching distinct file extensions:', error);
            throw error;
        }
    },
    
    /**
     * Get file extension statistics for a tenant
     */
    async getFileExtensionStatistics(tenantId) {
        try {
            const response = await api.get('/api/v1/file-transfers/statistics/extensions', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching file extension statistics:', error);
            throw error;
        }
    },
    
    /**
     * Extract file extension from filename
     */
    extractExtension(fileName) {
        if (!fileName || typeof fileName !== 'string') {
            return null;
        }
        
        const lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }
        
        return null;
    },
    
    /**
     * Normalize file extension (ensure lowercase and starts with dot)
     */
    normalizeExtension(extension) {
        if (!extension || typeof extension !== 'string') {
            return null;
        }
        
        extension = extension.trim().toLowerCase();
        
        // Add dot if missing
        if (!extension.startsWith('.')) {
            extension = '.' + extension;
        }
        
        return extension;
    },
    
    /**
     * Get file category based on extension
     */
    getFileCategory(extension) {
        if (!extension) {
            return 'Unknown';
        }
        
        extension = this.normalizeExtension(extension);
        
        const categories = {
            'Text Files': ['.txt', '.dat', '.log', '.csv', '.tsv'],
            'Data Files': ['.json', '.xml', '.yaml', '.yml', '.properties'],
            'Archive Files': ['.zip', '.gz', '.tar', '.bz2', '.xz', '.7z'],
            'Document Files': ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'],
            'Image Files': ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.tiff', '.svg'],
            'Binary Files': ['.exe', '.dll', '.so', '.dylib', '.bin'],
            'Database Files': ['.db', '.sqlite', '.mdb', '.accdb'],
            'Script Files': ['.sh', '.bat', '.ps1', '.py', '.js', '.sql']
        };
        
        for (const [category, extensions] of Object.entries(categories)) {
            if (extensions.includes(extension)) {
                return category;
            }
        }
        
        return 'Other';
    },
    
    /**
     * Get file extension icon for UI display
     */
    getExtensionIcon(extension) {
        if (!extension) {
            return 'description';
        }
        
        extension = this.normalizeExtension(extension);
        
        const iconMap = {
            '.txt': 'description',
            '.csv': 'table_chart',
            '.json': 'code',
            '.xml': 'code',
            '.pdf': 'picture_as_pdf',
            '.zip': 'folder_zip',
            '.gz': 'folder_zip',
            '.tar': 'folder_zip',
            '.jpg': 'image',
            '.jpeg': 'image',
            '.png': 'image',
            '.gif': 'image',
            '.sql': 'storage',
            '.log': 'article',
            '.dat': 'data_object',
            '.exe': 'apps',
            '.dll': 'extension',
            '.sh': 'terminal',
            '.bat': 'terminal',
            '.py': 'code',
            '.js': 'code',
            '.html': 'web',
            '.css': 'palette'
        };
        
        return iconMap[extension] || 'insert_drive_file';
    },
    
    /**
     * Get file extension color for UI display
     */
    getExtensionColor(extension) {
        if (!extension) {
            return 'default';
        }
        
        const category = this.getFileCategory(extension);
        
        const colorMap = {
            'Text Files': 'primary',
            'Data Files': 'secondary',
            'Archive Files': 'warning',
            'Document Files': 'info',
            'Image Files': 'success',
            'Binary Files': 'error',
            'Database Files': 'primary',
            'Script Files': 'secondary',
            'Other': 'default',
            'Unknown': 'default'
        };
        
        return colorMap[category] || 'default';
    },
    
    /**
     * Check if file extension should be compressed
     */
    shouldCompress(extension) {
        if (!extension) {
            return false;
        }
        
        extension = this.normalizeExtension(extension);
        
        const compressibleExtensions = new Set([
            '.txt', '.dat', '.log', '.csv', '.tsv', '.json', '.xml', 
            '.yaml', '.yml', '.sql', '.html', '.css', '.js', '.py'
        ]);
        
        const alreadyCompressedExtensions = new Set([
            '.zip', '.gz', '.bz2', '.xz', '.7z', '.rar', '.jpg', '.jpeg', 
            '.png', '.gif', '.mp3', '.mp4', '.avi', '.mov', '.pdf'
        ]);
        
        return compressibleExtensions.has(extension) && !alreadyCompressedExtensions.has(extension);
    },
    
    /**
     * Check if file extension indicates binary content
     */
    isBinaryExtension(extension) {
        if (!extension) {
            return false;
        }
        
        extension = this.normalizeExtension(extension);
        
        const binaryExtensions = new Set([
            '.exe', '.dll', '.so', '.dylib', '.bin', '.obj', '.lib', 
            '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.ico', '.pdf',
            '.zip', '.gz', '.bz2', '.xz', '.7z', '.rar'
        ]);
        
        return binaryExtensions.has(extension);
    },
    
    /**
     * Validate file extension format
     */
    validateExtension(extension) {
        const errors = [];
        
        if (extension === null || extension === undefined) {
            return { isValid: true, errors: [], message: 'Extension is optional' };
        }
        
        if (typeof extension !== 'string') {
            errors.push('Extension must be a string');
            return { isValid: false, errors };
        }
        
        extension = extension.trim();
        
        if (extension === '') {
            return { isValid: true, errors: [], message: 'Empty extension is valid (optional)' };
        }
        
        if (!extension.startsWith('.')) {
            errors.push('Extension must start with a dot (.)');
        }
        
        if (extension.length < 2) {
            errors.push('Extension must have at least one character after the dot');
        }
        
        if (extension.length > 10) {
            errors.push('Extension cannot be longer than 10 characters');
        }
        
        if (!/^\.?[a-zA-Z0-9]+$/.test(extension)) {
            errors.push('Extension can only contain letters and numbers');
        }
        
        return {
            isValid: errors.length === 0,
            errors,
            message: errors.length === 0 ? 'Extension is valid' : 'Extension validation failed'
        };
    },
    
    /**
     * Get recommended file extensions for a service type
     */
    getRecommendedExtensions(serviceType) {
        const recommendations = {
            'CUSTOMER_DATA': ['.csv', '.txt', '.dat'],
            'TRANSACTION_DATA': ['.json', '.xml', '.csv'],
            'LOG_DATA': ['.log', '.txt'],
            'CONFIGURATION_DATA': ['.yaml', '.yml', '.properties', '.json'],
            'REPORT_DATA': ['.pdf', '.csv', '.xlsx'],
            'BACKUP_DATA': ['.zip', '.gz', '.tar'],
            'ARCHIVE_DATA': ['.zip', '.7z', '.tar.gz']
        };
        
        return recommendations[serviceType] || ['.txt', '.dat', '.csv', '.json'];
    },
    
    /**
     * Format extension statistics for display
     */
    formatExtensionStatistics(statistics) {
        const total = Object.values(statistics).reduce((sum, count) => sum + count, 0);
        
        return Object.entries(statistics)
            .map(([extension, count]) => ({
                extension,
                count,
                percentage: total > 0 ? Math.round((count / total) * 100 * 100) / 100 : 0,
                category: this.getFileCategory(extension),
                icon: this.getExtensionIcon(extension),
                color: this.getExtensionColor(extension),
                shouldCompress: this.shouldCompress(extension),
                isBinary: this.isBinaryExtension(extension)
            }))
            .sort((a, b) => b.count - a.count);
    },
    
    /**
     * Group extensions by category for display
     */
    groupExtensionsByCategory(extensions) {
        const grouped = {};
        
        extensions.forEach(ext => {
            const category = this.getFileCategory(ext);
            if (!grouped[category]) {
                grouped[category] = [];
            }
            grouped[category].push(ext);
        });
        
        // Sort extensions within each category
        Object.keys(grouped).forEach(category => {
            grouped[category].sort();
        });
        
        return grouped;
    }
};

export default fileExtensionService;