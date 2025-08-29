package com.filetransfer.web.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Caching configuration for performance optimization.
 * Implements caching for frequently accessed configurations and lookups.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SUB_SERVICE_CONFIG_CACHE = "subServiceConfigs";
    public static final String TENANT_TIMEZONE_CACHE = "tenantTimezones";
    public static final String FILE_SCHEMA_CACHE = "fileSchemas";
    public static final String CUT_OFF_TIME_CACHE = "cutOffTimes";
    public static final String HOLIDAY_CACHE = "holidays";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            SUB_SERVICE_CONFIG_CACHE,
            TENANT_TIMEZONE_CACHE,
            FILE_SCHEMA_CACHE,
            CUT_OFF_TIME_CACHE,
            HOLIDAY_CACHE
        );
        
        // Set cache to allow null values for optional lookups
        cacheManager.setAllowNullValues(true);
        
        return cacheManager;
    }

    /**
     * Custom key generator for multi-parameter cache keys
     */
    @Bean("customKeyGenerator")
    public KeyGenerator keyGenerator() {
        return new CustomKeyGenerator();
    }

    /**
     * Custom key generator implementation
     */
    public static class CustomKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, Method method, Object... params) {
            return method.getName() + "_" + Arrays.deepHashCode(params);
        }
    }
}