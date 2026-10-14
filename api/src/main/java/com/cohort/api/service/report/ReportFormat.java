package com.cohort.api.service.report;

public enum ReportFormat {
    pdf("application/pdf", "pdf"),
    csv("text/csv", "csv");

    private final String contentType;
    private final String extension;

    ReportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
