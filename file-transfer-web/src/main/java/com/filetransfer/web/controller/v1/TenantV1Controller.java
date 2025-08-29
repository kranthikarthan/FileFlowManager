package com.filetransfer.web.controller.v1;

import com.filetransfer.web.dto.TenantDto;
import com.filetransfer.web.service.TenantService;
import com.filetransfer.web.versioning.ApiVersion;
import com.filetransfer.web.versioning.VersioningStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Tenant Controller API Version 1.0 - 1.2
 * Provides backward compatibility for legacy clients
 */
@RestController
@RequestMapping("/api/v1/tenants")
@CrossOrigin(origins = "*")
@ApiVersion(
    value = {"1.0", "1.1", "1.2"}, 
    deprecated = true,
    deprecationMessage = "API v1.x is deprecated. Please migrate to v2.0 for enhanced features and better performance.",
    sunsetDate = "2024-06-01",
    strategy = VersioningStrategy.URL_PATH
)
public class TenantV1Controller {
    
    @Autowired
    private TenantService tenantService;
    
    /**
     * Get all tenants (v1.0+ compatible)
     */
    @GetMapping
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<List<TenantDto>> getAllTenants() {
        List<TenantDto> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Get active tenants (v1.1+ feature)
     */
    @GetMapping("/active")
    @ApiVersion(value = {"1.1", "1.2"}, since = "1.1")
    public ResponseEntity<List<TenantDto>> getActiveTenants() {
        List<TenantDto> tenants = tenantService.getActiveTenants();
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Get tenant by ID (v1.0+ compatible)
     */
    @GetMapping("/{id}")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<TenantDto> getTenantById(@PathVariable Long id) {
        TenantDto tenant = tenantService.getTenantById(id);
        if (tenant != null) {
            return ResponseEntity.ok(tenant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create tenant (v1.0+ compatible)
     * Note: In v1.x, timezone was optional
     */
    @PostMapping
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody TenantDto tenantDto) {
        // For backward compatibility, set default timezone if not provided
        if (tenantDto.getTimezone() == null || tenantDto.getTimezone().isEmpty()) {
            tenantDto.setTimezone("UTC");
        }
        
        TenantDto createdTenant = tenantService.createTenant(tenantDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
    }
    
    /**
     * Update tenant (v1.0+ compatible)
     */
    @PutMapping("/{id}")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<TenantDto> updateTenant(@PathVariable Long id, @Valid @RequestBody TenantDto tenantDto) {
        // Ensure timezone is set for backward compatibility
        if (tenantDto.getTimezone() == null || tenantDto.getTimezone().isEmpty()) {
            tenantDto.setTimezone("UTC");
        }
        
        TenantDto updatedTenant = tenantService.updateTenant(id, tenantDto);
        if (updatedTenant != null) {
            return ResponseEntity.ok(updatedTenant);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete tenant (v1.0+ compatible)
     */
    @DeleteMapping("/{id}")
    @ApiVersion({"1.0", "1.1", "1.2"})
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        boolean deleted = tenantService.deleteTenant(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get tenant statistics (v1.2+ feature)
     */
    @GetMapping("/{id}/stats")
    @ApiVersion(value = {"1.2"}, since = "1.2")
    public ResponseEntity<Map<String, Object>> getTenantStats(@PathVariable Long id) {
        Map<String, Object> stats = tenantService.getTenantStatistics(id);
        if (stats != null) {
            return ResponseEntity.ok(stats);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Bulk create tenants (v1.1+ feature)
     */
    @PostMapping("/bulk")
    @ApiVersion(value = {"1.1", "1.2"}, since = "1.1")
    public ResponseEntity<List<TenantDto>> createTenantsInBulk(@Valid @RequestBody List<TenantDto> tenantDtos) {
        // Apply backward compatibility defaults
        for (TenantDto tenantDto : tenantDtos) {
            if (tenantDto.getTimezone() == null || tenantDto.getTimezone().isEmpty()) {
                tenantDto.setTimezone("UTC");
            }
        }
        
        List<TenantDto> createdTenants = tenantService.createTenantsInBulk(tenantDtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenants);
    }
}