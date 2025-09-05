package com.filetransfer.web.repository;

import com.filetransfer.web.entity.WorkflowExecution;
import com.filetransfer.web.entity.WorkflowExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for workflow executions
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    
    // Find by tenant
    List<WorkflowExecution> findByTenantId(String tenantId);
    
    // Find by execution status
    List<WorkflowExecution> findByTenantIdAndExecutionStatus(String tenantId, WorkflowExecutionStatus status);
    
    // Find active executions
    @Query("SELECT w FROM WorkflowExecution w WHERE w.tenantId = :tenantId AND w.executionStatus IN :activeStatuses")
    List<WorkflowExecution> findActiveExecutions(@Param("tenantId") String tenantId, 
                                                @Param("activeStatuses") List<WorkflowExecutionStatus> activeStatuses);
    
    // Find by file transfer
    List<WorkflowExecution> findByFileTransferId(Long fileTransferId);
    
    // Find by workflow definition
    List<WorkflowExecution> findByWorkflowDefinitionId(Long workflowDefinitionId);
    
    // Find executions needing retry
    @Query("SELECT w FROM WorkflowExecution w WHERE w.executionStatus = 'WAITING_FOR_RETRY' AND w.nextRetryAt <= :now")
    List<WorkflowExecution> findExecutionsReadyForRetry(@Param("now") LocalDateTime now);
    
    // Find timed out executions
    @Query("SELECT w FROM WorkflowExecution w WHERE w.executionStatus IN ('RUNNING', 'WAITING_FOR_EVENT') AND w.timeoutAt <= :now")
    List<WorkflowExecution> findTimedOutExecutions(@Param("now") LocalDateTime now);
    
    // Find executions waiting for approval
    List<WorkflowExecution> findByTenantIdAndExecutionStatus(String tenantId, WorkflowExecutionStatus status);
    
    // Get execution statistics
    @Query("SELECT COUNT(w), SUM(CASE WHEN w.executionStatus = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "AVG(w.durationMinutes) FROM WorkflowExecution w WHERE w.tenantId = :tenantId")
    Object[] getExecutionStatistics(@Param("tenantId") String tenantId);
}