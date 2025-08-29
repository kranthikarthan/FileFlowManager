package com.filetransfer.web.repository;

import com.filetransfer.web.entity.SubServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubServiceConfigurationRepository extends JpaRepository<SubServiceConfiguration, Long> {
    
    List<SubServiceConfiguration> findByTenantId(String tenantId);
    
    List<SubServiceConfiguration> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    
    List<SubServiceConfiguration> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    Optional<SubServiceConfiguration> findByTenantIdAndServiceNameAndSubServiceName(
        String tenantId, String serviceName, String subServiceName);
    
    List<SubServiceConfiguration> findByTenantIdAndServiceNameAndEnabled(
        String tenantId, String serviceName, Boolean enabled);
    
    @Query("SELECT DISTINCT ssc.serviceName FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId")
    List<String> findDistinctServiceNamesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT ssc.subServiceName FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.serviceName = :serviceName")
    List<String> findDistinctSubServiceNamesForService(@Param("tenantId") String tenantId, 
                                                      @Param("serviceName") String serviceName);
    
    @Query("SELECT ssc FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.enabled = true")
    List<SubServiceConfiguration> findAllEnabledForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(ssc) FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.serviceName = :serviceName")
    long countByTenantIdAndServiceName(@Param("tenantId") String tenantId, @Param("serviceName") String serviceName);
    
    @Query("SELECT ssc FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.serviceName = :serviceName AND ssc.subServiceName = :subServiceName AND ssc.enabled = true")
    Optional<SubServiceConfiguration> findEnabledSubService(@Param("tenantId") String tenantId,
                                                           @Param("serviceName") String serviceName,
                                                           @Param("subServiceName") String subServiceName);
    
    // Performance optimization queries
    @Query("SELECT ssc FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.enabled = true ORDER BY ssc.serviceName, ssc.subServiceName")
    List<SubServiceConfiguration> findEnabledConfigsForBatchProcessing(@Param("tenantId") String tenantId);
    
    @Query("SELECT ssc FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId ORDER BY ssc.createdAt DESC")
    List<SubServiceConfiguration> findMostFrequentlyUsedConfigs(@Param("tenantId") String tenantId, @Param("limit") int limit);
    
    @Query("SELECT COUNT(ssc) > 0 FROM SubServiceConfiguration ssc WHERE ssc.tenantId = :tenantId AND ssc.serviceName = :serviceName AND ssc.subServiceName = :subServiceName")
    boolean existsByTenantIdAndServiceNameAndSubServiceName(@Param("tenantId") String tenantId, 
                                                          @Param("serviceName") String serviceName, 
                                                          @Param("subServiceName") String subServiceName);
    
    // For validation purposes
    boolean existsByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    boolean existsByTenantIdAndServiceNameAndSubServiceNameAndIdNot(
        String tenantId, String serviceName, String subServiceName, Long id);
}