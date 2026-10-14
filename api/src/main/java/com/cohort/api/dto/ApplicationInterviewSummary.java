package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;

public record ApplicationInterviewSummary(
        UUID id,
        short roundNumber,
        InterviewRoundType roundType,
        Instant scheduledAt,
        Instant completedAt,
        Interview.Outcome outcome) {
    public static ApplicationInterviewSummary from(Interview interview) {
        return new ApplicationInterviewSummary(
                interview.getId(),
                interview.getRoundNumber(),
                interview.getRoundType(),
                interview.getScheduledAt(),
                interview.getCompletedAt(),
                interview.getOutcome());
    }
}
