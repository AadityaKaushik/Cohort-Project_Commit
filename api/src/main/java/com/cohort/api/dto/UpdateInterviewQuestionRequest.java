package com.cohort.api.dto;

import com.cohort.api.entity.InterviewQuestion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record UpdateInterviewQuestionRequest(
        String questionText,
        UUID topicId,
        InterviewQuestion.Category category,
        InterviewQuestion.Difficulty difficulty,
        @Min(1) @Max(5) Short selfRating,
        Integer timeTakenMinutes,
        String notes) {
}
