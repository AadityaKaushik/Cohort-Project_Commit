package com.cohort.api.service.fl;

import java.util.List;

public record FlLedgerEntryData(
        String clientId,
        int schemaVersion,
        double preClipNorm,
        double postClipNorm,
        double noiseMultiplier,
        double epsilon,
        List<Double> transmittedVector) {
}
