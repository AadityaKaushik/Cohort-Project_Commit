package com.cohort.api.dto;

import com.cohort.api.validation.MeaningfulText;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewFeedbackRequest(
        @Min(1) @Max(5) Short selfRating,
        @Size(max = 5000) @MeaningfulText String whatWentWell,
        @Size(max = 5000) @MeaningfulText String whatWentPoorly,
        @Size(max = 5000) @MeaningfulText String improvementAreas,
        @Size(max = 5000) @MeaningfulText String externalFeedback,
        @NotNull @JsonProperty("is_draft") Boolean isDraft) {
}
