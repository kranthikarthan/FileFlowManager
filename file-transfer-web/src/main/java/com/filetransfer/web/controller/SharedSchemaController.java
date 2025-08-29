package com.filetransfer.web.controller;

import com.filetransfer.web.entity.SharedSchema;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.service.SharedSchemaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing shared schemas that can be reused across subservices.
 */
@RestController
@RequestMapping("/api/shared-schemas")
@CrossOrigin(origins = "http://localhost:3000")
public class SharedSchemaController {
    
    @Autowired
    private SharedSchemaService sharedSchemaService;
    
    /**
     * Get all schemas available to a tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<SharedSchema>> getAvailableSchemas(@PathVariable String tenantId) {
        List<SharedSchema> schemas = sharedSchemaService.getAvailableSchemasForTenant(tenantId);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Get schemas by file type
     */
    @GetMapping("/tenant/{tenantId}/file-type/{fileType}")
    public ResponseEntity<List<SharedSchema>> getSchemasByFileType(@PathVariable String tenantId, 
                                                                  @PathVariable FileType fileType) {
        List<SharedSchema> schemas = sharedSchemaService.getSchemasByFileType(tenantId, fileType);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Get schemas that support count validation (for EOT)
     */
    @GetMapping("/tenant/{tenantId}/count-validation")
    public ResponseEntity<List<SharedSchema>> getCountValidationSchemas(@PathVariable String tenantId) {
        List<SharedSchema> schemas = sharedSchemaService.getCountValidationSchemas(tenantId);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Search schemas
     */
    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<List<SharedSchema>> searchSchemas(@PathVariable String tenantId, 
                                                           @RequestParam String query) {
        List<SharedSchema> schemas = sharedSchemaService.searchSchemas(tenantId, query);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Get most popular schemas
     */
    @GetMapping("/tenant/{tenantId}/popular")
    public ResponseEntity<List<SharedSchema>> getMostPopularSchemas(@PathVariable String tenantId) {
        List<SharedSchema> schemas = sharedSchemaService.getMostPopularSchemas(tenantId);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Get specific schema by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SharedSchema> getSchema(@PathVariable Long id) {
        Optional<SharedSchema> schema = sharedSchemaService.getSchemaById(id);
        return schema.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Create new shared schema
     */
    @PostMapping
    public ResponseEntity<?> createSchema(@Valid @RequestBody SharedSchema schema) {
        try {
            SharedSchema createdSchema = sharedSchemaService.createSharedSchema(schema);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSchema);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create schema: " + e.getMessage()));
        }
    }
    
    /**
     * Update existing schema
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchema(@PathVariable Long id, 
                                         @Valid @RequestBody SharedSchema schema) {
        try {
            SharedSchema updatedSchema = sharedSchemaService.updateSharedSchema(id, schema);
            return ResponseEntity.ok(updatedSchema);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update schema: " + e.getMessage()));
        }
    }
    
    /**
     * Delete schema
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchema(@PathVariable Long id) {
        try {
            sharedSchemaService.deleteSharedSchema(id);
            return ResponseEntity.ok(Map.of("message", "Schema deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage(), "type", "SCHEMA_IN_USE"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete schema: " + e.getMessage()));
        }
    }
    
    /**
     * Create new version of existing schema
     */
    @PostMapping("/{id}/version")
    public ResponseEntity<?> createSchemaVersion(@PathVariable Long id,
                                                @RequestParam String version,
                                                @RequestBody String schemaDefinition) {
        try {
            SharedSchema newVersion = sharedSchemaService.createSchemaVersion(id, version, schemaDefinition);
            return ResponseEntity.status(HttpStatus.CREATED).body(newVersion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create schema version: " + e.getMessage()));
        }
    }
    
    /**
     * Clone schema for different tenant
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<?> cloneSchema(@PathVariable Long id,
                                        @RequestParam String targetTenantId,
                                        @RequestParam String newName) {
        try {
            SharedSchema clonedSchema = sharedSchemaService.cloneSchema(id, targetTenantId, newName);
            return ResponseEntity.status(HttpStatus.CREATED).body(clonedSchema);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clone schema: " + e.getMessage()));
        }
    }
    
    /**
     * Get all versions of a schema
     */
    @GetMapping("/tenant/{tenantId}/schema/{schemaName}/versions")
    public ResponseEntity<List<SharedSchema>> getSchemaVersions(@PathVariable String tenantId,
                                                               @PathVariable String schemaName) {
        List<SharedSchema> versions = sharedSchemaService.getSchemaVersions(tenantId, schemaName);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * Get usage statistics
     */
    @GetMapping("/tenant/{tenantId}/statistics")
    public ResponseEntity<Map<FileType, Long>> getUsageStatistics(@PathVariable String tenantId) {
        Map<FileType, Long> stats = sharedSchemaService.getUsageStatisticsByFileType(tenantId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get schemas by tags
     */
    @GetMapping("/tenant/{tenantId}/tags")
    public ResponseEntity<List<SharedSchema>> getSchemasByTags(@PathVariable String tenantId,
                                                              @RequestParam List<String> tags) {
        List<SharedSchema> schemas = sharedSchemaService.getSchemasByTags(tenantId, tags);
        return ResponseEntity.ok(schemas);
    }
    
    /**
     * Record schema usage (for analytics)
     */
    @PostMapping("/{id}/usage")
    public ResponseEntity<?> recordUsage(@PathVariable Long id) {
        try {
            sharedSchemaService.recordSchemaUsage(id);
            return ResponseEntity.ok(Map.of("message", "Usage recorded"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to record usage: " + e.getMessage()));
        }
    }
}