package com.cohort.api.service.recommendation.tiering;

import org.springframework.stereotype.Component;

/** Tier B -- "Your data, thin": 2-4 observations. §21.6's formula, discounted to 0.7 weight. */
@Component
public class TierBScoringStrategy extends AbstractWeightedScoringStrategy {

    @Override
    public EvidenceTier tier() {
        return EvidenceTier.B;
    }
}
