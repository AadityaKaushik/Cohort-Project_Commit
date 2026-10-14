package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;

public record UpdateInterviewRequest(
        Instant scheduledAt,
        Instant completedAt,
        @Min(1) @Max(Short.MAX_VALUE) Short roundNumber,
        InterviewRoundType roundType,
        Integer durationMinutes,
        Interview.Mode mode,
        String interviewerName,
        String interviewerRole,
        Interview.Outcome outcome,
        @Min(1) @Max(5) Short perceivedDifficulty,
        String notes) {
}
