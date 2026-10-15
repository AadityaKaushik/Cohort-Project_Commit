package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** communityRating stays null until contribution aggregates exist; the UI shows the minimum contributor count. */
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
        int communityMinContributors,
        List<DirectoryRolePattern> rolePatterns) {
}
