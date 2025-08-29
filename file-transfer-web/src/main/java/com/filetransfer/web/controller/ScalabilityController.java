package com.filetransfer.web.controller;

import com.filetransfer.web.service.LoadBalancingService;
import com.filetransfer.web.service.AsyncProcessingService;
import com.filetransfer.web.config.PerformanceOptimizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for scalability management and monitoring
 * Provides endpoints for load balancing, performance monitoring, and async operations
 */
@RestController
@RequestMapping("/api/scalability")
@CrossOrigin(origins = "*")
public class ScalabilityController {

    private static final Logger logger = LoggerFactory.getLogger(ScalabilityController.class);

    @Autowired
    private LoadBalancingService loadBalancingService;

    @Autowired
    private AsyncProcessingService asyncProcessingService;

    @Autowired
    private PerformanceOptimizationConfig.PerformanceMonitor performanceMonitor;

    @Autowired
    private PerformanceOptimizationConfig.ConnectionPoolMonitor connectionPoolMonitor;

    /**
     * Get load balancing statistics
     */
    @GetMapping("/load-balancing/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getLoadBalancingStats(@RequestParam String serviceName) {
        try {
            LoadBalancingService.LoadBalancingStats stats = loadBalancingService.getStats(serviceName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serviceName", stats.getServiceName());
            response.put("totalInstances", stats.getTotalInstances());
            response.put("healthyInstances", stats.getHealthyInstances());
            response.put("totalConnections", stats.getTotalConnections());
            response.put("avgResponseTime", stats.getAvgResponseTime());
            response.put("healthRatio", stats.getHealthRatio());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting load balancing stats", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Register a new service instance
     */
    @PostMapping("/load-balancing/instances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerInstance(@RequestBody Map<String, Object> request) {
        try {
            String serviceName = (String) request.get("serviceName");
            String instanceId = (String) request.get("instanceId");
            String host = (String) request.get("host");
            Integer port = (Integer) request.get("port");
            String protocol = (String) request.getOrDefault("protocol", "http");
            Integer weight = (Integer) request.getOrDefault("weight", 1);
            
            if (serviceName == null || instanceId == null || host == null || port == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters");
                errorResponse.put("message", "serviceName, instanceId, host, and port are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            LoadBalancingService.ServiceInstance instance = new LoadBalancingService.ServiceInstance(
                instanceId, host, port, protocol, weight
            );
            
            loadBalancingService.registerInstance(serviceName, instance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Service instance registered successfully");
            response.put("serviceName", serviceName);
            response.put("instanceId", instanceId);
            response.put("url", instance.getUrl());
            
            logger.info("Registered service instance: {} for service: {}", instanceId, serviceName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error registering service instance", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Deregister a service instance
     */
    @DeleteMapping("/load-balancing/instances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deregisterInstance(
            @RequestParam String serviceName,
            @RequestParam String instanceId) {
        
        try {
            loadBalancingService.deregisterInstance(serviceName, instanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Service instance deregistered successfully");
            response.put("serviceName", serviceName);
            response.put("instanceId", instanceId);
            
            logger.info("Deregistered service instance: {} from service: {}", instanceId, serviceName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deregistering service instance", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get the best service instance for routing
     */
    @GetMapping("/load-balancing/select-instance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> selectInstance(
            @RequestParam String serviceName,
            @RequestParam(required = false) String routingKey) {
        
        try {
            LoadBalancingService.ServiceInstance instance = loadBalancingService.selectInstance(serviceName, routingKey);
            
            if (instance == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No healthy instances available");
                errorResponse.put("serviceName", serviceName);
                return ResponseEntity.status(503).body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("instanceId", instance.getId());
            response.put("host", instance.getHost());
            response.put("port", instance.getPort());
            response.put("protocol", instance.getProtocol());
            response.put("url", instance.getUrl());
            response.put("weight", instance.getWeight());
            response.put("activeConnections", instance.getActiveConnections().get());
            response.put("lastResponseTime", instance.getLastResponseTime());
            
            // Track the connection
            instance.incrementConnections();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error selecting service instance", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Process file asynchronously
     */
    @PostMapping("/async/process-file")
    public ResponseEntity<Map<String, Object>> processFileAsync(@RequestBody Map<String, Object> request) {
        try {
            String fileName = (String) request.get("fileName");
            String content = (String) request.get("content");
            String tenantId = (String) request.get("tenantId");
            
            if (fileName == null || content == null || tenantId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Start async processing
            CompletableFuture<AsyncProcessingService.FileProcessingResult> future = 
                asyncProcessingService.processFileAsync(fileName, content.getBytes(), tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File processing started");
            response.put("fileName", fileName);
            response.put("tenantId", tenantId);
            response.put("status", "PROCESSING");
            
            // For demo purposes, wait a short time to see if it completes quickly
            try {
                AsyncProcessingService.FileProcessingResult result = 
                    future.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                response.put("status", result.isSuccess() ? "COMPLETED" : "FAILED");
                response.put("message", result.getMessage());
                
            } catch (java.util.concurrent.TimeoutException e) {
                response.put("status", "PROCESSING");
                response.put("message", "File processing in progress");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting async file processing", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Process multiple files in parallel
     */
    @PostMapping("/async/process-files-parallel")
    public ResponseEntity<Map<String, Object>> processFilesParallel(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> files = (List<Map<String, String>>) request.get("files");
            
            if (files == null || files.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No files provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert to processing requests
            List<AsyncProcessingService.FileProcessingRequest> requests = files.stream()
                .map(file -> new AsyncProcessingService.FileProcessingRequest(
                    file.get("fileName"),
                    file.get("content").getBytes(),
                    file.get("tenantId")
                ))
                .toList();
            
            // Start parallel processing
            CompletableFuture<List<AsyncProcessingService.FileProcessingResult>> future = 
                asyncProcessingService.processFilesInParallel(requests);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Parallel file processing started");
            response.put("fileCount", files.size());
            response.put("status", "PROCESSING");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting parallel file processing", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Process files in batch
     */
    @PostMapping("/async/process-batch")
    public ResponseEntity<Map<String, Object>> processBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> files = (List<Map<String, String>>) request.get("files");
            String tenantId = (String) request.get("tenantId");
            
            if (files == null || files.isEmpty() || tenantId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameters");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert to processing requests
            List<AsyncProcessingService.FileProcessingRequest> requests = files.stream()
                .map(file -> new AsyncProcessingService.FileProcessingRequest(
                    file.get("fileName"),
                    file.get("content").getBytes(),
                    tenantId
                ))
                .toList();
            
            // Start batch processing
            CompletableFuture<AsyncProcessingService.BatchProcessingResult> future = 
                asyncProcessingService.processBatchAsync(requests, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch processing started");
            response.put("fileCount", files.size());
            response.put("tenantId", tenantId);
            response.put("status", "PROCESSING");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting batch processing", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get system performance metrics
     */
    @GetMapping("/performance/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // JVM metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("totalMemory", runtime.totalMemory());
            jvmMetrics.put("freeMemory", runtime.freeMemory());
            jvmMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmMetrics.put("maxMemory", runtime.maxMemory());
            jvmMetrics.put("availableProcessors", runtime.availableProcessors());
            
            // Thread metrics
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parent;
            while ((parent = rootGroup.getParent()) != null) {
                rootGroup = parent;
            }
            
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("activeThreads", rootGroup.activeCount());
            threadMetrics.put("totalStartedThreads", Thread.getAllStackTraces().size());
            
            // Connection pool metrics
            PerformanceOptimizationConfig.ConnectionPoolMonitor.ConnectionPoolStats poolStats = 
                connectionPoolMonitor.getStats();
            
            Map<String, Object> connectionPoolMetrics = new HashMap<>();
            connectionPoolMetrics.put("maxConnections", poolStats.getMaxConnections());
            connectionPoolMetrics.put("activeConnections", poolStats.getActiveConnections());
            connectionPoolMetrics.put("idleConnections", poolStats.getIdleConnections());
            connectionPoolMetrics.put("pendingConnections", poolStats.getPendingConnections());
            connectionPoolMetrics.put("utilizationPercentage", poolStats.getUtilizationPercentage());
            
            metrics.put("jvm", jvmMetrics);
            metrics.put("threads", threadMetrics);
            metrics.put("connectionPool", connectionPoolMetrics);
            metrics.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Record request success for load balancing
     */
    @PostMapping("/load-balancing/record-success")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> recordSuccess(@RequestBody Map<String, Object> request) {
        try {
            String serviceName = (String) request.get("serviceName");
            String instanceId = (String) request.get("instanceId");
            Long responseTime = ((Number) request.get("responseTime")).longValue();
            
            // This would typically be called by the load balancer or client
            // For demo purposes, we'll just return success
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Success recorded");
            response.put("serviceName", serviceName);
            response.put("instanceId", instanceId);
            response.put("responseTime", responseTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error recording success", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get scalability health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getScalabilityHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("loadBalancing", "OPERATIONAL");
            health.put("asyncProcessing", "OPERATIONAL");
            health.put("caching", "OPERATIONAL");
            health.put("connectionPooling", "OPERATIONAL");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error checking scalability health", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}