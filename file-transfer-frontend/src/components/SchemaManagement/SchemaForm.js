import React, { useState, useEffect } from 'react';
import {
  TextField,
  Button,
  Box,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Typography,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  IconButton,
  Chip
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Add as AddIcon,
  Delete as DeleteIcon
} from '@mui/icons-material';

const SchemaForm = ({ schema, tenantId, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    tenantId: tenantId || '',
    serviceType: '',
    schemaName: '',
    schemaVersion: '1.0',
    schemaType: 'CSV',
    schemaDefinition: '',
    description: '',
    isActive: true
  });
  const [validationRules, setValidationRules] = useState([]);
  const [fields, setFields] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (schema) {
      setFormData({
        tenantId: schema.tenantId,
        serviceType: schema.serviceType,
        schemaName: schema.schemaName,
        schemaVersion: schema.schemaVersion,
        schemaType: schema.schemaType,
        schemaDefinition: schema.schemaDefinition,
        description: schema.description,
        isActive: schema.isActive
      });
      setValidationRules(schema.validationRules || []);
      setFields(schema.fields || []);
    }
  }, [schema]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const url = schema ? `/api/schemas/${schema.id}` : '/api/schemas';
      const method = schema ? 'PUT' : 'POST';
      const params = schema ? `?updatedBy=admin` : `?createdBy=admin`;

      const response = await fetch(url + params, {
        method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ...formData,
          validationRules,
          fields
        })
      });

      if (response.ok) {
        onSave();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Failed to save schema');
      }
    } catch (err) {
      setError('Error saving schema: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const addValidationRule = () => {
    setValidationRules(prev => [...prev, {
      id: Date.now(),
      ruleName: '',
      ruleType: 'REGEX',
      ruleDefinition: '',
      ruleOrder: prev.length + 1,
      isActive: true,
      errorMessage: ''
    }]);
  };

  const updateValidationRule = (index, field, value) => {
    setValidationRules(prev => prev.map((rule, i) => 
      i === index ? { ...rule, [field]: value } : rule
    ));
  };

  const removeValidationRule = (index) => {
    setValidationRules(prev => prev.filter((_, i) => i !== index));
  };

  const addField = () => {
    setFields(prev => [...prev, {
      id: Date.now(),
      fieldName: '',
      fieldType: 'STRING',
      fieldLength: null,
      isRequired: false,
      isUnique: false,
      defaultValue: '',
      validationRegex: '',
      fieldOrder: prev.length + 1,
      description: ''
    }]);
  };

  const updateField = (index, field, value) => {
    setFields(prev => prev.map((f, i) => 
      i === index ? { ...f, [field]: value } : f
    ));
  };

  const removeField = (index) => {
    setFields(prev => prev.filter((_, i) => i !== index));
  };

  const getSchemaTemplate = (type) => {
    const templates = {
      CSV: {
        delimiter: ',',
        hasHeader: true,
        fields: [
          { name: 'field1', type: 'STRING', required: true },
          { name: 'field2', type: 'INTEGER', required: false }
        ]
      },
      JSON: {
        type: 'object',
        properties: {
          id: { type: 'string' },
          name: { type: 'string' },
          value: { type: 'number' }
        },
        required: ['id', 'name']
      },
      XML: {
        root: 'data',
        elements: [
          { name: 'item', type: 'element' },
          { name: 'value', type: 'text' }
        ]
      },
      FIXED_WIDTH: {
        fields: [
          { name: 'field1', start: 1, length: 10, type: 'STRING' },
          { name: 'field2', start: 11, length: 5, type: 'INTEGER' }
        ]
      }
    };
    return templates[type] || {};
  };

  const handleSchemaTypeChange = (type) => {
    handleInputChange('schemaType', type);
    const template = getSchemaTemplate(type);
    handleInputChange('schemaDefinition', JSON.stringify(template, null, 2));
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Schema Name"
            value={formData.schemaName}
            onChange={(e) => handleInputChange('schemaName', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Service Type"
            value={formData.serviceType}
            onChange={(e) => handleInputChange('serviceType', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <FormControl fullWidth margin="normal">
            <InputLabel>Schema Type</InputLabel>
            <Select
              value={formData.schemaType}
              onChange={(e) => handleSchemaTypeChange(e.target.value)}
              label="Schema Type"
            >
              <MenuItem value="CSV">CSV</MenuItem>
              <MenuItem value="JSON">JSON</MenuItem>
              <MenuItem value="XML">XML</MenuItem>
              <MenuItem value="FIXED_WIDTH">Fixed Width</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            label="Schema Version"
            value={formData.schemaVersion}
            onChange={(e) => handleInputChange('schemaVersion', e.target.value)}
            required
            margin="normal"
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Description"
            value={formData.description}
            onChange={(e) => handleInputChange('description', e.target.value)}
            multiline
            rows={2}
            margin="normal"
          />
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={formData.isActive}
                onChange={(e) => handleInputChange('isActive', e.target.checked)}
              />
            }
            label="Active"
          />
        </Grid>
      </Grid>

      {/* Schema Definition */}
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Schema Definition</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <TextField
            fullWidth
            label="Schema Definition (JSON)"
            value={formData.schemaDefinition}
            onChange={(e) => handleInputChange('schemaDefinition', e.target.value)}
            multiline
            rows={8}
            margin="normal"
            helperText="Define the schema structure in JSON format"
          />
        </AccordionDetails>
      </Accordion>

      {/* Validation Rules */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">
            Validation Rules ({validationRules.length})
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box mb={2}>
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={addValidationRule}
            >
              Add Validation Rule
            </Button>
          </Box>
          
          {validationRules.map((rule, index) => (
            <Box key={rule.id} border={1} borderColor="grey.300" p={2} mb={2} borderRadius={1}>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} md={3}>
                  <TextField
                    fullWidth
                    label="Rule Name"
                    value={rule.ruleName}
                    onChange={(e) => updateValidationRule(index, 'ruleName', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Rule Type</InputLabel>
                    <Select
                      value={rule.ruleType}
                      onChange={(e) => updateValidationRule(index, 'ruleType', e.target.value)}
                      label="Rule Type"
                    >
                      <MenuItem value="REGEX">Regex</MenuItem>
                      <MenuItem value="JSON_SCHEMA">JSON Schema</MenuItem>
                      <MenuItem value="XML_SCHEMA">XML Schema</MenuItem>
                      <MenuItem value="CUSTOM">Custom</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Rule Definition"
                    value={rule.ruleDefinition}
                    onChange={(e) => updateValidationRule(index, 'ruleDefinition', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <TextField
                    fullWidth
                    label="Error Message"
                    value={rule.errorMessage}
                    onChange={(e) => updateValidationRule(index, 'errorMessage', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={1}>
                  <IconButton
                    color="error"
                    onClick={() => removeValidationRule(index)}
                    size="small"
                  >
                    <DeleteIcon />
                  </IconButton>
                </Grid>
              </Grid>
            </Box>
          ))}
        </AccordionDetails>
      </Accordion>

      {/* Fields */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">
            Fields ({fields.length})
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box mb={2}>
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={addField}
            >
              Add Field
            </Button>
          </Box>
          
          {fields.map((field, index) => (
            <Box key={field.id} border={1} borderColor="grey.300" p={2} mb={2} borderRadius={1}>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} md={2}>
                  <TextField
                    fullWidth
                    label="Field Name"
                    value={field.fieldName}
                    onChange={(e) => updateField(index, 'fieldName', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Field Type</InputLabel>
                    <Select
                      value={field.fieldType}
                      onChange={(e) => updateField(index, 'fieldType', e.target.value)}
                      label="Field Type"
                    >
                      <MenuItem value="STRING">String</MenuItem>
                      <MenuItem value="INTEGER">Integer</MenuItem>
                      <MenuItem value="DECIMAL">Decimal</MenuItem>
                      <MenuItem value="DATE">Date</MenuItem>
                      <MenuItem value="BOOLEAN">Boolean</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={1}>
                  <TextField
                    fullWidth
                    label="Length"
                    type="number"
                    value={field.fieldLength || ''}
                    onChange={(e) => updateField(index, 'fieldLength', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <TextField
                    fullWidth
                    label="Default Value"
                    value={field.defaultValue}
                    onChange={(e) => updateField(index, 'defaultValue', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <TextField
                    fullWidth
                    label="Validation Regex"
                    value={field.validationRegex}
                    onChange={(e) => updateField(index, 'validationRegex', e.target.value)}
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={2}>
                  <Box display="flex" gap={1}>
                    <Chip
                      label="Required"
                      color={field.isRequired ? 'primary' : 'default'}
                      size="small"
                      onClick={() => updateField(index, 'isRequired', !field.isRequired)}
                    />
                    <Chip
                      label="Unique"
                      color={field.isUnique ? 'primary' : 'default'}
                      size="small"
                      onClick={() => updateField(index, 'isUnique', !field.isUnique)}
                    />
                  </Box>
                </Grid>
                <Grid item xs={12} md={1}>
                  <IconButton
                    color="error"
                    onClick={() => removeField(index)}
                    size="small"
                  >
                    <DeleteIcon />
                  </IconButton>
                </Grid>
              </Grid>
            </Box>
          ))}
        </AccordionDetails>
      </Accordion>

      <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
        <Button onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button
          type="submit"
          variant="contained"
          disabled={loading}
        >
          {loading ? 'Saving...' : (schema ? 'Update Schema' : 'Create Schema')}
        </Button>
      </Box>
    </Box>
  );
};

export default SchemaForm;