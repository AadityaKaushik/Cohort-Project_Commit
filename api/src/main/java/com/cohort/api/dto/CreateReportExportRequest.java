package com.cohort.api.dto;

import com.cohort.api.service.report.ReportFormat;
import com.cohort.api.service.report.ReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateReportExportRequest(
        @NotNull ReportType reportType,
        @NotNull ReportFormat format,
        LocalDate dateFrom,
        LocalDate dateTo) {
}
