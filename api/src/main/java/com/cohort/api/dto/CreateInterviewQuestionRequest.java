package com.cohort.api.dto;

import com.cohort.api.entity.InterviewQuestion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInterviewQuestionRequest(
        @NotBlank String questionText,
        UUID topicId,
        @NotNull InterviewQuestion.Category category,
        InterviewQuestion.Difficulty difficulty,
        @Min(1) @Max(5) Short selfRating,
        Integer timeTakenMinutes,
        String notes) {
}
