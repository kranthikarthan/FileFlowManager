# Azure Production Environment Configuration
# Enterprise-scale configuration for production workloads

environment = "production"
azure_region = "East US"

# Network Configuration - Production Scale
vnet_cidr = "10.0.0.0/16"
aks_subnet_cidr = "10.0.1.0/24"
database_subnet_cidr = "10.0.2.0/24"
appgw_subnet_cidr = "10.0.3.0/24"

# Database Configuration - Choose PostgreSQL or SQL MI
# PostgreSQL Configuration
db_sku_name = "GP_Standard_D16s_v3" # 16 vCPU, 64 GB RAM
db_storage_mb = 2097152              # 2 TB storage

# Azure SQL Managed Instance Configuration (Alternative)
use_sql_mi = false                   # Set to true to use SQL MI instead of PostgreSQL
sql_mi_sku_name = "GP_Gen5"         # General Purpose Gen5
sql_mi_vcores = 32                  # 32 vCores for enterprise
sql_mi_storage_gb = 4096            # 4 TB storage for enterprise
sql_mi_license_type = "LicenseIncluded"
sql_mi_admin_login = "filetransferadmin"
sql_mi_admin_password = ""  # Configure via Azure Key Vault or environment variable
sql_mi_subnet_cidr = "10.0.4.0/24"
enable_sql_mi_failover = true       # Enable failover group for DR
enable_sql_mi_security_features = true

# Redis Configuration - Enterprise Scale
redis_capacity = 6                   # 26 GB cache
redis_family = "P"                   # Premium for enterprise
redis_sku_name = "Premium"

# Application Gateway Configuration - Enterprise Scale
appgw_capacity = 20                  # High capacity for enterprise load

# Application Replicas - Enterprise Scale
web_app_replicas = 10                # High availability
batch_app_replicas = 6               # Enterprise batch processing
frontend_app_replicas = 6            # High availability frontend

# Performance Configuration - Enterprise Tuned
enable_auto_scaling = true
cpu_target_utilization = 70
memory_target_utilization = 75       # Conservative for production

# Security Configuration - Enterprise Grade
enable_encryption = true
ssl_certificate_name = "file-transfer-production-ssl"

# Monitoring Configuration - Enterprise Level
enable_enhanced_monitoring = true
alert_email = "production-alerts@example.com"
teams_webhook_url = "https://outlook.office.com/webhook/..."  # Production Teams webhook
alert_phone = "5551234567"           # SMS alerts for critical issues

# Backup and DR Configuration - Enterprise
enable_geo_replication = true        # Cross-region for DR
backup_retention_days = 90           # Extended retention for compliance
dr_region = "West US 2"              # DR region

# Cost Optimization - Production Balanced
enable_spot_instances = false        # No spot instances for production reliability

# VM Sizes - Enterprise Performance
vm_sizes = {
  web_vm_size      = "Standard_D16s_v3"  # 16 vCPU, 64 GB RAM
  batch_vm_size    = "Standard_E32s_v3"  # 32 vCPU, 256 GB RAM
  frontend_vm_size = "Standard_D8s_v3"   # 8 vCPU, 32 GB RAM
}

# Enterprise Features - Full Suite
enable_enterprise_features = true
compliance_mode = "SOC2"             # SOC2 compliance for enterprise
audit_logging_enabled = true        # Full audit logging

# Development and Testing - Production Settings
enable_debug_mode = false            # No debug in production
test_data_enabled = false           # No test data in production

# Azure-specific Configuration - Enterprise
enable_azure_policy = true          # Governance and compliance
enable_defender = true              # Microsoft Defender for containers
log_analytics_retention_days = 90   # Extended retention for compliance

# Additional tags for production
additional_tags = {
  Environment     = "production"
  CostCenter      = "operations"
  Owner           = "platform-team"
  Purpose         = "production-workload"
  Compliance      = "SOC2"
  BackupRequired  = "true"
  MonitoringLevel = "enterprise"
  SLA             = "99.9"
}