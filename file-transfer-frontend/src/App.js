import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Container, Box, Button, Avatar, Menu, MenuItem } from '@mui/material';
import { AccountCircle, Logout } from '@mui/icons-material';
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

function App() {
  const [user, setUser] = useState(null);
  const [anchorEl, setAnchorEl] = useState(null);

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

  // Show login page if user is not authenticated
  if (!user) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            File Transfer Management System
          </Typography>
          
          <Box display="flex" alignItems="center">
            <Typography variant="body2" sx={{ mr: 2 }}>
              Welcome, {user.name}
            </Typography>
            <Button
              color="inherit"
              onClick={handleMenuOpen}
              startIcon={<AccountCircle />}
            >
              {user.organization}
            </Button>
            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
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
      
      <Navigation />
      
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
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
        </Routes>
      </Container>
    </Box>
  );
}

export default App;