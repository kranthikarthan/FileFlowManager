package com.filetransfer.web.service;

import com.filetransfer.web.entity.SharedSchema;
import com.filetransfer.web.entity.SchemaUsageMapping;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.entity.TransferDirection;
import com.filetransfer.web.repository.SharedSchemaRepository;
import com.filetransfer.web.repository.SchemaUsageMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing shared schemas that can be reused across subservices and directions.
 */
@Service
@Transactional
public class SharedSchemaService {
    
    private static final Logger logger = LoggerFactory.getLogger(SharedSchemaService.class);
    
    @Autowired
    private SharedSchemaRepository sharedSchemaRepository;
    
    @Autowired
    private SchemaUsageMappingRepository schemaUsageMappingRepository;
    
    @Autowired
    private SecurityContextService securityContextService;
    
    /**
     * Get all schemas available to a tenant (cached)
     */
    @Cacheable(value = "sharedSchemas", key = "'tenant_' + #tenantId")
    public List<SharedSchema> getAvailableSchemasForTenant(String tenantId) {
        logger.debug("Loading available schemas for tenant: {}", tenantId);
        return sharedSchemaRepository.findAvailableSchemasForTenant(tenantId);
    }
    
    /**
     * Get schemas by file type for a tenant
     */
    @Cacheable(value = "sharedSchemas", key = "'tenant_' + #tenantId + '_type_' + #fileType")
    public List<SharedSchema> getSchemasByFileType(String tenantId, FileType fileType) {
        logger.debug("Loading schemas for tenant: {} and file type: {}", tenantId, fileType);
        return sharedSchemaRepository.findByTenantAndFileType(tenantId, fileType);
    }
    
    /**
     * Get schemas that support count validation (for EOT processing)
     */
    @Cacheable(value = "sharedSchemas", key = "'count_validation_' + #tenantId")
    public List<SharedSchema> getCountValidationSchemas(String tenantId) {
        logger.debug("Loading count validation schemas for tenant: {}", tenantId);
        return sharedSchemaRepository.findCountValidationSchemas(tenantId);
    }
    
    /**
     * Create a new shared schema
     */
    @CacheEvict(value = "sharedSchemas", allEntries = true)
    public SharedSchema createSharedSchema(SharedSchema schema) {
        // Validate unique name for tenant
        if (sharedSchemaRepository.existsByTenantAndSchemaName(schema.getTenantId(), schema.getSchemaName(), null)) {
            throw new IllegalArgumentException("Schema with name '" + schema.getSchemaName() + "' already exists for this tenant");
        }
        
        schema.setCreatedBy(securityContextService.getCurrentUserId());
        schema.setCreatedAt(LocalDateTime.now());
        
        SharedSchema savedSchema = sharedSchemaRepository.save(schema);
        
        logger.info("Created new shared schema: {} for tenant: {}", savedSchema.getSchemaName(), savedSchema.getTenantId());
        return savedSchema;
    }
    
    /**
     * Update an existing shared schema
     */
    @CacheEvict(value = "sharedSchemas", allEntries = true)
    public SharedSchema updateSharedSchema(Long schemaId, SharedSchema updatedSchema) {
        SharedSchema existingSchema = sharedSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found with ID: " + schemaId));
        
        // Check if name change conflicts
        if (!existingSchema.getSchemaName().equals(updatedSchema.getSchemaName())) {
            if (sharedSchemaRepository.existsByTenantAndSchemaName(existingSchema.getTenantId(), updatedSchema.getSchemaName(), schemaId)) {
                throw new IllegalArgumentException("Schema with name '" + updatedSchema.getSchemaName() + "' already exists for this tenant");
            }
        }
        
        // Update fields
        existingSchema.setSchemaName(updatedSchema.getSchemaName());
        existingSchema.setSchemaVersion(updatedSchema.getSchemaVersion());
        existingSchema.setSchemaDefinition(updatedSchema.getSchemaDefinition());
        existingSchema.setDescription(updatedSchema.getDescription());
        existingSchema.setIsGlobal(updatedSchema.getIsGlobal());
        existingSchema.setIsActive(updatedSchema.getIsActive());
        existingSchema.setTags(updatedSchema.getTags());
        existingSchema.setEotCountFieldPath(updatedSchema.getEotCountFieldPath());
        existingSchema.setSupportsCountValidation(updatedSchema.getSupportsCountValidation());
        existingSchema.setUpdatedBy(securityContextService.getCurrentUserId());
        existingSchema.setUpdatedAt(LocalDateTime.now());
        
        SharedSchema savedSchema = sharedSchemaRepository.save(existingSchema);
        
        logger.info("Updated shared schema: {} for tenant: {}", savedSchema.getSchemaName(), savedSchema.getTenantId());
        return savedSchema;
    }
    
    /**
     * Delete a shared schema (only if not in use)
     */
    @CacheEvict(value = "sharedSchemas", allEntries = true)
    public void deleteSharedSchema(Long schemaId) {
        SharedSchema schema = sharedSchemaRepository.findById(schemaId)
            .orElseThrow(() -> new RuntimeException("Schema not found with ID: " + schemaId));
        
        // Check if schema is in active use
        long activeUsages = schemaUsageMappingRepository.countActiveUsages(schemaId);
        if (activeUsages > 0) {
            throw new IllegalStateException(String.format(
                "Cannot delete schema '%s'. It is currently used by %d subservice(s). " +
                "Please remove all usages before deletion.", 
                schema.getSchemaName(), activeUsages));
        }
        
        sharedSchemaRepository.delete(schema);
        
        logger.info("Deleted shared schema: {} (ID: {})", schema.getSchemaName(), schemaId);
    }
    
    /**
     * Get schema by ID
     */
    public Optional<SharedSchema> getSchemaById(Long schemaId) {
        return sharedSchemaRepository.findById(schemaId);
    }
    
    /**
     * Search schemas by name or description
     */
    public List<SharedSchema> searchSchemas(String tenantId, String searchTerm) {
        logger.debug("Searching schemas for tenant: {} with term: {}", tenantId, searchTerm);
        return sharedSchemaRepository.searchSchemas(tenantId, searchTerm);
    }
    
    /**
     * Get schemas by tags
     */
    public List<SharedSchema> getSchemasByTags(String tenantId, List<String> tags) {
        logger.debug("Loading schemas for tenant: {} with tags: {}", tenantId, tags);
        return sharedSchemaRepository.findByTenantAndTags(tenantId, tags);
    }
    
    /**
     * Get most popular schemas
     */
    public List<SharedSchema> getMostPopularSchemas(String tenantId) {
        return sharedSchemaRepository.findMostPopularSchemas(tenantId);
    }
    
    /**
     * Record schema usage
     */
    public void recordSchemaUsage(Long schemaId) {
        Optional<SharedSchema> schemaOpt = sharedSchemaRepository.findById(schemaId);
        if (schemaOpt.isPresent()) {
            SharedSchema schema = schemaOpt.get();
            schema.incrementUsageCount();
            sharedSchemaRepository.save(schema);
            
            logger.trace("Recorded usage for schema: {} (new count: {})", schema.getSchemaName(), schema.getUsageCount());
        }
    }
    
    /**
     * Get usage statistics
     */
    public Map<FileType, Long> getUsageStatisticsByFileType(String tenantId) {
        List<Object[]> stats = sharedSchemaRepository.getUsageStatisticsByFileType(tenantId);
        return stats.stream()
            .collect(Collectors.toMap(
                row -> (FileType) row[0],
                row -> ((Number) row[2]).longValue(), // totalUsage
                (existing, replacement) -> existing + replacement
            ));
    }
    
    /**
     * Create a new version of an existing schema
     */
    @CacheEvict(value = "sharedSchemas", allEntries = true)
    public SharedSchema createSchemaVersion(Long baseSchemaId, String newVersion, String schemaDefinition) {
        SharedSchema baseSchema = sharedSchemaRepository.findById(baseSchemaId)
            .orElseThrow(() -> new RuntimeException("Base schema not found with ID: " + baseSchemaId));
        
        // Check if version already exists
        Optional<SharedSchema> existingVersion = sharedSchemaRepository.findByTenantAndNameAndVersion(
            baseSchema.getTenantId(), baseSchema.getSchemaName(), newVersion);
        
        if (existingVersion.isPresent()) {
            throw new IllegalArgumentException("Version " + newVersion + " already exists for schema " + baseSchema.getSchemaName());
        }
        
        // Create new version
        SharedSchema newVersionSchema = new SharedSchema();
        newVersionSchema.setTenantId(baseSchema.getTenantId());
        newVersionSchema.setSchemaName(baseSchema.getSchemaName());
        newVersionSchema.setSchemaVersion(newVersion);
        newVersionSchema.setSchemaType(baseSchema.getSchemaType());
        newVersionSchema.setFileType(baseSchema.getFileType());
        newVersionSchema.setSchemaDefinition(schemaDefinition);
        newVersionSchema.setDescription(baseSchema.getDescription() + " (Version " + newVersion + ")");
        newVersionSchema.setIsGlobal(baseSchema.getIsGlobal());
        newVersionSchema.setTags(baseSchema.getTags());
        newVersionSchema.setCreatedBy(securityContextService.getCurrentUserId());
        
        SharedSchema savedSchema = sharedSchemaRepository.save(newVersionSchema);
        
        logger.info("Created new version {} of schema: {} for tenant: {}", newVersion, savedSchema.getSchemaName(), savedSchema.getTenantId());
        return savedSchema;
    }
    
    /**
     * Get all versions of a schema
     */
    public List<SharedSchema> getSchemaVersions(String tenantId, String schemaName) {
        return sharedSchemaRepository.searchSchemas(tenantId, schemaName)
            .stream()
            .filter(schema -> schema.getSchemaName().equals(schemaName))
            .collect(Collectors.toList());
    }
    
    /**
     * Clone schema for different tenant or as template
     */
    @CacheEvict(value = "sharedSchemas", allEntries = true)
    public SharedSchema cloneSchema(Long sourceSchemaId, String targetTenantId, String newName) {
        SharedSchema sourceSchema = sharedSchemaRepository.findById(sourceSchemaId)
            .orElseThrow(() -> new RuntimeException("Source schema not found with ID: " + sourceSchemaId));
        
        // Check if target name is available
        if (sharedSchemaRepository.existsByTenantAndSchemaName(targetTenantId, newName, null)) {
            throw new IllegalArgumentException("Schema with name '" + newName + "' already exists for target tenant");
        }
        
        SharedSchema clonedSchema = new SharedSchema();
        clonedSchema.setTenantId(targetTenantId);
        clonedSchema.setSchemaName(newName);
        clonedSchema.setSchemaVersion("1.0");
        clonedSchema.setSchemaType(sourceSchema.getSchemaType());
        clonedSchema.setFileType(sourceSchema.getFileType());
        clonedSchema.setSchemaDefinition(sourceSchema.getSchemaDefinition());
        clonedSchema.setDescription("Cloned from " + sourceSchema.getSchemaName());
        clonedSchema.setIsGlobal(false); // Cloned schemas are not global by default
        clonedSchema.setTags(sourceSchema.getTags());
        clonedSchema.setEotCountFieldPath(sourceSchema.getEotCountFieldPath());
        clonedSchema.setSupportsCountValidation(sourceSchema.getSupportsCountValidation());
        clonedSchema.setCreatedBy(securityContextService.getCurrentUserId());
        
        SharedSchema savedSchema = sharedSchemaRepository.save(clonedSchema);
        
        logger.info("Cloned schema {} to {} for tenant: {}", sourceSchema.getSchemaName(), newName, targetTenantId);
        return savedSchema;
    }
}