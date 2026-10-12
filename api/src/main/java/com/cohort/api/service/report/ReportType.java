package com.cohort.api.service.report;

/**
 * US-80 / FR-102..103: what an export contains. Lowercase so {@code name()}
 * is exactly the value V21's CHECK constraint allows. {@code progress_report}
 * is both sections in one PDF -- the "share my progress with a mentor" case.
 */
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

    /** A CSV holds exactly one table, so only single-section reports can be CSV (V21 enforces the same rule). */
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.pdf || this != progress_report;
    }
}
