package com.cohort.api.service.recommendation.tiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationScorerTest {
    private final RecommendationScorer scorer =
            new RecommendationScorer(new EvidenceTierClassifier(), List.of(
                    new TierAScoringStrategy(), new TierBScoringStrategy(), new TierCScoringStrategy(), new TierDScoringStrategy()));

    @Test
    void tierAScoreMatchesTheDocumentedFormulaAtFreshEvidence() {
        Instant asOf = Instant.parse("2026-06-01T00:00:00Z");
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.3, 0.3, 5, true, asOf);
        RecommendationCandidate candidate = RecommendationCandidate.weakTopic(snapshot, List.of());

        RecommendationScorer.ScoredCandidate result = scorer.score(candidate, asOf);

        assertThat(result.tier()).isEqualTo(EvidenceTier.A);
        assertThat(result.score()).isCloseTo(0.7, offset(1e-9));
    }

    @Test
    void scoreDecaysAsEvidenceAges() {
        Instant evidenceAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant asOf = evidenceAt.plus(45, ChronoUnit.DAYS);
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.3, 0.3, 5, true, evidenceAt);
        RecommendationCandidate candidate = RecommendationCandidate.weakTopic(snapshot, List.of());

        RecommendationScorer.ScoredCandidate result = scorer.score(candidate, asOf);

        assertThat(result.score()).isCloseTo(0.35, offset(1e-6));
    }

    @Test
    void tierBScoresLowerThanTierAForOtherwiseIdenticalEvidence() {
        Instant asOf = Instant.parse("2026-06-01T00:00:00Z");
        TopicMasterySnapshot tierASnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.3, 0.3, 5, true, asOf);
        TopicMasterySnapshot tierBSnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.3, 0.3, 3, true, asOf);

        double tierAScore = scorer.score(RecommendationCandidate.weakTopic(tierASnapshot, List.of()), asOf).score();
        double tierBScore = scorer.score(RecommendationCandidate.weakTopic(tierBSnapshot, List.of()), asOf).score();

        assertThat(tierBScore).isLessThan(tierAScore);
    }

    @Test
    void tierDIgnoresSeverityAndRecencyEntirely() {
        RecommendationCandidate baseline = RecommendationCandidate.baseline(0, List.of());
        Instant asOf = Instant.now();

        RecommendationScorer.ScoredCandidate result = scorer.score(baseline, asOf);

        assertThat(result.tier()).isEqualTo(EvidenceTier.D);
        assertThat(result.score()).isCloseTo(0.12, offset(1e-9));
    }

    @Test
    void aCandidateWithNoDatedEvidenceScoresZeroUnderTheWeightedStrategies() {
        RecommendationCandidate candidate = RecommendationCandidate.stalledApplication(
                UUID.randomUUID(), "Acme Corp — Backend Engineer", 0.5, 5, List.of());

        RecommendationScorer.ScoredCandidate result = scorer.score(candidate, Instant.now());

        assertThat(result.score()).isZero();
    }
}
