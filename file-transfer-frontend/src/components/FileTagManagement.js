import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  CardActions,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Tooltip,
  Alert,
  AlertTitle,
  CircularProgress,
  Tabs,
  Tab,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  LocalOffer,
  Palette,
  Save,
  Cancel,
  Download,
  Upload,
  AutoAwesome,
  TrendingUp,
  Assessment,
  Visibility,
  VisibilityOff
} from '@mui/icons-material';
import { fileTaggingService } from '../services/fileTaggingService';
import './FileTagManagement.css';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

export const FileTagManagement = ({ selectedTenant = 'default' }) => {
  const [activeTab, setActiveTab] = useState(0);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Tag creation/editing
  const [showTagDialog, setShowTagDialog] = useState(false);
  const [editingTag, setEditingTag] = useState(null);
  const [tagForm, setTagForm] = useState({
    tagName: '',
    tagColor: '#2196f3',
    tagDescription: ''
  });
  
  // Statistics
  const [tagStatistics, setTagStatistics] = useState(null);
  const [mostUsedTags, setMostUsedTags] = useState([]);
  
  // Filters
  const [showSystemTags, setShowSystemTags] = useState(true);
  const [showUserTags, setShowUserTags] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchTags();
    fetchStatistics();
    fetchMostUsedTags();
    initializeSystemTags();
  }, [selectedTenant]);

  const fetchTags = async () => {
    setLoading(true);
    try {
      const tagsData = await fileTaggingService.getAllTags(selectedTenant);
      setTags(tagsData);
      setError(null);
    } catch (err) {
      setError('Failed to fetch tags: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const stats = await fileTaggingService.getTagStatistics(selectedTenant);
      setTagStatistics(fileTaggingService.formatTagStatistics(stats));
    } catch (err) {
      console.error('Error fetching tag statistics:', err);
    }
  };

  const fetchMostUsedTags = async () => {
    try {
      const mostUsed = await fileTaggingService.getMostUsedTags(selectedTenant, 10);
      setMostUsedTags(mostUsed);
    } catch (err) {
      console.error('Error fetching most used tags:', err);
    }
  };

  const initializeSystemTags = async () => {
    try {
      await fileTaggingService.initializeSystemTags(selectedTenant);
    } catch (err) {
      console.error('Error initializing system tags:', err);
    }
  };

  const handleCreateTag = async () => {
    try {
      setLoading(true);
      
      const validation = fileTaggingService.validateTagName(tagForm.tagName);
      if (!validation.isValid) {
        setError('Invalid tag name: ' + validation.errors.join(', '));
        return;
      }
      
      const colorValidation = fileTaggingService.validateTagColor(tagForm.tagColor);
      if (!colorValidation.isValid) {
        setError('Invalid tag color: ' + colorValidation.errors.join(', '));
        return;
      }
      
      if (editingTag) {
        await fileTaggingService.updateTag(
          editingTag.id,
          validation.normalizedName,
          colorValidation.normalizedColor,
          tagForm.tagDescription
        );
        setSuccess('Tag updated successfully');
      } else {
        await fileTaggingService.createTag(
          selectedTenant,
          validation.normalizedName,
          colorValidation.normalizedColor,
          tagForm.tagDescription,
          'user'
        );
        setSuccess('Tag created successfully');
      }
      
      setShowTagDialog(false);
      setEditingTag(null);
      resetTagForm();
      fetchTags();
      fetchStatistics();
      
    } catch (err) {
      setError('Failed to save tag: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEditTag = (tag) => {
    setEditingTag(tag);
    setTagForm({
      tagName: tag.tagName,
      tagColor: tag.tagColor,
      tagDescription: tag.tagDescription || ''
    });
    setShowTagDialog(true);
  };

  const handleDeleteTag = async (tagId) => {
    if (!window.confirm('Are you sure you want to delete this tag? This action cannot be undone.')) {
      return;
    }
    
    try {
      setLoading(true);
      await fileTaggingService.deleteTag(tagId);
      setSuccess('Tag deleted successfully');
      fetchTags();
      fetchStatistics();
    } catch (err) {
      setError('Failed to delete tag: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const resetTagForm = () => {
    setTagForm({
      tagName: '',
      tagColor: '#2196f3',
      tagDescription: ''
    });
  };

  const handleCloseDialog = () => {
    setShowTagDialog(false);
    setEditingTag(null);
    resetTagForm();
  };

  const exportTags = () => {
    const url = fileTaggingService.exportTagsToCSV(filteredTags);
    const a = document.createElement('a');
    a.href = url;
    a.download = `file-tags-${selectedTenant}-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Filter tags based on current filters
  const filteredTags = tags.filter(tag => {
    if (!showSystemTags && tag.isSystemTag) return false;
    if (!showUserTags && !tag.isSystemTag) return false;
    if (searchTerm && !tag.tagName.toLowerCase().includes(searchTerm.toLowerCase())) return false;
    return true;
  });

  const renderTagsOverview = () => {
    return (
      <Grid container spacing={3}>
        {/* Statistics Cards */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <LocalOffer sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" color="primary">
                {tagStatistics?.totalTags || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Tags
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <TrendingUp sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" color="success.main">
                {tagStatistics?.totalUsages || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Usages
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <Assessment sx={{ fontSize: 40, color: 'info.main', mb: 1 }} />
              <Typography variant="h4" color="info.main">
                {tagStatistics?.averageUsage || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Avg Usage
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <AutoAwesome sx={{ fontSize: 40, color: 'warning.main', mb: 1 }} />
              <Typography variant="h4" color="warning.main">
                {tagStatistics?.systemTags || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                System Tags
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Most Used Tags */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Most Used Tags
              </Typography>
              <List dense>
                {mostUsedTags.slice(0, 8).map((tag, index) => (
                  <ListItem key={tag.id}>
                    <Chip
                      label={fileTaggingService.formatTagDisplayText(tag)}
                      size="small"
                      style={{
                        backgroundColor: tag.tagColor,
                        color: fileTaggingService.getTagContrastColor(tag.tagColor),
                        marginRight: 8
                      }}
                    />
                    <ListItemText
                      primary={`${tag.usageCount} uses`}
                      secondary={tag.tagDescription}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* Quick Actions */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Quick Actions
              </Typography>
              <Box display="flex" flexDirection="column" gap={1}>
                <Button
                  startIcon={<Add />}
                  variant="contained"
                  onClick={() => setShowTagDialog(true)}
                  fullWidth
                >
                  Create New Tag
                </Button>
                <Button
                  startIcon={<Download />}
                  variant="outlined"
                  onClick={exportTags}
                  fullWidth
                >
                  Export Tags
                </Button>
                <Button
                  startIcon={<AutoAwesome />}
                  variant="outlined"
                  onClick={initializeSystemTags}
                  fullWidth
                >
                  Initialize System Tags
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  const renderTagsList = () => {
    return (
      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              All Tags ({filteredTags.length})
            </Typography>
            <Box display="flex" gap={1}>
              <TextField
                size="small"
                placeholder="Search tags..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                sx={{ width: 200 }}
              />
              <Button
                startIcon={<Add />}
                variant="contained"
                onClick={() => setShowTagDialog(true)}
              >
                New Tag
              </Button>
            </Box>
          </Box>

          <Box mb={2}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={showSystemTags}
                  onChange={(e) => setShowSystemTags(e.target.checked)}
                />
              }
              label="Show System Tags"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={showUserTags}
                  onChange={(e) => setShowUserTags(e.target.checked)}
                />
              }
              label="Show User Tags"
            />
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Tag</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Usage</TableCell>
                  <TableCell>Created By</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredTags.map((tag) => (
                  <TableRow key={tag.id}>
                    <TableCell>
                      <Chip
                        label={fileTaggingService.formatTagDisplayText(tag)}
                        style={{
                          backgroundColor: tag.tagColor,
                          color: fileTaggingService.getTagContrastColor(tag.tagColor)
                        }}
                      />
                    </TableCell>
                    <TableCell>{tag.tagDescription || '-'}</TableCell>
                    <TableCell>
                      <Chip
                        label={tag.isSystemTag ? 'System' : 'User'}
                        size="small"
                        color={tag.isSystemTag ? 'default' : 'primary'}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{tag.usageCount || 0}</TableCell>
                    <TableCell>{tag.createdBy || '-'}</TableCell>
                    <TableCell>
                      <Tooltip title="Edit Tag">
                        <IconButton
                          size="small"
                          onClick={() => handleEditTag(tag)}
                          disabled={tag.isSystemTag}
                        >
                          <Edit />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete Tag">
                        <IconButton
                          size="small"
                          onClick={() => handleDeleteTag(tag.id)}
                          disabled={tag.isSystemTag}
                          color="error"
                        >
                          <Delete />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    );
  };

  const renderTagDialog = () => {
    const tagColors = fileTaggingService.getTagColors();
    
    return (
      <Dialog open={showTagDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingTag ? 'Edit Tag' : 'Create New Tag'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Tag Name"
              value={tagForm.tagName}
              onChange={(e) => setTagForm({ ...tagForm, tagName: e.target.value })}
              margin="normal"
              required
              helperText="Use letters, numbers, spaces, hyphens, and underscores only"
            />
            
            <TextField
              fullWidth
              label="Description"
              value={tagForm.tagDescription}
              onChange={(e) => setTagForm({ ...tagForm, tagDescription: e.target.value })}
              margin="normal"
              multiline
              rows={2}
              helperText="Optional description for this tag"
            />
            
            <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
              Tag Color
            </Typography>
            <Grid container spacing={1}>
              {tagColors.map((color) => (
                <Grid item key={color.value}>
                  <Tooltip title={color.name}>
                    <Box
                      sx={{
                        width: 32,
                        height: 32,
                        backgroundColor: color.value,
                        border: tagForm.tagColor === color.value ? '3px solid #000' : '1px solid #ccc',
                        borderRadius: 1,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                      onClick={() => setTagForm({ ...tagForm, tagColor: color.value })}
                    >
                      {tagForm.tagColor === color.value && (
                        <Palette sx={{ color: fileTaggingService.getTagContrastColor(color.value), fontSize: 16 }} />
                      )}
                    </Box>
                  </Tooltip>
                </Grid>
              ))}
            </Grid>
            
            <TextField
              fullWidth
              label="Custom Color"
              value={tagForm.tagColor}
              onChange={(e) => setTagForm({ ...tagForm, tagColor: e.target.value })}
              margin="normal"
              helperText="Enter a hex color code (e.g., #2196f3)"
            />
            
            {/* Preview */}
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Preview:
              </Typography>
              <Chip
                label={fileTaggingService.formatTagDisplayText({ tagName: tagForm.tagName || 'Sample Tag' })}
                style={{
                  backgroundColor: tagForm.tagColor,
                  color: fileTaggingService.getTagContrastColor(tagForm.tagColor)
                }}
              />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={handleCreateTag}
            variant="contained"
            disabled={loading || !tagForm.tagName.trim()}
          >
            {editingTag ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    );
  };

  const renderStatistics = () => {
    if (!tagStatistics) {
      return (
        <Card>
          <CardContent sx={{ textAlign: 'center' }}>
            <CircularProgress />
            <Typography variant="body2" sx={{ mt: 2 }}>
              Loading statistics...
            </Typography>
          </CardContent>
        </Card>
      );
    }

    return (
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Tag Usage Statistics
              </Typography>
              
              <Grid container spacing={2}>
                <Grid item xs={6} md={3}>
                  <Box textAlign="center">
                    <Typography variant="h4" color="primary">
                      {tagStatistics.totalTags}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Tags
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={6} md={3}>
                  <Box textAlign="center">
                    <Typography variant="h4" color="success.main">
                      {tagStatistics.totalUsages}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Usages
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={6} md={3}>
                  <Box textAlign="center">
                    <Typography variant="h4" color="info.main">
                      {tagStatistics.systemTags}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      System Tags
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={6} md={3}>
                  <Box textAlign="center">
                    <Typography variant="h4" color="secondary.main">
                      {tagStatistics.userTags}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      User Tags
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Most Used Tags Chart */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Most Popular Tags
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Rank</TableCell>
                      <TableCell>Tag</TableCell>
                      <TableCell>Usage Count</TableCell>
                      <TableCell>Type</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {mostUsedTags.map((tag, index) => (
                      <TableRow key={tag.id}>
                        <TableCell>{index + 1}</TableCell>
                        <TableCell>
                          <Chip
                            label={fileTaggingService.formatTagDisplayText(tag)}
                            size="small"
                            style={{
                              backgroundColor: tag.tagColor,
                              color: fileTaggingService.getTagContrastColor(tag.tagColor)
                            }}
                          />
                        </TableCell>
                        <TableCell>{tag.usageCount}</TableCell>
                        <TableCell>
                          <Chip
                            label={tag.isSystemTag ? 'System' : 'User'}
                            size="small"
                            color={tag.isSystemTag ? 'default' : 'primary'}
                            variant="outlined"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <LocalOffer sx={{ mr: 2, verticalAlign: 'middle' }} />
        File Tag Management
      </Typography>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        Organize and categorize your file transfers with custom tags for better management and filtering.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          <AlertTitle>Error</AlertTitle>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)} sx={{ mb: 3 }}>
        <Tab label="Overview" />
        <Tab label="Manage Tags" />
        <Tab label="Statistics" />
      </Tabs>

      <TabPanel value={activeTab} index={0}>
        {renderTagsOverview()}
      </TabPanel>

      <TabPanel value={activeTab} index={1}>
        {renderTagsList()}
      </TabPanel>

      <TabPanel value={activeTab} index={2}>
        {renderStatistics()}
      </TabPanel>

      {renderTagDialog()}

      {loading && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999
          }}
        >
          <CircularProgress size={60} />
        </Box>
      )}
    </Box>
  );
};

export default FileTagManagement;