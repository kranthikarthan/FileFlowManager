package com.filetransfer.web.service;

import com.filetransfer.web.entity.SharedSchema;
import com.filetransfer.web.entity.FileType;
import com.filetransfer.web.repository.SharedSchemaRepository;
import com.filetransfer.web.repository.SchemaUsageMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedSchemaServiceTest {

    @Mock
    private SharedSchemaRepository sharedSchemaRepository;

    @Mock
    private SchemaUsageMappingRepository schemaUsageMappingRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private SharedSchemaService sharedSchemaService;

    private SharedSchema testSchema;
    private final String TEST_TENANT = "TEST_TENANT";
    private final String TEST_USER = "test_user";

    @BeforeEach
    void setUp() {
        testSchema = new SharedSchema();
        testSchema.setId(1L);
        testSchema.setTenantId(TEST_TENANT);
        testSchema.setSchemaName("TestSchema");
        testSchema.setSchemaVersion("1.0");
        testSchema.setSchemaType(SharedSchema.SchemaType.JSON_SCHEMA);
        testSchema.setFileType(FileType.JSON);
        testSchema.setSchemaDefinition("{\"type\":\"object\"}");
        testSchema.setDescription("Test schema");
        testSchema.setIsGlobal(false);
        testSchema.setIsActive(true);
        testSchema.setSupportsCountValidation(true);
        testSchema.setEotCountFieldPath("header.recordCount");

        when(securityContextService.getCurrentUserId()).thenReturn(TEST_USER);
    }

    @Test
    void testGetAvailableSchemasForTenant() {
        // Given
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.findAvailableSchemasForTenant(TEST_TENANT))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.getAvailableSchemasForTenant(TEST_TENANT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSchema.getSchemaName(), result.get(0).getSchemaName());
        verify(sharedSchemaRepository).findAvailableSchemasForTenant(TEST_TENANT);
    }

    @Test
    void testGetSchemasByFileType() {
        // Given
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.findByTenantAndFileType(TEST_TENANT, FileType.JSON))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.getSchemasByFileType(TEST_TENANT, FileType.JSON);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(FileType.JSON, result.get(0).getFileType());
        verify(sharedSchemaRepository).findByTenantAndFileType(TEST_TENANT, FileType.JSON);
    }

    @Test
    void testGetCountValidationSchemas() {
        // Given
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.findCountValidationSchemas(TEST_TENANT))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.getCountValidationSchemas(TEST_TENANT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getSupportsCountValidation());
        verify(sharedSchemaRepository).findCountValidationSchemas(TEST_TENANT);
    }

    @Test
    void testCreateSharedSchema_Success() {
        // Given
        SharedSchema newSchema = new SharedSchema();
        newSchema.setTenantId(TEST_TENANT);
        newSchema.setSchemaName("NewSchema");
        newSchema.setSchemaVersion("1.0");
        newSchema.setSchemaType(SharedSchema.SchemaType.JSON_SCHEMA);
        newSchema.setFileType(FileType.JSON);

        when(sharedSchemaRepository.existsByTenantAndSchemaName(TEST_TENANT, "NewSchema", null))
                .thenReturn(false);
        when(sharedSchemaRepository.save(any(SharedSchema.class))).thenReturn(newSchema);

        // When
        SharedSchema result = sharedSchemaService.createSharedSchema(newSchema);

        // Then
        assertNotNull(result);
        assertEquals("NewSchema", result.getSchemaName());
        verify(sharedSchemaRepository).existsByTenantAndSchemaName(TEST_TENANT, "NewSchema", null);
        verify(sharedSchemaRepository).save(newSchema);
    }

    @Test
    void testCreateSharedSchema_DuplicateName() {
        // Given
        SharedSchema newSchema = new SharedSchema();
        newSchema.setTenantId(TEST_TENANT);
        newSchema.setSchemaName("ExistingSchema");
        
        when(sharedSchemaRepository.existsByTenantAndSchemaName(TEST_TENANT, "ExistingSchema", null))
                .thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> sharedSchemaService.createSharedSchema(newSchema));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(sharedSchemaRepository, never()).save(any());
    }

    @Test
    void testUpdateSharedSchema_Success() {
        // Given
        Long schemaId = 1L;
        SharedSchema updatedSchema = new SharedSchema();
        updatedSchema.setSchemaName("UpdatedSchema");
        updatedSchema.setSchemaVersion("2.0");
        updatedSchema.setDescription("Updated description");

        when(sharedSchemaRepository.findById(schemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.existsByTenantAndSchemaName(TEST_TENANT, "UpdatedSchema", schemaId))
                .thenReturn(false);
        when(sharedSchemaRepository.save(any(SharedSchema.class))).thenReturn(testSchema);

        // When
        SharedSchema result = sharedSchemaService.updateSharedSchema(schemaId, updatedSchema);

        // Then
        assertNotNull(result);
        verify(sharedSchemaRepository).findById(schemaId);
        verify(sharedSchemaRepository).save(testSchema);
    }

    @Test
    void testUpdateSharedSchema_NotFound() {
        // Given
        Long schemaId = 999L;
        SharedSchema updatedSchema = new SharedSchema();
        
        when(sharedSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sharedSchemaService.updateSharedSchema(schemaId, updatedSchema));
        
        assertTrue(exception.getMessage().contains("not found"));
        verify(sharedSchemaRepository, never()).save(any());
    }

    @Test
    void testDeleteSharedSchema_Success() {
        // Given
        Long schemaId = 1L;
        when(sharedSchemaRepository.findById(schemaId)).thenReturn(Optional.of(testSchema));
        when(schemaUsageMappingRepository.countActiveUsages(schemaId)).thenReturn(0L);

        // When
        sharedSchemaService.deleteSharedSchema(schemaId);

        // Then
        verify(sharedSchemaRepository).findById(schemaId);
        verify(schemaUsageMappingRepository).countActiveUsages(schemaId);
        verify(sharedSchemaRepository).delete(testSchema);
    }

    @Test
    void testDeleteSharedSchema_InUse() {
        // Given
        Long schemaId = 1L;
        when(sharedSchemaRepository.findById(schemaId)).thenReturn(Optional.of(testSchema));
        when(schemaUsageMappingRepository.countActiveUsages(schemaId)).thenReturn(3L);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> sharedSchemaService.deleteSharedSchema(schemaId));
        
        assertTrue(exception.getMessage().contains("currently used"));
        verify(sharedSchemaRepository, never()).delete(any());
    }

    @Test
    void testCreateSchemaVersion_Success() {
        // Given
        Long baseSchemaId = 1L;
        String newVersion = "2.0";
        String newDefinition = "{\"type\":\"object\",\"updated\":true}";
        
        when(sharedSchemaRepository.findById(baseSchemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.findByTenantAndNameAndVersion(TEST_TENANT, "TestSchema", newVersion))
                .thenReturn(Optional.empty());
        when(sharedSchemaRepository.save(any(SharedSchema.class))).thenReturn(testSchema);

        // When
        SharedSchema result = sharedSchemaService.createSchemaVersion(baseSchemaId, newVersion, newDefinition);

        // Then
        assertNotNull(result);
        verify(sharedSchemaRepository).findById(baseSchemaId);
        verify(sharedSchemaRepository).findByTenantAndNameAndVersion(TEST_TENANT, "TestSchema", newVersion);
        verify(sharedSchemaRepository).save(any(SharedSchema.class));
    }

    @Test
    void testCreateSchemaVersion_VersionExists() {
        // Given
        Long baseSchemaId = 1L;
        String existingVersion = "1.0";
        String newDefinition = "{\"type\":\"object\"}";
        
        when(sharedSchemaRepository.findById(baseSchemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.findByTenantAndNameAndVersion(TEST_TENANT, "TestSchema", existingVersion))
                .thenReturn(Optional.of(testSchema));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sharedSchemaService.createSchemaVersion(baseSchemaId, existingVersion, newDefinition));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(sharedSchemaRepository, never()).save(any());
    }

    @Test
    void testCloneSchema_Success() {
        // Given
        Long sourceSchemaId = 1L;
        String targetTenantId = "TARGET_TENANT";
        String newName = "ClonedSchema";
        
        when(sharedSchemaRepository.findById(sourceSchemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.existsByTenantAndSchemaName(targetTenantId, newName, null))
                .thenReturn(false);
        when(sharedSchemaRepository.save(any(SharedSchema.class))).thenReturn(testSchema);

        // When
        SharedSchema result = sharedSchemaService.cloneSchema(sourceSchemaId, targetTenantId, newName);

        // Then
        assertNotNull(result);
        verify(sharedSchemaRepository).findById(sourceSchemaId);
        verify(sharedSchemaRepository).existsByTenantAndSchemaName(targetTenantId, newName, null);
        verify(sharedSchemaRepository).save(any(SharedSchema.class));
    }

    @Test
    void testCloneSchema_NameExists() {
        // Given
        Long sourceSchemaId = 1L;
        String targetTenantId = "TARGET_TENANT";
        String existingName = "ExistingSchema";
        
        when(sharedSchemaRepository.findById(sourceSchemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.existsByTenantAndSchemaName(targetTenantId, existingName, null))
                .thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sharedSchemaService.cloneSchema(sourceSchemaId, targetTenantId, existingName));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(sharedSchemaRepository, never()).save(any());
    }

    @Test
    void testRecordSchemaUsage() {
        // Given
        Long schemaId = 1L;
        testSchema.setUsageCount(5);
        when(sharedSchemaRepository.findById(schemaId)).thenReturn(Optional.of(testSchema));
        when(sharedSchemaRepository.save(testSchema)).thenReturn(testSchema);

        // When
        sharedSchemaService.recordSchemaUsage(schemaId);

        // Then
        verify(sharedSchemaRepository).findById(schemaId);
        verify(sharedSchemaRepository).save(testSchema);
        assertEquals(6, testSchema.getUsageCount());
    }

    @Test
    void testSearchSchemas() {
        // Given
        String searchTerm = "test";
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.searchSchemas(TEST_TENANT, searchTerm))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.searchSchemas(TEST_TENANT, searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sharedSchemaRepository).searchSchemas(TEST_TENANT, searchTerm);
    }

    @Test
    void testGetSchemasByTags() {
        // Given
        List<String> tags = Arrays.asList("payment", "validation");
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.findByTenantAndTags(TEST_TENANT, tags))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.getSchemasByTags(TEST_TENANT, tags);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sharedSchemaRepository).findByTenantAndTags(TEST_TENANT, tags);
    }

    @Test
    void testGetMostPopularSchemas() {
        // Given
        List<SharedSchema> expectedSchemas = Arrays.asList(testSchema);
        when(sharedSchemaRepository.findMostPopularSchemas(TEST_TENANT))
                .thenReturn(expectedSchemas);

        // When
        List<SharedSchema> result = sharedSchemaService.getMostPopularSchemas(TEST_TENANT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sharedSchemaRepository).findMostPopularSchemas(TEST_TENANT);
    }
}