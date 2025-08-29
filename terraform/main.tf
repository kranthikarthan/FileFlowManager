# File Transfer System - Infrastructure as Code
# Terraform configuration for enterprise-scale infrastructure

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  backend "s3" {
    bucket = "file-transfer-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-east-1"
    
    dynamodb_table = "file-transfer-terraform-locks"
    encrypt        = true
  }
}

# Provider configurations
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "file-transfer-system"
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = "platform-team"
    }
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name]
    }
  }
}

# Local values
locals {
  cluster_name = "file-transfer-${var.environment}"
  
  # Enterprise-scale node groups
  node_groups = {
    web_nodes = {
      instance_types = ["c5.2xlarge", "c5.4xlarge"]  # CPU optimized for web workload
      capacity_type  = "ON_DEMAND"
      min_size       = 3
      max_size       = 20
      desired_size   = 6
      
      labels = {
        workload-type = "web-application"
        node-type     = "compute-optimized"
      }
      
      taints = []
    }
    
    batch_nodes = {
      instance_types = ["m5.4xlarge", "m5.8xlarge"]   # Memory optimized for batch workload
      capacity_type  = "SPOT"                         # Cost optimization for batch
      min_size       = 2
      max_size       = 50
      desired_size   = 5
      
      labels = {
        workload-type = "batch-processing"
        node-type     = "memory-optimized"
      }
      
      taints = [
        {
          key    = "workload-type"
          value  = "batch-processing"
          effect = "NO_SCHEDULE"
        }
      ]
    }
    
    frontend_nodes = {
      instance_types = ["t3.medium", "t3.large"]      # Burstable for frontend
      capacity_type  = "ON_DEMAND"
      min_size       = 2
      max_size       = 10
      desired_size   = 3
      
      labels = {
        workload-type = "frontend-application"
        node-type     = "general-purpose"
      }
      
      taints = []
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  filter {
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }
}

data "aws_caller_identity" "current" {}

# VPC Configuration
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "file-transfer-vpc-${var.environment}"
  cidr = var.vpc_cidr

  azs             = slice(data.aws_availability_zones.available.names, 0, 3)
  private_subnets = var.private_subnet_cidrs
  public_subnets  = var.public_subnet_cidrs

  enable_nat_gateway   = true
  enable_vpn_gateway   = false
  enable_dns_hostnames = true
  enable_dns_support   = true

  # Enterprise networking features
  enable_flow_log                      = true
  create_flow_log_cloudwatch_iam_role  = true
  create_flow_log_cloudwatch_log_group = true

  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = local.cluster_name
  cluster_version = var.kubernetes_version

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true

  # Enterprise cluster configuration
  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent = true
    }
    aws-ebs-csi-driver = {
      most_recent = true
    }
  }

  # Enterprise node groups
  eks_managed_node_groups = {
    for name, config in local.node_groups : name => {
      instance_types = config.instance_types
      capacity_type  = config.capacity_type
      
      min_size     = config.min_size
      max_size     = config.max_size
      desired_size = config.desired_size
      
      labels = config.labels
      taints = config.taints
      
      # Enterprise node configuration
      block_device_mappings = {
        xvda = {
          device_name = "/dev/xvda"
          ebs = {
            volume_size           = 100
            volume_type           = "gp3"
            iops                  = 3000
            throughput            = 150
            encrypted             = true
            delete_on_termination = true
          }
        }
      }
      
      # User data for enterprise optimization
      user_data = base64encode(templatefile("${path.module}/user-data/node-setup.sh", {
        cluster_name = local.cluster_name
        node_type    = config.labels.workload-type
      }))
    }
  }

  # Cluster security group rules
  cluster_security_group_additional_rules = {
    ingress_nodes_ephemeral_ports_tcp = {
      description                = "Node groups to cluster API"
      protocol                   = "tcp"
      from_port                  = 1025
      to_port                    = 65535
      type                       = "ingress"
      source_node_security_group = true
    }
  }

  # Node security group rules
  node_security_group_additional_rules = {
    ingress_self_all = {
      description = "Node to node all ports/protocols"
      protocol    = "-1"
      from_port   = 0
      to_port     = 0
      type        = "ingress"
      self        = true
    }
  }
}

# RDS Database - Enterprise Configuration
module "database" {
  source = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "file-transfer-db-${var.environment}"

  # Enterprise database configuration
  engine               = "postgres"
  engine_version       = "15.4"
  family              = "postgres15"
  major_engine_version = "15"
  instance_class       = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_encrypted     = true
  storage_type          = "gp3"
  iops                  = 12000
  storage_throughput    = 500

  db_name  = "filetransfer"
  username = "filetransfer_user"
  port     = 5432

  # Multi-AZ for enterprise availability
  multi_az               = var.environment == "production"
  publicly_accessible    = false
  vpc_security_group_ids = [module.database_security_group.security_group_id]
  
  db_subnet_group_name   = module.vpc.database_subnet_group
  
  # Enterprise backup configuration
  backup_retention_period = var.environment == "production" ? 30 : 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  # Performance Insights for enterprise monitoring
  performance_insights_enabled          = true
  performance_insights_retention_period = var.environment == "production" ? 731 : 7
  
  # Enhanced monitoring
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_enhanced_monitoring.arn
  
  # Enterprise parameters
  parameters = [
    {
      name  = "shared_buffers"
      value = "2GB"
    },
    {
      name  = "effective_cache_size"
      value = "8GB"
    },
    {
      name  = "work_mem"
      value = "256MB"
    },
    {
      name  = "maintenance_work_mem"
      value = "1GB"
    },
    {
      name  = "max_connections"
      value = "500"
    },
    {
      name  = "random_page_cost"
      value = "1.1"
    }
  ]

  tags = {
    Name = "file-transfer-database-${var.environment}"
  }
}

# ElastiCache Redis - Enterprise Configuration
module "redis" {
  source = "terraform-aws-modules/elasticache/aws"
  version = "~> 1.0"

  cluster_id           = "file-transfer-redis-${var.environment}"
  description          = "Redis cluster for file transfer system"
  
  node_type            = var.redis_node_type
  num_cache_nodes      = var.redis_num_nodes
  parameter_group_name = aws_elasticache_parameter_group.redis_params.name
  port                 = 6379
  
  # Enterprise Redis configuration
  engine_version       = "7.0"
  
  # Multi-AZ for enterprise availability
  az_mode              = var.redis_num_nodes > 1 ? "cross-az" : "single-az"
  
  # Security
  subnet_group_name    = aws_elasticache_subnet_group.redis.name
  security_group_ids   = [module.redis_security_group.security_group_id]
  
  # Enterprise backup
  snapshot_retention_limit = var.environment == "production" ? 7 : 1
  snapshot_window         = "03:00-05:00"
  
  # Maintenance
  maintenance_window = "sun:05:00-sun:07:00"
  
  # Monitoring
  notification_topic_arn = aws_sns_topic.alerts.arn
}

# Application Load Balancer
module "alb" {
  source = "terraform-aws-modules/alb/aws"
  version = "~> 8.0"

  name = "file-transfer-alb-${var.environment}"

  load_balancer_type = "application"
  
  vpc_id          = module.vpc.vpc_id
  subnets         = module.vpc.public_subnets
  security_groups = [module.alb_security_group.security_group_id]

  # Enterprise ALB configuration
  enable_deletion_protection = var.environment == "production"
  
  # Access logging for enterprise compliance
  access_logs = {
    bucket  = aws_s3_bucket.alb_logs.id
    prefix  = "alb-logs"
    enabled = true
  }

  target_groups = [
    {
      name             = "file-transfer-web-${var.environment}"
      backend_protocol = "HTTP"
      backend_port     = 8080
      target_type      = "ip"
      
      health_check = {
        enabled             = true
        healthy_threshold   = 2
        unhealthy_threshold = 3
        timeout             = 10
        interval            = 30
        path                = "/actuator/health"
        matcher             = "200"
        port                = "traffic-port"
        protocol            = "HTTP"
      }
    },
    {
      name             = "file-transfer-batch-${var.environment}"
      backend_protocol = "HTTP"
      backend_port     = 8082
      target_type      = "ip"
      
      health_check = {
        enabled             = true
        healthy_threshold   = 2
        unhealthy_threshold = 5
        timeout             = 15
        interval            = 60
        path                = "/actuator/health"
        matcher             = "200"
        port                = "traffic-port"
        protocol            = "HTTP"
      }
    }
  ]

  https_listeners = [
    {
      port               = 443
      protocol           = "HTTPS"
      certificate_arn    = aws_acm_certificate.main.arn
      target_group_index = 0
      
      action_type = "forward"
    }
  ]

  http_tcp_listeners = [
    {
      port        = 80
      protocol    = "HTTP"
      action_type = "redirect"
      redirect = {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  ]
}

# S3 Buckets for enterprise storage
resource "aws_s3_bucket" "file_storage" {
  bucket = "file-transfer-storage-${var.environment}-${random_id.bucket_suffix.hex}"
}

resource "aws_s3_bucket_versioning" "file_storage" {
  bucket = aws_s3_bucket.file_storage.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "file_storage" {
  bucket = aws_s3_bucket.file_storage.id

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = aws_kms_key.file_storage.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "file_storage" {
  bucket = aws_s3_bucket.file_storage.id

  rule {
    id     = "enterprise_lifecycle"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    transition {
      days          = 365
      storage_class = "DEEP_ARCHIVE"
    }

    expiration {
      days = var.environment == "production" ? 2555 : 365  # 7 years for production
    }
  }
}

# CloudWatch Log Groups for enterprise monitoring
resource "aws_cloudwatch_log_group" "application_logs" {
  for_each = toset(["web", "batch", "frontend"])
  
  name              = "/aws/eks/file-transfer-${each.key}-${var.environment}"
  retention_in_days = var.environment == "production" ? 90 : 30
  kms_key_id        = aws_kms_key.logs.arn
}

# SNS Topics for enterprise alerting
resource "aws_sns_topic" "alerts" {
  name = "file-transfer-alerts-${var.environment}"
  
  kms_master_key_id = aws_kms_key.sns.id
}

resource "aws_sns_topic_subscription" "email_alerts" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_sns_topic_subscription" "slack_alerts" {
  count = var.slack_webhook_url != "" ? 1 : 0
  
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "https"
  endpoint  = var.slack_webhook_url
}

# KMS Keys for enterprise encryption
resource "aws_kms_key" "file_storage" {
  description             = "KMS key for file storage encryption"
  deletion_window_in_days = 7
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })
}

resource "aws_kms_alias" "file_storage" {
  name          = "alias/file-transfer-storage-${var.environment}"
  target_key_id = aws_kms_key.file_storage.key_id
}

# Random ID for unique resource naming
resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# Security Groups
module "alb_security_group" {
  source = "terraform-aws-modules/security-group/aws"
  version = "~> 5.0"

  name        = "file-transfer-alb-${var.environment}"
  description = "Security group for Application Load Balancer"
  vpc_id      = module.vpc.vpc_id

  ingress_with_cidr_blocks = [
    {
      from_port   = 80
      to_port     = 80
      protocol    = "tcp"
      description = "HTTP"
      cidr_blocks = "0.0.0.0/0"
    },
    {
      from_port   = 443
      to_port     = 443
      protocol    = "tcp"
      description = "HTTPS"
      cidr_blocks = "0.0.0.0/0"
    }
  ]

  egress_with_cidr_blocks = [
    {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      description = "All outbound traffic"
      cidr_blocks = "0.0.0.0/0"
    }
  ]
}

module "database_security_group" {
  source = "terraform-aws-modules/security-group/aws"
  version = "~> 5.0"

  name        = "file-transfer-database-${var.environment}"
  description = "Security group for RDS database"
  vpc_id      = module.vpc.vpc_id

  ingress_with_source_security_group_id = [
    {
      from_port                = 5432
      to_port                  = 5432
      protocol                 = "tcp"
      description              = "PostgreSQL from EKS nodes"
      source_security_group_id = module.eks.node_security_group_id
    }
  ]
}

module "redis_security_group" {
  source = "terraform-aws-modules/security-group/aws"
  version = "~> 5.0"

  name        = "file-transfer-redis-${var.environment}"
  description = "Security group for Redis cluster"
  vpc_id      = module.vpc.vpc_id

  ingress_with_source_security_group_id = [
    {
      from_port                = 6379
      to_port                  = 6379
      protocol                 = "tcp"
      description              = "Redis from EKS nodes"
      source_security_group_id = module.eks.node_security_group_id
    }
  ]
}

# IAM Roles for enterprise security
resource "aws_iam_role" "rds_enhanced_monitoring" {
  name = "rds-monitoring-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  role       = aws_iam_role.rds_enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# Outputs for CI/CD integration
output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.eks.cluster_endpoint
}

output "cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "database_endpoint" {
  description = "RDS database endpoint"
  value       = module.database.db_instance_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "Redis cluster endpoint"
  value       = module.redis.cluster_address
}

output "load_balancer_dns" {
  description = "Load balancer DNS name"
  value       = module.alb.lb_dns_name
}

output "s3_bucket_name" {
  description = "S3 bucket for file storage"
  value       = aws_s3_bucket.file_storage.id
}