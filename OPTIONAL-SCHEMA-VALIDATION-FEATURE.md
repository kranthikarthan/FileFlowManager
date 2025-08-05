# Optional Schema Validation Feature

## Overview
The Schema Validation feature is now **optional** and can be enabled or disabled per service/subservice level. This provides flexibility for organizations that want to implement strict file validation while allowing others to operate without schema constraints.

## Key Features

### 1. Optional Per-Service Configuration
- **Enable/Disable**: Schema validation can be enabled or disabled per service configuration
- **Service-Level Control**: Each service can have its own schema validation settings
- **Subservice Support**: Individual subservices can have different validation rules
- **Default Disabled**: Schema validation is disabled by default for backward compatibility

### 2. Validation Modes
- **STRICT**: Files that don't match schema are rejected
- **LENIENT**: Files with validation issues are accepted with warnings
- **WARNING_ONLY**: Validation issues are logged but files are accepted

### 3. COBOL Copybook Support
- **COBOL_COPYBOOK Schema Type**: Full support for COBOL copybook validation
- **PIC Clause Support**: Validates COBOL picture clauses (X(n), 9(n), 9(n)V99, etc.)
- **Record Length Validation**: Validates fixed-length COBOL records
- **Field Position Validation**: Validates field positions and formats
- **Level Support**: Supports COBOL level structures

## Database Changes

### New Columns in service_configurations Table
```sql
-- Schema validation settings
schema_validation_enabled BIT NOT NULL DEFAULT 0
schema_id BIGINT NULL
schema_validation_mode VARCHAR(20) NOT NULL DEFAULT 'STRICT'
```

### Migration Script
Run the migration script to add schema validation columns:
```bash
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -i scripts/migrate-schema-validation.sql
```

## Backend Implementation

### Spring Boot Web Application
- **ServiceConfiguration Entity**: Updated with schema validation fields
- **ServiceConfigurationService**: Enhanced to handle schema validation settings
- **FileSchemaService**: Added COBOL copybook validation support
- **Validation Modes**: Implemented STRICT, LENIENT, WARNING_ONLY modes

### Spring Boot Batch Application
- **FileProcessingService**: Integrated schema validation during file processing
- **SchemaValidationService**: REST client for calling web API validation
- **Conditional Validation**: Only validates if schema validation is enabled

### COBOL Copybook Validation
```java
private boolean validateCobolRecord(String record, String copybookDefinition) {
    // Validates COBOL record structure
    // Supports PIC clauses: X(n), 9(n), 9(n)V99
    // Validates field positions and formats
    // Returns true if record matches copybook definition
}
```

## Frontend Implementation

### Service Configuration Form
- **Schema Validation Toggle**: Enable/disable schema validation per service
- **Schema Selection**: Dropdown to select active schemas
- **Validation Mode Selection**: Choose between STRICT, LENIENT, WARNING_ONLY
- **Visual Indicators**: Chips showing validation status and mode

### Schema Management
- **COBOL Copybook Template**: Pre-built template for COBOL copybook schemas
- **Schema Type Support**: Added COBOL_COPYBOOK to schema type options
- **Field Definition**: Support for COBOL field types and picture clauses

## Configuration Examples

### Enable Schema Validation for a Service
```json
{
  "serviceName": "financial-service",
  "subServiceName": "transaction-processing",
  "schemaValidationEnabled": true,
  "schemaId": 1,
  "schemaValidationMode": "STRICT"
}
```

### COBOL Copybook Schema Definition
```json
{
  "tenantId": "tenant1",
  "serviceType": "mainframe-service",
  "schemaName": "Customer Record Schema",
  "schemaType": "COBOL_COPYBOOK",
  "schemaDefinition": {
    "type": "cobol_copybook",
    "recordLength": 80,
    "fields": [
      {"name": "CUSTOMER-ID", "level": "05", "type": "PIC", "picture": "X(10)", "start": 1, "length": 10, "required": true},
      {"name": "CUSTOMER-NAME", "level": "05", "type": "PIC", "picture": "X(30)", "start": 11, "length": 30, "required": true},
      {"name": "ACCOUNT-BALANCE", "level": "05", "type": "PIC", "picture": "9(10)V99", "start": 41, "length": 12, "required": true},
      {"name": "LAST-UPDATE-DATE", "level": "05", "type": "PIC", "picture": "9(8)", "start": 53, "length": 8, "required": true},
      {"name": "STATUS-CODE", "level": "05", "type": "PIC", "picture": "X(1)", "start": 61, "length": 1, "required": true}
    ]
  }
}
```

## API Endpoints

### Service Configuration with Schema Validation
```http
PUT /api/services/{id}
Content-Type: application/json

{
  "schemaValidationEnabled": true,
  "schemaId": 1,
  "schemaValidationMode": "STRICT"
}
```

### Schema Validation Response
```json
{
  "valid": true,
  "message": "Validation passed",
  "fileName": "customer.dat",
  "fileSize": 1024,
  "tenantId": "tenant1",
  "serviceType": "mainframe-service"
}
```

## Deployment Configuration

### Environment Variables
```yaml
# Schema Validation Settings
SCHEMA_VALIDATION_ENABLED: "true"
SCHEMA_VALIDATION_DEFAULT_MODE: "STRICT"
```

### Kubernetes ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: schema-validation-config
  namespace: file-transfer
data:
  SCHEMA_VALIDATION_ENABLED: "true"
  SCHEMA_VALIDATION_DEFAULT_MODE: "STRICT"
```

## Benefits

### For Organizations
- **Flexibility**: Choose which services need schema validation
- **Gradual Adoption**: Enable validation incrementally
- **Risk Management**: Different validation modes for different risk levels
- **Legacy Support**: Support for COBOL and mainframe systems

### For Developers
- **Optional Feature**: No impact on existing services
- **Backward Compatibility**: Existing configurations continue to work
- **Extensible Design**: Easy to add new validation types
- **Clear Configuration**: Explicit enable/disable per service

### For Operations
- **Service-Level Control**: Fine-grained control over validation
- **Monitoring**: Track validation success/failure rates
- **Troubleshooting**: Clear error messages for validation failures
- **Performance**: Only validate when needed

## Migration Path

### For Existing Deployments
1. **Run Migration Script**: Add schema validation columns
2. **Update Configuration**: Add schema validation environment variables
3. **Enable Per Service**: Gradually enable validation for specific services
4. **Monitor Performance**: Track validation impact on processing

### For New Deployments
1. **Install with Schema Support**: Deploy with schema validation enabled
2. **Configure Services**: Set up schema validation per service requirements
3. **Create Schemas**: Define schemas for different file types
4. **Test Validation**: Verify validation works correctly

## COBOL Copybook Support Details

### Supported COBOL Features
- **PIC Clauses**: X(n), 9(n), 9(n)V99, A(n), etc.
- **Record Length**: Fixed-length record validation
- **Field Positions**: Absolute and relative positioning
- **Level Structures**: COBOL level hierarchy support
- **Data Types**: Alphanumeric, numeric, decimal validation

### COBOL Validation Example
```cobol
01  CUSTOMER-RECORD.
    05  CUSTOMER-ID      PIC X(10).
    05  CUSTOMER-NAME    PIC X(30).
    05  ACCOUNT-BALANCE  PIC 9(10)V99.
    05  LAST-UPDATE-DATE PIC 9(8).
    05  STATUS-CODE      PIC X(1).
    05  FILLER           PIC X(19).
```

### Validation Rules
- **Record Length**: Must be exactly 80 characters
- **Field Formats**: Must match PIC clause specifications
- **Required Fields**: Non-null validation for required fields
- **Data Types**: Alphanumeric vs numeric validation

## Future Enhancements

### Planned COBOL Features
1. **Complex Structures**: Nested COBOL record structures
2. **Redefines**: COBOL REDEFINES clause support
3. **Occurs**: COBOL OCCURS clause for arrays
4. **Comp Fields**: COMP, COMP-3 field support
5. **Sign Handling**: Signed numeric field support

### General Schema Features
1. **Schema Versioning**: Enhanced version control
2. **Schema Migration**: Automatic schema updates
3. **Validation Analytics**: Advanced validation reporting
4. **Schema Discovery**: Automatic schema detection
5. **Performance Optimization**: Caching and optimization

This optional schema validation feature provides the flexibility needed for enterprise environments while maintaining backward compatibility and supporting legacy systems like COBOL.