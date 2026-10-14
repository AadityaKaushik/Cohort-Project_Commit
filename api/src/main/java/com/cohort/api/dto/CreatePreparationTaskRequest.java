package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePreparationTaskRequest(
        @NotNull UUID topicId,
        @NotBlank String title,
        String description,
        LocalDate dueDate,
        Integer estimatedEffortMinutes) {
}
