import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Alert,
  Switch,
  FormControlLabel,
  IconButton,
  Tooltip,
  Divider,
  useTheme,
  useMediaQuery
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Warning,
  Weekend,
  Event,
  Search,
  Download,
  Upload,
  AutoAwesome,
  CalendarMonth,
  ErrorOutline
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { holidayService } from '../../services/holidayService';

/**
 * Enhanced Holiday Management Component
 * Handles holiday management with edge case detection and Sunday collision handling
 */
const EnhancedHolidayManagement = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // State management
  const [holidays, setHolidays] = useState([]);
  const [tenants, setTenants] = useState([]);
  const [selectedTenant, setSelectedTenant] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingHoliday, setEditingHoliday] = useState(null);
  const [sundayDialogOpen, setSundayDialogOpen] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    tenantId: '',
    holidayName: '',
    holidayDate: new Date(),
    description: '',
    isRecurring: false,
    affectsProcessing: true
  });

  // Sunday management state
  const [sundayConfig, setSundayConfig] = useState({
    year: new Date().getFullYear(),
    startDate: new Date(),
    endDate: new Date(new Date().getFullYear(), 11, 31),
    holidayName: 'Sunday Holiday'
  });

  // Edge case detection state
  const [edgeCases, setEdgeCases] = useState([]);

  // Load initial data
  useEffect(() => {
    loadTenants();
  }, []);

  // Load holidays when tenant changes
  useEffect(() => {
    if (selectedTenant) {
      loadHolidays();
    }
  }, [selectedTenant]);

  /**
   * Load available tenants
   */
  const loadTenants = async () => {
    try {
      const tenantsData = await holidayService.getTenants();
      setTenants(tenantsData);
      if (tenantsData.length > 0 && !selectedTenant) {
        setSelectedTenant(tenantsData[0].tenantId);
      }
    } catch (err) {
      setError('Failed to load tenants: ' + err.message);
    }
  };

  /**
   * Load holidays for selected tenant
   */
  const loadHolidays = async () => {
    if (!selectedTenant) return;

    setLoading(true);
    try {
      const holidaysData = await holidayService.getHolidaysForTenant(selectedTenant);
      setHolidays(holidaysData);
      
      // Detect edge cases
      detectEdgeCases(holidaysData);
      
    } catch (err) {
      setError('Failed to load holidays: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Detect holiday edge cases (holiday + Sunday collisions)
   */
  const detectEdgeCases = (holidaysData) => {
    const cases = [];
    const currentYear = new Date().getFullYear();
    
    holidaysData.forEach(holiday => {
      const holidayDate = new Date(holiday.holidayDate);
      const isSunday = holidayDate.getDay() === 0;
      
      if (isSunday && holidayDate.getFullYear() === currentYear) {
        cases.push({
          type: 'holiday_sunday_collision',
          holidayId: holiday.id,
          holidayName: holiday.holidayName,
          date: holiday.holidayDate,
          message: `Holiday "${holiday.holidayName}" falls on Sunday`,
          severity: 'warning'
        });
      }
      
      // Check for holidays too close to each other
      const adjacentHolidays = holidaysData.filter(h => {
        const hDate = new Date(h.holidayDate);
        const daysDiff = Math.abs((hDate - holidayDate) / (1000 * 60 * 60 * 24));
        return h.id !== holiday.id && daysDiff <= 1;
      });
      
      if (adjacentHolidays.length > 0) {
        cases.push({
          type: 'adjacent_holidays',
          holidayId: holiday.id,
          holidayName: holiday.holidayName,
          date: holiday.holidayDate,
          message: `Holiday "${holiday.holidayName}" is adjacent to other holidays`,
          severity: 'info'
        });
      }
    });
    
    setEdgeCases(cases);
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Check for edge cases before saving
      const holidayDate = new Date(formData.holidayDate);
      const isSunday = holidayDate.getDay() === 0;
      
      if (isSunday) {
        const confirm = window.confirm(
          `WARNING: This holiday falls on Sunday (${holidayDate.toDateString()}). ` +
          'This may create processing conflicts. Continue?'
        );
        if (!confirm) {
          setLoading(false);
          return;
        }
      }

      let result;
      if (editingHoliday) {
        result = await holidayService.updateHoliday(editingHoliday.id, formData);
      } else {
        result = await holidayService.createHoliday(formData);
      }

      setSuccess(editingHoliday ? 'Holiday updated successfully' : 'Holiday created successfully');
      setDialogOpen(false);
      resetForm();
      await loadHolidays();

    } catch (err) {
      setError('Failed to save holiday: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle holiday deletion
   */
  const handleDelete = async (holidayId, holidayName) => {
    const confirm = window.confirm(`Delete holiday "${holidayName}"?`);
    if (!confirm) return;

    setLoading(true);
    try {
      await holidayService.deleteHoliday(holidayId);
      setSuccess('Holiday deleted successfully');
      await loadHolidays();
    } catch (err) {
      setError('Failed to delete holiday: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle Sunday holiday management
   */
  const handleSundayHolidays = async (action) => {
    setLoading(true);
    try {
      let result;
      
      switch (action) {
        case 'CREATE_YEAR':
          result = await holidayService.createSundayHolidays(
            selectedTenant, 
            sundayConfig.year, 
            sundayConfig.holidayName
          );
          setSuccess(`Created ${result.length} Sunday holidays for ${sundayConfig.year}`);
          break;
          
        case 'CREATE_RANGE':
          result = await holidayService.createSundayHolidaysForDateRange(
            selectedTenant,
            sundayConfig.startDate,
            sundayConfig.endDate,
            sundayConfig.holidayName
          );
          setSuccess(`Created ${result.length} Sunday holidays for date range`);
          break;
          
        case 'REMOVE_YEAR':
          const removed = await holidayService.removeSundayHolidays(
            selectedTenant, 
            sundayConfig.year
          );
          setSuccess(`Removed ${removed} Sunday holidays for ${sundayConfig.year}`);
          break;
          
        default:
          throw new Error('Unknown Sunday holiday action');
      }

      setSundayDialogOpen(false);
      await loadHolidays();

    } catch (err) {
      setError('Failed to manage Sunday holidays: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Open edit dialog
   */
  const openEditDialog = (holiday = null) => {
    if (holiday) {
      setEditingHoliday(holiday);
      setFormData({
        tenantId: holiday.tenantId,
        holidayName: holiday.holidayName,
        holidayDate: new Date(holiday.holidayDate),
        description: holiday.description || '',
        isRecurring: holiday.isRecurring || false,
        affectsProcessing: holiday.affectsProcessing !== false
      });
    } else {
      setEditingHoliday(null);
      setFormData({
        tenantId: selectedTenant,
        holidayName: '',
        holidayDate: new Date(),
        description: '',
        isRecurring: false,
        affectsProcessing: true
      });
    }
    setDialogOpen(true);
  };

  /**
   * Reset form
   */
  const resetForm = () => {
    setFormData({
      tenantId: selectedTenant,
      holidayName: '',
      holidayDate: new Date(),
      description: '',
      isRecurring: false,
      affectsProcessing: true
    });
    setEditingHoliday(null);
  };

  /**
   * Render edge case alerts
   */
  const renderEdgeCaseAlerts = () => {
    if (edgeCases.length === 0) return null;

    return (
      <Card elevation={1} sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom color="warning.main">
            <Warning sx={{ mr: 1, verticalAlign: 'middle' }} />
            Holiday Edge Cases Detected
          </Typography>
          
          {edgeCases.map((edgeCase, index) => (
            <Alert key={index} severity={edgeCase.severity} sx={{ mb: 1 }}>
              <Typography variant="body2">
                {edgeCase.message}
              </Typography>
              <Typography variant="caption" color="textSecondary">
                Date: {new Date(edgeCase.date).toDateString()}
              </Typography>
            </Alert>
          ))}
        </CardContent>
      </Card>
    );
  };

  /**
   * Render holidays table
   */
  const renderHolidaysTable = () => (
    <TableContainer component={Paper} elevation={2}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Holiday Name</TableCell>
            <TableCell>Date</TableCell>
            <TableCell>Day of Week</TableCell>
            <TableCell>Edge Cases</TableCell>
            <TableCell>Affects Processing</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {holidays.map((holiday) => {
            const holidayDate = new Date(holiday.holidayDate);
            const isSunday = holidayDate.getDay() === 0;
            const dayName = holidayDate.toLocaleDateString('en-US', { weekday: 'long' });
            
            return (
              <TableRow key={holiday.id}>
                <TableCell>
                  <Typography variant="body2">
                    {holiday.holidayName}
                  </Typography>
                  {holiday.description && (
                    <Typography variant="caption" color="textSecondary">
                      {holiday.description}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  {holidayDate.toDateString()}
                </TableCell>
                <TableCell>
                  <Box display="flex" alignItems="center" gap={1}>
                    {dayName}
                    {isSunday && <Weekend color="warning" />}
                  </Box>
                </TableCell>
                <TableCell>
                  {isSunday && (
                    <Chip
                      size="small"
                      label="Sunday Collision"
                      color="warning"
                      icon={<Warning />}
                    />
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={holiday.affectsProcessing ? 'Yes' : 'No'}
                    color={holiday.affectsProcessing ? 'warning' : 'success'}
                  />
                </TableCell>
                <TableCell>
                  <Box display="flex" gap={1}>
                    <Tooltip title="Edit Holiday">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={() => openEditDialog(holiday)}
                      >
                        <Edit />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete Holiday">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDelete(holiday.id, holiday.holidayName)}
                      >
                        <Delete />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );

  /**
   * Render Sunday holiday management dialog
   */
  const renderSundayDialog = () => (
    <Dialog open={sundayDialogOpen} onClose={() => setSundayDialogOpen(false)} maxWidth="md" fullWidth>
      <DialogTitle>
        <Weekend sx={{ mr: 1, verticalAlign: 'middle' }} />
        Sunday Holiday Management
      </DialogTitle>
      <DialogContent>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Alert severity="info">
              Manage Sunday holidays for the entire year or specific date range. 
              This helps handle edge cases where holidays coincide with Sundays.
            </Alert>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              type="number"
              label="Year"
              value={sundayConfig.year}
              onChange={(e) => setSundayConfig(prev => ({ ...prev, year: parseInt(e.target.value) }))}
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Holiday Name"
              value={sundayConfig.holidayName}
              onChange={(e) => setSundayConfig(prev => ({ ...prev, holidayName: e.target.value }))}
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DatePicker
                label="Start Date"
                value={sundayConfig.startDate}
                onChange={(newValue) => setSundayConfig(prev => ({ ...prev, startDate: newValue }))}
                renderInput={(params) => <TextField {...params} fullWidth />}
              />
            </LocalizationProvider>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DatePicker
                label="End Date"
                value={sundayConfig.endDate}
                onChange={(newValue) => setSundayConfig(prev => ({ ...prev, endDate: newValue }))}
                renderInput={(params) => <TextField {...params} fullWidth />}
              />
            </LocalizationProvider>
          </Grid>
          
          <Grid item xs={12}>
            <Divider />
          </Grid>
          
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>
              Actions
            </Typography>
            <Box display="flex" gap={2} flexWrap="wrap">
              <Button
                variant="contained"
                color="primary"
                startIcon={<Add />}
                onClick={() => handleSundayHolidays('CREATE_YEAR')}
                disabled={loading}
              >
                Create All Sundays for Year
              </Button>
              
              <Button
                variant="contained"
                color="secondary"
                startIcon={<Event />}
                onClick={() => handleSundayHolidays('CREATE_RANGE')}
                disabled={loading}
              >
                Create Sundays for Date Range
              </Button>
              
              <Button
                variant="outlined"
                color="error"
                startIcon={<Delete />}
                onClick={() => handleSundayHolidays('REMOVE_YEAR')}
                disabled={loading}
              >
                Remove All Sundays for Year
              </Button>
            </Box>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setSundayDialogOpen(false)}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );

  /**
   * Render holiday form dialog
   */
  const renderHolidayDialog = () => (
    <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
      <DialogTitle>
        {editingHoliday ? 'Edit Holiday' : 'Add New Holiday'}
      </DialogTitle>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Holiday Name"
                value={formData.holidayName}
                onChange={(e) => setFormData(prev => ({ ...prev, holidayName: e.target.value }))}
                required
              />
            </Grid>
            
            <Grid item xs={12}>
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <DatePicker
                  label="Holiday Date"
                  value={formData.holidayDate}
                  onChange={(newValue) => {
                    setFormData(prev => ({ ...prev, holidayDate: newValue }));
                    
                    // Check for Sunday collision
                    if (newValue && newValue.getDay() === 0) {
                      setError('WARNING: This date falls on Sunday. This may create processing conflicts.');
                    } else {
                      setError(null);
                    }
                  }}
                  renderInput={(params) => <TextField {...params} fullWidth />}
                />
              </LocalizationProvider>
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isRecurring}
                    onChange={(e) => setFormData(prev => ({ ...prev, isRecurring: e.target.checked }))}
                  />
                }
                label="Recurring Holiday"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.affectsProcessing}
                    onChange={(e) => setFormData(prev => ({ ...prev, affectsProcessing: e.target.checked }))}
                  />
                }
                label="Affects File Processing"
              />
            </Grid>
            
            {/* Edge case warning */}
            {formData.holidayDate && new Date(formData.holidayDate).getDay() === 0 && (
              <Grid item xs={12}>
                <Alert severity="warning" icon={<Weekend />}>
                  <Typography variant="body2">
                    <strong>Sunday Collision Detected!</strong>
                  </Typography>
                  <Typography variant="caption">
                    This holiday falls on Sunday. Consider the following:
                  </Typography>
                  <ul style={{ margin: '8px 0', paddingLeft: '20px' }}>
                    <li>Processing may be automatically disabled on Sundays</li>
                    <li>Holiday + Sunday may trigger special business rules</li>
                    <li>Monitor processing status carefully on this date</li>
                  </ul>
                </Alert>
              </Grid>
            )}
          </Grid>
        </form>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setDialogOpen(false)}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} variant="contained" disabled={loading}>
          {editingHoliday ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 2 }}>
        {/* Header */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h4" gutterBottom>
            Holiday Management
          </Typography>
          <Box display="flex" gap={2}>
            <Button
              variant="outlined"
              startIcon={<AutoAwesome />}
              onClick={() => setSundayDialogOpen(true)}
            >
              Manage Sundays
            </Button>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => openEditDialog()}
            >
              Add Holiday
            </Button>
          </Box>
        </Box>

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
              <Grid item>
                <Typography variant="body2" color="textSecondary">
                  Total Holidays: {holidays.length}
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Edge Case Alerts */}
        {renderEdgeCaseAlerts()}

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

        {/* Holidays Table */}
        {renderHolidaysTable()}

        {/* Holiday Form Dialog */}
        {renderHolidayDialog()}

        {/* Sunday Management Dialog */}
        {renderSundayDialog()}
      </Box>
    </LocalizationProvider>
  );
};

export default EnhancedHolidayManagement;