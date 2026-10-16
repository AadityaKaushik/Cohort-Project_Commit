package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class ReportExportNotFoundException extends RuntimeException {

    public ReportExportNotFoundException() {
        super("Report not found.");
    }
}
