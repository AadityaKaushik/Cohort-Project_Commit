package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;

/** Everything the dev federated learning page shows. available is false when fl-service cannot be reached. */
public record FlDemoStatusResponse(
        boolean available,
        String unavailableReason,
        boolean optedIn,
        int yourRatedRounds,
        int minimumRatedRounds,
        Double yourPredictedConfidence,
        int syntheticClients,
        List<Double> noiseLevels,
        Double centralCeiling,
        CurrentModel model,
        List<VersionPoint> versions,
        List<RoundRow> rounds) {

    public record Weight(String feature, double value) {
    }

    public record CurrentModel(int version, int roundNumber, int participantCount, Double checkAccuracy, List<Weight> weights, double intercept) {
    }

    public record VersionPoint(int version, int roundNumber, int participantCount, Double checkAccuracy) {
    }

    public record RoundRow(
            int roundNumber, Instant createdAt, boolean accepted, String reason, int cohortSize, int satOutCount,
            Integer modelVersion, Double checkAccuracy, Double noiseMultiplier) {
    }
}
