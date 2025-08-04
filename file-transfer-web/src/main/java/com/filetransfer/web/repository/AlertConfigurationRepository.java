package com.filetransfer.web.repository;

import com.filetransfer.web.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {
    
    List<AlertConfiguration> findByTenantId(String tenantId);
    
    List<AlertConfiguration> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    
    List<AlertConfiguration> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    List<AlertConfiguration> findByTenantIdAndAlertType(String tenantId, AlertConfiguration.AlertType alertType);
    
    @Query("SELECT ac FROM AlertConfiguration ac WHERE ac.tenantId = :tenantId AND ac.serviceName = :serviceName AND ac.subServiceName = :subServiceName AND ac.alertType = :alertType AND ac.enabled = true")
    List<AlertConfiguration> findActiveAlertsForService(@Param("tenantId") String tenantId, 
                                                       @Param("serviceName") String serviceName, 
                                                       @Param("subServiceName") String subServiceName, 
                                                       @Param("alertType") AlertConfiguration.AlertType alertType);
    
    @Query("SELECT ac FROM AlertConfiguration ac WHERE ac.tenantId = :tenantId AND ac.enabled = true")
    List<AlertConfiguration> findActiveAlertsForTenant(@Param("tenantId") String tenantId);
}