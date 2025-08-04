package com.filetransfer.web.controller;

import com.filetransfer.web.dto.TenantDto;
import com.filetransfer.web.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = "*")
public class TenantController {
    
    @Autowired
    private TenantService tenantService;
    
    @GetMapping
    public ResponseEntity<List<TenantDto>> getAllTenants() {
        List<TenantDto> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<TenantDto>> getActiveTenants() {
        List<TenantDto> tenants = tenantService.getActiveTenants();
        return ResponseEntity.ok(tenants);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TenantDto> getTenantById(@PathVariable Long id) {
        return tenantService.getTenantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-tenant-id/{tenantId}")
    public ResponseEntity<TenantDto> getTenantByTenantId(@PathVariable String tenantId) {
        return tenantService.getTenantByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/active/{tenantId}")
    public ResponseEntity<TenantDto> getActiveTenantByTenantId(@PathVariable String tenantId) {
        return tenantService.getActiveTenantByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody TenantDto tenantDto) {
        try {
            TenantDto createdTenant = tenantService.createTenant(tenantDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TenantDto> updateTenant(@PathVariable Long id, @Valid @RequestBody TenantDto tenantDto) {
        try {
            TenantDto updatedTenant = tenantService.updateTenant(id, tenantDto);
            return ResponseEntity.ok(updatedTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        try {
            tenantService.deleteTenant(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<TenantDto>> searchTenants(@RequestParam String searchTerm) {
        List<TenantDto> tenants = tenantService.searchTenants(searchTerm);
        return ResponseEntity.ok(tenants);
    }
    
    @GetMapping("/exists/{tenantId}")
    public ResponseEntity<Boolean> checkTenantExists(@PathVariable String tenantId) {
        boolean exists = tenantService.existsByTenantId(tenantId);
        return ResponseEntity.ok(exists);
    }
}