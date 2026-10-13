package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full detail view (US-11): every stored field plus the status timeline and, since P24, its interviews (FR-17). */
public record ApplicationDetailResponse(
        UUID id,
        CompanySummary company,
        UUID resumeId,
        String jobTitle,
        String jobDescription,
        String jobUrl,
        String location,
        Application.WorkMode workMode,
        Application.JobType jobType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        LocalDate dateApplied,
        ApplicationStatus status,
        ApplicationStatus currentStage,
        long daysInStage,
        Application.Priority priority,
        String source,
        @JsonProperty("is_referral") boolean isReferral,
        LocalDate deadline,
        String recruiterName,
        String recruiterEmail,
        String notes,
        String coverLetter,
        Instant archivedAt,
        Instant stageEnteredAt,
        Instant createdAt,
        Instant updatedAt,
        List<ApplicationStatusHistoryResponse> history,
        List<ApplicationInterviewSummary> interviews,
        List<ApplicationStatus> allowedNextStatuses,
        List<ApplicationStatus> correctionStatuses) {
}
