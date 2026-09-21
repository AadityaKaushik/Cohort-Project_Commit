package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;

/**
 * Strategy pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): one scoring
 * strategy per evidence tier (US-53/54, PROJECT_PLAN.md §21.6). Swapping
 * which strategy applies never touches the caller ({@code RecommendationScorer},
 * P42) -- it just looks up whichever {@link EvidenceTier} {@link EvidenceTierClassifier}
 * assigned. Tiers A-C share one formula (only {@code tier_weight} differs,
 * per §21.6's own definition), implemented once in
 * {@link AbstractWeightedScoringStrategy}; Tier D is a genuinely different
 * algorithm -- see {@link TierDScoringStrategy} -- which is what makes this
 * a real Strategy family rather than one class with a lookup table of
 * constants.
 */
public interface EvidenceTierScoringStrategy {

    EvidenceTier tier();

    /**
     * @param federatedAdjustment §21.6's {@code federated_adjustment}, bounded to [0.75, 1.25] once P52+ wires in
     *                            a real federated signal; fixed at 1.0 everywhere in this sprint slice ("federated
     *                            ranking is off").
     */
    double score(RecommendationCandidate candidate, Instant asOf, double federatedAdjustment);
}
