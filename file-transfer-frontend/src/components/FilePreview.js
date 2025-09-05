import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Paper,
  Chip,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Tab,
  IconButton,
  Tooltip,
  Divider
} from '@mui/material';
import {
  Close,
  Download,
  Fullscreen,
  Code,
  TableChart,
  Description,
  Image,
  PictureAsPdf,
  DataObject,
  Refresh
} from '@mui/icons-material';
import { filePreviewService } from '../services/filePreviewService';

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

export const FilePreview = ({ 
  open, 
  onClose, 
  filePath, 
  fileName, 
  fileSize,
  maxWidth = 'lg',
  fullScreen = false 
}) => {
  const [previewData, setPreviewData] = useState(null);
  const [metadata, setMetadata] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState(0);
  const [maxLines, setMaxLines] = useState(100);

  useEffect(() => {
    if (open && filePath) {
      loadPreview();
      loadMetadata();
    }
  }, [open, filePath, maxLines]);

  const loadPreview = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const preview = await filePreviewService.getFilePreview(filePath, maxLines);
      setPreviewData(preview);
    } catch (err) {
      setError(err.message || 'Failed to load file preview');
    } finally {
      setLoading(false);
    }
  };

  const loadMetadata = async () => {
    try {
      const meta = await filePreviewService.getFileMetadata(filePath);
      setMetadata(meta);
    } catch (err) {
      console.error('Error loading metadata:', err);
    }
  };

  const handleDownload = async () => {
    try {
      const blob = await filePreviewService.downloadFileForPreview(filePath);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'file';
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading file:', err);
    }
  };

  const renderPreviewContent = () => {
    if (loading) {
      return (
        <Box display="flex" justifyContent="center" alignItems="center" minHeight={200}>
          <CircularProgress />
          <Typography sx={{ ml: 2 }}>Loading preview...</Typography>
        </Box>
      );
    }

    if (error) {
      return (
        <Alert severity="error">
          <Typography variant="h6">Preview Error</Typography>
          <Typography>{error}</Typography>
          <Button onClick={loadPreview} startIcon={<Refresh />} sx={{ mt: 1 }}>
            Retry
          </Button>
        </Alert>
      );
    }

    if (!previewData) {
      return (
        <Typography color="text.secondary" align="center">
          No preview data available
        </Typography>
      );
    }

    if (previewData.isBinary) {
      return renderBinaryPreview();
    }

    switch (previewData.previewType) {
      case 'csv':
        return renderCsvPreview();
      case 'json':
        return renderJsonPreview();
      case 'xml':
        return renderXmlPreview();
      case 'image':
        return renderImagePreview();
      case 'pdf':
        return renderPdfPreview();
      default:
        return renderTextPreview();
    }
  };

  const renderTextPreview = () => {
    const content = Array.isArray(previewData.content) 
      ? previewData.content.join('\n') 
      : previewData.content;

    return (
      <Paper sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: 500, overflow: 'auto' }}>
        <pre style={{ 
          fontFamily: 'Monaco, Consolas, "Courier New", monospace',
          fontSize: '12px',
          lineHeight: '1.4',
          margin: 0,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word'
        }}>
          {content}
        </pre>
        
        {previewData.truncated && (
          <Alert severity="info" sx={{ mt: 2 }}>
            Preview truncated. Showing first {maxLines} lines.
            <Button 
              onClick={() => setMaxLines(maxLines + 500)} 
              size="small" 
              sx={{ ml: 1 }}
            >
              Load More
            </Button>
          </Alert>
        )}
      </Paper>
    );
  };

  const renderCsvPreview = () => {
    if (!Array.isArray(previewData.content) || previewData.content.length === 0) {
      return <Typography>No CSV data to display</Typography>;
    }

    const headers = previewData.content[0].split(',');
    const rows = previewData.content.slice(1, 11); // Show first 10 rows

    return (
      <Box>
        <Typography variant="h6" gutterBottom>
          CSV Preview ({previewData.content.length - 1} rows)
        </Typography>
        
        <TableContainer component={Paper} sx={{ maxHeight: 400 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={{ bgcolor: 'primary.50', fontWeight: 'bold' }}>
                  #
                </TableCell>
                {headers.map((header, index) => (
                  <TableCell 
                    key={index} 
                    sx={{ bgcolor: 'primary.50', fontWeight: 'bold' }}
                  >
                    {header.trim()}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, rowIndex) => {
                const cells = row.split(',');
                return (
                  <TableRow key={rowIndex}>
                    <TableCell sx={{ bgcolor: 'grey.50', fontWeight: 'bold' }}>
                      {rowIndex + 1}
                    </TableCell>
                    {cells.map((cell, cellIndex) => (
                      <TableCell key={cellIndex}>
                        {cell.trim()}
                      </TableCell>
                    ))}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
        
        {rows.length >= 10 && (
          <Alert severity="info" sx={{ mt: 2 }}>
            Showing first 10 rows. Total rows: {previewData.content.length - 1}
          </Alert>
        )}
      </Box>
    );
  };

  const renderJsonPreview = () => {
    const content = Array.isArray(previewData.content) 
      ? previewData.content.join('\n') 
      : previewData.content;

    try {
      const parsed = JSON.parse(content);
      const formatted = JSON.stringify(parsed, null, 2);
      
      return (
        <Box>
          <Typography variant="h6" gutterBottom>
            JSON Preview
          </Typography>
          
          <Paper sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: 500, overflow: 'auto' }}>
            <pre style={{ 
              fontFamily: 'Monaco, Consolas, "Courier New", monospace',
              fontSize: '12px',
              lineHeight: '1.4',
              margin: 0,
              color: '#1976d2'
            }}>
              {formatted}
            </pre>
          </Paper>
          
          <Alert severity="success" sx={{ mt: 2 }}>
            Valid JSON format
          </Alert>
        </Box>
      );
    } catch (error) {
      return (
        <Box>
          <Typography variant="h6" gutterBottom>
            JSON Preview (Invalid)
          </Typography>
          
          <Alert severity="error" sx={{ mb: 2 }}>
            Invalid JSON format: {error.message}
          </Alert>
          
          <Paper sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: 500, overflow: 'auto' }}>
            <pre style={{ 
              fontFamily: 'Monaco, Consolas, "Courier New", monospace',
              fontSize: '12px',
              lineHeight: '1.4',
              margin: 0
            }}>
              {content}
            </pre>
          </Paper>
        </Box>
      );
    }
  };

  const renderXmlPreview = () => {
    const content = Array.isArray(previewData.content) 
      ? previewData.content.join('\n') 
      : previewData.content;

    return (
      <Box>
        <Typography variant="h6" gutterBottom>
          XML Preview
        </Typography>
        
        <Paper sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: 500, overflow: 'auto' }}>
          <pre style={{ 
            fontFamily: 'Monaco, Consolas, "Courier New", monospace',
            fontSize: '12px',
            lineHeight: '1.4',
            margin: 0,
            color: '#d32f2f'
          }}>
            {content}
          </pre>
        </Paper>
      </Box>
    );
  };

  const renderBinaryPreview = () => {
    return (
      <Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          This is a binary file. Preview is not available for binary content.
        </Alert>
        
        {previewData.hexPreview && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Hex Preview (First 256 bytes)
            </Typography>
            <Paper sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: 300, overflow: 'auto' }}>
              <pre style={{ 
                fontFamily: 'Monaco, Consolas, "Courier New", monospace',
                fontSize: '11px',
                lineHeight: '1.3',
                margin: 0
              }}>
                {previewData.hexPreview}
              </pre>
            </Paper>
          </Box>
        )}
      </Box>
    );
  };

  const renderImagePreview = () => {
    return (
      <Box textAlign="center">
        <Typography variant="h6" gutterBottom>
          Image Preview
        </Typography>
        <img 
          src={`data:image/jpeg;base64,${previewData.content}`} 
          alt={fileName}
          style={{ 
            maxWidth: '100%', 
            maxHeight: '400px',
            border: '1px solid #e0e0e0',
            borderRadius: '4px'
          }}
          onError={() => setError('Failed to load image preview')}
        />
      </Box>
    );
  };

  const renderPdfPreview = () => {
    return (
      <Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          PDF files require a browser plugin for preview. Click download to view the full file.
        </Alert>
        <Box textAlign="center">
          <PictureAsPdf sx={{ fontSize: 64, color: 'error.main' }} />
          <Typography variant="h6" sx={{ mt: 2 }}>
            PDF Document
          </Typography>
          <Button 
            variant="contained" 
            onClick={handleDownload}
            startIcon={<Download />}
            sx={{ mt: 2 }}
          >
            Download PDF
          </Button>
        </Box>
      </Box>
    );
  };

  const renderFileInfo = () => {
    if (!previewData && !metadata) return null;

    const info = previewData || metadata;

    return (
      <Box>
        <Typography variant="h6" gutterBottom>
          File Information
        </Typography>
        
        <Box display="flex" flexWrap="wrap" gap={1} mb={2}>
          <Chip 
            label={`Size: ${filePreviewService.formatFileSize(info.fileSize)}`}
            variant="outlined"
            size="small"
          />
          <Chip 
            label={`Type: ${info.previewType || 'Unknown'}`}
            variant="outlined"
            size="small"
            color="primary"
          />
          {info.fileExtension && (
            <Chip 
              label={`Extension: ${info.fileExtension}`}
              variant="outlined"
              size="small"
              color="secondary"
            />
          )}
          {info.isBinary !== undefined && (
            <Chip 
              label={info.isBinary ? 'Binary' : 'Text'}
              variant="outlined"
              size="small"
              color={info.isBinary ? 'warning' : 'success'}
            />
          )}
        </Box>

        {info.structureInfo && (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Structure Information
            </Typography>
            <Box display="flex" flexWrap="wrap" gap={1}>
              {Object.entries(info.structureInfo).map(([key, value]) => (
                <Chip 
                  key={key}
                  label={`${key}: ${value}`}
                  variant="outlined"
                  size="small"
                />
              ))}
            </Box>
          </Box>
        )}
      </Box>
    );
  };

  const getPreviewIcon = () => {
    if (!previewData) return <Description />;
    
    const iconMap = {
      'text': <Description />,
      'csv': <TableChart />,
      'json': <DataObject />,
      'xml': <Code />,
      'image': <Image />,
      'pdf': <PictureAsPdf />
    };
    
    return iconMap[previewData.previewType] || <Description />;
  };

  const canShowPreview = () => {
    if (!filePath || !fileName) return false;
    
    const check = filePreviewService.canPreviewFile(fileName, fileSize);
    return check.canPreview;
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth={maxWidth} 
      fullWidth
      fullScreen={fullScreen}
    >
      <DialogTitle>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box display="flex" alignItems="center">
            {getPreviewIcon()}
            <Typography variant="h6" sx={{ ml: 1 }}>
              {fileName || 'File Preview'}
            </Typography>
          </Box>
          <Box>
            <Tooltip title="Download">
              <IconButton onClick={handleDownload}>
                <Download />
              </IconButton>
            </Tooltip>
            <Tooltip title="Close">
              <IconButton onClick={onClose}>
                <Close />
              </IconButton>
            </Tooltip>
          </Box>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        {!canShowPreview() ? (
          <Alert severity="warning">
            <Typography variant="h6">Preview Not Available</Typography>
            <Typography>
              {filePreviewService.canPreviewFile(fileName, fileSize).reason}
            </Typography>
            <Button 
              onClick={handleDownload} 
              startIcon={<Download />} 
              sx={{ mt: 1 }}
            >
              Download File
            </Button>
          </Alert>
        ) : (
          <Box>
            <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)} sx={{ mb: 2 }}>
              <Tab label="Preview" />
              <Tab label="Info" />
            </Tabs>

            <TabPanel value={activeTab} index={0}>
              {renderPreviewContent()}
            </TabPanel>

            <TabPanel value={activeTab} index={1}>
              {renderFileInfo()}
            </TabPanel>
          </Box>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        <Button 
          onClick={handleDownload} 
          variant="contained" 
          startIcon={<Download />}
        >
          Download
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default FilePreview;