package com.filetransfer.batch.repository;

import com.filetransfer.batch.entity.ServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceConfigurationRepository extends JpaRepository<ServiceConfiguration, Long> {
    
    List<ServiceConfiguration> findByTenantId(String tenantId);
    
    List<ServiceConfiguration> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    
    List<ServiceConfiguration> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    Optional<ServiceConfiguration> findByTenantIdAndServiceNameAndSubServiceName(
        String tenantId, String serviceName, String subServiceName);
    
    @Query("SELECT DISTINCT s.tenantId FROM ServiceConfiguration s WHERE s.enabled = true")
    List<String> findAllActiveTenantIds();
    
    @Query("SELECT s FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.enabled = true")
    List<ServiceConfiguration> findAllEnabledServicesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT s FROM ServiceConfiguration s WHERE s.enabled = true")
    List<ServiceConfiguration> findAllEnabledServices();
    
    @Query("SELECT DISTINCT s.serviceName FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.enabled = true")
    List<String> findDistinctServiceNamesForTenant(@Param("tenantId") String tenantId);
    
    boolean existsByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    @Deprecated
    List<ServiceConfiguration> findByEnabledTrue();
}