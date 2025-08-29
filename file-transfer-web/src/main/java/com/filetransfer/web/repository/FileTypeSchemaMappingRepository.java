package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTypeSchemaMapping;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.SubServiceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileTypeSchemaMappingRepository extends JpaRepository<FileTypeSchemaMapping, Long> {
    
    List<FileTypeSchemaMapping> findBySubServiceConfiguration(SubServiceConfiguration subServiceConfiguration);
    
    Optional<FileTypeSchemaMapping> findBySubServiceConfigurationAndFileType(
        SubServiceConfiguration subServiceConfiguration, FileType fileType);
    
    @Query("SELECT ftsm FROM FileTypeSchemaMapping ftsm WHERE ftsm.subServiceConfiguration.id = :subServiceConfigId")
    List<FileTypeSchemaMapping> findBySubServiceConfigurationId(@Param("subServiceConfigId") Long subServiceConfigId);
    
    @Query("SELECT ftsm FROM FileTypeSchemaMapping ftsm WHERE " +
           "ftsm.subServiceConfiguration.tenantId = :tenantId AND " +
           "ftsm.subServiceConfiguration.serviceName = :serviceName AND " +
           "ftsm.subServiceConfiguration.subServiceName = :subServiceName AND " +
           "ftsm.fileType = :fileType")
    Optional<FileTypeSchemaMapping> findByServiceAndFileType(
        @Param("tenantId") String tenantId,
        @Param("serviceName") String serviceName,
        @Param("subServiceName") String subServiceName,
        @Param("fileType") FileType fileType);
    
    @Query("SELECT DISTINCT ftsm.fileType FROM FileTypeSchemaMapping ftsm WHERE " +
           "ftsm.subServiceConfiguration.tenantId = :tenantId")
    List<FileType> findDistinctFileTypesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(ftsm) FROM FileTypeSchemaMapping ftsm WHERE " +
           "ftsm.inboundSchemaId = :schemaId OR ftsm.outboundSchemaId = :schemaId")
    long countBySchemaId(@Param("schemaId") Long schemaId);
    
    void deleteBySubServiceConfiguration(SubServiceConfiguration subServiceConfiguration);
}