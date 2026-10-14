package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DismissRecommendationRequest(@NotBlank String reason) {
}
