# File Transfer Management API Documentation

## Overview

The File Transfer Management API provides RESTful endpoints for managing and monitoring file transfers across different services. All endpoints return JSON responses and use standard HTTP status codes.

## Base URL

```
http://localhost:8080/api
```

## Authentication

Currently, the API does not require authentication. In a production environment, you should implement appropriate authentication and authorization mechanisms.

## Common Response Formats

### Success Response
```json
{
  "data": [...],
  "status": "success"
}
```

### Error Response
```json
{
  "error": "Error message",
  "status": "error",
  "timestamp": "2023-12-07T10:30:00Z"
}
```

## Endpoints

### File Transfers

#### Get All File Transfers
```
GET /file-transfers
```

**Description**: Retrieves all file transfer records.

**Response**:
```json
[
  {
    "id": 1,
    "fileName": "data_001.dat",
    "serviceType": "service1",
    "sourcePath": "/app/data/inbound/service1/data_001.dat",
    "targetPath": "/app/data/outbound/service1/data_001.dat",
    "status": "COMPLETED",
    "direction": "INBOUND",
    "errorMessage": null,
    "createdAt": "2023-12-07T10:00:00Z",
    "processedAt": "2023-12-07T10:01:00Z",
    "fileSize": 1024,
    "checksum": "d41d8cd98f00b204e9800998ecf8427e",
    "batchJobExecutionId": "job_123"
  }
]
```

#### Get File Transfer by ID
```
GET /file-transfers/{id}
```

**Parameters**:
- `id` (path): File transfer record ID

**Response**: Single file transfer object (same structure as above)

#### Get File Transfers by Service
```
GET /file-transfers/service/{serviceType}
```

**Parameters**:
- `serviceType` (path): Service identifier (e.g., "service1", "service2")

**Response**: Array of file transfer objects for the specified service

#### Get File Transfers by Status
```
GET /file-transfers/status/{status}
```

**Parameters**:
- `status` (path): Transfer status

**Valid Statuses**:
- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`
- `FAILED`
- `CANCELLED`
- `WAITING_FOR_END_MARKER`

**Response**: Array of file transfer objects with the specified status

#### Get File Transfers by Direction
```
GET /file-transfers/direction/{direction}
```

**Parameters**:
- `direction` (path): Transfer direction

**Valid Directions**:
- `INBOUND`
- `OUTBOUND`

**Response**: Array of file transfer objects with the specified direction

#### Get File Transfers by Service and Status
```
GET /file-transfers/service/{serviceType}/status/{status}
```

**Parameters**:
- `serviceType` (path): Service identifier
- `status` (path): Transfer status

**Response**: Array of file transfer objects matching both criteria

#### Get File Transfers by Date Range
```
GET /file-transfers/date-range?startDate={startDate}&endDate={endDate}
```

**Parameters**:
- `startDate` (query): Start date in ISO format (e.g., "2023-12-01T00:00:00Z")
- `endDate` (query): End date in ISO format

**Response**: Array of file transfer objects within the date range

#### Get File Transfers by Service and Date Range
```
GET /file-transfers/service/{serviceType}/date-range?startDate={startDate}&endDate={endDate}
```

**Parameters**:
- `serviceType` (path): Service identifier
- `startDate` (query): Start date in ISO format
- `endDate` (query): End date in ISO format

**Response**: Array of file transfer objects matching service and date criteria

### Transfer Operations

#### Retry Transfer
```
POST /file-transfers/{id}/retry
```

**Description**: Retries a failed or cancelled transfer.

**Parameters**:
- `id` (path): File transfer record ID

**Valid States for Retry**:
- `FAILED`
- `CANCELLED`

**Response**:
```json
"Transfer retry initiated successfully"
```

**Error Cases**:
- Transfer not found: `404 Not Found`
- Invalid status for retry: `400 Bad Request`

#### Cancel Transfer
```
POST /file-transfers/{id}/cancel
```

**Description**: Cancels a pending or failed transfer.

**Parameters**:
- `id` (path): File transfer record ID

**Valid States for Cancel**:
- `PENDING`
- `FAILED`

**Response**:
```json
"Transfer cancelled successfully"
```

**Error Cases**:
- Transfer not found: `404 Not Found`
- Invalid status for cancel: `400 Bad Request`

### Service Management

#### Get All Service Types
```
GET /file-transfers/services
```

**Description**: Retrieves all distinct service types that have file transfers.

**Response**:
```json
["service1", "service2", "service3"]
```

## Enhanced Cut-Off Time Management

### Get Cut-Off Time for Service
```
GET /api/services/{id}/cutoff-time/{date}
```

**Parameters**:
- `id` (path): Service configuration ID
- `date` (path): Date in YYYY-MM-DD format

**Response**:
```json
{
  "serviceId": 1,
  "date": "2024-01-15",
  "cutOffTime": "18:00:00",
  "cutOffTimeType": "WEEKDAY_WEEKEND",
  "isHoliday": false,
  "isSunday": false
}
```

### Check Cut-Off Time for Service
```
POST /api/services/{id}/check-cutoff
```

**Request Body**:
```json
{
  "date": "2024-01-15",
  "time": "17:30:00"
}
```

**Response**:
```json
{
  "serviceId": 1,
  "date": "2024-01-15",
  "time": "17:30:00",
  "cutOffTime": "18:00:00",
  "isBeforeCutOff": true,
  "isHoliday": false,
  "isSunday": false
}
```

### Get Cut-Off Time by Tenant and Service Name
```
GET /api/services/tenant/{tenantId}/service/{serviceName}/cutoff-time/{date}
```

**Parameters**:
- `tenantId` (path): Tenant identifier
- `serviceName` (path): Service name
- `date` (path): Date in YYYY-MM-DD format

## Holiday Management

### Create Sunday Holidays for a Year
```
POST /api/holidays/tenant/{tenantId}/create-sunday-holidays/{year}?holidayName=Sunday Holiday
```

**Parameters**:
- `tenantId` (path): Tenant identifier
- `year` (path): Year to create Sunday holidays for
- `holidayName` (query): Name for the Sunday holidays

### Create Sunday Holidays for Date Range
```
POST /api/holidays/tenant/{tenantId}/create-sunday-holidays-range?startDate=2024-01-01&endDate=2024-12-31&holidayName=Sunday Holiday
```

**Parameters**:
- `tenantId` (path): Tenant identifier
- `startDate` (query): Start date in YYYY-MM-DD format
- `endDate` (query): End date in YYYY-MM-DD format
- `holidayName` (query): Name for the Sunday holidays

### Remove Sunday Holidays for a Year
```
DELETE /api/holidays/tenant/{tenantId}/remove-sunday-holidays/{year}
```

**Parameters**:
- `tenantId` (path): Tenant identifier
- `year` (path): Year to remove Sunday holidays for

### Check if Date is Holiday or Sunday
```
GET /api/holidays/tenant/{tenantId}/is-holiday-or-sunday/{date}?allSundaysAsHolidays=true
```

**Parameters**:
- `tenantId` (path): Tenant identifier
- `date` (path): Date in YYYY-MM-DD format
- `allSundaysAsHolidays` (query): Whether to treat all Sundays as holidays

**Response**:
```json
{
  "date": "2024-01-14",
  "isHoliday": true,
  "isSunday": true,
  "holidayName": "Sunday Holiday"
}
```

## Schema Management

### Create File Schema
```
POST /api/schemas?createdBy=admin
```

**Request Body**:
```json
{
  "tenantId": "tenant1",
  "serviceType": "service1",
  "schemaName": "CSV Data Schema",
  "schemaVersion": "1.0",
  "schemaType": "CSV",
  "schemaDefinition": "{\"type\":\"csv\",\"delimiter\":\",\",\"hasHeader\":true,\"fields\":[{\"name\":\"transaction_id\",\"type\":\"STRING\",\"required\":true,\"length\":50}]}",
  "description": "Standard CSV schema for financial transactions",
  "isActive": true
}
```

### Update File Schema
```
PUT /api/schemas/{schemaId}?updatedBy=admin
```

### Delete File Schema
```
DELETE /api/schemas/{schemaId}
```

### Get Schema by ID
```
GET /api/schemas/{schemaId}
```

### Get Schemas by Service Type
```
GET /api/schemas/tenant/{tenantId}/service/{serviceType}
```

### Get All Schemas for Tenant
```
GET /api/schemas/tenant/{tenantId}
```

### Add Validation Rule to Schema
```
POST /api/schemas/{schemaId}/validation-rules
```

**Request Body**:
```json
{
  "ruleName": "File Size Check",
  "ruleType": "CUSTOM",
  "ruleDefinition": "fileSize <= 10485760",
  "ruleOrder": 1,
  "isActive": true,
  "errorMessage": "File size exceeds maximum allowed size of 10MB"
}
```

### Add Field to Schema
```
POST /api/schemas/{schemaId}/fields
```

**Request Body**:
```json
{
  "fieldName": "transaction_id",
  "fieldType": "STRING",
  "fieldLength": 50,
  "isRequired": true,
  "isUnique": false,
  "defaultValue": "",
  "validationRegex": "^[A-Z0-9]+$",
  "fieldOrder": 1,
  "description": "Unique transaction identifier"
}
```

### Validate File Against Schema
```
POST /api/schemas/validate-file
```

**Parameters**:
- `tenantId` (form): Tenant identifier
- `serviceType` (form): Service type
- `file` (form): File to validate

**Response**:
```json
{
  "valid": true,
  "message": "Validation passed",
  "fileName": "data.csv",
  "fileSize": 1024,
  "tenantId": "tenant1",
  "serviceType": "service1"
}
```

### Get Schema Templates
```
GET /api/schemas/templates
```

**Response**:
```json
{
  "CSV": {
    "type": "CSV",
    "description": "Comma-separated values file schema",
    "example": {
      "delimiter": ",",
      "hasHeader": true,
      "fields": [
        {"name": "field1", "type": "STRING", "required": true},
        {"name": "field2", "type": "INTEGER", "required": false}
      ]
    }
  },
  "JSON": {
    "type": "JSON",
    "description": "JSON file schema",
    "example": {
      "type": "object",
      "properties": {
        "id": {"type": "string"},
        "name": {"type": "string"},
        "value": {"type": "number"}
      },
      "required": ["id", "name"]
    }
  },
  "COBOL_COPYBOOK": {
    "type": "COBOL_COPYBOOK",
    "description": "COBOL copybook file schema",
    "example": {
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
}
```

## Service Configuration
```
PUT /api/services/{id}
```

**Request Body**:
```json
{
  "serviceName": "business-service",
  "tenantId": "tenant1",
  "cutOffTimeType": "WEEKDAY_WEEKEND",
  "cutOffTime": "18:00:00",
  "weekdayCutOffTime": "18:00:00",
  "weekendCutOffTime": "12:00:00",
  "allSundaysAsHolidays": false,
  "enabled": true
}
```

**Enhanced Configuration Types**:
- `DAILY`: Single cut-off time for all days
- `WEEKDAY_WEEKEND`: Different times for weekdays and weekends
- `PER_DAY`: Individual times for each day of the week

## Data Models

### FileTransferRecord

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique identifier |
| fileName | String | Name of the transferred file |
| serviceType | String | Service that processed the file |
| sourcePath | String | Source file path |
| targetPath | String | Destination file path |
| status | Enum | Current transfer status |
| direction | Enum | Transfer direction (INBOUND/OUTBOUND) |
| errorMessage | String | Error message if transfer failed |
| createdAt | DateTime | When the transfer record was created |
| processedAt | DateTime | When the transfer was processed |
| fileSize | Long | File size in bytes |
| checksum | String | File checksum (MD5) |
| batchJobExecutionId | String | Associated batch job ID |

### Status Enum Values

- **PENDING**: Transfer is queued for processing
- **IN_PROGRESS**: Transfer is currently being processed
- **COMPLETED**: Transfer completed successfully
- **FAILED**: Transfer failed and can be retried
- **CANCELLED**: Transfer was cancelled by user
- **WAITING_FOR_END_MARKER**: Waiting for end-of-transmission marker

### Direction Enum Values

- **INBOUND**: File received from external source
- **OUTBOUND**: File sent to external destination

## Error Handling

### HTTP Status Codes

- `200 OK`: Request successful
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

### Error Response Format

```json
{
  "error": "Detailed error message",
  "status": "error",
  "timestamp": "2023-12-07T10:30:00Z",
  "path": "/api/file-transfers/999"
}
```

## Rate Limiting

Currently, no rate limiting is implemented. In production, consider implementing rate limiting to prevent abuse.

## CORS

The API includes CORS configuration to allow requests from `http://localhost:3000` (React frontend). Update this configuration for your production frontend URL.

## Health Checks

### Application Health
```
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "Azure SQL MI",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### Application Info
```
GET /actuator/info
```

### Metrics
```
GET /actuator/metrics
```

## Examples

### Getting Recent Failed Transfers

```bash
curl -X GET "http://localhost:8080/api/file-transfers/status/FAILED"
```

### Retrying a Failed Transfer

```bash
curl -X POST "http://localhost:8080/api/file-transfers/123/retry"
```

### Getting Transfers from Last 24 Hours

```bash
START_DATE=$(date -u -d '1 day ago' '+%Y-%m-%dT%H:%M:%SZ')
END_DATE=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

curl -X GET "http://localhost:8080/api/file-transfers/date-range?startDate=${START_DATE}&endDate=${END_DATE}"
```

### Getting Service Statistics

```bash
# Get all service types
curl -X GET "http://localhost:8080/api/file-transfers/services"

# Get completed transfers for service1
curl -X GET "http://localhost:8080/api/file-transfers/service/service1/status/COMPLETED"
```

## SDK Examples

### JavaScript/React

```javascript
// Get all transfers
const response = await fetch('/api/file-transfers');
const transfers = await response.json();

// Retry a transfer
const retryResponse = await fetch(`/api/file-transfers/${transferId}/retry`, {
  method: 'POST'
});

// Filter by service and status
const filteredResponse = await fetch(`/api/file-transfers/service/service1/status/FAILED`);
const failedTransfers = await filteredResponse.json();
```

### Java

```java
// Using RestTemplate
RestTemplate restTemplate = new RestTemplate();

// Get all transfers
FileTransferRecord[] transfers = restTemplate.getForObject(
    "/api/file-transfers", 
    FileTransferRecord[].class
);

// Retry transfer
restTemplate.postForObject(
    "/api/file-transfers/{id}/retry", 
    null, 
    String.class, 
    transferId
);
```

## Changelog

### Version 1.0.0
- Initial API release
- Basic CRUD operations for file transfers
- Service management endpoints
- Transfer retry/cancel functionality