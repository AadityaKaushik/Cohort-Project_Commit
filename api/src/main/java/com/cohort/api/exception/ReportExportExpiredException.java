package com.cohort.api.exception;

public class ReportExportExpiredException extends RuntimeException {
    public ReportExportExpiredException() {
        super("This report has expired. Generate a new one to download it.");
    }
}
