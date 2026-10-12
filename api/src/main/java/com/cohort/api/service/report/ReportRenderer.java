package com.cohort.api.service.report;

/** Turns a format-independent {@link Report} into the bytes of one file format. */
public interface ReportRenderer {

    ReportFormat format();

    byte[] render(Report report);
}
