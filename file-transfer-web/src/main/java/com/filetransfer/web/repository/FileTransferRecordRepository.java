package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileTransferRecord;
import com.filetransfer.web.entity.TransferStatus;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileTransferRecordRepository extends JpaRepository<FileTransferRecord, Long> {
    
    List<FileTransferRecord> findByTenantId(String tenantId);
    
    List<FileTransferRecord> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    List<FileTransferRecord> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    List<FileTransferRecord> findByTenantIdAndStatus(String tenantId, TransferStatus status);
    
    List<FileTransferRecord> findByTenantIdAndDirection(String tenantId, TransferDirection direction);
    
    List<FileTransferRecord> findByTenantIdAndServiceNameAndStatus(String tenantId, String serviceName, TransferStatus status);
    
    List<FileTransferRecord> findByTenantIdAndServiceNameAndDirection(String tenantId, String serviceName, TransferDirection direction);
    
    // BACKWARD COMPATIBILITY - Keep old methods for existing code
    List<FileTransferRecord> findByTenantIdAndServiceType(String tenantId, String serviceType);
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndSubServiceType(String tenantId, String serviceType, String subServiceType);
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndStatus(String tenantId, String serviceType, TransferStatus status);
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndDirection(String tenantId, String serviceType, TransferDirection direction);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndServiceNameAndDateRange(@Param("tenantId") String tenantId,
                                                                     @Param("serviceName") String serviceName,
                                                                     @Param("startDate") LocalDateTime startDate,
                                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.subServiceName = :subServiceName AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndServiceNameAndSubServiceNameAndCreatedAtBetween(@Param("tenantId") String tenantId,
                                                                                              @Param("serviceName") String serviceName,
                                                                                              @Param("subServiceName") String subServiceName,
                                                                                              @Param("startDate") LocalDateTime startDate,
                                                                                              @Param("endDate") LocalDateTime endDate);
    
    // BACKWARD COMPATIBILITY - Keep old method names
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.createdAt BETWEEN :startDate AND :endDate")
    List<FileTransferRecord> findByTenantIdAndServiceTypeAndDateRange(@Param("tenantId") String tenantId,
                                                                    @Param("serviceName") String serviceName,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate);
    
    List<FileTransferRecord> findByTenantIdAndFileName(String tenantId, String fileName);
    
    @Query("SELECT DISTINCT f.serviceName FROM FileTransferRecord f WHERE f.tenantId = :tenantId")
    List<String> findDistinctServiceNamesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT f.subServiceName FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.subServiceName IS NOT NULL")
    List<String> findDistinctSubServiceNamesForService(@Param("tenantId") String tenantId, @Param("serviceName") String serviceName);
    
    // File extension filtering methods
    List<FileTransferRecord> findByTenantIdAndFileExtension(String tenantId, String fileExtension);
    
    List<FileTransferRecord> findByTenantIdAndFileExtensionIn(String tenantId, List<String> fileExtensions);
    
    @Query("SELECT DISTINCT f.fileExtension FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.fileExtension IS NOT NULL ORDER BY f.fileExtension")
    List<String> findDistinctFileExtensionsForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT f.fileExtension, COUNT(f) FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.fileExtension IS NOT NULL GROUP BY f.fileExtension ORDER BY COUNT(f) DESC")
    List<Object[]> getFileExtensionStatistics(@Param("tenantId") String tenantId);
    
    List<FileTransferRecord> findByTenantIdAndServiceNameAndFileExtension(String tenantId, String serviceName, String fileExtension);
    
    // Recent files methods
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId ORDER BY f.createdAt DESC")
    List<FileTransferRecord> findRecentFiles(@Param("tenantId") String tenantId, Pageable pageable);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FileTransferRecord> findRecentFilesSince(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.status = 'COMPLETED' ORDER BY f.processedAt DESC")
    List<FileTransferRecord> findRecentlyProcessedFiles(@Param("tenantId") String tenantId, Pageable pageable);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.status = 'FAILED' ORDER BY f.createdAt DESC")
    List<FileTransferRecord> findRecentFailedFiles(@Param("tenantId") String tenantId, Pageable pageable);
    
    // BACKWARD COMPATIBILITY - Keep old method names  
    @Query("SELECT DISTINCT f.serviceName FROM FileTransferRecord f WHERE f.tenantId = :tenantId")
    List<String> findDistinctServiceTypesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT DISTINCT f.subServiceName FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.subServiceName IS NOT NULL")
    List<String> findDistinctSubServiceTypesForService(@Param("tenantId") String tenantId, @Param("serviceName") String serviceName);
    
    @Query("SELECT COUNT(f) FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.status IN (:activeStatuses)")
    long countActiveTransfersForService(@Param("tenantId") String tenantId, 
                                      @Param("serviceName") String serviceName,
                                      @Param("activeStatuses") List<TransferStatus> activeStatuses);
    
    @Query("SELECT COUNT(f) FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.subServiceName = :subServiceName AND f.status IN (:activeStatuses)")
    long countActiveTransfersForSubService(@Param("tenantId") String tenantId,
                                         @Param("serviceName") String serviceName,
                                         @Param("subServiceName") String subServiceName,
                                         @Param("activeStatuses") List<TransferStatus> activeStatuses);
    
    // NEW: Additional methods for processing control
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.status = :status")
    List<FileTransferRecord> findByTenantIdAndStatusString(@Param("tenantId") String tenantId, @Param("status") String status);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.status = :status")
    List<FileTransferRecord> findByTenantIdAndServiceNameAndStatusString(@Param("tenantId") String tenantId, @Param("serviceName") String serviceName, @Param("status") String status);
    
    @Query("SELECT f FROM FileTransferRecord f WHERE f.tenantId = :tenantId AND f.serviceName = :serviceName AND f.subServiceName = :subServiceName AND f.status = :status")
    List<FileTransferRecord> findByTenantIdAndServiceNameAndSubServiceNameAndStatusString(@Param("tenantId") String tenantId, @Param("serviceName") String serviceName, @Param("subServiceName") String subServiceName, @Param("status") String status);
    
    @Query("SELECT DISTINCT f.tenantId FROM FileTransferRecord f")
    List<String> findDistinctTenantIds();
    
    // NEW: Methods for processing time tracking
    List<FileTransferRecord> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate);
}