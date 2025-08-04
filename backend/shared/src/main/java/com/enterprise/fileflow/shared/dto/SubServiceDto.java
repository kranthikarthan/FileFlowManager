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
 * DTO for SubService data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubServiceDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String serviceCode;
    private ServiceStatus status;
    
    // Override fields
    private String inputFolderPathOverride;
    private String outputFolderPathOverride;
    private String fileNamePrefixOverride;
    private String sotPatternOverride;
    private String eotPatternOverride;
    private String dataFilePatternOverride;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime cutoffTimeOverride;
    
    private Integer alertThresholdMinutesOverride;
    private List<ValidationRuleDto> validationRules;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
}