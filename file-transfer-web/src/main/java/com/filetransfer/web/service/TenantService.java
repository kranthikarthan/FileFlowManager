package com.filetransfer.web.service;

import com.filetransfer.web.dto.TenantDto;
import com.filetransfer.web.entity.Tenant;
import com.filetransfer.web.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TenantService {
    
    @Autowired
    private TenantRepository tenantRepository;
    
    public List<TenantDto> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<TenantDto> getActiveTenants() {
        return tenantRepository.findByEnabled(true).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<TenantDto> getTenantById(Long id) {
        return tenantRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public Optional<TenantDto> getTenantByTenantId(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(this::convertToDto);
    }
    
    public Optional<TenantDto> getActiveTenantByTenantId(String tenantId) {
        return tenantRepository.findActiveTenantByTenantId(tenantId)
                .map(this::convertToDto);
    }
    
    public TenantDto createTenant(TenantDto tenantDto) {
        if (tenantRepository.existsByTenantId(tenantDto.getTenantId())) {
            throw new IllegalArgumentException("Tenant ID already exists: " + tenantDto.getTenantId());
        }
        
        Tenant tenant = convertToEntity(tenantDto);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setCreatedBy("system"); // TODO: Get from security context
        
        Tenant savedTenant = tenantRepository.save(tenant);
        return convertToDto(savedTenant);
    }
    
    public TenantDto updateTenant(Long id, TenantDto tenantDto) {
        Tenant existingTenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + id));
        
        // Check if tenant ID is being changed and if it already exists
        if (!existingTenant.getTenantId().equals(tenantDto.getTenantId()) && 
            tenantRepository.existsByTenantId(tenantDto.getTenantId())) {
            throw new IllegalArgumentException("Tenant ID already exists: " + tenantDto.getTenantId());
        }
        
        existingTenant.setTenantId(tenantDto.getTenantId());
        existingTenant.setTenantName(tenantDto.getTenantName());
        existingTenant.setTimezone(tenantDto.getTimezone());
        existingTenant.setEnabled(tenantDto.getEnabled());
        existingTenant.setUpdatedAt(LocalDateTime.now());
        existingTenant.setUpdatedBy("system"); // TODO: Get from security context
        
        Tenant savedTenant = tenantRepository.save(existingTenant);
        return convertToDto(savedTenant);
    }
    
    public void deleteTenant(Long id) {
        if (!tenantRepository.existsById(id)) {
            throw new IllegalArgumentException("Tenant not found with id: " + id);
        }
        tenantRepository.deleteById(id);
    }
    
    public List<TenantDto> searchTenants(String searchTerm) {
        return tenantRepository.searchTenants(searchTerm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public boolean existsByTenantId(String tenantId) {
        return tenantRepository.existsByTenantId(tenantId);
    }
    
    private TenantDto convertToDto(Tenant tenant) {
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setTenantId(tenant.getTenantId());
        dto.setTenantName(tenant.getTenantName());
        dto.setTimezone(tenant.getTimezone());
        dto.setEnabled(tenant.getEnabled());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        dto.setCreatedBy(tenant.getCreatedBy());
        dto.setUpdatedBy(tenant.getUpdatedBy());
        return dto;
    }
    
    private Tenant convertToEntity(TenantDto dto) {
        Tenant tenant = new Tenant();
        tenant.setId(dto.getId());
        tenant.setTenantId(dto.getTenantId());
        tenant.setTenantName(dto.getTenantName());
        tenant.setTimezone(dto.getTimezone());
        tenant.setEnabled(dto.getEnabled());
        tenant.setCreatedAt(dto.getCreatedAt());
        tenant.setUpdatedAt(dto.getUpdatedAt());
        tenant.setCreatedBy(dto.getCreatedBy());
        tenant.setUpdatedBy(dto.getUpdatedBy());
        return tenant;
    }
}