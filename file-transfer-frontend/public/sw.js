// Service Worker for File Transfer Management System PWA
const CACHE_NAME = 'file-transfer-v1.0.0';
const STATIC_CACHE = 'file-transfer-static-v1.0.0';
const DYNAMIC_CACHE = 'file-transfer-dynamic-v1.0.0';

// Files to cache immediately (critical for app shell)
const STATIC_FILES = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png'
];

// API endpoints to cache with different strategies
const API_ENDPOINTS = [
  '/api/tenants',
  '/api/services',
  '/api/shared-schemas',
  '/api/eot-validation',
  '/api/dashboard'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  console.log('[SW] Installing service worker...');
  
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => {
        console.log('[SW] Pre-caching static assets');
        return cache.addAll(STATIC_FILES);
      })
      .then(() => {
        console.log('[SW] Static assets cached successfully');
        return self.skipWaiting(); // Activate immediately
      })
      .catch((error) => {
        console.error('[SW] Failed to cache static assets:', error);
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating service worker...');
  
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== STATIC_CACHE && 
                cacheName !== DYNAMIC_CACHE && 
                cacheName !== CACHE_NAME) {
              console.log('[SW] Deleting old cache:', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        console.log('[SW] Service worker activated');
        return self.clients.claim(); // Take control immediately
      })
  );
});

// Fetch event - implement caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }

  // Skip Chrome extensions and non-http requests
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Different strategies for different types of requests
  if (isStaticAsset(request)) {
    event.respondWith(cacheFirstStrategy(request, STATIC_CACHE));
  } else if (isAPIRequest(request)) {
    event.respondWith(networkFirstStrategy(request, DYNAMIC_CACHE));
  } else if (isNavigationRequest(request)) {
    event.respondWith(staleWhileRevalidateStrategy(request, DYNAMIC_CACHE));
  } else {
    event.respondWith(networkFirstStrategy(request, DYNAMIC_CACHE));
  }
});

// Cache First Strategy - for static assets
async function cacheFirstStrategy(request, cacheName) {
  try {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      console.log('[SW] Serving from cache:', request.url);
      return cachedResponse;
    }

    console.log('[SW] Fetching and caching:', request.url);
    const response = await fetch(request);
    
    if (response.status === 200) {
      const cache = await caches.open(cacheName);
      cache.put(request, response.clone());
    }
    
    return response;
  } catch (error) {
    console.error('[SW] Cache first strategy failed:', error);
    return new Response('Asset not available offline', { status: 503 });
  }
}

// Network First Strategy - for API requests
async function networkFirstStrategy(request, cacheName) {
  try {
    console.log('[SW] Network first for:', request.url);
    const response = await fetch(request);
    
    if (response.status === 200 && isAPIRequest(request)) {
      const cache = await caches.open(cacheName);
      cache.put(request, response.clone());
    }
    
    return response;
  } catch (error) {
    console.log('[SW] Network failed, trying cache:', request.url);
    const cachedResponse = await caches.match(request);
    
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Return offline page for navigation requests
    if (isNavigationRequest(request)) {
      return caches.match('/') || new Response('Offline', { status: 503 });
    }
    
    return new Response('Request failed and no cache available', { status: 503 });
  }
}

// Stale While Revalidate Strategy - for app shell and pages
async function staleWhileRevalidateStrategy(request, cacheName) {
  const cachedResponse = await caches.match(request);
  
  const fetchPromise = fetch(request)
    .then((response) => {
      if (response.status === 200) {
        const cache = caches.open(cacheName);
        cache.then((c) => c.put(request, response.clone()));
      }
      return response;
    })
    .catch(() => cachedResponse || new Response('Offline', { status: 503 }));

  return cachedResponse || fetchPromise;
}

// Helper functions
function isStaticAsset(request) {
  return request.url.includes('/static/') || 
         request.url.includes('.js') || 
         request.url.includes('.css') || 
         request.url.includes('.png') || 
         request.url.includes('.jpg') || 
         request.url.includes('.svg') ||
         request.url.includes('.ico');
}

function isAPIRequest(request) {
  return request.url.includes('/api/') || 
         API_ENDPOINTS.some(endpoint => request.url.includes(endpoint));
}

function isNavigationRequest(request) {
  return request.mode === 'navigate' || 
         (request.method === 'GET' && request.headers.get('accept')?.includes('text/html'));
}

// Background sync for offline actions
self.addEventListener('sync', (event) => {
  console.log('[SW] Background sync triggered:', event.tag);
  
  if (event.tag === 'file-upload') {
    event.waitUntil(syncFileUploads());
  } else if (event.tag === 'config-update') {
    event.waitUntil(syncConfigUpdates());
  }
});

// Sync offline file uploads when connection is restored
async function syncFileUploads() {
  try {
    const uploads = await getStoredUploads();
    
    for (const upload of uploads) {
      try {
        await fetch('/api/files/upload', {
          method: 'POST',
          body: upload.data,
          headers: upload.headers
        });
        
        await removeStoredUpload(upload.id);
        console.log('[SW] Synced offline upload:', upload.id);
      } catch (error) {
        console.error('[SW] Failed to sync upload:', upload.id, error);
      }
    }
  } catch (error) {
    console.error('[SW] Background sync failed:', error);
  }
}

// Sync configuration updates
async function syncConfigUpdates() {
  try {
    const updates = await getStoredConfigUpdates();
    
    for (const update of updates) {
      try {
        await fetch(update.url, {
          method: update.method,
          body: JSON.stringify(update.data),
          headers: {
            'Content-Type': 'application/json',
            ...update.headers
          }
        });
        
        await removeStoredConfigUpdate(update.id);
        console.log('[SW] Synced config update:', update.id);
      } catch (error) {
        console.error('[SW] Failed to sync config update:', update.id, error);
      }
    }
  } catch (error) {
    console.error('[SW] Config sync failed:', error);
  }
}

// Push notifications for system alerts
self.addEventListener('push', (event) => {
  console.log('[SW] Push notification received');
  
  let notificationData = {
    title: 'File Transfer System',
    body: 'New notification',
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    tag: 'general'
  };

  if (event.data) {
    try {
      const data = event.data.json();
      notificationData = {
        title: data.title || notificationData.title,
        body: data.body || notificationData.body,
        icon: data.icon || notificationData.icon,
        badge: data.badge || notificationData.badge,
        tag: data.tag || notificationData.tag,
        data: data.data || {},
        actions: data.actions || []
      };
    } catch (error) {
      console.error('[SW] Failed to parse push data:', error);
    }
  }

  event.waitUntil(
    self.registration.showNotification(notificationData.title, {
      body: notificationData.body,
      icon: notificationData.icon,
      badge: notificationData.badge,
      tag: notificationData.tag,
      data: notificationData.data,
      actions: notificationData.actions,
      requireInteraction: notificationData.tag === 'critical',
      vibrate: [200, 100, 200]
    })
  );
});

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
  console.log('[SW] Notification clicked:', event.notification.tag);
  
  event.notification.close();

  const clickAction = event.action || 'default';
  const notificationData = event.notification.data || {};

  let url = '/';
  
  switch (clickAction) {
    case 'view-transfers':
      url = '/transfers';
      break;
    case 'view-eot':
      url = '/eot-validation';
      break;
    case 'view-alerts':
      url = '/alerts';
      break;
    default:
      url = notificationData.url || '/';
      break;
  }

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        // Check if app is already open
        for (const client of clientList) {
          if (client.url.includes(url) && 'focus' in client) {
            return client.focus();
          }
        }
        
        // Open new window/tab
        if (clients.openWindow) {
          return clients.openWindow(url);
        }
      })
  );
});

// Utility functions for offline storage
async function getStoredUploads() {
  // Implementation would depend on IndexedDB or other storage mechanism
  return [];
}

async function removeStoredUpload(id) {
  // Implementation would depend on storage mechanism
}

async function getStoredConfigUpdates() {
  // Implementation would depend on IndexedDB or other storage mechanism
  return [];
}

async function removeStoredConfigUpdate(id) {
  // Implementation would depend on storage mechanism
}

// Update available notification
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    console.log('[SW] Skipping waiting and activating new version');
    self.skipWaiting();
  }
});

console.log('[SW] Service worker script loaded');