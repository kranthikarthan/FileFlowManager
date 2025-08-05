package com.filetransfer.web.entity;

/**
 * Enum to define different types of cut-off time configurations
 */
public enum CutOffTimeType {
    /**
     * Single cut-off time applies to all days
     */
    DAILY,
    
    /**
     * Different cut-off times for weekdays vs weekends
     * Weekdays: Monday-Friday, Weekends: Saturday-Sunday
     */
    WEEKDAY_WEEKEND,
    
    /**
     * Individual cut-off times for each day of the week
     */
    PER_DAY
}