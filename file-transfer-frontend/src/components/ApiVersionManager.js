/**
 * API Version Manager Component
 * Provides UI for managing API versions, viewing compatibility, and handling migrations
 */

import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  AlertTitle,
  Chip,
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
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
  Divider,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  CheckCircle as CheckCircleIcon,
  Info as InfoIcon,
  Upgrade as UpgradeIcon,
  Timeline as TimelineIcon,
  Settings as SettingsIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import apiVersionService from '../services/apiVersionService';

const ApiVersionManager = ({ open, onClose }) => {
  const [currentVersion, setCurrentVersion] = useState('2.0');
  const [availableVersions, setAvailableVersions] = useState([]);
  const [versionInfo, setVersionInfo] = useState(null);
  const [migrationPlan, setMigrationPlan] = useState(null);
  const [breakingChanges, setBreakingChanges] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showMigrationDialog, setShowMigrationDialog] = useState(false);
  const [selectedTargetVersion, setSelectedTargetVersion] = useState('');

  useEffect(() => {
    if (open) {
      loadVersionInfo();
    }
  }, [open]);

  const loadVersionInfo = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Get current version info
      const currentVersionInfo = apiVersionService.getCurrentVersion();
      setCurrentVersion(currentVersionInfo.version);
      
      // Load available versions from server
      const response = await fetch('/api/versions');
      if (response.ok) {
        const data = await response.json();
        setAvailableVersions(data.supportedVersions || []);
        setVersionInfo(data);
      }
    } catch (err) {
      setError('Failed to load version information');
      console.error('Error loading version info:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleVersionChange = async (newVersion) => {
    setLoading(true);
    setError(null);
    
    try {
      await apiVersionService.setVersion(newVersion);
      setCurrentVersion(newVersion);
      
      // Reload version info
      await loadVersionInfo();
      
      // Show success message
      if (window.showNotification) {
        window.showNotification({
          type: 'success',
          message: `Successfully switched to API version ${newVersion}`
        });
      }
    } catch (err) {
      setError(`Failed to switch to version ${newVersion}: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleShowMigrationPlan = async (targetVersion) => {
    setSelectedTargetVersion(targetVersion);
    setLoading(true);
    
    try {
      const [migrationData, changesData] = await Promise.all([
        apiVersionService.getMigrationPlan(currentVersion, targetVersion),
        apiVersionService.getBreakingChanges(currentVersion, targetVersion)
      ]);
      
      setMigrationPlan(migrationData);
      setBreakingChanges(changesData);
      setShowMigrationDialog(true);
    } catch (err) {
      setError(`Failed to load migration plan: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const getVersionStatusColor = (version) => {
    if (!version) return 'default';
    
    if (version.deprecated) return 'error';
    if (version.status === 'beta') return 'warning';
    if (version.status === 'CURRENT') return 'success';
    return 'default';
  };

  const getVersionStatusIcon = (version) => {
    if (!version) return <InfoIcon />;
    
    if (version.deprecated) return <WarningIcon />;
    if (version.status === 'beta') return <InfoIcon />;
    if (version.status === 'CURRENT') return <CheckCircleIcon />;
    return <InfoIcon />;
  };

  const formatVersionFeatures = (features) => {
    if (!features || !Array.isArray(features)) return [];
    
    return features.map(feature => {
      // Convert feature names to readable format
      return feature.replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
    });
  };

  const renderVersionTable = () => (
    <TableContainer component={Paper} sx={{ mt: 2 }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Version</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Release Date</TableCell>
            <TableCell>Features</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {availableVersions.map((version) => (
            <TableRow key={version.version}>
              <TableCell>
                <Box display="flex" alignItems="center" gap={1}>
                  <Typography variant="body2" fontWeight="bold">
                    {version.version}
                  </Typography>
                  {version.version === currentVersion && (
                    <Chip label="Current" size="small" color="primary" />
                  )}
                </Box>
              </TableCell>
              <TableCell>
                <Chip
                  icon={getVersionStatusIcon(version)}
                  label={version.deprecated ? 'Deprecated' : version.status || 'Active'}
                  color={getVersionStatusColor(version)}
                  size="small"
                />
              </TableCell>
              <TableCell>
                <Typography variant="body2">
                  {version.releaseDate ? new Date(version.releaseDate).toLocaleDateString() : 'N/A'}
                </Typography>
              </TableCell>
              <TableCell>
                <Box display="flex" flexWrap="wrap" gap={0.5}>
                  {formatVersionFeatures(version.features).slice(0, 3).map((feature, index) => (
                    <Chip key={index} label={feature} size="small" variant="outlined" />
                  ))}
                  {version.features && version.features.length > 3 && (
                    <Chip label={`+${version.features.length - 3} more`} size="small" variant="outlined" />
                  )}
                </Box>
              </TableCell>
              <TableCell>
                <Box display="flex" gap={1}>
                  {version.version !== currentVersion && (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleVersionChange(version.version)}
                      disabled={loading}
                    >
                      Switch
                    </Button>
                  )}
                  {version.version !== currentVersion && (
                    <Button
                      size="small"
                      variant="text"
                      startIcon={<UpgradeIcon />}
                      onClick={() => handleShowMigrationPlan(version.version)}
                      disabled={loading}
                    >
                      Migrate
                    </Button>
                  )}
                </Box>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );

  const renderMigrationDialog = () => (
    <Dialog
      open={showMigrationDialog}
      onClose={() => setShowMigrationDialog(false)}
      maxWidth="md"
      fullWidth
    >
      <DialogTitle>
        <Box display="flex" alignItems="center" gap={1}>
          <UpgradeIcon />
          Migration Plan: {currentVersion} → {selectedTargetVersion}
        </Box>
      </DialogTitle>
      <DialogContent>
        {migrationPlan && (
          <Box>
            {/* Migration Overview */}
            <Alert severity={migrationPlan.migrationPlan?.feasible ? 'info' : 'error'} sx={{ mb: 2 }}>
              <AlertTitle>
                {migrationPlan.migrationPlan?.feasible ? 'Migration Feasible' : 'Migration Not Feasible'}
              </AlertTitle>
              {migrationPlan.migrationPlan?.reason && (
                <Typography variant="body2">{migrationPlan.migrationPlan.reason}</Typography>
              )}
              {migrationPlan.migrationPlan?.estimatedEffort && (
                <Typography variant="body2">
                  Estimated Effort: <strong>{migrationPlan.migrationPlan.estimatedEffort}</strong>
                </Typography>
              )}
            </Alert>

            {/* Migration Steps */}
            {migrationPlan.migrationPlan?.steps && (
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6">Migration Steps</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <List>
                    {migrationPlan.migrationPlan.steps.map((step, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <Chip
                            label={index + 1}
                            size="small"
                            color={step.effort === 'HIGH' ? 'error' : step.effort === 'MEDIUM' ? 'warning' : 'success'}
                          />
                        </ListItemIcon>
                        <ListItemText
                          primary={step.title}
                          secondary={
                            <Box>
                              <Typography variant="body2">{step.description}</Typography>
                              <Chip
                                label={`${step.effort} effort`}
                                size="small"
                                color={step.effort === 'HIGH' ? 'error' : step.effort === 'MEDIUM' ? 'warning' : 'success'}
                                sx={{ mt: 1 }}
                              />
                            </Box>
                          }
                        />
                      </ListItem>
                    ))}
                  </List>
                </AccordionDetails>
              </Accordion>
            )}

            {/* Breaking Changes */}
            {breakingChanges && breakingChanges.breakingChanges && breakingChanges.breakingChanges.length > 0 && (
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Typography variant="h6">Breaking Changes</Typography>
                    <Chip
                      label={breakingChanges.breakingChanges.length}
                      size="small"
                      color="error"
                    />
                  </Box>
                </AccordionSummary>
                <AccordionDetails>
                  <List>
                    {breakingChanges.breakingChanges.map((change, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <ErrorIcon color="error" />
                        </ListItemIcon>
                        <ListItemText
                          primary={change.description}
                          secondary={
                            <Box>
                              <Typography variant="body2" color="text.secondary">
                                Endpoint: {change.endpoint}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                Type: {change.changeType}
                              </Typography>
                              {change.mitigation && (
                                <Typography variant="body2" color="primary">
                                  Mitigation: {change.mitigation}
                                </Typography>
                              )}
                            </Box>
                          }
                        />
                      </ListItem>
                    ))}
                  </List>
                </AccordionDetails>
              </Accordion>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setShowMigrationDialog(false)}>
          Close
        </Button>
        {migrationPlan?.migrationPlan?.feasible && (
          <Button
            variant="contained"
            onClick={() => handleVersionChange(selectedTargetVersion)}
            disabled={loading}
          >
            Proceed with Migration
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box display="flex" alignItems="center" gap={1}>
            <SettingsIcon />
            API Version Manager
          </Box>
          <Tooltip title="Refresh">
            <IconButton onClick={loadVersionInfo} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Current Version Info */}
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} md={4}>
                <Typography variant="h6" gutterBottom>
                  Current API Version
                </Typography>
                <Box display="flex" alignItems="center" gap={1}>
                  <Typography variant="h4" color="primary">
                    {currentVersion}
                  </Typography>
                  {versionInfo?.supportedVersions?.find(v => v.version === currentVersion)?.deprecated && (
                    <Chip
                      icon={<WarningIcon />}
                      label="Deprecated"
                      color="error"
                      size="small"
                    />
                  )}
                </Box>
              </Grid>
              <Grid item xs={12} md={4}>
                <FormControl fullWidth>
                  <InputLabel>Switch Version</InputLabel>
                  <Select
                    value={currentVersion}
                    onChange={(e) => handleVersionChange(e.target.value)}
                    disabled={loading}
                  >
                    {availableVersions.map((version) => (
                      <MenuItem key={version.version} value={version.version}>
                        <Box display="flex" alignItems="center" justifyContent="space-between" width="100%">
                          <span>{version.version}</span>
                          {version.deprecated && (
                            <Chip label="Deprecated" size="small" color="error" />
                          )}
                        </Box>
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2" color="text.secondary">
                  Strategy: {apiVersionService.versionStrategy}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Features: {apiVersionService.hasFeature('pagination') ? 'Enhanced' : 'Basic'}
                </Typography>
              </Grid>
            </Grid>
          </CardContent>
        </Card>

        {/* Version Table */}
        <Typography variant="h6" gutterBottom>
          Available Versions
        </Typography>
        {renderVersionTable()}

        {/* Compatibility Info */}
        {versionInfo && (
          <Card sx={{ mt: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Information
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2" gutterBottom>
                    <strong>Total Versions:</strong> {versionInfo.totalVersions}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Supported:</strong> {versionInfo.supportedVersions?.length || 0}
                  </Typography>
                  <Typography variant="body2" gutterBottom>
                    <strong>Deprecated:</strong> {versionInfo.deprecatedVersions?.length || 0}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2" color="text.secondary">
                    Last updated: {new Date().toLocaleString()}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>

      {/* Migration Dialog */}
      {renderMigrationDialog()}
    </Dialog>
  );
};

export default ApiVersionManager;