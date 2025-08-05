package com.filetransfer.web.controller;

import com.filetransfer.web.dto.HolidayDto;
import com.filetransfer.web.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "*")
public class HolidayController {
    
    @Autowired
    private HolidayService holidayService;
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<HolidayDto>> getHolidaysForTenant(@PathVariable String tenantId) {
        List<HolidayDto> holidays = holidayService.getHolidaysForTenant(tenantId);
        return ResponseEntity.ok(holidays);
    }
    
    @GetMapping("/tenant/{tenantId}/date-range")
    public ResponseEntity<List<HolidayDto>> getHolidaysForTenantAndDateRange(
            @PathVariable String tenantId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<HolidayDto> holidays = holidayService.getHolidaysForTenantAndDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(holidays);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<HolidayDto> getHolidayById(@PathVariable Long id) {
        return holidayService.getHolidayById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/tenant/{tenantId}/date/{date}")
    public ResponseEntity<HolidayDto> getHolidayByTenantIdAndDate(
            @PathVariable String tenantId, 
            @PathVariable LocalDate date) {
        HolidayDto holiday = holidayService.getHolidayByTenantIdAndDate(tenantId, date);
        return holiday != null ? ResponseEntity.ok(holiday) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/tenant/{tenantId}/is-holiday/{date}")
    public ResponseEntity<Boolean> isHoliday(@PathVariable String tenantId, @PathVariable LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(tenantId, date);
        return ResponseEntity.ok(isHoliday);
    }
    
    @PostMapping
    public ResponseEntity<HolidayDto> createHoliday(@Valid @RequestBody HolidayDto holidayDto) {
        try {
            HolidayDto createdHoliday = holidayService.createHoliday(holidayDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdHoliday);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<HolidayDto> updateHoliday(@PathVariable Long id, @Valid @RequestBody HolidayDto holidayDto) {
        try {
            HolidayDto updatedHoliday = holidayService.updateHoliday(id, holidayDto);
            return ResponseEntity.ok(updatedHoliday);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        try {
            holidayService.deleteHoliday(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/tenant/{tenantId}/date/{date}")
    public ResponseEntity<Void> deleteHolidayByTenantIdAndDate(
            @PathVariable String tenantId, 
            @PathVariable LocalDate date) {
        holidayService.deleteHolidayByTenantIdAndDate(tenantId, date);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<List<HolidayDto>> searchHolidays(
            @PathVariable String tenantId, 
            @RequestParam String searchTerm) {
        List<HolidayDto> holidays = holidayService.searchHolidays(tenantId, searchTerm);
        return ResponseEntity.ok(holidays);
    }
    
    @PostMapping("/tenant/{tenantId}/create-sunday-holidays/{year}")
    public ResponseEntity<List<HolidayDto>> createSundayHolidays(
            @PathVariable String tenantId,
            @PathVariable int year,
            @RequestParam(required = false, defaultValue = "Sunday Holiday") String holidayName) {
        try {
            List<HolidayDto> createdHolidays = holidayService.createSundayHolidays(tenantId, year, holidayName);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdHolidays);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/tenant/{tenantId}/create-sunday-holidays-range")
    public ResponseEntity<List<HolidayDto>> createSundayHolidaysForDateRange(
            @PathVariable String tenantId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false, defaultValue = "Sunday Holiday") String holidayName) {
        try {
            List<HolidayDto> createdHolidays = holidayService.createSundayHolidaysForDateRange(tenantId, startDate, endDate, holidayName);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdHolidays);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/tenant/{tenantId}/remove-sunday-holidays/{year}")
    public ResponseEntity<Integer> removeSundayHolidays(
            @PathVariable String tenantId,
            @PathVariable int year) {
        try {
            int removedCount = holidayService.removeSundayHolidays(tenantId, year);
            return ResponseEntity.ok(removedCount);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/tenant/{tenantId}/is-holiday-or-sunday/{date}")
    public ResponseEntity<Boolean> isHolidayOrSunday(
            @PathVariable String tenantId, 
            @PathVariable LocalDate date,
            @RequestParam(required = false, defaultValue = "false") boolean allSundaysAsHolidays) {
        boolean isHolidayOrSunday = holidayService.isHolidayOrSunday(tenantId, date, allSundaysAsHolidays);
        return ResponseEntity.ok(isHolidayOrSunday);
    }
}