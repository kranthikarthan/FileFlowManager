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
  AccordionDetails
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  PlayArrow as PlayIcon,
  Stop as StopIcon,
  ExpandMore as ExpandMoreIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon
} from '@mui/icons-material';
import { serviceAPI } from '../services/serviceAPI';

export const ServiceConfiguration = () => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [open, setOpen] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [validationTest, setValidationTest] = useState({
    fileName: '',
    fileType: 'sot',
    result: null
  });

  const [formData, setFormData] = useState({
    serviceName: '',
    inboundPath: '',
    outboundPath: '',
    startMarkerPrefix: 'SOT_',
    endMarkerPrefix: 'EOT_',
    dataFilePattern: '*.*',
    enabled: true,
    maxRetries: 3,
    pollIntervalSeconds: 30,
    sotFileValidationRegex: '',
    eotFileValidationRegex: '',
    dataFileValidationRegex: '',
    description: ''
  });

  useEffect(() => {
    fetchServices();
  }, []);

  const fetchServices = async () => {
    try {
      setLoading(true);
      const response = await serviceAPI.getAllServices();
      setServices(response.data);
    } catch (err) {
      setError('Failed to fetch services');
      console.error('Error fetching services:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      if (editingService) {
        await serviceAPI.updateService(editingService.id, formData);
        setSuccess('Service updated successfully');
      } else {
        await serviceAPI.createService(formData);
        setSuccess('Service created successfully');
      }
      
      handleClose();
      fetchServices();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save service');
    }
  };

  const handleEdit = (service) => {
    setEditingService(service);
    setFormData({ ...service });
    setOpen(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this service?')) {
      try {
        await serviceAPI.deleteService(id);
        setSuccess('Service deleted successfully');
        fetchServices();
      } catch (err) {
        setError('Failed to delete service');
      }
    }
  };

  const handleToggleStatus = async (id) => {
    try {
      await serviceAPI.toggleServiceStatus(id);
      setSuccess('Service status updated');
      fetchServices();
    } catch (err) {
      setError('Failed to update service status');
    }
  };

  const handleClose = () => {
    setOpen(false);
    setEditingService(null);
    setFormData({
      serviceName: '',
      inboundPath: '',
      outboundPath: '',
      startMarkerPrefix: 'SOT_',
      endMarkerPrefix: 'EOT_',
      dataFilePattern: '*.*',
      enabled: true,
      maxRetries: 3,
      pollIntervalSeconds: 30,
      sotFileValidationRegex: '',
      eotFileValidationRegex: '',
      dataFileValidationRegex: '',
      description: ''
    });
  };

  const testValidation = async (service, fileName, fileType) => {
    try {
      const response = await serviceAPI.validateFile({
        fileName,
        serviceType: service.serviceName,
        fileType
      });
      
      setValidationTest({
        fileName,
        fileType,
        result: response.data
      });
    } catch (err) {
      setError('Failed to test validation');
    }
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Service Configuration</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpen(true)}
        >
          Add Service
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
        {services.map((service) => (
          <Grid item xs={12} md={6} lg={4} key={service.id}>
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">{service.serviceName}</Typography>
                  <Chip
                    label={service.enabled ? 'Enabled' : 'Disabled'}
                    color={service.enabled ? 'success' : 'default'}
                    size="small"
                  />
                </Box>

                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {service.description || 'No description'}
                </Typography>

                <Box mb={2}>
                  <Typography variant="body2">
                    <strong>Inbound:</strong> {service.inboundPath}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Outbound:</strong> {service.outboundPath}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Prefixes:</strong> {service.startMarkerPrefix} / {service.endMarkerPrefix}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Pattern:</strong> {service.dataFilePattern}
                  </Typography>
                  <Typography variant="body2">
                    <strong>Poll Interval:</strong> {service.pollIntervalSeconds}s
                  </Typography>
                </Box>

                {/* Validation Section */}
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="body2">File Validation</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Box>
                      {service.sotFileValidationRegex && (
                        <Typography variant="caption" display="block">
                          <strong>SOT Regex:</strong> {service.sotFileValidationRegex}
                        </Typography>
                      )}
                      {service.eotFileValidationRegex && (
                        <Typography variant="caption" display="block">
                          <strong>EOT Regex:</strong> {service.eotFileValidationRegex}
                        </Typography>
                      )}
                      {service.dataFileValidationRegex && (
                        <Typography variant="caption" display="block">
                          <strong>Data Regex:</strong> {service.dataFileValidationRegex}
                        </Typography>
                      )}

                      {/* Validation Test */}
                      <Box mt={2}>
                        <Typography variant="caption">Test Validation:</Typography>
                        <Grid container spacing={1} alignItems="center">
                          <Grid item xs={12}>
                            <TextField
                              size="small"
                              placeholder="Enter filename to test"
                              value={validationTest.fileName}
                              onChange={(e) => setValidationTest(prev => ({
                                ...prev,
                                fileName: e.target.value
                              }))}
                              fullWidth
                            />
                          </Grid>
                          <Grid item xs={6}>
                            <FormControl size="small" fullWidth>
                              <Select
                                value={validationTest.fileType}
                                onChange={(e) => setValidationTest(prev => ({
                                  ...prev,
                                  fileType: e.target.value
                                }))}
                              >
                                <MenuItem value="sot">SOT File</MenuItem>
                                <MenuItem value="eot">EOT File</MenuItem>
                                <MenuItem value="data">Data File</MenuItem>
                              </Select>
                            </FormControl>
                          </Grid>
                          <Grid item xs={6}>
                            <Button
                              size="small"
                              onClick={() => testValidation(service, validationTest.fileName, validationTest.fileType)}
                              disabled={!validationTest.fileName}
                              fullWidth
                            >
                              Test
                            </Button>
                          </Grid>
                        </Grid>
                        
                        {validationTest.result && (
                          <Box mt={1}>
                            <Chip
                              label={validationTest.result.valid ? 'Valid' : 'Invalid'}
                              color={validationTest.result.valid ? 'success' : 'error'}
                              size="small"
                            />
                          </Box>
                        )}
                      </Box>
                    </Box>
                  </AccordionDetails>
                </Accordion>
              </CardContent>

              <CardActions>
                <Button
                  size="small"
                  startIcon={<EditIcon />}
                  onClick={() => handleEdit(service)}
                >
                  Edit
                </Button>
                <Button
                  size="small"
                  startIcon={service.enabled ? <StopIcon /> : <PlayIcon />}
                  color={service.enabled ? 'warning' : 'primary'}
                  onClick={() => handleToggleStatus(service.id)}
                >
                  {service.enabled ? 'Disable' : 'Enable'}
                </Button>
                <Button
                  size="small"
                  startIcon={<DeleteIcon />}
                  color="error"
                  onClick={() => handleDelete(service.id)}
                >
                  Delete
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Service Form Dialog */}
      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingService ? 'Edit Service' : 'Add New Service'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Service Name"
                value={formData.serviceName}
                onChange={(e) => setFormData({ ...formData, serviceName: e.target.value })}
                required
              />
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
                label="Inbound Path"
                value={formData.inboundPath}
                onChange={(e) => setFormData({ ...formData, inboundPath: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Outbound Path"
                value={formData.outboundPath}
                onChange={(e) => setFormData({ ...formData, outboundPath: e.target.value })}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Start Marker Prefix"
                value={formData.startMarkerPrefix}
                onChange={(e) => setFormData({ ...formData, startMarkerPrefix: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="End Marker Prefix"
                value={formData.endMarkerPrefix}
                onChange={(e) => setFormData({ ...formData, endMarkerPrefix: e.target.value })}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Data File Pattern"
                value={formData.dataFilePattern}
                onChange={(e) => setFormData({ ...formData, dataFilePattern: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Poll Interval (seconds)"
                type="number"
                value={formData.pollIntervalSeconds}
                onChange={(e) => setFormData({ ...formData, pollIntervalSeconds: parseInt(e.target.value) })}
                required
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Max Retries"
                type="number"
                value={formData.maxRetries}
                onChange={(e) => setFormData({ ...formData, maxRetries: parseInt(e.target.value) })}
                required
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

            {/* Validation Regex Fields */}
            <Grid item xs={12}>
              <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                File Validation (Optional)
              </Typography>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="SOT File Validation Regex"
                value={formData.sotFileValidationRegex}
                onChange={(e) => setFormData({ ...formData, sotFileValidationRegex: e.target.value })}
                helperText="Regular expression to validate start-of-transmission files"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="EOT File Validation Regex"
                value={formData.eotFileValidationRegex}
                onChange={(e) => setFormData({ ...formData, eotFileValidationRegex: e.target.value })}
                helperText="Regular expression to validate end-of-transmission files"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Data File Validation Regex"
                value={formData.dataFileValidationRegex}
                onChange={(e) => setFormData({ ...formData, dataFileValidationRegex: e.target.value })}
                helperText="Regular expression to validate data files"
              />
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
            disabled={!formData.serviceName || !formData.inboundPath || !formData.outboundPath}
          >
            {editingService ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};