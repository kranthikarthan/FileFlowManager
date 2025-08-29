/**
 * Cypress E2E Tests - Tenant Management
 * End-to-end tests for complete tenant management workflows
 */

describe('Tenant Management E2E Tests', () => {
  
  beforeEach(() => {
    // Set up API interceptors
    cy.intercept('GET', '/api/v2/tenants*', { fixture: 'tenants.json' }).as('getTenants');
    cy.intercept('POST', '/api/v2/tenants', { fixture: 'tenant-created.json' }).as('createTenant');
    cy.intercept('PUT', '/api/v2/tenants/*', { fixture: 'tenant-updated.json' }).as('updateTenant');
    cy.intercept('DELETE', '/api/v2/tenants/*', { statusCode: 204 }).as('deleteTenant');
    
    // Visit tenant management page
    cy.visit('/tenants');
  });

  it('should display tenant list with pagination', () => {
    // Wait for tenants to load
    cy.wait('@getTenants');
    
    // Check if tenant list is displayed
    cy.get('[data-testid="tenant-list"]').should('be.visible');
    cy.get('[data-testid="tenant-item"]').should('have.length.greaterThan', 0);
    
    // Check pagination controls
    cy.get('[data-testid="pagination"]').should('be.visible');
    cy.get('[data-testid="page-size-selector"]').should('be.visible');
  });

  it('should create a new tenant successfully', () => {
    // Click create tenant button
    cy.get('[data-testid="create-tenant-button"]').click();
    
    // Fill tenant form
    cy.get('[data-testid="tenant-name-input"]').type('E2E Test Tenant');
    cy.get('[data-testid="tenant-description-input"]').type('Created via E2E test');
    cy.get('[data-testid="tenant-timezone-select"]').click();
    cy.get('[data-value="America/New_York"]').click();
    cy.get('[data-testid="tenant-email-input"]').type('e2e@example.com');
    
    // Submit form
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Wait for API call
    cy.wait('@createTenant');
    
    // Check success notification
    cy.get('[data-testid="success-notification"]').should('contain', 'Tenant created successfully');
    
    // Verify tenant appears in list
    cy.get('[data-testid="tenant-list"]').should('contain', 'E2E Test Tenant');
  });

  it('should edit an existing tenant', () => {
    // Click on first tenant edit button
    cy.get('[data-testid="tenant-item"]').first().within(() => {
      cy.get('[data-testid="edit-tenant-button"]').click();
    });
    
    // Modify tenant details
    cy.get('[data-testid="tenant-name-input"]').clear().type('Updated E2E Tenant');
    cy.get('[data-testid="tenant-description-input"]').clear().type('Updated via E2E test');
    
    // Save changes
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Wait for API call
    cy.wait('@updateTenant');
    
    // Check success notification
    cy.get('[data-testid="success-notification"]').should('contain', 'Tenant updated successfully');
  });

  it('should delete a tenant with confirmation', () => {
    // Click on first tenant delete button
    cy.get('[data-testid="tenant-item"]').first().within(() => {
      cy.get('[data-testid="delete-tenant-button"]').click();
    });
    
    // Confirm deletion in modal
    cy.get('[data-testid="delete-confirmation-modal"]').should('be.visible');
    cy.get('[data-testid="confirm-delete-button"]').click();
    
    // Wait for API call
    cy.wait('@deleteTenant');
    
    // Check success notification
    cy.get('[data-testid="success-notification"]').should('contain', 'Tenant deleted successfully');
  });

  it('should handle API errors gracefully', () => {
    // Mock API error
    cy.intercept('POST', '/api/v2/tenants', { statusCode: 500, body: { error: 'Internal server error' } }).as('createTenantError');
    
    // Try to create tenant
    cy.get('[data-testid="create-tenant-button"]').click();
    cy.get('[data-testid="tenant-name-input"]').type('Error Test Tenant');
    cy.get('[data-testid="tenant-timezone-select"]').click();
    cy.get('[data-value="UTC"]').click();
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Wait for error
    cy.wait('@createTenantError');
    
    // Check error notification
    cy.get('[data-testid="error-notification"]').should('contain', 'Failed to create tenant');
  });

  it('should support search and filtering', () => {
    // Use search functionality
    cy.get('[data-testid="tenant-search-input"]').type('Test');
    
    // Wait for filtered results
    cy.wait('@getTenants');
    
    // Check filtered results
    cy.get('[data-testid="tenant-item"]').each(($el) => {
      cy.wrap($el).should('contain.text', 'Test');
    });
    
    // Clear search
    cy.get('[data-testid="clear-search-button"]').click();
    
    // Check all results returned
    cy.wait('@getTenants');
    cy.get('[data-testid="tenant-item"]').should('have.length.greaterThan', 0);
  });

  it('should handle responsive design across screen sizes', () => {
    // Test desktop view
    cy.viewport(1200, 800);
    cy.get('[data-testid="tenant-list"]').should('be.visible');
    cy.get('[data-testid="sidebar"]').should('be.visible');
    
    // Test tablet view
    cy.viewport(768, 1024);
    cy.get('[data-testid="tenant-list"]').should('be.visible');
    
    // Test mobile view
    cy.viewport(375, 667);
    cy.get('[data-testid="mobile-menu-button"]').should('be.visible');
    cy.get('[data-testid="mobile-menu-button"]').click();
    cy.get('[data-testid="mobile-navigation"]').should('be.visible');
  });

  it('should support dark and light themes', () => {
    // Check initial theme
    cy.get('body').should('have.class', 'light-theme');
    
    // Switch to dark theme
    cy.get('[data-testid="theme-toggle"]').click();
    cy.get('body').should('have.class', 'dark-theme');
    
    // Switch back to light theme
    cy.get('[data-testid="theme-toggle"]').click();
    cy.get('body').should('have.class', 'light-theme');
  });

  it('should validate form inputs properly', () => {
    // Open create tenant form
    cy.get('[data-testid="create-tenant-button"]').click();
    
    // Try to submit empty form
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Check validation errors
    cy.get('[data-testid="tenant-name-error"]').should('contain', 'required');
    cy.get('[data-testid="tenant-timezone-error"]').should('contain', 'required');
    
    // Fill invalid email
    cy.get('[data-testid="tenant-email-input"]').type('invalid-email');
    cy.get('[data-testid="save-tenant-button"]').click();
    cy.get('[data-testid="tenant-email-error"]').should('contain', 'valid email');
    
    // Fill valid data
    cy.get('[data-testid="tenant-name-input"]').type('Valid Tenant');
    cy.get('[data-testid="tenant-description-input"]').type('Valid description');
    cy.get('[data-testid="tenant-timezone-select"]').click();
    cy.get('[data-value="UTC"]').click();
    cy.get('[data-testid="tenant-email-input"]').clear().type('valid@example.com');
    
    // Form should be valid now
    cy.get('[data-testid="save-tenant-button"]').should('not.be.disabled');
  });

  it('should handle offline scenarios', () => {
    // Simulate offline mode
    cy.window().then((win) => {
      cy.stub(win.navigator, 'onLine').value(false);
      win.dispatchEvent(new Event('offline'));
    });
    
    // Check offline indicator
    cy.get('[data-testid="offline-indicator"]').should('be.visible');
    
    // Try to create tenant while offline
    cy.get('[data-testid="create-tenant-button"]').click();
    cy.get('[data-testid="tenant-name-input"]').type('Offline Test');
    cy.get('[data-testid="save-tenant-button"]').click();
    
    // Should show offline message
    cy.get('[data-testid="offline-notification"]').should('contain', 'offline');
    
    // Simulate coming back online
    cy.window().then((win) => {
      cy.stub(win.navigator, 'onLine').value(true);
      win.dispatchEvent(new Event('online'));
    });
    
    // Offline indicator should disappear
    cy.get('[data-testid="offline-indicator"]').should('not.exist');
  });

});