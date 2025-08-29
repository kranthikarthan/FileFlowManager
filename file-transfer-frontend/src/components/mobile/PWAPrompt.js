import React, { useState, useEffect } from 'react';
import {
  Snackbar,
  Alert,
  Button,
  Box,
  Typography,
  IconButton,
  Slide,
  Card,
  CardContent,
  CardActions,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  useMediaQuery,
  useTheme as useMuiTheme,
} from '@mui/material';
import {
  GetApp as InstallIcon,
  Close as CloseIcon,
  Refresh as UpdateIcon,
  PhoneAndroid as MobileIcon,
  Wifi as OnlineIcon,
  WifiOff as OfflineIcon,
  Notifications as NotificationsIcon,
} from '@mui/icons-material';
import { useTheme } from '../../theme/themeProvider';
import { usePWA } from '../../hooks/usePWA';

const PWAPrompt = () => {
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));
  
  const {
    isInstallable,
    isOffline,
    updateAvailable,
    installApp,
    updateApp,
    requestNotificationPermission,
    subscribeToPushNotifications,
  } = usePWA();

  const [showInstallPrompt, setShowInstallPrompt] = useState(false);
  const [showUpdatePrompt, setShowUpdatePrompt] = useState(false);
  const [showOfflinePrompt, setShowOfflinePrompt] = useState(false);
  const [showNotificationPrompt, setShowNotificationPrompt] = useState(false);
  const [installDismissed, setInstallDismissed] = useState(false);

  useEffect(() => {
    // Show install prompt on mobile after delay
    if (isInstallable && isMobile && !installDismissed) {
      const timer = setTimeout(() => {
        setShowInstallPrompt(true);
      }, 30000); // Show after 30 seconds

      return () => clearTimeout(timer);
    }
  }, [isInstallable, isMobile, installDismissed]);

  useEffect(() => {
    // Show update prompt when available
    if (updateAvailable) {
      setShowUpdatePrompt(true);
    }
  }, [updateAvailable]);

  useEffect(() => {
    // Show offline notification
    if (isOffline) {
      setShowOfflinePrompt(true);
    } else {
      setShowOfflinePrompt(false);
    }
  }, [isOffline]);

  useEffect(() => {
    // Show notification permission prompt after user interaction
    const checkNotificationPermission = () => {
      if ('Notification' in window && Notification.permission === 'default') {
        const timer = setTimeout(() => {
          setShowNotificationPrompt(true);
        }, 60000); // Show after 1 minute

        return () => clearTimeout(timer);
      }
    };

    const handleUserInteraction = () => {
      checkNotificationPermission();
      document.removeEventListener('click', handleUserInteraction);
    };

    document.addEventListener('click', handleUserInteraction);
    return () => document.removeEventListener('click', handleUserInteraction);
  }, []);

  const handleInstall = async () => {
    const success = await installApp();
    if (success) {
      setShowInstallPrompt(false);
    }
  };

  const handleInstallDismiss = () => {
    setShowInstallPrompt(false);
    setInstallDismissed(true);
    localStorage.setItem('pwa_install_dismissed', 'true');
  };

  const handleUpdate = async () => {
    await updateApp();
    setShowUpdatePrompt(false);
  };

  const handleEnableNotifications = async () => {
    const granted = await requestNotificationPermission();
    if (granted) {
      await subscribeToPushNotifications();
      setShowNotificationPrompt(false);
    }
  };

  return (
    <>
      {/* Install App Prompt */}
      <Dialog
        open={showInstallPrompt}
        onClose={handleInstallDismiss}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            background: isDark 
              ? 'rgba(26, 46, 26, 0.98)'
              : 'rgba(248, 253, 248, 0.98)',
            backdropFilter: 'blur(20px)',
          },
        }}
      >
        <DialogTitle sx={{ 
          background: isDark
            ? 'linear-gradient(135deg, rgba(129, 199, 132, 0.1) 0%, rgba(165, 214, 167, 0.1) 100%)'
            : 'linear-gradient(135deg, rgba(46, 125, 50, 0.1) 0%, rgba(76, 175, 80, 0.1) 100%)',
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <MobileIcon color="primary" />
            <Typography variant="h6" fontWeight={600}>
              Install File Transfer App
            </Typography>
          </Box>
        </DialogTitle>
        
        <DialogContent sx={{ py: 3 }}>
          <Typography variant="body1" paragraph>
            Get the best experience with our mobile app! Install it on your device for:
          </Typography>
          
          <Box component="ul" sx={{ pl: 2, m: 0 }}>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              ⚡ Faster loading and better performance
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              📱 Native mobile experience
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              🔔 Push notifications for alerts
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              📶 Offline access to cached data
            </Typography>
            <Typography component="li" variant="body2">
              🏠 Quick access from your home screen
            </Typography>
          </Box>
        </DialogContent>
        
        <DialogActions sx={{ p: 3, pt: 0 }}>
          <Button onClick={handleInstallDismiss} color="inherit">
            Maybe Later
          </Button>
          <Button 
            onClick={handleInstall}
            variant="contained"
            startIcon={<InstallIcon />}
          >
            Install App
          </Button>
        </DialogActions>
      </Dialog>

      {/* Update Available Prompt */}
      <Snackbar
        open={showUpdatePrompt}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        sx={{ mb: isMobile ? 8 : 2 }}
      >
        <Alert
          severity="info"
          action={
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                color="inherit"
                size="small"
                onClick={handleUpdate}
                startIcon={<UpdateIcon />}
              >
                Update
              </Button>
              <IconButton
                size="small"
                color="inherit"
                onClick={() => setShowUpdatePrompt(false)}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          }
          sx={{
            background: isDark 
              ? 'rgba(26, 46, 26, 0.95)'
              : 'rgba(248, 253, 248, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid',
            borderColor: isDark ? 'rgba(129, 199, 132, 0.3)' : 'rgba(46, 125, 50, 0.3)',
          }}
        >
          A new version of the app is available!
        </Alert>
      </Snackbar>

      {/* Offline Status */}
      <Snackbar
        open={showOfflinePrompt}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
        TransitionComponent={Slide}
        TransitionProps={{ direction: 'down' }}
      >
        <Alert
          severity="warning"
          icon={<OfflineIcon />}
          sx={{
            background: isDark 
              ? 'rgba(26, 46, 26, 0.95)'
              : 'rgba(248, 253, 248, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid',
            borderColor: 'warning.main',
          }}
        >
          You're offline. Some features may be limited.
        </Alert>
      </Snackbar>

      {/* Notification Permission Prompt */}
      <Dialog
        open={showNotificationPrompt}
        onClose={() => setShowNotificationPrompt(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            background: isDark 
              ? 'rgba(26, 46, 26, 0.98)'
              : 'rgba(248, 253, 248, 0.98)',
            backdropFilter: 'blur(20px)',
          },
        }}
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <NotificationsIcon color="primary" />
            <Typography variant="h6" fontWeight={600}>
              Enable Notifications
            </Typography>
          </Box>
        </DialogTitle>
        
        <DialogContent>
          <Typography variant="body1" paragraph>
            Stay updated with important alerts and notifications about:
          </Typography>
          
          <Box component="ul" sx={{ pl: 2, m: 0 }}>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              🚨 Critical system alerts
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              📊 EOT validation discrepancies
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 1 }}>
              ⏰ Cut-off time reminders
            </Typography>
            <Typography component="li" variant="body2">
              ✅ Processing completion updates
            </Typography>
          </Box>
        </DialogContent>
        
        <DialogActions>
          <Button onClick={() => setShowNotificationPrompt(false)} color="inherit">
            Not Now
          </Button>
          <Button 
            onClick={handleEnableNotifications}
            variant="contained"
            startIcon={<NotificationsIcon />}
          >
            Enable Notifications
          </Button>
        </DialogActions>
      </Dialog>

      {/* Mini Install Prompt for Desktop */}
      {isInstallable && !isMobile && !installDismissed && (
        <Snackbar
          open={true}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
          sx={{ mb: 2, ml: 2 }}
        >
          <Card sx={{ 
            minWidth: 300,
            background: isDark 
              ? 'rgba(26, 46, 26, 0.95)'
              : 'rgba(248, 253, 248, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid',
            borderColor: isDark ? 'rgba(129, 199, 132, 0.3)' : 'rgba(46, 125, 50, 0.3)',
          }}>
            <CardContent sx={{ pb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <InstallIcon color="primary" />
                <Box>
                  <Typography variant="subtitle2" fontWeight={600}>
                    Install App
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Add to your desktop for quick access
                  </Typography>
                </Box>
              </Box>
            </CardContent>
            <CardActions sx={{ pt: 0, justifyContent: 'space-between' }}>
              <Button size="small" onClick={handleInstallDismiss} color="inherit">
                Dismiss
              </Button>
              <Button size="small" onClick={handleInstall} variant="contained">
                Install
              </Button>
            </CardActions>
          </Card>
        </Snackbar>
      )}
    </>
  );
};

export default PWAPrompt;