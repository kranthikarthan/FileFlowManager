# File Flow Management System

A comprehensive, enterprise-grade microservices-based File Flow Management System built with Spring Boot, React, and Azure cloud services. The system automatically manages file movement from configurable input folders to output folders with comprehensive validation, monitoring, and alerting capabilities.

## 🏗️ System Architecture

### Backend Services
- **Batch Service** (Spring Boot + Spring Batch): File monitoring, processing, and validation
- **Web API Service** (Spring Boot): REST APIs for management and enquiry operations
- **Shared Library**: Common entities, DTOs, and utilities

### Frontend
- **React Application**: Modern, responsive UI with SSO integration using Azure AD

### Infrastructure
- **Azure Kubernetes Service (AKS)**: Container orchestration
- **Azure SQL Managed Instance**: Database persistence
- **Azure Blob Storage**: File staging and schema storage
- **Azure Container Registry**: Docker image repository
- **Azure DevOps**: CI/CD pipeline

## 🚀 Features

### File Processing
- **Automated File Monitoring**: Real-time monitoring of configurable input directories
- **SOT/EOT Pattern Matching**: Start of Transmission and End of Transmission file handling
- **Multi-format Validation**: 
  - XML validation using XSD schemas
  - JSON validation using JSON Schema
  - Flat file validation using YAML/Copybook rules
- **Batch Processing**: Groups related files for atomic processing
- **Error Handling & Retry**: Configurable retry mechanisms with exponential backoff

### Multi-tenancy & Configuration
- **Tenant-aware Architecture**: Support for multiple organizations with isolated data
- **Service & Sub-service Hierarchy**: Hierarchical configuration with inheritance
- **Flexible Configuration**: Override settings at sub-service level
- **Holiday Calendar Support**: Tenant-specific holiday management
- **Time Zone Awareness**: Multi-timezone support for global operations

### Security & Authentication
- **SSO Integration**: Azure AD integration with MSAL
- **Role-based Access Control**: Fine-grained permissions (Admin, Operator, User)
- **JWT Token Validation**: Secure API access
- **Audit Trail**: Complete audit logging for all operations

### Monitoring & Alerting
- **Real-time Dashboard**: File processing statistics and health metrics
- **Alert Management**: Configurable alerts for missing EOT files
- **Cut-off Time Monitoring**: Business day processing with holiday support
- **Performance Metrics**: Prometheus metrics integration
- **Health Checks**: Kubernetes-ready health endpoints

## 📋 Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **Docker** and **Docker Compose**
- **Azure CLI**
- **kubectl** (for Kubernetes deployment)
- **Maven 3.8+**
- **Azure Subscription** with the following services:
  - Azure Kubernetes Service (AKS)
  - Azure SQL Managed Instance
  - Azure Blob Storage
  - Azure Container Registry
  - Azure Active Directory

## 🛠️ Local Development Setup

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/file-flow-management.git
cd file-flow-management
```

### 2. Environment Configuration
Create environment files for local development:

**.env.local** (backend services):
```env
# Database Configuration
DB_HOST=localhost
DB_PORT=1433
DB_NAME=fileflow
DB_USERNAME=sa
DB_PASSWORD=YourPassword123!

# Azure Storage (use Azurite for local development)
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
AZURE_STORAGE_CONTAINER=fileflow-staging

# JWT Configuration
JWT_ISSUER_URI=https://login.microsoftonline.com/your-tenant-id/v2.0
JWT_AUDIENCE=api://fileflow-api
TENANT_ID=your-azure-tenant-id

# File Processing
SCHEMA_BASE_PATH=./schemas
TEMP_DIRECTORY=./temp/fileflow
```

**frontend/.env.local** (React app):
```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_CLIENT_ID=your-azure-ad-client-id
REACT_APP_AUTHORITY=https://login.microsoftonline.com/your-tenant-id
REACT_APP_REDIRECT_URI=http://localhost:3000
```

### 3. Start Local Infrastructure
```bash
# Start SQL Server and Azurite using Docker Compose
docker-compose up -d sqlserver azurite

# Wait for SQL Server to be ready
docker-compose logs -f sqlserver
```

### 4. Build and Run Backend Services
```bash
# Build shared library
cd backend/shared
mvn clean install

# Start Batch Service
cd ../batch-service
mvn spring-boot:run

# Start Web API Service (in new terminal)
cd ../web-api-service
mvn spring-boot:run
```

### 5. Start Frontend
```bash
cd frontend
npm install
npm start
```

The application will be available at:
- **Frontend**: http://localhost:3000
- **Web API**: http://localhost:8080/api
- **Batch API**: http://localhost:8081/batch-api
- **API Documentation**: http://localhost:8080/api/swagger-ui.html

## 🐳 Docker Deployment

### Build Docker Images
```bash
# Build all services
docker-compose build

# Or build individually
docker build -t fileflow-batch-service ./backend/batch-service
docker build -t fileflow-web-api ./backend/web-api-service
docker build -t fileflow-frontend ./frontend
```

### Run with Docker Compose
```bash
docker-compose up -d
```

## ☁️ Azure Cloud Deployment

### 1. Azure Resource Setup
```bash
# Create resource group
az group create --name fileflow-rg --location eastus

# Create AKS cluster
az aks create \
  --resource-group fileflow-rg \
  --name fileflow-aks \
  --node-count 3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Create Azure Container Registry
az acr create \
  --resource-group fileflow-rg \
  --name fileflowregistry \
  --sku Standard

# Create SQL Managed Instance (this takes 4-6 hours)
az sql mi create \
  --name fileflow-sqlmi \
  --resource-group fileflow-rg \
  --location eastus \
  --admin-user fileflow_admin \
  --admin-password YourSecurePassword123!
```

### 2. Configure Kubernetes
```bash
# Get AKS credentials
az aks get-credentials --resource-group fileflow-rg --name fileflow-aks

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager for TLS
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.2/cert-manager.yaml
```

### 3. Deploy to Kubernetes
```bash
# Update image references in deployment files
# Then apply manifests
kubectl apply -f infrastructure/k8s/
```

### 4. Azure DevOps Pipeline Setup
1. Create Azure DevOps project
2. Create service connections:
   - Azure Container Registry
   - Azure Kubernetes Service
   - SonarCloud (optional)
3. Import the pipeline: `infrastructure/azure-pipelines/azure-pipelines.yml`
4. Configure variable groups with secrets
5. Run the pipeline

## 📊 Usage Guide

### Service Configuration
1. **Access Admin Panel**: Navigate to `/admin` in the web application
2. **Create Tenant**: Set up organization with timezone and SSO settings
3. **Configure Services**: Define input/output folders, file patterns, and cut-off times
4. **Setup Sub-services**: Create sub-services with override configurations
5. **Upload Schemas**: Upload XSD, JSON Schema, or YAML validation rules

### File Processing Workflow
1. **File Detection**: System monitors configured input directories
2. **Pattern Matching**: Files are matched against SOT, Data, and EOT patterns
3. **Batch Formation**: Related files are grouped into processing batches
4. **Validation**: Content validation using configured schemas
5. **Processing**: Files are moved to output directories
6. **Notification**: Alerts generated for errors or missing files

### Monitoring and Operations
- **Dashboard**: Real-time view of file processing statistics
- **File Transactions**: Search and filter processed files
- **Service Management**: Start/stop monitoring, close/reopen services
- **Alert Management**: Configure and view processing alerts

## 🔧 Configuration Reference

### Application Properties
Key configuration properties for each service:

**Batch Service**:
- `fileflow.monitoring.poll-interval`: Directory polling frequency
- `fileflow.monitoring.thread-pool-size`: Processing thread pool size
- `fileflow.monitoring.max-retry-attempts`: Maximum retry attempts
- `fileflow.validation.schema-base-path`: Schema files location

**Web API Service**:
- `fileflow.cors.allowed-origins`: CORS allowed origins
- `spring.security.oauth2.resourceserver.jwt`: JWT validation settings
- `storage.azure.connection-string`: Azure Blob Storage connection

### File Patterns
Use regex patterns for file matching:
- **SOT Pattern**: `^SOT_\w+_\d{8}\.txt$`
- **Data Pattern**: `^DATA_\w+_\d{8}_\d{4}\.xml$`
- **EOT Pattern**: `^EOT_\w+_\d{8}\.txt$`

## 🧪 Testing

### Unit Tests
```bash
# Backend tests
cd backend/batch-service
mvn test

cd ../web-api-service
mvn test

# Frontend tests
cd frontend
npm test
```

### Integration Tests
```bash
# Run integration tests with test containers
mvn integration-test -Dspring.profiles.active=integration
```

### Load Testing
Use included JMeter scripts in `tests/performance/` directory.

## 📈 Monitoring and Observability

### Metrics
- **Prometheus Metrics**: Available at `/actuator/prometheus`
- **Custom Metrics**: File processing rates, error counts, processing times
- **Grafana Dashboards**: Pre-built dashboards for monitoring

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Log Aggregation**: Configure log shipping to Azure Monitor
- **Log Levels**: Configurable per package

### Health Checks
- **Kubernetes Probes**: Liveness and readiness probes configured
- **Dependency Checks**: Database, Azure Storage, and external service health
- **Custom Health Indicators**: File system access, schema validation

## 🔒 Security Considerations

### Authentication & Authorization
- **Azure AD Integration**: Enterprise SSO with MFA support
- **API Security**: JWT token validation with audience verification
- **Role-based Access**: Granular permissions for different user types

### Data Protection
- **Encryption in Transit**: TLS 1.3 for all communications
- **Encryption at Rest**: Azure SQL TDE and Blob Storage encryption
- **Secrets Management**: Azure Key Vault integration (recommended)

### Network Security
- **Virtual Network**: Deploy AKS in private VNet
- **Network Policies**: Kubernetes network policies for pod-to-pod communication
- **Private Endpoints**: Use private endpoints for Azure services

## 🐛 Troubleshooting

### Common Issues

**File Monitoring Not Working**:
- Check input directory permissions
- Verify file patterns in service configuration
- Review batch service logs for errors

**Authentication Failures**:
- Verify Azure AD app registration configuration
- Check JWT issuer and audience settings
- Ensure proper CORS configuration

**Database Connection Issues**:
- Verify SQL MI network connectivity
- Check connection string format
- Validate database user permissions

### Debug Mode
Enable debug logging by setting:
```yaml
logging:
  level:
    com.enterprise.fileflow: DEBUG
    org.springframework.security: DEBUG
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Spring Boot best practices
- Use consistent code formatting (provided .editorconfig)
- Write unit tests for new features
- Update documentation for API changes

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- **Documentation**: Check this README and inline code documentation
- **Issues**: Create GitHub issues for bugs and feature requests
- **Security Issues**: Email security@yourcompany.com

## 🗂️ Project Structure

```
file-flow-management/
├── backend/
│   ├── shared/                 # Common entities and DTOs
│   ├── batch-service/          # File processing service
│   └── web-api-service/        # REST API service
├── frontend/                   # React application
├── infrastructure/
│   ├── k8s/                   # Kubernetes manifests
│   ├── helm/                  # Helm charts
│   └── azure-pipelines/       # CI/CD pipelines
├── schemas/                   # Sample validation schemas
│   ├── xml/                   # XSD schemas
│   ├── json/                  # JSON schemas
│   └── copybook/              # Flat file schemas
├── docs/                      # Additional documentation
├── docker-compose.yml         # Local development environment
└── README.md                  # This file
```

---

**Built with ❤️ using Spring Boot, React, and Azure Cloud Services**