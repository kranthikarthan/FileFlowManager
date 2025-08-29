/**
 * Enhanced application configuration
 * Centralized configuration for monitoring, security, performance, and feature flags
 */

const config = {
  // Application Information
  app: {
    name: 'File Transfer Management System',
    version: process.env.REACT_APP_VERSION || '1.2.0',
    environment: process.env.NODE_ENV || 'development',
    buildNumber: process.env.REACT_APP_BUILD_NUMBER || 'local',
    commitHash: process.env.REACT_APP_COMMIT_HASH || 'unknown',
  },

  // API Configuration
  api: {
    baseUrl: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
    batchUrl: process.env.REACT_APP_BATCH_API_URL || 'http://localhost:8082',
    timeout: parseInt(process.env.REACT_APP_API_TIMEOUT) || 30000,
    retryAttempts: parseInt(process.env.REACT_APP_API_RETRY_ATTEMPTS) || 3,
    retryDelay: parseInt(process.env.REACT_APP_API_RETRY_DELAY) || 1000,
  },

  // Monitoring Configuration
  monitoring: {
    sentry: {
      dsn: process.env.REACT_APP_SENTRY_DSN || '',
      environment: process.env.NODE_ENV || 'development',
      tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
      replaysSessionSampleRate: 0.1,
      replaysOnErrorSampleRate: 1.0,
    },
    analytics: {
      enabled: process.env.REACT_APP_ANALYTICS_ENABLED !== 'false',
      batchSize: parseInt(process.env.REACT_APP_ANALYTICS_BATCH_SIZE) || 10,
      flushInterval: parseInt(process.env.REACT_APP_ANALYTICS_FLUSH_INTERVAL) || 30000,
    },
    performance: {
      enabled: true,
      trackWebVitals: true,
      trackUserInteractions: true,
      trackApiCalls: true,
      trackComponentPerformance: process.env.NODE_ENV !== 'production',
    },
  },

  // Security Configuration
  security: {
    csp: {
      enabled: true,
      reportOnly: process.env.NODE_ENV !== 'production',
    },
    xss: {
      enabled: true,
      sanitizeHtml: true,
      preventInlineScripts: true,
    },
    rateLimiting: {
      enabled: true,
      api: { requests: 100, window: 60000 },
      login: { requests: 5, window: 300000 },
      fileUpload: { requests: 10, window: 60000 },
      search: { requests: 30, window: 60000 },
    },
    validation: {
      enabled: true,
      strictMode: process.env.NODE_ENV === 'production',
      maxRequestSize: 10 * 1024 * 1024, // 10MB
    },
    encryption: {
      enabled: true,
      algorithm: 'AES-GCM',
      keyRotationInterval: 24 * 60 * 60 * 1000, // 24 hours
    },
  },

  // Performance Configuration
  performance: {
    caching: {
      enabled: true,
      strategies: {
        api: 'networkFirst',
        static: 'cacheFirst',
        images: 'cacheFirst',
      },
      ttl: {
        api: 5 * 60 * 1000, // 5 minutes
        static: 24 * 60 * 60 * 1000, // 24 hours
        images: 7 * 24 * 60 * 60 * 1000, // 7 days
      },
    },
    lazyLoading: {
      enabled: true,
      threshold: 0.1,
      rootMargin: '50px',
    },
    bundleSplitting: {
      enabled: true,
      chunkSizeLimit: 244 * 1024, // 244KB
    },
    imageOptimization: {
      enabled: true,
      formats: ['webp', 'avif', 'jpg', 'png'],
      quality: 80,
      responsiveSizes: [480, 768, 1024, 1200, 1920],
    },
    memoryManagement: {
      enabled: true,
      cleanupInterval: 5 * 60 * 1000, // 5 minutes
      memoryThreshold: 80, // 80% of available memory
    },
  },

  // Offline/PWA Configuration
  offline: {
    enabled: true,
    cacheStrategy: 'networkFirst',
    precacheRoutes: ['/', '/dashboard', '/tenants', '/services', '/profile'],
    backgroundSync: {
      enabled: true,
      patterns: ['file-upload', 'analytics', 'security-events'],
    },
    notifications: {
      enabled: true,
      vapidKey: process.env.REACT_APP_VAPID_KEY || '',
    },
  },

  // Feature Flags
  features: {
    // Core Features
    tenantManagement: true,
    serviceConfiguration: true,
    fileTransfers: true,
    monitoring: true,
    
    // Advanced Features
    batchProcessing: true,
    realTimeNotifications: process.env.REACT_APP_REALTIME_ENABLED === 'true',
    advancedAnalytics: process.env.REACT_APP_ANALYTICS_ADVANCED === 'true',
    multiLanguage: process.env.REACT_APP_I18N_ENABLED === 'true',
    
    // Experimental Features
    experimentalUI: process.env.REACT_APP_EXPERIMENTAL_UI === 'true',
    betaFeatures: process.env.REACT_APP_BETA_FEATURES === 'true',
    debugMode: process.env.NODE_ENV === 'development',
  },

  // UI/UX Configuration
  ui: {
    theme: {
      mode: 'system', // 'light', 'dark', 'system'
      primaryColor: '#667eea',
      secondaryColor: '#764ba2',
      transitions: {
        duration: 300,
        easing: 'ease-in-out',
      },
    },
    layout: {
      sidebarWidth: 280,
      headerHeight: 64,
      footerHeight: 48,
      contentPadding: 24,
    },
    breakpoints: {
      xs: 0,
      sm: 600,
      md: 960,
      lg: 1280,
      xl: 1920,
    },
    animations: {
      enabled: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
      duration: 300,
    },
  },

  // Accessibility Configuration
  accessibility: {
    enabled: true,
    announceNavigation: true,
    highContrast: false,
    focusVisible: true,
    reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    screenReaderOptimizations: true,
  },

  // Development Configuration
  development: {
    debugMode: process.env.NODE_ENV === 'development',
    hotReload: process.env.NODE_ENV === 'development',
    mockApi: process.env.REACT_APP_MOCK_API === 'true',
    showPerformancePanel: process.env.REACT_APP_PERF_PANEL === 'true',
    enableDevTools: process.env.NODE_ENV === 'development',
    logLevel: process.env.REACT_APP_LOG_LEVEL || 'info',
  },

  // Storage Configuration
  storage: {
    prefix: 'ft_',
    encryption: true,
    expiration: {
      session: 24 * 60 * 60 * 1000, // 24 hours
      persistent: 7 * 24 * 60 * 60 * 1000, // 7 days
      cache: 30 * 24 * 60 * 60 * 1000, // 30 days
    },
    quota: {
      indexedDB: 50 * 1024 * 1024, // 50MB
      localStorage: 5 * 1024 * 1024, // 5MB
      sessionStorage: 5 * 1024 * 1024, // 5MB
    },
  },

  // Error Handling Configuration
  errorHandling: {
    enabled: true,
    showUserFriendlyMessages: true,
    captureConsoleErrors: true,
    captureUnhandledRejections: true,
    captureResourceErrors: true,
    fallbackToOffline: true,
    retryStrategies: {
      network: { attempts: 3, delay: 1000, backoff: 2 },
      api: { attempts: 2, delay: 500, backoff: 1.5 },
      render: { attempts: 1, delay: 0, backoff: 1 },
    },
  },

  // Internationalization Configuration
  i18n: {
    enabled: process.env.REACT_APP_I18N_ENABLED === 'true',
    defaultLanguage: 'en',
    supportedLanguages: ['en', 'es', 'fr', 'de'],
    fallbackLanguage: 'en',
    detectBrowserLanguage: true,
    persistLanguageChoice: true,
  },

  // Third-party Integrations
  integrations: {
    googleAnalytics: {
      enabled: !!process.env.REACT_APP_GA_TRACKING_ID,
      trackingId: process.env.REACT_APP_GA_TRACKING_ID || '',
    },
    hotjar: {
      enabled: !!process.env.REACT_APP_HOTJAR_ID,
      id: process.env.REACT_APP_HOTJAR_ID || '',
    },
    intercom: {
      enabled: !!process.env.REACT_APP_INTERCOM_APP_ID,
      appId: process.env.REACT_APP_INTERCOM_APP_ID || '',
    },
  },

  // Testing Configuration
  testing: {
    enabled: process.env.NODE_ENV === 'test',
    mockDelay: 100,
    mockFailureRate: 0.05, // 5% failure rate for testing error handling
    e2e: {
      baseUrl: process.env.REACT_APP_E2E_BASE_URL || 'http://localhost:3000',
      timeout: 30000,
    },
  },
};

// Configuration validation
const validateConfig = () => {
  const requiredFields = [
    'app.name',
    'app.version',
    'api.baseUrl',
  ];

  const missingFields = requiredFields.filter(field => {
    const value = field.split('.').reduce((obj, key) => obj?.[key], config);
    return !value;
  });

  if (missingFields.length > 0) {
    console.warn('Missing required configuration fields:', missingFields);
  }

  return missingFields.length === 0;
};

// Environment-specific overrides
if (config.app.environment === 'production') {
  config.development.debugMode = false;
  config.development.enableDevTools = false;
  config.performance.trackComponentPerformance = false;
  config.errorHandling.captureConsoleErrors = false;
}

if (config.app.environment === 'test') {
  config.monitoring.analytics.enabled = false;
  config.offline.enabled = false;
  config.integrations.googleAnalytics.enabled = false;
  config.integrations.hotjar.enabled = false;
  config.integrations.intercom.enabled = false;
}

// Validate configuration on load
validateConfig();

export default config;