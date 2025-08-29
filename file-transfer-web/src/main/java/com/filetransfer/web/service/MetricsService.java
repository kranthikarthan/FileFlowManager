package com.filetransfer.web.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Service for managing custom application metrics
 * Provides business-specific metrics and counters
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;
    
    // Counters for business events
    private final Counter fileTransferAttempts;
    private final Counter fileTransferSuccesses;
    private final Counter fileTransferFailures;
    private final Counter validationAttempts;
    private final Counter validationSuccesses;
    private final Counter validationFailures;
    private final Counter cutoffExtensions;
    private final Counter alertsGenerated;
    
    // Timers for performance tracking
    private final Timer fileProcessingTime;
    private final Timer validationTime;
    private final Timer databaseQueryTime;
    
    // Gauges for current state
    private final AtomicLong activeFileTransfers = new AtomicLong(0);
    private final AtomicLong queuedTransfers = new AtomicLong(0);
    private final AtomicLong configuredSubServices = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> tenantMetrics = new ConcurrentHashMap<>();

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.fileTransferAttempts = Counter.builder("file.transfer.attempts")
            .description("Total number of file transfer attempts")
            .register(meterRegistry);
            
        this.fileTransferSuccesses = Counter.builder("file.transfer.successes")
            .description("Total number of successful file transfers")
            .register(meterRegistry);
            
        this.fileTransferFailures = Counter.builder("file.transfer.failures")
            .description("Total number of failed file transfers")
            .register(meterRegistry);
            
        this.validationAttempts = Counter.builder("file.validation.attempts")
            .description("Total number of file validation attempts")
            .register(meterRegistry);
            
        this.validationSuccesses = Counter.builder("file.validation.successes")
            .description("Total number of successful file validations")
            .register(meterRegistry);
            
        this.validationFailures = Counter.builder("file.validation.failures")
            .description("Total number of failed file validations")
            .register(meterRegistry);
            
        this.cutoffExtensions = Counter.builder("cutoff.extensions")
            .description("Total number of cutoff time extensions")
            .register(meterRegistry);
            
        this.alertsGenerated = Counter.builder("alerts.generated")
            .description("Total number of alerts generated")
            .register(meterRegistry);
        
        // Initialize timers
        this.fileProcessingTime = Timer.builder("file.processing.time")
            .description("Time taken to process files")
            .register(meterRegistry);
            
        this.validationTime = Timer.builder("file.validation.time")
            .description("Time taken to validate files")
            .register(meterRegistry);
            
        this.databaseQueryTime = Timer.builder("database.query.time")
            .description("Time taken for database queries")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("file.transfer.active")
            .description("Number of currently active file transfers")
            .register(meterRegistry, activeFileTransfers, AtomicLong::get);
            
        Gauge.builder("file.transfer.queued")
            .description("Number of queued file transfers")
            .register(meterRegistry, queuedTransfers, AtomicLong::get);
            
        Gauge.builder("subservices.configured")
            .description("Number of configured sub-services")
            .register(meterRegistry, configuredSubServices, AtomicLong::get);
    }

    // File Transfer Metrics
    
    public void recordFileTransferAttempt(String tenantId, String serviceType, String direction) {
        fileTransferAttempts.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("service_type", serviceType),
                Tag.of("direction", direction)
            )
        );
        logger.debug("Recorded file transfer attempt: tenant={}, service={}, direction={}", 
                    tenantId, serviceType, direction);
    }
    
    public void recordFileTransferSuccess(String tenantId, String serviceType, String direction, long fileSizeBytes) {
        fileTransferSuccesses.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("service_type", serviceType),
                Tag.of("direction", direction)
            )
        );
        
        // Record file size distribution
        meterRegistry.summary("file.transfer.size.bytes")
            .record(fileSizeBytes);
            
        logger.debug("Recorded file transfer success: tenant={}, service={}, direction={}, size={}", 
                    tenantId, serviceType, direction, fileSizeBytes);
    }
    
    public void recordFileTransferFailure(String tenantId, String serviceType, String direction, String errorType) {
        fileTransferFailures.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("service_type", serviceType),
                Tag.of("direction", direction),
                Tag.of("error_type", errorType)
            )
        );
        logger.warn("Recorded file transfer failure: tenant={}, service={}, direction={}, error={}", 
                   tenantId, serviceType, direction, errorType);
    }

    // Validation Metrics
    
    public void recordValidationAttempt(String tenantId, String fileType, String schemaType) {
        validationAttempts.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("file_type", fileType),
                Tag.of("schema_type", schemaType)
            )
        );
    }
    
    public void recordValidationSuccess(String tenantId, String fileType, String schemaType) {
        validationSuccesses.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("file_type", fileType),
                Tag.of("schema_type", schemaType)
            )
        );
    }
    
    public void recordValidationFailure(String tenantId, String fileType, String schemaType, String errorType) {
        validationFailures.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("file_type", fileType),
                Tag.of("schema_type", schemaType),
                Tag.of("error_type", errorType)
            )
        );
    }

    // Timing Metrics
    
    public Timer.Sample startFileProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileProcessingTime(Timer.Sample sample, String tenantId, String operation) {
        sample.stop(Timer.builder("file.processing.time")
            .tag("tenant", tenantId)
            .tag("operation", operation)
            .register(meterRegistry));
    }
    
    public Timer.Sample startValidationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordValidationTime(Timer.Sample sample, String fileType) {
        sample.stop(Timer.builder("file.validation.time")
            .tag("file_type", fileType)
            .register(meterRegistry));
    }
    
    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDatabaseQueryTime(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("database.query.time")
            .tag("query_type", queryType)
            .register(meterRegistry));
    }

    // Gauge Updates
    
    public void incrementActiveTransfers() {
        activeFileTransfers.incrementAndGet();
    }
    
    public void decrementActiveTransfers() {
        activeFileTransfers.decrementAndGet();
    }
    
    public void incrementQueuedTransfers() {
        queuedTransfers.incrementAndGet();
    }
    
    public void decrementQueuedTransfers() {
        queuedTransfers.decrementAndGet();
    }
    
    public void updateConfiguredSubServices(long count) {
        configuredSubServices.set(count);
    }

    // Business Metrics
    
    public void recordCutoffExtension(String tenantId, String serviceType, Duration extensionDuration) {
        cutoffExtensions.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("service_type", serviceType)
            )
        );
        
        // Record extension duration
        meterRegistry.timer("cutoff.extension.duration")
            .record(extensionDuration);
            
        logger.info("Recorded cutoff extension: tenant={}, service={}, duration={}", 
                   tenantId, serviceType, extensionDuration);
    }
    
    public void recordAlertGenerated(String tenantId, String alertLevel, String alertType) {
        alertsGenerated.increment(
            Tags.of(
                Tag.of("tenant", tenantId),
                Tag.of("alert_level", alertLevel),
                Tag.of("alert_type", alertType)
            )
        );
        logger.info("Recorded alert: tenant={}, level={}, type={}", tenantId, alertLevel, alertType);
    }

    // Tenant-specific metrics
    
    public void recordTenantActivity(String tenantId, String activity) {
        String key = tenantId + "_" + activity;
        tenantMetrics.computeIfAbsent(key, k -> {
            AtomicLong counter = new AtomicLong(0);
            Gauge.builder("tenant.activity")
                .tag("tenant", tenantId)
                .tag("activity", activity)
                .register(meterRegistry, counter, AtomicLong::get);
            return counter;
        }).incrementAndGet();
    }
    
    // Utility methods for complex measurements
    
    public <T> T recordExecutionTime(String metricName, String tenantId, Supplier<T> operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = operation.get();
            sample.stop(Timer.builder(metricName)
                .tag("tenant", tenantId)
                .tag("status", "success")
                .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder(metricName)
                .tag("tenant", tenantId)
                .tag("status", "error")
                .register(meterRegistry));
            throw e;
        }
    }
}