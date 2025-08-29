# File Transfer System - Azure Terraform Variables
# Infrastructure configuration variables for Azure enterprise deployment

variable "azure_region" {
  description = "Azure region for infrastructure deployment"
  type        = string
  default     = "East US"
  
  validation {
    condition = contains([
      "East US", "East US 2", "West US", "West US 2", "West US 3",
      "Central US", "North Central US", "South Central US", "West Central US",
      "Canada Central", "Canada East",
      "North Europe", "West Europe", "UK South", "UK West",
      "Germany West Central", "Switzerland North",
      "Australia East", "Australia Southeast", "Japan East", "Japan West",
      "Korea Central", "Southeast Asia", "East Asia"
    ], var.azure_region)
    error_message = "Azure region must be a valid Azure region."
  }
}

variable "environment" {
  description = "Environment name (staging, production, etc.)"
  type        = string
  
  validation {
    condition     = contains(["staging", "production", "development"], var.environment)
    error_message = "Environment must be one of: staging, production, development."
  }
}

variable "kubernetes_version" {
  description = "Kubernetes version for AKS cluster"
  type        = string
  default     = "1.28.3"
}

# Network Configuration
variable "vnet_cidr" {
  description = "CIDR block for Virtual Network"
  type        = string
  default     = "10.0.0.0/16"
}

variable "aks_subnet_cidr" {
  description = "CIDR block for AKS subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "database_subnet_cidr" {
  description = "CIDR block for database subnet"
  type        = string
  default     = "10.0.2.0/24"
}

variable "appgw_subnet_cidr" {
  description = "CIDR block for Application Gateway subnet"
  type        = string
  default     = "10.0.3.0/24"
}

# Database Configuration - PostgreSQL
variable "db_sku_name" {
  description = "PostgreSQL Flexible Server SKU name"
  type        = string
  default     = "GP_Standard_D8s_v3"  # Enterprise: 8 vCPU, 32 GB RAM
  
  validation {
    condition = can(regex("^(B|GP|MO)_Standard_[A-Za-z0-9_]+$", var.db_sku_name))
    error_message = "DB SKU name must be a valid Azure PostgreSQL Flexible Server SKU."
  }
}

variable "db_storage_mb" {
  description = "PostgreSQL storage in MB"
  type        = number
  default     = 1048576  # 1TB for enterprise
  
  validation {
    condition     = var.db_storage_mb >= 32768 && var.db_storage_mb <= 33554432
    error_message = "DB storage must be between 32 GB and 32 TB."
  }
}

# Database Configuration - SQL Managed Instance
variable "use_sql_mi" {
  description = "Use Azure SQL Managed Instance instead of PostgreSQL"
  type        = bool
  default     = false
}

variable "sql_mi_sku_name" {
  description = "SQL Managed Instance SKU name"
  type        = string
  default     = "GP_Gen5"  # General Purpose, Gen5
  
  validation {
    condition = contains([
      "GP_Gen4", "GP_Gen5", "BC_Gen4", "BC_Gen5"
    ], var.sql_mi_sku_name)
    error_message = "SQL MI SKU must be one of: GP_Gen4, GP_Gen5, BC_Gen4, BC_Gen5."
  }
}

variable "sql_mi_vcores" {
  description = "SQL Managed Instance vCores"
  type        = number
  default     = 16  # Enterprise: 16 vCores
  
  validation {
    condition = contains([
      4, 8, 16, 24, 32, 40, 64, 80
    ], var.sql_mi_vcores)
    error_message = "SQL MI vCores must be one of: 4, 8, 16, 24, 32, 40, 64, 80."
  }
}

variable "sql_mi_storage_gb" {
  description = "SQL Managed Instance storage in GB"
  type        = number
  default     = 2048  # 2TB for enterprise
  
  validation {
    condition     = var.sql_mi_storage_gb >= 32 && var.sql_mi_storage_gb <= 16384
    error_message = "SQL MI storage must be between 32 GB and 16 TB."
  }
}

variable "sql_mi_license_type" {
  description = "SQL Managed Instance license type"
  type        = string
  default     = "LicenseIncluded"
  
  validation {
    condition     = contains(["LicenseIncluded", "BasePrice"], var.sql_mi_license_type)
    error_message = "SQL MI license type must be LicenseIncluded or BasePrice."
  }
}

variable "sql_mi_admin_login" {
  description = "SQL Managed Instance administrator login"
  type        = string
  default     = "filetransferadmin"
}

variable "sql_mi_admin_password" {
  description = "SQL Managed Instance administrator password"
  type        = string
  sensitive   = true
}

variable "sql_mi_subnet_cidr" {
  description = "CIDR block for SQL MI subnet"
  type        = string
  default     = "10.0.4.0/24"
}

variable "enable_sql_mi_failover" {
  description = "Enable SQL MI failover group for DR"
  type        = bool
  default     = false
}

variable "enable_sql_mi_security_features" {
  description = "Enable SQL MI advanced security features"
  type        = bool
  default     = true
}

# Redis Configuration
variable "redis_capacity" {
  description = "Redis cache capacity"
  type        = number
  default     = 6  # Enterprise: 26 GB cache
  
  validation {
    condition     = contains([0, 1, 2, 3, 4, 5, 6], var.redis_capacity)
    error_message = "Redis capacity must be between 0 and 6."
  }
}

variable "redis_family" {
  description = "Redis cache family"
  type        = string
  default     = "P"  # Premium for enterprise
  
  validation {
    condition     = contains(["C", "P"], var.redis_family)
    error_message = "Redis family must be C (Basic/Standard) or P (Premium)."
  }
}

variable "redis_sku_name" {
  description = "Redis cache SKU name"
  type        = string
  default     = "Premium"
  
  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.redis_sku_name)
    error_message = "Redis SKU must be Basic, Standard, or Premium."
  }
}

# Application Gateway Configuration
variable "appgw_capacity" {
  description = "Application Gateway capacity"
  type        = number
  default     = 10  # Enterprise capacity
  
  validation {
    condition     = var.appgw_capacity >= 1 && var.appgw_capacity <= 125
    error_message = "Application Gateway capacity must be between 1 and 125."
  }
}

# Application Configuration
variable "web_app_replicas" {
  description = "Number of web application replicas"
  type        = number
  default     = 6  # Enterprise baseline
  
  validation {
    condition     = var.web_app_replicas >= 2 && var.web_app_replicas <= 100
    error_message = "Web app replicas must be between 2 and 100."
  }
}

variable "batch_app_replicas" {
  description = "Number of batch application replicas"
  type        = number
  default     = 4  # Enterprise batch processing
  
  validation {
    condition     = var.batch_app_replicas >= 1 && var.batch_app_replicas <= 50
    error_message = "Batch app replicas must be between 1 and 50."
  }
}

variable "frontend_app_replicas" {
  description = "Number of frontend application replicas"
  type        = number
  default     = 3  # Enterprise frontend
  
  validation {
    condition     = var.frontend_app_replicas >= 2 && var.frontend_app_replicas <= 20
    error_message = "Frontend app replicas must be between 2 and 20."
  }
}

# Performance Configuration
variable "enable_auto_scaling" {
  description = "Enable horizontal pod autoscaling"
  type        = bool
  default     = true
}

variable "cpu_target_utilization" {
  description = "Target CPU utilization for autoscaling (%)"
  type        = number
  default     = 70
  
  validation {
    condition     = var.cpu_target_utilization >= 50 && var.cpu_target_utilization <= 90
    error_message = "CPU target utilization must be between 50 and 90 percent."
  }
}

variable "memory_target_utilization" {
  description = "Target memory utilization for autoscaling (%)"
  type        = number
  default     = 80
  
  validation {
    condition     = var.memory_target_utilization >= 60 && var.memory_target_utilization <= 90
    error_message = "Memory target utilization must be between 60 and 90 percent."
  }
}

# Security Configuration
variable "enable_encryption" {
  description = "Enable encryption for data at rest and in transit"
  type        = bool
  default     = true
}

variable "ssl_certificate_name" {
  description = "Name of SSL certificate in Key Vault"
  type        = string
  default     = "file-transfer-ssl-cert"
}

# Monitoring and Alerting
variable "enable_enhanced_monitoring" {
  description = "Enable enhanced monitoring with Application Insights"
  type        = bool
  default     = true
}

variable "alert_email" {
  description = "Email address for critical alerts"
  type        = string
  default     = "alerts@example.com"
  
  validation {
    condition     = can(regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", var.alert_email))
    error_message = "Alert email must be a valid email address."
  }
}

variable "teams_webhook_url" {
  description = "Microsoft Teams webhook URL for notifications"
  type        = string
  default     = ""
  sensitive   = true
}

variable "alert_phone" {
  description = "Phone number for SMS alerts (without country code)"
  type        = string
  default     = ""
}

# Backup and Disaster Recovery
variable "enable_geo_replication" {
  description = "Enable geo-replication for disaster recovery"
  type        = bool
  default     = false  # Set to true for production
}

variable "backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 30
  
  validation {
    condition     = var.backup_retention_days >= 1 && var.backup_retention_days <= 365
    error_message = "Backup retention must be between 1 and 365 days."
  }
}

variable "dr_region" {
  description = "Disaster recovery region"
  type        = string
  default     = "West US 2"
}

# Cost Optimization
variable "enable_spot_instances" {
  description = "Enable spot instances for cost optimization (non-production)"
  type        = bool
  default     = false
}

variable "vm_sizes" {
  description = "Azure VM sizes for different workloads"
  type = object({
    web_vm_size      = string
    batch_vm_size    = string
    frontend_vm_size = string
  })
  
  default = {
    web_vm_size      = "Standard_D8s_v3"   # 8 vCPU, 32 GB RAM
    batch_vm_size    = "Standard_E16s_v3"  # 16 vCPU, 128 GB RAM
    frontend_vm_size = "Standard_B4ms"     # 4 vCPU, 16 GB RAM
  }
}

# Enterprise Features
variable "enable_enterprise_features" {
  description = "Enable enterprise-specific features"
  type        = bool
  default     = true
}

variable "compliance_mode" {
  description = "Compliance mode (SOC2, HIPAA, PCI, etc.)"
  type        = string
  default     = "SOC2"
  
  validation {
    condition     = contains(["SOC2", "HIPAA", "PCI", "GDPR", "NONE"], var.compliance_mode)
    error_message = "Compliance mode must be one of: SOC2, HIPAA, PCI, GDPR, NONE."
  }
}

variable "audit_logging_enabled" {
  description = "Enable comprehensive audit logging"
  type        = bool
  default     = true
}

# Development and Testing
variable "enable_debug_mode" {
  description = "Enable debug mode for development environments"
  type        = bool
  default     = false
}

variable "test_data_enabled" {
  description = "Enable test data seeding"
  type        = bool
  default     = false
}

# Azure-specific Configuration
variable "enable_azure_policy" {
  description = "Enable Azure Policy for governance"
  type        = bool
  default     = true
}

variable "enable_defender" {
  description = "Enable Microsoft Defender for containers"
  type        = bool
  default     = true
}

variable "log_analytics_retention_days" {
  description = "Log Analytics workspace retention in days"
  type        = number
  default     = 90
  
  validation {
    condition     = var.log_analytics_retention_days >= 30 && var.log_analytics_retention_days <= 730
    error_message = "Log Analytics retention must be between 30 and 730 days."
  }
}

# Tags
variable "additional_tags" {
  description = "Additional tags to apply to all Azure resources"
  type        = map(string)
  default     = {}
}