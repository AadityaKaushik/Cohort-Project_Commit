package com.cohort.api.dto;

import com.cohort.api.entity.PreparationTask;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PreparationTaskResponse(
        UUID id,
        UUID topicId,
        String topicName,
        String title,
        String description,
        LocalDate dueDate,
        Integer estimatedEffortMinutes,
        PreparationTask.Status status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
