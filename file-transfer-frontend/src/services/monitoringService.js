import * as Sentry from '@sentry/react';
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

/**
 * Comprehensive monitoring service for frontend application
 * Provides error tracking, performance monitoring, user analytics, and real-time metrics
 */
class MonitoringService {
  constructor() {
    this.isInitialized = false;
    this.performanceMetrics = new Map();
    this.userMetrics = new Map();
    this.errorCounts = new Map();
    this.sessionId = this.generateSessionId();
    
    this.init();
  }

  /**
   * Initialize monitoring services
   */
  init() {
    if (this.isInitialized) return;

    try {
      // Initialize Sentry for error tracking
      this.initializeSentry();
      
      // Initialize Web Vitals monitoring
      this.initializeWebVitals();
      
      // Initialize user activity monitoring
      this.initializeUserActivityMonitoring();
      
      // Initialize performance monitoring
      this.initializePerformanceMonitoring();
      
      // Initialize real-time metrics collection
      this.initializeRealTimeMetrics();
      
      this.isInitialized = true;
      console.log('Monitoring service initialized successfully');
      
    } catch (error) {
      console.error('Failed to initialize monitoring service:', error);
    }
  }

  /**
   * Initialize Sentry error tracking
   */
  initializeSentry() {
    Sentry.init({
      dsn: process.env.REACT_APP_SENTRY_DSN || '',
      environment: process.env.NODE_ENV || 'development',
      integrations: [
        new Sentry.BrowserTracing(),
        new Sentry.Replay(),
      ],
      tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
      replaysSessionSampleRate: 0.1,
      replaysOnErrorSampleRate: 1.0,
      beforeSend: (event) => {
        // Filter out certain errors
        if (this.shouldIgnoreError(event)) {
          return null;
        }
        return event;
      },
    });

    // Set user context
    Sentry.setUser({
      id: localStorage.getItem('userId') || 'anonymous',
      email: localStorage.getItem('userEmail') || undefined,
    });

    // Set session context
    Sentry.setTag('sessionId', this.sessionId);
    Sentry.setTag('userAgent', navigator.userAgent);
    Sentry.setTag('viewport', `${window.innerWidth}x${window.innerHeight}`);
  }

  /**
   * Initialize Web Vitals monitoring
   */
  initializeWebVitals() {
    const vitalsCallback = (metric) => {
      this.recordWebVital(metric);
      this.sendMetricToBackend('web-vital', metric);
    };

    getCLS(vitalsCallback);
    getFID(vitalsCallback);
    getFCP(vitalsCallback);
    getLCP(vitalsCallback);
    getTTFB(vitalsCallback);
  }

  /**
   * Initialize user activity monitoring
   */
  initializeUserActivityMonitoring() {
    // Track page views
    this.trackPageView();
    
    // Track user interactions
    this.trackUserInteractions();
    
    // Track session duration
    this.trackSessionDuration();
    
    // Track user engagement
    this.trackUserEngagement();
  }

  /**
   * Initialize performance monitoring
   */
  initializePerformanceMonitoring() {
    // Monitor React component performance
    this.monitorReactPerformance();
    
    // Monitor API call performance
    this.monitorApiPerformance();
    
    // Monitor resource loading
    this.monitorResourceLoading();
    
    // Monitor memory usage
    this.monitorMemoryUsage();
  }

  /**
   * Initialize real-time metrics collection
   */
  initializeRealTimeMetrics() {
    // Collect metrics every 30 seconds
    setInterval(() => {
      this.collectRealTimeMetrics();
    }, 30000);

    // Send metrics to backend every 2 minutes
    setInterval(() => {
      this.sendMetricsToBackend();
    }, 120000);
  }

  /**
   * Track custom events
   */
  trackEvent(eventName, properties = {}) {
    const event = {
      name: eventName,
      properties: {
        ...properties,
        sessionId: this.sessionId,
        timestamp: new Date().toISOString(),
        url: window.location.href,
        referrer: document.referrer,
        userAgent: navigator.userAgent,
      },
    };

    // Send to Sentry
    Sentry.addBreadcrumb({
      message: eventName,
      level: 'info',
      data: properties,
    });

    // Store locally
    this.storeEventLocally(event);

    // Send to backend
    this.sendEventToBackend(event);

    console.log('Event tracked:', eventName, properties);
  }

  /**
   * Track errors
   */
  trackError(error, context = {}) {
    const errorInfo = {
      message: error.message,
      stack: error.stack,
      name: error.name,
      context,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      url: window.location.href,
    };

    // Send to Sentry
    Sentry.captureException(error, {
      tags: context,
      extra: errorInfo,
    });

    // Update error counts
    const errorKey = `${error.name}-${error.message}`;
    this.errorCounts.set(errorKey, (this.errorCounts.get(errorKey) || 0) + 1);

    // Send to backend
    this.sendErrorToBackend(errorInfo);

    console.error('Error tracked:', errorInfo);
  }

  /**
   * Track performance metrics
   */
  trackPerformance(name, duration, metadata = {}) {
    const performanceData = {
      name,
      duration,
      metadata,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
    };

    // Store locally
    this.performanceMetrics.set(name, performanceData);

    // Send to Sentry
    Sentry.addBreadcrumb({
      message: `Performance: ${name}`,
      level: 'info',
      data: { duration, ...metadata },
    });

    // Send to backend
    this.sendMetricToBackend('performance', performanceData);

    console.log('Performance tracked:', name, duration, 'ms');
  }

  /**
   * Track user behavior
   */
  trackUserBehavior(action, target, metadata = {}) {
    const behaviorData = {
      action,
      target,
      metadata,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      page: window.location.pathname,
    };

    // Store locally
    this.userMetrics.set(`${action}-${Date.now()}`, behaviorData);

    // Send to backend
    this.sendUserBehaviorToBackend(behaviorData);
  }

  /**
   * Record Web Vitals
   */
  recordWebVital(metric) {
    const vitalData = {
      name: metric.name,
      value: metric.value,
      rating: metric.rating,
      delta: metric.delta,
      id: metric.id,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
    };

    console.log('Web Vital recorded:', vitalData);

    // Send to Sentry
    Sentry.addBreadcrumb({
      message: `Web Vital: ${metric.name}`,
      level: 'info',
      data: vitalData,
    });
  }

  /**
   * Track page views
   */
  trackPageView() {
    const pageViewData = {
      url: window.location.href,
      title: document.title,
      referrer: document.referrer,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
    };

    this.trackEvent('page_view', pageViewData);
  }

  /**
   * Track user interactions
   */
  trackUserInteractions() {
    // Track clicks
    document.addEventListener('click', (event) => {
      const target = event.target;
      this.trackUserBehavior('click', this.getElementSelector(target), {
        x: event.clientX,
        y: event.clientY,
        button: event.button,
      });
    });

    // Track form submissions
    document.addEventListener('submit', (event) => {
      const form = event.target;
      this.trackUserBehavior('form_submit', this.getElementSelector(form), {
        formId: form.id,
        formClass: form.className,
      });
    });

    // Track input focus
    document.addEventListener('focusin', (event) => {
      if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
        this.trackUserBehavior('input_focus', this.getElementSelector(event.target), {
          inputType: event.target.type,
          inputName: event.target.name,
        });
      }
    });
  }

  /**
   * Track session duration
   */
  trackSessionDuration() {
    this.sessionStartTime = Date.now();

    window.addEventListener('beforeunload', () => {
      const sessionDuration = Date.now() - this.sessionStartTime;
      this.trackEvent('session_end', {
        duration: sessionDuration,
        sessionId: this.sessionId,
      });
    });
  }

  /**
   * Track user engagement
   */
  trackUserEngagement() {
    let lastActivity = Date.now();
    let totalEngagementTime = 0;

    const updateActivity = () => {
      const now = Date.now();
      const timeSinceLastActivity = now - lastActivity;
      
      if (timeSinceLastActivity < 30000) { // Less than 30 seconds = engaged
        totalEngagementTime += timeSinceLastActivity;
      }
      
      lastActivity = now;
    };

    ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'].forEach(event => {
      document.addEventListener(event, updateActivity, { passive: true });
    });

    // Report engagement every 5 minutes
    setInterval(() => {
      this.trackEvent('user_engagement', {
        totalEngagementTime,
        sessionId: this.sessionId,
      });
    }, 300000);
  }

  /**
   * Monitor React component performance
   */
  monitorReactPerformance() {
    // This would be implemented with React Profiler
    // For now, we'll track basic component lifecycle events
    const originalConsoleWarn = console.warn;
    console.warn = (...args) => {
      if (args[0] && args[0].includes('React')) {
        this.trackEvent('react_warning', { warning: args[0] });
      }
      originalConsoleWarn.apply(console, args);
    };
  }

  /**
   * Monitor API call performance
   */
  monitorApiPerformance() {
    const originalFetch = window.fetch;
    window.fetch = async (...args) => {
      const startTime = performance.now();
      const url = args[0];
      
      try {
        const response = await originalFetch.apply(window, args);
        const endTime = performance.now();
        const duration = endTime - startTime;
        
        this.trackPerformance('api_call', duration, {
          url: typeof url === 'string' ? url : url.url,
          method: args[1]?.method || 'GET',
          status: response.status,
          success: response.ok,
        });
        
        return response;
      } catch (error) {
        const endTime = performance.now();
        const duration = endTime - startTime;
        
        this.trackError(error, {
          type: 'api_error',
          url: typeof url === 'string' ? url : url.url,
          duration,
        });
        
        throw error;
      }
    };
  }

  /**
   * Monitor resource loading
   */
  monitorResourceLoading() {
    window.addEventListener('load', () => {
      const navigation = performance.getEntriesByType('navigation')[0];
      const resources = performance.getEntriesByType('resource');
      
      this.trackPerformance('page_load', navigation.loadEventEnd - navigation.fetchStart, {
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
        resourceCount: resources.length,
      });
      
      // Track slow resources
      resources.forEach(resource => {
        if (resource.duration > 1000) { // Slower than 1 second
          this.trackPerformance('slow_resource', resource.duration, {
            name: resource.name,
            type: resource.initiatorType,
          });
        }
      });
    });
  }

  /**
   * Monitor memory usage
   */
  monitorMemoryUsage() {
    if ('memory' in performance) {
      setInterval(() => {
        const memory = performance.memory;
        this.trackEvent('memory_usage', {
          usedJSHeapSize: memory.usedJSHeapSize,
          totalJSHeapSize: memory.totalJSHeapSize,
          jsHeapSizeLimit: memory.jsHeapSizeLimit,
          usagePercentage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100,
        });
      }, 60000); // Every minute
    }
  }

  /**
   * Collect real-time metrics
   */
  collectRealTimeMetrics() {
    const metrics = {
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      viewport: `${window.innerWidth}x${window.innerHeight}`,
      connectionType: navigator.connection?.effectiveType || 'unknown',
      isOnline: navigator.onLine,
      performanceMetricsCount: this.performanceMetrics.size,
      userMetricsCount: this.userMetrics.size,
      errorCount: Array.from(this.errorCounts.values()).reduce((sum, count) => sum + count, 0),
    };

    this.sendMetricToBackend('realtime', metrics);
  }

  /**
   * Send event to backend
   */
  async sendEventToBackend(event) {
    try {
      await fetch('/api/analytics/events', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(event),
      });
    } catch (error) {
      console.error('Failed to send event to backend:', error);
    }
  }

  /**
   * Send error to backend
   */
  async sendErrorToBackend(error) {
    try {
      await fetch('/api/analytics/errors', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(error),
      });
    } catch (error) {
      console.error('Failed to send error to backend:', error);
    }
  }

  /**
   * Send metric to backend
   */
  async sendMetricToBackend(type, metric) {
    try {
      await fetch('/api/analytics/metrics', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ type, metric }),
      });
    } catch (error) {
      console.error('Failed to send metric to backend:', error);
    }
  }

  /**
   * Send user behavior to backend
   */
  async sendUserBehaviorToBackend(behavior) {
    try {
      await fetch('/api/analytics/behavior', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(behavior),
      });
    } catch (error) {
      console.error('Failed to send user behavior to backend:', error);
    }
  }

  /**
   * Send all metrics to backend
   */
  async sendMetricsToBackend() {
    const allMetrics = {
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      performance: Array.from(this.performanceMetrics.values()),
      user: Array.from(this.userMetrics.values()),
      errors: Array.from(this.errorCounts.entries()),
    };

    try {
      await fetch('/api/analytics/batch', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(allMetrics),
      });

      // Clear local caches after successful send
      this.performanceMetrics.clear();
      this.userMetrics.clear();
      
    } catch (error) {
      console.error('Failed to send metrics batch to backend:', error);
    }
  }

  /**
   * Store event locally for offline support
   */
  storeEventLocally(event) {
    try {
      const events = JSON.parse(localStorage.getItem('analytics_events') || '[]');
      events.push(event);
      
      // Keep only last 100 events
      if (events.length > 100) {
        events.splice(0, events.length - 100);
      }
      
      localStorage.setItem('analytics_events', JSON.stringify(events));
    } catch (error) {
      console.error('Failed to store event locally:', error);
    }
  }

  /**
   * Get element selector for tracking
   */
  getElementSelector(element) {
    if (element.id) return `#${element.id}`;
    if (element.className) return `.${element.className.split(' ')[0]}`;
    return element.tagName.toLowerCase();
  }

  /**
   * Check if error should be ignored
   */
  shouldIgnoreError(event) {
    const ignoredErrors = [
      'Script error.',
      'Non-Error promise rejection captured',
      'ChunkLoadError',
    ];
    
    return ignoredErrors.some(ignored => 
      event.exception?.values?.[0]?.value?.includes(ignored)
    );
  }

  /**
   * Generate unique session ID
   */
  generateSessionId() {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Get current session metrics
   */
  getSessionMetrics() {
    return {
      sessionId: this.sessionId,
      performanceMetricsCount: this.performanceMetrics.size,
      userMetricsCount: this.userMetrics.size,
      errorCount: Array.from(this.errorCounts.values()).reduce((sum, count) => sum + count, 0),
      isInitialized: this.isInitialized,
    };
  }
}

// Create singleton instance
const monitoringService = new MonitoringService();

export default monitoringService;