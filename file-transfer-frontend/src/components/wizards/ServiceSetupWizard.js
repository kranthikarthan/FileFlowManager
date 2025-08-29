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
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Avatar,
} from '@mui/material';
import {
  AccountTree as ServiceIcon,
  Schema as SchemaIcon,
  Schedule as ScheduleIcon,
  Folder as DirectoryIcon,
  Security as SecurityIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  ExpandMore as ExpandIcon,
  ExpandLess as CollapseIcon,
  Rocket as LaunchIcon,
  Storage as StorageIcon,
  Transform as TransformIcon,
  Notifications as NotificationIcon,
  Timeline as FlowIcon,
} from '@mui/icons-material';
import { useTheme } from '../../theme/themeProvider';

const ServiceSetupWizard = ({ open, onClose, onComplete, tenantId }) => {
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));
  
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [availableSchemas, setAvailableSchemas] = useState([]);
  
  const [serviceData, setServiceData] = useState({
    // Step 1: Basic Service Info
    service: {
      serviceName: '',
      description: '',
      category: 'PAYMENT',
      enabled: true,
      priority: 'MEDIUM',
    },
    // Step 2: Sub-Services
    subServices: [],
    // Step 3: File Processing Configuration
    fileProcessing: {
      maxFileSize: '100MB',
      allowedFormats: ['JSON', 'XML', 'CSV'],
      encryptionRequired: false,
      compressionEnabled: false,
      retentionDays: 90,
    },
    // Step 4: Schema Assignments
    schemaAssignments: [],
    // Step 5: Cut-off Times
    cutOffTimes: [],
    // Step 6: Notifications & Alerts
    notifications: {
      successNotifications: true,
      errorNotifications: true,
      delayAlerts: true,
      emailRecipients: [],
      slackWebhook: '',
    },
  });

  const steps = [
    {
      label: 'Service Details',
      description: 'Define the service and its basic configuration',
      icon: <ServiceIcon />,
      content: 'service',
    },
    {
      label: 'Sub-Services',
      description: 'Create sub-services with directions and file types',
      icon: <FlowIcon />,
      content: 'subServices',
    },
    {
      label: 'File Processing',
      description: 'Configure file handling and processing rules',
      icon: <StorageIcon />,
      content: 'fileProcessing',
    },
    {
      label: 'Schema Assignment',
      description: 'Link schemas to sub-services for validation',
      icon: <SchemaIcon />,
      content: 'schemaAssignments',
    },
    {
      label: 'Cut-off Times',
      description: 'Set processing deadlines and schedules',
      icon: <ScheduleIcon />,
      content: 'cutOffTimes',
    },
    {
      label: 'Notifications',
      description: 'Configure alerts and monitoring',
      icon: <NotificationIcon />,
      content: 'notifications',
    },
    {
      label: 'Complete Setup',
      description: 'Review and finalize service configuration',
      icon: <CheckIcon />,
      content: 'review',
    },
  ];

  const serviceCategories = [
    'PAYMENT', 'TRADE', 'REPORTING', 'COMPLIANCE', 'INTEGRATION', 'ANALYTICS', 'OTHER'
  ];

  const fileSizes = [
    '10MB', '50MB', '100MB', '500MB', '1GB', '5GB'
  ];

  const fileTypes = [
    'JSON', 'XML', 'CSV', 'COBOL_FLAT_FILE', 'FIXED_WIDTH', 'BINARY_FILE', 'TEXT'
  ];

  const directions = ['INBOUND', 'OUTBOUND'];

  const priorities = [
    { value: 'LOW', label: 'Low Priority', color: '#81c784' },
    { value: 'MEDIUM', label: 'Medium Priority', color: '#ffb74d' },
    { value: 'HIGH', label: 'High Priority', color: '#f44336' },
    { value: 'CRITICAL', label: 'Critical Priority', color: '#9c27b0' }
  ];

  useEffect(() => {
    if (open && tenantId) {
      loadAvailableSchemas();
    }
  }, [open, tenantId]);

  const loadAvailableSchemas = async () => {
    try {
      // Simulate API call to load available schemas
      const mockSchemas = [
        { id: 1, name: 'Payment_JSON_v1', type: 'JSON_SCHEMA', fileType: 'JSON', supportsCountValidation: true },
        { id: 2, name: 'Trade_XML_v2', type: 'XML_SCHEMA', fileType: 'XML', supportsCountValidation: false },
        { id: 3, name: 'Report_CSV_v1', type: 'CSV_DEFINITION', fileType: 'CSV', supportsCountValidation: true },
      ];
      setAvailableSchemas(mockSchemas);
    } catch (error) {
      console.error('Failed to load schemas:', error);
    }
  };

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
      case 0: // Service Details
        if (!serviceData.service.serviceName) newErrors.serviceName = 'Service name is required';
        break;
      case 1: // Sub-Services
        if (serviceData.subServices.length === 0) {
          newErrors.subServices = 'At least one sub-service is required';
        }
        break;
      // Other validations...
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleComplete = async () => {
    setLoading(true);
    try {
      await createServiceComplete();
      onComplete?.(serviceData);
      onClose();
    } catch (error) {
      console.error('Service setup failed:', error);
      setErrors({ general: 'Service setup failed. Please try again.' });
    } finally {
      setLoading(false);
    }
  };

  const createServiceComplete = async () => {
    // Simulate progressive service creation
    const steps = [
      'Creating service configuration...',
      'Setting up sub-services...',
      'Configuring file processing...',
      'Assigning schemas...',
      'Setting cut-off times...',
      'Configuring notifications...',
      'Finalizing setup...',
    ];
    
    for (let i = 0; i < steps.length; i++) {
      await new Promise(resolve => setTimeout(resolve, 800));
    }
  };

  const addSubService = () => {
    const newSubService = {
      id: Date.now(),
      subServiceName: '',
      description: '',
      direction: 'INBOUND',
      fileType: 'JSON',
      enabled: true,
      inboundPath: '',
      outboundPath: '',
      schemaValidationEnabled: true,
      eotValidationEnabled: false,
    };
    
    setServiceData(prev => ({
      ...prev,
      subServices: [...prev.subServices, newSubService],
    }));
  };

  const updateSubService = (index, field, value) => {
    setServiceData(prev => {
      const newSubServices = [...prev.subServices];
      newSubServices[index][field] = value;
      return { ...prev, subServices: newSubServices };
    });
  };

  const removeSubService = (index) => {
    setServiceData(prev => ({
      ...prev,
      subServices: prev.subServices.filter((_, i) => i !== index),
    }));
  };

  const addCutOffTime = () => {
    const newCutOff = {
      id: Date.now(),
      subServiceName: '',
      cutOffTime: '18:00',
      timeZone: 'UTC',
      type: 'DAILY',
      enabled: true,
    };
    
    setServiceData(prev => ({
      ...prev,
      cutOffTimes: [...prev.cutOffTimes, newCutOff],
    }));
  };

  const addSchemaAssignment = () => {
    const newAssignment = {
      id: Date.now(),
      subServiceName: '',
      schemaId: '',
      direction: 'INBOUND',
      isPrimary: true,
      validationEnabled: true,
    };
    
    setServiceData(prev => ({
      ...prev,
      schemaAssignments: [...prev.schemaAssignments, newAssignment],
    }));
  };

  const renderStepContent = (stepIndex) => {
    switch (stepIndex) {
      case 0:
        return renderServiceDetailsStep();
      case 1:
        return renderSubServicesStep();
      case 2:
        return renderFileProcessingStep();
      case 3:
        return renderSchemaAssignmentStep();
      case 4:
        return renderCutOffTimesStep();
      case 5:
        return renderNotificationsStep();
      case 6:
        return renderReviewStep();
      default:
        return null;
    }
  };

  const renderServiceDetailsStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info" icon={<InfoIcon />}>
          Let's start by defining your service. This will be the main container for related file transfer operations.
        </Alert>
      </Grid>
      
      <Grid item xs={12} md={8}>
        <TextField
          fullWidth
          label="Service Name"
          value={serviceData.service.serviceName}
          onChange={(e) => setServiceData(prev => ({
            ...prev,
            service: { ...prev.service, serviceName: e.target.value.toUpperCase().replace(/\s+/g, '_') }
          }))}
          error={!!errors.serviceName}
          helperText={errors.serviceName || 'Unique identifier for this service (e.g., PAYMENT_SERVICE)'}
          placeholder="PAYMENT_SERVICE"
        />
      </Grid>
      
      <Grid item xs={12} md={4}>
        <FormControl fullWidth>
          <InputLabel>Category</InputLabel>
          <Select
            value={serviceData.service.category}
            onChange={(e) => setServiceData(prev => ({
              ...prev,
              service: { ...prev.service, category: e.target.value }
            }))}
            label="Category"
          >
            {serviceCategories.map(category => (
              <MenuItem key={category} value={category}>{category}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      
      <Grid item xs={12}>
        <TextField
          fullWidth
          multiline
          rows={3}
          label="Description"
          value={serviceData.service.description}
          onChange={(e) => setServiceData(prev => ({
            ...prev,
            service: { ...prev.service, description: e.target.value }
          }))}
          helperText="Describe what this service handles and its business purpose"
          placeholder="Handles payment file transfers between internal systems and external partners"
        />
      </Grid>
      
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>Priority Level</InputLabel>
          <Select
            value={serviceData.service.priority}
            onChange={(e) => setServiceData(prev => ({
              ...prev,
              service: { ...prev.service, priority: e.target.value }
            }))}
            label="Priority Level"
          >
            {priorities.map(priority => (
              <MenuItem key={priority.value} value={priority.value}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box
                    sx={{
                      width: 12,
                      height: 12,
                      borderRadius: '50%',
                      backgroundColor: priority.color,
                    }}
                  />
                  {priority.label}
                </Box>
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <FormControlLabel
          control={
            <Switch
              checked={serviceData.service.enabled}
              onChange={(e) => setServiceData(prev => ({
                ...prev,
                service: { ...prev.service, enabled: e.target.checked }
              }))}
            />
          }
          label="Enable service immediately"
        />
      </Grid>
    </Grid>
  );

  const renderSubServicesStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Sub-services represent specific file transfer flows within your service. Each can have different directions, file types, and configurations.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Sub-Services ({serviceData.subServices.length})
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={addSubService}
          >
            Add Sub-Service
          </Button>
        </Box>
        
        {errors.subServices && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errors.subServices}
          </Alert>
        )}
        
        {serviceData.subServices.map((subService, index) => (
          <Accordion key={subService.id} defaultExpanded sx={{ mb: 2 }}>
            <AccordionSummary expandIcon={<ExpandIcon />}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flex: 1 }}>
                <Avatar sx={{ 
                  bgcolor: isDark ? 'primary.main' : 'primary.light',
                  width: 32, 
                  height: 32 
                }}>
                  {index + 1}
                </Avatar>
                <Box>
                  <Typography variant="subtitle1" fontWeight={600}>
                    {subService.subServiceName || `Sub-Service ${index + 1}`}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {subService.direction} • {subService.fileType}
                  </Typography>
                </Box>
                <Box sx={{ ml: 'auto' }}>
                  <Chip
                    label={subService.enabled ? 'Enabled' : 'Disabled'}
                    color={subService.enabled ? 'success' : 'default'}
                    size="small"
                  />
                </Box>
              </Box>
            </AccordionSummary>
            
            <AccordionDetails>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Sub-Service Name"
                    value={subService.subServiceName}
                    onChange={(e) => updateSubService(index, 'subServiceName', e.target.value.toUpperCase().replace(/\s+/g, '_'))}
                    placeholder="INBOUND_PAYMENTS"
                  />
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Description"
                    value={subService.description}
                    onChange={(e) => updateSubService(index, 'description', e.target.value)}
                    placeholder="Processes inbound payment files"
                  />
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <FormControl fullWidth>
                    <InputLabel>Direction</InputLabel>
                    <Select
                      value={subService.direction}
                      onChange={(e) => updateSubService(index, 'direction', e.target.value)}
                      label="Direction"
                    >
                      {directions.map(direction => (
                        <MenuItem key={direction} value={direction}>{direction}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <FormControl fullWidth>
                    <InputLabel>File Type</InputLabel>
                    <Select
                      value={subService.fileType}
                      onChange={(e) => updateSubService(index, 'fileType', e.target.value)}
                      label="File Type"
                    >
                      {fileTypes.map(type => (
                        <MenuItem key={type} value={type}>{type}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={subService.enabled}
                          onChange={(e) => updateSubService(index, 'enabled', e.target.checked)}
                        />
                      }
                      label="Enabled"
                    />
                    <IconButton
                      color="error"
                      onClick={() => removeSubService(index)}
                      disabled={serviceData.subServices.length === 1}
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Box>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Inbound Path"
                    value={subService.inboundPath}
                    onChange={(e) => updateSubService(index, 'inboundPath', e.target.value)}
                    placeholder="/data/inbound/payments"
                    helperText="Directory path for incoming files"
                  />
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Outbound Path"
                    value={subService.outboundPath}
                    onChange={(e) => updateSubService(index, 'outboundPath', e.target.value)}
                    placeholder="/data/outbound/payments"
                    helperText="Directory path for processed files"
                  />
                </Grid>
                
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={subService.schemaValidationEnabled}
                          onChange={(e) => updateSubService(index, 'schemaValidationEnabled', e.target.checked)}
                        />
                      }
                      label="Schema Validation"
                    />
                    <FormControlLabel
                      control={
                        <Switch
                          checked={subService.eotValidationEnabled}
                          onChange={(e) => updateSubService(index, 'eotValidationEnabled', e.target.checked)}
                        />
                      }
                      label="EOT Count Validation"
                    />
                  </Box>
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>
        ))}
        
        {serviceData.subServices.length === 0 && (
          <Card sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <FlowIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No Sub-Services Yet
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Add your first sub-service to define file transfer flows
              </Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={addSubService}>
                Create First Sub-Service
              </Button>
            </CardContent>
          </Card>
        )}
      </Grid>
    </Grid>
  );

  const renderFileProcessingStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Configure how files are processed, including size limits, formats, and retention policies.
        </Alert>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>Maximum File Size</InputLabel>
          <Select
            value={serviceData.fileProcessing.maxFileSize}
            onChange={(e) => setServiceData(prev => ({
              ...prev,
              fileProcessing: { ...prev.fileProcessing, maxFileSize: e.target.value }
            }))}
            label="Maximum File Size"
          >
            {fileSizes.map(size => (
              <MenuItem key={size} value={size}>{size}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          type="number"
          label="Retention Days"
          value={serviceData.fileProcessing.retentionDays}
          onChange={(e) => setServiceData(prev => ({
            ...prev,
            fileProcessing: { ...prev.fileProcessing, retentionDays: parseInt(e.target.value) }
          }))}
          helperText="How long to keep processed files"
        />
      </Grid>
      
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>
          Allowed File Formats
        </Typography>
        <Autocomplete
          multiple
          options={fileTypes}
          value={serviceData.fileProcessing.allowedFormats}
          onChange={(event, newValue) => {
            setServiceData(prev => ({
              ...prev,
              fileProcessing: { ...prev.fileProcessing, allowedFormats: newValue }
            }));
          }}
          renderTags={(value, getTagProps) =>
            value.map((option, index) => (
              <Chip variant="outlined" label={option} {...getTagProps({ index })} />
            ))
          }
          renderInput={(params) => (
            <TextField
              {...params}
              placeholder="Select file formats"
              helperText="Choose which file types this service will accept"
            />
          )}
        />
      </Grid>
      
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>
          Processing Options
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          <FormControlLabel
            control={
              <Switch
                checked={serviceData.fileProcessing.encryptionRequired}
                onChange={(e) => setServiceData(prev => ({
                  ...prev,
                  fileProcessing: { ...prev.fileProcessing, encryptionRequired: e.target.checked }
                }))}
              />
            }
            label="Require file encryption"
          />
          <FormControlLabel
            control={
              <Switch
                checked={serviceData.fileProcessing.compressionEnabled}
                onChange={(e) => setServiceData(prev => ({
                  ...prev,
                  fileProcessing: { ...prev.fileProcessing, compressionEnabled: e.target.checked }
                }))}
              />
            }
            label="Enable file compression"
          />
        </Box>
      </Grid>
    </Grid>
  );

  const renderSchemaAssignmentStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Assign schemas to sub-services for file validation. Schemas can be reused across multiple sub-services.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Schema Assignments ({serviceData.schemaAssignments.length})
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={addSchemaAssignment}
            disabled={serviceData.subServices.length === 0}
          >
            Add Assignment
          </Button>
        </Box>
        
        {serviceData.subServices.length === 0 && (
          <Alert severity="warning">
            Please create sub-services first before assigning schemas.
          </Alert>
        )}
        
        {serviceData.schemaAssignments.length === 0 && serviceData.subServices.length > 0 && (
          <Card sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <SchemaIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No Schema Assignments
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Assign schemas to enable file validation for your sub-services
              </Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={addSchemaAssignment}>
                Create First Assignment
              </Button>
            </CardContent>
          </Card>
        )}
      </Grid>
    </Grid>
  );

  const renderCutOffTimesStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Set cut-off times for your sub-services to control when processing deadlines occur.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Cut-off Times ({serviceData.cutOffTimes.length})
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={addCutOffTime}
            disabled={serviceData.subServices.length === 0}
          >
            Add Cut-off Time
          </Button>
        </Box>
        
        {serviceData.subServices.length === 0 && (
          <Alert severity="warning">
            Please create sub-services first before setting cut-off times.
          </Alert>
        )}
      </Grid>
    </Grid>
  );

  const renderNotificationsStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="info">
          Configure notifications and alerts for your service to stay informed about processing status.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>
          Notification Settings
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          <FormControlLabel
            control={
              <Switch
                checked={serviceData.notifications.successNotifications}
                onChange={(e) => setServiceData(prev => ({
                  ...prev,
                  notifications: { ...prev.notifications, successNotifications: e.target.checked }
                }))}
              />
            }
            label="Success notifications"
          />
          <FormControlLabel
            control={
              <Switch
                checked={serviceData.notifications.errorNotifications}
                onChange={(e) => setServiceData(prev => ({
                  ...prev,
                  notifications: { ...prev.notifications, errorNotifications: e.target.checked }
                }))}
              />
            }
            label="Error notifications"
          />
          <FormControlLabel
            control={
              <Switch
                checked={serviceData.notifications.delayAlerts}
                onChange={(e) => setServiceData(prev => ({
                  ...prev,
                  notifications: { ...prev.notifications, delayAlerts: e.target.checked }
                }))}
              />
            }
            label="Delay alerts"
          />
        </Box>
      </Grid>
    </Grid>
  );

  const renderReviewStep = () => (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Alert severity="success" icon={<CheckIcon />}>
          Excellent! Your service configuration is complete. Review the summary below and complete the setup.
        </Alert>
      </Grid>
      
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom color="primary">
              Service Configuration Summary
            </Typography>
            
            <List>
              <ListItem>
                <ListItemIcon>
                  <ServiceIcon color="primary" />
                </ListItemIcon>
                <ListItemText
                  primary={`Service: ${serviceData.service.serviceName}`}
                  secondary={`Category: ${serviceData.service.category} • Priority: ${serviceData.service.priority}`}
                />
              </ListItem>
              
              <ListItem>
                <ListItemIcon>
                  <FlowIcon color="primary" />
                </ListItemIcon>
                <ListItemText
                  primary={`${serviceData.subServices.length} Sub-Services Configured`}
                  secondary={serviceData.subServices.map(s => s.subServiceName).join(', ')}
                />
              </ListItem>
              
              <ListItem>
                <ListItemIcon>
                  <StorageIcon color="primary" />
                </ListItemIcon>
                <ListItemText
                  primary={`File Processing: ${serviceData.fileProcessing.maxFileSize} max size`}
                  secondary={`${serviceData.fileProcessing.allowedFormats.length} formats allowed • ${serviceData.fileProcessing.retentionDays} days retention`}
                />
              </ListItem>
            </List>
          </CardContent>
        </Card>
      </Grid>
      
      {loading && (
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Creating Your Service...
              </Typography>
              <LinearProgress sx={{ mt: 2 }} />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Setting up all components and configurations.
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
      maxWidth="lg"
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
          <ServiceIcon color="primary" />
          <Typography variant="h5" fontWeight={600}>
            Service Setup Wizard
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Complete guided setup for your new service
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

export default ServiceSetupWizard;