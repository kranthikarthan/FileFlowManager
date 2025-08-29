package com.filetransfer.web.service;

import com.filetransfer.web.entity.SubServiceConfiguration;
import com.filetransfer.web.entity.CutOffTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FileNamingConventionServiceTest {

    private FileNamingConventionService service;
    private SubServiceConfiguration config;

    @BeforeEach
    void setUp() {
        service = new FileNamingConventionService();
        
        config = new SubServiceConfiguration();
        config.setServiceName("TestService");
        config.setSubServiceName("TestSubService");
        config.setStartMarkerPrefix("SOT_");
        config.setEndMarkerPrefix("EOT_");
        config.setSotFilePattern("SOT_*");
        config.setEotFilePattern("EOT_*");
        config.setDataFilePattern("*.*");
    }

    @Test
    void testValidateSotFileName() {
        String fileName = "SOT_20240115.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid());
        assertEquals(LocalDate.of(2024, 1, 15), result.getExtractedDate());
        assertFalse(result.hasIssues());
    }

    @Test
    void testValidateEotFileName() {
        String fileName = "EOT_20240115.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.EOT);
        
        assertTrue(result.isValid());
        assertEquals(LocalDate.of(2024, 1, 15), result.getExtractedDate());
    }

    @Test
    void testValidateDataFileName() {
        String fileName = "DATA_TestService_20240115_001.csv";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.DATA);
        
        assertTrue(result.isValid());
        assertEquals(LocalDate.of(2024, 1, 15), result.getExtractedDate());
        assertTrue(result.getInfo().stream().anyMatch(info -> info.contains("sequence number")));
    }

    @Test
    void testInvalidSotPrefix() {
        String fileName = "WRONG_20240115.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("must start with prefix")));
    }

    @Test
    void testInvalidEotPrefix() {
        String fileName = "WRONG_20240115.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.EOT);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("must start with prefix")));
    }

    @Test
    void testDateExtractionFormats() {
        // Test YYYYMMDD format
        String fileName1 = "SOT_20240115.dat";
        FileNamingConventionService.NamingValidationResult result1 = 
            service.validateFileName(fileName1, config, FileNamingConventionService.FileNameType.SOT);
        assertEquals(LocalDate.of(2024, 1, 15), result1.getExtractedDate());

        // Test YYMMDD format
        String fileName2 = "SOT_240115.dat";
        FileNamingConventionService.NamingValidationResult result2 = 
            service.validateFileName(fileName2, config, FileNamingConventionService.FileNameType.SOT);
        assertEquals(LocalDate.of(2024, 1, 15), result2.getExtractedDate());

        // Test YYYY-MM-DD format
        String fileName3 = "SOT_2024-01-15.dat";
        FileNamingConventionService.NamingValidationResult result3 = 
            service.validateFileName(fileName3, config, FileNamingConventionService.FileNameType.SOT);
        assertEquals(LocalDate.of(2024, 1, 15), result3.getExtractedDate());

        // Test DD-MM-YYYY format
        String fileName4 = "SOT_15-01-2024.dat";
        FileNamingConventionService.NamingValidationResult result4 = 
            service.validateFileName(fileName4, config, FileNamingConventionService.FileNameType.SOT);
        assertEquals(LocalDate.of(2024, 1, 15), result4.getExtractedDate());
    }

    @Test
    void testNoDateInFileName() {
        String fileName = "SOT_NODATE.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid()); // Valid pattern, but warning about date
        assertNull(result.getExtractedDate());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("Could not extract date")));
    }

    @Test
    void testFutureDateWarning() {
        LocalDate futureDate = LocalDate.now().plusDays(400); // Over a year in future
        String fileName = "SOT_" + futureDate.toString().replace("-", "") + ".dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("more than a year in the future")));
    }

    @Test
    void testOldDateWarning() {
        LocalDate oldDate = LocalDate.now().minusDays(400); // Over a year old
        String fileName = "SOT_" + oldDate.toString().replace("-", "") + ".dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("more than a year old")));
    }

    @Test
    void testTimestampValidation() {
        String fileName = "SOT_20240115_123456.dat"; // With timestamp
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid());
        assertTrue(result.getInfo().stream().anyMatch(info -> info.contains("timestamp: 12:34:56")));
    }

    @Test
    void testInvalidTimestamp() {
        String fileName = "SOT_20240115_256070.dat"; // Invalid time 25:60:70
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Invalid timestamp")));
    }

    @Test
    void testDataFileSequenceNumbers() {
        String fileName = "DATA_20240115_001.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.DATA);
        
        assertTrue(result.isValid());
        assertTrue(result.getInfo().stream().anyMatch(info -> info.contains("sequence number: 001")));
    }

    @Test
    void testDataFileHighSequenceWarning() {
        String fileName = "DATA_20240115_9999.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.DATA);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("unusually high")));
    }

    @Test
    void testFileExtensionDetection() {
        // Test XML file
        String xmlFile = "DATA_20240115.xml";
        FileNamingConventionService.NamingValidationResult xmlResult = 
            service.validateFileName(xmlFile, config, FileNamingConventionService.FileNameType.DATA);
        assertTrue(xmlResult.getInfo().stream().anyMatch(info -> info.contains("XML data file detected")));

        // Test JSON file
        String jsonFile = "DATA_20240115.json";
        FileNamingConventionService.NamingValidationResult jsonResult = 
            service.validateFileName(jsonFile, config, FileNamingConventionService.FileNameType.DATA);
        assertTrue(jsonResult.getInfo().stream().anyMatch(info -> info.contains("JSON data file detected")));

        // Test CSV file
        String csvFile = "DATA_20240115.csv";
        FileNamingConventionService.NamingValidationResult csvResult = 
            service.validateFileName(csvFile, config, FileNamingConventionService.FileNameType.DATA);
        assertTrue(csvResult.getInfo().stream().anyMatch(info -> info.contains("CSV data file detected")));
    }

    @Test
    void testSotFileExtensionWarning() {
        String fileName = "SOT_20240115.pdf"; // Unusual extension for SOT
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("should typically have .txt or .dat extension")));
    }

    @Test
    void testEotFileSummaryInfo() {
        String fileName = "EOT_SUMMARY_20240115.dat";
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.EOT);
        
        assertTrue(result.isValid());
        assertTrue(result.getInfo().stream().anyMatch(info -> info.contains("summary information")));
    }

    @Test
    void testGenerateSuggestedFileNames() {
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        
        // Test SOT suggestions
        List<String> sotSuggestions = service.generateSuggestedFileNames(config, 
            FileNamingConventionService.FileNameType.SOT, testDate);
        
        assertFalse(sotSuggestions.isEmpty());
        assertTrue(sotSuggestions.stream().anyMatch(name -> name.startsWith("SOT_")));
        assertTrue(sotSuggestions.stream().anyMatch(name -> name.contains("20240115")));

        // Test EOT suggestions
        List<String> eotSuggestions = service.generateSuggestedFileNames(config, 
            FileNamingConventionService.FileNameType.EOT, testDate);
        
        assertFalse(eotSuggestions.isEmpty());
        assertTrue(eotSuggestions.stream().anyMatch(name -> name.startsWith("EOT_")));

        // Test DATA suggestions
        List<String> dataSuggestions = service.generateSuggestedFileNames(config, 
            FileNamingConventionService.FileNameType.DATA, testDate);
        
        assertFalse(dataSuggestions.isEmpty());
        assertTrue(dataSuggestions.stream().anyMatch(name -> name.contains("TestService")));
    }

    @Test
    void testCustomPatterns() {
        config.setSotFilePattern("SOT_TestService_*.dat");
        config.setEotFilePattern("EOT_TestService_*.dat");
        config.setDataFilePattern("TestService_*.csv");

        // Test matching custom pattern
        String fileName1 = "SOT_TestService_20240115.dat";
        FileNamingConventionService.NamingValidationResult result1 = 
            service.validateFileName(fileName1, config, FileNamingConventionService.FileNameType.SOT);
        assertTrue(result1.isValid());

        // Test not matching custom pattern
        String fileName2 = "SOT_20240115.dat";
        FileNamingConventionService.NamingValidationResult result2 = 
            service.validateFileName(fileName2, config, FileNamingConventionService.FileNameType.SOT);
        assertFalse(result2.isValid());
        assertTrue(result2.getErrors().stream().anyMatch(error -> error.contains("does not match pattern")));
    }

    @Test
    void testValidationResultSummary() {
        FileNamingConventionService.NamingValidationResult result = 
            new FileNamingConventionService.NamingValidationResult();
        
        result.addError("Test error");
        result.addWarning("Test warning");
        result.addInfo("Test info");

        String summary = result.getSummary();
        assertTrue(summary.contains("Errors: Test error"));
        assertTrue(summary.contains("Warnings: Test warning"));
        assertTrue(summary.contains("Info: Test info"));
    }

    @Test
    void testEmptyPatterns() {
        config.setSotFilePattern("");
        config.setEotFilePattern("");
        config.setDataFilePattern("");

        String fileName = "SOT_20240115.dat";
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid()); // Should use default patterns
    }

    @Test
    void testNullPatterns() {
        config.setSotFilePattern(null);
        config.setEotFilePattern(null);
        config.setDataFilePattern(null);

        String fileName = "SOT_20240115.dat";
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        assertTrue(result.isValid()); // Should use default patterns
    }

    @Test
    void testExceptionHandling() {
        // Test with invalid file name that might cause parsing errors
        String fileName = "SOT_\u0000\u0001\u0002.dat"; // Control characters
        
        FileNamingConventionService.NamingValidationResult result = 
            service.validateFileName(fileName, config, FileNamingConventionService.FileNameType.SOT);
        
        // Should not throw exception, but may have validation issues
        assertNotNull(result);
    }
}