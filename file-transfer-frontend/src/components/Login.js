import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Card,
  CardContent,
  Grid,
  Alert,
  Divider,
  Avatar
} from '@mui/material';
import {
  Login as LoginIcon,
  Security as SecurityIcon,
  Business as BusinessIcon
} from '@mui/icons-material';
import { ssoAPI, authAPI } from '../services/serviceAPI';

export const Login = ({ onLogin }) => {
  const [ssoConfigs, setSsoConfigs] = useState([]);
  const [selectedOrg, setSelectedOrg] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [loginMode, setLoginMode] = useState('sso'); // 'sso' or 'local'
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });

  useEffect(() => {
    fetchSsoConfigurations();
  }, []);

  const fetchSsoConfigurations = async () => {
    try {
      const response = await ssoAPI.getEnabledSsoConfigurations();
      setSsoConfigs(response.data);
    } catch (err) {
      console.error('Failed to fetch SSO configurations:', err);
    }
  };

  const handleSsoLogin = async () => {
    if (!selectedOrg) {
      setError('Please select an organization');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Redirect to SSO provider
      const response = await authAPI.loginWithSSO(selectedOrg);
      
      // In a real implementation, this would redirect to the SSO provider
      // For demo purposes, we'll simulate a successful login
      const mockUser = {
        id: 1,
        email: 'demo@example.com',
        name: 'Demo User',
        organization: selectedOrg
      };
      
      localStorage.setItem('authToken', 'demo-jwt-token');
      localStorage.setItem('user', JSON.stringify(mockUser));
      
      if (onLogin) {
        onLogin(mockUser);
      }
      
    } catch (err) {
      setError('SSO login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLocalLogin = async () => {
    if (!credentials.username || !credentials.password) {
      setError('Please enter username and password');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // For demo purposes, accept any credentials
      const mockUser = {
        id: 1,
        email: credentials.username,
        name: 'Local User',
        organization: 'local'
      };
      
      localStorage.setItem('authToken', 'demo-jwt-token-local');
      localStorage.setItem('user', JSON.stringify(mockUser));
      
      if (onLogin) {
        onLogin(mockUser);
      }
      
    } catch (err) {
      setError('Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const getProviderIcon = (provider) => {
    switch (provider) {
      case 'AZURE_AD': return '🔷';
      case 'GOOGLE': return '🔴';
      case 'OKTA': return '🔵';
      case 'KEYCLOAK': return '🔑';
      case 'CUSTOM_OIDC': return '⚙️';
      case 'SAML2': return '🛡️';
      default: return '🔐';
    }
  };

  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight="100vh"
      bgcolor="background.default"
    >
      <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%' }}>
        <Box textAlign="center" mb={3}>
          <Avatar sx={{ mx: 'auto', mb: 2, bgcolor: 'primary.main' }}>
            <SecurityIcon />
          </Avatar>
          <Typography variant="h4" gutterBottom>
            File Transfer System
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Please sign in to continue
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Login Mode Selector */}
        <Box display="flex" mb={3}>
          <Button
            variant={loginMode === 'sso' ? 'contained' : 'outlined'}
            onClick={() => setLoginMode('sso')}
            fullWidth
            sx={{ mr: 1 }}
          >
            SSO Login
          </Button>
          <Button
            variant={loginMode === 'local' ? 'contained' : 'outlined'}
            onClick={() => setLoginMode('local')}
            fullWidth
            sx={{ ml: 1 }}
          >
            Local Login
          </Button>
        </Box>

        {loginMode === 'sso' ? (
          // SSO Login
          <Box>
            <FormControl fullWidth sx={{ mb: 3 }}>
              <InputLabel>Select Organization</InputLabel>
              <Select
                value={selectedOrg}
                onChange={(e) => setSelectedOrg(e.target.value)}
                label="Select Organization"
              >
                {ssoConfigs.map((config) => (
                  <MenuItem key={config.id} value={config.organizationId}>
                    <Box display="flex" alignItems="center">
                      <span style={{ marginRight: 8 }}>
                        {getProviderIcon(config.provider)}
                      </span>
                      {config.organizationName}
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {selectedOrg && (
              <Card variant="outlined" sx={{ mb: 3 }}>
                <CardContent>
                  {(() => {
                    const config = ssoConfigs.find(c => c.organizationId === selectedOrg);
                    return config ? (
                      <Box>
                        <Typography variant="body2" color="text.secondary">
                          You will be redirected to:
                        </Typography>
                        <Typography variant="body1">
                          {getProviderIcon(config.provider)} {config.provider.replace('_', ' ')}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {config.description}
                        </Typography>
                      </Box>
                    ) : null;
                  })()}
                </CardContent>
              </Card>
            )}

            <Button
              variant="contained"
              fullWidth
              onClick={handleSsoLogin}
              disabled={loading || !selectedOrg}
              startIcon={<BusinessIcon />}
              size="large"
            >
              {loading ? 'Redirecting...' : 'Continue with SSO'}
            </Button>

            {ssoConfigs.length === 0 && (
              <Alert severity="info" sx={{ mt: 2 }}>
                No SSO providers configured. Please contact your administrator.
              </Alert>
            )}
          </Box>
        ) : (
          // Local Login
          <Box>
            <TextField
              fullWidth
              label="Username"
              value={credentials.username}
              onChange={(e) => setCredentials({ ...credentials, username: e.target.value })}
              margin="normal"
              autoComplete="username"
            />
            <TextField
              fullWidth
              label="Password"
              type="password"
              value={credentials.password}
              onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
              margin="normal"
              autoComplete="current-password"
            />
            <Button
              variant="contained"
              fullWidth
              onClick={handleLocalLogin}
              disabled={loading}
              startIcon={<LoginIcon />}
              size="large"
              sx={{ mt: 2 }}
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </Button>
            
            <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mt: 2 }}>
              Demo: Use any username/password
            </Typography>
          </Box>
        )}

        <Divider sx={{ my: 3 }} />
        
        <Typography variant="body2" color="text.secondary" textAlign="center">
          File Transfer Management System v1.0
        </Typography>
      </Paper>
    </Box>
  );
};