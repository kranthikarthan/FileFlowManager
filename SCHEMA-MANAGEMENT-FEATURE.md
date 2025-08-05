# File Schema Management Feature

## Overview
The File Schema Management feature provides comprehensive capabilities for defining, managing, and validating file schemas across different service types. This feature enables administrators to create structured validation rules and field definitions that are stored in the database and used during file processing.

## Key Features

### 1. Schema Definition
- **Multiple Schema Types**: Support for CSV, JSON, XML, and Fixed Width file formats
- **Version Control**: Schema versioning for backward compatibility
- **Multi-tenant Support**: Tenant-specific schema management
- **Service Type Association**: Schemas linked to specific service types

### 2. Validation Rules
- **Regex Validation**: Pattern-based validation using regular expressions
- **JSON Schema Validation**: JSON structure validation
- **XML Schema Validation**: XML structure validation
- **Custom Rules**: Custom validation logic with expression evaluation
- **Rule Ordering**: Configurable rule execution order
- **Error Messages**: Customizable error messages for failed validations

### 3. Field Definitions
- **Field Types**: Support for STRING, INTEGER, DECIMAL, DATE, BOOLEAN
- **Field Constraints**: Required fields, unique constraints, length limits
- **Default Values**: Configurable default values for fields
- **Validation Regex**: Field-level regex validation
- **Field Ordering**: Configurable field order for structured files

### 4. File Validation
- **Real-time Validation**: Upload and validate files against schemas
- **Validation Logging**: Track validation results and usage statistics
- **Multi-format Support**: Validate various file formats
- **Detailed Reporting**: Comprehensive validation result reporting

## Database Schema

### Tables Created
1. **file_schemas** - Main schema definitions
2. **schema_validation_rules** - Validation rules for schemas
3. **schema_fields** - Field definitions for schemas
4. **schema_usage_log** - Validation usage tracking

### Key Relationships
- One-to-many relationship between schemas and validation rules
- One-to-many relationship between schemas and fields
- One-to-many relationship between schemas and usage logs

## API Endpoints

### Schema Management
- `POST /api/schemas` - Create new schema
- `PUT /api/schemas/{schemaId}` - Update existing schema
- `DELETE /api/schemas/{schemaId}` - Delete schema
- `GET /api/schemas/{schemaId}` - Get schema by ID
- `GET /api/schemas/tenant/{tenantId}` - Get all schemas for tenant
- `GET /api/schemas/tenant/{tenantId}/service/{serviceType}` - Get schemas by service type

### Validation Rules
- `POST /api/schemas/{schemaId}/validation-rules` - Add validation rule
- `DELETE /api/schemas/validation-rules/{ruleId}` - Delete validation rule

### Field Management
- `POST /api/schemas/{schemaId}/fields` - Add field definition
- `DELETE /api/schemas/fields/{fieldId}` - Delete field definition

### File Validation
- `POST /api/schemas/validate-file` - Validate uploaded file
- `POST /api/schemas/validate-file-content` - Validate file content

### Templates
- `GET /api/schemas/templates` - Get schema templates

## Frontend Components

### SchemaList.js
- **Schema Listing**: Display all schemas in a table format
- **Schema Actions**: Create, edit, view, and delete schemas
- **Status Indicators**: Visual indicators for schema status and type
- **File Validation**: Integrated file upload and validation

### SchemaForm.js
- **Schema Creation**: Comprehensive form for creating schemas
- **Schema Editing**: Full editing capabilities for existing schemas
- **Validation Rules**: Dynamic addition and management of validation rules
- **Field Definitions**: Dynamic addition and management of field definitions
- **Schema Templates**: Pre-built templates for common schema types

### FileUploadDialog.js
- **File Selection**: Drag-and-drop file upload interface
- **Service Type Selection**: Dropdown for selecting target service type
- **Validation Results**: Real-time validation result display
- **Progress Tracking**: Visual progress indicators during validation

## Backend Services

### FileSchemaService
- **Schema CRUD Operations**: Complete create, read, update, delete operations
- **Validation Logic**: Comprehensive file validation against schemas
- **Rule Processing**: Multi-type validation rule processing
- **Usage Tracking**: Validation result logging and statistics

### Validation Types Supported
1. **Regex Validation**: Pattern matching using regular expressions
2. **Custom Rules**: Expression-based validation with variables
3. **JSON Schema**: JSON structure validation (placeholder for implementation)
4. **XML Schema**: XML structure validation (placeholder for implementation)

## Integration Points

### Service Configuration Integration
- Schemas are associated with specific service types
- Validation occurs during file processing
- Service-specific validation rules and field definitions

### Multi-tenancy Integration
- Tenant-specific schema management
- Isolated schema definitions per tenant
- Tenant-aware validation and logging

### Enhanced Cut-off Time Integration
- Schema validation integrated with cut-off time processing
- Validation results logged with processing context
- Service-specific validation timing

## Usage Examples

### Creating a CSV Schema
```json
{
  "tenantId": "tenant1",
  "serviceType": "financial-service",
  "schemaName": "Transaction CSV Schema",
  "schemaType": "CSV",
  "schemaDefinition": {
    "type": "csv",
    "delimiter": ",",
    "hasHeader": true,
    "fields": [
      {"name": "transaction_id", "type": "STRING", "required": true, "length": 50},
      {"name": "amount", "type": "DECIMAL", "required": true, "precision": 10, "scale": 2},
      {"name": "currency", "type": "STRING", "required": true, "length": 3},
      {"name": "transaction_date", "type": "DATE", "required": true, "format": "yyyy-MM-dd"}
    ]
  }
}
```

### Adding Validation Rules
```json
{
  "ruleName": "File Size Check",
  "ruleType": "CUSTOM",
  "ruleDefinition": "fileSize <= 10485760",
  "errorMessage": "File size exceeds maximum allowed size of 10MB"
}
```

### File Validation Response
```json
{
  "valid": true,
  "message": "Validation passed",
  "fileName": "transactions.csv",
  "fileSize": 2048,
  "tenantId": "tenant1",
  "serviceType": "financial-service"
}
```

## Benefits

### For Administrators
- **Centralized Schema Management**: All schemas managed in one place
- **Flexible Validation Rules**: Customizable validation logic
- **Version Control**: Schema versioning for compatibility
- **Usage Analytics**: Track validation usage and success rates

### For Developers
- **RESTful API**: Complete REST API for schema management
- **Extensible Design**: Easy to add new validation types
- **Database Integration**: Full database persistence and querying
- **Frontend Integration**: Ready-to-use React components

### For End Users
- **Real-time Validation**: Immediate feedback on file validation
- **User-friendly Interface**: Intuitive schema management interface
- **Template Support**: Pre-built templates for common formats
- **Detailed Feedback**: Comprehensive validation result reporting

## Future Enhancements

### Planned Features
1. **JSON Schema Validation**: Full JSON Schema specification support
2. **XML Schema Validation**: XML Schema validation implementation
3. **Advanced Expression Engine**: More sophisticated custom rule evaluation
4. **Schema Import/Export**: Bulk schema management capabilities
5. **Validation Analytics**: Advanced validation statistics and reporting
6. **Schema Versioning**: Enhanced version control and migration
7. **API Rate Limiting**: Validation API rate limiting
8. **Caching**: Schema and validation rule caching for performance

### Integration Opportunities
1. **Machine Learning**: AI-powered schema suggestions
2. **Data Quality Scoring**: Automated data quality assessment
3. **Schema Discovery**: Automatic schema detection from sample files
4. **Compliance Reporting**: Regulatory compliance validation
5. **Integration with ETL**: Direct integration with ETL processes

## Deployment Notes

### Database Migration
Run the schema creation script:
```bash
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -i scripts/create-schema-tables.sql
```

### Configuration
Ensure the following environment variables are set:
- `ENHANCED_CUTOFF_ENABLED=true`
- `SCHEMA_VALIDATION_ENABLED=true`
- `MULTI_TENANT_ENABLED=true`

### Frontend Integration
Add the schema management components to your React application:
```javascript
import SchemaList from './components/SchemaManagement/SchemaList';
import SchemaForm from './components/SchemaManagement/SchemaForm';
import FileUploadDialog from './components/SchemaManagement/FileUploadDialog';
```

This comprehensive schema management feature provides the foundation for robust file validation and processing in the File Transfer Management System.