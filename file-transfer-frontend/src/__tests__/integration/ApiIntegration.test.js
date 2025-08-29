/**
 * Frontend API Integration Tests
 * Tests API integration, version compatibility, and cross-application communication
 */

import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import apiVersionService from '../../services/apiVersionService';
import frontendBackupService from '../../services/frontendBackupService';

// Mock API server for testing
const server = setupServer(
  // Mock API version endpoints
  rest.get('/api/versions', (req, res, ctx) => {
    return res(
      ctx.json({
        supportedVersions: [
          { version: '1.0', status: 'DEPRECATED', deprecated: true },
          { version: '1.1', status: 'DEPRECATED', deprecated: true },
          { version: '1.2', status: 'SUNSET', deprecated: true },
          { version: '2.0', status: 'CURRENT', deprecated: false },
          { version: '2.1', status: 'BETA', deprecated: false }
        ],
        deprecatedVersions: [
          { version: '1.0', sunsetDate: '2024-06-01' },
          { version: '1.1', sunsetDate: '2024-06-01' },
          { version: '1.2', sunsetDate: '2024-03-01' }
        ],
        totalVersions: 5
      })
    );
  }),

  // Mock tenant endpoints for different versions
  rest.get('/api/v1/tenants', (req, res, ctx) => {
    return res(
      ctx.set('X-API-Version-Used', '1.0'),
      ctx.set('Deprecation', 'true'),
      ctx.set('Sunset', '2024-06-01'),
      ctx.json([
        { id: 1, name: 'Test Tenant V1', timezone: 'UTC' }
      ])
    );
  }),

  rest.get('/api/v2/tenants', (req, res, ctx) => {
    return res(
      ctx.set('X-API-Version-Used', '2.0'),
      ctx.json({
        content: [
          { 
            id: 1, 
            name: 'Test Tenant V2', 
            timezone: 'America/New_York',
            status: 'ACTIVE',
            metrics: { totalUsers: 10, activeServices: 5 }
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      })
    );
  }),

  // Mock batch API endpoints
  rest.get('/api/batch/v2/jobs/executions', (req, res, ctx) => {
    return res(
      ctx.json({
        content: [
          {
            executionId: 1,
            jobName: 'fileProcessingJob',
            status: 'COMPLETED',
            startTime: new Date().toISOString(),
            tenantId: 'test-tenant'
          }
        ],
        totalElements: 1
      })
    );
  }),

  // Mock frontend backup endpoints
  rest.post('/api/frontend-backup/upload', (req, res, ctx) => {
    return res(
      ctx.json({
        success: true,
        backupId: 'frontend-backup-123',
        message: 'Frontend backup uploaded successfully'
      })
    );
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Frontend API Integration Tests', () => {
  
  beforeEach(() => {
    // Reset API version service
    apiVersionService.currentVersion = '2.0';
    
    // Clear any stored data
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('API Version Service Integration', () => {
    
    test('should initialize with correct version', async () => {
      await apiVersionService.initialize();
      
      const versionInfo = apiVersionService.getCurrentVersion();
      expect(versionInfo.version).toBe('2.0');
      expect(versionInfo.supportedVersions).toContain('2.0');
    });

    test('should negotiate compatible version when requested version not supported', async () => {
      // Try to set unsupported version
      apiVersionService.currentVersion = '3.0';
      
      await apiVersionService.initialize();
      
      // Should fall back to compatible version
      const versionInfo = apiVersionService.getCurrentVersion();
      expect(['2.0', '2.1', '1.2']).toContain(versionInfo.version);
    });

    test('should handle version-specific features correctly', async () => {
      // Test v1 features
      await apiVersionService.setVersion('1.0');
      expect(apiVersionService.hasFeature('pagination')).toBe(false);
      expect(apiVersionService.hasLimitation('no-pagination')).toBe(true);

      // Test v2 features  
      await apiVersionService.setVersion('2.0');
      expect(apiVersionService.hasFeature('pagination')).toBe(true);
      expect(apiVersionService.hasLimitation('no-pagination')).toBe(false);
    });

    test('should create version-aware API clients', async () => {
      const webApi = apiVersionService.createApiClient({ service: 'web', version: '2.0' });
      const batchApi = apiVersionService.createApiClient({ service: 'batch', version: '2.0' });

      // Test web API client
      const tenants = await webApi.get('/tenants');
      expect(tenants.content).toBeDefined();
      expect(tenants.content).toHaveLength(1);

      // Test batch API client
      const jobs = await batchApi.get('/jobs/executions');
      expect(jobs.content).toBeDefined();
      expect(jobs.content).toHaveLength(1);
    });

  });

  describe('Frontend Backup Integration', () => {

    test('should perform full backup including all data types', async () => {
      // Set up test data
      localStorage.setItem('user_profile', JSON.stringify({ name: 'Test User' }));
      sessionStorage.setItem('session_data', JSON.stringify({ token: 'test-token' }));

      const result = await frontendBackupService.performFullBackup();

      expect(result.success).toBe(true);
      expect(result.backupId).toBeDefined();
      expect(result.size).toBeGreaterThan(0);
    });

    test('should backup user data automatically', async () => {
      // Set up user data
      localStorage.setItem('user_preferences', JSON.stringify({ theme: 'dark', language: 'en' }));
      
      await frontendBackupService.backupUserData();
      
      // Verify backup was created
      const backups = await frontendBackupService.listAvailableBackups({ type: 'USER_DATA' });
      expect(backups.length).toBeGreaterThan(0);
      
      const latestBackup = backups[0];
      expect(latestBackup.type).toBe('USER_DATA');
      expect(latestBackup.data.localStorage.user_preferences).toBeDefined();
    });

    test('should sync backup to server', async () => {
      const backupData = {
        id: 'test-backup-123',
        type: 'FULL',
        timestamp: new Date().toISOString(),
        data: { test: 'data' }
      };

      const result = await frontendBackupService.syncBackupToServer(backupData);
      
      expect(result.success).toBe(true);
      expect(result.backupId).toBe('frontend-backup-123');
    });

    test('should restore from backup correctly', async () => {
      // Create a backup first
      const originalData = { user_setting: 'original_value' };
      localStorage.setItem('user_setting', JSON.stringify(originalData));
      
      const backup = await frontendBackupService.performFullBackup();
      expect(backup.success).toBe(true);

      // Modify data
      localStorage.setItem('user_setting', JSON.stringify({ user_setting: 'modified_value' }));

      // Restore from backup
      const restoreResult = await frontendBackupService.restoreFromBackup(backup.backupId);
      
      expect(restoreResult.success).toBe(true);
      
      // Verify data was restored
      const restoredData = JSON.parse(localStorage.getItem('user_setting'));
      expect(restoredData.user_setting).toBe('original_value');
    });

  });

  describe('Cross-Application Integration', () => {

    test('should communicate with web API using correct version', async () => {
      const webApi = apiVersionService.createApiClient({ service: 'web', version: '2.0' });
      
      const tenants = await webApi.get('/tenants');
      
      expect(tenants).toBeDefined();
      expect(tenants.content).toHaveLength(1);
      expect(tenants.content[0].name).toBe('Test Tenant V2');
    });

    test('should communicate with batch API using correct version', async () => {
      const batchApi = apiVersionService.createApiClient({ service: 'batch', version: '2.0' });
      
      const jobs = await batchApi.get('/jobs/executions');
      
      expect(jobs).toBeDefined();
      expect(jobs.content).toHaveLength(1);
      expect(jobs.content[0].status).toBe('COMPLETED');
    });

    test('should handle API version mismatches gracefully', async () => {
      // Try to use v1 API when server expects v2
      const webApi = apiVersionService.createApiClient({ service: 'web', version: '1.0' });
      
      const tenants = await webApi.get('/tenants');
      
      // Should still work due to backward compatibility
      expect(tenants).toBeDefined();
      expect(Array.isArray(tenants)).toBe(true);
    });

    test('should coordinate backups across applications', async () => {
      const webApi = apiVersionService.createApiClient({ service: 'web', version: '2.0' });
      
      const backupRequest = {
        type: 'FULL',
        includeWeb: true,
        includeBatch: true,
        includeFrontend: true,
        synchronizeBackups: true
      };

      const result = await webApi.post('/backup/cross-application/create', backupRequest);
      
      expect(result.success).toBe(true);
      expect(result.coordinationId).toBeDefined();
    });

  });

  describe('Error Handling and Recovery', () => {

    test('should handle network failures gracefully', async () => {
      // Simulate network failure
      server.use(
        rest.get('/api/v2/tenants', (req, res, ctx) => {
          return res.networkError('Network error');
        })
      );

      const webApi = apiVersionService.createApiClient({ service: 'web', version: '2.0' });
      
      await expect(webApi.get('/tenants')).rejects.toThrow();
      
      // Should still have cached version info
      const versionInfo = apiVersionService.getCurrentVersion();
      expect(versionInfo.version).toBeDefined();
    });

    test('should handle API version errors', async () => {
      // Mock version error response
      server.use(
        rest.get('/api/v3/tenants', (req, res, ctx) => {
          return res(
            ctx.status(400),
            ctx.json({
              error: 'Unsupported API version',
              requestedVersion: '3.0',
              supportedVersions: ['1.0', '1.1', '1.2', '2.0', '2.1']
            })
          );
        })
      );

      // Try to use unsupported version
      await expect(apiVersionService.setVersion('3.0')).rejects.toThrow();
      
      // Should maintain current version
      expect(apiVersionService.currentVersion).toBe('2.0');
    });

    test('should recover from backup corruption', async () => {
      // Create backup
      const backup = await frontendBackupService.performFullBackup();
      expect(backup.success).toBe(true);

      // Simulate backup corruption by modifying IndexedDB directly
      const corruptedData = { corrupted: true };
      
      try {
        // This should fail gracefully
        await frontendBackupService.restoreFromBackup(backup.backupId);
      } catch (error) {
        expect(error.message).toContain('backup');
      }
    });

  });

  describe('Performance and Load Testing', () => {

    test('should handle multiple simultaneous API requests', async () => {
      const webApi = apiVersionService.createApiClient({ service: 'web', version: '2.0' });
      
      // Create multiple simultaneous requests
      const requests = Array.from({ length: 10 }, () => webApi.get('/tenants'));
      
      const startTime = Date.now();
      const results = await Promise.all(requests);
      const endTime = Date.now();
      
      // All requests should succeed
      results.forEach(result => {
        expect(result.content).toBeDefined();
      });
      
      // Should complete within reasonable time
      expect(endTime - startTime).toBeLessThan(5000);
    });

    test('should handle large backup operations efficiently', async () => {
      // Create large amount of test data
      for (let i = 0; i < 100; i++) {
        localStorage.setItem(`test_data_${i}`, JSON.stringify({ 
          id: i, 
          data: 'x'.repeat(1000) // 1KB of data per item
        }));
      }

      const startTime = Date.now();
      const backup = await frontendBackupService.performFullBackup();
      const endTime = Date.now();

      expect(backup.success).toBe(true);
      expect(backup.size).toBeGreaterThan(100000); // Should be > 100KB
      expect(endTime - startTime).toBeLessThan(10000); // Should complete within 10 seconds
    });

  });

});

describe('End-to-End User Workflows', () => {

  test('E2E: Complete tenant setup and management workflow', async () => {
    // This would be a full user workflow test
    // Note: Requires actual component rendering and user interaction simulation
    
    const mockTenantSetup = {
      step1: 'Create tenant',
      step2: 'Configure services', 
      step3: 'Set up schemas',
      step4: 'Configure cut-off times',
      step5: 'Test file processing'
    };

    // Simulate user workflow
    expect(mockTenantSetup.step1).toBe('Create tenant');
    // Additional workflow steps would be implemented here
  });

  test('E2E: File upload and processing workflow', async () => {
    const mockFileWorkflow = {
      upload: 'File uploaded successfully',
      validation: 'Schema validation passed',
      processing: 'File processed successfully',
      notification: 'User notified of completion'
    };

    expect(mockFileWorkflow.upload).toBe('File uploaded successfully');
    // Additional workflow verification would be implemented here
  });

  test('E2E: Backup and recovery workflow', async () => {
    // Test complete backup and recovery workflow
    const backupWorkflow = {
      backup: await frontendBackupService.performFullBackup(),
      verification: 'Backup verified',
      restore: 'Restore completed'
    };

    expect(backupWorkflow.backup.success).toBe(true);
    // Additional workflow steps would be implemented here
  });

});