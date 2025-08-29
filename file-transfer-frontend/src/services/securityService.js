import DOMPurify from 'dompurify';
import CryptoJS from 'crypto-js';
import Cookies from 'js-cookie';
import { v4 as uuidv4 } from 'uuid';

/**
 * Comprehensive security service for frontend application
 * Provides input validation, XSS protection, secure storage, rate limiting, and security headers
 */
class SecurityService {
  constructor() {
    this.initializeSecurity();
    this.rateLimiters = new Map();
    this.securityEventLog = [];
    this.encryptionKey = this.generateEncryptionKey();
    this.csrfToken = this.generateCSRFToken();
  }

  /**
   * Initialize security measures
   */
  initializeSecurity() {
    try {
      // Set up Content Security Policy
      this.setupCSP();
      
      // Initialize secure headers
      this.setupSecureHeaders();
      
      // Set up XSS protection
      this.setupXSSProtection();
      
      // Initialize secure storage
      this.initializeSecureStorage();
      
      // Set up input validation
      this.setupInputValidation();
      
      // Initialize rate limiting
      this.initializeRateLimiting();
      
      // Set up security event monitoring
      this.setupSecurityMonitoring();
      
      console.log('Security service initialized successfully');
      
    } catch (error) {
      console.error('Failed to initialize security service:', error);
      this.logSecurityEvent('SECURITY_INIT_FAILED', { error: error.message });
    }
  }

  /**
   * Set up Content Security Policy
   */
  setupCSP() {
    const cspDirectives = {
      'default-src': ["'self'"],
      'script-src': ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
      'style-src': ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
      'font-src': ["'self'", "https://fonts.gstatic.com"],
      'img-src': ["'self'", "data:", "https:"],
      'connect-src': ["'self'", "https://api.sentry.io"],
      'media-src': ["'self'"],
      'object-src': ["'none'"],
      'frame-src': ["'none'"],
      'base-uri': ["'self'"],
      'form-action': ["'self'"],
      'frame-ancestors': ["'none'"],
      'upgrade-insecure-requests': []
    };

    const cspString = Object.entries(cspDirectives)
      .map(([directive, sources]) => `${directive} ${sources.join(' ')}`)
      .join('; ');

    // Set CSP meta tag
    const cspMeta = document.createElement('meta');
    cspMeta.httpEquiv = 'Content-Security-Policy';
    cspMeta.content = cspString;
    document.head.appendChild(cspMeta);
  }

  /**
   * Set up secure headers
   */
  setupSecureHeaders() {
    // X-Content-Type-Options
    const noSniffMeta = document.createElement('meta');
    noSniffMeta.httpEquiv = 'X-Content-Type-Options';
    noSniffMeta.content = 'nosniff';
    document.head.appendChild(noSniffMeta);

    // X-Frame-Options
    const frameOptionsMeta = document.createElement('meta');
    frameOptionsMeta.httpEquiv = 'X-Frame-Options';
    frameOptionsMeta.content = 'DENY';
    document.head.appendChild(frameOptionsMeta);

    // X-XSS-Protection
    const xssProtectionMeta = document.createElement('meta');
    xssProtectionMeta.httpEquiv = 'X-XSS-Protection';
    xssProtectionMeta.content = '1; mode=block';
    document.head.appendChild(xssProtectionMeta);

    // Referrer Policy
    const referrerPolicyMeta = document.createElement('meta');
    referrerPolicyMeta.name = 'referrer';
    referrerPolicyMeta.content = 'strict-origin-when-cross-origin';
    document.head.appendChild(referrerPolicyMeta);
  }

  /**
   * Set up XSS protection
   */
  setupXSSProtection() {
    // Configure DOMPurify
    DOMPurify.setConfig({
      ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li'],
      ALLOWED_ATTR: ['href', 'target'],
      ALLOW_DATA_ATTR: false,
      FORBID_SCRIPT: true,
      FORBID_TAGS: ['script', 'object', 'embed', 'link', 'style', 'iframe'],
      FORBID_ATTR: ['style', 'onload', 'onerror', 'onclick'],
    });

    // Override innerHTML to automatically sanitize
    const originalInnerHTML = Element.prototype.__lookupSetter__('innerHTML');
    if (originalInnerHTML) {
      Object.defineProperty(Element.prototype, 'innerHTML', {
        set: function(value) {
          if (typeof value === 'string') {
            originalInnerHTML.call(this, DOMPurify.sanitize(value));
          } else {
            originalInnerHTML.call(this, value);
          }
        },
        get: Element.prototype.__lookupGetter__('innerHTML')
      });
    }
  }

  /**
   * Initialize secure storage
   */
  initializeSecureStorage() {
    this.secureStorage = {
      setItem: (key, value, options = {}) => {
        try {
          const encryptedValue = this.encryptData(JSON.stringify(value));
          const storageItem = {
            data: encryptedValue,
            timestamp: Date.now(),
            expiresAt: options.expiresIn ? Date.now() + options.expiresIn : null,
            checksum: this.generateChecksum(encryptedValue)
          };

          if (options.secure) {
            // Use sessionStorage for sensitive data
            sessionStorage.setItem(key, JSON.stringify(storageItem));
          } else {
            localStorage.setItem(key, JSON.stringify(storageItem));
          }

          this.logSecurityEvent('SECURE_STORAGE_SET', { key, secure: options.secure });
        } catch (error) {
          this.logSecurityEvent('SECURE_STORAGE_ERROR', { key, error: error.message });
          throw error;
        }
      },

      getItem: (key, options = {}) => {
        try {
          const storage = options.secure ? sessionStorage : localStorage;
          const storedItem = storage.getItem(key);
          
          if (!storedItem) return null;

          const parsedItem = JSON.parse(storedItem);
          
          // Check expiration
          if (parsedItem.expiresAt && Date.now() > parsedItem.expiresAt) {
            storage.removeItem(key);
            this.logSecurityEvent('SECURE_STORAGE_EXPIRED', { key });
            return null;
          }

          // Verify checksum
          if (this.generateChecksum(parsedItem.data) !== parsedItem.checksum) {
            storage.removeItem(key);
            this.logSecurityEvent('SECURE_STORAGE_TAMPERED', { key });
            throw new Error('Data integrity check failed');
          }

          const decryptedData = this.decryptData(parsedItem.data);
          return JSON.parse(decryptedData);
          
        } catch (error) {
          this.logSecurityEvent('SECURE_STORAGE_READ_ERROR', { key, error: error.message });
          return null;
        }
      },

      removeItem: (key, options = {}) => {
        const storage = options.secure ? sessionStorage : localStorage;
        storage.removeItem(key);
        this.logSecurityEvent('SECURE_STORAGE_REMOVED', { key });
      }
    };
  }

  /**
   * Set up input validation
   */
  setupInputValidation() {
    this.validators = {
      email: (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
      },

      password: (password) => {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special char
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        return passwordRegex.test(password);
      },

      url: (url) => {
        try {
          const urlObj = new URL(url);
          return ['http:', 'https:'].includes(urlObj.protocol);
        } catch {
          return false;
        }
      },

      filename: (filename) => {
        const filenameRegex = /^[a-zA-Z0-9._-]+$/;
        return filenameRegex.test(filename) && !filename.includes('..');
      },

      tenantId: (tenantId) => {
        const tenantIdRegex = /^[a-zA-Z0-9_-]{1,50}$/;
        return tenantIdRegex.test(tenantId);
      },

      alphanumeric: (value) => {
        const alphanumericRegex = /^[a-zA-Z0-9]+$/;
        return alphanumericRegex.test(value);
      }
    };
  }

  /**
   * Initialize rate limiting
   */
  initializeRateLimiting() {
    this.rateLimitConfig = {
      api: { requests: 100, window: 60000 }, // 100 requests per minute
      login: { requests: 5, window: 300000 }, // 5 login attempts per 5 minutes
      fileUpload: { requests: 10, window: 60000 }, // 10 file uploads per minute
      search: { requests: 30, window: 60000 }, // 30 searches per minute
    };
  }

  /**
   * Set up security event monitoring
   */
  setupSecurityMonitoring() {
    // Monitor for console access attempts
    const originalConsole = { ...console };
    Object.keys(console).forEach(method => {
      console[method] = (...args) => {
        if (method === 'clear') {
          this.logSecurityEvent('CONSOLE_CLEAR_ATTEMPT', { timestamp: Date.now() });
        }
        return originalConsole[method].apply(console, args);
      };
    });

    // Monitor for developer tools
    this.detectDevTools();

    // Monitor for suspicious activities
    this.monitorSuspiciousActivities();
  }

  /**
   * Sanitize HTML input
   */
  sanitizeHtml(html) {
    if (typeof html !== 'string') return html;
    
    const sanitized = DOMPurify.sanitize(html);
    
    if (sanitized !== html) {
      this.logSecurityEvent('XSS_ATTEMPT_BLOCKED', { 
        original: html.substring(0, 100),
        sanitized: sanitized.substring(0, 100)
      });
    }
    
    return sanitized;
  }

  /**
   * Validate input
   */
  validateInput(value, type, options = {}) {
    if (!value && !options.required) return true;
    if (!value && options.required) return false;

    // Basic sanitization
    const sanitizedValue = this.sanitizeHtml(value);
    
    // Length validation
    if (options.maxLength && sanitizedValue.length > options.maxLength) {
      this.logSecurityEvent('INPUT_LENGTH_VIOLATION', { 
        type, 
        length: sanitizedValue.length, 
        maxLength: options.maxLength 
      });
      return false;
    }

    // Type-specific validation
    if (this.validators[type]) {
      const isValid = this.validators[type](sanitizedValue);
      if (!isValid) {
        this.logSecurityEvent('INPUT_VALIDATION_FAILED', { type, value: sanitizedValue.substring(0, 50) });
      }
      return isValid;
    }

    return true;
  }

  /**
   * Check rate limit
   */
  checkRateLimit(action, identifier = 'global') {
    const key = `${action}_${identifier}`;
    const config = this.rateLimitConfig[action];
    
    if (!config) return true; // No rate limit configured

    const now = Date.now();
    const rateLimiter = this.rateLimiters.get(key) || { requests: [], windowStart: now };

    // Remove old requests outside the window
    rateLimiter.requests = rateLimiter.requests.filter(
      timestamp => now - timestamp < config.window
    );

    // Check if limit exceeded
    if (rateLimiter.requests.length >= config.requests) {
      this.logSecurityEvent('RATE_LIMIT_EXCEEDED', { 
        action, 
        identifier, 
        requests: rateLimiter.requests.length,
        limit: config.requests 
      });
      return false;
    }

    // Add current request
    rateLimiter.requests.push(now);
    this.rateLimiters.set(key, rateLimiter);

    return true;
  }

  /**
   * Encrypt data
   */
  encryptData(data) {
    try {
      return CryptoJS.AES.encrypt(data, this.encryptionKey).toString();
    } catch (error) {
      this.logSecurityEvent('ENCRYPTION_ERROR', { error: error.message });
      throw error;
    }
  }

  /**
   * Decrypt data
   */
  decryptData(encryptedData) {
    try {
      const bytes = CryptoJS.AES.decrypt(encryptedData, this.encryptionKey);
      return bytes.toString(CryptoJS.enc.Utf8);
    } catch (error) {
      this.logSecurityEvent('DECRYPTION_ERROR', { error: error.message });
      throw error;
    }
  }

  /**
   * Generate secure token
   */
  generateSecureToken(length = 32) {
    const array = new Uint8Array(length);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Generate CSRF token
   */
  generateCSRFToken() {
    const token = this.generateSecureToken();
    Cookies.set('csrf-token', token, { 
      secure: location.protocol === 'https:', 
      sameSite: 'strict',
      httpOnly: false 
    });
    return token;
  }

  /**
   * Get CSRF token
   */
  getCSRFToken() {
    return this.csrfToken || Cookies.get('csrf-token');
  }

  /**
   * Secure API request
   */
  async secureApiRequest(url, options = {}) {
    // Check rate limit
    if (!this.checkRateLimit('api', url)) {
      throw new Error('Rate limit exceeded');
    }

    // Add security headers
    const secureOptions = {
      ...options,
      headers: {
        ...options.headers,
        'X-CSRF-Token': this.getCSRFToken(),
        'X-Requested-With': 'XMLHttpRequest',
        'X-Request-ID': uuidv4(),
      },
    };

    // Validate and sanitize request body
    if (secureOptions.body && typeof secureOptions.body === 'object') {
      secureOptions.body = JSON.stringify(this.sanitizeRequestData(secureOptions.body));
      secureOptions.headers['Content-Type'] = 'application/json';
    }

    try {
      const response = await fetch(url, secureOptions);
      
      // Log successful API call
      this.logSecurityEvent('API_REQUEST_SUCCESS', { 
        url, 
        method: options.method || 'GET',
        status: response.status 
      });
      
      return response;
      
    } catch (error) {
      this.logSecurityEvent('API_REQUEST_ERROR', { 
        url, 
        method: options.method || 'GET',
        error: error.message 
      });
      throw error;
    }
  }

  /**
   * Sanitize request data
   */
  sanitizeRequestData(data) {
    if (typeof data === 'string') {
      return this.sanitizeHtml(data);
    }
    
    if (Array.isArray(data)) {
      return data.map(item => this.sanitizeRequestData(item));
    }
    
    if (data && typeof data === 'object') {
      const sanitized = {};
      for (const [key, value] of Object.entries(data)) {
        sanitized[key] = this.sanitizeRequestData(value);
      }
      return sanitized;
    }
    
    return data;
  }

  /**
   * Detect developer tools
   */
  detectDevTools() {
    let devtools = false;
    
    setInterval(() => {
      const before = Date.now();
      console.clear();
      const after = Date.now();
      
      if (after - before > 100) {
        if (!devtools) {
          devtools = true;
          this.logSecurityEvent('DEVTOOLS_DETECTED', { timestamp: Date.now() });
        }
      } else {
        devtools = false;
      }
    }, 1000);
  }

  /**
   * Monitor suspicious activities
   */
  monitorSuspiciousActivities() {
    // Monitor for rapid fire events
    let eventCount = 0;
    const eventThreshold = 100;
    const timeWindow = 1000; // 1 second

    ['click', 'keydown', 'submit'].forEach(eventType => {
      document.addEventListener(eventType, () => {
        eventCount++;
        
        setTimeout(() => {
          eventCount--;
        }, timeWindow);
        
        if (eventCount > eventThreshold) {
          this.logSecurityEvent('SUSPICIOUS_ACTIVITY_DETECTED', { 
            type: 'rapid_events',
            eventType,
            count: eventCount 
          });
        }
      });
    });

    // Monitor for unusual navigation patterns
    let navigationCount = 0;
    window.addEventListener('popstate', () => {
      navigationCount++;
      
      setTimeout(() => {
        navigationCount--;
      }, 5000);
      
      if (navigationCount > 10) {
        this.logSecurityEvent('SUSPICIOUS_ACTIVITY_DETECTED', { 
          type: 'rapid_navigation',
          count: navigationCount 
        });
      }
    });
  }

  /**
   * Generate checksum
   */
  generateChecksum(data) {
    return CryptoJS.SHA256(data).toString();
  }

  /**
   * Generate encryption key
   */
  generateEncryptionKey() {
    // In production, this should be derived from a secure source
    const userAgent = navigator.userAgent;
    const timestamp = Date.now().toString();
    const random = Math.random().toString();
    
    return CryptoJS.SHA256(userAgent + timestamp + random).toString();
  }

  /**
   * Log security event
   */
  logSecurityEvent(type, details = {}) {
    const event = {
      type,
      details,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      sessionId: this.generateSecureToken(16),
    };

    this.securityEventLog.push(event);
    
    // Keep only last 100 events
    if (this.securityEventLog.length > 100) {
      this.securityEventLog.shift();
    }

    // Send critical events to backend immediately
    const criticalEvents = [
      'XSS_ATTEMPT_BLOCKED',
      'RATE_LIMIT_EXCEEDED',
      'DEVTOOLS_DETECTED',
      'SUSPICIOUS_ACTIVITY_DETECTED',
      'SECURE_STORAGE_TAMPERED'
    ];

    if (criticalEvents.includes(type)) {
      this.sendSecurityEventToBackend(event);
    }

    console.warn('Security Event:', event);
  }

  /**
   * Send security event to backend
   */
  async sendSecurityEventToBackend(event) {
    try {
      await fetch('/api/security/events', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': this.getCSRFToken(),
        },
        body: JSON.stringify(event),
      });
    } catch (error) {
      console.error('Failed to send security event to backend:', error);
    }
  }

  /**
   * Get security summary
   */
  getSecuritySummary() {
    const eventCounts = this.securityEventLog.reduce((counts, event) => {
      counts[event.type] = (counts[event.type] || 0) + 1;
      return counts;
    }, {});

    return {
      totalEvents: this.securityEventLog.length,
      eventCounts,
      rateLimiters: this.rateLimiters.size,
      csrfToken: !!this.csrfToken,
      lastEvent: this.securityEventLog[this.securityEventLog.length - 1],
    };
  }

  /**
   * Clear security data
   */
  clearSecurityData() {
    this.securityEventLog.length = 0;
    this.rateLimiters.clear();
    this.csrfToken = this.generateCSRFToken();
    
    // Clear secure storage
    ['localStorage', 'sessionStorage'].forEach(storageType => {
      const storage = window[storageType];
      const keys = Object.keys(storage);
      keys.forEach(key => {
        try {
          const item = JSON.parse(storage.getItem(key));
          if (item && item.data && item.checksum) {
            storage.removeItem(key);
          }
        } catch (e) {
          // Not a secure storage item, ignore
        }
      });
    });
    
    this.logSecurityEvent('SECURITY_DATA_CLEARED', {});
  }
}

// Create singleton instance
const securityService = new SecurityService();

export default securityService;