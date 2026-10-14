package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateInterviewRequest(
        @NotNull UUID applicationId,
        Instant scheduledAt,
        Instant completedAt,
        @NotNull @Min(1) @Max(Short.MAX_VALUE) Short roundNumber,
        @NotNull InterviewRoundType roundType,
        Integer durationMinutes,
        Interview.Mode mode,
        String interviewerName,
        String interviewerRole,
        Interview.Outcome outcome,
        @Min(1) @Max(5) Short perceivedDifficulty,
        String notes) {
}
