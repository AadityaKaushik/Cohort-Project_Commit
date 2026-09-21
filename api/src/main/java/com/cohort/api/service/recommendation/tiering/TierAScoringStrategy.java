package com.cohort.api.service.recommendation.tiering;

import org.springframework.stereotype.Component;

/** Tier A -- "Your data": &gt;=5 of the user's own observations. §21.6's formula at full weight (1.0). */
@Component
public class TierAScoringStrategy extends AbstractWeightedScoringStrategy {

    @Override
    public EvidenceTier tier() {
        return EvidenceTier.A;
    }
}
