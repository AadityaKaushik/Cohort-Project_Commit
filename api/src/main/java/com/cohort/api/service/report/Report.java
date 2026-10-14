package com.cohort.api.service.report;

import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.dto.RejectionDistributionResponse;
import com.cohort.api.entity.ApplicationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public record Report(
        String title,
        String ownerName,
        LocalDate dateFrom,
        LocalDate dateTo,
        Instant generatedAt,
        ZoneId zone,
        List<ApplicationRow> applications,
        AnalyticsSummary analytics) {
    public boolean includesApplications() {
        return applications != null;
    }

    public boolean includesAnalytics() {
        return analytics != null;
    }

    public record ApplicationRow(
            String companyName,
            String jobTitle,
            ApplicationStatus status,
            ApplicationStatus currentStage,
            LocalDate dateApplied,
            Long daysInCurrentStage,
            int interviewCount,
            String location,
            String workMode,
            String priority,
            String source,
            boolean referral) {
    }

    public record AnalyticsSummary(
            int applicationCount,
            Map<ApplicationStatus, Long> statusBreakdown,
            FunnelResponse funnel,
            ConversionRatesResponse conversions,
            DwellTimesResponse dwellTimes,
            RejectionDistributionResponse rejections) {
    }
}
