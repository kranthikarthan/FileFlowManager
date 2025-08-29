package com.filetransfer.batch.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for batch application endpoints
 * Implements distributed rate limiting using Bucket4j and Redis
 */
@Component
public class BatchRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BatchRateLimitingFilter.class);

    @Value("${batch.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${batch.security.rate-limiting.requests-per-minute:30}")
    private int requestsPerMinute;

    @Value("${batch.security.rate-limiting.burst-capacity:10}")
    private int burstCapacity;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private LettuceBasedProxyManager<String> proxyManager;

    // Local cache for buckets when Redis is not available
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks and monitoring
        String requestURI = request.getRequestURI();
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String clientKey = getClientKey(request);
            Bucket bucket = getBucket(clientKey);

            if (bucket.tryConsume(1)) {
                // Add rate limiting headers
                addRateLimitHeaders(response, bucket);
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                handleRateLimitExceeded(request, response, clientKey);
            }

        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            // Allow request to proceed if rate limiting fails
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Check if path should be excluded from rate limiting
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.contains("/static/");
    }

    /**
     * Generate client key for rate limiting
     */
    private String getClientKey(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String endpoint = request.getRequestURI();
        
        // Different rate limits for different endpoint types
        String rateLimitType = determineRateLimitType(endpoint);
        
        return String.format("batch_rate_limit:%s:%s:%s", rateLimitType, clientIp, 
                           userAgent != null ? Integer.toString(userAgent.hashCode()) : "unknown");
    }

    /**
     * Determine rate limit type based on endpoint
     */
    private String determineRateLimitType(String endpoint) {
        if (endpoint.contains("/jobs/")) {
            return "job_execution";
        } else if (endpoint.contains("/performance/")) {
            return "performance_query";
        } else if (endpoint.contains("/statistics/")) {
            return "statistics_query";
        } else {
            return "general";
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Get rate limiting bucket for client
     */
    private Bucket getBucket(String clientKey) {
        if (proxyManager != null) {
            // Use distributed bucket with Redis
            return proxyManager.builder()
                .build(clientKey, () -> createBucketConfiguration());
        } else {
            // Use local bucket as fallback
            return localBuckets.computeIfAbsent(clientKey, 
                key -> Bucket.builder()
                    .addLimit(createBandwidth())
                    .build());
        }
    }

    /**
     * Create bucket configuration for distributed buckets
     */
    private io.github.bucket4j.distributed.BucketConfiguration createBucketConfiguration() {
        return io.github.bucket4j.distributed.BucketConfiguration.builder()
            .addLimit(createBandwidth())
            .build();
    }

    /**
     * Create bandwidth limit
     */
    private Bandwidth createBandwidth() {
        return Bandwidth.builder()
            .capacity(requestsPerMinute + burstCapacity)
            .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
            .initialTokens(burstCapacity)
            .build();
    }

    /**
     * Add rate limiting headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, Bucket bucket) {
        long availableTokens = bucket.getAvailableTokens();
        response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Window", "60");
    }

    /**
     * Handle rate limit exceeded
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                       String clientKey) throws IOException {
        
        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        logger.warn("Rate limit exceeded for client: {} on endpoint: {}", clientIp, endpoint);
        
        // Set response headers
        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setHeader("Content-Type", "application/json");
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Window", "60");
        response.setHeader("Retry-After", "60");
        
        // Set response body
        String jsonResponse = String.format("""
            {
                "error": "Rate limit exceeded",
                "message": "Too many requests. Please try again later.",
                "rateLimitInfo": {
                    "limit": %d,
                    "window": "60 seconds",
                    "retryAfter": "60 seconds"
                },
                "timestamp": "%s"
            }
            """, requestsPerMinute, java.time.Instant.now().toString());
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}