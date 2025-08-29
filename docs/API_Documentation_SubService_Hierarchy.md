# File Transfer Management System - SubService Hierarchy API Documentation

## Overview

This document describes the API endpoints for the enhanced SubService hierarchy implementation, including timezone-aware cut-off management, file type validation, and runtime cut-off extensions.

## Table of Contents

1. [SubService Configuration API](#subservice-configuration-api)
2. [Cut-off Extension API](#cut-off-extension-api)
3. [File Type Validation API](#file-type-validation-api)
4. [Timezone Integration API](#timezone-integration-api)
5. [Error Handling](#error-handling)
6. [Migration Guide](#migration-guide)

## SubService Configuration API

### Base URL: `/api/sub-services`

#### Get All Sub-Services for Tenant
```http
GET /api/sub-services/tenant/{tenantId}
```

**Parameters:**
- `tenantId` (path): The tenant identifier

**Response:**
```json
[
  {
    "id": 1,
    "serviceName": "PaymentProcessing",
    "subServiceName": "CreditCardPayments",
    "tenantId": "tenant-001",
    "inboundPath": "/data/payments/inbound",
    "outboundPath": "/data/payments/outbound",
    "enabled": true,
    "cutOffTime": "18:00:00",
    "cutOffTimeType": "DAILY",
    "schemaValidationEnabled": true,
    "binaryFileBypass": true,
    "description": "Credit card payment processing sub-service"
  }
]
```

#### Get Specific Sub-Service
```http
GET /api/sub-services/{id}
```

**Parameters:**
- `id` (path): The sub-service configuration ID

#### Create New Sub-Service
```http
POST /api/sub-services
```

**Request Body:**
```json
{
  "serviceName": "PaymentProcessing",
  "subServiceName": "DebitCardPayments",
  "tenantId": "tenant-001",
  "inboundPath": "/data/payments/debit/inbound",
  "outboundPath": "/data/payments/debit/outbound",
  "enabled": true,
  "cutOffTime": "17:30:00",
  "cutOffTimeType": "WEEKDAY_WEEKEND",
  "weekdayCutOffTime": "17:30:00",
  "weekendCutOffTime": "15:00:00",
  "schemaValidationEnabled": true,
  "binaryFileBypass": false,
  "description": "Debit card payment processing sub-service"
}
```

#### Update Sub-Service
```http
PUT /api/sub-services/{id}
```

#### Delete Sub-Service
```http
DELETE /api/sub-services/{id}
```

**Response (Conflict):**
```json
{
  "error": "Cannot delete service 'PaymentProcessing'. There are 5 active file transfers in progress.",
  "type": "ACTIVE_TRANSFERS_EXIST"
}
```

#### Get Cut-off Information (Timezone-Aware)
```http
GET /api/sub-services/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/cut-off-info
```

**Response:**
```json
{
  "cutOffTime": "18:00:00",
  "inProcessingWindow": true,
  "timeUntilCutOffMinutes": 180,
  "timeUntilCutOffHours": 3,
  "timeZone": {
    "zoneId": "America/New_York",
    "displayName": "Eastern Standard Time",
    "offset": "-05:00",
    "currentTime": "2024-01-15 15:30:45 EST"
  },
  "currentTenantTime": "2024-01-15T15:30:45"
}
```

#### Get Available File Types
```http
GET /api/sub-services/file-types
```

**Response:**
```json
{
  "COBOL_FLAT_FILE": "COBOL Flat File",
  "BINARY_FILE": "Binary File",
  "XML": "XML File",
  "JSON": "JSON File",
  "CSV": "CSV File",
  "FIXED_WIDTH": "Fixed Width File",
  "TEXT": "Text File"
}
```

## Cut-off Extension API

### Base URL: `/api/cutoff-extensions`

#### Request Cut-off Extension
```http
POST /api/cutoff-extensions/request
```

**Request Body:**
```json
{
  "tenantId": "tenant-001",
  "serviceName": "PaymentProcessing",
  "subServiceName": "CreditCardPayments",
  "extensionDate": "2024-01-20T00:00:00",
  "originalCutOffTime": "18:00:00",
  "extendedCutOffTime": "20:00:00",
  "reason": "Critical end-of-month processing required",
  "priority": "HIGH",
  "notes": "Additional 2 hours needed for reconciliation"
}
```

**Response:**
```json
{
  "id": 123,
  "status": "PENDING",
  "priority": "HIGH",
  "requestedBy": "user@example.com",
  "requestedAt": "2024-01-15T15:30:00",
  "extensionDate": "2024-01-20T00:00:00",
  "extendedCutOffTime": "20:00:00"
}
```

#### Approve Extension
```http
POST /api/cutoff-extensions/{id}/approve
```

**Response:**
```json
{
  "id": 123,
  "status": "APPROVED",
  "approvedBy": "manager@example.com",
  "approvedAt": "2024-01-15T16:00:00"
}
```

#### Reject Extension
```http
POST /api/cutoff-extensions/{id}/reject
```

**Request Body:**
```json
{
  "rejectionReason": "Insufficient business justification"
}
```

#### Get Pending Extensions
```http
GET /api/cutoff-extensions/pending/tenant/{tenantId}
```

## File Type Validation API

### File Upload and Validation
```http
POST /api/files/validate
```

**Request:** Multipart form data with file and metadata

**Response:**
```json
{
  "fileName": "payments_20240115.dat",
  "fileType": "COBOL_FLAT_FILE",
  "valid": true,
  "schemaValidationPassed": true,
  "namingConventionValid": true,
  "extractedDate": "2024-01-15",
  "warnings": [
    "File contains 2 records with non-standard formatting"
  ],
  "info": [
    "COBOL validation passed with 1,000 records processed",
    "File naming convention detected: SOT file with date stamp"
  ]
}
```

### Schema Validation Results
```json
{
  "validationResult": {
    "valid": false,
    "errors": [
      "Line 45, Field CUSTOMER-ID: Invalid numeric value 'ABCDE'",
      "Line 67: Expected length 120, but got 115"
    ],
    "warnings": [
      "Line 12: Field BALANCE contains unusually high value"
    ],
    "validLines": 998,
    "totalLines": 1000
  }
}
```

## Timezone Integration API

### Get Tenant Timezone Information
```http
GET /api/sub-services/tenant/{tenantId}/timezone-info
```

**Response:**
```json
{
  "zoneId": "America/New_York",
  "displayName": "Eastern Standard Time",
  "offset": "-05:00",
  "currentTime": "2024-01-15 15:30:45 EST"
}
```

### Cut-off Time Calculations
All cut-off times are calculated in the tenant's configured timezone. The system automatically handles:

- Daylight Saving Time transitions
- Holiday adjustments
- Weekend vs. weekday cut-off times
- Runtime extensions

## Error Handling

### Error Response Format
```json
{
  "errorId": "ABC12345",
  "timestamp": "2024-01-15T15:30:45",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for sub-service configuration",
  "path": "/api/sub-services",
  "errorType": "VALIDATION_ERROR",
  "details": {
    "serviceName": "Service name is required",
    "cutOffTime": "Cut-off time must be in HH:mm:ss format"
  }
}
```

### Common Error Types

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `SUBSERVICE_NOT_FOUND` | 404 | Requested sub-service doesn't exist |
| `SUBSERVICE_ALREADY_EXISTS` | 409 | Sub-service with same name already exists |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `ACTIVE_TRANSFERS_EXIST` | 409 | Cannot delete service with active transfers |
| `CUTOFF_EXTENSION_INVALID` | 400 | Invalid cut-off extension request |
| `FILE_PROCESSING_ERROR` | 422 | File validation or processing failed |

## Migration Guide

### From ServiceConfiguration to SubServiceConfiguration

1. **Database Migration:**
   ```bash
   # Run the migration script
   sqlcmd -S server -d database -i scripts/migrate-subservice-hierarchy.sql
   ```

2. **API Changes:**
   - Old: `GET /api/services/tenant/{tenantId}`
   - New: `GET /api/sub-services/tenant/{tenantId}`

3. **Configuration Updates:**
   - Cut-off times moved from service to sub-service level
   - File type schemas now support direction-specific validation
   - Timezone awareness added to all time calculations

### Breaking Changes

1. **Cut-off Time Management:**
   - Cut-off times are now configured at sub-service level
   - All times are tenant timezone-aware
   - Extensions require approval workflow

2. **File Validation:**
   - Direction-specific schema validation (INBOUND/OUTBOUND)
   - Enhanced file type detection with magic number analysis
   - COBOL copybook parsing for flat files

3. **Error Responses:**
   - Structured error responses with unique error IDs
   - Detailed validation error information
   - Retry mechanisms for transient failures

## Authentication

All endpoints require valid authentication. Include the JWT token in the Authorization header:

```http
Authorization: Bearer <jwt-token>
```

## Rate Limiting

API endpoints are rate-limited to prevent abuse:
- 100 requests per minute for read operations
- 50 requests per minute for write operations
- 10 requests per minute for file upload/validation

## Examples

### Complete Sub-Service Setup Workflow

1. **Create Sub-Service:**
   ```bash
   curl -X POST /api/sub-services \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "serviceName": "PaymentProcessing",
       "subServiceName": "CreditCardPayments",
       "tenantId": "tenant-001",
       "inboundPath": "/data/payments/inbound",
       "outboundPath": "/data/payments/outbound",
       "cutOffTime": "18:00:00"
     }'
   ```

2. **Request Cut-off Extension:**
   ```bash
   curl -X POST /api/cutoff-extensions/request \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "tenantId": "tenant-001",
       "serviceName": "PaymentProcessing",
       "subServiceName": "CreditCardPayments",
       "extensionDate": "2024-01-20T00:00:00",
       "extendedCutOffTime": "20:00:00",
       "reason": "Month-end processing"
     }'
   ```

3. **Check Cut-off Status:**
   ```bash
   curl -X GET /api/sub-services/tenant/tenant-001/service/PaymentProcessing/subservice/CreditCardPayments/cut-off-info \
     -H "Authorization: Bearer $TOKEN"
   ```

For more examples and detailed integration guides, see the [Integration Examples](./integration-examples/) directory.