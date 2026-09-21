package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Combines {@link EvidenceTierClassifier} with whichever
 * {@link EvidenceTierScoringStrategy} matches the assigned tier -- the one
 * place the Strategy family is actually selected from and invoked, so
 * {@code RecommendationFacade} (P42) never has to know there are four of
 * them.
 */
@Component
public class RecommendationScorer {

    /** Federated ranking has no signal to gate on until P52+; always off in this sprint slice. */
    private static final boolean FEDERATED_RANKING_ENABLED = false;

    /** §21.6: "federated_adjustment = 1.0 when federated ranking is off." */
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
