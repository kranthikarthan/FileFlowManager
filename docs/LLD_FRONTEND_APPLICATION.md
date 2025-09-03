# File Transfer Frontend Application - Low Level Design (LLD)

## 1. Document Overview

### 1.1 Purpose
This document provides detailed low-level design specifications for the File Transfer Frontend Application, including component architecture, state management, UI/UX patterns, and implementation details.

### 1.2 Scope
- React component architecture and design patterns
- State management and data flow
- UI/UX implementation details
- Performance optimization strategies
- Mobile and PWA implementation

## 2. Application Structure

### 2.1 Directory Structure
```
src/
├── components/                         # React Components
│   ├── Dashboard.js                   # Main dashboard
│   ├── FileTransferList.js           # File transfer management
│   ├── AckNackManagement.js          # ACK/NACK management
│   ├── CompressionManagement.js     # File compression management
│   ├── Navigation.js                 # Main navigation
│   ├── Login.js                      # Authentication
│   ├── ServiceManagement.js          # Service CRUD operations
│   ├── ServiceConfiguration.js       # Service configuration
│   ├── SsoConfiguration.js           # SSO setup
│   ├── TenantManagement.js          # Tenant management
│   ├── HolidayManagement.js         # Holiday calendar
│   ├── AlertManagement.js           # Alert configuration
│   ├── SubServiceManagement.js      # Sub-service hierarchy
│   ├── CutOffExtensionManagement.js # Cut-off extensions
│   ├── SharedSchemaManagement.js    # Schema management
│   ├── EotValidationDashboard.js    # EOT validation
│   ├── ApiVersionManager.js         # API version management
│   ├── ServiceConfiguration/        # Service config subcomponents
│   │   └── ServiceConfigurationForm.js
│   ├── SchemaManagement/           # Schema management subcomponents
│   ├── HolidayManagement/          # Holiday management subcomponents
│   ├── ProcessingControl/          # Processing control subcomponents
│   ├── analytics/                  # Analytics components
│   ├── mobile/                     # Mobile-specific components
│   │   ├── MobileNavigation.js
│   │   ├── PWAPrompt.js
│   │   └── ThemeToggle.js
│   └── wizards/                    # Setup wizards
│       ├── TenantSetupWizard.js
│       └── ServiceSetupWizard.js
├── services/                       # API Services
│   ├── api.js                     # Base API configuration
│   ├── serviceAPI.js              # Service-specific APIs
│   ├── ackNackService.js          # ACK/NACK operations
│   ├── compressionService.js      # File compression operations
│   ├── analyticsService.js        # Analytics and reporting
│   ├── apiVersionService.js       # API versioning
│   ├── frontendBackupService.js   # Backup operations
│   ├── monitoringService.js       # System monitoring
│   ├── performanceService.js      # Performance metrics
│   ├── processingControlService.js # Processing control
│   └── securityService.js         # Security operations
├── hooks/                          # Custom React Hooks
│   ├── useAuth.js                 # Authentication hook
│   ├── useApi.js                  # API integration hook
│   ├── useTenant.js               # Tenant management hook
│   ├── useRealTimeUpdates.js      # WebSocket/SSE hook
│   ├── usePWA.js                  # PWA functionality hook
│   └── usePerformance.js          # Performance monitoring hook
├── utils/                          # Utility Functions
│   ├── dateUtils.js               # Date/time utilities
│   ├── fileUtils.js               # File handling utilities
│   ├── validationUtils.js         # Form validation
│   ├── formatUtils.js             # Data formatting
│   ├── enhancedAppWrapper.js      # App enhancement utilities
│   └── constants.js               # Application constants
├── theme/                          # Theming and Styling
│   ├── themeProvider.js           # Theme context provider
│   ├── lightTheme.js              # Light theme configuration
│   ├── darkTheme.js               # Dark theme configuration
│   └── customComponents.js        # Custom styled components
├── styles/                         # CSS Styles
│   ├── mobile.css                 # Mobile-specific styles
│   ├── components/                # Component-specific styles
│   └── global.css                 # Global styles
├── config/                         # Configuration
│   ├── appConfig.js               # Application configuration
│   ├── apiVersionConfig.js        # API version configuration
│   └── backupConfig.js            # Backup configuration
├── __tests__/                      # Test Files
│   ├── components/                # Component tests
│   ├── services/                  # Service tests
│   ├── hooks/                     # Hook tests
│   └── integration/               # Integration tests
├── App.js                          # Main application component
├── index.js                       # Application entry point
├── index.css                      # Global CSS
└── setupTests.js                  # Test configuration
```

## 3. Component Architecture

### 3.1 Main Application Component

#### App.js - Root Component
```javascript
function AppContent() {
  const [user, setUser] = useState(null);
  const [anchorEl, setAnchorEl] = useState(null);
  const [tenantWizardOpen, setTenantWizardOpen] = useState(false);
  const [serviceWizardOpen, setServiceWizardOpen] = useState(false);
  
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));

  // Authentication effect
  useEffect(() => {
    const token = localStorage.getItem('authToken');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      try {
        setUser(JSON.parse(userData));
      } catch (err) {
        console.error('Failed to parse user data:', err);
        handleLogout();
      }
    }
  }, []);

  // Login handler
  const handleLogin = async (credentials) => {
    try {
      const response = await authService.login(credentials);
      setUser(response.user);
      localStorage.setItem('authToken', response.token);
      localStorage.setItem('user', JSON.stringify(response.user));
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  // Logout handler
  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    setAnchorEl(null);
  };

  // Main render logic with routing
  return (
    <Router>
      {!user ? (
        <Login onLogin={handleLogin} />
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
          {/* App Bar */}
          <AppBar position="static" color="primary">
            <Toolbar>
              <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                File Transfer Management
              </Typography>
              {/* User menu and actions */}
            </Toolbar>
          </AppBar>

          {/* Navigation */}
          {!isMobile && <Navigation />}

          {/* Main Content */}
          <Container maxWidth="xl" sx={{ flexGrow: 1, py: 3 }}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/transfers" element={<FileTransferList />} />
              <Route path="/ack-nack" element={<AckNackManagement selectedTenant={user?.tenantId || 'default'} />} />
              <Route path="/service-config" element={<ServiceConfiguration />} />
              <Route path="/sso-config" element={<SsoConfiguration />} />
              <Route path="/services" element={<ServiceManagement />} />
              <Route path="/tenants" element={<TenantManagement />} />
              <Route path="/holidays" element={<HolidayManagement />} />
              <Route path="/alerts" element={<AlertManagement />} />
              <Route path="/sub-services" element={<SubServiceManagement tenantId={user?.tenantId || 'default'} />} />
              <Route path="/cutoff-extensions" element={<CutOffExtensionManagement tenantId={user?.tenantId || 'default'} />} />
              <Route path="/shared-schemas" element={<SharedSchemaManagement tenantId={user?.tenantId || 'default'} />} />
              <Route path="/eot-validation" element={<EotValidationDashboard tenantId={user?.tenantId || 'default'} />} />
            </Routes>
          </Container>

          {/* Mobile Navigation */}
          {isMobile && <MobileNavigation />}

          {/* Setup Wizards */}
          <TenantSetupWizard open={tenantWizardOpen} onClose={() => setTenantWizardOpen(false)} />
          <ServiceSetupWizard open={serviceWizardOpen} onClose={() => setServiceWizardOpen(false)} />

          {/* PWA Prompt */}
          <PWAPrompt />
        </Box>
      )}
    </Router>
  );
}
```

### 3.2 Feature Component Design

#### FileTransferList Component
```javascript
export const FileTransferList = () => {
  // State management
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [services, setServices] = useState([]);
  const [filters, setFilters] = useState({
    service: '',
    status: '',
    direction: ''
  });

  // Data fetching with ACK/NACK enhancement
  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch transfer data based on filters
      let response;
      if (filters.service && filters.status) {
        response = await fileTransferAPI.getFileTransfersByServiceAndStatus(filters.service, filters.status);
      } else if (filters.service) {
        response = await fileTransferAPI.getFileTransfersByService(filters.service);
      } else if (filters.status) {
        response = await fileTransferAPI.getFileTransfersByStatus(filters.status);
      } else if (filters.direction) {
        response = await fileTransferAPI.getFileTransfersByDirection(filters.direction);
      } else {
        response = await fileTransferAPI.getAllFileTransfers();
      }
      
      // Enhance transfer data with ACK/NACK information
      const transfersWithAckNack = await Promise.all(
        response.data.map(async (transfer) => {
          try {
            const ackNackInfo = await ackNackService.getAckNackForFileTransfer(transfer.id);
            return { ...transfer, ackNackInfo };
          } catch (error) {
            return { ...transfer, ackNackInfo: null };
          }
        })
      );
      
      setTransfers(transfersWithAckNack);
    } catch (err) {
      setError('Failed to fetch file transfers');
      console.error('Error fetching transfers:', err);
    } finally {
      setLoading(false);
    }
  };

  // DataGrid columns configuration
  const columns = [
    {
      field: 'id',
      headerName: 'ID',
      width: 90,
    },
    {
      field: 'fileName',
      headerName: 'File Name',
      width: 200,
      flex: 1,
    },
    {
      field: 'serviceType',
      headerName: 'Service',
      width: 120,
    },
    {
      field: 'direction',
      headerName: 'Direction',
      width: 100,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          size="small" 
          color={params.value === 'INBOUND' ? 'primary' : 'secondary'}
        />
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 150,
      renderCell: (params) => (
        <Chip 
          label={params.value.replace(/_/g, ' ')} 
          size="small" 
          color={statusColors[params.value]}
        />
      ),
    },
    {
      field: 'ackNackStatus',
      headerName: 'ACK/NACK',
      width: 120,
      renderCell: (params) => {
        const ackNackInfo = params.row.ackNackInfo;
        if (!ackNackInfo) {
          return params.row.direction === 'INBOUND' && params.row.status === 'COMPLETED' ? (
            <Tooltip title="Generate ACK">
              <IconButton
                size="small"
                onClick={() => handleGenerateAck(params.row.id)}
                color="success"
              >
                <CheckCircleIcon />
              </IconButton>
            </Tooltip>
          ) : '-';
        }
        
        return (
          <Tooltip title={`${ackNackInfo.type}: ${ackNackInfo.status}`}>
            <Chip
              label={ackNackInfo.type}
              size="small"
              color={ackNackInfo.type === 'ACK' ? 'success' : 'error'}
              icon={ackNackInfo.type === 'ACK' ? <CheckCircleIcon /> : <ErrorIcon />}
            />
          </Tooltip>
        );
      },
    },
    // Additional columns...
  ];

  // Event handlers
  const handleGenerateAck = async (fileTransferId) => {
    try {
      await ackNackService.generateAck(fileTransferId);
      fetchData(); // Refresh data
    } catch (err) {
      setError('Failed to generate ACK');
    }
  };

  // Main render
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        File Transfers
      </Typography>
      
      {/* Filters */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} sm={4}>
          <FormControl fullWidth size="small">
            <InputLabel>Service</InputLabel>
            <Select
              value={filters.service}
              label="Service"
              onChange={handleFilterChange('service')}
            >
              <MenuItem value="">All Services</MenuItem>
              {services.map((service) => (
                <MenuItem key={service} value={service}>
                  {service}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
        {/* Additional filters */}
      </Grid>

      {/* Data Grid */}
      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={transfers}
          columns={columns}
          loading={loading}
          pageSizeOptions={[25, 50, 100]}
          disableRowSelectionOnClick
          getRowClassName={(params) => 
            params.row.status === 'FAILED' ? 'error-row' : ''
          }
        />
      </Paper>

      {/* Error Display */}
      {error && (
        <Alert severity="error" sx={{ mt: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
    </Box>
  );
};
```

#### AckNackManagement Component
```javascript
const AckNackManagement = ({ selectedTenant }) => {
  // State management
  const [ackNackRecords, setAckNackRecords] = useState([]);
  const [statistics, setStatistics] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterType, setFilterType] = useState('ALL');
  
  // Dialog states
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [nackDialogOpen, setNackDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState(null);
  
  // Form states
  const [uploadFile, setUploadFile] = useState(null);
  const [nackReason, setNackReason] = useState({ code: '', description: '' });

  // Data loading effect
  useEffect(() => {
    if (selectedTenant) {
      loadAckNackData();
    }
  }, [selectedTenant, filterStatus, filterType]);

  // Data loading function
  const loadAckNackData = async () => {
    if (!selectedTenant) return;
    
    try {
      setLoading(true);
      setError(null);
      
      // Fetch filtered data
      let records;
      if (filterStatus !== 'ALL') {
        records = await ackNackService.getAckNackRecordsByStatus(selectedTenant, filterStatus);
      } else if (filterType !== 'ALL') {
        records = await ackNackService.getAckNackRecordsByType(selectedTenant, filterType);
      } else {
        records = await ackNackService.getAllAckNackRecords(selectedTenant);
      }
      
      // Fetch statistics
      const stats = await ackNackService.getAckNackStatistics(selectedTenant);
      
      setAckNackRecords(records);
      setStatistics(stats);
    } catch (error) {
      setError('Failed to load ACK/NACK data: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  // Event handlers
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

  // Render statistics cards
  const renderStatistics = () => (
    <Grid container spacing={2} sx={{ mb: 3 }}>
      {[
        { label: 'Total Records', value: statistics.totalRecords || 0, color: 'primary' },
        { label: 'ACK Files', value: statistics.ackCount || 0, color: 'success' },
        { label: 'NACK Files', value: statistics.nackCount || 0, color: 'error' },
        { label: 'Pending', value: statistics.pendingCount || 0, color: 'warning' },
        { label: 'Sent', value: statistics.sentCount || 0, color: 'info' },
        { label: 'Failed', value: statistics.failedCount || 0, color: 'error' }
      ].map((stat, index) => (
        <Grid item xs={12} sm={6} md={2} key={index}>
          <Card>
            <CardContent>
              <Typography variant="h6" component="div" color={`${stat.color}.main`}>
                {stat.value}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {stat.label}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );

  // Main render
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

      {renderStatistics()}

      {/* Filters and Table */}
      {/* Dialog components */}
      {/* Floating Action Button */}
    </Box>
  );
};
```

## 4. State Management Architecture

### 4.1 Context-Based State Management

#### Theme Context Provider
```javascript
const ThemeContext = createContext();

export const CustomThemeProvider = ({ children }) => {
  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem('darkMode');
    return saved ? JSON.parse(saved) : false;
  });

  const toggleTheme = useCallback(() => {
    setIsDark(prev => {
      const newValue = !prev;
      localStorage.setItem('darkMode', JSON.stringify(newValue));
      return newValue;
    });
  }, []);

  const theme = useMemo(() => 
    createTheme({
      palette: {
        mode: isDark ? 'dark' : 'light',
        primary: {
          main: '#1976d2',
          light: '#42a5f5',
          dark: '#1565c0',
        },
        secondary: {
          main: '#dc004e',
        },
        background: {
          default: isDark ? '#121212' : '#f5f5f5',
          paper: isDark ? '#1e1e1e' : '#ffffff',
        },
      },
      components: {
        MuiButton: {
          styleOverrides: {
            root: {
              textTransform: 'none',
              borderRadius: 8,
            },
          },
        },
        MuiCard: {
          styleOverrides: {
            root: {
              borderRadius: 12,
              boxShadow: isDark 
                ? '0 4px 6px rgba(0, 0, 0, 0.3)' 
                : '0 2px 4px rgba(0, 0, 0, 0.1)',
            },
          },
        },
      },
    }), [isDark]);

  const contextValue = useMemo(() => ({
    isDark,
    toggleTheme,
    theme
  }), [isDark, toggleTheme, theme]);

  return (
    <ThemeContext.Provider value={contextValue}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
```

#### Authentication Context
```javascript
const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(localStorage.getItem('authToken'));

  useEffect(() => {
    if (token) {
      validateToken();
    } else {
      setLoading(false);
    }
  }, [token]);

  const validateToken = async () => {
    try {
      const response = await authService.validateToken(token);
      setUser(response.user);
    } catch (error) {
      logout();
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials) => {
    try {
      const response = await authService.login(credentials);
      setUser(response.user);
      setToken(response.token);
      localStorage.setItem('authToken', response.token);
      localStorage.setItem('user', JSON.stringify(response.user));
      return response;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
  };

  const contextValue = {
    user,
    token,
    login,
    logout,
    loading,
    isAuthenticated: !!user
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### 4.2 Custom Hooks Design

#### API Integration Hook
```javascript
export const useApi = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const callApi = useCallback(async (apiFunction, ...args) => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiFunction(...args);
      return result;
    } catch (err) {
      setError(err.message || 'An error occurred');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return { loading, error, callApi, clearError };
};
```

#### Real-Time Updates Hook
```javascript
export const useRealTimeUpdates = (endpoint, dependencies = []) => {
  const [data, setData] = useState(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(`/api/v1/stream${endpoint}`);
    
    eventSource.onopen = () => {
      setConnected(true);
      console.log('SSE connection opened');
    };
    
    eventSource.onmessage = (event) => {
      try {
        const newData = JSON.parse(event.data);
        setData(newData);
      } catch (error) {
        console.error('Error parsing SSE data:', error);
      }
    };
    
    eventSource.onerror = (error) => {
      setConnected(false);
      console.error('SSE connection error:', error);
    };
    
    return () => {
      eventSource.close();
      setConnected(false);
    };
  }, dependencies);

  return { data, connected };
};
```

#### Performance Monitoring Hook
```javascript
export const usePerformance = (componentName) => {
  const [metrics, setMetrics] = useState({
    renderTime: 0,
    renderCount: 0,
    lastRender: null
  });

  const measureRender = useCallback(() => {
    const startTime = performance.now();
    
    return () => {
      const endTime = performance.now();
      const renderTime = endTime - startTime;
      
      setMetrics(prev => ({
        renderTime,
        renderCount: prev.renderCount + 1,
        lastRender: new Date()
      }));
      
      // Report performance metrics
      if (renderTime > 16) { // > 16ms indicates potential performance issue
        console.warn(`Slow render detected in ${componentName}: ${renderTime}ms`);
      }
    };
  }, [componentName]);

  useEffect(() => {
    // Report component mount
    performanceService.recordComponentMount(componentName);
    
    return () => {
      // Report component unmount
      performanceService.recordComponentUnmount(componentName);
    };
  }, [componentName]);

  return { metrics, measureRender };
};
```

## 5. Service Layer Design

### 5.1 API Service Implementation

#### Base API Configuration
```javascript
import axios from 'axios';

// Create axios instance with default configuration
export const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for authentication
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    const tenantId = localStorage.getItem('currentTenant');
    if (tenantId) {
      config.headers['X-Tenant-ID'] = tenantId;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token expired, redirect to login
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      // Insufficient permissions
      console.error('Access denied:', error.response.data);
    } else if (error.response?.status >= 500) {
      // Server error
      console.error('Server error:', error.response.data);
    }
    
    return Promise.reject(error);
  }
);
```

#### Specialized Service Implementation
```javascript
export const ackNackService = {
  
  async getAllAckNackRecords(tenantId) {
    try {
      const response = await api.get(`/api/v1/ack-nack/${tenantId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching ACK/NACK records:', error);
      throw error;
    }
  },

  async generateAck(fileTransferId) {
    try {
      const response = await api.post(`/api/v1/ack-nack/generate-ack/${fileTransferId}`);
      return response.data;
    } catch (error) {
      console.error('Error generating ACK:', error);
      throw error;
    }
  },

  async uploadAckNackFile(tenantId, file) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await api.post(`/api/v1/ack-nack/${tenantId}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          console.log(`Upload progress: ${percentCompleted}%`);
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error uploading ACK/NACK file:', error);
      throw error;
    }
  },

  // Utility methods
  formatStatus(status) {
    const statusMap = {
      'PENDING': 'Pending',
      'GENERATED': 'Generated',
      'SENT': 'Sent',
      'RECEIVED': 'Received',
      'PROCESSED': 'Processed',
      'FAILED': 'Failed',
      'EXPIRED': 'Expired'
    };
    return statusMap[status] || status;
  },

  getStatusColor(status) {
    const colorMap = {
      'PENDING': 'warning',
      'GENERATED': 'info',
      'SENT': 'primary',
      'RECEIVED': 'info',
      'PROCESSED': 'success',
      'FAILED': 'error',
      'EXPIRED': 'error'
    };
    return colorMap[status] || 'default';
  }
};
```

## 6. UI/UX Implementation

### 6.1 Responsive Design Implementation

#### Mobile-First CSS Architecture
```css
/* mobile.css - Mobile-first responsive design */

/* Base mobile styles */
.mobile-container {
  padding: 16px;
  max-width: 100vw;
  overflow-x: hidden;
}

.mobile-navigation {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--surface-color);
  border-top: 1px solid var(--border-color);
  z-index: 1000;
  display: flex;
  justify-content: space-around;
  padding: 8px 0;
}

.mobile-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  min-width: 60px;
  text-decoration: none;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.mobile-nav-item.active {
  color: var(--primary-main);
}

.mobile-nav-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.mobile-nav-label {
  font-size: 12px;
  font-weight: 500;
}

/* Tablet styles */
@media (min-width: 768px) {
  .mobile-container {
    padding: 24px;
  }
  
  .mobile-navigation {
    display: none;
  }
}

/* Desktop styles */
@media (min-width: 1024px) {
  .mobile-container {
    padding: 32px;
    max-width: none;
  }
}

/* Component-specific responsive styles */
.ack-nack-table {
  width: 100%;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .ack-nack-table {
    font-size: 0.875rem;
  }
  
  .ack-nack-table th,
  .ack-nack-table td {
    padding: 8px 4px;
  }
  
  .ack-nack-actions {
    flex-direction: column;
    gap: 4px;
  }
}

/* Dark theme adjustments */
.dark-theme {
  --surface-color: rgba(255, 255, 255, 0.05);
  --border-color: rgba(255, 255, 255, 0.12);
  --text-primary: rgba(255, 255, 255, 0.87);
  --text-secondary: rgba(255, 255, 255, 0.6);
}

.light-theme {
  --surface-color: #ffffff;
  --border-color: rgba(0, 0, 0, 0.12);
  --text-primary: rgba(0, 0, 0, 0.87);
  --text-secondary: rgba(0, 0, 0, 0.6);
}
```

### 6.2 Progressive Web App Implementation

#### Service Worker Configuration
```javascript
// public/sw.js - Service Worker for PWA functionality
const CACHE_NAME = 'file-transfer-v1.0.0';
const urlsToCache = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
  '/favicon.ico'
];

// Install event
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
  );
});

// Fetch event with cache-first strategy
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        // Return cached version or fetch from network
        if (response) {
          return response;
        }
        
        return fetch(event.request).then((response) => {
          // Don't cache non-successful responses
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response;
          }
          
          // Clone the response
          const responseToCache = response.clone();
          
          caches.open(CACHE_NAME)
            .then((cache) => {
              cache.put(event.request, responseToCache);
            });
          
          return response;
        });
      })
  );
});

// Background sync for offline actions
self.addEventListener('sync', (event) => {
  if (event.tag === 'background-sync') {
    event.waitUntil(doBackgroundSync());
  }
});

async function doBackgroundSync() {
  // Sync pending actions when back online
  const pendingActions = await getPendingActions();
  
  for (const action of pendingActions) {
    try {
      await syncAction(action);
      await removePendingAction(action.id);
    } catch (error) {
      console.error('Failed to sync action:', error);
    }
  }
}
```

#### PWA Manifest
```json
{
  "name": "File Transfer Management System",
  "short_name": "File Transfer",
  "description": "Enterprise file transfer management with real-time monitoring",
  "start_url": "/",
  "display": "standalone",
  "theme_color": "#1976d2",
  "background_color": "#ffffff",
  "orientation": "portrait-primary",
  "categories": ["business", "productivity"],
  "icons": [
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "maskable any"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "maskable any"
    }
  ],
  "shortcuts": [
    {
      "name": "File Transfers",
      "short_name": "Transfers",
      "description": "View file transfer status",
      "url": "/transfers",
      "icons": [
        {
          "src": "/icons/transfers-96x96.png",
          "sizes": "96x96"
        }
      ]
    },
    {
      "name": "ACK/NACK",
      "short_name": "ACK/NACK",
      "description": "Manage acknowledgment files",
      "url": "/ack-nack",
      "icons": [
        {
          "src": "/icons/ack-nack-96x96.png",
          "sizes": "96x96"
        }
      ]
    }
  ]
}
```

## 7. Performance Optimization

### 7.1 Component Optimization

#### React.memo and useMemo Implementation
```javascript
// Memoized component for performance
const FileTransferRow = React.memo(({ transfer, onAction }) => {
  const ackNackInfo = useMemo(() => {
    return transfer.ackNackInfo ? {
      ...transfer.ackNackInfo,
      displayStatus: ackNackService.formatStatus(transfer.ackNackInfo.status),
      statusColor: ackNackService.getStatusColor(transfer.ackNackInfo.status)
    } : null;
  }, [transfer.ackNackInfo]);

  const actionButtons = useMemo(() => {
    return (
      <Box display="flex" gap={0.5}>
        {transfer.status === 'FAILED' && (
          <Tooltip title="Retry Transfer">
            <IconButton
              size="small"
              onClick={() => onAction('retry', transfer.id)}
            >
              <RetryIcon />
            </IconButton>
          </Tooltip>
        )}
        {transfer.direction === 'INBOUND' && transfer.status === 'COMPLETED' && !ackNackInfo && (
          <Tooltip title="Generate ACK">
            <IconButton
              size="small"
              onClick={() => onAction('generateAck', transfer.id)}
              color="success"
            >
              <CheckCircleIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>
    );
  }, [transfer.status, transfer.direction, transfer.id, ackNackInfo, onAction]);

  return (
    <TableRow hover>
      <TableCell>{transfer.id}</TableCell>
      <TableCell>{transfer.fileName}</TableCell>
      <TableCell>
        <Chip 
          label={transfer.status} 
          color={getStatusColor(transfer.status)}
          size="small"
        />
      </TableCell>
      <TableCell>
        {ackNackInfo ? (
          <Chip
            label={ackNackInfo.type}
            color={ackNackInfo.statusColor}
            size="small"
          />
        ) : '-'}
      </TableCell>
      <TableCell>{actionButtons}</TableCell>
    </TableRow>
  );
}, (prevProps, nextProps) => {
  // Custom comparison function for React.memo
  return prevProps.transfer.id === nextProps.transfer.id &&
         prevProps.transfer.status === nextProps.transfer.status &&
         prevProps.transfer.ackNackInfo?.status === nextProps.transfer.ackNackInfo?.status;
});
```

#### Virtual Scrolling for Large Datasets
```javascript
import { FixedSizeList as List } from 'react-window';

const VirtualizedFileTransferList = ({ transfers, onAction }) => {
  const Row = ({ index, style }) => {
    const transfer = transfers[index];
    
    return (
      <div style={style}>
        <FileTransferRow 
          transfer={transfer} 
          onAction={onAction}
        />
      </div>
    );
  };

  return (
    <List
      height={600}
      itemCount={transfers.length}
      itemSize={60}
      itemData={transfers}
    >
      {Row}
    </List>
  );
};
```

### 7.2 Bundle Optimization

#### Code Splitting Implementation
```javascript
// Lazy loading for route components
const Dashboard = lazy(() => import('./components/Dashboard'));
const FileTransferList = lazy(() => import('./components/FileTransferList'));
const AckNackManagement = lazy(() => import('./components/AckNackManagement'));
const ServiceConfiguration = lazy(() => import('./components/ServiceConfiguration'));

// Route configuration with suspense
const AppRoutes = () => (
  <Suspense fallback={<CircularProgress />}>
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/transfers" element={<FileTransferList />} />
      <Route path="/ack-nack" element={<AckNackManagement />} />
      <Route path="/service-config" element={<ServiceConfiguration />} />
    </Routes>
  </Suspense>
);

// Dynamic imports for heavy components
const loadAnalyticsModule = () => import('./components/analytics/AnalyticsDashboard');
const loadReportingModule = () => import('./components/reporting/ReportBuilder');
```

#### Webpack Configuration Optimization
```javascript
// webpack.config.enterprise.js
const path = require('path');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');

module.exports = {
  entry: './src/index.js',
  
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
        },
        common: {
          name: 'common',
          minChunks: 2,
          chunks: 'all',
          enforce: true,
        },
      },
    },
    usedExports: true,
    sideEffects: false,
  },
  
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@components': path.resolve(__dirname, 'src/components'),
      '@services': path.resolve(__dirname, 'src/services'),
      '@utils': path.resolve(__dirname, 'src/utils'),
    },
  },
  
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env', '@babel/preset-react'],
            plugins: [
              '@babel/plugin-proposal-class-properties',
              ['import', { libraryName: '@mui/material', style: false }, 'mui'],
              ['import', { libraryName: '@mui/icons-material', style: false }, 'mui-icons'],
            ],
          },
        },
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader', 'postcss-loader'],
      },
    ],
  },
  
  plugins: [
    process.env.ANALYZE && new BundleAnalyzerPlugin(),
  ].filter(Boolean),
};
```

## 8. Testing Framework

### 8.1 Component Testing Strategy

#### Unit Testing with React Testing Library
```javascript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import { BrowserRouter } from 'react-router-dom';
import AckNackManagement from '../AckNackManagement';
import { ackNackService } from '../../services/ackNackService';

// Mock the service
jest.mock('../../services/ackNackService');

const renderWithProviders = (component) => {
  return render(
    <BrowserRouter>
      <ThemeProvider theme={createTheme()}>
        {component}
      </ThemeProvider>
    </BrowserRouter>
  );
};

describe('AckNackManagement', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('displays ACK/NACK statistics correctly', async () => {
    // Mock API responses
    ackNackService.getAllAckNackRecords.mockResolvedValue([
      { id: 1, type: 'ACK', status: 'SENT', originalFileName: 'test.dat' },
      { id: 2, type: 'NACK', status: 'FAILED', originalFileName: 'test2.dat' }
    ]);
    
    ackNackService.getAckNackStatistics.mockResolvedValue({
      totalRecords: 2,
      ackCount: 1,
      nackCount: 1,
      sentCount: 1,
      failedCount: 1
    });

    renderWithProviders(<AckNackManagement selectedTenant="test-tenant" />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // Total records
      expect(screen.getByText('1')).toBeInTheDocument(); // ACK count
    });

    // Verify service calls
    expect(ackNackService.getAllAckNackRecords).toHaveBeenCalledWith('test-tenant');
    expect(ackNackService.getAckNackStatistics).toHaveBeenCalledWith('test-tenant');
  });

  test('handles file upload correctly', async () => {
    ackNackService.uploadAckNackFile.mockResolvedValue({ message: 'Success' });
    ackNackService.getAllAckNackRecords.mockResolvedValue([]);
    ackNackService.getAckNackStatistics.mockResolvedValue({});

    renderWithProviders(<AckNackManagement selectedTenant="test-tenant" />);

    // Open upload dialog
    const uploadButton = screen.getByText('Upload ACK/NACK');
    fireEvent.click(uploadButton);

    // Simulate file selection
    const fileInput = screen.getByRole('button', { name: /upload/i });
    const file = new File(['test content'], 'test.ack', { type: 'text/plain' });
    
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Submit upload
    const submitButton = screen.getByText('Upload');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(ackNackService.uploadAckNackFile).toHaveBeenCalledWith('test-tenant', file);
    });
  });
});
```

### 8.2 Integration Testing

#### End-to-End Testing with Cypress
```javascript
// cypress/e2e/ack-nack-management.cy.js
describe('ACK/NACK Management', () => {
  beforeEach(() => {
    // Login before each test
    cy.login('test-user', 'test-password');
    cy.visit('/ack-nack');
  });

  it('should display ACK/NACK statistics', () => {
    // Wait for page to load
    cy.get('[data-testid=ack-nack-stats]').should('be.visible');
    
    // Check statistics cards
    cy.get('[data-testid=total-records]').should('contain', '0');
    cy.get('[data-testid=ack-count]').should('contain', '0');
    cy.get('[data-testid=nack-count]').should('contain', '0');
  });

  it('should upload ACK/NACK file successfully', () => {
    // Click upload button
    cy.get('[data-testid=upload-button]').click();
    
    // Upload file
    cy.get('input[type=file]').selectFile('cypress/fixtures/test.ack');
    
    // Submit upload
    cy.get('[data-testid=submit-upload]').click();
    
    // Verify success message
    cy.get('.MuiAlert-message').should('contain', 'uploaded successfully');
    
    // Verify table update
    cy.get('[data-testid=ack-nack-table]').should('contain', 'test.ack');
  });

  it('should filter ACK/NACK records by status', () => {
    // Select status filter
    cy.get('[data-testid=status-filter]').click();
    cy.get('[data-value=PENDING]').click();
    
    // Verify filtered results
    cy.get('[data-testid=ack-nack-table] tbody tr').each(($row) => {
      cy.wrap($row).should('contain', 'PENDING');
    });
  });

  it('should generate ACK for completed transfer', () => {
    // Navigate to transfers
    cy.visit('/transfers');
    
    // Find completed inbound transfer
    cy.get('[data-testid=transfer-table]')
      .contains('COMPLETED')
      .parent()
      .within(() => {
        cy.get('[data-testid=generate-ack-button]').click();
      });
    
    // Verify ACK generation
    cy.get('.MuiAlert-message').should('contain', 'ACK generated');
    
    // Navigate back to ACK/NACK management
    cy.visit('/ack-nack');
    
    // Verify ACK record exists
    cy.get('[data-testid=ack-nack-table]').should('contain', 'ACK');
  });
});
```

## 9. State Management Patterns

### 9.1 Context API Implementation

#### Global State Context
```javascript
const AppStateContext = createContext();

export const AppStateProvider = ({ children }) => {
  const [state, setState] = useState({
    user: null,
    tenant: null,
    notifications: [],
    theme: 'light',
    loading: false,
    error: null
  });

  // Actions
  const actions = {
    setUser: (user) => setState(prev => ({ ...prev, user })),
    setTenant: (tenant) => setState(prev => ({ ...prev, tenant })),
    addNotification: (notification) => setState(prev => ({
      ...prev,
      notifications: [...prev.notifications, { ...notification, id: Date.now() }]
    })),
    removeNotification: (id) => setState(prev => ({
      ...prev,
      notifications: prev.notifications.filter(n => n.id !== id)
    })),
    setTheme: (theme) => setState(prev => ({ ...prev, theme })),
    setLoading: (loading) => setState(prev => ({ ...prev, loading })),
    setError: (error) => setState(prev => ({ ...prev, error }))
  };

  const contextValue = {
    state,
    actions
  };

  return (
    <AppStateContext.Provider value={contextValue}>
      {children}
    </AppStateContext.Provider>
  );
};

export const useAppState = () => {
  const context = useContext(AppStateContext);
  if (!context) {
    throw new Error('useAppState must be used within AppStateProvider');
  }
  return context;
};
```

### 9.2 Local Component State Patterns

#### Form State Management
```javascript
const useFormState = (initialValues, validationSchema) => {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = useCallback((field) => (event) => {
    const value = event.target.value;
    setValues(prev => ({ ...prev, [field]: value }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: null }));
    }
  }, [errors]);

  const handleBlur = useCallback((field) => () => {
    setTouched(prev => ({ ...prev, [field]: true }));
    
    // Validate field on blur
    if (validationSchema && validationSchema[field]) {
      const fieldError = validationSchema[field](values[field]);
      if (fieldError) {
        setErrors(prev => ({ ...prev, [field]: fieldError }));
      }
    }
  }, [values, validationSchema]);

  const validate = useCallback(() => {
    if (!validationSchema) return true;
    
    const newErrors = {};
    let isValid = true;
    
    Object.keys(validationSchema).forEach(field => {
      const error = validationSchema[field](values[field]);
      if (error) {
        newErrors[field] = error;
        isValid = false;
      }
    });
    
    setErrors(newErrors);
    return isValid;
  }, [values, validationSchema]);

  const handleSubmit = useCallback(async (onSubmit) => {
    setIsSubmitting(true);
    
    try {
      if (validate()) {
        await onSubmit(values);
        setValues(initialValues);
        setTouched({});
        setErrors({});
      }
    } catch (error) {
      console.error('Form submission error:', error);
    } finally {
      setIsSubmitting(false);
    }
  }, [values, validate, onSubmit, initialValues]);

  return {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit,
    setValues,
    setErrors
  };
};
```

## 10. UI Component Design Patterns

### 10.1 Compound Component Pattern

#### Modal Component with Compound Pattern
```javascript
const Modal = ({ children, open, onClose }) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      {children}
    </Dialog>
  );
};

const ModalHeader = ({ children, onClose }) => (
  <DialogTitle>
    <Box display="flex" justifyContent="space-between" alignItems="center">
      {children}
      {onClose && (
        <IconButton onClick={onClose}>
          <CloseIcon />
        </IconButton>
      )}
    </Box>
  </DialogTitle>
);

const ModalContent = ({ children }) => (
  <DialogContent dividers>
    {children}
  </DialogContent>
);

const ModalActions = ({ children }) => (
  <DialogActions>
    {children}
  </DialogActions>
);

// Compound component usage
const AckNackUploadModal = ({ open, onClose, onUpload }) => {
  const [file, setFile] = useState(null);
  
  return (
    <Modal open={open} onClose={onClose}>
      <ModalHeader onClose={onClose}>
        Upload ACK/NACK File
      </ModalHeader>
      <ModalContent>
        <FileUploader
          accept=".ack,.nack"
          onChange={setFile}
          value={file}
        />
      </ModalContent>
      <ModalActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button 
          onClick={() => onUpload(file)} 
          disabled={!file}
          variant="contained"
        >
          Upload
        </Button>
      </ModalActions>
    </Modal>
  );
};
```

### 10.2 Render Props Pattern

#### Data Fetching Component with Render Props
```javascript
const DataFetcher = ({ url, children, dependencies = [] }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await api.get(url);
        
        if (!cancelled) {
          setData(response.data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();
    
    return () => {
      cancelled = true;
    };
  }, [url, ...dependencies]);

  return children({ data, loading, error, refetch: () => fetchData() });
};

// Usage example
const FileTransferStats = ({ tenantId }) => (
  <DataFetcher url={`/api/v1/file-transfers/${tenantId}/statistics`} dependencies={[tenantId]}>
    {({ data, loading, error, refetch }) => {
      if (loading) return <CircularProgress />;
      if (error) return <Alert severity="error">{error}</Alert>;
      
      return (
        <Grid container spacing={2}>
          {Object.entries(data || {}).map(([key, value]) => (
            <Grid item xs={12} sm={6} md={3} key={key}>
              <StatCard title={key} value={value} onRefresh={refetch} />
            </Grid>
          ))}
        </Grid>
      );
    }}
  </DataFetcher>
);
```

## 11. Mobile and PWA Implementation

### 11.1 Mobile-Responsive Components

#### Mobile Navigation Component
```javascript
const MobileNavigation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  const navigationItems = [
    { path: '/', label: 'Dashboard', icon: <DashboardIcon /> },
    { path: '/transfers', label: 'Transfers', icon: <TransferIcon /> },
    { path: '/ack-nack', label: 'ACK/NACK', icon: <CheckCircleIcon /> },
    { path: '/services', label: 'Services', icon: <SettingsIcon /> },
    { path: '/alerts', label: 'Alerts', icon: <NotificationsIcon /> }
  ];

  return (
    <Paper 
      elevation={3}
      sx={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        zIndex: 1000,
        borderRadius: '16px 16px 0 0'
      }}
    >
      <BottomNavigation
        value={location.pathname}
        onChange={(event, newValue) => navigate(newValue)}
        showLabels
      >
        {navigationItems.map((item) => (
          <BottomNavigationAction
            key={item.path}
            label={item.label}
            value={item.path}
            icon={item.icon}
          />
        ))}
      </BottomNavigation>
    </Paper>
  );
};
```

#### Touch-Optimized Components
```javascript
const TouchOptimizedButton = ({ children, onClick, ...props }) => {
  const [pressed, setPressed] = useState(false);
  
  const handleTouchStart = () => setPressed(true);
  const handleTouchEnd = () => setPressed(false);
  
  return (
    <Button
      {...props}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      onClick={onClick}
      sx={{
        minHeight: 48, // Touch target size
        minWidth: 48,
        transform: pressed ? 'scale(0.95)' : 'scale(1)',
        transition: 'transform 0.1s ease',
        ...props.sx
      }}
    >
      {children}
    </Button>
  );
};

const SwipeableCard = ({ children, onSwipeLeft, onSwipeRight }) => {
  const [startX, setStartX] = useState(0);
  const [currentX, setCurrentX] = useState(0);
  const [swiping, setSwiping] = useState(false);

  const handleTouchStart = (e) => {
    setStartX(e.touches[0].clientX);
    setSwiping(true);
  };

  const handleTouchMove = (e) => {
    if (!swiping) return;
    setCurrentX(e.touches[0].clientX);
  };

  const handleTouchEnd = () => {
    if (!swiping) return;
    
    const diff = currentX - startX;
    const threshold = 100;
    
    if (Math.abs(diff) > threshold) {
      if (diff > 0 && onSwipeRight) {
        onSwipeRight();
      } else if (diff < 0 && onSwipeLeft) {
        onSwipeLeft();
      }
    }
    
    setSwiping(false);
    setCurrentX(0);
    setStartX(0);
  };

  return (
    <Card
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      sx={{
        transform: swiping ? `translateX(${(currentX - startX) * 0.3}px)` : 'none',
        transition: swiping ? 'none' : 'transform 0.2s ease',
      }}
    >
      {children}
    </Card>
  );
};
```

### 11.2 PWA Features Implementation

#### Push Notifications
```javascript
const usePushNotifications = () => {
  const [subscription, setSubscription] = useState(null);
  const [supported, setSupported] = useState(false);

  useEffect(() => {
    setSupported('serviceWorker' in navigator && 'PushManager' in window);
  }, []);

  const subscribeToPush = async () => {
    if (!supported) return;

    try {
      const registration = await navigator.serviceWorker.ready;
      
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: process.env.REACT_APP_VAPID_PUBLIC_KEY
      });

      setSubscription(subscription);
      
      // Send subscription to server
      await api.post('/api/v1/push/subscribe', {
        subscription: subscription.toJSON()
      });

      console.log('Push subscription successful');
    } catch (error) {
      console.error('Push subscription failed:', error);
    }
  };

  const unsubscribeFromPush = async () => {
    if (subscription) {
      try {
        await subscription.unsubscribe();
        setSubscription(null);
        
        // Notify server
        await api.post('/api/v1/push/unsubscribe', {
          subscription: subscription.toJSON()
        });
      } catch (error) {
        console.error('Push unsubscribe failed:', error);
      }
    }
  };

  return {
    subscription,
    supported,
    subscribeToPush,
    unsubscribeFromPush
  };
};
```

#### Offline Capability
```javascript
const useOfflineCapability = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [offlineActions, setOfflineActions] = useState([]);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      syncOfflineActions();
    };
    
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const addOfflineAction = useCallback((action) => {
    if (!isOnline) {
      setOfflineActions(prev => [...prev, { ...action, timestamp: Date.now() }]);
      
      // Store in localStorage for persistence
      const stored = JSON.parse(localStorage.getItem('offlineActions') || '[]');
      stored.push(action);
      localStorage.setItem('offlineActions', JSON.stringify(stored));
    }
  }, [isOnline]);

  const syncOfflineActions = async () => {
    const stored = JSON.parse(localStorage.getItem('offlineActions') || '[]');
    
    for (const action of stored) {
      try {
        await executeAction(action);
      } catch (error) {
        console.error('Failed to sync offline action:', error);
      }
    }
    
    // Clear synced actions
    localStorage.removeItem('offlineActions');
    setOfflineActions([]);
  };

  return {
    isOnline,
    offlineActions,
    addOfflineAction
  };
};
```

## 12. Performance Monitoring

### 12.1 Frontend Performance Metrics

#### Performance Monitoring Service
```javascript
class PerformanceMonitoringService {
  constructor() {
    this.metrics = {
      pageLoads: new Map(),
      apiCalls: new Map(),
      componentRenders: new Map(),
      userInteractions: new Map()
    };
    
    this.observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        this.processPerformanceEntry(entry);
      }
    });
    
    this.observer.observe({ entryTypes: ['navigation', 'resource', 'measure'] });
  }

  recordPageLoad(pageName, loadTime) {
    this.metrics.pageLoads.set(pageName, {
      loadTime,
      timestamp: Date.now()
    });
    
    // Send to analytics service
    this.sendMetric('page_load', { pageName, loadTime });
  }

  recordApiCall(endpoint, duration, success) {
    const key = `${endpoint}_${success ? 'success' : 'error'}`;
    
    if (!this.metrics.apiCalls.has(key)) {
      this.metrics.apiCalls.set(key, []);
    }
    
    this.metrics.apiCalls.get(key).push({
      duration,
      timestamp: Date.now()
    });
    
    this.sendMetric('api_call', { endpoint, duration, success });
  }

  recordComponentRender(componentName, renderTime) {
    if (!this.metrics.componentRenders.has(componentName)) {
      this.metrics.componentRenders.set(componentName, []);
    }
    
    this.metrics.componentRenders.get(componentName).push({
      renderTime,
      timestamp: Date.now()
    });
    
    // Alert on slow renders
    if (renderTime > 16) {
      this.sendMetric('slow_render', { componentName, renderTime });
    }
  }

  recordUserInteraction(action, element, duration) {
    this.metrics.userInteractions.set(`${action}_${element}`, {
      duration,
      timestamp: Date.now()
    });
    
    this.sendMetric('user_interaction', { action, element, duration });
  }

  async sendMetric(type, data) {
    try {
      await api.post('/api/v1/frontend-metrics', {
        type,
        data,
        timestamp: Date.now(),
        userAgent: navigator.userAgent,
        url: window.location.href
      });
    } catch (error) {
      console.error('Failed to send performance metric:', error);
    }
  }

  getMetricsSummary() {
    return {
      pageLoads: Object.fromEntries(this.metrics.pageLoads),
      apiCallStats: this.calculateApiStats(),
      componentRenderStats: this.calculateRenderStats(),
      userInteractionStats: Object.fromEntries(this.metrics.userInteractions)
    };
  }

  calculateApiStats() {
    const stats = {};
    
    for (const [key, calls] of this.metrics.apiCalls) {
      const durations = calls.map(call => call.duration);
      stats[key] = {
        count: durations.length,
        avgDuration: durations.reduce((a, b) => a + b, 0) / durations.length,
        maxDuration: Math.max(...durations),
        minDuration: Math.min(...durations)
      };
    }
    
    return stats;
  }
}

export const performanceMonitoringService = new PerformanceMonitoringService();
```

### 12.2 Bundle Size Optimization

#### Dynamic Import Strategy
```javascript
// Lazy loading with preloading
const preloadComponent = (componentImport) => {
  const componentImporter = () => componentImport();
  componentImporter.preload = componentImport;
  return componentImporter;
};

// Preload critical components
const Dashboard = lazy(preloadComponent(() => import('./components/Dashboard')));
const FileTransferList = lazy(preloadComponent(() => import('./components/FileTransferList')));

// Preload on user interaction
const handleNavigationHover = (path) => {
  switch (path) {
    case '/dashboard':
      Dashboard.preload();
      break;
    case '/transfers':
      FileTransferList.preload();
      break;
    // Additional preloading
  }
};

// Resource hints for critical resources
const ResourceHints = () => (
  <Helmet>
    <link rel="preload" href="/api/v1/dashboard/stats" as="fetch" crossOrigin="anonymous" />
    <link rel="prefetch" href="/api/v1/file-transfers" />
    <link rel="preconnect" href="https://api.filetransfer.com" />
  </Helmet>
);
```

## 13. Error Handling and User Experience

### 13.1 Error Boundary Implementation

#### React Error Boundary
```javascript
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error,
      errorInfo
    });
    
    // Log error to monitoring service
    console.error('Error caught by boundary:', error, errorInfo);
    
    // Send error to backend for logging
    this.logErrorToService(error, errorInfo);
  }

  async logErrorToService(error, errorInfo) {
    try {
      await api.post('/api/v1/frontend-errors', {
        error: error.toString(),
        errorInfo: errorInfo.componentStack,
        userAgent: navigator.userAgent,
        url: window.location.href,
        timestamp: new Date().toISOString()
      });
    } catch (logError) {
      console.error('Failed to log error to service:', logError);
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box 
          display="flex" 
          flexDirection="column" 
          alignItems="center" 
          justifyContent="center" 
          minHeight="50vh"
          p={3}
        >
          <Typography variant="h4" gutterBottom color="error">
            Something went wrong
          </Typography>
          <Typography variant="body1" color="text.secondary" align="center" mb={3}>
            We're sorry, but something unexpected happened. Please try refreshing the page.
          </Typography>
          <Button 
            variant="contained" 
            onClick={() => window.location.reload()}
            startIcon={<RefreshIcon />}
          >
            Refresh Page
          </Button>
          
          {process.env.NODE_ENV === 'development' && (
            <Box mt={3} p={2} bgcolor="background.paper" borderRadius={1}>
              <Typography variant="h6" gutterBottom>
                Error Details (Development Only)
              </Typography>
              <pre style={{ fontSize: '12px', overflow: 'auto' }}>
                {this.state.error && this.state.error.toString()}
                {this.state.errorInfo.componentStack}
              </pre>
            </Box>
          )}
        </Box>
      );
    }

    return this.props.children;
  }
}
```

### 13.2 User Feedback and Loading States

#### Loading State Management
```javascript
const LoadingOverlay = ({ loading, children, message = 'Loading...' }) => (
  <Box position="relative">
    {children}
    {loading && (
      <Box
        position="absolute"
        top={0}
        left={0}
        right={0}
        bottom={0}
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        bgcolor="rgba(255, 255, 255, 0.8)"
        zIndex={1000}
      >
        <CircularProgress size={40} />
        <Typography variant="body2" mt={2}>
          {message}
        </Typography>
      </Box>
    )}
  </Box>
);

// Skeleton loading for better UX
const TableSkeleton = ({ rows = 5, columns = 6 }) => (
  <Table>
    <TableHead>
      <TableRow>
        {Array.from({ length: columns }).map((_, index) => (
          <TableCell key={index}>
            <Skeleton variant="text" width="80%" />
          </TableCell>
        ))}
      </TableRow>
    </TableHead>
    <TableBody>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <TableRow key={rowIndex}>
          {Array.from({ length: columns }).map((_, colIndex) => (
            <TableCell key={colIndex}>
              <Skeleton variant="text" />
            </TableCell>
          ))}
        </TableRow>
      ))}
    </TableBody>
  </Table>
);
```

## 14. Accessibility Implementation

### 14.1 WCAG Compliance

#### Accessible Form Components
```javascript
const AccessibleFormField = ({ 
  id, 
  label, 
  required, 
  error, 
  helperText, 
  children, 
  ...props 
}) => {
  const errorId = error ? `${id}-error` : undefined;
  const helperTextId = helperText ? `${id}-helper` : undefined;
  
  return (
    <FormControl fullWidth error={!!error} {...props}>
      <InputLabel 
        htmlFor={id}
        required={required}
        id={`${id}-label`}
      >
        {label}
      </InputLabel>
      {React.cloneElement(children, {
        id,
        'aria-labelledby': `${id}-label`,
        'aria-describedby': [errorId, helperTextId].filter(Boolean).join(' '),
        'aria-invalid': !!error,
        required
      })}
      {error && (
        <FormHelperText id={errorId} role="alert">
          {error}
        </FormHelperText>
      )}
      {helperText && !error && (
        <FormHelperText id={helperTextId}>
          {helperText}
        </FormHelperText>
      )}
    </FormControl>
  );
};

// Usage example
const ServiceConfigForm = () => (
  <form>
    <AccessibleFormField
      id="service-name"
      label="Service Name"
      required
      error={errors.serviceName}
      helperText="Enter a unique name for this service"
    >
      <TextField
        value={values.serviceName}
        onChange={handleChange('serviceName')}
        onBlur={handleBlur('serviceName')}
      />
    </AccessibleFormField>
  </form>
);
```

#### Keyboard Navigation
```javascript
const useKeyboardNavigation = (items, onSelect) => {
  const [selectedIndex, setSelectedIndex] = useState(0);

  useEffect(() => {
    const handleKeyDown = (event) => {
      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          setSelectedIndex(prev => 
            prev < items.length - 1 ? prev + 1 : prev
          );
          break;
        case 'ArrowUp':
          event.preventDefault();
          setSelectedIndex(prev => prev > 0 ? prev - 1 : prev);
          break;
        case 'Enter':
          event.preventDefault();
          if (items[selectedIndex]) {
            onSelect(items[selectedIndex]);
          }
          break;
        case 'Escape':
          event.preventDefault();
          setSelectedIndex(0);
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [items, selectedIndex, onSelect]);

  return selectedIndex;
};
```

## 15. Build and Deployment Configuration

### 15.1 Production Build Optimization

#### Webpack Production Configuration
```javascript
const path = require('path');
const TerserPlugin = require('terser-webpack-plugin');
const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer');

module.exports = {
  mode: 'production',
  
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          compress: {
            drop_console: true,
            drop_debugger: true,
          },
          mangle: true,
        },
      }),
    ],
    
    splitChunks: {
      chunks: 'all',
      maxInitialRequests: 10,
      maxAsyncRequests: 10,
      cacheGroups: {
        default: {
          minChunks: 2,
          priority: -20,
          reuseExistingChunk: true,
        },
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          priority: -10,
          chunks: 'all',
        },
        mui: {
          test: /[\\/]node_modules[\\/]@mui[\\/]/,
          name: 'mui',
          chunks: 'all',
          priority: 10,
        },
      },
    },
  },
  
  plugins: [
    process.env.ANALYZE_BUNDLE && new BundleAnalyzerPlugin({
      analyzerMode: 'static',
      openAnalyzer: false,
      reportFilename: 'bundle-report.html',
    }),
  ].filter(Boolean),
};
```

### 15.2 Docker Configuration for Frontend

#### Multi-Stage Frontend Dockerfile
```dockerfile
# Build stage
FROM node:18-alpine AS builder
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production

# Copy source code
COPY . .

# Build application
RUN npm run build

# Production stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built application
COPY --from=builder /app/build .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Add security headers
COPY security-headers.conf /etc/nginx/conf.d/security-headers.conf

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup && \
    chown -R appuser:appgroup /usr/share/nginx/html /var/cache/nginx

USER appuser

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

This Low Level Design document provides comprehensive implementation details for the Frontend Application component, covering all aspects from component architecture to performance optimization and accessibility considerations.