# File Transfer Management System

A comprehensive full-stack application for managing file transfers between different services with configurable folders, start/end transmission markers, and real-time monitoring capabilities.

## Architecture Overview

The system consists of three main components:

1. **Spring Boot Batch Application** - Handles file monitoring and batch processing
2. **Spring Boot Web Application** - Provides REST API for file transfer management
3. **React Frontend** - User interface for monitoring and managing file transfers

## Features

### Core Functionality
- **Automated File Monitoring**: Monitors configurable folders for incoming files
- **Start/End Transmission Markers**: Supports SOT/EOT files for each data transfer
- **Multiple Service Types**: Configurable services with different file patterns and paths
- **File Transfer Tracking**: Complete audit trail of all file transfers
- **Real-time Status Updates**: Live monitoring of transfer progress

### Management Capabilities
- **File Status Inquiry**: Query transfers by service, status, date range, etc.
- **Transfer Operations**: Retry failed transfers, cancel pending transfers
- **Service Management**: Enable/disable services, view service health
- **Dashboard Analytics**: Visual insights into transfer statistics

### Technical Features
- **Containerized Deployment**: Full Docker support with Docker Compose
- **Database Integration**: MySQL for persistent storage
- **RESTful API**: Comprehensive REST endpoints for all operations
- **Modern UI**: React with Material-UI components
- **Configurable**: YAML-based configuration for services and paths

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd file-transfer-system
   ```

2. **Start the application**
   ```bash
   chmod +x scripts/start.sh
   ./scripts/start.sh
   ```

3. **Access the application**
   - Frontend: http://localhost:3000
   - Web API: http://localhost:8080
   - Batch App: http://localhost:8081
   - Database: localhost:3306

### Stopping the Application
```bash
chmod +x scripts/stop.sh
./scripts/stop.sh
```

### Complete Cleanup
```bash
chmod +x scripts/cleanup.sh
./scripts/cleanup.sh
```

## Application Components

### 1. Spring Boot Batch Application (Port 8081)

**Location**: `file-transfer-batch/`

**Responsibilities**:
- File system monitoring for inbound directories
- Processing start/end transmission markers
- File transfer execution from inbound to outbound folders
- Batch job scheduling and execution

**Key Configuration**:
```yaml
file-transfer:
  enabled: true
  poll-interval-seconds: 30
  services:
    service1:
      inbound-path: /app/data/inbound/service1
      outbound-path: /app/data/outbound/service1
      start-marker-prefix: SOT_
      end-marker-prefix: EOT_
```

### 2. Spring Boot Web Application (Port 8080)

**Location**: `file-transfer-web/`

**Responsibilities**:
- REST API for file transfer management
- Database operations and queries
- Transfer retry/cancel operations
- Service status management

**Key Endpoints**:
- `GET /api/file-transfers` - List all transfers
- `GET /api/file-transfers/service/{serviceType}` - Transfers by service
- `POST /api/file-transfers/{id}/retry` - Retry transfer
- `POST /api/file-transfers/{id}/cancel` - Cancel transfer

### 3. React Frontend (Port 3000)

**Location**: `file-transfer-frontend/`

**Features**:
- Dashboard with transfer statistics and charts
- File transfer list with filtering and sorting
- Service management interface
- Real-time status updates
- Responsive Material-UI design

## Configuration

### Service Configuration

Services are configured in the `application.yml` files. Each service can have:

- **inbound-path**: Directory to monitor for incoming files
- **outbound-path**: Destination directory for processed files
- **start-marker-prefix**: Prefix for start of transmission files
- **end-marker-prefix**: Prefix for end of transmission files
- **data-file-pattern**: Pattern for data files (e.g., "*.dat", "*.xml")
- **enabled**: Whether the service is active
- **max-retries**: Maximum retry attempts for failed transfers

### Database Configuration

The system uses MySQL for data persistence. The database schema is automatically created during startup.

**Tables**:
- `file_transfer_records`: Main table for tracking all file transfers

### Environment Variables

For Docker deployment, configure these environment variables:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/filetransfer
  SPRING_DATASOURCE_USERNAME: filetransfer
  SPRING_DATASOURCE_PASSWORD: filetransfer123
```

## File Transfer Workflow

1. **File Detection**: Batch application monitors inbound directories
2. **Start Marker Processing**: When SOT file is detected, system identifies corresponding data files
3. **End Marker Waiting**: Data files wait for corresponding EOT file
4. **Transfer Execution**: Once EOT is received, files are transferred to outbound directory
5. **Status Tracking**: All operations are logged in the database
6. **Web Interface**: Users can monitor progress and manage transfers

## Monitoring and Management

### Dashboard
- Total transfers summary
- Recent activity (24h)
- Status distribution charts
- Service-wise statistics

### File Transfer List
- Comprehensive list with filtering
- Status-based filtering (Pending, Completed, Failed, etc.)
- Service and direction filtering
- Retry/Cancel operations

### Service Management
- Service health monitoring
- Enable/disable services
- Service statistics
- Start/stop operations

## Development

### Building Individual Components

**Spring Boot Applications**:
```bash
cd file-transfer-batch
./mvnw clean package
```

**React Frontend**:
```bash
cd file-transfer-frontend
npm install
npm start
```

### Running Locally

1. Start MySQL database
2. Update configuration files with local database settings
3. Run Spring Boot applications
4. Start React development server

### Testing

Sample data is automatically inserted during database initialization for testing purposes.

## API Documentation

### File Transfer Endpoints

- `GET /api/file-transfers` - Get all file transfers
- `GET /api/file-transfers/{id}` - Get specific transfer
- `GET /api/file-transfers/service/{serviceType}` - Get transfers by service
- `GET /api/file-transfers/status/{status}` - Get transfers by status
- `GET /api/file-transfers/direction/{direction}` - Get transfers by direction
- `GET /api/file-transfers/date-range` - Get transfers by date range
- `POST /api/file-transfers/{id}/retry` - Retry failed transfer
- `POST /api/file-transfers/{id}/cancel` - Cancel pending transfer
- `GET /api/file-transfers/services` - Get all service types

### Transfer Statuses

- `PENDING` - Transfer queued for processing
- `IN_PROGRESS` - Transfer currently being processed
- `COMPLETED` - Transfer completed successfully
- `FAILED` - Transfer failed (can be retried)
- `CANCELLED` - Transfer was cancelled
- `WAITING_FOR_END_MARKER` - Waiting for EOT file

## Troubleshooting

### Common Issues

1. **Services not starting**: Check Docker logs with `docker-compose logs [service-name]`
2. **Database connection issues**: Ensure MySQL container is healthy
3. **File permissions**: Verify Docker has access to mounted volumes
4. **Port conflicts**: Ensure ports 3000, 8080, 8081, 3306 are available

### Logs

View logs for specific services:
```bash
docker-compose logs -f file-transfer-batch
docker-compose logs -f file-transfer-web
docker-compose logs -f file-transfer-frontend
```

### Health Checks

- Web API: http://localhost:8080/actuator/health
- Batch App: http://localhost:8081/actuator/health
- Database: Check MySQL container health

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test thoroughly
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.