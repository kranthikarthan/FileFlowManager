package com.filetransfer.web.repository;

import com.filetransfer.web.entity.SsoConfiguration;
import com.filetransfer.web.entity.SsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SsoConfigurationRepository extends JpaRepository<SsoConfiguration, Long> {
    
    Optional<SsoConfiguration> findByOrganizationId(String organizationId);
    
    List<SsoConfiguration> findByEnabled(Boolean enabled);
    
    List<SsoConfiguration> findByProvider(SsoProvider provider);
    
    @Query("SELECT s FROM SsoConfiguration s WHERE s.enabled = true ORDER BY s.organizationName")
    List<SsoConfiguration> findAllEnabledConfigurations();
    
    boolean existsByOrganizationId(String organizationId);
}