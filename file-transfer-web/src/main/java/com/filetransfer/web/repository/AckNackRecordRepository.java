package com.filetransfer.web.repository;

import com.filetransfer.web.entity.AckNackRecord;
import com.filetransfer.web.entity.AckNackStatus;
import com.filetransfer.web.entity.AckNackType;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AckNackRecordRepository extends JpaRepository<AckNackRecord, Long> {
    
    List<AckNackRecord> findByTenantId(String tenantId);
    
    List<AckNackRecord> findByTenantIdAndStatus(String tenantId, AckNackStatus status);
    
    List<AckNackRecord> findByTenantIdAndType(String tenantId, AckNackType type);
    
    List<AckNackRecord> findByTenantIdAndDirection(String tenantId, TransferDirection direction);
    
    List<AckNackRecord> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    List<AckNackRecord> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    Optional<AckNackRecord> findByOriginalFileTransferId(Long originalFileTransferId);
    
    List<AckNackRecord> findByOriginalFileTransferIdIn(List<Long> originalFileTransferIds);
    
    List<AckNackRecord> findByStatus(AckNackStatus status);
    
    List<AckNackRecord> findByStatusIn(List<AckNackStatus> statuses);
    
    @Query("SELECT a FROM AckNackRecord a WHERE a.status = :status AND a.expiresAt < :currentTime")
    List<AckNackRecord> findExpiredRecords(@Param("status") AckNackStatus status, @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT a FROM AckNackRecord a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AckNackRecord> findByTenantIdAndDateRange(@Param("tenantId") String tenantId, 
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AckNackRecord a WHERE a.tenantId = :tenantId AND a.serviceName = :serviceName AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AckNackRecord> findByTenantIdAndServiceNameAndDateRange(@Param("tenantId") String tenantId,
                                                                @Param("serviceName") String serviceName,
                                                                @Param("startDate") LocalDateTime startDate, 
                                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AckNackRecord a WHERE a.tenantId = :tenantId AND a.type = :type AND a.status = :status")
    Long countByTenantIdAndTypeAndStatus(@Param("tenantId") String tenantId, @Param("type") AckNackType type, @Param("status") AckNackStatus status);
    
    @Query("SELECT DISTINCT a.serviceName FROM AckNackRecord a WHERE a.tenantId = :tenantId")
    List<String> findDistinctServiceNamesForTenant(@Param("tenantId") String tenantId);
}