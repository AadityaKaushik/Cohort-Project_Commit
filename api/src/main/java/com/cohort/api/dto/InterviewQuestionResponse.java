package com.cohort.api.dto;

import com.cohort.api.entity.InterviewQuestion;
import java.time.Instant;
import java.util.UUID;

public record InterviewQuestionResponse(
        UUID id,
        UUID interviewId,
        String questionText,
        UUID topicId,
        InterviewQuestion.Category category,
        InterviewQuestion.Difficulty difficulty,
        Short selfRating,
        Integer timeTakenMinutes,
        UUID linkedQuestionId,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
