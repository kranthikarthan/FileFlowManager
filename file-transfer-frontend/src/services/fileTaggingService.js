import { api } from './api';

/**
 * Service for file tagging operations
 */
export const fileTaggingService = {
    
    /**
     * Get all tags for a tenant
     */
    async getAllTags(tenantId) {
        try {
            const response = await api.get('/api/v1/file-tags', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching tags:', error);
            throw error;
        }
    },
    
    /**
     * Get user-created tags
     */
    async getUserTags(tenantId) {
        try {
            const response = await api.get('/api/v1/file-tags/user-tags', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching user tags:', error);
            throw error;
        }
    },
    
    /**
     * Get system tags
     */
    async getSystemTags(tenantId) {
        try {
            const response = await api.get('/api/v1/file-tags/system-tags', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching system tags:', error);
            throw error;
        }
    },
    
    /**
     * Create a new tag
     */
    async createTag(tenantId, tagName, tagColor = '#2196f3', tagDescription = '', createdBy = 'user') {
        try {
            const response = await api.post('/api/v1/file-tags', null, {
                params: { tenantId, tagName, tagColor, tagDescription, createdBy }
            });
            return response.data;
        } catch (error) {
            console.error('Error creating tag:', error);
            throw error;
        }
    },
    
    /**
     * Update an existing tag
     */
    async updateTag(tagId, tagName, tagColor, tagDescription) {
        try {
            const response = await api.put(`/api/v1/file-tags/${tagId}`, null, {
                params: { tagName, tagColor, tagDescription }
            });
            return response.data;
        } catch (error) {
            console.error('Error updating tag:', error);
            throw error;
        }
    },
    
    /**
     * Delete a tag
     */
    async deleteTag(tagId) {
        try {
            const response = await api.delete(`/api/v1/file-tags/${tagId}`);
            return response.data;
        } catch (error) {
            console.error('Error deleting tag:', error);
            throw error;
        }
    },
    
    /**
     * Add tag to file transfer
     */
    async addTagToFileTransfer(fileTransferId, tagId, taggedBy = 'user', reason = '') {
        try {
            const response = await api.post(`/api/v1/file-tags/file-transfers/${fileTransferId}/tags/${tagId}`, null, {
                params: { taggedBy, reason }
            });
            return response.data;
        } catch (error) {
            console.error('Error adding tag to file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Remove tag from file transfer
     */
    async removeTagFromFileTransfer(fileTransferId, tagId) {
        try {
            const response = await api.delete(`/api/v1/file-tags/file-transfers/${fileTransferId}/tags/${tagId}`);
            return response.data;
        } catch (error) {
            console.error('Error removing tag from file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Get tags for a file transfer
     */
    async getTagsForFileTransfer(fileTransferId) {
        try {
            const response = await api.get(`/api/v1/file-tags/file-transfers/${fileTransferId}/tags`);
            return response.data;
        } catch (error) {
            console.error('Error fetching tags for file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Get file transfers by tag
     */
    async getFileTransfersByTag(tenantId, tagName) {
        try {
            const response = await api.get(`/api/v1/file-tags/tag/${tagName}/file-transfers`, {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching file transfers by tag:', error);
            throw error;
        }
    },
    
    /**
     * Bulk add tags to multiple file transfers
     */
    async bulkAddTags(fileTransferIds, tagIds, taggedBy = 'user', reason = '') {
        try {
            const response = await api.post('/api/v1/file-tags/bulk/add', {
                fileTransferIds,
                tagIds,
                taggedBy,
                reason
            });
            return response.data;
        } catch (error) {
            console.error('Error in bulk tag operation:', error);
            throw error;
        }
    },
    
    /**
     * Bulk remove tags from multiple file transfers
     */
    async bulkRemoveTags(fileTransferIds, tagIds) {
        try {
            const response = await api.post('/api/v1/file-tags/bulk/remove', {
                fileTransferIds,
                tagIds
            });
            return response.data;
        } catch (error) {
            console.error('Error in bulk tag removal:', error);
            throw error;
        }
    },
    
    /**
     * Search tags by name
     */
    async searchTags(tenantId, searchTerm) {
        try {
            const response = await api.get('/api/v1/file-tags/search', {
                params: { tenantId, searchTerm }
            });
            return response.data;
        } catch (error) {
            console.error('Error searching tags:', error);
            throw error;
        }
    },
    
    /**
     * Get tag statistics
     */
    async getTagStatistics(tenantId) {
        try {
            const response = await api.get('/api/v1/file-tags/statistics', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching tag statistics:', error);
            throw error;
        }
    },
    
    /**
     * Get most used tags
     */
    async getMostUsedTags(tenantId, limit = 10) {
        try {
            const response = await api.get('/api/v1/file-tags/most-used', {
                params: { tenantId, limit }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching most used tags:', error);
            throw error;
        }
    },
    
    /**
     * Initialize system tags for a tenant
     */
    async initializeSystemTags(tenantId) {
        try {
            const response = await api.post('/api/v1/file-tags/system-tags/initialize', null, {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error initializing system tags:', error);
            throw error;
        }
    },
    
    /**
     * Auto-tag a file transfer
     */
    async autoTagFileTransfer(fileTransferId, tenantId) {
        try {
            const response = await api.post(`/api/v1/file-tags/file-transfers/${fileTransferId}/auto-tag`, null, {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error auto-tagging file transfer:', error);
            throw error;
        }
    },
    
    // Utility functions for frontend
    
    /**
     * Get predefined tag colors
     */
    getTagColors() {
        return [
            { name: 'Blue', value: '#2196f3' },
            { name: 'Red', value: '#f44336' },
            { name: 'Green', value: '#4caf50' },
            { name: 'Orange', value: '#ff9800' },
            { name: 'Purple', value: '#9c27b0' },
            { name: 'Teal', value: '#009688' },
            { name: 'Pink', value: '#e91e63' },
            { name: 'Indigo', value: '#3f51b5' },
            { name: 'Brown', value: '#795548' },
            { name: 'Grey', value: '#9e9e9e' },
            { name: 'Deep Orange', value: '#ff5722' },
            { name: 'Light Green', value: '#8bc34a' }
        ];
    },
    
    /**
     * Validate tag name
     */
    validateTagName(tagName) {
        const errors = [];
        
        if (!tagName || tagName.trim().length === 0) {
            errors.push('Tag name is required');
        } else {
            if (tagName.length > 100) {
                errors.push('Tag name cannot exceed 100 characters');
            }
            
            if (!/^[a-zA-Z0-9\s\-_]+$/.test(tagName)) {
                errors.push('Tag name can only contain letters, numbers, spaces, hyphens, and underscores');
            }
            
            if (tagName.trim() !== tagName) {
                errors.push('Tag name cannot start or end with spaces');
            }
        }
        
        return {
            isValid: errors.length === 0,
            errors,
            normalizedName: tagName ? tagName.trim().toLowerCase().replace(/\s+/g, '-') : ''
        };
    },
    
    /**
     * Validate tag color
     */
    validateTagColor(tagColor) {
        const hexColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
        
        if (!tagColor) {
            return { isValid: true, normalizedColor: '#2196f3' }; // Default blue
        }
        
        const isValid = hexColorRegex.test(tagColor);
        
        return {
            isValid,
            errors: isValid ? [] : ['Color must be a valid hex color code (e.g., #2196f3)'],
            normalizedColor: isValid ? tagColor.toLowerCase() : '#2196f3'
        };
    },
    
    /**
     * Format tag for display
     */
    formatTagForDisplay(tag) {
        return {
            ...tag,
            displayName: tag.tagName.replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase()),
            isSystemTag: tag.isSystemTag || false,
            usageText: `${tag.usageCount || 0} ${(tag.usageCount || 0) === 1 ? 'use' : 'uses'}`
        };
    },
    
    /**
     * Group tags by category
     */
    groupTagsByCategory(tags) {
        const grouped = {
            system: [],
            user: [],
            popular: [],
            recent: []
        };
        
        const now = new Date();
        const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        
        tags.forEach(tag => {
            const formattedTag = this.formatTagForDisplay(tag);
            
            if (tag.isSystemTag) {
                grouped.system.push(formattedTag);
            } else {
                grouped.user.push(formattedTag);
            }
            
            if ((tag.usageCount || 0) >= 5) {
                grouped.popular.push(formattedTag);
            }
            
            if (tag.createdAt && new Date(tag.createdAt) > oneWeekAgo) {
                grouped.recent.push(formattedTag);
            }
        });
        
        // Sort groups
        grouped.system.sort((a, b) => a.displayName.localeCompare(b.displayName));
        grouped.user.sort((a, b) => a.displayName.localeCompare(b.displayName));
        grouped.popular.sort((a, b) => (b.usageCount || 0) - (a.usageCount || 0));
        grouped.recent.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
        return grouped;
    },
    
    /**
     * Get tag suggestions based on file characteristics
     */
    getTagSuggestions(fileTransfer) {
        const suggestions = [];
        
        // Size-based suggestions
        if (fileTransfer.fileSize > 100 * 1024 * 1024) { // > 100MB
            suggestions.push({
                tagName: 'large-file',
                reason: 'File size exceeds 100MB',
                confidence: 90
            });
        }
        
        // Status-based suggestions
        if (fileTransfer.status === 'COMPLETED') {
            suggestions.push({
                tagName: 'processed',
                reason: 'File processed successfully',
                confidence: 95
            });
        } else if (fileTransfer.status === 'FAILED') {
            suggestions.push({
                tagName: 'needs-attention',
                reason: 'Processing failed - requires attention',
                confidence: 95
            });
        }
        
        // Service-based suggestions
        if (fileTransfer.serviceName && fileTransfer.serviceName.includes('CUSTOMER')) {
            suggestions.push({
                tagName: 'customer-data',
                reason: 'Contains customer information',
                confidence: 85
            });
        }
        
        // Extension-based suggestions
        if (fileTransfer.fileExtension) {
            switch (fileTransfer.fileExtension.toLowerCase()) {
                case '.log':
                    suggestions.push({
                        tagName: 'log-data',
                        reason: 'Log file detected',
                        confidence: 90
                    });
                    break;
                case '.zip':
                case '.gz':
                case '.tar':
                    suggestions.push({
                        tagName: 'backup-data',
                        reason: 'Archive file detected',
                        confidence: 85
                    });
                    break;
                case '.csv':
                case '.xlsx':
                    if (fileTransfer.serviceName && fileTransfer.serviceName.includes('CUSTOMER')) {
                        suggestions.push({
                            tagName: 'sensitive-data',
                            reason: 'Potential sensitive customer data',
                            confidence: 75
                        });
                    }
                    break;
            }
        }
        
        return suggestions.sort((a, b) => b.confidence - a.confidence);
    },
    
    /**
     * Filter file transfers by tags
     */
    filterFileTransfersByTags(fileTransfers, selectedTags, matchType = 'any') {
        if (!selectedTags || selectedTags.length === 0) {
            return fileTransfers;
        }
        
        return fileTransfers.filter(transfer => {
            const transferTags = transfer.tags || [];
            const transferTagNames = transferTags.map(tag => tag.tagName);
            
            if (matchType === 'all') {
                return selectedTags.every(tagName => transferTagNames.includes(tagName));
            } else {
                return selectedTags.some(tagName => transferTagNames.includes(tagName));
            }
        });
    },
    
    /**
     * Get tag usage statistics for display
     */
    formatTagStatistics(statistics) {
        return {
            totalTags: statistics.totalTags || 0,
            totalUsages: statistics.totalUsages || 0,
            systemTags: statistics.systemTags || 0,
            userTags: statistics.userTags || 0,
            averageUsage: statistics.averageUsagePerTag ? 
                Math.round(statistics.averageUsagePerTag * 10) / 10 : 0
        };
    },
    
    /**
     * Generate tag color based on tag name
     */
    generateTagColor(tagName) {
        const colors = this.getTagColors();
        
        // Generate consistent color based on tag name hash
        let hash = 0;
        for (let i = 0; i < tagName.length; i++) {
            const char = tagName.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32-bit integer
        }
        
        const colorIndex = Math.abs(hash) % colors.length;
        return colors[colorIndex].value;
    },
    
    /**
     * Export tags to CSV
     */
    exportTagsToCSV(tags) {
        const headers = ['Tag Name', 'Color', 'Description', 'Usage Count', 'System Tag', 'Created By', 'Created At'];
        const rows = tags.map(tag => [
            tag.tagName,
            tag.tagColor,
            tag.tagDescription || '',
            tag.usageCount || 0,
            tag.isSystemTag ? 'Yes' : 'No',
            tag.createdBy || '',
            tag.createdAt ? new Date(tag.createdAt).toLocaleDateString() : ''
        ]);
        
        const csvContent = [headers, ...rows]
            .map(row => row.map(cell => `"${cell}"`).join(','))
            .join('\n');
        
        const blob = new Blob([csvContent], { type: 'text/csv' });
        return URL.createObjectURL(blob);
    },
    
    /**
     * Import tags from CSV
     */
    parseTagsFromCSV(csvContent) {
        const lines = csvContent.trim().split('\n');
        const headers = lines[0].split(',').map(h => h.replace(/"/g, '').trim());
        
        const tags = [];
        
        for (let i = 1; i < lines.length; i++) {
            const values = lines[i].split(',').map(v => v.replace(/"/g, '').trim());
            
            if (values.length >= headers.length) {
                const tag = {
                    tagName: values[0],
                    tagColor: values[1] || this.generateTagColor(values[0]),
                    tagDescription: values[2] || '',
                    isSystemTag: false // Imported tags are always user tags
                };
                
                // Validate tag
                const validation = this.validateTagName(tag.tagName);
                if (validation.isValid) {
                    tags.push(tag);
                }
            }
        }
        
        return tags;
    },
    
    /**
     * Get recommended tags for bulk operations
     */
    getRecommendedBulkTags(fileTransfers) {
        const recommendations = new Map();
        
        fileTransfers.forEach(transfer => {
            const suggestions = this.getTagSuggestions(transfer);
            
            suggestions.forEach(suggestion => {
                if (!recommendations.has(suggestion.tagName)) {
                    recommendations.set(suggestion.tagName, {
                        tagName: suggestion.tagName,
                        applicableFiles: 0,
                        totalConfidence: 0,
                        reasons: new Set()
                    });
                }
                
                const rec = recommendations.get(suggestion.tagName);
                rec.applicableFiles++;
                rec.totalConfidence += suggestion.confidence;
                rec.reasons.add(suggestion.reason);
            });
        });
        
        // Convert to array and calculate average confidence
        return Array.from(recommendations.values())
            .map(rec => ({
                ...rec,
                averageConfidence: rec.totalConfidence / rec.applicableFiles,
                reasons: Array.from(rec.reasons),
                applicabilityPercentage: (rec.applicableFiles / fileTransfers.length) * 100
            }))
            .filter(rec => rec.applicabilityPercentage >= 20) // Only show if applicable to 20%+ of files
            .sort((a, b) => b.averageConfidence - a.averageConfidence);
    },
    
    /**
     * Format tag display text
     */
    formatTagDisplayText(tag) {
        if (!tag) return '';
        
        return tag.tagName
            .split('-')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    },
    
    /**
     * Get tag contrast color (for text on colored background)
     */
    getTagContrastColor(backgroundColor) {
        // Convert hex to RGB
        const r = parseInt(backgroundColor.slice(1, 3), 16);
        const g = parseInt(backgroundColor.slice(3, 5), 16);
        const b = parseInt(backgroundColor.slice(5, 7), 16);
        
        // Calculate luminance
        const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        
        // Return white text for dark backgrounds, black for light
        return luminance > 0.5 ? '#000000' : '#ffffff';
    },
    
    /**
     * Create tag chip props for Material-UI
     */
    createTagChipProps(tag) {
        return {
            label: this.formatTagDisplayText(tag),
            style: {
                backgroundColor: tag.tagColor,
                color: this.getTagContrastColor(tag.tagColor),
                fontWeight: tag.isSystemTag ? 'bold' : 'normal'
            },
            variant: tag.isSystemTag ? 'filled' : 'outlined',
            size: 'small'
        };
    }
};

export default fileTaggingService;