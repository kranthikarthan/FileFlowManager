# Enhanced File Transfer Management System

This document describes the enhanced features that have been added to the File Transfer Management System, including sub-services, cut-off time management, holiday processing, multi-tenancy with timezone support, and comprehensive alert management.

## 🚀 New Features Overview

### 1. Multi-Tenancy with Timezone Support
- **Tenant Management**: Create and manage multiple tenants with unique timezone settings
- **Timezone Support**: Each tenant can have its own timezone for accurate cut-off time calculations
- **Isolated Data**: All data (services, holidays, alerts) is isolated per tenant

### 2. Sub-Services (Optional Feature)
- **Hierarchical Services**: Services can now have optional sub-services
- **Precedence Logic**: Sub-services take precedence over parent services during processing
- **Flexible Configuration**: Each sub-service can have its own cut-off time and configuration

### 3. Enhanced Cut-Off Time Management
- **Flexible Configuration**: Multiple cut-off time types - Daily, Weekday vs Weekend, or Per Day
- **Daily Cut-Off**: Single configurable cut-off time for all days
- **Weekday vs Weekend**: Different cut-off times for weekdays (Mon-Fri) and weekends (Sat-Sun)
- **Per Day Configuration**: Individual cut-off times for each day of the week
- **Sunday Holiday Support**: Option to automatically treat all Sundays as holidays
- **Timezone-Aware**: Cut-off times are calculated based on tenant timezone
- **EOT Processing**: End-of-transmission (EOT) files must be received before cut-off time
- **Smart Resolution**: Intelligent cut-off time resolution based on configuration type and date

### 4. Enhanced Holiday Management
- **Holiday Calendar**: Define holidays for each tenant
- **Automatic Sunday Holidays**: Bulk creation of Sunday holidays for any year or date range
- **Sunday Holiday Management**: Remove Sunday holidays with single operations
- **Enhanced Holiday Checks**: Combined holiday and Sunday validation for services
- **No Processing**: File processing is automatically skipped on holidays
- **Flexible Configuration**: Add, edit, and delete holidays with descriptions

### 5. Comprehensive Alert System
- **Multiple Alert Types**: Cut-off missed, EOT not received, processing failed
- **Configurable Duration**: Set how long before cut-off time alerts should be generated
- **Multi-Channel Notifications**: Email and other notification channels
- **Alert History**: Track all alerts with acknowledgment capabilities

## 📊 Database Schema Enhancements

### New Tables

#### 1. `tenants`
```sql
CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
```

#### 2. `holidays`
```sql
CREATE TABLE holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
```

#### 3. `alert_configurations`
```sql
CREATE TABLE alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type ENUM('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED') NOT NULL,
    alert_duration_minutes INTEGER NOT NULL DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_recipients TEXT,
    notification_channels JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
```

#### 4. `alert_history`
```sql
CREATE TABLE alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NULL,
    sub_service_name VARCHAR(100) NULL,
    alert_type ENUM('CUT_OFF_MISSED', 'EOT_NOT_RECEIVED', 'PROCESSING_FAILED') NOT NULL,
    alert_message TEXT NOT NULL,
    alert_level ENUM('INFO', 'WARNING', 'ERROR', 'CRITICAL') NOT NULL DEFAULT 'WARNING',
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL,
    acknowledged_by VARCHAR(100)
);
```

### Enhanced Tables

#### 1. `file_transfer_records`
- Added `sub_service_type VARCHAR(100) NULL`
- Added `tenant_id VARCHAR(100) NOT NULL`

#### 2. `service_configurations`
- Added `sub_service_name VARCHAR(100) NULL`
- Added `tenant_id VARCHAR(100) NOT NULL`
- Added `cut_off_time TIME NOT NULL DEFAULT '23:59:59'`
- Added `cut_off_time_type VARCHAR(20) NOT NULL DEFAULT 'DAILY'`
- Added weekday/weekend specific cut-off times
- Added individual day cut-off times (Monday through Sunday)
- Added `all_sundays_as_holidays BIT NOT NULL DEFAULT 0`

## 🔧 Backend Implementation

### New Entity Classes

1. **Tenant.java** - Multi-tenant support with timezone
2. **Holiday.java** - Holiday management per tenant
3. **AlertConfiguration.java** - Alert configuration management
4. **AlertHistory.java** - Alert history tracking

### New DTOs

1. **TenantDto.java** - Tenant data transfer
2. **HolidayDto.java** - Holiday data transfer
3. **AlertConfigurationDto.java** - Alert configuration data transfer
4. **AlertHistoryDto.java** - Alert history data transfer

### New Services

1. **TenantService.java** - Tenant management operations
2. **HolidayService.java** - Holiday management operations
3. **AlertService.java** - Alert configuration and history management

### New Controllers

1. **TenantController.java** - REST endpoints for tenant management
2. **HolidayController.java** - REST endpoints for holiday management
3. **AlertController.java** - REST endpoints for alert management

### Enhanced Services

- **ServiceConfigurationService.java** - Updated to support sub-services and tenant isolation
- **FileTransferManagementService.java** - Updated to support new fields

## 🎨 Frontend Implementation

### New Components

1. **TenantManagement.js** - Complete tenant management interface
2. **HolidayManagement.js** - Holiday calendar management
3. **AlertManagement.js** - Alert configuration and history management

### Enhanced Components

- **Navigation.js** - Added new menu items for enhanced features
- **App.js** - Added new routes for enhanced features

### Key Features

#### Tenant Management
- Create, edit, and delete tenants
- Set timezone for each tenant
- Enable/disable tenants
- Search and filter tenants

#### Holiday Management
- Add holidays for specific tenants
- Date picker for holiday selection
- Holiday descriptions and names
- Bulk holiday management

#### Alert Management
- **Configurations Tab**:
  - Create alert configurations per service/sub-service
  - Set alert types and durations
  - Configure email recipients
  - Enable/disable alerts

- **History Tab**:
  - View all alert history
  - Acknowledge unacknowledged alerts
  - Filter by alert level and type
  - Track acknowledgment status

## 🔄 API Endpoints

### Tenant Management
```
GET    /api/tenants                    - Get all tenants
GET    /api/tenants/active             - Get active tenants
GET    /api/tenants/{id}               - Get tenant by ID
GET    /api/tenants/by-tenant-id/{id}  - Get tenant by tenant ID
POST   /api/tenants                    - Create new tenant
PUT    /api/tenants/{id}               - Update tenant
DELETE /api/tenants/{id}               - Delete tenant
GET    /api/tenants/search             - Search tenants
GET    /api/tenants/exists/{id}        - Check tenant exists
```

### Holiday Management
```
GET    /api/holidays/tenant/{tenantId}           - Get holidays for tenant
GET    /api/holidays/tenant/{tenantId}/date-range - Get holidays in date range
GET    /api/holidays/{id}                        - Get holiday by ID
GET    /api/holidays/tenant/{tenantId}/date/{date} - Get holiday by date
GET    /api/holidays/tenant/{tenantId}/is-holiday/{date} - Check if date is holiday
POST   /api/holidays                             - Create new holiday
PUT    /api/holidays/{id}                        - Update holiday
DELETE /api/holidays/{id}                        - Delete holiday
DELETE /api/holidays/tenant/{tenantId}/date/{date} - Delete holiday by date
GET    /api/holidays/tenant/{tenantId}/search    - Search holidays
```

### Alert Management
```
# Configurations
GET    /api/alerts/configurations/tenant/{tenantId}     - Get alert configs for tenant
GET    /api/alerts/configurations/tenant/{tenantId}/active - Get active alert configs
GET    /api/alerts/configurations/service               - Get configs for service
GET    /api/alerts/configurations/{id}                  - Get config by ID
POST   /api/alerts/configurations                       - Create alert config
PUT    /api/alerts/configurations/{id}                  - Update alert config
DELETE /api/alerts/configurations/{id}                  - Delete alert config

# History
GET    /api/alerts/history/tenant/{tenantId}            - Get alert history for tenant
GET    /api/alerts/history/service                      - Get history for service
GET    /api/alerts/history/tenant/{tenantId}/unacknowledged - Get unacknowledged alerts
POST   /api/alerts/history                              - Create alert history
PUT    /api/alerts/history/{id}/acknowledge             - Acknowledge alert

# Utility
POST   /api/alerts/send-cutoff-alert                    - Send cut-off alert
```

### Enhanced Service Endpoints
```
GET    /api/services/tenant/{tenantId}                  - Get services for tenant
GET    /api/services/tenant/{tenantId}/enabled          - Get enabled services for tenant
GET    /api/services/tenant/{tenantId}/service/{name}   - Get services by name for tenant
GET    /api/services/tenant/{tenantId}/service/{name}/subservices - Get sub-services
GET    /api/services/tenant/{tenantId}/stats            - Get service stats for tenant
```

## 🚀 Usage Examples

### Creating a Tenant
```javascript
const tenantData = {
    tenantId: "company-a",
    tenantName: "Company A",
    timezone: "America/New_York",
    enabled: true
};

await axios.post('/api/tenants', tenantData);
```

### Adding a Holiday
```javascript
const holidayData = {
    tenantId: "company-a",
    holidayDate: "2024-12-25",
    holidayName: "Christmas Day",
    description: "Office closed for Christmas"
};

await axios.post('/api/holidays', holidayData);
```

### Creating Alert Configuration
```javascript
const alertConfig = {
    tenantId: "company-a",
    serviceName: "service1",
    subServiceName: "subservice1",
    alertType: "CUT_OFF_MISSED",
    alertDurationMinutes: 30,
    enabled: true,
    emailRecipients: "admin@company.com,ops@company.com"
};

await axios.post('/api/alerts/configurations', alertConfig);
```

## 🔒 Security Considerations

1. **Tenant Isolation**: All data is properly isolated by tenant ID
2. **Input Validation**: Comprehensive validation on all endpoints
3. **SQL Injection Prevention**: Using parameterized queries
4. **XSS Prevention**: Proper input sanitization in frontend

## 🧪 Testing

### Database Setup
```bash
# Run the enhanced init.sql to create all tables
mysql -u username -p database_name < init.sql
```

### API Testing
```bash
# Test tenant creation
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"test","tenantName":"Test Tenant","timezone":"UTC"}'

# Test holiday creation
curl -X POST http://localhost:8080/api/holidays \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"test","holidayDate":"2024-01-01","holidayName":"New Year"}'
```

## 📈 Performance Considerations

1. **Indexing**: Proper database indexes on tenant_id, service_name, and date fields
2. **Caching**: Consider implementing caching for frequently accessed data
3. **Pagination**: Large datasets are paginated for better performance
4. **Query Optimization**: Efficient queries with proper joins

## 🔮 Future Enhancements

1. **Bulk Operations**: Bulk import/export of holidays and configurations
2. **Advanced Notifications**: Slack, Teams, SMS integrations
3. **Reporting**: Advanced reporting and analytics
4. **Audit Trail**: Comprehensive audit logging
5. **API Rate Limiting**: Implement rate limiting for API endpoints
6. **Webhook Support**: Webhook notifications for alerts

## 🐛 Troubleshooting

### Common Issues

1. **Timezone Issues**: Ensure timezone strings are valid IANA timezone identifiers
2. **Date Format**: Use ISO 8601 format (YYYY-MM-DD) for dates
3. **Tenant Isolation**: Verify tenant_id is properly set in all requests
4. **Alert Configuration**: Ensure alert duration is at least 1 minute

### Debug Mode
Enable debug logging in application.properties:
```properties
logging.level.com.filetransfer=DEBUG
```

## 📚 Additional Resources

- [Enhanced Cut-Off Time Features](README-Enhanced-CutOff-Features.md) - Detailed guide for new cut-off time management
- [Microservices Architecture](README-Microservices.md) - Detailed microservices setup and configuration
- [API Documentation](docs/API.md) - Complete API reference
- [Frontend Guide](file-transfer-frontend/README.md) - React frontend setup and usage
- [Migration Script](scripts/migrate-enhanced-cutoff.sql) - Database migration for existing installations

## 📞 Support

For issues and questions:
1. Check the troubleshooting section
2. Review the API documentation
3. Check server logs for detailed error messages
4. Verify database connectivity and permissions

---

**Note**: This enhanced system maintains backward compatibility while adding powerful new features for enterprise-grade file transfer management.