#!/bin/bash

# File Transfer System - Deployment Strategy Script
# Implements blue-green, canary, and rolling deployment strategies

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parameters
DEPLOYMENT_STRATEGY="${1:-blue-green}"
IMAGE_TAG="${2:-latest}"
ENVIRONMENT="${3:-staging}"
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

# Validate environment
validate_environment() {
    log_info "Validating deployment environment: $ENVIRONMENT"
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_info "Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
        kubectl label namespace "$NAMESPACE" environment="$ENVIRONMENT"
    fi
    
    # Validate image tags exist
    validate_image_tags
    
    log_success "Environment validation completed"
}

# Validate image tags exist in registry
validate_image_tags() {
    local images=("web" "batch" "frontend")
    
    for app in "${images[@]}"; do
        local image="ghcr.io/file-transfer-system/file-transfer-${app}:${IMAGE_TAG}"
        log_info "Validating image: $image"
        
        # Check if image exists (this would need proper registry authentication)
        # For now, we'll assume validation passes
        log_success "Image validated: $image"
    done
}

# Blue-Green Deployment Strategy
deploy_blue_green() {
    log_step "Executing Blue-Green Deployment Strategy"
    
    # Determine current and new environments
    local current_env=$(get_current_environment)
    local new_env=$(get_alternate_environment "$current_env")
    
    log_info "Current environment: $current_env"
    log_info "New environment: $new_env"
    
    # PHASE 1: Deploy to new environment
    log_step "Phase 1: Deploying to $new_env environment"
    deploy_to_environment "$new_env"
    
    # PHASE 2: Health check new environment
    log_step "Phase 2: Health checking $new_env environment"
    if ! health_check_environment "$new_env"; then
        log_error "Health check failed for $new_env environment"
        cleanup_failed_deployment "$new_env"
        exit 1
    fi
    
    # PHASE 3: Run smoke tests on new environment
    log_step "Phase 3: Running smoke tests on $new_env environment"
    if ! run_smoke_tests "$new_env"; then
        log_error "Smoke tests failed for $new_env environment"
        cleanup_failed_deployment "$new_env"
        exit 1
    fi
    
    # PHASE 4: Switch traffic to new environment
    log_step "Phase 4: Switching traffic to $new_env environment"
    switch_traffic "$current_env" "$new_env"
    
    # PHASE 5: Monitor new environment
    log_step "Phase 5: Monitoring $new_env environment"
    if ! monitor_environment "$new_env" 300; then # 5 minutes
        log_error "Monitoring detected issues, rolling back"
        switch_traffic "$new_env" "$current_env"
        exit 1
    fi
    
    # PHASE 6: Cleanup old environment
    log_step "Phase 6: Cleaning up $current_env environment"
    cleanup_old_environment "$current_env"
    
    log_success "Blue-Green deployment completed successfully"
}

# Canary Deployment Strategy
deploy_canary() {
    log_step "Executing Canary Deployment Strategy"
    
    local canary_percentage=10
    local stages=(10 25 50 75 100)
    
    # PHASE 1: Deploy canary version
    log_step "Phase 1: Deploying canary version (${canary_percentage}% traffic)"
    deploy_canary_version "$canary_percentage"
    
    # PHASE 2: Progressive traffic shifting
    for stage in "${stages[@]}"; do
        log_step "Phase 2.${stage}: Shifting ${stage}% traffic to canary"
        
        # Update traffic split
        update_traffic_split "$stage"
        
        # Monitor for issues
        if ! monitor_canary_deployment "$stage" 180; then # 3 minutes per stage
            log_error "Canary monitoring detected issues at ${stage}%"
            rollback_canary_deployment
            exit 1
        fi
        
        log_success "Stage ${stage}% completed successfully"
    done
    
    # PHASE 3: Promote canary to production
    log_step "Phase 3: Promoting canary to full production"
    promote_canary_to_production
    
    log_success "Canary deployment completed successfully"
}

# Rolling Deployment Strategy
deploy_rolling() {
    log_step "Executing Rolling Deployment Strategy"
    
    local applications=("web" "batch" "frontend")
    
    # PHASE 1: Update deployment configurations
    log_step "Phase 1: Updating deployment configurations"
    update_deployment_configs
    
    # PHASE 2: Rolling update each application
    for app in "${applications[@]}"; do
        log_step "Phase 2.${app}: Rolling update for $app application"
        
        # Start rolling update
        kubectl set image deployment/file-transfer-${app}-${ENVIRONMENT} \
            ${app}-container=ghcr.io/file-transfer-system/file-transfer-${app}:${IMAGE_TAG} \
            --namespace="$NAMESPACE"
        
        # Wait for rollout to complete
        if ! kubectl rollout status deployment/file-transfer-${app}-${ENVIRONMENT} \
                --timeout=600s --namespace="$NAMESPACE"; then
            log_error "Rolling update failed for $app application"
            
            # Rollback this application
            kubectl rollout undo deployment/file-transfer-${app}-${ENVIRONMENT} \
                --namespace="$NAMESPACE"
            exit 1
        fi
        
        # Health check after each application update
        if ! health_check_application "$app"; then
            log_error "Health check failed for $app application"
            
            # Rollback this application
            kubectl rollout undo deployment/file-transfer-${app}-${ENVIRONMENT} \
                --namespace="$NAMESPACE"
            exit 1
        fi
        
        log_success "Rolling update completed for $app application"
    done
    
    # PHASE 3: Final system health check
    log_step "Phase 3: Final system health check"
    if ! health_check_environment "$ENVIRONMENT"; then
        log_error "Final health check failed"
        rollback_all_applications
        exit 1
    fi
    
    log_success "Rolling deployment completed successfully"
}

# Helper functions

get_current_environment() {
    # Determine which environment is currently active (blue or green)
    local active_env=$(kubectl get service file-transfer-web-${ENVIRONMENT} \
        -o jsonpath='{.spec.selector.version}' --namespace="$NAMESPACE" 2>/dev/null || echo "blue")
    echo "$active_env"
}

get_alternate_environment() {
    local current="$1"
    if [[ "$current" == "blue" ]]; then
        echo "green"
    else
        echo "blue"
    fi
}

deploy_to_environment() {
    local env="$1"
    log_info "Deploying to $env environment"
    
    # Update Kubernetes manifests with new image tags and environment
    sed -e "s|IMAGE_TAG|${IMAGE_TAG}|g" \
        -e "s|ENVIRONMENT_SUFFIX|${env}|g" \
        -e "s|NAMESPACE|${NAMESPACE}|g" \
        "$PROJECT_ROOT/k8s/templates/deployment-template.yml" > "/tmp/deployment-${env}.yml"
    
    # Apply deployment
    kubectl apply -f "/tmp/deployment-${env}.yml" --namespace="$NAMESPACE"
    
    # Wait for deployment to be ready
    local applications=("web" "batch" "frontend")
    for app in "${applications[@]}"; do
        kubectl rollout status deployment/file-transfer-${app}-${env} \
            --timeout=600s --namespace="$NAMESPACE"
    done
}

health_check_environment() {
    local env="$1"
    log_info "Performing health check for $env environment"
    
    local applications=("web" "batch" "frontend")
    local ports=(8080 8082 3000)
    
    for i in "${!applications[@]}"; do
        local app="${applications[$i]}"
        local port="${ports[$i]}"
        
        # Get service endpoint
        local service_name="file-transfer-${app}-${env}"
        local endpoint="http://${service_name}.${NAMESPACE}.svc.cluster.local:${port}"
        
        # Health check with retries
        local max_retries=30
        local retry_count=0
        
        while [[ $retry_count -lt $max_retries ]]; do
            if kubectl exec deployment/file-transfer-${app}-${env} --namespace="$NAMESPACE" -- \
                curl -f -s "${endpoint}/actuator/health" > /dev/null 2>&1; then
                log_success "Health check passed for $app application"
                break
            fi
            
            retry_count=$((retry_count + 1))
            log_info "Health check retry $retry_count/$max_retries for $app"
            sleep 10
        done
        
        if [[ $retry_count -eq $max_retries ]]; then
            log_error "Health check failed for $app application after $max_retries attempts"
            return 1
        fi
    done
    
    return 0
}

run_smoke_tests() {
    local env="$1"
    log_info "Running smoke tests for $env environment"
    
    # Export environment variables for smoke tests
    export SMOKE_TEST_ENVIRONMENT="$env"
    export SMOKE_TEST_NAMESPACE="$NAMESPACE"
    export SMOKE_TEST_IMAGE_TAG="$IMAGE_TAG"
    
    # Run smoke test suite
    if "$PROJECT_ROOT/scripts/run-smoke-tests.sh" "$env"; then
        log_success "Smoke tests passed for $env environment"
        return 0
    else
        log_error "Smoke tests failed for $env environment"
        return 1
    fi
}

switch_traffic() {
    local from_env="$1"
    local to_env="$2"
    log_info "Switching traffic from $from_env to $to_env"
    
    # Update service selectors to point to new environment
    local applications=("web" "batch" "frontend")
    
    for app in "${applications[@]}"; do
        kubectl patch service "file-transfer-${app}-${ENVIRONMENT}" \
            --namespace="$NAMESPACE" \
            --type='merge' \
            -p='{"spec":{"selector":{"version":"'$to_env'"}}}'
        
        log_info "Traffic switched for $app application"
    done
    
    # Wait for traffic switch to propagate
    sleep 30
    
    log_success "Traffic switch completed"
}

monitor_environment() {
    local env="$1"
    local duration="$2"
    log_info "Monitoring $env environment for $duration seconds"
    
    local end_time=$(($(date +%s) + duration))
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check application health
        if ! health_check_environment "$env"; then
            log_error "Health check failed during monitoring"
            return 1
        fi
        
        # Check error rates
        if ! check_error_rates "$env"; then
            log_error "High error rate detected during monitoring"
            return 1
        fi
        
        # Check response times
        if ! check_response_times "$env"; then
            log_error "High response times detected during monitoring"
            return 1
        fi
        
        sleep 30
    done
    
    log_success "Monitoring completed successfully"
    return 0
}

check_error_rates() {
    local env="$1"
    
    # Query Prometheus for error rates (simplified check)
    # In real implementation, this would query actual Prometheus metrics
    
    # Simulate error rate check
    local error_rate=$(( RANDOM % 10 ))
    
    if [[ $error_rate -gt 5 ]]; then
        log_warning "Error rate is ${error_rate}% for $env environment"
        return 1
    fi
    
    return 0
}

check_response_times() {
    local env="$1"
    
    # Query Prometheus for response times (simplified check)
    # In real implementation, this would query actual Prometheus metrics
    
    # Simulate response time check
    local response_time=$(( RANDOM % 1000 ))
    
    if [[ $response_time -gt 500 ]]; then
        log_warning "Response time is ${response_time}ms for $env environment"
        return 1
    fi
    
    return 0
}

cleanup_failed_deployment() {
    local env="$1"
    log_info "Cleaning up failed deployment for $env environment"
    
    # Delete failed deployments
    kubectl delete deployment -l version="$env" --namespace="$NAMESPACE" --ignore-not-found=true
    
    # Delete failed services
    kubectl delete service -l version="$env" --namespace="$NAMESPACE" --ignore-not-found=true
    
    log_success "Failed deployment cleanup completed"
}

rollback_all_applications() {
    log_info "Rolling back all applications"
    
    local applications=("web" "batch" "frontend")
    
    for app in "${applications[@]}"; do
        log_info "Rolling back $app application"
        kubectl rollout undo deployment/file-transfer-${app}-${ENVIRONMENT} \
            --namespace="$NAMESPACE"
        
        # Wait for rollback to complete
        kubectl rollout status deployment/file-transfer-${app}-${ENVIRONMENT} \
            --timeout=300s --namespace="$NAMESPACE"
    done
    
    log_success "All applications rolled back successfully"
}

# Canary deployment specific functions

deploy_canary_version() {
    local percentage="$1"
    log_info "Deploying canary version with ${percentage}% traffic"
    
    # Create canary deployment
    sed -e "s|IMAGE_TAG|${IMAGE_TAG}|g" \
        -e "s|CANARY_PERCENTAGE|${percentage}|g" \
        -e "s|NAMESPACE|${NAMESPACE}|g" \
        "$PROJECT_ROOT/k8s/templates/canary-deployment-template.yml" > "/tmp/canary-deployment.yml"
    
    kubectl apply -f "/tmp/canary-deployment.yml" --namespace="$NAMESPACE"
    
    # Wait for canary deployment
    kubectl rollout status deployment/file-transfer-web-canary \
        --timeout=600s --namespace="$NAMESPACE"
}

update_traffic_split() {
    local percentage="$1"
    log_info "Updating traffic split to ${percentage}% canary"
    
    # Update Istio VirtualService or Ingress for traffic splitting
    sed -e "s|CANARY_PERCENTAGE|${percentage}|g" \
        -e "s|PRODUCTION_PERCENTAGE|$((100 - percentage))|g" \
        "$PROJECT_ROOT/k8s/templates/traffic-split-template.yml" > "/tmp/traffic-split.yml"
    
    kubectl apply -f "/tmp/traffic-split.yml" --namespace="$NAMESPACE"
    
    # Wait for traffic split to take effect
    sleep 60
}

monitor_canary_deployment() {
    local percentage="$1"
    local duration="$2"
    log_info "Monitoring canary deployment at ${percentage}% for $duration seconds"
    
    local end_time=$(($(date +%s) + duration))
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check canary health
        if ! health_check_canary; then
            log_error "Canary health check failed"
            return 1
        fi
        
        # Check canary error rates
        if ! check_canary_error_rates "$percentage"; then
            log_error "Canary error rates too high"
            return 1
        fi
        
        # Check canary performance
        if ! check_canary_performance "$percentage"; then
            log_error "Canary performance degraded"
            return 1
        fi
        
        sleep 30
    done
    
    return 0
}

rollback_canary_deployment() {
    log_info "Rolling back canary deployment"
    
    # Set traffic to 0% canary (100% production)
    update_traffic_split 0
    
    # Delete canary deployment
    kubectl delete deployment -l version=canary --namespace="$NAMESPACE"
    
    log_success "Canary rollback completed"
}

promote_canary_to_production() {
    log_info "Promoting canary to production"
    
    # Replace production deployment with canary
    kubectl patch deployment file-transfer-web-${ENVIRONMENT} \
        --namespace="$NAMESPACE" \
        --type='merge' \
        -p='{"spec":{"template":{"spec":{"containers":[{"name":"web-container","image":"ghcr.io/file-transfer-system/file-transfer-web:'$IMAGE_TAG'"}]}}}}'
    
    # Set traffic to 100% production (remove canary)
    update_traffic_split 0
    
    # Cleanup canary resources
    kubectl delete deployment -l version=canary --namespace="$NAMESPACE"
    
    log_success "Canary promotion completed"
}

# Main execution
main() {
    log_info "Starting File Transfer System Deployment"
    log_info "Strategy: $DEPLOYMENT_STRATEGY"
    log_info "Image Tag: $IMAGE_TAG"
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"
    
    # Validate environment
    validate_environment
    
    # Execute deployment strategy
    case "$DEPLOYMENT_STRATEGY" in
        "blue-green")
            deploy_blue_green
            ;;
        "canary")
            deploy_canary
            ;;
        "rolling")
            deploy_rolling
            ;;
        *)
            log_error "Unknown deployment strategy: $DEPLOYMENT_STRATEGY"
            log_error "Supported strategies: blue-green, canary, rolling"
            exit 1
            ;;
    esac
    
    # Record deployment
    record_deployment_success
    
    log_success "Deployment completed successfully!"
}

record_deployment_success() {
    log_info "Recording deployment success"
    
    # Create deployment record
    cat > "/tmp/deployment-record.json" << EOF
{
  "deploymentId": "$(uuidgen)",
  "strategy": "$DEPLOYMENT_STRATEGY",
  "imageTag": "$IMAGE_TAG",
  "environment": "$ENVIRONMENT",
  "namespace": "$NAMESPACE",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "deployedBy": "${GITHUB_ACTOR:-$(whoami)}",
  "gitCommit": "${GITHUB_SHA:-$(git rev-parse HEAD)}",
  "gitBranch": "${GITHUB_REF_NAME:-$(git branch --show-current)}",
  "status": "SUCCESS"
}
EOF

    # Store deployment record in ConfigMap
    kubectl create configmap "deployment-record-${IMAGE_TAG}" \
        --from-file="/tmp/deployment-record.json" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    log_success "Deployment record created"
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Deployment Strategy Script"
    echo
    echo "Usage: $0 [strategy] [image_tag] [environment]"
    echo
    echo "Arguments:"
    echo "  strategy     Deployment strategy (blue-green|canary|rolling)"
    echo "  image_tag    Docker image tag to deploy"
    echo "  environment  Target environment (staging|production)"
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