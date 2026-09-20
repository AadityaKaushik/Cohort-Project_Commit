package com.cohort.api.dto;

import com.cohort.api.entity.PracticeAttempt;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** US-37: always inserts a new row — repeated attempts are appended, never overwritten. */
public record CreatePracticeAttemptRequest(
        @NotNull PracticeAttempt.Status status,
        @Min(1) @Max(5) Short confidence,
        Integer timeTakenMinutes,
        String notes,
        Instant attemptedAt) {
}
