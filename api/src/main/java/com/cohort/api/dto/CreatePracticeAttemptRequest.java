package com.cohort.api.dto;

import com.cohort.api.entity.PracticeAttempt;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** US-37: repeat attempts add rows. */
public record CreatePracticeAttemptRequest(
        @NotNull PracticeAttempt.Status status,
        @Min(1) @Max(5) Short confidence,
        Integer timeTakenMinutes,
        String notes,
        Instant attemptedAt) {
}
