import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Alert,
    IconButton,
    Tooltip,
    Grid,
    Card,
    CardContent,
    LinearProgress,
    Fab,
    Badge
} from '@mui/material';
import {
    CheckCircle as AckIcon,
    Cancel as NackIcon,
    Refresh as RefreshIcon,
    Upload as UploadIcon,
    Send as SendIcon,
    GetApp as DownloadIcon,
    Visibility as ViewIcon,
    Replay as RetryIcon
} from '@mui/icons-material';
import { ackNackService } from '../services/ackNackService';
import './AckNackManagement.css';

const AckNackManagement = ({ selectedTenant }) => {
    const [ackNackRecords, setAckNackRecords] = useState([]);
    const [statistics, setStatistics] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [filterType, setFilterType] = useState('ALL');
    const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
    const [nackDialogOpen, setNackDialogOpen] = useState(false);
    const [viewDialogOpen, setViewDialogOpen] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState(null);
    const [uploadFile, setUploadFile] = useState(null);
    const [nackReason, setNackReason] = useState({ code: '', description: '' });
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => {
        if (selectedTenant) {
            loadAckNackData();
        }
    }, [selectedTenant, filterStatus, filterType]);

    const loadAckNackData = async () => {
        if (!selectedTenant) return;
        
        try {
            setLoading(true);
            setError(null);
            
            let records;
            if (filterStatus !== 'ALL') {
                records = await ackNackService.getAckNackRecordsByStatus(selectedTenant, filterStatus);
            } else if (filterType !== 'ALL') {
                records = await ackNackService.getAckNackRecordsByType(selectedTenant, filterType);
            } else {
                records = await ackNackService.getAllAckNackRecords(selectedTenant);
            }
            
            const stats = await ackNackService.getAckNackStatistics(selectedTenant);
            
            setAckNackRecords(records);
            setStatistics(stats);
        } catch (error) {
            setError('Failed to load ACK/NACK data: ' + error.message);
            console.error('Error loading ACK/NACK data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await loadAckNackData();
        setRefreshing(false);
    };

    const handleUploadFile = async () => {
        if (!uploadFile) return;
        
        try {
            setLoading(true);
            await ackNackService.uploadAckNackFile(selectedTenant, uploadFile);
            setUploadDialogOpen(false);
            setUploadFile(null);
            await loadAckNackData();
        } catch (error) {
            setError('Failed to upload ACK/NACK file: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateNack = async () => {
        if (!selectedRecord || !nackReason.code) return;
        
        try {
            setLoading(true);
            await ackNackService.generateNack(
                selectedRecord.originalFileTransferId, 
                nackReason.code, 
                nackReason.description
            );
            setNackDialogOpen(false);
            setNackReason({ code: '', description: '' });
            setSelectedRecord(null);
            await loadAckNackData();
        } catch (error) {
            setError('Failed to generate NACK: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateAck = async (fileTransferId) => {
        try {
            setLoading(true);
            await ackNackService.generateAck(fileTransferId);
            await loadAckNackData();
        } catch (error) {
            setError('Failed to generate ACK: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleRetryAckNack = async (ackNackId) => {
        try {
            setLoading(true);
            await ackNackService.retryAckNack(ackNackId);
            await loadAckNackData();
        } catch (error) {
            setError('Failed to retry ACK/NACK: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleSendPending = async () => {
        try {
            setLoading(true);
            await ackNackService.sendPendingAckNackFiles(selectedTenant);
            await loadAckNackData();
        } catch (error) {
            setError('Failed to send pending ACK/NACK files: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const getStatusChip = (status) => (
        <Chip
            label={ackNackService.formatStatus(status)}
            color={ackNackService.getStatusColor(status)}
            size="small"
        />
    );

    const getTypeChip = (type) => (
        <Chip
            label={type}
            color={ackNackService.getTypeColor(type)}
            size="small"
            icon={type === 'ACK' ? <AckIcon /> : <NackIcon />}
        />
    );

    if (!selectedTenant) {
        return (
            <Box p={3}>
                <Alert severity="info">Please select a tenant to view ACK/NACK records.</Alert>
            </Box>
        );
    }

    return (
        <Box p={3}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Typography variant="h4" component="h1">
                    ACK/NACK Management
                </Typography>
                <Box>
                    <Button
                        variant="outlined"
                        startIcon={<UploadIcon />}
                        onClick={() => setUploadDialogOpen(true)}
                        sx={{ mr: 1 }}
                    >
                        Upload ACK/NACK
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<SendIcon />}
                        onClick={handleSendPending}
                        sx={{ mr: 1 }}
                    >
                        Send Pending
                    </Button>
                    <IconButton onClick={handleRefresh} disabled={refreshing}>
                        <RefreshIcon />
                    </IconButton>
                </Box>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Statistics Cards */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div">
                                {statistics.totalRecords || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Total Records
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="success.main">
                                {statistics.ackCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                ACK Files
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="error.main">
                                {statistics.nackCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                NACK Files
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="warning.main">
                                {statistics.pendingCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Pending
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="primary.main">
                                {statistics.sentCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Sent
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="error.main">
                                {statistics.failedCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Failed
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {/* Filters */}
            <Box display="flex" gap={2} mb={2}>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                    <InputLabel>Status</InputLabel>
                    <Select
                        value={filterStatus}
                        label="Status"
                        onChange={(e) => setFilterStatus(e.target.value)}
                    >
                        <MenuItem value="ALL">All Status</MenuItem>
                        <MenuItem value="PENDING">Pending</MenuItem>
                        <MenuItem value="GENERATED">Generated</MenuItem>
                        <MenuItem value="SENT">Sent</MenuItem>
                        <MenuItem value="RECEIVED">Received</MenuItem>
                        <MenuItem value="PROCESSED">Processed</MenuItem>
                        <MenuItem value="FAILED">Failed</MenuItem>
                        <MenuItem value="EXPIRED">Expired</MenuItem>
                    </Select>
                </FormControl>
                
                <FormControl size="small" sx={{ minWidth: 120 }}>
                    <InputLabel>Type</InputLabel>
                    <Select
                        value={filterType}
                        label="Type"
                        onChange={(e) => setFilterType(e.target.value)}
                    >
                        <MenuItem value="ALL">All Types</MenuItem>
                        <MenuItem value="ACK">ACK</MenuItem>
                        <MenuItem value="NACK">NACK</MenuItem>
                    </Select>
                </FormControl>
            </Box>

            {loading && <LinearProgress sx={{ mb: 2 }} />}

            {/* ACK/NACK Records Table */}
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Type</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Original File</TableCell>
                            <TableCell>ACK/NACK File</TableCell>
                            <TableCell>Service</TableCell>
                            <TableCell>Direction</TableCell>
                            <TableCell>Created</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {ackNackRecords.map((record) => (
                            <TableRow key={record.id} hover>
                                <TableCell>{getTypeChip(record.type)}</TableCell>
                                <TableCell>{getStatusChip(record.status)}</TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        {record.originalFileName}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2" noWrap>
                                        {record.ackNackFileName}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2">
                                        {record.serviceName}
                                        {record.subServiceName && (
                                            <Typography variant="caption" display="block">
                                                {record.subServiceName}
                                            </Typography>
                                        )}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Chip
                                        label={record.direction}
                                        color={record.direction === 'INBOUND' ? 'primary' : 'secondary'}
                                        size="small"
                                    />
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2">
                                        {new Date(record.createdAt).toLocaleDateString()}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {new Date(record.createdAt).toLocaleTimeString()}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Box display="flex" gap={1}>
                                        <Tooltip title="View Details">
                                            <IconButton
                                                size="small"
                                                onClick={() => {
                                                    setSelectedRecord(record);
                                                    setViewDialogOpen(true);
                                                }}
                                            >
                                                <ViewIcon />
                                            </IconButton>
                                        </Tooltip>
                                        
                                        {record.status === 'FAILED' && (
                                            <Tooltip title="Retry">
                                                <IconButton
                                                    size="small"
                                                    onClick={() => handleRetryAckNack(record.id)}
                                                    color="primary"
                                                >
                                                    <RetryIcon />
                                                </IconButton>
                                            </Tooltip>
                                        )}
                                        
                                        {record.content && (
                                            <Tooltip title="Download">
                                                <IconButton
                                                    size="small"
                                                    onClick={() => downloadAckNackFile(record)}
                                                    color="secondary"
                                                >
                                                    <DownloadIcon />
                                                </IconButton>
                                            </Tooltip>
                                        )}
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                        
                        {ackNackRecords.length === 0 && !loading && (
                            <TableRow>
                                <TableCell colSpan={8} align="center">
                                    <Typography variant="body2" color="text.secondary">
                                        No ACK/NACK records found
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Upload ACK/NACK Dialog */}
            <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Upload ACK/NACK File</DialogTitle>
                <DialogContent>
                    <Box mt={2}>
                        <input
                            type="file"
                            accept=".ack,.nack"
                            onChange={(e) => setUploadFile(e.target.files[0])}
                            style={{ width: '100%' }}
                        />
                        {uploadFile && (
                            <Typography variant="body2" sx={{ mt: 1 }}>
                                Selected: {uploadFile.name}
                            </Typography>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
                    <Button 
                        onClick={handleUploadFile} 
                        disabled={!uploadFile}
                        variant="contained"
                    >
                        Upload
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Generate NACK Dialog */}
            <Dialog open={nackDialogOpen} onClose={() => setNackDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Generate NACK</DialogTitle>
                <DialogContent>
                    <TextField
                        fullWidth
                        label="Reason Code"
                        value={nackReason.code}
                        onChange={(e) => setNackReason({ ...nackReason, code: e.target.value })}
                        margin="normal"
                        required
                    />
                    <TextField
                        fullWidth
                        label="Reason Description"
                        value={nackReason.description}
                        onChange={(e) => setNackReason({ ...nackReason, description: e.target.value })}
                        margin="normal"
                        multiline
                        rows={3}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setNackDialogOpen(false)}>Cancel</Button>
                    <Button 
                        onClick={handleGenerateNack} 
                        disabled={!nackReason.code}
                        variant="contained"
                        color="error"
                    >
                        Generate NACK
                    </Button>
                </DialogActions>
            </Dialog>

            {/* View Details Dialog */}
            <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>
                    ACK/NACK Details - {selectedRecord?.ackNackFileName}
                </DialogTitle>
                <DialogContent>
                    {selectedRecord && (
                        <Box>
                            <Grid container spacing={2}>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth
                                        label="Type"
                                        value={selectedRecord.type}
                                        InputProps={{ readOnly: true }}
                                        margin="normal"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth
                                        label="Status"
                                        value={selectedRecord.status}
                                        InputProps={{ readOnly: true }}
                                        margin="normal"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth
                                        label="Original File"
                                        value={selectedRecord.originalFileName}
                                        InputProps={{ readOnly: true }}
                                        margin="normal"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth
                                        label="Service"
                                        value={`${selectedRecord.serviceName}${selectedRecord.subServiceName ? '/' + selectedRecord.subServiceName : ''}`}
                                        InputProps={{ readOnly: true }}
                                        margin="normal"
                                    />
                                </Grid>
                                {selectedRecord.reasonCode && (
                                    <Grid item xs={12} sm={6}>
                                        <TextField
                                            fullWidth
                                            label="Reason Code"
                                            value={selectedRecord.reasonCode}
                                            InputProps={{ readOnly: true }}
                                            margin="normal"
                                        />
                                    </Grid>
                                )}
                                {selectedRecord.reasonDescription && (
                                    <Grid item xs={12} sm={6}>
                                        <TextField
                                            fullWidth
                                            label="Reason Description"
                                            value={selectedRecord.reasonDescription}
                                            InputProps={{ readOnly: true }}
                                            margin="normal"
                                        />
                                    </Grid>
                                )}
                                <Grid item xs={12}>
                                    <TextField
                                        fullWidth
                                        label="File Content"
                                        value={selectedRecord.content || 'No content available'}
                                        InputProps={{ readOnly: true }}
                                        margin="normal"
                                        multiline
                                        rows={4}
                                    />
                                </Grid>
                            </Grid>
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
                    {selectedRecord?.content && (
                        <Button 
                            onClick={() => downloadAckNackFile(selectedRecord)}
                            startIcon={<DownloadIcon />}
                        >
                            Download
                        </Button>
                    )}
                </DialogActions>
            </Dialog>

            {/* Floating Action Button for quick actions */}
            {statistics.pendingCount > 0 && (
                <Fab
                    color="primary"
                    aria-label="send pending"
                    sx={{ position: 'fixed', bottom: 16, right: 16 }}
                    onClick={handleSendPending}
                >
                    <Badge badgeContent={statistics.pendingCount} color="error">
                        <SendIcon />
                    </Badge>
                </Fab>
            )}
        </Box>
    );

    function downloadAckNackFile(record) {
        if (!record.content) return;
        
        const blob = new Blob([record.content], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = record.ackNackFileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    }
};

export default AckNackManagement;