package com.filetransfer.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the many-to-many relationship between file transfers and tags
 */
@Entity
@Table(name = "file_transfer_tags")
public class FileTransferTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_transfer_id", nullable = false)
    private Long fileTransferId;
    
    @Column(name = "file_tag_id", nullable = false)
    private Long fileTagId;
    
    @Column(name = "tagged_by", length = 100)
    private String taggedBy;
    
    @Column(name = "tagged_at", nullable = false)
    private LocalDateTime taggedAt;
    
    @Column(name = "tag_reason", length = 500)
    private String tagReason; // Why this tag was applied
    
    @Column(name = "auto_tagged")
    private Boolean autoTagged = false; // Whether tag was applied automatically
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_transfer_id", insertable = false, updatable = false)
    private FileTransferRecord fileTransfer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_tag_id", insertable = false, updatable = false)
    private FileTag fileTag;
    
    // Constructors
    public FileTransferTag() {
        this.taggedAt = LocalDateTime.now();
    }
    
    public FileTransferTag(Long fileTransferId, Long fileTagId, String taggedBy) {
        this();
        this.fileTransferId = fileTransferId;
        this.fileTagId = fileTagId;
        this.taggedBy = taggedBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getFileTransferId() { return fileTransferId; }
    public void setFileTransferId(Long fileTransferId) { this.fileTransferId = fileTransferId; }
    
    public Long getFileTagId() { return fileTagId; }
    public void setFileTagId(Long fileTagId) { this.fileTagId = fileTagId; }
    
    public String getTaggedBy() { return taggedBy; }
    public void setTaggedBy(String taggedBy) { this.taggedBy = taggedBy; }
    
    public LocalDateTime getTaggedAt() { return taggedAt; }
    public void setTaggedAt(LocalDateTime taggedAt) { this.taggedAt = taggedAt; }
    
    public String getTagReason() { return tagReason; }
    public void setTagReason(String tagReason) { this.tagReason = tagReason; }
    
    public Boolean getAutoTagged() { return autoTagged; }
    public void setAutoTagged(Boolean autoTagged) { this.autoTagged = autoTagged; }
    
    public FileTransferRecord getFileTransfer() { return fileTransfer; }
    public void setFileTransfer(FileTransferRecord fileTransfer) { this.fileTransfer = fileTransfer; }
    
    public FileTag getFileTag() { return fileTag; }
    public void setFileTag(FileTag fileTag) { this.fileTag = fileTag; }
}