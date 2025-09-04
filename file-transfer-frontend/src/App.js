import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Container, Box, Button, Avatar, Menu, MenuItem, useMediaQuery, useTheme as useMuiTheme, Fab, Zoom, Tooltip } from '@mui/material';
import { AccountCircle, Logout, Add as AddIcon, Business as TenantIcon, Build as ServiceIcon } from '@mui/icons-material';
import { Navigation } from './components/Navigation';
import { FileTransferList } from './components/FileTransferList';
import { ServiceManagement } from './components/ServiceManagement';
import { ServiceConfiguration } from './components/ServiceConfiguration';
import { SsoConfiguration } from './components/SsoConfiguration';
import { Dashboard } from './components/Dashboard';
import { Login } from './components/Login';
import TenantManagement from './components/TenantManagement';
import HolidayManagement from './components/HolidayManagement';
import AlertManagement from './components/AlertManagement';
import SubServiceManagement from './components/SubServiceManagement';
import CutOffExtensionManagement from './components/CutOffExtensionManagement';
import SharedSchemaManagement from './components/SharedSchemaManagement';
import EotValidationDashboard from './components/EotValidationDashboard';
import AckNackManagement from './components/AckNackManagement';
import CompressionManagement from './components/CompressionManagement';
import HsmManagement from './components/HsmManagement';
import ContentAnalysis from './components/ContentAnalysis';
import FileTagManagement from './components/FileTagManagement';
import { CustomThemeProvider, useTheme } from './theme/themeProvider';
import MobileNavigation from './components/mobile/MobileNavigation';
import ThemeToggle from './components/mobile/ThemeToggle';
import TenantSetupWizard from './components/wizards/TenantSetupWizard';
import ServiceSetupWizard from './components/wizards/ServiceSetupWizard';
import PWAPrompt from './components/mobile/PWAPrompt';
import './styles/mobile.css';

// Main App Content Component
function AppContent() {
  const [user, setUser] = useState(null);
  const [anchorEl, setAnchorEl] = useState(null);
  const [tenantWizardOpen, setTenantWizardOpen] = useState(false);
  const [serviceWizardOpen, setServiceWizardOpen] = useState(false);
  
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));

  useEffect(() => {
    // Check for existing auth token
    const token = localStorage.getItem('authToken');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      try {
        setUser(JSON.parse(userData));
      } catch (err) {
        console.error('Failed to parse user data:', err);
        handleLogout();
      }
    }
  }, []);

  const handleLogin = (userData) => {
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    setUser(null);
    setAnchorEl(null);
  };

  const handleMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleTenantSetupComplete = (tenantData) => {
    console.log('Tenant setup completed:', tenantData);
    // Refresh data or navigate as needed
  };

  const handleServiceSetupComplete = (serviceData) => {
    console.log('Service setup completed:', serviceData);
    // Refresh data or navigate as needed
  };

  if (!user) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <Box sx={{ 
      flexGrow: 1, 
      minHeight: '100vh',
      background: isDark 
        ? 'radial-gradient(ellipse at top, rgba(13, 27, 13, 1) 0%, rgba(26, 46, 26, 1) 100%)'
        : 'radial-gradient(ellipse at top, rgba(248, 253, 248, 1) 0%, rgba(232, 245, 232, 1) 100%)',
    }}>
      {/* Enhanced AppBar with Glassmorphism */}
      <AppBar 
        position="sticky" 
        elevation={0}
        sx={{
          background: isDark
            ? 'rgba(26, 46, 26, 0.9)'
            : 'rgba(248, 253, 248, 0.9)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid',
          borderBottomColor: isDark ? 'rgba(129, 199, 132, 0.2)' : 'rgba(46, 125, 50, 0.2)',
        }}
      >
        <Toolbar>
          <Typography 
            variant="h6" 
            component="div" 
            sx={{ 
              flexGrow: 1, 
              fontWeight: 700,
              background: `linear-gradient(135deg, ${isDark ? '#81c784' : '#2e7d32'} 0%, ${isDark ? '#a5d6a7' : '#4caf50'} 100%)`,
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            File Transfer Management System
          </Typography>
          
          <Box display="flex" alignItems="center" gap={2}>
            {/* Theme Toggle */}
            {!isMobile && <ThemeToggle />}
            
            {/* User Info */}
            <Typography variant="body2" sx={{ color: 'text.primary' }}>
              Welcome, {user.name}
            </Typography>
            
            <Button
              color="inherit"
              onClick={handleMenuOpen}
              startIcon={<AccountCircle />}
              sx={{
                color: 'text.primary',
                background: isDark 
                  ? 'rgba(129, 199, 132, 0.1)' 
                  : 'rgba(46, 125, 50, 0.1)',
                borderRadius: 3,
                px: 2,
                '&:hover': {
                  background: isDark 
                    ? 'rgba(129, 199, 132, 0.2)' 
                    : 'rgba(46, 125, 50, 0.2)',
                },
              }}
            >
              {user.organization}
            </Button>
            
            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
              PaperProps={{
                sx: {
                  mt: 1,
                  borderRadius: 2,
                  boxShadow: isDark 
                    ? '0 8px 32px rgba(0, 0, 0, 0.4)' 
                    : '0 8px 32px rgba(46, 125, 50, 0.15)',
                  backdropFilter: 'blur(20px)',
                  background: isDark 
                    ? 'rgba(26, 46, 26, 0.95)' 
                    : 'rgba(248, 253, 248, 0.95)',
                },
              }}
            >
              <MenuItem onClick={handleMenuClose}>
                <Typography variant="body2">
                  {user.email}
                </Typography>
              </MenuItem>
              <MenuItem onClick={handleLogout}>
                <Logout fontSize="small" sx={{ mr: 1 }} />
                Logout
              </MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </AppBar>
      
      {/* Navigation - Conditional rendering for mobile */}
      {!isMobile && <Navigation />}
      
      {/* Main Content Container */}
      <Container 
        maxWidth="xl" 
        sx={{ 
          mt: 4, 
          mb: isMobile ? 10 : 4, // Extra bottom margin for mobile nav
          px: isMobile ? 2 : 3,
        }}
      >
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transfers" element={<FileTransferList />} />
          <Route path="/service-config" element={<ServiceConfiguration />} />
          <Route path="/sso-config" element={<SsoConfiguration />} />
          <Route path="/services" element={<ServiceManagement />} />
          <Route path="/tenants" element={<TenantManagement />} />
          <Route path="/holidays" element={<HolidayManagement />} />
          <Route path="/alerts" element={<AlertManagement />} />
          <Route path="/sub-services" element={<SubServiceManagement tenantId={user?.tenantId || 'default'} />} />
          <Route path="/cutoff-extensions" element={<CutOffExtensionManagement tenantId={user?.tenantId || 'default'} />} />
          <Route path="/shared-schemas" element={<SharedSchemaManagement tenantId={user?.tenantId || 'default'} />} />
          <Route path="/eot-validation" element={<EotValidationDashboard tenantId={user?.tenantId || 'default'} />} />
          <Route path="/ack-nack" element={<AckNackManagement selectedTenant={user?.tenantId || 'default'} />} />
          <Route path="/compression" element={<CompressionManagement selectedTenant={user?.tenantId || 'default'} />} />
          <Route path="/hsm" element={<HsmManagement selectedTenant={user?.tenantId || 'default'} />} />
          <Route path="/content-analysis" element={<ContentAnalysis />} />
          <Route path="/file-tags" element={<FileTagManagement selectedTenant={user?.tenantId || 'default'} />} />
        </Routes>
      </Container>

      {/* Mobile Navigation */}
      {isMobile && <MobileNavigation />}

      {/* Floating Action Buttons for Quick Setup */}
      {!isMobile && (
        <Box
          sx={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
            zIndex: 1000,
          }}
        >
          <Zoom in={true} style={{ transitionDelay: '100ms' }}>
            <Tooltip title="Quick Service Setup" placement="left">
              <Fab
                color="secondary"
                onClick={() => setServiceWizardOpen(true)}
                sx={{
                  background: `linear-gradient(135deg, ${isDark ? '#a5d6a7' : '#4caf50'} 0%, ${isDark ? '#81c784' : '#2e7d32'} 100%)`,
                  '&:hover': {
                    background: `linear-gradient(135deg, ${isDark ? '#c8e6c9' : '#66bb6a'} 0%, ${isDark ? '#a5d6a7' : '#388e3c'} 100%)`,
                  },
                }}
              >
                <ServiceIcon />
              </Fab>
            </Tooltip>
          </Zoom>

          <Zoom in={true} style={{ transitionDelay: '200ms' }}>
            <Tooltip title="Quick Tenant Setup" placement="left">
              <Fab
                color="primary"
                onClick={() => setTenantWizardOpen(true)}
                sx={{
                  background: `linear-gradient(135deg, ${isDark ? '#81c784' : '#2e7d32'} 0%, ${isDark ? '#66bb6a' : '#1b5e20'} 100%)`,
                  '&:hover': {
                    background: `linear-gradient(135deg, ${isDark ? '#a5d6a7' : '#4caf50'} 0%, ${isDark ? '#81c784' : '#2e7d32'} 100%)`,
                  },
                }}
              >
                <TenantIcon />
              </Fab>
            </Tooltip>
          </Zoom>
        </Box>
      )}

      {/* Setup Wizards */}
      <TenantSetupWizard
        open={tenantWizardOpen}
        onClose={() => setTenantWizardOpen(false)}
        onComplete={handleTenantSetupComplete}
      />

      <ServiceSetupWizard
        open={serviceWizardOpen}
        onClose={() => setServiceWizardOpen(false)}
        onComplete={handleServiceSetupComplete}
        tenantId={user?.tenantId || 'default'}
      />

      {/* PWA Features */}
      <PWAPrompt />
    </Box>
  );
}

// Main App Component with Theme Provider
function App() {
  return (
    <CustomThemeProvider>
      <Router>
        <AppContent />
      </Router>
    </CustomThemeProvider>
  );
}

export default App;