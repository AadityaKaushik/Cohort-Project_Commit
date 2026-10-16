package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Community fields cover the role family with the most contributors and stay null until it reaches communityMinContributors. */
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
