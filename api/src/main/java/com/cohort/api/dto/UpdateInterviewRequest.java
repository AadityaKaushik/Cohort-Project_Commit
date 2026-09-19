package com.cohort.api.dto;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;

/**
 * US-31: partial update — a null field means "leave unchanged" (same
 * convention as {@code UpdateApplicationRequest}/{@code UpdateResumeRequest}).
 * {@code applicationId} is deliberately not editable: an interview never
 * moves to a different application once created.
 */
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
