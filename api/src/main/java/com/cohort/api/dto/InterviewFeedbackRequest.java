package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * US-30: {@code PUT /interviews/{id}/feedback} upserts the one debrief per
 * interview. Every content field is optional — DR-07 requires partial
 * records to be allowed — but {@code isDraft} is required so the caller is
 * always explicit about whether this save is a draft or the final
 * submission (a draft never sets {@code submittedAt}).
 */
public record InterviewFeedbackRequest(
        @Min(1) @Max(5) Short selfRating,
        String whatWentWell,
        String whatWentPoorly,
        String improvementAreas,
        String externalFeedback,
        @NotNull @JsonProperty("is_draft") Boolean isDraft) {
}
