package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileSchema;
import com.filetransfer.web.entity.SchemaUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SchemaUsageLogRepository extends JpaRepository<SchemaUsageLog, Long> {
    
    List<SchemaUsageLog> findBySchemaOrderByValidationTimestampDesc(FileSchema schema);
    
    List<SchemaUsageLog> findBySchemaAndValidationResult(FileSchema schema, String validationResult);
    
    @Query("SELECT sul FROM SchemaUsageLog sul WHERE sul.schema = :schema AND sul.validationTimestamp BETWEEN :startDate AND :endDate")
    List<SchemaUsageLog> findBySchemaAndValidationTimestampBetween(@Param("schema") FileSchema schema,
                                                                 @Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(sul) FROM SchemaUsageLog sul WHERE sul.schema = :schema AND sul.validationResult = 'PASSED'")
    long countPassedValidations(@Param("schema") FileSchema schema);
    
    @Query("SELECT COUNT(sul) FROM SchemaUsageLog sul WHERE sul.schema = :schema AND sul.validationResult = 'FAILED'")
    long countFailedValidations(@Param("schema") FileSchema schema);
}