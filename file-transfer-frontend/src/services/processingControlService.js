import axios from 'axios';
import { API_BASE_URL } from '../config/apiConfig';

/**
 * Processing Control Service - API client for processing control operations
 * Provides methods for start/stop/pause/resume operations at all levels
 */
class ProcessingControlService {
  constructor() {
    this.baseURL = `${API_BASE_URL}/processing-control`;
    this.axiosInstance = axios.create({
      baseURL: this.baseURL,
      timeout: 30000, // 30 seconds timeout
    });

    // Add request interceptor for authentication
    this.axiosInstance.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('authToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Add response interceptor for error handling
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        console.error('Processing Control API Error:', error);
        if (error.response?.status === 401) {
          localStorage.removeItem('authToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // ===== TENANT LEVEL OPERATIONS =====

  /**
   * Start processing for entire tenant
   */
  async startTenantProcessing(tenantId, userId) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/start`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to start tenant processing');
    }
  }

  /**
   * Stop processing for entire tenant
   */
  async stopTenantProcessing(tenantId, userId, graceful = true) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/stop`, null, {
        params: { userId, graceful }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to stop tenant processing');
    }
  }

  /**
   * Pause processing for entire tenant
   */
  async pauseTenantProcessing(tenantId, userId) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/pause`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to pause tenant processing');
    }
  }

  /**
   * Resume processing for entire tenant
   */
  async resumeTenantProcessing(tenantId, userId) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/resume`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to resume tenant processing');
    }
  }

  /**
   * Get tenant processing status
   */
  async getTenantProcessingStatus(tenantId) {
    try {
      const response = await this.axiosInstance.get(`/tenant/${tenantId}/status`);
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get tenant processing status');
    }
  }

  // ===== SERVICE LEVEL OPERATIONS =====

  /**
   * Start processing for specific service
   */
  async startServiceProcessing(tenantId, serviceName, userId) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/service/${serviceName}/start`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to start service processing');
    }
  }

  /**
   * Stop processing for specific service
   */
  async stopServiceProcessing(tenantId, serviceName, userId, graceful = true) {
    try {
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/service/${serviceName}/stop`, null, {
        params: { userId, graceful }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to stop service processing');
    }
  }

  /**
   * Reset service for the day
   */
  async resetServiceForDay(tenantId, serviceName, userId, resetDate = null) {
    try {
      const params = { userId };
      if (resetDate) {
        params.resetDate = this.formatDate(resetDate);
      }
      
      const response = await this.axiosInstance.post(`/tenant/${tenantId}/service/${serviceName}/reset`, null, {
        params
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to reset service for day');
    }
  }

  /**
   * Get service processing status
   */
  async getServiceProcessingStatus(tenantId, serviceName) {
    try {
      const response = await this.axiosInstance.get(`/tenant/${tenantId}/service/${serviceName}/status`);
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get service processing status');
    }
  }

  // ===== SUB-SERVICE LEVEL OPERATIONS =====

  /**
   * Start processing for specific sub-service
   */
  async startSubServiceProcessing(tenantId, serviceName, subServiceName, userId) {
    try {
      const response = await this.axiosInstance.post(
        `/tenant/${tenantId}/service/${serviceName}/subservice/${subServiceName}/start`, 
        null, 
        { params: { userId } }
      );
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to start sub-service processing');
    }
  }

  /**
   * Stop processing for specific sub-service
   */
  async stopSubServiceProcessing(tenantId, serviceName, subServiceName, userId, graceful = true) {
    try {
      const response = await this.axiosInstance.post(
        `/tenant/${tenantId}/service/${serviceName}/subservice/${subServiceName}/stop`, 
        null, 
        { params: { userId, graceful } }
      );
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to stop sub-service processing');
    }
  }

  /**
   * Reset sub-service for the day
   */
  async resetSubServiceForDay(tenantId, serviceName, subServiceName, userId, resetDate = null) {
    try {
      const params = { userId };
      if (resetDate) {
        params.resetDate = this.formatDate(resetDate);
      }
      
      const response = await this.axiosInstance.post(
        `/tenant/${tenantId}/service/${serviceName}/subservice/${subServiceName}/reset`, 
        null, 
        { params }
      );
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to reset sub-service for day');
    }
  }

  // ===== FILE LEVEL OPERATIONS =====

  /**
   * Start processing for specific file
   */
  async startFileProcessing(fileId, userId) {
    try {
      const response = await this.axiosInstance.post(`/file/${fileId}/start`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to start file processing');
    }
  }

  /**
   * Stop processing for specific file
   */
  async stopFileProcessing(fileId, userId, force = false) {
    try {
      const response = await this.axiosInstance.post(`/file/${fileId}/stop`, null, {
        params: { userId, force }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to stop file processing');
    }
  }

  /**
   * Pause processing for specific file
   */
  async pauseFileProcessing(fileId, userId) {
    try {
      const response = await this.axiosInstance.post(`/file/${fileId}/pause`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to pause file processing');
    }
  }

  /**
   * Resume processing for specific file
   */
  async resumeFileProcessing(fileId, userId) {
    try {
      const response = await this.axiosInstance.post(`/file/${fileId}/resume`, null, {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to resume file processing');
    }
  }

  /**
   * Get file processing status
   */
  async getFileProcessingStatus(fileId) {
    try {
      const response = await this.axiosInstance.get(`/file/${fileId}/status`);
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get file processing status');
    }
  }

  // ===== BATCH PROCESSING OPERATIONS =====

  /**
   * Start batch processing
   */
  async startBatchProcessing(tenantId, serviceName = null, subServiceName = null, processingDate = null, userId) {
    try {
      const params = { tenantId, userId };
      if (serviceName) params.serviceName = serviceName;
      if (subServiceName) params.subServiceName = subServiceName;
      if (processingDate) params.processingDate = this.formatDate(processingDate);
      
      const response = await this.axiosInstance.post('/batch/start', null, { params });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to start batch processing');
    }
  }

  /**
   * Stop batch processing
   */
  async stopBatchProcessing(jobExecutionId, userId, force = false) {
    try {
      const params = { userId, force };
      if (jobExecutionId) params.jobExecutionId = jobExecutionId;
      
      const response = await this.axiosInstance.post('/batch/stop', null, { params });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to stop batch processing');
    }
  }

  /**
   * Get batch processing status
   */
  async getBatchProcessingStatus(tenantId = null, jobExecutionId = null) {
    try {
      const params = {};
      if (tenantId) params.tenantId = tenantId;
      if (jobExecutionId) params.jobExecutionId = jobExecutionId;
      
      const response = await this.axiosInstance.get('/batch/status', { params });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get batch processing status');
    }
  }

  // ===== DATA LOADING METHODS =====

  /**
   * Get available tenants (mock implementation)
   */
  async getTenants() {
    try {
      // This would typically be a separate API call
      return [
        { tenantId: 'tenant1', tenantName: 'Tenant 1' },
        { tenantId: 'tenant2', tenantName: 'Tenant 2' },
        { tenantId: 'demo', tenantName: 'Demo Tenant' }
      ];
    } catch (error) {
      throw this.handleError(error, 'Failed to get tenants');
    }
  }

  /**
   * Get services for tenant
   */
  async getServices(tenantId) {
    try {
      // This would call the service configuration API
      const response = await axios.get(`${API_BASE_URL}/service-configurations/tenant/${tenantId}`);
      
      // Transform data to include processing status
      const services = response.data.map(service => ({
        ...service,
        serviceEnabled: service.enabled,
        totalSubServices: service.subServices?.length || 0,
        enabledSubServices: service.subServices?.filter(sub => sub.enabled).length || 0,
        activeFiles: 0 // Would be populated from actual API
      }));
      
      return services;
    } catch (error) {
      throw this.handleError(error, 'Failed to get services');
    }
  }

  /**
   * Get active files for tenant
   */
  async getActiveFiles(tenantId) {
    try {
      // This would call the file transfer API
      const response = await axios.get(`${API_BASE_URL}/file-transfers`, {
        params: { tenantId }
      });
      
      // Filter for active files only
      return response.data.filter(file => 
        ['PENDING', 'PROCESSING', 'PAUSED'].includes(file.status)
      );
    } catch (error) {
      throw this.handleError(error, 'Failed to get active files');
    }
  }

  // ===== UTILITY METHODS =====

  /**
   * Format date for API requests
   */
  formatDate(date) {
    if (!date) return null;
    if (typeof date === 'string') return date;
    return date.toISOString().split('T')[0];
  }

  /**
   * Handle API errors
   */
  handleError(error, defaultMessage) {
    const message = error.response?.data?.error || error.response?.data?.message || error.message || defaultMessage;
    const status = error.response?.status;
    
    return new Error(`${message} (Status: ${status})`);
  }

  /**
   * Get processing status color
   */
  getStatusColor(status) {
    switch (status) {
      case 'ACTIVE':
      case 'PROCESSING':
      case 'COMPLETED':
        return 'success';
      case 'PAUSED':
      case 'PENDING':
        return 'warning';
      case 'STOPPED':
      case 'FAILED':
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  }

  /**
   * Get processing status icon
   */
  getStatusIcon(status) {
    switch (status) {
      case 'ACTIVE':
      case 'PROCESSING':
        return 'play_arrow';
      case 'PAUSED':
        return 'pause';
      case 'STOPPED':
        return 'stop';
      case 'COMPLETED':
        return 'check_circle';
      case 'FAILED':
      case 'CANCELLED':
        return 'error';
      default:
        return 'help';
    }
  }

  /**
   * Validate processing control parameters
   */
  validateParameters(action, parameters) {
    if (!parameters.tenantId) {
      throw new Error('Tenant ID is required');
    }

    if (!parameters.userId) {
      throw new Error('User ID is required');
    }

    if (action.includes('SERVICE') && !parameters.serviceName) {
      throw new Error('Service name is required');
    }

    if (action.includes('SUBSERVICE') && !parameters.subServiceName) {
      throw new Error('Sub-service name is required');
    }

    if (action.includes('FILE') && !parameters.fileId) {
      throw new Error('File ID is required');
    }

    if (action.includes('RESET') && !parameters.resetDate) {
      throw new Error('Reset date is required');
    }

    return true;
  }

  /**
   * Check for holiday conflicts
   */
  async checkHolidayConflicts(tenantId, date) {
    try {
      // This would call the holiday API
      const response = await axios.get(`${API_BASE_URL}/holidays/tenant/${tenantId}/is-holiday/${this.formatDate(date)}`);
      const isHoliday = response.data;
      
      const isSunday = date.getDay() === 0;
      
      return {
        isHoliday,
        isSunday,
        hasConflict: isHoliday && isSunday
      };
    } catch (error) {
      console.warn('Failed to check holiday conflicts:', error);
      return {
        isHoliday: false,
        isSunday: date.getDay() === 0,
        hasConflict: false
      };
    }
  }

  /**
   * Get processing control audit log
   */
  async getProcessingControlAuditLog(tenantId, startDate, endDate) {
    try {
      // This would call an audit API
      const params = { tenantId };
      if (startDate) params.startDate = this.formatDate(startDate);
      if (endDate) params.endDate = this.formatDate(endDate);
      
      // Mock implementation - would be replaced with actual API call
      return [
        {
          timestamp: new Date().toISOString(),
          action: 'TENANT_START',
          userId: 'admin',
          details: 'Started tenant processing'
        }
      ];
    } catch (error) {
      throw this.handleError(error, 'Failed to get audit log');
    }
  }

  /**
   * Get system health status for processing control
   */
  async getSystemHealthStatus() {
    try {
      // This would call system health API
      const response = await axios.get(`${API_BASE_URL}/actuator/health`);
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get system health status');
    }
  }

  /**
   * Emergency stop all processing (admin only)
   */
  async emergencyStopAll(userId, reason) {
    try {
      const response = await this.axiosInstance.post('/emergency/stop-all', {
        userId,
        reason,
        timestamp: new Date().toISOString()
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to execute emergency stop');
    }
  }

  /**
   * Get processing metrics for dashboard
   */
  async getProcessingMetrics(tenantId) {
    try {
      const [tenantStatus, batchStatus] = await Promise.all([
        this.getTenantProcessingStatus(tenantId),
        this.getBatchProcessingStatus(tenantId)
      ]);

      return {
        tenant: tenantStatus,
        batch: batchStatus,
        summary: {
          totalActiveFiles: tenantStatus.activeFiles || 0,
          totalServices: tenantStatus.totalServices || 0,
          enabledServices: tenantStatus.enabledServices || 0,
          processingStatus: tenantStatus.processingStatus || 'UNKNOWN'
        }
      };
    } catch (error) {
      throw this.handleError(error, 'Failed to get processing metrics');
    }
  }
}

// Create and export singleton instance
export const processingControlService = new ProcessingControlService();
export default processingControlService;