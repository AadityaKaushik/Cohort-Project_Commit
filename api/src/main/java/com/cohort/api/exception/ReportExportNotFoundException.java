package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class ReportExportNotFoundException extends RuntimeException {

    public ReportExportNotFoundException() {
        super("Report not found.");
    }
}
