# File Transfer System Monitoring Implementation Guide

## 📊 Overview

This guide covers the comprehensive monitoring and observability solution implemented for the File Transfer Management System. The monitoring stack provides real-time insights into system health, performance metrics, business KPIs, and proactive alerting.

## 🏗️ Architecture

### Monitoring Stack Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Prometheus    │    │     Grafana     │
│   (Micrometer)  │───▶│   (Metrics)     │───▶│ (Visualization) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│     Zipkin      │    │  AlertManager   │
│   (Tracing)     │    │   (Alerting)    │
└─────────────────┘    └─────────────────┘
         
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Logstash      │    │   Elasticsearch │
│     Logs        │───▶│ (Processing)    │───▶│   (Storage)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │     Kibana      │
                                              │ (Log Analysis)  │
                                              └─────────────────┘
```

## 🚀 Quick Start

### 1. Start the Monitoring Stack

```bash
# Make script executable
chmod +x scripts/start-monitoring.sh

# Start all monitoring services
./scripts/start-monitoring.sh
```

### 2. Access Monitoring Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3001 | admin/admin123 |
| **Prometheus** | http://localhost:9090 | - |
| **Zipkin** | http://localhost:9411 | - |
| **Kibana** | http://localhost:5601 | - |
| **AlertManager** | http://localhost:9093 | - |

### 3. Verify Application Metrics

```bash
# Check application health
curl http://localhost:8080/actuator/health

# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Check custom monitoring endpoints
curl http://localhost:8080/api/monitoring/health
curl http://localhost:8080/api/monitoring/dashboard
```

## 📈 Metrics Collection

### Application Metrics (Micrometer + Prometheus)

#### Business Metrics
```java
// File transfer metrics
file.transfer.attempts_total{tenant, service_type, direction}
file.transfer.successes_total{tenant, service_type, direction}
file.transfer.failures_total{tenant, service_type, direction, error_type}

// Validation metrics
file.validation.attempts_total{tenant, file_type, schema_type}
file.validation.successes_total{tenant, file_type, schema_type}
file.validation.failures_total{tenant, file_type, schema_type, error_type}

// Business operations
cutoff.extensions_total{tenant, service_type}
alerts.generated_total{tenant, alert_level, alert_type}
```

#### Performance Metrics
```java
// Timing metrics
file.processing.time_seconds{tenant, operation}
file.validation.time_seconds{file_type}
database.query.time_seconds{query_type}

// System gauges
file.transfer.active
file.transfer.queued
subservices.configured
```

#### System Metrics
```java
// JVM metrics
jvm.memory.used_bytes{area="heap|non-heap"}
jvm.gc.pause_seconds
jvm.threads.live

// HTTP metrics
http.server.requests_seconds{method, status, uri}
```

### Custom Metrics Integration

```java
// In your service classes
@Autowired
private MetricsService metricsService;

// Record business events
metricsService.recordFileTransferAttempt(tenantId, serviceType, direction);
metricsService.recordFileTransferSuccess(tenantId, serviceType, direction, fileSize);
metricsService.recordFileTransferFailure(tenantId, serviceType, direction, errorType);

// Time operations
Timer.Sample sample = metricsService.startFileProcessingTimer();
// ... perform operation ...
metricsService.recordFileProcessingTime(sample, tenantId, operation);

// Update gauges
metricsService.incrementActiveTransfers();
metricsService.updateConfiguredSubServices(count);
```

## 📊 Dashboards

### Grafana Dashboards

#### 1. File Transfer System Overview
- **File Transfer Rate**: Real-time transfer attempts, successes, failures
- **Success Rate Gauge**: Percentage of successful transfers
- **Processing Time Percentiles**: 50th, 95th, 99th percentile response times
- **Queue Status**: Active and queued file transfers
- **Tenant Distribution**: Transfer volume by tenant
- **Data Volume**: Bytes transferred over time

#### 2. System Health Dashboard
- **Memory Usage**: JVM heap and non-heap memory utilization
- **CPU Usage**: Application and system CPU consumption
- **Database Metrics**: Connection pool, query performance
- **Disk Usage**: File system utilization
- **Network I/O**: Transfer throughput

#### 3. Business Intelligence Dashboard
- **SLA Compliance**: Transfer success rates vs targets
- **Cutoff Time Analysis**: Extensions and violations
- **Error Analysis**: Failure categorization and trends
- **Tenant Performance**: Per-tenant metrics and comparisons

### Custom Dashboard Creation

```json
// Example Grafana panel configuration
{
  "targets": [
    {
      "expr": "rate(file_transfer_successes_total[5m])",
      "legendFormat": "Success Rate - {{tenant}}"
    }
  ],
  "title": "File Transfer Success Rate by Tenant",
  "type": "timeseries"
}
```

## 🔍 Distributed Tracing

### Zipkin Integration

Distributed tracing tracks requests across service boundaries:

```java
// Automatic tracing with @Timed annotation
@Timed(value = "file.processing", description = "File processing time")
public void processFile(String fileName) {
    // Method automatically traced
}

// Manual span creation
Span span = tracer.nextSpan()
    .name("file-validation")
    .tag("tenant.id", tenantId)
    .tag("file.type", fileType)
    .start();
try {
    // Validation logic
} finally {
    span.end();
}
```

### Correlation IDs

All logs and traces include correlation IDs for request tracking:

```java
// Automatic correlation ID injection
GET /api/files HTTP/1.1
X-Correlation-ID: 123e4567-e89b-12d3-a456-426614174000

// Log output includes correlation context
2024-01-20 10:30:45.123 [http-nio-8080-exec-1] INFO [123e4567-e89b-12d3-a456-426614174000,abc123,def456] com.filetransfer.web.service.FileValidationService - File validation started
```

## 📋 Logging

### Structured Logging

#### Log Levels and Categories

```yaml
# Development logging
logging:
  level:
    com.filetransfer: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

# Production logging  
logging:
  level:
    com.filetransfer: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

#### Log Files

| Log File | Purpose | Retention |
|----------|---------|-----------|
| `file-transfer-web.log` | Application logs | 30 days |
| `file-transfer-web-json.log` | Structured JSON logs (prod) | 30 days |
| `audit.log` | Business audit events | 365 days |
| `performance.log` | Performance metrics | 30 days |
| `error.log` | Error-only logs | 90 days |

#### JSON Log Format (Production)

```json
{
  "timestamp": "2024-01-20T10:30:45.123Z",
  "level": "INFO",
  "message": "File transfer completed successfully",
  "application": "file-transfer-web",
  "environment": "production",
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "traceId": "abc123",
  "spanId": "def456",
  "tenantId": "tenant-001",
  "userId": "user-123",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.filetransfer.web.service.FileTransferService"
}
```

### ELK Stack Integration

#### Logstash Pipeline

```ruby
# Parse application logs
input {
  file {
    path => "/app/logs/*.log"
    start_position => "beginning"
  }
}

filter {
  if [message] =~ /^\{/ {
    json {
      source => "message"
    }
  } else {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} \[%{DATA:correlation}\] %{DATA:logger} - %{GREEDYDATA:msg}" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "file-transfer-logs-%{+YYYY.MM.dd}"
  }
}
```

## 🚨 Alerting

### Alert Rules

#### System Alerts

```yaml
# High memory usage
- alert: HighMemoryUsage
  expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) * 100 > 85
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High JVM memory usage"
    description: "Memory usage is {{ $value }}%"

# Application down
- alert: ApplicationDown
  expr: up{job="file-transfer-web"} == 0
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Application is down"
```

#### Business Alerts

```yaml
# High failure rate
- alert: HighFileTransferFailureRate
  expr: rate(file_transfer_failures_total[10m]) / rate(file_transfer_attempts_total[10m]) > 0.1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "High file transfer failure rate"
    description: "Failure rate is {{ $value | humanizePercentage }}"

# Cutoff time exceeded
- alert: CutoffTimeExceeded
  expr: time() > cutoff_time_next_seconds
  labels:
    severity: critical
  annotations:
    summary: "Cutoff time exceeded"
```

### Alert Notifications

#### Configuration

```yaml
# AlertManager configuration
route:
  group_by: ['alertname', 'tenant']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
  - match:
      severity: warning
    receiver: 'warning-alerts'

receivers:
- name: 'critical-alerts'
  email_configs:
  - to: 'ops-team@company.com'
    subject: 'CRITICAL: {{ .GroupLabels.alertname }}'
  slack_configs:
  - api_url: 'YOUR_SLACK_WEBHOOK'
    channel: '#critical-alerts'

- name: 'warning-alerts'
  email_configs:
  - to: 'dev-team@company.com'
    subject: 'WARNING: {{ .GroupLabels.alertname }}'
```

#### Custom Alert Generation

```java
// Generate custom business alerts
alertingService.generateAlert(
    "FILE_PROCESSING_BACKLOG", 
    AlertLevel.HIGH,
    "File processing backlog exceeded threshold: " + queueSize + " files",
    "business",
    tenantId
);

// Check alert history
List<AlertHistory> recentAlerts = alertingService.getRecentAlerts(tenantId, 10);

// Acknowledge alerts
alertingService.acknowledgeAlert(alertId, "user@company.com");
```

## 🎯 Performance Monitoring

### Key Performance Indicators (KPIs)

#### Business KPIs
- **File Transfer Success Rate**: Target > 99.5%
- **Average Processing Time**: Target < 30 seconds
- **Cutoff Compliance Rate**: Target > 98%
- **Data Volume Throughput**: GB/hour

#### Technical KPIs
- **Application Response Time**: Target < 200ms (95th percentile)
- **Database Query Time**: Target < 100ms (95th percentile)
- **Memory Usage**: Target < 80%
- **Error Rate**: Target < 0.1%

### Performance Optimization

#### Monitoring Query Performance

```java
// Database query timing
@Timed(value = "database.query", extraTags = {"operation", "findByTenantId"})
public List<FileTransferRecord> findByTenantId(String tenantId) {
    return repository.findByTenantId(tenantId);
}

// Cache hit rate monitoring
@Cacheable(value = "tenantCache")
@Timed(value = "cache.access", extraTags = {"cache", "tenant"})
public Tenant findTenant(String tenantId) {
    return tenantRepository.findById(tenantId);
}
```

#### Memory Leak Detection

```java
// Memory usage tracking
@EventListener
public void handleMemoryWarning(MemoryWarningEvent event) {
    alertingService.generateAlert(
        "MEMORY_WARNING",
        AlertLevel.WARNING,
        "Memory usage: " + event.getUsagePercentage() + "%",
        "system",
        null
    );
}
```

## 🔧 Configuration

### Application Properties

```yaml
# Monitoring configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    enable:
      jvm: true
      system: true
      web: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.9,0.95,0.99

# Tracing configuration
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for development
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

# Alert thresholds
monitoring:
  alerts:
    file-transfer-failure-rate: 0.1
    validation-failure-rate: 0.05
    response-time-ms: 5000
    memory-usage-percent: 85
```

### Environment Variables

```bash
# Monitoring endpoints
ZIPKIN_URL=http://zipkin:9411/api/v2/spans
PROMETHEUS_URL=http://prometheus:9090
GRAFANA_URL=http://grafana:3001

# Alert notification
SMTP_SERVER=smtp.company.com
SMTP_USERNAME=alerts@company.com
SLACK_WEBHOOK_URL=https://hooks.slack.com/...
PAGERDUTY_INTEGRATION_KEY=your-key-here

# Sampling rates
TRACING_SAMPLE_RATE=0.1  # 10% in production
```

## 📚 Troubleshooting

### Common Issues

#### 1. Metrics Not Appearing

**Problem**: Application metrics not showing in Prometheus
**Solution**: 
```bash
# Check actuator endpoint
curl http://localhost:8080/actuator/prometheus

# Verify Prometheus configuration
docker logs file-transfer-prometheus

# Check application logs
tail -f logs/file-transfer-web.log | grep -i metric
```

#### 2. High Memory Usage Alerts

**Problem**: Continuous memory warnings
**Solution**:
```bash
# Check JVM heap dump
curl http://localhost:8080/actuator/heapdump -o heapdump.hprof

# Analyze with Eclipse MAT or VisualVM
# Increase heap size if needed
JAVA_OPTS="-Xms2g -Xmx4g"
```

#### 3. Traces Not Appearing in Zipkin

**Problem**: Distributed traces not visible
**Solution**:
```bash
# Verify Zipkin connectivity
curl http://localhost:9411/health

# Check application trace configuration
curl http://localhost:8080/actuator/info

# Increase sampling rate temporarily
management.tracing.sampling.probability=1.0
```

### Performance Tuning

#### Database Query Optimization

```sql
-- Add indexes for monitoring queries
CREATE INDEX idx_file_transfer_tenant_status 
ON file_transfer_records(tenant_id, status, created_at);

CREATE INDEX idx_alert_history_tenant_time 
ON alert_history(tenant_id, generated_at DESC);
```

#### JVM Tuning

```bash
# Production JVM settings
JAVA_OPTS="
  -Xms2g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UseStringDeduplication
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/app/logs/
"
```

## 🎯 Best Practices

### 1. Metric Naming
- Use consistent naming conventions: `component.action.unit`
- Include relevant tags: `tenant`, `service_type`, `direction`
- Avoid high cardinality tags (user IDs, file names)

### 2. Alert Design
- Set appropriate thresholds based on SLAs
- Implement alert suppression to prevent spam
- Include actionable information in alert descriptions
- Use severity levels consistently

### 3. Log Management
- Use structured logging (JSON) in production
- Include correlation IDs for request tracing
- Separate audit logs from application logs
- Implement log rotation and retention policies

### 4. Dashboard Design
- Focus on business KPIs on overview dashboards
- Use appropriate time ranges for different metrics
- Include alerting status on dashboards
- Design for different audiences (business vs technical)

## 📋 Maintenance

### Daily Tasks
- Review alert dashboard for any issues
- Check system resource utilization
- Verify monitoring services are healthy

### Weekly Tasks
- Review and acknowledge old alerts
- Analyze performance trends
- Update alert thresholds if needed

### Monthly Tasks
- Review and optimize slow queries
- Clean up old logs and metrics data
- Update monitoring documentation
- Perform monitoring service updates

## 🔮 Advanced Features

### Custom Business Dashboards

```javascript
// Create tenant-specific dashboard
const tenantDashboard = {
  title: "Tenant Performance Dashboard",
  panels: [
    {
      title: "Transfer Success Rate",
      targets: [{
        expr: `rate(file_transfer_successes_total{tenant="${tenantId}"}[5m]) / rate(file_transfer_attempts_total{tenant="${tenantId}"}[5m])`
      }]
    }
  ]
};
```

### Predictive Alerting

```java
// Trend-based alerting
@Component
public class PredictiveAlerting {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkTrends() {
        // Analyze failure rate trends
        double currentFailureRate = getCurrentFailureRate();
        double trendSlope = calculateTrendSlope();
        
        if (trendSlope > 0.05) { // 5% increase trend
            alertingService.generateAlert(
                "INCREASING_FAILURE_TREND",
                AlertLevel.WARNING,
                "Failure rate is trending upward",
                "predictive",
                null
            );
        }
    }
}
```

---

## 📞 Support

For monitoring-related issues:
1. Check the troubleshooting section above
2. Review application and monitoring service logs
3. Consult the monitoring dashboard for system health
4. Contact the DevOps team for infrastructure issues

**Monitoring Stack Status**: ✅ Production Ready
**Last Updated**: January 2024