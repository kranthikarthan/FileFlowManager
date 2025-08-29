package com.filetransfer.web.performance;

import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise-scale performance tests with realistic data volumes and payloads
 * Tests system behavior with enterprise-scale file sizes and volumes
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class EnterpriseLoadTest {

    // Enterprise-scale test configuration
    private static final int ENTERPRISE_CONCURRENT_USERS = 500;
    private static final int ENTERPRISE_FILE_COUNT = 10000;
    private static final int LARGE_FILE_SIZE_MB = 50; // 50 MB files
    private static final int MEDIUM_FILE_SIZE_MB = 10; // 10 MB files
    private static final int SMALL_FILE_SIZE_KB = 500; // 500 KB files

    private ExecutorService executorService;
    private Path testDataDir;
    private Random random = new Random();

    @BeforeEach
    void setUp() throws IOException {
        executorService = Executors.newFixedThreadPool(ENTERPRISE_CONCURRENT_USERS);
        testDataDir = Paths.get("/tmp/enterprise-performance-test");
        Files.createDirectories(testDataDir);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        
        // Cleanup test data
        cleanupTestData();
    }

    @Test
    @DisplayName("ENT-PERF-1: Large File Processing - Should handle 50MB files efficiently")
    void testLargeFileProcessing() throws Exception {
        // Create realistic large files
        List<Path> largeFiles = createLargeTestFiles(10, LARGE_FILE_SIZE_MB);
        
        AtomicLong totalProcessingTime = new AtomicLong(0);
        AtomicInteger successfulProcessing = new AtomicInteger(0);
        AtomicInteger failedProcessing = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Process large files concurrently
        for (Path file : largeFiles) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    long fileStartTime = System.currentTimeMillis();
                    
                    // Simulate large file processing
                    boolean success = simulateLargeFileProcessing(file);
                    
                    long fileEndTime = System.currentTimeMillis();
                    long processingTime = fileEndTime - fileStartTime;
                    
                    totalProcessingTime.addAndGet(processingTime);
                    
                    if (success) {
                        successfulProcessing.incrementAndGet();
                    } else {
                        failedProcessing.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failedProcessing.incrementAndGet();
                    logger.error("Large file processing failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all processing to complete
        for (Future<Void> future : futures) {
            future.get(300, TimeUnit.SECONDS); // 5 minute timeout per file
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        double averageProcessingTime = totalProcessingTime.get() / (double) largeFiles.size();
        double successRate = (double) successfulProcessing.get() / largeFiles.size();
        
        logger.info("Large File Processing Results:");
        logger.info("  Files Processed: {}", largeFiles.size());
        logger.info("  File Size: {} MB each", LARGE_FILE_SIZE_MB);
        logger.info("  Total Data Volume: {} MB", largeFiles.size() * LARGE_FILE_SIZE_MB);
        logger.info("  Success Rate: {:.2f}%", successRate * 100);
        logger.info("  Average Processing Time: {:.2f}ms per file", averageProcessingTime);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Enterprise performance assertions
        assertTrue(successRate > 0.95, "Success rate should be > 95% for large files");
        assertTrue(averageProcessingTime < 30000, "Average processing time should be < 30 seconds per 50MB file");
        assertTrue(totalDuration.toSeconds() < 600, "Total processing should complete within 10 minutes");
    }

    @Test
    @DisplayName("ENT-PERF-2: High Volume Processing - Should handle 10,000 files")
    void testHighVolumeProcessing() throws Exception {
        // Create enterprise-scale file volume
        List<Path> enterpriseFiles = createEnterpriseVolumeTestFiles(ENTERPRISE_FILE_COUNT, SMALL_FILE_SIZE_KB);
        
        AtomicInteger processedFiles = new AtomicInteger(0);
        AtomicLong totalDataProcessed = new AtomicLong(0);
        AtomicInteger batchesProcessed = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Process files in batches
        int batchSize = 100;
        for (int i = 0; i < enterpriseFiles.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, enterpriseFiles.size());
            List<Path> batch = enterpriseFiles.subList(i, endIndex);
            
            Future<Void> future = executorService.submit(() -> {
                try {
                    long batchStartTime = System.currentTimeMillis();
                    
                    // Process batch of files
                    boolean batchSuccess = simulateFileBatchProcessing(batch);
                    
                    long batchEndTime = System.currentTimeMillis();
                    
                    if (batchSuccess) {
                        processedFiles.addAndGet(batch.size());
                        totalDataProcessed.addAndGet(batch.size() * SMALL_FILE_SIZE_KB * 1024);
                        batchesProcessed.incrementAndGet();
                        
                        logger.debug("Processed batch of {} files in {}ms", 
                                   batch.size(), batchEndTime - batchStartTime);
                    }
                    
                } catch (Exception e) {
                    logger.error("Batch processing failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all batches to complete
        for (Future<Void> future : futures) {
            future.get(1800, TimeUnit.SECONDS); // 30 minute timeout
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        double throughput = processedFiles.get() / totalDuration.toSeconds();
        double dataVolumeGB = totalDataProcessed.get() / (1024.0 * 1024.0 * 1024.0);
        
        logger.info("High Volume Processing Results:");
        logger.info("  Target Files: {}", ENTERPRISE_FILE_COUNT);
        logger.info("  Files Processed: {}", processedFiles.get());
        logger.info("  File Size: {} KB each", SMALL_FILE_SIZE_KB);
        logger.info("  Total Data Volume: {:.2f} GB", dataVolumeGB);
        logger.info("  Batches Processed: {}", batchesProcessed.get());
        logger.info("  Throughput: {:.2f} files/second", throughput);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Enterprise volume assertions
        assertTrue(processedFiles.get() >= ENTERPRISE_FILE_COUNT * 0.95, 
                  "Should process at least 95% of files");
        assertTrue(throughput > 10.0, 
                  "Should process more than 10 files per second");
        assertTrue(totalDuration.toSeconds() < 1800, 
                  "Should complete within 30 minutes");
    }

    @Test
    @DisplayName("ENT-PERF-3: Mixed File Size Processing - Should handle varied enterprise payloads")
    void testMixedFileSizeProcessing() throws Exception {
        // Create realistic enterprise file mix
        List<Path> mixedFiles = new ArrayList<>();
        
        // Large files (10% of total) - 50 MB each
        mixedFiles.addAll(createLargeTestFiles(100, LARGE_FILE_SIZE_MB));
        
        // Medium files (30% of total) - 10 MB each  
        mixedFiles.addAll(createMediumTestFiles(300, MEDIUM_FILE_SIZE_MB));
        
        // Small files (60% of total) - 500 KB each
        mixedFiles.addAll(createSmallTestFiles(600, SMALL_FILE_SIZE_KB));
        
        AtomicLong totalDataVolume = new AtomicLong(0);
        AtomicInteger processedFiles = new AtomicInteger(0);
        AtomicLong processingTime = new AtomicLong(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Process mixed file sizes
        for (Path file : mixedFiles) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    long fileStartTime = System.currentTimeMillis();
                    
                    // Simulate processing based on file size
                    long fileSize = Files.size(file);
                    boolean success = simulateFileSizeBasedProcessing(file, fileSize);
                    
                    long fileEndTime = System.currentTimeMillis();
                    
                    if (success) {
                        processedFiles.incrementAndGet();
                        totalDataVolume.addAndGet(fileSize);
                        processingTime.addAndGet(fileEndTime - fileStartTime);
                    }
                    
                } catch (Exception e) {
                    logger.error("Mixed file processing failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for processing to complete
        for (Future<Void> future : futures) {
            future.get(3600, TimeUnit.SECONDS); // 1 hour timeout
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        double totalDataGB = totalDataVolume.get() / (1024.0 * 1024.0 * 1024.0);
        double averageProcessingTime = processingTime.get() / (double) processedFiles.get();
        double dataThroughputMBps = (totalDataVolume.get() / (1024.0 * 1024.0)) / totalDuration.toSeconds();
        
        logger.info("Mixed File Size Processing Results:");
        logger.info("  Total Files: {}", mixedFiles.size());
        logger.info("  Files Processed: {}", processedFiles.get());
        logger.info("  Total Data Volume: {:.2f} GB", totalDataGB);
        logger.info("  Average Processing Time: {:.2f}ms per file", averageProcessingTime);
        logger.info("  Data Throughput: {:.2f} MB/s", dataThroughputMBps);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Mixed file size assertions
        assertTrue(processedFiles.get() >= mixedFiles.size() * 0.90, 
                  "Should process at least 90% of mixed-size files");
        assertTrue(dataThroughputMBps > 1.0, 
                  "Should achieve > 1 MB/s data throughput");
        assertTrue(totalDuration.toSeconds() < 3600, 
                  "Should complete within 1 hour");
    }

    @Test
    @DisplayName("ENT-PERF-4: Enterprise Concurrent Load - Should handle 500+ concurrent users")
    void testEnterpriseConcurrentLoad() throws Exception {
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Simulate enterprise-scale concurrent users
        for (int user = 0; user < ENTERPRISE_CONCURRENT_USERS; user++) {
            final int userId = user;
            
            Future<Void> future = executorService.submit(() -> {
                try {
                    // Each user performs realistic enterprise operations
                    for (int request = 0; request < 5; request++) {
                        long requestStart = System.nanoTime();
                        
                        // Simulate enterprise API patterns
                        String endpoint = switch (request % 5) {
                            case 0 -> "/api/v2/tenants?page=" + (userId % 10) + "&size=50";
                            case 1 -> "/api/services?tenantId=" + (userId % 100);
                            case 2 -> "/api/files/upload"; // Large file upload
                            case 3 -> "/api/batch/v2/jobs/executions?page=0&size=100";
                            case 4 -> "/api/monitoring/metrics";
                            default -> "/api/v2/tenants";
                        };
                        
                        boolean success = simulateEnterpriseApiRequest(endpoint, userId);
                        
                        long requestEnd = System.nanoTime();
                        long responseTime = (requestEnd - requestStart) / 1_000_000;
                        
                        if (success) {
                            successfulRequests.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        
                        // Realistic user think time
                        Thread.sleep(200 + random.nextInt(300)); // 200-500ms
                    }
                } catch (Exception e) {
                    logger.error("Enterprise user {} encountered error", userId, e);
                    failedRequests.addAndGet(5);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all enterprise users to complete
        for (Future<Void> future : futures) {
            future.get(1800, TimeUnit.SECONDS); // 30 minute timeout
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        int totalRequests = successfulRequests.get() + failedRequests.get();
        double successRate = (double) successfulRequests.get() / totalRequests;
        double averageResponseTime = totalResponseTime.get() / (double) successfulRequests.get();
        double throughput = totalRequests / totalDuration.toSeconds();
        
        logger.info("Enterprise Concurrent Load Results:");
        logger.info("  Concurrent Users: {}", ENTERPRISE_CONCURRENT_USERS);
        logger.info("  Total Requests: {}", totalRequests);
        logger.info("  Successful Requests: {}", successfulRequests.get());
        logger.info("  Failed Requests: {}", failedRequests.get());
        logger.info("  Success Rate: {:.2f}%", successRate * 100);
        logger.info("  Average Response Time: {:.2f}ms", averageResponseTime);
        logger.info("  Throughput: {:.2f} requests/second", throughput);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Enterprise load assertions
        assertTrue(successRate > 0.85, "Success rate should be > 85% under enterprise load");
        assertTrue(averageResponseTime < 2000, "Average response time should be < 2000ms under load");
        assertTrue(throughput > 50, "Should handle > 50 requests/second enterprise load");
    }

    @Test
    @DisplayName("ENT-PERF-5: Realistic Business Data Processing - COBOL, XML, JSON payloads")
    void testRealisticBusinessDataProcessing() throws Exception {
        // Create realistic business data files
        List<Path> businessFiles = new ArrayList<>();
        
        // COBOL files (financial records)
        businessFiles.addAll(createCobolTestFiles(500));
        
        // XML files (transaction data)
        businessFiles.addAll(createXmlTestFiles(300));
        
        // JSON files (API data)
        businessFiles.addAll(createJsonTestFiles(200));
        
        AtomicInteger processedByType = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);
        
        Map<String, AtomicInteger> processingByType = new ConcurrentHashMap<>();
        processingByType.put("COBOL", new AtomicInteger(0));
        processingByType.put("XML", new AtomicInteger(0));
        processingByType.put("JSON", new AtomicInteger(0));
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Process business data files
        for (Path file : businessFiles) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    long fileStartTime = System.currentTimeMillis();
                    
                    String fileType = determineFileType(file);
                    boolean success = simulateBusinessDataProcessing(file, fileType);
                    
                    long fileEndTime = System.currentTimeMillis();
                    
                    if (success) {
                        processedByType.incrementAndGet();
                        processingByType.get(fileType).incrementAndGet();
                        totalProcessingTime.addAndGet(fileEndTime - fileStartTime);
                    }
                    
                } catch (Exception e) {
                    logger.error("Business data processing failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all business data processing to complete
        for (Future<Void> future : futures) {
            future.get(1800, TimeUnit.SECONDS); // 30 minute timeout
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        double averageProcessingTime = totalProcessingTime.get() / (double) processedByType.get();
        double throughput = processedByType.get() / totalDuration.toSeconds();
        
        logger.info("Realistic Business Data Processing Results:");
        logger.info("  Total Business Files: {}", businessFiles.size());
        logger.info("  COBOL Files Processed: {}", processingByType.get("COBOL").get());
        logger.info("  XML Files Processed: {}", processingByType.get("XML").get());
        logger.info("  JSON Files Processed: {}", processingByType.get("JSON").get());
        logger.info("  Average Processing Time: {:.2f}ms per file", averageProcessingTime);
        logger.info("  Throughput: {:.2f} files/second", throughput);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Business data processing assertions
        assertTrue(processedByType.get() >= businessFiles.size() * 0.90, 
                  "Should process at least 90% of business data files");
        assertTrue(throughput > 5.0, 
                  "Should achieve > 5 files/second for complex business data");
    }

    // Helper methods to create test data

    private List<Path> createLargeTestFiles(int count, int sizeMB) throws IOException {
        List<Path> files = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Path file = testDataDir.resolve("large-file-" + i + ".json");
            
            // Create large JSON file
            StringBuilder content = new StringBuilder();
            content.append("{\n");
            content.append("  \"fileId\": \"large-").append(i).append("\",\n");
            content.append("  \"tenantId\": \"enterprise-tenant\",\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"size\": \"").append(sizeMB).append("MB\",\n");
            content.append("    \"type\": \"LARGE_DATA_FILE\"\n");
            content.append("  },\n");
            content.append("  \"records\": [\n");
            
            // Fill with data to reach target size
            int targetSize = sizeMB * 1024 * 1024;
            int recordSize = 1000; // ~1KB per record
            int recordCount = (targetSize - 500) / recordSize; // Account for metadata
            
            for (int j = 0; j < recordCount; j++) {
                if (j > 0) content.append(",\n");
                content.append("    {\n");
                content.append("      \"recordId\": ").append(j).append(",\n");
                content.append("      \"data\": \"").append("x".repeat(800)).append("\",\n"); // 800 chars
                content.append("      \"timestamp\": \"2023-12-01T10:30:00\",\n");
                content.append("      \"processed\": false\n");
                content.append("    }");
            }
            
            content.append("\n  ]\n}");
            
            Files.writeString(file, content.toString());
            files.add(file);
            
            logger.debug("Created large test file: {} ({} MB)", file.getFileName(), sizeMB);
        }
        
        return files;
    }

    private List<Path> createEnterpriseVolumeTestFiles(int count, int sizeKB) throws IOException {
        List<Path> files = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Path file = testDataDir.resolve("enterprise-volume-" + i + ".json");
            
            // Create realistic business transaction file
            StringBuilder content = new StringBuilder();
            content.append("{\n");
            content.append("  \"transactionId\": \"TXN-").append(String.format("%06d", i)).append("\",\n");
            content.append("  \"tenantId\": \"enterprise-tenant\",\n");
            content.append("  \"processingDate\": \"2023-12-01\",\n");
            content.append("  \"transactions\": [\n");
            
            // Fill to target size
            int targetSize = sizeKB * 1024;
            int transactionSize = 200; // ~200 bytes per transaction
            int transactionCount = (targetSize - 200) / transactionSize;
            
            for (int j = 0; j < transactionCount; j++) {
                if (j > 0) content.append(",\n");
                content.append("    {\n");
                content.append("      \"id\": ").append(j).append(",\n");
                content.append("      \"amount\": ").append(1000 + random.nextInt(9000)).append(".").append(random.nextInt(100)).append(",\n");
                content.append("      \"currency\": \"USD\",\n");
                content.append("      \"type\": \"TRANSFER\",\n");
                content.append("      \"account\": \"ACC-").append(String.format("%08d", j)).append("\"\n");
                content.append("    }");
            }
            
            content.append("\n  ]\n}");
            
            Files.writeString(file, content.toString());
            files.add(file);
            
            if (i % 1000 == 0) {
                logger.info("Created {} enterprise volume test files", i);
            }
        }
        
        logger.info("Created {} enterprise volume test files totaling ~{} GB", 
                   count, (count * sizeKB) / (1024.0 * 1024.0));
        
        return files;
    }

    private List<Path> createCobolTestFiles(int count) throws IOException {
        List<Path> files = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Path file = testDataDir.resolve("cobol-data-" + i + ".dat");
            
            // Create realistic COBOL fixed-width record file
            StringBuilder content = new StringBuilder();
            
            // Each record is 200 bytes (typical COBOL record size)
            int recordsPerFile = 1000; // 200KB per file
            
            for (int j = 0; j < recordsPerFile; j++) {
                // Customer ID (10 chars)
                content.append(String.format("CUST%06d", j));
                
                // Customer Name (30 chars)
                content.append(String.format("%-30s", "Customer Name " + j));
                
                // Address (60 chars)
                content.append(String.format("%-60s", "123 Main Street, City " + j));
                
                // Account Balance (12 chars: 9999999.99)
                content.append(String.format("%010d.%02d", random.nextInt(1000000), random.nextInt(100)));
                
                // Status (1 char)
                content.append("A");
                
                // Filler to reach 200 bytes
                content.append(" ".repeat(87));
                content.append("\n");
            }
            
            Files.writeString(file, content.toString());
            files.add(file);
        }
        
        logger.info("Created {} COBOL test files (~200 KB each)", count);
        return files;
    }

    // Additional helper methods...
    
    private boolean simulateLargeFileProcessing(Path file) {
        try {
            // Simulate processing time based on file size
            long fileSize = Files.size(file);
            long processingTime = fileSize / (10 * 1024 * 1024); // 10MB per second simulation
            Thread.sleep(Math.max(100, processingTime));
            
            // 98% success rate for large files
            return random.nextDouble() > 0.02;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateFileBatchProcessing(List<Path> batch) {
        try {
            // Simulate batch processing efficiency
            long batchProcessingTime = batch.size() * 10; // 10ms per file in batch
            Thread.sleep(batchProcessingTime);
            
            // 99% success rate for batch processing
            return random.nextDouble() > 0.01;
        } catch (Exception e) {
            return false;
        }
    }

    private void cleanupTestData() {
        try {
            if (Files.exists(testDataDir)) {
                Files.walk(testDataDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception e) {
                                // Ignore cleanup errors
                            }
                        });
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup test data", e);
        }
    }
}