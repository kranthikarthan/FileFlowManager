package com.enterprise.fileflow.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Entity representing a tenant in the multi-tenant system
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Tenant code is required")
    private String code;

    @Column(nullable = false)
    @NotBlank(message = "Tenant name is required")
    private String name;

    @Column(nullable = false)
    @NotNull(message = "Timezone is required")
    private ZoneId timezone;

    @Column(name = "sso_provider_url")
    private String ssoProviderUrl;

    @Column(name = "sso_client_id")
    private String ssoClientId;

    @Column(name = "sso_client_secret")
    private String ssoClientSecret;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Service> services;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HolidayCalendar> holidayCalendars;
}