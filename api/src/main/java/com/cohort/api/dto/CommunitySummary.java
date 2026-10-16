package com.cohort.api.dto;

import java.util.List;
import java.util.Map;

/** Aggregate for one company and role family. Below the minimum contributors, everything else is null or empty. Tips are sorted alphabetically. */
public record CommunitySummary(
        String roleFamily,
        int contributors,
        Double rating,
        Double difficulty,
        List<RoundShare> commonRounds,
        Map<String, Integer> outcomes,
        List<String> tips) {

    public record RoundShare(String roundType, int percent, Double averageDifficulty) {
    }
}
