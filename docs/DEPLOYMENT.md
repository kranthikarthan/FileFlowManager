# Deployment Guide

This guide covers different deployment scenarios for the File Transfer Management System.

## Docker Compose Deployment (Recommended)

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum
- 2GB free disk space

### Quick Start
```bash
# Clone repository
git clone <repository-url>
cd file-transfer-system

# Start application
chmod +x scripts/start.sh
./scripts/start.sh
```

### Manual Docker Compose
```bash
# Build and start services
docker-compose up --build -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Stop services
docker-compose down
```

## Production Deployment

### Environment Configuration

Create production environment files:

**`.env.prod`**:
```env
# Database
MYSQL_ROOT_PASSWORD=secure_root_password
MYSQL_PASSWORD=secure_password
MYSQL_DATABASE=filetransfer_prod

# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
BATCH_SERVER_PORT=8081

# Security
JWT_SECRET=your_jwt_secret_here
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Production Docker Compose

**`docker-compose.prod.yml`**:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: filetransfer
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql_prod_data:/var/lib/mysql
      - ./backups:/backup
    restart: always
    command: --default-authentication-plugin=mysql_native_password

  file-transfer-batch:
    build: ./file-transfer-batch
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - prod_transfer_data:/app/data
      - prod_logs:/app/logs
    restart: always
    depends_on:
      - mysql

  file-transfer-web:
    build: ./file-transfer-web
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - prod_logs:/app/logs
    restart: always
    depends_on:
      - mysql

  file-transfer-frontend:
    build: 
      context: ./file-transfer-frontend
      args:
        REACT_APP_API_URL: https://api.yourdomain.com
    restart: always
    depends_on:
      - file-transfer-web

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/prod.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - file-transfer-frontend
    restart: always

volumes:
  mysql_prod_data:
  prod_transfer_data:
  prod_logs:
```

### SSL Configuration

**`nginx/prod.conf`**:
```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server file-transfer-web:8080;
    }

    server {
        listen 80;
        server_name yourdomain.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl;
        server_name yourdomain.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location / {
            proxy_pass http://file-transfer-frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## Kubernetes Deployment

### Prerequisites
- Kubernetes cluster 1.20+
- kubectl configured
- Persistent Volume support

### Namespace
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: file-transfer
```

### ConfigMap
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

### Secret
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

## Cloud Deployment

### AWS ECS with Fargate

**`task-definition.json`**:
```json
{
  "family": "file-transfer-system",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "file-transfer-web",
      "image": "your-ecr-repo/file-transfer-web:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/file-transfer",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Azure Container Instances

**`deploy-azure.sh`**:
```bash
#!/bin/bash

# Create resource group
az group create --name file-transfer-rg --location eastus

# Create container registry
az acr create --resource-group file-transfer-rg \
              --name filetransferregistry \
              --sku Basic

# Build and push images
az acr build --registry filetransferregistry \
             --image file-transfer-web:latest \
             ./file-transfer-web

# Deploy container group
az container create --resource-group file-transfer-rg \
                    --name file-transfer-system \
                    --image filetransferregistry.azurecr.io/file-transfer-web:latest \
                    --cpu 1 \
                    --memory 2 \
                    --ports 8080 \
                    --environment-variables SPRING_PROFILES_ACTIVE=azure
```

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
```yaml
# Filebeat configuration
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
    - add_docker_metadata: ~

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
```

## Backup and Recovery

### Database Backup
```bash
#!/bin/bash
# backup-db.sh

CONTAINER_NAME="file-transfer-mysql"
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec $CONTAINER_NAME mysqldump \
  -u root -p$MYSQL_ROOT_PASSWORD \
  --all-databases > $BACKUP_DIR/backup_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/backup_$DATE.sql

# Keep only last 7 days of backups
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

### Data Volume Backup
```bash
#!/bin/bash
# backup-volumes.sh

docker run --rm \
  -v file-transfer_mysql_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/mysql_data_$(date +%Y%m%d).tar.gz -C /data .

docker run --rm \
  -v file-transfer_file_transfer_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/transfer_data_$(date +%Y%m%d).tar.gz -C /data .
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
```dockerfile
# Add to Dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
CMD ["java", $JAVA_OPTS, "-jar", "app.jar"]
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

1. **Container startup failures**
   ```bash
   # Check logs
   docker-compose logs [service-name]
   
   # Check resource usage
   docker stats
   ```

2. **Database connection issues**
   ```bash
   # Test database connectivity
   docker exec -it mysql mysql -u root -p
   
   # Check network
   docker network ls
   docker network inspect bridge
   ```

3. **File permission issues**
   ```bash
   # Fix volume permissions
   docker-compose exec file-transfer-batch chown -R app:app /app/data
   ```

### Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Check database
docker exec mysql mysqladmin ping -u root -p
```

### Log Analysis
```bash
# Follow application logs
docker-compose logs -f file-transfer-web

# Search for errors
docker-compose logs file-transfer-batch | grep ERROR

# Check specific time range
docker-compose logs --since="2023-12-07T10:00:00" file-transfer-web
```