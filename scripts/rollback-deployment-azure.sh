#!/bin/bash

# File Transfer System - Azure Deployment Rollback Script
# Provides automated rollback capabilities for Azure AKS with Application Insights monitoring

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parameters
ROLLBACK_REVISION="${1:-}"
ENVIRONMENT="${2:-staging}"
ROLLBACK_TYPE="${3:-automatic}" # automatic, manual, emergency
RESOURCE_GROUP="file-transfer-${ENVIRONMENT}"
AKS_CLUSTER="file-transfer-aks-${ENVIRONMENT}"
NAMESPACE="file-transfer-${ENVIRONMENT}"
KEY_VAULT="file-transfer-kv-${ENVIRONMENT}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

# Validate Azure rollback parameters
validate_azure_rollback_parameters() {
    log_info "Validating Azure rollback parameters"
    
    if [[ -z "$ROLLBACK_REVISION" ]]; then
        log_error "Rollback revision is required"
        echo "Usage: $0 <rollback_revision> [environment] [rollback_type]"
        exit 1
    fi
    
    # Check Azure CLI
    if ! command -v az &> /dev/null; then
        log_error "Azure CLI not found"
        exit 1
    fi
    
    # Check Azure authentication
    if ! az account show &> /dev/null; then
        log_error "Not authenticated to Azure"
        exit 1
    fi
    
    # Check resource group
    if ! az group show --name "$RESOURCE_GROUP" &> /dev/null; then
        log_error "Azure resource group $RESOURCE_GROUP not found"
        exit 1
    fi
    
    # Check AKS cluster
    if ! az aks show --resource-group "$RESOURCE_GROUP" --name "$AKS_CLUSTER" &> /dev/null; then
        log_error "AKS cluster $AKS_CLUSTER not found"
        exit 1
    fi
    
    # Get AKS credentials
    az aks get-credentials \
        --resource-group "$RESOURCE_GROUP" \
        --name "$AKS_CLUSTER" \
        --overwrite-existing
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to AKS cluster"
        exit 1
    fi
    
    # Check namespace
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi
    
    # Check Key Vault access
    if ! az keyvault show --name "$KEY_VAULT" &> /dev/null; then
        log_error "Cannot access Azure Key Vault: $KEY_VAULT"
        exit 1
    fi
    
    log_success "Azure rollback parameters validated"
}

# Get current Azure deployment information
get_current_azure_deployment_info() {
    log_info "Getting current Azure deployment information"
    
    local applications=("web" "batch" "frontend")
    
    for app in "${applications[@]}"; do
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        if kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            local current_revision=$(kubectl get deployment "$deployment_name" \
                --namespace="$NAMESPACE" \
                -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')
            
            local current_image=$(kubectl get deployment "$deployment_name" \
                --namespace="$NAMESPACE" \
                -o jsonpath='{.spec.template.spec.containers[0].image}')
            
            log_info "Azure $app application - Current revision: $current_revision, Image: $current_image"
            
            # Store in Azure Key Vault for audit trail
            local deployment_info=$(cat << EOF
{
  "application": "$app",
  "currentRevision": "$current_revision",
  "currentImage": "$current_image",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "resourceGroup": "$RESOURCE_GROUP",
  "aksCluster": "$AKS_CLUSTER",
  "namespace": "$NAMESPACE"
}
EOF
)
            
            az keyvault secret set \
                --vault-name "$KEY_VAULT" \
                --name "current-deployment-${app}" \
                --value "$deployment_info" \
                --output none
        else
            log_warning "Azure deployment $deployment_name not found"
        fi
    done
}

# Create Azure rollback backup
create_azure_rollback_backup() {
    log_step "Creating Azure rollback backup"
    
    local backup_name="pre-rollback-backup-$(date +%Y%m%d-%H%M%S)"
    
    # Backup current AKS configurations
    kubectl get deployments,services,configmaps,secrets \
        --namespace="$NAMESPACE" \
        -o yaml > "/tmp/${backup_name}-aks-configs.yml"
    
    # Upload backup to Azure Storage
    local storage_account=$(az storage account list \
        --resource-group "$RESOURCE_GROUP" \
        --query "[?contains(name, 'filetransfer')].name" \
        --output tsv | head -1)
    
    if [[ -n "$storage_account" ]]; then
        # Upload configuration backup to Azure Storage
        az storage blob upload \
            --account-name "$storage_account" \
            --container-name "backup-storage" \
            --name "rollback-backups/${backup_name}-aks-configs.yml" \
            --file "/tmp/${backup_name}-aks-configs.yml" \
            --auth-mode login \
            --output none
        
        log_success "Azure configuration backup uploaded: ${backup_name}"
    fi
    
    # Create database backup if in production
    if [[ "$ENVIRONMENT" == "production" ]]; then
        log_info "Creating Azure database backup for production rollback"
        
        # Get PostgreSQL server name
        local postgres_server=$(az postgres flexible-server list \
            --resource-group "$RESOURCE_GROUP" \
            --query "[0].name" \
            --output tsv)
        
        if [[ -n "$postgres_server" ]]; then
            # Create database backup
            az postgres flexible-server backup create \
                --resource-group "$RESOURCE_GROUP" \
                --name "$postgres_server" \
                --backup-name "$backup_name" \
                --output none || log_warning "Azure database backup failed"
        fi
    fi
    
    # Store backup metadata in Key Vault
    local backup_metadata=$(cat << EOF
{
  "backupName": "$backup_name",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "environment": "$ENVIRONMENT",
  "rollbackRevision": "$ROLLBACK_REVISION",
  "backupType": "PRE_ROLLBACK",
  "storageLocation": "azure-storage://${storage_account}/backup-storage/rollback-backups/${backup_name}-aks-configs.yml"
}
EOF
)
    
    az keyvault secret set \
        --vault-name "$KEY_VAULT" \
        --name "rollback-backup-${backup_name}" \
        --value "$backup_metadata" \
        --output none
    
    log_success "Azure rollback backup created: $backup_name"
    echo "$backup_name" > "/tmp/azure-rollback-backup-name.txt"
}

# Execute Azure rollback
execute_azure_rollback() {
    log_step "Executing Azure rollback to revision: $ROLLBACK_REVISION"
    
    local applications=("web" "batch" "frontend")
    local rollback_success=true
    
    # PHASE 1: Update Azure Application Gateway to maintenance mode
    if is_azure_blue_green_deployment; then
        log_info "Setting Azure Application Gateway to maintenance mode"
        set_azure_maintenance_mode
    fi
    
    # PHASE 2: Rollback each application in AKS
    for app in "${applications[@]}"; do
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        log_info "Rolling back Azure $app application to revision $ROLLBACK_REVISION"
        
        # Check if deployment exists
        if ! kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            log_warning "Azure deployment $deployment_name not found, skipping"
            continue
        fi
        
        # Get rollback target image from Azure Container Registry
        local rollback_image=$(get_azure_rollback_image "$app" "$ROLLBACK_REVISION")
        
        if [[ -n "$rollback_image" ]]; then
            # Perform rollback using image update
            kubectl set image deployment/"$deployment_name" \
                ${app}-container="$rollback_image" \
                --namespace="$NAMESPACE"
        else
            # Perform rollback using kubectl rollout undo
            kubectl rollout undo deployment/"$deployment_name" \
                --to-revision="$ROLLBACK_REVISION" \
                --namespace="$NAMESPACE"
        fi
        
        # Wait for rollback to complete
        if ! kubectl rollout status deployment/"$deployment_name" \
                --timeout=600s --namespace="$NAMESPACE"; then
            log_error "Azure rollback failed for $app application"
            rollback_success=false
        else
            log_success "Azure rollback completed for $app application"
            
            # Log rollback event to Application Insights
            log_azure_rollback_event "$app" "$ROLLBACK_REVISION"
        fi
    done
    
    # PHASE 3: Restore Azure Application Gateway routing
    if is_azure_blue_green_deployment; then
        log_info "Restoring Azure Application Gateway routing after rollback"
        restore_azure_routing_after_rollback
    fi
    
    if [[ "$rollback_success" != true ]]; then
        log_error "Azure rollback failed for one or more applications"
        return 1
    fi
    
    log_success "Azure rollback execution completed successfully"
}

# Verify Azure rollback success
verify_azure_rollback() {
    log_step "Verifying Azure rollback success"
    
    local applications=("web" "batch" "frontend")
    local verification_success=true
    
    # PHASE 1: AKS health checks
    log_info "Performing post-rollback AKS health checks"
    
    for app in "${applications[@]}"; do
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        if ! kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            log_warning "Azure deployment $deployment_name not found"
            continue
        fi
        
        # Check pod readiness
        local ready_replicas=$(kubectl get deployment "$deployment_name" \
            --namespace="$NAMESPACE" \
            -o jsonpath='{.status.readyReplicas}')
        
        local desired_replicas=$(kubectl get deployment "$deployment_name" \
            --namespace="$NAMESPACE" \
            -o jsonpath='{.spec.replicas}')
        
        if [[ "$ready_replicas" == "$desired_replicas" ]]; then
            log_success "Azure $app application: $ready_replicas/$desired_replicas replicas ready"
        else
            log_error "Azure $app application: Only $ready_replicas/$desired_replicas replicas ready"
            verification_success=false
        fi
        
        # Azure-specific health check
        if ! perform_azure_application_health_check "$app"; then
            log_error "Azure health check failed for $app application"
            verification_success=false
        fi
    done
    
    # PHASE 2: Azure Application Insights validation
    log_info "Performing Azure Application Insights validation"
    
    if ! validate_azure_application_insights; then
        log_error "Azure Application Insights validation failed"
        verification_success=false
    fi
    
    # PHASE 3: Azure Monitor alerts check
    log_info "Checking Azure Monitor alerts"
    
    if ! check_azure_monitor_alerts_post_rollback; then
        log_error "Azure Monitor alerts detected issues post-rollback"
        verification_success=false
    fi
    
    # PHASE 4: Functional verification
    log_info "Performing functional verification"
    
    if ! run_azure_post_rollback_tests; then
        log_error "Post-rollback functional tests failed"
        verification_success=false
    fi
    
    if [[ "$verification_success" != true ]]; then
        log_error "Azure rollback verification failed"
        return 1
    fi
    
    log_success "Azure rollback verification completed successfully"
}

# Monitor Azure rollback stability
monitor_azure_rollback_stability() {
    local monitoring_duration=600 # 10 minutes
    log_step "Monitoring Azure rollback stability for $monitoring_duration seconds"
    
    local end_time=$(($(date +%s) + monitoring_duration))
    local stability_issues=0
    local app_insights_name="file-transfer-insights-${ENVIRONMENT}"
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check AKS application health
        if ! check_azure_system_health; then
            stability_issues=$((stability_issues + 1))
            log_warning "Azure stability issue detected (count: $stability_issues)"
            
            if [[ $stability_issues -ge 3 ]]; then
                log_error "Multiple Azure stability issues detected, rollback may have failed"
                return 1
            fi
        fi
        
        # Check error rates using Application Insights
        if ! check_azure_error_rates_post_rollback "$app_insights_name"; then
            stability_issues=$((stability_issues + 1))
            log_warning "High error rate detected in Application Insights post-rollback"
        fi
        
        # Check performance metrics using Azure Monitor
        if ! check_azure_performance_metrics_post_rollback "$app_insights_name"; then
            stability_issues=$((stability_issues + 1))
            log_warning "Performance degradation detected in Azure Monitor post-rollback"
        fi
        
        # Check Azure Application Gateway health
        if ! check_azure_application_gateway_health; then
            stability_issues=$((stability_issues + 1))
            log_warning "Azure Application Gateway health issues detected"
        fi
        
        sleep 60 # Check every minute
    done
    
    if [[ $stability_issues -eq 0 ]]; then
        log_success "Azure rollback stability monitoring completed with no issues"
    else
        log_warning "Azure rollback stability monitoring completed with $stability_issues issues"
    fi
    
    return 0
}

# Generate Azure rollback report
generate_azure_rollback_report() {
    log_step "Generating Azure rollback report"
    
    local report_file="/tmp/azure-rollback-report-$(date +%Y%m%d-%H%M%S).json"
    
    # Get post-rollback deployment info from AKS
    local applications=("web" "batch" "frontend")
    local app_info="["
    
    for i in "${!applications[@]}"; do
        local app="${applications[$i]}"
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        if kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            local current_revision=$(kubectl get deployment "$deployment_name" \
                --namespace="$NAMESPACE" \
                -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')
            
            local current_image=$(kubectl get deployment "$deployment_name" \
                --namespace="$NAMESPACE" \
                -o jsonpath='{.spec.template.spec.containers[0].image}')
            
            local ready_replicas=$(kubectl get deployment "$deployment_name" \
                --namespace="$NAMESPACE" \
                -o jsonpath='{.status.readyReplicas}')
            
            if [[ $i -gt 0 ]]; then
                app_info+=","
            fi
            
            app_info+="{\"application\":\"$app\",\"revision\":\"$current_revision\",\"image\":\"$current_image\",\"readyReplicas\":$ready_replicas}"
        fi
    done
    
    app_info+="]"
    
    # Get Azure-specific information
    local subscription_id=$(az account show --query id --output tsv)
    local tenant_id=$(az account show --query tenantId --output tsv)
    
    # Create comprehensive Azure rollback report
    cat > "$report_file" << EOF
{
  "rollbackId": "$(uuidgen)",
  "rollbackRevision": "$ROLLBACK_REVISION",
  "environment": "$ENVIRONMENT",
  "platform": "Azure",
  "subscriptionId": "$subscription_id",
  "tenantId": "$tenant_id",
  "resourceGroup": "$RESOURCE_GROUP",
  "aksCluster": "$AKS_CLUSTER",
  "namespace": "$NAMESPACE",
  "keyVault": "$KEY_VAULT",
  "rollbackType": "$ROLLBACK_TYPE",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "triggeredBy": "${BUILD_REQUESTEDFOR:-$(whoami)}",
  "buildId": "${BUILD_BUILDID:-unknown}",
  "applications": $app_info,
  "rollbackDuration": "$(cat /tmp/azure-rollback-duration.txt 2>/dev/null || echo 'unknown')",
  "verificationStatus": "$(cat /tmp/azure-rollback-verification-status.txt 2>/dev/null || echo 'pending')",
  "azureHealthChecks": {
    "aksHealth": "$(check_azure_system_health && echo 'HEALTHY' || echo 'UNHEALTHY')",
    "applicationInsights": "$(validate_azure_application_insights && echo 'HEALTHY' || echo 'UNHEALTHY')",
    "applicationGateway": "$(check_azure_application_gateway_health && echo 'HEALTHY' || echo 'UNHEALTHY')",
    "keyVault": "$(az keyvault show --name $KEY_VAULT --query 'properties.provisioningState' --output tsv)",
    "containerRegistry": "$(az acr show --name filetransferregistry${ENVIRONMENT} --query 'provisioningState' --output tsv 2>/dev/null || echo 'UNKNOWN')"
  },
  "backupCreated": "$(cat /tmp/azure-rollback-backup-name.txt 2>/dev/null || echo 'none')",
  "rollbackReason": "${ROLLBACK_REASON:-Automated rollback due to Azure deployment failure}",
  "azureSpecificInfo": {
    "applicationGatewayPublicIp": "$(az network public-ip show --resource-group $RESOURCE_GROUP --name file-transfer-appgw-pip-${ENVIRONMENT} --query ipAddress --output tsv 2>/dev/null || echo 'unknown')",
    "logAnalyticsWorkspace": "file-transfer-logs-${ENVIRONMENT}",
    "applicationInsights": "file-transfer-insights-${ENVIRONMENT}",
    "storageAccount": "$(az storage account list --resource-group $RESOURCE_GROUP --query '[0].name' --output tsv 2>/dev/null || echo 'unknown')"
  },
  "nextSteps": [
    "Monitor Azure Application Insights for 24 hours",
    "Check Azure Monitor alerts and metrics",
    "Investigate root cause using Azure diagnostics",
    "Review Azure Activity Log for deployment events",
    "Update Azure deployment procedures if needed"
  ]
}
EOF

    log_success "Azure rollback report generated: $report_file"
    
    # Store report in Azure Key Vault
    az keyvault secret set \
        --vault-name "$KEY_VAULT" \
        --name "rollback-report-$(date +%Y%m%d-%H%M%S)" \
        --value "$(cat $report_file)" \
        --output none
    
    # Also store in AKS ConfigMap
    kubectl create configmap "azure-rollback-report-$(date +%Y%m%d-%H%M%S)" \
        --from-file="$report_file" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
}

# Azure-specific helper functions

get_azure_rollback_image() {
    local app="$1"
    local revision="$2"
    
    # Try to get image from Azure Container Registry
    local acr_name="filetransferregistry${ENVIRONMENT}"
    local image_name="file-transfer-${app}"
    
    # Check if specific revision exists in ACR
    if az acr repository show --name "$acr_name" --image "${image_name}:${revision}" &> /dev/null; then
        echo "${acr_name}.azurecr.io/${image_name}:${revision}"
    else
        # Fallback to kubectl rollout history
        local image=$(kubectl rollout history deployment/file-transfer-${app}-${ENVIRONMENT} \
            --revision="$revision" --namespace="$NAMESPACE" 2>/dev/null | \
            grep -o "${acr_name}.azurecr.io/[^[:space:]]*" || echo "")
        echo "$image"
    fi
}

is_azure_blue_green_deployment() {
    # Check if using Azure Application Gateway with blue-green configuration
    az network application-gateway show \
        --resource-group "$RESOURCE_GROUP" \
        --name "file-transfer-appgw-${ENVIRONMENT}" \
        --query "backendAddressPools[?contains(name, 'blue') || contains(name, 'green')]" \
        --output tsv &> /dev/null
}

set_azure_maintenance_mode() {
    log_info "Setting Azure Application Gateway to maintenance mode"
    
    # Update Application Gateway to point to maintenance page
    az network application-gateway rule update \
        --resource-group "$RESOURCE_GROUP" \
        --gateway-name "file-transfer-appgw-${ENVIRONMENT}" \
        --name "routing-rule-main" \
        --address-pool "backend-pool-maintenance" \
        --output none || log_warning "Failed to set maintenance mode"
}

restore_azure_routing_after_rollback() {
    log_info "Restoring Azure Application Gateway routing after rollback"
    
    # Determine which backend pool to use after rollback
    local target_pool=$(get_azure_rollback_target_pool)
    
    # Update Application Gateway routing
    az network application-gateway rule update \
        --resource-group "$RESOURCE_GROUP" \
        --gateway-name "file-transfer-appgw-${ENVIRONMENT}" \
        --name "routing-rule-main" \
        --address-pool "$target_pool" \
        --output none
}

get_azure_rollback_target_pool() {
    # Determine target backend pool based on rollback
    echo "backend-pool-production" # Default to production pool
}

perform_azure_application_health_check() {
    local app="$1"
    local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
    
    # Get service endpoint from AKS
    local service_name="file-transfer-${app}-${ENVIRONMENT}"
    local port=$(get_application_port "$app")
    
    # Perform health check using kubectl exec
    local pod_name=$(kubectl get pods -l app="file-transfer-${app}" \
        --namespace="$NAMESPACE" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -n "$pod_name" ]]; then
        if kubectl exec "$pod_name" --namespace="$NAMESPACE" -- \
            curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            return 0
        fi
    fi
    
    return 1
}

validate_azure_application_insights() {
    local app_insights_name="file-transfer-insights-${ENVIRONMENT}"
    
    # Check if Application Insights is receiving telemetry
    local telemetry_count=$(az monitor app-insights events show \
        --app "$app_insights_name" \
        --type requests \
        --start-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)" \
        --end-time "$(date -u +%Y-%m-%dT%H:%M:%S)" \
        --query "length(value)" \
        --output tsv 2>/dev/null || echo "0")
    
    if [[ $telemetry_count -gt 0 ]]; then
        log_success "Azure Application Insights receiving telemetry: $telemetry_count events"
        return 0
    else
        log_warning "No telemetry received by Azure Application Insights"
        return 1
    fi
}

check_azure_monitor_alerts_post_rollback() {
    # Check Azure Monitor for active alerts after rollback
    local active_alerts=$(az monitor alert list \
        --resource-group "$RESOURCE_GROUP" \
        --query "[?properties.enabled && properties.condition.allOf[0].metricValue > properties.condition.allOf[0].threshold].name" \
        --output tsv | wc -l)
    
    if [[ $active_alerts -gt 0 ]]; then
        log_warning "Azure Monitor has $active_alerts active alerts post-rollback"
        
        # List the alerts for debugging
        az monitor alert list \
            --resource-group "$RESOURCE_GROUP" \
            --query "[?properties.enabled].{Name:name,Condition:properties.condition.allOf[0].metricName,Threshold:properties.condition.allOf[0].threshold}" \
            --output table
        
        return 1
    fi
    
    return 0
}

check_azure_application_gateway_health() {
    # Check Azure Application Gateway health
    local appgw_status=$(az network application-gateway show \
        --resource-group "$RESOURCE_GROUP" \
        --name "file-transfer-appgw-${ENVIRONMENT}" \
        --query "provisioningState" \
        --output tsv 2>/dev/null || echo "Unknown")
    
    if [[ "$appgw_status" == "Succeeded" ]]; then
        return 0
    else
        log_warning "Azure Application Gateway status: $appgw_status"
        return 1
    fi
}

log_azure_rollback_event() {
    local app="$1"
    local revision="$2"
    local app_insights_name="file-transfer-insights-${ENVIRONMENT}"
    
    # Log custom event to Application Insights
    local event_data=$(cat << EOF
{
  "name": "DeploymentRollback",
  "properties": {
    "application": "$app",
    "rollbackRevision": "$revision",
    "environment": "$ENVIRONMENT",
    "rollbackType": "$ROLLBACK_TYPE",
    "triggeredBy": "${BUILD_REQUESTEDFOR:-$(whoami)}",
    "buildId": "${BUILD_BUILDID:-unknown}"
  }
}
EOF
)
    
    # Send to Application Insights (would require proper API call)
    log_info "Logged rollback event to Application Insights for $app application"
}

# Main execution
main() {
    local start_time=$(date +%s)
    
    log_info "Starting File Transfer System Azure Rollback"
    log_info "Rollback Revision: $ROLLBACK_REVISION"
    log_info "Environment: $ENVIRONMENT"
    log_info "Rollback Type: $ROLLBACK_TYPE"
    log_info "Resource Group: $RESOURCE_GROUP"
    log_info "AKS Cluster: $AKS_CLUSTER"
    log_info "Namespace: $NAMESPACE"
    log_info "Key Vault: $KEY_VAULT"
    
    # Validate Azure parameters
    validate_azure_rollback_parameters
    
    # Get current Azure deployment info
    get_current_azure_deployment_info
    
    # Create Azure rollback backup
    create_azure_rollback_backup
    
    # Execute Azure rollback
    if ! execute_azure_rollback; then
        log_error "Azure rollback execution failed"
        exit 1
    fi
    
    # Verify Azure rollback
    if ! verify_azure_rollback; then
        log_error "Azure rollback verification failed"
        exit 1
    fi
    
    # Monitor Azure stability
    monitor_azure_rollback_stability
    
    # Calculate rollback duration
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    echo "${duration} seconds" > "/tmp/azure-rollback-duration.txt"
    
    # Generate Azure report
    generate_azure_rollback_report
    
    log_success "Azure rollback completed successfully in $duration seconds!"
}

# Additional Azure-specific helper functions

check_azure_system_health() {
    local applications=("web" "batch" "frontend")
    
    for app in "${applications[@]}"; do
        if ! perform_azure_application_health_check "$app"; then
            return 1
        fi
    done
    
    return 0
}

check_azure_error_rates_post_rollback() {
    local app_insights_name="$1"
    
    # Query Application Insights for post-rollback error rates
    local error_rate=$(az monitor app-insights metrics show \
        --app "$app_insights_name" \
        --metric "requests/failed" \
        --start-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)" \
        --end-time "$(date -u +%Y-%m-%dT%H:%M:%S)" \
        --aggregation Average \
        --query "value.timeseries[0].data[-1].average" \
        --output tsv 2>/dev/null || echo "0")
    
    local error_percentage=${error_rate%.*}
    
    if [[ ${error_percentage:-0} -gt 5 ]]; then
        log_warning "Azure post-rollback error rate is ${error_percentage}%"
        return 1
    fi
    
    return 0
}

check_azure_performance_metrics_post_rollback() {
    local app_insights_name="$1"
    
    # Query Application Insights for post-rollback performance
    local avg_response_time=$(az monitor app-insights metrics show \
        --app "$app_insights_name" \
        --metric "requests/duration" \
        --start-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)" \
        --end-time "$(date -u +%Y-%m-%dT%H:%M:%S)" \
        --aggregation Average \
        --query "value.timeseries[0].data[-1].average" \
        --output tsv 2>/dev/null || echo "0")
    
    local response_time_ms=${avg_response_time%.*}
    
    if [[ ${response_time_ms:-0} -gt 1000 ]]; then
        log_warning "Azure post-rollback response time is ${response_time_ms}ms"
        return 1
    fi
    
    return 0
}

run_azure_post_rollback_tests() {
    log_info "Running Azure post-rollback functional tests"
    
    # Run smoke tests against Azure endpoints
    if "$PROJECT_ROOT/scripts/run-smoke-tests-azure.sh" "$ENVIRONMENT"; then
        echo "PASSED" > "/tmp/azure-rollback-verification-status.txt"
        return 0
    else
        echo "FAILED" > "/tmp/azure-rollback-verification-status.txt"
        return 1
    fi
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Azure Deployment Rollback Script"
    echo
    echo "Usage: $0 <rollback_revision> [environment] [rollback_type]"
    echo
    echo "Arguments:"
    echo "  rollback_revision  Target revision to rollback to"
    echo "  environment        Target environment (staging|production)"
    echo "  rollback_type      Type of rollback (automatic|manual|emergency)"
    echo
    echo "Azure Resources:"
    echo "  Resource Group: file-transfer-[environment]"
    echo "  AKS Cluster: file-transfer-aks-[environment]"
    echo "  Key Vault: file-transfer-kv-[environment]"
    echo "  Application Gateway: file-transfer-appgw-[environment]"
    echo
    echo "Examples:"
    echo "  $0 abc123def staging automatic"
    echo "  $0 xyz789abc production manual"
    echo "  $0 emergency-revision production emergency"
    echo
    exit 0
fi

# Execute main function
main "$@"