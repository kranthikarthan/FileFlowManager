package com.filetransfer.web.service;

import com.filetransfer.web.dto.HolidayDto;
import com.filetransfer.web.entity.Holiday;
import com.filetransfer.web.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class HolidayService {
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    public List<HolidayDto> getHolidaysForTenant(String tenantId) {
        return holidayRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<HolidayDto> getHolidaysForTenantAndDateRange(String tenantId, LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByTenantIdAndHolidayDateBetween(tenantId, startDate, endDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<HolidayDto> getHolidayById(Long id) {
        return holidayRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public HolidayDto getHolidayByTenantIdAndDate(String tenantId, LocalDate date) {
        Holiday holiday = holidayRepository.findByTenantIdAndDate(tenantId, date);
        return holiday != null ? convertToDto(holiday) : null;
    }
    
    public boolean isHoliday(String tenantId, LocalDate date) {
        return holidayRepository.isHoliday(tenantId, date);
    }
    
    public HolidayDto createHoliday(HolidayDto holidayDto) {
        // Check if holiday already exists for this tenant and date
        if (holidayRepository.findByTenantIdAndDate(holidayDto.getTenantId(), holidayDto.getHolidayDate()) != null) {
            throw new IllegalArgumentException("Holiday already exists for tenant " + holidayDto.getTenantId() + 
                                             " and date " + holidayDto.getHolidayDate());
        }
        
        Holiday holiday = convertToEntity(holidayDto);
        holiday.setCreatedAt(LocalDateTime.now());
        holiday.setCreatedBy("system"); // TODO: Get from security context
        
        Holiday savedHoliday = holidayRepository.save(holiday);
        return convertToDto(savedHoliday);
    }
    
    public HolidayDto updateHoliday(Long id, HolidayDto holidayDto) {
        Holiday existingHoliday = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found with id: " + id));
        
        // Check if holiday already exists for this tenant and date (excluding current holiday)
        Holiday existingHolidayForDate = holidayRepository.findByTenantIdAndDate(holidayDto.getTenantId(), holidayDto.getHolidayDate());
        if (existingHolidayForDate != null && !existingHolidayForDate.getId().equals(id)) {
            throw new IllegalArgumentException("Holiday already exists for tenant " + holidayDto.getTenantId() + 
                                             " and date " + holidayDto.getHolidayDate());
        }
        
        existingHoliday.setTenantId(holidayDto.getTenantId());
        existingHoliday.setHolidayDate(holidayDto.getHolidayDate());
        existingHoliday.setHolidayName(holidayDto.getHolidayName());
        existingHoliday.setDescription(holidayDto.getDescription());
        existingHoliday.setUpdatedAt(LocalDateTime.now());
        existingHoliday.setUpdatedBy("system"); // TODO: Get from security context
        
        Holiday savedHoliday = holidayRepository.save(existingHoliday);
        return convertToDto(savedHoliday);
    }
    
    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday not found with id: " + id);
        }
        holidayRepository.deleteById(id);
    }
    
    public void deleteHolidayByTenantIdAndDate(String tenantId, LocalDate holidayDate) {
        holidayRepository.deleteByTenantIdAndHolidayDate(tenantId, holidayDate);
    }
    
    public List<HolidayDto> searchHolidays(String tenantId, String searchTerm) {
        return holidayRepository.searchHolidays(tenantId, searchTerm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private HolidayDto convertToDto(Holiday holiday) {
        HolidayDto dto = new HolidayDto();
        dto.setId(holiday.getId());
        dto.setTenantId(holiday.getTenantId());
        dto.setHolidayDate(holiday.getHolidayDate());
        dto.setHolidayName(holiday.getHolidayName());
        dto.setDescription(holiday.getDescription());
        dto.setCreatedAt(holiday.getCreatedAt());
        dto.setUpdatedAt(holiday.getUpdatedAt());
        dto.setCreatedBy(holiday.getCreatedBy());
        dto.setUpdatedBy(holiday.getUpdatedBy());
        return dto;
    }
    
    private Holiday convertToEntity(HolidayDto dto) {
        Holiday holiday = new Holiday();
        holiday.setId(dto.getId());
        holiday.setTenantId(dto.getTenantId());
        holiday.setHolidayDate(dto.getHolidayDate());
        holiday.setHolidayName(dto.getHolidayName());
        holiday.setDescription(dto.getDescription());
        holiday.setCreatedAt(dto.getCreatedAt());
        holiday.setUpdatedAt(dto.getUpdatedAt());
        holiday.setCreatedBy(dto.getCreatedBy());
        holiday.setUpdatedBy(dto.getUpdatedBy());
        return holiday;
    }
}