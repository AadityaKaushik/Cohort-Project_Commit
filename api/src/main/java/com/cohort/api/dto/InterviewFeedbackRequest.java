package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InterviewFeedbackRequest(
        @Min(1) @Max(5) Short selfRating,
        String whatWentWell,
        String whatWentPoorly,
        String improvementAreas,
        String externalFeedback,
        @NotNull @JsonProperty("is_draft") Boolean isDraft) {
}
