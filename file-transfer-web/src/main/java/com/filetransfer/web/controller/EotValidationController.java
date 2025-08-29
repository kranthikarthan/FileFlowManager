package com.filetransfer.web.controller;

import com.filetransfer.web.entity.DailyFileCountTracker;
import com.filetransfer.web.service.EotValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for EOT validation and file count tracking.
 */
@RestController
@RequestMapping("/api/eot-validation")
@CrossOrigin(origins = "http://localhost:3000")
public class EotValidationController {
    
    @Autowired
    private EotValidationService eotValidationService;
    
    /**
     * Get validation results for a date range
     */
    @GetMapping("/tenant/{tenantId}/results")
    public ResponseEntity<List<DailyFileCountTracker>> getValidationResults(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyFileCountTracker> results = eotValidationService.getValidationResults(tenantId, startDate, endDate);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get pending validations (missing EOT files)
     */
    @GetMapping("/tenant/{tenantId}/pending")
    public ResponseEntity<List<DailyFileCountTracker>> getPendingValidations(
            @PathVariable String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoffDate) {
        
        if (cutoffDate == null) {
            cutoffDate = LocalDate.now().minusDays(1); // Default to yesterday
        }
        
        List<DailyFileCountTracker> pending = eotValidationService.getPendingValidations(tenantId, cutoffDate);
        return ResponseEntity.ok(pending);
    }
    
    /**
     * Get count discrepancies
     */
    @GetMapping("/tenant/{tenantId}/discrepancies")
    public ResponseEntity<List<DailyFileCountTracker>> getDiscrepancies(@PathVariable String tenantId) {
        List<DailyFileCountTracker> discrepancies = eotValidationService.getDiscrepancies(tenantId);
        return ResponseEntity.ok(discrepancies);
    }
    
    /**
     * Get validation statistics
     */
    @GetMapping("/tenant/{tenantId}/statistics")
    public ResponseEntity<EotValidationService.ValidationStatistics> getValidationStatistics(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        EotValidationService.ValidationStatistics stats = eotValidationService.getValidationStatistics(tenantId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get validation dashboard data
     */
    @GetMapping("/tenant/{tenantId}/dashboard")
    public ResponseEntity<Map<String, Object>> getValidationDashboard(@PathVariable String tenantId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30); // Last 30 days
        
        EotValidationService.ValidationStatistics stats = eotValidationService.getValidationStatistics(tenantId, startDate, endDate);
        List<DailyFileCountTracker> recentDiscrepancies = eotValidationService.getDiscrepancies(tenantId);
        List<DailyFileCountTracker> pendingValidations = eotValidationService.getPendingValidations(tenantId, LocalDate.now().minusDays(1));
        
        Map<String, Object> dashboard = Map.of(
            "statistics", stats,
            "recentDiscrepancies", recentDiscrepancies.stream().limit(10).toList(),
            "pendingValidations", pendingValidations.stream().limit(10).toList(),
            "dateRange", Map.of("startDate", startDate, "endDate", endDate)
        );
        
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * Get validation details for specific subservice and date
     */
    @GetMapping("/tenant/{tenantId}/service/{serviceName}/subservice/{subServiceName}")
    public ResponseEntity<List<DailyFileCountTracker>> getSubServiceValidation(
            @PathVariable String tenantId,
            @PathVariable String serviceName,
            @PathVariable String subServiceName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyFileCountTracker> results = eotValidationService.getValidationResults(tenantId, startDate, endDate)
                .stream()
                .filter(tracker -> tracker.getServiceName().equals(serviceName))
                .filter(tracker -> tracker.getSubServiceName().equals(subServiceName))
                .toList();
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Force revalidation for a specific tracker
     */
    @PostMapping("/tracker/{trackerId}/revalidate")
    public ResponseEntity<?> forceRevalidation(@PathVariable Long trackerId) {
        try {
            // This would trigger a revalidation of the specific tracker
            // Implementation would depend on specific business requirements
            return ResponseEntity.ok(Map.of("message", "Revalidation triggered"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get validation trends (for charts)
     */
    @GetMapping("/tenant/{tenantId}/trends")
    public ResponseEntity<Map<String, Object>> getValidationTrends(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyFileCountTracker> results = eotValidationService.getValidationResults(tenantId, startDate, endDate);
        
        // Group by date for trend analysis
        Map<LocalDate, Long> matchedTrend = results.stream()
                .filter(tracker -> tracker.getValidationStatus() == DailyFileCountTracker.ValidationStatus.MATCHED)
                .collect(java.util.stream.Collectors.groupingBy(
                    DailyFileCountTracker::getProcessingDate,
                    java.util.stream.Collectors.counting()
                ));
        
        Map<LocalDate, Long> discrepancyTrend = results.stream()
                .filter(tracker -> tracker.getValidationStatus() == DailyFileCountTracker.ValidationStatus.DISCREPANCY)
                .collect(java.util.stream.Collectors.groupingBy(
                    DailyFileCountTracker::getProcessingDate,
                    java.util.stream.Collectors.counting()
                ));
        
        Map<String, Object> trends = Map.of(
            "matchedTrend", matchedTrend,
            "discrepancyTrend", discrepancyTrend,
            "dateRange", Map.of("startDate", startDate, "endDate", endDate)
        );
        
        return ResponseEntity.ok(trends);
    }
    
    /**
     * Export validation report
     */
    @GetMapping("/tenant/{tenantId}/export")
    public ResponseEntity<Map<String, Object>> exportValidationReport(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "json") String format) {
        
        List<DailyFileCountTracker> results = eotValidationService.getValidationResults(tenantId, startDate, endDate);
        EotValidationService.ValidationStatistics stats = eotValidationService.getValidationStatistics(tenantId, startDate, endDate);
        
        Map<String, Object> report = Map.of(
            "reportDate", LocalDate.now(),
            "dateRange", Map.of("startDate", startDate, "endDate", endDate),
            "statistics", stats,
            "details", results,
            "format", format
        );
        
        return ResponseEntity.ok(report);
    }
}