import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Tooltip,
  Card,
  CardContent,
  Grid,
  Button,
  Tabs,
  Tab,
  Avatar,
  Divider,
  Alert
} from '@mui/material';
import {
  Schedule,
  CheckCircle,
  Error,
  Refresh,
  Visibility,
  PlayArrow,
  GetApp,
  TrendingUp,
  AccessTime,
  InsertDriveFile,
  Folder,
  CloudDone,
  CloudOff
} from '@mui/icons-material';
import { fileTransferAPI } from '../services/api';
import { fileExtensionService } from '../services/fileExtensionService';
import { format, formatDistanceToNow } from 'date-fns';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      {...other}
    >
      {value === index && <Box>{children}</Box>}
    </div>
  );
}

export const RecentFiles = ({ tenantId = 'default', onFileSelect, compact = false }) => {
  const [activeTab, setActiveTab] = useState(0);
  const [recentFiles, setRecentFiles] = useState([]);
  const [processedFiles, setProcessedFiles] = useState([]);
  const [failedFiles, setFailedFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchRecentFiles();
  }, [tenantId]);

  const fetchRecentFiles = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [recent, processed, failed] = await Promise.all([
        fileTransferAPI.get('/api/v1/file-transfers/recent', { 
          params: { tenantId, limit: 20 } 
        }),
        fileTransferAPI.get('/api/v1/file-transfers/recent/processed', { 
          params: { tenantId, limit: 10 } 
        }),
        fileTransferAPI.get('/api/v1/file-transfers/recent/failed', { 
          params: { tenantId, limit: 10 } 
        })
      ]);
      
      setRecentFiles(recent.data);
      setProcessedFiles(processed.data);
      setFailedFiles(failed.data);
    } catch (err) {
      setError('Failed to load recent files: ' + err.message);
      console.error('Error fetching recent files:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileClick = (file) => {
    if (onFileSelect) {
      onFileSelect(file);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED': return <CheckCircle color="success" />;
      case 'FAILED': return <Error color="error" />;
      case 'IN_PROGRESS': return <Schedule color="primary" />;
      case 'PENDING': return <AccessTime color="warning" />;
      case 'CANCELLED': return <CloudOff color="action" />;
      default: return <InsertDriveFile />;
    }
  };

  const getDirectionIcon = (direction) => {
    return direction === 'INBOUND' ? <GetApp color="info" /> : <CloudDone color="success" />;
  };

  const formatFileTime = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffHours = (now - date) / (1000 * 60 * 60);
    
    if (diffHours < 24) {
      return formatDistanceToNow(date, { addSuffix: true });
    } else {
      return format(date, 'MMM dd, yyyy HH:mm');
    }
  };

  const renderFileList = (files, emptyMessage = 'No recent files') => {
    if (files.length === 0) {
      return (
        <Box textAlign="center" py={4}>
          <InsertDriveFile sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
          <Typography color="text.secondary">
            {emptyMessage}
          </Typography>
        </Box>
      );
    }

    return (
      <List>
        {files.map((file, index) => (
          <ListItem 
            key={file.id || index}
            button={!!onFileSelect}
            onClick={() => handleFileClick(file)}
            sx={{
              borderRadius: 1,
              mb: 0.5,
              '&:hover': {
                bgcolor: 'action.hover'
              }
            }}
          >
            <ListItemIcon>
              <Avatar sx={{ bgcolor: 'transparent' }}>
                {getStatusIcon(file.status)}
              </Avatar>
            </ListItemIcon>
            
            <ListItemText
              primary={
                <Box display="flex" alignItems="center" gap={1}>
                  <Typography variant="body1" noWrap sx={{ maxWidth: 200 }}>
                    {file.fileName}
                  </Typography>
                  {file.fileExtension && (
                    <Chip
                      label={file.fileExtension}
                      size="small"
                      color={fileExtensionService.getExtensionColor(file.fileExtension)}
                      variant="outlined"
                    />
                  )}
                  <Tooltip title={`${file.direction} Transfer`}>
                    {getDirectionIcon(file.direction)}
                  </Tooltip>
                </Box>
              }
              secondary={
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    {file.serviceName}
                    {file.subServiceName && ` • ${file.subServiceName}`}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {formatFileTime(file.createdAt)}
                    {file.fileSize && ` • ${Math.round(file.fileSize / 1024)} KB`}
                  </Typography>
                </Box>
              }
            />
            
            <ListItemSecondaryAction>
              <Box display="flex" alignItems="center" gap={0.5}>
                <Chip
                  label={file.status}
                  size="small"
                  color={
                    file.status === 'COMPLETED' ? 'success' :
                    file.status === 'FAILED' ? 'error' :
                    file.status === 'IN_PROGRESS' ? 'primary' : 'default'
                  }
                />
                {onFileSelect && (
                  <Tooltip title="View Details">
                    <IconButton size="small" onClick={() => handleFileClick(file)}>
                      <Visibility />
                    </IconButton>
                  </Tooltip>
                )}
              </Box>
            </ListItemSecondaryAction>
          </ListItem>
        ))}
      </List>
    );
  };

  const renderCompactView = () => {
    const displayFiles = recentFiles.slice(0, 5);
    
    return (
      <Card>
        <CardContent>
          <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
            <Typography variant="h6">
              <Schedule sx={{ mr: 1, verticalAlign: 'middle' }} />
              Recent Files
            </Typography>
            <Tooltip title="Refresh">
              <IconButton onClick={fetchRecentFiles} size="small">
                <Refresh />
              </IconButton>
            </Tooltip>
          </Box>
          
          {loading ? (
            <Box textAlign="center" py={2}>
              <Typography color="text.secondary">Loading...</Typography>
            </Box>
          ) : error ? (
            <Alert severity="error" size="small">
              {error}
            </Alert>
          ) : (
            renderFileList(displayFiles, 'No recent files')
          )}
          
          {recentFiles.length > 5 && (
            <Box textAlign="center" mt={2}>
              <Button size="small" onClick={() => onFileSelect && onFileSelect('view-all')}>
                View All Recent Files
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>
    );
  };

  const renderFullView = () => {
    return (
      <Box>
        <Box display="flex" alignItems="center" justifyContent="space-between" mb={3}>
          <Typography variant="h4">
            <Schedule sx={{ mr: 2, verticalAlign: 'middle' }} />
            Recent Files
          </Typography>
          <Button
            startIcon={<Refresh />}
            onClick={fetchRecentFiles}
            disabled={loading}
          >
            Refresh
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Statistics Cards */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <TrendingUp sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
                <Typography variant="h4" color="primary">
                  {recentFiles.length}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Recent Files
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <CheckCircle sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
                <Typography variant="h4" color="success.main">
                  {processedFiles.length}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Recently Processed
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Error sx={{ fontSize: 40, color: 'error.main', mb: 1 }} />
                <Typography variant="h4" color="error.main">
                  {failedFiles.length}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Recent Failures
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Tabbed File Lists */}
        <Paper>
          <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
            <Tab label={`All Recent (${recentFiles.length})`} />
            <Tab label={`Processed (${processedFiles.length})`} />
            <Tab label={`Failed (${failedFiles.length})`} />
          </Tabs>

          <TabPanel value={activeTab} index={0}>
            {renderFileList(recentFiles, 'No recent files found')}
          </TabPanel>

          <TabPanel value={activeTab} index={1}>
            {renderFileList(processedFiles, 'No recently processed files')}
          </TabPanel>

          <TabPanel value={activeTab} index={2}>
            {renderFileList(failedFiles, 'No recent failures')}
          </TabPanel>
        </Paper>
      </Box>
    );
  };

  if (compact) {
    return renderCompactView();
  }

  return renderFullView();
};

export default RecentFiles;