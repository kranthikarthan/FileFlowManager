package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.ServiceConfiguration;
import com.filetransfer.batch.entity.CutOffTimeType;
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
     * Check if current time is before cut-off for a given date
     */
    public boolean isCurrentTimeBeforeCutOff(ServiceConfiguration config, LocalDate date) {
        return isBeforeCutOffTime(config, date, LocalTime.now());
    }
}