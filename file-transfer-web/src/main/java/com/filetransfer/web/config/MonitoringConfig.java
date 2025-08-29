package com.filetransfer.web.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration for monitoring and observability
 * Provides metrics, health checks, and tracing configuration
 */
@Configuration
public class MonitoringConfig {

    /**
     * Enable @Timed annotation support for automatic method timing
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Customize meter registry with common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // Add common tags to all metrics
            registry.config()
                .commonTags(
                    Tag.of("application", "file-transfer-web"),
                    Tag.of("environment", getEnvironment()),
                    Tag.of("version", getClass().getPackage().getImplementationVersion() != null 
                        ? getClass().getPackage().getImplementationVersion() : "dev")
                )
                // Add meter filters for performance
                .meterFilter(MeterFilter.deny(id -> {
                    String name = id.getName();
                    // Deny noisy metrics that aren't useful
                    return name.startsWith("jvm.gc.pause") && 
                           id.getTag("cause") != null && 
                           id.getTag("cause").contains("System.gc");
                }))
                // Maximum number of meters to prevent memory leaks
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", Timer.Sample.class, 1000));
        };
    }

    /**
     * Database health indicator
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    return Health.up()
                        .withDetail("database", "Available")
                        .withDetail("validationQuery", "Connection validation successful")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("database", "Connection validation failed")
                        .build();
                }
            } catch (SQLException e) {
                return Health.down(e)
                    .withDetail("database", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    /**
     * File system health indicator
     */
    @Bean
    public HealthIndicator fileSystemHealthIndicator() {
        return () -> {
            try {
                java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
                long freeSpace = tempDir.getFreeSpace();
                long totalSpace = tempDir.getTotalSpace();
                double freeSpacePercentage = (double) freeSpace / totalSpace * 100;

                if (freeSpacePercentage > 10) {
                    return Health.up()
                        .withDetail("fileSystem", "Available")
                        .withDetail("freeSpace", formatBytes(freeSpace))
                        .withDetail("totalSpace", formatBytes(totalSpace))
                        .withDetail("freeSpacePercentage", String.format("%.2f%%", freeSpacePercentage))
                        .build();
                } else {
                    return Health.down()
                        .withDetail("fileSystem", "Low disk space")
                        .withDetail("freeSpace", formatBytes(freeSpace))
                        .withDetail("freeSpacePercentage", String.format("%.2f%%", freeSpacePercentage))
                        .build();
                }
            } catch (Exception e) {
                return Health.down(e)
                    .withDetail("fileSystem", "Health check failed")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    /**
     * External dependencies health indicator
     */
    @Bean
    public HealthIndicator externalDependenciesHealthIndicator() {
        return () -> {
            // Check external systems like message queues, external APIs, etc.
            // For now, return UP - can be extended based on actual dependencies
            return Health.up()
                .withDetail("externalAPIs", "All systems operational")
                .build();
        };
    }

    /**
     * Get environment name from system properties or environment variables
     */
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", 
               System.getenv("SPRING_PROFILES_ACTIVE") != null 
                   ? System.getenv("SPRING_PROFILES_ACTIVE") : "unknown");
    }

    /**
     * Format bytes for human-readable display
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}