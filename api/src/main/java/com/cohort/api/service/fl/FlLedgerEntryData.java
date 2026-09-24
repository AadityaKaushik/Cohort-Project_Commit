package com.cohort.api.service.fl;

import java.util.List;

/** One client's entry from fl-service's {@code POST /fl/round} response (P55) — {@code clientId} is the Cohort user id, as a string, exactly as fl-service echoes it back. */
public record FlLedgerEntryData(
        String clientId,
        int schemaVersion,
        double preClipNorm,
        double postClipNorm,
        double noiseMultiplier,
        double epsilon,
        List<Double> transmittedVector) {
}
