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

/**
 * What {@link ReportBuilder#build()} produces: a format-independent report
 * document. {@link ReportRenderer} strategies turn it into PDF or CSV bytes;
 * nothing here knows about either format. A section that was not requested
 * is {@code null} (not empty) -- "no applications in range" and "applications
 * not included" are different things and render differently.
 */
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

    /** One row of the applications table. {@code daysInCurrentStage} is null for a closed (terminal) application, where "time in stage" has stopped meaning anything. */
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

    /**
     * FR-103: funnel, conversion rates and their sample sizes, plus dwell
     * times and where applications ended -- each exactly the DTO the in-app
     * analytics endpoints return, computed by the same calculator.
     */
    public record AnalyticsSummary(
            int applicationCount,
            Map<ApplicationStatus, Long> statusBreakdown,
            FunnelResponse funnel,
            ConversionRatesResponse conversions,
            DwellTimesResponse dwellTimes,
            RejectionDistributionResponse rejections) {
    }
}
