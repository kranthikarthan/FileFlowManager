package com.filetransfer.web.service;

import com.filetransfer.web.entity.SubServiceConfiguration;
import com.filetransfer.web.repository.SubServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Performance-optimized service for SubService operations.
 * Uses caching, bulk operations, and optimized queries.
 */
@Service
public class PerformanceOptimizedSubServiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizedSubServiceService.class);
    
    @Autowired
    private SubServiceConfigurationRepository subServiceConfigRepository;
    
    // In-memory cache for frequently accessed configurations
    private final Map<String, SubServiceConfiguration> hotConfigCache = new ConcurrentHashMap<>();
    
    /**
     * Get SubService configuration with optimized lookup strategy
     */
    @Cacheable(value = "subServiceConfigs", key = "#tenantId + '_' + #serviceName + '_' + #subServiceName")
    public Optional<SubServiceConfiguration> getOptimizedSubServiceConfig(String tenantId, String serviceName, String subServiceName) {
        String cacheKey = tenantId + "_" + serviceName + "_" + subServiceName;
        
        // First check hot cache for most frequently accessed configs
        SubServiceConfiguration hotConfig = hotConfigCache.get(cacheKey);
        if (hotConfig != null) {
            logger.trace("Retrieved config from hot cache: {}", cacheKey);
            return Optional.of(hotConfig);
        }
        
        // Use optimized repository query
        Optional<SubServiceConfiguration> config = subServiceConfigRepository
            .findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName);
        
        // Add to hot cache if found and enabled
        config.ifPresent(c -> {
            if (c.getEnabled()) {
                hotConfigCache.put(cacheKey, c);
                logger.trace("Added config to hot cache: {}", cacheKey);
            }
        });
        
        return config;
    }
    
    /**
     * Bulk load all enabled SubService configurations for a tenant
     */
    @Cacheable(value = "subServiceConfigs", key = "'bulk_enabled_' + #tenantId")
    public Map<String, SubServiceConfiguration> getBulkEnabledConfigurations(String tenantId) {
        logger.debug("Bulk loading enabled configurations for tenant: {}", tenantId);
        
        List<SubServiceConfiguration> configs = subServiceConfigRepository.findByTenantIdAndEnabled(tenantId, true);
        
        return configs.stream()
            .collect(Collectors.toMap(
                config -> config.getServiceName() + "_" + config.getSubServiceName(),
                config -> config,
                (existing, replacement) -> replacement // Handle duplicates
            ));
    }
    
    /**
     * Get configurations for batch processing (optimized for file monitoring)
     */
    @Cacheable(value = "subServiceConfigs", key = "'batch_processing_' + #tenantId")
    public List<SubServiceConfiguration> getConfigurationsForBatchProcessing(String tenantId) {
        logger.debug("Loading configurations for batch processing: {}", tenantId);
        
        // Use a single optimized query that fetches all needed fields
        return subServiceConfigRepository.findEnabledConfigsForBatchProcessing(tenantId);
    }
    
    /**
     * Preload frequently accessed configurations into hot cache
     */
    public void preloadHotConfigurations(String tenantId) {
        logger.info("Preloading hot configurations for tenant: {}", tenantId);
        
        try {
            List<SubServiceConfiguration> frequentConfigs = subServiceConfigRepository
                .findMostFrequentlyUsedConfigs(tenantId, 50); // Top 50 most used
            
            frequentConfigs.forEach(config -> {
                String cacheKey = config.getTenantId() + "_" + 
                                config.getServiceName() + "_" + 
                                config.getSubServiceName();
                hotConfigCache.put(cacheKey, config);
            });
            
            logger.info("Preloaded {} hot configurations for tenant: {}", frequentConfigs.size(), tenantId);
            
        } catch (Exception e) {
            logger.warn("Failed to preload hot configurations for tenant {}: {}", tenantId, e.getMessage());
        }
    }
    
    /**
     * Clear hot cache for a specific configuration
     */
    public void evictHotCache(String tenantId, String serviceName, String subServiceName) {
        String cacheKey = tenantId + "_" + serviceName + "_" + subServiceName;
        hotConfigCache.remove(cacheKey);
        logger.debug("Evicted hot cache for: {}", cacheKey);
    }
    
    /**
     * Clear all hot cache entries for a tenant
     */
    public void evictTenantHotCache(String tenantId) {
        hotConfigCache.entrySet().removeIf(entry -> entry.getKey().startsWith(tenantId + "_"));
        logger.info("Evicted all hot cache entries for tenant: {}", tenantId);
    }
    
    /**
     * Get hot cache statistics
     */
    public HotCacheStats getHotCacheStats() {
        return new HotCacheStats(
            hotConfigCache.size(),
            hotConfigCache.keySet().stream()
                .collect(Collectors.groupingBy(
                    key -> key.split("_")[0], // Group by tenant ID
                    Collectors.counting()
                ))
        );
    }
    
    /**
     * Optimize hot cache by removing least recently used entries
     */
    public void optimizeHotCache() {
        int maxSize = 1000; // Maximum hot cache size
        
        if (hotConfigCache.size() > maxSize) {
            logger.info("Hot cache size ({}) exceeds maximum ({}), optimizing...", 
                       hotConfigCache.size(), maxSize);
            
            // Simple LRU: remove 20% of entries
            int toRemove = (int) (hotConfigCache.size() * 0.2);
            hotConfigCache.keySet().stream()
                .limit(toRemove)
                .forEach(hotConfigCache::remove);
            
            logger.info("Optimized hot cache, new size: {}", hotConfigCache.size());
        }
    }
    
    /**
     * Performance monitoring data
     */
    public static class HotCacheStats {
        private final int totalEntries;
        private final Map<String, Long> entriesPerTenant;
        
        public HotCacheStats(int totalEntries, Map<String, Long> entriesPerTenant) {
            this.totalEntries = totalEntries;
            this.entriesPerTenant = entriesPerTenant;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public Map<String, Long> getEntriesPerTenant() { return entriesPerTenant; }
    }
}