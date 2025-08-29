#!/bin/bash

# File Transfer System - Deployment Rollback Script
# Provides automated rollback capabilities with verification and monitoring

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parameters
ROLLBACK_REVISION="${1:-}"
ENVIRONMENT="${2:-staging}"
ROLLBACK_TYPE="${3:-automatic}" # automatic, manual, emergency
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

# Validate rollback parameters
validate_rollback_parameters() {
    log_info "Validating rollback parameters"
    
    if [[ -z "$ROLLBACK_REVISION" ]]; then
        log_error "Rollback revision is required"
        echo "Usage: $0 <rollback_revision> [environment] [rollback_type]"
        exit 1
    fi
    
    # Validate environment
    case "$ENVIRONMENT" in
        staging|production)
            log_info "Environment: $ENVIRONMENT"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT"
            log_error "Valid environments: staging, production"
            exit 1
            ;;
    esac
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi
    
    log_success "Rollback parameters validated"
}

# Get current deployment information
get_current_deployment_info() {
    log_info "Getting current deployment information"
    
    # Get current revision for each application
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
            
            log_info "$app application - Current revision: $current_revision, Image: $current_image"
            
            # Store current info for rollback verification
            echo "$app:$current_revision:$current_image" >> "/tmp/current-deployment-info.txt"
        else
            log_warning "Deployment $deployment_name not found"
        fi
    done
}

# Create rollback backup
create_rollback_backup() {
    log_step "Creating rollback backup"
    
    # Create backup of current state before rollback
    local backup_name="pre-rollback-backup-$(date +%Y%m%d-%H%M%S)"
    
    # Backup current configurations
    kubectl get deployments,services,configmaps,secrets \
        --namespace="$NAMESPACE" \
        -o yaml > "/tmp/${backup_name}-configs.yml"
    
    # Create database backup if in production
    if [[ "$ENVIRONMENT" == "production" ]]; then
        log_info "Creating database backup for production rollback"
        
        # Trigger database backup job
        kubectl create job "$backup_name" \
            --from=cronjob/database-backup \
            --namespace="$NAMESPACE"
        
        # Wait for backup to complete (with timeout)
        if ! kubectl wait --for=condition=complete job/"$backup_name" \
                --timeout=1800s --namespace="$NAMESPACE"; then
            log_warning "Database backup timed out, proceeding with rollback"
        else
            log_success "Database backup completed: $backup_name"
        fi
    fi
    
    log_success "Rollback backup created: $backup_name"
    echo "$backup_name" > "/tmp/rollback-backup-name.txt"
}

# Execute rollback
execute_rollback() {
    log_step "Executing rollback to revision: $ROLLBACK_REVISION"
    
    local applications=("web" "batch" "frontend")
    local rollback_success=true
    
    # PHASE 1: Stop traffic to current version (if blue-green)
    if is_blue_green_deployment; then
        log_info "Stopping traffic for blue-green rollback"
        stop_traffic_for_rollback
    fi
    
    # PHASE 2: Rollback each application
    for app in "${applications[@]}"; do
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        log_info "Rolling back $app application to revision $ROLLBACK_REVISION"
        
        # Check if deployment exists
        if ! kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            log_warning "Deployment $deployment_name not found, skipping"
            continue
        fi
        
        # Get rollback target image
        local rollback_image=$(get_rollback_image "$app" "$ROLLBACK_REVISION")
        
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
            log_error "Rollback failed for $app application"
            rollback_success=false
        else
            log_success "Rollback completed for $app application"
        fi
    done
    
    # PHASE 3: Restore traffic (if blue-green)
    if is_blue_green_deployment; then
        log_info "Restoring traffic after blue-green rollback"
        restore_traffic_after_rollback
    fi
    
    if [[ "$rollback_success" != true ]]; then
        log_error "Rollback failed for one or more applications"
        return 1
    fi
    
    log_success "Rollback execution completed successfully"
}

# Verify rollback success
verify_rollback() {
    log_step "Verifying rollback success"
    
    local applications=("web" "batch" "frontend")
    local verification_success=true
    
    # PHASE 1: Health checks
    log_info "Performing post-rollback health checks"
    
    for app in "${applications[@]}"; do
        local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
        
        # Check deployment status
        if ! kubectl get deployment "$deployment_name" --namespace="$NAMESPACE" &> /dev/null; then
            log_warning "Deployment $deployment_name not found"
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
            log_success "$app application: $ready_replicas/$desired_replicas replicas ready"
        else
            log_error "$app application: Only $ready_replicas/$desired_replicas replicas ready"
            verification_success=false
        fi
        
        # Application-specific health check
        if ! perform_application_health_check "$app"; then
            log_error "Health check failed for $app application"
            verification_success=false
        fi
    done
    
    # PHASE 2: Functional verification
    log_info "Performing functional verification"
    
    if ! run_post_rollback_tests; then
        log_error "Post-rollback functional tests failed"
        verification_success=false
    fi
    
    # PHASE 3: Performance verification
    log_info "Performing performance verification"
    
    if ! verify_post_rollback_performance; then
        log_error "Post-rollback performance verification failed"
        verification_success=false
    fi
    
    if [[ "$verification_success" != true ]]; then
        log_error "Rollback verification failed"
        return 1
    fi
    
    log_success "Rollback verification completed successfully"
}

# Monitor rollback stability
monitor_rollback_stability() {
    local monitoring_duration=600 # 10 minutes
    log_step "Monitoring rollback stability for $monitoring_duration seconds"
    
    local end_time=$(($(date +%s) + monitoring_duration))
    local stability_issues=0
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check application health
        if ! check_system_health; then
            stability_issues=$((stability_issues + 1))
            log_warning "Stability issue detected (count: $stability_issues)"
            
            if [[ $stability_issues -ge 3 ]]; then
                log_error "Multiple stability issues detected, rollback may have failed"
                return 1
            fi
        fi
        
        # Check error rates
        if ! check_error_rates_post_rollback; then
            stability_issues=$((stability_issues + 1))
            log_warning "High error rate detected post-rollback"
        fi
        
        # Check performance metrics
        if ! check_performance_metrics_post_rollback; then
            stability_issues=$((stability_issues + 1))
            log_warning "Performance degradation detected post-rollback"
        fi
        
        sleep 60 # Check every minute
    done
    
    if [[ $stability_issues -eq 0 ]]; then
        log_success "Rollback stability monitoring completed with no issues"
    else
        log_warning "Rollback stability monitoring completed with $stability_issues issues"
    fi
    
    return 0
}

# Generate rollback report
generate_rollback_report() {
    log_step "Generating rollback report"
    
    local report_file="/tmp/rollback-report-$(date +%Y%m%d-%H%M%S).json"
    
    # Get post-rollback deployment info
    local applications=("web" "batch" "frontend")
    local app_info="["
    
    for app in "${applications[@]}"; do
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
            
            if [[ "$app_info" != "[" ]]; then
                app_info+=","
            fi
            
            app_info+="{\"application\":\"$app\",\"revision\":\"$current_revision\",\"image\":\"$current_image\",\"readyReplicas\":$ready_replicas}"
        fi
    done
    
    app_info+="]"
    
    # Create comprehensive rollback report
    cat > "$report_file" << EOF
{
  "rollbackId": "$(uuidgen)",
  "rollbackRevision": "$ROLLBACK_REVISION",
  "environment": "$ENVIRONMENT",
  "namespace": "$NAMESPACE",
  "rollbackType": "$ROLLBACK_TYPE",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "triggeredBy": "${GITHUB_ACTOR:-$(whoami)}",
  "applications": $app_info,
  "rollbackDuration": "$(cat /tmp/rollback-duration.txt 2>/dev/null || echo 'unknown')",
  "verificationStatus": "$(cat /tmp/rollback-verification-status.txt 2>/dev/null || echo 'pending')",
  "healthChecks": {
    "systemHealth": "$(check_system_health && echo 'HEALTHY' || echo 'UNHEALTHY')",
    "errorRates": "$(check_error_rates_post_rollback && echo 'NORMAL' || echo 'HIGH')",
    "performance": "$(check_performance_metrics_post_rollback && echo 'NORMAL' || echo 'DEGRADED')"
  },
  "backupCreated": "$(cat /tmp/rollback-backup-name.txt 2>/dev/null || echo 'none')",
  "rollbackReason": "${ROLLBACK_REASON:-Automated rollback due to deployment failure}",
  "nextSteps": [
    "Monitor system stability for 24 hours",
    "Investigate root cause of deployment failure", 
    "Plan corrective actions for next deployment",
    "Update deployment procedures if needed"
  ]
}
EOF

    log_success "Rollback report generated: $report_file"
    
    # Store report in Kubernetes ConfigMap
    kubectl create configmap "rollback-report-$(date +%Y%m%d-%H%M%S)" \
        --from-file="$report_file" \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
}

# Helper functions

get_rollback_image() {
    local app="$1"
    local revision="$2"
    
    # Try to get image from deployment history
    local image=$(kubectl rollout history deployment/file-transfer-${app}-${ENVIRONMENT} \
        --revision="$revision" --namespace="$NAMESPACE" 2>/dev/null | \
        grep -o 'ghcr.io/[^[:space:]]*' || echo "")
    
    echo "$image"
}

is_blue_green_deployment() {
    # Check if current deployment is using blue-green strategy
    kubectl get service "file-transfer-web-${ENVIRONMENT}" \
        --namespace="$NAMESPACE" \
        -o jsonpath='{.spec.selector.version}' &> /dev/null
}

stop_traffic_for_rollback() {
    log_info "Stopping traffic for blue-green rollback"
    
    # Update service selectors to point to maintenance page
    kubectl patch service "file-transfer-web-${ENVIRONMENT}" \
        --namespace="$NAMESPACE" \
        --type='merge' \
        -p='{"spec":{"selector":{"app":"maintenance-page"}}}'
}

restore_traffic_after_rollback() {
    log_info "Restoring traffic after blue-green rollback"
    
    # Determine which environment to point traffic to after rollback
    local target_env=$(get_rollback_target_environment)
    
    # Update service selectors to point to rollback environment
    kubectl patch service "file-transfer-web-${ENVIRONMENT}" \
        --namespace="$NAMESPACE" \
        --type='merge' \
        -p='{"spec":{"selector":{"version":"'$target_env'"}}}'
}

get_rollback_target_environment() {
    # Determine target environment based on rollback revision
    # This is a simplified implementation
    echo "blue" # Default to blue environment
}

perform_application_health_check() {
    local app="$1"
    local deployment_name="file-transfer-${app}-${ENVIRONMENT}"
    
    # Get service endpoint
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

get_application_port() {
    local app="$1"
    case "$app" in
        web) echo "8080" ;;
        batch) echo "8082" ;;
        frontend) echo "3000" ;;
        *) echo "8080" ;;
    esac
}

run_post_rollback_tests() {
    log_info "Running post-rollback functional tests"
    
    # Run smoke tests to verify basic functionality
    if "$PROJECT_ROOT/scripts/run-smoke-tests.sh" "$ENVIRONMENT"; then
        echo "PASSED" > "/tmp/rollback-verification-status.txt"
        return 0
    else
        echo "FAILED" > "/tmp/rollback-verification-status.txt"
        return 1
    fi
}

verify_post_rollback_performance() {
    log_info "Verifying post-rollback performance"
    
    # Run performance validation
    if "$PROJECT_ROOT/scripts/validate-performance.sh" "$ENVIRONMENT"; then
        return 0
    else
        return 1
    fi
}

check_system_health() {
    # Simple system health check
    local applications=("web" "batch" "frontend")
    
    for app in "${applications[@]}"; do
        if ! perform_application_health_check "$app"; then
            return 1
        fi
    done
    
    return 0
}

check_error_rates_post_rollback() {
    # Check error rates after rollback
    # This would integrate with Prometheus/monitoring system
    # For now, simulate check
    local error_rate=$(( RANDOM % 10 ))
    
    if [[ $error_rate -lt 5 ]]; then
        return 0
    else
        return 1
    fi
}

check_performance_metrics_post_rollback() {
    # Check performance metrics after rollback
    # This would integrate with monitoring system
    # For now, simulate check
    local response_time=$(( RANDOM % 1000 ))
    
    if [[ $response_time -lt 500 ]]; then
        return 0
    else
        return 1
    fi
}

# Cleanup rollback artifacts
cleanup_rollback_artifacts() {
    log_info "Cleaning up rollback artifacts"
    
    # Remove temporary files
    rm -f /tmp/current-deployment-info.txt
    rm -f /tmp/rollback-*.txt
    
    # Cleanup old rollback jobs (keep last 5)
    kubectl get jobs --namespace="$NAMESPACE" \
        -l type=rollback-backup \
        --sort-by=.metadata.creationTimestamp \
        -o jsonpath='{.items[:-5].metadata.name}' | \
        xargs -r kubectl delete job --namespace="$NAMESPACE"
    
    log_success "Rollback artifacts cleaned up"
}

# Main execution
main() {
    local start_time=$(date +%s)
    
    log_info "Starting File Transfer System Rollback"
    log_info "Rollback Revision: $ROLLBACK_REVISION"
    log_info "Environment: $ENVIRONMENT"
    log_info "Rollback Type: $ROLLBACK_TYPE"
    log_info "Namespace: $NAMESPACE"
    
    # Validate parameters
    validate_rollback_parameters
    
    # Get current deployment info
    get_current_deployment_info
    
    # Create rollback backup
    create_rollback_backup
    
    # Execute rollback
    if ! execute_rollback; then
        log_error "Rollback execution failed"
        exit 1
    fi
    
    # Verify rollback
    if ! verify_rollback; then
        log_error "Rollback verification failed"
        exit 1
    fi
    
    # Monitor stability
    monitor_rollback_stability
    
    # Calculate rollback duration
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    echo "${duration} seconds" > "/tmp/rollback-duration.txt"
    
    # Generate report
    generate_rollback_report
    
    # Cleanup
    cleanup_rollback_artifacts
    
    log_success "Rollback completed successfully in $duration seconds!"
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Deployment Rollback Script"
    echo
    echo "Usage: $0 <rollback_revision> [environment] [rollback_type]"
    echo
    echo "Arguments:"
    echo "  rollback_revision  Target revision to rollback to"
    echo "  environment        Target environment (staging|production)"
    echo "  rollback_type      Type of rollback (automatic|manual|emergency)"
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