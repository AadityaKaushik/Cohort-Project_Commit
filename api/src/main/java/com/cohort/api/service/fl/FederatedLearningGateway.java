package com.cohort.api.service.fl;

import java.util.List;

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

    /**
     * US-61/US-63 (P55 extends the Adapter introduced in P52): runs one
     * privacy-ledger round over the given (currently opted-in) clients'
     * exported features, and returns the real per-client
     * post-clip-post-noise vectors plus round metadata — what
     * {@code PrivacyLedgerService} persists. {@code clientIds} is what makes
     * a withdrawal (US-64) exclude a stale, already-exported vector.
     */
    FlRoundOutcome runRound(int roundNumber, List<String> clientIds);
}
