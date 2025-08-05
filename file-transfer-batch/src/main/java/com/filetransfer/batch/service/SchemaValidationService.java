package com.filetransfer.batch.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;

@Service
public class SchemaValidationService {
    
    @Value("${filetransfer.web.api.url:http://localhost:8080}")
    private String webApiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public FileProcessingService.ValidationResult validateFile(String tenantId, String serviceType, String fileName, InputStream fileContent, Long fileSize) {
        try {
            // Convert InputStream to byte array
            byte[] fileBytes = fileContent.readAllBytes();
            
            // Create multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("tenantId", tenantId);
            body.add("serviceType", serviceType);
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Call web API for validation
            ResponseEntity<ValidationResponse> response = restTemplate.postForEntity(
                webApiUrl + "/api/schemas/validate-file",
                requestEntity,
                ValidationResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ValidationResponse validationResponse = response.getBody();
                return new FileProcessingService.ValidationResult(
                    validationResponse.isValid(),
                    validationResponse.getMessage()
                );
            } else {
                return new FileProcessingService.ValidationResult(false, "Validation service unavailable");
            }
            
        } catch (Exception e) {
            return new FileProcessingService.ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }
    
    // Response class for validation API
    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private String fileName;
        private Long fileSize;
        private String tenantId;
        private String serviceType;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    }
}