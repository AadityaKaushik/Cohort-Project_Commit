package com.cohort.api.service.fl;

import java.util.List;

public interface FederatedLearningGateway {
    FlModelInfo fetchCurrentModel();

    FlRoundOutcome runRound(int roundNumber, List<String> clientIds);

    /** A lower noise level is only accepted by fl-service when its demo mode is on. */
    default FlRoundOutcome runRound(int roundNumber, List<String> clientIds, Double noiseMultiplier) {
        return runRound(roundNumber, clientIds);
    }
}
