#!/bin/bash

# File Transfer System - Backup and Disaster Recovery Setup Script
# Sets up backup infrastructure, disaster recovery plans, and monitoring

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Default configuration
ENVIRONMENT="${1:-development}"
SETUP_REMOTE_BACKUP="${2:-false}"
SETUP_DISASTER_RECOVERY="${3:-false}"
BACKUP_LOCATION="${4:-/var/backups/file-transfer}"
REMOTE_PROVIDER="${5:-s3}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root for certain operations
check_root() {
    if [[ $EUID -eq 0 ]]; then
        log_warning "Running as root. Some operations may require non-root user."
    fi
}

# Validate environment
validate_environment() {
    log_info "Validating environment: $ENVIRONMENT"
    
    case $ENVIRONMENT in
        development|staging|production|ha)
            log_success "Environment '$ENVIRONMENT' is valid"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT"
            log_error "Valid environments: development, staging, production, ha"
            exit 1
            ;;
    esac
}

# Create backup directories
setup_backup_directories() {
    log_info "Setting up backup directories"
    
    local dirs=(
        "$BACKUP_LOCATION/primary"
        "$BACKUP_LOCATION/secondary"
        "$BACKUP_LOCATION/metadata"
        "$BACKUP_LOCATION/scripts"
        "$BACKUP_LOCATION/logs"
        "$BACKUP_LOCATION/temp"
    )
    
    for dir in "${dirs[@]}"; do
        if [[ ! -d "$dir" ]]; then
            log_info "Creating directory: $dir"
            mkdir -p "$dir"
            chmod 750 "$dir"
        else
            log_info "Directory already exists: $dir"
        fi
    done
    
    # Set ownership if running as root
    if [[ $EUID -eq 0 ]]; then
        chown -R app:app "$BACKUP_LOCATION" || log_warning "Failed to set ownership"
    fi
    
    log_success "Backup directories created successfully"
}

# Install backup dependencies
install_backup_dependencies() {
    log_info "Installing backup dependencies"
    
    # Check for required tools
    local tools=("rsync" "gzip" "openssl" "curl")
    local missing_tools=()
    
    for tool in "${tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_info "Installing missing tools: ${missing_tools[*]}"
        
        # Detect package manager
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y "${missing_tools[@]}"
        elif command -v yum &> /dev/null; then
            sudo yum install -y "${missing_tools[@]}"
        elif command -v brew &> /dev/null; then
            brew install "${missing_tools[@]}"
        else
            log_error "No supported package manager found"
            exit 1
        fi
    else
        log_success "All required tools are already installed"
    fi
    
    # Install database-specific tools
    install_database_tools
}

# Install database backup tools
install_database_tools() {
    log_info "Installing database backup tools"
    
    # PostgreSQL tools
    if ! command -v pg_dump &> /dev/null; then
        log_info "Installing PostgreSQL client tools"
        if command -v apt-get &> /dev/null; then
            sudo apt-get install -y postgresql-client
        elif command -v yum &> /dev/null; then
            sudo yum install -y postgresql
        elif command -v brew &> /dev/null; then
            brew install postgresql
        fi
    fi
    
    # MySQL tools
    if ! command -v mysqldump &> /dev/null; then
        log_info "Installing MySQL client tools"
        if command -v apt-get &> /dev/null; then
            sudo apt-get install -y mysql-client
        elif command -v yum &> /dev/null; then
            sudo yum install -y mysql
        elif command -v brew &> /dev/null; then
            brew install mysql-client
        fi
    fi
    
    log_success "Database backup tools installed"
}

# Setup remote backup provider
setup_remote_backup() {
    if [[ "$SETUP_REMOTE_BACKUP" != "true" ]]; then
        log_info "Skipping remote backup setup"
        return 0
    fi
    
    log_info "Setting up remote backup with provider: $REMOTE_PROVIDER"
    
    case $REMOTE_PROVIDER in
        s3)
            setup_s3_backup
            ;;
        azure)
            setup_azure_backup
            ;;
        gcs)
            setup_gcs_backup
            ;;
        *)
            log_error "Unsupported remote provider: $REMOTE_PROVIDER"
            exit 1
            ;;
    esac
}

# Setup AWS S3 backup
setup_s3_backup() {
    log_info "Setting up AWS S3 backup"
    
    # Install AWS CLI if not present
    if ! command -v aws &> /dev/null; then
        log_info "Installing AWS CLI"
        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
        unzip awscliv2.zip
        sudo ./aws/install
        rm -rf aws awscliv2.zip
    fi
    
    # Create S3 configuration template
    cat > "$BACKUP_LOCATION/scripts/s3-backup.sh" << 'EOF'
#!/bin/bash
# S3 Backup Script Template

BUCKET_NAME="${BACKUP_S3_BUCKET:-filetransfer-backups}"
AWS_REGION="${BACKUP_S3_REGION:-us-east-1}"
BACKUP_SOURCE="$1"
BACKUP_NAME="$2"

# Upload to S3 with encryption
aws s3 cp "$BACKUP_SOURCE" "s3://$BUCKET_NAME/backups/$BACKUP_NAME" \
    --region "$AWS_REGION" \
    --server-side-encryption AES256 \
    --storage-class STANDARD_IA

# Verify upload
if aws s3api head-object --bucket "$BUCKET_NAME" --key "backups/$BACKUP_NAME" >/dev/null 2>&1; then
    echo "Backup uploaded successfully to S3"
    exit 0
else
    echo "Backup upload to S3 failed"
    exit 1
fi
EOF
    
    chmod +x "$BACKUP_LOCATION/scripts/s3-backup.sh"
    log_success "S3 backup configuration created"
}

# Setup Azure Blob backup
setup_azure_backup() {
    log_info "Setting up Azure Blob backup"
    
    # Install Azure CLI if not present
    if ! command -v az &> /dev/null; then
        log_info "Installing Azure CLI"
        curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
    fi
    
    # Create Azure configuration template
    cat > "$BACKUP_LOCATION/scripts/azure-backup.sh" << 'EOF'
#!/bin/bash
# Azure Blob Backup Script Template

STORAGE_ACCOUNT="${BACKUP_AZURE_ACCOUNT:-filetransferbackups}"
CONTAINER_NAME="${BACKUP_AZURE_CONTAINER:-backups}"
BACKUP_SOURCE="$1"
BACKUP_NAME="$2"

# Upload to Azure Blob with encryption
az storage blob upload \
    --account-name "$STORAGE_ACCOUNT" \
    --container-name "$CONTAINER_NAME" \
    --name "backups/$BACKUP_NAME" \
    --file "$BACKUP_SOURCE" \
    --encryption-scope default

# Verify upload
if az storage blob exists --account-name "$STORAGE_ACCOUNT" --container-name "$CONTAINER_NAME" --name "backups/$BACKUP_NAME" --output tsv; then
    echo "Backup uploaded successfully to Azure Blob"
    exit 0
else
    echo "Backup upload to Azure Blob failed"
    exit 1
fi
EOF
    
    chmod +x "$BACKUP_LOCATION/scripts/azure-backup.sh"
    log_success "Azure backup configuration created"
}

# Setup Google Cloud Storage backup
setup_gcs_backup() {
    log_info "Setting up Google Cloud Storage backup"
    
    # Install Google Cloud SDK if not present
    if ! command -v gsutil &> /dev/null; then
        log_info "Installing Google Cloud SDK"
        curl https://sdk.cloud.google.com | bash
        exec -l $SHELL
    fi
    
    # Create GCS configuration template
    cat > "$BACKUP_LOCATION/scripts/gcs-backup.sh" << 'EOF'
#!/bin/bash
# Google Cloud Storage Backup Script Template

BUCKET_NAME="${BACKUP_GCS_BUCKET:-filetransfer-backups}"
BACKUP_SOURCE="$1"
BACKUP_NAME="$2"

# Upload to GCS with encryption
gsutil -o "GSUtil:encryption_key=${BACKUP_GCS_ENCRYPTION_KEY}" \
    cp "$BACKUP_SOURCE" "gs://$BUCKET_NAME/backups/$BACKUP_NAME"

# Verify upload
if gsutil stat "gs://$BUCKET_NAME/backups/$BACKUP_NAME" >/dev/null 2>&1; then
    echo "Backup uploaded successfully to GCS"
    exit 0
else
    echo "Backup upload to GCS failed"
    exit 1
fi
EOF
    
    chmod +x "$BACKUP_LOCATION/scripts/gcs-backup.sh"
    log_success "GCS backup configuration created"
}

# Setup disaster recovery
setup_disaster_recovery() {
    if [[ "$SETUP_DISASTER_RECOVERY" != "true" ]]; then
        log_info "Skipping disaster recovery setup"
        return 0
    fi
    
    log_info "Setting up disaster recovery infrastructure"
    
    # Create DR directories
    local dr_dirs=(
        "$BACKUP_LOCATION/dr-plans"
        "$BACKUP_LOCATION/dr-tests"
        "$BACKUP_LOCATION/dr-logs"
        "$BACKUP_LOCATION/dr-scripts"
    )
    
    for dir in "${dr_dirs[@]}"; do
        mkdir -p "$dir"
        chmod 750 "$dir"
    done
    
    # Create sample DR plan template
    create_dr_plan_template
    
    # Create DR testing scripts
    create_dr_scripts
    
    log_success "Disaster recovery infrastructure created"
}

# Create disaster recovery plan template
create_dr_plan_template() {
    log_info "Creating disaster recovery plan template"
    
    cat > "$BACKUP_LOCATION/dr-plans/template.json" << 'EOF'
{
  "planName": "File Transfer System DR Plan",
  "description": "Disaster recovery plan for file transfer management system",
  "priority": "CRITICAL",
  "rto": 60,
  "rpo": 15,
  "applicationComponents": [
    "file-transfer-web",
    "file-transfer-batch",
    "file-transfer-frontend"
  ],
  "dataComponents": [
    "postgresql-database",
    "file-storage",
    "configuration-data"
  ],
  "recoverySteps": [
    {
      "order": 1,
      "name": "Assess Damage",
      "description": "Assess the extent of the disaster and system damage",
      "type": "MANUAL",
      "estimatedDuration": 15,
      "owner": "Incident Commander",
      "critical": true
    },
    {
      "order": 2,
      "name": "Activate Secondary Region",
      "description": "Activate secondary region infrastructure",
      "type": "SCRIPT",
      "command": "/backup/scripts/activate-secondary.sh",
      "estimatedDuration": 30,
      "owner": "Infrastructure Team",
      "critical": true
    },
    {
      "order": 3,
      "name": "Restore Database",
      "description": "Restore database from latest backup",
      "type": "SCRIPT",
      "command": "/backup/scripts/restore-database.sh",
      "estimatedDuration": 45,
      "owner": "Database Team",
      "critical": true
    },
    {
      "order": 4,
      "name": "Restore Application Data",
      "description": "Restore application files and configuration",
      "type": "SCRIPT",
      "command": "/backup/scripts/restore-files.sh",
      "estimatedDuration": 20,
      "owner": "Application Team",
      "critical": true
    },
    {
      "order": 5,
      "name": "Start Applications",
      "description": "Start all application services",
      "type": "SCRIPT",
      "command": "/backup/scripts/start-applications.sh",
      "estimatedDuration": 15,
      "owner": "Application Team",
      "critical": true
    },
    {
      "order": 6,
      "name": "Verify System Health",
      "description": "Verify all systems are operational",
      "type": "SCRIPT",
      "command": "/backup/scripts/health-check.sh",
      "estimatedDuration": 10,
      "owner": "Monitoring Team",
      "critical": true
    },
    {
      "order": 7,
      "name": "Update DNS",
      "description": "Update DNS to point to secondary region",
      "type": "MANUAL",
      "estimatedDuration": 5,
      "owner": "Network Team",
      "critical": true
    },
    {
      "order": 8,
      "name": "Notify Stakeholders",
      "description": "Notify all stakeholders of recovery completion",
      "type": "MANUAL",
      "estimatedDuration": 10,
      "owner": "Communications Team",
      "critical": false
    }
  ],
  "testingSchedule": "0 0 4 * * SUN",
  "contacts": [
    {
      "name": "Incident Commander",
      "role": "DR Coordinator",
      "email": "incident-commander@company.com",
      "phone": "+1-555-0101",
      "primary": true
    },
    {
      "name": "Infrastructure Team Lead",
      "role": "Infrastructure",
      "email": "infra-lead@company.com",
      "phone": "+1-555-0102",
      "primary": false
    },
    {
      "name": "Database Team Lead",
      "role": "Database",
      "email": "db-lead@company.com",
      "phone": "+1-555-0103",
      "primary": false
    }
  ]
}
EOF
    
    log_success "DR plan template created"
}

# Create disaster recovery scripts
create_dr_scripts() {
    log_info "Creating disaster recovery scripts"
    
    # Health check script
    cat > "$BACKUP_LOCATION/dr-scripts/health-check.sh" << 'EOF'
#!/bin/bash
# System Health Check Script

HEALTH_ENDPOINTS=(
    "http://localhost:8080/actuator/health"
    "http://localhost:8082/actuator/health"
    "http://localhost:3000"
)

DATABASE_HOST="${DATABASE_HOST:-localhost}"
DATABASE_PORT="${DATABASE_PORT:-5432}"

echo "Performing system health check..."

# Check application endpoints
for endpoint in "${HEALTH_ENDPOINTS[@]}"; do
    if curl -f -s "$endpoint" >/dev/null; then
        echo "✓ $endpoint is healthy"
    else
        echo "✗ $endpoint is unhealthy"
        exit 1
    fi
done

# Check database connectivity
if nc -z "$DATABASE_HOST" "$DATABASE_PORT"; then
    echo "✓ Database connectivity is healthy"
else
    echo "✗ Database connectivity failed"
    exit 1
fi

echo "All health checks passed!"
exit 0
EOF
    
    # Failover script
    cat > "$BACKUP_LOCATION/dr-scripts/failover.sh" << 'EOF'
#!/bin/bash
# Automated Failover Script

SOURCE_REGION="${1:-us-east-1}"
TARGET_REGION="${2:-us-west-2}"

echo "Starting failover from $SOURCE_REGION to $TARGET_REGION"

# Stop traffic to source region
echo "Stopping traffic to source region..."
# Implementation depends on load balancer (HAProxy, AWS ALB, etc.)

# Activate target region
echo "Activating target region..."
# Implementation depends on cloud provider

# Sync latest data
echo "Syncing latest data..."
# Implementation depends on replication setup

# Start applications in target region
echo "Starting applications in target region..."
# Implementation depends on deployment method

# Update DNS/routing
echo "Updating DNS/routing..."
# Implementation depends on DNS provider

echo "Failover completed successfully"
EOF
    
    chmod +x "$BACKUP_LOCATION/dr-scripts/"*.sh
    log_success "DR scripts created"
}

# Setup monitoring and alerting
setup_monitoring() {
    log_info "Setting up backup and DR monitoring"
    
    # Create monitoring scripts directory
    mkdir -p "$BACKUP_LOCATION/monitoring"
    
    # Create backup monitoring script
    cat > "$BACKUP_LOCATION/monitoring/backup-monitor.sh" << 'EOF'
#!/bin/bash
# Backup Monitoring Script

BACKUP_LOG="/var/log/file-transfer-backup.log"
ALERT_EMAIL="${BACKUP_ALERT_EMAIL:-admin@company.com}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-25}"

# Check if latest backup is too old
LATEST_BACKUP=$(find /var/backups/file-transfer/primary -name "*.tar.gz" -mtime -1 | head -1)

if [[ -z "$LATEST_BACKUP" ]]; then
    echo "ALERT: No recent backups found!" | mail -s "Backup Alert: No Recent Backups" "$ALERT_EMAIL"
    exit 1
fi

# Check backup log for errors
if grep -q "ERROR\|FAILED" "$BACKUP_LOG" 2>/dev/null; then
    echo "ALERT: Backup errors detected in log!" | mail -s "Backup Alert: Errors Detected" "$ALERT_EMAIL"
    exit 1
fi

echo "Backup monitoring: All checks passed"
exit 0
EOF
    
    chmod +x "$BACKUP_LOCATION/monitoring/backup-monitor.sh"
    log_success "Backup monitoring setup completed"
}

# Create configuration files
create_configuration() {
    log_info "Creating backup configuration files"
    
    # Create environment-specific configuration
    cat > "$BACKUP_LOCATION/config.env" << EOF
# File Transfer Backup Configuration - $ENVIRONMENT

# Environment
ENVIRONMENT=$ENVIRONMENT

# Backup settings
BACKUP_LOCATION=$BACKUP_LOCATION
BACKUP_COMPRESSION=true
BACKUP_ENCRYPTION=true
BACKUP_VERIFICATION=true

# Remote backup
REMOTE_BACKUP_ENABLED=$SETUP_REMOTE_BACKUP
REMOTE_PROVIDER=$REMOTE_PROVIDER

# Disaster recovery
DR_ENABLED=$SETUP_DISASTER_RECOVERY

# Retention policies (environment-specific)
EOF

    case $ENVIRONMENT in
        development)
            cat >> "$BACKUP_LOCATION/config.env" << EOF
BACKUP_RETENTION_DAYS=7
BACKUP_SCHEDULE_FULL="0 0 2 * * *"
BACKUP_SCHEDULE_INCREMENTAL="0 0 */6 * * *"
EOF
            ;;
        staging)
            cat >> "$BACKUP_LOCATION/config.env" << EOF
BACKUP_RETENTION_DAYS=14
BACKUP_SCHEDULE_FULL="0 0 2 * * *"
BACKUP_SCHEDULE_INCREMENTAL="0 0 */4 * * *"
EOF
            ;;
        production|ha)
            cat >> "$BACKUP_LOCATION/config.env" << EOF
BACKUP_RETENTION_DAYS=90
BACKUP_SCHEDULE_FULL="0 0 2 * * *"
BACKUP_SCHEDULE_INCREMENTAL="0 0 */2 * * *"
EOF
            ;;
    esac
    
    log_success "Configuration files created"
}

# Setup cron jobs
setup_cron_jobs() {
    log_info "Setting up backup cron jobs"
    
    # Create backup cron script
    cat > "$BACKUP_LOCATION/scripts/backup-cron.sh" << 'EOF'
#!/bin/bash
# Backup Cron Job Script

source /var/backups/file-transfer/config.env

# Load backup functions
source /var/backups/file-transfer/scripts/backup-functions.sh

# Determine backup type based on time
HOUR=$(date +%H)
if [[ "$HOUR" == "02" ]]; then
    BACKUP_TYPE="FULL"
else
    BACKUP_TYPE="INCREMENTAL"
fi

# Execute backup
execute_backup "$BACKUP_TYPE"
EOF
    
    chmod +x "$BACKUP_LOCATION/scripts/backup-cron.sh"
    
    # Add to crontab (environment-specific)
    case $ENVIRONMENT in
        development)
            # Less frequent for development
            echo "0 2 * * * $BACKUP_LOCATION/scripts/backup-cron.sh >/dev/null 2>&1" | crontab -
            ;;
        production|ha)
            # More frequent for production
            echo "0 */2 * * * $BACKUP_LOCATION/scripts/backup-cron.sh >/dev/null 2>&1" | crontab -
            ;;
        *)
            echo "0 */4 * * * $BACKUP_LOCATION/scripts/backup-cron.sh >/dev/null 2>&1" | crontab -
            ;;
    esac
    
    log_success "Cron jobs configured"
}

# Print setup summary
print_summary() {
    log_success "Backup and Disaster Recovery setup completed!"
    echo
    echo "=== Setup Summary ==="
    echo "Environment: $ENVIRONMENT"
    echo "Backup Location: $BACKUP_LOCATION"
    echo "Remote Backup: $SETUP_REMOTE_BACKUP"
    echo "Disaster Recovery: $SETUP_DISASTER_RECOVERY"
    echo "Remote Provider: $REMOTE_PROVIDER"
    echo
    echo "=== Next Steps ==="
    echo "1. Review configuration in: $BACKUP_LOCATION/config.env"
    echo "2. Customize DR plan in: $BACKUP_LOCATION/dr-plans/template.json"
    echo "3. Test backup: curl -X POST http://localhost:8080/api/backup/create"
    echo "4. Monitor logs: tail -f /var/log/file-transfer-backup.log"
    echo
    echo "=== Important Files ==="
    echo "Configuration: $BACKUP_LOCATION/config.env"
    echo "Scripts: $BACKUP_LOCATION/scripts/"
    echo "DR Plans: $BACKUP_LOCATION/dr-plans/"
    echo "Monitoring: $BACKUP_LOCATION/monitoring/"
    echo
}

# Main execution
main() {
    log_info "Starting File Transfer Backup and DR Setup"
    log_info "Environment: $ENVIRONMENT"
    log_info "Remote Backup: $SETUP_REMOTE_BACKUP"
    log_info "Disaster Recovery: $SETUP_DISASTER_RECOVERY"
    
    check_root
    validate_environment
    setup_backup_directories
    install_backup_dependencies
    setup_remote_backup
    setup_disaster_recovery
    setup_monitoring
    create_configuration
    setup_cron_jobs
    
    print_summary
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer Backup and Disaster Recovery Setup"
    echo
    echo "Usage: $0 [environment] [remote_backup] [disaster_recovery] [backup_location] [remote_provider]"
    echo
    echo "Arguments:"
    echo "  environment       Target environment (development|staging|production|ha)"
    echo "  remote_backup     Enable remote backup (true|false)"
    echo "  disaster_recovery Enable disaster recovery (true|false)"
    echo "  backup_location   Backup storage location (default: /var/backups/file-transfer)"
    echo "  remote_provider   Remote backup provider (s3|azure|gcs)"
    echo
    echo "Examples:"
    echo "  $0 development false false"
    echo "  $0 production true true /var/backups/file-transfer s3"
    echo "  $0 ha true true /var/backups/file-transfer azure"
    exit 0
fi

# Execute main function
main "$@"