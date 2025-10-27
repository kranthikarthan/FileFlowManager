package com.enterprise.fileflow.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Entity representing holiday calendar for tenant-specific holidays
 */
@Entity
@Table(name = "holiday_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HolidayCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant is required")
    private Tenant tenant;

    @Column(name = "holiday_date", nullable = false)
    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    @Column(nullable = false)
    @NotBlank(message = "Holiday name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}