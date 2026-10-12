package com.cohort.api.service.report;

/** FR-102: the two export formats, with the HTTP content type and file extension each is served with. */
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
