package com.filetransfer.web.repository;

import com.filetransfer.web.entity.CutOffExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CutOffExtensionRepository extends JpaRepository<CutOffExtension, Long> {
    
    List<CutOffExtension> findByTenantId(String tenantId);
    
    List<CutOffExtension> findByTenantIdAndStatus(String tenantId, CutOffExtension.ExtensionStatus status);
    
    List<CutOffExtension> findByTenantIdAndServiceNameAndSubServiceName(
        String tenantId, String serviceName, String subServiceName);
    
    Optional<CutOffExtension> findByTenantIdAndServiceNameAndSubServiceNameAndExtensionDateAndStatus(
        String tenantId, String serviceName, String subServiceName, 
        LocalDate extensionDate, CutOffExtension.ExtensionStatus status);
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.tenantId = :tenantId " +
           "AND coe.serviceName = :serviceName AND coe.subServiceName = :subServiceName " +
           "AND coe.extensionDate = :extensionDate AND coe.status = 'ACTIVE' " +
           "AND coe.effectiveFrom <= :currentTime AND coe.effectiveUntil > :currentTime")
    Optional<CutOffExtension> findActiveExtensionForService(
        @Param("tenantId") String tenantId,
        @Param("serviceName") String serviceName,
        @Param("subServiceName") String subServiceName,
        @Param("extensionDate") LocalDate extensionDate,
        @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.status = 'PENDING' " +
           "ORDER BY coe.priority DESC, coe.requestedAt ASC")
    List<CutOffExtension> findPendingExtensionsOrderedByPriority();
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.status = 'APPROVED' " +
           "AND coe.extensionDate = :today")
    List<CutOffExtension> findApprovedExtensionsForToday(@Param("today") LocalDate today);
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.status = 'ACTIVE' " +
           "AND coe.effectiveUntil <= :currentTime")
    List<CutOffExtension> findExpiredActiveExtensions(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.requestedBy = :userId " +
           "ORDER BY coe.requestedAt DESC")
    List<CutOffExtension> findByRequestedByOrderByRequestedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT coe FROM CutOffExtension coe WHERE coe.tenantId = :tenantId " +
           "AND coe.extensionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY coe.extensionDate DESC, coe.requestedAt DESC")
    List<CutOffExtension> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(coe) FROM CutOffExtension coe WHERE coe.tenantId = :tenantId " +
           "AND coe.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") String tenantId, 
                                 @Param("status") CutOffExtension.ExtensionStatus status);
    
    @Query("SELECT COUNT(coe) FROM CutOffExtension coe WHERE coe.tenantId = :tenantId " +
           "AND coe.serviceName = :serviceName AND coe.subServiceName = :subServiceName " +
           "AND coe.extensionDate = :extensionDate AND coe.status IN ('PENDING', 'APPROVED', 'ACTIVE')")
    long countActiveOrPendingExtensionsForServiceAndDate(
        @Param("tenantId") String tenantId,
        @Param("serviceName") String serviceName,
        @Param("subServiceName") String subServiceName,
        @Param("extensionDate") LocalDate extensionDate);
    
    // For auto-approval logic
    @Query("SELECT COUNT(coe) FROM CutOffExtension coe WHERE coe.requestedBy = :userId " +
           "AND coe.requestedAt >= :since AND coe.status = 'APPROVED'")
    long countRecentApprovedExtensionsByUser(@Param("userId") String userId, 
                                           @Param("since") LocalDateTime since);
}