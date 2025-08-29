#!/bin/bash

# File Transfer System - Azure Deployment Strategy Script
# Implements blue-green, canary, and rolling deployment strategies for Azure AKS

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parameters
DEPLOYMENT_STRATEGY="${1:-blue-green}"
IMAGE_TAG="${2:-latest}"
ENVIRONMENT="${3:-staging}"
RESOURCE_GROUP="file-transfer-${ENVIRONMENT}"
AKS_CLUSTER="file-transfer-aks-${ENVIRONMENT}"
NAMESPACE="file-transfer-${ENVIRONMENT}"

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

# Validate Azure environment
validate_azure_environment() {
    log_info "Validating Azure environment: $ENVIRONMENT"
    
    # Check Azure CLI
    if ! command -v az &> /dev/null; then
        log_error "Azure CLI not found"
        exit 1
    fi
    
    # Check authentication
    if ! az account show &> /dev/null; then
        log_error "Not authenticated to Azure"
        exit 1
    fi
    
    # Check resource group
    if ! az group show --name "$RESOURCE_GROUP" &> /dev/null; then
        log_error "Resource group $RESOURCE_GROUP not found"
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
        log_info "Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
        kubectl label namespace "$NAMESPACE" environment="$ENVIRONMENT"
    fi
    
    # Validate container registry access
    validate_acr_access
    
    log_success "Azure environment validation completed"
}

# Validate Azure Container Registry access
validate_acr_access() {
    log_info "Validating Azure Container Registry access"
    
    local acr_name="filetransferregistry"
    local images=("web" "batch" "frontend")
    
    # Check ACR access
    if ! az acr show --name "$acr_name" &> /dev/null; then
        log_error "Cannot access Azure Container Registry: $acr_name"
        exit 1
    fi
    
    # Validate images exist
    for app in "${images[@]}"; do
        local image_name="file-transfer-${app}"
        log_info "Validating image: ${acr_name}.azurecr.io/${image_name}:${IMAGE_TAG}"
        
        if az acr repository show --name "$acr_name" --image "${image_name}:${IMAGE_TAG}" &> /dev/null; then
            log_success "Image validated: ${image_name}:${IMAGE_TAG}"
        else
            log_warning "Image not found: ${image_name}:${IMAGE_TAG}, will use latest"
        fi
    done
}

# Azure Blue-Green Deployment Strategy
deploy_blue_green_azure() {
    log_step "Executing Azure Blue-Green Deployment Strategy"
    
    # Determine current and new slots
    local current_slot=$(get_current_azure_slot)
    local new_slot=$(get_alternate_azure_slot "$current_slot")
    
    log_info "Current slot: $current_slot"
    log_info "New slot: $new_slot"
    
    # PHASE 1: Deploy to new slot
    log_step "Phase 1: Deploying to $new_slot slot"
    deploy_to_azure_slot "$new_slot"
    
    # PHASE 2: Health check new slot
    log_step "Phase 2: Health checking $new_slot slot"
    if ! health_check_azure_slot "$new_slot"; then
        log_error "Health check failed for $new_slot slot"
        cleanup_failed_azure_slot "$new_slot"
        exit 1
    fi
    
    # PHASE 3: Run smoke tests on new slot
    log_step "Phase 3: Running smoke tests on $new_slot slot"
    if ! run_smoke_tests_azure_slot "$new_slot"; then
        log_error "Smoke tests failed for $new_slot slot"
        cleanup_failed_azure_slot "$new_slot"
        exit 1
    fi
    
    # PHASE 4: Switch traffic using Azure Traffic Manager
    log_step "Phase 4: Switching traffic to $new_slot slot"
    switch_azure_traffic "$current_slot" "$new_slot"
    
    # PHASE 5: Monitor new slot with Azure Application Insights
    log_step "Phase 5: Monitoring $new_slot slot with Application Insights"
    if ! monitor_azure_slot "$new_slot" 300; then # 5 minutes
        log_error "Monitoring detected issues, rolling back"
        switch_azure_traffic "$new_slot" "$current_slot"
        exit 1
    fi
    
    # PHASE 6: Update Azure DNS and cleanup
    log_step "Phase 6: Updating DNS and cleaning up $current_slot slot"
    update_azure_dns "$new_slot"
    cleanup_old_azure_slot "$current_slot"
    
    log_success "Azure Blue-Green deployment completed successfully"
}

# Azure Canary Deployment Strategy
deploy_canary_azure() {
    log_step "Executing Azure Canary Deployment Strategy"
    
    local canary_percentage=10
    local stages=(10 25 50 75 100)
    
    # PHASE 1: Deploy canary with Azure App Service slots
    log_step "Phase 1: Deploying canary version (${canary_percentage}% traffic)"
    deploy_azure_canary_version "$canary_percentage"
    
    # PHASE 2: Progressive traffic shifting using Azure Traffic Manager
    for stage in "${stages[@]}"; do
        log_step "Phase 2.${stage}: Shifting ${stage}% traffic to canary"
        
        # Update Azure Traffic Manager routing
        update_azure_traffic_split "$stage"
        
        # Monitor with Azure Application Insights
        if ! monitor_azure_canary_deployment "$stage" 180; then # 3 minutes per stage
            log_error "Azure canary monitoring detected issues at ${stage}%"
            rollback_azure_canary_deployment
            exit 1
        fi
        
        log_success "Azure canary stage ${stage}% completed successfully"
    done
    
    # PHASE 3: Promote canary using Azure slot swap
    log_step "Phase 3: Promoting canary to production using Azure slot swap"
    promote_azure_canary_to_production
    
    log_success "Azure canary deployment completed successfully"
}

# Azure Rolling Deployment Strategy
deploy_rolling_azure() {
    log_step "Executing Azure Rolling Deployment Strategy"
    
    local applications=("web" "batch" "frontend")
    
    # PHASE 1: Update AKS deployment configurations
    log_step "Phase 1: Updating AKS deployment configurations"
    update_azure_deployment_configs
    
    # PHASE 2: Rolling update each application in AKS
    for app in "${applications[@]}"; do
        log_step "Phase 2.${app}: AKS rolling update for $app application"
        
        # Start rolling update in AKS
        kubectl set image deployment/file-transfer-${app}-${ENVIRONMENT} \
            ${app}-container=filetransferregistry.azurecr.io/file-transfer-${app}:${IMAGE_TAG} \
            --namespace="$NAMESPACE"
        
        # Wait for rollout to complete
        if ! kubectl rollout status deployment/file-transfer-${app}-${ENVIRONMENT} \
                --timeout=600s --namespace="$NAMESPACE"; then
            log_error "AKS rolling update failed for $app application"
            
            # Rollback this application
            kubectl rollout undo deployment/file-transfer-${app}-${ENVIRONMENT} \
                --namespace="$NAMESPACE"
            exit 1
        fi
        
        # Health check with Azure health probes
        if ! health_check_azure_application "$app"; then
            log_error "Azure health check failed for $app application"
            
            # Rollback this application
            kubectl rollout undo deployment/file-transfer-${app}-${ENVIRONMENT} \
                --namespace="$NAMESPACE"
            exit 1
        fi
        
        log_success "AKS rolling update completed for $app application"
    done
    
    # PHASE 3: Final system health check with Azure Monitor
    log_step "Phase 3: Final Azure system health check"
    if ! health_check_azure_environment "$ENVIRONMENT"; then
        log_error "Final Azure health check failed"
        rollback_all_azure_applications
        exit 1
    fi
    
    log_success "Azure rolling deployment completed successfully"
}

# Azure-specific helper functions

get_current_azure_slot() {
    # Get current active slot from Azure App Service
    local current_slot=$(az webapp deployment slot list \
        --resource-group "$RESOURCE_GROUP" \
        --name "file-transfer-web-${ENVIRONMENT}" \
        --query "[?defaultHostName != null].name" \
        --output tsv 2>/dev/null || echo "production")
    echo "$current_slot"
}

get_alternate_azure_slot() {
    local current="$1"
    if [[ "$current" == "blue" ]]; then
        echo "green"
    else
        echo "blue"
    fi
}

deploy_to_azure_slot() {
    local slot="$1"
    log_info "Deploying to Azure slot: $slot"
    
    # Update AKS deployment with slot-specific labels
    sed -e "s|IMAGE_TAG|${IMAGE_TAG}|g" \
        -e "s|ENVIRONMENT_SUFFIX|${slot}|g" \
        -e "s|NAMESPACE|${NAMESPACE}|g" \
        -e "s|AZURE_SLOT|${slot}|g" \
        "$PROJECT_ROOT/k8s/azure/deployment-template.yml" > "/tmp/azure-deployment-${slot}.yml"
    
    # Apply deployment to AKS
    kubectl apply -f "/tmp/azure-deployment-${slot}.yml" --namespace="$NAMESPACE"
    
    # Wait for deployment to be ready
    local applications=("web" "batch" "frontend")
    for app in "${applications[@]}"; do
        kubectl rollout status deployment/file-transfer-${app}-${slot} \
            --timeout=600s --namespace="$NAMESPACE"
    done
    
    # Configure Azure Application Gateway for new slot
    configure_azure_application_gateway "$slot"
}

configure_azure_application_gateway() {
    local slot="$1"
    log_info "Configuring Azure Application Gateway for slot: $slot"
    
    # Get AKS internal load balancer IP
    local lb_ip=$(kubectl get service file-transfer-web-${slot} \
        --namespace="$NAMESPACE" \
        -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    
    # Update Application Gateway backend pool
    az network application-gateway address-pool update \
        --resource-group "$RESOURCE_GROUP" \
        --gateway-name "file-transfer-appgw-${ENVIRONMENT}" \
        --name "backend-pool-${slot}" \
        --servers "$lb_ip"
}

health_check_azure_slot() {
    local slot="$1"
    log_info "Performing Azure health check for slot: $slot"
    
    local applications=("web" "batch" "frontend")
    local ports=(8080 8082 3000)
    
    for i in "${!applications[@]}"; do
        local app="${applications[$i]}"
        local port="${ports[$i]}"
        
        # Get service endpoint from AKS
        local service_name="file-transfer-${app}-${slot}"
        
        # Health check with Azure-specific retry logic
        local max_retries=30
        local retry_count=0
        
        while [[ $retry_count -lt $max_retries ]]; do
            if kubectl exec deployment/file-transfer-${app}-${slot} --namespace="$NAMESPACE" -- \
                curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
                log_success "Azure health check passed for $app application in $slot slot"
                break
            fi
            
            retry_count=$((retry_count + 1))
            log_info "Azure health check retry $retry_count/$max_retries for $app in $slot"
            sleep 10
        done
        
        if [[ $retry_count -eq $max_retries ]]; then
            log_error "Azure health check failed for $app application in $slot slot"
            return 1
        fi
    done
    
    # Additional Azure-specific health checks
    check_azure_application_insights_health "$slot"
    
    return 0
}

check_azure_application_insights_health() {
    local slot="$1"
    log_info "Checking Azure Application Insights for slot: $slot"
    
    # Query Application Insights for health metrics
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
        log_success "Application Insights receiving telemetry: $telemetry_count events"
    else
        log_warning "No telemetry received by Application Insights"
    fi
}

switch_azure_traffic() {
    local from_slot="$1"
    local to_slot="$2"
    log_info "Switching Azure traffic from $from_slot to $to_slot"
    
    # Update Azure Application Gateway routing rules
    az network application-gateway rule update \
        --resource-group "$RESOURCE_GROUP" \
        --gateway-name "file-transfer-appgw-${ENVIRONMENT}" \
        --name "routing-rule-main" \
        --address-pool "backend-pool-${to_slot}"
    
    # Update Azure Traffic Manager if configured
    if az network traffic-manager profile show \
        --resource-group "$RESOURCE_GROUP" \
        --name "file-transfer-tm-${ENVIRONMENT}" &> /dev/null; then
        
        # Update Traffic Manager endpoint
        az network traffic-manager endpoint update \
            --resource-group "$RESOURCE_GROUP" \
            --profile-name "file-transfer-tm-${ENVIRONMENT}" \
            --name "endpoint-${to_slot}" \
            --type azureEndpoints \
            --endpoint-status Enabled \
            --weight 100
        
        # Disable old endpoint
        az network traffic-manager endpoint update \
            --resource-group "$RESOURCE_GROUP" \
            --profile-name "file-transfer-tm-${ENVIRONMENT}" \
            --name "endpoint-${from_slot}" \
            --type azureEndpoints \
            --endpoint-status Disabled \
            --weight 0
    fi
    
    # Wait for traffic switch to propagate
    sleep 60
    
    log_success "Azure traffic switch completed"
}

monitor_azure_slot() {
    local slot="$1"
    local duration="$2"
    log_info "Monitoring Azure slot $slot for $duration seconds using Application Insights"
    
    local end_time=$(($(date +%s) + duration))
    local app_insights_name="file-transfer-insights-${ENVIRONMENT}"
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check application health
        if ! health_check_azure_slot "$slot"; then
            log_error "Azure health check failed during monitoring"
            return 1
        fi
        
        # Check error rates using Application Insights
        if ! check_azure_error_rates "$app_insights_name"; then
            log_error "High error rate detected in Application Insights"
            return 1
        fi
        
        # Check response times using Application Insights
        if ! check_azure_response_times "$app_insights_name"; then
            log_error "High response times detected in Application Insights"
            return 1
        fi
        
        # Check Azure Monitor alerts
        if ! check_azure_monitor_alerts; then
            log_error "Azure Monitor alerts detected issues"
            return 1
        fi
        
        sleep 30
    done
    
    log_success "Azure monitoring completed successfully"
    return 0
}

check_azure_error_rates() {
    local app_insights_name="$1"
    
    # Query Application Insights for error rates
    local error_rate=$(az monitor app-insights metrics show \
        --app "$app_insights_name" \
        --metric "requests/failed" \
        --start-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)" \
        --end-time "$(date -u +%Y-%m-%dT%H:%M:%S)" \
        --aggregation Average \
        --query "value.timeseries[0].data[-1].average" \
        --output tsv 2>/dev/null || echo "0")
    
    # Convert to percentage (simplified)
    local error_percentage=${error_rate%.*}
    
    if [[ ${error_percentage:-0} -gt 5 ]]; then
        log_warning "Azure error rate is ${error_percentage}%"
        return 1
    fi
    
    return 0
}

check_azure_response_times() {
    local app_insights_name="$1"
    
    # Query Application Insights for response times
    local avg_response_time=$(az monitor app-insights metrics show \
        --app "$app_insights_name" \
        --metric "requests/duration" \
        --start-time "$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)" \
        --end-time "$(date -u +%Y-%m-%dT%H:%M:%S)" \
        --aggregation Average \
        --query "value.timeseries[0].data[-1].average" \
        --output tsv 2>/dev/null || echo "0")
    
    # Convert to milliseconds (simplified)
    local response_time_ms=${avg_response_time%.*}
    
    if [[ ${response_time_ms:-0} -gt 1000 ]]; then
        log_warning "Azure response time is ${response_time_ms}ms"
        return 1
    fi
    
    return 0
}

check_azure_monitor_alerts() {
    # Check Azure Monitor for active alerts
    local active_alerts=$(az monitor alert list \
        --resource-group "$RESOURCE_GROUP" \
        --query "[?properties.enabled && properties.condition.allOf[0].metricValue > properties.condition.allOf[0].threshold].name" \
        --output tsv | wc -l)
    
    if [[ $active_alerts -gt 0 ]]; then
        log_warning "Azure Monitor has $active_alerts active alerts"
        return 1
    fi
    
    return 0
}

# Azure Canary specific functions
deploy_azure_canary_version() {
    local percentage="$1"
    log_info "Deploying Azure canary version with ${percentage}% traffic"
    
    # Create canary deployment in AKS
    sed -e "s|IMAGE_TAG|${IMAGE_TAG}|g" \
        -e "s|CANARY_PERCENTAGE|${percentage}|g" \
        -e "s|NAMESPACE|${NAMESPACE}|g" \
        -e "s|AZURE_ENVIRONMENT|${ENVIRONMENT}|g" \
        "$PROJECT_ROOT/k8s/azure/canary-deployment-template.yml" > "/tmp/azure-canary-deployment.yml"
    
    kubectl apply -f "/tmp/azure-canary-deployment.yml" --namespace="$NAMESPACE"
    
    # Wait for canary deployment
    kubectl rollout status deployment/file-transfer-web-canary \
        --timeout=600s --namespace="$NAMESPACE"
    
    # Configure Azure Application Gateway for canary
    configure_azure_canary_routing "$percentage"
}

configure_azure_canary_routing() {
    local percentage="$1"
    log_info "Configuring Azure canary routing for ${percentage}% traffic"
    
    # Update Azure Application Gateway with weighted routing
    az network application-gateway url-path-map rule update \
        --resource-group "$RESOURCE_GROUP" \
        --gateway-name "file-transfer-appgw-${ENVIRONMENT}" \
        --path-map-name "path-map-main" \
        --name "canary-rule" \
        --address-pool "backend-pool-canary" \
        --http-settings "http-settings-canary" \
        --paths "/*" \
        --rule-type PathBasedRouting
}

update_azure_traffic_split() {
    local percentage="$1"
    log_info "Updating Azure traffic split to ${percentage}% canary"
    
    # Update Azure Traffic Manager endpoint weights
    if az network traffic-manager profile show \
        --resource-group "$RESOURCE_GROUP" \
        --name "file-transfer-tm-${ENVIRONMENT}" &> /dev/null; then
        
        # Update canary endpoint weight
        az network traffic-manager endpoint update \
            --resource-group "$RESOURCE_GROUP" \
            --profile-name "file-transfer-tm-${ENVIRONMENT}" \
            --name "endpoint-canary" \
            --type azureEndpoints \
            --weight "$percentage"
        
        # Update production endpoint weight
        local production_percentage=$((100 - percentage))
        az network traffic-manager endpoint update \
            --resource-group "$RESOURCE_GROUP" \
            --profile-name "file-transfer-tm-${ENVIRONMENT}" \
            --name "endpoint-production" \
            --type azureEndpoints \
            --weight "$production_percentage"
    fi
    
    # Wait for traffic split to take effect
    sleep 60
}

monitor_azure_canary_deployment() {
    local percentage="$1"
    local duration="$2"
    log_info "Monitoring Azure canary deployment at ${percentage}% using Application Insights"
    
    local end_time=$(($(date +%s) + duration))
    local app_insights_name="file-transfer-insights-${ENVIRONMENT}"
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check canary health in AKS
        if ! health_check_azure_canary; then
            log_error "Azure canary health check failed"
            return 1
        fi
        
        # Check canary error rates in Application Insights
        if ! check_azure_canary_error_rates "$app_insights_name" "$percentage"; then
            log_error "Azure canary error rates too high"
            return 1
        fi
        
        # Check canary performance in Application Insights
        if ! check_azure_canary_performance "$app_insights_name" "$percentage"; then
            log_error "Azure canary performance degraded"
            return 1
        fi
        
        sleep 30
    done
    
    return 0
}

# Record deployment in Azure
record_azure_deployment_success() {
    log_info "Recording Azure deployment success"
    
    # Create deployment record in Azure Key Vault
    local deployment_record=$(cat << EOF
{
  "deploymentId": "$(uuidgen)",
  "strategy": "$DEPLOYMENT_STRATEGY",
  "imageTag": "$IMAGE_TAG",
  "environment": "$ENVIRONMENT",
  "resourceGroup": "$RESOURCE_GROUP",
  "aksCluster": "$AKS_CLUSTER",
  "namespace": "$NAMESPACE",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "deployedBy": "${BUILD_REQUESTEDFOR:-$(whoami)}",
  "buildId": "${BUILD_BUILDID:-unknown}",
  "gitCommit": "${BUILD_SOURCEVERSION:-$(git rev-parse HEAD)}",
  "gitBranch": "${BUILD_SOURCEBRANCH:-$(git branch --show-current)}",
  "status": "SUCCESS",
  "azureRegion": "$(az account list-locations --query "[?name=='$(az configure --defaults location --output tsv 2>/dev/null || echo 'eastus')'].displayName" --output tsv)"
}
EOF
)

    # Store in Azure Key Vault
    az keyvault secret set \
        --vault-name "file-transfer-kv-${ENVIRONMENT}" \
        --name "deployment-record-${IMAGE_TAG}" \
        --value "$deployment_record"
    
    # Also store in AKS ConfigMap
    kubectl create configmap "azure-deployment-record-${IMAGE_TAG}" \
        --from-literal="deployment-record=$deployment_record" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    log_success "Azure deployment record created"
}

# Main execution
main() {
    log_info "Starting File Transfer System Azure Deployment"
    log_info "Strategy: $DEPLOYMENT_STRATEGY"
    log_info "Image Tag: $IMAGE_TAG"
    log_info "Environment: $ENVIRONMENT"
    log_info "Resource Group: $RESOURCE_GROUP"
    log_info "AKS Cluster: $AKS_CLUSTER"
    log_info "Namespace: $NAMESPACE"
    
    # Validate Azure environment
    validate_azure_environment
    
    # Execute deployment strategy
    case "$DEPLOYMENT_STRATEGY" in
        "blue-green")
            deploy_blue_green_azure
            ;;
        "canary")
            deploy_canary_azure
            ;;
        "rolling")
            deploy_rolling_azure
            ;;
        *)
            log_error "Unknown deployment strategy: $DEPLOYMENT_STRATEGY"
            log_error "Supported strategies: blue-green, canary, rolling"
            exit 1
            ;;
    esac
    
    # Record deployment in Azure
    record_azure_deployment_success
    
    log_success "Azure deployment completed successfully!"
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Azure Deployment Strategy Script"
    echo
    echo "Usage: $0 [strategy] [image_tag] [environment]"
    echo
    echo "Arguments:"
    echo "  strategy     Deployment strategy (blue-green|canary|rolling)"
    echo "  image_tag    Docker image tag to deploy"
    echo "  environment  Target environment (staging|production)"
    echo
    echo "Azure Resources:"
    echo "  Resource Group: file-transfer-[environment]"
    echo "  AKS Cluster: file-transfer-aks-[environment]"
    echo "  Container Registry: filetransferregistry.azurecr.io"
    echo "  Key Vault: file-transfer-kv-[environment]"
    echo
    echo "Examples:"
    echo "  $0 blue-green v1.2.0 staging"
    echo "  $0 canary latest production"
    echo "  $0 rolling feature-branch staging"
    echo
    exit 0
fi

# Execute main function
main "$@"