package com.cohort.api.dto;

import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Question;
import java.util.UUID;

/** PATCH semantics: only non-null fields are applied (US-36, own questions only). */
public record UpdateQuestionRequest(UUID topicId, String questionText, Question.Category category, Difficulty difficulty) {
}
