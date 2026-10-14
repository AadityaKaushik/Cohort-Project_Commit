package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

public record InterviewDetailResponse(
        UUID id,
        UUID applicationId,
        CompanySummary company,
        String jobTitle,
        Instant scheduledAt,
        Instant completedAt,
        short roundNumber,
        InterviewRoundType roundType,
        Integer durationMinutes,
        Interview.Mode mode,
        String interviewerName,
        String interviewerRole,
        Interview.Outcome outcome,
        Short perceivedDifficulty,
        String notes,
        boolean hasFeedback,
        Instant createdAt,
        Instant updatedAt) {
}
