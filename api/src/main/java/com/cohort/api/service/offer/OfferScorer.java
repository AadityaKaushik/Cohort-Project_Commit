package com.cohort.api.service.offer;

import com.cohort.api.entity.OfferFactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class OfferScorer {
    public static final int DEFAULT_WEIGHT = 3;
    private static final int MAX_RATING = 5;
    private static final int TIED_WITHIN_POINTS = 1;

    private OfferScorer() {
    }

    public static int weightOf(Map<OfferFactor, Integer> weights, OfferFactor factor) {
        return weights.getOrDefault(factor, DEFAULT_WEIGHT);
    }

    /** Percentage of the best possible weighted rating. Null until every factor is rated. */
    public static Integer score(Map<OfferFactor, Integer> weights, Map<OfferFactor, Integer> ratings) {
        int earned = 0;
        int possible = 0;
        for (OfferFactor factor : OfferFactor.values()) {
            Integer rating = ratings.get(factor);
            if (rating == null) {
                return null;
            }
            int weight = weightOf(weights, factor);
            earned += weight * rating;
            possible += weight * MAX_RATING;
        }
        return (int) Math.round(100.0 * earned / possible);
    }

    public record Ranked(String companyName, Map<OfferFactor, Integer> ratings, int score) {
    }

    /** Plain-language reason the first offer is ahead of the second. Expects the list sorted best first. */
    public static String explainLeader(Map<OfferFactor, Integer> weights, List<Ranked> sorted) {
        if (sorted.size() < 2) {
            return null;
        }
        Ranked leader = sorted.get(0);
        Ranked runnerUp = sorted.get(1);
        int gap = leader.score() - runnerUp.score();
        if (gap <= TIED_WITHIN_POINTS) {
            return leader.companyName() + " and " + runnerUp.companyName()
                    + " are within a point of each other. Your weights and ratings do not separate them.";
        }

        List<OfferFactor> byInfluence = new ArrayList<>(Arrays.asList(OfferFactor.values()));
        byInfluence.sort(Comparator.comparingInt((OfferFactor factor) -> -influence(weights, leader, runnerUp, factor)));

        String favouring = byInfluence.stream()
            .filter(factor -> influence(weights, leader, runnerUp, factor) > 0)
            .limit(2)
            .map(factor -> describe(weights, leader, runnerUp, factor))
            .collect(Collectors.joining(" and "));

        StringBuilder text = new StringBuilder(leader.companyName() + " leads " + runnerUp.companyName()
                + " by " + gap + (gap == 1 ? " point" : " points") + ", mostly on " + favouring + ".");
        OfferFactor against = byInfluence.get(byInfluence.size() - 1);
        if (influence(weights, leader, runnerUp, against) < 0) {
            text.append(" ").append(runnerUp.companyName()).append(" is ahead on ")
                    .append(describe(weights, runnerUp, leader, against)).append(".");
        }
        return text.toString();
    }

    private static int influence(Map<OfferFactor, Integer> weights, Ranked a, Ranked b, OfferFactor factor) {
        return weightOf(weights, factor) * (a.ratings().get(factor) - b.ratings().get(factor));
    }

    private static String describe(Map<OfferFactor, Integer> weights, Ranked ahead, Ranked behind, OfferFactor factor) {
        return label(factor) + " (rated " + ahead.ratings().get(factor) + " against " + behind.ratings().get(factor)
                + ", weight " + weightOf(weights, factor) + ")";
    }

    public static String label(OfferFactor factor) {
        return factor.name().replace('_', ' ');
    }
}
