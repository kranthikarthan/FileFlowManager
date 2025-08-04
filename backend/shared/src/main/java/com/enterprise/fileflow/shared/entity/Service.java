package com.enterprise.fileflow.shared.entity;

import com.enterprise.fileflow.shared.enums.ServiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Entity representing a file processing service
 */
@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Service extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Service code is required")
    private String code;

    @Column(nullable = false)
    @NotBlank(message = "Service name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant is required")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "input_folder_path")
    private String inputFolderPath;

    @Column(name = "output_folder_path")
    private String outputFolderPath;

    @Column(name = "file_name_prefix")
    private String fileNamePrefix;

    @Column(name = "sot_pattern")
    private String sotPattern;

    @Column(name = "eot_pattern")
    private String eotPattern;

    @Column(name = "data_file_pattern")
    private String dataFilePattern;

    @Column(name = "cutoff_time")
    private LocalTime cutoffTime;

    @Column(name = "alert_threshold_minutes")
    private Integer alertThresholdMinutes;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubService> subServices;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileTransaction> fileTransactions;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ValidationRule> validationRules;
}