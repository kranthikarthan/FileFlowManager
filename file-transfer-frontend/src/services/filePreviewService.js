import { api } from './api';

/**
 * Service for file preview operations
 */
export const filePreviewService = {
    
    /**
     * Get file preview content
     */
    async getFilePreview(filePath, maxLines = 100) {
        try {
            const response = await api.get('/api/v1/file-preview/content', {
                params: { filePath, maxLines }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching file preview:', error);
            throw error;
        }
    },
    
    /**
     * Get file metadata
     */
    async getFileMetadata(filePath) {
        try {
            const response = await api.get('/api/v1/file-preview/metadata', {
                params: { filePath }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching file metadata:', error);
            throw error;
        }
    },
    
    /**
     * Download file for preview
     */
    async downloadFileForPreview(filePath) {
        try {
            const response = await api.get('/api/v1/file-preview/download', {
                params: { filePath },
                responseType: 'blob'
            });
            return response.data;
        } catch (error) {
            console.error('Error downloading file for preview:', error);
            throw error;
        }
    },
    
    /**
     * Check if file can be previewed
     */
    canPreviewFile(fileName, fileSize) {
        const maxSize = 10 * 1024 * 1024; // 10MB
        
        if (fileSize > maxSize) {
            return { canPreview: false, reason: 'File too large for preview' };
        }
        
        const extension = this.getFileExtension(fileName);
        const previewType = this.getPreviewType(extension);
        
        if (previewType === 'unknown') {
            return { canPreview: false, reason: 'File type not supported for preview' };
        }
        
        return { canPreview: true, previewType };
    },
    
    /**
     * Get file extension
     */
    getFileExtension(fileName) {
        if (!fileName) return '';
        
        const lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }
        return '';
    },
    
    /**
     * Determine preview type based on extension
     */
    getPreviewType(extension) {
        const typeMap = {
            '.txt': 'text',
            '.log': 'text',
            '.dat': 'text',
            '.csv': 'csv',
            '.json': 'json',
            '.xml': 'xml',
            '.yaml': 'yaml',
            '.yml': 'yaml',
            '.html': 'html',
            '.css': 'css',
            '.js': 'javascript',
            '.sql': 'sql',
            '.py': 'python',
            '.java': 'java',
            '.pdf': 'pdf',
            '.jpg': 'image',
            '.jpeg': 'image',
            '.png': 'image',
            '.gif': 'image',
            '.bmp': 'image',
            '.svg': 'image'
        };
        
        return typeMap[extension] || 'unknown';
    },
    
    /**
     * Get preview icon for file type
     */
    getPreviewIcon(previewType) {
        const iconMap = {
            'text': 'description',
            'csv': 'table_chart',
            'json': 'data_object',
            'xml': 'code',
            'yaml': 'settings',
            'html': 'web',
            'css': 'palette',
            'javascript': 'code',
            'sql': 'storage',
            'python': 'code',
            'java': 'code',
            'pdf': 'picture_as_pdf',
            'image': 'image',
            'unknown': 'insert_drive_file'
        };
        
        return iconMap[previewType] || 'insert_drive_file';
    },
    
    /**
     * Format file size for display
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    
    /**
     * Format content for display based on type
     */
    formatContent(content, previewType) {
        if (!content) return '';
        
        if (Array.isArray(content)) {
            switch (previewType) {
                case 'csv':
                    return this.formatCsvContent(content);
                case 'json':
                    return this.formatJsonContent(content);
                case 'xml':
                    return this.formatXmlContent(content);
                default:
                    return content.join('\n');
            }
        }
        
        return content.toString();
    },
    
    /**
     * Format CSV content for display
     */
    formatCsvContent(lines) {
        if (lines.length === 0) return '';
        
        // Parse CSV into table format
        const headers = lines[0].split(',').map(h => h.trim());
        const rows = lines.slice(1, 11).map(line => // Show first 10 rows
            line.split(',').map(cell => cell.trim())
        );
        
        return {
            type: 'table',
            headers,
            rows,
            totalRows: lines.length - 1,
            showingRows: Math.min(10, lines.length - 1)
        };
    },
    
    /**
     * Format JSON content for display
     */
    formatJsonContent(lines) {
        try {
            const jsonString = lines.join('\n');
            const parsed = JSON.parse(jsonString);
            return {
                type: 'json',
                formatted: JSON.stringify(parsed, null, 2),
                raw: jsonString,
                isValid: true
            };
        } catch (error) {
            return {
                type: 'json',
                formatted: lines.join('\n'),
                raw: lines.join('\n'),
                isValid: false,
                error: error.message
            };
        }
    },
    
    /**
     * Format XML content for display
     */
    formatXmlContent(lines) {
        const xmlString = lines.join('\n');
        
        // Basic XML formatting (indentation)
        try {
            const formatted = this.formatXmlString(xmlString);
            return {
                type: 'xml',
                formatted,
                raw: xmlString,
                isValid: true
            };
        } catch (error) {
            return {
                type: 'xml',
                formatted: xmlString,
                raw: xmlString,
                isValid: false,
                error: error.message
            };
        }
    },
    
    /**
     * Basic XML formatting
     */
    formatXmlString(xml) {
        let formatted = '';
        let indent = '';
        const tab = '  ';
        
        xml.split(/>\s*</).forEach((node) => {
            if (node.match(/^\/\w/)) {
                indent = indent.substring(tab.length);
            }
            
            formatted += indent + '<' + node + '>\n';
            
            if (node.match(/^<?\w[^>]*[^\/]$/)) {
                indent += tab;
            }
        });
        
        return formatted.substring(1, formatted.length - 2);
    },
    
    /**
     * Get syntax highlighting language for code files
     */
    getSyntaxLanguage(previewType) {
        const languageMap = {
            'json': 'json',
            'xml': 'xml',
            'yaml': 'yaml',
            'html': 'html',
            'css': 'css',
            'javascript': 'javascript',
            'sql': 'sql',
            'python': 'python',
            'java': 'java'
        };
        
        return languageMap[previewType] || 'text';
    },
    
    /**
     * Create downloadable URL for file content
     */
    createDownloadUrl(content, fileName, mimeType = 'text/plain') {
        const blob = new Blob([content], { type: mimeType });
        return URL.createObjectURL(blob);
    },
    
    /**
     * Validate preview request
     */
    validatePreviewRequest(filePath, fileSize) {
        const errors = [];
        
        if (!filePath || filePath.trim().length === 0) {
            errors.push('File path is required');
        }
        
        if (fileSize > 10 * 1024 * 1024) { // 10MB limit
            errors.push('File too large for preview (max 10MB)');
        }
        
        return {
            isValid: errors.length === 0,
            errors
        };
    },
    
    /**
     * Get preview limitations message
     */
    getPreviewLimitations(previewType, fileSize, lineCount) {
        const limitations = [];
        
        if (lineCount >= 1000) {
            limitations.push('Showing first 1000 lines only');
        }
        
        if (fileSize > 1024 * 1024) { // > 1MB
            limitations.push('Large file - preview may be slow');
        }
        
        switch (previewType) {
            case 'pdf':
                limitations.push('PDF preview requires browser plugin');
                break;
            case 'image':
                limitations.push('Image preview may not show actual size');
                break;
            case 'csv':
                limitations.push('CSV preview shows table format - may not match original formatting');
                break;
        }
        
        return limitations;
    },
    
    /**
     * Generate preview summary
     */
    generatePreviewSummary(previewData) {
        if (!previewData) return {};
        
        const summary = {
            fileName: previewData.fileName,
            fileSize: this.formatFileSize(previewData.fileSize),
            previewType: previewData.previewType,
            isBinary: previewData.isBinary
        };
        
        if (!previewData.isBinary && Array.isArray(previewData.content)) {
            summary.lineCount = previewData.totalLines || previewData.content.length;
            summary.truncated = previewData.truncated;
        }
        
        if (previewData.structureInfo) {
            summary.structureInfo = previewData.structureInfo;
        }
        
        return summary;
    }
};

export default filePreviewService;