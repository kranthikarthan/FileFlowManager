# Migration Guide: ServiceConfiguration to SubService Hierarchy

## Overview

This guide provides step-by-step instructions for migrating from the legacy ServiceConfiguration system to the new SubService hierarchy with enhanced features.

## Table of Contents

1. [Pre-Migration Checklist](#pre-migration-checklist)
2. [Database Migration](#database-migration)
3. [Application Configuration](#application-configuration)
4. [API Migration](#api-migration)
5. [Frontend Updates](#frontend-updates)
6. [Testing and Validation](#testing-and-validation)
7. [Rollback Plan](#rollback-plan)
8. [Post-Migration Tasks](#post-migration-tasks)

## Pre-Migration Checklist

### Requirements Verification

- [ ] Java 11+ runtime environment
- [ ] Spring Boot 2.7+ framework
- [ ] Database system (SQL Server/PostgreSQL/MySQL)
- [ ] Backup of current system and data
- [ ] Maintenance window scheduled
- [ ] Stakeholder notifications sent

### Data Assessment

- [ ] Inventory of existing ServiceConfigurations
- [ ] Identification of tenant-specific configurations
- [ ] Review of cut-off time configurations
- [ ] Schema and file type mapping analysis
- [ ] Active file transfer status check

### Environment Preparation

- [ ] Development environment updated and tested
- [ ] Test data prepared for validation
- [ ] Database backup created
- [ ] Application logs archived
- [ ] Monitoring alerts configured

## Database Migration

### Step 1: Backup Current Database

```sql
-- SQL Server example
BACKUP DATABASE FileTransferDB 
TO DISK = 'C:\Backups\FileTransferDB_PreMigration.bak'
WITH FORMAT, INIT;

-- PostgreSQL example
pg_dump -h localhost -U username -d filetransfer > backup_premigration.sql
```

### Step 2: Run Migration Script

Execute the migration script to create new tables and migrate data:

```bash
# For SQL Server
sqlcmd -S server -d FileTransferDB -i scripts/migrate-subservice-hierarchy.sql

# For PostgreSQL
psql -h localhost -U username -d filetransfer -f scripts/migrate-subservice-hierarchy.sql

# For MySQL
mysql -h localhost -u username -p filetransfer < scripts/migrate-subservice-hierarchy.sql
```

### Step 3: Verify Migration Results

```sql
-- Check new tables were created
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME IN ('sub_service_configurations', 'cutoff_extensions', 'sub_service_file_type_configs');

-- Verify data migration counts
SELECT 
    'service_configurations' as source_table, COUNT(*) as count 
FROM service_configurations
UNION ALL
SELECT 
    'sub_service_configurations' as target_table, COUNT(*) as count 
FROM sub_service_configurations;

-- Check file transfer records update
SELECT 
    file_type, 
    COUNT(*) as count 
FROM file_transfer_records 
GROUP BY file_type;
```

### Step 4: Create Performance Indexes

```bash
# Run performance optimization script
sqlcmd -S server -d FileTransferDB -i scripts/performance-optimization-indexes.sql
```

## Application Configuration

### Step 1: Update Dependencies

Add new dependencies to your `pom.xml`:

```xml
<!-- Spring Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Spring Retry -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>

<!-- Spring AOP (for retry) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Step 2: Update Application Properties

Add new configuration properties:

```properties
# Cache configuration
spring.cache.type=simple
spring.cache.cache-names=subServiceConfigs,tenantTimezones,fileSchemas,cutOffTimes,holidays

# Retry configuration
spring.retry.enabled=true

# File processing configuration
file-transfer.validation.cobol.enabled=true
file-transfer.validation.schema.strict-mode=true
file-transfer.cut-off.timezone-aware=true

# Performance settings
file-transfer.cache.hot-cache-size=1000
file-transfer.cache.preload-enabled=true
```

### Step 3: Enable New Features

Update your main application class:

```java
@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableScheduling
public class FileTransferApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileTransferApplication.class, args);
    }
}
```

## API Migration

### Step 1: Update Endpoints

#### Legacy Service Configuration Endpoints
```java
// OLD - Remove these
@GetMapping("/api/services/tenant/{tenantId}")
@PostMapping("/api/services")
@PutMapping("/api/services/{id}")
@DeleteMapping("/api/services/{id}")
```

#### New SubService Configuration Endpoints
```java
// NEW - Add these
@GetMapping("/api/sub-services/tenant/{tenantId}")
@PostMapping("/api/sub-services")
@PutMapping("/api/sub-services/{id}")
@DeleteMapping("/api/sub-services/{id}")

// Additional new endpoints
@GetMapping("/api/sub-services/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}/cut-off-info")
@GetMapping("/api/sub-services/tenant/{tenantId}/timezone-info")
@PostMapping("/api/cutoff-extensions/request")
@PostMapping("/api/cutoff-extensions/{id}/approve")
@PostMapping("/api/cutoff-extensions/{id}/reject")
```

### Step 2: Update Request/Response Models

#### Legacy ServiceConfiguration
```java
// OLD - Deprecate but keep for backward compatibility
public class ServiceConfigurationDto {
    private String serviceName;
    private String cutOffTime;
    private String dataSchemaId;
    // ... other fields
}
```

#### New SubServiceConfiguration
```java
// NEW - Primary model
public class SubServiceConfigurationDto {
    private String serviceName;
    private String subServiceName;
    private String cutOffTime;
    private CutOffTimeType cutOffTimeType;
    private Map<FileType, Long> inboundSchemas;
    private Map<FileType, Long> outboundSchemas;
    // ... enhanced fields
}
```

### Step 3: Implement Backward Compatibility

Create adapter layer for existing clients:

```java
@RestController
@RequestMapping("/api/services")
@Deprecated
public class LegacyServiceConfigurationController {
    
    @Autowired
    private SubServiceConfigurationService subServiceService;
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<LegacyServiceDto>> getLegacyServices(@PathVariable String tenantId) {
        // Convert SubService configurations to legacy format
        List<SubServiceConfiguration> subServices = subServiceService.getSubServicesForTenant(tenantId);
        List<LegacyServiceDto> legacyServices = convertToLegacyFormat(subServices);
        return ResponseEntity.ok(legacyServices);
    }
    
    private List<LegacyServiceDto> convertToLegacyFormat(List<SubServiceConfiguration> subServices) {
        // Implementation to maintain backward compatibility
        // Group sub-services by service name and create legacy representation
    }
}
```

## Frontend Updates

### Step 1: Update Navigation

Update the navigation component to include new menu items:

```javascript
// Add to Navigation.js
<Tab 
  icon={<AccountTreeIcon />} 
  iconPosition="start" 
  label="Sub-Services" 
  value="/sub-services" 
/>
<Tab 
  icon={<ScheduleIcon />} 
  iconPosition="start" 
  label="Cut-off Extensions" 
  value="/cutoff-extensions" 
/>
```

### Step 2: Update Service Management

Create new components or update existing ones:

```javascript
// Replace ServiceManagement.js usage with SubServiceManagement.js
import SubServiceManagement from './components/SubServiceManagement';
import CutOffExtensionManagement from './components/CutOffExtensionManagement';

// Update routes in App.js
<Route path="/sub-services" element={<SubServiceManagement tenantId={user?.tenantId} />} />
<Route path="/cutoff-extensions" element={<CutOffExtensionManagement tenantId={user?.tenantId} />} />
```

### Step 3: Update API Calls

Update frontend service calls:

```javascript
// OLD API calls
const getServices = async (tenantId) => {
  return fetch(`/api/services/tenant/${tenantId}`);
};

// NEW API calls
const getSubServices = async (tenantId) => {
  return fetch(`/api/sub-services/tenant/${tenantId}`);
};

const getCutOffInfo = async (tenantId, serviceName, subServiceName) => {
  return fetch(`/api/sub-services/tenant/${tenantId}/service/${serviceName}/subservice/${subServiceName}/cut-off-info`);
};
```

## Testing and Validation

### Step 1: Unit Tests

Run existing and new unit tests:

```bash
# Run all tests
mvn test

# Run specific test suites
mvn test -Dtest=SubServiceConfigurationServiceTest
mvn test -Dtest=CobolCopybookParserTest
mvn test -Dtest=TenantTimeZoneServiceTest
```

### Step 2: Integration Tests

Execute integration test suite:

```bash
# Run integration tests
mvn test -Dtest=SubServiceHierarchyIntegrationTest
mvn test -Dtest=*IntegrationTest

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Step 3: Data Validation

Verify migrated data integrity:

```sql
-- Validate sub-service creation from service configurations
SELECT 
    sc.tenant_id,
    sc.service_name,
    COUNT(ssc.id) as subservice_count
FROM service_configurations sc
LEFT JOIN sub_service_configurations ssc 
    ON sc.tenant_id = ssc.tenant_id 
    AND sc.service_name = ssc.service_name
GROUP BY sc.tenant_id, sc.service_name;

-- Check file transfer record updates
SELECT 
    status,
    file_type,
    COUNT(*) as count
FROM file_transfer_records 
GROUP BY status, file_type
ORDER BY status, file_type;

-- Verify schema mappings
SELECT 
    file_type,
    COUNT(*) as mapping_count
FROM sub_service_file_type_configs
GROUP BY file_type;
```

### Step 4: Functional Testing

Test key workflows:

1. **Sub-Service CRUD Operations**
   - Create new sub-service
   - Update configuration
   - Delete sub-service (with active transfer check)

2. **Cut-off Extension Workflow**
   - Request extension
   - Approve/reject extension
   - Verify timezone calculations

3. **File Processing**
   - Upload files of different types
   - Verify validation rules
   - Test schema enforcement

4. **Timezone Functionality**
   - Test cut-off calculations across timezones
   - Verify DST handling
   - Check holiday processing

## Rollback Plan

### Step 1: Preparation

Before migration, ensure rollback capability:

```sql
-- Create rollback scripts
-- 1. Drop new tables
-- 2. Restore original schema
-- 3. Restore data from backup

-- Example rollback script
DROP TABLE IF EXISTS cutoff_extensions;
DROP TABLE IF EXISTS sub_service_file_type_configs;
DROP TABLE IF EXISTS sub_service_configurations;

-- Restore columns removed from file_transfer_records
ALTER TABLE file_transfer_records DROP COLUMN file_type;
ALTER TABLE file_transfer_records DROP COLUMN schema_validation_passed;
```

### Step 2: Rollback Procedure

If rollback is required:

1. **Stop Application**
   ```bash
   systemctl stop file-transfer-app
   ```

2. **Restore Database**
   ```sql
   -- SQL Server
   RESTORE DATABASE FileTransferDB 
   FROM DISK = 'C:\Backups\FileTransferDB_PreMigration.bak'
   WITH REPLACE;
   
   -- PostgreSQL
   dropdb filetransfer
   createdb filetransfer
   psql -h localhost -U username -d filetransfer < backup_premigration.sql
   ```

3. **Deploy Previous Version**
   ```bash
   # Deploy previous application version
   # Update configuration files
   # Restart services
   ```

4. **Validate Rollback**
   ```bash
   # Run health checks
   # Verify functionality
   # Check data integrity
   ```

## Post-Migration Tasks

### Step 1: Performance Monitoring

Monitor system performance after migration:

```sql
-- Monitor query performance
SELECT 
    query_text,
    execution_count,
    average_duration
FROM sys.query_performance_statistics
WHERE query_text LIKE '%sub_service_configurations%'
ORDER BY average_duration DESC;

-- Check index usage
SELECT 
    tablename,
    indexname,
    idx_scan,
    idx_tup_read
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### Step 2: Cache Optimization

Optimize cache performance:

```java
// Monitor cache hit ratios
@Component
public class CacheMonitor {
    
    @EventListener
    public void handleCacheHit(CacheHitEvent event) {
        // Log cache statistics
    }
    
    @EventListener
    public void handleCacheMiss(CacheMissEvent event) {
        // Log cache misses for optimization
    }
}
```

### Step 3: User Training

Provide training for users:

1. **Send Migration Notification**
   - Highlight new features
   - Provide user guide links
   - Schedule training sessions

2. **Update Documentation**
   - Update internal wikis
   - Create video tutorials
   - Update help desk scripts

3. **Monitor Support Requests**
   - Track migration-related issues
   - Update FAQs based on questions
   - Provide additional training as needed

### Step 4: Legacy System Cleanup

Plan legacy system retirement:

```java
// Add deprecation warnings
@RestController
@RequestMapping("/api/services")
@Deprecated // Mark for removal in next major version
public class LegacyServiceConfigurationController {
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<LegacyServiceDto>> getLegacyServices(@PathVariable String tenantId) {
        logger.warn("Legacy API endpoint accessed. Please migrate to /api/sub-services");
        // Implementation...
    }
}
```

### Step 5: Continuous Monitoring

Set up ongoing monitoring:

1. **Application Metrics**
   - Response times
   - Error rates
   - Cache hit ratios
   - Database performance

2. **Business Metrics**
   - File processing volumes
   - Cut-off extension requests
   - Validation failure rates
   - User adoption of new features

3. **Alerts and Notifications**
   - Performance degradation
   - High error rates
   - System capacity issues
   - Security incidents

## Support and Troubleshooting

### Common Migration Issues

#### Database Migration Failures
- **Symptom**: Migration script errors
- **Solution**: Check database permissions, disk space, and existing data constraints

#### Performance Degradation
- **Symptom**: Slower response times
- **Solution**: Verify indexes were created, check cache configuration, monitor query execution

#### Data Inconsistency
- **Symptom**: Missing or incorrect data after migration
- **Solution**: Re-run migration scripts, validate data mapping logic

#### Frontend Issues
- **Symptom**: UI errors or missing functionality
- **Solution**: Clear browser cache, verify API endpoint updates, check console errors

### Getting Help

- **Documentation**: Refer to this guide and API documentation
- **Support Team**: Contact technical support with migration-specific issues
- **Community**: Use internal forums for questions and best practices
- **Vendor Support**: Contact vendor for complex technical issues

Remember to document any issues encountered and their solutions for future reference and to help other teams going through similar migrations.