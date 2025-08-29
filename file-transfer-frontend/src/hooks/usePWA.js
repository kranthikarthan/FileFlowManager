import { useState, useEffect } from 'react';

// Custom hook for PWA functionality
export const usePWA = () => {
  const [isInstallable, setIsInstallable] = useState(false);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [installPrompt, setInstallPrompt] = useState(null);

  useEffect(() => {
    // Register service worker
    registerServiceWorker();
    
    // Listen for install prompt
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault();
      setInstallPrompt(e);
      setIsInstallable(true);
    };

    // Listen for online/offline status
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    // Listen for app installed
    const handleAppInstalled = () => {
      setIsInstallable(false);
      setInstallPrompt(null);
      console.log('PWA was installed');
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const registerServiceWorker = async () => {
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.register('/sw.js');
        
        console.log('Service Worker registered:', registration);

        // Listen for updates
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              setUpdateAvailable(true);
              console.log('New version available');
            }
          });
        });

        // Check for existing update
        if (registration.waiting) {
          setUpdateAvailable(true);
        }

      } catch (error) {
        console.error('Service Worker registration failed:', error);
      }
    }
  };

  const installApp = async () => {
    if (!installPrompt) return false;

    try {
      const result = await installPrompt.prompt();
      console.log('Install prompt result:', result);
      
      if (result.outcome === 'accepted') {
        setIsInstallable(false);
        setInstallPrompt(null);
        return true;
      }
    } catch (error) {
      console.error('Install failed:', error);
    }
    
    return false;
  };

  const updateApp = async () => {
    if (!updateAvailable) return;

    try {
      const registration = await navigator.serviceWorker.getRegistration();
      
      if (registration && registration.waiting) {
        registration.waiting.postMessage({ type: 'SKIP_WAITING' });
        
        // Wait for the new service worker to take control
        navigator.serviceWorker.addEventListener('controllerchange', () => {
          window.location.reload();
        });
      }
    } catch (error) {
      console.error('Update failed:', error);
    }
  };

  const requestNotificationPermission = async () => {
    if (!('Notification' in window)) {
      console.log('This browser does not support notifications');
      return false;
    }

    if (Notification.permission === 'granted') {
      return true;
    }

    if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }

    return false;
  };

  const subscribeToPushNotifications = async () => {
    try {
      const registration = await navigator.serviceWorker.getRegistration();
      
      if (!registration) {
        console.error('No service worker registration found');
        return null;
      }

      // Check if already subscribed
      let subscription = await registration.pushManager.getSubscription();
      
      if (!subscription) {
        // Create new subscription
        const vapidPublicKey = process.env.REACT_APP_VAPID_PUBLIC_KEY;
        
        if (!vapidPublicKey) {
          console.error('VAPID public key not found');
          return null;
        }

        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
        });
      }

      console.log('Push subscription:', subscription);
      return subscription;
      
    } catch (error) {
      console.error('Failed to subscribe to push notifications:', error);
      return null;
    }
  };

  const enableBackgroundSync = async (tag, data) => {
    try {
      const registration = await navigator.serviceWorker.ready;
      
      if ('sync' in registration) {
        await registration.sync.register(tag);
        
        // Store data for sync (would use IndexedDB in real implementation)
        localStorage.setItem(`sync_${tag}`, JSON.stringify(data));
        
        console.log('Background sync registered:', tag);
        return true;
      }
    } catch (error) {
      console.error('Background sync failed:', error);
    }
    
    return false;
  };

  // Utility function to convert VAPID key
  const urlBase64ToUint8Array = (base64String) => {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  };

  return {
    isInstallable,
    isOffline,
    updateAvailable,
    installApp,
    updateApp,
    requestNotificationPermission,
    subscribeToPushNotifications,
    enableBackgroundSync,
  };
};