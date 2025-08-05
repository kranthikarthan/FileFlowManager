package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileSchemaRepository extends JpaRepository<FileSchema, Long> {
    
    List<FileSchema> findByTenantIdAndIsActiveTrue(String tenantId);
    
    List<FileSchema> findByTenantIdAndServiceTypeAndIsActiveTrue(String tenantId, String serviceType);
    
    List<FileSchema> findByTenantIdAndServiceType(String tenantId, String serviceType);
    
    @Query("SELECT fs FROM FileSchema fs WHERE fs.tenantId = :tenantId AND fs.serviceType = :serviceType AND fs.schemaVersion = :version")
    FileSchema findByTenantIdAndServiceTypeAndVersion(@Param("tenantId") String tenantId, 
                                                     @Param("serviceType") String serviceType, 
                                                     @Param("version") String version);
    
    @Query("SELECT fs FROM FileSchema fs WHERE fs.tenantId = :tenantId AND fs.schemaName LIKE %:name%")
    List<FileSchema> findByTenantIdAndSchemaNameContaining(@Param("tenantId") String tenantId, 
                                                          @Param("name") String name);
    
    boolean existsByTenantIdAndServiceTypeAndSchemaVersion(String tenantId, String serviceType, String schemaVersion);
}