package com.enterprise.fileflow.shared.dto;

import com.enterprise.fileflow.shared.enums.FileStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for File Transaction data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTransactionDto {

    private Long id;
    private String transactionId;
    private String serviceCode;
    private String subServiceCode;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private FileStatus status;
    private String batchId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
    
    private String movedToPath;
    private String validationResult;
    private String validationErrors;
    private Integer retryCount;
    private String errorMessage;
    private String notes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String createdBy;
    private String updatedBy;
}