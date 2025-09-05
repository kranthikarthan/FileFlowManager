import { api } from './api';

/**
 * Service for ACK/NACK file management
 */
export const ackNackService = {
    
    /**
     * Get all ACK/NACK records for a tenant
     */
    async getAllAckNackRecords(tenantId) {
        try {
            const response = await api.get(`/api/v1/ack-nack/${tenantId}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching ACK/NACK records:', error);
            throw error;
        }
    },
    
    /**
     * Get ACK/NACK records by status
     */
    async getAckNackRecordsByStatus(tenantId, status) {
        try {
            const response = await api.get(`/api/v1/ack-nack/${tenantId}/status/${status}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching ACK/NACK records by status:', error);
            throw error;
        }
    },
    
    /**
     * Get ACK/NACK records by type
     */
    async getAckNackRecordsByType(tenantId, type) {
        try {
            const response = await api.get(`/api/v1/ack-nack/${tenantId}/type/${type}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching ACK/NACK records by type:', error);
            throw error;
        }
    },
    
    /**
     * Get ACK/NACK record for a specific file transfer
     */
    async getAckNackForFileTransfer(fileTransferId) {
        try {
            const response = await api.get(`/api/v1/ack-nack/file-transfer/${fileTransferId}`);
            return response.data;
        } catch (error) {
            if (error.response && error.response.status === 404) {
                return null; // No ACK/NACK found
            }
            console.error('Error fetching ACK/NACK for file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Generate ACK for a file transfer
     */
    async generateAck(fileTransferId) {
        try {
            const response = await api.post(`/api/v1/ack-nack/generate-ack/${fileTransferId}`);
            return response.data;
        } catch (error) {
            console.error('Error generating ACK:', error);
            throw error;
        }
    },
    
    /**
     * Generate NACK for a file transfer
     */
    async generateNack(fileTransferId, reasonCode, reasonDescription) {
        try {
            const response = await api.post(`/api/v1/ack-nack/generate-nack/${fileTransferId}`, {
                reasonCode,
                reasonDescription
            });
            return response.data;
        } catch (error) {
            console.error('Error generating NACK:', error);
            throw error;
        }
    },
    
    /**
     * Upload ACK/NACK file
     */
    async uploadAckNackFile(tenantId, file) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            
            const response = await api.post(`/api/v1/ack-nack/${tenantId}/upload`, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            return response.data;
        } catch (error) {
            console.error('Error uploading ACK/NACK file:', error);
            throw error;
        }
    },
    
    /**
     * Retry failed ACK/NACK
     */
    async retryAckNack(ackNackId) {
        try {
            const response = await api.post(`/api/v1/ack-nack/retry/${ackNackId}`);
            return response.data;
        } catch (error) {
            console.error('Error retrying ACK/NACK:', error);
            throw error;
        }
    },
    
    /**
     * Send pending ACK/NACK files for a tenant
     */
    async sendPendingAckNackFiles(tenantId) {
        try {
            const response = await api.post(`/api/v1/ack-nack/${tenantId}/send-pending`);
            return response.data;
        } catch (error) {
            console.error('Error sending pending ACK/NACK files:', error);
            throw error;
        }
    },
    
    /**
     * Get ACK/NACK statistics for a tenant
     */
    async getAckNackStatistics(tenantId) {
        try {
            const response = await api.get(`/api/v1/ack-nack/${tenantId}/statistics`);
            return response.data;
        } catch (error) {
            console.error('Error fetching ACK/NACK statistics:', error);
            throw error;
        }
    },
    
    /**
     * Format ACK/NACK status for display
     */
    formatStatus(status) {
        const statusMap = {
            'PENDING': 'Pending',
            'GENERATED': 'Generated',
            'SENT': 'Sent',
            'RECEIVED': 'Received',
            'PROCESSED': 'Processed',
            'FAILED': 'Failed',
            'EXPIRED': 'Expired'
        };
        return statusMap[status] || status;
    },
    
    /**
     * Get status color for UI display
     */
    getStatusColor(status) {
        const colorMap = {
            'PENDING': 'warning',
            'GENERATED': 'info',
            'SENT': 'primary',
            'RECEIVED': 'info',
            'PROCESSED': 'success',
            'FAILED': 'error',
            'EXPIRED': 'error'
        };
        return colorMap[status] || 'default';
    },
    
    /**
     * Get type color for UI display
     */
    getTypeColor(type) {
        return type === 'ACK' ? 'success' : 'error';
    }
};

export default ackNackService;