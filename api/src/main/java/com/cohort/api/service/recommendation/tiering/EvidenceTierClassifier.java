package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import org.springframework.stereotype.Component;

/**
 * Assigns a candidate its {@link EvidenceTier}, per the definitions in
 * PROJECT_PLAN.md's tier table (above §21.6): a {@link RecommendationType#baseline}
 * candidate is always Tier D by definition (it is the "no data yet"
 * generator); everything else is Tier A with &gt;=5 observations or Tier B
 * otherwise. Tier C (federated) is deliberately unreachable in this sprint
 * slice -- {@code federatedRankingEnabled} is always passed {@code false}
 * until P52+ builds a real federated signal to gate on; the branch exists
 * now so wiring it in later is a one-line change here, not a new class.
 */
@Component
public class EvidenceTierClassifier {

    static final int TIER_A_MINIMUM_OBSERVATIONS = 5;

    public EvidenceTier classify(RecommendationCandidate candidate, boolean federatedRankingEnabled) {
        if (candidate.type() == RecommendationType.baseline) {
            return EvidenceTier.D;
        }
        if (federatedRankingEnabled) {
            return EvidenceTier.C;
        }
        return candidate.observationCount() >= TIER_A_MINIMUM_OBSERVATIONS ? EvidenceTier.A : EvidenceTier.B;
    }
}
