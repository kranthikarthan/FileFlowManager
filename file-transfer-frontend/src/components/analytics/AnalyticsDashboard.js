import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Chip,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Paper,
  useTheme,
  useMediaQuery
} from '@mui/material';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import {
  TrendingUp,
  Assessment,
  Speed,
  Security,
  AttachMoney,
  Warning,
  CheckCircle,
  Error,
  Info,
  Refresh,
  Download,
  FilterList
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { analyticsService } from '../../services/analyticsService';
import { formatBytes, formatDuration, formatCurrency } from '../../utils/formatters';

/**
 * Advanced Analytics Dashboard Component
 * Provides comprehensive business intelligence and real-time analytics
 */
const AnalyticsDashboard = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // State management
  const [selectedTab, setSelectedTab] = useState(0);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), // 30 days ago
    endDate: new Date()
  });
  const [selectedTenant, setSelectedTenant] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Analytics data state
  const [summaryData, setSummaryData] = useState(null);
  const [trendData, setTrendData] = useState([]);
  const [serviceData, setServiceData] = useState([]);
  const [realTimeData, setRealTimeData] = useState(null);
  const [predictiveData, setPredictiveData] = useState(null);

  // Available tenants
  const [tenants, setTenants] = useState([]);

  // Load initial data
  useEffect(() => {
    loadTenants();
  }, []);

  // Load analytics data when parameters change
  useEffect(() => {
    if (selectedTenant) {
      loadAnalyticsData();
    }
  }, [selectedTenant, dateRange]);

  // Real-time data refresh
  useEffect(() => {
    let interval;
    if (selectedTenant && selectedTab === 0) { // Real-time tab
      interval = setInterval(() => {
        loadRealTimeData();
      }, 30000); // Refresh every 30 seconds
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [selectedTenant, selectedTab]);

  /**
   * Load available tenants
   */
  const loadTenants = async () => {
    try {
      const tenantsData = await analyticsService.getTenants();
      setTenants(tenantsData);
      if (tenantsData.length > 0 && !selectedTenant) {
        setSelectedTenant(tenantsData[0].tenantId);
      }
    } catch (err) {
      console.error('Error loading tenants:', err);
      setError('Failed to load tenants');
    }
  };

  /**
   * Load comprehensive analytics data
   */
  const loadAnalyticsData = async () => {
    if (!selectedTenant) return;

    setLoading(true);
    setError(null);

    try {
      // Load all analytics data in parallel
      const [summary, trends, services, realTime, predictive] = await Promise.all([
        analyticsService.getAnalyticsSummary(selectedTenant, dateRange.startDate, dateRange.endDate),
        analyticsService.getTrendingAnalytics(selectedTenant, dateRange.startDate, dateRange.endDate),
        analyticsService.getServiceAnalytics(selectedTenant, dateRange.startDate, dateRange.endDate),
        analyticsService.getRealTimeDashboard(selectedTenant),
        analyticsService.getPredictiveAnalytics(selectedTenant, 30)
      ]);

      setSummaryData(summary);
      setTrendData(trends);
      setServiceData(services);
      setRealTimeData(realTime);
      setPredictiveData(predictive);

    } catch (err) {
      console.error('Error loading analytics data:', err);
      setError('Failed to load analytics data');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load real-time data only
   */
  const loadRealTimeData = async () => {
    if (!selectedTenant) return;

    try {
      const realTime = await analyticsService.getRealTimeDashboard(selectedTenant);
      setRealTimeData(realTime);
    } catch (err) {
      console.error('Error loading real-time data:', err);
    }
  };

  /**
   * Export analytics data
   */
  const handleExport = async () => {
    if (!selectedTenant) return;

    try {
      setLoading(true);
      await analyticsService.exportAnalyticsData(selectedTenant, dateRange.startDate, dateRange.endDate);
    } catch (err) {
      console.error('Error exporting data:', err);
      setError('Failed to export data');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Render summary metrics cards
   */
  const renderSummaryCards = () => {
    if (!summaryData) return null;

    const cards = [
      {
        title: 'Total Files',
        value: summaryData.totalFiles?.toLocaleString() || '0',
        icon: <Assessment />,
        color: theme.palette.primary.main
      },
      {
        title: 'Success Rate',
        value: `${(summaryData.successRate || 0).toFixed(1)}%`,
        icon: <CheckCircle />,
        color: summaryData.successRate >= 95 ? theme.palette.success.main : theme.palette.warning.main
      },
      {
        title: 'Avg Processing Time',
        value: formatDuration((summaryData.avgProcessingTimeSeconds || 0) * 1000),
        icon: <Speed />,
        color: theme.palette.info.main
      },
      {
        title: 'Data Volume',
        value: formatBytes(summaryData.totalDataVolumeBytes || 0),
        icon: <TrendingUp />,
        color: theme.palette.secondary.main
      },
      {
        title: 'Quality Score',
        value: `${(summaryData.qualityScore || 0).toFixed(1)}%`,
        icon: <Security />,
        color: summaryData.qualityScore >= 90 ? theme.palette.success.main : theme.palette.error.main
      },
      {
        title: 'Total Cost',
        value: formatCurrency(summaryData.totalEstimatedCost || 0),
        icon: <AttachMoney />,
        color: theme.palette.warning.main
      }
    ];

    return (
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {cards.map((card, index) => (
          <Grid item xs={12} sm={6} md={4} lg={2} key={index}>
            <Card elevation={2}>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography variant="body2" color="textSecondary">
                      {card.title}
                    </Typography>
                    <Typography variant="h6" sx={{ color: card.color, fontWeight: 'bold' }}>
                      {card.value}
                    </Typography>
                  </Box>
                  <Box sx={{ color: card.color }}>
                    {card.icon}
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    );
  };

  /**
   * Render trending chart
   */
  const renderTrendChart = () => {
    if (!trendData || trendData.length === 0) return null;

    return (
      <Card elevation={2} sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            File Transfer Trends
          </Typography>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="totalFiles" 
                stroke={theme.palette.primary.main} 
                name="Total Files"
              />
              <Line 
                type="monotone" 
                dataKey="successfulTransfers" 
                stroke={theme.palette.success.main} 
                name="Successful"
              />
              <Line 
                type="monotone" 
                dataKey="failedTransfers" 
                stroke={theme.palette.error.main} 
                name="Failed"
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    );
  };

  /**
   * Render service breakdown chart
   */
  const renderServiceChart = () => {
    if (!serviceData || serviceData.length === 0) return null;

    const COLORS = [
      theme.palette.primary.main,
      theme.palette.secondary.main,
      theme.palette.success.main,
      theme.palette.warning.main,
      theme.palette.error.main,
      theme.palette.info.main
    ];

    return (
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Files by Service
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={serviceData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="totalFiles"
                  >
                    {serviceData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Service Performance
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={serviceData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="serviceName" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="successfulTransfers" fill={theme.palette.success.main} name="Successful" />
                  <Bar dataKey="failedTransfers" fill={theme.palette.error.main} name="Failed" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  /**
   * Render real-time dashboard
   */
  const renderRealTimeDashboard = () => {
    if (!realTimeData) return null;

    return (
      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Last Hour Activity
              </Typography>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="body2">Events</Typography>
                <Chip label={realTimeData.lastHourEvents || 0} color="primary" />
              </Box>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="body2">Uploads</Typography>
                <Chip label={realTimeData.lastHourUploads || 0} color="success" />
              </Box>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="body2">Failures</Typography>
                <Chip 
                  label={realTimeData.lastHourFailures || 0} 
                  color={realTimeData.lastHourFailures > 0 ? "error" : "default"} 
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                24 Hour Summary
              </Typography>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="body2">Total Events</Typography>
                <Chip label={realTimeData.last24HourEvents || 0} color="primary" />
              </Box>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="body2">Data Volume</Typography>
                <Chip label={formatBytes(realTimeData.last24HourDataVolume || 0)} color="info" />
              </Box>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="body2">Processing Queue</Typography>
                <Chip 
                  label={realTimeData.currentlyProcessing || 0} 
                  color={realTimeData.currentlyProcessing > 10 ? "warning" : "success"} 
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Errors
              </Typography>
              <Box sx={{ maxHeight: 200, overflowY: 'auto' }}>
                {realTimeData.recentErrors && realTimeData.recentErrors.length > 0 ? (
                  realTimeData.recentErrors.map((error, index) => (
                    <Alert key={index} severity="error" sx={{ mb: 1, fontSize: '0.8rem' }}>
                      <Typography variant="caption">
                        {error.serviceName}: {error.errorMessage}
                      </Typography>
                    </Alert>
                  ))
                ) : (
                  <Typography variant="body2" color="textSecondary">
                    No recent errors
                  </Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  /**
   * Render predictive analytics
   */
  const renderPredictiveAnalytics = () => {
    if (!predictiveData) return null;

    return (
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Volume Predictions (30 days)
              </Typography>
              {predictiveData.volumePredictions && (
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={predictiveData.volumePredictions.forecast || []}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip />
                    <Area 
                      type="monotone" 
                      dataKey="predictedFiles" 
                      stroke={theme.palette.primary.main} 
                      fill={theme.palette.primary.light}
                      name="Predicted Files"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recommendations
              </Typography>
              <Box sx={{ maxHeight: 300, overflowY: 'auto' }}>
                {predictiveData.recommendations && predictiveData.recommendations.length > 0 ? (
                  predictiveData.recommendations.map((rec, index) => (
                    <Alert key={index} severity="info" sx={{ mb: 1 }}>
                      <Typography variant="body2">
                        <strong>{rec.category}:</strong> {rec.recommendation}
                      </Typography>
                      {rec.estimatedImpact && (
                        <Typography variant="caption" color="textSecondary">
                          Impact: {rec.estimatedImpact}
                        </Typography>
                      )}
                    </Alert>
                  ))
                ) : (
                  <Typography variant="body2" color="textSecondary">
                    No recommendations available
                  </Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  /**
   * Render tab content
   */
  const renderTabContent = () => {
    switch (selectedTab) {
      case 0: // Overview
        return (
          <Box>
            {renderSummaryCards()}
            {renderTrendChart()}
            {renderServiceChart()}
          </Box>
        );
      case 1: // Real-time
        return renderRealTimeDashboard();
      case 2: // Predictive
        return renderPredictiveAnalytics();
      default:
        return null;
    }
  };

  if (loading && !summaryData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 2 }}>
        {/* Header */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h4" gutterBottom>
            Analytics Dashboard
          </Typography>
          <Box display="flex" gap={2} alignItems="center">
            <Button
              variant="outlined"
              startIcon={<Refresh />}
              onClick={loadAnalyticsData}
              disabled={loading}
            >
              Refresh
            </Button>
            <Button
              variant="outlined"
              startIcon={<Download />}
              onClick={handleExport}
              disabled={loading || !selectedTenant}
            >
              Export
            </Button>
          </Box>
        </Box>

        {/* Filters */}
        <Paper elevation={1} sx={{ p: 2, mb: 3 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth>
                <InputLabel>Tenant</InputLabel>
                <Select
                  value={selectedTenant}
                  onChange={(e) => setSelectedTenant(e.target.value)}
                  label="Tenant"
                >
                  {tenants.map((tenant) => (
                    <MenuItem key={tenant.tenantId} value={tenant.tenantId}>
                      {tenant.tenantName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <DatePicker
                label="Start Date"
                value={dateRange.startDate}
                onChange={(newValue) => setDateRange(prev => ({ ...prev, startDate: newValue }))}
                renderInput={(params) => <TextField {...params} fullWidth />}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <DatePicker
                label="End Date"
                value={dateRange.endDate}
                onChange={(newValue) => setDateRange(prev => ({ ...prev, endDate: newValue }))}
                renderInput={(params) => <TextField {...params} fullWidth />}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                variant="contained"
                startIcon={<FilterList />}
                onClick={loadAnalyticsData}
                disabled={loading || !selectedTenant}
                fullWidth
              >
                Apply Filters
              </Button>
            </Grid>
          </Grid>
        </Paper>

        {/* Error Display */}
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Tabs */}
        <Paper elevation={1} sx={{ mb: 2 }}>
          <Tabs 
            value={selectedTab} 
            onChange={(e, newValue) => setSelectedTab(newValue)}
            variant={isMobile ? "scrollable" : "standard"}
            scrollButtons="auto"
          >
            <Tab label="Overview" />
            <Tab label="Real-time" />
            <Tab label="Predictive" />
          </Tabs>
        </Paper>

        {/* Tab Content */}
        <Box>
          {renderTabContent()}
        </Box>

        {/* Loading Overlay */}
        {loading && (
          <Box 
            position="fixed" 
            top={0} 
            left={0} 
            width="100%" 
            height="100%" 
            bgcolor="rgba(255, 255, 255, 0.8)" 
            display="flex" 
            justifyContent="center" 
            alignItems="center"
            zIndex={9999}
          >
            <CircularProgress />
          </Box>
        )}
      </Box>
    </LocalizationProvider>
  );
};

export default AnalyticsDashboard;