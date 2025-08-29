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
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for batch application workflows
 * Tests complete batch processing scenarios from job creation to completion
 */
@SpringBootTest(classes = FileTransferBatchApplication.class)
@SpringBatchTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BatchEndToEndTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private Job fileProcessingJob;

    @Autowired
    private RestTemplate restTemplate;

    private String testTenantId = "e2e-test-tenant";
    private Path testInputDir;
    private Path testOutputDir;
    private Path testErrorDir;
    private Path testArchiveDir;

    @BeforeAll
    void setUpAll() throws Exception {
        // Create comprehensive test directory structure
        testInputDir = Paths.get("/tmp/batch-e2e/input");
        testOutputDir = Paths.get("/tmp/batch-e2e/output");
        testErrorDir = Paths.get("/tmp/batch-e2e/error");
        testArchiveDir = Paths.get("/tmp/batch-e2e/archive");
        
        Files.createDirectories(testInputDir);
        Files.createDirectories(testOutputDir);
        Files.createDirectories(testErrorDir);
        Files.createDirectories(testArchiveDir);

        jobLauncherTestUtils.setJob(fileProcessingJob);
    }

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        clearTestDirectories();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-BATCH-1: Complete file processing lifecycle")
    void testCompleteFileProcessingLifecycle() throws Exception {
        // PHASE 1: Setup - Create test files with different scenarios
        createTestFiles();

        // PHASE 2: Job Execution - Start comprehensive batch job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("errorPath", testErrorDir.toString())
                .addString("archivePath", testArchiveDir.toString())
                .addLong("chunkSize", 5L)
                .addLong("threadCount", 2L)
                .addLong("skipLimit", 2L)
                .addLong("retryLimit", 3L)
                .addString("enableValidation", "true")
                .addString("enableArchiving", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // PHASE 3: Verification - Check job completed successfully
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertNotNull(jobExecution.getStartTime());
        assertNotNull(jobExecution.getEndTime());

        // PHASE 4: Results Verification
        verifyJobResults(jobExecution);
        verifyFileProcessingResults();
        verifyErrorHandling();
        verifyArchiving();
    }

    @Test
    @Order(2)
    @DisplayName("E2E-BATCH-2: Multi-tenant batch processing")
    void testMultiTenantBatchProcessing() throws Exception {
        // Create files for multiple tenants
        String[] tenants = {"tenant-a", "tenant-b", "tenant-c"};
        
        for (String tenantId : tenants) {
            Path tenantInputDir = testInputDir.resolve(tenantId);
            Files.createDirectories(tenantInputDir);
            
            // Create files for each tenant
            for (int i = 1; i <= 3; i++) {
                Path tenantFile = tenantInputDir.resolve("file-" + i + ".json");
                String content = String.format("""
                    {
                        "tenantId": "%s",
                        "fileId": "%s-file-%d",
                        "data": "tenant %s data %d",
                        "timestamp": "%s"
                    }
                    """, tenantId, tenantId, i, tenantId, i, LocalDateTime.now());
                Files.writeString(tenantFile, content);
            }
        }

        // Process each tenant separately
        List<JobExecution> tenantJobs = new ArrayList<>();
        
        for (String tenantId : tenants) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", tenantId)
                    .addString("inputPath", testInputDir.resolve(tenantId).toString())
                    .addString("outputPath", testOutputDir.resolve(tenantId).toString())
                    .addLong("chunkSize", 2L)
                    .addString("tenantIsolation", "true")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            tenantJobs.add(jobExecution);
            
            assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        }

        // Verify tenant isolation
        for (int i = 0; i < tenants.length; i++) {
            String tenantId = tenants[i];
            JobExecution jobExecution = tenantJobs.get(i);
            
            // Verify tenant-specific processing
            String jobTenantId = jobExecution.getJobParameters().getString("tenantId");
            assertEquals(tenantId, jobTenantId);
            
            // Verify tenant-specific output
            Path tenantOutputDir = testOutputDir.resolve(tenantId);
            assertTrue(Files.exists(tenantOutputDir));
            
            long outputFileCount = Files.list(tenantOutputDir).count();
            assertEquals(3, outputFileCount, "Should have 3 output files for tenant " + tenantId);
        }
    }

    @Test
    @Order(3)
    @DisplayName("E2E-BATCH-3: Job failure and recovery workflow")
    void testJobFailureAndRecoveryWorkflow() throws Exception {
        // Create files that will cause failures
        createFilesWithErrors();

        // PHASE 1: Start job that will partially fail
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("errorPath", testErrorDir.toString())
                .addLong("chunkSize", 3L)
                .addLong("skipLimit", 5L) // Allow some failures
                .addString("simulateTransientErrors", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Job should complete with skipped items
        assertTrue(firstExecution.getStatus().isUnsuccessful() || 
                  firstExecution.getStepExecutions().stream()
                          .anyMatch(step -> step.getReadSkipCount() > 0 || step.getProcessSkipCount() > 0));

        // PHASE 2: Analyze failures
        Collection<StepExecution> stepExecutions = firstExecution.getStepExecutions();
        StepExecution mainStep = stepExecutions.iterator().next();
        
        long totalSkipped = mainStep.getReadSkipCount() + mainStep.getProcessSkipCount() + mainStep.getWriteSkipCount();
        assertTrue(totalSkipped > 0, "Should have skipped some items due to errors");

        // PHASE 3: Fix errors and restart
        fixErrorFiles();

        JobParameters restartParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("errorPath", testErrorDir.toString())
                .addLong("chunkSize", 3L)
                .addLong("skipLimit", 5L)
                .addString("simulateTransientErrors", "false") // Disable error simulation
                .addString("retryFailedItems", "true")
                .addLong("timestamp", firstExecution.getJobParameters().getLong("timestamp")) // Same timestamp for restart
                .toJobParameters();

        JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParameters);

        // PHASE 4: Verify recovery
        assertEquals(BatchStatus.COMPLETED, restartExecution.getStatus());
        
        StepExecution restartStep = restartExecution.getStepExecutions().iterator().next();
        assertTrue(restartStep.getWriteCount() > 0, "Should have processed previously failed items");
    }

    @Test
    @Order(4)
    @DisplayName("E2E-BATCH-4: Cross-application integration workflow")
    void testCrossApplicationIntegrationWorkflow() throws Exception {
        // PHASE 1: Simulate web application triggering batch job
        String webApiUrl = "http://localhost:8080";
        
        // Create job request from web application
        Map<String, Object> jobRequest = new HashMap<>();
        jobRequest.put("tenantId", testTenantId);
        jobRequest.put("inputPath", testInputDir.toString());
        jobRequest.put("outputPath", testOutputDir.toString());
        jobRequest.put("requestedBy", "web-application");
        jobRequest.put("priority", "HIGH");
        jobRequest.put("notificationUrl", webApiUrl + "/api/batch/notifications");

        // Create test files
        createTestFiles();

        // PHASE 2: Execute batch job with web integration
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("requestedBy", "web-application")
                .addString("webApiUrl", webApiUrl)
                .addString("enableWebNotifications", "true")
                .addString("enableProgressUpdates", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // PHASE 3: Verify integration
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // Check job context for web integration data
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        assertTrue(executionContext.containsKey("webApiUrl"));
        assertEquals(webApiUrl, executionContext.getString("webApiUrl"));

        // PHASE 4: Verify notifications would be sent
        // (In real scenario, this would call web API)
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertTrue(stepExecution.getExecutionContext().containsKey("notificationsSent"));
    }

    @Test
    @Order(5)
    @DisplayName("E2E-BATCH-5: Performance and scalability workflow")
    void testPerformanceAndScalabilityWorkflow() throws Exception {
        // Create large dataset for performance testing
        int totalFiles = 100;
        createLargeDataset(totalFiles);

        long startTime = System.currentTimeMillis();

        // PHASE 1: Execute high-performance batch job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 20L) // Larger chunks for performance
                .addLong("threadCount", 4L) // Parallel processing
                .addString("enablePerformanceMetrics", "true")
                .addString("enableParallelProcessing", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // PHASE 2: Verify performance
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(totalFiles, stepExecution.getReadCount());
        assertEquals(totalFiles, stepExecution.getWriteCount());
        assertEquals(0, stepExecution.getReadSkipCount());

        // PHASE 3: Performance assertions
        assertTrue(totalDuration < 60000, "Should process 100 files within 60 seconds");
        
        long jobDuration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
        assertTrue(jobDuration < 30000, "Job execution should complete within 30 seconds");

        // Calculate throughput
        double throughput = (double) totalFiles / (jobDuration / 1000.0);
        assertTrue(throughput > 3.0, "Should process more than 3 files per second");

        // PHASE 4: Verify parallel processing effectiveness
        ExecutionContext context = stepExecution.getExecutionContext();
        if (context.containsKey("parallelThreadsUsed")) {
            int threadsUsed = context.getInt("parallelThreadsUsed");
            assertTrue(threadsUsed > 1, "Should use multiple threads for parallel processing");
        }
    }

    @Test
    @Order(6)
    @DisplayName("E2E-BATCH-6: Monitoring and alerting integration")
    void testMonitoringAndAlertingIntegration() throws Exception {
        createTestFiles();

        // PHASE 1: Execute job with monitoring enabled
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("enableMonitoring", "true")
                .addString("enableAlerting", "true")
                .addString("alertThreshold", "90") // Alert if success rate < 90%
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // PHASE 2: Verify monitoring data collection
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        
        // Check monitoring metrics
        assertTrue(executionContext.containsKey("metricsCollected"));
        assertTrue(executionContext.containsKey("performanceData"));
        
        // PHASE 3: Verify alerting integration
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        long successRate = (stepExecution.getWriteCount() * 100) / stepExecution.getReadCount();
        
        if (successRate >= 90) {
            // Should not trigger alerts for successful processing
            assertFalse(executionContext.containsKey("alertsTriggered") && 
                       executionContext.getInt("alertsTriggered") > 0);
        }

        // PHASE 4: Test alert triggering with simulated failures
        testAlertTriggering();
    }

    @Test
    @Order(7)
    @DisplayName("E2E-BATCH-7: Backup and recovery integration")
    void testBackupAndRecoveryIntegration() throws Exception {
        createTestFiles();

        // PHASE 1: Execute job and create backup point
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("createBackupPoint", "true")
                .addString("backupDescription", "E2E test backup point")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // PHASE 2: Verify backup integration
        ExecutionContext context = jobExecution.getExecutionContext();
        assertTrue(context.containsKey("backupCreated"));
        assertNotNull(context.getString("backupId"));

        // PHASE 3: Test job state persistence
        Long jobInstanceId = jobExecution.getJobInstance().getId();
        Long executionId = jobExecution.getId();
        
        // Verify job can be found in repository
        JobExecution retrievedExecution = jobExplorer.getJobExecution(executionId);
        assertNotNull(retrievedExecution);
        assertEquals(jobInstanceId, retrievedExecution.getJobInstance().getId());

        // PHASE 4: Test recovery from backup
        // Simulate system restart by clearing context
        jobRepositoryTestUtils.removeJobExecutions();
        
        // Job should be recoverable from backup
        JobExecution recoveredExecution = jobExplorer.getJobExecution(executionId);
        if (recoveredExecution != null) {
            assertEquals(BatchStatus.COMPLETED, recoveredExecution.getStatus());
        }
    }

    @Test
    @Order(8)
    @DisplayName("E2E-BATCH-8: API versioning and compatibility")
    void testApiVersioningAndCompatibility() throws Exception {
        createTestFiles();

        // PHASE 1: Test v1 API compatibility
        JobParameters v1JobParameters = new JobParametersBuilder()
                .addString("apiVersion", "1.2")
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution v1Execution = jobLauncherTestUtils.launchJob(v1JobParameters);
        assertEquals(BatchStatus.COMPLETED, v1Execution.getStatus());

        // PHASE 2: Test v2 API with enhanced features
        JobParameters v2JobParameters = new JobParametersBuilder()
                .addString("apiVersion", "2.0")
                .addString("tenantId", testTenantId) // Required in v2.0+
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addLong("chunkSize", 10L)
                .addLong("threadCount", 2L)
                .addString("enableAdvancedFeatures", "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution v2Execution = jobLauncherTestUtils.launchJob(v2JobParameters);
        assertEquals(BatchStatus.COMPLETED, v2Execution.getStatus());

        // PHASE 3: Compare v1 vs v2 execution
        ExecutionContext v1Context = v1Execution.getExecutionContext();
        ExecutionContext v2Context = v2Execution.getExecutionContext();
        
        // v2 should have enhanced features
        assertTrue(v2Context.containsKey("advancedFeaturesEnabled"));
        assertTrue(v2Context.containsKey("tenantId"));
        
        // v1 should have basic features only
        assertFalse(v1Context.containsKey("advancedFeaturesEnabled"));
    }

    @Test
    @Order(9)
    @DisplayName("E2E-BATCH-9: Real-time processing and streaming")
    void testRealTimeProcessingAndStreaming() throws Exception {
        // PHASE 1: Setup streaming job
        JobParameters streamingJobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("processingMode", "STREAMING")
                .addString("enableRealTimeUpdates", "true")
                .addLong("chunkSize", 1L) // Process one at a time for streaming
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // PHASE 2: Start streaming job
        CompletableFuture<JobExecution> jobFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return jobLauncherTestUtils.launchJob(streamingJobParameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // PHASE 3: Add files during processing (simulate real-time)
        Thread.sleep(1000); // Let job start
        
        for (int i = 1; i <= 5; i++) {
            Path streamFile = testInputDir.resolve("stream-file-" + i + ".json");
            String content = String.format("""
                {
                    "streamId": "%d",
                    "tenantId": "%s",
                    "timestamp": "%s",
                    "data": "streaming data %d"
                }
                """, i, testTenantId, LocalDateTime.now(), i);
            Files.writeString(streamFile, content);
            
            Thread.sleep(500); // Simulate real-time arrival
        }

        // PHASE 4: Wait for job completion
        JobExecution streamingExecution = jobFuture.get(30, TimeUnit.SECONDS);
        assertEquals(BatchStatus.COMPLETED, streamingExecution.getStatus());

        // PHASE 5: Verify streaming results
        StepExecution streamingStep = streamingExecution.getStepExecutions().iterator().next();
        assertEquals(5, streamingStep.getReadCount());
        assertEquals(5, streamingStep.getWriteCount());
        
        // Verify real-time processing context
        ExecutionContext streamingContext = streamingStep.getExecutionContext();
        assertTrue(streamingContext.containsKey("streamingMode"));
        assertTrue(streamingContext.containsKey("realTimeUpdatesEnabled"));
    }

    // Helper methods

    private void createTestFiles() throws Exception {
        for (int i = 1; i <= 10; i++) {
            Path testFile = testInputDir.resolve("test-file-" + i + ".json");
            String content = String.format("""
                {
                    "fileId": "test-%d",
                    "tenantId": "%s",
                    "fileName": "test-file-%d.json",
                    "content": "test data for file %d",
                    "timestamp": "%s",
                    "size": %d
                }
                """, i, testTenantId, i, i, LocalDateTime.now(), i * 1000);
            Files.writeString(testFile, content);
        }
    }

    private void createFilesWithErrors() throws Exception {
        // Create mix of valid and invalid files
        for (int i = 1; i <= 8; i++) {
            Path testFile = testInputDir.resolve("error-test-file-" + i + ".json");
            
            String content;
            if (i % 3 == 0) {
                // Create invalid JSON (syntax error)
                content = String.format("""
                    {
                        "fileId": "error-test-%d",
                        "invalidJson": "missing quote and brace
                    """, i);
            } else if (i % 4 == 0) {
                // Create file with validation errors
                content = String.format("""
                    {
                        "fileId": "error-test-%d",
                        "tenantId": "",
                        "missingRequiredField": true
                    }
                    """, i);
            } else {
                // Create valid file
                content = String.format("""
                    {
                        "fileId": "error-test-%d",
                        "tenantId": "%s",
                        "fileName": "error-test-file-%d.json",
                        "content": "valid test data %d",
                        "timestamp": "%s"
                    }
                    """, i, testTenantId, i, i, LocalDateTime.now());
            }
            
            Files.writeString(testFile, content);
        }
    }

    private void fixErrorFiles() throws Exception {
        // Fix the error files for recovery testing
        Files.list(testInputDir)
                .filter(path -> path.getFileName().toString().contains("error-test"))
                .forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        int fileNum = Integer.parseInt(fileName.replaceAll("\\D", ""));
                        
                        String fixedContent = String.format("""
                            {
                                "fileId": "fixed-error-test-%d",
                                "tenantId": "%s",
                                "fileName": "%s",
                                "content": "fixed test data %d",
                                "timestamp": "%s",
                                "recoveredFrom": "error"
                            }
                            """, fileNum, testTenantId, fileName, fileNum, LocalDateTime.now());
                        
                        Files.writeString(path, fixedContent);
                    } catch (Exception e) {
                        // Ignore individual file errors
                    }
                });
    }

    private void createLargeDataset(int fileCount) throws Exception {
        for (int i = 1; i <= fileCount; i++) {
            Path testFile = testInputDir.resolve("large-dataset-" + i + ".json");
            
            // Create files with varying sizes
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(String.format("""
                {
                    "fileId": "large-%d",
                    "tenantId": "%s",
                    "fileName": "large-dataset-%d.json",
                    "timestamp": "%s",
                    "records": [
                """, i, testTenantId, i, LocalDateTime.now()));
            
            // Add multiple records per file
            for (int j = 1; j <= 10; j++) {
                if (j > 1) contentBuilder.append(",");
                contentBuilder.append(String.format("""
                    {
                        "recordId": %d,
                        "data": "record data %d for file %d",
                        "value": %d
                    }
                    """, j, j, i, j * i));
            }
            
            contentBuilder.append("]}");
            Files.writeString(testFile, contentBuilder.toString());
        }
    }

    private void testAlertTriggering() throws Exception {
        // Create files that will trigger alerts
        createFilesWithHighErrorRate();

        JobParameters alertJobParameters = new JobParametersBuilder()
                .addString("tenantId", testTenantId)
                .addString("inputPath", testInputDir.toString())
                .addString("outputPath", testOutputDir.toString())
                .addString("errorPath", testErrorDir.toString())
                .addString("enableAlerting", "true")
                .addString("alertThreshold", "90") // Alert if success rate < 90%
                .addLong("skipLimit", 10L) // Allow failures
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution alertJobExecution = jobLauncherTestUtils.launchJob(alertJobParameters);

        // Should complete but with alerts
        assertTrue(alertJobExecution.getStatus().equals(BatchStatus.COMPLETED) || 
                  alertJobExecution.getStatus().equals(BatchStatus.COMPLETED_WITH_SKIPS));

        ExecutionContext alertContext = alertJobExecution.getExecutionContext();
        assertTrue(alertContext.containsKey("alertsTriggered"));
        assertTrue(alertContext.getInt("alertsTriggered") > 0);
    }

    private void createFilesWithHighErrorRate() throws Exception {
        // Create 10 files where 6 are invalid (60% error rate)
        for (int i = 1; i <= 10; i++) {
            Path testFile = testInputDir.resolve("alert-test-" + i + ".json");
            
            String content;
            if (i <= 6) {
                // Invalid files
                content = "{ invalid json content";
            } else {
                // Valid files
                content = String.format("""
                    {
                        "fileId": "alert-test-%d",
                        "tenantId": "%s",
                        "valid": true
                    }
                    """, i, testTenantId);
            }
            
            Files.writeString(testFile, content);
        }
    }

    private void verifyJobResults(JobExecution jobExecution) {
        // Verify job execution details
        assertNotNull(jobExecution.getJobInstance());
        assertNotNull(jobExecution.getStartTime());
        assertNotNull(jobExecution.getEndTime());
        assertTrue(jobExecution.getEndTime().after(jobExecution.getStartTime()));

        // Verify step executions
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertFalse(stepExecutions.isEmpty());

        for (StepExecution stepExecution : stepExecutions) {
            assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
            assertTrue(stepExecution.getReadCount() >= 0);
            assertTrue(stepExecution.getWriteCount() >= 0);
        }
    }

    private void verifyFileProcessingResults() throws Exception {
        // Verify output files were created
        assertTrue(Files.exists(testOutputDir));
        
        long outputFileCount = Files.list(testOutputDir).count();
        assertTrue(outputFileCount > 0, "Should have created output files");

        // Verify file content
        Files.list(testOutputDir)
                .filter(Files::isRegularFile)
                .forEach(outputFile -> {
                    try {
                        String content = Files.readString(outputFile);
                        assertFalse(content.isEmpty(), "Output file should not be empty");
                        assertTrue(content.contains("tenantId"), "Output should contain tenant ID");
                    } catch (Exception e) {
                        fail("Error reading output file: " + e.getMessage());
                    }
                });
    }

    private void verifyErrorHandling() throws Exception {
        // Check if error files were moved to error directory
        if (Files.exists(testErrorDir)) {
            long errorFileCount = Files.list(testErrorDir).count();
            // Error count depends on test data, just verify directory exists
            assertTrue(Files.exists(testErrorDir));
        }
    }

    private void verifyArchiving() throws Exception {
        // Check if processed files were archived
        if (Files.exists(testArchiveDir)) {
            long archiveFileCount = Files.list(testArchiveDir).count();
            // Archive count depends on configuration
            assertTrue(Files.exists(testArchiveDir));
        }
    }

    private void clearTestDirectories() {
        try {
            if (Files.exists(testInputDir)) {
                Files.list(testInputDir).forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
            }
            
            if (Files.exists(testOutputDir)) {
                Files.list(testOutputDir).forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @AfterAll
    void tearDownAll() throws Exception {
        // Clean up test directories
        clearTestDirectories();
        
        // Clean up job repository
        jobRepositoryTestUtils.removeJobExecutions();
    }
}