package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import org.springframework.stereotype.Component;

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
