# Enhanced Cut-Off Time Management System

This document describes the enhanced cut-off time management features that have been added to the File Transfer Management System. The system now supports flexible cut-off time configurations per day, weekday vs weekend, specific days of the week, and automatic Sunday holiday management.

## Overview

The enhanced cut-off time system allows administrators to configure different cut-off times based on:

- **Daily**: Single cut-off time for all days
- **Weekday vs Weekend**: Different cut-off times for weekdays (Monday-Friday) and weekends (Saturday-Sunday)
- **Per Day**: Individual cut-off times for each day of the week
- **Automatic Sunday Holidays**: Option to automatically treat all Sundays as holidays

## Database Schema Changes

### ServiceConfiguration Entity Enhancements

The `service_configurations` table has been enhanced with the following new columns:

```sql
cut_off_time_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',
weekday_cut_off_time TIME NULL,
weekend_cut_off_time TIME NULL,
monday_cut_off_time TIME NULL,
tuesday_cut_off_time TIME NULL,
wednesday_cut_off_time TIME NULL,
thursday_cut_off_time TIME NULL,
friday_cut_off_time TIME NULL,
saturday_cut_off_time TIME NULL,
sunday_cut_off_time TIME NULL,
all_sundays_as_holidays BIT NOT NULL DEFAULT 0
```

### Cut-Off Time Types

The system supports three types of cut-off time configurations:

1. **DAILY**: Single cut-off time applies to all days
2. **WEEKDAY_WEEKEND**: Different cut-off times for weekdays vs weekends
3. **PER_DAY**: Individual cut-off times for each day of the week

## Core Components

### 1. CutOffTimeType Enum

```java
public enum CutOffTimeType {
    DAILY,              // Single cut-off time for all days
    WEEKDAY_WEEKEND,    // Weekdays vs Weekends
    PER_DAY            // Individual times for each day
}
```

### 2. Enhanced ServiceConfiguration Entity

The `ServiceConfiguration` entity now includes:

- `cutOffTimeType`: The type of cut-off time configuration
- Weekday/Weekend specific times
- Individual day-specific times
- `allSundaysAsHolidays`: Flag to treat all Sundays as holidays

### 3. CutOffTimeService

A new service that handles all cut-off time logic:

- `getCutOffTimeForDate(ServiceConfiguration config, LocalDate date)`: Returns the effective cut-off time for a specific date
- `isBeforeCutOffTime(ServiceConfiguration config, LocalDate date, LocalTime time)`: Checks if a time is before the cut-off
- `validateCutOffTimeConfiguration(ServiceConfiguration config)`: Validates cut-off time settings
- `getCutOffTimeDescription(ServiceConfiguration config)`: Returns a human-readable description

### 4. Enhanced HolidayService

New methods for Sunday holiday management:

- `createSundayHolidays(String tenantId, int year)`: Creates holidays for all Sundays in a year
- `createSundayHolidaysForDateRange(String tenantId, LocalDate start, LocalDate end, String name)`: Creates Sunday holidays for a date range
- `removeSundayHolidays(String tenantId, int year)`: Removes all Sunday holidays for a year
- `isHolidayOrSunday(String tenantId, LocalDate date, boolean allSundaysAsHolidays)`: Enhanced holiday check

## API Endpoints

### Service Configuration Cut-Off Time Endpoints

#### Get Effective Cut-Off Time
```
GET /api/services/{id}/cutoff-time/{date}
```

Response:
```json
{
  "serviceId": 1,
  "date": "2024-01-15",
  "cutOffTime": "18:00:00",
  "description": "Weekdays: 18:00:00, Weekends: 12:00:00"
}
```

#### Get Cut-Off Time by Service Name
```
GET /api/services/tenant/{tenantId}/service/{serviceName}/cutoff-time/{date}
```

#### Check if Time is Before Cut-Off
```
POST /api/services/{id}/check-cutoff
```

Request body:
```json
{
  "date": "2024-01-15",
  "time": "17:30:00"
}
```

Response:
```json
{
  "serviceId": 1,
  "date": "2024-01-15",
  "time": "17:30:00",
  "cutOffTime": "18:00:00",
  "isBeforeCutOff": true,
  "message": "Within cut-off time"
}
```

### Sunday Holiday Management Endpoints

#### Create Sunday Holidays for a Year
```
POST /api/holidays/tenant/{tenantId}/create-sunday-holidays/{year}?holidayName=Sunday Holiday
```

#### Create Sunday Holidays for Date Range
```
POST /api/holidays/tenant/{tenantId}/create-sunday-holidays-range?startDate=2024-01-01&endDate=2024-12-31&holidayName=Sunday Holiday
```

#### Remove Sunday Holidays for a Year
```
DELETE /api/holidays/tenant/{tenantId}/remove-sunday-holidays/{year}
```

#### Check if Date is Holiday or Sunday
```
GET /api/holidays/tenant/{tenantId}/is-holiday-or-sunday/{date}?allSundaysAsHolidays=true
```

## Configuration Examples

### Example 1: Daily Cut-Off Time

```json
{
  "serviceName": "DataProcessingService",
  "cutOffTimeType": "DAILY",
  "cutOffTime": "23:00:00",
  "allSundaysAsHolidays": false
}
```

This configuration sets a single cut-off time of 11:00 PM for all days.

### Example 2: Weekday vs Weekend Cut-Off Times

```json
{
  "serviceName": "BusinessService",
  "cutOffTimeType": "WEEKDAY_WEEKEND",
  "cutOffTime": "18:00:00",
  "weekdayCutOffTime": "18:00:00",
  "weekendCutOffTime": "12:00:00",
  "allSundaysAsHolidays": false
}
```

This configuration sets different cut-off times for weekdays (6:00 PM) and weekends (12:00 PM).

### Example 3: Per-Day Cut-Off Times

```json
{
  "serviceName": "CustomService",
  "cutOffTimeType": "PER_DAY",
  "cutOffTime": "20:00:00",
  "mondayCutOffTime": "18:00:00",
  "tuesdayCutOffTime": "18:00:00",
  "wednesdayCutOffTime": "18:00:00",
  "thursdayCutOffTime": "18:00:00",
  "fridayCutOffTime": "17:00:00",
  "saturdayCutOffTime": "12:00:00",
  "sundayCutOffTime": "10:00:00",
  "allSundaysAsHolidays": true
}
```

This configuration sets individual cut-off times for each day of the week and treats all Sundays as holidays.

## Usage Scenarios

### Scenario 1: Business Hours Service

A service that operates during business hours with early Friday cut-off:

- Monday-Thursday: 18:00 cut-off
- Friday: 16:00 cut-off (early weekend start)
- Saturday-Sunday: 10:00 cut-off (limited weekend processing)

### Scenario 2: Financial Service

A financial service with strict weekend restrictions:

- Weekdays: 20:00 cut-off
- Weekends: No processing (all Sundays as holidays)

### Scenario 3: Retail Service

A retail service with extended hours:

- Monday-Friday: 22:00 cut-off
- Saturday: 20:00 cut-off
- Sunday: 18:00 cut-off

## Implementation Details

### Cut-Off Time Resolution Logic

The system uses the following priority order to determine the effective cut-off time:

1. **PER_DAY**: Use day-specific time if configured
2. **WEEKDAY_WEEKEND**: Use weekday/weekend specific time if configured
3. **DAILY**: Use default cut-off time
4. **Fallback**: Use system default (23:59:59)

### Holiday Integration

When `allSundaysAsHolidays` is enabled:

1. The system can automatically create holiday records for all Sundays
2. Holiday checks include Sunday validation
3. Processing rules respect Sunday holidays

### Validation

The system validates:

- Time format (HH:mm:ss)
- Cut-off time type consistency
- Required fields based on configuration type
- Time ordering (optional business rule validation)

## Migration Considerations

### Existing Services

- Existing services will default to `DAILY` cut-off type
- Current `cut_off_time` values are preserved
- No breaking changes to existing API endpoints

### Data Migration

```sql
-- All existing services will use DAILY type by default
UPDATE service_configurations 
SET cut_off_time_type = 'DAILY' 
WHERE cut_off_time_type IS NULL;
```

## Best Practices

### 1. Configuration Management

- Start with `DAILY` configuration for simplicity
- Use `WEEKDAY_WEEKEND` for business services
- Use `PER_DAY` only when necessary for complex requirements

### 2. Holiday Management

- Create Sunday holidays at the beginning of each year
- Use consistent holiday naming conventions
- Review and clean up old holiday records periodically

### 3. Monitoring

- Monitor cut-off time violations
- Set up alerts for missed cut-off times
- Track holiday processing metrics

### 4. Testing

- Test cut-off time calculations across date boundaries
- Verify holiday integration works correctly
- Test timezone handling (if applicable)

## Frontend Integration

The frontend should support:

1. **Cut-Off Time Type Selection**: Radio buttons or dropdown for type selection
2. **Conditional Fields**: Show relevant time fields based on selected type
3. **Time Pickers**: User-friendly time input controls
4. **Holiday Management**: Interface for Sunday holiday creation/removal
5. **Validation**: Client-side validation for time formats and consistency

### Example Frontend Form Structure

```javascript
const CutOffTimeForm = () => {
  const [cutOffType, setCutOffType] = useState('DAILY');
  
  return (
    <form>
      <select value={cutOffType} onChange={(e) => setCutOffType(e.target.value)}>
        <option value="DAILY">Daily</option>
        <option value="WEEKDAY_WEEKEND">Weekday vs Weekend</option>
        <option value="PER_DAY">Per Day</option>
      </select>
      
      {cutOffType === 'DAILY' && (
        <input type="time" name="cutOffTime" />
      )}
      
      {cutOffType === 'WEEKDAY_WEEKEND' && (
        <>
          <input type="time" name="weekdayCutOffTime" placeholder="Weekday Cut-off" />
          <input type="time" name="weekendCutOffTime" placeholder="Weekend Cut-off" />
        </>
      )}
      
      {cutOffType === 'PER_DAY' && (
        <>
          <input type="time" name="mondayCutOffTime" placeholder="Monday" />
          <input type="time" name="tuesdayCutOffTime" placeholder="Tuesday" />
          {/* ... other days */}
        </>
      )}
      
      <checkbox name="allSundaysAsHolidays" />
    </form>
  );
};
```

## Troubleshooting

### Common Issues

1. **Invalid Time Format**: Ensure times are in HH:mm:ss format
2. **Missing Required Fields**: Verify all required fields for the selected cut-off type
3. **Holiday Conflicts**: Check for existing holidays when creating Sunday holidays
4. **Timezone Issues**: Ensure consistent timezone handling across the system

### Debugging

Use the cut-off time description endpoint to verify configuration:

```bash
curl -X GET "http://localhost:8080/api/services/1/cutoff-time/2024-01-15"
```

This will return the effective cut-off time and configuration description for debugging.

## Conclusion

The enhanced cut-off time management system provides flexible configuration options while maintaining backward compatibility. The system supports complex business requirements while keeping the interface simple for basic use cases.

The Sunday holiday automation feature reduces manual administrative overhead and ensures consistent holiday processing across all services.