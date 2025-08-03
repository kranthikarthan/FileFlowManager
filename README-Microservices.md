# File Transfer Microservices System

A cloud-native, enterprise-grade file transfer management system built with microservices architecture, designed for deployment on Azure Kubernetes Service (AKS) with Azure SQL Managed Instance.

## 🏗️ **Microservices Architecture**

The system has been redesigned from a monolithic application to a distributed microservices architecture with the following services:

### **Core Services**

1. **Configuration Service** (`config-service`)
   - **Port**: 8080
   - **Purpose**: Centralized configuration management
   - **Features**: Service configuration, SSO settings, validation rules
   - **Database**: Azure SQL MI (shared schema)

2. **Processing Service** (`processing-service`)
   - **Port**: 8081
   - **Purpose**: File transfer batch processing
   - **Features**: File monitoring, SOT/EOT handling, file validation
   - **Storage**: Azure Blob Storage + Local PVC

3. **Authentication Service** (`auth-service`)
   - **Port**: 8082
   - **Purpose**: User authentication and authorization
   - **Features**: SSO integration, JWT token management, user profiles
   - **Database**: Azure SQL MI (auth schema)

4. **Notification Service** (`notification-service`)
   - **Port**: 8083
   - **Purpose**: Event notifications and alerts
   - **Features**: Email, Teams, Slack notifications, event tracking
   - **Message Queue**: RabbitMQ

5. **API Gateway** (`api-gateway`)
   - **Port**: 8080
   - **Purpose**: Single entry point for all client requests
   - **Features**: Routing, load balancing, rate limiting, authentication

6. **Frontend Service** (`frontend`)
   - **Port**: 80
   - **Purpose**: React-based user interface
   - **Features**: Service management, file monitoring, configuration UI

### **Infrastructure Services**

7. **Service Discovery** (Eureka Server)
   - **Port**: 8761
   - **Purpose**: Service registration and discovery
   - **Features**: Health checks, load balancing

8. **Distributed Tracing** (Zipkin)
   - **Port**: 9411
   - **Purpose**: Request tracing across services
   - **Features**: Performance monitoring, debugging

## 🚀 **Technology Stack**

### **Application Stack**
- **Java**: 17 (LTS)
- **Spring Boot**: 3.2.0
- **Spring Cloud**: 2023.0.0
- **Spring Security**: OAuth2 + JWT
- **Spring Batch**: File processing
- **React**: 18.2 (Frontend)
- **Material-UI**: 5.x (UI Components)

### **Azure Services**
- **Azure Kubernetes Service (AKS)**: Container orchestration
- **Azure SQL Managed Instance**: Primary database
- **Azure Container Registry (ACR)**: Container images
- **Azure Key Vault**: Secrets management
- **Azure Blob Storage**: File storage
- **Azure Managed Identity**: Secure authentication
- **Azure Monitor**: Logging and monitoring

### **Supporting Technologies**
- **Docker**: Containerization
- **Helm**: Kubernetes package management
- **Nginx**: Ingress controller
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **RabbitMQ**: Message queuing
- **Redis**: Caching layer

## 📁 **Project Structure**

```
file-transfer-microservices/
├── microservices/
│   ├── file-transfer-config-service/      # Configuration management
│   ├── file-transfer-processing-service/  # File processing
│   ├── file-transfer-auth-service/        # Authentication
│   ├── file-transfer-notification-service/# Notifications
│   ├── file-transfer-gateway-service/     # API Gateway
│   └── file-transfer-frontend/            # React UI
├── k8s/                                   # Kubernetes manifests
│   ├── namespace.yml
│   ├── config-service.yml
│   ├── processing-service.yml
│   ├── auth-service.yml
│   ├── notification-service.yml
│   ├── gateway-service.yml
│   ├── frontend-service.yml
│   └── infrastructure/
├── helm/                                  # Helm charts
│   └── file-transfer/
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
├── azure-pipelines/                      # Azure DevOps pipelines
│   ├── build-pipeline.yml
│   ├── release-pipeline.yml
│   └── pr-pipeline.yml
├── scripts/                              # Deployment scripts
│   ├── deploy-to-aks.sh
│   ├── setup-azure-resources.sh
│   └── monitoring-setup.sh
├── docs/                                 # Documentation
│   ├── microservices-architecture.md
│   ├── azure-deployment-guide.md
│   └── api-documentation.md
└── README-Microservices.md              # This file
```

## 🔧 **Configuration**

### **Azure SQL Managed Instance**

Each service connects to Azure SQL MI with optimized connection settings:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://${AZURE_SQL_MI_SERVER}:1433;database=${AZURE_SQL_MI_DATABASE};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;
    username: ${AZURE_SQL_MI_USERNAME}
    password: ${AZURE_SQL_MI_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### **Azure Key Vault Integration**

Sensitive configuration is stored in Azure Key Vault:

```yaml
spring:
  cloud:
    azure:
      keyvault:
        secret:
          endpoint: ${AZURE_KEYVAULT_URI}
          property-sources:
            - endpoint: ${AZURE_KEYVAULT_URI}
              name: filetransfer-secrets
```

### **Service Discovery**

All services register with Eureka for discovery:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-service:8761/eureka
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
```

## 🚀 **Quick Start (Azure Deployment)**

### **Prerequisites**

1. **Azure CLI** installed and configured
2. **kubectl** installed
3. **Helm 3.x** installed
4. **Docker** installed (for local development)
5. Azure subscription with appropriate permissions

### **Automated Deployment**

```bash
# Clone the repository
git clone https://github.com/your-org/file-transfer-microservices.git
cd file-transfer-microservices

# Make deployment script executable
chmod +x scripts/deploy-to-aks.sh

# Set environment variables (optional)
export RESOURCE_GROUP="file-transfer-rg"
export AKS_CLUSTER_NAME="file-transfer-aks"
export ACR_NAME="filetransferacr"
export LOCATION="East US"

# Deploy to Azure
./scripts/deploy-to-aks.sh
```

### **Manual Deployment Steps**

1. **Create Azure Resources**
   ```bash
   # Create resource group
   az group create --name file-transfer-rg --location "East US"
   
   # Create AKS cluster
   az aks create \
     --resource-group file-transfer-rg \
     --name file-transfer-aks \
     --node-count 3 \
     --enable-addons monitoring \
     --enable-managed-identity \
     --enable-workload-identity \
     --generate-ssh-keys
   
   # Create Azure Container Registry
   az acr create \
     --resource-group file-transfer-rg \
     --name filetransferacr \
     --sku Standard
   ```

2. **Build and Push Images**
   ```bash
   # Build all services
   cd microservices
   for service in */; do
     cd $service
     mvn clean package -DskipTests
     docker build -t filetransferacr.azurecr.io/filetransfer/${service%/}:latest .
     cd ..
   done
   
   # Push to ACR
   az acr login --name filetransferacr
   docker push filetransferacr.azurecr.io/filetransfer/config-service:latest
   # ... push all images
   ```

3. **Deploy with Helm**
   ```bash
   # Add Helm repositories
   helm repo add bitnami https://charts.bitnami.com/bitnami
   helm repo update
   
   # Deploy the application
   helm install file-transfer helm/file-transfer \
     --namespace file-transfer \
     --create-namespace \
     --values helm/file-transfer/values-production.yaml
   ```

## 🔄 **CI/CD Pipeline**

### **Azure DevOps Pipeline Features**

- **Multi-service builds** with parallel execution
- **SonarCloud integration** for code quality
- **Security scanning** with credential scanning
- **Docker vulnerability scanning**
- **Automated testing** (unit, integration, E2E)
- **Quality gates** before deployment
- **Helm chart packaging** and deployment

### **Pipeline Stages**

1. **Build & Test** - Compile, test, and analyze code
2. **Security Scan** - Credential and vulnerability scanning
3. **Build Images** - Create and push Docker images
4. **Quality Gate** - SonarCloud quality validation
5. **Integration Tests** - End-to-end testing
6. **Deploy to Dev** - Automatic deployment to development
7. **Deploy to Prod** - Manual approval for production

### **Quality Gates**

- **Code Coverage**: Minimum 80%
- **Security Vulnerabilities**: No high/critical issues
- **Code Smells**: Grade A or better
- **Duplicated Code**: Less than 3%
- **Unit Test Pass Rate**: 100%

## 📊 **Monitoring & Observability**

### **Application Monitoring**

- **Prometheus**: Metrics collection from all services
- **Grafana**: Custom dashboards for business metrics
- **Azure Monitor**: Integration with Azure services
- **Application Insights**: Performance and dependency tracking

### **Distributed Tracing**

- **Zipkin**: Request tracing across microservices
- **Spring Cloud Sleuth**: Automatic trace propagation
- **Correlation IDs**: Track requests across service boundaries

### **Health Checks**

Each service provides health endpoints:
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Metrics**: `/actuator/prometheus`

### **Alerting**

- **Service downtime** alerts
- **High error rate** notifications
- **Performance degradation** warnings
- **Resource utilization** alerts

## 🔒 **Security**

### **Authentication & Authorization**

- **Azure Workload Identity**: Secure pod authentication
- **JWT tokens**: Stateless authentication
- **RBAC**: Kubernetes role-based access control
- **Network Policies**: Service-to-service communication control

### **Secrets Management**

- **Azure Key Vault**: Centralized secrets storage
- **Kubernetes Secrets**: Runtime secret injection
- **Secret rotation**: Automated credential updates

### **Container Security**

- **Non-root containers**: All containers run as non-root users
- **Read-only filesystems**: Immutable container filesystems
- **Security scanning**: Vulnerability assessment in CI/CD
- **Pod Security Standards**: Enforced security policies

## 📈 **Scaling & Performance**

### **Horizontal Pod Autoscaling (HPA)**

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
```

### **Vertical Pod Autoscaling (VPA)**

- **Resource recommendations**: Automatic resource sizing
- **Right-sizing**: Optimal CPU/memory allocation
- **Cost optimization**: Efficient resource utilization

### **Database Performance**

- **Connection pooling**: HikariCP with optimized settings
- **Read replicas**: Separate read/write workloads
- **Query optimization**: Performance monitoring and tuning

## 🔧 **Development**

### **Local Development Setup**

```bash
# Start infrastructure services
docker-compose -f docker-compose.dev.yml up -d

# Run services locally
cd microservices/file-transfer-config-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# In separate terminals, start other services
cd microservices/file-transfer-processing-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### **Testing**

```bash
# Unit tests
mvn test

# Integration tests
mvn verify -Pintegration-tests

# End-to-end tests
mvn verify -Pe2e-tests -Dtest.environment=local
```

### **Code Quality**

```bash
# SonarQube analysis
mvn sonar:sonar \
  -Dsonar.projectKey=file-transfer-microservices \
  -Dsonar.host.url=http://localhost:9000

# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check
```

## 🐛 **Troubleshooting**

### **Common Issues**

1. **Service Discovery Issues**
   ```bash
   # Check Eureka registration
   kubectl port-forward svc/eureka-service 8761:8761
   # Visit http://localhost:8761
   ```

2. **Database Connection Issues**
   ```bash
   # Check SQL MI connectivity
   kubectl exec -it deployment/config-service -- \
     curl -f http://localhost:8080/actuator/health
   ```

3. **Image Pull Issues**
   ```bash
   # Check ACR authentication
   az acr login --name filetransferacr
   kubectl get pods -n file-transfer
   ```

### **Debugging Commands**

```bash
# View all resources
kubectl get all -n file-transfer

# Check pod logs
kubectl logs -f deployment/config-service -n file-transfer

# Describe problematic pods
kubectl describe pod <pod-name> -n file-transfer

# Execute commands in containers
kubectl exec -it deployment/config-service -- /bin/sh
```

## 📋 **Maintenance**

### **Backup Strategy**

- **Database backups**: Automated Azure SQL MI backups
- **Configuration backups**: Azure Key Vault backup
- **Application data**: Azure Blob Storage backup

### **Updates & Upgrades**

- **Rolling updates**: Zero-downtime deployments
- **Blue-green deployments**: For major updates
- **Canary releases**: Gradual rollout of new features

### **Monitoring & Alerts**

- **Service health**: Continuous health monitoring
- **Performance metrics**: Response time and throughput tracking
- **Resource usage**: CPU, memory, and storage monitoring

## 🤝 **Contributing**

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Follow coding standards and write tests
4. Run quality checks (`mvn verify`)
5. Commit changes (`git commit -am 'Add new feature'`)
6. Push to branch (`git push origin feature/new-feature`)
7. Create a Pull Request

### **Development Guidelines**

- **Code Coverage**: Minimum 80% for new code
- **Documentation**: Update docs for new features
- **Testing**: Write unit and integration tests
- **Security**: Follow secure coding practices

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🎯 **Migration from Monolith**

### **Key Changes**

1. **Separated Concerns**: Each service has a specific responsibility
2. **Independent Deployment**: Services can be deployed independently
3. **Technology Diversity**: Services can use different technologies
4. **Fault Isolation**: Failure in one service doesn't affect others
5. **Scalability**: Individual services can be scaled based on demand

### **Migration Benefits**

- **Better Resource Utilization**: Scale services independently
- **Improved Fault Tolerance**: Service isolation and redundancy
- **Faster Development**: Teams can work on services independently
- **Technology Flexibility**: Choose the right tool for each service
- **Cloud-Native**: Built for modern cloud platforms

### **Enterprise Features**

- **Multi-tenancy**: Organization-based data isolation
- **Audit Logging**: Comprehensive audit trail
- **Compliance**: Security and regulatory compliance
- **High Availability**: 99.9% uptime SLA
- **Disaster Recovery**: Multi-region backup and recovery

## 📞 **Support**

For support and questions:
- **Documentation**: Check the `/docs` folder
- **Issues**: Create GitHub issues for bugs
- **Discussions**: Use GitHub discussions for questions
- **Email**: filetransfer-support@yourcompany.com