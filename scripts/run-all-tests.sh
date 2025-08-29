#!/bin/bash

# File Transfer System - Comprehensive Test Runner
# Runs unit, integration, performance, security, and end-to-end tests across all applications

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Test configuration
TEST_TYPE="${1:-all}"
ENVIRONMENT="${2:-test}"
PARALLEL="${3:-true}"
REPORT_FORMAT="${4:-html}"
COVERAGE_ENABLED="${5:-true}"

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

log_section() {
    echo -e "${PURPLE}[SECTION]${NC} $1"
}

# Test result tracking
declare -A TEST_RESULTS
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Initialize test environment
initialize_test_environment() {
    log_info "Initializing test environment: $ENVIRONMENT"
    
    # Create test reports directory
    mkdir -p "$PROJECT_ROOT/target/test-reports"
    mkdir -p "$PROJECT_ROOT/target/coverage-reports"
    
    # Set up test database if needed
    setup_test_database
    
    # Start required services for integration tests
    start_test_services
    
    log_success "Test environment initialized"
}

# Setup test database
setup_test_database() {
    log_info "Setting up test database"
    
    # Use H2 in-memory database for tests
    export SPRING_PROFILES_ACTIVE="test"
    export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    export SPRING_DATASOURCE_USERNAME="sa"
    export SPRING_DATASOURCE_PASSWORD=""
    export SPRING_JPA_HIBERNATE_DDL_AUTO="create-drop"
    
    log_success "Test database configured"
}

# Start required services for integration tests
start_test_services() {
    log_info "Starting test services"
    
    # Check if Docker is available for integration tests
    if command -v docker &> /dev/null; then
        # Start test containers if needed
        if [[ "$TEST_TYPE" == "all" || "$TEST_TYPE" == "integration" || "$TEST_TYPE" == "e2e" ]]; then
            log_info "Starting test containers..."
            
            # Start minimal test infrastructure
            docker-compose -f "$PROJECT_ROOT/docker/test-stack.yml" up -d --quiet-pull 2>/dev/null || {
                log_warning "Could not start test containers, using embedded services"
            }
        fi
    else
        log_warning "Docker not available, using embedded test services"
    fi
}

# Run unit tests
run_unit_tests() {
    log_section "Running Unit Tests"
    
    local app_results=()
    
    # Web application unit tests
    log_info "Running web application unit tests..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "test" "surefire"; then
        app_results+=("web:PASS")
        log_success "Web application unit tests passed"
    else
        app_results+=("web:FAIL")
        log_error "Web application unit tests failed"
    fi
    
    # Batch application unit tests
    log_info "Running batch application unit tests..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-batch" "test" "surefire"; then
        app_results+=("batch:PASS")
        log_success "Batch application unit tests passed"
    else
        app_results+=("batch:FAIL")
        log_error "Batch application unit tests failed"
    fi
    
    # Frontend unit tests
    log_info "Running frontend unit tests..."
    if run_npm_tests "$PROJECT_ROOT/file-transfer-frontend" "test"; then
        app_results+=("frontend:PASS")
        log_success "Frontend unit tests passed"
    else
        app_results+=("frontend:FAIL")
        log_error "Frontend unit tests failed"
    fi
    
    TEST_RESULTS["unit"]="${app_results[*]}"
}

# Run integration tests
run_integration_tests() {
    log_section "Running Integration Tests"
    
    local app_results=()
    
    # Web application integration tests
    log_info "Running web application integration tests..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "integration-test" "failsafe"; then
        app_results+=("web:PASS")
        log_success "Web application integration tests passed"
    else
        app_results+=("web:FAIL")
        log_error "Web application integration tests failed"
    fi
    
    # Batch application integration tests
    log_info "Running batch application integration tests..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-batch" "integration-test" "failsafe"; then
        app_results+=("batch:PASS")
        log_success "Batch application integration tests passed"
    else
        app_results+=("batch:FAIL")
        log_error "Batch application integration tests failed"
    fi
    
    # Frontend integration tests
    log_info "Running frontend integration tests..."
    if run_npm_tests "$PROJECT_ROOT/file-transfer-frontend" "test:integration"; then
        app_results+=("frontend:PASS")
        log_success "Frontend integration tests passed"
    else
        app_results+=("frontend:FAIL")
        log_error "Frontend integration tests failed"
    fi
    
    TEST_RESULTS["integration"]="${app_results[*]}"
}

# Run end-to-end tests
run_e2e_tests() {
    log_section "Running End-to-End Tests"
    
    log_info "Starting all applications for E2E tests..."
    
    # Start applications in background
    start_applications_for_e2e
    
    # Wait for applications to be ready
    wait_for_applications
    
    # Run E2E test suite
    log_info "Executing end-to-end test suite..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "verify" "failsafe" "EndToEndIntegrationTest"; then
        TEST_RESULTS["e2e"]="PASS"
        log_success "End-to-end tests passed"
    else
        TEST_RESULTS["e2e"]="FAIL"
        log_error "End-to-end tests failed"
    fi
    
    # Stop applications
    stop_applications_after_e2e
}

# Run performance tests
run_performance_tests() {
    log_section "Running Performance Tests"
    
    log_info "Executing performance test suite..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "verify" "failsafe" "LoadTest,PerformanceTest"; then
        TEST_RESULTS["performance"]="PASS"
        log_success "Performance tests passed"
    else
        TEST_RESULTS["performance"]="FAIL"
        log_error "Performance tests failed"
    fi
}

# Run security tests
run_security_tests() {
    log_section "Running Security Tests"
    
    log_info "Executing security test suite..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "verify" "failsafe" "SecurityIntegrationTest,SecurityTest"; then
        TEST_RESULTS["security"]="PASS"
        log_success "Security tests passed"
    else
        TEST_RESULTS["security"]="FAIL"
        log_error "Security tests failed"
    fi
}

# Run cross-application tests
run_cross_app_tests() {
    log_section "Running Cross-Application Tests"
    
    log_info "Starting all applications for cross-app tests..."
    start_all_applications
    
    log_info "Executing cross-application test suite..."
    if run_maven_tests "$PROJECT_ROOT/file-transfer-web" "verify" "failsafe" "CrossApp"; then
        TEST_RESULTS["cross-app"]="PASS"
        log_success "Cross-application tests passed"
    else
        TEST_RESULTS["cross-app"]="FAIL"
        log_error "Cross-application tests failed"
    fi
    
    stop_all_applications
}

# Helper function to run Maven tests
run_maven_tests() {
    local project_dir="$1"
    local phase="$2"
    local plugin="$3"
    local test_filter="${4:-}"
    
    cd "$project_dir"
    
    local maven_cmd="mvn clean $phase"
    
    if [[ -n "$test_filter" ]]; then
        maven_cmd="$maven_cmd -Dtest=$test_filter"
    fi
    
    if [[ "$COVERAGE_ENABLED" == "true" ]]; then
        maven_cmd="$maven_cmd jacoco:report"
    fi
    
    if [[ "$PARALLEL" == "true" ]]; then
        maven_cmd="$maven_cmd -T 1C"
    fi
    
    maven_cmd="$maven_cmd -Dspring.profiles.active=$ENVIRONMENT"
    
    log_info "Executing: $maven_cmd"
    
    if $maven_cmd > "/tmp/maven-test-$phase.log" 2>&1; then
        return 0
    else
        log_error "Maven test failed. Log: /tmp/maven-test-$phase.log"
        return 1
    fi
}

# Helper function to run NPM tests
run_npm_tests() {
    local project_dir="$1"
    local test_script="$2"
    
    cd "$project_dir"
    
    log_info "Executing: npm run $test_script"
    
    if npm run "$test_script" > "/tmp/npm-test-$test_script.log" 2>&1; then
        return 0
    else
        log_error "NPM test failed. Log: /tmp/npm-test-$test_script.log"
        return 1
    fi
}

# Start applications for E2E tests
start_applications_for_e2e() {
    log_info "Starting applications for E2E tests..."
    
    # Start web application
    cd "$PROJECT_ROOT/file-transfer-web"
    nohup mvn spring-boot:run -Dspring.profiles.active=test -Dserver.port=8080 > /tmp/web-app.log 2>&1 &
    echo $! > /tmp/web-app.pid
    
    # Start batch application
    cd "$PROJECT_ROOT/file-transfer-batch"
    nohup mvn spring-boot:run -Dspring.profiles.active=test -Dserver.port=8082 > /tmp/batch-app.log 2>&1 &
    echo $! > /tmp/batch-app.pid
    
    # Start frontend application
    cd "$PROJECT_ROOT/file-transfer-frontend"
    nohup npm start > /tmp/frontend-app.log 2>&1 &
    echo $! > /tmp/frontend-app.pid
}

# Wait for applications to be ready
wait_for_applications() {
    log_info "Waiting for applications to be ready..."
    
    local max_wait=120 # 2 minutes
    local wait_time=0
    
    while [[ $wait_time -lt $max_wait ]]; do
        if curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1 && \
           curl -f -s http://localhost:8082/actuator/health >/dev/null 2>&1 && \
           curl -f -s http://localhost:3000 >/dev/null 2>&1; then
            log_success "All applications are ready"
            return 0
        fi
        
        sleep 5
        wait_time=$((wait_time + 5))
        log_info "Waiting for applications... ($wait_time/$max_wait seconds)"
    done
    
    log_error "Applications did not start within $max_wait seconds"
    return 1
}

# Stop applications after E2E tests
stop_applications_after_e2e() {
    log_info "Stopping applications..."
    
    # Stop applications using PID files
    for app in web-app batch-app frontend-app; do
        if [[ -f "/tmp/$app.pid" ]]; then
            local pid=$(cat "/tmp/$app.pid")
            if kill "$pid" 2>/dev/null; then
                log_info "Stopped $app (PID: $pid)"
            fi
            rm -f "/tmp/$app.pid"
        fi
    done
}

# Generate test reports
generate_test_reports() {
    log_section "Generating Test Reports"
    
    local report_dir="$PROJECT_ROOT/target/test-reports"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    # Create comprehensive test report
    cat > "$report_dir/test-summary-$timestamp.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>File Transfer System - Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f5f5f5; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .pass { color: green; font-weight: bold; }
        .fail { color: red; font-weight: bold; }
        .skip { color: orange; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>File Transfer System - Comprehensive Test Report</h1>
        <p><strong>Generated:</strong> $(date)</p>
        <p><strong>Environment:</strong> $ENVIRONMENT</p>
        <p><strong>Test Type:</strong> $TEST_TYPE</p>
    </div>
    
    <div class="section">
        <h2>Test Summary</h2>
        <table>
            <tr><th>Test Suite</th><th>Status</th><th>Details</th></tr>
EOF

    # Add test results to report
    for test_suite in "${!TEST_RESULTS[@]}"; do
        local result="${TEST_RESULTS[$test_suite]}"
        local status_class="pass"
        
        if [[ "$result" == *"FAIL"* ]]; then
            status_class="fail"
        elif [[ "$result" == *"SKIP"* ]]; then
            status_class="skip"
        fi
        
        cat >> "$report_dir/test-summary-$timestamp.html" << EOF
            <tr>
                <td>$(echo "$test_suite" | tr '[:lower:]' '[:upper:]')</td>
                <td class="$status_class">$result</td>
                <td>Details in individual test reports</td>
            </tr>
EOF
    done
    
    cat >> "$report_dir/test-summary-$timestamp.html" << EOF
        </table>
    </div>
    
    <div class="section">
        <h2>Application Coverage</h2>
        <ul>
            <li><strong>Web Application:</strong> API endpoints, services, controllers</li>
            <li><strong>Batch Application:</strong> Job processing, monitoring, scalability</li>
            <li><strong>Frontend Application:</strong> UI components, API integration, user workflows</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>Test Types Executed</h2>
        <ul>
            <li><strong>Unit Tests:</strong> Individual component testing</li>
            <li><strong>Integration Tests:</strong> Cross-component and API testing</li>
            <li><strong>End-to-End Tests:</strong> Complete user workflow testing</li>
            <li><strong>Performance Tests:</strong> Load and stress testing</li>
            <li><strong>Security Tests:</strong> Vulnerability and security control testing</li>
            <li><strong>Cross-Application Tests:</strong> Multi-service integration testing</li>
        </ul>
    </div>
</body>
</html>
EOF

    log_success "Test report generated: $report_dir/test-summary-$timestamp.html"
}

# Print test summary
print_test_summary() {
    log_section "Test Execution Summary"
    
    echo
    echo "=== FILE TRANSFER SYSTEM TEST RESULTS ==="
    echo "Environment: $ENVIRONMENT"
    echo "Test Type: $TEST_TYPE"
    echo "Timestamp: $(date)"
    echo
    
    local overall_status="PASS"
    
    for test_suite in "${!TEST_RESULTS[@]}"; do
        local result="${TEST_RESULTS[$test_suite]}"
        echo "$(printf '%-20s' "$(echo "$test_suite" | tr '[:lower:]' '[:upper:]'):")" "$result"
        
        if [[ "$result" == *"FAIL"* ]]; then
            overall_status="FAIL"
        fi
    done
    
    echo
    echo "=== OVERALL STATUS: $overall_status ==="
    echo
    
    if [[ "$overall_status" == "PASS" ]]; then
        log_success "All tests passed successfully!"
    else
        log_error "Some tests failed. Check individual test reports for details."
    fi
}

# Main execution function
main() {
    log_info "Starting File Transfer System Test Suite"
    log_info "Test Type: $TEST_TYPE"
    log_info "Environment: $ENVIRONMENT"
    log_info "Parallel Execution: $PARALLEL"
    log_info "Coverage Enabled: $COVERAGE_ENABLED"
    
    # Initialize test environment
    initialize_test_environment
    
    # Run tests based on type
    case $TEST_TYPE in
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "e2e")
            run_e2e_tests
            ;;
        "performance")
            run_performance_tests
            ;;
        "security")
            run_security_tests
            ;;
        "cross-app")
            run_cross_app_tests
            ;;
        "all")
            run_unit_tests
            run_integration_tests
            run_e2e_tests
            run_performance_tests
            run_security_tests
            run_cross_app_tests
            ;;
        *)
            log_error "Unknown test type: $TEST_TYPE"
            log_error "Valid types: unit, integration, e2e, performance, security, cross-app, all"
            exit 1
            ;;
    esac
    
    # Generate reports
    generate_test_reports
    
    # Print summary
    print_test_summary
    
    # Cleanup
    cleanup_test_environment
    
    # Exit with appropriate code
    local exit_code=0
    for result in "${TEST_RESULTS[@]}"; do
        if [[ "$result" == *"FAIL"* ]]; then
            exit_code=1
            break
        fi
    done
    
    exit $exit_code
}

# Cleanup test environment
cleanup_test_environment() {
    log_info "Cleaning up test environment"
    
    # Stop test containers
    if command -v docker &> /dev/null; then
        docker-compose -f "$PROJECT_ROOT/docker/test-stack.yml" down --quiet 2>/dev/null || true
    fi
    
    # Clean up temporary files
    rm -f /tmp/*-app.pid /tmp/*-app.log /tmp/*-test-*.log
    
    log_success "Test environment cleaned up"
}

# Script help
if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
    echo "File Transfer System - Comprehensive Test Runner"
    echo
    echo "Usage: $0 [test_type] [environment] [parallel] [report_format] [coverage]"
    echo
    echo "Arguments:"
    echo "  test_type      Type of tests to run (unit|integration|e2e|performance|security|cross-app|all)"
    echo "  environment    Test environment (test|development|staging)"
    echo "  parallel       Run tests in parallel (true|false)"
    echo "  report_format  Report format (html|json|xml|all)"
    echo "  coverage       Enable coverage reporting (true|false)"
    echo
    echo "Examples:"
    echo "  $0 all test true html true"
    echo "  $0 unit test false json false"
    echo "  $0 integration test true html true"
    echo "  $0 e2e test false html false"
    echo "  $0 performance test false html false"
    echo
    exit 0
fi

# Execute main function
main "$@"