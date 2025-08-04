package com.enterprise.fileflow.shared.entity;

import com.enterprise.fileflow.shared.enums.FileStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a file transaction in the system
 */
@Entity
@Table(name = "file_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FileTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @NotNull(message = "Service is required")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_service_id")
    private SubService subService;

    @Column(name = "file_name", nullable = false)
    @NotBlank(message = "File name is required")
    private String fileName;

    @Column(name = "file_path", nullable = false)
    @NotBlank(message = "File path is required")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", nullable = false)
    @NotBlank(message = "File type is required")
    private String fileType; // SOT, DATA, EOT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status = FileStatus.RECEIVED;

    @Column(name = "batch_id")
    private String batchId; // Groups related files together

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "moved_to_path")
    private String movedToPath;

    @Column(name = "validation_result")
    private String validationResult;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String notes;
}