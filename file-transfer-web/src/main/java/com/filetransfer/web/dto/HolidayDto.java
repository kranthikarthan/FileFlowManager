package com.filetransfer.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class HolidayDto {
    
    private Long id;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;
    
    @NotBlank(message = "Holiday name is required")
    private String holidayName;
    
    private String description;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Constructors
    public HolidayDto() {}
    
    public HolidayDto(String tenantId, LocalDate holidayDate, String holidayName, String description) {
        this.tenantId = tenantId;
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public LocalDate getHolidayDate() {
        return holidayDate;
    }
    
    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }
    
    public String getHolidayName() {
        return holidayName;
    }
    
    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}