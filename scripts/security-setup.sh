#!/bin/bash

# Security Setup Script for File Transfer System
# This script sets up security configurations, generates keys, and validates security settings

set -e

echo "🔒 File Transfer System Security Setup"
echo "====================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=${1:-development}
GENERATE_KEYS=${2:-true}
SETUP_REDIS=${3:-false}

echo -e "${BLUE}Environment: ${ENVIRONMENT}${NC}"
echo -e "${BLUE}Generate Keys: ${GENERATE_KEYS}${NC}"
echo -e "${BLUE}Setup Redis: ${SETUP_REDIS}${NC}"
echo ""

# Create security directory
echo -e "${GREEN}📁 Creating security directories...${NC}"
mkdir -p config/security
mkdir -p config/ssl
mkdir -p logs/security
chmod 700 config/security

# Generate encryption master key
if [[ "$GENERATE_KEYS" == "true" ]]; then
    echo -e "${GREEN}🔑 Generating encryption keys...${NC}"
    
    # Generate master encryption key (256-bit)
    MASTER_KEY=$(openssl rand -base64 32)
    echo "ENCRYPTION_MASTER_KEY=${MASTER_KEY}" > config/security/encryption.env
    echo "Generated master encryption key"
    
    # Generate JWT secret
    JWT_SECRET=$(openssl rand -base64 64)
    echo "JWT_SECRET=${JWT_SECRET}" >> config/security/encryption.env
    echo "Generated JWT secret"
    
    # Generate API keys for different services
    ADMIN_API_KEY=$(openssl rand -base64 32)
    MONITOR_API_KEY=$(openssl rand -base64 32)
    echo "ADMIN_API_KEY=${ADMIN_API_KEY}" >> config/security/encryption.env
    echo "MONITOR_API_KEY=${MONITOR_API_KEY}" >> config/security/encryption.env
    echo "Generated service API keys"
    
    # Set proper permissions
    chmod 600 config/security/encryption.env
    echo -e "${YELLOW}⚠️  Security keys saved to config/security/encryption.env${NC}"
    echo -e "${YELLOW}⚠️  Keep this file secure and do not commit to version control!${NC}"
fi

# Generate SSL certificates for development
if [[ "$ENVIRONMENT" == "development" ]]; then
    echo -e "${GREEN}🛡️  Generating self-signed SSL certificates for development...${NC}"
    
    openssl req -x509 -newkey rsa:4096 -keyout config/ssl/private.key -out config/ssl/certificate.crt \
        -days 365 -nodes -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost" 2>/dev/null
    
    chmod 600 config/ssl/private.key
    chmod 644 config/ssl/certificate.crt
    echo "Generated SSL certificates for development"
fi

# Create security configuration files
echo -e "${GREEN}⚙️  Creating security configuration files...${NC}"

# Rate limiting configuration
cat > config/security/rate-limits.yml << EOF
# Rate Limiting Configuration
rate-limiting:
  profiles:
    development:
      api-requests-per-minute: 120
      login-attempts-per-hour: 20
      file-upload-per-minute: 20
    
    production:
      api-requests-per-minute: 60
      login-attempts-per-hour: 10
      file-upload-per-minute: 10
    
    high-security:
      api-requests-per-minute: 30
      login-attempts-per-hour: 5
      file-upload-per-minute: 5

  # IP whitelist for admin operations
  admin-whitelist:
    - 127.0.0.1
    - 10.0.0.0/8
    - 192.168.0.0/16

  # Rate limit bypass for internal services
  bypass-patterns:
    - /actuator/health
    - /actuator/info
EOF

# Security headers configuration
cat > config/security/headers.yml << EOF
# Security Headers Configuration
security-headers:
  content-security-policy:
    development: "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'"
    production: "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:"
  
  strict-transport-security:
    max-age: 31536000
    include-subdomains: true
    preload: true
  
  frame-options: DENY
  content-type-options: nosniff
  xss-protection: "1; mode=block"
  referrer-policy: "strict-origin-when-cross-origin"
EOF

# Input validation rules
cat > config/security/validation-rules.yml << EOF
# Input Validation Rules
validation:
  patterns:
    tenant-id: "^[a-zA-Z0-9_-]{3,50}$"
    service-name: "^[a-zA-Z0-9_-]{2,100}$"
    file-name: "^[a-zA-Z0-9._-]{1,255}$"
    api-key: "^[A-Za-z0-9+/]{40,}={0,2}$"
  
  max-lengths:
    general-text: 1000
    description: 2000
    file-path: 500
    url: 2000
  
  blocked-patterns:
    - "<script"
    - "javascript:"
    - "vbscript:"
    - "../"
    - "union select"
    - "drop table"
    - "exec("
    - "system("
EOF

# Setup Redis for distributed rate limiting (if requested)
if [[ "$SETUP_REDIS" == "true" ]]; then
    echo -e "${GREEN}⚡ Setting up Redis for distributed rate limiting...${NC}"
    
    # Create Redis configuration
    cat > config/security/redis-security.conf << EOF
# Redis Security Configuration
bind 127.0.0.1
protected-mode yes
port 6379
requirepass $(openssl rand -base64 32)
maxclients 1000
timeout 300

# Disable dangerous commands
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG "CONFIG_$(openssl rand -hex 8)"
rename-command SHUTDOWN "SHUTDOWN_$(openssl rand -hex 8)"
rename-command DEBUG ""
rename-command EVAL ""
EOF
    
    echo "Redis security configuration created"
fi

# Create environment-specific security configurations
echo -e "${GREEN}🌍 Creating environment-specific configurations...${NC}"

# Development environment
cat > config/security/development.env << EOF
# Development Security Configuration
SECURITY_PROFILE=development
RATE_LIMITING_ENABLED=true
RATE_LIMITING_DISTRIBUTED=false
INPUT_VALIDATION_ENABLED=true
ENCRYPTION_ENABLED=true
AUDIT_LOGGING_ENABLED=true
SSL_ENABLED=false
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
CSP_POLICY=relaxed
EOF

# Production environment
cat > config/security/production.env << EOF
# Production Security Configuration
SECURITY_PROFILE=production
RATE_LIMITING_ENABLED=true
RATE_LIMITING_DISTRIBUTED=true
INPUT_VALIDATION_ENABLED=true
ENCRYPTION_ENABLED=true
AUDIT_LOGGING_ENABLED=true
SSL_ENABLED=true
CORS_ALLOWED_ORIGINS=https://yourdomain.com
CSP_POLICY=strict
MALWARE_SCANNING_ENABLED=true
EOF

# Set proper permissions
chmod 600 config/security/*.env
chmod 644 config/security/*.yml
chmod 644 config/security/*.conf

# Create security validation script
echo -e "${GREEN}🔍 Creating security validation script...${NC}"

cat > scripts/validate-security.sh << 'EOF'
#!/bin/bash

# Security Validation Script
echo "🔍 Validating security configuration..."

# Check for required environment variables
required_vars=(
    "ENCRYPTION_MASTER_KEY"
    "JWT_SECRET"
)

missing_vars=()
for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        missing_vars+=("$var")
    fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
    echo "❌ Missing required environment variables:"
    printf ' - %s\n' "${missing_vars[@]}"
    exit 1
fi

# Validate key strength
if [[ ${#ENCRYPTION_MASTER_KEY} -lt 40 ]]; then
    echo "❌ Encryption master key is too short (minimum 40 characters)"
    exit 1
fi

if [[ ${#JWT_SECRET} -lt 60 ]]; then
    echo "❌ JWT secret is too short (minimum 60 characters)"
    exit 1
fi

# Check file permissions
if [[ -f "config/security/encryption.env" ]]; then
    perms=$(stat -c "%a" config/security/encryption.env)
    if [[ "$perms" != "600" ]]; then
        echo "❌ Encryption file has incorrect permissions: $perms (should be 600)"
        exit 1
    fi
fi

# Test encryption service
echo "🧪 Testing encryption service..."
curl -s -f http://localhost:8080/api/security/health > /dev/null
if [[ $? -eq 0 ]]; then
    echo "✅ Security service is responding"
else
    echo "⚠️  Security service is not responding (application may not be running)"
fi

echo "✅ Security validation completed successfully"
EOF

chmod +x scripts/validate-security.sh

# Create security monitoring script
echo -e "${GREEN}📊 Creating security monitoring script...${NC}"

cat > scripts/security-monitor.sh << 'EOF'
#!/bin/bash

# Security Monitoring Script
echo "📊 Security monitoring dashboard"
echo "================================"

# Check rate limiting status
echo "🚦 Rate Limiting Status:"
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
     "http://localhost:8080/api/security/rate-limit/statistics" | \
     jq -r '.[] | "\(.name): \(.status // "active")"' 2>/dev/null || echo "  API not accessible"

echo ""

# Check recent security events
echo "🔒 Recent Security Events:"
tail -n 10 logs/security/security-events.log 2>/dev/null || echo "  No security log found"

echo ""

# Check failed authentication attempts
echo "🚫 Failed Authentication Attempts (last hour):"
grep "authentication failed" logs/file-transfer-web.log 2>/dev/null | \
    tail -n 5 | \
    awk '{print "  " $1 " " $2 " - " $NF}' || echo "  No failed attempts found"

echo ""

# Check system resources
echo "💻 System Resources:"
echo "  Memory: $(free -h | awk 'NR==2{printf "%.1f%%", $3*100/$2 }')"
echo "  Disk: $(df -h / | awk 'NR==2{print $5}')"
echo "  CPU: $(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1"%"}')"
EOF

chmod +x scripts/security-monitor.sh

# Create example environment loading script
echo -e "${GREEN}🔧 Creating environment loading script...${NC}"

cat > scripts/load-security-env.sh << EOF
#!/bin/bash

# Load Security Environment Variables
ENVIRONMENT=\${1:-development}

echo "Loading security environment for: \$ENVIRONMENT"

# Load base security configuration
if [[ -f "config/security/encryption.env" ]]; then
    source config/security/encryption.env
    echo "✅ Loaded encryption configuration"
else
    echo "❌ Encryption configuration not found"
    exit 1
fi

# Load environment-specific configuration
if [[ -f "config/security/\$ENVIRONMENT.env" ]]; then
    source "config/security/\$ENVIRONMENT.env"
    echo "✅ Loaded \$ENVIRONMENT configuration"
else
    echo "❌ Environment configuration not found for \$ENVIRONMENT"
    exit 1
fi

# Export variables for application
export ENCRYPTION_MASTER_KEY
export JWT_SECRET
export ADMIN_API_KEY
export MONITOR_API_KEY
export SECURITY_PROFILE
export RATE_LIMITING_ENABLED
export INPUT_VALIDATION_ENABLED
export ENCRYPTION_ENABLED

echo "✅ Security environment loaded successfully"
echo "🚀 Ready to start application with security configuration"
EOF

chmod +x scripts/load-security-env.sh

# Create security checklist
echo -e "${GREEN}📋 Creating security checklist...${NC}"

cat > docs/Security_Checklist.md << 'EOF'
# Security Implementation Checklist

## ✅ Completed Security Features

### 🔒 Authentication & Authorization
- [x] JWT token-based authentication
- [x] Role-based access control (RBAC)
- [x] OAuth2 integration support
- [x] Password policy enforcement
- [x] Session management

### 🛡️ Rate Limiting
- [x] API endpoint rate limiting
- [x] Login attempt limiting
- [x] File upload rate limiting
- [x] Tenant-specific rate limiting
- [x] Admin operation rate limiting
- [x] Bulk operation rate limiting

### 🔐 Encryption & Hashing
- [x] AES-GCM encryption for sensitive data
- [x] BCrypt password hashing
- [x] Secure token generation
- [x] Data integrity verification
- [x] File content encryption

### 🚫 Input Validation
- [x] SQL injection prevention
- [x] XSS attack prevention
- [x] Path traversal prevention
- [x] HTML sanitization
- [x] Parameter validation
- [x] File name validation

### 🛡️ Security Headers
- [x] Content Security Policy (CSP)
- [x] HTTP Strict Transport Security (HSTS)
- [x] X-Frame-Options
- [x] X-Content-Type-Options
- [x] X-XSS-Protection
- [x] Referrer Policy
- [x] Permissions Policy

### 📊 Security Monitoring
- [x] Security event logging
- [x] Failed authentication tracking
- [x] Rate limit violation logging
- [x] Security health checks
- [x] Audit trail

## 🔧 Configuration Tasks

### Environment Setup
- [ ] Generate production encryption keys
- [ ] Configure SSL/TLS certificates
- [ ] Set up Redis for distributed rate limiting
- [ ] Configure OAuth2 providers
- [ ] Set up monitoring alerts

### Production Hardening
- [ ] Review and tighten CSP policies
- [ ] Enable malware scanning
- [ ] Configure IP whitelisting
- [ ] Set up intrusion detection
- [ ] Implement log aggregation

### Regular Maintenance
- [ ] Rotate encryption keys
- [ ] Review security logs
- [ ] Update security dependencies
- [ ] Conduct security audits
- [ ] Test disaster recovery

## 🚨 Security Recommendations

1. **Key Management**: Use a dedicated key management service in production
2. **Rate Limiting**: Use Redis for distributed rate limiting in multi-instance deployments
3. **SSL/TLS**: Always use HTTPS in production with valid certificates
4. **Monitoring**: Set up real-time security event monitoring
5. **Backup**: Regularly backup security configurations and encryption keys
EOF

# Final security summary
echo ""
echo -e "${GREEN}🎉 Security setup completed successfully!${NC}"
echo ""
echo -e "${BLUE}📁 Created files:${NC}"
echo "  • config/security/encryption.env (🔒 KEEP SECURE)"
echo "  • config/security/rate-limits.yml"
echo "  • config/security/headers.yml"
echo "  • config/security/validation-rules.yml"
echo "  • config/security/development.env"
echo "  • config/security/production.env"
echo "  • scripts/validate-security.sh"
echo "  • scripts/security-monitor.sh"
echo "  • scripts/load-security-env.sh"
echo "  • docs/Security_Checklist.md"

if [[ "$ENVIRONMENT" == "development" ]]; then
    echo "  • config/ssl/certificate.crt"
    echo "  • config/ssl/private.key"
fi

echo ""
echo -e "${YELLOW}⚠️  IMPORTANT SECURITY NOTES:${NC}"
echo "1. The encryption.env file contains sensitive keys - never commit to version control"
echo "2. In production, use proper certificate authority signed SSL certificates"
echo "3. Set up Redis with authentication for distributed rate limiting"
echo "4. Review and customize rate limits based on your requirements"
echo "5. Monitor security logs regularly for suspicious activity"

echo ""
echo -e "${GREEN}🚀 Next steps:${NC}"
echo "1. Source the environment variables: source scripts/load-security-env.sh"
echo "2. Validate security setup: ./scripts/validate-security.sh"
echo "3. Start the application with security enabled"
echo "4. Monitor security: ./scripts/security-monitor.sh"

echo ""
echo -e "${BLUE}Security setup completed for environment: ${ENVIRONMENT}${NC}"
EOF

<function_calls>
<invoke name="run_terminal_cmd">
<parameter name="command">chmod +x /workspace/scripts/security-setup.sh