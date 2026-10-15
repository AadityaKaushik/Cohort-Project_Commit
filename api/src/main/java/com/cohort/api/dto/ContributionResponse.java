package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContributionResponse(
        UUID id,
        String companySlug,
        String companyName,
        String roleFamily,
        String level,
        int year,
        List<ContributionRoundInput> rounds,
        int overallDifficulty,
        int overallExperience,
        String outcome,
        String tip,
        Instant createdAt) {
}
