package com.filetransfer.web.repository;

import com.filetransfer.web.entity.HsmValidationRecord;
import com.filetransfer.web.entity.HsmValidationStatus;
import com.filetransfer.web.entity.HsmProvider;
import com.filetransfer.web.entity.HsmOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HsmValidationRecordRepository extends JpaRepository<HsmValidationRecord, Long> {
    
    List<HsmValidationRecord> findByTenantId(String tenantId);
    
    List<HsmValidationRecord> findByTenantIdAndStatus(String tenantId, HsmValidationStatus status);
    
    List<HsmValidationRecord> findByTenantIdAndHsmProvider(String tenantId, HsmProvider hsmProvider);
    
    List<HsmValidationRecord> findByTenantIdAndOperation(String tenantId, HsmOperation operation);
    
    List<HsmValidationRecord> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    List<HsmValidationRecord> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    Optional<HsmValidationRecord> findByFileTransferId(Long fileTransferId);
    
    List<HsmValidationRecord> findByFileTransferIdIn(List<Long> fileTransferIds);
    
    List<HsmValidationRecord> findByStatus(HsmValidationStatus status);
    
    List<HsmValidationRecord> findByStatusIn(List<HsmValidationStatus> statuses);
    
    @Query("SELECT h FROM HsmValidationRecord h WHERE h.status = :status AND h.startedAt < :timeoutThreshold")
    List<HsmValidationRecord> findTimedOutRecords(@Param("status") HsmValidationStatus status, 
                                                  @Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    @Query("SELECT h FROM HsmValidationRecord h WHERE h.tenantId = :tenantId AND h.createdAt BETWEEN :startDate AND :endDate")
    List<HsmValidationRecord> findByTenantIdAndDateRange(@Param("tenantId") String tenantId, 
                                                         @Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT h FROM HsmValidationRecord h WHERE h.tenantId = :tenantId AND h.serviceName = :serviceName AND h.createdAt BETWEEN :startDate AND :endDate")
    List<HsmValidationRecord> findByTenantIdAndServiceNameAndDateRange(@Param("tenantId") String tenantId,
                                                                       @Param("serviceName") String serviceName,
                                                                       @Param("startDate") LocalDateTime startDate, 
                                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(h) FROM HsmValidationRecord h WHERE h.tenantId = :tenantId AND h.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") HsmValidationStatus status);
    
    @Query("SELECT COUNT(h) FROM HsmValidationRecord h WHERE h.tenantId = :tenantId AND h.hsmProvider = :provider")
    Long countByTenantIdAndProvider(@Param("tenantId") String tenantId, @Param("provider") HsmProvider provider);
    
    @Query("SELECT DISTINCT h.serviceName FROM HsmValidationRecord h WHERE h.tenantId = :tenantId")
    List<String> findDistinctServiceNamesForTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT AVG(h.processingTimeMs) FROM HsmValidationRecord h WHERE h.tenantId = :tenantId AND h.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTimeByTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT h.hsmProvider, COUNT(h) FROM HsmValidationRecord h WHERE h.tenantId = :tenantId GROUP BY h.hsmProvider")
    List<Object[]> getProviderUsageByTenant(@Param("tenantId") String tenantId);
    
    @Query("SELECT h.operation, COUNT(h) FROM HsmValidationRecord h WHERE h.tenantId = :tenantId GROUP BY h.operation")
    List<Object[]> getOperationUsageByTenant(@Param("tenantId") String tenantId);
}