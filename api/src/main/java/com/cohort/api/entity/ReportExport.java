package com.cohort.api.entity;

import com.cohort.api.service.report.ReportFormat;
import com.cohort.api.service.report.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "report_exports")
public class ReportExport {
    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFormat format;

    @Column(name = "date_range_from")
    private LocalDate dateRangeFrom;

    @Column(name = "date_range_to")
    private LocalDate dateRangeTo;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ReportExport() {
        // JPA
    }

    public ReportExport(
            UUID userId, ReportType reportType, ReportFormat format, LocalDate dateRangeFrom, LocalDate dateRangeTo,
            String filePath, String fileName, String contentType, long fileSizeBytes, Instant generatedAt, Instant expiresAt) {
        this.userId = userId;
        this.reportType = reportType;
        this.format = format;
        this.dateRangeFrom = dateRangeFrom;
        this.dateRangeTo = dateRangeTo;
        this.filePath = filePath;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public ReportFormat getFormat() {
        return format;
    }

    public LocalDate getDateRangeFrom() {
        return dateRangeFrom;
    }

    public LocalDate getDateRangeTo() {
        return dateRangeTo;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
