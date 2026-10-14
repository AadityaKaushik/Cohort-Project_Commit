package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;
import org.springframework.stereotype.Component;

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
