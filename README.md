# File Transfer Management System

A comprehensive full-stack application for managing file transfers between different services with configurable folders, start/end transmission markers, real-time monitoring capabilities, dynamic service configuration, file validation, and SSO authentication.

## 🆕 **New Features Added**

### **Dynamic Service Configuration**
- **Frontend Service Management**: Complete CRUD interface for managing file transfer services
- **Configurable File Prefixes**: Dynamic configuration of SOT/EOT prefixes per service
- **Real-time Validation**: Test file validation patterns directly from the UI
- **Service Status Control**: Enable/disable services with real-time status updates

### **Advanced File Validation**
- **Regex-based Validation**: Configure custom regex patterns for SOT, EOT, and data files
- **Per-Service Validation**: Different validation rules for each service type
- **Live Testing**: Test validation patterns against file names before deployment
- **Validation Feedback**: Real-time validation results with clear success/failure indicators

### **SSO Authentication System**
- **Multi-Provider Support**: Azure AD, Google, Okta, Keycloak, Custom OIDC, SAML2
- **Organization-based Configuration**: Configure different SSO providers per organization
- **Attribute Mapping**: Map SSO provider attributes to local user attributes
- **Dynamic Login Interface**: Organization selection with provider-specific branding
- **Secure Configuration**: Encrypted storage of SSO credentials and secrets

### **Enhanced User Interface**
- **Authentication Flow**: Complete login/logout with session management
- **Service Configuration UI**: Intuitive interface for service management
- **SSO Configuration UI**: Comprehensive SSO provider setup and testing
- **Validation Testing UI**: Interactive file validation testing tools
- **User Management**: Profile display with organization context

## Architecture Overview

The system consists of three main components:

1. **Spring Boot Batch Application** - Handles file monitoring and batch processing
2. **Spring Boot Web Application** - Provides REST API for file transfer management and authentication
3. **React Frontend** - User interface for monitoring, managing file transfers, services, and SSO

## Features

### Core Functionality
- **Automated File Monitoring**: Monitors configurable folders for incoming files
- **Start/End Transmission Markers**: Supports SOT/EOT files for each data transfer
- **Multiple Service Types**: Configurable services with different file patterns and paths
- **File Transfer Tracking**: Complete audit trail of all file transfers
- **Real-time Status Updates**: Live monitoring of transfer progress

### Service Management
- **Dynamic Service Configuration**: Add, edit, and delete services through the UI
- **Configurable File Prefixes**: Customize SOT/EOT prefixes per service
- **File Pattern Configuration**: Set file patterns and polling intervals
- **Service Status Management**: Enable/disable services independently
- **Validation Rules**: Configure regex-based file validation per service

### File Validation System
- **Regex-based Validation**: Custom validation patterns for different file types
- **Multi-level Validation**: Separate rules for SOT, EOT, and data files
- **Real-time Testing**: Test validation patterns before applying them
- **Validation Feedback**: Clear success/failure indicators for file validation

### SSO Authentication
- **Multi-Provider Support**: Support for major SSO providers
- **Organization Management**: Configure different SSO setups per organization
- **Attribute Mapping**: Map provider attributes to local user fields
- **Secure Configuration**: Encrypted credential storage
- **Provider Testing**: Test SSO connections before activation

### Management Capabilities
- **File Status Inquiry**: Query transfers by service, status, date range, etc.
- **Transfer Operations**: Retry failed transfers, cancel pending transfers
- **Service Health Monitoring**: Real-time service status and statistics
- **Dashboard Analytics**: Visual insights into transfer statistics
- **User Authentication**: Secure access with SSO or local authentication

### Technical Features
- **Kubernetes Deployment**: Production-grade deployment using Kubernetes manifests
- **Database Integration**: MySQL for persistent storage with dynamic schema
- **RESTful API**: Comprehensive REST endpoints for all operations
- **Modern UI**: React with Material-UI components
- **Security**: Spring Security with OAuth2 and JWT support
- **Configuration Management**: Database-driven configuration

## Quick Start

### Prerequisites
- Kubernetes cluster (v1.20+)
- kubectl configured
- Persistent Volume support
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd file-transfer-system
   ```

2. **Deploy to Kubernetes**
   - Apply the provided Kubernetes manifests in the `k8s/` directory:
   ```bash
   kubectl apply -f k8s/
   ```
   - This will deploy MySQL, backend services, and the frontend to your cluster.

3. **Access the application**
   - Frontend: http://<your-k8s-ingress-or-service>
   - Web API: http://<your-k8s-ingress-or-service>/api
   - Batch App: http://<your-k8s-ingress-or-service>:8081
   - Database: <your-mysql-service>:3306

4. **Default Login**
   - **SSO**: Demo Organization (demo-org) with Azure AD provider
   - **Local**: Any username/password (demo mode)

### Stopping the Application
```bash
kubectl delete -f k8s/
```

## Application Components

### 1. Spring Boot Batch Application (Port 8081)

**Location**: `file-transfer-batch/`

**Enhanced Responsibilities**:
- File system monitoring for inbound directories
- Processing start/end transmission markers with configurable prefixes
- File validation using regex patterns
- Dynamic service configuration loading
- File transfer execution from inbound to outbound folders
- Batch job scheduling and execution

### 2. Spring Boot Web Application (Port 8080)

**Location**: `file-transfer-web/`

**Enhanced Responsibilities**:
- REST API for file transfer management
- **NEW**: Service configuration CRUD operations
- **NEW**: SSO configuration management
- **NEW**: File validation API endpoints
- **NEW**: Authentication and authorization
- Database operations and queries
- Transfer retry/cancel operations
- Service status management

**New Key Endpoints**:
- `GET/POST/PUT/DELETE /api/services` - Service configuration management
- `POST /api/services/validate-file` - File validation testing
- `GET/POST/PUT/DELETE /api/sso` - SSO configuration management
- `POST /api/sso/{id}/test` - SSO connection testing
- `POST /api/auth/login` - Authentication endpoints

### 3. React Frontend (Port 3000)

**Location**: `file-transfer-frontend/`

**Enhanced Features**:
- **NEW**: Service Configuration page with full CRUD operations
- **NEW**: SSO Configuration page with provider management
- **NEW**: Authentication flow with login/logout
- **NEW**: File validation testing interface
- **NEW**: Organization-based SSO selection
- Dashboard with transfer statistics and charts
- File transfer list with filtering and sorting
- Service management interface
- Real-time status updates
- Responsive Material-UI design

## Configuration

### Service Configuration

Services can now be configured dynamically through the UI or API:

```json
{
  "serviceName": "service1",
  "inboundPath": "/app/data/inbound/service1",
  "outboundPath": "/app/data/outbound/service1",
  "startMarkerPrefix": "SOT_",
  "endMarkerPrefix": "EOT_",
  "dataFilePattern": "*.dat",
  "enabled": true,
  "maxRetries": 3,
  "pollIntervalSeconds": 30,
  "sotFileValidationRegex": "^SOT_[A-Z0-9]+_\\d{8}\\.txt$",
  "eotFileValidationRegex": "^EOT_[A-Z0-9]+_\\d{8}\\.txt$",
  "dataFileValidationRegex": "^DATA_[A-Z0-9]+_\\d{8}\\.(dat|xml)$",
  "description": "Primary data service"
}
```

### SSO Configuration

SSO providers can be configured per organization:

```json
{
  "organizationId": "acme-corp",
  "organizationName": "ACME Corporation",
  "provider": "AZURE_AD",
  "clientId": "your-client-id",
  "clientSecret": "your-client-secret",
  "issuerUri": "https://login.microsoftonline.com/tenant-id/v2.0",
  "scopes": "openid,profile,email",
  "attributesMapping": {
    "email": "email",
    "name": "name",
    "firstName": "given_name",
    "lastName": "family_name"
  },
  "enabled": true
}
```

### File Validation

Configure regex patterns for different file types:

- **SOT Files**: `^SOT_[A-Z0-9]+_\d{8}\.txt$`
- **EOT Files**: `^EOT_[A-Z0-9]+_\d{8}\.txt$`
- **Data Files**: `^DATA_[A-Z0-9]+_\d{8}\.(dat|xml)$`

### Database Configuration

Enhanced database schema includes:

**New Tables**:
- `service_configurations`: Dynamic service configuration storage
- `sso_configurations`: SSO provider configurations
- `sso_attributes_mapping`: Attribute mapping for SSO providers

## File Transfer Workflow

1. **Service Configuration**: Configure services dynamically through UI
2. **File Detection**: Batch application monitors inbound directories using configured paths
3. **File Validation**: Validate incoming files against configured regex patterns
4. **Start Marker Processing**: When SOT file is detected, system identifies corresponding data files
5. **End Marker Waiting**: Data files wait for corresponding EOT file
6. **Transfer Execution**: Once EOT is received, files are transferred to outbound directory
7. **Status Tracking**: All operations are logged in the database
8. **Web Interface**: Users can monitor progress and manage transfers

## User Interface Guide

### Login Process
1. **Choose Login Method**: SSO or Local authentication
2. **SSO Login**: Select organization from configured SSO providers
3. **Local Login**: Use username/password (demo accepts any credentials)

### Service Configuration
1. **Access**: Navigate to "Service Config" tab
2. **Add Service**: Click "Add Service" button
3. **Configure**: Set paths, prefixes, patterns, and validation rules
4. **Test Validation**: Use the validation testing feature
5. **Manage**: Enable/disable, edit, or delete services

### SSO Configuration
1. **Access**: Navigate to "SSO Config" tab (admin access)
2. **Add Provider**: Click "Add SSO Provider" button
3. **Configure**: Set organization details and provider settings
4. **Attribute Mapping**: Map SSO attributes to local fields
5. **Test**: Use connection testing feature
6. **Manage**: Enable/disable or modify configurations

### File Monitoring
1. **Dashboard**: View overall statistics and recent activity
2. **File Transfers**: Monitor all transfers with filtering options
3. **Service Management**: View service health and statistics
4. **Operations**: Retry failed transfers or cancel pending ones

## API Documentation

### Service Configuration Endpoints

- `GET /api/services` - Get all services
- `POST /api/services` - Create new service
- `PUT /api/services/{id}` - Update service
- `DELETE /api/services/{id}` - Delete service
- `POST /api/services/{id}/toggle` - Toggle service status
- `POST /api/services/validate-file` - Test file validation

### SSO Configuration Endpoints

- `GET /api/sso` - Get all SSO configurations
- `POST /api/sso` - Create new SSO configuration
- `PUT /api/sso/{id}` - Update SSO configuration
- `DELETE /api/sso/{id}` - Delete SSO configuration
- `POST /api/sso/{id}/test` - Test SSO connection

### Authentication Endpoints

- `POST /api/auth/login` - Local login
- `GET /api/auth/sso/{orgId}` - SSO login redirect
- `POST /api/auth/logout` - Logout
- `GET /api/auth/user` - Get current user

## Security Features

### Authentication
- **Multi-Provider SSO**: Support for major enterprise SSO providers
- **Local Authentication**: Fallback local authentication option
- **JWT Tokens**: Secure token-based authentication
- **Session Management**: Proper session handling and timeout

### Authorization
- **Role-based Access**: Different access levels for different users
- **Organization Isolation**: Users see only their organization's data
- **API Security**: Protected endpoints with proper authentication

### Data Protection
- **Encrypted Storage**: SSO credentials stored securely
- **Input Validation**: Comprehensive input validation on all endpoints
- **SQL Injection Protection**: Parameterized queries and JPA protection
- **XSS Protection**: Frontend input sanitization

## Development

### Building Individual Components

**Spring Boot Applications**:
```bash
cd file-transfer-batch
./mvnw clean package

cd file-transfer-web
./mvnw clean package
```

**React Frontend**:
```bash
cd file-transfer-frontend
npm install
npm start
```

### Testing

Sample data is automatically inserted during database initialization for testing purposes, including:
- Sample service configurations
- Sample SSO configuration
- Sample file transfer records

## Production Deployment

### Kubernetes Deployment

1. **Edit the manifests in `k8s/` as needed for your environment (e.g., image tags, resource requests, ingress settings).**
2. **Apply the manifests:**
   ```bash
   kubectl apply -f k8s/
   ```
3. **Monitor the pods and services:**
   ```bash
   kubectl get pods -n file-transfer
   kubectl get svc -n file-transfer
   ```
4. **Access the application via your configured ingress or service endpoint.**

### Environment Variables

Set environment variables in your Kubernetes manifests or use ConfigMaps/Secrets for sensitive data:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: file-transfer-config
  namespace: file-transfer
data:
  database-url: "jdbc:mysql://mysql:3306/filetransfer"
  database-username: "filetransfer"
---
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

### Security Considerations

1. **Change Default Credentials**: Update all default passwords and secrets
2. **Enable HTTPS**: Use SSL/TLS for all communications
3. **Configure Firewall**: Restrict access to necessary ports only
4. **Regular Updates**: Keep all dependencies updated
5. **Backup Strategy**: Implement regular database backups
6. **Monitoring**: Set up application and infrastructure monitoring

## Troubleshooting

### Common Issues

1. **SSO Login Fails**: Check SSO configuration and provider settings
2. **File Validation Errors**: Verify regex patterns are correctly formatted
3. **Service Not Processing Files**: Check service status and path configuration
4. **Database Connection Issues**: Verify database credentials and network connectivity

### Debug Mode

Enable debug logging by setting:
```yaml
logging:
  level:
    com.filetransfer: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test thoroughly
4. Update documentation
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🎉 **Enhanced Features Summary**

✅ **Dynamic Service Management** - Full CRUD operations through UI
✅ **Advanced File Validation** - Regex-based validation with testing
✅ **SSO Authentication** - Multi-provider SSO with organization support
✅ **Configurable File Prefixes** - Dynamic SOT/EOT prefix configuration
✅ **Enhanced Security** - JWT authentication and encrypted credential storage
✅ **Modern UI/UX** - Intuitive interface for all configuration operations
✅ **Real-time Testing** - Test configurations before deployment
✅ **Organization Management** - Multi-tenant SSO configuration
✅ **Comprehensive API** - RESTful endpoints for all operations
✅ **Production Ready** - Complete deployment and security documentation