package com.cohort.api.service.recommendation.tiering;

import org.springframework.stereotype.Component;

@Component
public class TierAScoringStrategy extends AbstractWeightedScoringStrategy {
    @Override
    public EvidenceTier tier() {
        return EvidenceTier.A;
    }
}
