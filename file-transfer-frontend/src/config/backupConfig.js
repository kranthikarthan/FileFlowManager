/**
 * Frontend Backup Configuration
 * Configuration settings for frontend application backup and disaster recovery
 */

const backupConfig = {
  // Basic backup settings
  enabled: process.env.REACT_APP_BACKUP_ENABLED === 'true',
  endpoint: process.env.REACT_APP_BACKUP_ENDPOINT || '/api/frontend-backup',
  version: process.env.REACT_APP_VERSION || '1.0.0',
  
  // Storage configuration
  storage: {
    // IndexedDB configuration
    indexedDB: {
      name: 'FrontendBackupDB',
      version: 1,
      stores: {
        userDataBackups: {
          keyPath: 'id',
          indexes: ['timestamp', 'type']
        },
        appStateBackups: {
          keyPath: 'id',
          indexes: ['timestamp', 'component']
        },
        configBackups: {
          keyPath: 'id',
          indexes: ['timestamp']
        },
        assetBackups: {
          keyPath: 'id',
          indexes: ['timestamp', 'assetType']
        },
        backupMetadata: {
          keyPath: 'backupId',
          indexes: ['createdAt', 'type']
        }
      }
    },
    
    // Local storage prefixes for user data identification
    userDataPrefixes: [
      'user_',
      'profile_',
      'preferences_',
      'settings_',
      'auth_',
      'tenant_',
      'service_',
      'dashboard_'
    ],
    
    // Critical data prefixes that trigger immediate backup
    criticalDataPrefixes: [
      'auth_',
      'session_',
      'user_profile',
      'tenant_config',
      'service_config'
    ]
  },
  
  // Backup types and schedules
  backup: {
    types: {
      FULL: 'FULL',
      USER_DATA: 'USER_DATA',
      APP_STATE: 'APP_STATE',
      CONFIG: 'CONFIG',
      EMERGENCY: 'EMERGENCY',
      RESTORE_POINT: 'RESTORE_POINT'
    },
    
    // Automatic backup schedules (in milliseconds)
    schedules: {
      userData: 5 * 60 * 1000,        // 5 minutes
      appState: 10 * 60 * 1000,       // 10 minutes
      configuration: 60 * 60 * 1000,  // 1 hour
      fullBackup: 24 * 60 * 60 * 1000, // 24 hours
      cleanup: 7 * 24 * 60 * 60 * 1000 // 7 days
    },
    
    // Backup retention policies
    retention: {
      days: parseInt(process.env.REACT_APP_BACKUP_RETENTION_DAYS) || 30,
      maxBackups: {
        full: 12,
        userData: 168,      // 1 week of 5-minute backups
        appState: 72,       // 3 days of 10-minute backups
        config: 24,         // 1 day of hourly backups
        emergency: 10,      // Keep last 10 emergency backups
        restorePoint: 5     // Keep last 5 restore points
      }
    }
  },
  
  // Data collection configuration
  dataCollection: {
    // User data sources
    userData: {
      localStorage: true,
      sessionStorage: true,
      indexedDB: true,
      cookies: true,
      cacheAPI: true
    },
    
    // Application state sources
    appState: {
      componentStates: true,
      routing: true,
      forms: true,
      uiState: true,
      temporaryData: true,
      performance: true
    },
    
    // Configuration sources
    configuration: {
      app: true,
      userPreferences: true,
      theme: true,
      featureFlags: true,
      api: true,
      security: true
    },
    
    // Asset sources
    assets: {
      images: true,
      documents: true,
      offlineData: true,
      serviceWorkerCache: true
    }
  },
  
  // Compression and encryption
  compression: {
    enabled: process.env.REACT_APP_BACKUP_COMPRESSION === 'true',
    algorithm: 'gzip',
    level: 6
  },
  
  encryption: {
    enabled: process.env.REACT_APP_BACKUP_ENCRYPTION === 'true',
    algorithm: 'AES-GCM',
    keyLength: 256
  },
  
  // Remote synchronization
  remote: {
    enabled: process.env.REACT_APP_BACKUP_REMOTE_ENABLED === 'true',
    syncInterval: 30 * 60 * 1000, // 30 minutes
    retryAttempts: 3,
    retryDelay: 5000
  },
  
  // Cross-application coordination
  crossApp: {
    enabled: process.env.REACT_APP_CROSS_APP_BACKUP_ENABLED === 'true',
    webApiUrl: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
    batchApiUrl: process.env.REACT_APP_BATCH_API_URL || 'http://localhost:8082',
    coordinationTimeout: 300000, // 5 minutes
    retryAttempts: 3,
    retryDelay: 5000
  },
  
  // User activity monitoring
  activity: {
    // User inactivity threshold for background operations
    inactivityThreshold: 10 * 60 * 1000, // 10 minutes
    
    // Events that trigger state backup
    triggerEvents: [
      'beforeunload',
      'visibilitychange',
      'pagehide',
      'storage'
    ],
    
    // Form events that trigger emergency backup
    formEvents: [
      'input',
      'change',
      'focusout'
    ],
    
    // Critical actions that create restore points
    criticalActions: [
      'tenant_create',
      'tenant_delete',
      'service_create',
      'service_delete',
      'bulk_operation',
      'data_import',
      'configuration_change'
    ]
  },
  
  // Performance optimization
  performance: {
    // Batch size for large operations
    batchSize: 100,
    
    // Debounce delays for frequent operations
    debounce: {
      formBackup: 2000,        // 2 seconds
      stateBackup: 5000,       // 5 seconds
      userDataBackup: 10000    // 10 seconds
    },
    
    // Maximum backup size (bytes)
    maxBackupSize: 50 * 1024 * 1024, // 50MB
    
    // Chunk size for large backups
    chunkSize: 1024 * 1024 // 1MB
  },
  
  // Error handling and monitoring
  monitoring: {
    // Enable detailed logging
    enableLogging: process.env.NODE_ENV === 'development',
    
    // Log levels
    logLevel: process.env.REACT_APP_LOG_LEVEL || 'info',
    
    // Performance metrics
    collectMetrics: true,
    
    // Error reporting
    reportErrors: process.env.REACT_APP_ERROR_REPORTING === 'true',
    
    // Health check interval
    healthCheckInterval: 60000 // 1 minute
  },
  
  // Security settings
  security: {
    // Sensitive data patterns to exclude from backups
    excludePatterns: [
      /password/i,
      /secret/i,
      /token/i,
      /key/i,
      /credit.*card/i,
      /ssn/i,
      /social.*security/i
    ],
    
    // Data anonymization
    anonymize: {
      enabled: true,
      fields: ['email', 'phone', 'address'],
      method: 'hash' // 'hash', 'mask', 'remove'
    },
    
    // Backup integrity verification
    integrity: {
      enabled: true,
      algorithm: 'SHA-256'
    }
  },
  
  // UI/UX settings
  ui: {
    // Show backup status in UI
    showStatus: true,
    
    // Show backup progress during operations
    showProgress: true,
    
    // Notification settings
    notifications: {
      enabled: true,
      types: {
        success: true,
        warning: true,
        error: true
      },
      duration: 5000 // 5 seconds
    },
    
    // Backup management interface
    management: {
      enabled: true,
      maxVisibleBackups: 20,
      showDetails: true,
      allowDownload: true,
      allowDelete: true
    }
  },
  
  // Environment-specific overrides
  environments: {
    development: {
      backup: {
        schedules: {
          userData: 30 * 1000,      // 30 seconds for testing
          appState: 60 * 1000,      // 1 minute
          configuration: 5 * 60 * 1000, // 5 minutes
          fullBackup: 10 * 60 * 1000    // 10 minutes
        },
        retention: {
          days: 1,
          maxBackups: {
            full: 3,
            userData: 20,
            appState: 10,
            config: 5
          }
        }
      },
      monitoring: {
        enableLogging: true,
        logLevel: 'debug'
      }
    },
    
    production: {
      compression: {
        enabled: true
      },
      encryption: {
        enabled: true
      },
      remote: {
        enabled: true
      },
      monitoring: {
        enableLogging: false,
        logLevel: 'error',
        reportErrors: true
      }
    },
    
    ha: {
      backup: {
        schedules: {
          userData: 2 * 60 * 1000,       // 2 minutes
          appState: 5 * 60 * 1000,       // 5 minutes
          configuration: 30 * 60 * 1000, // 30 minutes
          fullBackup: 6 * 60 * 60 * 1000 // 6 hours
        },
        retention: {
          days: 90,
          maxBackups: {
            full: 48,
            userData: 720,  // 1 day of 2-minute backups
            appState: 288,  // 1 day of 5-minute backups
            config: 48      // 1 day of 30-minute backups
          }
        }
      },
      performance: {
        maxBackupSize: 100 * 1024 * 1024, // 100MB
        chunkSize: 2 * 1024 * 1024         // 2MB
      }
    }
  }
};

// Apply environment-specific overrides
const currentEnv = process.env.NODE_ENV || 'development';
if (backupConfig.environments[currentEnv]) {
  const envConfig = backupConfig.environments[currentEnv];
  
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
  
  deepMerge(backupConfig, envConfig);
}

export default backupConfig;