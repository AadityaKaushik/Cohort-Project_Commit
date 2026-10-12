package com.cohort.api.dto;

import com.cohort.api.service.report.ReportFormat;
import com.cohort.api.service.report.ReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** US-80 / FR-102: what to export, in which format, optionally limited to applications dated within {@code [date_from, date_to]}. */
public record CreateReportExportRequest(
        @NotNull ReportType reportType,
        @NotNull ReportFormat format,
        LocalDate dateFrom,
        LocalDate dateTo) {
}
