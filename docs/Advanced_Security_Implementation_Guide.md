# Advanced Security Implementation Guide

## 🔒 Overview

This guide covers the comprehensive advanced security implementation for the File Transfer Management System. The security framework provides enterprise-grade protection against common vulnerabilities and implements defense-in-depth security principles.

## 🏗️ Security Architecture

### Security Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                      │
├─────────────────────────────────────────────────────────────┤
│ Security Headers Filter (CSP, HSTS, X-Frame-Options, etc.) │
├─────────────────────────────────────────────────────────────┤
│          Rate Limiting Filter (Bucket4j + Redis)           │
├─────────────────────────────────────────────────────────────┤
│      Input Validation Filter (XSS, SQLi, Path Traversal)   │
├─────────────────────────────────────────────────────────────┤
│        Spring Security (Authentication & Authorization)     │
├─────────────────────────────────────────────────────────────┤
│                Application Layer Services                   │
├─────────────────────────────────────────────────────────────┤
│            Encryption Service (AES-GCM, BCrypt)            │
├─────────────────────────────────────────────────────────────┤
│                   Database Layer                           │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### 1. Run Security Setup

```bash
# Basic setup for development
./scripts/security-setup.sh development true false

# Production setup with Redis
./scripts/security-setup.sh production true true
```

### 2. Load Security Environment

```bash
# Load security configuration
source scripts/load-security-env.sh development

# Validate security setup
./scripts/validate-security.sh
```

### 3. Start Application with Security

```bash
# Start with security profile
java -jar file-transfer-web.jar --spring.profiles.active=development,security
```

## 🛡️ Security Features

### 1. Rate Limiting

#### Implementation
- **Token Bucket Algorithm**: Implemented using Bucket4j
- **Distributed Support**: Redis backend for multi-instance deployments
- **Granular Controls**: Different limits for different endpoint types
- **Graceful Degradation**: Fails open if rate limiting service is unavailable

#### Rate Limit Types

| Type | Default Limit | Scope | Use Case |
|------|---------------|-------|----------|
| **API_GENERAL** | 60/minute | Per user/IP | General API access |
| **LOGIN_ATTEMPTS** | 10/hour | Per IP | Brute force protection |
| **FILE_UPLOAD** | 10/minute | Per user | Resource-intensive operations |
| **TENANT_SPECIFIC** | 1000/minute | Per tenant | Multi-tenant isolation |
| **ADMIN_OPERATIONS** | 20/minute | Per admin user | Administrative functions |
| **BULK_OPERATIONS** | 5/5minutes | Per user | Large-scale operations |

#### Configuration

```yaml
security:
  rate-limiting:
    enabled: true
    distributed: true  # Use Redis for distribution
    api:
      requests-per-minute: 60
    login:
      attempts-per-hour: 10
```

#### Usage

```java
// Check rate limit programmatically
boolean allowed = rateLimitingService.isAllowed(
    "user123", 
    RateLimitType.API_GENERAL
);

// Get rate limit information
RateLimitInfo info = rateLimitingService.getRateLimitInfo(
    "user123", 
    RateLimitType.API_GENERAL
);
```

### 2. Input Validation & Sanitization

#### Multi-Layer Validation
1. **Request Filter**: Validates parameters, headers, and path segments
2. **Service Layer**: Business logic validation using `InputValidationService`
3. **Controller Layer**: `@Valid` annotations for DTO validation

#### Protection Against
- **SQL Injection**: Pattern detection and parameterized queries
- **XSS Attacks**: HTML sanitization using OWASP Java HTML Sanitizer
- **Path Traversal**: Directory traversal prevention
- **Command Injection**: System command pattern detection
- **LDAP Injection**: LDAP query sanitization

#### Validation Types

```java
// Tenant ID validation
ValidationResult result = inputValidationService.validateTenantId("tenant-123");

// File name validation (prevents path traversal)
ValidationResult result = inputValidationService.validateFileName("data.csv");

// Email validation
ValidationResult result = inputValidationService.validateEmail("user@example.com");

// Password policy validation
ValidationResult result = inputValidationService.validatePassword("SecurePass123!");

// General text validation with length limits
ValidationResult result = inputValidationService.validateText(input, 1000, true);
```

#### Custom Validation Rules

```yaml
validation:
  patterns:
    tenant-id: "^[a-zA-Z0-9_-]{3,50}$"
    service-name: "^[a-zA-Z0-9_-]{2,100}$"
    file-name: "^[a-zA-Z0-9._-]{1,255}$"
  max-lengths:
    general-text: 1000
    description: 2000
```

### 3. Encryption & Cryptography

#### Encryption Standards
- **Algorithm**: AES-256-GCM (Authenticated Encryption)
- **Key Management**: Configurable master key with secure defaults
- **Password Hashing**: BCrypt with strength 12
- **Token Generation**: Cryptographically secure random tokens

#### Encryption Service

```java
// Encrypt sensitive data
EncryptionResult result = encryptionService.encryptData("sensitive information");

// Decrypt data
EncryptionResult result = encryptionService.decryptData(encryptedData);

// Hash passwords
String hashedPassword = encryptionService.hashPassword("userPassword123");

// Verify passwords
boolean isValid = encryptionService.verifyPassword("userPassword123", hashedPassword);

// Generate secure tokens
String apiKey = encryptionService.generateApiKey();
String sessionToken = encryptionService.generateSessionToken();

// Data integrity verification
boolean isValid = encryptionService.verifyDataIntegrity(originalData, expectedHash);
```

#### Key Management

```bash
# Generate new master key
openssl rand -base64 32

# Set environment variable
export ENCRYPTION_MASTER_KEY="your-generated-key-here"
```

### 4. Security Headers

#### Comprehensive Header Protection

```http
# Prevent content type sniffing
X-Content-Type-Options: nosniff

# XSS Protection
X-XSS-Protection: 1; mode=block

# Prevent clickjacking
X-Frame-Options: DENY

# Enforce HTTPS
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload

# Content Security Policy
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'

# Referrer Policy
Referrer-Policy: strict-origin-when-cross-origin

# Permissions Policy
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

#### Environment-Specific CSP

```java
// Development (more permissive)
"default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'"

// Production (strict)
"default-src 'self'; script-src 'self'; object-src 'none'"

// API endpoints (very strict)
"default-src 'none'; connect-src 'self'"
```

### 5. CORS Configuration

#### Secure CORS Setup

```yaml
security:
  cors:
    allowed-origins:
      - https://yourdomain.com
      - https://app.yourdomain.com
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowed-headers:
      - Authorization
      - Content-Type
      - X-Tenant-ID
      - X-Correlation-ID
    exposed-headers:
      - X-RateLimit-Limit
      - X-RateLimit-Remaining
    allow-credentials: true
    max-age: 3600
```

### 6. Authentication & Authorization

#### Multi-Factor Authentication Support

```yaml
security:
  authentication:
    password-policy:
      min-length: 8
      require-uppercase: true
      require-lowercase: true
      require-digits: true
      require-special-chars: true
    
    jwt:
      secret: ${JWT_SECRET}
      expiration-hours: 24
      refresh-expiration-days: 7
    
    oauth2:
      enabled: true
      providers:
        google:
          client-id: ${GOOGLE_CLIENT_ID}
        azure:
          tenant-id: ${AZURE_TENANT_ID}
```

#### Role-Based Access Control

```java
// Method-level security
@PreAuthorize("hasRole('ADMIN')")
public void adminOperation() { }

@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public void managementOperation() { }

// Check permissions programmatically
@PreAuthorize("@securityService.canAccessTenant(#tenantId)")
public void tenantOperation(String tenantId) { }
```

## 🔧 Configuration

### Environment Variables

```bash
# Encryption
ENCRYPTION_MASTER_KEY=base64-encoded-key
JWT_SECRET=long-random-secret

# Rate Limiting
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=secure-password

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
AZURE_TENANT_ID=your-azure-tenant-id
```

### Security Profiles

#### Development Profile

```yaml
spring:
  profiles: development

security:
  rate-limiting:
    api:
      requests-per-minute: 120  # More lenient
  headers:
    csp:
      script-src: "'self' 'unsafe-inline' 'unsafe-eval'"
  encryption:
    encrypt-file-metadata: false
```

#### Production Profile

```yaml
spring:
  profiles: production

security:
  rate-limiting:
    distributed: true
    api:
      requests-per-minute: 30  # More restrictive
  headers:
    csp:
      script-src: "'self'"  # Strict CSP
  encryption:
    encrypt-file-metadata: true
  file-upload:
    scan-for-malware: true
```

## 📊 Security Monitoring

### Security Events Logging

```java
// Automatic security event logging
logger.warn("Rate limit exceeded for identifier: {} type: {}", identifier, type);
logger.warn("Invalid parameter detected: {} = {}", paramName, paramValue);
logger.error("Encryption failed for data type: {}", dataType);
```

### Security Metrics

```java
// Custom security metrics
@Autowired
private MetricsService metricsService;

// Record security events
metricsService.recordSecurityEvent("rate_limit_exceeded", tenantId);
metricsService.recordSecurityEvent("invalid_input_detected", tenantId);
metricsService.recordSecurityEvent("encryption_failure", tenantId);
```

### Security Dashboard

```bash
# Monitor security status
./scripts/security-monitor.sh

# Output:
📊 Security monitoring dashboard
================================
🚦 Rate Limiting Status:
  API_GENERAL: active
  LOGIN_ATTEMPTS: active
  FILE_UPLOAD: active

🔒 Recent Security Events:
  2024-01-20 10:30:15 - Rate limit exceeded: user123
  2024-01-20 10:25:33 - Invalid input detected: tenant-name

🚫 Failed Authentication Attempts (last hour):
  10:15:22 - authentication failed for user: admin@test.com
```

## 🔍 Security Testing

### Automated Security Tests

```java
@Test
public void testRateLimiting() {
    // Test rate limiting enforcement
    for (int i = 0; i < 65; i++) {
        boolean allowed = rateLimitingService.isAllowed("test-user", RateLimitType.API_GENERAL);
        if (i < 60) {
            assertTrue(allowed);
        } else {
            assertFalse(allowed); // Should be rate limited
        }
    }
}

@Test
public void testInputValidation() {
    // Test XSS prevention
    ValidationResult result = inputValidationService.validateText("<script>alert('xss')</script>", 100, true);
    assertFalse(result.isValid());
    
    // Test SQL injection prevention
    result = inputValidationService.validateText("'; DROP TABLE users; --", 100, true);
    assertFalse(result.isValid());
}

@Test
public void testEncryption() {
    String originalData = "sensitive information";
    EncryptionResult encrypted = encryptionService.encryptData(originalData);
    assertTrue(encrypted.isSuccess());
    
    EncryptionResult decrypted = encryptionService.decryptData(encrypted.getData());
    assertTrue(decrypted.isSuccess());
    assertEquals(originalData, decrypted.getData());
}
```

### Security Scan Integration

```bash
# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# Security vulnerability scanning
docker run --rm -v $(pwd):/workspace securecodewarrior/github-action-add-sarif

# SSL/TLS testing
testssl.sh https://your-domain.com
```

## 🚨 Security Incident Response

### Detection

1. **Automated Monitoring**: Real-time security event detection
2. **Rate Limit Violations**: Automatic blocking and alerting
3. **Failed Authentication**: Pattern detection for brute force attacks
4. **Input Validation Failures**: Potential attack attempt logging

### Response Procedures

1. **Immediate Response**
   ```bash
   # Block suspicious IP
   iptables -A INPUT -s SUSPICIOUS_IP -j DROP
   
   # Reset rate limits for affected users
   curl -X POST "http://localhost:8080/api/security/rate-limit/reset?identifier=user123&type=API_GENERAL"
   ```

2. **Investigation**
   ```bash
   # Review security logs
   grep "security violation" logs/security-events.log
   
   # Check rate limiting status
   curl "http://localhost:8080/api/security/rate-limit/statistics"
   ```

3. **Recovery**
   ```bash
   # Rotate compromised keys
   ./scripts/security-setup.sh production true
   
   # Update security configuration
   ./scripts/validate-security.sh
   ```

## 🎯 Best Practices

### 1. Key Management
- Use dedicated key management service (AWS KMS, Azure Key Vault, HashiCorp Vault)
- Rotate encryption keys regularly
- Never store keys in code or configuration files
- Use environment variables or secure configuration management

### 2. Rate Limiting Strategy
- Set limits based on actual usage patterns
- Use distributed rate limiting in multi-instance deployments
- Implement different limits for different user types
- Monitor and adjust limits based on attack patterns

### 3. Input Validation
- Validate all inputs at multiple layers
- Use whitelist approach rather than blacklist
- Sanitize output as well as input
- Implement proper error handling without information disclosure

### 4. Encryption Practices
- Use authenticated encryption (AES-GCM)
- Never use ECB mode
- Use unique IVs for each encryption operation
- Implement proper key derivation functions

### 5. Security Headers
- Use strict CSP policies in production
- Enable HSTS with preload
- Implement proper CORS policies
- Use security headers testing tools

## 🔧 Troubleshooting

### Common Issues

#### 1. Rate Limiting Not Working

**Problem**: Rate limits not being enforced
**Solution**:
```bash
# Check rate limiting service status
curl http://localhost:8080/api/security/health

# Verify Redis connection (if using distributed)
redis-cli ping

# Check configuration
grep -r "rate-limiting" src/main/resources/
```

#### 2. Encryption Failures

**Problem**: Data encryption/decryption failing
**Solution**:
```bash
# Verify master key is set
echo $ENCRYPTION_MASTER_KEY

# Check key format (should be base64)
echo $ENCRYPTION_MASTER_KEY | base64 -d | wc -c  # Should output 32

# Test encryption service
curl -X POST http://localhost:8080/api/security/hash \
  -H "Content-Type: application/json" \
  -d '{"data":"test","algorithm":"sha256"}'
```

#### 3. CORS Issues

**Problem**: CORS errors in browser
**Solution**:
```yaml
# Add specific origins to configuration
security:
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://yourdomain.com
```

#### 4. CSP Violations

**Problem**: Content Security Policy blocking resources
**Solution**:
```javascript
// Check browser console for CSP violations
// Adjust CSP policy based on requirements

// For development, use more permissive policy
"script-src 'self' 'unsafe-inline' 'unsafe-eval'"

// For production, use strict policy and fix violations
"script-src 'self'"
```

## 📋 Security Checklist

### Pre-Production

- [ ] Generate strong encryption keys
- [ ] Configure SSL/TLS certificates
- [ ] Set up distributed rate limiting with Redis
- [ ] Configure OAuth2 providers
- [ ] Set strict CSP policies
- [ ] Enable all security headers
- [ ] Set up security monitoring
- [ ] Configure audit logging
- [ ] Test all security features
- [ ] Conduct penetration testing

### Production Deployment

- [ ] Use HTTPS everywhere
- [ ] Enable malware scanning
- [ ] Set up intrusion detection
- [ ] Configure log aggregation
- [ ] Set up security alerts
- [ ] Implement backup procedures
- [ ] Document incident response procedures
- [ ] Train team on security procedures

### Regular Maintenance

- [ ] Review security logs weekly
- [ ] Update security dependencies monthly
- [ ] Rotate encryption keys quarterly
- [ ] Conduct security audits annually
- [ ] Review and update security policies
- [ ] Test disaster recovery procedures

## 📞 Support

For security-related issues:

1. **Review Logs**: Check security event logs in `logs/security/`
2. **Run Diagnostics**: Use `./scripts/validate-security.sh`
3. **Monitor Status**: Use `./scripts/security-monitor.sh`
4. **Check Configuration**: Verify environment variables and configuration files

**Security Features Status**: ✅ Production Ready  
**Last Security Review**: January 2024  
**Next Scheduled Review**: July 2024

---

## 🔐 Security Contact

For security vulnerabilities or concerns:
- **Security Team**: security@yourcompany.com
- **Emergency Contact**: +1-XXX-XXX-XXXX
- **PGP Key**: Available at https://yourcompany.com/security/pgp-key

**Remember**: Security is everyone's responsibility. Report suspicious activity immediately.