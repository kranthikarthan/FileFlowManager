/**
 * Complete System Workflow E2E Tests
 * Tests the entire file transfer system from tenant setup to file processing
 */

describe('Complete System Workflow E2E Tests', () => {
  
  let testTenantId;
  let testServiceId;
  let testSubServiceId;
  let testSchemaId;

  beforeEach(() => {
    // Set up comprehensive API interceptors
    cy.intercept('POST', '/api/v2/tenants', { fixture: 'tenant-created.json' }).as('createTenant');
    cy.intercept('POST', '/api/services', { fixture: 'service-created.json' }).as('createService');
    cy.intercept('POST', '/api/subservices', { fixture: 'subservice-created.json' }).as('createSubService');
    cy.intercept('POST', '/api/schemas', { fixture: 'schema-created.json' }).as('createSchema');
    cy.intercept('POST', '/api/cutoff-times', { fixture: 'cutoff-created.json' }).as('createCutOff');
    cy.intercept('POST', '/api/files/upload', { fixture: 'file-uploaded.json' }).as('uploadFile');
    cy.intercept('GET', '/api/files/*/status', { fixture: 'file-processed.json' }).as('getFileStatus');
    cy.intercept('POST', '/api/batch/v2/jobs/start', { fixture: 'job-started.json' }).as('startBatchJob');
    cy.intercept('GET', '/api/batch/v2/jobs/executions/*', { fixture: 'job-completed.json' }).as('getJobStatus');
    
    // Visit application
    cy.visit('/');
  });

  it('E2E-COMPLETE-1: Full tenant setup to file processing workflow', () => {
    // STEP 1: Create Tenant
    cy.get('[data-testid="guided-setup-button"]').click();
    cy.get('[data-testid="setup-tenant-option"]').click();
    
    // Fill tenant information
    cy.get('[data-testid="tenant-name-input"]').type('E2E Complete Test Tenant');
    cy.get('[data-testid="tenant-description-input"]').type('Complete end-to-end test tenant');
    cy.get('[data-testid="tenant-timezone-select"]').click();
    cy.get('[data-value="America/New_York"]').click();
    cy.get('[data-testid="tenant-email-input"]').type('e2e-complete@example.com');
    cy.get('[data-testid="next-step-button"]').click();
    
    cy.wait('@createTenant').then((interception) => {
      testTenantId = interception.response.body.id;
    });

    // STEP 2: Create Service
    cy.get('[data-testid="service-name-input"]').type('E2E Test Service');
    cy.get('[data-testid="service-description-input"]').type('End-to-end test service');
    cy.get('[data-testid="next-step-button"]').click();
    
    cy.wait('@createService').then((interception) => {
      testServiceId = interception.response.body.id;
    });

    // STEP 3: Create Sub-Service
    cy.get('[data-testid="subservice-name-input"]').type('E2E Test Sub-Service');
    cy.get('[data-testid="subservice-description-input"]').type('End-to-end test sub-service');
    cy.get('[data-testid="next-step-button"]').click();
    
    cy.wait('@createSubService').then((interception) => {
      testSubServiceId = interception.response.body.id;
    });

    // STEP 4: Configure Cut-off Times
    cy.get('[data-testid="cutoff-time-input"]').type('23:59');
    cy.get('[data-testid="cutoff-type-select"]').click();
    cy.get('[data-value="END_OF_DAY"]').click();
    cy.get('[data-testid="next-step-button"]').click();
    
    cy.wait('@createCutOff');

    // STEP 5: Create Schema
    cy.get('[data-testid="schema-name-input"]').type('E2E Test Schema');
    cy.get('[data-testid="file-type-select"]').click();
    cy.get('[data-value="JSON"]').click();
    cy.get('[data-testid="direction-select"]').click();
    cy.get('[data-value="INBOUND"]').click();
    
    // Add schema content
    cy.get('[data-testid="schema-content-editor"]').type(`{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "value": {"type": "number"}
      },
      "required": ["name", "value"]
    }`);
    
    cy.get('[data-testid="finish-setup-button"]').click();
    
    cy.wait('@createSchema').then((interception) => {
      testSchemaId = interception.response.body.id;
    });

    // STEP 6: Verify Setup Complete
    cy.get('[data-testid="setup-complete-notification"]')
      .should('contain', 'Tenant setup completed successfully');

    // STEP 7: Navigate to File Upload
    cy.get('[data-testid="navigate-to-files"]').click();
    cy.url().should('include', '/files');

    // STEP 8: Upload File
    const testFile = new File(['{"name": "test", "value": 123}'], 'test.json', { type: 'application/json' });
    
    cy.get('[data-testid="file-upload-dropzone"]').selectFile({
      contents: Cypress.Buffer.from('{"name": "test", "value": 123}'),
      fileName: 'test.json',
      mimeType: 'application/json'
    }, { action: 'drag-drop' });
    
    cy.get('[data-testid="upload-button"]').click();
    
    cy.wait('@uploadFile');

    // STEP 9: Verify File Processing
    cy.get('[data-testid="processing-status"]').should('contain', 'Processing');
    
    cy.wait('@getFileStatus');
    
    cy.get('[data-testid="processing-status"]').should('contain', 'Completed');
    cy.get('[data-testid="validation-result"]').should('contain', 'Valid');

    // STEP 10: Check Dashboard Updates
    cy.get('[data-testid="navigate-to-dashboard"]').click();
    cy.url().should('include', '/dashboard');
    
    cy.get('[data-testid="files-processed-today"]').should('contain', '1');
    cy.get('[data-testid="success-rate"]').should('be.visible');
  });

  it('E2E-COMPLETE-2: Service configuration and file type setup workflow', () => {
    // Navigate to services
    cy.visit('/services');
    
    // Create service with multiple file types
    cy.get('[data-testid="create-service-button"]').click();
    
    cy.get('[data-testid="service-name-input"]').type('Multi-Type Service');
    cy.get('[data-testid="service-description-input"]').type('Service supporting multiple file types');
    
    // Configure multiple file types
    cy.get('[data-testid="add-file-type-button"]').click();
    cy.get('[data-testid="file-type-select-0"]').click();
    cy.get('[data-value="JSON"]').click();
    
    cy.get('[data-testid="add-file-type-button"]').click();
    cy.get('[data-testid="file-type-select-1"]').click();
    cy.get('[data-value="XML"]').click();
    
    cy.get('[data-testid="add-file-type-button"]').click();
    cy.get('[data-testid="file-type-select-2"]').click();
    cy.get('[data-value="COBOL"]').click();
    
    // Configure directions
    cy.get('[data-testid="inbound-checkbox"]').check();
    cy.get('[data-testid="outbound-checkbox"]').check();
    
    cy.get('[data-testid="save-service-button"]').click();
    
    cy.wait('@createService');
    
    // Verify service appears in list
    cy.get('[data-testid="service-list"]').should('contain', 'Multi-Type Service');
    
    // Test file type validation
    cy.get('[data-testid="service-item"]').first().within(() => {
      cy.get('[data-testid="test-file-types-button"]').click();
    });
    
    // Upload different file types
    const jsonFile = '{"test": "data"}';
    const xmlFile = '<root><test>data</test></root>';
    
    cy.get('[data-testid="test-json-upload"]').selectFile({
      contents: Cypress.Buffer.from(jsonFile),
      fileName: 'test.json',
      mimeType: 'application/json'
    });
    
    cy.get('[data-testid="test-xml-upload"]').selectFile({
      contents: Cypress.Buffer.from(xmlFile),
      fileName: 'test.xml',
      mimeType: 'application/xml'
    });
    
    cy.get('[data-testid="validate-files-button"]').click();
    
    cy.get('[data-testid="json-validation-result"]').should('contain', 'Valid');
    cy.get('[data-testid="xml-validation-result"]').should('contain', 'Valid');
  });

  it('E2E-COMPLETE-3: EOT validation and file count verification workflow', () => {
    // Navigate to EOT validation
    cy.visit('/eot-validation');
    
    // Configure EOT validation for a sub-service
    cy.get('[data-testid="configure-eot-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="service-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="direction-select"]').click();
    cy.get('[data-value="INBOUND"]').click();
    
    // Configure count field
    cy.get('[data-testid="count-field-path-input"]').type('fileCount');
    cy.get('[data-testid="tolerance-percentage-input"]').type('5');
    
    cy.get('[data-testid="enable-eot-validation"]').check();
    cy.get('[data-testid="save-eot-config-button"]').click();
    
    // Submit EOT file
    cy.get('[data-testid="submit-eot-button"]').click();
    
    const eotContent = '{"fileCount": 5, "processingDate": "2023-12-01", "totalSize": 1048576}';
    cy.get('[data-testid="eot-content-editor"]').type(eotContent);
    cy.get('[data-testid="processing-date-input"]').type('2023-12-01');
    cy.get('[data-testid="submit-eot-file-button"]').click();
    
    // Simulate file uploads
    for (let i = 1; i <= 5; i++) {
      cy.get('[data-testid="simulate-file-upload"]').click();
      cy.get('[data-testid="file-name-input"]').type(`data-file-${i}.json`);
      cy.get('[data-testid="file-content-input"]').type(`{"id": ${i}, "data": "test"}`);
      cy.get('[data-testid="upload-simulated-file"]').click();
    }
    
    // Trigger EOT validation
    cy.get('[data-testid="validate-eot-button"]').click();
    
    // Verify validation results
    cy.get('[data-testid="eot-validation-result"]').should('contain', 'Valid');
    cy.get('[data-testid="expected-count"]').should('contain', '5');
    cy.get('[data-testid="actual-count"]').should('contain', '5');
    cy.get('[data-testid="validation-status"]').should('contain', 'PASSED');
  });

});

describe('Cross-Application E2E Workflows', () => {

  it('E2E-CROSS-1: Web to Batch integration workflow', () => {
    // Start from web application
    cy.visit('/batch-integration');
    
    // Configure batch job from web interface
    cy.get('[data-testid="create-batch-job-button"]').click();
    
    cy.get('[data-testid="job-name-input"]').type('E2E Integration Job');
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="input-path-input"]').type('/data/input/e2e');
    cy.get('[data-testid="output-path-input"]').type('/data/output/e2e');
    cy.get('[data-testid="chunk-size-input"]').type('100');
    cy.get('[data-testid="thread-count-input"]').type('4');
    
    cy.get('[data-testid="start-batch-job-button"]').click();
    
    cy.wait('@startBatchJob').then((interception) => {
      const jobExecutionId = interception.response.body.jobExecutionId;
      
      // Monitor job progress
      cy.get('[data-testid="job-monitoring-panel"]').should('be.visible');
      cy.get('[data-testid="job-execution-id"]').should('contain', jobExecutionId);
      
      // Wait for job completion
      cy.get('[data-testid="job-status"]').should('contain', 'COMPLETED', { timeout: 30000 });
      
      // Verify job results
      cy.get('[data-testid="files-processed"]').should('be.visible');
      cy.get('[data-testid="success-rate"]').should('contain', '100%');
    });
  });

  it('E2E-CROSS-2: Frontend to Web to Batch complete workflow', () => {
    // Start from dashboard
    cy.visit('/dashboard');
    
    // Navigate through guided setup
    cy.get('[data-testid="quick-setup-wizard"]').click();
    cy.get('[data-testid="complete-setup-option"]').click();
    
    // Quick tenant setup
    cy.get('[data-testid="tenant-quick-name"]').type('Quick Setup Tenant');
    cy.get('[data-testid="tenant-quick-timezone"]').click();
    cy.get('[data-value="UTC"]').click();
    cy.get('[data-testid="create-tenant-quick"]').click();
    
    cy.wait('@createTenant');
    
    // Quick service setup
    cy.get('[data-testid="service-quick-name"]').type('Quick Setup Service');
    cy.get('[data-testid="service-quick-type"]').click();
    cy.get('[data-value="FILE_TRANSFER"]').click();
    cy.get('[data-testid="create-service-quick"]').click();
    
    cy.wait('@createService');
    
    // Quick sub-service setup
    cy.get('[data-testid="subservice-quick-name"]').type('Quick Setup Sub-Service');
    cy.get('[data-testid="subservice-direction"]').click();
    cy.get('[data-value="INBOUND"]').click();
    cy.get('[data-testid="create-subservice-quick"]').click();
    
    cy.wait('@createSubService');
    
    // Configure schema quickly
    cy.get('[data-testid="schema-template-select"]').click();
    cy.get('[data-value="SIMPLE_JSON"]').click();
    cy.get('[data-testid="apply-schema-template"]').click();
    
    cy.wait('@createSchema');
    
    // Complete setup
    cy.get('[data-testid="complete-quick-setup"]').click();
    
    // Navigate to file processing
    cy.get('[data-testid="test-file-processing"]').click();
    
    // Upload test file
    cy.get('[data-testid="file-upload-area"]').selectFile({
      contents: Cypress.Buffer.from('{"name": "quick test", "value": 456}'),
      fileName: 'quick-test.json',
      mimeType: 'application/json'
    }, { action: 'drag-drop' });
    
    cy.get('[data-testid="process-file-button"]').click();
    
    cy.wait('@uploadFile');
    
    // Monitor processing through batch system
    cy.get('[data-testid="batch-processing-status"]').should('be.visible');
    cy.get('[data-testid="view-batch-details"]').click();
    
    // Should redirect to batch monitoring
    cy.url().should('include', '/batch-monitoring');
    
    cy.wait('@getJobStatus');
    
    // Verify complete workflow
    cy.get('[data-testid="job-status"]').should('contain', 'COMPLETED');
    cy.get('[data-testid="files-processed"]').should('contain', '1');
    cy.get('[data-testid="validation-passed"]').should('contain', 'true');
  });

  it('E2E-CROSS-3: API version compatibility across applications', () => {
    // Test v1 to v2 migration workflow
    cy.visit('/api-management');
    
    // Check current API versions
    cy.get('[data-testid="web-api-version"]').should('contain', '2.0');
    cy.get('[data-testid="batch-api-version"]').should('contain', '2.0');
    
    // Test version downgrade (for legacy client simulation)
    cy.get('[data-testid="change-api-version"]').click();
    cy.get('[data-testid="version-select"]').click();
    cy.get('[data-value="1.2"]').click();
    cy.get('[data-testid="apply-version-change"]').click();
    
    // Should show deprecation warning
    cy.get('[data-testid="deprecation-warning"]').should('be.visible');
    cy.get('[data-testid="deprecation-warning"]').should('contain', 'deprecated');
    cy.get('[data-testid="sunset-date"]').should('contain', '2024-03-01');
    
    // Test functionality with deprecated version
    cy.visit('/tenants');
    cy.get('[data-testid="tenant-list"]').should('be.visible');
    
    // Should work but with limitations
    cy.get('[data-testid="feature-limitation-notice"]').should('contain', 'limited features');
    
    // Upgrade back to current version
    cy.get('[data-testid="upgrade-to-current"]').click();
    cy.get('[data-testid="confirm-upgrade"]').click();
    
    // Should show enhanced features
    cy.get('[data-testid="enhanced-features-notice"]').should('contain', 'enhanced features');
    cy.get('[data-testid="pagination-controls"]').should('be.visible');
  });

  it('E2E-CROSS-4: Backup and disaster recovery workflow', () => {
    // Navigate to backup management
    cy.visit('/backup-management');
    
    // Create manual backup
    cy.get('[data-testid="create-backup-button"]').click();
    
    cy.get('[data-testid="backup-type-select"]').click();
    cy.get('[data-value="FULL"]').click();
    
    cy.get('[data-testid="include-web-checkbox"]').check();
    cy.get('[data-testid="include-batch-checkbox"]').check();
    cy.get('[data-testid="include-frontend-checkbox"]').check();
    
    cy.get('[data-testid="backup-description-input"]').type('E2E test backup');
    
    cy.get('[data-testid="start-backup-button"]').click();
    
    // Monitor backup progress
    cy.get('[data-testid="backup-progress"]').should('be.visible');
    cy.get('[data-testid="backup-status"]').should('contain', 'In Progress');
    
    // Wait for backup completion
    cy.get('[data-testid="backup-status"]').should('contain', 'Completed', { timeout: 60000 });
    
    // Verify backup details
    cy.get('[data-testid="backup-size"]').should('be.visible');
    cy.get('[data-testid="backup-duration"]').should('be.visible');
    cy.get('[data-testid="backup-applications"]').should('contain', '3');
    
    // Test restore functionality
    cy.get('[data-testid="test-restore-button"]').click();
    cy.get('[data-testid="restore-type-select"]').click();
    cy.get('[data-value="TEST_RESTORE"]').click();
    
    cy.get('[data-testid="confirm-test-restore"]').click();
    
    // Monitor restore test
    cy.get('[data-testid="restore-test-status"]').should('contain', 'Testing');
    cy.get('[data-testid="restore-test-status"]').should('contain', 'Completed', { timeout: 30000 });
    cy.get('[data-testid="restore-test-result"]').should('contain', 'Success');
  });

  it('E2E-CROSS-5: Monitoring and alerting across all applications', () => {
    // Navigate to monitoring dashboard
    cy.visit('/monitoring');
    
    // Check system health across all apps
    cy.get('[data-testid="web-app-health"]').should('contain', 'UP');
    cy.get('[data-testid="batch-app-health"]').should('contain', 'UP');
    cy.get('[data-testid="frontend-app-health"]').should('contain', 'UP');
    
    // Check metrics from all applications
    cy.get('[data-testid="web-metrics-panel"]').should('be.visible');
    cy.get('[data-testid="batch-metrics-panel"]').should('be.visible');
    cy.get('[data-testid="frontend-metrics-panel"]').should('be.visible');
    
    // Test alert creation
    cy.get('[data-testid="create-test-alert"]').click();
    
    cy.get('[data-testid="alert-level-select"]').click();
    cy.get('[data-value="WARNING"]').click();
    
    cy.get('[data-testid="alert-message-input"]').type('E2E test alert');
    cy.get('[data-testid="alert-component-select"]').click();
    cy.get('[data-value="FILE_PROCESSING"]').click();
    
    cy.get('[data-testid="create-alert-button"]').click();
    
    // Verify alert appears in all relevant dashboards
    cy.get('[data-testid="alert-list"]').should('contain', 'E2E test alert');
    cy.get('[data-testid="alert-level"]').should('contain', 'WARNING');
    
    // Check alert propagation to batch monitoring
    cy.visit('/batch-monitoring');
    cy.get('[data-testid="recent-alerts"]').should('contain', 'E2E test alert');
    
    // Check alert in web monitoring
    cy.visit('/web-monitoring');
    cy.get('[data-testid="system-alerts"]').should('contain', 'E2E test alert');
  });

});

describe('Mobile and Responsive E2E Tests', () => {

  it('E2E-MOBILE-1: Complete workflow on mobile device', () => {
    // Set mobile viewport
    cy.viewport(375, 667);
    
    cy.visit('/');
    
    // Test mobile navigation
    cy.get('[data-testid="mobile-menu-button"]').click();
    cy.get('[data-testid="mobile-navigation"]').should('be.visible');
    
    // Navigate to tenant management on mobile
    cy.get('[data-testid="mobile-nav-tenants"]').click();
    cy.get('[data-testid="mobile-navigation"]').should('not.be.visible');
    
    // Test mobile tenant creation
    cy.get('[data-testid="mobile-create-button"]').click();
    
    // Mobile form should be optimized
    cy.get('[data-testid="mobile-form-container"]').should('be.visible');
    cy.get('[data-testid="tenant-name-input"]').should('be.visible');
    
    // Fill form on mobile
    cy.get('[data-testid="tenant-name-input"]').type('Mobile Test Tenant');
    cy.get('[data-testid="tenant-description-textarea"]').type('Created on mobile device');
    
    // Mobile timezone picker
    cy.get('[data-testid="mobile-timezone-picker"]').click();
    cy.get('[data-testid="timezone-search"]').type('New York');
    cy.get('[data-value="America/New_York"]').click();
    
    cy.get('[data-testid="mobile-save-button"]').click();
    
    cy.wait('@createTenant');
    
    // Mobile success notification
    cy.get('[data-testid="mobile-success-toast"]').should('be.visible');
    cy.get('[data-testid="mobile-success-toast"]').should('contain', 'Tenant created');
  });

  it('E2E-MOBILE-2: Dark/Light theme workflow on mobile', () => {
    cy.viewport(375, 667);
    cy.visit('/');
    
    // Check initial theme
    cy.get('body').should('have.class', 'light-theme');
    
    // Access theme settings on mobile
    cy.get('[data-testid="mobile-menu-button"]').click();
    cy.get('[data-testid="mobile-nav-settings"]').click();
    
    // Switch to dark theme
    cy.get('[data-testid="theme-toggle-mobile"]').click();
    cy.get('body').should('have.class', 'dark-theme');
    
    // Verify dark theme elements
    cy.get('[data-testid="dark-theme-header"]').should('be.visible');
    cy.get('[data-testid="dark-theme-sidebar"]').should('be.visible');
    
    // Test auto theme switching
    cy.get('[data-testid="auto-theme-toggle"]').click();
    
    // Simulate time-based switching (mock system time)
    cy.window().then((win) => {
      // Mock current hour as night time
      cy.stub(win.Date.prototype, 'getHours').returns(22);
    });
    
    cy.get('[data-testid="refresh-theme"]').click();
    cy.get('body').should('have.class', 'dark-theme');
    
    // Mock day time
    cy.window().then((win) => {
      cy.stub(win.Date.prototype, 'getHours').returns(10);
    });
    
    cy.get('[data-testid="refresh-theme"]').click();
    cy.get('body').should('have.class', 'light-theme');
  });

});

describe('Offline and PWA E2E Tests', () => {

  it('E2E-PWA-1: Offline functionality workflow', () => {
    cy.visit('/');
    
    // Create some data while online
    cy.get('[data-testid="create-tenant-button"]').click();
    cy.get('[data-testid="tenant-name-input"]').type('Offline Test Tenant');
    cy.get('[data-testid="tenant-timezone-select"]').click();
    cy.get('[data-value="UTC"]').click();
    cy.get('[data-testid="save-tenant-button"]').click();
    
    cy.wait('@createTenant');
    
    // Go offline
    cy.window().then((win) => {
      cy.stub(win.navigator, 'onLine').value(false);
      win.dispatchEvent(new Event('offline'));
    });
    
    // Check offline indicator
    cy.get('[data-testid="offline-indicator"]').should('be.visible');
    cy.get('[data-testid="offline-mode-banner"]').should('contain', 'working offline');
    
    // Test offline data access
    cy.visit('/tenants');
    cy.get('[data-testid="tenant-list"]').should('be.visible');
    cy.get('[data-testid="cached-data-notice"]').should('be.visible');
    
    // Try to create tenant while offline
    cy.get('[data-testid="create-tenant-button"]').click();
    cy.get('[data-testid="tenant-name-input"]').type('Offline Created Tenant');
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Should queue for sync
    cy.get('[data-testid="queued-for-sync-notice"]').should('be.visible');
    
    // Go back online
    cy.window().then((win) => {
      cy.stub(win.navigator, 'onLine').value(true);
      win.dispatchEvent(new Event('online'));
    });
    
    // Check sync process
    cy.get('[data-testid="syncing-notice"]').should('be.visible');
    cy.get('[data-testid="sync-complete-notice"]').should('be.visible', { timeout: 10000 });
    
    // Verify data was synced
    cy.get('[data-testid="tenant-list"]').should('contain', 'Offline Created Tenant');
  });

  it('E2E-PWA-2: Install and notification workflow', () => {
    cy.visit('/');
    
    // Simulate PWA install prompt
    cy.window().then((win) => {
      const installEvent = new Event('beforeinstallprompt');
      installEvent.prompt = cy.stub().resolves();
      win.dispatchEvent(installEvent);
    });
    
    // Check install prompt
    cy.get('[data-testid="pwa-install-prompt"]').should('be.visible');
    cy.get('[data-testid="install-app-button"]').click();
    
    // Test push notification setup
    cy.get('[data-testid="enable-notifications-button"]').click();
    
    // Mock notification permission
    cy.window().then((win) => {
      cy.stub(win.Notification, 'requestPermission').resolves('granted');
    });
    
    cy.get('[data-testid="notification-permission-granted"]').should('be.visible');
    
    // Test notification for file processing completion
    cy.visit('/files');
    
    // Upload file
    cy.get('[data-testid="file-upload-area"]').selectFile({
      contents: Cypress.Buffer.from('{"test": "notification"}'),
      fileName: 'notification-test.json',
      mimeType: 'application/json'
    });
    
    cy.get('[data-testid="upload-with-notification"]').click();
    
    // Should show notification setup
    cy.get('[data-testid="notification-setup"]').should('be.visible');
    cy.get('[data-testid="notify-on-completion"]').check();
    
    cy.wait('@uploadFile');
    
    // Simulate file processing completion
    cy.window().then((win) => {
      win.dispatchEvent(new CustomEvent('fileProcessingComplete', {
        detail: { fileName: 'notification-test.json', status: 'success' }
      }));
    });
    
    // Should trigger notification
    cy.get('[data-testid="notification-sent"]').should('be.visible');
  });

});