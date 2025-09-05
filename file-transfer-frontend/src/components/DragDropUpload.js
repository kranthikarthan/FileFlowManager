import React, { useState, useRef, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Grid,
  Card,
  CardContent,
  Divider
} from '@mui/material';
import {
  CloudUpload,
  InsertDriveFile,
  Delete,
  CheckCircle,
  Error,
  Warning,
  Refresh,
  PlayArrow,
  Stop,
  Folder
} from '@mui/icons-material';
import { fileTransferAPI } from '../services/api';
import { fileExtensionService } from '../services/fileExtensionService';
import { fileTaggingService } from '../services/fileTaggingService';
import './DragDropUpload.css';

export const DragDropUpload = ({ 
  tenantId = 'default',
  onUploadComplete,
  acceptedFileTypes = '',
  maxFileSize = 100 * 1024 * 1024, // 100MB default
  allowMultiple = true 
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadQueue, setUploadQueue] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [showConfigDialog, setShowConfigDialog] = useState(false);
  const [uploadConfig, setUploadConfig] = useState({
    serviceName: '',
    subServiceName: '',
    targetDirectory: '',
    autoProcess: true,
    compressionEnabled: false,
    autoTag: true
  });
  
  const fileInputRef = useRef(null);
  const dropZoneRef = useRef(null);

  // Drag and drop handlers
  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    const files = Array.from(e.dataTransfer.files);
    handleFiles(files);
  }, []);

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);
    handleFiles(files);
  };

  const handleFiles = (files) => {
    const validFiles = files.filter(file => validateFile(file));
    
    const newUploads = validFiles.map(file => ({
      id: Date.now() + Math.random(),
      file,
      name: file.name,
      size: file.size,
      status: 'pending', // pending, uploading, completed, error
      progress: 0,
      error: null,
      result: null,
      extension: fileExtensionService.extractExtension(file.name),
      suggestedTags: fileTaggingService.getTagSuggestions({
        fileName: file.name,
        fileSize: file.size,
        fileExtension: fileExtensionService.extractExtension(file.name)
      })
    }));

    setUploadQueue(prev => [...prev, ...newUploads]);
  };

  const validateFile = (file) => {
    // File size validation
    if (file.size > maxFileSize) {
      console.warn(`File ${file.name} exceeds maximum size limit`);
      return false;
    }

    // File type validation
    if (acceptedFileTypes && acceptedFileTypes.length > 0) {
      const extension = fileExtensionService.extractExtension(file.name);
      if (extension && !acceptedFileTypes.includes(extension)) {
        console.warn(`File type ${extension} not accepted for ${file.name}`);
        return false;
      }
    }

    return true;
  };

  const removeFromQueue = (uploadId) => {
    setUploadQueue(prev => prev.filter(upload => upload.id !== uploadId));
  };

  const clearQueue = () => {
    setUploadQueue([]);
  };

  const startUpload = async () => {
    if (!uploadConfig.serviceName) {
      setShowConfigDialog(true);
      return;
    }

    setUploading(true);

    for (const upload of uploadQueue.filter(u => u.status === 'pending')) {
      try {
        // Update status to uploading
        setUploadQueue(prev => prev.map(u => 
          u.id === upload.id ? { ...u, status: 'uploading', progress: 0 } : u
        ));

        // Create form data
        const formData = new FormData();
        formData.append('file', upload.file);
        formData.append('tenantId', tenantId);
        formData.append('serviceName', uploadConfig.serviceName);
        formData.append('subServiceName', uploadConfig.subServiceName || '');
        formData.append('targetDirectory', uploadConfig.targetDirectory || '');
        formData.append('autoProcess', uploadConfig.autoProcess);
        formData.append('compressionEnabled', uploadConfig.compressionEnabled);

        // Simulate upload progress
        const progressInterval = setInterval(() => {
          setUploadQueue(prev => prev.map(u => 
            u.id === upload.id && u.status === 'uploading' 
              ? { ...u, progress: Math.min(u.progress + 10, 90) } 
              : u
          ));
        }, 100);

        // Upload file (using existing API or create new upload endpoint)
        const response = await fileTransferAPI.uploadFile(formData);

        clearInterval(progressInterval);

        // Update status to completed
        setUploadQueue(prev => prev.map(u => 
          u.id === upload.id 
            ? { ...u, status: 'completed', progress: 100, result: response.data }
            : u
        ));

        // Auto-tag if enabled
        if (uploadConfig.autoTag && response.data.id) {
          try {
            await fileTaggingService.autoTagFileTransfer(response.data.id, tenantId);
          } catch (tagError) {
            console.warn('Auto-tagging failed:', tagError);
          }
        }

      } catch (error) {
        // Update status to error
        setUploadQueue(prev => prev.map(u => 
          u.id === upload.id 
            ? { ...u, status: 'error', error: error.message }
            : u
        ));
      }
    }

    setUploading(false);

    if (onUploadComplete) {
      const completedUploads = uploadQueue.filter(u => u.status === 'completed');
      onUploadComplete(completedUploads);
    }
  };

  const retryUpload = async (uploadId) => {
    const upload = uploadQueue.find(u => u.id === uploadId);
    if (!upload) return;

    setUploadQueue(prev => prev.map(u => 
      u.id === uploadId 
        ? { ...u, status: 'pending', progress: 0, error: null }
        : u
    ));
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'pending': return <Folder color="action" />;
      case 'uploading': return <CloudUpload color="primary" />;
      case 'completed': return <CheckCircle color="success" />;
      case 'error': return <Error color="error" />;
      default: return <InsertDriveFile />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending': return 'default';
      case 'uploading': return 'primary';
      case 'completed': return 'success';
      case 'error': return 'error';
      default: return 'default';
    }
  };

  const renderUploadZone = () => {
    return (
      <Paper
        ref={dropZoneRef}
        className={`upload-zone ${isDragOver ? 'drag-over' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        sx={{
          p: 4,
          textAlign: 'center',
          cursor: 'pointer',
          border: '2px dashed',
          borderColor: isDragOver ? 'primary.main' : 'grey.300',
          bgcolor: isDragOver ? 'primary.50' : 'grey.50',
          transition: 'all 0.3s ease',
          '&:hover': {
            borderColor: 'primary.main',
            bgcolor: 'primary.50'
          }
        }}
      >
        <CloudUpload sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
        <Typography variant="h6" gutterBottom>
          {isDragOver ? 'Drop files here' : 'Drag & drop files here'}
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          or click to browse and select files
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Max file size: {fileExtensionService.formatFileSize ? 
            fileExtensionService.formatFileSize(maxFileSize) : 
            Math.round(maxFileSize / 1024 / 1024) + 'MB'
          }
        </Typography>
        
        <input
          ref={fileInputRef}
          type="file"
          multiple={allowMultiple}
          accept={acceptedFileTypes}
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />
      </Paper>
    );
  };

  const renderUploadQueue = () => {
    if (uploadQueue.length === 0) return null;

    return (
      <Card sx={{ mt: 2 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Upload Queue ({uploadQueue.length} files)
            </Typography>
            <Box display="flex" gap={1}>
              <Button
                startIcon={<PlayArrow />}
                onClick={startUpload}
                disabled={uploading || uploadQueue.every(u => u.status !== 'pending')}
                variant="contained"
                size="small"
              >
                Start Upload
              </Button>
              <Button
                startIcon={<Delete />}
                onClick={clearQueue}
                disabled={uploading}
                variant="outlined"
                size="small"
                color="error"
              >
                Clear All
              </Button>
            </Box>
          </Box>

          <List>
            {uploadQueue.map((upload) => (
              <ListItem key={upload.id} divider>
                <ListItemIcon>
                  {getStatusIcon(upload.status)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="body1">{upload.name}</Typography>
                      <Chip
                        label={upload.status}
                        size="small"
                        color={getStatusColor(upload.status)}
                      />
                      {upload.extension && (
                        <Chip
                          label={upload.extension}
                          size="small"
                          variant="outlined"
                          color={fileExtensionService.getExtensionColor(upload.extension)}
                        />
                      )}
                    </Box>
                  }
                  secondary={
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Size: {Math.round(upload.size / 1024)} KB
                        {upload.error && (
                          <Typography component="span" color="error" sx={{ ml: 1 }}>
                            • Error: {upload.error}
                          </Typography>
                        )}
                      </Typography>
                      
                      {upload.status === 'uploading' && (
                        <LinearProgress 
                          variant="determinate" 
                          value={upload.progress} 
                          sx={{ mt: 1 }}
                        />
                      )}
                      
                      {upload.suggestedTags && upload.suggestedTags.length > 0 && (
                        <Box mt={1}>
                          <Typography variant="caption" color="text.secondary">
                            Suggested tags:
                          </Typography>
                          <Box display="flex" gap={0.5} mt={0.5} flexWrap="wrap">
                            {upload.suggestedTags.slice(0, 3).map((suggestion, index) => (
                              <Chip
                                key={index}
                                label={suggestion.tagName}
                                size="small"
                                variant="outlined"
                                color="secondary"
                                style={{ fontSize: '0.7rem' }}
                              />
                            ))}
                          </Box>
                        </Box>
                      )}
                    </Box>
                  }
                />
                <ListItemSecondaryAction>
                  {upload.status === 'error' && (
                    <Tooltip title="Retry Upload">
                      <IconButton
                        edge="end"
                        onClick={() => retryUpload(upload.id)}
                        color="primary"
                        size="small"
                      >
                        <Refresh />
                      </IconButton>
                    </Tooltip>
                  )}
                  <Tooltip title="Remove from Queue">
                    <IconButton
                      edge="end"
                      onClick={() => removeFromQueue(upload.id)}
                      disabled={upload.status === 'uploading'}
                      color="error"
                      size="small"
                    >
                      <Delete />
                    </IconButton>
                  </Tooltip>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </CardContent>
      </Card>
    );
  };

  const renderUploadConfig = () => {
    return (
      <Dialog open={showConfigDialog} onClose={() => setShowConfigDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Configuration</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" paragraph>
            Configure upload settings for the selected files.
          </Typography>
          
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Service Name"
                value={uploadConfig.serviceName}
                onChange={(e) => setUploadConfig({...uploadConfig, serviceName: e.target.value})}
                required
                helperText="Target service for file processing"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Sub-Service Name"
                value={uploadConfig.subServiceName}
                onChange={(e) => setUploadConfig({...uploadConfig, subServiceName: e.target.value})}
                helperText="Optional sub-service specification"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Target Directory"
                value={uploadConfig.targetDirectory}
                onChange={(e) => setUploadConfig({...uploadConfig, targetDirectory: e.target.value})}
                helperText="Target directory path (leave empty for default)"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Auto Process</InputLabel>
                <Select
                  value={uploadConfig.autoProcess}
                  onChange={(e) => setUploadConfig({...uploadConfig, autoProcess: e.target.value})}
                  label="Auto Process"
                >
                  <MenuItem value={true}>Yes - Process immediately</MenuItem>
                  <MenuItem value={false}>No - Queue for manual processing</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Enable Compression</InputLabel>
                <Select
                  value={uploadConfig.compressionEnabled}
                  onChange={(e) => setUploadConfig({...uploadConfig, compressionEnabled: e.target.value})}
                  label="Enable Compression"
                >
                  <MenuItem value={false}>No compression</MenuItem>
                  <MenuItem value={true}>Auto-compress if beneficial</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Auto Tagging</InputLabel>
                <Select
                  value={uploadConfig.autoTag}
                  onChange={(e) => setUploadConfig({...uploadConfig, autoTag: e.target.value})}
                  label="Auto Tagging"
                >
                  <MenuItem value={true}>Yes - Apply suggested tags automatically</MenuItem>
                  <MenuItem value={false}>No - Manual tagging only</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowConfigDialog(false)}>Cancel</Button>
          <Button 
            onClick={() => {
              setShowConfigDialog(false);
              startUpload();
            }}
            variant="contained"
            disabled={!uploadConfig.serviceName}
          >
            Start Upload
          </Button>
        </DialogActions>
      </Dialog>
    );
  };

  const renderUploadSummary = () => {
    if (uploadQueue.length === 0) return null;

    const summary = {
      total: uploadQueue.length,
      pending: uploadQueue.filter(u => u.status === 'pending').length,
      uploading: uploadQueue.filter(u => u.status === 'uploading').length,
      completed: uploadQueue.filter(u => u.status === 'completed').length,
      error: uploadQueue.filter(u => u.status === 'error').length
    };

    return (
      <Card sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Upload Summary
          </Typography>
          
          <Grid container spacing={2}>
            <Grid item xs={6} md={3}>
              <Box textAlign="center">
                <Typography variant="h4" color="primary">
                  {summary.total}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Total Files
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={6} md={3}>
              <Box textAlign="center">
                <Typography variant="h4" color="warning.main">
                  {summary.pending}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Pending
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={6} md={3}>
              <Box textAlign="center">
                <Typography variant="h4" color="success.main">
                  {summary.completed}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Completed
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={6} md={3}>
              <Box textAlign="center">
                <Typography variant="h4" color="error.main">
                  {summary.error}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Errors
                </Typography>
              </Box>
            </Grid>
          </Grid>
          
          {summary.completed === summary.total && summary.total > 0 && (
            <Alert severity="success" sx={{ mt: 2 }}>
              <Typography variant="body1">
                All files uploaded successfully! 
                {onUploadComplete && ' Check the file transfers list for processing status.'}
              </Typography>
            </Alert>
          )}
          
          {summary.error > 0 && (
            <Alert severity="error" sx={{ mt: 2 }}>
              <Typography variant="body1">
                {summary.error} file{summary.error !== 1 ? 's' : ''} failed to upload. 
                Check the queue for details and retry if needed.
              </Typography>
            </Alert>
          )}
        </CardContent>
      </Card>
    );
  };

  return (
    <Box className="drag-drop-upload-container">
      <Typography variant="h5" gutterBottom>
        <CloudUpload sx={{ mr: 1, verticalAlign: 'middle' }} />
        File Upload
      </Typography>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        Drag and drop files or click to browse. Files will be queued for upload and processing.
      </Typography>

      {renderUploadZone()}
      {renderUploadQueue()}
      {renderUploadSummary()}
      {renderUploadConfig()}
    </Box>
  );
};

export default DragDropUpload;