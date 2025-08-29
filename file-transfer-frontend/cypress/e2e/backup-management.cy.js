/**
 * Backup Management E2E Tests
 * Tests complete backup and disaster recovery workflows from the frontend
 */

describe('Backup Management E2E Tests', () => {
  
  beforeEach(() => {
    cy.intercept('GET', '/api/backup/list*', { fixture: 'backups.json' }).as('getBackups');
    cy.intercept('POST', '/api/backup/create', { fixture: 'backup-created.json' }).as('createBackup');
    cy.intercept('POST', '/api/backup/restore', { fixture: 'restore-started.json' }).as('startRestore');
    cy.intercept('GET', '/api/backup/status', { fixture: 'backup-status.json' }).as('getBackupStatus');
    cy.intercept('POST', '/api/backup/cross-application/create', { fixture: 'cross-app-backup-created.json' }).as('createCrossAppBackup');
    
    cy.visit('/backup-management');
  });

  it('should complete backup creation and management workflow', () => {
    // STEP 1: View Backup Dashboard
    cy.wait('@getBackups');
    cy.wait('@getBackupStatus');
    
    cy.get('[data-testid="backup-dashboard"]').should('be.visible');
    cy.get('[data-testid="total-backups"]').should('be.visible');
    cy.get('[data-testid="backup-storage-usage"]').should('be.visible');
    cy.get('[data-testid="last-backup-date"]').should('be.visible');
    
    // STEP 2: Create Manual Backup
    cy.get('[data-testid="create-backup-button"]').click();
    
    cy.get('[data-testid="backup-type-select"]').click();
    cy.get('[data-value="FULL"]').click();
    
    // Configure backup options
    cy.get('[data-testid="include-database-checkbox"]').check();
    cy.get('[data-testid="include-files-checkbox"]').check();
    cy.get('[data-testid="include-application-state-checkbox"]').check();
    
    cy.get('[data-testid="enable-compression-checkbox"]').check();
    cy.get('[data-testid="enable-encryption-checkbox"]').check();
    cy.get('[data-testid="enable-verification-checkbox"]').check();
    
    cy.get('[data-testid="backup-description-input"]').type('E2E test manual backup');
    
    cy.get('[data-testid="start-backup-button"]').click();
    cy.wait('@createBackup');
    
    // STEP 3: Monitor Backup Progress
    cy.get('[data-testid="backup-progress-modal"]').should('be.visible');
    cy.get('[data-testid="backup-progress-bar"]').should('be.visible');
    cy.get('[data-testid="backup-status-text"]').should('contain', 'Creating backup');
    
    // Simulate backup progress updates
    cy.get('[data-testid="backup-phase"]').should('contain', 'Database backup');
    cy.get('[data-testid="backup-phase"]').should('contain', 'File backup', { timeout: 10000 });
    cy.get('[data-testid="backup-phase"]').should('contain', 'Verification', { timeout: 15000 });
    cy.get('[data-testid="backup-phase"]').should('contain', 'Completed', { timeout: 20000 });
    
    // STEP 4: Verify Backup Created
    cy.get('[data-testid="backup-success-message"]').should('be.visible');
    cy.get('[data-testid="backup-id"]').should('be.visible');
    cy.get('[data-testid="backup-size"]').should('be.visible');
    cy.get('[data-testid="backup-duration"]').should('be.visible');
    
    cy.get('[data-testid="close-progress-modal"]').click();
    
    // STEP 5: Verify Backup in List
    cy.get('[data-testid="refresh-backup-list"]').click();
    cy.wait('@getBackups');
    
    cy.get('[data-testid="backup-list"]').should('contain', 'E2E test manual backup');
    cy.get('[data-testid="backup-type-full"]').should('be.visible');
    cy.get('[data-testid="backup-status-completed"]').should('be.visible');
  });

  it('should complete cross-application backup workflow', () => {
    // STEP 1: Create Cross-Application Backup
    cy.get('[data-testid="cross-app-backup-tab"]').click();
    
    cy.get('[data-testid="create-cross-app-backup-button"]').click();
    
    // Select applications to backup
    cy.get('[data-testid="include-web-app-checkbox"]').check();
    cy.get('[data-testid="include-batch-app-checkbox"]').check();
    cy.get('[data-testid="include-frontend-app-checkbox"]').check();
    
    // Configure coordination options
    cy.get('[data-testid="synchronize-backups-checkbox"]').check();
    cy.get('[data-testid="create-snapshot-checkbox"]').check();
    cy.get('[data-testid="verify-consistency-checkbox"]').check();
    
    cy.get('[data-testid="backup-description-input"]').type('E2E cross-application backup test');
    
    cy.get('[data-testid="start-cross-app-backup-button"]').click();
    cy.wait('@createCrossAppBackup');
    
    // STEP 2: Monitor Cross-App Backup Progress
    cy.get('[data-testid="cross-app-progress-modal"]').should('be.visible');
    
    // Should show progress for each application
    cy.get('[data-testid="web-app-progress"]').should('be.visible');
    cy.get('[data-testid="batch-app-progress"]').should('be.visible');
    cy.get('[data-testid="frontend-app-progress"]').should('be.visible');
    
    // Monitor coordination phases
    cy.get('[data-testid="coordination-phase"]').should('contain', 'Preparing applications');
    cy.get('[data-testid="coordination-phase"]').should('contain', 'Creating snapshot', { timeout: 10000 });
    cy.get('[data-testid="coordination-phase"]').should('contain', 'Executing backups', { timeout: 15000 });
    cy.get('[data-testid="coordination-phase"]').should('contain', 'Verifying consistency', { timeout: 20000 });
    cy.get('[data-testid="coordination-phase"]').should('contain', 'Completed', { timeout: 25000 });
    
    // STEP 3: Verify Cross-App Backup Results
    cy.get('[data-testid="cross-app-backup-success"]').should('be.visible');
    cy.get('[data-testid="coordination-id"]').should('be.visible');
    cy.get('[data-testid="applications-backed-up"]').should('contain', '3');
    cy.get('[data-testid="consistency-check"]').should('contain', 'PASSED');
  });

  it('should test backup restore workflow', () => {
    // STEP 1: Select Backup to Restore
    cy.get('[data-testid="backup-list"]').should('be.visible');
    
    cy.get('[data-testid="backup-item"]').first().within(() => {
      cy.get('[data-testid="backup-type"]').should('be.visible');
      cy.get('[data-testid="restore-backup-button"]').click();
    });
    
    // STEP 2: Configure Restore Options
    cy.get('[data-testid="restore-modal"]').should('be.visible');
    
    cy.get('[data-testid="restore-database-checkbox"]').check();
    cy.get('[data-testid="restore-files-checkbox"]').check();
    cy.get('[data-testid="restore-application-state-checkbox"]').check();
    
    // Safety options
    cy.get('[data-testid="create-restore-point-checkbox"]').check();
    cy.get('[data-testid="verify-restore-checkbox"]').check();
    
    cy.get('[data-testid="restore-description-input"]').type('E2E test restore operation');
    
    // STEP 3: Confirm Restore (with safety confirmation)
    cy.get('[data-testid="start-restore-button"]').click();
    
    cy.get('[data-testid="restore-confirmation-modal"]').should('be.visible');
    cy.get('[data-testid="restore-warning"]').should('contain', 'This will overwrite current data');
    
    cy.get('[data-testid="confirm-restore-checkbox"]').check();
    cy.get('[data-testid="type-confirm-text"]').type('CONFIRM RESTORE');
    
    cy.get('[data-testid="execute-restore-button"]').click();
    cy.wait('@startRestore');
    
    // STEP 4: Monitor Restore Progress
    cy.get('[data-testid="restore-progress-modal"]').should('be.visible');
    cy.get('[data-testid="restore-progress-bar"]').should('be.visible');
    
    // Monitor restore phases
    cy.get('[data-testid="restore-phase"]').should('contain', 'Creating restore point');
    cy.get('[data-testid="restore-phase"]').should('contain', 'Restoring database', { timeout: 10000 });
    cy.get('[data-testid="restore-phase"]').should('contain', 'Restoring files', { timeout: 15000 });
    cy.get('[data-testid="restore-phase"]').should('contain', 'Verification', { timeout: 20000 });
    cy.get('[data-testid="restore-phase"]').should('contain', 'Completed', { timeout: 25000 });
    
    // STEP 5: Verify Restore Results
    cy.get('[data-testid="restore-success-message"]').should('be.visible');
    cy.get('[data-testid="restore-id"]').should('be.visible');
    cy.get('[data-testid="restore-duration"]').should('be.visible');
    cy.get('[data-testid="verification-result"]').should('contain', 'PASSED');
  });

  it('should test disaster recovery simulation', () => {
    // STEP 1: Access Disaster Recovery
    cy.get('[data-testid="disaster-recovery-tab"]').click();
    
    cy.get('[data-testid="dr-dashboard"]').should('be.visible');
    cy.get('[data-testid="dr-status"]').should('be.visible');
    cy.get('[data-testid="primary-region"]').should('contain', 'us-east-1');
    cy.get('[data-testid="secondary-region"]').should('contain', 'us-west-2');
    
    // STEP 2: Create DR Plan
    cy.get('[data-testid="create-dr-plan-button"]').click();
    
    cy.get('[data-testid="plan-name-input"]').type('E2E DR Test Plan');
    cy.get('[data-testid="plan-description-textarea"]').type('End-to-end disaster recovery test plan');
    cy.get('[data-testid="plan-priority-select"]').click();
    cy.get('[data-value="CRITICAL"]').click();
    
    // Configure RTO/RPO
    cy.get('[data-testid="rto-input"]').type('60'); // 60 minutes
    cy.get('[data-testid="rpo-input"]').type('15'); // 15 minutes
    
    // Add recovery steps
    cy.get('[data-testid="add-recovery-step-button"]').click();
    cy.get('[data-testid="step-name-input"]').type('Activate Secondary Region');
    cy.get('[data-testid="step-description-textarea"]').type('Switch traffic to secondary region');
    cy.get('[data-testid="step-type-select"]').click();
    cy.get('[data-value="AUTOMATED"]').click();
    
    cy.get('[data-testid="save-dr-plan-button"]').click();
    
    // STEP 3: Test DR Plan
    cy.get('[data-testid="test-dr-plan-button"]').click();
    
    cy.get('[data-testid="test-type-select"]').click();
    cy.get('[data-value="SIMULATION"]').click();
    
    cy.get('[data-testid="start-dr-test-button"]').click();
    
    // STEP 4: Monitor DR Test
    cy.get('[data-testid="dr-test-progress"]').should('be.visible');
    cy.get('[data-testid="dr-test-steps"]').should('be.visible');
    
    cy.get('[data-testid="dr-step-status"]').should('contain', 'EXECUTING');
    cy.get('[data-testid="dr-step-status"]').should('contain', 'COMPLETED', { timeout: 30000 });
    
    // STEP 5: Verify DR Test Results
    cy.get('[data-testid="dr-test-results"]').should('be.visible');
    cy.get('[data-testid="test-passed"]').should('contain', 'true');
    cy.get('[data-testid="recovery-time"]').should('be.visible');
    cy.get('[data-testid="rto-compliance"]').should('contain', 'WITHIN_TARGET');
  });

});

describe('API Version Management E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/versions*', { fixture: 'api-versions.json' }).as('getApiVersions');
    cy.intercept('POST', '/api/versions/*/deprecate', { fixture: 'version-deprecated.json' }).as('deprecateVersion');
    cy.intercept('GET', '/api/versions/migration-plan*', { fixture: 'migration-plan.json' }).as('getMigrationPlan');
    
    cy.visit('/api-management');
  });

  it('should complete API version management workflow', () => {
    // STEP 1: View Version Dashboard
    cy.wait('@getApiVersions');
    
    cy.get('[data-testid="api-version-dashboard"]').should('be.visible');
    cy.get('[data-testid="current-web-version"]').should('contain', '2.0');
    cy.get('[data-testid="current-batch-version"]').should('contain', '2.0');
    cy.get('[data-testid="current-frontend-version"]').should('contain', '2.0');
    
    // STEP 2: View Version Compatibility Matrix
    cy.get('[data-testid="compatibility-matrix-tab"]').click();
    
    cy.get('[data-testid="compatibility-table"]').should('be.visible');
    cy.get('[data-testid="version-1-0-compatibility"]').should('contain', '1.1, 1.2');
    cy.get('[data-testid="version-2-0-compatibility"]').should('contain', '1.2, 2.1');
    
    // STEP 3: Test Version Migration Planning
    cy.get('[data-testid="migration-planning-tab"]').click();
    
    cy.get('[data-testid="from-version-select"]').click();
    cy.get('[data-value="1.2"]').click();
    
    cy.get('[data-testid="to-version-select"]').click();
    cy.get('[data-value="2.0"]').click();
    
    cy.get('[data-testid="generate-migration-plan-button"]').click();
    cy.wait('@getMigrationPlan');
    
    // STEP 4: Review Migration Plan
    cy.get('[data-testid="migration-plan-results"]').should('be.visible');
    cy.get('[data-testid="migration-feasible"]').should('contain', 'true');
    cy.get('[data-testid="estimated-effort"]').should('contain', 'MEDIUM');
    
    cy.get('[data-testid="migration-steps"]').should('be.visible');
    cy.get('[data-testid="breaking-changes"]').should('be.visible');
    
    // STEP 5: Test Version Switch
    cy.get('[data-testid="version-switching-tab"]').click();
    
    cy.get('[data-testid="target-version-select"]').click();
    cy.get('[data-value="2.1"]').click();
    
    cy.get('[data-testid="switch-version-button"]').click();
    
    // Should show beta warning
    cy.get('[data-testid="beta-version-warning"]').should('be.visible');
    cy.get('[data-testid="acknowledge-beta-risks"]').check();
    
    cy.get('[data-testid="confirm-version-switch"]').click();
    
    // STEP 6: Verify Version Switch
    cy.get('[data-testid="version-switch-success"]').should('be.visible');
    cy.get('[data-testid="current-version-display"]').should('contain', '2.1');
    cy.get('[data-testid="beta-features-notice"]').should('be.visible');
  });

});

describe('Performance Monitoring E2E Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/monitoring/metrics*', { fixture: 'performance-metrics.json' }).as('getMetrics');
    cy.intercept('GET', '/api/scalability/performance/metrics', { fixture: 'scalability-metrics.json' }).as('getScalabilityMetrics');
    cy.intercept('POST', '/api/scalability/simulate-load', { fixture: 'load-test-started.json' }).as('startLoadTest');
    
    cy.visit('/performance-monitoring');
  });

  it('should complete performance monitoring and testing workflow', () => {
    // STEP 1: View Performance Dashboard
    cy.wait('@getMetrics');
    cy.wait('@getScalabilityMetrics');
    
    cy.get('[data-testid="performance-dashboard"]').should('be.visible');
    
    // Check real-time metrics
    cy.get('[data-testid="cpu-usage-chart"]').should('be.visible');
    cy.get('[data-testid="memory-usage-chart"]').should('be.visible');
    cy.get('[data-testid="response-time-chart"]').should('be.visible');
    cy.get('[data-testid="throughput-chart"]').should('be.visible');
    
    // STEP 2: Test Load Simulation
    cy.get('[data-testid="load-testing-tab"]').click();
    
    cy.get('[data-testid="concurrent-users-input"]').type('25');
    cy.get('[data-testid="requests-per-user-input"]').type('10');
    cy.get('[data-testid="test-duration-input"]').type('30');
    
    // Select target applications
    cy.get('[data-testid="target-web-app-checkbox"]').check();
    cy.get('[data-testid="target-batch-app-checkbox"]').check();
    
    cy.get('[data-testid="start-load-test-button"]').click();
    cy.wait('@startLoadTest');
    
    // STEP 3: Monitor Load Test
    cy.get('[data-testid="load-test-progress"]').should('be.visible');
    cy.get('[data-testid="real-time-metrics"]').should('be.visible');
    
    // Should show real-time updates
    cy.get('[data-testid="current-users"]').should('be.visible');
    cy.get('[data-testid="requests-per-second"]').should('be.visible');
    cy.get('[data-testid="average-response-time"]').should('be.visible');
    cy.get('[data-testid="error-rate"]').should('be.visible');
    
    // STEP 4: View Load Test Results
    cy.get('[data-testid="load-test-status"]').should('contain', 'COMPLETED', { timeout: 45000 });
    
    cy.get('[data-testid="load-test-results"]').should('be.visible');
    cy.get('[data-testid="total-requests"]').should('contain', '250'); // 25 users * 10 requests
    cy.get('[data-testid="success-rate"]').should('be.visible');
    cy.get('[data-testid="p95-response-time"]').should('be.visible');
    
    // STEP 5: Check Auto-scaling Triggers
    cy.get('[data-testid="auto-scaling-events"]').should('be.visible');
    cy.get('[data-testid="scaling-decisions"]').should('be.visible');
  });

});