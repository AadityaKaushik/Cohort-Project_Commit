package com.cohort.api.dto;

import com.cohort.api.entity.PreparationTask;
import java.time.LocalDate;
import java.util.UUID;

/** PATCH semantics: only non-null fields are applied. Setting {@code status} to {@code completed} stamps {@code completedAt} (US-40: "completion updates progress"). */
public record UpdatePreparationTaskRequest(
        UUID topicId,
        String title,
        String description,
        LocalDate dueDate,
        Integer estimatedEffortMinutes,
        PreparationTask.Status status) {
}
