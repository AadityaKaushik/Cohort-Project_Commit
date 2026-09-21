package com.cohort.api.service.recommendation.tiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceTierClassifierTest {

    private final EvidenceTierClassifier classifier = new EvidenceTierClassifier();

    @Test
    void aBaselineCandidateIsAlwaysTierDRegardlessOfObservationCount() {
        RecommendationCandidate baseline = RecommendationCandidate.baseline(0, List.of());

        assertThat(classifier.classify(baseline, false)).isEqualTo(EvidenceTier.D);
        assertThat(classifier.classify(baseline, true)).isEqualTo(EvidenceTier.D);
    }

    @Test
    void fiveOrMoreObservationsIsTierA() {
        RecommendationCandidate candidate = RecommendationCandidate.stalledApplication(
                java.util.UUID.randomUUID(), "Acme", 0.3, 5, List.of());

        assertThat(classifier.classify(candidate, false)).isEqualTo(EvidenceTier.A);
    }

    @Test
    void fewerThanFiveObservationsIsTierB() {
        RecommendationCandidate candidate = RecommendationCandidate.stalledApplication(
                java.util.UUID.randomUUID(), "Acme", 0.3, 2, List.of());

        assertThat(classifier.classify(candidate, false)).isEqualTo(EvidenceTier.B);
    }

    @Test
    void federatedRankingEnabledYieldsTierCForANonBaselineCandidate() {
        RecommendationCandidate candidate = RecommendationCandidate.stalledApplication(
                java.util.UUID.randomUUID(), "Acme", 0.3, 5, List.of());

        assertThat(classifier.classify(candidate, true)).isEqualTo(EvidenceTier.C);
    }

    @Test
    void zeroObservationsIsStillTierBNotExcluded() {
        // A stalled application always carries at least the fact of the stall itself, even with no personal-median
        // sample history -- tiering never excludes a candidate, it only ever grades its confidence.
        RecommendationCandidate candidate = RecommendationCandidate.stalledApplication(
                java.util.UUID.randomUUID(), "Acme", 0.3, 0, List.of());

        assertThat(classifier.classify(candidate, false)).isEqualTo(EvidenceTier.B);
    }
}
