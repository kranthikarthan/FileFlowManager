import React, { useState, useEffect } from 'react';
import {
  Box,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  Button,
  Typography,
  TextField,
  Grid,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
  Chip,
  Alert,
  Autocomplete,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  LinearProgress,
  Collapse,
  IconButton,
  Tooltip,
  useMediaQuery,
  useTheme as useMuiTheme,
} from '@mui/material';
import {
  Business as BusinessIcon,
  Security as SecurityIcon,
  Schedule as ScheduleIcon,
  AccountTree as ServiceIcon,
  Schema as SchemaIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  ExpandMore as ExpandIcon,
  ExpandLess as CollapseIcon,
  Rocket as LaunchIcon,
} from '@mui/icons-material';
import { useTheme } from '../../theme/themeProvider';

const TenantSetupWizard = ({ open, onClose, onComplete }) => {
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));
  
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [setupData, setSetupData] = useState({
    // Step 1: Basic Tenant Info
    tenant: {
      tenantId: '',
      name: '',
      description: '',
      contactEmail: '',
      timezone: 'UTC',
      isActive: true,
    },
    // Step 2: SSO Configuration
    sso: {
      enabled: false,
      provider: 'AZURE_AD',
      clientId: '',
      clientSecret: '',
      tenantId: '',
      redirectUri: '',
      scope: 'openid profile email',
    },
    // Step 3: Holiday Calendar
    holidays: [],
    // Step 4: Services
    services: [],
    // Step 5: Schemas
    schemas: [],
    // Step 6: Notification Settings
    notifications: {
      emailEnabled: true,
      alertsEnabled: true,
      dailyReports: true,
      discrepancyAlerts: true,
    },
  });

  const steps = [
    {
      label: 'Basic Information',
      description: 'Set up tenant details and basic configuration',
      icon: <BusinessIcon />,
      content: 'tenant',
    },
    {
      label: 'Security & SSO',
      description: 'Configure authentication and security settings',
      icon: <SecurityIcon />,
      content: 'sso',
    },
    {
      label: 'Holiday Calendar',
      description: 'Set up business holidays and working days',
      icon: <ScheduleIcon />,
      content: 'holidays',
    },
    {
      label: 'Services Setup',
      description: 'Create services and sub-services',
      icon: <ServiceIcon />,
      content: 'services',
    },
    {
      label: 'Schema Library',
      description: 'Set up shared schemas for file validation',
      icon: <SchemaIcon />,
      content: 'schemas',
    },
    {
      label: 'Complete Setup',
      description: 'Review and finalize configuration',
      icon: <CheckIcon />,
      content: 'review',
    },
  ];

  const timezones = [
    'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
    'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Asia/Tokyo', 'Asia/Shanghai',
    'Asia/Kolkata', 'Australia/Sydney'
  ];

  const handleNext = async () => {
    if (validateCurrentStep()) {
      if (activeStep === steps.length - 1) {
        await handleComplete();
      } else {
        setActiveStep(prev => prev + 1);
      }
    }
  };

  const handleBack = () => {
    setActiveStep(prev => prev - 1);
  };

  const validateCurrentStep = () => {
    const newErrors = {};
    
    switch (activeStep) {
      case 0: // Basic Information
        if (!setupData.tenant.tenantId) newErrors.tenantId = 'Tenant ID is required';
        if (!setupData.tenant.name) newErrors.name = 'Tenant name is required';
        if (!setupData.tenant.contactEmail) newErrors.contactEmail = 'Contact email is required';
        break;
      case 1: // SSO Configuration
        if (setupData.sso.enabled) {
          if (!setupData.sso.clientId) newErrors.clientId = 'Client ID is required';
          if (!setupData.sso.tenantId) newErrors.ssoTenantId = 'SSO Tenant ID is required';
        }
        break;
      // Other steps are optional or validated differently
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleComplete = async () => {
    setLoading(true);
    try {
      // Create tenant and all sub-components
      await createTenantComplete();
      onComplete?.(setupData);
      onClose();
    } catch (error) {
      console.error('Setup failed:', error);
      setErrors({ general: 'Setup failed. Please try again.' });
    } finally {
      setLoading(false);
    }
  };

  const createTenantComplete = async () => {
    // Simulate API calls with progress tracking
    const steps = [
      'Creating tenant...',
      'Setting up SSO...',
      'Adding holidays...',
      'Creating services...',
      'Setting up schemas...',
      'Finalizing configuration...',
    ];
    
    for (let i = 0; i < steps.length; i++) {
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
    }
  };

  const addService = () => {
    const newService = {
      id: Date.now(),
      name: '',
      description: '',
      enabled: true,
      subServices: [],
    };
    setSetupData(prev => ({
      ...prev,
      services: [...prev.services, newService],
    }));
  };

  const addSubService = (serviceIndex) => {
    const newSubService = {
      id: Date.now(),
      name: '',
      description: '',
      direction: 'INBOUND',
      fileType: 'JSON',
      enabled: true,
    };
    
    setSetupData(prev => {
      const newServices = [...prev.services];
      newServices[serviceIndex].subServices.push(newSubService);
      return { ...prev, services: newServices };
    });
  };

  const updateService = (serviceIndex, field, value) => {
    setSetupData(prev => {
      const newServices = [...prev.services];
      newServices[serviceIndex][field] = value;
      return { ...prev, services: newServices };
    });
  };

  const updateSubService = (serviceIndex, subServiceIndex, field, value) => {
    setSetupData(prev => {
      const newServices = [...prev.services];
      newServices[serviceIndex].subServices[subServiceIndex][field] = value;
      return { ...prev, services: newServices };
    });
  };

  const addSchema = () => {
    const newSchema = {
      id: Date.now(),
      name: '',
      version: '1.0',
      type: 'JSON_SCHEMA',
      fileType: 'JSON',
      definition: '',
      description: '',
      supportsCountValidation: false,
    };
    setSetupData(prev => ({
      ...prev,
      schemas: [...prev.schemas, newSchema],
    }));
  };

  const renderStepContent = (stepIndex) => {
    switch (stepIndex) {
      case 0:
        return renderBasicInfoStep();
      case 1:
        return renderSSOStep();
      case 2:
        return renderHolidaysStep();
      case 3:
        return renderServicesStep();
      case 4:
        return renderSchemasStep();
      case 5:
        return renderReviewStep();
      default:
        return null;
    }
  };

  const renderBasicInfoStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info" icon={<InfoIcon />}>
          Let's start by setting up your tenant's basic information. This will be used throughout the system.
        </Alert>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Tenant ID"
          value={setupData.tenant.tenantId}
          onChange={(e) => setSetupData(prev => ({
            ...prev,
            tenant: { ...prev.tenant, tenantId: e.target.value.toUpperCase() }
          }))}
          error={!!errors.tenantId}
          helperText={errors.tenantId || 'Unique identifier for your organization'}
          placeholder="ACME_CORP"
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Organization Name"
          value={setupData.tenant.name}
          onChange={(e) => setSetupData(prev => ({
            ...prev,
            tenant: { ...prev.tenant, name: e.target.value }
          }))}
          error={!!errors.name}
          helperText={errors.name || 'Display name for your organization'}
          placeholder="ACME Corporation"
        />
      </Grid>
      
      <Grid item xs={12}>
        <TextField
          fullWidth
          multiline
          rows={3}
          label="Description"
          value={setupData.tenant.description}
          onChange={(e) => setSetupData(prev => ({
            ...prev,
            tenant: { ...prev.tenant, description: e.target.value }
          }))}
          helperText="Brief description of your organization or project"
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          type="email"
          label="Contact Email"
          value={setupData.tenant.contactEmail}
          onChange={(e) => setSetupData(prev => ({
            ...prev,
            tenant: { ...prev.tenant, contactEmail: e.target.value }
          }))}
          error={!!errors.contactEmail}
          helperText={errors.contactEmail || 'Primary contact for this tenant'}
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>Timezone</InputLabel>
          <Select
            value={setupData.tenant.timezone}
            onChange={(e) => setSetupData(prev => ({
              ...prev,
              tenant: { ...prev.tenant, timezone: e.target.value }
            }))}
            label="Timezone"
          >
            {timezones.map(tz => (
              <MenuItem key={tz} value={tz}>{tz}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      
      <Grid item xs={12}>
        <FormControlLabel
          control={
            <Switch
              checked={setupData.tenant.isActive}
              onChange={(e) => setSetupData(prev => ({
                ...prev,
                tenant: { ...prev.tenant, isActive: e.target.checked }
              }))}
            />
          }
          label="Activate tenant immediately"
        />
      </Grid>
    </Grid>
  );

  const renderSSOStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Configure Single Sign-On (SSO) for secure authentication. This is optional but recommended for enterprise use.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <FormControlLabel
          control={
            <Switch
              checked={setupData.sso.enabled}
              onChange={(e) => setSetupData(prev => ({
                ...prev,
                sso: { ...prev.sso, enabled: e.target.checked }
              }))}
            />
          }
          label="Enable SSO Authentication"
        />
      </Grid>
      
      <Collapse in={setupData.sso.enabled}>
        <Grid container spacing={3} sx={{ mt: 1 }}>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>SSO Provider</InputLabel>
              <Select
                value={setupData.sso.provider}
                onChange={(e) => setSetupData(prev => ({
                  ...prev,
                  sso: { ...prev.sso, provider: e.target.value }
                }))}
                label="SSO Provider"
              >
                <MenuItem value="AZURE_AD">Azure Active Directory</MenuItem>
                <MenuItem value="GOOGLE">Google Workspace</MenuItem>
                <MenuItem value="OKTA">Okta</MenuItem>
                <MenuItem value="SAML">Generic SAML</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Client ID"
              value={setupData.sso.clientId}
              onChange={(e) => setSetupData(prev => ({
                ...prev,
                sso: { ...prev.sso, clientId: e.target.value }
              }))}
              error={!!errors.clientId}
              helperText={errors.clientId}
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Client Secret"
              type="password"
              value={setupData.sso.clientSecret}
              onChange={(e) => setSetupData(prev => ({
                ...prev,
                sso: { ...prev.sso, clientSecret: e.target.value }
              }))}
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="SSO Tenant ID"
              value={setupData.sso.tenantId}
              onChange={(e) => setSetupData(prev => ({
                ...prev,
                sso: { ...prev.sso, tenantId: e.target.value }
              }))}
              error={!!errors.ssoTenantId}
              helperText={errors.ssoTenantId}
            />
          </Grid>
        </Grid>
      </Collapse>
    </Grid>
  );

  const renderHolidaysStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Set up your organization's holiday calendar. This affects cut-off times and processing schedules.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>
          Holiday Calendar Setup
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          You can add holidays individually or import from a standard calendar. Common holidays will be suggested based on your timezone.
        </Typography>
        
        <Button
          variant="outlined"
          startIcon={<AddIcon />}
          onClick={() => {
            // Add holiday logic here
          }}
          sx={{ mr: 2 }}
        >
          Add Holiday
        </Button>
        
        <Button
          variant="outlined"
          onClick={() => {
            // Import holidays logic here
          }}
        >
          Import Standard Holidays
        </Button>
      </Grid>
      
      {setupData.holidays.length > 0 && (
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Configured Holidays ({setupData.holidays.length})
              </Typography>
              {/* Holiday list would go here */}
            </CardContent>
          </Card>
        </Grid>
      )}
    </Grid>
  );

  const renderServicesStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Create services and sub-services for your file transfer operations. Each service can have multiple sub-services with different configurations.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Services ({setupData.services.length})
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={addService}
          >
            Add Service
          </Button>
        </Box>
        
        {setupData.services.map((service, serviceIndex) => (
          <Card key={service.id} sx={{ mb: 2 }}>
            <CardContent>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Service Name"
                    value={service.name}
                    onChange={(e) => updateService(serviceIndex, 'name', e.target.value)}
                    placeholder="PAYMENT_SERVICE"
                  />
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Description"
                    value={service.description}
                    onChange={(e) => updateService(serviceIndex, 'description', e.target.value)}
                    placeholder="Payment processing service"
                  />
                </Grid>
                
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle2">
                      Sub-Services ({service.subServices.length})
                    </Typography>
                    <Button
                      size="small"
                      startIcon={<AddIcon />}
                      onClick={() => addSubService(serviceIndex)}
                    >
                      Add Sub-Service
                    </Button>
                  </Box>
                  
                  {service.subServices.map((subService, subIndex) => (
                    <Box key={subService.id} sx={{ ml: 2, mt: 1, p: 2, border: '1px dashed', borderColor: 'divider', borderRadius: 1 }}>
                      <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                          <TextField
                            fullWidth
                            size="small"
                            label="Sub-Service Name"
                            value={subService.name}
                            onChange={(e) => updateSubService(serviceIndex, subIndex, 'name', e.target.value)}
                          />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                          <FormControl fullWidth size="small">
                            <InputLabel>Direction</InputLabel>
                            <Select
                              value={subService.direction}
                              onChange={(e) => updateSubService(serviceIndex, subIndex, 'direction', e.target.value)}
                              label="Direction"
                            >
                              <MenuItem value="INBOUND">Inbound</MenuItem>
                              <MenuItem value="OUTBOUND">Outbound</MenuItem>
                            </Select>
                          </FormControl>
                        </Grid>
                      </Grid>
                    </Box>
                  ))}
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        ))}
        
        {setupData.services.length === 0 && (
          <Card sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <ServiceIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No Services Yet
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Add your first service to get started with file transfer operations
              </Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={addService}>
                Create First Service
              </Button>
            </CardContent>
          </Card>
        )}
      </Grid>
    </Grid>
  );

  const renderSchemasStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Set up shared schemas for file validation. These can be reused across multiple services and directions.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Shared Schemas ({setupData.schemas.length})
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={addSchema}
          >
            Add Schema
          </Button>
        </Box>
        
        {setupData.schemas.length === 0 && (
          <Card sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <SchemaIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No Schemas Yet
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Schemas are optional but recommended for file validation
              </Typography>
              <Button variant="outlined" startIcon={<AddIcon />} onClick={addSchema}>
                Add First Schema
              </Button>
            </CardContent>
          </Card>
        )}
      </Grid>
    </Grid>
  );

  const renderReviewStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="success" icon={<CheckIcon />}>
          Great! Your tenant setup is ready. Review the configuration below and click "Complete Setup" to finish.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom color="primary">
              Setup Summary
            </Typography>
            
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" gutterBottom>Tenant Information</Typography>
                <Typography variant="body2">ID: {setupData.tenant.tenantId}</Typography>
                <Typography variant="body2">Name: {setupData.tenant.name}</Typography>
                <Typography variant="body2">Timezone: {setupData.tenant.timezone}</Typography>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" gutterBottom>Configuration</Typography>
                <Chip 
                  label={setupData.sso.enabled ? "SSO Enabled" : "SSO Disabled"} 
                  color={setupData.sso.enabled ? "success" : "default"}
                  size="small"
                  sx={{ mr: 1, mb: 1 }}
                />
                <Chip 
                  label={`${setupData.services.length} Services`}
                  color="primary"
                  size="small"
                  sx={{ mr: 1, mb: 1 }}
                />
                <Chip 
                  label={`${setupData.schemas.length} Schemas`}
                  color="secondary"
                  size="small"
                  sx={{ mr: 1, mb: 1 }}
                />
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      </Grid>
      
      {loading && (
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Creating Your Tenant...
              </Typography>
              <LinearProgress sx={{ mt: 2 }} />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                This may take a few moments to complete.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      )}
    </Grid>
  );

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      fullScreen={isMobile}
      PaperProps={{
        sx: {
          borderRadius: isMobile ? 0 : 3,
          background: isDark 
            ? 'rgba(13, 27, 13, 0.98)'
            : 'rgba(248, 253, 248, 0.98)',
          backdropFilter: 'blur(20px)',
        },
      }}
    >
      <DialogTitle sx={{ 
        background: isDark
          ? 'linear-gradient(135deg, rgba(129, 199, 132, 0.1) 0%, rgba(165, 214, 167, 0.1) 100%)'
          : 'linear-gradient(135deg, rgba(46, 125, 50, 0.1) 0%, rgba(76, 175, 80, 0.1) 100%)',
        borderBottom: '1px solid',
        borderColor: 'divider',
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <LaunchIcon color="primary" />
          <Typography variant="h5" fontWeight={600}>
            Tenant Setup Wizard
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Complete guided setup for your new tenant
        </Typography>
      </DialogTitle>

      <DialogContent sx={{ p: 3 }}>
        <Stepper 
          activeStep={activeStep} 
          orientation={isMobile ? "vertical" : "horizontal"}
          sx={{ mb: 4 }}
        >
          {steps.map((step, index) => (
            <Step key={step.label}>
              <StepLabel
                StepIconComponent={() => (
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      background: index <= activeStep 
                        ? `linear-gradient(135deg, ${isDark ? '#81c784' : '#2e7d32'} 0%, ${isDark ? '#a5d6a7' : '#4caf50'} 100%)`
                        : isDark ? 'rgba(74, 92, 74, 0.3)' : 'rgba(200, 230, 201, 0.3)',
                      color: index <= activeStep ? 'white' : 'text.secondary',
                      border: index === activeStep ? '2px solid' : 'none',
                      borderColor: 'primary.main',
                    }}
                  >
                    {React.cloneElement(step.icon, { 
                      sx: { fontSize: 20 } 
                    })}
                  </Box>
                )}
              >
                <Box>
                  <Typography variant="subtitle2" fontWeight={600}>
                    {step.label}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {step.description}
                  </Typography>
                </Box>
              </StepLabel>
              {isMobile && (
                <StepContent>
                  {renderStepContent(index)}
                </StepContent>
              )}
            </Step>
          ))}
        </Stepper>

        {!isMobile && (
          <Box sx={{ mt: 4 }}>
            {renderStepContent(activeStep)}
          </Box>
        )}

        {errors.general && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {errors.general}
          </Alert>
        )}
      </DialogContent>

      <DialogActions sx={{ 
        p: 3, 
        borderTop: '1px solid', 
        borderColor: 'divider',
        background: isDark 
          ? 'rgba(26, 46, 26, 0.5)'
          : 'rgba(232, 245, 232, 0.5)',
      }}>
        <Button onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button
          disabled={activeStep === 0 || loading}
          onClick={handleBack}
          sx={{ mr: 1 }}
        >
          Back
        </Button>
        <Button
          variant="contained"
          onClick={handleNext}
          disabled={loading}
          startIcon={activeStep === steps.length - 1 ? <LaunchIcon /> : null}
        >
          {activeStep === steps.length - 1 ? 'Complete Setup' : 'Next'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default TenantSetupWizard;