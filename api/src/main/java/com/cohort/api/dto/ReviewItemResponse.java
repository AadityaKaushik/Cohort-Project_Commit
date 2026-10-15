package com.cohort.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReviewItemResponse(
        UUID topicId,
        String topicName,
        UUID questionId,
        String questionText,
        LocalDate dueDate,
        long daysOverdue,
        Double masteryScore,
        Instant lastPractisedAt,
        String reason) {
}
