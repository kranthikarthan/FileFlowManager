package com.filetransfer.web.service;

import com.filetransfer.web.entity.Tenant;
import com.filetransfer.web.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TenantTimeZoneService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantTimeZoneService.class);
    
    @Autowired
    private TenantRepository tenantRepository;
    
    /**
     * Get tenant's timezone or default to system timezone (cached)
     */
    @Cacheable(value = "tenantTimezones", key = "#tenantId")
    public ZoneId getTenantTimeZone(String tenantId) {
        try {
            Optional<Tenant> tenantOpt = tenantRepository.findByTenantId(tenantId);
            
            if (tenantOpt.isPresent()) {
                String timezoneStr = tenantOpt.get().getTimezone();
                if (timezoneStr != null && !timezoneStr.trim().isEmpty()) {
                    return ZoneId.of(timezoneStr);
                }
            }
            
            logger.debug("No timezone found for tenant {}, using system default", tenantId);
            return ZoneId.systemDefault();
            
        } catch (Exception e) {
            logger.warn("Error getting timezone for tenant {}, using system default: {}", tenantId, e.getMessage());
            return ZoneId.systemDefault();
        }
    }
    
    /**
     * Convert UTC time to tenant's local time
     */
    public LocalDateTime convertUtcToTenantTime(String tenantId, LocalDateTime utcDateTime) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        
        // Convert UTC to tenant timezone
        ZonedDateTime utcZoned = utcDateTime.atZone(ZoneOffset.UTC);
        ZonedDateTime tenantZoned = utcZoned.withZoneSameInstant(tenantZone);
        
        return tenantZoned.toLocalDateTime();
    }
    
    /**
     * Convert tenant's local time to UTC
     */
    public LocalDateTime convertTenantTimeToUtc(String tenantId, LocalDateTime tenantDateTime) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        
        // Convert tenant time to UTC
        ZonedDateTime tenantZoned = tenantDateTime.atZone(tenantZone);
        ZonedDateTime utcZoned = tenantZoned.withZoneSameInstant(ZoneOffset.UTC);
        
        return utcZoned.toLocalDateTime();
    }
    
    /**
     * Get current time in tenant's timezone
     */
    public LocalDateTime getCurrentTenantTime(String tenantId) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        return LocalDateTime.now(tenantZone);
    }
    
    /**
     * Get current date in tenant's timezone
     */
    public LocalDate getCurrentTenantDate(String tenantId) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        return LocalDate.now(tenantZone);
    }
    
    /**
     * Check if a specific time has passed in tenant's timezone
     */
    public boolean hasTimePassedInTenantZone(String tenantId, LocalTime cutOffTime, LocalDate targetDate) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        
        // Current time in tenant timezone
        LocalDateTime currentTenantTime = LocalDateTime.now(tenantZone);
        
        // Cut-off time on target date in tenant timezone
        LocalDateTime cutOffDateTime = targetDate.atTime(cutOffTime);
        
        return currentTenantTime.isAfter(cutOffDateTime);
    }
    
    /**
     * Get cut-off time with timezone consideration
     */
    public ZonedDateTime getCutOffTimeWithTimeZone(String tenantId, LocalDate date, LocalTime cutOffTime) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        LocalDateTime cutOffDateTime = date.atTime(cutOffTime);
        
        return cutOffDateTime.atZone(tenantZone);
    }
    
    /**
     * Format time for display in tenant's timezone
     */
    public String formatTimeForTenant(String tenantId, LocalDateTime dateTime, String pattern) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(tenantZone);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return zonedDateTime.format(formatter);
    }
    
    /**
     * Calculate time until cut-off in tenant timezone
     */
    public Duration getTimeUntilCutOff(String tenantId, LocalDate date, LocalTime cutOffTime) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        
        LocalDateTime currentTenantTime = LocalDateTime.now(tenantZone);
        LocalDateTime cutOffDateTime = date.atTime(cutOffTime);
        
        if (currentTenantTime.toLocalDate().isAfter(date)) {
            // Cut-off has passed, return negative duration
            return Duration.between(cutOffDateTime, currentTenantTime).negated();
        } else if (currentTenantTime.toLocalDate().isBefore(date)) {
            // Cut-off is in the future
            return Duration.between(currentTenantTime, cutOffDateTime);
        } else {
            // Same date
            if (currentTenantTime.toLocalTime().isAfter(cutOffTime)) {
                // Cut-off has passed today
                return Duration.between(cutOffDateTime, currentTenantTime).negated();
            } else {
                // Cut-off is later today
                return Duration.between(currentTenantTime, cutOffDateTime);
            }
        }
    }
    
    /**
     * Check if we're in the processing window for a tenant
     */
    public boolean isInProcessingWindow(String tenantId, LocalDate date, LocalTime cutOffTime) {
        Duration timeUntilCutOff = getTimeUntilCutOff(tenantId, date, cutOffTime);
        return !timeUntilCutOff.isNegative(); // Positive duration means cut-off hasn't passed
    }
    
    /**
     * Get timezone display information for tenant
     */
    public TimeZoneInfo getTenantTimeZoneInfo(String tenantId) {
        ZoneId tenantZone = getTenantTimeZone(tenantId);
        ZonedDateTime now = ZonedDateTime.now(tenantZone);
        
        return new TimeZoneInfo(
            tenantZone.getId(),
            tenantZone.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH),
            now.getOffset().toString(),
            now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
        );
    }
    
    /**
     * TimeZone information class
     */
    public static class TimeZoneInfo {
        private final String zoneId;
        private final String displayName;
        private final String offset;
        private final String currentTime;
        
        public TimeZoneInfo(String zoneId, String displayName, String offset, String currentTime) {
            this.zoneId = zoneId;
            this.displayName = displayName;
            this.offset = offset;
            this.currentTime = currentTime;
        }
        
        public String getZoneId() { return zoneId; }
        public String getDisplayName() { return displayName; }
        public String getOffset() { return offset; }
        public String getCurrentTime() { return currentTime; }
    }
}