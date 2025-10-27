package com.enterprise.fileflow.shared.dto;

import com.enterprise.fileflow.shared.enums.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Service data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String tenantCode;
    private ServiceStatus status;
    private String inputFolderPath;
    private String outputFolderPath;
    private String fileNamePrefix;
    private String sotPattern;
    private String eotPattern;
    private String dataFilePattern;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime cutoffTime;
    
    private Integer alertThresholdMinutes;
    private List<SubServiceDto> subServices;
    private List<ValidationRuleDto> validationRules;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
}