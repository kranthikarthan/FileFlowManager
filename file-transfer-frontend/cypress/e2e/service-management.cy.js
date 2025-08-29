/**
 * Service Management E2E Tests
 * Tests complete service and sub-service management workflows
 */

describe('Service Management E2E Tests', () => {
  
  beforeEach(() => {
    // Set up API interceptors for service management
    cy.intercept('GET', '/api/services*', { fixture: 'services.json' }).as('getServices');
    cy.intercept('POST', '/api/services', { fixture: 'service-created.json' }).as('createService');
    cy.intercept('PUT', '/api/services/*', { fixture: 'service-updated.json' }).as('updateService');
    cy.intercept('DELETE', '/api/services/*', { statusCode: 204 }).as('deleteService');
    
    cy.intercept('GET', '/api/subservices*', { fixture: 'subservices.json' }).as('getSubServices');
    cy.intercept('POST', '/api/subservices', { fixture: 'subservice-created.json' }).as('createSubService');
    
    cy.intercept('GET', '/api/v2/tenants*', { fixture: 'tenants.json' }).as('getTenants');
    
    cy.visit('/services');
  });

  it('should complete service hierarchy setup workflow', () => {
    // STEP 1: Create Service Type
    cy.get('[data-testid="create-service-button"]').click();
    
    // Select tenant
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    // Fill service details
    cy.get('[data-testid="service-name-input"]').type('E2E Financial Service');
    cy.get('[data-testid="service-description-input"]').type('Financial data transfer service');
    cy.get('[data-testid="service-code-input"]').type('FIN001');
    
    // Configure service settings
    cy.get('[data-testid="enable-monitoring-checkbox"]').check();
    cy.get('[data-testid="enable-alerts-checkbox"]').check();
    cy.get('[data-testid="enable-audit-checkbox"]').check();
    
    cy.get('[data-testid="save-service-button"]').click();
    
    cy.wait('@createService').then((interception) => {
      const serviceId = interception.response.body.id;
      
      // STEP 2: Create Sub-Services
      cy.get('[data-testid="add-subservice-button"]').click();
      
      // Inbound sub-service
      cy.get('[data-testid="subservice-name-input"]').type('Financial Data Inbound');
      cy.get('[data-testid="subservice-description-input"]').type('Receive financial data files');
      cy.get('[data-testid="direction-select"]').click();
      cy.get('[data-value="INBOUND"]').click();
      
      // Configure file types
      cy.get('[data-testid="add-file-type-button"]').click();
      cy.get('[data-testid="file-type-select-0"]').click();
      cy.get('[data-value="JSON"]').click();
      
      cy.get('[data-testid="add-file-type-button"]').click();
      cy.get('[data-testid="file-type-select-1"]').click();
      cy.get('[data-value="XML"]').click();
      
      cy.get('[data-testid="save-subservice-button"]').click();
      
      cy.wait('@createSubService');
      
      // STEP 3: Configure Cut-off Times
      cy.get('[data-testid="configure-cutoff-button"]').click();
      
      cy.get('[data-testid="cutoff-time-input"]').type('17:00');
      cy.get('[data-testid="cutoff-timezone-select"]').click();
      cy.get('[data-value="America/New_York"]').click();
      cy.get('[data-testid="cutoff-type-select"]').click();
      cy.get('[data-value="END_OF_DAY"]').click();
      
      cy.get('[data-testid="save-cutoff-button"]').click();
      
      // STEP 4: Configure Holidays
      cy.get('[data-testid="configure-holidays-button"]').click();
      
      cy.get('[data-testid="add-holiday-button"]').click();
      cy.get('[data-testid="holiday-name-input"]').type('New Year');
      cy.get('[data-testid="holiday-date-input"]').type('2024-01-01');
      cy.get('[data-testid="holiday-recurring-checkbox"]').check();
      
      cy.get('[data-testid="save-holiday-button"]').click();
      
      // STEP 5: Verify Complete Service Setup
      cy.get('[data-testid="service-setup-complete"]').should('be.visible');
      cy.get('[data-testid="service-status"]').should('contain', 'ACTIVE');
      cy.get('[data-testid="subservice-count"]').should('contain', '1');
      cy.get('[data-testid="file-types-count"]').should('contain', '2');
    });
  });

  it('should handle service configuration wizard', () => {
    // Start guided service setup
    cy.get('[data-testid="guided-service-setup"]').click();
    
    // WIZARD STEP 1: Basic Information
    cy.get('[data-testid="wizard-step-1"]').should('be.visible');
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="service-name-input"]').type('Wizard Created Service');
    cy.get('[data-testid="service-type-select"]').click();
    cy.get('[data-value="FINANCIAL"]').click();
    
    cy.get('[data-testid="next-button"]').click();
    
    // WIZARD STEP 2: File Types and Directions
    cy.get('[data-testid="wizard-step-2"]').should('be.visible');
    
    // Select multiple file types
    cy.get('[data-testid="file-type-json"]').check();
    cy.get('[data-testid="file-type-xml"]').check();
    cy.get('[data-testid="file-type-cobol"]').check();
    
    // Select directions
    cy.get('[data-testid="direction-inbound"]').check();
    cy.get('[data-testid="direction-outbound"]').check();
    
    cy.get('[data-testid="next-button"]').click();
    
    // WIZARD STEP 3: Processing Schedule
    cy.get('[data-testid="wizard-step-3"]').should('be.visible');
    
    // Configure cut-off times
    cy.get('[data-testid="cutoff-time-input"]').type('18:00');
    cy.get('[data-testid="grace-period-input"]').type('30');
    
    // Configure processing schedule
    cy.get('[data-testid="processing-schedule-select"]').click();
    cy.get('[data-value="HOURLY"]').click();
    
    cy.get('[data-testid="next-button"]').click();
    
    // WIZARD STEP 4: Monitoring and Alerts
    cy.get('[data-testid="wizard-step-4"]').should('be.visible');
    
    cy.get('[data-testid="enable-monitoring"]').check();
    cy.get('[data-testid="enable-alerts"]').check();
    cy.get('[data-testid="alert-email-input"]').type('alerts@example.com');
    
    // Configure alert thresholds
    cy.get('[data-testid="error-rate-threshold"]').type('5');
    cy.get('[data-testid="processing-delay-threshold"]').type('60');
    
    cy.get('[data-testid="next-button"]').click();
    
    // WIZARD STEP 5: Review and Confirm
    cy.get('[data-testid="wizard-step-5"]').should('be.visible');
    
    // Review configuration
    cy.get('[data-testid="review-service-name"]').should('contain', 'Wizard Created Service');
    cy.get('[data-testid="review-file-types"]').should('contain', 'JSON, XML, COBOL');
    cy.get('[data-testid="review-directions"]').should('contain', 'INBOUND, OUTBOUND');
    cy.get('[data-testid="review-cutoff-time"]').should('contain', '18:00');
    
    // Complete wizard
    cy.get('[data-testid="complete-wizard-button"]').click();
    
    cy.wait('@createService');
    
    // Verify service was created with all configurations
    cy.get('[data-testid="wizard-success-message"]').should('be.visible');
    cy.get('[data-testid="service-created-id"]').should('be.visible');
    
    // Navigate to service details
    cy.get('[data-testid="view-created-service"]').click();
    
    // Verify all wizard configurations were applied
    cy.get('[data-testid="service-file-types"]').should('contain', 'JSON');
    cy.get('[data-testid="service-file-types"]').should('contain', 'XML');
    cy.get('[data-testid="service-file-types"]').should('contain', 'COBOL');
    cy.get('[data-testid="service-monitoring"]').should('contain', 'ENABLED');
    cy.get('[data-testid="service-alerts"]').should('contain', 'ENABLED');
  });

  it('should test service validation and error handling', () => {
    // Test service creation validation
    cy.get('[data-testid="create-service-button"]').click();
    
    // Try to save without required fields
    cy.get('[data-testid="save-service-button"]').click();
    
    // Check validation errors
    cy.get('[data-testid="tenant-error"]').should('contain', 'required');
    cy.get('[data-testid="service-name-error"]').should('contain', 'required');
    
    // Fill invalid data
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="service-name-input"]').type('A'); // Too short
    cy.get('[data-testid="service-code-input"]').type('INVALID CODE!@#'); // Invalid characters
    
    cy.get('[data-testid="save-service-button"]').click();
    
    // Check specific validation errors
    cy.get('[data-testid="service-name-error"]').should('contain', 'minimum length');
    cy.get('[data-testid="service-code-error"]').should('contain', 'invalid characters');
    
    // Fix validation errors
    cy.get('[data-testid="service-name-input"]').clear().type('Valid Service Name');
    cy.get('[data-testid="service-code-input"]').clear().type('VALID001');
    cy.get('[data-testid="service-description-input"]').type('Valid service description');
    
    // Form should be valid now
    cy.get('[data-testid="save-service-button"]').should('not.be.disabled');
  });

  it('should test sub-service dependency management', () => {
    // Navigate to sub-service dependencies
    cy.visit('/services/1/subservices');
    
    // Create parent sub-service
    cy.get('[data-testid="create-subservice-button"]').click();
    
    cy.get('[data-testid="subservice-name-input"]').type('Parent Processing');
    cy.get('[data-testid="subservice-type-select"]').click();
    cy.get('[data-value="DATA_VALIDATION"]').click();
    cy.get('[data-testid="processing-order-input"]').type('1');
    
    cy.get('[data-testid="save-subservice-button"]').click();
    
    cy.wait('@createSubService').then((interception) => {
      const parentSubServiceId = interception.response.body.id;
      
      // Create dependent sub-service
      cy.get('[data-testid="create-subservice-button"]').click();
      
      cy.get('[data-testid="subservice-name-input"]').type('Child Processing');
      cy.get('[data-testid="subservice-type-select"]').click();
      cy.get('[data-value="DATA_TRANSFORMATION"]').click();
      cy.get('[data-testid="processing-order-input"]').type('2');
      
      // Set dependency
      cy.get('[data-testid="depends-on-select"]').click();
      cy.get(`[data-value="${parentSubServiceId}"]`).click();
      
      cy.get('[data-testid="save-subservice-button"]').click();
      
      cy.wait('@createSubService');
      
      // Verify dependency relationship
      cy.get('[data-testid="subservice-dependencies"]').should('be.visible');
      cy.get('[data-testid="dependency-graph"]').should('contain', 'Parent Processing');
      cy.get('[data-testid="dependency-arrow"]').should('be.visible');
      cy.get('[data-testid="dependent-subservice"]').should('contain', 'Child Processing');
    });
  });

});

describe('Schema Management E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/schemas*', { fixture: 'schemas.json' }).as('getSchemas');
    cy.intercept('POST', '/api/schemas', { fixture: 'schema-created.json' }).as('createSchema');
    cy.intercept('POST', '/api/schemas/reuse', { fixture: 'schema-reused.json' }).as('reuseSchema');
    cy.intercept('POST', '/api/schemas/*/validate', { fixture: 'schema-validated.json' }).as('validateSchema');
    
    cy.visit('/schemas');
  });

  it('should complete schema creation and reuse workflow', () => {
    // STEP 1: Create JSON Schema
    cy.get('[data-testid="create-schema-button"]').click();
    
    cy.get('[data-testid="schema-name-input"]').type('Financial Transaction Schema');
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="service-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="direction-select"]').click();
    cy.get('[data-value="INBOUND"]').click();
    cy.get('[data-testid="file-type-select"]').click();
    cy.get('[data-value="JSON"]').click();
    
    // Add schema content using editor
    cy.get('[data-testid="schema-editor"]').type(`{
      "type": "object",
      "properties": {
        "transactionId": {"type": "string"},
        "amount": {"type": "number", "minimum": 0},
        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
        "timestamp": {"type": "string", "format": "date-time"}
      },
      "required": ["transactionId", "amount", "currency", "timestamp"]
    }`);
    
    // Validate schema
    cy.get('[data-testid="validate-schema-button"]').click();
    cy.wait('@validateSchema');
    cy.get('[data-testid="schema-validation-result"]').should('contain', 'Valid');
    
    // Save schema
    cy.get('[data-testid="save-schema-button"]').click();
    cy.wait('@createSchema');
    
    // STEP 2: Reuse Schema for Different Direction
    cy.get('[data-testid="schema-list"]').should('contain', 'Financial Transaction Schema');
    
    cy.get('[data-testid="schema-item"]').first().within(() => {
      cy.get('[data-testid="reuse-schema-button"]').click();
    });
    
    // Configure reuse parameters
    cy.get('[data-testid="target-direction-select"]').click();
    cy.get('[data-value="OUTBOUND"]').click();
    cy.get('[data-testid="target-subservice-select"]').click();
    cy.get('[data-value="2"]').click();
    
    cy.get('[data-testid="confirm-reuse-button"]').click();
    cy.wait('@reuseSchema');
    
    // STEP 3: Verify Schema Reuse
    cy.get('[data-testid="reuse-success-message"]').should('be.visible');
    cy.get('[data-testid="reused-schema-id"]').should('be.visible');
    
    // Check that both schemas exist
    cy.get('[data-testid="schema-list"]').should('contain', 'Financial Transaction Schema');
    cy.get('[data-testid="schema-reuse-indicator"]').should('be.visible');
  });

  it('should test COBOL copybook schema workflow', () => {
    // Create COBOL schema
    cy.get('[data-testid="create-schema-button"]').click();
    
    cy.get('[data-testid="schema-name-input"]').type('COBOL Customer Record');
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="service-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="file-type-select"]').click();
    cy.get('[data-value="COBOL"]').click();
    
    // Add COBOL copybook
    cy.get('[data-testid="cobol-editor"]').type(`       01  CUSTOMER-RECORD.
           05  CUSTOMER-ID         PIC X(10).
           05  CUSTOMER-NAME       PIC X(30).
           05  CUSTOMER-ADDRESS.
               10  ADDRESS-LINE-1  PIC X(30).
               10  ADDRESS-LINE-2  PIC X(30).
               10  CITY            PIC X(20).
               10  STATE           PIC X(2).
               10  ZIP-CODE        PIC X(10).
           05  CUSTOMER-BALANCE    PIC 9(8)V99.`);
    
    // Parse copybook
    cy.get('[data-testid="parse-copybook-button"]').click();
    cy.get('[data-testid="copybook-parse-result"]').should('contain', 'Successfully parsed');
    
    // Verify parsed structure
    cy.get('[data-testid="parsed-fields"]').should('contain', 'CUSTOMER-ID');
    cy.get('[data-testid="parsed-fields"]').should('contain', 'CUSTOMER-NAME');
    cy.get('[data-testid="parsed-fields"]').should('contain', 'CUSTOMER-BALANCE');
    
    cy.get('[data-testid="save-schema-button"]').click();
    cy.wait('@createSchema');
    
    // Test COBOL file validation
    cy.get('[data-testid="test-cobol-validation"]').click();
    
    // Upload test COBOL file
    const cobolData = 'CUST001   John Smith                    123 Main Street              Apt 4B                       New York            NY12345     00001500.99';
    
    cy.get('[data-testid="cobol-file-upload"]').selectFile({
      contents: Cypress.Buffer.from(cobolData),
      fileName: 'customer.dat',
      mimeType: 'application/octet-stream'
    });
    
    cy.get('[data-testid="validate-cobol-file"]').click();
    
    // Verify validation results
    cy.get('[data-testid="cobol-validation-result"]').should('contain', 'Valid');
    cy.get('[data-testid="parsed-customer-id"]').should('contain', 'CUST001');
    cy.get('[data-testid="parsed-customer-name"]').should('contain', 'John Smith');
  });

});

describe('File Processing E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('POST', '/api/files/upload', { fixture: 'file-uploaded.json' }).as('uploadFile');
    cy.intercept('GET', '/api/files/*/status', { fixture: 'file-processing-status.json' }).as('getFileStatus');
    cy.intercept('POST', '/api/files/*/validate', { fixture: 'file-validated.json' }).as('validateFile');
    cy.intercept('GET', '/api/files/processing-queue', { fixture: 'processing-queue.json' }).as('getQueue');
    
    cy.visit('/files');
  });

  it('should complete end-to-end file processing workflow', () => {
    // STEP 1: Select tenant and service
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="service-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="direction-select"]').click();
    cy.get('[data-value="INBOUND"]').click();
    
    // STEP 2: Upload multiple files
    const files = [
      { name: 'transaction1.json', content: '{"id": "TXN001", "amount": 1000.50}' },
      { name: 'transaction2.json', content: '{"id": "TXN002", "amount": 2500.75}' },
      { name: 'transaction3.json', content: '{"id": "TXN003", "amount": 750.00}' }
    ];
    
    files.forEach((file, index) => {
      cy.get('[data-testid="file-upload-dropzone"]').selectFile({
        contents: Cypress.Buffer.from(file.content),
        fileName: file.name,
        mimeType: 'application/json'
      }, { action: 'drag-drop' });
      
      cy.get(`[data-testid="uploaded-file-${index}"]`).should('contain', file.name);
    });
    
    // STEP 3: Configure processing options
    cy.get('[data-testid="enable-validation"]').check();
    cy.get('[data-testid="enable-transformation"]').check();
    cy.get('[data-testid="enable-archiving"]').check();
    
    // Set processing priority
    cy.get('[data-testid="processing-priority-select"]').click();
    cy.get('[data-value="HIGH"]').click();
    
    // STEP 4: Start processing
    cy.get('[data-testid="start-processing-button"]').click();
    
    cy.wait('@uploadFile');
    
    // STEP 5: Monitor processing progress
    cy.get('[data-testid="processing-progress"]').should('be.visible');
    cy.get('[data-testid="files-in-queue"]').should('contain', '3');
    
    // Wait for processing to complete
    cy.get('[data-testid="processing-status"]').should('contain', 'Processing', { timeout: 5000 });
    cy.get('[data-testid="processing-status"]').should('contain', 'Completed', { timeout: 30000 });
    
    // STEP 6: Verify results
    cy.get('[data-testid="files-processed"]').should('contain', '3');
    cy.get('[data-testid="files-successful"]').should('contain', '3');
    cy.get('[data-testid="files-failed"]').should('contain', '0');
    cy.get('[data-testid="success-rate"]').should('contain', '100%');
    
    // STEP 7: Check individual file results
    cy.get('[data-testid="view-results-button"]').click();
    
    files.forEach((file, index) => {
      cy.get(`[data-testid="file-result-${index}"]`).within(() => {
        cy.get('[data-testid="file-name"]').should('contain', file.name);
        cy.get('[data-testid="file-status"]').should('contain', 'PROCESSED');
        cy.get('[data-testid="validation-status"]').should('contain', 'VALID');
      });
    });
  });

  it('should test file naming convention validation', () => {
    // Configure naming conventions
    cy.get('[data-testid="configure-naming-button"]').click();
    
    // Set SOT (Start of Day) pattern
    cy.get('[data-testid="sot-pattern-input"]').type('SOT_{tenant}_{service}_{date}.json');
    cy.get('[data-testid="sot-example"]').should('contain', 'SOT_TENANT001_SERVICE001_20231201.json');
    
    // Set EOT (End of Day) pattern
    cy.get('[data-testid="eot-pattern-input"]').type('EOT_{tenant}_{service}_{date}.json');
    cy.get('[data-testid="eot-example"]').should('contain', 'EOT_TENANT001_SERVICE001_20231201.json');
    
    // Set data file pattern
    cy.get('[data-testid="data-pattern-input"]').type('DATA_{tenant}_{service}_{sequence}_{date}.json');
    cy.get('[data-testid="data-example"]').should('contain', 'DATA_TENANT001_SERVICE001_001_20231201.json');
    
    cy.get('[data-testid="save-naming-config"]').click();
    
    // Test valid file names
    const validFiles = [
      'SOT_TENANT001_SERVICE001_20231201.json',
      'DATA_TENANT001_SERVICE001_001_20231201.json',
      'DATA_TENANT001_SERVICE001_002_20231201.json',
      'EOT_TENANT001_SERVICE001_20231201.json'
    ];
    
    validFiles.forEach((fileName, index) => {
      cy.get('[data-testid="file-upload-dropzone"]').selectFile({
        contents: Cypress.Buffer.from('{"test": "data"}'),
        fileName: fileName,
        mimeType: 'application/json'
      }, { action: 'drag-drop' });
      
      cy.get(`[data-testid="file-naming-status-${index}"]`).should('contain', 'Valid');
    });
    
    // Test invalid file name
    cy.get('[data-testid="file-upload-dropzone"]').selectFile({
      contents: Cypress.Buffer.from('{"test": "data"}'),
      fileName: 'INVALID_FILE_NAME.json',
      mimeType: 'application/json'
    }, { action: 'drag-drop' });
    
    cy.get('[data-testid="file-naming-error"]').should('be.visible');
    cy.get('[data-testid="file-naming-error"]').should('contain', 'does not match naming convention');
  });

});

describe('Dashboard and Analytics E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/dashboard/metrics', { fixture: 'dashboard-metrics.json' }).as('getDashboardMetrics');
    cy.intercept('GET', '/api/analytics/reports*', { fixture: 'analytics-reports.json' }).as('getAnalytics');
    cy.intercept('GET', '/api/monitoring/health', { fixture: 'system-health.json' }).as('getSystemHealth');
    
    cy.visit('/dashboard');
  });

  it('should display comprehensive system dashboard', () => {
    // Wait for dashboard to load
    cy.wait('@getDashboardMetrics');
    cy.wait('@getSystemHealth');
    
    // Verify main metrics cards
    cy.get('[data-testid="total-tenants-card"]').should('be.visible');
    cy.get('[data-testid="active-services-card"]').should('be.visible');
    cy.get('[data-testid="files-processed-today-card"]').should('be.visible');
    cy.get('[data-testid="system-health-card"]').should('be.visible');
    
    // Verify charts and graphs
    cy.get('[data-testid="processing-trend-chart"]').should('be.visible');
    cy.get('[data-testid="success-rate-chart"]').should('be.visible');
    cy.get('[data-testid="tenant-activity-chart"]').should('be.visible');
    
    // Test real-time updates
    cy.get('[data-testid="real-time-toggle"]').click();
    cy.get('[data-testid="real-time-indicator"]').should('be.visible');
    
    // Simulate real-time data update
    cy.get('[data-testid="simulate-update"]').click();
    cy.get('[data-testid="files-processed-today"]').should('not.contain', '0');
  });

  it('should navigate through analytics workflows', () => {
    // Navigate to analytics
    cy.get('[data-testid="view-analytics-button"]').click();
    cy.url().should('include', '/analytics');
    
    // Test different report types
    cy.get('[data-testid="report-type-select"]').click();
    cy.get('[data-value="TENANT_PERFORMANCE"]').click();
    
    cy.get('[data-testid="date-range-picker"]').click();
    cy.get('[data-testid="last-30-days"]').click();
    
    cy.get('[data-testid="generate-report-button"]').click();
    cy.wait('@getAnalytics');
    
    // Verify report generation
    cy.get('[data-testid="report-results"]').should('be.visible');
    cy.get('[data-testid="tenant-performance-table"]').should('be.visible');
    
    // Test report export
    cy.get('[data-testid="export-report-button"]').click();
    cy.get('[data-testid="export-format-select"]').click();
    cy.get('[data-value="PDF"]').click();
    cy.get('[data-testid="confirm-export-button"]').click();
    
    cy.get('[data-testid="export-success-message"]').should('be.visible');
  });

});