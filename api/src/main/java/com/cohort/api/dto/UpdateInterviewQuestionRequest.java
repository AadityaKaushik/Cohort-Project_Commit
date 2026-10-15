package com.cohort.api.dto;

import com.cohort.api.entity.InterviewQuestion;
import com.cohort.api.validation.MeaningfulText;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateInterviewQuestionRequest(
        @Size(max = 5000) @MeaningfulText String questionText,
        UUID topicId,
        InterviewQuestion.Category category,
        InterviewQuestion.Difficulty difficulty,
        @Min(1) @Max(5) Short selfRating,
        Integer timeTakenMinutes,
        @Size(max = 10000) @MeaningfulText String notes) {
}
