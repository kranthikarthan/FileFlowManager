package com.filetransfer.batch.service;

import com.filetransfer.batch.entity.Holiday;
import com.filetransfer.batch.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Service to handle holiday checking for batch processing
 */
@Service
public class HolidayService {
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    /**
     * Check if a date is a holiday for a specific tenant
     */
    public boolean isHoliday(String tenantId, LocalDate date) {
        return holidayRepository.isHoliday(tenantId, date);
    }
    
    /**
     * Check if a date falls on Sunday
     */
    public boolean isSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
    
    /**
     * Enhanced holiday check that includes Sunday check for services with allSundaysAsHolidays enabled
     */
    public boolean isHolidayOrSunday(String tenantId, LocalDate date, boolean allSundaysAsHolidays) {
        // Check regular holidays
        if (isHoliday(tenantId, date)) {
            return true;
        }
        
        // Check if all Sundays are treated as holidays
        if (allSundaysAsHolidays && isSunday(date)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if processing should be skipped for a given date and configuration
     */
    public boolean shouldSkipProcessing(String tenantId, LocalDate date, boolean allSundaysAsHolidays) {
        return isHolidayOrSunday(tenantId, date, allSundaysAsHolidays);
    }
}