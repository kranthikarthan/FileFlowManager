package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileSchema;
import com.filetransfer.web.entity.SchemaValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemaValidationRuleRepository extends JpaRepository<SchemaValidationRule, Long> {
    
    List<SchemaValidationRule> findBySchemaAndIsActiveTrueOrderByRuleOrder(FileSchema schema);
    
    List<SchemaValidationRule> findBySchema(FileSchema schema);
    
    List<SchemaValidationRule> findBySchemaAndRuleType(FileSchema schema, String ruleType);
}