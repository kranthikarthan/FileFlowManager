package com.enterprise.fileflow.webapi.controller;

import com.enterprise.fileflow.shared.dto.ServiceDto;
import com.enterprise.fileflow.webapi.service.ServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Service management operations
 */
@RestController
@RequestMapping("/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service management operations")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    @GetMapping
    @Operation(summary = "Get all services", description = "Retrieve paginated list of services")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ServiceDto>> getAllServices(
            @RequestParam(required = false) String tenantCode,
            Pageable pageable) {
        return ResponseEntity.ok(serviceManagementService.getAllServices(tenantCode, pageable));
    }

    @GetMapping("/{serviceCode}")
    @Operation(summary = "Get service by code", description = "Retrieve service details by service code")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ServiceDto> getServiceByCode(@PathVariable String serviceCode) {
        return ResponseEntity.ok(serviceManagementService.getServiceByCode(serviceCode));
    }

    @PostMapping
    @Operation(summary = "Create service", description = "Create a new service")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> createService(@Valid @RequestBody ServiceDto serviceDto) {
        return ResponseEntity.ok(serviceManagementService.createService(serviceDto));
    }

    @PutMapping("/{serviceCode}")
    @Operation(summary = "Update service", description = "Update an existing service")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable String serviceCode,
            @Valid @RequestBody ServiceDto serviceDto) {
        return ResponseEntity.ok(serviceManagementService.updateService(serviceCode, serviceDto));
    }

    @DeleteMapping("/{serviceCode}")
    @Operation(summary = "Delete service", description = "Delete a service")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable String serviceCode) {
        serviceManagementService.deleteService(serviceCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{serviceCode}/close")
    @Operation(summary = "Close service", description = "Close service for the day")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> closeService(@PathVariable String serviceCode) {
        serviceManagementService.closeService(serviceCode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{serviceCode}/reopen")
    @Operation(summary = "Reopen service", description = "Reopen a closed service")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> reopenService(@PathVariable String serviceCode) {
        serviceManagementService.reopenService(serviceCode);
        return ResponseEntity.ok().build();
    }
}