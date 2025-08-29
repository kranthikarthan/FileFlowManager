package com.filetransfer.web.service;

import com.filetransfer.web.entity.*;
import com.filetransfer.web.repository.DailyFileCountTrackerRepository;
import com.filetransfer.web.repository.SchemaUsageMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EotValidationServiceTest {

    @Mock
    private DailyFileCountTrackerRepository dailyFileCountRepository;

    @Mock
    private SchemaUsageMappingRepository schemaUsageMappingRepository;

    @Mock
    private CobolCopybookParser cobolCopybookParser;

    @InjectMocks
    private EotValidationService eotValidationService;

    private DailyFileCountTracker testTracker;
    private SharedSchema testSchema;
    private SchemaUsageMapping testMapping;
    private final String TEST_TENANT = "TEST_TENANT";
    private final String TEST_SERVICE = "PAYMENT_SERVICE";
    private final String TEST_SUBSERVICE = "INBOUND_PAYMENTS";
    private final LocalDate TEST_DATE = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        // Setup test tracker
        testTracker = new DailyFileCountTracker();
        testTracker.setId(1L);
        testTracker.setTenantId(TEST_TENANT);
        testTracker.setServiceName(TEST_SERVICE);
        testTracker.setSubServiceName(TEST_SUBSERVICE);
        testTracker.setProcessingDate(TEST_DATE);
        testTracker.setFileType(FileType.JSON);
        testTracker.setDirection(TransferDirection.INBOUND);
        testTracker.setActualCount(10);
        testTracker.setSotReceived(true);
        testTracker.setEotReceived(false);

        // Setup test schema
        testSchema = new SharedSchema();
        testSchema.setId(1L);
        testSchema.setSchemaName("EOT_JSON_Schema");
        testSchema.setFileType(FileType.JSON);
        testSchema.setSupportsCountValidation(true);
        testSchema.setEotCountFieldPath("header.recordCount");
        testSchema.setSchemaDefinition("{\"type\":\"object\",\"properties\":{\"header\":{\"type\":\"object\",\"properties\":{\"recordCount\":{\"type\":\"integer\"}}}}}");

        // Setup test mapping
        testMapping = new SchemaUsageMapping();
        testMapping.setSharedSchema(testSchema);
        testMapping.setDirection(TransferDirection.INBOUND);
    }

    @Test
    void testRecordDataFile() {
        // Given
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.recordDataFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.JSON, TransferDirection.INBOUND);

        // Then
        verify(dailyFileCountRepository).findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND);
        verify(dailyFileCountRepository).save(testTracker);
        assertEquals(11, testTracker.getActualCount()); // Should increment from 10 to 11
    }

    @Test
    void testRecordDataFile_NewTracker() {
        // Given
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.empty());
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.recordDataFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.JSON, TransferDirection.INBOUND);

        // Then
        verify(dailyFileCountRepository).save(any(DailyFileCountTracker.class));
    }

    @Test
    void testRecordSotFile() {
        // Given
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.recordSotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                         TEST_DATE, FileType.JSON, TransferDirection.INBOUND);

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getSotReceived());
    }

    @Test
    void testProcessEotFile_JsonSuccess() {
        // Given
        String jsonContent = "{\"header\":{\"recordCount\":10,\"timestamp\":\"2024-01-15T10:00:00\"}}";
        byte[] fileContent = jsonContent.getBytes();
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.JSON, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.json");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(10, testTracker.getEotCountValue());
        assertEquals(DailyFileCountTracker.ValidationStatus.MATCHED, testTracker.getValidationStatus());
    }

    @Test
    void testProcessEotFile_JsonDiscrepancy() {
        // Given
        String jsonContent = "{\"header\":{\"recordCount\":12,\"timestamp\":\"2024-01-15T10:00:00\"}}";
        byte[] fileContent = jsonContent.getBytes();
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.JSON, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.json");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(12, testTracker.getEotCountValue());
        assertEquals(DailyFileCountTracker.ValidationStatus.DISCREPANCY, testTracker.getValidationStatus());
        assertEquals(2, testTracker.getDiscrepancyCount()); // 12 - 10 = 2
    }

    @Test
    void testProcessEotFile_NoSchemaMapping() {
        // Given
        byte[] fileContent = "{}".getBytes();
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.JSON, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList());
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.JSON, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.json");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertNull(testTracker.getEotCountValue());
        assertEquals("No schema mapping configured", testTracker.getEotFieldPath());
    }

    @Test
    void testProcessEotFile_XmlSuccess() {
        // Given
        String xmlContent = "<?xml version=\"1.0\"?><eot><recordCount>10</recordCount><timestamp>2024-01-15T10:00:00</timestamp></eot>";
        byte[] fileContent = xmlContent.getBytes();
        
        testSchema.setFileType(FileType.XML);
        testSchema.setEotCountFieldPath("/eot/recordCount");
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.XML, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.XML, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.xml");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(10, testTracker.getEotCountValue());
    }

    @Test
    void testProcessEotFile_CsvSuccess() {
        // Given
        String csvContent = "recordCount,timestamp\n10,2024-01-15T10:00:00";
        byte[] fileContent = csvContent.getBytes();
        
        testSchema.setFileType(FileType.CSV);
        testSchema.setEotCountFieldPath("recordCount");
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.CSV, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.CSV, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.csv");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(10, testTracker.getEotCountValue());
    }

    @Test
    void testProcessEotFile_TextWithRegex() {
        // Given
        String textContent = "File processing completed. Records processed: 10";
        byte[] fileContent = textContent.getBytes();
        
        testSchema.setFileType(FileType.TEXT);
        testSchema.setEotCountFieldPath("regex:Records processed: (\\d+)");
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.TEXT, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.TEXT, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.txt");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(10, testTracker.getEotCountValue());
    }

    @Test
    void testProcessEotFile_TextWithPosition() {
        // Given
        String textContent = "0000000010RECORDS_PROCESSED";
        byte[] fileContent = textContent.getBytes();
        
        testSchema.setFileType(FileType.FIXED_WIDTH);
        testSchema.setEotCountFieldPath("pos:0-10");
        
        when(dailyFileCountRepository.findBySubServiceAndDate(
                TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, TEST_DATE, FileType.FIXED_WIDTH, TransferDirection.INBOUND))
                .thenReturn(Optional.of(testTracker));
        when(schemaUsageMappingRepository.findEotCountValidationMappings(TEST_TENANT))
                .thenReturn(Arrays.asList(testMapping));
        when(dailyFileCountRepository.save(any(DailyFileCountTracker.class))).thenReturn(testTracker);

        // When
        eotValidationService.processEotFile(TEST_TENANT, TEST_SERVICE, TEST_SUBSERVICE, 
                                          TEST_DATE, FileType.FIXED_WIDTH, TransferDirection.INBOUND,
                                          fileContent, "eot_20240115.dat");

        // Then
        verify(dailyFileCountRepository).save(testTracker);
        assertTrue(testTracker.getEotReceived());
        assertEquals(10, testTracker.getEotCountValue());
    }

    @Test
    void testGetValidationResults() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        List<DailyFileCountTracker> expectedResults = Arrays.asList(testTracker);
        
        when(dailyFileCountRepository.findByDateRange(TEST_TENANT, startDate, endDate))
                .thenReturn(expectedResults);

        // When
        List<DailyFileCountTracker> result = eotValidationService.getValidationResults(TEST_TENANT, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dailyFileCountRepository).findByDateRange(TEST_TENANT, startDate, endDate);
    }

    @Test
    void testGetPendingValidations() {
        // Given
        LocalDate cutoffDate = LocalDate.of(2024, 1, 14);
        List<DailyFileCountTracker> expectedResults = Arrays.asList(testTracker);
        
        when(dailyFileCountRepository.findPendingValidation(TEST_TENANT, cutoffDate))
                .thenReturn(expectedResults);

        // When
        List<DailyFileCountTracker> result = eotValidationService.getPendingValidations(TEST_TENANT, cutoffDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dailyFileCountRepository).findPendingValidation(TEST_TENANT, cutoffDate);
    }

    @Test
    void testGetDiscrepancies() {
        // Given
        List<DailyFileCountTracker> expectedResults = Arrays.asList(testTracker);
        
        when(dailyFileCountRepository.findDiscrepanciesByTenant(TEST_TENANT))
                .thenReturn(expectedResults);

        // When
        List<DailyFileCountTracker> result = eotValidationService.getDiscrepancies(TEST_TENANT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dailyFileCountRepository).findDiscrepanciesByTenant(TEST_TENANT);
    }

    @Test
    void testGetValidationStatistics() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        List<Object[]> mockStats = Arrays.asList(
                new Object[]{DailyFileCountTracker.ValidationStatus.MATCHED, 10L},
                new Object[]{DailyFileCountTracker.ValidationStatus.DISCREPANCY, 2L},
                new Object[]{DailyFileCountTracker.ValidationStatus.PENDING, 1L}
        );
        
        when(dailyFileCountRepository.getValidationStatistics(TEST_TENANT, startDate, endDate))
                .thenReturn(mockStats);

        // When
        EotValidationService.ValidationStatistics result = 
                eotValidationService.getValidationStatistics(TEST_TENANT, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getMatchedCount());
        assertEquals(2L, result.getDiscrepancyCount());
        assertEquals(1L, result.getPendingCount());
        assertEquals(13L, result.getTotalCount());
        assertTrue(result.getMatchPercentage() > 70.0); // 10/13 * 100
        verify(dailyFileCountRepository).getValidationStatistics(TEST_TENANT, startDate, endDate);
    }

    @Test
    void testExtractCountFromJson_NestedPath() throws Exception {
        // Given
        String jsonContent = "{\"header\":{\"summary\":{\"recordCount\":25}}}";
        testSchema.setEotCountFieldPath("header.summary.recordCount");

        // When
        int result = eotValidationService.extractCountFromJson(jsonContent, testSchema.getEotCountFieldPath());

        // Then
        assertEquals(25, result);
    }

    @Test
    void testExtractCountFromJson_InvalidPath() {
        // Given
        String jsonContent = "{\"header\":{\"recordCount\":10}}";
        String invalidPath = "header.invalidField";

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> eotValidationService.extractCountFromJson(jsonContent, invalidPath));
    }

    @Test
    void testValidationStatistics_Calculations() {
        // Given
        EotValidationService.ValidationStatistics stats = new EotValidationService.ValidationStatistics();
        stats.setMatchedCount(80L);
        stats.setDiscrepancyCount(15L);
        stats.setPendingCount(5L);

        // When & Then
        assertEquals(100L, stats.getTotalCount());
        assertEquals(80.0, stats.getMatchPercentage(), 0.01);
    }

    @Test
    void testValidationStatistics_ZeroTotal() {
        // Given
        EotValidationService.ValidationStatistics stats = new EotValidationService.ValidationStatistics();

        // When & Then
        assertEquals(0L, stats.getTotalCount());
        assertEquals(0.0, stats.getMatchPercentage(), 0.01);
    }
}