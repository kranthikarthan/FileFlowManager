package com.filetransfer.web.controller.v2;

import com.filetransfer.web.dto.TenantDto;
import com.filetransfer.web.dto.v2.TenantV2Dto;
import com.filetransfer.web.service.TenantService;
import com.filetransfer.web.versioning.ApiVersion;
import com.filetransfer.web.versioning.VersioningStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * Tenant Controller API Version 2.0+
 * Enhanced version with improved features, pagination, and validation
 */
@RestController
@RequestMapping("/api/v2/tenants")
@CrossOrigin(origins = "*")
@Validated
@ApiVersion(
    value = {"2.0", "2.1"}, 
    strategy = VersioningStrategy.URL_PATH,
    requiresVersion = true
)
public class TenantV2Controller {
    
    @Autowired
    private TenantService tenantService;
    
    /**
     * Get all tenants with pagination and sorting (v2.0+)
     */
    @GetMapping
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Page<TenantV2Dto>> getAllTenants(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TenantV2Dto> tenants = tenantService.getAllTenantsV2(pageable, search, active);
        
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Get tenant by ID with enhanced details (v2.0+)
     */
    @GetMapping("/{id}")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<TenantV2Dto> getTenantById(@PathVariable Long id) {
        TenantV2Dto tenant = tenantService.getTenantByIdV2(id);
        if (tenant != null) {
            return ResponseEntity.ok(tenant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create tenant with enhanced validation (v2.0+)
     */
    @PostMapping
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<TenantV2Dto> createTenant(@Valid @RequestBody TenantV2Dto tenantDto) {
        // In v2.0+, timezone is required and validated
        TenantV2Dto createdTenant = tenantService.createTenantV2(tenantDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
    }
    
    /**
     * Update tenant (v2.0+)
     */
    @PutMapping("/{id}")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<TenantV2Dto> updateTenant(@PathVariable Long id, @Valid @RequestBody TenantV2Dto tenantDto) {
        TenantV2Dto updatedTenant = tenantService.updateTenantV2(id, tenantDto);
        if (updatedTenant != null) {
            return ResponseEntity.ok(updatedTenant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Partial update tenant (v2.0+)
     */
    @PatchMapping("/{id}")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<TenantV2Dto> patchTenant(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        TenantV2Dto updatedTenant = tenantService.patchTenantV2(id, updates);
        if (updatedTenant != null) {
            return ResponseEntity.ok(updatedTenant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete tenant (v2.0+)
     */
    @DeleteMapping("/{id}")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        boolean deleted = tenantService.deleteTenant(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get tenant configuration (v2.0+)
     */
    @GetMapping("/{id}/configuration")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getTenantConfiguration(@PathVariable Long id) {
        Map<String, Object> configuration = tenantService.getTenantConfiguration(id);
        if (configuration != null) {
            return ResponseEntity.ok(configuration);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Update tenant configuration (v2.0+)
     */
    @PutMapping("/{id}/configuration")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> updateTenantConfiguration(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> configuration) {
        
        Map<String, Object> updatedConfiguration = tenantService.updateTenantConfiguration(id, configuration);
        if (updatedConfiguration != null) {
            return ResponseEntity.ok(updatedConfiguration);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get tenant statistics with detailed metrics (v2.0+)
     */
    @GetMapping("/{id}/statistics")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getTenantStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String metrics) {
        
        Map<String, Object> statistics = tenantService.getTenantStatisticsV2(id, period, metrics);
        if (statistics != null) {
            return ResponseEntity.ok(statistics);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get tenant health status (v2.0+)
     */
    @GetMapping("/{id}/health")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> getTenantHealth(@PathVariable Long id) {
        Map<String, Object> health = tenantService.getTenantHealth(id);
        if (health != null) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Bulk operations with transaction support (v2.0+)
     */
    @PostMapping("/bulk")
    @ApiVersion({"2.0", "2.1"})
    public ResponseEntity<Map<String, Object>> bulkOperations(@Valid @RequestBody Map<String, Object> bulkRequest) {
        Map<String, Object> result = tenantService.executeBulkOperations(bulkRequest);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Export tenant data (v2.1+ feature)
     */
    @GetMapping("/{id}/export")
    @ApiVersion(value = {"2.1"}, since = "2.1")
    public ResponseEntity<Map<String, Object>> exportTenantData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String[] includeFields) {
        
        Map<String, Object> exportData = tenantService.exportTenantData(id, format, includeFields);
        if (exportData != null) {
            return ResponseEntity.ok(exportData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Import tenant data (v2.1+ feature)
     */
    @PostMapping("/{id}/import")
    @ApiVersion(value = {"2.1"}, since = "2.1")
    public ResponseEntity<Map<String, Object>> importTenantData(
            @PathVariable Long id,
            @RequestBody Map<String, Object> importData,
            @RequestParam(defaultValue = "merge") String strategy) {
        
        Map<String, Object> result = tenantService.importTenantData(id, importData, strategy);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get tenant audit trail (v2.1+ feature)
     */
    @GetMapping("/{id}/audit")
    @ApiVersion(value = {"2.1"}, since = "2.1")
    public ResponseEntity<Page<Map<String, Object>>> getTenantAuditTrail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String user) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<Map<String, Object>> auditTrail = tenantService.getTenantAuditTrail(id, pageable, action, user);
        
        return ResponseEntity.ok(auditTrail);
    }
}