package com.cohort.api.service.report;

import com.cohort.api.service.analytics.ApplicationSnapshot;
import java.time.LocalDate;

/**
 * One application as {@link ReportBuilder} receives it: the descriptive
 * fields the applications table shows, plus the same
 * {@link ApplicationSnapshot} every analytics metric computes from -- so the
 * table and the analytics section are always built from one consistent set
 * of rows, and the analytics never need a second data path.
 */
public record ReportApplicationInput(
        String companyName,
        String jobTitle,
        LocalDate dateApplied,
        String location,
        String workMode,
        String priority,
        String source,
        boolean referral,
        int interviewCount,
        ApplicationSnapshot snapshot) {
}
