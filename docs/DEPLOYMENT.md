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
  database-url: "jdbc:mysql://mysql:3306/filetransfer"
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
  mysql-root-password: <base64-encoded-root-password>
```

### MySQL Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: file-transfer
spec:
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_DATABASE
          value: filetransfer
        - name: MYSQL_USER
          valueFrom:
            configMapKeyRef:
              name: file-transfer-config
              key: database-username
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: file-transfer-secret
              key: database-password
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: file-transfer-secret
              key: mysql-root-password
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-storage
        persistentVolumeClaim:
          claimName: mysql-pvc
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
- Use Kubernetes CronJobs or external backup tools to back up MySQL persistent volumes.

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
-- MySQL configuration
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB
SET GLOBAL innodb_log_file_size = 268435456;     -- 256MB
SET GLOBAL max_connections = 200;
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
   kubectl exec -it <mysql-pod> -n file-transfer -- mysql -u root -p
   
   # Check network
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
kubectl exec -it <mysql-pod> -n file-transfer -- mysqladmin ping -u root -p
```

### Log Analysis
```bash
# Follow application logs
kubectl logs -f <web-pod> -n file-transfer

# Search for errors
kubectl logs <batch-pod> -n file-transfer | grep ERROR
```