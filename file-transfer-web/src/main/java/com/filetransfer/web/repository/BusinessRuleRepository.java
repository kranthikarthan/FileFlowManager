package com.filetransfer.web.repository;

import com.filetransfer.web.entity.BusinessRule;
import com.filetransfer.web.entity.RuleType;
import com.filetransfer.web.entity.TransferDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for business rules
 */
@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {
    
    // Find by tenant
    List<BusinessRule> findByTenantId(String tenantId);
    
    // Find active rules for tenant
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.isActive = true " +
           "AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now) " +
           "AND (r.effectiveUntil IS NULL OR r.effectiveUntil >= :now) " +
           "ORDER BY r.rulePriority DESC, r.executionOrder ASC")
    List<BusinessRule> findActiveRulesForTenant(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);
    
    // Overloaded method without explicit now parameter
    default List<BusinessRule> findActiveRulesForTenant(String tenantId) {
        return findActiveRulesForTenant(tenantId, LocalDateTime.now());
    }
    
    // Find by rule type
    List<BusinessRule> findByTenantIdAndRuleType(String tenantId, RuleType ruleType);
    
    // Find by service
    List<BusinessRule> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    // Find by service and sub-service
    List<BusinessRule> findByTenantIdAndServiceNameAndSubServiceName(String tenantId, String serviceName, String subServiceName);
    
    // Find by category
    List<BusinessRule> findByTenantIdAndRuleCategory(String tenantId, String ruleCategory);
    
    // Find by direction
    List<BusinessRule> findByTenantIdAndAppliesToDirection(String tenantId, TransferDirection direction);
    
    // Find by priority range
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.rulePriority BETWEEN :minPriority AND :maxPriority")
    List<BusinessRule> findByPriorityRange(@Param("tenantId") String tenantId, 
                                          @Param("minPriority") Integer minPriority, 
                                          @Param("maxPriority") Integer maxPriority);
    
    // Find high priority rules
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.rulePriority >= 80 ORDER BY r.rulePriority DESC")
    List<BusinessRule> findHighPriorityRules(@Param("tenantId") String tenantId);
    
    // Find rules by execution statistics
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.executionCount > :minExecutions ORDER BY r.executionCount DESC")
    List<BusinessRule> findMostExecutedRules(@Param("tenantId") String tenantId, @Param("minExecutions") Long minExecutions);
    
    // Find rules with high failure rate
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.executionCount > 0 AND (r.failureCount * 100.0 / r.executionCount) > :failureThreshold")
    List<BusinessRule> findRulesWithHighFailureRate(@Param("tenantId") String tenantId, @Param("failureThreshold") Double failureThreshold);
    
    // Find rules never executed
    List<BusinessRule> findByTenantIdAndExecutionCount(String tenantId, Long executionCount);
    
    // Find recently created rules
    List<BusinessRule> findByTenantIdAndCreatedAtAfter(String tenantId, LocalDateTime since);
    
    // Find rules by creator
    List<BusinessRule> findByTenantIdAndCreatedBy(String tenantId, String createdBy);
    
    // Search rules by name
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND LOWER(r.ruleName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<BusinessRule> searchByRuleName(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    // Find rules by effectiveness (high success rate and usage)
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.executionCount >= :minExecutions " +
           "AND (r.successCount * 100.0 / r.executionCount) >= :minSuccessRate ORDER BY r.successCount DESC")
    List<BusinessRule> findEffectiveRules(@Param("tenantId") String tenantId, 
                                         @Param("minExecutions") Long minExecutions, 
                                         @Param("minSuccessRate") Double minSuccessRate);
    
    // Get rule statistics
    @Query("SELECT COUNT(r), SUM(r.executionCount), AVG(r.rulePriority) FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.isActive = true")
    Object[] getRuleStatistics(@Param("tenantId") String tenantId);
    
    // Get rule performance statistics
    @Query("SELECT r.ruleType, COUNT(r), AVG(r.averageExecutionTimeMs), AVG(r.successCount * 100.0 / NULLIF(r.executionCount, 0)) " +
           "FROM BusinessRule r WHERE r.tenantId = :tenantId GROUP BY r.ruleType")
    List<Object[]> getRulePerformanceByType(@Param("tenantId") String tenantId);
    
    // Find rules affecting specific file characteristics
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId AND r.isActive = true " +
           "AND (r.appliesToFileTypes IS NULL OR r.appliesToFileTypes LIKE CONCAT('%', :fileType, '%')) " +
           "AND (r.appliesToDirection IS NULL OR r.appliesToDirection = :direction) " +
           "AND (r.serviceName IS NULL OR r.serviceName = :serviceName)")
    List<BusinessRule> findRulesForFileCharacteristics(@Param("tenantId") String tenantId,
                                                       @Param("fileType") String fileType,
                                                       @Param("direction") TransferDirection direction,
                                                       @Param("serviceName") String serviceName);
    
    // Find rules due for review (old rules with low usage)
    @Query("SELECT r FROM BusinessRule r WHERE r.tenantId = :tenantId " +
           "AND r.createdAt < :reviewDate " +
           "AND (r.executionCount = 0 OR r.lastExecutedAt < :lastUsedDate)")
    List<BusinessRule> findRulesDueForReview(@Param("tenantId") String tenantId,
                                            @Param("reviewDate") LocalDateTime reviewDate,
                                            @Param("lastUsedDate") LocalDateTime lastUsedDate);
    
    // Find conflicting rules (same priority, overlapping conditions)
    @Query("SELECT r1, r2 FROM BusinessRule r1, BusinessRule r2 WHERE r1.tenantId = :tenantId AND r2.tenantId = :tenantId " +
           "AND r1.id < r2.id AND r1.rulePriority = r2.rulePriority " +
           "AND r1.ruleCategory = r2.ruleCategory AND r1.isActive = true AND r2.isActive = true")
    List<Object[]> findPotentialConflictingRules(@Param("tenantId") String tenantId);
}