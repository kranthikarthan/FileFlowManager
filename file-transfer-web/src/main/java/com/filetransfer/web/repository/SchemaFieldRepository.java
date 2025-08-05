package com.filetransfer.web.repository;

import com.filetransfer.web.entity.FileSchema;
import com.filetransfer.web.entity.SchemaField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemaFieldRepository extends JpaRepository<SchemaField, Long> {
    
    List<SchemaField> findBySchemaOrderByFieldOrder(FileSchema schema);
    
    List<SchemaField> findBySchemaAndIsRequiredTrue(FileSchema schema);
    
    List<SchemaField> findBySchemaAndFieldType(FileSchema schema, String fieldType);
}