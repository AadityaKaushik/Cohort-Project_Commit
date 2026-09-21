package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Tier D -- "Heuristic": rules, no data yet. Genuinely different from the
 * other three tiers, not just a different constant: a
 * {@link com.cohort.api.service.recommendation.RecommendationType#baseline}
 * candidate has no mastery evidence and no dated observation to compute
 * severity or recency from (§21.3: "None -- heuristic, and says so"), so
 * this strategy does not call into {@link AbstractWeightedScoringStrategy}'s
 * formula at all -- it returns a fixed, low, tier-weighted score so a
 * baseline recommendation still surfaces (there being nothing else to show
 * a brand-new user) without ever outranking a real, evidence-backed one.
 */
@Component
public class TierDScoringStrategy implements EvidenceTierScoringStrategy {

    static final double HEURISTIC_BASE_SCORE = 0.5;

    @Override
    public EvidenceTier tier() {
        return EvidenceTier.D;
    }

    @Override
    public double score(RecommendationCandidate candidate, Instant asOf, double federatedAdjustment) {
        return HEURISTIC_BASE_SCORE * candidate.actionability() * tier().weight();
    }
}
