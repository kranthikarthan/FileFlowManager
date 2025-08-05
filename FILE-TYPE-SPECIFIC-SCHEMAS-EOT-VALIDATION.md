# File Type Specific Schemas and EOT Validation

## Overview
This feature enables different schema layouts for SOT, data, and EOT files while maintaining the same schema type. It also implements EOT file validation to ensure data file count accuracy and generates alerts for mismatches without stopping service processing.

## Key Features

### 1. File Type Specific Schemas
- **SOT Schema**: Start-of-transmission file validation
- **Data Schema**: Data file validation (supports binary bypass)
- **EOT Schema**: End-of-transmission file validation
- **Same Schema Type**: All three can use the same schema type (CSV, JSON, XML, etc.)
- **Different Layouts**: Each file type can have different field structures

### 2. Binary File Bypass (Data Files Only)
- **Data Files Only**: Binary bypass only applies to data files
- **SOT/EOT Files**: Always validated as text files
- **Automatic Detection**: Binary files are automatically detected and bypassed
- **Configurable**: Can be enabled/disabled per service

### 3. EOT File Validation
- **File Count Tracking**: Automatically tracks processed data files
- **EOT Field Mapping**: Configurable field name for total files count
- **Mismatch Detection**: Compares expected vs actual file counts
- **Alert Generation**: Creates alerts for mismatches without stopping processing
- **Daily Reset**: File counts reset daily for accurate tracking

## Database Changes

### New Columns in service_configurations Table
```sql
-- File type specific schema IDs
sot_schema_id BIGINT NULL
data_schema_id BIGINT NULL
eot_schema_id BIGINT NULL

-- EOT file validation settings
eot_total_files_field VARCHAR(200) NULL
eot_validation_enabled BIT NOT NULL DEFAULT 0
```

### Migration Script
```bash
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -i scripts/migrate-schema-validation.sql
```

## Backend Implementation

### Spring Boot Web Application

#### File Type Specific Validation
```java
public ValidationResult validateFile(String tenantId, String serviceType, String fileName, 
                                   InputStream fileContent, Long fileSize, Boolean binaryFileBypass, String fileType) {
    // Determine which schema to use based on file type
    Long schemaId = determineSchemaId(tenantId, serviceType, fileType);
    
    // Only check binary bypass for data files
    if ("DATA".equals(fileType) && binaryFileBypass && isBinaryFile(fileContent, fileName)) {
        return new ValidationResult(true, "Binary file bypassed - no validation performed");
    }
    
    // Apply schema validation
    return validateWithSchema(schemaId, fileContent, fileName, fileSize);
}
```

#### EOT File Validation Service
```java
@Service
public class FileProcessingTrackingService {
    
    // Track data files for EOT validation
    public void trackDataFile(String tenantId, String serviceType, String fileName) {
        String key = generateKey(tenantId, serviceType);
        dailyFileCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    // Validate EOT file against expected count
    public EotValidationResult validateEotFile(String tenantId, String serviceType, 
                                             String eotContent, String totalFilesField) {
        int expectedCount = extractTotalFilesFromEot(eotContent, totalFilesField);
        int actualCount = getDailyFileCount(tenantId, serviceType);
        
        boolean isValid = expectedCount == actualCount;
        String message = isValid ? 
            "EOT file validation passed - file count matches" :
            String.format("EOT file mismatch: Expected %d files, but processed %d files", 
                         expectedCount, actualCount);
        
        if (!isValid) {
            generateMismatchAlert(tenantId, serviceType, expectedCount, actualCount);
        }
        
        return new EotValidationResult(isValid, message, expectedCount, actualCount);
    }
}
```

### Spring Boot Batch Application

#### File Type Detection and Processing
```java
private void processFile(ServiceConfiguration service, Path file, String fileType) {
    // Schema validation with file type
    if (service.getSchemaValidationEnabled()) {
        ValidationResult validationResult = schemaValidationService.validateFile(
            service.getTenantId(),
            service.getServiceName(),
            file.getFileName().toString(),
            Files.newInputStream(file),
            fileSize,
            service.getBinaryFileBypass(),
            fileType  // SOT, DATA, or EOT
        );
    }
    
    // Track data files for EOT validation
    if ("DATA".equals(fileType)) {
        trackDataFile(service.getTenantId(), service.getServiceName(), file.getFileName().toString());
    }
    
    // Special handling for EOT files
    if ("EOT".equals(fileType) && service.getEotValidationEnabled()) {
        ValidationResult eotValidationResult = validateEotFile(service, file);
        if (!eotValidationResult.isValid()) {
            // Generate alert but don't stop processing
            generateAlert(service, eotValidationResult);
        }
    }
}
```

## Frontend Implementation

### Service Configuration Form

#### File Type Specific Schema Selection
```javascript
// SOT Schema Selection
<FormControl fullWidth>
  <InputLabel>SOT Schema</InputLabel>
  <Select value={formData.sotSchemaId} onChange={handleSotSchemaChange}>
    {schemas.map(schema => (
      <MenuItem key={schema.id} value={schema.id}>
        {schema.schemaName} ({schema.schemaType})
      </MenuItem>
    ))}
  </Select>
</FormControl>

// Data Schema Selection (with binary bypass)
<FormControl fullWidth>
  <InputLabel>Data Schema</InputLabel>
  <Select value={formData.dataSchemaId} onChange={handleDataSchemaChange}>
    {schemas.map(schema => (
      <MenuItem key={schema.id} value={schema.id}>
        {schema.schemaName} ({schema.schemaType})
      </MenuItem>
    ))}
  </Select>
</FormControl>

// EOT Schema Selection
<FormControl fullWidth>
  <InputLabel>EOT Schema</InputLabel>
  <Select value={formData.eotSchemaId} onChange={handleEotSchemaChange}>
    {schemas.map(schema => (
      <MenuItem key={schema.id} value={schema.id}>
        {schema.schemaName} ({schema.schemaType})
      </MenuItem>
    ))}
  </Select>
</FormControl>
```

#### EOT Validation Configuration
```javascript
// EOT Validation Toggle
<FormControlLabel
  control={
    <Switch
      checked={formData.eotValidationEnabled}
      onChange={handleEotValidationToggle}
    />
  }
  label="Enable EOT File Validation"
/>

// Total Files Field Configuration
<TextField
  fullWidth
  label="Total Files Field Name"
  value={formData.eotTotalFilesField}
  onChange={handleTotalFilesFieldChange}
  placeholder="e.g., TOTAL_FILES, FILE_COUNT"
  helperText="Field name in EOT file containing total data files count"
/>
```

## Configuration Examples

### Service Configuration with File Type Specific Schemas
```json
{
  "serviceName": "document-processing",
  "subServiceName": "file-upload",
  "schemaValidationEnabled": true,
  "binaryFileBypass": true,
  "sotSchemaId": 1,
  "dataSchemaId": 2,
  "eotSchemaId": 3,
  "eotValidationEnabled": true,
  "eotTotalFilesField": "TOTAL_FILES"
}
```

### API Request with File Type
```http
POST /api/schemas/validate-file
Content-Type: multipart/form-data

tenantId: tenant1
serviceType: document-service
fileType: DATA
binaryFileBypass: true
file: [file data]
```

### EOT Validation API
```http
POST /api/schemas/validate-eot
Content-Type: application/x-www-form-urlencoded

tenantId: tenant1
serviceType: document-service
eotContent: TOTAL_FILES,PROCESS_DATE
150,2024-01-15
totalFilesField: TOTAL_FILES
```

## File Processing Flow

### 1. SOT File Processing
```
SOT File Received → SOT Schema Validation → Process → Track Start
```

### 2. Data File Processing
```
Data File Received → Check Binary → Binary Bypass (if enabled) → Data Schema Validation → Process → Track Count
```

### 3. EOT File Processing
```
EOT File Received → EOT Schema Validation → Extract Expected Count → Compare with Actual Count → Generate Alert (if mismatch) → Process
```

## Alert System

### EOT Validation Mismatch Alert
```json
{
  "alertType": "EOT_VALIDATION_MISMATCH",
  "severity": "WARNING",
  "message": "EOT Validation Mismatch - Service: document-service, Tenant: tenant1, Expected: 150, Actual: 145, Difference: 5",
  "tenantId": "tenant1",
  "serviceType": "document-service",
  "createdAt": "2024-01-15T10:30:45"
}
```

### Alert Handling
- **Non-Blocking**: Alerts don't stop file processing
- **Immediate Notification**: Real-time alert generation
- **Escalation**: Can trigger escalation procedures
- **Dashboard Integration**: Alerts appear in monitoring dashboard

## Use Cases

### Document Processing Service
- **SOT Files**: CSV with header information
- **Data Files**: Mixed binary (PDFs, images) and text files
- **EOT Files**: CSV with summary and file counts
- **Binary Bypass**: Enabled for data files
- **EOT Validation**: Enabled with "TOTAL_FILES" field

### Financial Data Service
- **SOT Files**: JSON with batch metadata
- **Data Files**: Fixed-width text files
- **EOT Files**: JSON with transaction counts
- **Binary Bypass**: Disabled (all files are text)
- **EOT Validation**: Enabled with "TRANSACTION_COUNT" field

### Media Processing Service
- **SOT Files**: XML with processing parameters
- **Data Files**: Binary media files (images, videos)
- **EOT Files**: XML with processing summary
- **Binary Bypass**: Enabled for data files
- **EOT Validation**: Enabled with "PROCESSED_FILES" field

## Benefits

### For Operations
- **Flexible Schema Management**: Different layouts for different file types
- **Binary File Handling**: Efficient processing of mixed file types
- **Data Integrity**: EOT validation ensures file count accuracy
- **Non-Blocking Alerts**: Mismatches don't stop processing
- **Clear Tracking**: Daily file count tracking per service

### For Development
- **Type-Specific Validation**: Appropriate validation per file type
- **Configurable Fields**: Flexible EOT field mapping
- **Extensible Design**: Easy to add new file types
- **Clear APIs**: Explicit file type parameters

### For Monitoring
- **Real-time Alerts**: Immediate notification of mismatches
- **Detailed Tracking**: Per-service, per-day file counts
- **Audit Trail**: Complete processing history
- **Performance Metrics**: Processing efficiency tracking

## Migration Path

### For Existing Deployments
1. **Run Migration**: Execute schema validation migration script
2. **Configure Schemas**: Set up file type specific schemas
3. **Enable EOT Validation**: Configure EOT validation per service
4. **Test Scenarios**: Verify SOT, data, and EOT file processing
5. **Monitor Alerts**: Set up alert monitoring and escalation

### For New Deployments
1. **Deploy with Support**: Install with file type specific schema support
2. **Create Schemas**: Define schemas for each file type
3. **Configure Services**: Set up service configurations
4. **Test Processing**: Verify end-to-end file processing
5. **Set Up Monitoring**: Configure alerting and monitoring

## Configuration Options

### Environment Variables
```yaml
# File type specific schema settings
FILE_TYPE_SCHEMAS_ENABLED: "true"
EOT_VALIDATION_ENABLED: "true"
BINARY_BYPASS_DATA_ONLY: "true"
```

### Kubernetes ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: file-type-schemas-config
  namespace: file-transfer
data:
  FILE_TYPE_SCHEMAS_ENABLED: "true"
  EOT_VALIDATION_ENABLED: "true"
  BINARY_BYPASS_DATA_ONLY: "true"
```

### Service Configuration
```json
{
  "sotSchemaId": 1,
  "dataSchemaId": 2,
  "eotSchemaId": 3,
  "binaryFileBypass": true,
  "eotValidationEnabled": true,
  "eotTotalFilesField": "TOTAL_FILES"
}
```

## Monitoring and Logging

### Processing Logs
```
2024-01-15 10:30:45 INFO  - SOT file processed: start_batch.csv
2024-01-15 10:30:46 INFO  - Data file processed: document1.pdf (binary bypassed)
2024-01-15 10:30:47 INFO  - Data file processed: data1.csv
2024-01-15 10:30:48 INFO  - EOT file processed: end_batch.csv (validation passed)
```

### Alert Logs
```
2024-01-15 10:30:48 WARN  - EOT Validation Mismatch: Expected 150, Actual 145
2024-01-15 10:30:48 INFO  - Alert generated: EOT_VALIDATION_MISMATCH
```

### Performance Metrics
- **File Type Processing Rates**: SOT, data, EOT processing speeds
- **Binary Bypass Efficiency**: Time saved by bypassing binary files
- **EOT Validation Accuracy**: Mismatch detection rates
- **Alert Response Times**: Time from mismatch to alert

## Future Enhancements

### Planned Features
1. **Custom File Types**: User-defined file type schemas
2. **Advanced EOT Parsing**: Support for complex EOT file formats
3. **Predictive Alerts**: Alert before EOT file arrives
4. **Batch Processing**: Group file processing for efficiency
5. **Schema Versioning**: Version control for schemas

### Advanced Validation
1. **Cross-File Validation**: Validate relationships between SOT, data, and EOT files
2. **Content Validation**: Validate file content beyond structure
3. **Business Rules**: Custom business rule validation
4. **Data Quality**: Data quality assessment and scoring
5. **Compliance Checking**: Regulatory compliance validation

This file type specific schema and EOT validation feature provides comprehensive file processing capabilities with flexible validation, efficient binary handling, and robust data integrity checking.