package com.cohort.api.service.fl;

/**
 * Adapter pattern (ARCHITECTURE_REVISION_JAVA.md §4.1, P52): the one
 * interface every caller in {@code api/} depends on for talking to
 * fl-service, instead of any caller knowing fl-service's REST contract
 * directly. {@link FlowerServiceAdapter} is the only class that knows the
 * actual shape of {@code GET /fl/model} — if that contract changes, or
 * fl-service is ever replaced by something else entirely, this interface
 * and its one implementation are the only things that change; every caller
 * (P53's circuit breaker, P57's Federated Lift viewer) is unaffected.
 */
public interface FederatedLearningGateway {

    /** US-62: the current global model — version, the round that produced it, and its flat parameter vector. */
    FlModelInfo fetchCurrentModel();
}
