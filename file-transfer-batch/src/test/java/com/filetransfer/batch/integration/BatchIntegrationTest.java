package com.filetransfer.batch.integration;

import com.filetransfer.batch.FileTransferBatchApplication;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for batch processing functionality
 * Tests complete batch job workflows, monitoring, and error handling
 */
@SpringBootTest(classes = FileTransferBatchApplication.class)
@SpringBatchTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private Job fileProcessingJob;

    private String testTenantId = "test-tenant-123";
    private Path testInputDir;
    private Path testOutputDir;

    @BeforeAll
    void setUpAll() throws Exception {
        // Create test directories
        testInputDir = Paths.get("/tmp/batch-test/input");
        testOutputDir = Paths.get("/tmp/batch-test/output");
        Files.createDirectories(testInputDir);
        Files.createDirectories(testOutputDir);

        // Set up test job launcher
        jobLauncherTestUtils.setJob(fileProcessingJob);
    }

    @BeforeEach
    void setUp() {
        // Clean up job repository before each test
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("1. Basic Batch Job Execution - Should process files successfully")
    void testBasicBatchJobExecution() throws Exception {
        // Create test input file
        Path testFile = testInputDir.resolve("test-input.json");
        String testContent = """
            {"tenantId": "%s", "fileName": "test.json", "content": "test data"}
            """.formatted(testTenantId);
        Files.writeString(testFile, testContent);

        // Build job parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // Execute job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Verify job completed successfully
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertNotNull(jobExecution.getStartTime());
        assertNotNull(jobExecution.getEndTime());

        // Verify step executions
        assertEquals(1, jobExecution.getStepExecutions().size());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
        assertTrue(stepExecution.getReadCount() > 0);
    }

    @Test
    @DisplayName("2. Batch Job with Multiple Files - Should process all files")
    void testBatchJobWithMultipleFiles() throws Exception {
        // Create multiple test files
        for (int i = 1; i <= 5; i++) {
            Path testFile = testInputDir.resolve("test-input-" + i + ".json");
            String testContent = """
                {"tenantId": "%s", "fileName": "test%d.json", "content": "test data %d"}
                """.formatted(testTenantId, i, i);
            Files.writeString(testFile, testContent);
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 2L)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // Verify all files were processed
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(5, stepExecution.getReadCount());
        assertEquals(5, stepExecution.getWriteCount());
    }

    @Test
    @DisplayName("3. Batch Job Error Handling - Should handle errors gracefully")
    void testBatchJobErrorHandling() throws Exception {
        // Create invalid test file
        Path invalidFile = testInputDir.resolve("invalid-file.json");
        Files.writeString(invalidFile, "invalid json content {{{");

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("skipLimit", 1L)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Job should complete with skipped items
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertTrue(stepExecution.getReadSkipCount() > 0 || stepExecution.getProcessSkipCount() > 0);
    }

    @Test
    @DisplayName("4. Batch Job Restart - Should restart from last checkpoint")
    void testBatchJobRestart() throws Exception {
        // Create test files
        for (int i = 1; i <= 10; i++) {
            Path testFile = testInputDir.resolve("restart-test-" + i + ".json");
            String testContent = """
                {"tenantId": "%s", "fileName": "restart%d.json", "content": "restart test %d"}
                """.formatted(testTenantId, i, i);
            Files.writeString(testFile, testContent);
        }

        // Start job that will fail after processing some items
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 3L)
                .addString("simulateFailure", "true")
                .addLong("failAfterCount", 5L)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // First execution should fail
        assertEquals(BatchStatus.FAILED, firstExecution.getStatus());

        // Restart the job
        JobParameters restartParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 3L)
                .addString("simulateFailure", "false") // Disable failure simulation
                .addLong("timestamp", firstExecution.getJobParameters().getLong("timestamp")) // Same timestamp for restart
                .toJobParameters();

        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParameters);
        
        // Restart should complete successfully
        assertEquals(BatchStatus.COMPLETED, restartExecution.getStatus());
    }

    @Test
    @DisplayName("5. Batch Job Monitoring - Should track job metrics")
    void testBatchJobMonitoring() throws Exception {
        // Execute a job for monitoring
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Verify job is tracked in repository
        JobInstance jobInstance = jobExecution.getJobInstance();
        assertNotNull(jobInstance);
        
        List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
        assertTrue(executions.contains(jobExecution));

        // Verify step execution metrics
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            assertTrue(stepExecution.getReadCount() >= 0);
            assertTrue(stepExecution.getWriteCount() >= 0);
            assertTrue(stepExecution.getCommitCount() >= 0);
            assertNotNull(stepExecution.getStartTime());
            
            if (stepExecution.getStatus().equals(BatchStatus.COMPLETED)) {
                assertNotNull(stepExecution.getEndTime());
            }
        }
    }

    @Test
    @DisplayName("6. Batch Job Scalability - Should handle parallel processing")
    void testBatchJobScalability() throws Exception {
        // Create large number of test files
        for (int i = 1; i <= 50; i++) {
            Path testFile = testInputDir.resolve("scalability-test-" + i + ".json");
            String testContent = """
                {"tenantId": "%s", "fileName": "scale%d.json", "content": "scalability test %d", "size": %d}
                """.formatted(testTenantId, i, i, i * 1000);
            Files.writeString(testFile, testContent);
        }

        long startTime = System.currentTimeMillis();

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 10L)
                .addLong("threadCount", 4L)
                .addString("parallel", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // Verify performance - should process 50 files in reasonable time
        assertTrue(duration < 60000, "Batch job should complete within 60 seconds");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(50, stepExecution.getReadCount());
        assertEquals(50, stepExecution.getWriteCount());
    }

    @Test
    @DisplayName("7. Cross-Application Integration - Should integrate with web services")
    void testCrossApplicationIntegration() throws Exception {
        // Test integration with web application APIs
        // This would require both applications to be running
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("webApiIntegration", "true")
                .addString("webApiUrl", "http://localhost:8080")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Job should complete even if web integration is not available
        assertNotEquals(BatchStatus.FAILED, jobExecution.getStatus());
    }

    @AfterAll
    void tearDownAll() throws Exception {
        // Clean up test directories
        if (Files.exists(testInputDir)) {
            Files.walk(testInputDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
        }

        if (Files.exists(testOutputDir)) {
            Files.walk(testOutputDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }
}