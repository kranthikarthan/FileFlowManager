import React, { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Typography,
  Alert
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  Upload as UploadIcon
} from '@mui/icons-material';
import SchemaForm from './SchemaForm';
import SchemaViewer from './SchemaViewer';
import FileUploadDialog from './FileUploadDialog';

const SchemaList = ({ tenantId }) => {
  const [schemas, setSchemas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openForm, setOpenForm] = useState(false);
  const [openViewer, setOpenViewer] = useState(false);
  const [openUpload, setOpenUpload] = useState(false);
  const [selectedSchema, setSelectedSchema] = useState(null);
  const [serviceType, setServiceType] = useState('');

  useEffect(() => {
    if (tenantId) {
      fetchSchemas();
    }
  }, [tenantId]);

  const fetchSchemas = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/schemas/tenant/${tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setSchemas(data);
      } else {
        setError('Failed to fetch schemas');
      }
    } catch (err) {
      setError('Error fetching schemas: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSchema = () => {
    setSelectedSchema(null);
    setOpenForm(true);
  };

  const handleEditSchema = (schema) => {
    setSelectedSchema(schema);
    setOpenForm(true);
  };

  const handleViewSchema = (schema) => {
    setSelectedSchema(schema);
    setOpenViewer(true);
  };

  const handleDeleteSchema = async (schemaId) => {
    if (window.confirm('Are you sure you want to delete this schema?')) {
      try {
        const response = await fetch(`/api/schemas/${schemaId}`, {
          method: 'DELETE'
        });
        if (response.ok) {
          fetchSchemas();
        } else {
          setError('Failed to delete schema');
        }
      } catch (err) {
        setError('Error deleting schema: ' + err.message);
      }
    }
  };

  const handleFormClose = () => {
    setOpenForm(false);
    setSelectedSchema(null);
  };

  const handleFormSave = () => {
    fetchSchemas();
    handleFormClose();
  };

  const handleViewerClose = () => {
    setOpenViewer(false);
    setSelectedSchema(null);
  };

  const handleUploadClose = () => {
    setOpenUpload(false);
  };

  const getSchemaTypeColor = (type) => {
    const colors = {
      'CSV': 'primary',
      'JSON': 'success',
      'XML': 'warning',
      'FIXED_WIDTH': 'info'
    };
    return colors[type] || 'default';
  };

  const getStatusColor = (isActive) => {
    return isActive ? 'success' : 'error';
  };

  if (loading) {
    return <Typography>Loading schemas...</Typography>;
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">File Schemas</Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<UploadIcon />}
            onClick={() => setOpenUpload(true)}
            sx={{ mr: 1 }}
          >
            Validate File
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateSchema}
          >
            Create Schema
          </Button>
        </Box>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Schema Name</TableCell>
              <TableCell>Service Type</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created By</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {schemas.map((schema) => (
              <TableRow key={schema.id}>
                <TableCell>
                  <Typography variant="subtitle2">{schema.schemaName}</Typography>
                  {schema.description && (
                    <Typography variant="caption" color="textSecondary">
                      {schema.description}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  <Chip label={schema.serviceType} size="small" />
                </TableCell>
                <TableCell>
                  <Chip
                    label={schema.schemaType}
                    color={getSchemaTypeColor(schema.schemaType)}
                    size="small"
                  />
                </TableCell>
                <TableCell>{schema.schemaVersion}</TableCell>
                <TableCell>
                  <Chip
                    label={schema.isActive ? 'Active' : 'Inactive'}
                    color={getStatusColor(schema.isActive)}
                    size="small"
                  />
                </TableCell>
                <TableCell>{schema.createdBy}</TableCell>
                <TableCell>
                  {new Date(schema.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    onClick={() => handleViewSchema(schema)}
                    title="View Schema"
                  >
                    <ViewIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => handleEditSchema(schema)}
                    title="Edit Schema"
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => handleDeleteSchema(schema.id)}
                    title="Delete Schema"
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {schemas.length === 0 && (
        <Box textAlign="center" py={4}>
          <Typography variant="h6" color="textSecondary">
            No schemas found
          </Typography>
          <Typography variant="body2" color="textSecondary">
            Create your first schema to get started
          </Typography>
        </Box>
      )}

      {/* Schema Form Dialog */}
      <Dialog open={openForm} onClose={handleFormClose} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedSchema ? 'Edit Schema' : 'Create New Schema'}
        </DialogTitle>
        <DialogContent>
          <SchemaForm
            schema={selectedSchema}
            tenantId={tenantId}
            onSave={handleFormSave}
            onCancel={handleFormClose}
          />
        </DialogContent>
      </Dialog>

      {/* Schema Viewer Dialog */}
      <Dialog open={openViewer} onClose={handleViewerClose} maxWidth="lg" fullWidth>
        <DialogTitle>Schema Details</DialogTitle>
        <DialogContent>
          <SchemaViewer schema={selectedSchema} />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleViewerClose}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* File Upload Dialog */}
      <Dialog open={openUpload} onClose={handleUploadClose} maxWidth="sm" fullWidth>
        <DialogTitle>Validate File Against Schema</DialogTitle>
        <DialogContent>
          <FileUploadDialog
            tenantId={tenantId}
            onClose={handleUploadClose}
          />
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default SchemaList;