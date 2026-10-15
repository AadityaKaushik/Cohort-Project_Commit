package com.cohort.api.dto;

import com.cohort.api.entity.ExperienceContribution.RoundType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContributionRoundInput(
        @NotNull RoundType roundType,
        @Min(1) @Max(5) int difficulty) {
}
