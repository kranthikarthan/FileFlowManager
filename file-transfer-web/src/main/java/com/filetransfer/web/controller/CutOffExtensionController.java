package com.filetransfer.web.controller;

import com.filetransfer.web.entity.CutOffExtension;
import com.filetransfer.web.service.CutOffExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cutoff-extensions")
@CrossOrigin(origins = "http://localhost:3000")
public class CutOffExtensionController {
    
    @Autowired
    private CutOffExtensionService cutOffExtensionService;
    
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<CutOffExtension>> getExtensionsForTenant(@PathVariable String tenantId) {
        List<CutOffExtension> extensions = cutOffExtensionService.getExtensionsForTenant(tenantId);
        return ResponseEntity.ok(extensions);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<CutOffExtension>> getPendingExtensions() {
        List<CutOffExtension> extensions = cutOffExtensionService.getPendingExtensions();
        return ResponseEntity.ok(extensions);
    }
    
    @PostMapping("/request")
    public ResponseEntity<?> requestExtension(@RequestBody Map<String, Object> request) {
        try {
            String tenantId = (String) request.get("tenantId");
            String serviceName = (String) request.get("serviceName");
            String subServiceName = (String) request.get("subServiceName");
            LocalDate extensionDate = LocalDate.parse((String) request.get("extensionDate"));
            LocalTime extendedCutOffTime = LocalTime.parse((String) request.get("extendedCutOffTime"));
            String reason = (String) request.get("reason");
            
            String priorityStr = (String) request.getOrDefault("priority", "NORMAL");
            CutOffExtension.ExtensionPriority priority = CutOffExtension.ExtensionPriority.valueOf(priorityStr);
            
            CutOffExtension extension = cutOffExtensionService.requestExtension(
                tenantId, serviceName, subServiceName, extensionDate, 
                extendedCutOffTime, reason, priority);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(extension);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to request extension: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveExtension(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String comments = request != null ? request.get("comments") : null;
            CutOffExtension extension = cutOffExtensionService.approveExtension(id, comments);
            return ResponseEntity.ok(extension);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to approve extension: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectExtension(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
            }
            
            CutOffExtension extension = cutOffExtensionService.rejectExtension(id, rejectionReason);
            return ResponseEntity.ok(extension);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reject extension: " + e.getMessage()));
        }
    }
    
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/sub-service/{subServiceName}/effective-cutoff")
    public ResponseEntity<?> getEffectiveCutOffTime(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName,
            @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            LocalTime effectiveTime = cutOffExtensionService.getEffectiveCutOffTime(
                tenantId, serviceName, subServiceName, targetDate, 
                targetDate.atStartOfDay());
            
            return ResponseEntity.ok(Map.of(
                "effectiveCutOffTime", effectiveTime.toString(),
                "date", targetDate.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/activate-today")
    public ResponseEntity<?> activateExtensionsForToday() {
        try {
            cutOffExtensionService.activateApprovedExtensionsForToday();
            return ResponseEntity.ok(Map.of("message", "Extensions activated for today"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to activate extensions: " + e.getMessage()));
        }
    }
    
    @PostMapping("/expire-old")
    public ResponseEntity<?> expireOldExtensions() {
        try {
            cutOffExtensionService.expireOldExtensions();
            return ResponseEntity.ok(Map.of("message", "Old extensions expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to expire extensions: " + e.getMessage()));
        }
    }
    
    @GetMapping("/priorities")
    public ResponseEntity<Map<String, Object>> getExtensionPriorities() {
        Map<String, Object> priorities = Map.of(
            "priorities", CutOffExtension.ExtensionPriority.values(),
            "descriptions", Map.of(
                "LOW", "Low priority - routine extensions",
                "NORMAL", "Normal priority - standard business need",
                "HIGH", "High priority - urgent business requirement",
                "CRITICAL", "Critical priority - system or business critical"
            )
        );
        return ResponseEntity.ok(priorities);
    }
    
    @GetMapping("/statuses")
    public ResponseEntity<Map<String, Object>> getExtensionStatuses() {
        Map<String, Object> statuses = Map.of(
            "statuses", CutOffExtension.ExtensionStatus.values(),
            "descriptions", Map.of(
                "PENDING", "Awaiting approval",
                "APPROVED", "Approved but not yet active",
                "REJECTED", "Rejected by approver",
                "ACTIVE", "Currently active",
                "EXPIRED", "Extension period has ended",
                "CANCELLED", "Cancelled by requestor or system"
            )
        );
        return ResponseEntity.ok(statuses);
    }
}