import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  Alert,
  Switch,
  FormControlLabel,
  CircularProgress
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { fileTransferAPI } from '../services/api';

export const ServiceManagement = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [services, setServices] = useState([]);
  const [serviceStats, setServiceStats] = useState({});

  useEffect(() => {
    fetchServiceData();
  }, []);

  const fetchServiceData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [servicesResponse, transfersResponse] = await Promise.all([
        fileTransferAPI.getDistinctServiceTypes(),
        fileTransferAPI.getAllFileTransfers()
      ]);

      const serviceTypes = servicesResponse.data;
      const allTransfers = transfersResponse.data;

      // Calculate statistics for each service
      const stats = {};
      serviceTypes.forEach(serviceType => {
        const serviceTransfers = allTransfers.filter(t => t.serviceType === serviceType);
        
        const statusCounts = serviceTransfers.reduce((acc, transfer) => {
          acc[transfer.status] = (acc[transfer.status] || 0) + 1;
          return acc;
        }, {});

        const recent = serviceTransfers.filter(transfer => {
          const transferDate = new Date(transfer.createdAt);
          const yesterday = new Date();
          yesterday.setDate(yesterday.getDate() - 1);
          return transferDate > yesterday;
        });

        stats[serviceType] = {
          total: serviceTransfers.length,
          statusCounts,
          recent: recent.length,
          lastTransfer: serviceTransfers.length > 0 
            ? new Date(Math.max(...serviceTransfers.map(t => new Date(t.createdAt))))
            : null
        };
      });

      setServices(serviceTypes);
      setServiceStats(stats);
    } catch (err) {
      setError('Failed to fetch service data');
      console.error('Error fetching service data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleServiceToggle = (serviceType, enabled) => {
    // This would typically call an API to enable/disable the service
    console.log(`${enabled ? 'Enabling' : 'Disabling'} service: ${serviceType}`);
    // For now, just show a message since this requires backend implementation
    setError(enabled 
      ? `Service ${serviceType} enabled (mock action)`
      : `Service ${serviceType} disabled (mock action)`
    );
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Service Management
      </Typography>

      {error && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {services.map(serviceType => {
          const stats = serviceStats[serviceType] || {};
          const isHealthy = stats.statusCounts?.FAILED ? 
            stats.statusCounts.FAILED / stats.total < 0.1 : true;

          return (
            <Grid item xs={12} md={6} lg={4} key={serviceType}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                    <Typography variant="h6" component="h2">
                      {serviceType}
                    </Typography>
                    <Chip 
                      label={isHealthy ? "Healthy" : "Issues"} 
                      color={isHealthy ? "success" : "error"}
                      size="small"
                    />
                  </Box>

                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        Total Transfers
                      </Typography>
                      <Typography variant="h6">
                        {stats.total || 0}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        Recent (24h)
                      </Typography>
                      <Typography variant="h6">
                        {stats.recent || 0}
                      </Typography>
                    </Grid>
                  </Grid>

                  {stats.statusCounts && (
                    <Box mt={2}>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        Status Distribution
                      </Typography>
                      <Box display="flex" gap={1} flexWrap="wrap">
                        {Object.entries(stats.statusCounts).map(([status, count]) => (
                          <Chip
                            key={status}
                            label={`${status.replace(/_/g, ' ')}: ${count}`}
                            size="small"
                            variant="outlined"
                          />
                        ))}
                      </Box>
                    </Box>
                  )}

                  {stats.lastTransfer && (
                    <Box mt={2}>
                      <Typography variant="body2" color="text.secondary">
                        Last Transfer: {stats.lastTransfer.toLocaleDateString()} {stats.lastTransfer.toLocaleTimeString()}
                      </Typography>
                    </Box>
                  )}
                </CardContent>
                
                <CardActions>
                  <FormControlLabel
                    control={
                      <Switch
                        defaultChecked={true}
                        onChange={(e) => handleServiceToggle(serviceType, e.target.checked)}
                        color="primary"
                      />
                    }
                    label="Enabled"
                  />
                  
                  <Button 
                    size="small" 
                    startIcon={<PlayArrowIcon />}
                    onClick={() => console.log(`Starting service: ${serviceType}`)}
                  >
                    Start
                  </Button>
                  
                  <Button 
                    size="small" 
                    startIcon={<StopIcon />}
                    color="error"
                    onClick={() => console.log(`Stopping service: ${serviceType}`)}
                  >
                    Stop
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          );
        })}
      </Grid>

      {services.length === 0 && !loading && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary">
            No services found
          </Typography>
          <Typography variant="body2" color="text.secondary" mt={1}>
            Services will appear here once file transfers are processed
          </Typography>
        </Paper>
      )}
    </Box>
  );
};