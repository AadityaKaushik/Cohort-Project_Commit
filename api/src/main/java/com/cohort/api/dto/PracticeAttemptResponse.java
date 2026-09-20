package com.cohort.api.dto;

import com.cohort.api.entity.PracticeAttempt;
import java.time.Instant;
import java.util.UUID;

public record PracticeAttemptResponse(
        UUID id,
        UUID questionId,
        PracticeAttempt.Status status,
        Short confidence,
        Integer timeTakenMinutes,
        String notes,
        Instant attemptedAt,
        Instant createdAt) {
}
