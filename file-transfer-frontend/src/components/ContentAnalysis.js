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
  LinearProgress,
  Alert,
  AlertTitle,
  Accordion,
  AccordionSummary,
  AccordionDetails,
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
  CircularProgress,
  Tooltip,
  IconButton,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Tab,
  Tabs,
  TabPanel
} from '@mui/material';
import {
  ExpandMore,
  Analytics,
  Assessment,
  BugReport,
  CheckCircle,
  Error,
  Warning,
  Info,
  FileUpload,
  Download,
  Refresh,
  Visibility,
  Code,
  TableChart,
  Description,
  DataObject,
  ViewColumn,
  ViewList,
  SwapHoriz,
  InsertDriveFile
} from '@mui/icons-material';
import { contentAnalysisService } from '../services/contentAnalysisService';
import './ContentAnalysis.css';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`analysis-tabpanel-${index}`}
      aria-labelledby={`analysis-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

export const ContentAnalysis = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [analysisResult, setAnalysisResult] = useState(null);
  const [comprehensiveResult, setComprehensiveResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [analysisProgress, setAnalysisProgress] = useState(0);
  const [showDetailsDialog, setShowDetailsDialog] = useState(false);
  const [selectedAnalysis, setSelectedAnalysis] = useState(null);

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setUploadedFile(file);
    setError(null);
    setLoading(true);
    setAnalysisProgress(0);

    try {
      // Simulate progress updates
      const progressInterval = setInterval(() => {
        setAnalysisProgress(prev => Math.min(prev + 10, 90));
      }, 200);

      const result = await contentAnalysisService.uploadAndAnalyze(file);
      
      clearInterval(progressInterval);
      setAnalysisProgress(100);
      
      setAnalysisResult(result);
      
      // If analysis successful, perform comprehensive analysis
      if (result.success && !result.isBinaryFile) {
        const comprehensive = await contentAnalysisService.analyzeComprehensive(
          result.filePath, 
          result.fileName
        );
        setComprehensiveResult(comprehensive);
      }
      
    } catch (err) {
      setError(err.message || 'Analysis failed');
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyzeExistingFile = async (filePath, fileName) => {
    setError(null);
    setLoading(true);

    try {
      const result = await contentAnalysisService.analyzeFile(filePath, fileName);
      setAnalysisResult(result);

      if (result.success && !result.isBinaryFile) {
        const comprehensive = await contentAnalysisService.analyzeComprehensive(
          filePath, 
          fileName
        );
        setComprehensiveResult(comprehensive);
      }
    } catch (err) {
      setError(err.message || 'Analysis failed');
    } finally {
      setLoading(false);
    }
  };

  const renderFileInfo = (analysisResult) => {
    if (!analysisResult) return null;

    const summary = contentAnalysisService.generateAnalysisSummary(analysisResult);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <InsertDriveFile sx={{ mr: 1, verticalAlign: 'middle' }} />
            File Information
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="body2" color="text.secondary">File Name</Typography>
              <Typography variant="body1">{summary.fileInfo.name}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body2" color="text.secondary">File Size</Typography>
              <Typography variant="body1">{summary.fileInfo.size}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body2" color="text.secondary">Detected Type</Typography>
              <Chip 
                label={summary.fileInfo.type}
                color="primary"
                variant="outlined"
                icon={<Code />}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body2" color="text.secondary">Encoding</Typography>
              <Typography variant="body1">{summary.fileInfo.encoding}</Typography>
            </Grid>
            {!summary.fileInfo.isBinary && (
              <>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2" color="text.secondary">Line Count</Typography>
                  <Typography variant="body1">{summary.structure.lineCount}</Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2" color="text.secondary">Average Line Length</Typography>
                  <Typography variant="body1">{summary.structure.avgLineLength} characters</Typography>
                </Grid>
              </>
            )}
          </Grid>
        </CardContent>
      </Card>
    );
  };

  const renderQualityAssessment = (analysisResult) => {
    if (!analysisResult || !analysisResult.qualityAssessment) return null;

    const summary = contentAnalysisService.generateAnalysisSummary(analysisResult);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <Assessment sx={{ mr: 1, verticalAlign: 'middle' }} />
            Quality Assessment
          </Typography>
          
          <Box display="flex" alignItems="center" mb={2}>
            <Typography variant="h4" color={`${summary.quality.color}.main`} sx={{ mr: 2 }}>
              {summary.quality.score}%
            </Typography>
            <Box>
              <Chip 
                label={summary.quality.grade}
                color={summary.quality.color}
                sx={{ mb: 1 }}
              />
              <LinearProgress 
                variant="determinate" 
                value={parseFloat(summary.quality.score)}
                color={summary.quality.color}
                sx={{ width: 200 }}
              />
            </Box>
          </Box>

          {summary.quality.issues.length > 0 && (
            <Alert severity={summary.quality.color === 'error' ? 'error' : 'warning'} sx={{ mt: 2 }}>
              <AlertTitle>Quality Issues Detected</AlertTitle>
              <List dense>
                {summary.quality.issues.map((issue, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Warning fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={issue} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}
        </CardContent>
      </Card>
    );
  };

  const renderStructureAnalysis = (analysisResult) => {
    if (!analysisResult || !analysisResult.structureAnalysis) return null;

    const formatted = contentAnalysisService.formatStructureAnalysis(analysisResult.structureAnalysis);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <Analytics sx={{ mr: 1, verticalAlign: 'middle' }} />
            Structure Analysis
          </Typography>
          
          <TableContainer>
            <Table size="small">
              <TableBody>
                {formatted.map((item, index) => (
                  <TableRow key={index}>
                    <TableCell component="th" scope="row" sx={{ fontWeight: 'medium' }}>
                      {item.key}
                    </TableCell>
                    <TableCell>{item.value}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    );
  };

  const renderSchemaValidation = (validationResult) => {
    if (!validationResult) return null;

    const errors = contentAnalysisService.formatValidationErrors(validationResult.validationErrors || []);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <BugReport sx={{ mr: 1, verticalAlign: 'middle' }} />
            Schema Validation
          </Typography>
          
          <Box display="flex" alignItems="center" mb={2}>
            {validationResult.success ? (
              <Chip 
                icon={<CheckCircle />}
                label="Validation Passed"
                color="success"
              />
            ) : (
              <Chip 
                icon={<Error />}
                label={`${errors.length} Errors Found`}
                color="error"
              />
            )}
          </Box>

          {errors.length > 0 && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Line</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Message</TableCell>
                    <TableCell>Severity</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {errors.slice(0, 10).map((error, index) => (
                    <TableRow key={index}>
                      <TableCell>{error.line}</TableCell>
                      <TableCell>
                        <Chip 
                          label={error.type}
                          size="small"
                          color={error.color}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{error.message}</TableCell>
                      <TableCell>
                        <Chip 
                          label={error.severity.toUpperCase()}
                          size="small"
                          color={error.severity === 'high' ? 'error' : 'warning'}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    );
  };

  const renderDataProfile = (dataProfile) => {
    if (!dataProfile) return null;

    const formattedColumns = contentAnalysisService.formatColumnProfiles(dataProfile.columnProfiles || []);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <TableChart sx={{ mr: 1, verticalAlign: 'middle' }} />
            Data Profile
          </Typography>
          
          {dataProfile.dataStatistics && (
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={6} md={3}>
                <Typography variant="body2" color="text.secondary">Records</Typography>
                <Typography variant="h6">{dataProfile.recordCount?.toLocaleString()}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography variant="body2" color="text.secondary">Columns</Typography>
                <Typography variant="h6">{dataProfile.columnCount}</Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography variant="body2" color="text.secondary">Avg Quality</Typography>
                <Typography variant="h6">
                  {dataProfile.dataStatistics.averageQualityScore?.toFixed(1) || 0}%
                </Typography>
              </Grid>
              <Grid item xs={6} md={3}>
                <Typography variant="body2" color="text.secondary">Completeness</Typography>
                <Typography variant="h6">
                  {dataProfile.dataStatistics.averageCompleteness?.toFixed(1) || 0}%
                </Typography>
              </Grid>
            </Grid>
          )}

          {formattedColumns.length > 0 && (
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography variant="subtitle1">Column Details ({formattedColumns.length} columns)</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Column</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Quality</TableCell>
                        <TableCell>Completeness</TableCell>
                        <TableCell>Uniqueness</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {formattedColumns.map((column, index) => (
                        <TableRow key={index}>
                          <TableCell>{column.columnName}</TableCell>
                          <TableCell>
                            <Chip 
                              label={column.formattedDataType}
                              size="small"
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell>
                            <Chip 
                              label={column.formattedQualityScore}
                              size="small"
                              color={contentAnalysisService.getQualityScoreColor(column.qualityScore || 0)}
                            />
                          </TableCell>
                          <TableCell>{column.formattedCompleteness}</TableCell>
                          <TableCell>{column.formattedUniqueness}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </AccordionDetails>
            </Accordion>
          )}

          {dataProfile.qualityInsights && dataProfile.qualityInsights.length > 0 && (
            <Alert severity="info" sx={{ mt: 2 }}>
              <AlertTitle>Quality Insights</AlertTitle>
              <List dense>
                {dataProfile.qualityInsights.map((insight, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Info fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={insight} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}
        </CardContent>
      </Card>
    );
  };

  const renderRecommendations = () => {
    if (!analysisResult && !comprehensiveResult) return null;

    let recommendations = [];
    
    if (analysisResult?.qualityAssessment?.recommendations) {
      recommendations = [...recommendations, ...analysisResult.qualityAssessment.recommendations];
    }
    
    if (comprehensiveResult?.dataProfile?.processingRecommendations) {
      recommendations = [...recommendations, ...comprehensiveResult.dataProfile.processingRecommendations];
    }

    if (recommendations.length === 0) return null;

    const prioritized = contentAnalysisService.prioritizeRecommendations(recommendations);

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <Visibility sx={{ mr: 1, verticalAlign: 'middle' }} />
            Recommendations
          </Typography>
          
          {prioritized.high.length > 0 && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <AlertTitle>High Priority</AlertTitle>
              <List dense>
                {prioritized.high.map((rec, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Error fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={rec} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}

          {prioritized.medium.length > 0 && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              <AlertTitle>Medium Priority</AlertTitle>
              <List dense>
                {prioritized.medium.map((rec, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Warning fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={rec} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}

          {prioritized.low.length > 0 && (
            <Alert severity="info">
              <AlertTitle>Suggestions</AlertTitle>
              <List dense>
                {prioritized.low.map((rec, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <Info fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={rec} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}
        </CardContent>
      </Card>
    );
  };

  const renderAnalysisActions = () => {
    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Analysis Actions
          </Typography>
          <Grid container spacing={2}>
            <Grid item>
              <Button
                variant="contained"
                component="label"
                startIcon={<FileUpload />}
                disabled={loading}
              >
                Upload & Analyze File
                <input
                  type="file"
                  hidden
                  onChange={handleFileUpload}
                  accept=".txt,.csv,.json,.xml,.dat,.log,.yaml,.yml"
                />
              </Button>
            </Grid>
            
            {analysisResult && (
              <Grid item>
                <Button
                  variant="outlined"
                  startIcon={<Download />}
                  onClick={() => {
                    const url = contentAnalysisService.exportAnalysisResults(analysisResult, 'json');
                    if (url) {
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = `analysis_${analysisResult.fileName}.json`;
                      a.click();
                      URL.revokeObjectURL(url);
                    }
                  }}
                >
                  Export Results
                </Button>
              </Grid>
            )}
            
            {analysisResult && (
              <Grid item>
                <Button
                  variant="outlined"
                  startIcon={<Refresh />}
                  onClick={() => {
                    if (analysisResult.filePath && analysisResult.fileName) {
                      handleAnalyzeExistingFile(analysisResult.filePath, analysisResult.fileName);
                    }
                  }}
                  disabled={loading}
                >
                  Re-analyze
                </Button>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>
    );
  };

  const renderAnalysisProgress = () => {
    if (!loading) return null;

    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Analyzing File...
          </Typography>
          <LinearProgress 
            variant="determinate" 
            value={analysisProgress} 
            sx={{ mb: 1 }}
          />
          <Typography variant="body2" color="text.secondary">
            {analysisProgress < 30 && "Reading file content..."}
            {analysisProgress >= 30 && analysisProgress < 60 && "Detecting file type..."}
            {analysisProgress >= 60 && analysisProgress < 90 && "Analyzing structure..."}
            {analysisProgress >= 90 && "Generating recommendations..."}
          </Typography>
        </CardContent>
      </Card>
    );
  };

  const renderComprehensiveResults = () => {
    if (!comprehensiveResult) return null;

    return (
      <Box>
        <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 2 }}>
          <Tab label="Content Analysis" />
          <Tab label="Schema Validation" />
          <Tab label="Data Profile" />
          <Tab label="Summary" />
        </Tabs>

        <TabPanel value={activeTab} index={0}>
          {renderFileInfo(comprehensiveResult.contentAnalysis)}
          {renderQualityAssessment(comprehensiveResult.contentAnalysis)}
          {renderStructureAnalysis(comprehensiveResult.contentAnalysis)}
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          {renderSchemaValidation(comprehensiveResult.schemaValidation)}
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          {renderDataProfile(comprehensiveResult.dataProfile)}
        </TabPanel>

        <TabPanel value={activeTab} index={3}>
          {renderAnalysisSummary()}
        </TabPanel>
      </Box>
    );
  };

  const renderAnalysisSummary = () => {
    if (!comprehensiveResult) return null;

    const summary = contentAnalysisService.generateComprehensiveSummary(comprehensiveResult);

    return (
      <Grid container spacing={2}>
        {/* Content Analysis Summary */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom color="primary">
                Content Analysis
              </Typography>
              {summary.contentAnalysis ? (
                <Box>
                  <Typography variant="body2" color="text.secondary">File Type</Typography>
                  <Typography variant="body1" gutterBottom>
                    {summary.contentAnalysis.fileInfo.type}
                  </Typography>
                  
                  <Typography variant="body2" color="text.secondary">Quality Score</Typography>
                  <Box display="flex" alignItems="center">
                    <Typography variant="h6" color={`${summary.contentAnalysis.quality.color}.main`}>
                      {summary.contentAnalysis.quality.score}%
                    </Typography>
                    <Chip 
                      label={summary.contentAnalysis.quality.grade}
                      color={summary.contentAnalysis.quality.color}
                      size="small"
                      sx={{ ml: 1 }}
                    />
                  </Box>
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Content analysis not available
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Schema Validation Summary */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom color="secondary">
                Schema Validation
              </Typography>
              {summary.schemaValidation ? (
                <Box>
                  <Typography variant="body2" color="text.secondary">Validation Status</Typography>
                  <Chip 
                    icon={summary.schemaValidation.passed ? <CheckCircle /> : <Error />}
                    label={summary.schemaValidation.passed ? 'Passed' : 'Failed'}
                    color={summary.schemaValidation.passed ? 'success' : 'error'}
                    sx={{ mb: 1 }}
                  />
                  
                  {summary.schemaValidation.errorCount > 0 && (
                    <Typography variant="body2">
                      {summary.schemaValidation.errorCount} validation errors found
                    </Typography>
                  )}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Schema validation not performed
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Data Profile Summary */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom color="info.main">
                Data Profile
              </Typography>
              {summary.dataProfile ? (
                <Box>
                  <Typography variant="body2" color="text.secondary">Data Quality</Typography>
                  <Typography variant="h6" color="info.main" gutterBottom>
                    {summary.dataProfile.qualityScore?.toFixed(1) || 0}%
                  </Typography>
                  
                  <Typography variant="body2" color="text.secondary">Completeness</Typography>
                  <Typography variant="body1">
                    {summary.dataProfile.completeness?.toFixed(1) || 0}%
                  </Typography>
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Data profiling not available
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    );
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <Analytics sx={{ mr: 2, verticalAlign: 'middle' }} />
        Content Analysis & Data Profiling
      </Typography>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        Intelligent file content analysis, schema validation, and data profiling for enhanced file processing.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          <AlertTitle>Analysis Error</AlertTitle>
          {error}
        </Alert>
      )}

      {renderAnalysisActions()}
      {renderAnalysisProgress()}

      {uploadedFile && !loading && (
        <Alert severity="info" sx={{ mb: 2 }}>
          <Typography variant="body2">
            File uploaded: <strong>{uploadedFile.name}</strong> 
            ({contentAnalysisService.formatFileSize(uploadedFile.size)})
          </Typography>
        </Alert>
      )}

      {analysisResult && !loading && (
        <Box>
          {comprehensiveResult ? renderComprehensiveResults() : (
            <Box>
              {renderFileInfo(analysisResult)}
              {renderQualityAssessment(analysisResult)}
              {renderStructureAnalysis(analysisResult)}
            </Box>
          )}
          {renderRecommendations()}
        </Box>
      )}

      {!analysisResult && !loading && (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Analytics sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No Analysis Results
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Upload a file to start content analysis and data profiling
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default ContentAnalysis;