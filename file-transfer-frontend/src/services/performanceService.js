import { openDB } from 'idb';
import debounce from 'lodash.debounce';
import throttle from 'lodash.throttle';

/**
 * Performance optimization service for frontend application
 * Provides caching, lazy loading, code splitting, resource optimization, and performance monitoring
 */
class PerformanceService {
  constructor() {
    this.cache = new Map();
    this.componentCache = new Map();
    this.resourceCache = new Map();
    this.performanceMetrics = new Map();
    this.observers = new Map();
    this.dbName = 'FileTransferCache';
    this.dbVersion = 1;
    
    this.initializePerformanceOptimizations();
  }

  /**
   * Initialize performance optimizations
   */
  async initializePerformanceOptimizations() {
    try {
      // Initialize IndexedDB for offline caching
      await this.initializeIndexedDB();
      
      // Set up intersection observer for lazy loading
      this.setupIntersectionObserver();
      
      // Initialize resource optimization
      this.initializeResourceOptimization();
      
      // Set up performance monitoring
      this.setupPerformanceMonitoring();
      
      // Initialize service worker for offline support
      this.initializeServiceWorker();
      
      // Set up memory management
      this.setupMemoryManagement();
      
      console.log('Performance service initialized successfully');
      
    } catch (error) {
      console.error('Failed to initialize performance service:', error);
    }
  }

  /**
   * Initialize IndexedDB for caching
   */
  async initializeIndexedDB() {
    try {
      this.db = await openDB(this.dbName, this.dbVersion, {
        upgrade(db) {
          // Create object stores
          if (!db.objectStoreNames.contains('apiCache')) {
            const apiStore = db.createObjectStore('apiCache', { keyPath: 'key' });
            apiStore.createIndex('timestamp', 'timestamp');
            apiStore.createIndex('expiry', 'expiry');
          }
          
          if (!db.objectStoreNames.contains('componentCache')) {
            const componentStore = db.createObjectStore('componentCache', { keyPath: 'key' });
            componentStore.createIndex('component', 'component');
            componentStore.createIndex('timestamp', 'timestamp');
          }
          
          if (!db.objectStoreNames.contains('resourceCache')) {
            const resourceStore = db.createObjectStore('resourceCache', { keyPath: 'url' });
            resourceStore.createIndex('type', 'type');
            resourceStore.createIndex('timestamp', 'timestamp');
          }
          
          if (!db.objectStoreNames.contains('userPreferences')) {
            db.createObjectStore('userPreferences', { keyPath: 'key' });
          }
        },
      });
      
      // Clean up expired entries on startup
      await this.cleanupExpiredEntries();
      
    } catch (error) {
      console.error('Failed to initialize IndexedDB:', error);
    }
  }

  /**
   * Set up intersection observer for lazy loading
   */
  setupIntersectionObserver() {
    this.intersectionObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const element = entry.target;
            
            // Lazy load images
            if (element.tagName === 'IMG' && element.dataset.src) {
              element.src = element.dataset.src;
              element.removeAttribute('data-src');
              this.intersectionObserver.unobserve(element);
            }
            
            // Lazy load components
            if (element.dataset.lazyComponent) {
              this.loadComponentLazily(element.dataset.lazyComponent);
              this.intersectionObserver.unobserve(element);
            }
          }
        });
      },
      {
        rootMargin: '50px 0px',
        threshold: 0.1,
      }
    );
  }

  /**
   * Initialize resource optimization
   */
  initializeResourceOptimization() {
    // Preload critical resources
    this.preloadCriticalResources();
    
    // Set up resource hints
    this.setupResourceHints();
    
    // Initialize image optimization
    this.initializeImageOptimization();
    
    // Set up bundle optimization
    this.setupBundleOptimization();
  }

  /**
   * Set up performance monitoring
   */
  setupPerformanceMonitoring() {
    // Monitor component render times
    this.monitorComponentPerformance();
    
    // Monitor API performance
    this.monitorApiPerformance();
    
    // Monitor memory usage
    this.monitorMemoryUsage();
    
    // Monitor user interactions
    this.monitorUserInteractions();
  }

  /**
   * Initialize service worker
   */
  async initializeServiceWorker() {
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.register('/sw.js');
        console.log('Service Worker registered:', registration);
        
        // Listen for updates
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed') {
              if (navigator.serviceWorker.controller) {
                // New content is available
                this.notifyUpdate();
              }
            }
          });
        });
        
      } catch (error) {
        console.error('Service Worker registration failed:', error);
      }
    }
  }

  /**
   * Set up memory management
   */
  setupMemoryManagement() {
    // Clean up caches periodically
    setInterval(() => {
      this.cleanupMemory();
    }, 300000); // Every 5 minutes

    // Listen for memory pressure
    if ('memory' in performance) {
      setInterval(() => {
        const memory = performance.memory;
        const usagePercentage = (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100;
        
        if (usagePercentage > 80) {
          console.warn('High memory usage detected:', usagePercentage, '%');
          this.performAggressiveCleanup();
        }
      }, 60000); // Every minute
    }
  }

  /**
   * Cache API response
   */
  async cacheApiResponse(url, data, options = {}) {
    const key = this.generateCacheKey(url, options);
    const expiryTime = Date.now() + (options.ttl || 300000); // Default 5 minutes
    
    const cacheEntry = {
      key,
      url,
      data,
      timestamp: Date.now(),
      expiry: expiryTime,
      options,
    };

    try {
      // Store in memory cache for immediate access
      this.cache.set(key, cacheEntry);
      
      // Store in IndexedDB for persistence
      if (this.db) {
        await this.db.put('apiCache', cacheEntry);
      }
      
      console.log('API response cached:', url);
      
    } catch (error) {
      console.error('Failed to cache API response:', error);
    }
  }

  /**
   * Get cached API response
   */
  async getCachedApiResponse(url, options = {}) {
    const key = this.generateCacheKey(url, options);
    
    try {
      // Check memory cache first
      let cacheEntry = this.cache.get(key);
      
      // If not in memory, check IndexedDB
      if (!cacheEntry && this.db) {
        cacheEntry = await this.db.get('apiCache', key);
        
        // If found in IndexedDB, add to memory cache
        if (cacheEntry) {
          this.cache.set(key, cacheEntry);
        }
      }
      
      // Check if entry is still valid
      if (cacheEntry && cacheEntry.expiry > Date.now()) {
        console.log('Cache hit for:', url);
        return cacheEntry.data;
      }
      
      // Remove expired entry
      if (cacheEntry) {
        this.cache.delete(key);
        if (this.db) {
          await this.db.delete('apiCache', key);
        }
      }
      
      return null;
      
    } catch (error) {
      console.error('Failed to get cached API response:', error);
      return null;
    }
  }

  /**
   * Cache component data
   */
  async cacheComponentData(component, data, props = {}) {
    const key = `${component}_${this.hashObject(props)}`;
    
    const cacheEntry = {
      key,
      component,
      data,
      props,
      timestamp: Date.now(),
    };

    try {
      this.componentCache.set(key, cacheEntry);
      
      if (this.db) {
        await this.db.put('componentCache', cacheEntry);
      }
      
    } catch (error) {
      console.error('Failed to cache component data:', error);
    }
  }

  /**
   * Get cached component data
   */
  async getCachedComponentData(component, props = {}) {
    const key = `${component}_${this.hashObject(props)}`;
    
    try {
      let cacheEntry = this.componentCache.get(key);
      
      if (!cacheEntry && this.db) {
        cacheEntry = await this.db.get('componentCache', key);
        
        if (cacheEntry) {
          this.componentCache.set(key, cacheEntry);
        }
      }
      
      return cacheEntry ? cacheEntry.data : null;
      
    } catch (error) {
      console.error('Failed to get cached component data:', error);
      return null;
    }
  }

  /**
   * Preload critical resources
   */
  preloadCriticalResources() {
    const criticalResources = [
      '/api/user/profile',
      '/api/tenants',
      '/api/services',
    ];

    criticalResources.forEach(url => {
      this.preloadResource(url);
    });
  }

  /**
   * Preload resource
   */
  async preloadResource(url, options = {}) {
    try {
      const cached = await this.getCachedApiResponse(url, options);
      if (!cached) {
        console.log('Preloading resource:', url);
        const response = await fetch(url, options);
        const data = await response.json();
        await this.cacheApiResponse(url, data, { ttl: 600000 }); // 10 minutes
      }
    } catch (error) {
      console.error('Failed to preload resource:', url, error);
    }
  }

  /**
   * Set up resource hints
   */
  setupResourceHints() {
    // DNS prefetch for external domains
    const externalDomains = [
      'https://api.sentry.io',
      'https://fonts.googleapis.com',
      'https://fonts.gstatic.com',
    ];

    externalDomains.forEach(domain => {
      const link = document.createElement('link');
      link.rel = 'dns-prefetch';
      link.href = domain;
      document.head.appendChild(link);
    });

    // Preconnect to important origins
    const importantOrigins = [
      window.location.origin,
    ];

    importantOrigins.forEach(origin => {
      const link = document.createElement('link');
      link.rel = 'preconnect';
      link.href = origin;
      document.head.appendChild(link);
    });
  }

  /**
   * Initialize image optimization
   */
  initializeImageOptimization() {
    // Set up lazy loading for images
    document.addEventListener('DOMContentLoaded', () => {
      const images = document.querySelectorAll('img[data-src]');
      images.forEach(img => {
        this.intersectionObserver.observe(img);
      });
    });

    // Implement responsive images
    this.implementResponsiveImages();
  }

  /**
   * Implement responsive images
   */
  implementResponsiveImages() {
    const originalCreateElement = document.createElement;
    document.createElement = function(tagName) {
      const element = originalCreateElement.call(this, tagName);
      
      if (tagName.toLowerCase() === 'img') {
        // Add loading="lazy" by default
        element.loading = 'lazy';
        
        // Add decode="async" for better performance
        element.decoding = 'async';
        
        // Implement responsive sizing
        element.addEventListener('load', function() {
          if (!this.hasAttribute('width') || !this.hasAttribute('height')) {
            this.style.aspectRatio = `${this.naturalWidth} / ${this.naturalHeight}`;
          }
        });
      }
      
      return element;
    };
  }

  /**
   * Set up bundle optimization
   */
  setupBundleOptimization() {
    // Dynamic import wrapper with caching
    this.dynamicImport = this.createCachedDynamicImport();
    
    // Component lazy loading
    this.setupComponentLazyLoading();
  }

  /**
   * Create cached dynamic import
   */
  createCachedDynamicImport() {
    const importCache = new Map();
    
    return async (path) => {
      if (importCache.has(path)) {
        return importCache.get(path);
      }
      
      const importPromise = import(path);
      importCache.set(path, importPromise);
      
      try {
        const module = await importPromise;
        return module;
      } catch (error) {
        importCache.delete(path);
        throw error;
      }
    };
  }

  /**
   * Set up component lazy loading
   */
  setupComponentLazyLoading() {
    // Create lazy component wrapper
    this.createLazyComponent = (importFunc) => {
      return React.lazy(() => {
        return importFunc().catch(error => {
          console.error('Failed to load component:', error);
          // Return a fallback component
          return { default: () => React.createElement('div', null, 'Component failed to load') };
        });
      });
    };
  }

  /**
   * Load component lazily
   */
  async loadComponentLazily(componentName) {
    try {
      const component = await this.dynamicImport(`../components/${componentName}`);
      console.log('Lazy loaded component:', componentName);
      return component;
    } catch (error) {
      console.error('Failed to lazy load component:', componentName, error);
      return null;
    }
  }

  /**
   * Monitor component performance
   */
  monitorComponentPerformance() {
    // React profiler integration would go here
    // For now, we'll track general component metrics
    
    this.componentMetrics = {
      renderCount: 0,
      totalRenderTime: 0,
      slowRenders: [],
    };

    // Track slow renders
    const originalSetTimeout = window.setTimeout;
    window.setTimeout = (callback, delay, ...args) => {
      const start = performance.now();
      return originalSetTimeout(() => {
        const end = performance.now();
        const duration = end - start;
        
        if (duration > 16) { // Slower than 60fps
          this.componentMetrics.slowRenders.push({
            duration,
            timestamp: Date.now(),
          });
        }
        
        callback.apply(null, args);
      }, delay);
    };
  }

  /**
   * Monitor API performance
   */
  monitorApiPerformance() {
    const originalFetch = window.fetch;
    
    window.fetch = async (...args) => {
      const start = performance.now();
      const url = args[0];
      
      try {
        const response = await originalFetch.apply(window, args);
        const end = performance.now();
        const duration = end - start;
        
        this.recordApiMetric(url, duration, response.status, true);
        
        return response;
      } catch (error) {
        const end = performance.now();
        const duration = end - start;
        
        this.recordApiMetric(url, duration, 0, false);
        
        throw error;
      }
    };
  }

  /**
   * Record API metric
   */
  recordApiMetric(url, duration, status, success) {
    const metric = {
      url: typeof url === 'string' ? url : url.url,
      duration,
      status,
      success,
      timestamp: Date.now(),
    };

    this.performanceMetrics.set(`api_${Date.now()}`, metric);
    
    // Keep only last 100 metrics
    if (this.performanceMetrics.size > 100) {
      const firstKey = this.performanceMetrics.keys().next().value;
      this.performanceMetrics.delete(firstKey);
    }
    
    console.log('API Performance:', metric);
  }

  /**
   * Monitor memory usage
   */
  monitorMemoryUsage() {
    if ('memory' in performance) {
      setInterval(() => {
        const memory = performance.memory;
        const usage = {
          used: memory.usedJSHeapSize,
          total: memory.totalJSHeapSize,
          limit: memory.jsHeapSizeLimit,
          percentage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100,
          timestamp: Date.now(),
        };

        this.performanceMetrics.set(`memory_${Date.now()}`, usage);
        
        if (usage.percentage > 90) {
          console.warn('High memory usage:', usage);
          this.performAggressiveCleanup();
        }
      }, 30000); // Every 30 seconds
    }
  }

  /**
   * Monitor user interactions
   */
  monitorUserInteractions() {
    const interactionTypes = ['click', 'scroll', 'keydown', 'touchstart'];
    
    interactionTypes.forEach(type => {
      document.addEventListener(type, throttle((event) => {
        const interaction = {
          type,
          timestamp: Date.now(),
          target: event.target.tagName,
          page: window.location.pathname,
        };

        this.performanceMetrics.set(`interaction_${Date.now()}`, interaction);
      }, 1000), { passive: true });
    });
  }

  /**
   * Debounced function creator
   */
  createDebouncedFunction(func, delay = 300) {
    return debounce(func, delay);
  }

  /**
   * Throttled function creator
   */
  createThrottledFunction(func, delay = 100) {
    return throttle(func, delay);
  }

  /**
   * Optimize images
   */
  optimizeImage(imgElement) {
    // Add intersection observer
    this.intersectionObserver.observe(imgElement);
    
    // Set up responsive loading
    imgElement.addEventListener('load', function() {
      this.style.opacity = '1';
      this.style.transition = 'opacity 0.3s ease';
    });

    imgElement.addEventListener('error', function() {
      this.style.display = 'none';
      console.error('Failed to load image:', this.src);
    });
  }

  /**
   * Clean up expired entries
   */
  async cleanupExpiredEntries() {
    if (!this.db) return;

    try {
      const tx = this.db.transaction(['apiCache'], 'readwrite');
      const store = tx.objectStore('apiCache');
      const now = Date.now();
      
      const entries = await store.getAll();
      
      for (const entry of entries) {
        if (entry.expiry && entry.expiry < now) {
          await store.delete(entry.key);
          this.cache.delete(entry.key);
        }
      }
      
      await tx.complete;
      console.log('Cleaned up expired cache entries');
      
    } catch (error) {
      console.error('Failed to cleanup expired entries:', error);
    }
  }

  /**
   * Clean up memory
   */
  cleanupMemory() {
    // Clear old performance metrics
    const cutoff = Date.now() - 3600000; // 1 hour ago
    
    for (const [key, metric] of this.performanceMetrics.entries()) {
      if (metric.timestamp < cutoff) {
        this.performanceMetrics.delete(key);
      }
    }

    // Clear old cache entries
    for (const [key, entry] of this.cache.entries()) {
      if (entry.timestamp < cutoff) {
        this.cache.delete(key);
      }
    }

    // Clear component cache
    for (const [key, entry] of this.componentCache.entries()) {
      if (entry.timestamp < cutoff) {
        this.componentCache.delete(key);
      }
    }

    console.log('Memory cleanup completed');
  }

  /**
   * Perform aggressive cleanup
   */
  performAggressiveCleanup() {
    console.log('Performing aggressive cleanup...');
    
    // Clear all caches
    this.cache.clear();
    this.componentCache.clear();
    this.resourceCache.clear();
    
    // Clear most performance metrics
    const recentCutoff = Date.now() - 300000; // Last 5 minutes
    
    for (const [key, metric] of this.performanceMetrics.entries()) {
      if (metric.timestamp < recentCutoff) {
        this.performanceMetrics.delete(key);
      }
    }

    // Clear IndexedDB caches
    if (this.db) {
      this.db.clear('apiCache').catch(console.error);
      this.db.clear('componentCache').catch(console.error);
      this.db.clear('resourceCache').catch(console.error);
    }

    // Force garbage collection if available
    if (window.gc) {
      window.gc();
    }

    console.log('Aggressive cleanup completed');
  }

  /**
   * Generate cache key
   */
  generateCacheKey(url, options = {}) {
    const optionsString = JSON.stringify(options);
    return `${url}_${this.hashString(optionsString)}`;
  }

  /**
   * Hash string
   */
  hashString(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return hash.toString(36);
  }

  /**
   * Hash object
   */
  hashObject(obj) {
    return this.hashString(JSON.stringify(obj));
  }

  /**
   * Notify about update
   */
  notifyUpdate() {
    // This would trigger a notification to the user about available updates
    console.log('New version available! Please refresh the page.');
  }

  /**
   * Get performance summary
   */
  getPerformanceSummary() {
    const apiMetrics = Array.from(this.performanceMetrics.values())
      .filter(metric => metric.url);
    
    const memoryMetrics = Array.from(this.performanceMetrics.values())
      .filter(metric => metric.used);

    return {
      cacheSize: this.cache.size,
      componentCacheSize: this.componentCache.size,
      performanceMetricsCount: this.performanceMetrics.size,
      apiCallsCount: apiMetrics.length,
      averageApiResponseTime: apiMetrics.length > 0 
        ? apiMetrics.reduce((sum, m) => sum + m.duration, 0) / apiMetrics.length 
        : 0,
      memoryUsage: memoryMetrics.length > 0 
        ? memoryMetrics[memoryMetrics.length - 1] 
        : null,
      componentMetrics: this.componentMetrics,
    };
  }
}

// Create singleton instance
const performanceService = new PerformanceService();

export default performanceService;