package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-63: one Privacy Ledger row — round metadata, pre/post-clip norm,
 * noise multiplier, cohort size, epsilon consumed, and the exact
 * transmitted vector, expandable in the UI (P56's {@code VectorInspector}).
 */
public record PrivacyLedgerEntryResponse(
        UUID id,
        int roundNumber,
        int schemaVersion,
        double preClipNorm,
        double postClipNorm,
        double noiseMultiplier,
        int cohortSize,
        double epsilon,
        List<Double> transmittedVector,
        Instant createdAt) {
}
