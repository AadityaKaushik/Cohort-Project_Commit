package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The community fields describe the role family with the most contributors. They stay null until that family has
 * communityMinContributors distinct contributors. communitySummaries has the same figures per role family.
 */
public record DirectoryCompanyDetail(
        String slug,
        String name,
        String industry,
        String locations,
        String sizeBand,
        String website,
        String summary,
        List<Map<String, Object>> sources,
        LocalDate lastChecked,
        Double communityRating,
        Double communityDifficulty,
        int communityContributors,
        int communityMinContributors,
        List<CommunitySummary> communitySummaries,
        List<DirectoryRolePattern> rolePatterns) {
}
