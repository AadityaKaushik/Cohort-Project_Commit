package com.cohort.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * A single resume version (PROJECT_PLAN.md §17.2, US-20). File fields are
 * all nullable: a resume record can exist before a file is ever uploaded to
 * it (US-20 creates the record, US-21/P20 uploads the file). {@code
 * parentResumeId} is the lineage column a clone (US-25/P21) populates —
 * {@code null} for an originally-created resume.
 */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "version_label")
    private String versionLabel;

    @Column(name = "target_role")
    private String targetRole;

    @Column
    private String summary;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "parent_resume_id")
    private UUID parentResumeId;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_original_name")
    private String fileOriginalName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_mime")
    private String fileMime;

    @Column(name = "file_uploaded_at")
    private Instant fileUploadedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Resume() {
        // JPA
    }

    public Resume(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public void applyStoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
        this.filePath = storageKey;
        this.fileOriginalName = originalFilename;
        this.fileMime = contentType;
        this.fileSizeBytes = sizeBytes;
        this.fileUploadedAt = Instant.now();
    }

    public void clearStoredFile() {
        this.filePath = null;
        this.fileOriginalName = null;
        this.fileMime = null;
        this.fileSizeBytes = null;
        this.fileUploadedAt = null;
    }

    public boolean hasFile() {
        return filePath != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public UUID getParentResumeId() {
        return parentResumeId;
    }

    public void setParentResumeId(UUID parentResumeId) {
        this.parentResumeId = parentResumeId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileOriginalName() {
        return fileOriginalName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFileMime() {
        return fileMime;
    }

    public Instant getFileUploadedAt() {
        return fileUploadedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
