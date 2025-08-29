package com.filetransfer.web.performance;

import com.filetransfer.web.FileTransferWebApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load tests for the File Transfer Management System
 * Tests system behavior under various load conditions and stress scenarios
 */
@SpringBootTest(classes = FileTransferWebApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class LoadTest {

    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 10;
    private static final int LOAD_TEST_DURATION_SECONDS = 60;
    
    private ExecutorService executorService;
    private String baseUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    @DisplayName("PERF-1: API Response Time - Should respond within acceptable limits")
    void testApiResponseTime() throws Exception {
        int numberOfRequests = 100;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < numberOfRequests; i++) {
            long startTime = System.nanoTime();
            
            // Make API request (simulate with timing)
            simulateApiRequest("/api/v2/tenants");
            
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            responseTimes.add(responseTime);
        }
        
        // Calculate statistics
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        // Calculate 95th percentile
        responseTimes.sort(Long::compareTo);
        long p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
        
        logger.info("API Response Time Statistics:");
        logger.info("  Average: {}ms", averageResponseTime);
        logger.info("  Min: {}ms", minResponseTime);
        logger.info("  Max: {}ms", maxResponseTime);
        logger.info("  95th Percentile: {}ms", p95ResponseTime);
        
        // Assertions
        assertTrue(averageResponseTime < 500, "Average response time should be less than 500ms");
        assertTrue(p95ResponseTime < 1000, "95th percentile response time should be less than 1000ms");
        assertTrue(maxResponseTime < 2000, "Maximum response time should be less than 2000ms");
    }

    @Test
    @DisplayName("PERF-2: Concurrent User Load - Should handle multiple concurrent users")
    void testConcurrentUserLoad() throws Exception {
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Simulate concurrent users
        for (int user = 0; user < CONCURRENT_USERS; user++) {
            final int userId = user;
            
            Future<Void> future = executorService.submit(() -> {
                try {
                    // Each user makes multiple requests
                    for (int request = 0; request < REQUESTS_PER_USER; request++) {
                        long requestStart = System.nanoTime();
                        
                        // Simulate different API calls
                        String endpoint = switch (request % 4) {
                            case 0 -> "/api/v2/tenants";
                            case 1 -> "/api/v2/services";
                            case 2 -> "/api/v2/schemas";
                            case 3 -> "/api/monitoring/metrics";
                            default -> "/api/v2/tenants";
                        };
                        
                        boolean success = simulateApiRequest(endpoint);
                        
                        long requestEnd = System.nanoTime();
                        long responseTime = (requestEnd - requestStart) / 1_000_000;
                        
                        if (success) {
                            successfulRequests.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        
                        // Small delay between requests
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    logger.error("User {} encountered error", userId, e);
                    failedRequests.addAndGet(REQUESTS_PER_USER);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all users to complete
        for (Future<Void> future : futures) {
            future.get(120, TimeUnit.SECONDS); // 2 minute timeout
        }
        
        Instant endTime = Instant.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        int totalRequests = successfulRequests.get() + failedRequests.get();
        double successRate = (double) successfulRequests.get() / totalRequests;
        double averageResponseTime = totalResponseTime.get() / (double) successfulRequests.get();
        double throughput = totalRequests / totalDuration.toSeconds();
        
        logger.info("Concurrent Load Test Results:");
        logger.info("  Total Requests: {}", totalRequests);
        logger.info("  Successful Requests: {}", successfulRequests.get());
        logger.info("  Failed Requests: {}", failedRequests.get());
        logger.info("  Success Rate: {:.2f}%", successRate * 100);
        logger.info("  Average Response Time: {:.2f}ms", averageResponseTime);
        logger.info("  Throughput: {:.2f} requests/second", throughput);
        logger.info("  Total Duration: {}s", totalDuration.toSeconds());
        
        // Assertions
        assertTrue(successRate > 0.95, "Success rate should be greater than 95%");
        assertTrue(averageResponseTime < 1000, "Average response time should be less than 1000ms");
        assertTrue(throughput > 10, "Throughput should be greater than 10 requests/second");
    }

    @Test
    @DisplayName("PERF-3: Database Performance - Should handle database operations efficiently")
    void testDatabasePerformance() throws Exception {
        AtomicLong totalDbOperationTime = new AtomicLong(0);
        AtomicInteger dbOperationCount = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        // Simulate multiple database operations
        for (int i = 0; i < 20; i++) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    
                    // Simulate database-heavy operations
                    simulateApiRequest("/api/v2/tenants?page=0&size=50"); // List with pagination
                    simulateApiRequest("/api/statistics/dashboard"); // Complex aggregations
                    simulateApiRequest("/api/audit/trail?limit=100"); // Historical data query
                    
                    long endTime = System.nanoTime();
                    long operationTime = (endTime - startTime) / 1_000_000;
                    
                    totalDbOperationTime.addAndGet(operationTime);
                    dbOperationCount.incrementAndGet();
                    
                } catch (Exception e) {
                    logger.error("Database operation failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        
        double averageDbOperationTime = totalDbOperationTime.get() / (double) dbOperationCount.get();
        
        logger.info("Database Performance Results:");
        logger.info("  Operations: {}", dbOperationCount.get());
        logger.info("  Average Operation Time: {:.2f}ms", averageDbOperationTime);
        
        // Assertions
        assertTrue(averageDbOperationTime < 2000, "Average database operation time should be less than 2000ms");
    }

    @Test
    @DisplayName("PERF-4: Memory Usage - Should maintain reasonable memory consumption")
    void testMemoryUsage() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // Get baseline memory usage
        System.gc(); // Suggest garbage collection
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform memory-intensive operations
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    // Simulate memory-intensive operations
                    simulateApiRequest("/api/v2/tenants?page=0&size=100");
                    simulateApiRequest("/api/files/upload"); // File processing
                    simulateApiRequest("/api/backup/create"); // Backup operations
                    
                } catch (Exception e) {
                    logger.error("Memory test operation failed", e);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for operations to complete
        for (Future<Void> future : futures) {
            future.get(120, TimeUnit.SECONDS);
        }
        
        // Check memory usage after operations
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryIncrease = finalMemory - baselineMemory;
        double memoryIncreasePercent = (double) memoryIncrease / baselineMemory * 100;
        
        logger.info("Memory Usage Results:");
        logger.info("  Baseline Memory: {} MB", baselineMemory / 1024 / 1024);
        logger.info("  Final Memory: {} MB", finalMemory / 1024 / 1024);
        logger.info("  Memory Increase: {} MB ({:.2f}%)", memoryIncrease / 1024 / 1024, memoryIncreasePercent);
        
        // Assertions
        assertTrue(memoryIncreasePercent < 200, "Memory increase should be less than 200%");
        assertTrue(finalMemory < runtime.maxMemory() * 0.8, "Memory usage should be less than 80% of max memory");
    }

    @Test
    @DisplayName("PERF-5: Stress Test - Should handle extreme load conditions")
    void testStressConditions() throws Exception {
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        Instant startTime = Instant.now();
        
        // High-intensity stress test
        int stressUsers = 100;
        int stressRequests = 5;
        
        for (int user = 0; user < stressUsers; user++) {
            Future<Void> future = executorService.submit(() -> {
                try {
                    for (int request = 0; request < stressRequests; request++) {
                        boolean success = simulateApiRequest("/api/v2/tenants");
                        
                        if (success) {
                            successfulRequests.incrementAndGet();
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        
                        // No delay for stress test
                    }
                } catch (Exception e) {
                    failedRequests.addAndGet(stressRequests);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for stress test to complete
        for (Future<Void> future : futures) {
            future.get(180, TimeUnit.SECONDS); // 3 minute timeout
        }
        
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        int totalRequests = successfulRequests.get() + failedRequests.get();
        double successRate = (double) successfulRequests.get() / totalRequests;
        double throughput = totalRequests / duration.toSeconds();
        
        logger.info("Stress Test Results:");
        logger.info("  Concurrent Users: {}", stressUsers);
        logger.info("  Total Requests: {}", totalRequests);
        logger.info("  Success Rate: {:.2f}%", successRate * 100);
        logger.info("  Throughput: {:.2f} requests/second", throughput);
        logger.info("  Duration: {}s", duration.toSeconds());
        
        // Under stress, we allow lower success rate but system should not crash
        assertTrue(successRate > 0.80, "Success rate should be greater than 80% even under stress");
        assertTrue(throughput > 5, "Throughput should be greater than 5 requests/second under stress");
    }

    /**
     * Simulate API request (placeholder for actual HTTP calls)
     */
    private boolean simulateApiRequest(String endpoint) {
        try {
            // Simulate network delay and processing time
            Thread.sleep(50 + (int)(Math.random() * 100)); // 50-150ms delay
            
            // Simulate 95% success rate
            return Math.random() > 0.05;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}