import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Alert,
    Card,
    CardContent,
    Grid,
    LinearProgress,
    Chip,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    IconButton,
    Tooltip,
    Fab,
    Slider,
    Switch,
    FormControlLabel,
    CircularProgress
} from '@mui/material';
import {
    Compress as CompressIcon,
    Uncompress as DecompressIcon,
    Speed as SpeedIcon,
    Storage as StorageIcon,
    Assessment as TestIcon,
    CloudUpload as UploadIcon,
    GetApp as DownloadIcon,
    Info as InfoIcon,
    TrendingUp as TrendingUpIcon,
    Timer as TimerIcon
} from '@mui/icons-material';
import { compressionService } from '../services/compressionService';
import './CompressionManagement.css';

const CompressionManagement = ({ selectedTenant }) => {
    const [compressionTypes, setCompressionTypes] = useState([]);
    const [statistics, setStatistics] = useState({});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    
    // Dialog states
    const [testDialogOpen, setTestDialogOpen] = useState(false);
    const [compressDialogOpen, setCompressDialogOpen] = useState(false);
    const [recommendationsDialogOpen, setRecommendationsDialogOpen] = useState(false);
    
    // Form states
    const [selectedFile, setSelectedFile] = useState(null);
    const [selectedCompressionType, setSelectedCompressionType] = useState('GZIP');
    const [prioritizeSpeed, setPrioritizeSpeed] = useState(false);
    const [testResults, setTestResults] = useState(null);
    const [recommendations, setRecommendations] = useState(null);
    const [compressionProgress, setCompressionProgress] = useState(0);

    useEffect(() => {
        loadCompressionData();
    }, [selectedTenant]);

    const loadCompressionData = async () => {
        try {
            setLoading(true);
            setError(null);
            
            const [types, stats] = await Promise.all([
                compressionService.getCompressionTypes(),
                selectedTenant ? compressionService.getCompressionStatistics(selectedTenant) : Promise.resolve({})
            ]);
            
            setCompressionTypes(types);
            setStatistics(stats);
        } catch (error) {
            setError('Failed to load compression data: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleTestCompression = async () => {
        if (!selectedFile) return;
        
        try {
            setLoading(true);
            const results = await compressionService.testCompressionEfficiency(selectedFile);
            setTestResults(results);
        } catch (error) {
            setError('Failed to test compression: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCompressFile = async () => {
        if (!selectedFile) return;
        
        try {
            setLoading(true);
            setCompressionProgress(0);
            
            // Simulate progress for better UX
            const progressInterval = setInterval(() => {
                setCompressionProgress(prev => Math.min(prev + 10, 90));
            }, 200);
            
            const result = await compressionService.compressUploadedFile(selectedFile, selectedCompressionType);
            
            clearInterval(progressInterval);
            setCompressionProgress(100);
            
            // Show compression results
            const savings = compressionService.calculateSavings(result.originalSize, result.compressedSize);
            
            setError(null);
            setTestResults({
                ...result,
                savings,
                success: true
            });
            
        } catch (error) {
            setError('Failed to compress file: ' + error.message);
        } finally {
            setLoading(false);
            setTimeout(() => setCompressionProgress(0), 2000);
        }
    };

    const renderCompressionTypeCard = (type) => (
        <Grid item xs={12} sm={6} md={4} key={type.type}>
            <Card 
                sx={{ 
                    height: '100%',
                    cursor: 'pointer',
                    '&:hover': { transform: 'translateY(-2px)', boxShadow: 3 }
                }}
                onClick={() => setSelectedCompressionType(type.type)}
            >
                <CardContent>
                    <Box display="flex" alignItems="center" mb={1}>
                        <Chip
                            label={type.displayName}
                            color={compressionService.getCompressionTypeColor(type.type)}
                            size="small"
                            sx={{ mr: 1 }}
                        />
                        {type.isFastCompression && (
                            <Tooltip title="Fast Compression">
                                <SpeedIcon fontSize="small" color="primary" />
                            </Tooltip>
                        )}
                        {type.isHighCompressionRatio && (
                            <Tooltip title="High Compression Ratio">
                                <StorageIcon fontSize="small" color="success" />
                            </Tooltip>
                        )}
                    </Box>
                    <Typography variant="body2" color="text.secondary" mb={1}>
                        {type.description}
                    </Typography>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="caption">
                            Avg Ratio: {(type.averageCompressionRatio * 100).toFixed(1)}%
                        </Typography>
                        <Typography variant="caption">
                            Ext: {type.fileExtension || 'N/A'}
                        </Typography>
                    </Box>
                </CardContent>
            </Card>
        </Grid>
    );

    const renderTestResults = () => {
        if (!testResults) return null;
        
        return (
            <Box mt={3}>
                <Typography variant="h6" gutterBottom>
                    Compression Test Results
                </Typography>
                
                {testResults.success ? (
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                            <Card>
                                <CardContent>
                                    <Typography variant="h6" color="primary">
                                        {compressionService.formatFileSize(testResults.originalSize)}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Original Size
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <Card>
                                <CardContent>
                                    <Typography variant="h6" color="success.main">
                                        {compressionService.formatFileSize(testResults.compressedSize)}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Compressed Size
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                        <Grid item xs={12}>
                            <Card>
                                <CardContent>
                                    <Typography variant="h6" gutterBottom>
                                        Compression Details
                                    </Typography>
                                    <Grid container spacing={2}>
                                        <Grid item xs={6} sm={3}>
                                            <Typography variant="body2" color="text.secondary">
                                                Compression Ratio
                                            </Typography>
                                            <Typography variant="h6">
                                                {(testResults.compressionRatio * 100).toFixed(1)}%
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6} sm={3}>
                                            <Typography variant="body2" color="text.secondary">
                                                Space Saved
                                            </Typography>
                                            <Typography variant="h6" color="success.main">
                                                {testResults.savings?.formatted || 'N/A'}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6} sm={3}>
                                            <Typography variant="body2" color="text.secondary">
                                                Compression Time
                                            </Typography>
                                            <Typography variant="h6">
                                                {testResults.compressionTime || 0}ms
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={6} sm={3}>
                                            <Typography variant="body2" color="text.secondary">
                                                Compression Type
                                            </Typography>
                                            <Chip 
                                                label={testResults.compressionType || selectedCompressionType}
                                                size="small"
                                                color="primary"
                                            />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>
                ) : (
                    <TableContainer component={Paper}>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Compression Type</TableCell>
                                    <TableCell>Compression Ratio</TableCell>
                                    <TableCell>Time (ms)</TableCell>
                                    <TableCell>Estimated Size</TableCell>
                                    <TableCell>Savings</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {Object.entries(testResults.results || {}).map(([type, result]) => {
                                    const estimatedSize = Math.round(testResults.originalSize * result.ratio);
                                    const savings = compressionService.calculateSavings(testResults.originalSize, estimatedSize);
                                    
                                    return (
                                        <TableRow key={type}>
                                            <TableCell>
                                                <Chip 
                                                    label={type}
                                                    size="small"
                                                    color={compressionService.getCompressionTypeColor(type)}
                                                />
                                            </TableCell>
                                            <TableCell>{(result.ratio * 100).toFixed(1)}%</TableCell>
                                            <TableCell>
                                                {result.timeMs > 0 ? `${result.timeMs}ms` : 'Failed'}
                                            </TableCell>
                                            <TableCell>
                                                {compressionService.formatFileSize(estimatedSize)}
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2" color="success.main">
                                                    {savings.formatted} ({savings.percentage.toFixed(1)}%)
                                                </Typography>
                                            </TableCell>
                                        </TableRow>
                                    );
                                })}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Box>
        );
    };

    if (!selectedTenant) {
        return (
            <Box p={3}>
                <Alert severity="info">Please select a tenant to view compression management.</Alert>
            </Box>
        );
    }

    return (
        <Box p={3}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Typography variant="h4" component="h1">
                    File Compression Management
                </Typography>
                <Box>
                    <Button
                        variant="outlined"
                        startIcon={<TestIcon />}
                        onClick={() => setTestDialogOpen(true)}
                        sx={{ mr: 1 }}
                    >
                        Test Compression
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<CompressIcon />}
                        onClick={() => setCompressDialogOpen(true)}
                    >
                        Compress File
                    </Button>
                </Box>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Statistics Cards */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div">
                                {statistics.totalCompressedFiles || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Compressed Files
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="success.main">
                                {compressionService.formatFileSize(statistics.totalCompressionSavings || 0)}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Space Saved
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="info.main">
                                {((statistics.averageCompressionRatio || 0) * 100).toFixed(1)}%
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Avg Compression
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="primary.main">
                                {statistics.mostUsedCompressionType || 'GZIP'}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Most Used Type
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {/* Compression Types Overview */}
            <Typography variant="h5" gutterBottom>
                Available Compression Types
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
                {compressionTypes.map(renderCompressionTypeCard)}
            </Grid>

            {loading && <LinearProgress sx={{ mb: 2 }} />}

            {/* Test Compression Dialog */}
            <Dialog open={testDialogOpen} onClose={() => setTestDialogOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>Test Compression Efficiency</DialogTitle>
                <DialogContent>
                    <Box mt={2}>
                        <input
                            type="file"
                            onChange={(e) => setSelectedFile(e.target.files[0])}
                            style={{ width: '100%', marginBottom: '16px' }}
                        />
                        
                        {selectedFile && (
                            <Box mb={2}>
                                <Typography variant="body2">
                                    Selected: {selectedFile.name} ({compressionService.formatFileSize(selectedFile.size)})
                                </Typography>
                                
                                {/* File validation */}
                                {(() => {
                                    const validation = compressionService.validateFileForCompression(selectedFile);
                                    if (!validation.isValid) {
                                        return (
                                            <Alert severity="warning" sx={{ mt: 1 }}>
                                                {validation.errors.join(', ')}
                                            </Alert>
                                        );
                                    }
                                    
                                    const recommended = compressionService.getRecommendedCompressionType(selectedFile, prioritizeSpeed);
                                    return (
                                        <Alert severity="info" sx={{ mt: 1 }}>
                                            Recommended: {recommended}
                                        </Alert>
                                    );
                                })()}
                            </Box>
                        )}
                        
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={prioritizeSpeed}
                                    onChange={(e) => setPrioritizeSpeed(e.target.checked)}
                                />
                            }
                            label="Prioritize Speed over Compression Ratio"
                        />
                        
                        {renderTestResults()}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setTestDialogOpen(false)}>Close</Button>
                    <Button 
                        onClick={handleTestCompression} 
                        disabled={!selectedFile || loading}
                        variant="contained"
                        startIcon={<TestIcon />}
                    >
                        Test All Types
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Compress File Dialog */}
            <Dialog open={compressDialogOpen} onClose={() => setCompressDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Compress File</DialogTitle>
                <DialogContent>
                    <Box mt={2}>
                        <input
                            type="file"
                            onChange={(e) => setSelectedFile(e.target.files[0])}
                            style={{ width: '100%', marginBottom: '16px' }}
                        />
                        
                        {selectedFile && (
                            <Box mb={2}>
                                <Typography variant="body2" gutterBottom>
                                    File: {selectedFile.name} ({compressionService.formatFileSize(selectedFile.size)})
                                </Typography>
                                
                                <Typography variant="body2" color="text.secondary" gutterBottom>
                                    Estimated compression time: {compressionService.estimateCompressionTime(selectedFile.size, selectedCompressionType)}ms
                                </Typography>
                            </Box>
                        )}
                        
                        <FormControl fullWidth sx={{ mb: 2 }}>
                            <InputLabel>Compression Type</InputLabel>
                            <Select
                                value={selectedCompressionType}
                                label="Compression Type"
                                onChange={(e) => setSelectedCompressionType(e.target.value)}
                            >
                                {compressionTypes
                                    .filter(type => type.type !== 'NONE')
                                    .map((type) => (
                                        <MenuItem key={type.type} value={type.type}>
                                            <Box display="flex" alignItems="center" width="100%">
                                                <Typography sx={{ flexGrow: 1 }}>
                                                    {type.displayName}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    ~{(type.averageCompressionRatio * 100).toFixed(0)}%
                                                </Typography>
                                            </Box>
                                        </MenuItem>
                                    ))}
                            </Select>
                        </FormControl>
                        
                        {compressionProgress > 0 && (
                            <Box mb={2}>
                                <Typography variant="body2" gutterBottom>
                                    Compressing... {compressionProgress}%
                                </Typography>
                                <LinearProgress variant="determinate" value={compressionProgress} />
                            </Box>
                        )}
                        
                        {renderTestResults()}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setCompressDialogOpen(false)}>Cancel</Button>
                    <Button 
                        onClick={handleCompressFile} 
                        disabled={!selectedFile || loading}
                        variant="contained"
                        startIcon={<CompressIcon />}
                    >
                        Compress File
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Compression Usage Statistics */}
            {statistics.compressionTypes && (
                <Box mt={4}>
                    <Typography variant="h5" gutterBottom>
                        Compression Usage Statistics
                    </Typography>
                    
                    <TableContainer component={Paper}>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Compression Type</TableCell>
                                    <TableCell>Usage Count</TableCell>
                                    <TableCell>Average Ratio</TableCell>
                                    <TableCell>Total Savings</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {statistics.compressionTypes.map((typeStats) => (
                                    <TableRow key={typeStats.type}>
                                        <TableCell>
                                            <Chip 
                                                label={typeStats.displayName}
                                                size="small"
                                                color={compressionService.getCompressionTypeColor(typeStats.type)}
                                            />
                                        </TableCell>
                                        <TableCell>{typeStats.usage}</TableCell>
                                        <TableCell>
                                            {compressionTypes.find(t => t.type === typeStats.type)?.averageCompressionRatio 
                                                ? (compressionTypes.find(t => t.type === typeStats.type).averageCompressionRatio * 100).toFixed(1) + '%'
                                                : 'N/A'}
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2" color="success.main">
                                                {/* This would be calculated from actual usage data */}
                                                N/A
                                            </Typography>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Box>
            )}
        </Box>
    );
};

export default CompressionManagement;