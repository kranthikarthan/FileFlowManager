package com.filetransfer.web.repository;

import com.filetransfer.web.entity.ServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceConfigurationRepository extends JpaRepository<ServiceConfiguration, Long> {
    
    @Deprecated
    Optional<ServiceConfiguration> findByServiceName(String serviceName);
    
    List<ServiceConfiguration> findByTenantId(String tenantId);
    
    List<ServiceConfiguration> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    
    List<ServiceConfiguration> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    List<ServiceConfiguration> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    @Query("SELECT s FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.enabled = true ORDER BY s.serviceName, s.subServiceName")
    List<ServiceConfiguration> findAllEnabledServicesForTenant(@org.springframework.data.repository.query.Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(s) FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.enabled = true")
    long countEnabledServicesForTenant(@org.springframework.data.repository.query.Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT s.serviceName FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.enabled = true")
    List<String> findDistinctServiceNamesForTenant(@org.springframework.data.repository.query.Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT s.subServiceName FROM ServiceConfiguration s WHERE s.tenantId = :tenantId AND s.serviceName = :serviceName AND s.enabled = true AND s.subServiceName IS NOT NULL")
    List<String> findSubServicesForService(@org.springframework.data.repository.query.Param("tenantId") String tenantId, 
                                          @org.springframework.data.repository.query.Param("serviceName") String serviceName);
    
    boolean existsByServiceNameAndSubServiceNameAndTenantId(String serviceName, String subServiceName, String tenantId);
}