import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Button,
  Alert,
  Chip,
  IconButton,
  Tooltip,
  Grid
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import RefreshIcon from '@mui/icons-material/Refresh';
import RetryIcon from '@mui/icons-material/Replay';
import CancelIcon from '@mui/icons-material/Cancel';
import { format } from 'date-fns';
import { fileTransferAPI } from '../services/api';

const statusColors = {
  PENDING: 'warning',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  FAILED: 'error',
  CANCELLED: 'default',
  WAITING_FOR_END_MARKER: 'secondary'
};

export const FileTransferList = () => {
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [services, setServices] = useState([]);
  const [filters, setFilters] = useState({
    service: '',
    status: '',
    direction: ''
  });

  useEffect(() => {
    fetchData();
    fetchServices();
  }, []);

  useEffect(() => {
    fetchData();
  }, [filters]);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      let response;
      if (filters.service && filters.status) {
        response = await fileTransferAPI.getFileTransfersByServiceAndStatus(filters.service, filters.status);
      } else if (filters.service) {
        response = await fileTransferAPI.getFileTransfersByService(filters.service);
      } else if (filters.status) {
        response = await fileTransferAPI.getFileTransfersByStatus(filters.status);
      } else if (filters.direction) {
        response = await fileTransferAPI.getFileTransfersByDirection(filters.direction);
      } else {
        response = await fileTransferAPI.getAllFileTransfers();
      }
      
      setTransfers(response.data);
    } catch (err) {
      setError('Failed to fetch file transfers');
      console.error('Error fetching transfers:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchServices = async () => {
    try {
      const response = await fileTransferAPI.getDistinctServiceTypes();
      setServices(response.data);
    } catch (err) {
      console.error('Error fetching services:', err);
    }
  };

  const handleRetry = async (id) => {
    try {
      await fileTransferAPI.retryTransfer(id);
      fetchData(); // Refresh the data
    } catch (err) {
      setError('Failed to retry transfer');
      console.error('Error retrying transfer:', err);
    }
  };

  const handleCancel = async (id) => {
    try {
      await fileTransferAPI.cancelTransfer(id);
      fetchData(); // Refresh the data
    } catch (err) {
      setError('Failed to cancel transfer');
      console.error('Error cancelling transfer:', err);
    }
  };

  const handleFilterChange = (field) => (event) => {
    setFilters(prev => ({
      ...prev,
      [field]: event.target.value
    }));
  };

  const clearFilters = () => {
    setFilters({
      service: '',
      status: '',
      direction: ''
    });
  };

  const columns = [
    {
      field: 'id',
      headerName: 'ID',
      width: 90,
    },
    {
      field: 'fileName',
      headerName: 'File Name',
      width: 200,
      flex: 1,
    },
    {
      field: 'serviceType',
      headerName: 'Service',
      width: 120,
    },
    {
      field: 'direction',
      headerName: 'Direction',
      width: 100,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          size="small" 
          color={params.value === 'INBOUND' ? 'primary' : 'secondary'}
        />
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 150,
      renderCell: (params) => (
        <Chip 
          label={params.value.replace(/_/g, ' ')} 
          size="small" 
          color={statusColors[params.value]}
        />
      ),
    },
    {
      field: 'fileSize',
      headerName: 'Size',
      width: 100,
      renderCell: (params) => {
        if (!params.value) return '-';
        const size = parseInt(params.value);
        if (size < 1024) return `${size} B`;
        if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
        return `${(size / (1024 * 1024)).toFixed(1)} MB`;
      },
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 160,
      renderCell: (params) => format(new Date(params.value), 'yyyy-MM-dd HH:mm:ss'),
    },
    {
      field: 'processedAt',
      headerName: 'Processed',
      width: 160,
      renderCell: (params) => 
        params.value ? format(new Date(params.value), 'yyyy-MM-dd HH:mm:ss') : '-',
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <Tooltip title="Retry Transfer">
            <IconButton
              size="small"
              onClick={() => handleRetry(params.row.id)}
              disabled={!['FAILED', 'CANCELLED'].includes(params.row.status)}
            >
              <RetryIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Cancel Transfer">
            <IconButton
              size="small"
              onClick={() => handleCancel(params.row.id)}
              disabled={!['PENDING', 'FAILED'].includes(params.row.status)}
            >
              <CancelIcon />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        File Transfers
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Service</InputLabel>
              <Select
                value={filters.service}
                onChange={handleFilterChange('service')}
                label="Service"
              >
                <MenuItem value="">All</MenuItem>
                {services.map(service => (
                  <MenuItem key={service} value={service}>{service}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                value={filters.status}
                onChange={handleFilterChange('status')}
                label="Status"
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="FAILED">Failed</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
                <MenuItem value="WAITING_FOR_END_MARKER">Waiting for End Marker</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth size="small">
              <InputLabel>Direction</InputLabel>
              <Select
                value={filters.direction}
                onChange={handleFilterChange('direction')}
                label="Direction"
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="INBOUND">Inbound</MenuItem>
                <MenuItem value="OUTBOUND">Outbound</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} sm={6} md={2}>
            <Button variant="outlined" onClick={clearFilters} fullWidth>
              Clear Filters
            </Button>
          </Grid>
          
          <Grid item xs={12} sm={6} md={2}>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={fetchData}
              fullWidth
            >
              Refresh
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Data Grid */}
      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={transfers}
          columns={columns}
          pageSize={10}
          rowsPerPageOptions={[10, 25, 50]}
          loading={loading}
          disableSelectionOnClick
          density="compact"
        />
      </Paper>
    </Box>
  );
};