package com.cohort.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregate for one company and role family. Everything except contributors is null or empty below the minimum.
 * Tips come back in alphabetical order so position says nothing about who wrote them or when.
 */
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
