import React, { useState, useEffect } from 'react';
import {
  TextField,
  Button,
  Box,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Typography,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Schema as SchemaIcon
} from '@mui/icons-material';

const ServiceConfigurationForm = ({ service, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    serviceName: '',
    subServiceName: '',
    tenantId: '',
    inboundPath: '',
    outboundPath: '',
    startMarkerPrefix: 'SOT_',
    endMarkerPrefix: 'EOT_',
    dataFilePattern: '*.*',
    enabled: true,
    maxRetries: 3,
    pollIntervalSeconds: 30,
    cutOffTime: '23:59:59',
    cutOffTimeType: 'DAILY',
    weekdayCutOffTime: '',
    weekendCutOffTime: '',
    mondayCutOffTime: '',
    tuesdayCutOffTime: '',
    wednesdayCutOffTime: '',
    thursdayCutOffTime: '',
    fridayCutOffTime: '',
    saturdayCutOffTime: '',
    sundayCutOffTime: '',
    allSundaysAsHolidays: false,
    sotFileValidationRegex: '',
    eotFileValidationRegex: '',
    dataFileValidationRegex: '',
    schemaValidationEnabled: false,
    schemaId: null,
    schemaValidationMode: 'STRICT',
    binaryFileBypass: false,
    description: ''
  });
  const [schemas, setSchemas] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (service) {
      setFormData({
        ...service,
        schemaValidationEnabled: service.schemaValidationEnabled || false,
        schemaId: service.schemaId || null,
        schemaValidationMode: service.schemaValidationMode || 'STRICT',
        binaryFileBypass: service.binaryFileBypass || false
      });
    }
    fetchSchemas();
  }, [service]);

  const fetchSchemas = async () => {
    try {
      const response = await fetch(`/api/schemas/tenant/${formData.tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setSchemas(data.filter(s => s.isActive));
      }
    } catch (err) {
      console.error('Error fetching schemas:', err);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const url = service ? `/api/services/${service.id}` : '/api/services';
      const method = service ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        onSave();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to save service configuration');
      }
    } catch (err) {
      setError('Error saving service configuration: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const getValidationModeColor = (mode) => {
    const colors = {
      'STRICT': 'error',
      'LENIENT': 'warning',
      'WARNING_ONLY': 'info'
    };
    return colors[mode] || 'default';
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Service Name"
            value={formData.serviceName}
            onChange={(e) => handleInputChange('serviceName', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Sub Service Name"
            value={formData.subServiceName}
            onChange={(e) => handleInputChange('subServiceName', e.target.value)}
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Tenant ID"
            value={formData.tenantId}
            onChange={(e) => handleInputChange('tenantId', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.enabled}
                onChange={(e) => handleInputChange('enabled', e.target.checked)}
              />
            }
            label="Enabled"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Inbound Path"
            value={formData.inboundPath}
            onChange={(e) => handleInputChange('inboundPath', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Outbound Path"
            value={formData.outboundPath}
            onChange={(e) => handleInputChange('outboundPath', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
      </Grid>

      {/* Schema Validation Section */}
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Box display="flex" alignItems="center" gap={1}>
            <SchemaIcon />
            <Typography variant="h6">Schema Validation</Typography>
            <Chip
              label={formData.schemaValidationEnabled ? 'Enabled' : 'Disabled'}
              color={formData.schemaValidationEnabled ? 'success' : 'default'}
              size="small"
            />
          </Box>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.schemaValidationEnabled}
                    onChange={(e) => handleInputChange('schemaValidationEnabled', e.target.checked)}
                  />
                }
                label="Enable Schema Validation"
              />
            </Grid>
            
            {formData.schemaValidationEnabled && (
              <>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Schema</InputLabel>
                    <Select
                      value={formData.schemaId || ''}
                      onChange={(e) => handleInputChange('schemaId', e.target.value)}
                      label="Schema"
                    >
                      <MenuItem value="">
                        <em>Select a schema</em>
                      </MenuItem>
                      {schemas.map((schema) => (
                        <MenuItem key={schema.id} value={schema.id}>
                          {schema.schemaName} ({schema.schemaType})
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Validation Mode</InputLabel>
                    <Select
                      value={formData.schemaValidationMode}
                      onChange={(e) => handleInputChange('schemaValidationMode', e.target.value)}
                      label="Validation Mode"
                    >
                      <MenuItem value="STRICT">
                        <Chip label="STRICT" color="error" size="small" />
                        Reject files that don't match schema
                      </MenuItem>
                      <MenuItem value="LENIENT">
                        <Chip label="LENIENT" color="warning" size="small" />
                        Accept files with warnings
                      </MenuItem>
                      <MenuItem value="WARNING_ONLY">
                        <Chip label="WARNING_ONLY" color="info" size="small" />
                        Log warnings but accept files
                      </MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12}>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={formData.binaryFileBypass}
                        onChange={(e) => handleInputChange('binaryFileBypass', e.target.checked)}
                      />
                    }
                    label="Bypass Binary Files"
                  />
                </Grid>
                
                <Grid item xs={12}>
                  <Typography variant="body2" color="textSecondary">
                    Schema validation will be performed on all files processed by this service.
                    Files that fail validation will be rejected based on the selected mode.
                    When binary file bypass is enabled, binary files will skip validation entirely.
                  </Typography>
                </Grid>
              </>
            )}
          </Grid>
        </AccordionDetails>
      </Accordion>

      {/* File Validation Patterns */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">File Validation Patterns</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="SOT File Validation Regex"
                value={formData.sotFileValidationRegex}
                onChange={(e) => handleInputChange('sotFileValidationRegex', e.target.value)}
                margin="normal"
                helperText="Regex pattern for SOT file validation"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="EOT File Validation Regex"
                value={formData.eotFileValidationRegex}
                onChange={(e) => handleInputChange('eotFileValidationRegex', e.target.value)}
                margin="normal"
                helperText="Regex pattern for EOT file validation"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Data File Validation Regex"
                value={formData.dataFileValidationRegex}
                onChange={(e) => handleInputChange('dataFileValidationRegex', e.target.value)}
                margin="normal"
                helperText="Regex pattern for data file validation"
              />
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      {/* Cut-off Time Configuration */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Cut-off Time Configuration</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Cut-off Time"
                type="time"
                value={formData.cutOffTime}
                onChange={(e) => handleInputChange('cutOffTime', e.target.value)}
                margin="normal"
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal">
                <InputLabel>Cut-off Time Type</InputLabel>
                <Select
                  value={formData.cutOffTimeType}
                  onChange={(e) => handleInputChange('cutOffTimeType', e.target.value)}
                  label="Cut-off Time Type"
                >
                  <MenuItem value="DAILY">Daily</MenuItem>
                  <MenuItem value="WEEKDAY_WEEKEND">Weekday/Weekend</MenuItem>
                  <MenuItem value="PER_DAY">Per Day</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
        <Button onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button
          type="submit"
          variant="contained"
          disabled={loading}
        >
          {loading ? 'Saving...' : (service ? 'Update Service' : 'Create Service')}
        </Button>
      </Box>
    </Box>
  );
};

export default ServiceConfigurationForm;