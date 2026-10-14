package com.cohort.api.service.recommendation.tiering;

import org.springframework.stereotype.Component;

@Component
public class TierBScoringStrategy extends AbstractWeightedScoringStrategy {
    @Override
    public EvidenceTier tier() {
        return EvidenceTier.B;
    }
}
