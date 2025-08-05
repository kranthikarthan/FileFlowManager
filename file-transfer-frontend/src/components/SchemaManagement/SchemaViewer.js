import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Grid,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Schema as SchemaIcon
} from '@mui/icons-material';

const SchemaViewer = ({ schema, onClose }) => {
  if (!schema) return null;

  const getSchemaTypeColor = (type) => {
    const colors = {
      'CSV': 'primary',
      'JSON': 'success',
      'XML': 'warning',
      'FIXED_WIDTH': 'info',
      'COBOL_COPYBOOK': 'secondary'
    };
    return colors[type] || 'default';
  };

  const getStatusColor = (isActive) => {
    return isActive ? 'success' : 'default';
  };

  return (
    <Dialog open={true} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" gap={1}>
          <SchemaIcon />
          <Typography variant="h6">Schema Details</Typography>
          <Chip
            label={schema.schemaType}
            color={getSchemaTypeColor(schema.schemaType)}
            size="small"
          />
          <Chip
            label={schema.isActive ? 'Active' : 'Inactive'}
            color={getStatusColor(schema.isActive)}
            size="small"
          />
        </Box>
      </DialogTitle>
      
      <DialogContent>
        <Grid container spacing={3}>
          {/* Basic Information */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>Basic Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">Schema Name</Typography>
                <Typography variant="body1">{schema.schemaName}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">Service Type</Typography>
                <Typography variant="body1">{schema.serviceType}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">Version</Typography>
                <Typography variant="body1">{schema.schemaVersion}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">Created By</Typography>
                <Typography variant="body1">{schema.createdBy || 'System'}</Typography>
              </Grid>
            </Grid>
          </Grid>

          {/* Description */}
          {schema.description && (
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>Description</Typography>
              <Typography variant="body1">{schema.description}</Typography>
            </Grid>
          )}

          {/* Schema Definition */}
          <Grid item xs={12}>
            <Accordion defaultExpanded>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="h6">Schema Definition</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Typography
                    component="pre"
                    variant="body2"
                    sx={{ 
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word'
                    }}
                  >
                    {schema.schemaDefinition}
                  </Typography>
                </Paper>
              </AccordionDetails>
            </Accordion>
          </Grid>

          {/* Validation Rules */}
          {schema.validationRules && schema.validationRules.length > 0 && (
            <Grid item xs={12}>
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6">
                    Validation Rules ({schema.validationRules.length})
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <TableContainer component={Paper}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Rule Type</TableCell>
                          <TableCell>Rule Name</TableCell>
                          <TableCell>Rule Definition</TableCell>
                          <TableCell>Error Message</TableCell>
                          <TableCell>Order</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {schema.validationRules.map((rule, index) => (
                          <TableRow key={index}>
                            <TableCell>
                              <Chip label={rule.ruleType} size="small" />
                            </TableCell>
                            <TableCell>{rule.ruleName}</TableCell>
                            <TableCell>
                              <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                                {rule.ruleDefinition}
                              </Typography>
                            </TableCell>
                            <TableCell>{rule.errorMessage}</TableCell>
                            <TableCell>{rule.ruleOrder}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </AccordionDetails>
              </Accordion>
            </Grid>
          )}

          {/* Schema Fields */}
          {schema.fields && schema.fields.length > 0 && (
            <Grid item xs={12}>
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6">
                    Schema Fields ({schema.fields.length})
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <TableContainer component={Paper}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Field Name</TableCell>
                          <TableCell>Field Type</TableCell>
                          <TableCell>Start Position</TableCell>
                          <TableCell>Length</TableCell>
                          <TableCell>Required</TableCell>
                          <TableCell>Description</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {schema.fields.map((field, index) => (
                          <TableRow key={index}>
                            <TableCell>{field.fieldName}</TableCell>
                            <TableCell>
                              <Chip label={field.fieldType} size="small" />
                            </TableCell>
                            <TableCell>{field.startPosition || '-'}</TableCell>
                            <TableCell>{field.fieldLength || '-'}</TableCell>
                            <TableCell>
                              <Chip 
                                label={field.required ? 'Yes' : 'No'} 
                                color={field.required ? 'error' : 'default'}
                                size="small" 
                              />
                            </TableCell>
                            <TableCell>{field.description || '-'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </AccordionDetails>
              </Accordion>
            </Grid>
          )}
        </Grid>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default SchemaViewer;