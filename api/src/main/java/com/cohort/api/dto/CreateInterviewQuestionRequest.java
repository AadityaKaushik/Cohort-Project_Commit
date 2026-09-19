package com.cohort.api.dto;

import com.cohort.api.entity.InterviewQuestion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** US-29: text and category are required; everything else is optional so a question can be logged in seconds. */
public record CreateInterviewQuestionRequest(
        @NotBlank String questionText,
        UUID topicId,
        @NotNull InterviewQuestion.Category category,
        InterviewQuestion.Difficulty difficulty,
        @Min(1) @Max(5) Short selfRating,
        Integer timeTakenMinutes,
        String notes) {
}
