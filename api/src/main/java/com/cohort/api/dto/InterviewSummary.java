package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

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
