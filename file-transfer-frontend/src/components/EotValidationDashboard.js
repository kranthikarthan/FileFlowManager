import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  LinearProgress,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  Pending as PendingIcon,
  Refresh as RefreshIcon,
  GetApp as DownloadIcon,
  Visibility as ViewIcon
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import './EotValidationDashboard.css';

const EotValidationDashboard = ({ tenantId }) => {
  const [dashboardData, setDashboardData] = useState(null);
  const [validationResults, setValidationResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Date range for queries
  const [startDate, setStartDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date;
  });
  const [endDate, setEndDate] = useState(new Date());
  
  // Detail dialog state
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedTracker, setSelectedTracker] = useState(null);
  
  // Filter states
  const [statusFilter, setStatusFilter] = useState('');
  const [serviceFilter, setServiceFilter] = useState('');

  useEffect(() => {
    loadDashboardData();
    loadValidationResults();
  }, [tenantId]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/eot-validation/tenant/${tenantId}/dashboard`);
      if (response.ok) {
        const data = await response.json();
        setDashboardData(data);
      } else {
        setError('Failed to load dashboard data');
      }
    } catch (err) {
      setError('Error loading dashboard: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadValidationResults = async () => {
    try {
      const startDateStr = startDate.toISOString().split('T')[0];
      const endDateStr = endDate.toISOString().split('T')[0];
      
      const response = await fetch(
        `/api/eot-validation/tenant/${tenantId}/results?startDate=${startDateStr}&endDate=${endDateStr}`
      );
      
      if (response.ok) {
        const data = await response.json();
        setValidationResults(data);
      } else {
        setError('Failed to load validation results');
      }
    } catch (err) {
      setError('Error loading validation results: ' + err.message);
    }
  };

  const handleRefresh = () => {
    loadDashboardData();
    loadValidationResults();
  };

  const handleDateRangeChange = () => {
    loadValidationResults();
  };

  const openDetailDialog = (tracker) => {
    setSelectedTracker(tracker);
    setDetailDialogOpen(true);
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'MATCHED':
        return <CheckCircleIcon color="success" />;
      case 'DISCREPANCY':
        return <ErrorIcon color="error" />;
      case 'PENDING':
        return <PendingIcon color="warning" />;
      case 'MISSING_EOT':
      case 'MISSING_SOT':
        return <WarningIcon color="warning" />;
      case 'ERROR':
        return <ErrorIcon color="error" />;
      default:
        return <PendingIcon />;
    }
  };

  const getStatusChip = (status) => {
    const colorMap = {
      'MATCHED': 'success',
      'DISCREPANCY': 'error',
      'PENDING': 'warning',
      'MISSING_EOT': 'warning',
      'MISSING_SOT': 'warning',
      'ERROR': 'error'
    };

    return (
      <Chip
        label={status.replace('_', ' ')}
        color={colorMap[status] || 'default'}
        size="small"
        icon={getStatusIcon(status)}
      />
    );
  };

  const getFilteredResults = () => {
    return validationResults.filter(result => {
      if (statusFilter && result.validationStatus !== statusFilter) {
        return false;
      }
      if (serviceFilter && result.serviceName !== serviceFilter) {
        return false;
      }
      return true;
    });
  };

  const getUniqueServices = () => {
    return [...new Set(validationResults.map(result => result.serviceName))];
  };

  const exportResults = async () => {
    try {
      const startDateStr = startDate.toISOString().split('T')[0];
      const endDateStr = endDate.toISOString().split('T')[0];
      
      const response = await fetch(
        `/api/eot-validation/tenant/${tenantId}/export?startDate=${startDateStr}&endDate=${endDateStr}&format=json`
      );
      
      if (response.ok) {
        const data = await response.json();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `eot-validation-report-${startDateStr}-${endDateStr}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }
    } catch (err) {
      setError('Error exporting results: ' + err.message);
    }
  };

  if (loading) {
    return (
      <Box className="eot-validation-dashboard">
        <Typography variant="h4" gutterBottom>EOT Validation Dashboard</Typography>
        <LinearProgress />
      </Box>
    );
  }

  const statistics = dashboardData?.statistics || {};
  const matchPercentage = statistics.matchPercentage || 0;

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box className="eot-validation-dashboard">
        <Box className="dashboard-header">
          <Typography variant="h4" gutterBottom>
            EOT Validation Dashboard
          </Typography>
          <Box className="header-actions">
            <Button
              startIcon={<RefreshIcon />}
              onClick={handleRefresh}
              variant="outlined"
            >
              Refresh
            </Button>
            <Button
              startIcon={<DownloadIcon />}
              onClick={exportResults}
              variant="contained"
            >
              Export
            </Button>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Statistics Cards */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card className="stat-card success">
              <CardContent>
                <Box className="stat-content">
                  <CheckCircleIcon className="stat-icon" />
                  <Box>
                    <Typography variant="h4">{statistics.matchedCount || 0}</Typography>
                    <Typography variant="body2">Matched</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card className="stat-card error">
              <CardContent>
                <Box className="stat-content">
                  <ErrorIcon className="stat-icon" />
                  <Box>
                    <Typography variant="h4">{statistics.discrepancyCount || 0}</Typography>
                    <Typography variant="body2">Discrepancies</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card className="stat-card warning">
              <CardContent>
                <Box className="stat-content">
                  <PendingIcon className="stat-icon" />
                  <Box>
                    <Typography variant="h4">{statistics.pendingCount || 0}</Typography>
                    <Typography variant="body2">Pending</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card className="stat-card">
              <CardContent>
                <Box className="stat-content">
                  <Box className="percentage-circle">
                    <Typography variant="h4">{Math.round(matchPercentage)}%</Typography>
                  </Box>
                  <Box>
                    <Typography variant="body2">Match Rate</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Date Range and Filters */}
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={3}>
                <DatePicker
                  label="Start Date"
                  value={startDate}
                  onChange={setStartDate}
                  renderInput={(params) => <TextField {...params} fullWidth />}
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <DatePicker
                  label="End Date"
                  value={endDate}
                  onChange={setEndDate}
                  renderInput={(params) => <TextField {...params} fullWidth />}
                />
              </Grid>
              <Grid item xs={12} md={2}>
                <FormControl fullWidth>
                  <InputLabel>Status</InputLabel>
                  <Select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    label="Status"
                  >
                    <MenuItem value="">All Statuses</MenuItem>
                    <MenuItem value="MATCHED">Matched</MenuItem>
                    <MenuItem value="DISCREPANCY">Discrepancy</MenuItem>
                    <MenuItem value="PENDING">Pending</MenuItem>
                    <MenuItem value="MISSING_EOT">Missing EOT</MenuItem>
                    <MenuItem value="MISSING_SOT">Missing SOT</MenuItem>
                    <MenuItem value="ERROR">Error</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={2}>
                <FormControl fullWidth>
                  <InputLabel>Service</InputLabel>
                  <Select
                    value={serviceFilter}
                    onChange={(e) => setServiceFilter(e.target.value)}
                    label="Service"
                  >
                    <MenuItem value="">All Services</MenuItem>
                    {getUniqueServices().map(service => (
                      <MenuItem key={service} value={service}>{service}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={2}>
                <Button
                  fullWidth
                  variant="contained"
                  onClick={handleDateRangeChange}
                >
                  Apply Filters
                </Button>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Validation Results Table */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Validation Results ({getFilteredResults().length} records)
            </Typography>
            
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Service</TableCell>
                    <TableCell>Sub-Service</TableCell>
                    <TableCell>File Type</TableCell>
                    <TableCell>Direction</TableCell>
                    <TableCell>Expected</TableCell>
                    <TableCell>Actual</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {getFilteredResults().map((tracker) => (
                    <TableRow key={tracker.id}>
                      <TableCell>{tracker.processingDate}</TableCell>
                      <TableCell>{tracker.serviceName}</TableCell>
                      <TableCell>{tracker.subServiceName}</TableCell>
                      <TableCell>
                        <Chip label={tracker.fileType} size="small" />
                      </TableCell>
                      <TableCell>
                        <Chip label={tracker.direction} size="small" />
                      </TableCell>
                      <TableCell>
                        {tracker.eotCountValue !== null ? tracker.eotCountValue : '-'}
                      </TableCell>
                      <TableCell>{tracker.actualCount}</TableCell>
                      <TableCell>
                        {getStatusChip(tracker.validationStatus)}
                        {tracker.discrepancyCount > 0 && (
                          <Tooltip title={`Difference: ${tracker.discrepancyCount}`}>
                            <Chip
                              label={`±${tracker.discrepancyCount}`}
                              size="small"
                              color="error"
                              sx={{ ml: 1 }}
                            />
                          </Tooltip>
                        )}
                      </TableCell>
                      <TableCell>
                        <IconButton onClick={() => openDetailDialog(tracker)}>
                          <ViewIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        {/* Detail Dialog */}
        <Dialog
          open={detailDialogOpen}
          onClose={() => setDetailDialogOpen(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Validation Details</DialogTitle>
          <DialogContent>
            {selectedTracker && (
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="h6">Basic Information</Typography>
                  <Typography><strong>Processing Date:</strong> {selectedTracker.processingDate}</Typography>
                  <Typography><strong>Service:</strong> {selectedTracker.serviceName}</Typography>
                  <Typography><strong>Sub-Service:</strong> {selectedTracker.subServiceName}</Typography>
                  <Typography><strong>File Type:</strong> {selectedTracker.fileType}</Typography>
                  <Typography><strong>Direction:</strong> {selectedTracker.direction}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="h6">Count Information</Typography>
                  <Typography><strong>Expected Count (EOT):</strong> {selectedTracker.eotCountValue || 'N/A'}</Typography>
                  <Typography><strong>Actual Count:</strong> {selectedTracker.actualCount}</Typography>
                  <Typography><strong>Discrepancy:</strong> {selectedTracker.discrepancyCount || 0}</Typography>
                  <Typography><strong>SOT Received:</strong> {selectedTracker.sotReceived ? 'Yes' : 'No'}</Typography>
                  <Typography><strong>EOT Received:</strong> {selectedTracker.eotReceived ? 'Yes' : 'No'}</Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="h6">Validation Status</Typography>
                  <Box sx={{ mb: 2 }}>
                    {getStatusChip(selectedTracker.validationStatus)}
                  </Box>
                  <Typography><strong>Message:</strong> {selectedTracker.validationMessage}</Typography>
                  {selectedTracker.eotFieldPath && (
                    <Typography><strong>EOT Field Path:</strong> {selectedTracker.eotFieldPath}</Typography>
                  )}
                  {selectedTracker.validatedAt && (
                    <Typography><strong>Validated At:</strong> {new Date(selectedTracker.validatedAt).toLocaleString()}</Typography>
                  )}
                </Grid>
              </Grid>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDetailDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      </Box>
    </LocalizationProvider>
  );
};

export default EotValidationDashboard;