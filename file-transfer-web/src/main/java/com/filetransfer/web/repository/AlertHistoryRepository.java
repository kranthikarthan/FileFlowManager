package com.filetransfer.web.repository;

import com.filetransfer.web.entity.AlertHistory;
import com.filetransfer.web.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    
    List<AlertHistory> findByTenantId(String tenantId);
    
    List<AlertHistory> findByTenantIdOrderBySentAtDesc(String tenantId);
    
    List<AlertHistory> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    List<AlertHistory> findByTenantIdAndAlertType(String tenantId, AlertConfiguration.AlertType alertType);
    
    List<AlertHistory> findByTenantIdAndAlertLevel(String tenantId, AlertHistory.AlertLevel alertLevel);
    
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.tenantId = :tenantId AND ah.sentAt >= :startDate AND ah.sentAt <= :endDate")
    List<AlertHistory> findByTenantIdAndDateRange(@Param("tenantId") String tenantId, 
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.tenantId = :tenantId AND ah.acknowledgedAt IS NULL")
    List<AlertHistory> findUnacknowledgedAlerts(@Param("tenantId") String tenantId);
    
    @Query("SELECT COUNT(ah) FROM AlertHistory ah WHERE ah.tenantId = :tenantId AND ah.alertLevel = :alertLevel AND ah.sentAt >= :startDate")
    Long countAlertsByLevelAndDate(@Param("tenantId") String tenantId, 
                                  @Param("alertLevel") AlertHistory.AlertLevel alertLevel, 
                                  @Param("startDate") LocalDateTime startDate);
}