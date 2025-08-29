package com.filetransfer.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Performance optimization configuration for high-throughput processing
 * Configures thread pools, connection pooling, and async processing
 */
@Configuration
@EnableAsync
public class PerformanceOptimizationConfig {

    @Value("${performance.thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${performance.thread-pool.max-size:50}")
    private int maxPoolSize;

    @Value("${performance.thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${performance.thread-pool.keep-alive:60}")
    private int keepAliveSeconds;

    @Value("${performance.http.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${performance.http.read-timeout:30000}")
    private int readTimeout;

    @Value("${performance.http.max-connections:200}")
    private int maxConnections;

    @Value("${performance.http.max-connections-per-route:50}")
    private int maxConnectionsPerRoute;

    /**
     * Primary async executor for general async operations
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * File processing executor for CPU-intensive tasks
     */
    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("FileProcessing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * Validation executor for parallel validation tasks
     */
    @Bean(name = "validationExecutor")
    public Executor validationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize / 2);
        executor.setMaxPoolSize(maxPoolSize / 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("Validation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Database operation executor for non-blocking DB operations
     */
    @Bean(name = "databaseExecutor")
    public Executor databaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(2000);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("Database-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(180);
        executor.initialize();
        return executor;
    }

    /**
     * High-performance HTTP client with connection pooling
     */
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        
        // Configure HTTP client with connection pooling
        org.apache.http.impl.client.CloseableHttpClient httpClient = 
            org.apache.http.impl.client.HttpClients.custom()
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setConnectionTimeToLive(30, java.util.concurrent.TimeUnit.SECONDS)
                .setKeepAliveStrategy((response, context) -> 20 * 1000) // 20 seconds
                .setRetryHandler(new org.apache.http.impl.client.DefaultHttpRequestRetryHandler(3, true))
                .build();
        
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add interceptors for logging and metrics
        restTemplate.getInterceptors().add(new HttpClientMetricsInterceptor());
        
        return restTemplate;
    }

    /**
     * Object pool configuration for reusable objects
     */
    @Bean
    public GenericObjectPoolConfig<?> objectPoolConfig() {
        GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(100);
        config.setMaxIdle(20);
        config.setMinIdle(5);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRunsMillis(30000);
        config.setMinEvictableIdleTimeMillis(60000);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(5000);
        return config;
    }

    /**
     * HTTP client metrics interceptor
     */
    public static class HttpClientMetricsInterceptor implements 
            org.springframework.http.client.ClientHttpRequestInterceptor {

        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {

            long startTime = System.currentTimeMillis();
            
            try {
                org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
                
                long duration = System.currentTimeMillis() - startTime;
                
                // Record metrics (integrate with your metrics service)
                recordHttpClientMetrics(request.getURI().toString(), 
                                      response.getStatusCode().value(), 
                                      duration);
                
                return response;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                recordHttpClientMetrics(request.getURI().toString(), -1, duration);
                throw e;
            }
        }

        private void recordHttpClientMetrics(String uri, int statusCode, long duration) {
            // Implement metrics recording
            System.out.printf("HTTP Client: %s -> %d (%dms)%n", uri, statusCode, duration);
        }
    }

    /**
     * Performance monitoring configuration
     */
    @Bean
    public PerformanceMonitor performanceMonitor() {
        return new PerformanceMonitor();
    }

    /**
     * Performance monitor for tracking system performance
     */
    public static class PerformanceMonitor {
        private final java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newScheduledThreadPool(1);

        public PerformanceMonitor() {
            // Start performance monitoring
            scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 30, java.util.concurrent.TimeUnit.SECONDS);
        }

        private void collectMetrics() {
            try {
                // Collect JVM metrics
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                long maxMemory = runtime.maxMemory();

                // Collect thread metrics
                ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
                ThreadGroup parent;
                while ((parent = rootGroup.getParent()) != null) {
                    rootGroup = parent;
                }
                int activeThreads = rootGroup.activeCount();

                // Log performance metrics
                System.out.printf("Performance Metrics - Memory: %d/%d MB (%.1f%%), Threads: %d%n",
                    usedMemory / (1024 * 1024),
                    maxMemory / (1024 * 1024),
                    (double) usedMemory / maxMemory * 100,
                    activeThreads);

            } catch (Exception e) {
                System.err.println("Error collecting performance metrics: " + e.getMessage());
            }
        }

        public void shutdown() {
            scheduler.shutdown();
        }
    }

    /**
     * Connection pool monitoring
     */
    @Bean
    public ConnectionPoolMonitor connectionPoolMonitor() {
        return new ConnectionPoolMonitor();
    }

    /**
     * Connection pool monitor
     */
    public static class ConnectionPoolMonitor {
        
        public ConnectionPoolStats getStats() {
            // Implement connection pool statistics collection
            return new ConnectionPoolStats(50, 25, 15, 10);
        }

        public static class ConnectionPoolStats {
            private final int maxConnections;
            private final int activeConnections;
            private final int idleConnections;
            private final int pendingConnections;

            public ConnectionPoolStats(int maxConnections, int activeConnections, 
                                     int idleConnections, int pendingConnections) {
                this.maxConnections = maxConnections;
                this.activeConnections = activeConnections;
                this.idleConnections = idleConnections;
                this.pendingConnections = pendingConnections;
            }

            // Getters
            public int getMaxConnections() { return maxConnections; }
            public int getActiveConnections() { return activeConnections; }
            public int getIdleConnections() { return idleConnections; }
            public int getPendingConnections() { return pendingConnections; }
            
            public double getUtilizationPercentage() {
                return (double) activeConnections / maxConnections * 100;
            }
        }
    }

    /**
     * Batch processing configuration
     */
    @Bean
    public BatchProcessingConfig batchProcessingConfig() {
        return new BatchProcessingConfig();
    }

    /**
     * Batch processing configuration
     */
    public static class BatchProcessingConfig {
        private final int batchSize = 100;
        private final int flushInterval = 5000; // 5 seconds

        public int getBatchSize() { return batchSize; }
        public int getFlushInterval() { return flushInterval; }
    }
}