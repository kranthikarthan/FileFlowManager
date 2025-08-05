package com.filetransfer.web.controller;

import com.filetransfer.web.dto.FileSchemaDto;
import com.filetransfer.web.dto.SchemaValidationRuleDto;
import com.filetransfer.web.dto.SchemaFieldDto;
import com.filetransfer.web.service.FileSchemaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schemas")
public class FileSchemaController {
    
    @Autowired
    private FileSchemaService fileSchemaService;
    
    // Schema Management Endpoints
    
    @PostMapping
    public ResponseEntity<FileSchemaDto> createSchema(@Valid @RequestBody FileSchemaDto schemaDto,
                                                    @RequestParam String createdBy) {
        FileSchemaDto createdSchema = fileSchemaService.createSchema(schemaDto, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSchema);
    }
    
    @PutMapping("/{schemaId}")
    public ResponseEntity<FileSchemaDto> updateSchema(@PathVariable Long schemaId,
                                                    @Valid @RequestBody FileSchemaDto schemaDto,
                                                    @RequestParam String updatedBy) {
        FileSchemaDto updatedSchema = fileSchemaService.updateSchema(schemaId, schemaDto, updatedBy);
        return ResponseEntity.ok(updatedSchema);
    }
    
    @DeleteMapping("/{schemaId}")
    public ResponseEntity<Void> deleteSchema(@PathVariable Long schemaId) {
        fileSchemaService.deleteSchema(schemaId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{schemaId}")
    public ResponseEntity<FileSchemaDto> getSchemaById(@PathVariable Long schemaId) {
        FileSchemaDto schema = fileSchemaService.getSchemaById(schemaId);
        return ResponseEntity.ok(schema);
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceType}")
    public ResponseEntity<List<FileSchemaDto>> getSchemasByServiceType(@PathVariable String tenantId,
                                                                      @PathVariable String serviceType) {
        List<FileSchemaDto> schemas = fileSchemaService.getSchemasByServiceType(tenantId, serviceType);
        return ResponseEntity.ok(schemas);
    }
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<FileSchemaDto>> getAllSchemas(@PathVariable String tenantId) {
        List<FileSchemaDto> schemas = fileSchemaService.getAllSchemas(tenantId);
        return ResponseEntity.ok(schemas);
    }
    
    // Validation Rule Management Endpoints
    
    @PostMapping("/{schemaId}/validation-rules")
    public ResponseEntity<SchemaValidationRuleDto> addValidationRule(@PathVariable Long schemaId,
                                                                   @Valid @RequestBody SchemaValidationRuleDto ruleDto) {
        SchemaValidationRuleDto createdRule = fileSchemaService.addValidationRule(schemaId, ruleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
    }
    
    @DeleteMapping("/validation-rules/{ruleId}")
    public ResponseEntity<Void> deleteValidationRule(@PathVariable Long ruleId) {
        fileSchemaService.deleteValidationRule(ruleId);
        return ResponseEntity.noContent().build();
    }
    
    // Field Management Endpoints
    
    @PostMapping("/{schemaId}/fields")
    public ResponseEntity<SchemaFieldDto> addField(@PathVariable Long schemaId,
                                                 @Valid @RequestBody SchemaFieldDto fieldDto) {
        SchemaFieldDto createdField = fileSchemaService.addField(schemaId, fieldDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdField);
    }
    
    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        fileSchemaService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }
    
    // File Validation Endpoints
    
    @PostMapping("/validate-file")
    public ResponseEntity<Map<String, Object>> validateFile(@RequestParam String tenantId,
                                                          @RequestParam String serviceType,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(defaultValue = "false") Boolean binaryFileBypass) {
        try {
            InputStream fileContent = file.getInputStream();
            Long fileSize = file.getSize();
            String fileName = file.getOriginalFilename();
            
            FileSchemaService.ValidationResult result = fileSchemaService.validateFile(tenantId, serviceType, fileName, fileContent, fileSize, binaryFileBypass);
            
            Map<String, Object> response = Map.of(
                "valid", result.isValid(),
                "message", result.getMessage(),
                "fileName", fileName,
                "fileSize", fileSize,
                "tenantId", tenantId,
                "serviceType", serviceType
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            Map<String, Object> errorResponse = Map.of(
                "valid", false,
                "message", "Error reading file: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/validate-file-content")
    public ResponseEntity<Map<String, Object>> validateFileContent(@RequestParam String tenantId,
                                                                 @RequestParam String serviceType,
                                                                 @RequestParam String fileName,
                                                                 @RequestParam Long fileSize,
                                                                 @RequestParam(defaultValue = "false") Boolean binaryFileBypass,
                                                                 @RequestBody String fileContent) {
        try {
            InputStream contentStream = new java.io.ByteArrayInputStream(fileContent.getBytes());
            
            FileSchemaService.ValidationResult result = fileSchemaService.validateFile(tenantId, serviceType, fileName, contentStream, fileSize, binaryFileBypass);
            
            Map<String, Object> response = Map.of(
                "valid", result.isValid(),
                "message", result.getMessage(),
                "fileName", fileName,
                "fileSize", fileSize,
                "tenantId", tenantId,
                "serviceType", serviceType
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "valid", false,
                "message", "Error validating file content: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Schema Template Endpoints
    
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getSchemaTemplates() {
        Map<String, Object> templates = Map.of(
            "CSV", Map.of(
                "type", "CSV",
                "description", "Comma-separated values file schema",
                "example", Map.of(
                    "delimiter", ",",
                    "hasHeader", true,
                    "fields", List.of(
                        Map.of("name", "field1", "type", "STRING", "required", true),
                        Map.of("name", "field2", "type", "INTEGER", "required", false)
                    )
                )
            ),
            "JSON", Map.of(
                "type", "JSON",
                "description", "JSON file schema",
                "example", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "id", Map.of("type", "string"),
                        "name", Map.of("type", "string"),
                        "value", Map.of("type", "number")
                    ),
                    "required", List.of("id", "name")
                )
            ),
            "XML", Map.of(
                "type", "XML",
                "description", "XML file schema",
                "example", Map.of(
                    "root", "data",
                    "elements", List.of(
                        Map.of("name", "item", "type", "element"),
                        Map.of("name", "value", "type", "text")
                    )
                )
            ),
            "FIXED_WIDTH", Map.of(
                "type", "FIXED_WIDTH",
                "description", "Fixed-width file schema",
                "example", Map.of(
                    "fields", List.of(
                        Map.of("name", "field1", "start", 1, "length", 10, "type", "STRING"),
                        Map.of("name", "field2", "start", 11, "length", 5, "type", "INTEGER")
                    )
                )
            )
        );
        
        return ResponseEntity.ok(templates);
    }
}