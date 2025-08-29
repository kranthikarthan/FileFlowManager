/**
 * Frontend Backup Service
 * Provides backup functionality for frontend application data, configuration, and state
 */

import { openDB } from 'idb';

class FrontendBackupService {
  constructor() {
    this.dbName = 'FrontendBackupDB';
    this.dbVersion = 1;
    this.backupEndpoint = '/api/frontend-backup';
    this.isInitialized = false;
    
    this.initialize();
  }

  /**
   * Initialize backup service
   */
  async initialize() {
    try {
      // Initialize IndexedDB for local backup storage
      await this.initializeDB();
      
      // Set up automatic backup scheduling
      this.setupAutomaticBackup();
      
      // Set up state change monitoring
      this.setupStateMonitoring();
      
      this.isInitialized = true;
      console.log('Frontend backup service initialized');
      
    } catch (error) {
      console.error('Failed to initialize frontend backup service:', error);
    }
  }

  /**
   * Initialize IndexedDB for backup storage
   */
  async initializeDB() {
    this.db = await openDB(this.dbName, this.dbVersion, {
      upgrade(db) {
        // User data backups
        if (!db.objectStoreNames.contains('userDataBackups')) {
          const userDataStore = db.createObjectStore('userDataBackups', { keyPath: 'id' });
          userDataStore.createIndex('timestamp', 'timestamp');
          userDataStore.createIndex('type', 'type');
        }
        
        // Application state backups
        if (!db.objectStoreNames.contains('appStateBackups')) {
          const appStateStore = db.createObjectStore('appStateBackups', { keyPath: 'id' });
          appStateStore.createIndex('timestamp', 'timestamp');
          appStateStore.createIndex('component', 'component');
        }
        
        // Configuration backups
        if (!db.objectStoreNames.contains('configBackups')) {
          const configStore = db.createObjectStore('configBackups', { keyPath: 'id' });
          configStore.createIndex('timestamp', 'timestamp');
        }
        
        // Asset backups
        if (!db.objectStoreNames.contains('assetBackups')) {
          const assetStore = db.createObjectStore('assetBackups', { keyPath: 'id' });
          assetStore.createIndex('timestamp', 'timestamp');
          assetStore.createIndex('assetType', 'assetType');
        }
        
        // Backup metadata
        if (!db.objectStoreNames.contains('backupMetadata')) {
          const metadataStore = db.createObjectStore('backupMetadata', { keyPath: 'backupId' });
          metadataStore.createIndex('createdAt', 'createdAt');
          metadataStore.createIndex('type', 'type');
        }
      },
    });
  }

  /**
   * Set up automatic backup scheduling
   */
  setupAutomaticBackup() {
    // Backup user data every 5 minutes
    setInterval(() => {
      this.backupUserData();
    }, 5 * 60 * 1000);

    // Backup application state every 10 minutes
    setInterval(() => {
      this.backupApplicationState();
    }, 10 * 60 * 1000);

    // Backup configuration every hour
    setInterval(() => {
      this.backupConfiguration();
    }, 60 * 60 * 1000);

    // Full backup daily (when user is active)
    setInterval(() => {
      if (this.isUserActive()) {
        this.performFullBackup();
      }
    }, 24 * 60 * 60 * 1000);

    // Cleanup old backups weekly
    setInterval(() => {
      this.cleanupOldBackups();
    }, 7 * 24 * 60 * 60 * 1000);
  }

  /**
   * Set up state change monitoring
   */
  setupStateMonitoring() {
    // Monitor storage events
    window.addEventListener('storage', (event) => {
      if (this.isCriticalData(event.key)) {
        this.createChangeBackup(event.key, event.newValue, event.oldValue);
      }
    });

    // Monitor before unload for emergency backup
    window.addEventListener('beforeunload', () => {
      this.createEmergencyBackup();
    });

    // Monitor visibility change for state backup
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.backupCurrentState();
      }
    });
  }

  /**
   * Perform full frontend backup
   */
  async performFullBackup(options = {}) {
    const backupId = this.generateBackupId('FULL');
    const startTime = Date.now();
    
    try {
      console.log('Starting full frontend backup:', backupId);
      
      const backupData = {
        id: backupId,
        type: 'FULL',
        timestamp: new Date().toISOString(),
        data: {}
      };

      // Backup user data
      const userData = await this.collectUserData();
      backupData.data.userData = userData;

      // Backup application state
      const appState = await this.collectApplicationState();
      backupData.data.applicationState = appState;

      // Backup configuration
      const configuration = await this.collectConfiguration();
      backupData.data.configuration = configuration;

      // Backup local assets
      const assets = await this.collectLocalAssets();
      backupData.data.assets = assets;

      // Backup preferences and settings
      const preferences = await this.collectUserPreferences();
      backupData.data.preferences = preferences;

      // Backup session information
      const sessionInfo = await this.collectSessionInfo();
      backupData.data.sessionInfo = sessionInfo;

      // Calculate backup size
      const backupSize = this.calculateBackupSize(backupData);
      backupData.size = backupSize;
      backupData.duration = Date.now() - startTime;

      // Store backup locally
      await this.storeBackupLocally(backupData);

      // Store backup metadata
      const metadata = this.createBackupMetadata(backupData);
      await this.storeBackupMetadata(metadata);

      // Sync to server if enabled
      if (options.syncToServer !== false) {
        await this.syncBackupToServer(backupData);
      }

      console.log('Full frontend backup completed:', backupId, 'Size:', backupSize, 'bytes');
      return { success: true, backupId, size: backupSize, duration: backupData.duration };

    } catch (error) {
      console.error('Full frontend backup failed:', backupId, error);
      return { success: false, backupId, error: error.message };
    }
  }

  /**
   * Backup user data
   */
  async backupUserData() {
    try {
      const userData = await this.collectUserData();
      const backupId = this.generateBackupId('USER_DATA');
      
      const backup = {
        id: backupId,
        type: 'USER_DATA',
        timestamp: new Date().toISOString(),
        data: userData,
        size: this.calculateDataSize(userData)
      };

      await this.db.put('userDataBackups', backup);
      console.log('User data backup completed:', backupId);
      
    } catch (error) {
      console.error('User data backup failed:', error);
    }
  }

  /**
   * Backup application state
   */
  async backupApplicationState() {
    try {
      const appState = await this.collectApplicationState();
      const backupId = this.generateBackupId('APP_STATE');
      
      const backup = {
        id: backupId,
        type: 'APP_STATE',
        timestamp: new Date().toISOString(),
        data: appState,
        size: this.calculateDataSize(appState)
      };

      await this.db.put('appStateBackups', backup);
      console.log('Application state backup completed:', backupId);
      
    } catch (error) {
      console.error('Application state backup failed:', error);
    }
  }

  /**
   * Backup configuration
   */
  async backupConfiguration() {
    try {
      const configuration = await this.collectConfiguration();
      const backupId = this.generateBackupId('CONFIG');
      
      const backup = {
        id: backupId,
        type: 'CONFIG',
        timestamp: new Date().toISOString(),
        data: configuration,
        size: this.calculateDataSize(configuration)
      };

      await this.db.put('configBackups', backup);
      console.log('Configuration backup completed:', backupId);
      
    } catch (error) {
      console.error('Configuration backup failed:', error);
    }
  }

  /**
   * Create emergency backup
   */
  async createEmergencyBackup() {
    try {
      const backupId = this.generateBackupId('EMERGENCY');
      
      const emergencyData = {
        userForms: this.collectActiveFormData(),
        unsavedChanges: this.collectUnsavedChanges(),
        currentRoute: window.location.pathname,
        scrollPositions: this.collectScrollPositions(),
        temporaryData: this.collectTemporaryData()
      };

      const backup = {
        id: backupId,
        type: 'EMERGENCY',
        timestamp: new Date().toISOString(),
        data: emergencyData,
        size: this.calculateDataSize(emergencyData)
      };

      await this.db.put('appStateBackups', backup);
      console.log('Emergency backup completed:', backupId);
      
    } catch (error) {
      console.error('Emergency backup failed:', error);
    }
  }

  /**
   * Restore from backup
   */
  async restoreFromBackup(backupId, options = {}) {
    try {
      console.log('Starting frontend restore from backup:', backupId);
      
      // Get backup metadata
      const metadata = await this.db.get('backupMetadata', backupId);
      if (!metadata) {
        throw new Error(`Backup not found: ${backupId}`);
      }

      // Get backup data based on type
      let backupData;
      switch (metadata.type) {
        case 'FULL':
          backupData = await this.getFullBackupData(backupId);
          break;
        case 'USER_DATA':
          backupData = await this.db.get('userDataBackups', backupId);
          break;
        case 'APP_STATE':
          backupData = await this.db.get('appStateBackups', backupId);
          break;
        case 'CONFIG':
          backupData = await this.db.get('configBackups', backupId);
          break;
        default:
          throw new Error(`Unsupported backup type: ${metadata.type}`);
      }

      if (!backupData) {
        throw new Error(`Backup data not found: ${backupId}`);
      }

      // Create restore point if requested
      if (options.createRestorePoint !== false) {
        await this.createRestorePoint();
      }

      // Restore data based on options
      const restoreResult = await this.performRestore(backupData, options);
      
      console.log('Frontend restore completed:', backupId);
      return { success: true, backupId, restored: restoreResult };

    } catch (error) {
      console.error('Frontend restore failed:', backupId, error);
      return { success: false, backupId, error: error.message };
    }
  }

  /**
   * Collect user data for backup
   */
  async collectUserData() {
    const userData = {};

    // Local storage data
    userData.localStorage = {};
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (this.isUserDataKey(key)) {
        userData.localStorage[key] = localStorage.getItem(key);
      }
    }

    // Session storage data
    userData.sessionStorage = {};
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i);
      if (this.isUserDataKey(key)) {
        userData.sessionStorage[key] = sessionStorage.getItem(key);
      }
    }

    // IndexedDB user data
    userData.indexedDB = await this.collectIndexedDBUserData();

    // Cookies (non-sensitive)
    userData.cookies = this.collectUserCookies();

    // Cache API data
    userData.cacheAPI = await this.collectCacheAPIData();

    return userData;
  }

  /**
   * Collect application state for backup
   */
  async collectApplicationState() {
    const appState = {};

    // Component states (from state management)
    appState.componentStates = this.collectComponentStates();

    // Route information
    appState.routing = {
      currentPath: window.location.pathname,
      currentParams: new URLSearchParams(window.location.search).toString(),
      history: this.getRouteHistory()
    };

    // Form data
    appState.forms = this.collectActiveFormData();

    // UI state
    appState.uiState = {
      theme: this.getCurrentTheme(),
      language: this.getCurrentLanguage(),
      layout: this.getCurrentLayout(),
      sidebarState: this.getSidebarState(),
      modalStates: this.getModalStates()
    };

    // Temporary data
    appState.temporaryData = this.collectTemporaryData();

    // Performance data
    appState.performance = this.collectPerformanceData();

    return appState;
  }

  /**
   * Collect configuration for backup
   */
  async collectConfiguration() {
    const configuration = {};

    // Application configuration
    configuration.app = {
      version: process.env.REACT_APP_VERSION,
      buildNumber: process.env.REACT_APP_BUILD_NUMBER,
      environment: process.env.NODE_ENV
    };

    // User preferences
    configuration.userPreferences = await this.collectUserPreferences();

    // Theme configuration
    configuration.theme = this.getThemeConfiguration();

    // Feature flags
    configuration.featureFlags = this.getFeatureFlags();

    // API configuration
    configuration.api = this.getAPIConfiguration();

    // Security configuration (non-sensitive)
    configuration.security = this.getSecurityConfiguration();

    return configuration;
  }

  /**
   * Collect local assets for backup
   */
  async collectLocalAssets() {
    const assets = {};

    // Cached images
    assets.images = await this.collectCachedImages();

    // Cached documents
    assets.documents = await this.collectCachedDocuments();

    // Offline data
    assets.offlineData = await this.collectOfflineData();

    // Service worker cache
    assets.serviceWorkerCache = await this.collectServiceWorkerCache();

    return assets;
  }

  /**
   * Sync backup to server
   */
  async syncBackupToServer(backupData) {
    try {
      // Compress backup data
      const compressedData = await this.compressBackupData(backupData);
      
      // Encrypt sensitive data
      const encryptedData = await this.encryptSensitiveData(compressedData);
      
      // Upload to server
      const response = await fetch(`${this.backupEndpoint}/upload`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Backup-Type': 'FRONTEND',
          'X-Backup-Version': '1.0'
        },
        body: JSON.stringify(encryptedData)
      });

      if (!response.ok) {
        throw new Error(`Server backup failed: ${response.status} ${response.statusText}`);
      }

      const result = await response.json();
      console.log('Backup synced to server:', result.backupId);
      
      return result;

    } catch (error) {
      console.error('Failed to sync backup to server:', error);
      throw error;
    }
  }

  /**
   * Download backup from server
   */
  async downloadBackupFromServer(backupId) {
    try {
      const response = await fetch(`${this.backupEndpoint}/download/${backupId}`, {
        method: 'GET',
        headers: {
          'X-Backup-Type': 'FRONTEND'
        }
      });

      if (!response.ok) {
        throw new Error(`Server backup download failed: ${response.status} ${response.statusText}`);
      }

      const encryptedData = await response.json();
      
      // Decrypt data
      const compressedData = await this.decryptBackupData(encryptedData);
      
      // Decompress data
      const backupData = await this.decompressBackupData(compressedData);
      
      return backupData;

    } catch (error) {
      console.error('Failed to download backup from server:', error);
      throw error;
    }
  }

  /**
   * List available backups
   */
  async listAvailableBackups(filter = {}) {
    try {
      const backups = [];
      
      // Get local backups
      const localBackups = await this.getLocalBackups(filter);
      backups.push(...localBackups);
      
      // Get server backups if available
      try {
        const serverBackups = await this.getServerBackups(filter);
        backups.push(...serverBackups);
      } catch (error) {
        console.warn('Could not fetch server backups:', error.message);
      }
      
      // Sort by timestamp (newest first)
      backups.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      
      return backups;

    } catch (error) {
      console.error('Failed to list available backups:', error);
      return [];
    }
  }

  /**
   * Get backup status and statistics
   */
  async getBackupStatus() {
    try {
      const localBackups = await this.getLocalBackups();
      const totalSize = localBackups.reduce((sum, backup) => sum + (backup.size || 0), 0);
      
      const lastFullBackup = localBackups
        .filter(b => b.type === 'FULL')
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))[0];
      
      const lastUserDataBackup = localBackups
        .filter(b => b.type === 'USER_DATA')
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))[0];

      return {
        totalBackups: localBackups.length,
        totalSize,
        lastFullBackup: lastFullBackup?.timestamp,
        lastUserDataBackup: lastUserDataBackup?.timestamp,
        backupTypes: this.groupBackupsByType(localBackups),
        oldestBackup: localBackups[localBackups.length - 1]?.timestamp,
        newestBackup: localBackups[0]?.timestamp
      };

    } catch (error) {
      console.error('Failed to get backup status:', error);
      return null;
    }
  }

  /**
   * Clean up old backups
   */
  async cleanupOldBackups(retentionDays = 30) {
    try {
      const cutoffDate = new Date(Date.now() - (retentionDays * 24 * 60 * 60 * 1000));
      let deletedCount = 0;
      let freedSpace = 0;

      // Clean up user data backups
      const userDataBackups = await this.db.getAll('userDataBackups');
      for (const backup of userDataBackups) {
        if (new Date(backup.timestamp) < cutoffDate) {
          await this.db.delete('userDataBackups', backup.id);
          deletedCount++;
          freedSpace += backup.size || 0;
        }
      }

      // Clean up app state backups
      const appStateBackups = await this.db.getAll('appStateBackups');
      for (const backup of appStateBackups) {
        if (new Date(backup.timestamp) < cutoffDate) {
          await this.db.delete('appStateBackups', backup.id);
          deletedCount++;
          freedSpace += backup.size || 0;
        }
      }

      // Clean up config backups (keep more of these)
      const configBackups = await this.db.getAll('configBackups');
      const configCutoffDate = new Date(Date.now() - (90 * 24 * 60 * 60 * 1000)); // 90 days
      for (const backup of configBackups) {
        if (new Date(backup.timestamp) < configCutoffDate) {
          await this.db.delete('configBackups', backup.id);
          deletedCount++;
          freedSpace += backup.size || 0;
        }
      }

      // Clean up metadata
      const metadata = await this.db.getAll('backupMetadata');
      for (const meta of metadata) {
        if (new Date(meta.createdAt) < cutoffDate) {
          await this.db.delete('backupMetadata', meta.backupId);
        }
      }

      console.log(`Backup cleanup completed: ${deletedCount} backups deleted, ${freedSpace} bytes freed`);
      return { deletedCount, freedSpace };

    } catch (error) {
      console.error('Backup cleanup failed:', error);
      return { deletedCount: 0, freedSpace: 0 };
    }
  }

  // Helper methods

  generateBackupId(type) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const random = Math.random().toString(36).substr(2, 9);
    return `frontend_${type.toLowerCase()}_${timestamp}_${random}`;
  }

  calculateBackupSize(data) {
    return new Blob([JSON.stringify(data)]).size;
  }

  calculateDataSize(data) {
    return new Blob([JSON.stringify(data)]).size;
  }

  isUserDataKey(key) {
    const userDataPrefixes = ['user_', 'profile_', 'preferences_', 'settings_', 'auth_'];
    return userDataPrefixes.some(prefix => key.startsWith(prefix));
  }

  isCriticalData(key) {
    const criticalPrefixes = ['auth_', 'session_', 'user_profile'];
    return criticalPrefixes.some(prefix => key.startsWith(prefix));
  }

  isUserActive() {
    // Simple activity check - can be enhanced
    return document.hasFocus() && !document.hidden;
  }

  async compressBackupData(data) {
    // Implement compression logic (could use pako.js or similar)
    return data; // Placeholder
  }

  async decompressBackupData(data) {
    // Implement decompression logic
    return data; // Placeholder
  }

  async encryptSensitiveData(data) {
    // Implement encryption for sensitive data
    return data; // Placeholder
  }

  async decryptBackupData(data) {
    // Implement decryption logic
    return data; // Placeholder
  }

  // Additional helper methods would be implemented here...
}

// Create singleton instance
const frontendBackupService = new FrontendBackupService();

export default frontendBackupService;