import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
  Alert,
  Tab,
  Tabs,
  Autocomplete,
  useMediaQuery,
  useTheme as useMuiTheme,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  FileCopy as CloneIcon,
  Visibility as ViewIcon,
  Search as SearchIcon,
  FilterList as FilterIcon
} from '@mui/icons-material';
import { useTheme } from '../theme/themeProvider';
import './SharedSchemaManagement.css';

const SharedSchemaManagement = ({ tenantId }) => {
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));
  const [schemas, setSchemas] = useState([]);
  const [filteredSchemas, setFilteredSchemas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Dialog states
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedSchema, setSelectedSchema] = useState(null);
  
  // Form states
  const [formData, setFormData] = useState({
    schemaName: '',
    schemaVersion: '1.0',
    schemaType: 'JSON_SCHEMA',
    fileType: 'JSON',
    schemaDefinition: '',
    description: '',
    isGlobal: false,
    supportsCountValidation: false,
    eotCountFieldPath: '',
    tags: []
  });
  
  // Filter and search states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterFileType, setFilterFileType] = useState('');
  const [filterSchemaType, setFilterSchemaType] = useState('');
  const [tabValue, setTabValue] = useState(0);
  
  const fileTypes = [
    'COBOL_FLAT_FILE',
    'BINARY_FILE', 
    'XML',
    'JSON',
    'CSV',
    'FIXED_WIDTH',
    'TEXT'
  ];
  
  const schemaTypes = [
    'JSON_SCHEMA',
    'XML_SCHEMA', 
    'COBOL_COPYBOOK',
    'CSV_DEFINITION',
    'FIXED_WIDTH_DEFINITION',
    'CUSTOM'
  ];

  useEffect(() => {
    loadSchemas();
  }, [tenantId]);

  useEffect(() => {
    applyFilters();
  }, [schemas, searchTerm, filterFileType, filterSchemaType, tabValue]);

  const loadSchemas = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/shared-schemas/tenant/${tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setSchemas(data);
      } else {
        setError('Failed to load schemas');
      }
    } catch (err) {
      setError('Error loading schemas: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = schemas;
    
    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(schema => 
        schema.schemaName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (schema.description && schema.description.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }
    
    // Apply file type filter
    if (filterFileType) {
      filtered = filtered.filter(schema => schema.fileType === filterFileType);
    }
    
    // Apply schema type filter
    if (filterSchemaType) {
      filtered = filtered.filter(schema => schema.schemaType === filterSchemaType);
    }
    
    // Apply tab filter
    switch (tabValue) {
      case 1: // Count validation schemas
        filtered = filtered.filter(schema => schema.supportsCountValidation);
        break;
      case 2: // Global schemas
        filtered = filtered.filter(schema => schema.isGlobal);
        break;
      case 3: // My schemas
        filtered = filtered.filter(schema => schema.tenantId === tenantId && !schema.isGlobal);
        break;
      default: // All schemas
        break;
    }
    
    setFilteredSchemas(filtered);
  };

  const handleCreateSchema = async () => {
    try {
      const response = await fetch('/api/shared-schemas', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...formData,
          tenantId: tenantId
        }),
      });

      if (response.ok) {
        setSuccess('Schema created successfully');
        setCreateDialogOpen(false);
        resetForm();
        loadSchemas();
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to create schema');
      }
    } catch (err) {
      setError('Error creating schema: ' + err.message);
    }
  };

  const handleUpdateSchema = async () => {
    try {
      const response = await fetch(`/api/shared-schemas/${selectedSchema.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        setSuccess('Schema updated successfully');
        setEditDialogOpen(false);
        resetForm();
        loadSchemas();
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to update schema');
      }
    } catch (err) {
      setError('Error updating schema: ' + err.message);
    }
  };

  const handleDeleteSchema = async (schemaId) => {
    if (!window.confirm('Are you sure you want to delete this schema?')) {
      return;
    }

    try {
      const response = await fetch(`/api/shared-schemas/${schemaId}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        setSuccess('Schema deleted successfully');
        loadSchemas();
      } else {
        const errorData = await response.json();
        if (errorData.type === 'SCHEMA_IN_USE') {
          setError('Cannot delete schema: ' + errorData.error);
        } else {
          setError(errorData.error || 'Failed to delete schema');
        }
      }
    } catch (err) {
      setError('Error deleting schema: ' + err.message);
    }
  };

  const handleCloneSchema = async (schema) => {
    const newName = prompt('Enter name for cloned schema:', schema.schemaName + '_copy');
    if (!newName) return;

    try {
      const response = await fetch(`/api/shared-schemas/${schema.id}/clone?targetTenantId=${tenantId}&newName=${encodeURIComponent(newName)}`, {
        method: 'POST',
      });

      if (response.ok) {
        setSuccess('Schema cloned successfully');
        loadSchemas();
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to clone schema');
      }
    } catch (err) {
      setError('Error cloning schema: ' + err.message);
    }
  };

  const openCreateDialog = () => {
    resetForm();
    setCreateDialogOpen(true);
  };

  const openEditDialog = (schema) => {
    setSelectedSchema(schema);
    setFormData({
      schemaName: schema.schemaName,
      schemaVersion: schema.schemaVersion,
      schemaType: schema.schemaType,
      fileType: schema.fileType,
      schemaDefinition: schema.schemaDefinition,
      description: schema.description || '',
      isGlobal: schema.isGlobal || false,
      supportsCountValidation: schema.supportsCountValidation || false,
      eotCountFieldPath: schema.eotCountFieldPath || '',
      tags: schema.tags || []
    });
    setEditDialogOpen(true);
  };

  const openViewDialog = (schema) => {
    setSelectedSchema(schema);
    setViewDialogOpen(true);
  };

  const resetForm = () => {
    setFormData({
      schemaName: '',
      schemaVersion: '1.0',
      schemaType: 'JSON_SCHEMA',
      fileType: 'JSON',
      schemaDefinition: '',
      description: '',
      isGlobal: false,
      supportsCountValidation: false,
      eotCountFieldPath: '',
      tags: []
    });
    setSelectedSchema(null);
  };

  const handleFormChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleTagsChange = (event, newTags) => {
    setFormData(prev => ({
      ...prev,
      tags: newTags
    }));
  };

  const renderSchemaForm = () => (
    <Grid container spacing={3}>
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Schema Name"
          value={formData.schemaName}
          onChange={(e) => handleFormChange('schemaName', e.target.value)}
          required
        />
      </Grid>
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Version"
          value={formData.schemaVersion}
          onChange={(e) => handleFormChange('schemaVersion', e.target.value)}
          required
        />
      </Grid>
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>Schema Type</InputLabel>
          <Select
            value={formData.schemaType}
            onChange={(e) => handleFormChange('schemaType', e.target.value)}
            label="Schema Type"
          >
            {schemaTypes.map(type => (
              <MenuItem key={type} value={type}>{type}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>File Type</InputLabel>
          <Select
            value={formData.fileType}
            onChange={(e) => handleFormChange('fileType', e.target.value)}
            label="File Type"
          >
            {fileTypes.map(type => (
              <MenuItem key={type} value={type}>{type}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>
      <Grid item xs={12}>
        <TextField
          fullWidth
          multiline
          rows={8}
          label="Schema Definition"
          value={formData.schemaDefinition}
          onChange={(e) => handleFormChange('schemaDefinition', e.target.value)}
          placeholder="Enter your schema definition here..."
          required
        />
      </Grid>
      <Grid item xs={12}>
        <TextField
          fullWidth
          label="Description"
          value={formData.description}
          onChange={(e) => handleFormChange('description', e.target.value)}
          multiline
          rows={2}
        />
      </Grid>
      <Grid item xs={12}>
        <Autocomplete
          multiple
          options={[]}
          freeSolo
          value={formData.tags}
          onChange={handleTagsChange}
          renderTags={(value, getTagProps) =>
            value.map((option, index) => (
              <Chip variant="outlined" label={option} {...getTagProps({ index })} />
            ))
          }
          renderInput={(params) => (
            <TextField
              {...params}
              label="Tags"
              placeholder="Add tags..."
            />
          )}
        />
      </Grid>
      <Grid item xs={12} md={6}>
        <FormControlLabel
          control={
            <Switch
              checked={formData.isGlobal}
              onChange={(e) => handleFormChange('isGlobal', e.target.checked)}
            />
          }
          label="Global Schema (available to all tenants)"
        />
      </Grid>
      <Grid item xs={12} md={6}>
        <FormControlLabel
          control={
            <Switch
              checked={formData.supportsCountValidation}
              onChange={(e) => handleFormChange('supportsCountValidation', e.target.checked)}
            />
          }
          label="Supports Count Validation (EOT)"
        />
      </Grid>
      {formData.supportsCountValidation && (
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="EOT Count Field Path"
            value={formData.eotCountFieldPath}
            onChange={(e) => handleFormChange('eotCountFieldPath', e.target.value)}
            placeholder="e.g., header.recordCount, /root/count, RECORD-COUNT"
            helperText="JSON path, XPath, or field name for count extraction"
          />
        </Grid>
      )}
    </Grid>
  );

  return (
    <Box className={`shared-schema-management ${isMobile ? 'mobile-container' : 'desktop-container'}`}>
      <Typography variant={isMobile ? "h5" : "h4"} gutterBottom>
        Shared Schema Management
      </Typography>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" onClose={() => setSuccess(null)} sx={{ mb: 2 }}>
          {success}
        </Alert>
      )}

      {/* Filters and Search */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                placeholder="Search schemas..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
                }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth>
                <InputLabel>File Type</InputLabel>
                <Select
                  value={filterFileType}
                  onChange={(e) => setFilterFileType(e.target.value)}
                  label="File Type"
                >
                  <MenuItem value="">All Types</MenuItem>
                  {fileTypes.map(type => (
                    <MenuItem key={type} value={type}>{type}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth>
                <InputLabel>Schema Type</InputLabel>
                <Select
                  value={filterSchemaType}
                  onChange={(e) => setFilterSchemaType(e.target.value)}
                  label="Schema Type"
                >
                  <MenuItem value="">All Types</MenuItem>
                  {schemaTypes.map(type => (
                    <MenuItem key={type} value={type}>{type}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<AddIcon />}
                onClick={openCreateDialog}
              >
                Add Schema
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
          <Tab label="All Schemas" />
          <Tab label="Count Validation" />
          <Tab label="Global Schemas" />
          <Tab label="My Schemas" />
        </Tabs>
      </Box>

      {/* Schema Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>File Type</TableCell>
              <TableCell>Usage</TableCell>
              <TableCell>Count Validation</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredSchemas.map((schema) => (
              <TableRow key={schema.id}>
                <TableCell>
                  <Box>
                    <Typography variant="body2" fontWeight="bold">
                      {schema.schemaName}
                    </Typography>
                    {schema.description && (
                      <Typography variant="caption" color="text.secondary">
                        {schema.description}
                      </Typography>
                    )}
                    {schema.tags && schema.tags.length > 0 && (
                      <Box sx={{ mt: 0.5 }}>
                        {schema.tags.map(tag => (
                          <Chip key={tag} label={tag} size="small" sx={{ mr: 0.5 }} />
                        ))}
                      </Box>
                    )}
                  </Box>
                </TableCell>
                <TableCell>{schema.schemaVersion}</TableCell>
                <TableCell>{schema.schemaType}</TableCell>
                <TableCell>
                  <Chip label={schema.fileType} size="small" />
                </TableCell>
                <TableCell>
                  <Box>
                    <Typography variant="body2">
                      {schema.usageCount || 0} uses
                    </Typography>
                    {schema.isGlobal && (
                      <Chip label="Global" size="small" color="primary" />
                    )}
                  </Box>
                </TableCell>
                <TableCell>
                  {schema.supportsCountValidation ? (
                    <Chip label="Yes" size="small" color="success" />
                  ) : (
                    <Chip label="No" size="small" color="default" />
                  )}
                </TableCell>
                <TableCell>
                  <IconButton onClick={() => openViewDialog(schema)}>
                    <ViewIcon />
                  </IconButton>
                  <IconButton onClick={() => openEditDialog(schema)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton onClick={() => handleCloneSchema(schema)}>
                    <CloneIcon />
                  </IconButton>
                  <IconButton 
                    onClick={() => handleDeleteSchema(schema.id)}
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

      {/* Create Dialog */}
      <Dialog 
        open={createDialogOpen} 
        onClose={() => setCreateDialogOpen(false)} 
        maxWidth="md" 
        fullWidth
        fullScreen={isMobile}
        PaperProps={{
          sx: {
            borderRadius: isMobile ? 0 : 3,
            background: isDark 
              ? 'rgba(26, 46, 26, 0.98)'
              : 'rgba(248, 253, 248, 0.98)',
            backdropFilter: 'blur(20px)',
          },
        }}
      >
        <DialogTitle>Create New Shared Schema</DialogTitle>
        <DialogContent>
          {renderSchemaForm()}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateSchema} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Edit Schema</DialogTitle>
        <DialogContent>
          {renderSchemaForm()}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleUpdateSchema} variant="contained">Update</Button>
        </DialogActions>
      </Dialog>

      {/* View Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="lg" fullWidth>
        <DialogTitle>Schema Details</DialogTitle>
        <DialogContent>
          {selectedSchema && (
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Typography variant="h6">Basic Information</Typography>
                <Typography><strong>Name:</strong> {selectedSchema.schemaName}</Typography>
                <Typography><strong>Version:</strong> {selectedSchema.schemaVersion}</Typography>
                <Typography><strong>Type:</strong> {selectedSchema.schemaType}</Typography>
                <Typography><strong>File Type:</strong> {selectedSchema.fileType}</Typography>
                <Typography><strong>Usage Count:</strong> {selectedSchema.usageCount || 0}</Typography>
                <Typography><strong>Global:</strong> {selectedSchema.isGlobal ? 'Yes' : 'No'}</Typography>
                <Typography><strong>Count Validation:</strong> {selectedSchema.supportsCountValidation ? 'Yes' : 'No'}</Typography>
                {selectedSchema.eotCountFieldPath && (
                  <Typography><strong>EOT Field Path:</strong> {selectedSchema.eotCountFieldPath}</Typography>
                )}
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6">Schema Definition</Typography>
                <TextField
                  fullWidth
                  multiline
                  rows={10}
                  value={selectedSchema.schemaDefinition}
                  InputProps={{ readOnly: true }}
                />
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SharedSchemaManagement;