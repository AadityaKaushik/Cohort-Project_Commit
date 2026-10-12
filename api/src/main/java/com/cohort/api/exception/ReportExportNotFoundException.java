package com.cohort.api.exception;

/** Thrown for both a missing export ID and one belonging to another user — FR-95/FR-104: always 404, never 403. */
public class ReportExportNotFoundException extends RuntimeException {

    public ReportExportNotFoundException() {
        super("Report not found.");
    }
}
