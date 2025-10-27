package com.enterprise.fileflow.shared.entity;

import com.enterprise.fileflow.shared.enums.ValidationStrategy;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entity representing validation rules for file content
 */
@Entity
@Table(name = "validation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ValidationRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_service_id")
    private SubService subService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Validation strategy is required")
    private ValidationStrategy strategy;

    @Column(name = "schema_file_path")
    private String schemaFilePath;

    @Column(name = "schema_content", columnDefinition = "TEXT")
    private String schemaContent;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = false;

    @Column(name = "file_pattern")
    private String filePattern; // Which files this rule applies to

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}