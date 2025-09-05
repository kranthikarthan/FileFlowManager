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
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    TextField,
    Alert,
    IconButton,
    Tooltip,
    Grid,
    Card,
    CardContent,
    LinearProgress,
    Switch,
    FormControlLabel,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    List,
    ListItem,
    ListItemText,
    ListItemIcon
} from '@mui/material';
import {
    Security as SecurityIcon,
    VpnKey as KeyIcon,
    Verified as VerifiedIcon,
    Error as ErrorIcon,
    Refresh as RefreshIcon,
    Settings as SettingsIcon,
    TestTube as TestIcon,
    CloudQueue as CloudIcon,
    Hardware as HardwareIcon,
    Speed as SpeedIcon,
    Shield as ShieldIcon,
    ExpandMore as ExpandMoreIcon,
    CheckCircle as CheckCircleIcon,
    Cancel as CancelIcon,
    Info as InfoIcon
} from '@mui/icons-material';
import { hsmService } from '../services/hsmService';
import './HsmManagement.css';

const HsmManagement = ({ selectedTenant }) => {
    const [hsmProviders, setHsmProviders] = useState([]);
    const [hsmOperations, setHsmOperations] = useState([]);
    const [hsmValidations, setHsmValidations] = useState([]);
    const [statistics, setStatistics] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    // Dialog states
    const [testDialogOpen, setTestDialogOpen] = useState(false);
    const [configDialogOpen, setConfigDialogOpen] = useState(false);
    const [providerInfoDialogOpen, setProviderInfoDialogOpen] = useState(false);
    const [selectedProvider, setSelectedProvider] = useState(null);
    
    // Test configuration state
    const [testConfig, setTestConfig] = useState({
        tenantId: selectedTenant,
        serviceName: '',
        subServiceName: ''
    });
    const [testResult, setTestResult] = useState(null);

    useEffect(() => {
        if (selectedTenant) {
            loadHsmData();
        }
    }, [selectedTenant]);

    const loadHsmData = async () => {
        try {
            setLoading(true);
            setError(null);
            
            const [providers, operations, validations, stats] = await Promise.all([
                hsmService.getHsmProviders(),
                hsmService.getHsmOperations(),
                hsmService.getHsmValidations(selectedTenant),
                hsmService.getHsmStatistics(selectedTenant)
            ]);
            
            setHsmProviders(providers);
            setHsmOperations(operations);
            setHsmValidations(validations);
            setStatistics(stats);
        } catch (error) {
            setError('Failed to load HSM data: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleTestConnection = async () => {
        if (!testConfig.serviceName) {
            setError('Service name is required for HSM connection test');
            return;
        }
        
        try {
            setLoading(true);
            const result = await hsmService.testHsmConnection(
                testConfig.tenantId, 
                testConfig.serviceName, 
                testConfig.subServiceName
            );
            setTestResult(result);
        } catch (error) {
            setError('HSM connection test failed: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleRetryValidation = async (validationId) => {
        try {
            setLoading(true);
            await hsmService.retryHsmValidation(validationId);
            await loadHsmData(); // Refresh data
        } catch (error) {
            setError('Failed to retry HSM validation: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const renderProviderCard = (provider) => (
        <Grid item xs={12} sm={6} md={4} key={provider.provider}>
            <Card 
                sx={{ 
                    height: '100%',
                    cursor: 'pointer',
                    '&:hover': { transform: 'translateY(-2px)', boxShadow: 3 }
                }}
                onClick={() => {
                    setSelectedProvider(provider);
                    setProviderInfoDialogOpen(true);
                }}
            >
                <CardContent>
                    <Box display="flex" alignItems="center" mb={1}>
                        <Chip
                            label={provider.displayName}
                            color={hsmService.getProviderColor(provider.provider)}
                            size="small"
                            sx={{ mr: 1 }}
                        />
                        {hsmService.isCloudProvider(provider.provider) && (
                            <Tooltip title="Cloud-based HSM">
                                <CloudIcon fontSize="small" color="primary" />
                            </Tooltip>
                        )}
                        {!hsmService.isCloudProvider(provider.provider) && provider.provider !== 'NONE' && (
                            <Tooltip title="Hardware HSM">
                                <HardwareIcon fontSize="small" color="secondary" />
                            </Tooltip>
                        )}
                    </Box>
                    
                    <Typography variant="body2" color="text.secondary" mb={1}>
                        {provider.description}
                    </Typography>
                    
                    <Box display="flex" alignItems="center" mb={1}>
                        <SecurityIcon fontSize="small" sx={{ mr: 1 }} />
                        <Typography variant="caption">
                            Security Level: {hsmService.getSecurityLevel(provider.provider)}/4
                        </Typography>
                    </Box>
                    
                    <Box display="flex" flexWrap="wrap" gap={0.5}>
                        {provider.supportedKeyAlgorithms.map((alg) => (
                            <Chip key={alg} label={alg} size="small" variant="outlined" />
                        ))}
                    </Box>
                </CardContent>
            </Card>
        </Grid>
    );

    const renderValidationRow = (validation) => (
        <TableRow key={validation.id} hover>
            <TableCell>
                <Chip
                    label={hsmService.formatStatus(validation.status)}
                    color={hsmService.getStatusColor(validation.status)}
                    size="small"
                />
            </TableCell>
            <TableCell>
                <Typography variant="body2" noWrap>
                    {validation.fileName}
                </Typography>
            </TableCell>
            <TableCell>
                <Chip
                    label={validation.hsmProvider}
                    color={hsmService.getProviderColor(validation.hsmProvider)}
                    size="small"
                />
            </TableCell>
            <TableCell>
                <Chip
                    label={validation.operation}
                    variant="outlined"
                    size="small"
                />
            </TableCell>
            <TableCell>
                <Typography variant="body2">
                    {validation.serviceName}
                    {validation.subServiceName && (
                        <Typography variant="caption" display="block">
                            {validation.subServiceName}
                        </Typography>
                    )}
                </Typography>
            </TableCell>
            <TableCell>
                <Typography variant="body2">
                    {hsmService.formatProcessingTime(validation.processingTimeMs)}
                </Typography>
            </TableCell>
            <TableCell>
                <Typography variant="body2">
                    {new Date(validation.createdAt).toLocaleDateString()}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                    {new Date(validation.createdAt).toLocaleTimeString()}
                </Typography>
            </TableCell>
            <TableCell>
                <Box display="flex" gap={1}>
                    {validation.status === 'FAILED' && (
                        <Tooltip title="Retry Validation">
                            <IconButton
                                size="small"
                                onClick={() => handleRetryValidation(validation.id)}
                                color="primary"
                            >
                                <RefreshIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                    
                    <Tooltip title="View Details">
                        <IconButton
                            size="small"
                            onClick={() => {
                                // Could open detailed view dialog
                                alert(`HSM Validation Details:\nFile: ${validation.fileName}\nProvider: ${validation.hsmProvider}\nOperation: ${validation.operation}\nStatus: ${validation.status}`);
                            }}
                        >
                            <InfoIcon />
                        </IconButton>
                    </Tooltip>
                </Box>
            </TableCell>
        </TableRow>
    );

    if (!selectedTenant) {
        return (
            <Box p={3}>
                <Alert severity="info">Please select a tenant to view HSM management.</Alert>
            </Box>
        );
    }

    return (
        <Box p={3}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Typography variant="h4" component="h1">
                    HSM Management
                </Typography>
                <Box>
                    <Button
                        variant="outlined"
                        startIcon={<TestIcon />}
                        onClick={() => setTestDialogOpen(true)}
                        sx={{ mr: 1 }}
                    >
                        Test HSM
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<SettingsIcon />}
                        onClick={() => setConfigDialogOpen(true)}
                        sx={{ mr: 1 }}
                    >
                        Configure
                    </Button>
                    <IconButton onClick={loadHsmData}>
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
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div">
                                {statistics.totalValidations || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Total Validations
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="success.main">
                                {statistics.passedValidations || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Passed
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="error.main">
                                {statistics.failedValidations || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Failed
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6" component="div" color="info.main">
                                {statistics.successRate ? (statistics.successRate * 100).toFixed(1) + '%' : '0%'}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Success Rate
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {loading && <LinearProgress sx={{ mb: 2 }} />}

            {/* HSM Providers Overview */}
            <Typography variant="h5" gutterBottom>
                Available HSM Providers
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
                {hsmProviders.map(renderProviderCard)}
            </Grid>

            {/* HSM Validations Table */}
            <Typography variant="h5" gutterBottom>
                HSM Validation Records
            </Typography>
            
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Status</TableCell>
                            <TableCell>File Name</TableCell>
                            <TableCell>HSM Provider</TableCell>
                            <TableCell>Operation</TableCell>
                            <TableCell>Service</TableCell>
                            <TableCell>Processing Time</TableCell>
                            <TableCell>Created</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {hsmValidations.map(renderValidationRow)}
                        
                        {hsmValidations.length === 0 && !loading && (
                            <TableRow>
                                <TableCell colSpan={8} align="center">
                                    <Typography variant="body2" color="text.secondary">
                                        No HSM validation records found
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Test HSM Connection Dialog */}
            <Dialog open={testDialogOpen} onClose={() => setTestDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Test HSM Connection</DialogTitle>
                <DialogContent>
                    <Box mt={2}>
                        <TextField
                            fullWidth
                            label="Service Name"
                            value={testConfig.serviceName}
                            onChange={(e) => setTestConfig({ ...testConfig, serviceName: e.target.value })}
                            margin="normal"
                            required
                        />
                        <TextField
                            fullWidth
                            label="Sub-Service Name"
                            value={testConfig.subServiceName}
                            onChange={(e) => setTestConfig({ ...testConfig, subServiceName: e.target.value })}
                            margin="normal"
                        />
                        
                        {testResult && (
                            <Alert 
                                severity={testResult.success ? 'success' : 'error'} 
                                sx={{ mt: 2 }}
                                icon={testResult.success ? <CheckCircleIcon /> : <CancelIcon />}
                            >
                                <Typography variant="body2">
                                    {testResult.message}
                                </Typography>
                                {testResult.errorCode && (
                                    <Typography variant="caption" display="block">
                                        Error Code: {testResult.errorCode}
                                    </Typography>
                                )}
                            </Alert>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setTestDialogOpen(false)}>Close</Button>
                    <Button 
                        onClick={handleTestConnection} 
                        disabled={!testConfig.serviceName || loading}
                        variant="contained"
                        startIcon={<TestIcon />}
                    >
                        Test Connection
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Provider Information Dialog */}
            <Dialog open={providerInfoDialogOpen} onClose={() => setProviderInfoDialogOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>
                    HSM Provider Information - {selectedProvider?.displayName}
                </DialogTitle>
                <DialogContent>
                    {selectedProvider && (
                        <Box>
                            <Typography variant="body1" gutterBottom>
                                {selectedProvider.description}
                            </Typography>
                            
                            <Grid container spacing={2} sx={{ mb: 3 }}>
                                <Grid item xs={12} sm={6}>
                                    <Card variant="outlined">
                                        <CardContent>
                                            <Typography variant="h6" gutterBottom>
                                                Provider Details
                                            </Typography>
                                            <Typography variant="body2" gutterBottom>
                                                <strong>Provider Class:</strong> {selectedProvider.providerClassName}
                                            </Typography>
                                            <Typography variant="body2" gutterBottom>
                                                <strong>Network-based:</strong> {selectedProvider.isNetworkBased ? 'Yes' : 'No'}
                                            </Typography>
                                            <Typography variant="body2" gutterBottom>
                                                <strong>Cloud-based:</strong> {selectedProvider.isCloudBased ? 'Yes' : 'No'}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>High Availability:</strong> {selectedProvider.supportsHighAvailability ? 'Yes' : 'No'}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <Card variant="outlined">
                                        <CardContent>
                                            <Typography variant="h6" gutterBottom>
                                                Supported Algorithms
                                            </Typography>
                                            <Box display="flex" flexWrap="wrap" gap={1}>
                                                {selectedProvider.supportedKeyAlgorithms.map((alg) => (
                                                    <Chip key={alg} label={alg} size="small" variant="outlined" />
                                                ))}
                                            </Box>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            </Grid>
                            
                            <Accordion>
                                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                    <Typography variant="h6">Setup Instructions</Typography>
                                </AccordionSummary>
                                <AccordionDetails>
                                    <List>
                                        {hsmService.getProviderSetupInstructions(selectedProvider.provider).map((instruction, index) => (
                                            <ListItem key={index}>
                                                <ListItemIcon>
                                                    <Typography variant="body2" color="primary">
                                                        {index + 1}.
                                                    </Typography>
                                                </ListItemIcon>
                                                <ListItemText primary={instruction} />
                                            </ListItem>
                                        ))}
                                    </List>
                                </AccordionDetails>
                            </Accordion>
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setProviderInfoDialogOpen(false)}>Close</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default HsmManagement;