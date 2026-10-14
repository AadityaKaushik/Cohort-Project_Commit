package com.cohort.api.dto;

import com.cohort.api.entity.PreparationTask;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePreparationTaskRequest(
        UUID topicId,
        String title,
        String description,
        LocalDate dueDate,
        Integer estimatedEffortMinutes,
        PreparationTask.Status status) {
}
