package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record InterviewFeedbackResponse(
        UUID id,
        UUID interviewId,
        Short selfRating,
        String whatWentWell,
        String whatWentPoorly,
        String improvementAreas,
        String externalFeedback,
        @JsonProperty("is_draft") boolean isDraft,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt) {
}
