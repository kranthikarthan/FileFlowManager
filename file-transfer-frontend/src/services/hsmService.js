import { api } from './api';

/**
 * Service for HSM (Hardware Security Module) operations
 */
export const hsmService = {
    
    /**
     * Get all available HSM providers
     */
    async getHsmProviders() {
        try {
            const response = await api.get('/api/v1/hsm/providers');
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM providers:', error);
            throw error;
        }
    },
    
    /**
     * Get all available HSM operations
     */
    async getHsmOperations() {
        try {
            const response = await api.get('/api/v1/hsm/operations');
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM operations:', error);
            throw error;
        }
    },
    
    /**
     * Get HSM validation records for a tenant
     */
    async getHsmValidations(tenantId) {
        try {
            const response = await api.get(`/api/v1/hsm/${tenantId}/validations`);
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM validations:', error);
            throw error;
        }
    },
    
    /**
     * Get HSM validation records by status
     */
    async getHsmValidationsByStatus(tenantId, status) {
        try {
            const response = await api.get(`/api/v1/hsm/${tenantId}/validations/status/${status}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM validations by status:', error);
            throw error;
        }
    },
    
    /**
     * Get HSM validation record for a specific file transfer
     */
    async getHsmValidationForFileTransfer(fileTransferId) {
        try {
            const response = await api.get(`/api/v1/hsm/validation/file-transfer/${fileTransferId}`);
            return response.data;
        } catch (error) {
            if (error.response && error.response.status === 404) {
                return null; // No HSM validation found
            }
            console.error('Error fetching HSM validation for file transfer:', error);
            throw error;
        }
    },
    
    /**
     * Test HSM connection and configuration
     */
    async testHsmConnection(tenantId, serviceName, subServiceName) {
        try {
            const response = await api.post('/api/v1/hsm/test-connection', {
                tenantId,
                serviceName,
                subServiceName
            });
            return response.data;
        } catch (error) {
            console.error('Error testing HSM connection:', error);
            throw error;
        }
    },
    
    /**
     * Get HSM statistics for a tenant
     */
    async getHsmStatistics(tenantId) {
        try {
            const response = await api.get(`/api/v1/hsm/${tenantId}/statistics`);
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM statistics:', error);
            throw error;
        }
    },
    
    /**
     * Get HSM status summary for a tenant
     */
    async getHsmStatusSummary(tenantId) {
        try {
            const response = await api.get(`/api/v1/hsm/${tenantId}/status-summary`);
            return response.data;
        } catch (error) {
            console.error('Error fetching HSM status summary:', error);
            throw error;
        }
    },
    
    /**
     * Retry failed HSM validation
     */
    async retryHsmValidation(validationId) {
        try {
            const response = await api.post(`/api/v1/hsm/validation/${validationId}/retry`);
            return response.data;
        } catch (error) {
            console.error('Error retrying HSM validation:', error);
            throw error;
        }
    },
    
    /**
     * Validate HSM configuration
     */
    async validateHsmConfig(config) {
        try {
            const response = await api.post('/api/v1/hsm/validate-config', config);
            return response.data;
        } catch (error) {
            console.error('Error validating HSM configuration:', error);
            throw error;
        }
    },
    
    /**
     * Format HSM status for display
     */
    formatStatus(status) {
        const statusMap = {
            'PENDING': 'Pending',
            'IN_PROGRESS': 'In Progress',
            'PASSED': 'Passed',
            'FAILED': 'Failed',
            'ERROR': 'Error',
            'SKIPPED': 'Skipped',
            'TIMEOUT': 'Timeout',
            'HSM_UNAVAILABLE': 'HSM Unavailable',
            'INVALID_KEY': 'Invalid Key',
            'INSUFFICIENT_PERMISSIONS': 'Insufficient Permissions'
        };
        return statusMap[status] || status;
    },
    
    /**
     * Get status color for UI display
     */
    getStatusColor(status) {
        const colorMap = {
            'PENDING': 'warning',
            'IN_PROGRESS': 'info',
            'PASSED': 'success',
            'FAILED': 'error',
            'ERROR': 'error',
            'SKIPPED': 'default',
            'TIMEOUT': 'error',
            'HSM_UNAVAILABLE': 'error',
            'INVALID_KEY': 'error',
            'INSUFFICIENT_PERMISSIONS': 'error'
        };
        return colorMap[status] || 'default';
    },
    
    /**
     * Get HSM provider color for UI display
     */
    getProviderColor(provider) {
        const colorMap = {
            'NONE': 'default',
            'AZURE_KEY_VAULT': 'primary',
            'AWS_CLOUD_HSM': 'secondary',
            'THALES_LUNA': 'info',
            'UTIMACO_CRYPTO_SERVER': 'success',
            'GEMALTO_SAFE_NET': 'warning',
            'NCIPHER_NSHIELD': 'error',
            'FORTANIX_DSM': 'primary',
            'SECUROSYS_PRIMUS': 'secondary'
        };
        return colorMap[provider] || 'default';
    },
    
    /**
     * Get HSM operation icon
     */
    getOperationIcon(operation) {
        const iconMap = {
            'SIGN': 'edit',
            'VERIFY': 'verified',
            'ENCRYPT': 'lock',
            'DECRYPT': 'lock_open',
            'HASH': 'fingerprint',
            'MAC': 'security',
            'KEY_GENERATION': 'vpn_key',
            'KEY_DERIVATION': 'key'
        };
        return iconMap[operation] || 'security';
    },
    
    /**
     * Check if HSM provider is cloud-based
     */
    isCloudProvider(provider) {
        const cloudProviders = ['AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'FORTANIX_DSM'];
        return cloudProviders.includes(provider);
    },
    
    /**
     * Check if HSM provider requires network connectivity
     */
    isNetworkBasedProvider(provider) {
        const networkProviders = ['THALES_LUNA', 'AWS_CLOUD_HSM', 'AZURE_KEY_VAULT', 'FORTANIX_DSM'];
        return networkProviders.includes(provider);
    },
    
    /**
     * Get security level indicator for HSM provider
     */
    getSecurityLevel(provider) {
        const securityLevels = {
            'NONE': 0,
            'AZURE_KEY_VAULT': 3,
            'AWS_CLOUD_HSM': 3,
            'THALES_LUNA': 4,
            'UTIMACO_CRYPTO_SERVER': 4,
            'GEMALTO_SAFE_NET': 4,
            'NCIPHER_NSHIELD': 4,
            'FORTANIX_DSM': 3,
            'SECUROSYS_PRIMUS': 4
        };
        return securityLevels[provider] || 0;
    },
    
    /**
     * Format processing time for display
     */
    formatProcessingTime(timeMs) {
        if (!timeMs) return 'N/A';
        
        if (timeMs < 1000) {
            return `${timeMs}ms`;
        } else if (timeMs < 60000) {
            return `${(timeMs / 1000).toFixed(1)}s`;
        } else {
            return `${(timeMs / 60000).toFixed(1)}m`;
        }
    },
    
    /**
     * Calculate HSM success rate
     */
    calculateSuccessRate(passedCount, totalCount) {
        if (totalCount === 0) return 0;
        return Math.round((passedCount / totalCount) * 100 * 100) / 100;
    },
    
    /**
     * Get recommended HSM provider based on requirements
     */
    getRecommendedProvider(requirements = {}) {
        const { cloudPreferred = false, highSecurity = false, performance = false } = requirements;
        
        if (cloudPreferred) {
            return highSecurity ? 'AZURE_KEY_VAULT' : 'AWS_CLOUD_HSM';
        }
        
        if (highSecurity) {
            return 'THALES_LUNA';
        }
        
        if (performance) {
            return 'NCIPHER_NSHIELD';
        }
        
        return 'AZURE_KEY_VAULT'; // Default recommendation
    },
    
    /**
     * Validate HSM configuration object
     */
    validateHsmConfiguration(config) {
        const errors = [];
        
        if (!config.hsmProvider || config.hsmProvider === 'NONE') {
            if (config.hsmValidationRequired) {
                errors.push('HSM provider must be selected when HSM validation is required');
            }
        }
        
        if (config.hsmValidationRequired) {
            if (!config.hsmKeyAlias) {
                errors.push('HSM key alias is required when HSM validation is enabled');
            }
            
            if (!config.hsmAlgorithm) {
                errors.push('HSM algorithm is required when HSM validation is enabled');
            }
            
            if (!config.hsmOperationOutbound) {
                errors.push('HSM operation for outbound files is required');
            }
            
            if (!config.hsmOperationInbound) {
                errors.push('HSM operation for inbound files is required');
            }
        }
        
        if (config.hsmTimeoutSeconds && (config.hsmTimeoutSeconds < 1 || config.hsmTimeoutSeconds > 300)) {
            errors.push('HSM timeout must be between 1 and 300 seconds');
        }
        
        if (config.hsmRetryAttempts && (config.hsmRetryAttempts < 0 || config.hsmRetryAttempts > 10)) {
            errors.push('HSM retry attempts must be between 0 and 10');
        }
        
        return {
            isValid: errors.length === 0,
            errors
        };
    },
    
    /**
     * Get HSM provider setup instructions
     */
    getProviderSetupInstructions(provider) {
        const instructions = {
            'AZURE_KEY_VAULT': [
                'Create an Azure Key Vault in your subscription',
                'Configure managed identity or service principal access',
                'Create or import cryptographic keys',
                'Set vault URL and authentication credentials',
                'Test connectivity and key access'
            ],
            'AWS_CLOUD_HSM': [
                'Create a CloudHSM cluster in AWS',
                'Initialize the cluster and create users',
                'Generate or import cryptographic keys',
                'Configure client certificates and network access',
                'Test connectivity and key operations'
            ],
            'THALES_LUNA': [
                'Install Luna Network HSM appliance',
                'Configure network connectivity and certificates',
                'Create cryptographic keys and assign policies',
                'Install client software and certificates',
                'Test HSM connectivity and key access'
            ]
        };
        
        return instructions[provider] || ['Provider-specific setup instructions not available'];
    }
};

export default hsmService;