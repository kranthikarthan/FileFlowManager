# SubService Management User Guide

## Introduction

The SubService Management system provides a hierarchical approach to managing file transfer configurations with enhanced features including timezone-aware cut-off times, file type validation, and runtime extensions.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Managing Sub-Services](#managing-sub-services)
3. [Cut-off Time Management](#cut-off-time-management)
4. [Cut-off Extensions](#cut-off-extensions)
5. [File Type Configuration](#file-type-configuration)
6. [Troubleshooting](#troubleshooting)

## Getting Started

### System Hierarchy

The new system follows this hierarchy:
```
Tenant → Service → SubService → Direction → FileType → Schema
```

**Example:**
- **Tenant**: YourCompany
- **Service**: PaymentProcessing
- **SubService**: CreditCardPayments
- **Direction**: INBOUND/OUTBOUND
- **FileType**: COBOL_FLAT_FILE, XML, JSON, etc.
- **Schema**: Validation rules for each file type

### Accessing the Interface

1. Navigate to the File Transfer Management System
2. Use the navigation menu to access "Sub-Services"
3. Select your tenant from the dropdown (if applicable)

## Managing Sub-Services

### Creating a New Sub-Service

1. **Click "Add Sub-Service"** in the Sub-Services dashboard
2. **Fill in the required information:**
   - **Service Name**: The parent service (e.g., "PaymentProcessing")
   - **Sub-Service Name**: The specific sub-service (e.g., "CreditCardPayments")
   - **Inbound Path**: Directory for incoming files
   - **Outbound Path**: Directory for outgoing files
   - **Description**: Optional description of the sub-service

3. **Configure Cut-off Settings:**
   - **Cut-off Time**: Daily processing deadline (e.g., "18:00:00")
   - **Cut-off Type**: Choose from:
     - `DAILY`: Same time every day
     - `WEEKDAY_WEEKEND`: Different times for weekdays and weekends
     - `INDIVIDUAL_DAYS`: Specific times for each day of the week

4. **Set Validation Options:**
   - **Schema Validation**: Enable/disable file schema validation
   - **Binary File Bypass**: Skip validation for binary files

5. **Click "Create"** to save the sub-service

### Editing Sub-Services

1. Find the sub-service in the list
2. Click the "Edit" button
3. Modify the desired settings
4. Click "Update" to save changes

⚠️ **Note**: Changes to cut-off times take effect immediately and apply to future processing cycles.

### Deleting Sub-Services

1. Click the "Delete" button next to the sub-service
2. Confirm the deletion

⚠️ **Warning**: You cannot delete a sub-service that has active file transfers in progress. Wait for transfers to complete or manually resolve them first.

## Cut-off Time Management

### Understanding Cut-off Times

Cut-off times determine when file processing stops for the day. Files received after the cut-off time are processed the next business day.

### Cut-off Time Types

#### Daily Cut-off
- Same time every day
- Example: 18:00:00 (6:00 PM)

#### Weekday/Weekend Cut-off
- Different times for weekdays and weekends
- Example: 
  - Weekdays: 18:00:00
  - Weekends: 16:00:00

#### Individual Day Cut-off
- Specific times for each day of the week
- Example:
  - Monday-Thursday: 18:00:00
  - Friday: 17:00:00
  - Saturday-Sunday: 16:00:00

### Timezone Considerations

All cut-off times are calculated in your organization's configured timezone. The system automatically handles:

- **Daylight Saving Time**: Automatic adjustment during DST transitions
- **Holiday Processing**: Automatic extensions on configured holidays
- **Current Status**: Real-time indication of whether you're within the processing window

### Viewing Cut-off Status

The dashboard shows:
- **Current cut-off time** for today
- **Time remaining** until cut-off
- **Processing window status** (Active/Closed)
- **Your timezone** and current local time

## Cut-off Extensions

### When to Request Extensions

Request cut-off extensions when you need to process files beyond the normal cut-off time due to:
- Critical business deadlines
- System maintenance windows
- Unexpected high volume processing
- End-of-month/quarter processing

### Requesting an Extension

1. **Navigate to "Cut-off Extensions"** in the menu
2. **Click "Request Extension"**
3. **Fill in the extension details:**
   - **Service/Sub-Service**: Select the affected sub-service
   - **Extension Date**: Date for which you need the extension
   - **Original Cut-off**: Shows the current cut-off time
   - **Extended Cut-off**: Your requested new cut-off time
   - **Reason**: Detailed business justification
   - **Priority**: Select urgency level (Low, Normal, High, Critical)
   - **Notes**: Additional context or instructions

4. **Click "Request Extension"**

### Extension Approval Process

1. **Pending**: Extension is awaiting approval
2. **Approved**: Extension is active and cut-off time is extended
3. **Rejected**: Extension was denied (reason provided)
4. **Active**: Extension is currently in effect
5. **Expired**: Extension date has passed

### Extension Priorities

- **Low**: Routine extensions, processed during business hours
- **Normal**: Standard extensions, processed within 2-4 hours
- **High**: Urgent extensions, processed within 1 hour
- **Critical**: Emergency extensions, may be auto-approved

### Monitoring Extensions

The Extensions dashboard shows:
- **Pending requests** requiring approval
- **Active extensions** currently in effect
- **Extension history** for audit purposes
- **Status updates** and approver information

## File Type Configuration

### Supported File Types

The system supports the following file types with specific validation:

#### COBOL Flat Files
- **Extension**: .dat, .txt
- **Validation**: COBOL copybook parsing
- **Features**: Field-level validation, numeric checks, fixed-width format

#### XML Files
- **Extension**: .xml
- **Validation**: XML schema (XSD)
- **Features**: Structure validation, namespace support

#### JSON Files
- **Extension**: .json
- **Validation**: JSON schema
- **Features**: Object structure validation, data type checks

#### CSV Files
- **Extension**: .csv
- **Validation**: Column structure and data types
- **Features**: Delimiter detection, header validation

#### Binary Files
- **Extension**: Various (.pdf, .jpg, .zip, etc.)
- **Validation**: File signature verification
- **Features**: Bypass option for performance

#### Fixed Width Files
- **Extension**: .fw, .txt
- **Validation**: Record length consistency
- **Features**: Position-based field validation

### Schema Configuration

1. **Navigate to Schema Management**
2. **Select the file type** you want to configure
3. **Upload or define the schema:**
   - **COBOL**: Upload copybook file
   - **XML**: Upload XSD schema
   - **JSON**: Define JSON schema
   - **CSV**: Define column structure

4. **Set direction-specific schemas:**
   - **Inbound Schema**: For files coming into the system
   - **Outbound Schema**: For files going out of the system

### File Naming Conventions

The system validates file names based on patterns:

#### Start-of-Day (SOT) Files
- **Pattern**: SOT_YYYYMMDD.ext
- **Example**: SOT_20240115.dat
- **Validation**: Prefix check, date extraction

#### End-of-Day (EOT) Files
- **Pattern**: EOT_YYYYMMDD.ext
- **Example**: EOT_20240115.dat
- **Validation**: Prefix check, summary information

#### Data Files
- **Pattern**: ServiceName_YYYYMMDD_nnn.ext
- **Example**: Payments_20240115_001.csv
- **Validation**: Service name, date, sequence number

## Troubleshooting

### Common Issues

#### Sub-Service Creation Fails
**Problem**: Error creating new sub-service
**Solutions**:
- Check that the service/sub-service combination is unique
- Verify all required fields are filled
- Ensure paths are valid and accessible

#### Cut-off Extension Rejected
**Problem**: Extension request was denied
**Solutions**:
- Review the rejection reason provided
- Provide more detailed business justification
- Consider alternative processing arrangements
- Contact your system administrator

#### File Validation Failures
**Problem**: Files are failing validation
**Solutions**:
- Check file format against the schema
- Verify file naming conventions
- Review validation error messages
- Test with a smaller sample file

#### Timezone Issues
**Problem**: Cut-off times seem incorrect
**Solutions**:
- Verify your organization's timezone setting
- Check for Daylight Saving Time adjustments
- Contact administrator to verify tenant configuration

#### Processing Window Closed
**Problem**: Files rejected due to cut-off time
**Solutions**:
- Request a cut-off extension
- Wait until next processing window
- Check if there are active extensions
- Review processing schedule

### Error Messages

#### "Active transfers exist"
Cannot delete sub-service while file transfers are in progress.
**Solution**: Wait for transfers to complete or manually resolve them.

#### "Schema validation failed"
File content doesn't match the expected schema.
**Solution**: Review file format and correct data issues.

#### "Cut-off time passed"
File submitted after the processing deadline.
**Solution**: Request extension or wait for next processing window.

#### "Duplicate extension request"
Extension already exists for the specified date.
**Solution**: Check existing extensions or modify the request.

### Getting Help

#### Self-Service Options
- Review this user guide
- Check the FAQ section
- Use the system's built-in help tooltips
- Review error message details

#### Contact Support
- **Email**: support@yourcompany.com
- **Phone**: +1-800-XXX-XXXX
- **Internal Chat**: Available during business hours
- **Ticket System**: For non-urgent issues

When contacting support, please provide:
- Your tenant ID
- Sub-service name
- Error messages or screenshots
- Steps to reproduce the issue
- Business impact description

## Best Practices

### Sub-Service Organization
- Use consistent naming conventions
- Group related processing into sub-services
- Document sub-service purposes clearly
- Regular review and cleanup of unused sub-services

### Cut-off Management
- Set realistic cut-off times
- Plan for peak processing periods
- Coordinate with downstream systems
- Monitor processing times regularly

### Extension Requests
- Provide detailed business justification
- Request extensions in advance when possible
- Use appropriate priority levels
- Monitor extension approval status

### File Processing
- Test file formats before production use
- Use descriptive file names
- Include sequence numbers for data files
- Monitor validation error reports

## Advanced Features

### Bulk Operations
- Import multiple sub-service configurations
- Bulk enable/disable sub-services
- Mass cut-off time updates

### Monitoring and Alerts
- Real-time processing status
- Cut-off time notifications
- Extension approval alerts
- File validation summaries

### Audit Trail
- Configuration change history
- Extension request tracking
- File processing logs
- User action auditing