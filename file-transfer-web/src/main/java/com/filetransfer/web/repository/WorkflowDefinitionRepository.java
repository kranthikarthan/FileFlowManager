package com.filetransfer.web.repository;

import com.filetransfer.web.entity.WorkflowDefinition;
import com.filetransfer.web.entity.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for workflow definitions
 */
@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    
    // Find by tenant
    List<WorkflowDefinition> findByTenantId(String tenantId);
    
    // Find active workflows for tenant
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.isActive = true ORDER BY w.workflowName")
    List<WorkflowDefinition> findActiveWorkflowsForTenant(@Param("tenantId") String tenantId);
    
    // Find by workflow type
    List<WorkflowDefinition> findByTenantIdAndWorkflowType(String tenantId, WorkflowType workflowType);
    
    // Find by service
    List<WorkflowDefinition> findByTenantIdAndServiceName(String tenantId, String serviceName);
    
    // Find templates
    List<WorkflowDefinition> findByTenantIdAndIsTemplate(String tenantId, Boolean isTemplate);
    
    // Find workflows by execution statistics
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId ORDER BY w.executionCount DESC")
    List<WorkflowDefinition> findMostExecutedWorkflows(@Param("tenantId") String tenantId);
    
    // Find workflows with high success rate
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.executionCount > 0 " +
           "AND (w.successCount * 100.0 / w.executionCount) >= :minSuccessRate ORDER BY w.successCount DESC")
    List<WorkflowDefinition> findHighPerformanceWorkflows(@Param("tenantId") String tenantId, @Param("minSuccessRate") Double minSuccessRate);
    
    // Find workflows needing attention
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND w.executionCount > 0 " +
           "AND (w.failureCount * 100.0 / w.executionCount) > :failureThreshold")
    List<WorkflowDefinition> findWorkflowsNeedingAttention(@Param("tenantId") String tenantId, @Param("failureThreshold") Double failureThreshold);
}