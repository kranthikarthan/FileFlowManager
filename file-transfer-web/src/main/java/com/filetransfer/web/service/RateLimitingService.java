package com.filetransfer.web.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for implementing rate limiting using Token Bucket algorithm
 * Supports both in-memory and distributed (Redis) rate limiting
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    @Value("${security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${security.rate-limiting.distributed:false}")
    private boolean distributedRateLimiting;

    // Default rate limits (can be overridden via configuration)
    @Value("${security.rate-limiting.api.requests-per-minute:60}")
    private int apiRequestsPerMinute;

    @Value("${security.rate-limiting.login.attempts-per-hour:10}")
    private int loginAttemptsPerHour;

    @Value("${security.rate-limiting.file-upload.requests-per-minute:10}")
    private int fileUploadRequestsPerMinute;

    @Value("${security.rate-limiting.tenant.requests-per-minute:1000}")
    private int tenantRequestsPerMinute;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // In-memory cache for rate limiting buckets
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * Rate limiting types for different API endpoints
     */
    public enum RateLimitType {
        API_GENERAL("api_general", Duration.ofMinutes(1)),
        LOGIN_ATTEMPTS("login_attempts", Duration.ofHours(1)),
        FILE_UPLOAD("file_upload", Duration.ofMinutes(1)),
        TENANT_SPECIFIC("tenant_specific", Duration.ofMinutes(1)),
        ADMIN_OPERATIONS("admin_operations", Duration.ofMinutes(1)),
        BULK_OPERATIONS("bulk_operations", Duration.ofMinutes(5));

        private final String key;
        private final Duration refillPeriod;

        RateLimitType(String key, Duration refillPeriod) {
            this.key = key;
            this.refillPeriod = refillPeriod;
        }

        public String getKey() {
            return key;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }
    }

    /**
     * Check if a request should be allowed based on rate limiting
     */
    public boolean isAllowed(String identifier, RateLimitType type) {
        if (!rateLimitingEnabled) {
            return true;
        }

        try {
            Bucket bucket = getBucket(identifier, type);
            boolean allowed = bucket.tryConsume(1);
            
            if (!allowed) {
                logger.warn("Rate limit exceeded for identifier: {} type: {}", identifier, type);
            }
            
            return allowed;
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: {} type: {}", identifier, type, e);
            // Fail open - allow request if rate limiting service fails
            return true;
        }
    }

    /**
     * Check if multiple tokens can be consumed
     */
    public boolean isAllowed(String identifier, RateLimitType type, int tokens) {
        if (!rateLimitingEnabled) {
            return true;
        }

        try {
            Bucket bucket = getBucket(identifier, type);
            boolean allowed = bucket.tryConsume(tokens);
            
            if (!allowed) {
                logger.warn("Rate limit exceeded for identifier: {} type: {} tokens: {}", 
                           identifier, type, tokens);
            }
            
            return allowed;
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: {} type: {} tokens: {}", 
                        identifier, type, tokens, e);
            return true;
        }
    }

    /**
     * Get remaining tokens for an identifier
     */
    public long getRemainingTokens(String identifier, RateLimitType type) {
        if (!rateLimitingEnabled) {
            return Long.MAX_VALUE;
        }

        try {
            Bucket bucket = getBucket(identifier, type);
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            logger.error("Error getting remaining tokens for identifier: {} type: {}", 
                        identifier, type, e);
            return Long.MAX_VALUE;
        }
    }

    /**
     * Reset rate limit for an identifier (admin function)
     */
    public void resetRateLimit(String identifier, RateLimitType type) {
        String cacheKey = buildCacheKey(identifier, type);
        
        if (distributedRateLimiting && redisTemplate != null) {
            redisTemplate.delete(cacheKey);
        } else {
            bucketCache.remove(cacheKey);
        }
        
        logger.info("Reset rate limit for identifier: {} type: {}", identifier, type);
    }

    /**
     * Get or create a bucket for rate limiting
     */
    private Bucket getBucket(String identifier, RateLimitType type) {
        String cacheKey = buildCacheKey(identifier, type);
        
        if (distributedRateLimiting && redisTemplate != null) {
            return getDistributedBucket(cacheKey, type);
        } else {
            return getLocalBucket(cacheKey, type);
        }
    }

    /**
     * Get bucket from local cache
     */
    private Bucket getLocalBucket(String cacheKey, RateLimitType type) {
        return bucketCache.computeIfAbsent(cacheKey, key -> createBucket(type));
    }

    /**
     * Get bucket from distributed cache (Redis)
     */
    private Bucket getDistributedBucket(String cacheKey, RateLimitType type) {
        // In a real implementation, you would use Bucket4j's Redis integration
        // For simplicity, falling back to local cache
        // TODO: Implement proper Redis-based distributed rate limiting
        return getLocalBucket(cacheKey, type);
    }

    /**
     * Create a new bucket with appropriate limits
     */
    private Bucket createBucket(RateLimitType type) {
        return switch (type) {
            case API_GENERAL -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(apiRequestsPerMinute, 
                         Refill.intervally(apiRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
                
            case LOGIN_ATTEMPTS -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(loginAttemptsPerHour, 
                         Refill.intervally(loginAttemptsPerHour, Duration.ofHours(1))))
                .build();
                
            case FILE_UPLOAD -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(fileUploadRequestsPerMinute, 
                         Refill.intervally(fileUploadRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
                
            case TENANT_SPECIFIC -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(tenantRequestsPerMinute, 
                         Refill.intervally(tenantRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
                
            case ADMIN_OPERATIONS -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(20, 
                         Refill.intervally(20, Duration.ofMinutes(1))))
                .build();
                
            case BULK_OPERATIONS -> Bucket4j.builder()
                .addLimit(Bandwidth.classic(5, 
                         Refill.intervally(5, Duration.ofMinutes(5))))
                .build();
        };
    }

    /**
     * Build cache key for rate limiting
     */
    private String buildCacheKey(String identifier, RateLimitType type) {
        return String.format("rate_limit:%s:%s", type.getKey(), identifier);
    }

    /**
     * Get rate limiting statistics
     */
    public RateLimitInfo getRateLimitInfo(String identifier, RateLimitType type) {
        if (!rateLimitingEnabled) {
            return new RateLimitInfo(Long.MAX_VALUE, Long.MAX_VALUE, false);
        }

        try {
            Bucket bucket = getBucket(identifier, type);
            long availableTokens = bucket.getAvailableTokens();
            long capacity = getCapacityForType(type);
            boolean isLimited = availableTokens < capacity;
            
            return new RateLimitInfo(availableTokens, capacity, isLimited);
        } catch (Exception e) {
            logger.error("Error getting rate limit info for identifier: {} type: {}", 
                        identifier, type, e);
            return new RateLimitInfo(Long.MAX_VALUE, Long.MAX_VALUE, false);
        }
    }

    /**
     * Get capacity for rate limit type
     */
    private long getCapacityForType(RateLimitType type) {
        return switch (type) {
            case API_GENERAL -> apiRequestsPerMinute;
            case LOGIN_ATTEMPTS -> loginAttemptsPerHour;
            case FILE_UPLOAD -> fileUploadRequestsPerMinute;
            case TENANT_SPECIFIC -> tenantRequestsPerMinute;
            case ADMIN_OPERATIONS -> 20;
            case BULK_OPERATIONS -> 5;
        };
    }

    /**
     * Rate limit information DTO
     */
    public static class RateLimitInfo {
        private final long availableTokens;
        private final long capacity;
        private final boolean isLimited;

        public RateLimitInfo(long availableTokens, long capacity, boolean isLimited) {
            this.availableTokens = availableTokens;
            this.capacity = capacity;
            this.isLimited = isLimited;
        }

        public long getAvailableTokens() {
            return availableTokens;
        }

        public long getCapacity() {
            return capacity;
        }

        public boolean isLimited() {
            return isLimited;
        }

        public double getUsagePercentage() {
            if (capacity == 0) return 0.0;
            return ((double) (capacity - availableTokens) / capacity) * 100.0;
        }
    }
}