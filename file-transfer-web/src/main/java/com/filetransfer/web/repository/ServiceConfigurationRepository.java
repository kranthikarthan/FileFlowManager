package com.filetransfer.web.repository;

import com.filetransfer.web.entity.ServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceConfigurationRepository extends JpaRepository<ServiceConfiguration, Long> {
    
    Optional<ServiceConfiguration> findByServiceName(String serviceName);
    
    List<ServiceConfiguration> findByEnabled(Boolean enabled);
    
    @Query("SELECT s FROM ServiceConfiguration s WHERE s.enabled = true ORDER BY s.serviceName")
    List<ServiceConfiguration> findAllEnabledServices();
    
    @Query("SELECT COUNT(s) FROM ServiceConfiguration s WHERE s.enabled = true")
    long countEnabledServices();
    
    boolean existsByServiceName(String serviceName);
}