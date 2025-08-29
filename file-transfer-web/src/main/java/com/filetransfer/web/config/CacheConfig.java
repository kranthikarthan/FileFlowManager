package com.filetransfer.web.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced caching configuration with multi-level caching strategy
 * Implements both local (Caffeine) and distributed (Redis) caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.strategy:local}")
    private String cacheStrategy;

    @Value("${cache.local.maximum-size:10000}")
    private long localCacheMaximumSize;

    @Value("${cache.local.expire-after-write:PT10M}")
    private Duration localCacheExpireAfterWrite;

    @Value("${cache.distributed.ttl:PT30M}")
    private Duration distributedCacheTtl;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * Local cache manager using Caffeine for high-performance local caching
     */
    @Bean
    @Primary
    @Profile("!distributed-cache")
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure Caffeine cache with performance optimizations
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(localCacheMaximumSize)
            .expireAfterWrite(localCacheExpireAfterWrite)
            .recordStats()
            .build());
        
        // Pre-configure cache names
        cacheManager.setCacheNames(
            "tenantCache",
            "subServiceConfigs", 
            "tenantTimezones",
            "fileSchemas",
            "validationResults",
            "configurationCache",
            "alertCache",
            "userCache",
            "sessionCache",
            "rateLimitCache"
        );
        
        return cacheManager;
    }

    /**
     * Distributed cache manager using Redisson for multi-instance caching
     */
    @Bean
    @Primary
    @Profile("distributed-cache")
    public CacheManager distributedCacheManager() {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient is required for distributed caching");
        }

        Map<String, CacheConfig> configMap = new HashMap<>();
        
        // Configure different TTL for different cache types
        configMap.put("tenantCache", new CacheConfig(
            Duration.ofHours(1).toMillis(),    // TTL: 1 hour
            Duration.ofMinutes(10).toMillis()  // Max idle: 10 minutes
        ));
        
        configMap.put("subServiceConfigs", new CacheConfig(
            Duration.ofMinutes(30).toMillis(), // TTL: 30 minutes
            Duration.ofMinutes(5).toMillis()   // Max idle: 5 minutes
        ));
        
        configMap.put("tenantTimezones", new CacheConfig(
            Duration.ofHours(6).toMillis(),    // TTL: 6 hours
            Duration.ofHours(1).toMillis()     // Max idle: 1 hour
        ));
        
        configMap.put("fileSchemas", new CacheConfig(
            Duration.ofMinutes(15).toMillis(), // TTL: 15 minutes
            Duration.ofMinutes(3).toMillis()   // Max idle: 3 minutes
        ));
        
        configMap.put("validationResults", new CacheConfig(
            Duration.ofMinutes(5).toMillis(),  // TTL: 5 minutes
            Duration.ofMinutes(1).toMillis()   // Max idle: 1 minute
        ));
        
        configMap.put("configurationCache", new CacheConfig(
            Duration.ofMinutes(20).toMillis(), // TTL: 20 minutes
            Duration.ofMinutes(5).toMillis()   // Max idle: 5 minutes
        ));
        
        configMap.put("alertCache", new CacheConfig(
            Duration.ofMinutes(2).toMillis(),  // TTL: 2 minutes
            Duration.ofSeconds(30).toMillis()  // Max idle: 30 seconds
        ));
        
        configMap.put("userCache", new CacheConfig(
            Duration.ofMinutes(30).toMillis(), // TTL: 30 minutes
            Duration.ofMinutes(10).toMillis()  // Max idle: 10 minutes
        ));
        
        configMap.put("sessionCache", new CacheConfig(
            Duration.ofMinutes(15).toMillis(), // TTL: 15 minutes
            Duration.ofMinutes(5).toMillis()   // Max idle: 5 minutes
        ));
        
        configMap.put("rateLimitCache", new CacheConfig(
            Duration.ofMinutes(1).toMillis(),  // TTL: 1 minute
            Duration.ofSeconds(10).toMillis()  // Max idle: 10 seconds
        ));

        return new RedissonSpringCacheManager(redissonClient, configMap);
    }

    /**
     * Multi-level cache manager (combines local and distributed caching)
     */
    @Bean
    @Profile("multi-level-cache")
    public CacheManager multiLevelCacheManager() {
        return new MultiLevelCacheManager(localCacheManager(), distributedCacheManager());
    }

    /**
     * Cache configuration for specific cache regions
     */
    @Bean
    public CacheConfigurationProperties cacheConfigurationProperties() {
        CacheConfigurationProperties properties = new CacheConfigurationProperties();
        
        // Tenant cache configuration
        properties.addCacheConfig("tenantCache", CacheConfigurationProperties.CacheConfig.builder()
            .ttl(Duration.ofHours(1))
            .maxSize(1000L)
            .refreshAfterWrite(Duration.ofMinutes(30))
            .build());
        
        // Sub-service configuration cache
        properties.addCacheConfig("subServiceConfigs", CacheConfigurationProperties.CacheConfig.builder()
            .ttl(Duration.ofMinutes(30))
            .maxSize(5000L)
            .refreshAfterWrite(Duration.ofMinutes(10))
            .build());
        
        // Timezone cache (rarely changes)
        properties.addCacheConfig("tenantTimezones", CacheConfigurationProperties.CacheConfig.builder()
            .ttl(Duration.ofHours(6))
            .maxSize(500L)
            .refreshAfterWrite(Duration.ofHours(2))
            .build());
        
        // File schema cache
        properties.addCacheConfig("fileSchemas", CacheConfigurationProperties.CacheConfig.builder()
            .ttl(Duration.ofMinutes(15))
            .maxSize(2000L)
            .refreshAfterWrite(Duration.ofMinutes(5))
            .build());
        
        // Validation results cache (short-lived)
        properties.addCacheConfig("validationResults", CacheConfigurationProperties.CacheConfig.builder()
            .ttl(Duration.ofMinutes(5))
            .maxSize(10000L)
            .refreshAfterWrite(Duration.ofMinutes(2))
            .build());
        
        return properties;
    }

    /**
     * Cache statistics and monitoring configuration
     */
    @Bean
    public CacheMetricsConfiguration cacheMetricsConfiguration() {
        return new CacheMetricsConfiguration();
    }

    /**
     * Multi-level cache manager implementation
     */
    public static class MultiLevelCacheManager implements CacheManager {
        private final CacheManager localCacheManager;
        private final CacheManager distributedCacheManager;

        public MultiLevelCacheManager(CacheManager localCacheManager, CacheManager distributedCacheManager) {
            this.localCacheManager = localCacheManager;
            this.distributedCacheManager = distributedCacheManager;
        }

        @Override
        public org.springframework.cache.Cache getCache(String name) {
            org.springframework.cache.Cache localCache = localCacheManager.getCache(name);
            org.springframework.cache.Cache distributedCache = distributedCacheManager.getCache(name);
            
            if (localCache != null && distributedCache != null) {
                return new MultiLevelCache(name, localCache, distributedCache);
            } else if (localCache != null) {
                return localCache;
            } else {
                return distributedCache;
            }
        }

        @Override
        public java.util.Collection<String> getCacheNames() {
            java.util.Set<String> cacheNames = new java.util.HashSet<>();
            cacheNames.addAll(localCacheManager.getCacheNames());
            cacheNames.addAll(distributedCacheManager.getCacheNames());
            return cacheNames;
        }
    }

    /**
     * Multi-level cache implementation
     */
    public static class MultiLevelCache implements org.springframework.cache.Cache {
        private final String name;
        private final org.springframework.cache.Cache localCache;
        private final org.springframework.cache.Cache distributedCache;

        public MultiLevelCache(String name, org.springframework.cache.Cache localCache, 
                              org.springframework.cache.Cache distributedCache) {
            this.name = name;
            this.localCache = localCache;
            this.distributedCache = distributedCache;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return localCache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            // Try local cache first
            ValueWrapper value = localCache.get(key);
            if (value != null) {
                return value;
            }
            
            // Fallback to distributed cache
            value = distributedCache.get(key);
            if (value != null) {
                // Populate local cache
                localCache.put(key, value.get());
            }
            
            return value;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            // Try local cache first
            T value = localCache.get(key, type);
            if (value != null) {
                return value;
            }
            
            // Fallback to distributed cache
            value = distributedCache.get(key, type);
            if (value != null) {
                // Populate local cache
                localCache.put(key, value);
            }
            
            return value;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            return localCache.get(key, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            // Update both caches
            localCache.put(key, value);
            distributedCache.put(key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            ValueWrapper existing = get(key);
            if (existing == null) {
                put(key, value);
                return null;
            }
            return existing;
        }

        @Override
        public void evict(Object key) {
            // Evict from both caches
            localCache.evict(key);
            distributedCache.evict(key);
        }

        @Override
        public void clear() {
            // Clear both caches
            localCache.clear();
            distributedCache.clear();
        }
    }

    /**
     * Cache configuration properties
     */
    public static class CacheConfigurationProperties {
        private final Map<String, CacheConfig> cacheConfigs = new HashMap<>();

        public void addCacheConfig(String cacheName, CacheConfig config) {
            cacheConfigs.put(cacheName, config);
        }

        public CacheConfig getCacheConfig(String cacheName) {
            return cacheConfigs.get(cacheName);
        }

        public static class CacheConfig {
            private Duration ttl;
            private Long maxSize;
            private Duration refreshAfterWrite;

            private CacheConfig(Builder builder) {
                this.ttl = builder.ttl;
                this.maxSize = builder.maxSize;
                this.refreshAfterWrite = builder.refreshAfterWrite;
            }

            public static Builder builder() {
                return new Builder();
            }

            public static class Builder {
                private Duration ttl;
                private Long maxSize;
                private Duration refreshAfterWrite;

                public Builder ttl(Duration ttl) {
                    this.ttl = ttl;
                    return this;
                }

                public Builder maxSize(Long maxSize) {
                    this.maxSize = maxSize;
                    return this;
                }

                public Builder refreshAfterWrite(Duration refreshAfterWrite) {
                    this.refreshAfterWrite = refreshAfterWrite;
                    return this;
                }

                public CacheConfig build() {
                    return new CacheConfig(this);
                }
            }

            // Getters
            public Duration getTtl() { return ttl; }
            public Long getMaxSize() { return maxSize; }
            public Duration getRefreshAfterWrite() { return refreshAfterWrite; }
        }
    }

    /**
     * Cache metrics configuration for monitoring
     */
    public static class CacheMetricsConfiguration {
        // Implementation for cache metrics collection
    }
}