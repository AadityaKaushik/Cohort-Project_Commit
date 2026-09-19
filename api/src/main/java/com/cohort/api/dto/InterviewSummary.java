package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the interviews list (US-28): carries enough application
 * context (company/job title) that {@code InterviewTable} never needs a
 * second round-trip just to link back to the application.
 */
public record InterviewSummary(
        UUID id,
        UUID applicationId,
        String companyName,
        String jobTitle,
        short roundNumber,
        InterviewRoundType roundType,
        Instant scheduledAt,
        Instant completedAt,
        Interview.Mode mode,
        Interview.Outcome outcome,
        boolean hasFeedback,
        Instant createdAt) {
}
