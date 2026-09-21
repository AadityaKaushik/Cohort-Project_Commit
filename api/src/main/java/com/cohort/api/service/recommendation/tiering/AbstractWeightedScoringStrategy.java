package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Duration;
import java.time.Instant;

/**
 * Shared formula for Tiers A-C (PROJECT_PLAN.md §21.6):
 * {@code score = severity x recency x actionability x tier_weight x federated_adjustment},
 * with {@code recency = exp(-ln2 x days_since_evidence / 45)}. Only
 * {@link EvidenceTierScoringStrategy#tier()} (and therefore {@code tier_weight})
 * differs between {@link TierAScoringStrategy}, {@link TierBScoringStrategy},
 * and {@link TierCScoringStrategy} -- exactly what §21.6 specifies. Tier D
 * does not extend this class: it has no dated evidence to compute recency
 * from at all (see {@link TierDScoringStrategy}).
 */
abstract class AbstractWeightedScoringStrategy implements EvidenceTierScoringStrategy {

    private static final double RECENCY_HALF_LIFE_DAYS = 45.0;
    private static final double LN2 = Math.log(2);

    @Override
    public double score(RecommendationCandidate candidate, Instant asOf, double federatedAdjustment) {
        return candidate.severity() * recency(candidate, asOf) * candidate.actionability() * tier().weight() * federatedAdjustment;
    }

    private double recency(RecommendationCandidate candidate, Instant asOf) {
        if (candidate.lastEvidenceAt() == null) {
            return 0.0;
        }
        double daysSinceEvidence = Math.max(0.0, Duration.between(candidate.lastEvidenceAt(), asOf).toMillis() / 86_400_000.0);
        return Math.exp(-LN2 * daysSinceEvidence / RECENCY_HALF_LIFE_DAYS);
    }
}
