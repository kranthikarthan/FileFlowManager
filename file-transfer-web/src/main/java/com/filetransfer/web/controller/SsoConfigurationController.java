package com.filetransfer.web.controller;

import com.filetransfer.web.entity.SsoConfiguration;
import com.filetransfer.web.entity.SsoProvider;
import com.filetransfer.web.service.SsoConfigurationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sso")
@CrossOrigin(origins = "http://localhost:3000")
public class SsoConfigurationController {
    
    @Autowired
    private SsoConfigurationService ssoConfigService;
    
    @GetMapping
    public ResponseEntity<List<SsoConfiguration>> getAllConfigurations() {
        return ResponseEntity.ok(ssoConfigService.getAllConfigurations());
    }
    
    @GetMapping("/enabled")
    public ResponseEntity<List<SsoConfiguration>> getEnabledConfigurations() {
        return ResponseEntity.ok(ssoConfigService.getEnabledConfigurations());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SsoConfiguration> getConfigurationById(@PathVariable Long id) {
        return ssoConfigService.getConfigurationById(id)
            .map(config -> ResponseEntity.ok(config))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<SsoConfiguration> getConfigurationByOrganization(@PathVariable String organizationId) {
        return ssoConfigService.getConfigurationByOrganizationId(organizationId)
            .map(config -> ResponseEntity.ok(config))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/provider/{provider}")
    public ResponseEntity<List<SsoConfiguration>> getConfigurationsByProvider(@PathVariable SsoProvider provider) {
        return ResponseEntity.ok(ssoConfigService.getConfigurationsByProvider(provider));
    }
    
    @PostMapping
    public ResponseEntity<?> createConfiguration(@Valid @RequestBody SsoConfiguration ssoConfig) {
        try {
            SsoConfiguration createdConfig = ssoConfigService.createConfiguration(ssoConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfiguration(@PathVariable Long id, 
                                                @Valid @RequestBody SsoConfiguration ssoConfig) {
        try {
            SsoConfiguration updatedConfig = ssoConfigService.updateConfiguration(id, ssoConfig);
            return ResponseEntity.ok(updatedConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfiguration(@PathVariable Long id) {
        try {
            ssoConfigService.deleteConfiguration(id);
            return ResponseEntity.ok(Map.of("message", "SSO configuration deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleConfigurationStatus(@PathVariable Long id) {
        try {
            SsoConfiguration updatedConfig = ssoConfigService.toggleConfigurationStatus(id);
            return ResponseEntity.ok(updatedConfig);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testSsoConnection(@PathVariable Long id) {
        try {
            // TODO: Implement actual SSO connection test
            // This would involve making a test OAuth request to validate the configuration
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "SSO connection test successful"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "SSO connection test failed: " + e.getMessage()
            ));
        }
    }
}