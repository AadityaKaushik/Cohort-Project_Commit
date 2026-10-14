package com.cohort.api.service.recommendation.tiering;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import java.time.Instant;

public interface EvidenceTierScoringStrategy {
    EvidenceTier tier();

    double score(RecommendationCandidate candidate, Instant asOf, double federatedAdjustment);
}
