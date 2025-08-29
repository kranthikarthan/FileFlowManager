# Schema Reuse and EOT Validation Guide

## Overview

This document describes the new **Schema Reuse** and **EOT (End-of-Day) Validation** functionality that has been added to the File Transfer Management System. These features provide:

1. **Shared Schema Library**: Ability to create and reuse schemas across multiple subservices and directions
2. **EOT File Count Validation**: Automatic validation of data file counts against values specified in EOT files

## Table of Contents

- [Schema Reuse Functionality](#schema-reuse-functionality)
- [EOT Validation System](#eot-validation-system)
- [Frontend Components](#frontend-components)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Schema Reuse Functionality

### Overview

The schema reuse system allows you to:
- Create shared schemas that can be used across multiple subservices
- Version control your schemas
- Tag and categorize schemas for easy discovery
- Clone schemas between tenants
- Track usage statistics

### Key Components

#### SharedSchema Entity
- **Schema Name**: Unique identifier within a tenant
- **Version**: Semantic versioning (e.g., "1.0", "2.1")
- **Schema Type**: JSON_SCHEMA, XML_SCHEMA, COBOL_COPYBOOK, etc.
- **File Type**: JSON, XML, CSV, COBOL_FLAT_FILE, etc.
- **Global Flag**: Allows sharing across all tenants
- **Count Validation Support**: Enables EOT validation features
- **Tags**: Categorization and search capabilities

#### SchemaUsageMapping Entity
- Links subservices to shared schemas
- Supports multiple schemas per subservice/direction
- Tracks primary vs. secondary schemas
- Records usage statistics

### Usage Examples

#### Creating a Shared Schema

```javascript
// Frontend: SharedSchemaManagement component
const newSchema = {
  schemaName: "Payment_Transaction_v2",
  schemaVersion: "2.0",
  schemaType: "JSON_SCHEMA",
  fileType: "JSON",
  schemaDefinition: `{
    "type": "object",
    "properties": {
      "header": {
        "type": "object",
        "properties": {
          "recordCount": {"type": "integer"}
        }
      },
      "transactions": {
        "type": "array",
        "items": {"$ref": "#/definitions/transaction"}
      }
    }
  }`,
  supportsCountValidation: true,
  eotCountFieldPath: "header.recordCount",
  tags: ["payment", "transaction", "v2"]
};
```

#### Reusing Schema Across Subservices

```java
// Backend: Create usage mappings
SchemaUsageMapping mapping = new SchemaUsageMapping(
    subServiceConfiguration,
    sharedSchema,
    TransferDirection.INBOUND,
    "admin"
);
mapping.setIsPrimary(true);
mapping.setValidationEnabled(true);
```

## EOT Validation System

### Overview

The EOT validation system automatically tracks daily file counts and validates them against counts specified in End-of-Day files. This ensures data integrity and helps identify missing or duplicate files.

### Key Components

#### DailyFileCountTracker Entity
- Tracks file counts per subservice per day
- Records SOT (Start-of-Day) and EOT file receipt
- Validates actual vs. expected counts
- Maintains validation status and messages

#### Validation Statuses
- **MATCHED**: Counts match perfectly
- **DISCREPANCY**: Count mismatch detected
- **PENDING**: Waiting for EOT file
- **MISSING_EOT**: EOT file not received
- **MISSING_SOT**: SOT file not received
- **ERROR**: Validation error occurred

### EOT Field Path Configuration

The system supports multiple field path formats:

#### JSON (JSONPath)
```
header.recordCount
summary.totalFiles
data.fileCount
```

#### XML (XPath)
```
/eot/recordCount
/summary/header/fileCount
//recordCount
```

#### CSV (Column Names)
```
recordCount
file_count
total_records
```

#### Fixed Width/Text
```
pos:10-15          # Position 10-15
line:2,pos:5-10    # Line 2, position 5-10
regex:Count:(\\d+) # Regex pattern
```

#### COBOL (Field Names)
```
RECORD-COUNT
FILE-TOTAL
TRANSACTION-COUNT
```

### Integration with File Processing

The system automatically integrates with the file monitoring pipeline:

```java
// FileMonitoringService integration
private void recordFileForCountTracking(SubServiceConfiguration config, 
                                      String fileName, FileType fileType, 
                                      LocalDate processingDate, 
                                      FileNamingConventionService.FileNameType nameType, 
                                      byte[] content) {
    switch (nameType) {
        case SOT:
            eotValidationService.recordSotFile(/* parameters */);
            break;
        case EOT:
            eotValidationService.processEotFile(/* parameters */);
            break;
        case DATA:
            eotValidationService.recordDataFile(/* parameters */);
            break;
    }
}
```

## Frontend Components

### SharedSchemaManagement Component

**Features:**
- Create, edit, delete shared schemas
- Search and filter schemas
- Tag management
- Version control
- Clone schemas
- Usage statistics

**Navigation:** `/shared-schemas`

### EOT Validation Dashboard

**Features:**
- Validation statistics overview
- Real-time count tracking
- Discrepancy alerts
- Historical reports
- Trend analysis

**Navigation:** `/eot-validation`

## API Endpoints

### Shared Schema API

```
GET    /api/shared-schemas/tenant/{tenantId}                    # Get all schemas
GET    /api/shared-schemas/tenant/{tenantId}/file-type/{type}   # Filter by file type
GET    /api/shared-schemas/tenant/{tenantId}/count-validation   # Count validation schemas
POST   /api/shared-schemas                                      # Create schema
PUT    /api/shared-schemas/{id}                                 # Update schema
DELETE /api/shared-schemas/{id}                                 # Delete schema
POST   /api/shared-schemas/{id}/version                         # Create new version
POST   /api/shared-schemas/{id}/clone                           # Clone schema
```

### EOT Validation API

```
GET    /api/eot-validation/tenant/{tenantId}/results           # Get validation results
GET    /api/eot-validation/tenant/{tenantId}/pending           # Get pending validations
GET    /api/eot-validation/tenant/{tenantId}/discrepancies     # Get discrepancies
GET    /api/eot-validation/tenant/{tenantId}/statistics        # Get statistics
GET    /api/eot-validation/tenant/{tenantId}/dashboard         # Dashboard data
GET    /api/eot-validation/tenant/{tenantId}/export            # Export report
```

## Configuration

### Database Schema

Run the migration script to create required tables:

```sql
-- Execute: /workspace/scripts/schema-reuse-migrations.sql
```

### Application Properties

```properties
# Enable EOT validation
file-transfer.eot-validation.enabled=true

# Count tracking configuration
file-transfer.eot-validation.alert-threshold-hours=24
file-transfer.eot-validation.cleanup-days=90

# Schema reuse configuration
file-transfer.schema-reuse.cache-ttl=3600
file-transfer.schema-reuse.max-versions=10
```

### Subservice Configuration

Update subservice configurations to include:

```java
subServiceConfig.setStartMarkerPrefix("SOT_");  // SOT file prefix
subServiceConfig.setEndMarkerPrefix("EOT_");    // EOT file prefix
```

## Best Practices

### Schema Design

1. **Use Semantic Versioning**: Version schemas properly (1.0, 1.1, 2.0)
2. **Add Descriptions**: Document schema purpose and usage
3. **Tag Appropriately**: Use consistent tagging for discoverability
4. **Global Schemas**: Make commonly used schemas global
5. **Count Validation**: Configure EOT field paths for critical data flows

### EOT Validation Setup

1. **Field Path Testing**: Test EOT field paths thoroughly
2. **Naming Conventions**: Use consistent SOT/EOT file naming
3. **Monitoring**: Set up alerts for discrepancies
4. **Regular Review**: Monitor validation statistics regularly

### Performance Optimization

1. **Caching**: Leverage built-in caching for frequently used schemas
2. **Indexing**: Database indexes are optimized for common queries
3. **Cleanup**: Regularly clean up old tracking records
4. **Batch Processing**: Process EOT files during low-traffic periods

## Troubleshooting

### Common Issues

#### Schema Validation Errors

**Problem**: Schema validation fails
**Solution**: 
1. Check schema definition syntax
2. Verify file type compatibility
3. Review validation logs

#### Count Discrepancies

**Problem**: EOT count doesn't match actual files
**Solution**:
1. Verify EOT field path configuration
2. Check file naming conventions
3. Review file processing logs
4. Validate EOT file format

#### Performance Issues

**Problem**: Slow schema operations
**Solution**:
1. Check database indexes
2. Review cache configuration
3. Optimize database queries
4. Monitor system resources

### Debugging

#### Enable Debug Logging

```properties
logging.level.com.filetransfer.web.service.SharedSchemaService=DEBUG
logging.level.com.filetransfer.web.service.EotValidationService=DEBUG
```

#### Common Log Messages

```
INFO  - Created new shared schema: Payment_Schema for tenant: ACME_CORP
DEBUG - Loading available schemas for tenant: ACME_CORP
WARN  - COUNT DISCREPANCY: PAYMENT_SERVICE/INBOUND on 2024-01-15 - EOT says 100, actual 98
ERROR - Error extracting count from JSON file eot_20240115.json: Field path not found
```

### Database Queries for Troubleshooting

```sql
-- Check schema usage
SELECT ss.schema_name, COUNT(sum.id) as usage_count
FROM shared_schemas ss
LEFT JOIN schema_usage_mappings sum ON ss.id = sum.shared_schema_id
GROUP BY ss.id, ss.schema_name;

-- Check validation discrepancies
SELECT * FROM daily_file_count_tracker
WHERE validation_status = 'DISCREPANCY'
AND processing_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY);

-- Check pending validations
SELECT * FROM daily_file_count_tracker
WHERE validation_status = 'PENDING'
AND processing_date < CURRENT_DATE;
```

## Migration Guide

### From Existing Schema System

1. **Backup Data**: Export existing schemas
2. **Run Migration**: Execute database migration scripts
3. **Import Schemas**: Convert existing schemas to shared schemas
4. **Update Configurations**: Link subservices to shared schemas
5. **Test Validation**: Verify EOT validation works correctly

### Rollback Plan

If issues occur:
1. **Disable Features**: Set enabled=false in configuration
2. **Restore Backup**: Restore previous schema configurations
3. **Remove Tables**: Drop new tables if necessary
4. **Restart Services**: Restart application services

## Support

For additional support:
1. Check application logs
2. Review database status
3. Monitor system metrics
4. Contact system administrators

---

**Last Updated**: January 2024
**Version**: 1.0