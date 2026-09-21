package com.cohort.api.service.recommendation.tiering;

import org.springframework.stereotype.Component;

/**
 * Tier C -- "Federated": global model, cohort &gt;= k. Same §21.6 formula as
 * A/B at 0.6 weight, but this is the one tier whose {@code federated_adjustment}
 * parameter is ever meant to be anything other than 1.0. Unreachable via
 * {@link EvidenceTierClassifier} until P52+ exists to supply a real
 * federated signal and gate this branch on -- present now so that day's
 * change is "flip a flag," not "design a new strategy."
 */
@Component
public class TierCScoringStrategy extends AbstractWeightedScoringStrategy {

    @Override
    public EvidenceTier tier() {
        return EvidenceTier.C;
    }
}
