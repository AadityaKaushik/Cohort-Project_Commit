package com.cohort.api.dto;

import com.cohort.api.entity.Difficulty;
import java.util.UUID;

public record MockInterviewQuestionResponse(
        UUID questionId,
        String questionText,
        UUID topicId,
        String topicName,
        Difficulty difficulty,
        Double masteryScore,
        String reason) {
}
