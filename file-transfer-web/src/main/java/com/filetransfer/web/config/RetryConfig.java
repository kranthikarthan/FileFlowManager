package com.filetransfer.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration for Spring Retry mechanism.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry is enabled via @EnableRetry annotation
    // Individual retry configurations are defined on service methods using @Retryable
}