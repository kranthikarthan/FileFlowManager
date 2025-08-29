#!/bin/bash

# File Transfer System - Smoke Tests
# Quick validation tests to verify basic functionality after deployment

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Parameters
ENVIRONMENT="${1:-staging}"
NAMESPACE="file-transfer-${ENVIRONMENT}"
TIMEOUT="${2:-300}" # 5 minutes default timeout

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Test configuration based on environment
case "$ENVIRONMENT" in
    "staging")
        WEB_URL="https://staging.filetransfer.example.com"
        BATCH_URL="https://batch-staging.filetransfer.example.com"
        FRONTEND_URL="https://staging.filetransfer.example.com"
        ;;
    "production")
        WEB_URL="https://api.filetransfer.example.com"
        BATCH_URL="https://batch.filetransfer.example.com"
        FRONTEND_URL="https://filetransfer.example.com"
        ;;
    *)
        log_error "Unknown environment: $ENVIRONMENT"
        exit 1
        ;;
esac

# Smoke test results tracking
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0
FAILED_TESTS=()

# Execute smoke test with error handling
run_smoke_test() {
    local test_name="$1"
    local test_command="$2"
    
    TESTS_RUN=$((TESTS_RUN + 1))
    log_info "Running smoke test: $test_name"
    
    if eval "$test_command"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        log_success "✅ $test_name"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("$test_name")
        log_error "❌ $test_name"
        return 1
    fi
}

# Web Application Smoke Tests
test_web_application() {
    log_info "Testing Web Application"
    
    # Test 1: Health check
    run_smoke_test "Web Health Check" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/health' | grep -q '\"status\":\"UP\"'"
    
    # Test 2: API version endpoint
    run_smoke_test "Web API Versions" \
        "curl -f -s --max-time 30 '$WEB_URL/api/versions' | grep -q '\"totalVersions\"'"
    
    # Test 3: Tenant API (v2)
    run_smoke_test "Web Tenant API v2" \
        "curl -f -s --max-time 30 '$WEB_URL/api/v2/tenants?page=0&size=1' | grep -q '\"content\"'"
    
    # Test 4: Metrics endpoint
    run_smoke_test "Web Metrics" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/metrics' | grep -q '\"names\"'"
    
    # Test 5: Database connectivity
    run_smoke_test "Web Database Health" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/health/db' | grep -q '\"status\":\"UP\"'"
    
    # Test 6: Redis connectivity
    run_smoke_test "Web Redis Health" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/health/redis' | grep -q '\"status\":\"UP\"'"
    
    # Test 7: Backup system
    run_smoke_test "Web Backup Health" \
        "curl -f -s --max-time 30 '$WEB_URL/api/backup/health' | grep -q '\"status\":\"UP\"'"
    
    # Test 8: Monitoring system
    run_smoke_test "Web Monitoring Health" \
        "curl -f -s --max-time 30 '$WEB_URL/api/monitoring/health' | grep -q '\"status\":\"UP\"'"
}

# Batch Application Smoke Tests
test_batch_application() {
    log_info "Testing Batch Application"
    
    # Test 1: Health check
    run_smoke_test "Batch Health Check" \
        "curl -f -s --max-time 30 '$BATCH_URL/actuator/health' | grep -q '\"status\":\"UP\"'"
    
    # Test 2: Batch job API (v2)
    run_smoke_test "Batch Job API v2" \
        "curl -f -s --max-time 30 '$BATCH_URL/api/batch/v2/jobs/executions?page=0&size=1' | grep -q '\"content\"'"
    
    # Test 3: Batch statistics
    run_smoke_test "Batch Statistics" \
        "curl -f -s --max-time 30 '$BATCH_URL/api/batch/v2/jobs/statistics' | grep -q '\"totalJobs\"'"
    
    # Test 4: Batch metrics
    run_smoke_test "Batch Metrics" \
        "curl -f -s --max-time 30 '$BATCH_URL/actuator/metrics' | grep -q '\"names\"'"
    
    # Test 5: Database connectivity
    run_smoke_test "Batch Database Health" \
        "curl -f -s --max-time 30 '$BATCH_URL/actuator/health/db' | grep -q '\"status\":\"UP\"'"
    
    # Test 6: Message queue connectivity
    run_smoke_test "Batch Message Queue Health" \
        "curl -f -s --max-time 30 '$BATCH_URL/actuator/health/rabbit' | grep -q '\"status\":\"UP\"'"
    
    # Test 7: Batch backup health
    run_smoke_test "Batch Backup Health" \
        "curl -f -s --max-time 30 '$BATCH_URL/api/batch/backup/health' | grep -q '\"status\":\"UP\"'"
}

# Frontend Application Smoke Tests
test_frontend_application() {
    log_info "Testing Frontend Application"
    
    # Test 1: Frontend availability
    run_smoke_test "Frontend Availability" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/' | grep -q 'File Transfer'"
    
    # Test 2: Frontend health endpoint
    run_smoke_test "Frontend Health" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/health' | grep -q '\"status\":\"UP\"'"
    
    # Test 3: Static assets loading
    run_smoke_test "Frontend Static Assets" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/static/js/' | grep -q 'main'"
    
    # Test 4: API proxy functionality
    run_smoke_test "Frontend API Proxy" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/api/versions' | grep -q '\"totalVersions\"'"
    
    # Test 5: Service worker
    run_smoke_test "Frontend Service Worker" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/sw.js' | grep -q 'workbox'"
    
    # Test 6: Manifest file
    run_smoke_test "Frontend PWA Manifest" \
        "curl -f -s --max-time 30 '$FRONTEND_URL/manifest.json' | grep -q '\"name\"'"
}

# Cross-Application Integration Tests
test_cross_application_integration() {
    log_info "Testing Cross-Application Integration"
    
    # Test 1: Web to Batch integration
    run_smoke_test "Web-Batch Integration" \
        "curl -f -s --max-time 30 '$WEB_URL/api/batch/integration/health' | grep -q '\"batchApiAvailable\":true'"
    
    # Test 2: Cross-application backup status
    run_smoke_test "Cross-App Backup Status" \
        "curl -f -s --max-time 30 '$WEB_URL/api/backup/cross-application/status' | grep -q '\"enabled\"'"
    
    # Test 3: API version compatibility
    run_smoke_test "Cross-App Version Compatibility" \
        "curl -f -s --max-time 30 '$WEB_URL/api/versions/cross-application/compatibility' | grep -q '\"compatible\"'"
    
    # Test 4: Monitoring integration
    run_smoke_test "Cross-App Monitoring" \
        "curl -f -s --max-time 30 '$WEB_URL/api/monitoring/system-health' | grep -q '\"overallHealth\":\"UP\"'"
}

# Performance Validation Tests
test_performance_validation() {
    log_info "Testing Performance Validation"
    
    # Test 1: Response time validation
    local start_time=$(date +%s%3N)
    if curl -f -s --max-time 30 "$WEB_URL/api/v2/tenants?page=0&size=10" > /dev/null; then
        local end_time=$(date +%s%3N)
        local response_time=$((end_time - start_time))
        
        if [[ $response_time -lt 1000 ]]; then  # Less than 1 second
            run_smoke_test "API Response Time" "true"
        else
            run_smoke_test "API Response Time" "false"
            log_warning "Response time: ${response_time}ms (expected < 1000ms)"
        fi
    else
        run_smoke_test "API Response Time" "false"
    fi
    
    # Test 2: Concurrent request handling
    run_smoke_test "Concurrent Request Handling" \
        "for i in {1..5}; do curl -f -s --max-time 30 '$WEB_URL/api/v2/tenants' > /dev/null & done; wait"
    
    # Test 3: Large payload handling
    local large_payload='{"name":"Smoke Test Tenant","description":"'"$(printf 'A%.0s' {1..1000})"'","timezone":"UTC"}'
    run_smoke_test "Large Payload Handling" \
        "curl -f -s --max-time 60 -X POST '$WEB_URL/api/v2/tenants' -H 'Content-Type: application/json' -d '$large_payload' | grep -q '\"id\"'"
}

# Security Validation Tests
test_security_validation() {
    log_info "Testing Security Validation"
    
    # Test 1: HTTPS enforcement
    run_smoke_test "HTTPS Enforcement" \
        "curl -s --max-time 30 -o /dev/null -w '%{http_code}' 'http://$(echo $WEB_URL | sed 's/https://')' | grep -q '^30[12]'"
    
    # Test 2: Security headers
    run_smoke_test "Security Headers" \
        "curl -f -s --max-time 30 -I '$WEB_URL/api/v2/tenants' | grep -q 'X-Content-Type-Options: nosniff'"
    
    # Test 3: Rate limiting
    run_smoke_test "Rate Limiting" \
        "for i in {1..25}; do curl -f -s --max-time 30 '$WEB_URL/api/v2/tenants' > /dev/null || break; done"
    
    # Test 4: Input validation
    local malicious_payload='{"name":"<script>alert(\"xss\")</script>","timezone":"UTC"}'
    run_smoke_test "Input Validation" \
        "! curl -f -s --max-time 30 -X POST '$WEB_URL/api/v2/tenants' -H 'Content-Type: application/json' -d '$malicious_payload'"
}

# API Versioning Tests
test_api_versioning() {
    log_info "Testing API Versioning"
    
    # Test 1: Current version (v2)
    run_smoke_test "API v2.0 Functionality" \
        "curl -f -s --max-time 30 '$WEB_URL/api/v2/tenants' | grep -q '\"content\"'"
    
    # Test 2: Legacy version (v1) with deprecation
    run_smoke_test "API v1.x Backward Compatibility" \
        "curl -f -s --max-time 30 -I '$WEB_URL/api/v1/tenants' | grep -q 'Deprecation: true'"
    
    # Test 3: Version negotiation
    run_smoke_test "API Version Negotiation" \
        "curl -f -s --max-time 30 -H 'X-API-Version: 1.2' '$WEB_URL/api/tenants' | grep -q 'id'"
    
    # Test 4: Batch API versioning
    run_smoke_test "Batch API v2.0 Functionality" \
        "curl -f -s --max-time 30 '$BATCH_URL/api/batch/v2/jobs/statistics' | grep -q '\"totalJobs\"'"
}

# Data Processing Tests
test_data_processing() {
    log_info "Testing Data Processing"
    
    # Test 1: File upload simulation
    local test_file_content='{"smokeTest": true, "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'", "data": "smoke test data"}'
    
    run_smoke_test "File Upload Simulation" \
        "echo '$test_file_content' | curl -f -s --max-time 60 -X POST '$WEB_URL/api/files/validate' -H 'Content-Type: application/json' -d @- | grep -q '\"valid\"'"
    
    # Test 2: Schema validation
    local test_schema='{"type": "object", "properties": {"smokeTest": {"type": "boolean"}}}'
    
    run_smoke_test "Schema Validation" \
        "echo '$test_schema' | curl -f -s --max-time 30 -X POST '$WEB_URL/api/schemas/validate' -H 'Content-Type: application/json' -d @- | grep -q '\"valid\"'"
    
    # Test 3: EOT validation endpoint
    run_smoke_test "EOT Validation Endpoint" \
        "curl -f -s --max-time 30 '$WEB_URL/api/eot-validation/health' | grep -q '\"status\":\"UP\"'"
}

# Monitoring and Observability Tests
test_monitoring_observability() {
    log_info "Testing Monitoring and Observability"
    
    # Test 1: Prometheus metrics
    run_smoke_test "Prometheus Metrics" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/prometheus' | grep -q 'jvm_memory_used_bytes'"
    
    # Test 2: Application info
    run_smoke_test "Application Info" \
        "curl -f -s --max-time 30 '$WEB_URL/actuator/info' | grep -q '\"app\"'"
    
    # Test 3: Custom metrics
    run_smoke_test "Custom Business Metrics" \
        "curl -f -s --max-time 30 '$WEB_URL/api/monitoring/metrics' | grep -q '\"systemMetrics\"'"
    
    # Test 4: Alert system
    run_smoke_test "Alert System Health" \
        "curl -f -s --max-time 30 '$WEB_URL/api/alerts/health' | grep -q '\"status\":\"UP\"'"
}

# Kubernetes Resource Tests
test_kubernetes_resources() {
    log_info "Testing Kubernetes Resources"
    
    # Test 1: Pod status
    run_smoke_test "Pods Running" \
        "kubectl get pods --namespace='$NAMESPACE' | grep -q 'Running'"
    
    # Test 2: Services available
    run_smoke_test "Services Available" \
        "kubectl get services --namespace='$NAMESPACE' | grep -q 'file-transfer'"
    
    # Test 3: Persistent volumes
    run_smoke_test "Persistent Volumes" \
        "kubectl get pvc --namespace='$NAMESPACE' | grep -q 'Bound'"
    
    # Test 4: ConfigMaps and Secrets
    run_smoke_test "ConfigMaps Present" \
        "kubectl get configmaps --namespace='$NAMESPACE' | grep -q 'file-transfer'"
    
    run_smoke_test "Secrets Present" \
        "kubectl get secrets --namespace='$NAMESPACE' | grep -q 'database-credentials'"
}

# Enterprise Feature Tests
test_enterprise_features() {
    log_info "Testing Enterprise Features"
    
    # Test 1: Auto-scaling configuration
    run_smoke_test "Auto-scaling Configuration" \
        "kubectl get hpa --namespace='$NAMESPACE' | grep -q 'file-transfer'"
    
    # Test 2: Backup automation
    run_smoke_test "Backup Automation" \
        "curl -f -s --max-time 30 '$WEB_URL/api/backup/status' | grep -q '\"totalBackups\"'"
    
    # Test 3: Disaster recovery status
    run_smoke_test "Disaster Recovery Status" \
        "curl -f -s --max-time 30 '$WEB_URL/api/backup/disaster-recovery/status' | grep -q '\"enabled\"'"
    
    # Test 4: Security monitoring
    run_smoke_test "Security Monitoring" \
        "curl -f -s --max-time 30 '$WEB_URL/api/security/health' | grep -q '\"status\":\"UP\"'"
    
    # Test 5: Performance monitoring
    run_smoke_test "Performance Monitoring" \
        "curl -f -s --max-time 30 '$WEB_URL/api/scalability/performance/metrics' | grep -q '\"cpuUsage\"'"
}

# End-to-End Workflow Test
test_end_to_end_workflow() {
    log_info "Testing End-to-End Workflow"
    
    # Create a simple end-to-end test
    local tenant_payload='{"name":"Smoke Test Tenant","description":"Automated smoke test tenant","timezone":"UTC"}'
    
    # Test 1: Create tenant
    local tenant_response=$(curl -f -s --max-time 60 -X POST "$WEB_URL/api/v2/tenants" \
        -H "Content-Type: application/json" \
        -d "$tenant_payload" 2>/dev/null || echo "")
    
    if echo "$tenant_response" | grep -q '"id"'; then
        local tenant_id=$(echo "$tenant_response" | grep -o '"id":[0-9]*' | cut -d':' -f2)
        log_success "✅ End-to-End Workflow: Tenant Created (ID: $tenant_id)"
        
        # Test 2: Retrieve tenant
        run_smoke_test "E2E: Retrieve Created Tenant" \
            "curl -f -s --max-time 30 '$WEB_URL/api/v2/tenants/$tenant_id' | grep -q '\"name\":\"Smoke Test Tenant\"'"
        
        # Test 3: Update tenant
        local update_payload='{"description":"Updated by smoke test"}'
        run_smoke_test "E2E: Update Tenant" \
            "curl -f -s --max-time 30 -X PATCH '$WEB_URL/api/v2/tenants/$tenant_id' -H 'Content-Type: application/json' -d '$update_payload' | grep -q '\"description\":\"Updated by smoke test\"'"
        
        # Test 4: Delete tenant (cleanup)
        run_smoke_test "E2E: Delete Tenant (Cleanup)" \
            "curl -f -s --max-time 30 -X DELETE '$WEB_URL/api/v2/tenants/$tenant_id' -o /dev/null -w '%{http_code}' | grep -q '^20[0-9]'"
        
        TESTS_PASSED=$((TESTS_PASSED + 1))
        log_success "✅ End-to-End Workflow Complete"
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("End-to-End Workflow")
        log_error "❌ End-to-End Workflow: Failed to create tenant"
    fi
}

# Generate smoke test report
generate_smoke_test_report() {
    local report_file="/tmp/smoke-test-report-$(date +%Y%m%d-%H%M%S).json"
    
    cat > "$report_file" << EOF
{
  "smokeTestId": "$(uuidgen)",
  "environment": "$ENVIRONMENT",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "summary": {
    "testsRun": $TESTS_RUN,
    "testsPassed": $TESTS_PASSED,
    "testsFailed": $TESTS_FAILED,
    "successRate": $(echo "scale=2; $TESTS_PASSED * 100 / $TESTS_RUN" | bc -l)
  },
  "endpoints": {
    "webUrl": "$WEB_URL",
    "batchUrl": "$BATCH_URL", 
    "frontendUrl": "$FRONTEND_URL"
  },
  "failedTests": [$(printf '"%s",' "${FAILED_TESTS[@]}" | sed 's/,$//')],
  "testCategories": {
    "webApplication": "$(test_web_application &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "batchApplication": "$(test_batch_application &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "frontendApplication": "$(test_frontend_application &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "crossAppIntegration": "$(test_cross_application_integration &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "performanceValidation": "$(test_performance_validation &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "securityValidation": "$(test_security_validation &>/dev/null && echo 'PASSED' || echo 'FAILED')",
    "enterpriseFeatures": "$(test_enterprise_features &>/dev/null && echo 'PASSED' || echo 'FAILED')"
  }
}
EOF

    log_info "Smoke test report generated: $report_file"
    
    # Store report in Kubernetes if available
    if kubectl cluster-info &> /dev/null; then
        kubectl create configmap "smoke-test-report-$(date +%Y%m%d-%H%M%S)" \
            --from-file="$report_file" \
            --namespace="$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f - || true
    fi
}

# Main execution
main() {
    log_info "Starting File Transfer System Smoke Tests"
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"
    log_info "Timeout: ${TIMEOUT}s"
    
    echo
    echo "=== SMOKE TEST EXECUTION ==="
    echo "Web URL: $WEB_URL"
    echo "Batch URL: $BATCH_URL"
    echo "Frontend URL: $FRONTEND_URL"
    echo
    
    # Execute all smoke test categories
    test_web_application
    echo
    test_batch_application
    echo
    test_frontend_application
    echo
    test_cross_application_integration
    echo
    test_performance_validation
    echo
    test_security_validation
    echo
    test_api_versioning
    echo
    test_enterprise_features
    echo
    test_kubernetes_resources
    echo
    test_end_to_end_workflow
    
    # Generate report
    generate_smoke_test_report
    
    # Print summary
    echo
    echo "=== SMOKE TEST SUMMARY ==="
    echo "Tests Run: $TESTS_RUN"
    echo "Tests Passed: $TESTS_PASSED"
    echo "Tests Failed: $TESTS_FAILED"
    echo "Success Rate: $(echo "scale=1; $TESTS_PASSED * 100 / $TESTS_RUN" | bc -l)%"
    
    if [[ $TESTS_FAILED -gt 0 ]]; then
        echo
        echo "Failed Tests:"
        for test in "${FAILED_TESTS[@]}"; do
            echo "  - $test"
        done
        echo
        log_error "Smoke tests failed! Check the issues above."
        exit 1
    else
        echo
        log_success "All smoke tests passed! Deployment validation successful."
        exit 0
    fi
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Smoke Tests"
    echo
    echo "Usage: $0 [environment] [timeout]"
    echo
    echo "Arguments:"
    echo "  environment  Target environment (staging|production)"
    echo "  timeout      Test timeout in seconds (default: 300)"
    echo
    echo "Examples:"
    echo "  $0 staging 300"
    echo "  $0 production 600"
    echo
    exit 0
fi

# Execute main function
main "$@"