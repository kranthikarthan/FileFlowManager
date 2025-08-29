# File Transfer System - Azure Infrastructure as Code
# Terraform configuration for enterprise-scale Azure infrastructure

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 2.0"
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
  
  backend "azurerm" {
    resource_group_name  = "file-transfer-terraform-rg"
    storage_account_name = "filetransferterraform"
    container_name       = "terraform-state"
    key                  = "infrastructure.terraform.tfstate"
  }
}

# Provider configurations
provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = true
      recover_soft_deleted_key_vaults = true
    }
    
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azuread" {}

provider "kubernetes" {
  host                   = azurerm_kubernetes_cluster.main.kube_config.0.host
  client_certificate     = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_certificate)
  client_key             = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.cluster_ca_certificate)
}

provider "helm" {
  kubernetes {
    host                   = azurerm_kubernetes_cluster.main.kube_config.0.host
    client_certificate     = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_certificate)
    client_key             = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.cluster_ca_certificate)
  }
}

# Local values
locals {
  cluster_name = "file-transfer-aks-${var.environment}"
  common_tags = {
    Environment = var.environment
    Project     = "file-transfer-system"
    ManagedBy   = "terraform"
    Owner       = "platform-team"
  }
  
  # Enterprise-scale node pools
  node_pools = {
    web_nodes = {
      vm_size             = "Standard_D8s_v3"  # 8 vCPU, 32 GB RAM
      node_count          = 6
      min_count           = 3
      max_count           = 20
      enable_auto_scaling = true
      
      node_labels = {
        "workload-type" = "web-application"
        "node-type"     = "compute-optimized"
      }
      
      node_taints = []
    }
    
    batch_nodes = {
      vm_size             = "Standard_E16s_v3" # 16 vCPU, 128 GB RAM
      node_count          = 5
      min_count           = 2
      max_count           = 50
      enable_auto_scaling = true
      
      node_labels = {
        "workload-type" = "batch-processing"
        "node-type"     = "memory-optimized"
      }
      
      node_taints = [
        {
          key    = "workload-type"
          value  = "batch-processing"
          effect = "NoSchedule"
        }
      ]
    }
    
    frontend_nodes = {
      vm_size             = "Standard_B4ms"    # 4 vCPU, 16 GB RAM
      node_count          = 3
      min_count           = 2
      max_count           = 10
      enable_auto_scaling = true
      
      node_labels = {
        "workload-type" = "frontend-application"
        "node-type"     = "general-purpose"
      }
      
      node_taints = []
    }
  }
}

# Data sources
data "azurerm_client_config" "current" {}

data "azuread_client_config" "current" {}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = "file-transfer-${var.environment}"
  location = var.azure_region
  
  tags = local.common_tags
}

# Virtual Network
resource "azurerm_virtual_network" "main" {
  name                = "file-transfer-vnet-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  address_space       = [var.vnet_cidr]
  
  tags = local.common_tags
}

# Subnets
resource "azurerm_subnet" "aks" {
  name                 = "aks-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.aks_subnet_cidr]
}

resource "azurerm_subnet" "database" {
  name                 = "database-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.database_subnet_cidr]
  
  delegation {
    name = "database-delegation"
    
    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

resource "azurerm_subnet" "application_gateway" {
  name                 = "appgw-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.appgw_subnet_cidr]
}

# Network Security Groups
resource "azurerm_network_security_group" "aks" {
  name                = "file-transfer-aks-nsg-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  # Allow inbound traffic for AKS
  security_rule {
    name                       = "AllowHTTPS"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
  
  security_rule {
    name                       = "AllowHTTP"
    priority                   = 1002
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
  
  tags = local.common_tags
}

# AKS Cluster - Enterprise Configuration
resource "azurerm_kubernetes_cluster" "main" {
  name                = local.cluster_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = "file-transfer-${var.environment}"
  kubernetes_version  = var.kubernetes_version
  
  # Enterprise cluster configuration
  default_node_pool {
    name                = "system"
    node_count          = 3
    vm_size             = "Standard_D4s_v3"
    vnet_subnet_id      = azurerm_subnet.aks.id
    enable_auto_scaling = true
    min_count           = 3
    max_count           = 10
    
    # Enterprise node configuration
    os_disk_size_gb = 100
    os_disk_type    = "Premium_LRS"
    
    node_labels = {
      "node-type" = "system"
    }
  }
  
  # Identity configuration
  identity {
    type = "SystemAssigned"
  }
  
  # Network configuration
  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
    network_policy    = "azure"
  }
  
  # Enterprise features
  role_based_access_control_enabled = true
  
  azure_policy_enabled = true
  
  oms_agent {
    log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  }
  
  # Auto-upgrade for enterprise maintenance
  automatic_channel_upgrade = "stable"
  
  tags = local.common_tags
}

# Additional Node Pools for Enterprise Workloads
resource "azurerm_kubernetes_cluster_node_pool" "workload_pools" {
  for_each = local.node_pools
  
  name                  = each.key
  kubernetes_cluster_id = azurerm_kubernetes_cluster.main.id
  vm_size               = each.value.vm_size
  node_count            = each.value.node_count
  vnet_subnet_id        = azurerm_subnet.aks.id
  
  # Auto-scaling configuration
  enable_auto_scaling = each.value.enable_auto_scaling
  min_count           = each.value.min_count
  max_count           = each.value.max_count
  
  # Enterprise node configuration
  os_disk_size_gb = 200
  os_disk_type    = "Premium_LRS"
  
  node_labels = each.value.node_labels
  
  # Node taints for workload isolation
  dynamic "node_taints" {
    for_each = each.value.node_taints
    content {
      key    = node_taints.value.key
      value  = node_taints.value.value
      effect = node_taints.value.effect
    }
  }
  
  tags = local.common_tags
}

# Azure Database for PostgreSQL - Enterprise Configuration
resource "azurerm_postgresql_flexible_server" "main" {
  name                   = "file-transfer-db-${var.environment}"
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  version                = "15"
  
  # Enterprise database configuration
  sku_name               = var.db_sku_name
  storage_mb             = var.db_storage_mb
  
  # High availability for enterprise
  high_availability {
    mode = var.environment == "production" ? "ZoneRedundant" : "SameZone"
  }
  
  # Network configuration
  delegated_subnet_id = azurerm_subnet.database.id
  private_dns_zone_id = azurerm_private_dns_zone.database.id
  
  # Backup configuration
  backup_retention_days = var.environment == "production" ? 35 : 7
  
  # Maintenance window
  maintenance_window {
    day_of_week  = 0  # Sunday
    start_hour   = 3
    start_minute = 0
  }
  
  depends_on = [azurerm_private_dns_zone_virtual_network_link.database]
  
  tags = local.common_tags
}

# PostgreSQL Database
resource "azurerm_postgresql_flexible_server_database" "main" {
  name      = "filetransfer"
  server_id = azurerm_postgresql_flexible_server.main.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# PostgreSQL Configuration for Enterprise Performance
resource "azurerm_postgresql_flexible_server_configuration" "enterprise_config" {
  for_each = {
    "shared_buffers"                = "2GB"
    "effective_cache_size"          = "8GB"
    "work_mem"                      = "256MB"
    "maintenance_work_mem"          = "1GB"
    "max_connections"               = "500"
    "random_page_cost"              = "1.1"
    "checkpoint_completion_target"  = "0.9"
    "wal_buffers"                  = "64MB"
  }
  
  server_id = azurerm_postgresql_flexible_server.main.id
  name      = each.key
  value     = each.value
}

# Azure Cache for Redis - Enterprise Configuration
resource "azurerm_redis_cache" "main" {
  name                = "file-transfer-redis-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku_name
  
  # Enterprise Redis configuration
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # Redis configuration for enterprise performance
  redis_configuration {
    maxmemory_reserved = 200
    maxmemory_delta    = 200
    maxmemory_policy   = "allkeys-lru"
  }
  
  # Backup configuration
  redis_version = "6"
  
  tags = local.common_tags
}

# Azure Container Registry - Enterprise Configuration
resource "azurerm_container_registry" "main" {
  name                = "filetransferregistry${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Premium"  # Enterprise SKU
  admin_enabled       = false
  
  # Enterprise features
  public_network_access_enabled = false
  network_rule_bypass_option   = "AzureServices"
  
  # Geo-replication for enterprise DR
  dynamic "georeplications" {
    for_each = var.enable_geo_replication ? [var.dr_region] : []
    content {
      location                = georeplications.value
      zone_redundancy_enabled = true
    }
  }
  
  # Trust policy for enterprise security
  trust_policy {
    enabled = true
  }
  
  # Retention policy for cost management
  retention_policy {
    enabled = true
    days    = var.environment == "production" ? 30 : 7
  }
  
  tags = local.common_tags
}

# Azure Application Gateway - Enterprise Load Balancer
resource "azurerm_public_ip" "appgw" {
  name                = "file-transfer-appgw-pip-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  allocation_method   = "Static"
  sku                 = "Standard"
  zones               = ["1", "2", "3"]  # Zone redundancy
  
  tags = local.common_tags
}

resource "azurerm_application_gateway" "main" {
  name                = "file-transfer-appgw-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  # Enterprise SKU
  sku {
    name     = "WAF_v2"
    tier     = "WAF_v2"
    capacity = var.appgw_capacity
  }
  
  # Auto-scaling for enterprise load
  autoscale_configuration {
    min_capacity = 2
    max_capacity = 20
  }
  
  gateway_ip_configuration {
    name      = "gateway-ip-config"
    subnet_id = azurerm_subnet.application_gateway.id
  }
  
  frontend_port {
    name = "frontend-port-80"
    port = 80
  }
  
  frontend_port {
    name = "frontend-port-443"
    port = 443
  }
  
  frontend_ip_configuration {
    name                 = "frontend-ip-config"
    public_ip_address_id = azurerm_public_ip.appgw.id
  }
  
  backend_address_pool {
    name = "backend-pool-production"
  }
  
  backend_address_pool {
    name = "backend-pool-blue"
  }
  
  backend_address_pool {
    name = "backend-pool-green"
  }
  
  backend_address_pool {
    name = "backend-pool-canary"
  }
  
  backend_http_settings {
    name                  = "http-settings-production"
    cookie_based_affinity = "Disabled"
    path                  = "/"
    port                  = 8080
    protocol              = "Http"
    request_timeout       = 60
    
    probe_name = "health-probe"
  }
  
  http_listener {
    name                           = "listener-80"
    frontend_ip_configuration_name = "frontend-ip-config"
    frontend_port_name             = "frontend-port-80"
    protocol                       = "Http"
  }
  
  request_routing_rule {
    name                       = "routing-rule-main"
    rule_type                  = "Basic"
    http_listener_name         = "listener-80"
    backend_address_pool_name  = "backend-pool-production"
    backend_http_settings_name = "http-settings-production"
    priority                   = 100
  }
  
  probe {
    name                = "health-probe"
    protocol            = "Http"
    path                = "/actuator/health"
    host                = "127.0.0.1"
    interval            = 30
    timeout             = 30
    unhealthy_threshold = 3
  }
  
  # WAF configuration for enterprise security
  waf_configuration {
    enabled          = true
    firewall_mode    = "Prevention"
    rule_set_type    = "OWASP"
    rule_set_version = "3.2"
    
    # Enterprise WAF rules
    disabled_rule_group {
      rule_group_name = "REQUEST-920-PROTOCOL-ENFORCEMENT"
      rules           = ["920300", "920440"]
    }
  }
  
  tags = local.common_tags
}

# Azure Key Vault - Enterprise Secrets Management
resource "azurerm_key_vault" "main" {
  name                = "file-transfer-kv-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "premium"  # Enterprise SKU
  
  # Enterprise security configuration
  enabled_for_disk_encryption     = true
  enabled_for_deployment          = true
  enabled_for_template_deployment = true
  enable_rbac_authorization      = true
  purge_protection_enabled       = var.environment == "production"
  
  # Network access restrictions
  public_network_access_enabled = false
  
  network_acls {
    default_action = "Deny"
    bypass         = "AzureServices"
    
    virtual_network_subnet_ids = [
      azurerm_subnet.aks.id
    ]
  }
  
  tags = local.common_tags
}

# Log Analytics Workspace - Enterprise Monitoring
resource "azurerm_log_analytics_workspace" "main" {
  name                = "file-transfer-logs-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = var.environment == "production" ? 90 : 30
  
  tags = local.common_tags
}

# Application Insights - Enterprise APM
resource "azurerm_application_insights" "main" {
  name                = "file-transfer-insights-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  workspace_id        = azurerm_log_analytics_workspace.main.id
  application_type    = "web"
  
  # Enterprise monitoring configuration
  retention_in_days   = var.environment == "production" ? 90 : 30
  sampling_percentage = var.environment == "production" ? 100 : 50
  
  tags = local.common_tags
}

# Azure Storage Account - Enterprise File Storage
resource "azurerm_storage_account" "main" {
  name                     = "filetransfer${var.environment}${random_id.storage_suffix.hex}"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = var.environment == "production" ? "GRS" : "LRS"
  
  # Enterprise security
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  
  # Enterprise features
  blob_properties {
    versioning_enabled = true
    
    delete_retention_policy {
      days = var.environment == "production" ? 30 : 7
    }
    
    container_delete_retention_policy {
      days = var.environment == "production" ? 30 : 7
    }
  }
  
  tags = local.common_tags
}

# Storage containers
resource "azurerm_storage_container" "file_storage" {
  name                  = "file-storage"
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"
}

resource "azurerm_storage_container" "backup_storage" {
  name                  = "backup-storage"
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"
}

# Random ID for unique resource naming
resource "random_id" "storage_suffix" {
  byte_length = 4
}

# Private DNS Zone for Database
resource "azurerm_private_dns_zone" "database" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name
  
  tags = local.common_tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "database" {
  name                  = "database-vnet-link"
  private_dns_zone_name = azurerm_private_dns_zone.database.name
  resource_group_name   = azurerm_resource_group.main.name
  virtual_network_id    = azurerm_virtual_network.main.id
  
  tags = local.common_tags
}

# Azure Monitor Action Groups for Enterprise Alerting
resource "azurerm_monitor_action_group" "main" {
  name                = "file-transfer-alerts-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  short_name          = "ft-alerts"
  
  # Email notifications
  email_receiver {
    name          = "admin-email"
    email_address = var.alert_email
  }
  
  # Teams webhook notifications
  dynamic "webhook_receiver" {
    for_each = var.teams_webhook_url != "" ? [1] : []
    content {
      name        = "teams-webhook"
      service_uri = var.teams_webhook_url
    }
  }
  
  # SMS notifications for critical alerts
  dynamic "sms_receiver" {
    for_each = var.alert_phone != "" ? [1] : []
    content {
      name         = "admin-sms"
      country_code = "1"
      phone_number = var.alert_phone
    }
  }
  
  tags = local.common_tags
}

# Outputs for CI/CD integration
output "cluster_name" {
  description = "AKS cluster name"
  value       = azurerm_kubernetes_cluster.main.name
}

output "cluster_endpoint" {
  description = "AKS cluster endpoint"
  value       = azurerm_kubernetes_cluster.main.kube_config.0.host
  sensitive   = true
}

output "resource_group_name" {
  description = "Azure resource group name"
  value       = azurerm_resource_group.main.name
}

output "database_fqdn" {
  description = "PostgreSQL database FQDN"
  value       = azurerm_postgresql_flexible_server.main.fqdn
  sensitive   = true
}

output "redis_hostname" {
  description = "Redis cache hostname"
  value       = azurerm_redis_cache.main.hostname
  sensitive   = true
}

output "container_registry_login_server" {
  description = "Azure Container Registry login server"
  value       = azurerm_container_registry.main.login_server
}

output "application_gateway_public_ip" {
  description = "Application Gateway public IP"
  value       = azurerm_public_ip.appgw.ip_address
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.main.vault_uri
}

output "application_insights_instrumentation_key" {
  description = "Application Insights instrumentation key"
  value       = azurerm_application_insights.main.instrumentation_key
  sensitive   = true
}

output "storage_account_name" {
  description = "Storage account name"
  value       = azurerm_storage_account.main.name
}