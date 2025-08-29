# Azure SQL Managed Instance Configuration
# Enterprise-grade managed database service with SQL Server compatibility

# Azure SQL Managed Instance
resource "azurerm_mssql_managed_instance" "main" {
  name                = "file-transfer-sqlmi-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  
  # Enterprise licensing and SKU
  license_type   = var.sql_mi_license_type
  sku_name       = var.sql_mi_sku_name
  vcores         = var.sql_mi_vcores
  storage_size_in_gb = var.sql_mi_storage_gb
  
  # Administrator credentials
  administrator_login          = var.sql_mi_admin_login
  administrator_login_password = var.sql_mi_admin_password
  
  # Network configuration
  subnet_id = azurerm_subnet.sql_mi.id
  
  # Enterprise features
  collation                        = "SQL_Latin1_General_CP1_CI_AS"
  public_data_endpoint_enabled     = false  # Enterprise security
  minimum_tls_version             = "1.2"   # Enterprise security
  proxy_override                  = "Proxy" # Enterprise connectivity
  timezone_id                     = "UTC"   # Standardized timezone
  
  # Backup and maintenance
  maintenance_configuration_name = azurerm_maintenance_configuration.sql_mi.name
  
  # Identity for Azure integration
  identity {
    type = "SystemAssigned"
  }
  
  tags = local.common_tags
}

# SQL Managed Instance Database
resource "azurerm_mssql_managed_database" "main" {
  name         = "filetransfer"
  managed_instance_id = azurerm_mssql_managed_instance.main.id
  
  # Enterprise backup configuration
  short_term_retention_days = var.environment == "production" ? 35 : 7
  
  # Long-term retention for enterprise compliance
  long_term_retention_policy {
    weekly_retention  = var.environment == "production" ? "P12W" : "P4W"   # 12 weeks production, 4 weeks staging
    monthly_retention = var.environment == "production" ? "P12M" : "P3M"   # 12 months production, 3 months staging
    yearly_retention  = var.environment == "production" ? "P7Y" : "P1Y"    # 7 years production, 1 year staging
    week_of_year      = 1
  }
  
  tags = local.common_tags
}

# Dedicated subnet for SQL MI (required)
resource "azurerm_subnet" "sql_mi" {
  name                 = "sql-mi-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.sql_mi_subnet_cidr]
  
  delegation {
    name = "sql-mi-delegation"
    
    service_delegation {
      name = "Microsoft.Sql/managedInstances"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
        "Microsoft.Network/virtualNetworks/subnets/prepareNetworkPolicies/action",
        "Microsoft.Network/virtualNetworks/subnets/unprepareNetworkPolicies/action"
      ]
    }
  }
}

# Network Security Group for SQL MI
resource "azurerm_network_security_group" "sql_mi" {
  name                = "file-transfer-sqlmi-nsg-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  # Required rules for SQL MI
  security_rule {
    name                       = "AllowSqlMiManagement"
    priority                   = 1000
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_ranges    = ["9000", "9003", "1438", "1440", "1452"]
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
  
  security_rule {
    name                       = "AllowSqlMiData"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "1433"
    source_address_prefix      = "VirtualNetwork"
    destination_address_prefix = "*"
  }
  
  security_rule {
    name                       = "AllowAzureCloudOutbound"
    priority                   = 1000
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "AzureCloud"
  }
  
  tags = local.common_tags
}

# Associate NSG with SQL MI subnet
resource "azurerm_subnet_network_security_group_association" "sql_mi" {
  subnet_id                 = azurerm_subnet.sql_mi.id
  network_security_group_id = azurerm_network_security_group.sql_mi.id
}

# Route table for SQL MI (required)
resource "azurerm_route_table" "sql_mi" {
  name                = "file-transfer-sqlmi-rt-${var.environment}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  
  # Required routes for SQL MI
  route {
    name           = "SqlMiManagementService"
    address_prefix = "0.0.0.0/0"
    next_hop_type  = "Internet"
  }
  
  tags = local.common_tags
}

# Associate route table with SQL MI subnet
resource "azurerm_subnet_route_table_association" "sql_mi" {
  subnet_id      = azurerm_subnet.sql_mi.id
  route_table_id = azurerm_route_table.sql_mi.id
}

# Maintenance configuration for SQL MI
resource "azurerm_maintenance_configuration" "sql_mi" {
  name                = "file-transfer-sqlmi-maintenance-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  scope               = "SQLManagedInstance"
  
  # Enterprise maintenance window
  window {
    start_date_time = "2024-01-01 03:00"
    duration        = "03:00"  # 3 hours maintenance window
    time_zone       = "UTC"
    recur_every     = "1Week"
  }
  
  tags = local.common_tags
}

# Private DNS zone for SQL MI
resource "azurerm_private_dns_zone" "sql_mi" {
  name                = "privatelink.sql.azuresynapse.net"
  resource_group_name = azurerm_resource_group.main.name
  
  tags = local.common_tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "sql_mi" {
  name                  = "sql-mi-vnet-link"
  resource_group_name   = azurerm_resource_group.main.name
  private_dns_zone_name = azurerm_private_dns_zone.sql_mi.name
  virtual_network_id    = azurerm_virtual_network.main.id
  
  tags = local.common_tags
}

# Azure SQL MI Failover Group for Enterprise DR
resource "azurerm_mssql_managed_instance_failover_group" "main" {
  count = var.enable_sql_mi_failover ? 1 : 0
  
  name                        = "file-transfer-sqlmi-fog-${var.environment}"
  location                    = azurerm_resource_group.main.location
  managed_instance_name       = azurerm_mssql_managed_instance.main.name
  partner_managed_instance_id = azurerm_mssql_managed_instance.dr[0].id
  partner_region              = var.dr_region
  
  read_write_endpoint_failover_policy {
    mode          = "Automatic"
    grace_minutes = 60  # 1 hour grace period for enterprise
  }
  
  tags = local.common_tags
}

# DR SQL MI instance (conditional)
resource "azurerm_mssql_managed_instance" "dr" {
  count = var.enable_sql_mi_failover ? 1 : 0
  
  name                = "file-transfer-sqlmi-dr-${var.environment}"
  resource_group_name = azurerm_resource_group.dr[0].name
  location            = var.dr_region
  
  # Same configuration as primary
  license_type   = var.sql_mi_license_type
  sku_name       = var.sql_mi_sku_name
  vcores         = var.sql_mi_vcores
  storage_size_in_gb = var.sql_mi_storage_gb
  
  administrator_login          = var.sql_mi_admin_login
  administrator_login_password = var.sql_mi_admin_password
  
  subnet_id = azurerm_subnet.sql_mi_dr[0].id
  
  # Enterprise features
  collation                        = "SQL_Latin1_General_CP1_CI_AS"
  public_data_endpoint_enabled     = false
  minimum_tls_version             = "1.2"
  proxy_override                  = "Proxy"
  timezone_id                     = "UTC"
  
  tags = local.common_tags
}

# SQL MI Performance monitoring
resource "azurerm_monitor_diagnostic_setting" "sql_mi" {
  name               = "sql-mi-diagnostics"
  target_resource_id = azurerm_mssql_managed_instance.main.id
  
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  
  # Enable all log categories for enterprise monitoring
  enabled_log {
    category = "SQLInsights"
  }
  
  enabled_log {
    category = "QueryStoreRuntimeStatistics"
  }
  
  enabled_log {
    category = "QueryStoreWaitStatistics"
  }
  
  enabled_log {
    category = "Errors"
  }
  
  # Enable all metrics
  metric {
    category = "AllMetrics"
    enabled  = true
    
    retention_policy {
      enabled = true
      days    = var.environment == "production" ? 90 : 30
    }
  }
}

# SQL MI Security - Vulnerability Assessment
resource "azurerm_mssql_managed_instance_vulnerability_assessment" "main" {
  count = var.enable_sql_mi_security_features ? 1 : 0
  
  managed_instance_id = azurerm_mssql_managed_instance.main.id
  storage_container_path = "${azurerm_storage_account.main.primary_blob_endpoint}vulnerability-assessment"
  storage_account_access_key = azurerm_storage_account.main.primary_access_key
  
  recurring_scans {
    enabled                   = true
    email_subscription_admins = true
    emails                    = [var.alert_email]
  }
}

# SQL MI Security - Advanced Threat Protection
resource "azurerm_mssql_managed_instance_security_alert_policy" "main" {
  count = var.enable_sql_mi_security_features ? 1 : 0
  
  managed_instance_name = azurerm_mssql_managed_instance.main.name
  resource_group_name   = azurerm_resource_group.main.name
  
  state                      = "Enabled"
  storage_endpoint           = azurerm_storage_account.main.primary_blob_endpoint
  storage_account_access_key = azurerm_storage_account.main.primary_access_key
  retention_days             = var.environment == "production" ? 90 : 30
  
  # Alert on all threat types for enterprise security
  disabled_alerts = []
  
  email_account_admins = true
  email_addresses     = [var.alert_email]
}

# Outputs for application configuration
output "sql_mi_fqdn" {
  description = "SQL Managed Instance FQDN"
  value       = azurerm_mssql_managed_instance.main.fqdn
  sensitive   = true
}

output "sql_mi_connection_string" {
  description = "SQL Managed Instance connection string"
  value = "Server=${azurerm_mssql_managed_instance.main.fqdn};Database=filetransfer;User Id=${var.sql_mi_admin_login};Password=${var.sql_mi_admin_password};Encrypt=true;TrustServerCertificate=false;Connection Timeout=30;"
  sensitive = true
}

output "sql_mi_jdbc_url" {
  description = "JDBC URL for SQL Managed Instance"
  value = "jdbc:sqlserver://${azurerm_mssql_managed_instance.main.fqdn}:1433;database=filetransfer;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.sql.azuresynapse.net;loginTimeout=30;"
  sensitive = true
}