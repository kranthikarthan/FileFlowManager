/**
 * Holiday Management E2E Tests
 * Tests complete holiday configuration and impact on processing workflows
 */

describe('Holiday Management E2E Tests', () => {
  
  beforeEach(() => {
    cy.intercept('GET', '/api/holidays*', { fixture: 'holidays.json' }).as('getHolidays');
    cy.intercept('POST', '/api/holidays', { fixture: 'holiday-created.json' }).as('createHoliday');
    cy.intercept('PUT', '/api/holidays/*', { fixture: 'holiday-updated.json' }).as('updateHoliday');
    cy.intercept('DELETE', '/api/holidays/*', { statusCode: 204 }).as('deleteHoliday');
    cy.intercept('GET', '/api/v2/tenants*', { fixture: 'tenants.json' }).as('getTenants');
    
    cy.visit('/holidays');
  });

  it('should complete holiday configuration workflow', () => {
    // STEP 1: Create Annual Holiday
    cy.get('[data-testid="create-holiday-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="holiday-name-input"]').type('New Year Day');
    cy.get('[data-testid="holiday-date-input"]').type('2024-01-01');
    cy.get('[data-testid="holiday-type-select"]').click();
    cy.get('[data-value="NATIONAL"]').click();
    
    // Configure recurrence
    cy.get('[data-testid="recurring-checkbox"]').check();
    cy.get('[data-testid="recurrence-pattern-select"]').click();
    cy.get('[data-value="ANNUAL"]').click();
    
    // Configure processing impact
    cy.get('[data-testid="affects-processing-checkbox"]').check();
    cy.get('[data-testid="processing-action-select"]').click();
    cy.get('[data-value="SKIP_PROCESSING"]').click();
    
    cy.get('[data-testid="save-holiday-button"]').click();
    cy.wait('@createHoliday');
    
    // STEP 2: Create Floating Holiday
    cy.get('[data-testid="create-holiday-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="holiday-name-input"]').type('Thanksgiving');
    cy.get('[data-testid="holiday-type-select"]').click();
    cy.get('[data-value="FLOATING"]').click();
    
    // Configure floating date rule
    cy.get('[data-testid="floating-rule-select"]').click();
    cy.get('[data-value="FOURTH_THURSDAY_NOVEMBER"]').click();
    
    cy.get('[data-testid="affects-processing-checkbox"]').check();
    cy.get('[data-testid="processing-action-select"]').click();
    cy.get('[data-value="DELAY_TO_NEXT_BUSINESS_DAY"]').click();
    
    cy.get('[data-testid="save-holiday-button"]').click();
    cy.wait('@createHoliday');
    
    // STEP 3: Test Holiday Calendar View
    cy.get('[data-testid="calendar-view-button"]').click();
    
    cy.get('[data-testid="holiday-calendar"]').should('be.visible');
    cy.get('[data-testid="calendar-holiday-new-year"]').should('be.visible');
    cy.get('[data-testid="calendar-holiday-thanksgiving"]').should('be.visible');
    
    // STEP 4: Test Holiday Impact on Cut-off Times
    cy.get('[data-testid="test-cutoff-impact"]').click();
    
    cy.get('[data-testid="test-date-input"]').type('2024-01-01'); // New Year
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="check-impact-button"]').click();
    
    // Should show processing will be skipped
    cy.get('[data-testid="impact-result"]').should('contain', 'Processing will be skipped');
    cy.get('[data-testid="next-processing-date"]').should('contain', '2024-01-02');
  });

  it('should test holiday processing impact workflow', () => {
    // Navigate to processing schedule
    cy.visit('/processing-schedule');
    
    // View schedule for holiday period
    cy.get('[data-testid="date-range-picker"]').click();
    cy.get('[data-testid="select-holiday-period"]').click(); // Dec 25 - Jan 2
    
    cy.get('[data-testid="view-schedule-button"]').click();
    
    // Should show holiday adjustments
    cy.get('[data-testid="holiday-adjustments"]').should('be.visible');
    cy.get('[data-testid="skipped-days"]').should('contain', 'December 25');
    cy.get('[data-testid="skipped-days"]').should('contain', 'January 1');
    
    // Test manual override
    cy.get('[data-testid="override-holiday-button"]').click();
    cy.get('[data-testid="override-date-select"]').click();
    cy.get('[data-value="2024-01-01"]').click();
    
    cy.get('[data-testid="override-reason-input"]').type('Critical business requirement');
    cy.get('[data-testid="approval-required-checkbox"]').check();
    
    cy.get('[data-testid="submit-override-button"]').click();
    
    // Should show approval workflow
    cy.get('[data-testid="approval-pending"]').should('be.visible');
    cy.get('[data-testid="approval-workflow-id"]').should('be.visible');
  });

});

describe('Cut-off Extension Management E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/cutoff-extensions*', { fixture: 'cutoff-extensions.json' }).as('getExtensions');
    cy.intercept('POST', '/api/cutoff-extensions', { fixture: 'extension-created.json' }).as('createExtension');
    cy.intercept('PUT', '/api/cutoff-extensions/*/approve', { fixture: 'extension-approved.json' }).as('approveExtension');
    cy.intercept('PUT', '/api/cutoff-extensions/*/reject', { fixture: 'extension-rejected.json' }).as('rejectExtension');
    
    cy.visit('/cutoff-extensions');
  });

  it('should complete cut-off extension request and approval workflow', () => {
    // STEP 1: Request Extension
    cy.get('[data-testid="request-extension-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="extension-date-input"]').type('2023-12-01');
    cy.get('[data-testid="extension-duration-input"]').type('120'); // 2 hours
    
    cy.get('[data-testid="extension-reason-textarea"]').type('Critical end-of-month processing requires additional time due to increased data volume');
    
    cy.get('[data-testid="priority-select"]').click();
    cy.get('[data-value="HIGH"]').click();
    
    cy.get('[data-testid="business-justification-textarea"]').type('Month-end financial reconciliation cannot be delayed');
    
    cy.get('[data-testid="submit-extension-request"]').click();
    cy.wait('@createExtension');
    
    // STEP 2: Verify Request Created
    cy.get('[data-testid="extension-request-success"]').should('be.visible');
    cy.get('[data-testid="extension-id"]').should('be.visible');
    
    // STEP 3: Approval Workflow
    cy.get('[data-testid="extension-list"]').should('contain', 'Critical end-of-month processing');
    
    cy.get('[data-testid="extension-item"]').first().within(() => {
      cy.get('[data-testid="extension-status"]').should('contain', 'PENDING');
      cy.get('[data-testid="review-extension-button"]').click();
    });
    
    // Manager review
    cy.get('[data-testid="review-modal"]').should('be.visible');
    cy.get('[data-testid="extension-details"]').should('contain', '120 minutes');
    cy.get('[data-testid="business-impact-assessment"]').should('be.visible');
    
    cy.get('[data-testid="manager-comments-textarea"]').type('Approved for critical month-end processing');
    cy.get('[data-testid="approve-extension-button"]').click();
    
    cy.wait('@approveExtension');
    
    // STEP 4: Verify Approval Impact
    cy.get('[data-testid="approval-success"]').should('be.visible');
    cy.get('[data-testid="new-cutoff-time"]').should('contain', '01:59'); // Original 23:59 + 120 min
    
    // STEP 5: Test Extension Monitoring
    cy.get('[data-testid="monitor-extension-button"]').click();
    
    cy.get('[data-testid="extension-monitoring"]').should('be.visible');
    cy.get('[data-testid="time-remaining"]').should('be.visible');
    cy.get('[data-testid="processing-status"]').should('be.visible');
    cy.get('[data-testid="auto-alerts"]').should('contain', 'ENABLED');
  });

  it('should test extension rejection and escalation workflow', () => {
    // Create extension request
    cy.get('[data-testid="request-extension-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    cy.get('[data-testid="subservice-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="extension-reason-textarea"]').type('Insufficient justification for testing');
    cy.get('[data-testid="priority-select"]').click();
    cy.get('[data-value="LOW"]').click();
    
    cy.get('[data-testid="submit-extension-request"]').click();
    cy.wait('@createExtension');
    
    // Reject extension
    cy.get('[data-testid="extension-item"]').first().within(() => {
      cy.get('[data-testid="review-extension-button"]').click();
    });
    
    cy.get('[data-testid="rejection-reason-textarea"]').type('Insufficient business justification provided');
    cy.get('[data-testid="reject-extension-button"]').click();
    
    cy.wait('@rejectExtension');
    
    // Test escalation
    cy.get('[data-testid="escalate-rejection-button"]').click();
    
    cy.get('[data-testid="escalation-reason-textarea"]').type('Business critical requirement not properly communicated');
    cy.get('[data-testid="escalate-to-select"]').click();
    cy.get('[data-value="SENIOR_MANAGER"]').click();
    
    cy.get('[data-testid="submit-escalation"]').click();
    
    // Verify escalation workflow
    cy.get('[data-testid="escalation-submitted"]').should('be.visible');
    cy.get('[data-testid="escalation-id"]').should('be.visible');
    cy.get('[data-testid="escalation-status"]').should('contain', 'ESCALATED');
  });

});

describe('SSO Configuration E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/sso-configurations*', { fixture: 'sso-configs.json' }).as('getSsoConfigs');
    cy.intercept('POST', '/api/sso-configurations', { fixture: 'sso-config-created.json' }).as('createSsoConfig');
    cy.intercept('POST', '/api/sso-configurations/*/test', { fixture: 'sso-test-result.json' }).as('testSsoConfig');
    
    cy.visit('/sso-configuration');
  });

  it('should complete SSO configuration and testing workflow', () => {
    // STEP 1: Configure SAML SSO
    cy.get('[data-testid="add-sso-provider-button"]').click();
    
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    cy.get('[data-testid="provider-type-select"]').click();
    cy.get('[data-value="SAML"]').click();
    
    cy.get('[data-testid="provider-name-input"]').type('Corporate SAML');
    cy.get('[data-testid="entity-id-input"]').type('https://corp.example.com/saml');
    cy.get('[data-testid="sso-url-input"]').type('https://corp.example.com/saml/sso');
    cy.get('[data-testid="logout-url-input"]').type('https://corp.example.com/saml/logout');
    
    // Upload certificate
    cy.get('[data-testid="certificate-upload"]').selectFile({
      contents: Cypress.Buffer.from('-----BEGIN CERTIFICATE-----\nMIIC...test...certificate\n-----END CERTIFICATE-----'),
      fileName: 'saml-cert.pem',
      mimeType: 'application/x-pem-file'
    });
    
    // Configure attribute mapping
    cy.get('[data-testid="email-attribute-input"]').type('http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress');
    cy.get('[data-testid="name-attribute-input"]').type('http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name');
    cy.get('[data-testid="role-attribute-input"]').type('http://schemas.microsoft.com/ws/2008/06/identity/claims/role');
    
    cy.get('[data-testid="save-sso-config-button"]').click();
    cy.wait('@createSsoConfig');
    
    // STEP 2: Test SSO Configuration
    cy.get('[data-testid="sso-config-item"]').first().within(() => {
      cy.get('[data-testid="test-sso-button"]').click();
    });
    
    cy.get('[data-testid="test-username-input"]').type('test.user@corp.example.com');
    cy.get('[data-testid="run-sso-test-button"]').click();
    
    cy.wait('@testSsoConfig');
    
    // STEP 3: Verify Test Results
    cy.get('[data-testid="sso-test-results"]').should('be.visible');
    cy.get('[data-testid="authentication-test"]').should('contain', 'PASSED');
    cy.get('[data-testid="attribute-mapping-test"]').should('contain', 'PASSED');
    cy.get('[data-testid="role-assignment-test"]').should('contain', 'PASSED');
    
    // STEP 4: Enable SSO for Tenant
    cy.get('[data-testid="enable-sso-button"]').click();
    cy.get('[data-testid="confirm-enable-sso"]').click();
    
    cy.get('[data-testid="sso-enabled-success"]').should('be.visible');
    cy.get('[data-testid="sso-status"]').should('contain', 'ACTIVE');
  });

  it('should test SSO login workflow', () => {
    // Navigate to login page
    cy.visit('/login');
    
    // Should show SSO option
    cy.get('[data-testid="sso-login-option"]').should('be.visible');
    cy.get('[data-testid="sso-provider-corporate-saml"]').should('be.visible');
    
    // Click SSO login
    cy.get('[data-testid="sso-provider-corporate-saml"]').click();
    
    // Should redirect to SSO provider (simulate)
    cy.url().should('include', 'saml/sso');
    
    // Simulate SSO authentication success
    cy.window().then((win) => {
      win.postMessage({
        type: 'SSO_AUTH_SUCCESS',
        user: {
          email: 'test.user@corp.example.com',
          name: 'Test User',
          roles: ['USER', 'MANAGER']
        },
        token: 'sso-jwt-token-123'
      }, '*');
    });
    
    // Should redirect back to application
    cy.url().should('not.include', 'login');
    cy.get('[data-testid="user-menu"]').should('be.visible');
    cy.get('[data-testid="user-name"]').should('contain', 'Test User');
    cy.get('[data-testid="sso-indicator"]').should('be.visible');
  });

});

describe('Alert Management E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/alerts*', { fixture: 'alerts.json' }).as('getAlerts');
    cy.intercept('POST', '/api/alerts', { fixture: 'alert-created.json' }).as('createAlert');
    cy.intercept('PUT', '/api/alerts/*/acknowledge', { fixture: 'alert-acknowledged.json' }).as('acknowledgeAlert');
    cy.intercept('POST', '/api/alerts/rules', { fixture: 'alert-rule-created.json' }).as('createAlertRule');
    
    cy.visit('/alerts');
  });

  it('should complete alert configuration and management workflow', () => {
    // STEP 1: Create Alert Rule
    cy.get('[data-testid="create-alert-rule-button"]').click();
    
    cy.get('[data-testid="rule-name-input"]').type('High Error Rate Alert');
    cy.get('[data-testid="tenant-select"]').click();
    cy.get('[data-value="1"]').click();
    
    // Configure alert conditions
    cy.get('[data-testid="metric-select"]').click();
    cy.get('[data-value="ERROR_RATE"]').click();
    
    cy.get('[data-testid="condition-select"]').click();
    cy.get('[data-value="GREATER_THAN"]').click();
    
    cy.get('[data-testid="threshold-input"]').type('5');
    cy.get('[data-testid="time-window-select"]').click();
    cy.get('[data-value="15_MINUTES"]').click();
    
    // Configure notifications
    cy.get('[data-testid="notification-email-input"]').type('alerts@example.com');
    cy.get('[data-testid="notification-slack-checkbox"]').check();
    cy.get('[data-testid="notification-dashboard-checkbox"]').check();
    
    // Set alert level
    cy.get('[data-testid="alert-level-select"]').click();
    cy.get('[data-value="WARNING"]').click();
    
    cy.get('[data-testid="save-alert-rule-button"]').click();
    cy.wait('@createAlertRule');
    
    // STEP 2: Trigger Test Alert
    cy.get('[data-testid="trigger-test-alert-button"]').click();
    
    cy.get('[data-testid="test-alert-type-select"]').click();
    cy.get('[data-value="HIGH_ERROR_RATE"]').click();
    
    cy.get('[data-testid="test-alert-message-input"]').type('E2E test alert for high error rate');
    
    cy.get('[data-testid="trigger-alert-button"]').click();
    cy.wait('@createAlert');
    
    // STEP 3: Verify Alert Appears
    cy.get('[data-testid="alert-list"]').should('contain', 'E2E test alert for high error rate');
    cy.get('[data-testid="alert-level-warning"]').should('be.visible');
    cy.get('[data-testid="alert-timestamp"]').should('be.visible');
    
    // STEP 4: Acknowledge Alert
    cy.get('[data-testid="alert-item"]').first().within(() => {
      cy.get('[data-testid="acknowledge-alert-button"]').click();
    });
    
    cy.get('[data-testid="acknowledgment-comment-input"]').type('Investigating high error rate issue');
    cy.get('[data-testid="estimated-resolution-select"]').click();
    cy.get('[data-value="30_MINUTES"]').click();
    
    cy.get('[data-testid="confirm-acknowledgment"]').click();
    cy.wait('@acknowledgeAlert');
    
    // STEP 5: Verify Alert Status
    cy.get('[data-testid="alert-status"]').should('contain', 'ACKNOWLEDGED');
    cy.get('[data-testid="acknowledged-by"]').should('be.visible');
    cy.get('[data-testid="estimated-resolution"]').should('contain', '30 minutes');
  });

  it('should test alert escalation workflow', () => {
    // Create critical alert that requires escalation
    cy.get('[data-testid="trigger-test-alert-button"]').click();
    
    cy.get('[data-testid="test-alert-type-select"]').click();
    cy.get('[data-value="SYSTEM_DOWN"]').click();
    
    cy.get('[data-testid="alert-level-select"]').click();
    cy.get('[data-value="CRITICAL"]').click();
    
    cy.get('[data-testid="trigger-alert-button"]').click();
    cy.wait('@createAlert');
    
    // Wait for auto-escalation timer
    cy.get('[data-testid="escalation-timer"]').should('be.visible');
    cy.get('[data-testid="escalation-countdown"]').should('be.visible');
    
    // Simulate escalation timeout
    cy.get('[data-testid="simulate-escalation-timeout"]').click();
    
    // Verify escalation
    cy.get('[data-testid="alert-escalated"]').should('be.visible');
    cy.get('[data-testid="escalation-level"]').should('contain', 'LEVEL_2');
    cy.get('[data-testid="escalated-to"]').should('contain', 'Senior Manager');
  });

});