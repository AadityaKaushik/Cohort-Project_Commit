package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
