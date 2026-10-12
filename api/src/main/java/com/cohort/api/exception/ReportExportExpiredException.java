package com.cohort.api.exception;

/** FR-104: the caller's own export exists but its download window has closed. Only ever raised after ownership is confirmed, so it reveals nothing about other users' exports. */
public class ReportExportExpiredException extends RuntimeException {

    public ReportExportExpiredException() {
        super("This report has expired. Generate a new one to download it.");
    }
}
