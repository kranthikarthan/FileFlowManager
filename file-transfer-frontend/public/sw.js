// Service Worker for File Transfer Frontend
// Provides offline functionality, caching strategies, and background sync

const CACHE_NAME = 'file-transfer-v1.2.0';
const RUNTIME_CACHE = 'runtime-cache-v1.2.0';
const API_CACHE = 'api-cache-v1.2.0';
const STATIC_CACHE = 'static-cache-v1.2.0';

// Resources to cache on install
const PRECACHE_RESOURCES = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
  '/favicon.ico',
  '/offline.html',
];

// API endpoints to cache
const CACHEABLE_API_PATTERNS = [
  /\/api\/tenants$/,
  /\/api\/services$/,
  /\/api\/user\/profile$/,
  /\/api\/config\/.*$/,
];

// Network-first patterns (always try network first)
const NETWORK_FIRST_PATTERNS = [
  /\/api\/auth\/.*$/,
  /\/api\/file-transfers$/,
  /\/api\/batch\/.*$/,
];

// Cache-first patterns (serve from cache, update in background)
const CACHE_FIRST_PATTERNS = [
  /\.(css|js|png|jpg|jpeg|svg|gif|woff|woff2|ttf|eot)$/,
  /\/static\/.*$/,
];

/**
 * Install event - cache static resources
 */
self.addEventListener('install', event => {
  console.log('Service Worker installing...');
  
  event.waitUntil(
    Promise.all([
      // Cache static resources
      caches.open(STATIC_CACHE).then(cache => {
        return cache.addAll(PRECACHE_RESOURCES);
      }),
      
      // Skip waiting to activate immediately
      self.skipWaiting()
    ])
  );
});

/**
 * Activate event - clean up old caches
 */
self.addEventListener('activate', event => {
  console.log('Service Worker activating...');
  
  event.waitUntil(
    Promise.all([
      // Clean up old caches
      caches.keys().then(cacheNames => {
        return Promise.all(
          cacheNames.map(cacheName => {
            if (cacheName !== CACHE_NAME && 
                cacheName !== RUNTIME_CACHE && 
                cacheName !== API_CACHE && 
                cacheName !== STATIC_CACHE) {
              console.log('Deleting old cache:', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      }),
      
      // Take control of all clients
      self.clients.claim()
    ])
  );
});

/**
 * Fetch event - implement caching strategies
 */
self.addEventListener('fetch', event => {
  const request = event.request;
  const url = new URL(request.url);
  
  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }
  
  // Skip chrome-extension and other non-http requests
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Apply different strategies based on request type
  if (isApiRequest(url)) {
    event.respondWith(handleApiRequest(request));
  } else if (isStaticResource(url)) {
    event.respondWith(handleStaticResource(request));
  } else if (isPageRequest(url)) {
    event.respondWith(handlePageRequest(request));
  } else {
    event.respondWith(handleGenericRequest(request));
  }
});

/**
 * Handle API requests with network-first or cache-first strategy
 */
async function handleApiRequest(request) {
  const url = new URL(request.url);
  
  // Network-first for critical API calls
  if (NETWORK_FIRST_PATTERNS.some(pattern => pattern.test(url.pathname))) {
    return networkFirst(request, API_CACHE);
  }
  
  // Cache-first for cacheable APIs
  if (CACHEABLE_API_PATTERNS.some(pattern => pattern.test(url.pathname))) {
    return cacheFirstWithUpdate(request, API_CACHE);
  }
  
  // Default to network-first
  return networkFirst(request, API_CACHE);
}

/**
 * Handle static resources with cache-first strategy
 */
async function handleStaticResource(request) {
  return cacheFirst(request, STATIC_CACHE);
}

/**
 * Handle page requests with network-first, falling back to offline page
 */
async function handlePageRequest(request) {
  try {
    // Try network first
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
      // Cache successful responses
      const cache = await caches.open(RUNTIME_CACHE);
      cache.put(request, networkResponse.clone());
      return networkResponse;
    }
    
    throw new Error('Network response not ok');
    
  } catch (error) {
    // Fall back to cache
    const cachedResponse = await caches.match(request);
    
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Fall back to offline page
    return caches.match('/offline.html');
  }
}

/**
 * Handle generic requests
 */
async function handleGenericRequest(request) {
  return networkFirst(request, RUNTIME_CACHE);
}

/**
 * Network-first strategy
 */
async function networkFirst(request, cacheName) {
  try {
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
    
  } catch (error) {
    console.log('Network failed, trying cache:', request.url);
    
    const cachedResponse = await caches.match(request);
    
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Return offline response for failed requests
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'This request requires an internet connection'
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );
  }
}

/**
 * Cache-first strategy
 */
async function cacheFirst(request, cacheName) {
  const cachedResponse = await caches.match(request);
  
  if (cachedResponse) {
    return cachedResponse;
  }
  
  try {
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
    
  } catch (error) {
    console.error('Both cache and network failed:', request.url);
    throw error;
  }
}

/**
 * Cache-first with background update
 */
async function cacheFirstWithUpdate(request, cacheName) {
  const cachedResponse = await caches.match(request);
  
  // Start background fetch
  const fetchPromise = fetch(request).then(response => {
    if (response.ok) {
      const cache = caches.open(cacheName);
      cache.then(c => c.put(request, response.clone()));
    }
    return response;
  }).catch(error => {
    console.error('Background fetch failed:', error);
  });
  
  // Return cached response immediately if available
  if (cachedResponse) {
    return cachedResponse;
  }
  
  // Wait for network if no cache
  try {
    return await fetchPromise;
  } catch (error) {
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'This content is not available offline'
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );
  }
}

/**
 * Check if request is for API
 */
function isApiRequest(url) {
  return url.pathname.startsWith('/api/');
}

/**
 * Check if request is for static resource
 */
function isStaticResource(url) {
  return CACHE_FIRST_PATTERNS.some(pattern => pattern.test(url.pathname)) ||
         url.pathname.startsWith('/static/');
}

/**
 * Check if request is for a page
 */
function isPageRequest(url) {
  return url.pathname === '/' || 
         (!url.pathname.includes('.') && !url.pathname.startsWith('/api/'));
}

/**
 * Background sync for offline actions
 */
self.addEventListener('sync', event => {
  console.log('Background sync triggered:', event.tag);
  
  if (event.tag === 'file-upload') {
    event.waitUntil(syncFileUploads());
  } else if (event.tag === 'analytics') {
    event.waitUntil(syncAnalytics());
  } else if (event.tag === 'security-events') {
    event.waitUntil(syncSecurityEvents());
  }
});

/**
 * Sync file uploads that failed while offline
 */
async function syncFileUploads() {
  try {
    const uploads = await getStoredUploads();
    
    for (const upload of uploads) {
      try {
        const response = await fetch('/api/file-transfers', {
          method: 'POST',
          body: upload.formData,
          headers: upload.headers
        });
        
        if (response.ok) {
          await removeStoredUpload(upload.id);
          console.log('Synced file upload:', upload.id);
        }
        
      } catch (error) {
        console.error('Failed to sync upload:', upload.id, error);
      }
    }
    
  } catch (error) {
    console.error('Background sync failed:', error);
  }
}

/**
 * Sync analytics data
 */
async function syncAnalytics() {
  try {
    const analyticsData = await getStoredAnalytics();
    
    if (analyticsData.length > 0) {
      const response = await fetch('/api/analytics/batch', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(analyticsData)
      });
      
      if (response.ok) {
        await clearStoredAnalytics();
        console.log('Synced analytics data');
      }
    }
    
  } catch (error) {
    console.error('Failed to sync analytics:', error);
  }
}

/**
 * Sync security events
 */
async function syncSecurityEvents() {
  try {
    const securityEvents = await getStoredSecurityEvents();
    
    for (const event of securityEvents) {
      try {
        const response = await fetch('/api/security/events', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(event)
        });
        
        if (response.ok) {
          await removeStoredSecurityEvent(event.id);
          console.log('Synced security event:', event.id);
        }
        
      } catch (error) {
        console.error('Failed to sync security event:', event.id, error);
      }
    }
    
  } catch (error) {
    console.error('Failed to sync security events:', error);
  }
}

/**
 * Push event for notifications
 */
self.addEventListener('push', event => {
  console.log('Push event received');
  
  if (!event.data) {
    return;
  }
  
  const data = event.data.json();
  const options = {
    body: data.body,
    icon: '/icon-192x192.png',
    badge: '/icon-72x72.png',
    vibrate: [200, 100, 200],
    data: data.data,
    actions: data.actions || [
      {
        action: 'view',
        title: 'View'
      },
      {
        action: 'dismiss',
        title: 'Dismiss'
      }
    ]
  };
  
  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

/**
 * Notification click handling
 */
self.addEventListener('notificationclick', event => {
  console.log('Notification clicked:', event.action);
  
  event.notification.close();
  
  if (event.action === 'view') {
    const urlToOpen = event.notification.data?.url || '/';
    
    event.waitUntil(
      clients.matchAll({ type: 'window' }).then(clientList => {
        // Check if there's already a window/tab open with this URL
        for (const client of clientList) {
          if (client.url === urlToOpen && 'focus' in client) {
            return client.focus();
          }
        }
        
        // Open new window/tab
        if (clients.openWindow) {
          return clients.openWindow(urlToOpen);
        }
      })
    );
  }
});

/**
 * Message handling for communication with main thread
 */
self.addEventListener('message', event => {
  console.log('Service Worker received message:', event.data);
  
  if (event.data && event.data.type) {
    switch (event.data.type) {
      case 'SKIP_WAITING':
        self.skipWaiting();
        break;
        
      case 'CACHE_API_RESPONSE':
        cacheApiResponse(event.data.request, event.data.response);
        break;
        
      case 'CLEAR_CACHE':
        clearCache(event.data.cacheName);
        break;
        
      case 'GET_CACHE_SIZE':
        getCacheSize().then(size => {
          event.ports[0].postMessage({ cacheSize: size });
        });
        break;
    }
  }
});

/**
 * Cache API response manually
 */
async function cacheApiResponse(request, response) {
  try {
    const cache = await caches.open(API_CACHE);
    await cache.put(request, new Response(JSON.stringify(response)));
    console.log('Manually cached API response:', request.url);
  } catch (error) {
    console.error('Failed to manually cache API response:', error);
  }
}

/**
 * Clear specific cache
 */
async function clearCache(cacheName) {
  try {
    const deleted = await caches.delete(cacheName);
    console.log('Cache cleared:', cacheName, deleted);
  } catch (error) {
    console.error('Failed to clear cache:', error);
  }
}

/**
 * Get total cache size
 */
async function getCacheSize() {
  try {
    const cacheNames = await caches.keys();
    let totalSize = 0;
    
    for (const cacheName of cacheNames) {
      const cache = await caches.open(cacheName);
      const requests = await cache.keys();
      
      for (const request of requests) {
        const response = await cache.match(request);
        if (response && response.body) {
          const reader = response.body.getReader();
          let size = 0;
          
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            size += value.length;
          }
          
          totalSize += size;
        }
      }
    }
    
    return totalSize;
    
  } catch (error) {
    console.error('Failed to calculate cache size:', error);
    return 0;
  }
}

// Helper functions for IndexedDB operations
async function getStoredUploads() {
  // Implementation would use IndexedDB to get stored uploads
  return [];
}

async function removeStoredUpload(id) {
  // Implementation would remove upload from IndexedDB
}

async function getStoredAnalytics() {
  // Implementation would get analytics data from IndexedDB
  return [];
}

async function clearStoredAnalytics() {
  // Implementation would clear analytics data from IndexedDB
}

async function getStoredSecurityEvents() {
  // Implementation would get security events from IndexedDB
  return [];
}

async function removeStoredSecurityEvent(id) {
  // Implementation would remove security event from IndexedDB
}

console.log('Service Worker loaded successfully');