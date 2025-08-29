/**
 * API Version Service for Frontend Application
 * Handles API versioning, compatibility, and version negotiation for client-side requests
 */

class ApiVersionService {
  constructor() {
    this.currentVersion = process.env.REACT_APP_API_VERSION || '2.0';
    this.supportedVersions = ['1.0', '1.1', '1.2', '2.0', '2.1'];
    this.versionStrategy = process.env.REACT_APP_VERSION_STRATEGY || 'URL_PATH';
    this.apiBaseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
    this.batchApiBaseUrl = process.env.REACT_APP_BATCH_API_URL || 'http://localhost:8082';
    
    // Version compatibility matrix
    this.compatibilityMatrix = {
      '1.0': ['1.1', '1.2'],
      '1.1': ['1.0', '1.2'],
      '1.2': ['1.1', '2.0'],
      '2.0': ['1.2', '2.1'],
      '2.1': ['2.0']
    };
    
    // Version-specific configurations
    this.versionConfigs = {
      '1.0': {
        deprecated: true,
        sunsetDate: '2024-06-01',
        features: ['basic-crud'],
        limitations: ['no-pagination', 'basic-validation']
      },
      '1.1': {
        deprecated: true,
        sunsetDate: '2024-06-01',
        features: ['basic-crud', 'bulk-operations'],
        limitations: ['no-pagination', 'basic-validation']
      },
      '1.2': {
        deprecated: true,
        sunsetDate: '2024-03-01',
        features: ['basic-crud', 'bulk-operations', 'statistics'],
        limitations: ['basic-validation']
      },
      '2.0': {
        deprecated: false,
        features: ['enhanced-crud', 'pagination', 'validation', 'configuration', 'health-checks'],
        limitations: []
      },
      '2.1': {
        deprecated: false,
        status: 'beta',
        features: ['enhanced-crud', 'pagination', 'validation', 'configuration', 'health-checks', 'export-import', 'audit-trails'],
        limitations: []
      }
    };
    
    this.initialize();
  }

  /**
   * Initialize the API version service
   */
  async initialize() {
    try {
      // Check API version compatibility
      await this.checkApiCompatibility();
      
      // Set up version-specific configurations
      this.setupVersionConfigurations();
      
      // Initialize request interceptors
      this.setupRequestInterceptors();
      
      console.log('API Version Service initialized with version:', this.currentVersion);
      
    } catch (error) {
      console.error('Failed to initialize API Version Service:', error);
      
      // Fallback to a compatible version
      await this.fallbackToCompatibleVersion();
    }
  }

  /**
   * Check API compatibility with the server
   */
  async checkApiCompatibility() {
    try {
      const response = await fetch(`${this.apiBaseUrl}/api/versions`, {
        method: 'GET',
        headers: this.getVersionHeaders(this.currentVersion)
      });

      if (response.ok) {
        const versionInfo = await response.json();
        this.serverVersions = versionInfo.supportedVersions;
        
        // Check if current version is supported
        const isSupported = this.serverVersions.some(v => v.version === this.currentVersion);
        
        if (!isSupported) {
          console.warn(`Version ${this.currentVersion} not supported by server`);
          await this.negotiateVersion();
        }
        
        return true;
      } else {
        throw new Error(`API version check failed: ${response.status}`);
      }
      
    } catch (error) {
      console.error('API compatibility check failed:', error);
      throw error;
    }
  }

  /**
   * Negotiate the best compatible version with the server
   */
  async negotiateVersion() {
    try {
      // Try to find a compatible version
      const compatibleVersion = this.findBestCompatibleVersion(this.currentVersion);
      
      if (compatibleVersion) {
        console.log(`Negotiating version from ${this.currentVersion} to ${compatibleVersion}`);
        this.currentVersion = compatibleVersion;
        
        // Update configuration for new version
        this.setupVersionConfigurations();
        
        return compatibleVersion;
      } else {
        throw new Error('No compatible API version found');
      }
      
    } catch (error) {
      console.error('Version negotiation failed:', error);
      throw error;
    }
  }

  /**
   * Find the best compatible version
   */
  findBestCompatibleVersion(requestedVersion) {
    // Check if any supported server versions are compatible with requested version
    if (this.serverVersions) {
      for (const serverVersion of this.serverVersions) {
        if (this.areVersionsCompatible(requestedVersion, serverVersion.version)) {
          return serverVersion.version;
        }
      }
    }
    
    // Fallback to latest supported version
    const supportedVersions = this.supportedVersions.filter(v => 
      this.serverVersions?.some(sv => sv.version === v)
    );
    
    return supportedVersions[supportedVersions.length - 1];
  }

  /**
   * Check if two versions are compatible
   */
  areVersionsCompatible(version1, version2) {
    if (version1 === version2) {
      return true;
    }
    
    const compatible1 = this.compatibilityMatrix[version1] || [];
    const compatible2 = this.compatibilityMatrix[version2] || [];
    
    return compatible1.includes(version2) || compatible2.includes(version1);
  }

  /**
   * Set up version-specific configurations
   */
  setupVersionConfigurations() {
    const config = this.versionConfigs[this.currentVersion];
    
    if (config) {
      // Show deprecation warnings
      if (config.deprecated) {
        this.showDeprecationWarning(config);
      }
      
      // Configure feature flags
      this.configureFeatures(config.features);
      
      // Apply limitations
      this.applyLimitations(config.limitations);
    }
  }

  /**
   * Show deprecation warning to users
   */
  showDeprecationWarning(config) {
    const message = `API version ${this.currentVersion} is deprecated and will be sunset on ${config.sunsetDate}. Please upgrade to version 2.0 or later.`;
    
    // Show user-friendly notification
    if (window.showNotification) {
      window.showNotification({
        type: 'warning',
        title: 'API Version Deprecated',
        message: message,
        persistent: true,
        actions: [
          {
            label: 'Learn More',
            action: () => window.open('/docs/api-migration', '_blank')
          }
        ]
      });
    } else {
      console.warn(message);
    }
  }

  /**
   * Configure features based on version
   */
  configureFeatures(features) {
    window.API_FEATURES = window.API_FEATURES || {};
    
    // Reset features
    Object.keys(window.API_FEATURES).forEach(key => {
      window.API_FEATURES[key] = false;
    });
    
    // Enable version-specific features
    features.forEach(feature => {
      window.API_FEATURES[feature] = true;
    });
    
    console.log('Configured API features:', window.API_FEATURES);
  }

  /**
   * Apply version limitations
   */
  applyLimitations(limitations) {
    window.API_LIMITATIONS = limitations || [];
    console.log('Applied API limitations:', window.API_LIMITATIONS);
  }

  /**
   * Set up request interceptors for automatic version handling
   */
  setupRequestInterceptors() {
    // Intercept fetch requests
    const originalFetch = window.fetch;
    
    window.fetch = async (url, options = {}) => {
      // Only intercept API requests
      if (typeof url === 'string' && (url.includes('/api/') || url.startsWith(this.apiBaseUrl) || url.startsWith(this.batchApiBaseUrl))) {
        options.headers = {
          ...options.headers,
          ...this.getVersionHeaders(this.currentVersion)
        };
        
        // Add version to URL if using URL_PATH strategy
        if (this.versionStrategy === 'URL_PATH') {
          url = this.addVersionToUrl(url);
        }
      }
      
      try {
        const response = await originalFetch(url, options);
        
        // Handle version-related response headers
        this.handleVersionHeaders(response);
        
        return response;
      } catch (error) {
        console.error('API request failed:', error);
        throw error;
      }
    };
    
    // Intercept axios requests if available
    if (window.axios) {
      window.axios.interceptors.request.use(config => {
        if (config.url && (config.url.includes('/api/') || config.url.startsWith(this.apiBaseUrl) || config.url.startsWith(this.batchApiBaseUrl))) {
          config.headers = {
            ...config.headers,
            ...this.getVersionHeaders(this.currentVersion)
          };
          
          if (this.versionStrategy === 'URL_PATH') {
            config.url = this.addVersionToUrl(config.url);
          }
        }
        
        return config;
      });
      
      window.axios.interceptors.response.use(
        response => {
          this.handleVersionHeaders(response);
          return response;
        },
        error => {
          if (error.response) {
            this.handleVersionHeaders(error.response);
          }
          return Promise.reject(error);
        }
      );
    }
  }

  /**
   * Get version headers based on strategy
   */
  getVersionHeaders(version) {
    const headers = {};
    
    switch (this.versionStrategy) {
      case 'HEADER':
        headers['X-API-Version'] = version;
        break;
      case 'ACCEPT_HEADER':
        headers['Accept-Version'] = version;
        break;
      case 'MEDIA_TYPE':
        headers['Accept'] = `application/vnd.filetransfer.v${version}+json`;
        break;
      case 'HYBRID':
        headers['X-API-Version'] = version;
        headers['Accept-Version'] = version;
        break;
      default:
        // URL_PATH strategy doesn't need headers
        break;
    }
    
    return headers;
  }

  /**
   * Add version to URL for URL_PATH strategy
   */
  addVersionToUrl(url) {
    if (this.versionStrategy !== 'URL_PATH') {
      return url;
    }
    
    // Check if URL already has version
    if (url.match(/\/api\/v[\d.]+\//)) {
      return url;
    }
    
    // Add version to URL
    if (url.includes('/api/batch/')) {
      return url.replace('/api/batch/', `/api/batch/v${this.currentVersion}/`);
    } else if (url.includes('/api/')) {
      return url.replace('/api/', `/api/v${this.currentVersion}/`);
    }
    
    return url;
  }

  /**
   * Handle version-related response headers
   */
  handleVersionHeaders(response) {
    const headers = response.headers;
    
    // Check for deprecation warnings
    if (headers.get && headers.get('Deprecation') === 'true') {
      const sunsetDate = headers.get('Sunset');
      const link = headers.get('Link');
      
      this.handleDeprecationWarning(sunsetDate, link);
    }
    
    // Check for version used
    const versionUsed = headers.get && headers.get('X-API-Version-Used');
    if (versionUsed && versionUsed !== this.currentVersion) {
      console.log(`Server used version ${versionUsed} instead of requested ${this.currentVersion}`);
    }
  }

  /**
   * Handle deprecation warnings from server
   */
  handleDeprecationWarning(sunsetDate, link) {
    const message = `The API version you're using is deprecated${sunsetDate ? ` and will be sunset on ${sunsetDate}` : ''}.`;
    
    if (window.showNotification) {
      window.showNotification({
        type: 'warning',
        title: 'API Deprecated',
        message: message,
        actions: link ? [{
          label: 'Upgrade Guide',
          action: () => window.open(link, '_blank')
        }] : []
      });
    }
  }

  /**
   * Get migration plan for upgrading to a newer version
   */
  async getMigrationPlan(fromVersion, toVersion) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/api/versions/migration-plan?fromVersion=${fromVersion}&toVersion=${toVersion}`, {
        headers: this.getVersionHeaders(this.currentVersion)
      });
      
      if (response.ok) {
        const migrationPlan = await response.json();
        return migrationPlan;
      } else {
        throw new Error(`Failed to get migration plan: ${response.status}`);
      }
      
    } catch (error) {
      console.error('Failed to get migration plan:', error);
      return null;
    }
  }

  /**
   * Get breaking changes between versions
   */
  async getBreakingChanges(fromVersion, toVersion) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/api/versions/breaking-changes?fromVersion=${fromVersion}&toVersion=${toVersion}`, {
        headers: this.getVersionHeaders(this.currentVersion)
      });
      
      if (response.ok) {
        const breakingChanges = await response.json();
        return breakingChanges;
      } else {
        throw new Error(`Failed to get breaking changes: ${response.status}`);
      }
      
    } catch (error) {
      console.error('Failed to get breaking changes:', error);
      return null;
    }
  }

  /**
   * Fallback to a compatible version when initialization fails
   */
  async fallbackToCompatibleVersion() {
    console.log('Falling back to compatible version...');
    
    // Try versions in order of preference
    const fallbackVersions = ['2.0', '1.2', '1.1', '1.0'];
    
    for (const version of fallbackVersions) {
      try {
        this.currentVersion = version;
        await this.checkApiCompatibility();
        console.log(`Successfully fell back to version ${version}`);
        return;
      } catch (error) {
        console.warn(`Version ${version} also failed:`, error.message);
      }
    }
    
    // If all versions fail, use the default and continue
    this.currentVersion = '2.0';
    console.warn('All version checks failed, continuing with default version 2.0');
  }

  /**
   * Check if a feature is available in current version
   */
  hasFeature(feature) {
    const config = this.versionConfigs[this.currentVersion];
    return config ? config.features.includes(feature) : false;
  }

  /**
   * Check if a limitation exists in current version
   */
  hasLimitation(limitation) {
    const config = this.versionConfigs[this.currentVersion];
    return config ? config.limitations.includes(limitation) : false;
  }

  /**
   * Get current version information
   */
  getCurrentVersion() {
    return {
      version: this.currentVersion,
      config: this.versionConfigs[this.currentVersion],
      strategy: this.versionStrategy,
      supportedVersions: this.supportedVersions
    };
  }

  /**
   * Manually set API version
   */
  async setVersion(version) {
    if (!this.supportedVersions.includes(version)) {
      throw new Error(`Version ${version} is not supported`);
    }
    
    const oldVersion = this.currentVersion;
    this.currentVersion = version;
    
    try {
      await this.checkApiCompatibility();
      this.setupVersionConfigurations();
      
      console.log(`Successfully switched from version ${oldVersion} to ${version}`);
      
      // Notify components of version change
      if (window.dispatchEvent) {
        window.dispatchEvent(new CustomEvent('apiVersionChanged', {
          detail: { oldVersion, newVersion: version }
        }));
      }
      
    } catch (error) {
      // Revert on failure
      this.currentVersion = oldVersion;
      throw error;
    }
  }

  /**
   * Get version-aware API endpoint
   */
  getEndpoint(path, options = {}) {
    const { service = 'web', version = this.currentVersion } = options;
    
    let baseUrl = service === 'batch' ? this.batchApiBaseUrl : this.apiBaseUrl;
    
    if (this.versionStrategy === 'URL_PATH') {
      if (service === 'batch') {
        return `${baseUrl}/api/batch/v${version}${path}`;
      } else {
        return `${baseUrl}/api/v${version}${path}`;
      }
    } else {
      if (service === 'batch') {
        return `${baseUrl}/api/batch${path}`;
      } else {
        return `${baseUrl}/api${path}`;
      }
    }
  }

  /**
   * Create a version-aware API client
   */
  createApiClient(options = {}) {
    const { service = 'web', version = this.currentVersion } = options;
    
    return {
      get: (path, config = {}) => this.request('GET', path, null, { ...config, service, version }),
      post: (path, data, config = {}) => this.request('POST', path, data, { ...config, service, version }),
      put: (path, data, config = {}) => this.request('PUT', path, data, { ...config, service, version }),
      patch: (path, data, config = {}) => this.request('PATCH', path, data, { ...config, service, version }),
      delete: (path, config = {}) => this.request('DELETE', path, null, { ...config, service, version })
    };
  }

  /**
   * Make a version-aware API request
   */
  async request(method, path, data, options = {}) {
    const { service = 'web', version = this.currentVersion, ...config } = options;
    const url = this.getEndpoint(path, { service, version });
    
    const requestOptions = {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...this.getVersionHeaders(version),
        ...config.headers
      },
      ...config
    };
    
    if (data) {
      requestOptions.body = JSON.stringify(data);
    }
    
    try {
      const response = await fetch(url, requestOptions);
      this.handleVersionHeaders(response);
      
      if (!response.ok) {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error(`API request failed: ${method} ${url}`, error);
      throw error;
    }
  }
}

// Create singleton instance
const apiVersionService = new ApiVersionService();

export default apiVersionService;