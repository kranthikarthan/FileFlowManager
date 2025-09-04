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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Alert,
  AlertTitle,
  LinearProgress,
  IconButton,
  Tooltip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  Divider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Switch,
  FormControlLabel
} from '@mui/material';
import {
  AccountTree,
  PlayArrow,
  Pause,
  Stop,
  Refresh,
  Add,
  Edit,
  Delete,
  Visibility,
  Timeline,
  Speed,
  Security,
  Approval,
  Error as ErrorIcon,
  CheckCircle,
  Schedule,
  Settings,
  TrendingUp,
  Assessment,
  Warning
} from '@mui/icons-material';
import './WorkflowManagement.css';

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

export const WorkflowManagement = ({ selectedTenant = 'default' }) => {
  const [activeTab, setActiveTab] = useState(0);
  const [workflows, setWorkflows] = useState([]);
  const [activeExecutions, setActiveExecutions] = useState([]);
  const [businessRules, setBusinessRules] = useState([]);
  const [workflowStatistics, setWorkflowStatistics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Dialog states
  const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
  const [showRuleDialog, setShowRuleDialog] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [selectedExecution, setSelectedExecution] = useState(null);

  useEffect(() => {
    fetchWorkflowData();
  }, [selectedTenant]);

  const fetchWorkflowData = async () => {
    setLoading(true);
    try {
      // Simulate API calls - these would be real API endpoints
      const mockWorkflows = [
        {
          id: 1,
          workflowName: 'Standard File Processing',
          workflowType: 'LINEAR_SEQUENTIAL',
          isActive: true,
          executionCount: 1250,
          successCount: 1180,
          failureCount: 70,
          averageDurationMinutes: 2.5,
          lastExecutedAt: new Date().toISOString()
        },
        {
          id: 2,
          workflowName: 'Customer Data Processing',
          workflowType: 'CONDITIONAL_SEQUENTIAL',
          isActive: true,
          executionCount: 450,
          successCount: 425,
          failureCount: 25,
          averageDurationMinutes: 8.2,
          lastExecutedAt: new Date().toISOString()
        },
        {
          id: 3,
          workflowName: 'Large File Workflow',
          workflowType: 'PARALLEL_EXECUTION',
          isActive: true,
          executionCount: 180,
          successCount: 165,
          failureCount: 15,
          averageDurationMinutes: 15.7,
          lastExecutedAt: new Date().toISOString()
        }
      ];

      const mockActiveExecutions = [
        {
          id: 1,
          executionId: 'WF-1234567890-1234',
          workflowName: 'Customer Data Processing',
          fileName: 'customer_data_20231201.csv',
          executionStatus: 'RUNNING',
          currentStep: 2,
          totalSteps: 4,
          startedAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5 minutes ago
          priority: 90
        },
        {
          id: 2,
          executionId: 'WF-1234567890-5678',
          workflowName: 'Standard File Processing',
          fileName: 'daily_report.txt',
          executionStatus: 'WAITING_FOR_APPROVAL',
          currentStep: 3,
          totalSteps: 5,
          startedAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(), // 15 minutes ago
          priority: 70
        }
      ];

      const mockBusinessRules = [
        {
          id: 1,
          ruleName: 'Auto-compress large files',
          ruleType: 'CONDITIONAL_PROCESSING',
          rulePriority: 80,
          isActive: true,
          executionCount: 340,
          successCount: 335,
          ruleCategory: 'PROCESSING'
        },
        {
          id: 2,
          ruleName: 'High priority customer data',
          ruleType: 'PRIORITY_PROCESSING',
          rulePriority: 90,
          isActive: true,
          executionCount: 125,
          successCount: 125,
          ruleCategory: 'ROUTING'
        }
      ];

      setWorkflows(mockWorkflows);
      setActiveExecutions(mockActiveExecutions);
      setBusinessRules(mockBusinessRules);
      
      // Calculate statistics
      const stats = {
        totalWorkflows: mockWorkflows.length,
        activeWorkflows: mockWorkflows.filter(w => w.isActive).length,
        totalExecutions: mockWorkflows.reduce((sum, w) => sum + w.executionCount, 0),
        averageSuccessRate: mockWorkflows.reduce((sum, w) => sum + (w.successCount / w.executionCount * 100), 0) / mockWorkflows.length,
        activeExecutionsCount: mockActiveExecutions.length,
        waitingApproval: mockActiveExecutions.filter(e => e.executionStatus === 'WAITING_FOR_APPROVAL').length
      };
      
      setWorkflowStatistics(stats);
      setError(null);
    } catch (err) {
      setError('Failed to fetch workflow data: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const renderWorkflowOverview = () => {
    if (!workflowStatistics) return null;

    return (
      <Grid container spacing={3}>
        {/* Statistics Cards */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <AccountTree sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" color="primary">
                {workflowStatistics.totalWorkflows}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Workflows
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <PlayArrow sx={{ fontSize: 40, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" color="success.main">
                {workflowStatistics.activeExecutionsCount}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Active Executions
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <TrendingUp sx={{ fontSize: 40, color: 'info.main', mb: 1 }} />
              <Typography variant="h4" color="info.main">
                {Math.round(workflowStatistics.averageSuccessRate)}%
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Success Rate
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent sx={{ textAlign: 'center' }}>
              <Approval sx={{ fontSize: 40, color: 'warning.main', mb: 1 }} />
              <Typography variant="h4" color="warning.main">
                {workflowStatistics.waitingApproval}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Waiting Approval
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Active Executions */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                <Timeline sx={{ mr: 1, verticalAlign: 'middle' }} />
                Active Workflow Executions
              </Typography>
              
              {activeExecutions.length === 0 ? (
                <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
                  No active workflow executions
                </Typography>
              ) : (
                <List>
                  {activeExecutions.map((execution) => (
                    <ListItem key={execution.id} divider>
                      <ListItemIcon>
                        {execution.executionStatus === 'RUNNING' && <PlayArrow color="primary" />}
                        {execution.executionStatus === 'WAITING_FOR_APPROVAL' && <Approval color="warning" />}
                        {execution.executionStatus === 'PAUSED' && <Pause color="action" />}
                        {execution.executionStatus === 'FAILED' && <ErrorIcon color="error" />}
                      </ListItemIcon>
                      
                      <ListItemText
                        primary={
                          <Box display="flex" alignItems="center" gap={1}>
                            <Typography variant="body1">
                              {execution.workflowName}
                            </Typography>
                            <Chip
                              label={execution.executionStatus.replace('_', ' ')}
                              size="small"
                              color={
                                execution.executionStatus === 'RUNNING' ? 'primary' :
                                execution.executionStatus === 'WAITING_FOR_APPROVAL' ? 'warning' :
                                execution.executionStatus === 'FAILED' ? 'error' : 'default'
                              }
                            />
                            <Chip
                              label={`Priority: ${execution.priority}`}
                              size="small"
                              variant="outlined"
                              color={execution.priority >= 80 ? 'error' : execution.priority >= 50 ? 'warning' : 'default'}
                            />
                          </Box>
                        }
                        secondary={
                          <Box>
                            <Typography variant="body2" color="text.secondary">
                              File: {execution.fileName} • Step {execution.currentStep}/{execution.totalSteps}
                            </Typography>
                            <LinearProgress
                              variant="determinate"
                              value={(execution.currentStep / execution.totalSteps) * 100}
                              sx={{ mt: 1, height: 6, borderRadius: 3 }}
                            />
                            <Typography variant="caption" color="text.secondary">
                              Started: {new Date(execution.startedAt).toLocaleString()}
                            </Typography>
                          </Box>
                        }
                      />
                      
                      <ListItemSecondaryAction>
                        <Tooltip title="View Execution Details">
                          <IconButton
                            size="small"
                            onClick={() => setSelectedExecution(execution)}
                          >
                            <Visibility />
                          </IconButton>
                        </Tooltip>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  const renderWorkflowDefinitions = () => {
    return (
      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Workflow Definitions ({workflows.length})
            </Typography>
            <Button
              startIcon={<Add />}
              variant="contained"
              onClick={() => setShowWorkflowDialog(true)}
            >
              Create Workflow
            </Button>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Workflow Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Executions</TableCell>
                  <TableCell>Success Rate</TableCell>
                  <TableCell>Avg Duration</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {workflows.map((workflow) => (
                  <TableRow key={workflow.id}>
                    <TableCell>
                      <Typography variant="body1" fontWeight="medium">
                        {workflow.workflowName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={workflow.workflowType.replace('_', ' ')}
                        size="small"
                        color="primary"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={workflow.isActive ? 'Active' : 'Inactive'}
                        size="small"
                        color={workflow.isActive ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>{workflow.executionCount.toLocaleString()}</TableCell>
                    <TableCell>
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography variant="body2">
                          {Math.round((workflow.successCount / workflow.executionCount) * 100)}%
                        </Typography>
                        <LinearProgress
                          variant="determinate"
                          value={(workflow.successCount / workflow.executionCount) * 100}
                          sx={{ width: 60, height: 4 }}
                          color={
                            (workflow.successCount / workflow.executionCount) >= 0.9 ? 'success' :
                            (workflow.successCount / workflow.executionCount) >= 0.7 ? 'warning' : 'error'
                          }
                        />
                      </Box>
                    </TableCell>
                    <TableCell>
                      {workflow.averageDurationMinutes.toFixed(1)}m
                    </TableCell>
                    <TableCell>
                      <Box display="flex" gap={0.5}>
                        <Tooltip title="View Details">
                          <IconButton
                            size="small"
                            onClick={() => setSelectedWorkflow(workflow)}
                          >
                            <Visibility />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Edit Workflow">
                          <IconButton
                            size="small"
                            onClick={() => {
                              setSelectedWorkflow(workflow);
                              setShowWorkflowDialog(true);
                            }}
                          >
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title={workflow.isActive ? 'Disable' : 'Enable'}>
                          <IconButton
                            size="small"
                            color={workflow.isActive ? 'error' : 'success'}
                          >
                            {workflow.isActive ? <Pause /> : <PlayArrow />}
                          </IconButton>
                        </Tooltip>
                      </Box>
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

  const renderBusinessRules = () => {
    return (
      <Card>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Business Rules ({businessRules.length})
            </Typography>
            <Button
              startIcon={<Add />}
              variant="contained"
              onClick={() => setShowRuleDialog(true)}
            >
              Create Rule
            </Button>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Rule Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Priority</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Executions</TableCell>
                  <TableCell>Success Rate</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {businessRules.map((rule) => (
                  <TableRow key={rule.id}>
                    <TableCell>
                      <Typography variant="body1" fontWeight="medium">
                        {rule.ruleName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={rule.ruleType.replace('_', ' ')}
                        size="small"
                        color="secondary"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={rule.rulePriority}
                        size="small"
                        color={
                          rule.rulePriority >= 80 ? 'error' :
                          rule.rulePriority >= 50 ? 'warning' : 'default'
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        {rule.ruleCategory}
                      </Typography>
                    </TableCell>
                    <TableCell>{rule.executionCount.toLocaleString()}</TableCell>
                    <TableCell>
                      {rule.executionCount > 0 ? 
                        Math.round((rule.successCount / rule.executionCount) * 100) + '%' : 
                        'N/A'
                      }
                    </TableCell>
                    <TableCell>
                      <Box display="flex" gap={0.5}>
                        <Tooltip title="Edit Rule">
                          <IconButton size="small">
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title={rule.isActive ? 'Disable' : 'Enable'}>
                          <IconButton
                            size="small"
                            color={rule.isActive ? 'error' : 'success'}
                          >
                            {rule.isActive ? <Pause /> : <PlayArrow />}
                          </IconButton>
                        </Tooltip>
                      </Box>
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

  const renderWorkflowTemplates = () => {
    const templates = [
      {
        name: 'Standard Processing',
        description: 'Basic file validation and processing workflow',
        steps: 4,
        category: 'General'
      },
      {
        name: 'Customer Data Processing',
        description: 'Secure processing workflow for customer data',
        steps: 6,
        category: 'Security'
      },
      {
        name: 'Large File Processing',
        description: 'Optimized workflow for large files with parallel processing',
        steps: 5,
        category: 'Performance'
      },
      {
        name: 'Error Recovery',
        description: 'Automated error recovery and escalation workflow',
        steps: 3,
        category: 'Error Handling'
      }
    ];

    return (
      <Grid container spacing={2}>
        {templates.map((template, index) => (
          <Grid item xs={12} md={6} lg={4} key={index}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  {template.name}
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  {template.description}
                </Typography>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Chip
                    label={template.category}
                    size="small"
                    color="primary"
                    variant="outlined"
                  />
                  <Typography variant="caption" color="text.secondary">
                    {template.steps} steps
                  </Typography>
                </Box>
              </CardContent>
              <CardActions>
                <Button size="small" startIcon={<Visibility />}>
                  Preview
                </Button>
                <Button size="small" startIcon={<Add />} color="primary">
                  Use Template
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>
    );
  };

  const renderWorkflowMonitoring = () => {
    return (
      <Grid container spacing={3}>
        {/* Performance Metrics */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                <Assessment sx={{ mr: 1, verticalAlign: 'middle' }} />
                Performance Metrics
              </Typography>
              
              <Box display="flex" flexDirection="column" gap={2}>
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2">Average Execution Time</Typography>
                  <Typography variant="h6" color="primary">
                    {workflowStatistics ? 
                      (workflows.reduce((sum, w) => sum + w.averageDurationMinutes, 0) / workflows.length).toFixed(1) + 'm' : 
                      'N/A'
                    }
                  </Typography>
                </Box>
                
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2">Total Executions Today</Typography>
                  <Typography variant="h6" color="success.main">
                    {workflowStatistics?.totalExecutions.toLocaleString() || 0}
                  </Typography>
                </Box>
                
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2">Success Rate</Typography>
                  <Typography variant="h6" color="info.main">
                    {Math.round(workflowStatistics?.averageSuccessRate || 0)}%
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* System Health */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                <Speed sx={{ mr: 1, verticalAlign: 'middle' }} />
                System Health
              </Typography>
              
              <List dense>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircle color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Workflow Engine"
                    secondary="Operating normally"
                  />
                </ListItem>
                
                <ListItem>
                  <ListItemIcon>
                    <CheckCircle color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Business Rules Engine"
                    secondary="All rules active and responsive"
                  />
                </ListItem>
                
                <ListItem>
                  <ListItemIcon>
                    <Warning color="warning" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Approval Queue"
                    secondary={`${workflowStatistics?.waitingApproval || 0} items waiting`}
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <AccountTree sx={{ mr: 2, verticalAlign: 'middle' }} />
        Workflow Automation
      </Typography>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        Intelligent workflow automation with business rules, conditional processing, and multi-step orchestration.
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

      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
          <Tab label="Overview" />
          <Tab label="Workflows" />
          <Tab label="Business Rules" />
          <Tab label="Templates" />
          <Tab label="Monitoring" />
        </Tabs>
        
        <Button
          startIcon={<Refresh />}
          onClick={fetchWorkflowData}
          disabled={loading}
        >
          Refresh
        </Button>
      </Box>

      <TabPanel value={activeTab} index={0}>
        {renderWorkflowOverview()}
      </TabPanel>

      <TabPanel value={activeTab} index={1}>
        {renderWorkflowDefinitions()}
      </TabPanel>

      <TabPanel value={activeTab} index={2}>
        {renderBusinessRules()}
      </TabPanel>

      <TabPanel value={activeTab} index={3}>
        {renderWorkflowTemplates()}
      </TabPanel>

      <TabPanel value={activeTab} index={4}>
        {renderWorkflowMonitoring()}
      </TabPanel>

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
          <Box textAlign="center" color="white">
            <AccountTree sx={{ fontSize: 60, mb: 2 }} />
            <Typography variant="h6">Loading Workflow Data...</Typography>
          </Box>
        </Box>
      )}
    </Box>
  );
};

export default WorkflowManagement;