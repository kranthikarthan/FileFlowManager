package com.filetransfer.web.service;

import com.filetransfer.web.entity.ServiceConfiguration;
import com.filetransfer.web.entity.CutOffTimeType;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Service to handle cut-off time logic based on different day types and configurations
 */
@Service
public class CutOffTimeService {
    
    /**
     * Get the appropriate cut-off time for a given date based on service configuration
     */
    public LocalTime getCutOffTimeForDate(ServiceConfiguration config, LocalDate date) {
        if (config == null || date == null) {
            return LocalTime.parse("23:59:59");
        }
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        switch (config.getCutOffTimeType()) {
            case DAILY:
                return parseTime(config.getCutOffTime());
                
            case WEEKDAY_WEEKEND:
                return getCutOffTimeForWeekdayWeekend(config, dayOfWeek);
                
            case PER_DAY:
                return getCutOffTimeForSpecificDay(config, dayOfWeek);
                
            default:
                return parseTime(config.getCutOffTime());
        }
    }
    
    /**
     * Get cut-off time based on weekday vs weekend configuration
     */
    private LocalTime getCutOffTimeForWeekdayWeekend(ServiceConfiguration config, DayOfWeek dayOfWeek) {
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        if (isWeekend && config.getWeekendCutOffTime() != null) {
            return parseTime(config.getWeekendCutOffTime());
        } else if (!isWeekend && config.getWeekdayCutOffTime() != null) {
            return parseTime(config.getWeekdayCutOffTime());
        }
        
        // Fallback to default cut-off time
        return parseTime(config.getCutOffTime());
    }
    
    /**
     * Get cut-off time for specific day of week
     */
    private LocalTime getCutOffTimeForSpecificDay(ServiceConfiguration config, DayOfWeek dayOfWeek) {
        String daySpecificTime = null;
        
        switch (dayOfWeek) {
            case MONDAY:
                daySpecificTime = config.getMondayCutOffTime();
                break;
            case TUESDAY:
                daySpecificTime = config.getTuesdayCutOffTime();
                break;
            case WEDNESDAY:
                daySpecificTime = config.getWednesdayCutOffTime();
                break;
            case THURSDAY:
                daySpecificTime = config.getThursdayCutOffTime();
                break;
            case FRIDAY:
                daySpecificTime = config.getFridayCutOffTime();
                break;
            case SATURDAY:
                daySpecificTime = config.getSaturdayCutOffTime();
                break;
            case SUNDAY:
                daySpecificTime = config.getSundayCutOffTime();
                break;
        }
        
        if (daySpecificTime != null && !daySpecificTime.trim().isEmpty()) {
            return parseTime(daySpecificTime);
        }
        
        // Fallback to default cut-off time
        return parseTime(config.getCutOffTime());
    }
    
    /**
     * Parse time string safely with fallback
     */
    private LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return LocalTime.parse("23:59:59");
        }
        
        try {
            return LocalTime.parse(timeString);
        } catch (Exception e) {
            // Log error and return default time
            return LocalTime.parse("23:59:59");
        }
    }
    
    /**
     * Check if a given date and time is before the cut-off time
     */
    public boolean isBeforeCutOffTime(ServiceConfiguration config, LocalDate date, LocalTime time) {
        LocalTime cutOffTime = getCutOffTimeForDate(config, date);
        return time.isBefore(cutOffTime) || time.equals(cutOffTime);
    }
    
    /**
     * Get cut-off time description for display purposes
     */
    public String getCutOffTimeDescription(ServiceConfiguration config) {
        if (config == null) {
            return "Default: 23:59:59";
        }
        
        switch (config.getCutOffTimeType()) {
            case DAILY:
                return "Daily: " + config.getCutOffTime();
                
            case WEEKDAY_WEEKEND:
                StringBuilder sb = new StringBuilder();
                sb.append("Weekdays: ").append(config.getWeekdayCutOffTime() != null ? 
                    config.getWeekdayCutOffTime() : config.getCutOffTime());
                sb.append(", Weekends: ").append(config.getWeekendCutOffTime() != null ? 
                    config.getWeekendCutOffTime() : config.getCutOffTime());
                return sb.toString();
                
            case PER_DAY:
                StringBuilder perDay = new StringBuilder("Per Day - ");
                perDay.append("Mon: ").append(config.getMondayCutOffTime() != null ? 
                    config.getMondayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Tue: ").append(config.getTuesdayCutOffTime() != null ? 
                    config.getTuesdayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Wed: ").append(config.getWednesdayCutOffTime() != null ? 
                    config.getWednesdayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Thu: ").append(config.getThursdayCutOffTime() != null ? 
                    config.getThursdayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Fri: ").append(config.getFridayCutOffTime() != null ? 
                    config.getFridayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Sat: ").append(config.getSaturdayCutOffTime() != null ? 
                    config.getSaturdayCutOffTime() : config.getCutOffTime()).append(", ");
                perDay.append("Sun: ").append(config.getSundayCutOffTime() != null ? 
                    config.getSundayCutOffTime() : config.getCutOffTime());
                return perDay.toString();
                
            default:
                return "Default: " + config.getCutOffTime();
        }
    }
    
    /**
     * Validate cut-off time configuration
     */
    public void validateCutOffTimeConfiguration(ServiceConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("Service configuration cannot be null");
        }
        
        // Validate main cut-off time
        validateTimeString(config.getCutOffTime(), "Main cut-off time");
        
        if (config.getCutOffTimeType() == CutOffTimeType.WEEKDAY_WEEKEND) {
            if (config.getWeekdayCutOffTime() != null) {
                validateTimeString(config.getWeekdayCutOffTime(), "Weekday cut-off time");
            }
            if (config.getWeekendCutOffTime() != null) {
                validateTimeString(config.getWeekendCutOffTime(), "Weekend cut-off time");
            }
        } else if (config.getCutOffTimeType() == CutOffTimeType.PER_DAY) {
            validateTimeString(config.getMondayCutOffTime(), "Monday cut-off time");
            validateTimeString(config.getTuesdayCutOffTime(), "Tuesday cut-off time");
            validateTimeString(config.getWednesdayCutOffTime(), "Wednesday cut-off time");
            validateTimeString(config.getThursdayCutOffTime(), "Thursday cut-off time");
            validateTimeString(config.getFridayCutOffTime(), "Friday cut-off time");
            validateTimeString(config.getSaturdayCutOffTime(), "Saturday cut-off time");
            validateTimeString(config.getSundayCutOffTime(), "Sunday cut-off time");
        }
    }
    
    /**
     * Validate individual time string
     */
    private void validateTimeString(String timeString, String fieldName) {
        if (timeString != null && !timeString.trim().isEmpty()) {
            try {
                LocalTime.parse(timeString);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid " + fieldName + ": " + timeString);
            }
        }
    }
}