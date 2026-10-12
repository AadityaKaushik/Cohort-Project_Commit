package com.cohort.api.dto;

import com.cohort.api.entity.ReportExport;
import com.cohort.api.service.report.ReportFormat;
import com.cohort.api.service.report.ReportType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One generated export. Deliberately omits the storage key -- clients only ever download through {@code GET /reports/{id}/file}. */
public record ReportExportResponse(
        UUID id,
        ReportType reportType,
        ReportFormat format,
        LocalDate dateFrom,
        LocalDate dateTo,
        String fileName,
        long fileSizeBytes,
        Instant generatedAt,
        Instant expiresAt) {

    public static ReportExportResponse from(ReportExport export) {
        return new ReportExportResponse(
                export.getId(), export.getReportType(), export.getFormat(), export.getDateRangeFrom(), export.getDateRangeTo(),
                export.getFileName(), export.getFileSizeBytes(), export.getGeneratedAt(), export.getExpiresAt());
    }
}
