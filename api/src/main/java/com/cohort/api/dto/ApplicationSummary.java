package com.cohort.api.dto;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
