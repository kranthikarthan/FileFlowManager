import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Tooltip,
  CircularProgress,
  useTheme
} from '@mui/material';
import {
  PlayArrow,
  Stop,
  Pause,
  Refresh,
  Settings,
  Warning,
  CheckCircle,
  Error,
  Schedule,
  RestartAlt,
  Speed,
  Timeline
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { processingControlService } from '../../services/processingControlService';

/**
 * Processing Control Dashboard - Comprehensive processing management
 * Provides start/stop/pause/resume controls at all levels with edge case handling
 */
const ProcessingControlDashboard = () => {
  const theme = useTheme();

  // State management
  const [tenants, setTenants] = useState([]);
  const [selectedTenant, setSelectedTenant] = useState('');
  const [tenantStatus, setTenantStatus] = useState(null);
  const [services, setServices] = useState([]);
  const [activeFiles, setActiveFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Dialog state
  const [confirmDialog, setConfirmDialog] = useState({
    open: false,
    title: '',
    message: '',
    action: null,
    parameters: {}
  });

  // Holiday edge case state
  const [holidayAlerts, setHolidayAlerts] = useState([]);

  // Load initial data
  useEffect(() => {
    loadTenants();
  }, []);

  // Load tenant data when selected
  useEffect(() => {
    if (selectedTenant) {
      loadTenantData();
    }
  }, [selectedTenant]);

  /**
   * Load available tenants
   */
  const loadTenants = async () => {
    try {
      const tenantsData = await processingControlService.getTenants();
      setTenants(tenantsData);
      if (tenantsData.length > 0 && !selectedTenant) {
        setSelectedTenant(tenantsData[0].tenantId);
      }
    } catch (err) {
      setError('Failed to load tenants: ' + err.message);
    }
  };

  /**
   * Load tenant processing data
   */
  const loadTenantData = async () => {
    if (!selectedTenant) return;

    setLoading(true);
    try {
      const [status, servicesData, files] = await Promise.all([
        processingControlService.getTenantProcessingStatus(selectedTenant),
        processingControlService.getServices(selectedTenant),
        processingControlService.getActiveFiles(selectedTenant)
      ]);

      setTenantStatus(status);
      setServices(servicesData);
      setActiveFiles(files);

      // Check for holiday edge cases
      checkHolidayEdgeCases(status);

    } catch (err) {
      setError('Failed to load tenant data: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Check for holiday edge cases and display alerts
   */
  const checkHolidayEdgeCases = (status) => {
    const alerts = [];
    const today = new Date();
    
    if (status.holidaySundayCollision) {
      alerts.push({
        type: 'warning',
        message: 'Today is both a holiday and Sunday - special processing rules may apply',
        date: today.toDateString()
      });
    }
    
    if (status.isHoliday && !status.isSunday) {
      alerts.push({
        type: 'info',
        message: 'Today is a holiday - processing may be limited',
        date: today.toDateString()
      });
    }
    
    setHolidayAlerts(alerts);
  };

  /**
   * Show confirmation dialog for processing actions
   */
  const showConfirmDialog = (title, message, action, parameters = {}) => {
    setConfirmDialog({
      open: true,
      title,
      message,
      action,
      parameters
    });
  };

  /**
   * Execute confirmed action
   */
  const executeAction = async () => {
    const { action, parameters } = confirmDialog;
    setConfirmDialog({ ...confirmDialog, open: false });
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      let result;
      
      switch (action) {
        case 'START_TENANT':
          result = await processingControlService.startTenantProcessing(
            parameters.tenantId, parameters.userId);
          break;
        case 'STOP_TENANT':
          result = await processingControlService.stopTenantProcessing(
            parameters.tenantId, parameters.userId, parameters.graceful);
          break;
        case 'START_SERVICE':
          result = await processingControlService.startServiceProcessing(
            parameters.tenantId, parameters.serviceName, parameters.userId);
          break;
        case 'STOP_SERVICE':
          result = await processingControlService.stopServiceProcessing(
            parameters.tenantId, parameters.serviceName, parameters.userId, parameters.graceful);
          break;
        case 'RESET_SERVICE':
          result = await processingControlService.resetServiceForDay(
            parameters.tenantId, parameters.serviceName, parameters.userId, parameters.resetDate);
          break;
        case 'START_SUBSERVICE':
          result = await processingControlService.startSubServiceProcessing(
            parameters.tenantId, parameters.serviceName, parameters.subServiceName, parameters.userId);
          break;
        case 'STOP_SUBSERVICE':
          result = await processingControlService.stopSubServiceProcessing(
            parameters.tenantId, parameters.serviceName, parameters.subServiceName, 
            parameters.userId, parameters.graceful);
          break;
        case 'RESET_SUBSERVICE':
          result = await processingControlService.resetSubServiceForDay(
            parameters.tenantId, parameters.serviceName, parameters.subServiceName, 
            parameters.userId, parameters.resetDate);
          break;
        case 'START_FILE':
          result = await processingControlService.startFileProcessing(
            parameters.fileId, parameters.userId);
          break;
        case 'STOP_FILE':
          result = await processingControlService.stopFileProcessing(
            parameters.fileId, parameters.userId, parameters.force);
          break;
        case 'PAUSE_FILE':
          result = await processingControlService.pauseFileProcessing(
            parameters.fileId, parameters.userId);
          break;
        case 'RESUME_FILE':
          result = await processingControlService.resumeFileProcessing(
            parameters.fileId, parameters.userId);
          break;
        default:
          throw new Error('Unknown action: ' + action);
      }

      setSuccess(result.message || 'Action completed successfully');
      
      // Handle edge case alerts
      if (result.holidaySundayCollision) {
        setHolidayAlerts(prev => [...prev, {
          type: 'warning',
          message: 'Action performed on date that is both holiday and Sunday',
          timestamp: new Date().toISOString()
        }]);
      }
      
      // Reload data
      await loadTenantData();

    } catch (err) {
      setError('Action failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Render tenant-level controls
   */
  const renderTenantControls = () => {
    if (!tenantStatus) return null;

    const isActive = tenantStatus.processingStatus === 'ACTIVE';
    
    return (
      <Card elevation={2} sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Tenant Processing Control
            </Typography>
            <Chip 
              label={tenantStatus.processingStatus}
              color={isActive ? 'success' : 'default'}
              icon={isActive ? <CheckCircle /> : <Stop />}
            />
          </Box>
          
          <Grid container spacing={2} alignItems="center">
            <Grid item>
              <Typography variant="body2" color="textSecondary">
                Services: {tenantStatus.enabledServices}/{tenantStatus.totalServices} enabled
              </Typography>
            </Grid>
            <Grid item>
              <Typography variant="body2" color="textSecondary">
                Active Files: {tenantStatus.activeFiles}
              </Typography>
            </Grid>
            <Grid item xs />
            <Grid item>
              <Button
                variant="contained"
                color="success"
                startIcon={<PlayArrow />}
                disabled={isActive || loading}
                onClick={() => showConfirmDialog(
                  'Start Tenant Processing',
                  `Start all processing for tenant ${selectedTenant}?`,
                  'START_TENANT',
                  { tenantId: selectedTenant, userId: 'current-user' }
                )}
              >
                Start All
              </Button>
            </Grid>
            <Grid item>
              <Button
                variant="contained"
                color="error"
                startIcon={<Stop />}
                disabled={!isActive || loading}
                onClick={() => showConfirmDialog(
                  'Stop Tenant Processing',
                  `Stop all processing for tenant ${selectedTenant}? This will affect all services.`,
                  'STOP_TENANT',
                  { tenantId: selectedTenant, userId: 'current-user', graceful: true }
                )}
              >
                Stop All
              </Button>
            </Grid>
            <Grid item>
              <Button
                variant="outlined"
                startIcon={<Refresh />}
                onClick={loadTenantData}
                disabled={loading}
              >
                Refresh
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
    );
  };

  /**
   * Render service-level controls
   */
  const renderServiceControls = () => {
    if (!services || services.length === 0) return null;

    return (
      <Card elevation={2} sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Service Processing Control
          </Typography>
          
          <List>
            {services.map((service, index) => (
              <React.Fragment key={service.serviceName}>
                <ListItem>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="subtitle1">
                          {service.serviceName}
                        </Typography>
                        <Chip 
                          size="small"
                          label={service.serviceEnabled ? 'Active' : 'Stopped'}
                          color={service.serviceEnabled ? 'success' : 'default'}
                        />
                      </Box>
                    }
                    secondary={
                      <Box>
                        <Typography variant="body2" color="textSecondary">
                          Sub-services: {service.enabledSubServices}/{service.totalSubServices} enabled
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                          Active files: {service.activeFiles}
                        </Typography>
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    <Box display="flex" gap={1}>
                      <Tooltip title="Start Service">
                        <IconButton
                          color="success"
                          disabled={service.serviceEnabled || loading}
                          onClick={() => showConfirmDialog(
                            'Start Service Processing',
                            `Start processing for service ${service.serviceName}?`,
                            'START_SERVICE',
                            { tenantId: selectedTenant, serviceName: service.serviceName, userId: 'current-user' }
                          )}
                        >
                          <PlayArrow />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Stop Service">
                        <IconButton
                          color="error"
                          disabled={!service.serviceEnabled || loading}
                          onClick={() => showConfirmDialog(
                            'Stop Service Processing',
                            `Stop processing for service ${service.serviceName}?`,
                            'STOP_SERVICE',
                            { tenantId: selectedTenant, serviceName: service.serviceName, userId: 'current-user', graceful: true }
                          )}
                        >
                          <Stop />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Reset Service for Day">
                        <IconButton
                          color="warning"
                          disabled={loading}
                          onClick={() => showConfirmDialog(
                            'Reset Service for Day',
                            `Reset service ${service.serviceName} for today? This will reset all file counts and processing status.`,
                            'RESET_SERVICE',
                            { tenantId: selectedTenant, serviceName: service.serviceName, userId: 'current-user', resetDate: new Date() }
                          )}
                        >
                          <RestartAlt />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </ListItemSecondaryAction>
                </ListItem>
                
                {/* Sub-services */}
                {service.subServices && service.subServices.map((subService) => (
                  <ListItem key={subService.subServiceName} sx={{ pl: 4 }}>
                    <ListItemText
                      primary={
                        <Box display="flex" alignItems="center" gap={1}>
                          <Typography variant="body1">
                            {subService.subServiceName}
                          </Typography>
                          <Chip 
                            size="small"
                            label={subService.enabled ? 'Active' : 'Stopped'}
                            color={subService.enabled ? 'success' : 'default'}
                            variant="outlined"
                          />
                        </Box>
                      }
                      secondary={`Active files: ${subService.activeFiles || 0}`}
                    />
                    <ListItemSecondaryAction>
                      <Box display="flex" gap={1}>
                        <Tooltip title="Start Sub-Service">
                          <IconButton
                            size="small"
                            color="success"
                            disabled={subService.enabled || loading}
                            onClick={() => showConfirmDialog(
                              'Start Sub-Service Processing',
                              `Start processing for sub-service ${subService.subServiceName}?`,
                              'START_SUBSERVICE',
                              { 
                                tenantId: selectedTenant, 
                                serviceName: service.serviceName,
                                subServiceName: subService.subServiceName, 
                                userId: 'current-user' 
                              }
                            )}
                          >
                            <PlayArrow />
                          </IconButton>
                        </Tooltip>
                        
                        <Tooltip title="Stop Sub-Service">
                          <IconButton
                            size="small"
                            color="error"
                            disabled={!subService.enabled || loading}
                            onClick={() => showConfirmDialog(
                              'Stop Sub-Service Processing',
                              `Stop processing for sub-service ${subService.subServiceName}?`,
                              'STOP_SUBSERVICE',
                              { 
                                tenantId: selectedTenant, 
                                serviceName: service.serviceName,
                                subServiceName: subService.subServiceName, 
                                userId: 'current-user', 
                                graceful: true 
                              }
                            )}
                          >
                            <Stop />
                          </IconButton>
                        </Tooltip>
                        
                        <Tooltip title="Reset Sub-Service for Day">
                          <IconButton
                            size="small"
                            color="warning"
                            disabled={loading}
                            onClick={() => showConfirmDialog(
                              'Reset Sub-Service for Day',
                              `Reset sub-service ${subService.subServiceName} for today?`,
                              'RESET_SUBSERVICE',
                              { 
                                tenantId: selectedTenant, 
                                serviceName: service.serviceName,
                                subServiceName: subService.subServiceName, 
                                userId: 'current-user', 
                                resetDate: new Date() 
                              }
                            )}
                          >
                            <RestartAlt />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
                
                {index < services.length - 1 && <Divider />}
              </React.Fragment>
            ))}
          </List>
        </CardContent>
      </Card>
    );
  };

  /**
   * Render active files control
   */
  const renderFileControls = () => {
    if (!activeFiles || activeFiles.length === 0) {
      return (
        <Card elevation={2}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Active Files
            </Typography>
            <Typography variant="body2" color="textSecondary">
              No active files currently processing
            </Typography>
          </CardContent>
        </Card>
      );
    }

    return (
      <Card elevation={2}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Active Files ({activeFiles.length})
          </Typography>
          
          <List>
            {activeFiles.map((file, index) => (
              <React.Fragment key={file.fileId}>
                <ListItem>
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="subtitle2">
                          {file.fileName}
                        </Typography>
                        <Chip 
                          size="small"
                          label={file.status}
                          color={getFileStatusColor(file.status)}
                        />
                      </Box>
                    }
                    secondary={
                      <Box>
                        <Typography variant="caption" color="textSecondary">
                          Service: {file.serviceName}/{file.subServiceName}
                        </Typography>
                        <br />
                        <Typography variant="caption" color="textSecondary">
                          Started: {file.processingStartTime ? 
                            new Date(file.processingStartTime).toLocaleString() : 'N/A'}
                        </Typography>
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    <Box display="flex" gap={1}>
                      <Tooltip title="Start File Processing">
                        <IconButton
                          size="small"
                          color="success"
                          disabled={file.status === 'PROCESSING' || file.status === 'COMPLETED' || loading}
                          onClick={() => showConfirmDialog(
                            'Start File Processing',
                            `Start processing file ${file.fileName}?`,
                            'START_FILE',
                            { fileId: file.fileId, userId: 'current-user' }
                          )}
                        >
                          <PlayArrow />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Pause File Processing">
                        <IconButton
                          size="small"
                          color="warning"
                          disabled={file.status !== 'PROCESSING' || loading}
                          onClick={() => showConfirmDialog(
                            'Pause File Processing',
                            `Pause processing file ${file.fileName}?`,
                            'PAUSE_FILE',
                            { fileId: file.fileId, userId: 'current-user' }
                          )}
                        >
                          <Pause />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Stop File Processing">
                        <IconButton
                          size="small"
                          color="error"
                          disabled={file.status !== 'PROCESSING' && file.status !== 'PAUSED' || loading}
                          onClick={() => showConfirmDialog(
                            'Stop File Processing',
                            `Stop processing file ${file.fileName}? This will mark the file as failed.`,
                            'STOP_FILE',
                            { fileId: file.fileId, userId: 'current-user', force: false }
                          )}
                        >
                          <Stop />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </ListItemSecondaryAction>
                </ListItem>
                {index < activeFiles.length - 1 && <Divider />}
              </React.Fragment>
            ))}
          </List>
        </CardContent>
      </Card>
    );
  };

  /**
   * Get color for file status chip
   */
  const getFileStatusColor = (status) => {
    switch (status) {
      case 'PROCESSING': return 'primary';
      case 'COMPLETED': return 'success';
      case 'FAILED': return 'error';
      case 'PAUSED': return 'warning';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  /**
   * Render holiday alerts
   */
  const renderHolidayAlerts = () => {
    if (holidayAlerts.length === 0) return null;

    return (
      <Box mb={2}>
        {holidayAlerts.map((alert, index) => (
          <Alert key={index} severity={alert.type} sx={{ mb: 1 }}>
            {alert.message}
            {alert.date && (
              <Typography variant="caption" display="block">
                Date: {alert.date}
              </Typography>
            )}
          </Alert>
        ))}
      </Box>
    );
  };

  /**
   * Render confirmation dialog
   */
  const renderConfirmDialog = () => (
    <Dialog open={confirmDialog.open} onClose={() => setConfirmDialog({ ...confirmDialog, open: false })}>
      <DialogTitle>{confirmDialog.title}</DialogTitle>
      <DialogContent>
        <Typography>{confirmDialog.message}</Typography>
        
        {confirmDialog.action && confirmDialog.action.includes('RESET') && (
          <Box mt={2}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DatePicker
                label="Reset Date"
                value={confirmDialog.parameters.resetDate || new Date()}
                onChange={(newValue) => setConfirmDialog(prev => ({
                  ...prev,
                  parameters: { ...prev.parameters, resetDate: newValue }
                }))}
                renderInput={(params) => <TextField {...params} fullWidth margin="normal" />}
              />
            </LocalizationProvider>
          </Box>
        )}
        
        {confirmDialog.action && confirmDialog.action.includes('STOP') && (
          <Box mt={2}>
            <FormControlLabel
              control={
                <Switch
                  checked={confirmDialog.parameters.graceful !== false}
                  onChange={(e) => setConfirmDialog(prev => ({
                    ...prev,
                    parameters: { ...prev.parameters, graceful: e.target.checked }
                  }))}
                />
              }
              label="Graceful shutdown (wait for active files to complete)"
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setConfirmDialog({ ...confirmDialog, open: false })}>
          Cancel
        </Button>
        <Button onClick={executeAction} variant="contained" color="primary">
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 2 }}>
        {/* Header */}
        <Typography variant="h4" gutterBottom>
          Processing Control Dashboard
        </Typography>
        
        {/* Tenant Selection */}
        <Card elevation={1} sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6} md={4}>
                <FormControl fullWidth>
                  <InputLabel>Select Tenant</InputLabel>
                  <Select
                    value={selectedTenant}
                    onChange={(e) => setSelectedTenant(e.target.value)}
                    label="Select Tenant"
                  >
                    {tenants.map((tenant) => (
                      <MenuItem key={tenant.tenantId} value={tenant.tenantId}>
                        {tenant.tenantName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Holiday Alerts */}
        {renderHolidayAlerts()}

        {/* Error/Success Messages */}
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

        {/* Loading Indicator */}
        {loading && (
          <Box display="flex" justifyContent="center" mb={2}>
            <CircularProgress />
          </Box>
        )}

        {/* Tenant Controls */}
        {renderTenantControls()}

        {/* Service Controls */}
        {renderServiceControls()}

        {/* File Controls */}
        {renderFileControls()}

        {/* Confirmation Dialog */}
        {renderConfirmDialog()}
      </Box>
    </LocalizationProvider>
  );
};

export default ProcessingControlDashboard;