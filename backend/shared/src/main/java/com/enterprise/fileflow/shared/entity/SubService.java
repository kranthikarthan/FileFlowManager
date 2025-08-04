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
 * Entity representing a sub-service that can override parent service configurations
 */
@Entity
@Table(name = "sub_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Sub-service code is required")
    private String code;

    @Column(nullable = false)
    @NotBlank(message = "Sub-service name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @NotNull(message = "Parent service is required")
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    // Override fields - if null, inherit from parent service
    @Column(name = "input_folder_path_override")
    private String inputFolderPathOverride;

    @Column(name = "output_folder_path_override")
    private String outputFolderPathOverride;

    @Column(name = "file_name_prefix_override")
    private String fileNamePrefixOverride;

    @Column(name = "sot_pattern_override")
    private String sotPatternOverride;

    @Column(name = "eot_pattern_override")
    private String eotPatternOverride;

    @Column(name = "data_file_pattern_override")
    private String dataFilePatternOverride;

    @Column(name = "cutoff_time_override")
    private LocalTime cutoffTimeOverride;

    @Column(name = "alert_threshold_minutes_override")
    private Integer alertThresholdMinutesOverride;

    @OneToMany(mappedBy = "subService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileTransaction> fileTransactions;

    @OneToMany(mappedBy = "subService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ValidationRule> validationRules;

    // Helper methods to get effective values (override or inherit)
    public String getEffectiveInputFolderPath() {
        return inputFolderPathOverride != null ? inputFolderPathOverride : service.getInputFolderPath();
    }

    public String getEffectiveOutputFolderPath() {
        return outputFolderPathOverride != null ? outputFolderPathOverride : service.getOutputFolderPath();
    }

    public String getEffectiveFileNamePrefix() {
        return fileNamePrefixOverride != null ? fileNamePrefixOverride : service.getFileNamePrefix();
    }

    public String getEffectiveSotPattern() {
        return sotPatternOverride != null ? sotPatternOverride : service.getSotPattern();
    }

    public String getEffectiveEotPattern() {
        return eotPatternOverride != null ? eotPatternOverride : service.getEotPattern();
    }

    public String getEffectiveDataFilePattern() {
        return dataFilePatternOverride != null ? dataFilePatternOverride : service.getDataFilePattern();
    }

    public LocalTime getEffectiveCutoffTime() {
        return cutoffTimeOverride != null ? cutoffTimeOverride : service.getCutoffTime();
    }

    public Integer getEffectiveAlertThresholdMinutes() {
        return alertThresholdMinutesOverride != null ? alertThresholdMinutesOverride : service.getAlertThresholdMinutes();
    }
}