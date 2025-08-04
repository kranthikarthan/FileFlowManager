package com.enterprise.fileflow.shared.dto;

import com.enterprise.fileflow.shared.enums.ValidationStrategy;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ValidationRule data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleDto {

    private Long id;
    private String serviceCode;
    private String subServiceCode;
    private ValidationStrategy strategy;
    private String schemaFilePath;
    private String schemaContent;
    private Boolean isMandatory;
    private String filePattern;
    private String description;
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
}