/**
 * Test setup configuration for File Transfer Frontend Application
 * Configures testing environment, mocks, and global test utilities
 */

import '@testing-library/jest-dom';
import { configure } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { rest } from 'msw';

// Configure testing library
configure({
  testIdAttribute: 'data-testid',
  asyncUtilTimeout: 5000,
});

// Mock IntersectionObserver
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  observe() { return null; }
  disconnect() { return null; }
  unobserve() { return null; }
};

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  constructor() {}
  observe() { return null; }
  disconnect() { return null; }
  unobserve() { return null; }
};

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // deprecated
    removeListener: jest.fn(), // deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
  length: 0,
  key: jest.fn(),
};
global.localStorage = localStorageMock;

// Mock sessionStorage
const sessionStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
  length: 0,
  key: jest.fn(),
};
global.sessionStorage = sessionStorageMock;

// Mock IndexedDB
import 'fake-indexeddb/auto';

// Mock fetch for tests
global.fetch = jest.fn();

// Mock notification API
global.showNotification = jest.fn();

// Mock service worker
Object.defineProperty(navigator, 'serviceWorker', {
  value: {
    register: jest.fn(() => Promise.resolve()),
    ready: Promise.resolve({
      unregister: jest.fn(() => Promise.resolve()),
    }),
  },
  writable: true,
});

// Setup MSW server for API mocking
const server = setupServer(
  // Default API mocks
  rest.get('/api/versions', (req, res, ctx) => {
    return res(
      ctx.json({
        supportedVersions: [
          { version: '1.0', status: 'DEPRECATED', deprecated: true },
          { version: '2.0', status: 'CURRENT', deprecated: false },
          { version: '2.1', status: 'BETA', deprecated: false }
        ],
        totalVersions: 3
      })
    );
  }),

  rest.get('/api/v2/tenants', (req, res, ctx) => {
    return res(
      ctx.json({
        content: [
          {
            id: 1,
            name: 'Test Tenant',
            description: 'Test tenant description',
            timezone: 'America/New_York',
            active: true
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      })
    );
  }),

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

  rest.get('/actuator/health', (req, res, ctx) => {
    return res(
      ctx.json({
        status: 'UP',
        components: {
          db: { status: 'UP' },
          redis: { status: 'UP' },
          diskSpace: { status: 'UP' }
        }
      })
    );
  }),

  // Error scenarios for testing
  rest.get('/api/error-test', (req, res, ctx) => {
    return res(
      ctx.status(500),
      ctx.json({ error: 'Internal server error' })
    );
  }),

  rest.get('/api/timeout-test', (req, res, ctx) => {
    return res(
      ctx.delay(10000), // 10 second delay to test timeout
      ctx.json({ message: 'This should timeout' })
    );
  })
);

// Start server before tests
beforeAll(() => {
  server.listen();
  
  // Set up test environment variables
  process.env.REACT_APP_API_BASE_URL = 'http://localhost:8080';
  process.env.REACT_APP_BATCH_API_URL = 'http://localhost:8082';
  process.env.REACT_APP_API_VERSION = '2.0';
  process.env.REACT_APP_VERSION_STRATEGY = 'URL_PATH';
  process.env.REACT_APP_BACKUP_ENABLED = 'true';
  process.env.NODE_ENV = 'test';
});

// Reset handlers after each test
afterEach(() => {
  server.resetHandlers();
  
  // Clear all mocks
  jest.clearAllMocks();
  
  // Clear storage mocks
  localStorageMock.clear.mockClear();
  sessionStorageMock.clear.mockClear();
  
  // Reset fetch mock
  global.fetch.mockReset();
});

// Stop server after tests
afterAll(() => {
  server.close();
});

// Custom test utilities
export const testUtils = {
  // Mock API response
  mockApiResponse: (endpoint, response, status = 200) => {
    server.use(
      rest.get(endpoint, (req, res, ctx) => {
        return res(
          ctx.status(status),
          ctx.json(response)
        );
      })
    );
  },

  // Mock API error
  mockApiError: (endpoint, error = 'Internal server error', status = 500) => {
    server.use(
      rest.get(endpoint, (req, res, ctx) => {
        return res(
          ctx.status(status),
          ctx.json({ error })
        );
      })
    );
  },

  // Wait for async operations
  waitForAsync: (timeout = 5000) => {
    return new Promise(resolve => setTimeout(resolve, timeout));
  },

  // Create test tenant data
  createTestTenant: (overrides = {}) => ({
    id: 1,
    name: 'Test Tenant',
    description: 'Test tenant description',
    timezone: 'America/New_York',
    active: true,
    ...overrides
  }),

  // Create test service data
  createTestService: (overrides = {}) => ({
    id: 1,
    tenantId: 1,
    name: 'Test Service',
    description: 'Test service description',
    active: true,
    ...overrides
  }),

  // Simulate user interaction delay
  simulateUserDelay: (min = 100, max = 500) => {
    const delay = Math.random() * (max - min) + min;
    return new Promise(resolve => setTimeout(resolve, delay));
  }
};

// Global test configuration
global.testConfig = {
  apiBaseUrl: 'http://localhost:8080',
  batchApiUrl: 'http://localhost:8082',
  frontendUrl: 'http://localhost:3000',
  defaultTimeout: 5000,
  retryAttempts: 3,
  testDataPrefix: 'test_'
};

// Console override for cleaner test output
const originalConsoleError = console.error;
console.error = (...args) => {
  // Suppress known test warnings
  const message = args[0];
  if (
    typeof message === 'string' &&
    (message.includes('Warning: ReactDOM.render is deprecated') ||
     message.includes('Warning: componentWillReceiveProps') ||
     message.includes('act(...) is not supported'))
  ) {
    return;
  }
  originalConsoleError(...args);
};