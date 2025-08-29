package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileTransferRecordRepository extends JpaRepository<FileTransferRecord, Long> {
    
    List<FileTransferRecord> findByTenantId(String tenantId);
    
    List<FileTransferRecord> findByTenantIdAndServiceType(String tenantId, String serviceType);
    
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndSubServiceType(String tenantId, String serviceType, String subServiceType);
    
    List<FileTransferRecord> findByTenantIdAndStatus(String tenantId, TransferStatus status);
    
    List<FileTransferRecord> findByTenantIdAndDirection(String tenantId, TransferDirection direction);
    
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndStatus(String tenantId, String serviceType, TransferStatus status);
    
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndDirection(String tenantId, String serviceType, TransferDirection direction);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceType = :serviceType AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndDateRange(@Param("tenantId") String tenantId,
                                                                    @Param("serviceType") String serviceType,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate);
    
    List<FileTransferRecord> findByTenantIdAndFileName(String tenantId, String fileName);
    
    @Query("SELECT DISTINCT f.serviceType FROM FileTransferRecord f WHERE f.tenantId = :tenantId")
    List<String> findDistinctServiceTypesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT f.subServiceType FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceType = :serviceType AND f.subServiceType IS NOT NULL")
    List<String> findDistinctSubServiceTypesForService(@Param("tenantId") String tenantId, @Param("serviceType") String serviceType);
    
    @Query("SELECT COUNT(f) FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceType = :serviceType AND f.status IN (:activeStatuses)")
    long countActiveTransfersForService(@Param("tenantId") String tenantId, 
                                      @Param("serviceType") String serviceType,
                                      @Param("activeStatuses") List<TransferStatus> activeStatuses);
    
    @Query("SELECT COUNT(f) FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceType = :serviceType AND f.subServiceType = :subServiceType AND f.status IN (:activeStatuses)")
    long countActiveTransfersForSubService(@Param("tenantId") String tenantId,
                                         @Param("serviceType") String serviceType,
                                         @Param("subServiceType") String subServiceType,
                                         @Param("activeStatuses") List<TransferStatus> activeStatuses);
}