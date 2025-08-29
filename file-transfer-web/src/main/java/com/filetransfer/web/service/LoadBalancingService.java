package com.filetransfer.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load balancing service for distributing workload across multiple instances
 * Implements various load balancing algorithms and health monitoring
 */
@Service
public class LoadBalancingService {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingService.class);

    @Value("${load-balancing.algorithm:round-robin}")
    private String loadBalancingAlgorithm;

    @Value("${load-balancing.health-check-interval:30000}")
    private long healthCheckInterval;

    @Value("${load-balancing.max-failures:3}")
    private int maxFailures;

    // Service instance registry
    private final Map<String, List<ServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, ServiceHealthMonitor> healthMonitors = new ConcurrentHashMap<>();

    /**
     * Load balancing algorithms
     */
    public enum LoadBalancingAlgorithm {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        LEAST_CONNECTIONS,
        LEAST_RESPONSE_TIME,
        HASH_BASED,
        RANDOM,
        WEIGHTED_RANDOM
    }

    /**
     * Service instance representation
     */
    public static class ServiceInstance {
        private final String id;
        private final String host;
        private final int port;
        private final String protocol;
        private final Map<String, String> metadata;
        private volatile boolean healthy;
        private volatile int weight;
        private volatile long lastResponseTime;
        private final AtomicLong activeConnections;
        private final AtomicInteger failureCount;

        public ServiceInstance(String id, String host, int port, String protocol, int weight) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.weight = weight;
            this.healthy = true;
            this.lastResponseTime = 0;
            this.activeConnections = new AtomicLong(0);
            this.failureCount = new AtomicInteger(0);
            this.metadata = new HashMap<>();
        }

        // Getters and utility methods
        public String getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getProtocol() { return protocol; }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public long getLastResponseTime() { return lastResponseTime; }
        public void setLastResponseTime(long lastResponseTime) { this.lastResponseTime = lastResponseTime; }
        public AtomicLong getActiveConnections() { return activeConnections; }
        public AtomicInteger getFailureCount() { return failureCount; }
        public Map<String, String> getMetadata() { return metadata; }

        public String getUrl() {
            return String.format("%s://%s:%d", protocol, host, port);
        }

        public void incrementConnections() {
            activeConnections.incrementAndGet();
        }

        public void decrementConnections() {
            activeConnections.decrementAndGet();
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        public void resetFailures() {
            failureCount.set(0);
        }

        public double getLoadScore() {
            // Calculate load score based on connections and response time
            double connectionScore = activeConnections.get() / (double) weight;
            double responseTimeScore = lastResponseTime / 1000.0; // Convert to seconds
            return connectionScore + responseTimeScore;
        }
    }

    /**
     * Register a service instance
     */
    public void registerInstance(String serviceName, ServiceInstance instance) {
        serviceRegistry.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(instance);
        roundRobinCounters.putIfAbsent(serviceName, new AtomicInteger(0));
        
        // Start health monitoring for the service
        healthMonitors.computeIfAbsent(serviceName, k -> new ServiceHealthMonitor(serviceName)).start();
        
        logger.info("Registered service instance: {} for service: {}", instance.getId(), serviceName);
    }

    /**
     * Deregister a service instance
     */
    public void deregisterInstance(String serviceName, String instanceId) {
        List<ServiceInstance> instances = serviceRegistry.get(serviceName);
        if (instances != null) {
            instances.removeIf(instance -> instance.getId().equals(instanceId));
            logger.info("Deregistered service instance: {} from service: {}", instanceId, serviceName);
        }
    }

    /**
     * Get the best service instance based on load balancing algorithm
     */
    public ServiceInstance selectInstance(String serviceName) {
        return selectInstance(serviceName, null);
    }

    /**
     * Get the best service instance with optional key for hash-based routing
     */
    public ServiceInstance selectInstance(String serviceName, String routingKey) {
        List<ServiceInstance> instances = getHealthyInstances(serviceName);
        
        if (instances.isEmpty()) {
            logger.warn("No healthy instances available for service: {}", serviceName);
            return null;
        }

        LoadBalancingAlgorithm algorithm = LoadBalancingAlgorithm.valueOf(
            loadBalancingAlgorithm.toUpperCase().replace("-", "_")
        );

        return switch (algorithm) {
            case ROUND_ROBIN -> selectRoundRobin(serviceName, instances);
            case WEIGHTED_ROUND_ROBIN -> selectWeightedRoundRobin(serviceName, instances);
            case LEAST_CONNECTIONS -> selectLeastConnections(instances);
            case LEAST_RESPONSE_TIME -> selectLeastResponseTime(instances);
            case HASH_BASED -> selectHashBased(instances, routingKey);
            case RANDOM -> selectRandom(instances);
            case WEIGHTED_RANDOM -> selectWeightedRandom(instances);
        };
    }

    /**
     * Round robin load balancing
     */
    private ServiceInstance selectRoundRobin(String serviceName, List<ServiceInstance> instances) {
        AtomicInteger counter = roundRobinCounters.get(serviceName);
        int index = counter.getAndIncrement() % instances.size();
        return instances.get(index);
    }

    /**
     * Weighted round robin load balancing
     */
    private ServiceInstance selectWeightedRoundRobin(String serviceName, List<ServiceInstance> instances) {
        // Create weighted list based on instance weights
        List<ServiceInstance> weightedInstances = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            for (int i = 0; i < instance.getWeight(); i++) {
                weightedInstances.add(instance);
            }
        }
        
        if (weightedInstances.isEmpty()) {
            return instances.get(0); // Fallback to first instance
        }
        
        AtomicInteger counter = roundRobinCounters.get(serviceName);
        int index = counter.getAndIncrement() % weightedInstances.size();
        return weightedInstances.get(index);
    }

    /**
     * Least connections load balancing
     */
    private ServiceInstance selectLeastConnections(List<ServiceInstance> instances) {
        return instances.stream()
            .min(Comparator.comparingLong(instance -> instance.getActiveConnections().get()))
            .orElse(instances.get(0));
    }

    /**
     * Least response time load balancing
     */
    private ServiceInstance selectLeastResponseTime(List<ServiceInstance> instances) {
        return instances.stream()
            .min(Comparator.comparingDouble(ServiceInstance::getLoadScore))
            .orElse(instances.get(0));
    }

    /**
     * Hash-based load balancing for session affinity
     */
    private ServiceInstance selectHashBased(List<ServiceInstance> instances, String routingKey) {
        if (routingKey == null) {
            routingKey = "default";
        }
        
        int hash = Math.abs(routingKey.hashCode());
        int index = hash % instances.size();
        return instances.get(index);
    }

    /**
     * Random load balancing
     */
    private ServiceInstance selectRandom(List<ServiceInstance> instances) {
        Random random = new Random();
        return instances.get(random.nextInt(instances.size()));
    }

    /**
     * Weighted random load balancing
     */
    private ServiceInstance selectWeightedRandom(List<ServiceInstance> instances) {
        int totalWeight = instances.stream().mapToInt(ServiceInstance::getWeight).sum();
        if (totalWeight == 0) {
            return selectRandom(instances);
        }
        
        Random random = new Random();
        int randomWeight = random.nextInt(totalWeight);
        
        int currentWeight = 0;
        for (ServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (randomWeight < currentWeight) {
                return instance;
            }
        }
        
        return instances.get(instances.size() - 1); // Fallback
    }

    /**
     * Get healthy instances for a service
     */
    private List<ServiceInstance> getHealthyInstances(String serviceName) {
        List<ServiceInstance> allInstances = serviceRegistry.get(serviceName);
        if (allInstances == null) {
            return Collections.emptyList();
        }
        
        return allInstances.stream()
            .filter(ServiceInstance::isHealthy)
            .filter(instance -> instance.getFailureCount().get() < maxFailures)
            .toList();
    }

    /**
     * Record successful request
     */
    public void recordSuccess(ServiceInstance instance, long responseTime) {
        instance.setLastResponseTime(responseTime);
        instance.resetFailures();
        instance.decrementConnections();
        
        logger.debug("Recorded successful request for instance: {} ({}ms)", 
                    instance.getId(), responseTime);
    }

    /**
     * Record failed request
     */
    public void recordFailure(ServiceInstance instance) {
        instance.recordFailure();
        instance.decrementConnections();
        
        if (instance.getFailureCount().get() >= maxFailures) {
            instance.setHealthy(false);
            logger.warn("Instance {} marked as unhealthy after {} failures", 
                       instance.getId(), instance.getFailureCount().get());
        }
    }

    /**
     * Get load balancing statistics
     */
    public LoadBalancingStats getStats(String serviceName) {
        List<ServiceInstance> instances = serviceRegistry.get(serviceName);
        if (instances == null) {
            return new LoadBalancingStats(serviceName, 0, 0, 0, 0);
        }
        
        long totalInstances = instances.size();
        long healthyInstances = instances.stream()
            .filter(ServiceInstance::isHealthy)
            .count();
        long totalConnections = instances.stream()
            .mapToLong(instance -> instance.getActiveConnections().get())
            .sum();
        double avgResponseTime = instances.stream()
            .filter(instance -> instance.getLastResponseTime() > 0)
            .mapToLong(ServiceInstance::getLastResponseTime)
            .average()
            .orElse(0.0);
        
        return new LoadBalancingStats(serviceName, totalInstances, healthyInstances, 
                                     totalConnections, avgResponseTime);
    }

    /**
     * Service health monitor
     */
    private class ServiceHealthMonitor {
        private final String serviceName;
        private volatile boolean running = false;

        public ServiceHealthMonitor(String serviceName) {
            this.serviceName = serviceName;
        }

        public void start() {
            if (!running) {
                running = true;
                // Start health check thread
                Thread healthCheckThread = new Thread(this::healthCheckLoop);
                healthCheckThread.setDaemon(true);
                healthCheckThread.setName("HealthCheck-" + serviceName);
                healthCheckThread.start();
            }
        }

        private void healthCheckLoop() {
            while (running) {
                try {
                    performHealthCheck();
                    Thread.sleep(healthCheckInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Health check failed for service: {}", serviceName, e);
                }
            }
        }

        private void performHealthCheck() {
            List<ServiceInstance> instances = serviceRegistry.get(serviceName);
            if (instances == null) return;

            for (ServiceInstance instance : instances) {
                try {
                    // Perform actual health check (HTTP request, TCP connection, etc.)
                    boolean isHealthy = performInstanceHealthCheck(instance);
                    
                    if (isHealthy && !instance.isHealthy()) {
                        instance.setHealthy(true);
                        instance.resetFailures();
                        logger.info("Instance {} is now healthy", instance.getId());
                    } else if (!isHealthy && instance.isHealthy()) {
                        instance.recordFailure();
                        if (instance.getFailureCount().get() >= maxFailures) {
                            instance.setHealthy(false);
                            logger.warn("Instance {} marked as unhealthy", instance.getId());
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Health check failed for instance: {}", instance.getId(), e);
                    instance.recordFailure();
                }
            }
        }

        private boolean performInstanceHealthCheck(ServiceInstance instance) {
            // Simple TCP connection check
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(instance.getHost(), instance.getPort()), 5000);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Load balancing statistics
     */
    public static class LoadBalancingStats {
        private final String serviceName;
        private final long totalInstances;
        private final long healthyInstances;
        private final long totalConnections;
        private final double avgResponseTime;

        public LoadBalancingStats(String serviceName, long totalInstances, long healthyInstances, 
                                 long totalConnections, double avgResponseTime) {
            this.serviceName = serviceName;
            this.totalInstances = totalInstances;
            this.healthyInstances = healthyInstances;
            this.totalConnections = totalConnections;
            this.avgResponseTime = avgResponseTime;
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public long getTotalInstances() { return totalInstances; }
        public long getHealthyInstances() { return healthyInstances; }
        public long getTotalConnections() { return totalConnections; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public double getHealthRatio() { 
            return totalInstances > 0 ? (double) healthyInstances / totalInstances : 0.0; 
        }
    }
}