package com.filetransfer.batch.repository;

import com.filetransfer.batch.entity.FileTransferRecord;
import com.filetransfer.batch.entity.TransferStatus;
import com.filetransfer.batch.entity.TransferDirection;
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
    
    // Legacy methods for backward compatibility (deprecated)
    @Deprecated
    List<FileTransferRecord> findByServiceType(String serviceType);
    
    @Deprecated
    List<FileTransferRecord> findByStatus(TransferStatus status);
    
    @Deprecated
    List<FileTransferRecord> findByDirection(TransferDirection direction);
    
    @Deprecated
    List<FileTransferRecord> findByServiceTypeAndStatus(String serviceType, TransferStatus status);
    
    @Deprecated
    List<FileTransferRecord> findByServiceTypeAndDirection(String serviceType, TransferDirection direction);
    
    @Deprecated
    @Query("SELECT f FROM FileTransferRecord f WHERE f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Deprecated
    @Query("SELECT f FROM FileTransferRecord f WHERE f.serviceType = :serviceType AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByServiceTypeAndDateRange(@Param("serviceType") String serviceType,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Deprecated
    List<FileTransferRecord> findByFileName(String fileName);
    
    @Deprecated
    @Query("SELECT DISTINCT f.serviceType FROM FileTransferRecord f")
    List<String> findDistinctServiceTypes();
}