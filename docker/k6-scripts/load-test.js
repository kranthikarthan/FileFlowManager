/**
 * K6 Load Testing Script for File Transfer Management System
 * Tests API performance under various load conditions
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const apiErrors = new Counter('api_errors');
const apiSuccessRate = new Rate('api_success_rate');
const apiResponseTime = new Trend('api_response_time');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 10 },   // Ramp up to 10 users
    { duration: '5m', target: 10 },   // Stay at 10 users
    { duration: '2m', target: 20 },   // Ramp up to 20 users
    { duration: '5m', target: 20 },   // Stay at 20 users
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '10m', target: 50 },  // Stay at 50 users
    { duration: '2m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.05'],    // Error rate should be below 5%
    api_success_rate: ['rate>0.95'],   // Success rate should be above 95%
  },
};

// Base URLs
const WEB_BASE_URL = 'http://file-transfer-web-test:8080';
const BATCH_BASE_URL = 'http://file-transfer-batch-test:8082';
const FRONTEND_BASE_URL = 'http://file-transfer-frontend-test:3000';

// Test data
const testTenant = {
  name: `Load Test Tenant ${Math.random()}`,
  description: 'Load testing tenant',
  timezone: 'America/New_York',
  contactEmail: 'loadtest@example.com',
  active: true
};

const testService = {
  name: `Load Test Service ${Math.random()}`,
  description: 'Load testing service',
  active: true
};

const testBatchJob = {
  tenantId: 'load-test-tenant',
  inputPath: '/data/test/input',
  outputPath: '/data/test/output',
  chunkSize: 100,
  threadCount: 2
};

export default function() {
  // Test different API versions
  const apiVersion = Math.random() > 0.5 ? 'v2' : 'v1';
  const headers = {
    'Content-Type': 'application/json',
    'X-API-Version': apiVersion === 'v1' ? '1.2' : '2.0'
  };

  group('Web API Load Test', () => {
    
    group('Tenant Management', () => {
      // Get tenants list
      let response = http.get(`${WEB_BASE_URL}/api/${apiVersion}/tenants`, { headers });
      
      check(response, {
        'get tenants status is 200': (r) => r.status === 200,
        'get tenants response time < 500ms': (r) => r.timings.duration < 500,
      });
      
      apiSuccessRate.add(response.status === 200);
      apiResponseTime.add(response.timings.duration);
      
      if (response.status !== 200) {
        apiErrors.add(1);
      }

      // Create tenant (v2 only for enhanced features)
      if (apiVersion === 'v2') {
        response = http.post(`${WEB_BASE_URL}/api/v2/tenants`, JSON.stringify(testTenant), { headers });
        
        check(response, {
          'create tenant status is 201': (r) => r.status === 201,
          'create tenant has ID': (r) => r.json('id') !== null,
        });
        
        apiSuccessRate.add(response.status === 201);
        
        if (response.status === 201) {
          const tenantId = response.json('id');
          
          // Get tenant details
          response = http.get(`${WEB_BASE_URL}/api/v2/tenants/${tenantId}`, { headers });
          
          check(response, {
            'get tenant details status is 200': (r) => r.status === 200,
            'tenant details match': (r) => r.json('name') === testTenant.name,
          });
          
          apiSuccessRate.add(response.status === 200);
        }
      }
    });

    group('Service Management', () => {
      // Get services
      let response = http.get(`${WEB_BASE_URL}/api/services`, { headers });
      
      check(response, {
        'get services status is 200': (r) => r.status === 200,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

    group('Schema Management', () => {
      // Get schemas
      let response = http.get(`${WEB_BASE_URL}/api/schemas`, { headers });
      
      check(response, {
        'get schemas status is 200': (r) => r.status === 200,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

    group('Monitoring Endpoints', () => {
      // Health check
      let response = http.get(`${WEB_BASE_URL}/actuator/health`, { headers });
      
      check(response, {
        'health check status is 200': (r) => r.status === 200,
        'health status is UP': (r) => r.json('status') === 'UP',
      });

      // Metrics
      response = http.get(`${WEB_BASE_URL}/actuator/metrics`, { headers });
      
      check(response, {
        'metrics status is 200': (r) => r.status === 200,
      });
    });

  });

  group('Batch API Load Test', () => {
    
    group('Job Management', () => {
      // Get job executions
      let response = http.get(`${BATCH_BASE_URL}/api/batch/${apiVersion}/jobs/executions`, { headers });
      
      check(response, {
        'get job executions status is 200': (r) => r.status === 200,
      });
      
      apiSuccessRate.add(response.status === 200);

      // Start batch job (v2 only for enhanced features)
      if (apiVersion === 'v2') {
        response = http.post(`${BATCH_BASE_URL}/api/batch/v2/jobs/start`, JSON.stringify(testBatchJob), { headers });
        
        check(response, {
          'start job status is 201': (r) => r.status === 201,
          'job execution ID exists': (r) => r.json('jobExecutionId') !== null,
        });
        
        apiSuccessRate.add(response.status === 201);
        
        if (response.status === 201) {
          const executionId = response.json('jobExecutionId');
          
          // Get job execution details
          response = http.get(`${BATCH_BASE_URL}/api/batch/v2/jobs/executions/${executionId}`, { headers });
          
          check(response, {
            'get job execution status is 200': (r) => r.status === 200,
            'execution status exists': (r) => r.json('status') !== null,
          });
          
          apiSuccessRate.add(response.status === 200);
        }
      }
    });

    group('Batch Statistics', () => {
      // Get batch statistics
      let response = http.get(`${BATCH_BASE_URL}/api/batch/${apiVersion}/jobs/statistics`, { headers });
      
      check(response, {
        'get statistics status is 200': (r) => r.status === 200,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

  });

  group('Frontend Load Test', () => {
    
    group('Static Assets', () => {
      // Test frontend availability
      let response = http.get(`${FRONTEND_BASE_URL}/`, { headers });
      
      check(response, {
        'frontend status is 200': (r) => r.status === 200,
        'frontend loads within 2s': (r) => r.timings.duration < 2000,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

    group('API Integration', () => {
      // Test frontend API calls through proxy
      let response = http.get(`${FRONTEND_BASE_URL}/api/v2/tenants`, { headers });
      
      check(response, {
        'frontend API proxy status is 200': (r) => r.status === 200,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

  });

  group('Cross-Application Integration', () => {
    
    group('Version Compatibility', () => {
      // Test API version compatibility across applications
      const v1Headers = { ...headers, 'X-API-Version': '1.2' };
      const v2Headers = { ...headers, 'X-API-Version': '2.0' };

      // Test v1 compatibility
      let response = http.get(`${WEB_BASE_URL}/api/v1/tenants`, { headers: v1Headers });
      check(response, {
        'v1 API compatibility': (r) => r.status === 200,
        'v1 deprecation header exists': (r) => r.headers['Deprecation'] === 'true',
      });

      // Test v2 current version
      response = http.get(`${WEB_BASE_URL}/api/v2/tenants`, { headers: v2Headers });
      check(response, {
        'v2 API current': (r) => r.status === 200,
        'v2 no deprecation': (r) => !r.headers['Deprecation'],
      });
    });

    group('Backup Coordination', () => {
      // Test cross-application backup
      const backupRequest = {
        type: 'MANUAL',
        includeWeb: true,
        includeBatch: true,
        includeFrontend: true,
        description: 'Load test backup'
      };

      let response = http.post(`${WEB_BASE_URL}/api/backup/cross-application/create`, 
                              JSON.stringify(backupRequest), { headers });
      
      check(response, {
        'cross-app backup initiated': (r) => r.status === 200,
        'coordination ID exists': (r) => r.json('coordinationId') !== null,
      });
      
      apiSuccessRate.add(response.status === 200);
    });

  });

  // Random sleep between 1-3 seconds to simulate real user behavior
  sleep(Math.random() * 2 + 1);
}

// Setup function (runs once before all VUs)
export function setup() {
  console.log('Starting load test setup...');
  
  // Wait for services to be ready
  let retries = 30;
  while (retries > 0) {
    const healthCheck = http.get(`${WEB_BASE_URL}/actuator/health`);
    if (healthCheck.status === 200) {
      console.log('Services are ready for load testing');
      break;
    }
    retries--;
    sleep(2);
  }
  
  if (retries === 0) {
    console.error('Services did not become ready in time');
    throw new Error('Services not ready for testing');
  }
  
  return { timestamp: new Date().toISOString() };
}

// Teardown function (runs once after all VUs)
export function teardown(data) {
  console.log(`Load test completed at: ${new Date().toISOString()}`);
  console.log(`Test started at: ${data.timestamp}`);
  
  // Generate final summary
  const summary = {
    testStart: data.timestamp,
    testEnd: new Date().toISOString(),
    configuration: options,
    notes: 'Load test executed against all three applications with version compatibility testing'
  };
  
  console.log('Test Summary:', JSON.stringify(summary, null, 2));
}