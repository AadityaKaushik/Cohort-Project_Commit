package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;

/** US-55: "dismiss records a reason." */
public record DismissRecommendationRequest(@NotBlank String reason) {
}
