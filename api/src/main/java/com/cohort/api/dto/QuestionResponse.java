package com.cohort.api.dto;

import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Question;
import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID topicId,
        String topicName,
        String questionText,
        Question.Category category,
        Difficulty difficulty,
        boolean isSystem,
        UUID sourceQuestionId,
        Instant createdAt,
        Instant updatedAt) {
}
