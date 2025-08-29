import React, { useEffect, useState, Suspense } from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { HelmetProvider } from 'react-helmet-async';
import { ErrorBoundary } from 'react-error-boundary';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

import monitoringService from '../services/monitoringService';
import securityService from '../services/securityService';
import performanceService from '../services/performanceService';

import LoadingFallback from '../components/common/LoadingFallback';
import ErrorFallback from '../components/common/ErrorFallback';
import { useTheme } from '../contexts/ThemeContext';

/**
 * Enhanced App Wrapper with comprehensive monitoring, security, and performance features
 * Provides error boundaries, security monitoring, performance tracking, and offline support
 */
const EnhancedAppWrapper = ({ children }) => {
  const [isInitialized, setIsInitialized] = useState(false);
  const [initError, setInitError] = useState(null);
  const { theme } = useTheme();

  useEffect(() => {
    initializeApplication();
  }, []);

  /**
   * Initialize all application services
   */
  const initializeApplication = async () => {
    try {
      console.log('Initializing enhanced application...');

      // Initialize monitoring service
      if (!monitoringService.isInitialized) {
        await monitoringService.init();
      }

      // Track application startup
      monitoringService.trackEvent('app_initialized', {
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        viewport: `${window.innerWidth}x${window.innerHeight}`,
        hasServiceWorker: 'serviceWorker' in navigator,
        hasIndexedDB: 'indexedDB' in window,
        hasWebGL: !!window.WebGLRenderingContext,
        cookiesEnabled: navigator.cookieEnabled,
        language: navigator.language,
        platform: navigator.platform,
        connectionType: navigator.connection?.effectiveType || 'unknown',
        memoryLimit: navigator.deviceMemory || 'unknown',
      });

      // Initialize performance monitoring
      performanceService.trackPerformance('app_initialization', Date.now());

      // Set up global error handling
      setupGlobalErrorHandling();

      // Set up performance monitoring
      setupPerformanceMonitoring();

      // Set up security monitoring
      setupSecurityMonitoring();

      // Set up offline/online monitoring
      setupConnectivityMonitoring();

      // Set up user activity monitoring
      setupUserActivityMonitoring();

      setIsInitialized(true);
      console.log('Enhanced application initialized successfully');

    } catch (error) {
      console.error('Failed to initialize enhanced application:', error);
      setInitError(error);
      
      // Track initialization failure
      monitoringService.trackError(error, {
        type: 'app_initialization_failed',
        timestamp: new Date().toISOString(),
      });
    }
  };

  /**
   * Set up global error handling
   */
  const setupGlobalErrorHandling = () => {
    // Unhandled promise rejections
    window.addEventListener('unhandledrejection', (event) => {
      console.error('Unhandled promise rejection:', event.reason);
      
      monitoringService.trackError(new Error(event.reason), {
        type: 'unhandled_promise_rejection',
        promise: event.promise,
      });
      
      securityService.logSecurityEvent('UNHANDLED_PROMISE_REJECTION', {
        reason: event.reason,
        timestamp: new Date().toISOString(),
      });
      
      // Prevent default browser behavior
      event.preventDefault();
    });

    // Global error handler
    window.addEventListener('error', (event) => {
      console.error('Global error:', event.error);
      
      monitoringService.trackError(event.error, {
        type: 'global_error',
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        message: event.message,
      });
      
      securityService.logSecurityEvent('GLOBAL_ERROR', {
        error: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        timestamp: new Date().toISOString(),
      });
    });

    // Resource loading errors
    window.addEventListener('error', (event) => {
      if (event.target && event.target !== window) {
        console.error('Resource loading error:', event.target);
        
        monitoringService.trackEvent('resource_load_error', {
          type: event.target.tagName,
          src: event.target.src || event.target.href,
          timestamp: new Date().toISOString(),
        });
      }
    }, true);
  };

  /**
   * Set up performance monitoring
   */
  const setupPerformanceMonitoring = () => {
    // Monitor component updates
    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'measure') {
          performanceService.trackPerformance(entry.name, entry.duration, {
            startTime: entry.startTime,
            detail: entry.detail,
          });
        }
      }
    });
    
    observer.observe({ entryTypes: ['measure'] });

    // Monitor long tasks
    if ('PerformanceObserver' in window) {
      const longTaskObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.duration > 50) { // Longer than 50ms
            monitoringService.trackEvent('long_task', {
              duration: entry.duration,
              startTime: entry.startTime,
              name: entry.name,
            });
            
            console.warn('Long task detected:', entry.duration, 'ms');
          }
        }
      });
      
      try {
        longTaskObserver.observe({ entryTypes: ['longtask'] });
      } catch (e) {
        // Long task API not supported
      }
    }

    // Monitor layout shifts
    if ('PerformanceObserver' in window) {
      const clsObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.value > 0.1) { // CLS threshold
            monitoringService.trackEvent('layout_shift', {
              value: entry.value,
              sources: entry.sources,
              hadRecentInput: entry.hadRecentInput,
            });
          }
        }
      });
      
      try {
        clsObserver.observe({ entryTypes: ['layout-shift'] });
      } catch (e) {
        // Layout shift API not supported
      }
    }
  };

  /**
   * Set up security monitoring
   */
  const setupSecurityMonitoring = () => {
    // Monitor console access
    let consoleAccessCount = 0;
    const originalConsole = { ...console };
    
    Object.keys(console).forEach(method => {
      console[method] = (...args) => {
        consoleAccessCount++;
        
        if (consoleAccessCount > 50) { // Suspicious console activity
          securityService.logSecurityEvent('EXCESSIVE_CONSOLE_ACCESS', {
            count: consoleAccessCount,
            method,
            timestamp: new Date().toISOString(),
          });
        }
        
        return originalConsole[method].apply(console, args);
      };
    });

    // Monitor for suspicious script injection
    const originalCreateElement = document.createElement;
    document.createElement = function(tagName) {
      const element = originalCreateElement.call(this, tagName);
      
      if (tagName.toLowerCase() === 'script') {
        securityService.logSecurityEvent('SCRIPT_ELEMENT_CREATED', {
          timestamp: new Date().toISOString(),
          stackTrace: new Error().stack,
        });
      }
      
      return element;
    };

    // Monitor clipboard access
    if (navigator.clipboard) {
      const originalReadText = navigator.clipboard.readText;
      navigator.clipboard.readText = function() {
        securityService.logSecurityEvent('CLIPBOARD_ACCESS', {
          type: 'read',
          timestamp: new Date().toISOString(),
        });
        return originalReadText.apply(this, arguments);
      };
    }

    // Monitor location changes
    let lastLocation = window.location.href;
    setInterval(() => {
      if (window.location.href !== lastLocation) {
        monitoringService.trackEvent('navigation', {
          from: lastLocation,
          to: window.location.href,
          timestamp: new Date().toISOString(),
        });
        lastLocation = window.location.href;
      }
    }, 1000);
  };

  /**
   * Set up connectivity monitoring
   */
  const setupConnectivityMonitoring = () => {
    const handleOnline = () => {
      monitoringService.trackEvent('connectivity_restored', {
        timestamp: new Date().toISOString(),
        offlineDuration: Date.now() - (window.offlineStartTime || Date.now()),
      });
      
      // Sync offline data
      if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: 'SYNC_OFFLINE_DATA'
        });
      }
      
      console.log('Application back online');
    };

    const handleOffline = () => {
      window.offlineStartTime = Date.now();
      
      monitoringService.trackEvent('connectivity_lost', {
        timestamp: new Date().toISOString(),
      });
      
      console.log('Application offline');
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Monitor connection quality
    if (navigator.connection) {
      const connection = navigator.connection;
      
      const trackConnectionChange = () => {
        monitoringService.trackEvent('connection_change', {
          effectiveType: connection.effectiveType,
          downlink: connection.downlink,
          rtt: connection.rtt,
          saveData: connection.saveData,
          timestamp: new Date().toISOString(),
        });
      };
      
      connection.addEventListener('change', trackConnectionChange);
      
      // Initial connection tracking
      trackConnectionChange();
    }
  };

  /**
   * Set up user activity monitoring
   */
  const setupUserActivityMonitoring = () => {
    let isActive = true;
    let lastActivity = Date.now();
    
    const activityEvents = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'];
    
    const updateActivity = () => {
      const now = Date.now();
      const wasInactive = !isActive;
      
      isActive = true;
      lastActivity = now;
      
      if (wasInactive) {
        monitoringService.trackEvent('user_became_active', {
          timestamp: new Date().toISOString(),
        });
      }
    };
    
    activityEvents.forEach(event => {
      document.addEventListener(event, updateActivity, { passive: true });
    });
    
    // Check for inactivity
    setInterval(() => {
      const now = Date.now();
      const timeSinceLastActivity = now - lastActivity;
      
      if (timeSinceLastActivity > 300000 && isActive) { // 5 minutes
        isActive = false;
        monitoringService.trackEvent('user_became_inactive', {
          inactiveDuration: timeSinceLastActivity,
          timestamp: new Date().toISOString(),
        });
      }
    }, 60000); // Check every minute

    // Track page visibility changes
    document.addEventListener('visibilitychange', () => {
      monitoringService.trackEvent('page_visibility_change', {
        hidden: document.hidden,
        timestamp: new Date().toISOString(),
      });
    });

    // Track focus/blur events
    window.addEventListener('focus', () => {
      monitoringService.trackEvent('window_focus', {
        timestamp: new Date().toISOString(),
      });
    });

    window.addEventListener('blur', () => {
      monitoringService.trackEvent('window_blur', {
        timestamp: new Date().toISOString(),
      });
    });
  };

  /**
   * Error boundary fallback component
   */
  const ErrorFallbackComponent = ({ error, resetErrorBoundary }) => {
    useEffect(() => {
      // Track error boundary activation
      monitoringService.trackError(error, {
        type: 'error_boundary',
        componentStack: error.componentStack,
      });
    }, [error]);

    return (
      <ErrorFallback 
        error={error} 
        resetErrorBoundary={resetErrorBoundary}
        onRetry={() => {
          monitoringService.trackEvent('error_boundary_retry', {
            error: error.message,
            timestamp: new Date().toISOString(),
          });
          resetErrorBoundary();
        }}
      />
    );
  };

  /**
   * Loading fallback component
   */
  const LoadingFallbackComponent = () => {
    useEffect(() => {
      const startTime = Date.now();
      
      return () => {
        const loadTime = Date.now() - startTime;
        performanceService.trackPerformance('component_loading', loadTime);
      };
    }, []);

    return <LoadingFallback />;
  };

  // Show error if initialization failed
  if (initError) {
    return (
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center', 
        minHeight: '100vh',
        padding: '20px',
        fontFamily: 'Arial, sans-serif'
      }}>
        <div style={{ 
          textAlign: 'center', 
          maxWidth: '500px',
          padding: '40px',
          backgroundColor: '#f8f9fa',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)'
        }}>
          <h2 style={{ color: '#dc3545', marginBottom: '16px' }}>
            Application Initialization Failed
          </h2>
          <p style={{ color: '#6c757d', marginBottom: '24px' }}>
            {initError.message || 'An unexpected error occurred during startup.'}
          </p>
          <button 
            onClick={() => window.location.reload()}
            style={{
              padding: '12px 24px',
              backgroundColor: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '14px'
            }}
          >
            Reload Application
          </button>
        </div>
      </div>
    );
  }

  // Show loading while initializing
  if (!isInitialized) {
    return <LoadingFallbackComponent />;
  }

  return (
    <ErrorBoundary
      FallbackComponent={ErrorFallbackComponent}
      onError={(error, errorInfo) => {
        console.error('Error Boundary caught an error:', error, errorInfo);
        
        monitoringService.trackError(error, {
          type: 'react_error_boundary',
          componentStack: errorInfo.componentStack,
        });

        securityService.logSecurityEvent('REACT_ERROR_BOUNDARY', {
          error: error.message,
          componentStack: errorInfo.componentStack,
          timestamp: new Date().toISOString(),
        });
      }}
      onReset={() => {
        monitoringService.trackEvent('error_boundary_reset', {
          timestamp: new Date().toISOString(),
        });
      }}
    >
      <HelmetProvider>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <Router>
            <Suspense fallback={<LoadingFallbackComponent />}>
              {children}
            </Suspense>
          </Router>
        </ThemeProvider>
      </HelmetProvider>
    </ErrorBoundary>
  );
};

export default EnhancedAppWrapper;