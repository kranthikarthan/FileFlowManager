package com.filetransfer.web.service;

import com.filetransfer.web.entity.Tenant;
import com.filetransfer.web.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TenantTimeZoneServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantTimeZoneService tenantTimeZoneService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testTenant = new Tenant();
        testTenant.setTenantId("test-tenant");
        testTenant.setTimezone("America/New_York");
        testTenant.setName("Test Tenant");
    }

    @Test
    void testGetTenantTimeZone() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        ZoneId timeZone = tenantTimeZoneService.getTenantTimeZone("test-tenant");

        assertEquals(ZoneId.of("America/New_York"), timeZone);
        verify(tenantRepository).findByTenantId("test-tenant");
    }

    @Test
    void testGetTenantTimeZoneDefault() {
        when(tenantRepository.findByTenantId("unknown-tenant")).thenReturn(Optional.empty());

        ZoneId timeZone = tenantTimeZoneService.getTenantTimeZone("unknown-tenant");

        assertEquals(ZoneId.systemDefault(), timeZone);
    }

    @Test
    void testGetTenantTimeZoneNullTimezone() {
        testTenant.setTimezone(null);
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        ZoneId timeZone = tenantTimeZoneService.getTenantTimeZone("test-tenant");

        assertEquals(ZoneId.systemDefault(), timeZone);
    }

    @Test
    void testGetTenantTimeZoneInvalidTimezone() {
        testTenant.setTimezone("Invalid/Timezone");
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        ZoneId timeZone = tenantTimeZoneService.getTenantTimeZone("test-tenant");

        assertEquals(ZoneId.systemDefault(), timeZone);
    }

    @Test
    void testConvertUtcToTenantTime() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDateTime utcTime = LocalDateTime.of(2024, 1, 15, 12, 0, 0); // Noon UTC
        LocalDateTime tenantTime = tenantTimeZoneService.convertUtcToTenantTime("test-tenant", utcTime);

        // EST is UTC-5, so noon UTC should be 7 AM EST
        assertEquals(LocalDateTime.of(2024, 1, 15, 7, 0, 0), tenantTime);
    }

    @Test
    void testConvertTenantTimeToUtc() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDateTime tenantTime = LocalDateTime.of(2024, 1, 15, 7, 0, 0); // 7 AM EST
        LocalDateTime utcTime = tenantTimeZoneService.convertTenantTimeToUtc("test-tenant", tenantTime);

        // 7 AM EST should be noon UTC
        assertEquals(LocalDateTime.of(2024, 1, 15, 12, 0, 0), utcTime);
    }

    @Test
    void testGetCurrentTenantTime() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDateTime tenantTime = tenantTimeZoneService.getCurrentTenantTime("test-tenant");

        assertNotNull(tenantTime);
        // Should be different from system time if tenant is in different timezone
    }

    @Test
    void testGetCurrentTenantDate() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate tenantDate = tenantTimeZoneService.getCurrentTenantDate("test-tenant");

        assertNotNull(tenantDate);
    }

    @Test
    void testHasTimePassedInTenantZone() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalTime cutOffTime = LocalTime.of(18, 0); // 6 PM
        LocalDate targetDate = LocalDate.now();

        boolean hasPassed = tenantTimeZoneService.hasTimePassedInTenantZone("test-tenant", cutOffTime, targetDate);

        // Result depends on current time, but method should not throw exception
        assertNotNull(hasPassed);
    }

    @Test
    void testGetCutOffTimeWithTimeZone() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalTime cutOffTime = LocalTime.of(18, 0);

        ZonedDateTime zonedCutOff = tenantTimeZoneService.getCutOffTimeWithTimeZone("test-tenant", date, cutOffTime);

        assertEquals(ZoneId.of("America/New_York"), zonedCutOff.getZone());
        assertEquals(date, zonedCutOff.toLocalDate());
        assertEquals(cutOffTime, zonedCutOff.toLocalTime());
    }

    @Test
    void testFormatTimeForTenant() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDateTime utcDateTime = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        String formatted = tenantTimeZoneService.formatTimeForTenant("test-tenant", utcDateTime, "yyyy-MM-dd HH:mm:ss z");

        assertNotNull(formatted);
        assertTrue(formatted.contains("2024-01-15"));
        // Should show EST/EDT timezone
    }

    @Test
    void testGetTimeUntilCutOff() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime cutOffTime = LocalTime.of(18, 0);

        Duration timeUntil = tenantTimeZoneService.getTimeUntilCutOff("test-tenant", tomorrow, cutOffTime);

        assertNotNull(timeUntil);
        // Should be positive for future cut-off
        assertTrue(timeUntil.toHours() >= 0);
    }

    @Test
    void testGetTimeUntilCutOffPassed() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalTime cutOffTime = LocalTime.of(18, 0);

        Duration timeUntil = tenantTimeZoneService.getTimeUntilCutOff("test-tenant", yesterday, cutOffTime);

        assertNotNull(timeUntil);
        // Should be negative for past cut-off
        assertTrue(timeUntil.isNegative());
    }

    @Test
    void testIsInProcessingWindow() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime cutOffTime = LocalTime.of(23, 59); // Very late cut-off

        boolean inWindow = tenantTimeZoneService.isInProcessingWindow("test-tenant", tomorrow, cutOffTime);

        // Should be true for future date with late cut-off
        assertTrue(inWindow);
    }

    @Test
    void testIsInProcessingWindowPassed() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalTime cutOffTime = LocalTime.of(1, 0); // Very early cut-off

        boolean inWindow = tenantTimeZoneService.isInProcessingWindow("test-tenant", yesterday, cutOffTime);

        // Should be false for past date with early cut-off
        assertFalse(inWindow);
    }

    @Test
    void testGetTenantTimeZoneInfo() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        TenantTimeZoneService.TimeZoneInfo info = tenantTimeZoneService.getTenantTimeZoneInfo("test-tenant");

        assertNotNull(info);
        assertEquals("America/New_York", info.getZoneId());
        assertNotNull(info.getDisplayName());
        assertNotNull(info.getOffset());
        assertNotNull(info.getCurrentTime());
        assertTrue(info.getCurrentTime().contains("2024"));
    }

    @Test
    void testTimeZoneInfoFields() {
        TenantTimeZoneService.TimeZoneInfo info = new TenantTimeZoneService.TimeZoneInfo(
            "America/New_York",
            "Eastern Standard Time",
            "-05:00",
            "2024-01-15 12:00:00 EST"
        );

        assertEquals("America/New_York", info.getZoneId());
        assertEquals("Eastern Standard Time", info.getDisplayName());
        assertEquals("-05:00", info.getOffset());
        assertEquals("2024-01-15 12:00:00 EST", info.getCurrentTime());
    }

    @Test
    void testDaylightSavingTimeHandling() {
        when(tenantRepository.findByTenantId("test-tenant")).thenReturn(Optional.of(testTenant));

        // Test summer time (EDT)
        LocalDateTime summerUtc = LocalDateTime.of(2024, 7, 15, 12, 0, 0);
        LocalDateTime summerTenant = tenantTimeZoneService.convertUtcToTenantTime("test-tenant", summerUtc);

        // Test winter time (EST)
        LocalDateTime winterUtc = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime winterTenant = tenantTimeZoneService.convertUtcToTenantTime("test-tenant", winterUtc);

        // Summer should be EDT (UTC-4), winter should be EST (UTC-5)
        assertEquals(LocalDateTime.of(2024, 7, 15, 8, 0, 0), summerTenant); // EDT
        assertEquals(LocalDateTime.of(2024, 1, 15, 7, 0, 0), winterTenant); // EST
    }

    @Test
    void testRepositoryException() {
        when(tenantRepository.findByTenantId("test-tenant")).thenThrow(new RuntimeException("Database error"));

        ZoneId timeZone = tenantTimeZoneService.getTenantTimeZone("test-tenant");

        // Should fallback to system default on error
        assertEquals(ZoneId.systemDefault(), timeZone);
    }
}