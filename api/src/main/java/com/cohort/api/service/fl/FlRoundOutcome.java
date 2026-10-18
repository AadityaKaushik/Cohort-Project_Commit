package com.cohort.api.service.fl;

import java.util.List;

/**
 * cohortSize counts clients that trained and sent an update; satOutCount counts those with too few rated rounds.
 * modelVersion is the version serving after the round, and checkAccuracy is its score on the synthetic check set.
 */
public record FlRoundOutcome(
        int roundNumber, boolean accepted, String reason, int cohortSize, List<FlLedgerEntryData> entries,
        int satOutCount, Integer modelVersion, Double checkAccuracy) {

    public FlRoundOutcome(int roundNumber, boolean accepted, String reason, int cohortSize, List<FlLedgerEntryData> entries) {
        this(roundNumber, accepted, reason, cohortSize, entries, 0, null, null);
    }
}
