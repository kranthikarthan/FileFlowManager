package com.filetransfer.web.controller;

import com.filetransfer.web.dto.AlertConfigurationDto;
import com.filetransfer.web.dto.AlertHistoryDto;
import com.filetransfer.web.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {
    
    @Autowired
    private AlertService alertService;
    
    // Alert Configuration endpoints
    @GetMapping("/configurations/tenant/{tenantId}")
    public ResponseEntity<List<AlertConfigurationDto>> getAlertConfigurationsForTenant(@PathVariable String tenantId) {
        List<AlertConfigurationDto> configurations = alertService.getAlertConfigurationsForTenant(tenantId);
        return ResponseEntity.ok(configurations);
    }
    
    @GetMapping("/configurations/tenant/{tenantId}/active")
    public ResponseEntity<List<AlertConfigurationDto>> getActiveAlertConfigurationsForTenant(@PathVariable String tenantId) {
        List<AlertConfigurationDto> configurations = alertService.getActiveAlertConfigurationsForTenant(tenantId);
        return ResponseEntity.ok(configurations);
    }
    
    @GetMapping("/configurations/service")
    public ResponseEntity<List<AlertConfigurationDto>> getAlertConfigurationsForService(
            @RequestParam String tenantId,
            @RequestParam String serviceName,
            @RequestParam(required = false) String subServiceName) {
        List<AlertConfigurationDto> configurations = alertService.getAlertConfigurationsForService(tenantId, serviceName, subServiceName);
        return ResponseEntity.ok(configurations);
    }
    
    @GetMapping("/configurations/{id}")
    public ResponseEntity<AlertConfigurationDto> getAlertConfigurationById(@PathVariable Long id) {
        return alertService.getAlertConfigurationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/configurations")
    public ResponseEntity<AlertConfigurationDto> createAlertConfiguration(@Valid @RequestBody AlertConfigurationDto alertConfigDto) {
        try {
            AlertConfigurationDto createdConfig = alertService.createAlertConfiguration(alertConfigDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/configurations/{id}")
    public ResponseEntity<AlertConfigurationDto> updateAlertConfiguration(@PathVariable Long id, @Valid @RequestBody AlertConfigurationDto alertConfigDto) {
        try {
            AlertConfigurationDto updatedConfig = alertService.updateAlertConfiguration(id, alertConfigDto);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/configurations/{id}")
    public ResponseEntity<Void> deleteAlertConfiguration(@PathVariable Long id) {
        try {
            alertService.deleteAlertConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Alert History endpoints
    @GetMapping("/history/tenant/{tenantId}")
    public ResponseEntity<List<AlertHistoryDto>> getAlertHistoryForTenant(@PathVariable String tenantId) {
        List<AlertHistoryDto> history = alertService.getAlertHistoryForTenant(tenantId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/history/service")
    public ResponseEntity<List<AlertHistoryDto>> getAlertHistoryForService(
            @RequestParam String tenantId,
            @RequestParam String serviceName,
            @RequestParam(required = false) String subServiceName) {
        List<AlertHistoryDto> history = alertService.getAlertHistoryForService(tenantId, serviceName, subServiceName);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/history/tenant/{tenantId}/unacknowledged")
    public ResponseEntity<List<AlertHistoryDto>> getUnacknowledgedAlerts(@PathVariable String tenantId) {
        List<AlertHistoryDto> alerts = alertService.getUnacknowledgedAlerts(tenantId);
        return ResponseEntity.ok(alerts);
    }
    
    @PostMapping("/history")
    public ResponseEntity<AlertHistoryDto> createAlertHistory(@Valid @RequestBody AlertHistoryDto alertHistoryDto) {
        try {
            AlertHistoryDto createdHistory = alertService.createAlertHistory(alertHistoryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdHistory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/history/{id}/acknowledge")
    public ResponseEntity<AlertHistoryDto> acknowledgeAlert(
            @PathVariable Long id, 
            @RequestParam String acknowledgedBy) {
        try {
            AlertHistoryDto acknowledgedAlert = alertService.acknowledgeAlert(id, acknowledgedBy);
            return ResponseEntity.ok(acknowledgedAlert);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Utility endpoints
    @PostMapping("/send-cutoff-alert")
    public ResponseEntity<Void> sendCutOffAlert(
            @RequestParam String tenantId,
            @RequestParam String serviceName,
            @RequestParam(required = false) String subServiceName,
            @RequestParam String message) {
        alertService.sendCutOffAlert(tenantId, serviceName, subServiceName, message);
        return ResponseEntity.ok().build();
    }
}