# File Transfer System - Terraform Variables
# Infrastructure configuration variables for enterprise deployment

variable "aws_region" {
  description = "AWS region for infrastructure deployment"
  type        = string
  default     = "us-east-1"
  
  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.aws_region))
    error_message = "AWS region must be in the format 'us-east-1'."
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
  description = "Kubernetes version for EKS cluster"
  type        = string
  default     = "1.28"
}

# Network Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
}

# Database Configuration
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.r5.2xlarge"  # Enterprise-grade instance
  
  validation {
    condition = can(regex("^db\\.[a-z0-9]+\\.[a-z0-9]+$", var.db_instance_class))
    error_message = "DB instance class must be a valid RDS instance type."
  }
}

variable "db_allocated_storage" {
  description = "Initial allocated storage for RDS (GB)"
  type        = number
  default     = 500  # 500GB for enterprise
  
  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 65536
    error_message = "DB allocated storage must be between 20 and 65536 GB."
  }
}

variable "db_max_allocated_storage" {
  description = "Maximum allocated storage for RDS auto-scaling (GB)"
  type        = number
  default     = 2000  # 2TB max for enterprise
  
  validation {
    condition     = var.db_max_allocated_storage >= var.db_allocated_storage
    error_message = "Max allocated storage must be greater than or equal to allocated storage."
  }
}

# Redis Configuration
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.r6g.2xlarge"  # Enterprise-grade Redis
}

variable "redis_num_nodes" {
  description = "Number of Redis nodes"
  type        = number
  default     = 3  # Multi-node for enterprise availability
  
  validation {
    condition     = var.redis_num_nodes >= 1 && var.redis_num_nodes <= 20
    error_message = "Redis node count must be between 1 and 20."
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

variable "ssl_certificate_arn" {
  description = "ARN of SSL certificate for HTTPS"
  type        = string
  default     = ""
}

# Monitoring and Alerting
variable "enable_enhanced_monitoring" {
  description = "Enable enhanced monitoring for RDS and ElastiCache"
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

variable "slack_webhook_url" {
  description = "Slack webhook URL for notifications"
  type        = string
  default     = ""
  sensitive   = true
}

# Backup and Disaster Recovery
variable "enable_cross_region_backup" {
  description = "Enable cross-region backup for disaster recovery"
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
  default     = "us-west-2"
}

# Cost Optimization
variable "enable_spot_instances" {
  description = "Enable spot instances for cost optimization (non-production)"
  type        = bool
  default     = false
}

variable "instance_types" {
  description = "EC2 instance types for different workloads"
  type = object({
    web_instances      = list(string)
    batch_instances    = list(string)
    frontend_instances = list(string)
  })
  
  default = {
    web_instances      = ["c5.2xlarge", "c5.4xlarge"]     # CPU optimized
    batch_instances    = ["m5.4xlarge", "m5.8xlarge"]     # Memory optimized
    frontend_instances = ["t3.medium", "t3.large"]        # Burstable
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

# External Integrations
variable "external_apis" {
  description = "Configuration for external API integrations"
  type = object({
    enable_third_party_apis = bool
    api_rate_limits        = map(number)
    api_timeout_seconds    = number
  })
  
  default = {
    enable_third_party_apis = true
    api_rate_limits = {
      "default" = 1000
      "premium" = 5000
    }
    api_timeout_seconds = 30
  }
}

# Tags
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}