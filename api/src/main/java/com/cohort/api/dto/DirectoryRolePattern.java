package com.cohort.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DirectoryRolePattern(
        String roleFamily,
        String level,
        List<Map<String, Object>> rounds,
        List<String> topicSlugs,
        String notes,
        String experienceSummary,
        Map<String, Object> pay,
        Integer difficultyLevel,
        String difficultyBasis,
        List<Map<String, Object>> sources,
        LocalDate lastChecked) {
}
