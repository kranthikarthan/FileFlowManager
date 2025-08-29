# Azure Staging Environment Configuration
# Optimized for development and testing with cost efficiency

environment = "staging"
azure_region = "East US"

# Network Configuration
vnet_cidr = "10.1.0.0/16"
aks_subnet_cidr = "10.1.1.0/24"
database_subnet_cidr = "10.1.2.0/24"
appgw_subnet_cidr = "10.1.3.0/24"

# Database Configuration - Staging Scale
db_sku_name = "GP_Standard_D4s_v3"  # 4 vCPU, 16 GB RAM
db_storage_mb = 262144               # 256 GB storage

# Redis Configuration - Staging Scale
redis_capacity = 2                   # 6 GB cache
redis_family = "P"                   # Premium for testing enterprise features
redis_sku_name = "Premium"

# Application Gateway Configuration
appgw_capacity = 2                   # Smaller capacity for staging

# Application Replicas - Staging Scale
web_app_replicas = 2
batch_app_replicas = 1
frontend_app_replicas = 2

# Performance Configuration
enable_auto_scaling = true
cpu_target_utilization = 70
memory_target_utilization = 80

# Security Configuration
enable_encryption = true
ssl_certificate_name = "file-transfer-staging-ssl"

# Monitoring Configuration
enable_enhanced_monitoring = true
alert_email = "staging-alerts@example.com"
teams_webhook_url = ""  # Optional for staging

# Backup and DR Configuration
enable_geo_replication = false       # Disabled for cost savings
backup_retention_days = 7            # Shorter retention for staging
dr_region = "West US 2"

# Cost Optimization
enable_spot_instances = true         # Use spot instances for cost savings

# VM Sizes - Cost-optimized for staging
vm_sizes = {
  web_vm_size      = "Standard_D4s_v3"   # 4 vCPU, 16 GB RAM
  batch_vm_size    = "Standard_E8s_v3"   # 8 vCPU, 64 GB RAM
  frontend_vm_size = "Standard_B2ms"     # 2 vCPU, 8 GB RAM
}

# Enterprise Features - Limited for staging
enable_enterprise_features = true
compliance_mode = "NONE"             # No compliance requirements for staging
audit_logging_enabled = false       # Disabled for cost savings

# Development and Testing
enable_debug_mode = true
test_data_enabled = true

# Azure-specific Configuration
enable_azure_policy = false         # Disabled for staging flexibility
enable_defender = false             # Disabled for cost savings
log_analytics_retention_days = 30   # Shorter retention for staging

# Additional tags for staging
additional_tags = {
  Environment = "staging"
  CostCenter  = "development"
  Owner       = "dev-team"
  Purpose     = "testing"
}