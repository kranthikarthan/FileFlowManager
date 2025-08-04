package com.filetransfer.web.repository;

import com.filetransfer.web.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    Optional<Tenant> findByTenantId(String tenantId);
    
    List<Tenant> findByEnabled(Boolean enabled);
    
    @Query("SELECT t FROM Tenant t WHERE t.tenantId = :tenantId AND t.enabled = true")
    Optional<Tenant> findActiveTenantByTenantId(@Param("tenantId") String tenantId);
    
    boolean existsByTenantId(String tenantId);
    
    @Query("SELECT t FROM Tenant t WHERE t.tenantName LIKE %:searchTerm% OR t.tenantId LIKE %:searchTerm%")
    List<Tenant> searchTenants(@Param("searchTerm") String searchTerm);
}