package com.cohort.api.service.fl;

import java.util.List;

public interface FederatedLearningGateway {
    FlModelInfo fetchCurrentModel();

    FlRoundOutcome runRound(int roundNumber, List<String> clientIds);
}
