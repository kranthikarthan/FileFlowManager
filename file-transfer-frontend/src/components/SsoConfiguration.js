import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  Card,
  CardContent,
  CardActions,
  Switch,
  FormControlLabel,
  Alert,
  Chip,
  IconButton,
  Tooltip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  TestTube as TestIcon,
  ExpandMore as ExpandMoreIcon,
  Security as SecurityIcon
} from '@mui/icons-material';
import { ssoAPI } from '../services/serviceAPI';

const SSO_PROVIDERS = [
  { value: 'AZURE_AD', label: 'Microsoft Azure AD', logo: '🔷' },
  { value: 'GOOGLE', label: 'Google Workspace', logo: '🔴' },
  { value: 'OKTA', label: 'Okta', logo: '🔵' },
  { value: 'KEYCLOAK', label: 'Keycloak', logo: '🔑' },
  { value: 'CUSTOM_OIDC', label: 'Custom OIDC', logo: '⚙️' },
  { value: 'SAML2', label: 'SAML 2.0', logo: '🛡️' }
];

export const SsoConfiguration = () => {
  const [ssoConfigs, setSsoConfigs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [open, setOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);
  const [showSecrets, setShowSecrets] = useState({});

  const [formData, setFormData] = useState({
    organizationId: '',
    organizationName: '',
    provider: 'AZURE_AD',
    clientId: '',
    clientSecret: '',
    issuerUri: '',
    authorizationUri: '',
    tokenUri: '',
    userInfoUri: '',
    jwkSetUri: '',
    redirectUri: '',
    scopes: 'openid,profile,email',
    attributesMapping: {},
    enabled: true,
    logoUrl: '',
    description: ''
  });

  const [attributeMapping, setAttributeMapping] = useState([
    { key: 'email', value: 'email' },
    { key: 'name', value: 'name' },
    { key: 'firstName', value: 'given_name' },
    { key: 'lastName', value: 'family_name' }
  ]);

  useEffect(() => {
    fetchSsoConfigurations();
  }, []);

  const fetchSsoConfigurations = async () => {
    try {
      setLoading(true);
      const response = await ssoAPI.getAllSsoConfigurations();
      setSsoConfigs(response.data);
    } catch (err) {
      setError('Failed to fetch SSO configurations');
      console.error('Error fetching SSO configs:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      // Convert attribute mapping array to object
      const mappingObject = {};
      attributeMapping.forEach(attr => {
        if (attr.key && attr.value) {
          mappingObject[attr.key] = attr.value;
        }
      });

      const configData = {
        ...formData,
        attributesMapping: mappingObject
      };

      if (editingConfig) {
        await ssoAPI.updateSsoConfiguration(editingConfig.id, configData);
        setSuccess('SSO configuration updated successfully');
      } else {
        await ssoAPI.createSsoConfiguration(configData);
        setSuccess('SSO configuration created successfully');
      }
      
      handleClose();
      fetchSsoConfigurations();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save SSO configuration');
    }
  };

  const handleEdit = (config) => {
    setEditingConfig(config);
    setFormData({ ...config });
    
    // Convert attributes mapping object to array
    const mappingArray = Object.entries(config.attributesMapping || {}).map(([key, value]) => ({
      key, value
    }));
    setAttributeMapping(mappingArray.length > 0 ? mappingArray : [
      { key: 'email', value: 'email' },
      { key: 'name', value: 'name' },
      { key: 'firstName', value: 'given_name' },
      { key: 'lastName', value: 'family_name' }
    ]);
    
    setOpen(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this SSO configuration?')) {
      try {
        await ssoAPI.deleteSsoConfiguration(id);
        setSuccess('SSO configuration deleted successfully');
        fetchSsoConfigurations();
      } catch (err) {
        setError('Failed to delete SSO configuration');
      }
    }
  };

  const handleToggleStatus = async (id) => {
    try {
      await ssoAPI.toggleSsoConfigurationStatus(id);
      setSuccess('SSO configuration status updated');
      fetchSsoConfigurations();
    } catch (err) {
      setError('Failed to update SSO configuration status');
    }
  };

  const handleTestConnection = async (id) => {
    try {
      await ssoAPI.testSsoConnection(id);
      setSuccess('SSO connection test successful');
    } catch (err) {
      setError('SSO connection test failed');
    }
  };

  const handleClose = () => {
    setOpen(false);
    setEditingConfig(null);
    setFormData({
      organizationId: '',
      organizationName: '',
      provider: 'AZURE_AD',
      clientId: '',
      clientSecret: '',
      issuerUri: '',
      authorizationUri: '',
      tokenUri: '',
      userInfoUri: '',
      jwkSetUri: '',
      redirectUri: '',
      scopes: 'openid,profile,email',
      attributesMapping: {},
      enabled: true,
      logoUrl: '',
      description: ''
    });
    setAttributeMapping([
      { key: 'email', value: 'email' },
      { key: 'name', value: 'name' },
      { key: 'firstName', value: 'given_name' },
      { key: 'lastName', value: 'family_name' }
    ]);
  };

  const addAttributeMapping = () => {
    setAttributeMapping([...attributeMapping, { key: '', value: '' }]);
  };

  const removeAttributeMapping = (index) => {
    setAttributeMapping(attributeMapping.filter((_, i) => i !== index));
  };

  const updateAttributeMapping = (index, field, value) => {
    const updated = [...attributeMapping];
    updated[index][field] = value;
    setAttributeMapping(updated);
  };

  const getProviderLabel = (provider) => {
    const providerConfig = SSO_PROVIDERS.find(p => p.value === provider);
    return providerConfig ? `${providerConfig.logo} ${providerConfig.label}` : provider;
  };

  const toggleSecretVisibility = (configId) => {
    setShowSecrets(prev => ({
      ...prev,
      [configId]: !prev[configId]
    }));
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          <SecurityIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
          SSO Configuration
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpen(true)}
        >
          Add SSO Provider
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Grid container spacing={3}>
        {ssoConfigs.map((config) => (
          <Grid item xs={12} md={6} lg={4} key={config.id}>
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">{config.organizationName}</Typography>
                  <Chip
                    label={config.enabled ? 'Enabled' : 'Disabled'}
                    color={config.enabled ? 'success' : 'default'}
                    size="small"
                  />
                </Box>

                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {getProviderLabel(config.provider)}
                </Typography>

                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {config.description || 'No description'}
                </Typography>

                <Box mb={2}>
                  <Typography variant="body2">
                    <strong>Organization ID:</strong> {config.organizationId}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Client ID:</strong> {config.clientId}
                  </Typography>
                  <Typography variant="body2" display="flex" alignItems="center">
                    <strong>Client Secret:</strong>
                    <Box ml={1} display="flex" alignItems="center">
                      {showSecrets[config.id] ? config.clientSecret : '••••••••'}
                      <IconButton
                        size="small"
                        onClick={() => toggleSecretVisibility(config.id)}
                        sx={{ ml: 0.5 }}
                      >
                        {showSecrets[config.id] ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                      </IconButton>
                    </Box>
                  </Typography>
                  {config.issuerUri && (
                    <Typography variant="body2">
                      <strong>Issuer URI:</strong> {config.issuerUri}
                    </Typography>
                  )}
                  <Typography variant="body2">
                    <strong>Scopes:</strong> {config.scopes}
                  </Typography>
                </Box>

                {/* Attribute Mapping */}
                {config.attributesMapping && Object.keys(config.attributesMapping).length > 0 && (
                  <Accordion>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                      <Typography variant="body2">Attribute Mapping</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                      <TableContainer>
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Local</TableCell>
                              <TableCell>Remote</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {Object.entries(config.attributesMapping).map(([key, value]) => (
                              <TableRow key={key}>
                                <TableCell>{key}</TableCell>
                                <TableCell>{value}</TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    </AccordionDetails>
                  </Accordion>
                )}
              </CardContent>

              <CardActions>
                <Button
                  size="small"
                  startIcon={<EditIcon />}
                  onClick={() => handleEdit(config)}
                >
                  Edit
                </Button>
                <Button
                  size="small"
                  startIcon={<TestIcon />}
                  color="primary"
                  onClick={() => handleTestConnection(config.id)}
                >
                  Test
                </Button>
                <Button
                  size="small"
                  color={config.enabled ? 'warning' : 'primary'}
                  onClick={() => handleToggleStatus(config.id)}
                >
                  {config.enabled ? 'Disable' : 'Enable'}
                </Button>
                <Button
                  size="small"
                  startIcon={<DeleteIcon />}
                  color="error"
                  onClick={() => handleDelete(config.id)}
                >
                  Delete
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* SSO Configuration Form Dialog */}
      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingConfig ? 'Edit SSO Configuration' : 'Add SSO Configuration'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Organization ID"
                value={formData.organizationId}
                onChange={(e) => setFormData({ ...formData, organizationId: e.target.value })}
                required
                helperText="Unique identifier for the organization"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Organization Name"
                value={formData.organizationName}
                onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormControl fullWidth required>
                <InputLabel>SSO Provider</InputLabel>
                <Select
                  value={formData.provider}
                  onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                  label="SSO Provider"
                >
                  {SSO_PROVIDERS.map((provider) => (
                    <MenuItem key={provider.value} value={provider.value}>
                      {provider.logo} {provider.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.enabled}
                    onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                  />
                }
                label="Enabled"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Client ID"
                value={formData.clientId}
                onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Client Secret"
                type="password"
                value={formData.clientSecret}
                onChange={(e) => setFormData({ ...formData, clientSecret: e.target.value })}
                required
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Issuer URI"
                value={formData.issuerUri}
                onChange={(e) => setFormData({ ...formData, issuerUri: e.target.value })}
                helperText="The issuer URI of the OAuth 2.0 provider"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Authorization URI"
                value={formData.authorizationUri}
                onChange={(e) => setFormData({ ...formData, authorizationUri: e.target.value })}
                helperText="Required for Custom OIDC"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Token URI"
                value={formData.tokenUri}
                onChange={(e) => setFormData({ ...formData, tokenUri: e.target.value })}
                helperText="Required for Custom OIDC"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="User Info URI"
                value={formData.userInfoUri}
                onChange={(e) => setFormData({ ...formData, userInfoUri: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="JWK Set URI"
                value={formData.jwkSetUri}
                onChange={(e) => setFormData({ ...formData, jwkSetUri: e.target.value })}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Redirect URI"
                value={formData.redirectUri}
                onChange={(e) => setFormData({ ...formData, redirectUri: e.target.value })}
                helperText="The redirect URI configured in your SSO provider"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Scopes"
                value={formData.scopes}
                onChange={(e) => setFormData({ ...formData, scopes: e.target.value })}
                helperText="Comma-separated list of OAuth scopes"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Logo URL"
                value={formData.logoUrl}
                onChange={(e) => setFormData({ ...formData, logoUrl: e.target.value })}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={2}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>

            {/* Attribute Mapping */}
            <Grid item xs={12}>
              <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                Attribute Mapping
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Map SSO provider attributes to local user attributes
              </Typography>

              {attributeMapping.map((attr, index) => (
                <Grid container spacing={2} key={index} sx={{ mb: 1 }}>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Local Attribute"
                      value={attr.key}
                      onChange={(e) => updateAttributeMapping(index, 'key', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Remote Attribute"
                      value={attr.value}
                      onChange={(e) => updateAttributeMapping(index, 'value', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={2}>
                    <Button
                      size="small"
                      color="error"
                      onClick={() => removeAttributeMapping(index)}
                      disabled={attributeMapping.length <= 1}
                    >
                      Remove
                    </Button>
                  </Grid>
                </Grid>
              ))}

              <Button
                size="small"
                onClick={addAttributeMapping}
                sx={{ mt: 1 }}
              >
                Add Mapping
              </Button>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} startIcon={<CancelIcon />}>
            Cancel
          </Button>
          <Button 
            onClick={handleSubmit} 
            variant="contained" 
            startIcon={<SaveIcon />}
            disabled={!formData.organizationId || !formData.organizationName || !formData.clientId || !formData.clientSecret}
          >
            {editingConfig ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};