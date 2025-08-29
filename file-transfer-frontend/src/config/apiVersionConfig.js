/**
 * Frontend API Version Configuration
 * Configuration for client-side API versioning and compatibility management
 */

const apiVersionConfig = {
  // Current API version settings
  current: {
    version: process.env.REACT_APP_API_VERSION || '2.0',
    strategy: process.env.REACT_APP_VERSION_STRATEGY || 'URL_PATH',
    strictMode: process.env.REACT_APP_VERSION_STRICT_MODE === 'true',
    requireVersion: process.env.REACT_APP_VERSION_REQUIRE === 'true'
  },

  // API endpoints
  endpoints: {
    web: {
      baseUrl: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
      versionPath: '/api/v{version}',
      healthCheck: '/actuator/health',
      versionInfo: '/api/versions'
    },
    batch: {
      baseUrl: process.env.REACT_APP_BATCH_API_URL || 'http://localhost:8082',
      versionPath: '/api/batch/v{version}',
      healthCheck: '/actuator/health',
      versionInfo: '/api/batch/versions'
    }
  },

  // Version strategies configuration
  strategies: {
    URL_PATH: {
      name: 'URL Path',
      description: 'Version specified in URL path: /api/v2/tenants',
      example: '/api/v2/tenants',
      implementation: 'path'
    },
    HEADER: {
      name: 'Header',
      description: 'Version specified in request header',
      example: 'X-API-Version: 2.0',
      implementation: 'header',
      headerName: 'X-API-Version'
    },
    QUERY_PARAMETER: {
      name: 'Query Parameter',
      description: 'Version specified in query parameter',
      example: '/api/tenants?version=2.0',
      implementation: 'query',
      parameterName: 'version'
    },
    MEDIA_TYPE: {
      name: 'Media Type',
      description: 'Version specified in Accept header',
      example: 'Accept: application/vnd.filetransfer.v2+json',
      implementation: 'media-type',
      mediaTypeTemplate: 'application/vnd.filetransfer.v{version}+json'
    },
    ACCEPT_HEADER: {
      name: 'Accept Version',
      description: 'Version specified in Accept-Version header',
      example: 'Accept-Version: 2.0',
      implementation: 'accept-header',
      headerName: 'Accept-Version'
    },
    HYBRID: {
      name: 'Hybrid',
      description: 'Multiple strategies with fallback',
      example: 'URL path, then headers, then query parameters',
      implementation: 'hybrid'
    }
  },

  // Supported versions
  versions: {
    '1.0': {
      status: 'deprecated',
      deprecated: true,
      sunsetDate: '2024-06-01',
      features: [
        'basic-crud',
        'simple-validation'
      ],
      limitations: [
        'no-pagination',
        'basic-error-handling',
        'limited-validation'
      ],
      endpoints: {
        tenants: '/api/v1/tenants',
        services: '/api/v1/services',
        files: '/api/v1/files'
      },
      compatibility: ['1.1', '1.2']
    },
    '1.1': {
      status: 'deprecated',
      deprecated: true,
      sunsetDate: '2024-06-01',
      features: [
        'basic-crud',
        'simple-validation',
        'bulk-operations'
      ],
      limitations: [
        'no-pagination',
        'basic-error-handling'
      ],
      endpoints: {
        tenants: '/api/v1/tenants',
        services: '/api/v1/services',
        files: '/api/v1/files',
        bulk: '/api/v1/bulk'
      },
      compatibility: ['1.0', '1.2']
    },
    '1.2': {
      status: 'sunset',
      deprecated: true,
      sunsetDate: '2024-03-01',
      features: [
        'basic-crud',
        'simple-validation',
        'bulk-operations',
        'basic-statistics'
      ],
      limitations: [
        'basic-error-handling'
      ],
      endpoints: {
        tenants: '/api/v1/tenants',
        services: '/api/v1/services',
        files: '/api/v1/files',
        bulk: '/api/v1/bulk',
        stats: '/api/v1/statistics'
      },
      compatibility: ['1.1', '2.0']
    },
    '2.0': {
      status: 'current',
      deprecated: false,
      features: [
        'enhanced-crud',
        'comprehensive-validation',
        'pagination',
        'sorting',
        'filtering',
        'configuration-management',
        'health-checks',
        'audit-trails',
        'bulk-operations-v2'
      ],
      limitations: [],
      endpoints: {
        tenants: '/api/v2/tenants',
        services: '/api/v2/services',
        files: '/api/v2/files',
        configuration: '/api/v2/configuration',
        health: '/api/v2/health',
        statistics: '/api/v2/statistics',
        audit: '/api/v2/audit'
      },
      compatibility: ['1.2', '2.1'],
      breakingChanges: [
        {
          description: 'Tenant ID now required for all operations',
          impact: 'All tenant-related endpoints require tenantId',
          mitigation: 'Include tenantId in all requests'
        },
        {
          description: 'Date format standardized to ISO 8601',
          impact: 'All date fields use yyyy-MM-ddTHH:mm:ss format',
          mitigation: 'Update date parsing and formatting'
        },
        {
          description: 'Enhanced validation rules',
          impact: 'Stricter validation for all input fields',
          mitigation: 'Review and update form validation'
        }
      ]
    },
    '2.1': {
      status: 'beta',
      deprecated: false,
      features: [
        'enhanced-crud',
        'comprehensive-validation',
        'pagination',
        'sorting',
        'filtering',
        'configuration-management',
        'health-checks',
        'audit-trails',
        'bulk-operations-v2',
        'data-export-import',
        'advanced-analytics',
        'real-time-monitoring'
      ],
      limitations: [
        'beta-features-may-change'
      ],
      endpoints: {
        tenants: '/api/v2/tenants',
        services: '/api/v2/services',
        files: '/api/v2/files',
        configuration: '/api/v2/configuration',
        health: '/api/v2/health',
        statistics: '/api/v2/statistics',
        audit: '/api/v2/audit',
        export: '/api/v2/export',
        import: '/api/v2/import',
        analytics: '/api/v2/analytics'
      },
      compatibility: ['2.0']
    }
  },

  // Version negotiation settings
  negotiation: {
    enabled: true,
    fallbackStrategy: 'latest-compatible',
    retryAttempts: 3,
    timeout: 5000
  },

  // Client compatibility settings
  client: {
    // Automatic version upgrade
    autoUpgrade: {
      enabled: process.env.REACT_APP_AUTO_VERSION_UPGRADE === 'true',
      targetVersion: '2.0',
      confirmationRequired: true
    },
    
    // Version caching
    caching: {
      enabled: true,
      duration: 24 * 60 * 60 * 1000, // 24 hours
      storageKey: 'api_version_cache'
    },
    
    // Error handling
    errorHandling: {
      showDeprecationWarnings: true,
      showVersionErrors: true,
      fallbackToCompatible: true,
      logVersionEvents: process.env.NODE_ENV === 'development'
    }
  },

  // Feature flags based on API version
  features: {
    // Map API version features to frontend feature flags
    featureMapping: {
      'pagination': ['enhanced-crud', 'pagination'],
      'bulkOperations': ['bulk-operations', 'bulk-operations-v2'],
      'statistics': ['basic-statistics', 'advanced-analytics'],
      'exportImport': ['data-export-import'],
      'auditTrails': ['audit-trails'],
      'realTimeMonitoring': ['real-time-monitoring'],
      'configurationManagement': ['configuration-management'],
      'healthChecks': ['health-checks']
    }
  },

  // UI adaptation based on version
  ui: {
    // Show version-specific UI elements
    showVersionInfo: process.env.NODE_ENV === 'development',
    showDeprecationBanner: true,
    showMigrationPrompts: true,
    
    // Version-specific styling
    versionStyling: {
      '1.0': { theme: 'legacy', color: '#ff9800' },
      '1.1': { theme: 'legacy', color: '#ff9800' },
      '1.2': { theme: 'transition', color: '#f44336' },
      '2.0': { theme: 'modern', color: '#4caf50' },
      '2.1': { theme: 'beta', color: '#2196f3' }
    }
  },

  // Environment-specific overrides
  environments: {
    development: {
      current: {
        strictMode: false
      },
      client: {
        errorHandling: {
          logVersionEvents: true,
          showVersionErrors: true
        }
      },
      ui: {
        showVersionInfo: true
      }
    },
    
    production: {
      current: {
        strictMode: false
      },
      client: {
        autoUpgrade: {
          enabled: false // Disable auto-upgrade in production
        },
        errorHandling: {
          logVersionEvents: false,
          showVersionErrors: false
        }
      },
      ui: {
        showVersionInfo: false
      }
    },
    
    testing: {
      current: {
        strictMode: true,
        version: '2.0'
      },
      negotiation: {
        enabled: false // Use exact version for testing
      }
    }
  }
};

// Apply environment-specific overrides
const currentEnv = process.env.NODE_ENV || 'development';
if (apiVersionConfig.environments[currentEnv]) {
  const envConfig = apiVersionConfig.environments[currentEnv];
  
  // Deep merge environment configuration
  function deepMerge(target, source) {
    for (const key in source) {
      if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
        if (!target[key]) target[key] = {};
        deepMerge(target[key], source[key]);
      } else {
        target[key] = source[key];
      }
    }
  }
  
  deepMerge(apiVersionConfig, envConfig);
}

export default apiVersionConfig;