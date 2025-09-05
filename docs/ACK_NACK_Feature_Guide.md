# ACK/NACK Feature Guide

## Quick Start

### 1. Enable ACK/NACK Processing

Add the following environment variables:
```bash
ACK_NACK_ENABLED=true
ACK_NACK_BASE_PATH=/data/ack-nack
ACK_NACK_INCOMING_PATH=/data/incoming/ack-nack
ACK_NACK_AUTO_GENERATE=true
```

### 2. Database Setup

Run the migration scripts:
- `V100__Add_ACK_NACK_Tables.sql` (MySQL)
- `sqlserver/V100__Add_ACK_NACK_Tables.sql` (SQL Server)

### 3. Directory Structure Setup

Create the required directories:
```bash
mkdir -p /data/ack-nack
mkdir -p /data/incoming/ack-nack
mkdir -p /data/incoming/ack-nack/processed
mkdir -p /data/incoming/ack-nack/error
```

## Using the Feature

### Web Interface

1. **Navigate to ACK/NACK Management**
   - Go to the "ACK/NACK" tab in the web interface
   - View statistics dashboard
   - Filter records by status or type

2. **Manual ACK/NACK Generation**
   - From File Transfer List: Click ACK/NACK icons for completed/failed inbound files
   - From ACK/NACK Management: Use generate buttons

3. **Upload Received ACK/NACK Files**
   - Use the "Upload ACK/NACK" button
   - Select .ack or .nack files received from partners
   - Files will be automatically processed

### API Usage

#### Generate ACK for completed inbound file:
```bash
POST /api/v1/ack-nack/generate-ack/{fileTransferId}
```

#### Generate NACK for failed inbound file:
```bash
POST /api/v1/ack-nack/generate-nack/{fileTransferId}
Content-Type: application/json

{
  "reasonCode": "VALIDATION_FAILED",
  "reasonDescription": "File format validation failed"
}
```

#### Upload received ACK/NACK file:
```bash
POST /api/v1/ack-nack/{tenantId}/upload
Content-Type: multipart/form-data

file: [ACK/NACK file]
```

### Batch Operations

#### Manual job triggers:
```bash
# Process incoming ACK/NACK files
POST /api/v1/batch/ack-nack/process-files

# Generate ACK files for completed transfers
POST /api/v1/batch/ack-nack/generate-acks/{tenantId}

# Generate NACK files for failed transfers  
POST /api/v1/batch/ack-nack/generate-nacks/{tenantId}
```

## File Formats

### ACK File Example:
```
ACK|customer_data_20231201.dat|CUSTOMER_SERVICE|DAILY_BATCH|20231201143022|SUCCESS|1024576|a1b2c3d4e5f6
```

### NACK File Example:
```
NACK|customer_data_20231201.dat|CUSTOMER_SERVICE|DAILY_BATCH|20231201143022|FAILED|VALIDATION_ERROR|Invalid record format in line 150
```

## Configuration

### Per-Service Configuration

ACK/NACK behavior can be configured per service via the `ack_nack_configuration` table:

- `auto_generate_ack` - Automatically generate ACK files
- `auto_generate_nack` - Automatically generate NACK files  
- `auto_send_ack_nack` - Automatically send generated files
- `ack_nack_timeout_hours` - Timeout for ACK/NACK responses
- `partner_ack_path` - Partner directory for ACK files
- `partner_nack_path` - Partner directory for NACK files

### Global Configuration

Set in `application-ack-nack.yml` or environment variables:

- Processing intervals
- File size limits
- Retry settings
- Directory paths
- Security options

## Monitoring

### Statistics Available:
- Total ACK/NACK records
- Count by type (ACK vs NACK)
- Count by status (Pending, Sent, Failed, etc.)
- Average response times
- Daily metrics

### Health Checks:
- Pending file count monitoring
- Failed operation thresholds
- Directory accessibility
- Partner connectivity

## Troubleshooting

### Common Issues:

1. **ACK/NACK not generated automatically**
   - Check `auto_generate_ack/nack` configuration
   - Verify file transfer completed successfully
   - Check logs for errors

2. **Files not being sent to partners**
   - Verify partner paths are configured
   - Check directory permissions
   - Review connection settings

3. **Incoming files not processed**
   - Check incoming directory structure
   - Verify file format matches expected pattern
   - Check batch job is running

### Log Locations:
- Web app: `/app/logs/file-transfer-web.log`
- Batch app: `/app/logs/file-transfer-batch.log`

### Debug Mode:
Set `ACK_NACK_LOG_LEVEL=DEBUG` for detailed logging.

## File Lifecycle

### Outbound Data Files (Our system → Partner):
1. Data/SOT/EOT file sent to partner
2. Partner processes file
3. Partner sends ACK/NACK to our incoming directory
4. Batch job processes incoming ACK/NACK
5. Original file transfer record updated with status

### Inbound Data Files (Partner → Our system):
1. Data/SOT/EOT file received from partner
2. Our system processes file
3. ACK/NACK automatically generated based on result
4. ACK/NACK sent to partner's directory
5. Status tracked in database

## Integration with Existing Features

- **SOT/EOT Validation**: ACK/NACK generated after SOT/EOT validation completes
- **Schema Validation**: NACK includes schema validation errors
- **File Type Detection**: ACK/NACK handling works with all supported file types
- **Multi-tenant**: Full tenant isolation for ACK/NACK operations
- **Service Configuration**: Leverages existing service configuration framework
- **Monitoring**: Integrates with existing alerting and monitoring systems