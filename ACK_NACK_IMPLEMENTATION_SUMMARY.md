# ACK/NACK Implementation Summary

This document outlines the implementation of ACK/NACK (Acknowledgment/Negative Acknowledgment) file handling capabilities across all three applications in the file transfer system.

## Overview

The ACK/NACK feature provides:
- **Outbound Files**: Ability to receive ACK/NACK files from partners confirming receipt and processing status
- **Inbound Files**: Ability to generate and send ACK/NACK files to partners confirming our receipt and processing status

## Implementation Details

### 1. Database Schema Changes

#### New Tables Created:
- `ack_nack_records` - Main table for tracking ACK/NACK files
- `ack_nack_configuration` - Configuration settings per service
- `ack_nack_audit_log` - Audit trail for ACK/NACK operations
- `ack_nack_dashboard_metrics` - Daily metrics for reporting

#### Key Features:
- Foreign key relationship to `file_transfer_records`
- Comprehensive indexing for performance
- Support for both MySQL and SQL Server
- Automatic metrics calculation
- Audit trail for status changes

### 2. Spring Boot Web Application

#### New Entities:
- `AckNackRecord` - Main entity for ACK/NACK tracking
- `AckNackStatus` - Enum for ACK/NACK status (PENDING, GENERATED, SENT, RECEIVED, PROCESSED, FAILED, EXPIRED)
- `AckNackType` - Enum for ACK/NACK type (ACK, NACK)

#### New Services:
- `AckNackService` - Core service for ACK/NACK operations
  - Generate ACK files for successful inbound transfers
  - Generate NACK files for failed inbound transfers
  - Process received ACK/NACK files for outbound transfers
  - Send pending ACK/NACK files to partners
  - Handle expiration and retry logic

#### New Controllers:
- `AckNackController` - REST API endpoints for ACK/NACK management
  - GET `/api/v1/ack-nack/{tenantId}` - Get all ACK/NACK records
  - GET `/api/v1/ack-nack/{tenantId}/status/{status}` - Filter by status
  - GET `/api/v1/ack-nack/{tenantId}/type/{type}` - Filter by type
  - POST `/api/v1/ack-nack/generate-ack/{fileTransferId}` - Generate ACK
  - POST `/api/v1/ack-nack/generate-nack/{fileTransferId}` - Generate NACK
  - POST `/api/v1/ack-nack/{tenantId}/upload` - Upload ACK/NACK file
  - GET `/api/v1/ack-nack/{tenantId}/statistics` - Get statistics

#### Enhanced Services:
- `FileTransferManagementService` - Updated to auto-generate ACK/NACK files

### 3. Spring Boot Batch Application

#### New Services:
- `AckNackService` - Batch-specific ACK/NACK service with scheduled jobs
  - `@Scheduled` job to process incoming ACK/NACK files
  - `@Scheduled` job to generate ACK files for completed inbound transfers
  - `@Scheduled` job to send pending ACK/NACK files
  - `@Scheduled` job to mark expired records

#### Batch Processing Components:
- `AckNackFileReader` - Reads ACK/NACK files from incoming directory
- `AckNackFileProcessor` - Processes and validates ACK/NACK files
- `AckNackFileWriter` - Saves processed ACK/NACK records to database
- `AckNackBatchConfig` - Spring Batch job configuration
- `AckNackBatchController` - REST endpoints for manual job triggering

#### Enhanced Services:
- `FileTransferService` - Updated to auto-generate ACK/NACK files on completion/failure

### 4. React Frontend Application

#### New Components:
- `AckNackManagement` - Main management interface for ACK/NACK files
  - Statistics dashboard with cards showing counts
  - Filterable table of ACK/NACK records
  - Upload functionality for received ACK/NACK files
  - Manual ACK/NACK generation capabilities
  - Retry functionality for failed operations

#### New Services:
- `ackNackService` - Frontend service for API communication
  - Complete CRUD operations for ACK/NACK records
  - File upload functionality
  - Statistics and reporting methods
  - Status and type formatting utilities

#### Enhanced Components:
- `FileTransferList` - Updated to show ACK/NACK status and provide quick actions
- `Navigation` - Added ACK/NACK tab
- `App.js` - Added routing for ACK/NACK management

## File Format

The ACK/NACK files use a pipe-delimited format:

### ACK File Format:
```
ACK|{originalFileName}|{serviceName}|{subServiceName}|{timestamp}|SUCCESS|{fileSize}|{checksum}
```

### NACK File Format:
```
NACK|{originalFileName}|{serviceName}|{subServiceName}|{timestamp}|FAILED|{reasonCode}|{reasonDescription}
```

## Directory Structure

```
/data/
├── ack-nack/                    # Base directory for generated ACK/NACK files
│   ├── {tenantId}/
│   │   ├── {serviceName}/
│   │   │   ├── outbound/        # Generated ACK/NACK files to send
│   │   │   └── sent/            # Archive of sent files
└── incoming/
    └── ack-nack/                # Incoming ACK/NACK files from partners
        ├── {tenantId}/
        ├── processed/           # Successfully processed files
        └── error/               # Files that failed processing
```

## Configuration

### Web Application (`application-ack-nack.yml`):
- Base paths for file storage
- Auto-generation settings
- Template configurations
- Partner integration settings
- Security and monitoring options

### Batch Application (`application-ack-nack.yml`):
- Scheduled job intervals
- Batch processing settings
- File processing limits
- Health check configuration

## Workflow

### For Inbound Files (Data/SOT/EOT received):
1. File is processed successfully → Auto-generate ACK
2. File processing fails → Auto-generate NACK
3. ACK/NACK file is created in outbound directory
4. Scheduled job sends ACK/NACK to partner
5. Status is tracked in database

### For Outbound Files (Data/SOT/EOT sent):
1. Partner sends ACK/NACK file to incoming directory
2. Batch job processes incoming ACK/NACK files
3. Original file transfer record is updated with ACK/NACK status
4. Files are moved to processed/error directories
5. Status is tracked in database

## Monitoring and Management

### Web Interface:
- ACK/NACK Management page with statistics dashboard
- Filter and search capabilities
- Manual ACK/NACK generation
- File upload for received ACK/NACK files
- Retry functionality for failed operations

### Batch Operations:
- Scheduled automatic processing
- Manual job triggering via REST API
- Comprehensive logging and error handling
- Automatic cleanup of expired records

## Integration Points

### Existing File Transfer Flow:
- `FileTransferService` (both apps) enhanced to trigger ACK/NACK generation
- `FileTransferList` (React) enhanced to show ACK/NACK status
- Database schema extended with new tables and relationships

### Configuration Management:
- ACK/NACK behavior configurable per tenant/service
- Template-based file generation
- Partner path configuration
- Timeout and retry settings

## Error Handling

- Failed ACK/NACK operations are logged and marked as FAILED
- Files that cannot be processed are moved to error directories
- Comprehensive error messages and audit trails
- Retry mechanisms for transient failures
- Automatic expiration of stale ACK/NACK records

## Security Considerations

- ACK/NACK files inherit security context from original transfers
- Checksum validation for file integrity
- Optional encryption support (configurable)
- Audit trail for all operations
- Path validation to prevent directory traversal

This implementation provides a robust, scalable solution for ACK/NACK file handling that integrates seamlessly with the existing file transfer system.