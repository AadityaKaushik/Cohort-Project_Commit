package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RecommendationScorer {
    private static final boolean FEDERATED_RANKING_ENABLED = false;

    private static final double FEDERATED_ADJUSTMENT_DISABLED = 1.0;

    public record ScoredCandidate(RecommendationCandidate candidate, EvidenceTier tier, double score) {
    }

    private final EvidenceTierClassifier classifier;
    private final Map<EvidenceTier, EvidenceTierScoringStrategy> strategiesByTier;

    public RecommendationScorer(EvidenceTierClassifier classifier, List<EvidenceTierScoringStrategy> strategies) {
        this.classifier = classifier;
        this.strategiesByTier = strategies.stream().collect(Collectors.toMap(EvidenceTierScoringStrategy::tier, Function.identity()));
    }

    public ScoredCandidate score(RecommendationCandidate candidate, Instant asOf) {
        EvidenceTier tier = classifier.classify(candidate, FEDERATED_RANKING_ENABLED);
        EvidenceTierScoringStrategy strategy = strategiesByTier.get(tier);
        double score = strategy.score(candidate, asOf, FEDERATED_ADJUSTMENT_DISABLED);
        return new ScoredCandidate(candidate, tier, score);
    }
}
