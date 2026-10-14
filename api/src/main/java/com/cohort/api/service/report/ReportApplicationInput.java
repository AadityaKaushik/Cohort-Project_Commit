package com.cohort.api.service.report;

import com.cohort.api.service.analytics.ApplicationSnapshot;
import java.time.LocalDate;

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
