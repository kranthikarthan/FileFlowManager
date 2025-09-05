# File Transfer System - Deployment and Infrastructure Documentation

## 1. Deployment Overview

### 1.1 Deployment Strategy
The File Transfer Management System supports multiple deployment patterns:
- **Development**: Docker Compose for local development
- **Testing**: Kubernetes with test data and mocked services
- **Staging**: Production-like Kubernetes environment
- **Production**: High-availability Kubernetes cluster with Azure integration

### 1.2 Infrastructure Requirements

#### Minimum Requirements (Development)
- **CPU**: 4 cores
- **Memory**: 8GB RAM
- **Storage**: 50GB SSD
- **Network**: 100Mbps
- **OS**: Linux/Windows/macOS with Docker support

#### Production Requirements
- **CPU**: 16+ cores (distributed across nodes)
- **Memory**: 32GB+ RAM (distributed across nodes)
- **Storage**: 500GB+ SSD with backup
- **Network**: 1Gbps+ with redundancy
- **Database**: Azure SQL Managed Instance or equivalent
- **File Storage**: Azure Blob Storage or equivalent
- **Monitoring**: Prometheus, Grafana, ELK Stack

## 2. Container Architecture

### 2.1 Docker Images

#### Frontend Application Image
```dockerfile
# file-transfer-frontend/Dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=builder /app/build .
COPY nginx.conf /etc/nginx/nginx.conf

# Security configurations
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup && \
    chown -R appuser:appgroup /usr/share/nginx/html

USER appuser
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

#### Web Application Image
```dockerfile
# file-transfer-web/Dockerfile
FROM maven:3.9-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app

# Install required tools
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create application user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser

# Create directories
RUN mkdir -p /app/logs /app/config /data && \
    chown -R appuser:appgroup /app /data

COPY --from=builder /app/target/file-transfer-web-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080 8090

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Batch Application Image
```dockerfile
# file-transfer-batch/Dockerfile
FROM maven:3.9-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app

# Install file processing tools
RUN apt-get update && \
    apt-get install -y curl rsync && \
    rm -rf /var/lib/apt/lists/*

# Create application user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser

# Create required directories
RUN mkdir -p /app/logs /app/heapdumps /data/incoming /data/processed /data/error /data/ack-nack && \
    chown -R appuser:appgroup /app /data

COPY --from=builder /app/target/file-transfer-batch-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081 8091

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 2.2 Docker Compose for Development

#### Complete Development Stack
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Database
  database:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: filetransfer
      MYSQL_USER: filetransfer
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Message Queue
  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: filetransfer
      RABBITMQ_DEFAULT_PASS: password
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Web Application
  web-app:
    build:
      context: ./file-transfer-web
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker,ack-nack
      SPRING_DATASOURCE_URL: jdbc:mysql://database:3306/filetransfer
      SPRING_DATASOURCE_USERNAME: filetransfer
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      ACK_NACK_ENABLED: true
      ACK_NACK_BASE_PATH: /data/ack-nack
    volumes:
      - file_data:/data
      - ./logs:/app/logs
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Batch Application
  batch-app:
    build:
      context: ./file-transfer-batch
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker,ack-nack
      SPRING_DATASOURCE_URL: jdbc:mysql://database:3306/filetransfer
      SPRING_DATASOURCE_USERNAME: filetransfer
      SPRING_DATASOURCE_PASSWORD: password
      FILE_TRANSFER_ENABLED: true
      ACK_NACK_ENABLED: true
      ACK_NACK_BASE_PATH: /data/ack-nack
      ACK_NACK_INCOMING_PATH: /data/incoming/ack-nack
    volumes:
      - file_data:/data
      - ./logs:/app/logs
    depends_on:
      database:
        condition: service_healthy
      web-app:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Frontend Application
  frontend:
    build:
      context: ./file-transfer-frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    environment:
      REACT_APP_API_BASE_URL: http://localhost:8080
    depends_on:
      - web-app
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:
  file_data:

networks:
  default:
    driver: bridge
```

## 3. Kubernetes Deployment

### 3.1 Namespace and RBAC Configuration

#### Namespace Setup
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: file-transfer
  labels:
    name: file-transfer
    environment: production
    istio-injection: enabled

---
# Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: file-transfer-sa
  namespace: file-transfer

---
# RBAC Configuration
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: file-transfer
  name: file-transfer-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: file-transfer-rolebinding
  namespace: file-transfer
subjects:
- kind: ServiceAccount
  name: file-transfer-sa
  namespace: file-transfer
roleRef:
  kind: Role
  name: file-transfer-role
  apiGroup: rbac.authorization.k8s.io
```

### 3.2 Application Deployments

#### Web Application Deployment
```yaml
# k8s/web-app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: file-transfer-web
  namespace: file-transfer
  labels:
    app: file-transfer-web
    component: backend
    version: v1.0.0
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: file-transfer-web
  template:
    metadata:
      labels:
        app: file-transfer-web
        component: backend
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8090"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: file-transfer-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        fsGroup: 1001
      containers:
      - name: web-app
        image: filetransfer.azurecr.io/file-transfer-web:latest
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8090
          name: actuator
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes,production,ack-nack"
        - name: AZURE_SQL_MI_SERVER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: server
        - name: AZURE_SQL_MI_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: username
        - name: AZURE_SQL_MI_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: password
        - name: ACK_NACK_BASE_PATH
          value: "/data/ack-nack"
        - name: ACK_NACK_INCOMING_PATH
          value: "/data/incoming/ack-nack"
        volumeMounts:
        - name: file-storage
          mountPath: /data
        - name: logs
          mountPath: /app/logs
        - name: config
          mountPath: /app/config
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8090
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8090
          initialDelaySeconds: 30
          periodSeconds: 15
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8090
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 10
      volumes:
      - name: file-storage
        persistentVolumeClaim:
          claimName: file-storage-pvc
      - name: logs
        emptyDir: {}
      - name: config
        configMap:
          name: file-transfer-config

---
# Service for Web Application
apiVersion: v1
kind: Service
metadata:
  name: file-transfer-web-service
  namespace: file-transfer
  labels:
    app: file-transfer-web
spec:
  selector:
    app: file-transfer-web
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: actuator
    port: 8090
    targetPort: 8090
  type: ClusterIP

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: file-transfer-web-hpa
  namespace: file-transfer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: file-transfer-web
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

#### Batch Application Deployment
```yaml
# k8s/batch-app-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: file-transfer-batch
  namespace: file-transfer
  labels:
    app: file-transfer-batch
    component: batch-processor
spec:
  serviceName: file-transfer-batch
  replicas: 1
  selector:
    matchLabels:
      app: file-transfer-batch
  template:
    metadata:
      labels:
        app: file-transfer-batch
        component: batch-processor
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8091"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: file-transfer-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        fsGroup: 1001
      containers:
      - name: batch-app
        image: filetransfer.azurecr.io/file-transfer-batch:latest
        ports:
        - containerPort: 8081
          name: http
        - containerPort: 8091
          name: actuator
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes,production,ack-nack"
        - name: AZURE_SQL_MI_SERVER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: server
        - name: FILE_TRANSFER_ENABLED
          value: "true"
        - name: ACK_NACK_ENABLED
          value: "true"
        - name: ACK_NACK_BASE_PATH
          value: "/data/ack-nack"
        - name: ACK_NACK_INCOMING_PATH
          value: "/data/incoming/ack-nack"
        volumeMounts:
        - name: file-storage
          mountPath: /data
        - name: logs
          mountPath: /app/logs
        - name: config
          mountPath: /app/config
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8091
          initialDelaySeconds: 120
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8091
          initialDelaySeconds: 60
          periodSeconds: 15
          timeoutSeconds: 5
          failureThreshold: 3
      volumes:
      - name: file-storage
        persistentVolumeClaim:
          claimName: file-storage-pvc
      - name: logs
        emptyDir: {}
      - name: config
        configMap:
          name: file-transfer-config
  volumeClaimTemplates:
  - metadata:
      name: file-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "managed-premium"
      resources:
        requests:
          storage: 100Gi
```

### 3.3 Configuration Management

#### ConfigMap Configuration
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: file-transfer-config
  namespace: file-transfer
data:
  # Application configuration
  application.yml: |
    spring:
      profiles:
        active: kubernetes,production,ack-nack
      datasource:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 20000
      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false
      
    file-transfer:
      enabled: true
      poll-interval-seconds: 30
      ack-nack:
        enabled: true
        auto-generate: true
        timeout-hours: 24
        poll-interval-seconds: 60
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
      metrics:
        export:
          prometheus:
            enabled: true
    
    logging:
      level:
        com.filetransfer: INFO
        org.springframework.batch: INFO
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"

  # Logback configuration
  logback-spring.xml: |
    <configuration>
      <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
      
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
          <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <message/>
            <mdc/>
            <pattern>
              <pattern>
                {
                  "service": "file-transfer-web",
                  "correlationId": "%X{correlationId:-}",
                  "tenantId": "%X{tenantId:-}"
                }
              </pattern>
            </pattern>
          </providers>
        </encoder>
      </appender>
      
      <root level="INFO">
        <appender-ref ref="STDOUT"/>
      </root>
    </configuration>

---
# Secrets
apiVersion: v1
kind: Secret
metadata:
  name: database-secret
  namespace: file-transfer
type: Opaque
data:
  server: <base64-encoded-server>
  username: <base64-encoded-username>
  password: <base64-encoded-password>
  
---
apiVersion: v1
kind: Secret
metadata:
  name: azure-storage-secret
  namespace: file-transfer
type: Opaque
data:
  connection-string: <base64-encoded-connection-string>
  container-name: <base64-encoded-container-name>
```

### 3.4 Persistent Storage

#### Persistent Volume Configuration
```yaml
# k8s/persistent-volumes.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: file-transfer-pv
  labels:
    app: file-transfer
spec:
  capacity:
    storage: 500Gi
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: azure-file-premium
  azureFile:
    secretName: azure-storage-secret
    shareName: file-transfer-share
    readOnly: false

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: file-storage-pvc
  namespace: file-transfer
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 500Gi
  storageClassName: azure-file-premium

---
# Azure Blob Storage for long-term archival
apiVersion: v1
kind: Secret
metadata:
  name: azure-blob-secret
  namespace: file-transfer
type: Opaque
data:
  account-name: <base64-encoded-account-name>
  account-key: <base64-encoded-account-key>
```

## 4. Ingress and Load Balancing

### 4.1 Ingress Configuration

#### NGINX Ingress Controller
```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: file-transfer-ingress
  namespace: file-transfer
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/client-max-body-size: "100m"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - filetransfer.company.com
    - api.filetransfer.company.com
    secretName: file-transfer-tls
  rules:
  - host: filetransfer.company.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: file-transfer-frontend-service
            port:
              number: 80
  - host: api.filetransfer.company.com
    http:
      paths:
      - path: /api/v1/transfers
        pathType: Prefix
        backend:
          service:
            name: file-transfer-web-service
            port:
              number: 8080
      - path: /api/v1/ack-nack
        pathType: Prefix
        backend:
          service:
            name: file-transfer-web-service
            port:
              number: 8080
      - path: /api/v1/batch
        pathType: Prefix
        backend:
          service:
            name: file-transfer-batch-service
            port:
              number: 8081

---
# Frontend Service
apiVersion: v1
kind: Service
metadata:
  name: file-transfer-frontend-service
  namespace: file-transfer
spec:
  selector:
    app: file-transfer-frontend
  ports:
  - port: 80
    targetPort: 80
  type: ClusterIP
```

### 4.2 Load Balancing Configuration

#### Service Load Balancing
```yaml
# k8s/load-balancer.yaml
apiVersion: v1
kind: Service
metadata:
  name: file-transfer-web-lb
  namespace: file-transfer
  annotations:
    service.beta.kubernetes.io/azure-load-balancer-internal: "true"
    service.beta.kubernetes.io/azure-load-balancer-internal-subnet: "internal-subnet"
spec:
  type: LoadBalancer
  selector:
    app: file-transfer-web
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800
```

## 5. Monitoring and Observability

### 5.1 Prometheus Monitoring

#### Prometheus Configuration
```yaml
# k8s/monitoring/prometheus.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: file-transfer
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    rule_files:
      - "/etc/prometheus/rules/*.yml"
    
    scrape_configs:
    - job_name: 'kubernetes-apiservers'
      kubernetes_sd_configs:
      - role: endpoints
      scheme: https
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      relabel_configs:
      - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: default;kubernetes;https
    
    - job_name: 'file-transfer-applications'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - file-transfer
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: file-transfer
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - name: prometheus
        image: prom/prometheus:latest
        ports:
        - containerPort: 9090
        volumeMounts:
        - name: config
          mountPath: /etc/prometheus
        - name: storage
          mountPath: /prometheus
        args:
        - '--config.file=/etc/prometheus/prometheus.yml'
        - '--storage.tsdb.path=/prometheus'
        - '--web.console.libraries=/etc/prometheus/console_libraries'
        - '--web.console.templates=/etc/prometheus/consoles'
        - '--storage.tsdb.retention.time=15d'
        - '--web.enable-lifecycle'
      volumes:
      - name: config
        configMap:
          name: prometheus-config
      - name: storage
        persistentVolumeClaim:
          claimName: prometheus-storage-pvc
```

#### Grafana Dashboard Configuration
```yaml
# k8s/monitoring/grafana.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
  namespace: file-transfer
data:
  file-transfer-dashboard.json: |
    {
      "dashboard": {
        "title": "File Transfer System",
        "panels": [
          {
            "title": "File Processing Rate",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(batch_files_processed_total[5m])",
                "legendFormat": "Files/sec"
              }
            ]
          },
          {
            "title": "ACK/NACK Statistics",
            "type": "stat",
            "targets": [
              {
                "expr": "sum(batch_ack_nack_generated_total) by (type)",
                "legendFormat": "{{type}}"
              }
            ]
          },
          {
            "title": "System Performance",
            "type": "graph",
            "targets": [
              {
                "expr": "process_cpu_usage",
                "legendFormat": "CPU Usage"
              },
              {
                "expr": "jvm_memory_used_bytes / jvm_memory_max_bytes",
                "legendFormat": "Memory Usage"
              }
            ]
          }
        ]
      }
    }
```

### 5.2 Logging Configuration

#### ELK Stack Deployment
```yaml
# k8s/monitoring/elasticsearch.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: elasticsearch
  namespace: file-transfer
spec:
  serviceName: elasticsearch
  replicas: 3
  selector:
    matchLabels:
      app: elasticsearch
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      containers:
      - name: elasticsearch
        image: docker.elastic.co/elasticsearch/elasticsearch:8.5.0
        ports:
        - containerPort: 9200
        - containerPort: 9300
        env:
        - name: cluster.name
          value: "file-transfer-logs"
        - name: node.name
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: discovery.seed_hosts
          value: "elasticsearch-0.elasticsearch,elasticsearch-1.elasticsearch,elasticsearch-2.elasticsearch"
        - name: cluster.initial_master_nodes
          value: "elasticsearch-0,elasticsearch-1,elasticsearch-2"
        - name: ES_JAVA_OPTS
          value: "-Xms1g -Xmx1g"
        volumeMounts:
        - name: data
          mountPath: /usr/share/elasticsearch/data
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "1000m"
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "managed-premium"
      resources:
        requests:
          storage: 50Gi

---
# Logstash Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: file-transfer
data:
  logstash.conf: |
    input {
      beats {
        port => 5044
      }
    }
    
    filter {
      if [fields][service] {
        mutate {
          add_field => { "service_name" => "%{[fields][service]}" }
        }
      }
      
      if [message] =~ /^\{.*\}$/ {
        json {
          source => "message"
        }
      }
      
      date {
        match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
      }
      
      # Extract correlation ID
      if [correlationId] {
        mutate {
          add_field => { "correlation_id" => "%{correlationId}" }
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

## 6. Azure Cloud Integration

### 6.1 Azure SQL Managed Instance

#### Database Connection Configuration
```yaml
# Azure SQL MI connection with managed identity
apiVersion: v1
kind: Secret
metadata:
  name: azure-sql-secret
  namespace: file-transfer
type: Opaque
data:
  connection-string: <base64-encoded-connection-string>

---
# Pod Identity for Azure integration
apiVersion: aadpodidentity.k8s.io/v1
kind: AzureIdentity
metadata:
  name: file-transfer-identity
  namespace: file-transfer
spec:
  type: 0
  resourceID: /subscriptions/<subscription-id>/resourcegroups/<rg-name>/providers/Microsoft.ManagedIdentity/userAssignedIdentities/file-transfer-identity
  clientID: <client-id>

---
apiVersion: aadpodidentity.k8s.io/v1
kind: AzureIdentityBinding
metadata:
  name: file-transfer-identity-binding
  namespace: file-transfer
spec:
  azureIdentity: file-transfer-identity
  selector: file-transfer-pod
```

#### Database Migration Job
```yaml
# k8s/jobs/database-migration.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: database-migration
  namespace: file-transfer
spec:
  template:
    metadata:
      labels:
        app: database-migration
    spec:
      restartPolicy: Never
      containers:
      - name: migration
        image: flyway/flyway:8
        env:
        - name: FLYWAY_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: FLYWAY_USER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: username
        - name: FLYWAY_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: password
        - name: FLYWAY_LOCATIONS
          value: "filesystem:/flyway/sql"
        volumeMounts:
        - name: migration-scripts
          mountPath: /flyway/sql
        command: ["flyway", "migrate"]
      volumes:
      - name: migration-scripts
        configMap:
          name: database-migration-scripts
```

### 6.2 Azure Blob Storage Integration

#### Blob Storage Configuration
```java
@Configuration
public class AzureBlobConfig {
    
    @Value("${azure.storage.connection-string}")
    private String connectionString;
    
    @Value("${azure.storage.container-name}")
    private String containerName;
    
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
    
    @Bean
    public BlobContainerClient blobContainerClient() {
        return blobServiceClient().getBlobContainerClient(containerName);
    }
}

@Service
public class AzureBlobStorageService {
    
    @Autowired
    private BlobContainerClient blobContainerClient;
    
    public void uploadFile(String blobName, InputStream data, long length) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.upload(data, length, true);
        
        // Set metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("uploadedAt", Instant.now().toString());
        metadata.put("service", "file-transfer-system");
        blobClient.setMetadata(metadata);
    }
    
    public void downloadFile(String blobName, OutputStream outputStream) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.download(outputStream);
    }
    
    public void archiveFile(String sourcePath, String archivePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(sourcePath))) {
            uploadFile(archivePath, inputStream, Files.size(Paths.get(sourcePath)));
            
            // Delete local file after successful upload
            Files.delete(Paths.get(sourcePath));
            
            logger.info("Archived file to blob storage: {} -> {}", sourcePath, archivePath);
        } catch (IOException e) {
            logger.error("Failed to archive file {}: {}", sourcePath, e.getMessage());
        }
    }
}
```

## 7. CI/CD Pipeline

### 7.1 GitHub Actions Workflow

#### Multi-Service Build and Deploy
```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: filetransfer.azurecr.io
  AKS_CLUSTER: file-transfer-aks
  AKS_RESOURCE_GROUP: file-transfer-rg

jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      web: ${{ steps.changes.outputs.web }}
      batch: ${{ steps.changes.outputs.batch }}
      frontend: ${{ steps.changes.outputs.frontend }}
    steps:
    - uses: actions/checkout@v3
    - uses: dorny/paths-filter@v2
      id: changes
      with:
        filters: |
          web:
            - 'file-transfer-web/**'
          batch:
            - 'file-transfer-batch/**'
          frontend:
            - 'file-transfer-frontend/**'

  build-web:
    needs: changes
    if: ${{ needs.changes.outputs.web == 'true' }}
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: |
        cd file-transfer-web
        mvn test
    
    - name: Build application
      run: |
        cd file-transfer-web
        mvn clean package -DskipTests
    
    - name: Login to Azure Container Registry
      uses: azure/docker-login@v1
      with:
        login-server: ${{ env.REGISTRY }}
        username: ${{ secrets.REGISTRY_USERNAME }}
        password: ${{ secrets.REGISTRY_PASSWORD }}
    
    - name: Build and push Docker image
      run: |
        cd file-transfer-web
        docker build -t ${{ env.REGISTRY }}/file-transfer-web:${{ github.sha }} .
        docker push ${{ env.REGISTRY }}/file-transfer-web:${{ github.sha }}

  build-batch:
    needs: changes
    if: ${{ needs.changes.outputs.batch == 'true' }}
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build and test
      run: |
        cd file-transfer-batch
        mvn clean test package -DskipTests
    
    - name: Build and push Docker image
      run: |
        cd file-transfer-batch
        docker build -t ${{ env.REGISTRY }}/file-transfer-batch:${{ github.sha }} .
        docker push ${{ env.REGISTRY }}/file-transfer-batch:${{ github.sha }}

  build-frontend:
    needs: changes
    if: ${{ needs.changes.outputs.frontend == 'true' }}
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: file-transfer-frontend/package-lock.json
    
    - name: Install dependencies
      run: |
        cd file-transfer-frontend
        npm ci
    
    - name: Run tests
      run: |
        cd file-transfer-frontend
        npm test -- --coverage --watchAll=false
    
    - name: Build application
      run: |
        cd file-transfer-frontend
        npm run build
    
    - name: Build and push Docker image
      run: |
        cd file-transfer-frontend
        docker build -t ${{ env.REGISTRY }}/file-transfer-frontend:${{ github.sha }} .
        docker push ${{ env.REGISTRY }}/file-transfer-frontend:${{ github.sha }}

  deploy:
    needs: [changes, build-web, build-batch, build-frontend]
    if: always() && (needs.build-web.result == 'success' || needs.build-batch.result == 'success' || needs.build-frontend.result == 'success')
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Azure Login
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}
    
    - name: Get AKS credentials
      run: |
        az aks get-credentials --resource-group ${{ env.AKS_RESOURCE_GROUP }} --name ${{ env.AKS_CLUSTER }}
    
    - name: Deploy with Helm
      run: |
        helm upgrade --install file-transfer ./helm/file-transfer \
          --namespace file-transfer \
          --create-namespace \
          --set global.imageTag=${{ github.sha }} \
          --set global.environment=production \
          --values ./helm/file-transfer/values-production.yaml \
          --wait --timeout=10m
```

## 8. Infrastructure as Code

### 8.1 Terraform Configuration

#### Azure Infrastructure Setup
```hcl
# terraform/main.tf
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~>3.0"
    }
  }
  
  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "terraformstateaccount"
    container_name       = "tfstate"
    key                  = "file-transfer.terraform.tfstate"
  }
}

provider "azurerm" {
  features {}
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
  
  tags = {
    Environment = var.environment
    Project     = "file-transfer"
    ManagedBy   = "terraform"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "main" {
  name                = "${var.prefix}-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  tags = azurerm_resource_group.main.tags
}

# Subnet for AKS
resource "azurerm_subnet" "aks" {
  name                 = "${var.prefix}-aks-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.1.0/24"]
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "main" {
  name                = "${var.prefix}-aks"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = "${var.prefix}-aks"
  
  default_node_pool {
    name                = "default"
    node_count          = var.node_count
    vm_size            = var.vm_size
    vnet_subnet_id     = azurerm_subnet.aks.id
    enable_auto_scaling = true
    min_count          = 2
    max_count          = 10
    
    upgrade_settings {
      max_surge = "33%"
    }
  }
  
  identity {
    type = "SystemAssigned"
  }
  
  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
  }
  
  monitor_metrics {
    annotations_allowed = "prometheus.io/scrape,prometheus.io/port,prometheus.io/path"
    labels_allowed     = "app,component,version"
  }
  
  tags = azurerm_resource_group.main.tags
}

# Azure SQL Managed Instance
resource "azurerm_mssql_managed_instance" "main" {
  name                = "${var.prefix}-sqlmi"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  
  administrator_login          = var.sql_admin_username
  administrator_login_password = var.sql_admin_password
  license_type                = "BasePrice"
  sku_name                    = var.sql_sku_name
  storage_size_in_gb          = var.sql_storage_size
  subnet_id                   = azurerm_subnet.sql.id
  vcores                      = var.sql_vcores
  
  tags = azurerm_resource_group.main.tags
}

# Storage Account for file storage
resource "azurerm_storage_account" "main" {
  name                     = "${var.prefix}storage"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  
  blob_properties {
    versioning_enabled = true
    
    delete_retention_policy {
      days = 30
    }
    
    container_delete_retention_policy {
      days = 30
    }
  }
  
  tags = azurerm_resource_group.main.tags
}

# Container Registry
resource "azurerm_container_registry" "main" {
  name                = "${var.prefix}acr"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Premium"
  admin_enabled       = false
  
  georeplications {
    location                = "East US 2"
    zone_redundancy_enabled = true
  }
  
  tags = azurerm_resource_group.main.tags
}

# Key Vault for secrets
resource "azurerm_key_vault" "main" {
  name                = "${var.prefix}-kv"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "premium"
  
  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = azurerm_kubernetes_cluster.main.identity[0].principal_id
    
    secret_permissions = [
      "Get", "List"
    ]
  }
  
  tags = azurerm_resource_group.main.tags
}
```

### 8.2 Helm Chart Configuration

#### Main Helm Chart Structure
```yaml
# helm/file-transfer/Chart.yaml
apiVersion: v2
name: file-transfer
description: File Transfer Management System
type: application
version: 1.0.0
appVersion: "1.0.0"

dependencies:
  - name: postgresql
    version: 11.9.13
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: 17.3.7
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
  - name: prometheus
    version: 15.5.3
    repository: https://prometheus-community.github.io/helm-charts
    condition: monitoring.prometheus.enabled
  - name: grafana
    version: 6.50.7
    repository: https://grafana.github.io/helm-charts
    condition: monitoring.grafana.enabled

# helm/file-transfer/values.yaml
global:
  imageRegistry: filetransfer.azurecr.io
  imageTag: latest
  environment: development
  
  # Storage configuration
  storage:
    className: managed-premium
    size: 100Gi
    
  # Security configuration
  security:
    enabled: true
    rbac:
      create: true
    podSecurityPolicy:
      enabled: true

# Application configurations
frontend:
  enabled: true
  replicaCount: 2
  image:
    repository: file-transfer-frontend
    tag: ""
    pullPolicy: IfNotPresent
  
  service:
    type: ClusterIP
    port: 80
  
  ingress:
    enabled: true
    className: nginx
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: /
    hosts:
      - host: filetransfer.local
        paths:
          - path: /
            pathType: Prefix
  
  resources:
    requests:
      memory: "128Mi"
      cpu: "100m"
    limits:
      memory: "256Mi"
      cpu: "200m"

webApp:
  enabled: true
  replicaCount: 2
  image:
    repository: file-transfer-web
    tag: ""
    pullPolicy: IfNotPresent
  
  service:
    type: ClusterIP
    port: 8080
  
  env:
    springProfilesActive: "kubernetes,production,ack-nack"
    ackNackEnabled: true
    ackNackBasePath: "/data/ack-nack"
  
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"
  
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70

batchApp:
  enabled: true
  replicaCount: 1
  image:
    repository: file-transfer-batch
    tag: ""
    pullPolicy: IfNotPresent
  
  service:
    type: ClusterIP
    port: 8081
  
  env:
    springProfilesActive: "kubernetes,production,ack-nack"
    fileTransferEnabled: true
    ackNackEnabled: true
  
  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "4Gi"
      cpu: "2000m"
  
  persistence:
    enabled: true
    size: 100Gi
    storageClass: managed-premium

# Database configuration
database:
  enabled: false  # Use external Azure SQL MI
  external:
    host: file-transfer-sqlmi.database.windows.net
    port: 1433
    database: filetransfer
    existingSecret: database-secret

# Monitoring configuration
monitoring:
  prometheus:
    enabled: true
    retention: 15d
    storage: 50Gi
  
  grafana:
    enabled: true
    adminPassword: ""
    persistence:
      enabled: true
      size: 10Gi
  
  alerts:
    enabled: true
    webhookUrl: ""
```

## 9. Environment-Specific Configurations

### 9.1 Development Environment

#### Local Development with Skaffold
```yaml
# skaffold.yaml
apiVersion: skaffold/v3
kind: Config
metadata:
  name: file-transfer-dev

build:
  artifacts:
  - image: file-transfer-web
    context: file-transfer-web
    docker:
      dockerfile: Dockerfile
    sync:
      manual:
      - src: "src/**/*.java"
        dest: "/app/src"
  - image: file-transfer-batch
    context: file-transfer-batch
    docker:
      dockerfile: Dockerfile
  - image: file-transfer-frontend
    context: file-transfer-frontend
    docker:
      dockerfile: Dockerfile
    sync:
      manual:
      - src: "src/**/*.js"
        dest: "/app/src"

deploy:
  helm:
    releases:
    - name: file-transfer-dev
      chartPath: helm/file-transfer
      valuesFiles:
      - helm/file-transfer/values-development.yaml
      setValues:
        global.imageTag: "{{.IMAGE_TAG}}"
        global.environment: "development"

portForward:
- resourceType: service
  resourceName: file-transfer-web-service
  port: 8080
- resourceType: service
  resourceName: file-transfer-frontend-service
  port: 3000
  localPort: 3000
```

### 9.2 Production Environment

#### Production Values Configuration
```yaml
# helm/file-transfer/values-production.yaml
global:
  imageRegistry: filetransfer.azurecr.io
  imageTag: "1.0.0"
  environment: production
  
  # High availability configuration
  replicaCount: 3
  
  # Resource limits for production
  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "4Gi"
      cpu: "2000m"

# Production database configuration
database:
  external:
    host: file-transfer-prod-sqlmi.database.windows.net
    port: 1433
    database: filetransfer_prod
    ssl: true
    connectionTimeout: 30000

# Production storage configuration
storage:
  className: managed-premium
  size: 1Ti
  backup:
    enabled: true
    schedule: "0 2 * * *"
    retention: "30d"

# Production monitoring
monitoring:
  prometheus:
    enabled: true
    retention: 90d
    storage: 200Gi
    alertmanager:
      enabled: true
      webhookUrl: "https://alerts.company.com/webhook"
  
  grafana:
    enabled: true
    persistence:
      enabled: true
      size: 50Gi
    plugins:
      - grafana-azure-monitor-datasource
      - grafana-piechart-panel

# Security configuration
security:
  networkPolicies:
    enabled: true
  podSecurityPolicy:
    enabled: true
  serviceAccount:
    create: true
    annotations:
      azure.workload.identity/client-id: "12345678-1234-1234-1234-123456789012"

# Ingress configuration
ingress:
  enabled: true
  className: nginx
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  hosts:
    - host: filetransfer.company.com
      paths:
        - path: /
          pathType: Prefix
          service: file-transfer-frontend-service
          port: 80
    - host: api.filetransfer.company.com
      paths:
        - path: /api
          pathType: Prefix
          service: file-transfer-web-service
          port: 8080
  tls:
    - secretName: file-transfer-tls
      hosts:
        - filetransfer.company.com
        - api.filetransfer.company.com
```

## 10. Backup and Disaster Recovery

### 10.1 Backup Strategy

#### Automated Backup Configuration
```yaml
# k8s/backup/backup-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: database-backup
  namespace: file-transfer
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
          - name: backup
            image: mcr.microsoft.com/mssql-tools
            env:
            - name: SQL_SERVER
              valueFrom:
                secretKeyRef:
                  name: database-secret
                  key: server
            - name: SQL_USERNAME
              valueFrom:
                secretKeyRef:
                  name: database-secret
                  key: username
            - name: SQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: database-secret
                  key: password
            - name: BACKUP_STORAGE_ACCOUNT
              valueFrom:
                secretKeyRef:
                  name: backup-secret
                  key: storage-account
            command:
            - /bin/bash
            - -c
            - |
              # Create database backup
              sqlcmd -S $SQL_SERVER -U $SQL_USERNAME -P $SQL_PASSWORD -Q "
              BACKUP DATABASE filetransfer 
              TO URL = 'https://$BACKUP_STORAGE_ACCOUNT.blob.core.windows.net/backups/filetransfer_$(date +%Y%m%d_%H%M%S).bak'
              WITH COMPRESSION, INIT, STATS = 10;"
              
              # Verify backup
              if [ $? -eq 0 ]; then
                echo "Database backup completed successfully"
              else
                echo "Database backup failed"
                exit 1
              fi

---
# File backup job
apiVersion: batch/v1
kind: CronJob
metadata:
  name: file-backup
  namespace: file-transfer
spec:
  schedule: "0 3 * * *"  # Daily at 3 AM
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
          - name: file-backup
            image: mcr.microsoft.com/azure-cli
            env:
            - name: AZURE_STORAGE_CONNECTION_STRING
              valueFrom:
                secretKeyRef:
                  name: backup-secret
                  key: connection-string
            volumeMounts:
            - name: file-storage
              mountPath: /data
            command:
            - /bin/bash
            - -c
            - |
              # Sync files to Azure Blob Storage
              az storage blob sync \
                --source /data \
                --container backups \
                --destination-path "files/$(date +%Y%m%d)" \
                --connection-string "$AZURE_STORAGE_CONNECTION_STRING"
              
              echo "File backup completed successfully"
          volumes:
          - name: file-storage
            persistentVolumeClaim:
              claimName: file-storage-pvc
```

### 10.2 Disaster Recovery Procedures

#### Disaster Recovery Playbook
```bash
#!/bin/bash
# scripts/disaster-recovery.sh

set -e

ENVIRONMENT=${1:-production}
BACKUP_DATE=${2:-latest}

echo "Starting disaster recovery for environment: $ENVIRONMENT"

# Step 1: Restore database
echo "Restoring database..."
kubectl create job database-restore-$(date +%s) \
  --from=cronjob/database-backup \
  --namespace=file-transfer

# Wait for database restore
kubectl wait --for=condition=complete job/database-restore-* \
  --namespace=file-transfer \
  --timeout=1800s

# Step 2: Restore file storage
echo "Restoring file storage..."
kubectl create job file-restore-$(date +%s) \
  --from=cronjob/file-backup \
  --namespace=file-transfer

# Step 3: Scale up applications
echo "Scaling up applications..."
kubectl scale deployment file-transfer-web --replicas=2 --namespace=file-transfer
kubectl scale statefulset file-transfer-batch --replicas=1 --namespace=file-transfer

# Step 4: Verify health
echo "Verifying application health..."
kubectl wait --for=condition=ready pod \
  -l app=file-transfer-web \
  --namespace=file-transfer \
  --timeout=300s

kubectl wait --for=condition=ready pod \
  -l app=file-transfer-batch \
  --namespace=file-transfer \
  --timeout=300s

# Step 5: Run health checks
echo "Running health checks..."
kubectl exec -n file-transfer \
  $(kubectl get pod -l app=file-transfer-web -o jsonpath='{.items[0].metadata.name}') \
  -- curl -f http://localhost:8080/actuator/health

echo "Disaster recovery completed successfully"
```

## 11. Monitoring and Alerting Setup

### 11.1 Prometheus Configuration

#### Prometheus Rules and Alerts
```yaml
# k8s/monitoring/prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: file-transfer-rules
  namespace: file-transfer
spec:
  groups:
  - name: file-transfer.rules
    rules:
    - alert: HighErrorRate
      expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: High error rate detected
        description: "Error rate is {{ $value }} errors per second"
    
    - alert: FileProcessingBacklog
      expr: batch_files_pending > 1000
      for: 10m
      labels:
        severity: critical
      annotations:
        summary: File processing backlog detected
        description: "{{ $value }} files are pending processing"
    
    - alert: ACKNACKTimeout
      expr: sum(batch_ack_nack_generated_total{status="EXPIRED"}) > 10
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: High number of expired ACK/NACK files
        description: "{{ $value }} ACK/NACK files have expired"
    
    - alert: DatabaseConnectionFailure
      expr: up{job="file-transfer-web"} == 0
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: Database connection failure
        description: "File transfer web service is down"
```

### 11.2 Grafana Dashboards

#### File Transfer System Dashboard
```json
{
  "dashboard": {
    "title": "File Transfer System Overview",
    "tags": ["file-transfer", "monitoring"],
    "panels": [
      {
        "title": "File Processing Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(batch_files_processed_total[5m])",
            "legendFormat": "Files/sec - {{service}}"
          }
        ],
        "yAxes": [
          {
            "label": "Files per second"
          }
        ]
      },
      {
        "title": "ACK/NACK Status Distribution",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum by (status) (batch_ack_nack_generated_total)",
            "legendFormat": "{{status}}"
          }
        ]
      },
      {
        "title": "System Resource Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "process_cpu_usage{job=~\"file-transfer.*\"}",
            "legendFormat": "CPU - {{job}}"
          },
          {
            "expr": "jvm_memory_used_bytes{job=~\"file-transfer.*\"} / jvm_memory_max_bytes{job=~\"file-transfer.*\"}",
            "legendFormat": "Memory - {{job}}"
          }
        ]
      },
      {
        "title": "Database Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active{job=~\"file-transfer.*\"}",
            "legendFormat": "Active Connections - {{job}}"
          },
          {
            "expr": "rate(spring_data_repository_invocations_total[5m])",
            "legendFormat": "DB Queries/sec - {{repository}}"
          }
        ]
      }
    ]
  }
}
```

## 12. Security Hardening

### 12.1 Network Security

#### Network Policies
```yaml
# k8s/security/network-policies.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: file-transfer-network-policy
  namespace: file-transfer
spec:
  podSelector:
    matchLabels:
      app: file-transfer-web
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    - podSelector:
        matchLabels:
          app: file-transfer-frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
  - to: []
    ports:
    - protocol: TCP
      port: 1433  # SQL Server
    - protocol: TCP
      port: 443   # HTTPS
```

### 12.2 Pod Security Standards

#### Pod Security Policy
```yaml
# k8s/security/pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: file-transfer-psp
  namespace: file-transfer
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  runAsGroup:
    rule: 'MustRunAs'
    ranges:
      - min: 1001
        max: 1001
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'MustRunAs'
    ranges:
      - min: 1001
        max: 1001
  readOnlyRootFilesystem: false
```

## 13. Operational Procedures

### 13.1 Deployment Procedures

#### Blue-Green Deployment Script
```bash
#!/bin/bash
# scripts/blue-green-deploy.sh

NAMESPACE="file-transfer"
NEW_VERSION=$1
CURRENT_VERSION=$(helm get values file-transfer -n $NAMESPACE -o json | jq -r '.global.imageTag')

if [ -z "$NEW_VERSION" ]; then
  echo "Usage: $0 <new-version>"
  exit 1
fi

echo "Starting blue-green deployment from $CURRENT_VERSION to $NEW_VERSION"

# Step 1: Deploy green environment
echo "Deploying green environment..."
helm upgrade file-transfer-green ./helm/file-transfer \
  --namespace $NAMESPACE \
  --set global.imageTag=$NEW_VERSION \
  --set nameOverride="green" \
  --values ./helm/file-transfer/values-production.yaml \
  --wait --timeout=10m

# Step 2: Run health checks on green
echo "Running health checks on green environment..."
kubectl wait --for=condition=ready pod \
  -l app=file-transfer-web,version=green \
  --namespace=$NAMESPACE \
  --timeout=300s

# Step 3: Run smoke tests
echo "Running smoke tests..."
./scripts/run-smoke-tests.sh green

if [ $? -ne 0 ]; then
  echo "Smoke tests failed, rolling back..."
  helm delete file-transfer-green -n $NAMESPACE
  exit 1
fi

# Step 4: Switch traffic to green
echo "Switching traffic to green..."
kubectl patch ingress file-transfer-ingress -n $NAMESPACE --type='json' \
  -p='[{"op": "replace", "path": "/spec/rules/0/http/paths/0/backend/service/name", "value": "file-transfer-web-green-service"}]'

# Step 5: Monitor for 10 minutes
echo "Monitoring green environment for 10 minutes..."
sleep 600

# Step 6: Remove blue environment
echo "Removing blue environment..."
helm delete file-transfer -n $NAMESPACE

# Step 7: Rename green to main
echo "Promoting green to main..."
helm upgrade file-transfer ./helm/file-transfer \
  --namespace $NAMESPACE \
  --set global.imageTag=$NEW_VERSION \
  --values ./helm/file-transfer/values-production.yaml \
  --wait --timeout=10m

echo "Blue-green deployment completed successfully"
```

### 13.2 Maintenance Procedures

#### Database Maintenance Script
```bash
#!/bin/bash
# scripts/database-maintenance.sh

NAMESPACE="file-transfer"
MAINTENANCE_MODE=${1:-false}

if [ "$MAINTENANCE_MODE" = "true" ]; then
  echo "Enabling maintenance mode..."
  
  # Scale down batch processing
  kubectl scale statefulset file-transfer-batch --replicas=0 -n $NAMESPACE
  
  # Update web app to maintenance mode
  kubectl patch deployment file-transfer-web -n $NAMESPACE \
    --patch '{"spec":{"template":{"spec":{"containers":[{"name":"web-app","env":[{"name":"MAINTENANCE_MODE","value":"true"}]}]}}}}'
  
  echo "Maintenance mode enabled"
else
  echo "Disabling maintenance mode..."
  
  # Scale up batch processing
  kubectl scale statefulset file-transfer-batch --replicas=1 -n $NAMESPACE
  
  # Remove maintenance mode
  kubectl patch deployment file-transfer-web -n $NAMESPACE \
    --patch '{"spec":{"template":{"spec":{"containers":[{"name":"web-app","env":[{"name":"MAINTENANCE_MODE","value":"false"}]}]}}}}'
  
  echo "Maintenance mode disabled"
fi

# Wait for rollout to complete
kubectl rollout status deployment/file-transfer-web -n $NAMESPACE
kubectl rollout status statefulset/file-transfer-batch -n $NAMESPACE

echo "Database maintenance procedure completed"
```

## 14. Performance Tuning

### 14.1 JVM Optimization

#### Production JVM Settings
```yaml
# JVM optimization for containers
env:
- name: JAVA_OPTS
  value: |
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=75.0
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+ParallelRefProcEnabled
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
    -Djava.security.egd=file:/dev/./urandom
    -Dspring.backgroundpreinitializer.ignore=true

# Memory and CPU requests/limits
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"

# JVM heap dump configuration
volumeMounts:
- name: heap-dumps
  mountPath: /app/heapdumps

env:
- name: JAVA_OPTS
  value: |
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/app/heapdumps/
    -XX:+ExitOnOutOfMemoryError
```

### 14.2 Database Performance Tuning

#### Connection Pool Optimization
```yaml
# Database connection configuration
env:
- name: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
  value: "30"
- name: SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE
  value: "10"
- name: SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT
  value: "20000"
- name: SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT
  value: "300000"
- name: SPRING_DATASOURCE_HIKARI_MAX_LIFETIME
  value: "1200000"
- name: SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD
  value: "60000"

# JPA/Hibernate optimization
- name: SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_BATCH_SIZE
  value: "50"
- name: SPRING_JPA_PROPERTIES_HIBERNATE_ORDER_INSERTS
  value: "true"
- name: SPRING_JPA_PROPERTIES_HIBERNATE_ORDER_UPDATES
  value: "true"
- name: SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_BATCH_VERSIONED_DATA
  value: "true"
```

This comprehensive deployment and infrastructure documentation provides complete guidance for deploying, monitoring, and maintaining the File Transfer Management System in production environments.