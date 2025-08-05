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
  Typography,
  Alert,
  Paper,
  LinearProgress,
  Chip,
  FormControlLabel,
  Switch
} from '@mui/material';
import { CloudUpload as UploadIcon } from '@mui/icons-material';

const FileUploadDialog = ({ tenantId, onClose }) => {
  const [serviceType, setServiceType] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [validationResult, setValidationResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [serviceTypes, setServiceTypes] = useState([]);
  const [binaryFileBypass, setBinaryFileBypass] = useState(false);

  useEffect(() => {
    fetchServiceTypes();
  }, []);

  const fetchServiceTypes = async () => {
    try {
      // This would typically come from your service configuration API
      const response = await fetch('/api/services');
      if (response.ok) {
        const services = await response.json();
        setServiceTypes(services.map(s => s.serviceType));
      }
    } catch (err) {
      console.error('Error fetching service types:', err);
    }
  };

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (file) {
      setSelectedFile(file);
      setValidationResult(null);
      setError(null);
    }
  };

  const handleValidation = async () => {
    if (!selectedFile || !serviceType) {
      setError('Please select a file and service type');
      return;
    }

    setLoading(true);
    setError(null);
    setValidationResult(null);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('tenantId', tenantId);
      formData.append('serviceType', serviceType);
      formData.append('binaryFileBypass', binaryFileBypass);

      const response = await fetch('/api/schemas/validate-file', {
        method: 'POST',
        body: formData
      });

      if (response.ok) {
        const result = await response.json();
        setValidationResult(result);
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Validation failed');
      }
    } catch (err) {
      setError('Error during validation: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const getValidationColor = (valid) => {
    return valid ? 'success' : 'error';
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Box>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <FormControl fullWidth>
            <InputLabel>Service Type</InputLabel>
            <Select
              value={serviceType}
              onChange={(e) => setServiceType(e.target.value)}
              label="Service Type"
            >
              {serviceTypes.map((type) => (
                <MenuItem key={type} value={type}>
                  {type}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>

        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={binaryFileBypass}
                onChange={(e) => setBinaryFileBypass(e.target.checked)}
              />
            }
            label="Bypass Binary Files"
          />
        </Grid>

        <Grid item xs={12}>
          <Paper
            variant="outlined"
            sx={{
              p: 3,
              textAlign: 'center',
              border: '2px dashed',
              borderColor: 'grey.300',
              backgroundColor: 'grey.50'
            }}
          >
            <input
              accept="*/*"
              style={{ display: 'none' }}
              id="file-upload"
              type="file"
              onChange={handleFileSelect}
            />
            <label htmlFor="file-upload">
              <Button
                variant="outlined"
                component="span"
                startIcon={<UploadIcon />}
                size="large"
              >
                Select File
              </Button>
            </label>
            
            {selectedFile && (
              <Box mt={2}>
                <Typography variant="body2" color="textSecondary">
                  Selected: {selectedFile.name}
                </Typography>
                <Typography variant="caption" color="textSecondary">
                  Size: {formatFileSize(selectedFile.size)}
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>

        {error && (
          <Grid item xs={12}>
            <Alert severity="error">{error}</Alert>
          </Grid>
        )}

        {loading && (
          <Grid item xs={12}>
            <Box>
              <Typography variant="body2" gutterBottom>
                Validating file...
              </Typography>
              <LinearProgress />
            </Box>
          </Grid>
        )}

        {validationResult && (
          <Grid item xs={12}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Validation Result
              </Typography>
              
              <Box display="flex" alignItems="center" mb={2}>
                <Chip
                  label={validationResult.valid ? 'PASSED' : 'FAILED'}
                  color={getValidationColor(validationResult.valid)}
                  sx={{ mr: 2 }}
                />
                <Typography variant="body2">
                  {validationResult.message}
                </Typography>
              </Box>

              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    File Name
                  </Typography>
                  <Typography variant="body2">
                    {validationResult.fileName}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    File Size
                  </Typography>
                  <Typography variant="body2">
                    {formatFileSize(validationResult.fileSize)}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    Service Type
                  </Typography>
                  <Typography variant="body2">
                    {validationResult.serviceType}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="textSecondary">
                    Tenant ID
                  </Typography>
                  <Typography variant="body2">
                    {validationResult.tenantId}
                  </Typography>
                </Grid>
              </Grid>
            </Paper>
          </Grid>
        )}

        <Grid item xs={12}>
          <Box display="flex" justifyContent="flex-end" gap={2}>
            <Button onClick={onClose}>
              Close
            </Button>
            <Button
              variant="contained"
              onClick={handleValidation}
              disabled={!selectedFile || !serviceType || loading}
            >
              Validate File
            </Button>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default FileUploadDialog;