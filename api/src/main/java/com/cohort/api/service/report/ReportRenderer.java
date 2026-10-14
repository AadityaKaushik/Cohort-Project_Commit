package com.cohort.api.service.report;

public interface ReportRenderer {
    ReportFormat format();

    byte[] render(Report report);
}
