package com.cohort.api.dto;

import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** US-36: a user's own addition to the question bank — always created with {@code isSystem = false}. */
public record CreateQuestionRequest(
        @NotNull UUID topicId,
        @NotBlank String questionText,
        @NotNull Question.Category category,
        Difficulty difficulty) {
}
