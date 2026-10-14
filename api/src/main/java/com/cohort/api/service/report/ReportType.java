package com.cohort.api.service.report;

public enum ReportType {
    applications(true, false),
    analytics_summary(false, true),
    progress_report(true, true);

    private final boolean includesApplications;
    private final boolean includesAnalytics;

    ReportType(boolean includesApplications, boolean includesAnalytics) {
        this.includesApplications = includesApplications;
        this.includesAnalytics = includesAnalytics;
    }

    public boolean includesApplications() {
        return includesApplications;
    }

    public boolean includesAnalytics() {
        return includesAnalytics;
    }

    public boolean supports(ReportFormat format) {
        return format == ReportFormat.pdf || this != progress_report;
    }
}
