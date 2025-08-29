import axios from 'axios';
import { API_BASE_URL } from '../config/apiConfig';

/**
 * Analytics Service - API client for analytics and business intelligence
 * Provides methods for accessing analytics data, reports, and insights
 */
class AnalyticsService {
  constructor() {
    this.baseURL = `${API_BASE_URL}/analytics`;
    this.axiosInstance = axios.create({
      baseURL: this.baseURL,
      timeout: 30000, // 30 seconds timeout for analytics queries
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
        console.error('Analytics API Error:', error);
        if (error.response?.status === 401) {
          // Handle unauthorized access
          localStorage.removeItem('authToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  /**
   * Get analytics summary for a tenant and date range
   */
  async getAnalyticsSummary(tenantId, startDate, endDate) {
    try {
      const response = await this.axiosInstance.get('/summary', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate)
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get analytics summary');
    }
  }

  /**
   * Get trending analytics data
   */
  async getTrendingAnalytics(tenantId, startDate, endDate, granularity = 'daily') {
    try {
      const response = await this.axiosInstance.get('/trends', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate),
          granularity
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get trending analytics');
    }
  }

  /**
   * Get service-level analytics breakdown
   */
  async getServiceAnalytics(tenantId, startDate, endDate) {
    try {
      const response = await this.axiosInstance.get('/services', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate)
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get service analytics');
    }
  }

  /**
   * Get real-time dashboard data
   */
  async getRealTimeDashboard(tenantId) {
    try {
      const response = await this.axiosInstance.get('/realtime/dashboard', {
        params: { tenantId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get real-time dashboard data');
    }
  }

  /**
   * Record analytics event
   */
  async recordAnalyticsEvent(eventData) {
    try {
      const response = await this.axiosInstance.post('/events', eventData);
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to record analytics event');
    }
  }

  /**
   * Generate business intelligence report
   */
  async generateReport(tenantId, reportType, startDate, endDate, createdBy) {
    try {
      const response = await this.axiosInstance.post('/reports/generate', null, {
        params: {
          tenantId,
          reportType,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate),
          createdBy
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to generate report');
    }
  }

  /**
   * Get or generate business intelligence report
   */
  async getOrGenerateReport(tenantId, reportType, startDate, endDate, createdBy) {
    try {
      const response = await this.axiosInstance.get('/reports', {
        params: {
          tenantId,
          reportType,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate),
          createdBy
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get report');
    }
  }

  /**
   * Get predictive analytics
   */
  async getPredictiveAnalytics(tenantId, forecastDays = 30) {
    try {
      const response = await this.axiosInstance.get('/predictive', {
        params: {
          tenantId,
          forecastDays
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get predictive analytics');
    }
  }

  /**
   * Get capacity planning report
   */
  async getCapacityPlanningReport(tenantId, planningMonths = 6) {
    try {
      const response = await this.axiosInstance.get('/capacity-planning', {
        params: {
          tenantId,
          planningMonths
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get capacity planning report');
    }
  }

  /**
   * Get SLA compliance report
   */
  async getSlaComplianceReport(tenantId, startDate, endDate) {
    try {
      const response = await this.axiosInstance.get('/sla-compliance', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate)
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get SLA compliance report');
    }
  }

  /**
   * Get cost analysis report
   */
  async getCostAnalysisReport(tenantId, startDate, endDate) {
    try {
      const response = await this.axiosInstance.get('/cost-analysis', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate)
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get cost analysis report');
    }
  }

  /**
   * Get raw analytics data
   */
  async getAnalyticsData(tenantId, startDate, endDate) {
    try {
      const response = await this.axiosInstance.get('/data', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate)
        }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get analytics data');
    }
  }

  /**
   * Get system health metrics
   */
  async getSystemHealthMetrics(tenantId) {
    try {
      const response = await this.axiosInstance.get('/health', {
        params: { tenantId }
      });
      return response.data;
    } catch (error) {
      throw this.handleError(error, 'Failed to get system health metrics');
    }
  }

  /**
   * Export analytics data
   */
  async exportAnalyticsData(tenantId, startDate, endDate, format = 'csv') {
    try {
      const response = await this.axiosInstance.get('/export', {
        params: {
          tenantId,
          startDate: this.formatDate(startDate),
          endDate: this.formatDate(endDate),
          format
        },
        responseType: 'blob'
      });

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `analytics_${tenantId}_${this.formatDate(startDate)}_to_${this.formatDate(endDate)}.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return true;
    } catch (error) {
      throw this.handleError(error, 'Failed to export analytics data');
    }
  }

  /**
   * Get available tenants (mock implementation - replace with actual API)
   */
  async getTenants() {
    try {
      // This would typically be a separate API call
      // For now, return mock data or extract from other APIs
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
   * Get report types
   */
  getReportTypes() {
    return [
      { value: 'DAILY_SUMMARY', label: 'Daily Summary' },
      { value: 'WEEKLY_SUMMARY', label: 'Weekly Summary' },
      { value: 'MONTHLY_SUMMARY', label: 'Monthly Summary' },
      { value: 'QUARTERLY_SUMMARY', label: 'Quarterly Summary' },
      { value: 'ANNUAL_SUMMARY', label: 'Annual Summary' },
      { value: 'PERFORMANCE_ANALYSIS', label: 'Performance Analysis' },
      { value: 'QUALITY_ANALYSIS', label: 'Quality Analysis' },
      { value: 'COST_ANALYSIS', label: 'Cost Analysis' },
      { value: 'SLA_COMPLIANCE', label: 'SLA Compliance' },
      { value: 'TREND_ANALYSIS', label: 'Trend Analysis' },
      { value: 'PREDICTIVE_ANALYSIS', label: 'Predictive Analysis' },
      { value: 'ANOMALY_DETECTION', label: 'Anomaly Detection' },
      { value: 'CAPACITY_PLANNING', label: 'Capacity Planning' },
      { value: 'CUSTOM_REPORT', label: 'Custom Report' }
    ];
  }

  /**
   * Get analytics event types
   */
  getAnalyticsEventTypes() {
    return [
      'FILE_UPLOAD_STARTED',
      'FILE_UPLOAD_COMPLETED',
      'FILE_UPLOAD_FAILED',
      'FILE_PROCESSING_STARTED',
      'FILE_PROCESSING_COMPLETED',
      'FILE_PROCESSING_FAILED',
      'VALIDATION_STARTED',
      'VALIDATION_COMPLETED',
      'VALIDATION_FAILED',
      'SCHEMA_VALIDATION_FAILED',
      'CUT_OFF_EXTENSION_REQUESTED',
      'CUT_OFF_EXTENSION_APPROVED',
      'SLA_BREACH_DETECTED',
      'SYSTEM_ERROR',
      'PERFORMANCE_ANOMALY',
      'SECURITY_INCIDENT'
    ];
  }

  /**
   * Real-time analytics event tracking
   */
  trackEvent(eventType, eventData) {
    // Asynchronously record event without blocking UI
    this.recordAnalyticsEvent({
      eventType,
      ...eventData,
      timestamp: new Date().toISOString()
    }).catch(error => {
      console.warn('Failed to track analytics event:', error);
    });
  }

  /**
   * Batch event tracking for performance
   */
  trackEvents(events) {
    // In a real implementation, this would batch events for efficiency
    events.forEach(event => this.trackEvent(event.eventType, event.eventData));
  }

  /**
   * Subscribe to real-time analytics updates (WebSocket)
   */
  subscribeToRealTimeUpdates(tenantId, callback) {
    // This would implement WebSocket connection for real-time updates
    // For now, use polling as fallback
    const interval = setInterval(async () => {
      try {
        const realTimeData = await this.getRealTimeDashboard(tenantId);
        callback(realTimeData);
      } catch (error) {
        console.error('Error getting real-time updates:', error);
      }
    }, 30000); // Poll every 30 seconds

    // Return unsubscribe function
    return () => clearInterval(interval);
  }

  /**
   * Get analytics insights and recommendations
   */
  async getInsights(tenantId, startDate, endDate) {
    try {
      const [summary, predictive, sla, cost] = await Promise.all([
        this.getAnalyticsSummary(tenantId, startDate, endDate),
        this.getPredictiveAnalytics(tenantId),
        this.getSlaComplianceReport(tenantId, startDate, endDate),
        this.getCostAnalysisReport(tenantId, startDate, endDate)
      ]);

      return {
        summary,
        predictive,
        sla,
        cost,
        insights: this.generateInsights(summary, predictive, sla, cost)
      };
    } catch (error) {
      throw this.handleError(error, 'Failed to get analytics insights');
    }
  }

  /**
   * Generate insights from analytics data
   */
  generateInsights(summary, predictive, sla, cost) {
    const insights = [];

    // Success rate insights
    if (summary.successRate < 95) {
      insights.push({
        type: 'warning',
        category: 'Reliability',
        message: `Success rate (${summary.successRate.toFixed(1)}%) is below 95% threshold`,
        recommendation: 'Investigate failed transfers and implement retry mechanisms',
        priority: 'high'
      });
    }

    // Performance insights
    if (summary.avgProcessingTimeSeconds > 30) {
      insights.push({
        type: 'warning',
        category: 'Performance',
        message: `Average processing time (${summary.avgProcessingTimeSeconds.toFixed(1)}s) exceeds 30 seconds`,
        recommendation: 'Optimize file processing pipeline and consider parallel processing',
        priority: 'medium'
      });
    }

    // Quality insights
    if (summary.qualityScore < 90) {
      insights.push({
        type: 'error',
        category: 'Quality',
        message: `Data quality score (${summary.qualityScore.toFixed(1)}%) is below 90%`,
        recommendation: 'Review schema validation rules and implement data quality checks',
        priority: 'high'
      });
    }

    // Cost insights
    if (cost && cost.totalCosts && cost.totalCosts.totalCost > 1000) {
      insights.push({
        type: 'info',
        category: 'Cost',
        message: `Monthly costs ($${cost.totalCosts.totalCost.toFixed(2)}) are significant`,
        recommendation: 'Consider cost optimization strategies and resource scaling',
        priority: 'medium'
      });
    }

    // SLA insights
    if (sla && sla.overallMetrics && sla.overallMetrics.complianceRate < 98) {
      insights.push({
        type: 'warning',
        category: 'SLA',
        message: `SLA compliance (${sla.overallMetrics.complianceRate.toFixed(1)}%) is below 98%`,
        recommendation: 'Implement proactive monitoring and automated scaling',
        priority: 'high'
      });
    }

    return insights;
  }

  // Utility methods

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
    const message = error.response?.data?.message || error.message || defaultMessage;
    const status = error.response?.status;
    
    return new Error(`${message} (Status: ${status})`);
  }

  /**
   * Validate date range
   */
  validateDateRange(startDate, endDate) {
    if (!startDate || !endDate) {
      throw new Error('Start date and end date are required');
    }

    if (new Date(startDate) > new Date(endDate)) {
      throw new Error('Start date must be before end date');
    }

    const daysDiff = Math.ceil((new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      throw new Error('Date range cannot exceed 365 days');
    }

    return true;
  }

  /**
   * Cache management for analytics data
   */
  getCacheKey(method, params) {
    return `analytics_${method}_${JSON.stringify(params)}`;
  }

  /**
   * Get cached data if available and not expired
   */
  getCachedData(cacheKey, maxAgeMinutes = 5) {
    try {
      const cached = localStorage.getItem(cacheKey);
      if (cached) {
        const { data, timestamp } = JSON.parse(cached);
        const age = (Date.now() - timestamp) / (1000 * 60);
        if (age < maxAgeMinutes) {
          return data;
        }
      }
    } catch (error) {
      console.warn('Error reading cache:', error);
    }
    return null;
  }

  /**
   * Cache data with timestamp
   */
  setCachedData(cacheKey, data) {
    try {
      const cacheData = {
        data,
        timestamp: Date.now()
      };
      localStorage.setItem(cacheKey, JSON.stringify(cacheData));
    } catch (error) {
      console.warn('Error setting cache:', error);
    }
  }
}

// Create and export singleton instance
export const analyticsService = new AnalyticsService();
export default analyticsService;