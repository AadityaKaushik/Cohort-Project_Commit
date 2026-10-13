package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One row of the applications list (US-10): company, role, status, stage, days in stage, date applied, priority. */
public record ApplicationSummary(
        UUID id,
        CompanySummary company,
        String jobTitle,
        ApplicationStatus status,
        ApplicationStatus currentStage,
        long daysInStage,
        LocalDate dateApplied,
        Application.Priority priority,
        boolean archived,
        List<ApplicationStatus> allowedNextStatuses,
        List<ApplicationStatus> correctionStatuses) {
}
