import { api } from './api';

/**
 * Service for file content analysis operations
 */
export const contentAnalysisService = {
    
    /**
     * Analyze file content
     */
    async analyzeFile(filePath, fileName) {
        try {
            const response = await api.post('/api/v1/content-analysis/analyze', null, {
                params: { filePath, fileName }
            });
            return response.data;
        } catch (error) {
            console.error('Error analyzing file content:', error);
            throw error;
        }
    },
    
    /**
     * Upload and analyze file
     */
    async uploadAndAnalyze(file, tenantId = 'default') {
        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('tenantId', tenantId);
            
            const response = await api.post('/api/v1/content-analysis/upload-and-analyze', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            return response.data;
        } catch (error) {
            console.error('Error uploading and analyzing file:', error);
            throw error;
        }
    },
    
    /**
     * Validate file against schema
     */
    async validateSchema(filePath, schemaPath, fileType) {
        try {
            const response = await api.post('/api/v1/content-analysis/validate-schema', null, {
                params: { filePath, schemaPath, fileType }
            });
            return response.data;
        } catch (error) {
            console.error('Error validating file schema:', error);
            throw error;
        }
    },
    
    /**
     * Generate data profile for file
     */
    async generateDataProfile(filePath, fileType) {
        try {
            const response = await api.post('/api/v1/content-analysis/profile', null, {
                params: { filePath, fileType }
            });
            return response.data;
        } catch (error) {
            console.error('Error generating data profile:', error);
            throw error;
        }
    },
    
    /**
     * Comprehensive file analysis
     */
    async analyzeComprehensive(filePath, fileName, schemaPath = null, fileType = null) {
        try {
            const params = { filePath, fileName };
            if (schemaPath) params.schemaPath = schemaPath;
            if (fileType) params.fileType = fileType;
            
            const response = await api.post('/api/v1/content-analysis/analyze-comprehensive', null, {
                params
            });
            return response.data;
        } catch (error) {
            console.error('Error performing comprehensive analysis:', error);
            throw error;
        }
    },
    
    /**
     * Detect file type
     */
    async detectFileType(filePath, fileName) {
        try {
            const response = await api.post('/api/v1/content-analysis/detect-file-type', null, {
                params: { filePath, fileName }
            });
            return response.data;
        } catch (error) {
            console.error('Error detecting file type:', error);
            throw error;
        }
    },
    
    /**
     * Get quality recommendations
     */
    async getQualityRecommendations(filePath, fileType) {
        try {
            const response = await api.post('/api/v1/content-analysis/quality-recommendations', null, {
                params: { filePath, fileType }
            });
            return response.data;
        } catch (error) {
            console.error('Error getting quality recommendations:', error);
            throw error;
        }
    },
    
    /**
     * Compare two files
     */
    async compareFiles(filePath1, fileName1, filePath2, fileName2) {
        try {
            const response = await api.post('/api/v1/content-analysis/compare-files', null, {
                params: { filePath1, fileName1, filePath2, fileName2 }
            });
            return response.data;
        } catch (error) {
            console.error('Error comparing files:', error);
            throw error;
        }
    },
    
    /**
     * Get content analysis statistics
     */
    async getContentStatistics(tenantId) {
        try {
            const response = await api.get('/api/v1/content-analysis/statistics/content', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching content analysis statistics:', error);
            throw error;
        }
    },
    
    /**
     * Get schema validation statistics
     */
    async getValidationStatistics(tenantId) {
        try {
            const response = await api.get('/api/v1/content-analysis/statistics/validation', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching validation statistics:', error);
            throw error;
        }
    },
    
    /**
     * Get data profiling statistics
     */
    async getProfilingStatistics(tenantId) {
        try {
            const response = await api.get('/api/v1/content-analysis/statistics/profiling', {
                params: { tenantId }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching profiling statistics:', error);
            throw error;
        }
    },
    
    /**
     * Format file type for display
     */
    formatFileType(fileType) {
        const typeMap = {
            'COBOL_FLAT_FILE': 'COBOL Flat File',
            'BINARY_FILE': 'Binary File',
            'XML': 'XML Document',
            'JSON': 'JSON Document',
            'CSV': 'CSV File',
            'FIXED_WIDTH': 'Fixed Width',
            'DELIMITED': 'Delimited File',
            'EDI': 'EDI Document'
        };
        
        return typeMap[fileType] || fileType;
    },
    
    /**
     * Get file type icon
     */
    getFileTypeIcon(fileType) {
        const iconMap = {
            'COBOL_FLAT_FILE': 'description',
            'BINARY_FILE': 'binary',
            'XML': 'code',
            'JSON': 'data_object',
            'CSV': 'table_chart',
            'FIXED_WIDTH': 'view_column',
            'DELIMITED': 'view_list',
            'EDI': 'swap_horiz'
        };
        
        return iconMap[fileType] || 'insert_drive_file';
    },
    
    /**
     * Get quality score color
     */
    getQualityScoreColor(score) {
        if (score >= 90) return 'success';
        if (score >= 80) return 'info';
        if (score >= 70) return 'warning';
        if (score >= 60) return 'error';
        return 'error';
    },
    
    /**
     * Get quality grade text
     */
    getQualityGrade(score) {
        if (score >= 90) return 'Excellent';
        if (score >= 80) return 'Good';
        if (score >= 70) return 'Fair';
        if (score >= 60) return 'Poor';
        return 'Critical';
    },
    
    /**
     * Format file size for display
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    
    /**
     * Format analysis duration
     */
    formatDuration(milliseconds) {
        if (milliseconds < 1000) {
            return `${milliseconds}ms`;
        } else if (milliseconds < 60000) {
            return `${(milliseconds / 1000).toFixed(1)}s`;
        } else {
            return `${(milliseconds / 60000).toFixed(1)}m`;
        }
    },
    
    /**
     * Calculate confidence percentage for display
     */
    formatConfidence(confidence) {
        return `${Math.round(confidence)}%`;
    },
    
    /**
     * Get encoding display name
     */
    formatEncoding(encoding) {
        const encodingMap = {
            'UTF-8': 'UTF-8 (Unicode)',
            'UTF-16LE': 'UTF-16 Little Endian',
            'UTF-16BE': 'UTF-16 Big Endian',
            'ASCII': 'ASCII (7-bit)',
            'UNKNOWN': 'Unknown Encoding'
        };
        
        return encodingMap[encoding] || encoding;
    },
    
    /**
     * Format structure analysis for display
     */
    formatStructureAnalysis(structureAnalysis) {
        if (!structureAnalysis) return [];
        
        const formatted = [];
        
        for (const [key, value] of Object.entries(structureAnalysis)) {
            let displayKey = key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
            let displayValue = value;
            
            if (typeof value === 'boolean') {
                displayValue = value ? 'Yes' : 'No';
            } else if (typeof value === 'number') {
                if (key.includes('Count') || key.includes('Length')) {
                    displayValue = value.toLocaleString();
                } else if (key.includes('Percentage') || key.includes('Ratio')) {
                    displayValue = `${value.toFixed(1)}%`;
                }
            } else if (Array.isArray(value)) {
                displayValue = value.join(', ');
            }
            
            formatted.push({ key: displayKey, value: displayValue });
        }
        
        return formatted;
    },
    
    /**
     * Format column profiles for display
     */
    formatColumnProfiles(columnProfiles) {
        if (!columnProfiles) return [];
        
        return columnProfiles.map(column => ({
            ...column,
            formattedDataType: this.formatDataType(column.dataType),
            formattedQualityScore: `${column.qualityScore?.toFixed(1) || 0}%`,
            formattedUniqueness: `${column.uniquenessPercentage?.toFixed(1) || 0}%`,
            formattedCompleteness: `${(100 - (column.nullPercentage || 0)).toFixed(1)}%`
        }));
    },
    
    /**
     * Format data type for display
     */
    formatDataType(dataType) {
        const typeMap = {
            'STRING': 'Text',
            'INTEGER': 'Integer',
            'DECIMAL': 'Decimal',
            'DATE': 'Date',
            'BOOLEAN': 'Boolean',
            'EMAIL': 'Email Address',
            'PHONE': 'Phone Number',
            'UNKNOWN': 'Unknown',
            'EMPTY': 'Empty'
        };
        
        return typeMap[dataType] || dataType;
    },
    
    /**
     * Get recommendations by priority
     */
    prioritizeRecommendations(recommendations) {
        if (!recommendations) return { high: [], medium: [], low: [] };
        
        const prioritized = { high: [], medium: [], low: [] };
        
        recommendations.forEach(rec => {
            const lower = rec.toLowerCase();
            if (lower.includes('critical') || lower.includes('urgent') || lower.includes('required')) {
                prioritized.high.push(rec);
            } else if (lower.includes('recommend') || lower.includes('should') || lower.includes('consider')) {
                prioritized.medium.push(rec);
            } else {
                prioritized.low.push(rec);
            }
        });
        
        return prioritized;
    },
    
    /**
     * Generate analysis summary
     */
    generateAnalysisSummary(analysisResult) {
        if (!analysisResult) return {};
        
        const summary = {
            fileInfo: {
                name: analysisResult.fileName,
                size: this.formatFileSize(analysisResult.fileSize),
                type: this.formatFileType(analysisResult.detectedFileType),
                encoding: this.formatEncoding(analysisResult.detectedEncoding),
                isBinary: analysisResult.isBinaryFile,
                isEmpty: analysisResult.isEmpty
            },
            structure: {
                lineCount: analysisResult.lineCount?.toLocaleString(),
                recordCount: analysisResult.recordCount?.toLocaleString(),
                columnCount: analysisResult.columnCount,
                avgLineLength: analysisResult.averageLineLength?.toFixed(1)
            },
            quality: {
                score: analysisResult.qualityAssessment?.qualityScore?.toFixed(1),
                grade: this.getQualityGrade(analysisResult.qualityAssessment?.qualityScore || 0),
                color: this.getQualityScoreColor(analysisResult.qualityAssessment?.qualityScore || 0),
                issues: analysisResult.qualityAssessment?.recommendations || []
            },
            performance: {
                analysisDuration: this.formatDuration(analysisResult.analysisDurationMs || 0)
            }
        };
        
        return summary;
    },
    
    /**
     * Generate comprehensive analysis summary
     */
    generateComprehensiveSummary(comprehensiveResult) {
        if (!comprehensiveResult) return {};
        
        const summary = {
            contentAnalysis: comprehensiveResult.contentAnalysis ? 
                this.generateAnalysisSummary(comprehensiveResult.contentAnalysis) : null,
            schemaValidation: comprehensiveResult.schemaValidation ? {
                passed: comprehensiveResult.schemaValidation.success,
                errorCount: comprehensiveResult.schemaValidation.validationErrors?.length || 0,
                complianceScore: comprehensiveResult.schemaValidation.schemaComplianceScore,
                errors: comprehensiveResult.schemaValidation.validationErrors || []
            } : null,
            dataProfile: comprehensiveResult.dataProfile ? {
                qualityScore: comprehensiveResult.dataProfile.dataStatistics?.averageQualityScore,
                completeness: comprehensiveResult.dataProfile.dataStatistics?.averageCompleteness,
                uniqueness: comprehensiveResult.dataProfile.dataStatistics?.averageUniqueness,
                insights: comprehensiveResult.dataProfile.qualityInsights || [],
                recommendations: comprehensiveResult.dataProfile.processingRecommendations || []
            } : null
        };
        
        return summary;
    },
    
    /**
     * Validate analysis result
     */
    validateAnalysisResult(result) {
        const validation = {
            isValid: true,
            errors: [],
            warnings: []
        };
        
        if (!result) {
            validation.isValid = false;
            validation.errors.push('No analysis result provided');
            return validation;
        }
        
        if (!result.success) {
            validation.isValid = false;
            validation.errors.push(result.message || 'Analysis failed');
        }
        
        if (result.qualityAssessment && result.qualityAssessment.qualityScore < 70) {
            validation.warnings.push('Low quality score detected - review recommended');
        }
        
        if (result.isBinaryFile && result.detectedFileType !== 'BINARY_FILE') {
            validation.warnings.push('File type mismatch - binary content detected but type is not binary');
        }
        
        return validation;
    },
    
    /**
     * Get analysis status icon
     */
    getAnalysisStatusIcon(status) {
        const iconMap = {
            'pending': 'schedule',
            'in_progress': 'autorenew',
            'completed': 'check_circle',
            'failed': 'error',
            'skipped': 'skip_next'
        };
        
        return iconMap[status] || 'help';
    },
    
    /**
     * Get analysis status color
     */
    getAnalysisStatusColor(status) {
        const colorMap = {
            'pending': 'warning',
            'in_progress': 'info',
            'completed': 'success',
            'failed': 'error',
            'skipped': 'default'
        };
        
        return colorMap[status] || 'default';
    },
    
    /**
     * Format validation errors for display
     */
    formatValidationErrors(errors) {
        if (!errors || !Array.isArray(errors)) return [];
        
        return errors.map(error => ({
            line: error.lineNumber,
            type: error.errorType,
            message: error.message,
            severity: this.getErrorSeverity(error.errorType),
            icon: this.getErrorIcon(error.errorType),
            color: this.getErrorColor(error.errorType)
        }));
    },
    
    /**
     * Get error severity
     */
    getErrorSeverity(errorType) {
        const severityMap = {
            'PARSE': 'high',
            'REQUIRED': 'high',
            'DATA_TYPE': 'medium',
            'LENGTH': 'medium',
            'PATTERN': 'medium',
            'STRUCTURE': 'high',
            'HEADER': 'high',
            'ALLOWED_VALUES': 'medium'
        };
        
        return severityMap[errorType] || 'medium';
    },
    
    /**
     * Get error icon
     */
    getErrorIcon(errorType) {
        const iconMap = {
            'PARSE': 'error_outline',
            'REQUIRED': 'warning',
            'DATA_TYPE': 'type_specimen',
            'LENGTH': 'straighten',
            'PATTERN': 'pattern',
            'STRUCTURE': 'account_tree',
            'HEADER': 'view_headline',
            'ALLOWED_VALUES': 'rule'
        };
        
        return iconMap[errorType] || 'info';
    },
    
    /**
     * Get error color
     */
    getErrorColor(errorType) {
        const severity = this.getErrorSeverity(errorType);
        const colorMap = {
            'high': 'error',
            'medium': 'warning',
            'low': 'info'
        };
        
        return colorMap[severity] || 'default';
    },
    
    /**
     * Calculate analysis progress
     */
    calculateAnalysisProgress(analysisSteps) {
        if (!analysisSteps) return 0;
        
        const completed = Object.values(analysisSteps).filter(step => step === 'completed').length;
        const total = Object.keys(analysisSteps).length;
        
        return total > 0 ? Math.round((completed / total) * 100) : 0;
    },
    
    /**
     * Get next recommended action
     */
    getNextRecommendedAction(analysisResult) {
        if (!analysisResult || !analysisResult.success) {
            return { action: 'retry', message: 'Retry analysis' };
        }
        
        if (analysisResult.qualityAssessment && analysisResult.qualityAssessment.qualityScore < 70) {
            return { action: 'review', message: 'Review quality issues' };
        }
        
        if (!analysisResult.isBinaryFile && analysisResult.detectedFileType) {
            return { action: 'validate', message: 'Validate against schema' };
        }
        
        return { action: 'process', message: 'Ready for processing' };
    },
    
    /**
     * Export analysis results
     */
    exportAnalysisResults(analysisResult, format = 'json') {
        if (!analysisResult) return null;
        
        const exportData = {
            fileName: analysisResult.fileName,
            analysisTimestamp: new Date().toISOString(),
            results: analysisResult
        };
        
        if (format === 'json') {
            const dataStr = JSON.stringify(exportData, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });
            return URL.createObjectURL(dataBlob);
        }
        
        // CSV export for tabular data
        if (format === 'csv' && analysisResult.columnProfiles) {
            const csvHeaders = ['Column Name', 'Data Type', 'Quality Score', 'Completeness', 'Uniqueness'];
            const csvRows = analysisResult.columnProfiles.map(col => [
                col.columnName,
                col.dataType,
                col.qualityScore?.toFixed(1) || '0',
                `${(100 - (col.nullPercentage || 0)).toFixed(1)}%`,
                `${col.uniquenessPercentage?.toFixed(1) || 0}%`
            ]);
            
            const csvContent = [csvHeaders, ...csvRows]
                .map(row => row.join(','))
                .join('\n');
            
            const dataBlob = new Blob([csvContent], { type: 'text/csv' });
            return URL.createObjectURL(dataBlob);
        }
        
        return null;
    }
};

export default contentAnalysisService;