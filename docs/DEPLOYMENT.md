# Deployment Guide

This guide covers Kubernetes-based deployment for the File Transfer Management System.

## Kubernetes Deployment (Recommended)

### Prerequisites
- Kubernetes cluster 1.20+
- kubectl configured
- Persistent Volume support
- 4GB RAM minimum
- 2GB free disk space

### Quick Start
```bash
# Clone repository
 git clone <repository-url>
 cd file-transfer-system

# Deploy all components to Kubernetes
kubectl apply -f k8s/
```

### Monitoring
```bash
kubectl get pods -n file-transfer
kubectl get svc -n file-transfer
```

### Stopping the Application
```bash
kubectl delete -f k8s/
```

## Production Deployment

### Environment Configuration

Set environment variables in your Kubernetes manifests or use ConfigMaps/Secrets for sensitive data:

**`ConfigMap`**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: file-transfer-config
  namespace: file-transfer
data:
  database-url: "jdbc:sqlserver://your-sql-mi-server.database.windows.net:1433;database=filetransfer;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
  database-username: "filetransfer"
```

**`Secret`**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: file-transfer-secret
  namespace: file-transfer
type: Opaque
data:
  database-password: <base64-encoded-password>
  azure-sql-mi-password: <base64-encoded-password>
```

### Azure SQL Managed Instance Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: azure-sql-config
  namespace: file-transfer
data:
  AZURE_SQL_MI_SERVER: "your-sql-mi-server.database.windows.net"
  AZURE_SQL_MI_DATABASE: "filetransfer"
  AZURE_SQL_MI_USERNAME: "filetransfer"
  AZURE_SQL_MI_PORT: "1433"
---
apiVersion: v1
kind: Secret
metadata:
  name: azure-sql-secret
  namespace: file-transfer
type: Opaque
stringData:
  AZURE_SQL_MI_PASSWORD: "YourSecurePassword123!"
  AZURE_KEYVAULT_URI: "https://your-keyvault.vault.azure.net/"
  AZURE_TENANT_ID: "your-tenant-id"
  AZURE_CLIENT_ID: "your-client-id"
  AZURE_CLIENT_SECRET: "your-client-secret"
```

### Application Deployments
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: file-transfer-web
  namespace: file-transfer
spec:
  replicas: 2
  selector:
    matchLabels:
      app: file-transfer-web
  template:
    metadata:
      labels:
        app: file-transfer-web
    spec:
      containers:
      - name: file-transfer-web
        image: file-transfer-web:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: file-transfer-config
              key: database-url
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: file-transfer-secret
              key: database-password
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Ingress and Service
- Configure your ingress and service manifests as needed for your environment.

## Monitoring and Logging

### Prometheus Configuration
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'file-transfer-web'
    static_configs:
      - targets: ['file-transfer-web:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboard
Import dashboard configuration from `monitoring/grafana-dashboard.json`

### Log Aggregation
- Use your preferred log aggregation solution (e.g., EFK, Loki, etc.)

## Backup and Recovery

### Database Backup
- Use Azure SQL MI built-in backup and point-in-time restore capabilities
- Configure automated backups through Azure portal or Azure CLI
- Set up geo-replication for disaster recovery

## Enhanced Configuration

### Enhanced Cut-Off Time Configuration

The system supports enhanced cut-off time management with the following configuration options:

#### Environment Variables for Enhanced Features
```yaml
# Enhanced Cut-Off Time Configuration
ENHANCED_CUTOFF_ENABLED: "true"
DEFAULT_CUTOFF_TIME_TYPE: "WEEKDAY_WEEKEND"
SUNDAY_HOLIDAY_ENABLED: "true"

# Holiday Management
HOLIDAY_SERVICE_ENABLED: "true"
AUTO_CREATE_SUNDAY_HOLIDAYS: "true"
```

#### Database Migration
Run the enhanced migration script to add new columns:
```bash
# For existing installations using Azure SQL MI
# Using Azure CLI to run migration script
az sql mi db execute --resource-group your-rg --managed-instance your-sql-mi --database filetransfer --file-path scripts/migrate-enhanced-cutoff.sql

# Or using sqlcmd with Azure SQL MI
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -i scripts/migrate-enhanced-cutoff.sql
```

#### Schema Validation Configuration

The schema validation feature is optional and can be enabled per service configuration. To enable schema validation:

1. **Database Migration**: Run the schema validation migration script:
```bash
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -i scripts/migrate-schema-validation.sql
```

2. **Environment Variables**: Add schema validation configuration:
```yaml
# Schema Validation Settings
SCHEMA_VALIDATION_ENABLED: "true"
SCHEMA_VALIDATION_DEFAULT_MODE: "STRICT"  # STRICT, LENIENT, WARNING_ONLY
```

3. **Service Configuration**: Enable schema validation per service:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: service-config-schema-validation
  namespace: file-transfer
data:
  # Schema validation settings
  SCHEMA_VALIDATION_ENABLED: "true"
  SCHEMA_ID: "1"  # Reference to schema in database
  SCHEMA_VALIDATION_MODE: "STRICT"
```

#### Enhanced Service Configuration
```yaml
# ConfigMap for enhanced features
apiVersion: v1
kind: ConfigMap
metadata:
  name: enhanced-config
  namespace: file-transfer
data:
  # Cut-off time configuration
  CUTOFF_TIME_TYPE: "WEEKDAY_WEEKEND"
  WEEKDAY_CUTOFF_TIME: "18:00:00"
  WEEKEND_CUTOFF_TIME: "12:00:00"
  
  # Holiday configuration
  ALL_SUNDAYS_AS_HOLIDAYS: "true"
  HOLIDAY_SERVICE_ENABLED: "true"
  
  # Multi-tenancy
  MULTI_TENANT_ENABLED: "true"
  DEFAULT_TENANT_ID: "default"
```

### Multi-Tenancy Configuration

#### Tenant Management
```yaml
# Tenant configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: tenant-config
  namespace: file-transfer
data:
  TENANT_ISOLATION_ENABLED: "true"
  TENANT_DATABASE_PREFIX: "ft_"
  TENANT_SCHEMA_SEPARATION: "true"
```

#### Timezone Support
```yaml
# Timezone configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: timezone-config
  namespace: file-transfer
data:
  DEFAULT_TIMEZONE: "UTC"
  TIMEZONE_SUPPORT_ENABLED: "true"
  DATE_FORMAT: "yyyy-MM-dd"
  TIME_FORMAT: "HH:mm:ss"
```

### Azure Integration Configuration

#### Azure Key Vault Integration
```yaml
# Azure Key Vault configuration
apiVersion: v1
kind: Secret
metadata:
  name: azure-keyvault-config
  namespace: file-transfer
type: Opaque
stringData:
  AZURE_KEYVAULT_URI: "https://your-keyvault.vault.azure.net/"
  AZURE_TENANT_ID: "your-tenant-id"
  AZURE_CLIENT_ID: "your-client-id"
  AZURE_CLIENT_SECRET: "your-client-secret"
```

#### Azure SQL Managed Instance
```yaml
# Azure SQL configuration
apiVersion: v1
kind: Secret
metadata:
  name: azure-sql-config
  namespace: file-transfer
type: Opaque
stringData:
  AZURE_SQL_MI_SERVER: "your-sql-mi-server.database.windows.net"
  AZURE_SQL_MI_DATABASE: "filetransfer"
  AZURE_SQL_MI_USERNAME: "filetransfer"
  AZURE_SQL_MI_PASSWORD: "YourSecurePassword123!"
```

### Enhanced Monitoring Configuration

#### Prometheus Metrics
```yaml
# Enhanced monitoring
apiVersion: v1
kind: ConfigMap
metadata:
  name: monitoring-config
  namespace: file-transfer
data:
  PROMETHEUS_ENABLED: "true"
  METRICS_ENDPOINT: "/actuator/prometheus"
  TRACE_SAMPLE_RATE: "0.1"
  LOG_LEVEL: "INFO"
```

#### Distributed Tracing
```yaml
# Zipkin configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: tracing-config
  namespace: file-transfer
data:
  ZIPKIN_URL: "http://zipkin-service:9411"
  SLEUTH_SAMPLER_PROBABILITY: "0.1"
  TRACE_ID_128_BIT: "true"
```

## Security Considerations

### Network Security
- Use private networks for service communication
- Implement proper firewall rules
- Enable SSL/TLS for all external communications

### Application Security
- Enable Spring Security with proper authentication
- Use secrets management for passwords
- Implement rate limiting
- Regular security updates

### Database Security
- Use strong passwords
- Enable SSL for database connections
- Regular backup encryption
- Principle of least privilege for database users

## Performance Tuning

### JVM Tuning
Add to your deployment manifest:
```yaml
env:
  - name: JAVA_OPTS
    value: "-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Tuning
```sql
-- Azure SQL MI configuration
-- These settings are managed by Azure SQL MI
-- Connection pooling and performance are optimized automatically
-- Monitor performance using Azure SQL Analytics
```

### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Troubleshooting

### Common Issues

1. **Pod startup failures**
   ```bash
   # Check logs
   kubectl logs <pod-name> -n file-transfer
   
   # Check resource usage
   kubectl top pods -n file-transfer
   ```

2. **Database connection issues**
   ```bash
   # Test database connectivity
   sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword
   
   # Check Azure SQL MI status
   az sql mi show --name your-sql-mi --resource-group your-rg
   
   # Check network connectivity
   kubectl get svc -n file-transfer
   kubectl get pods -o wide -n file-transfer
   ```

3. **File permission issues**
   ```bash
   # Fix volume permissions
   kubectl exec -it <batch-pod> -n file-transfer -- chown -R app:app /app/data
   ```

### Health Checks
```bash
# Check application health
kubectl exec -it <web-pod> -n file-transfer -- curl http://localhost:8080/actuator/health
kubectl exec -it <batch-pod> -n file-transfer -- curl http://localhost:8081/actuator/health

# Check database
sqlcmd -S your-sql-mi-server.database.windows.net -d filetransfer -U filetransfer -P YourPassword -Q "SELECT 1"
```

### Log Analysis
```bash
# Follow application logs
kubectl logs -f <web-pod> -n file-transfer

# Search for errors
kubectl logs <batch-pod> -n file-transfer | grep ERROR
```