package com.cohort.api.service.fl;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns the shared model into a bounded nudge on recommendation ranking. The model predicts the chance the user
 * rates their next round 4 or 5 for confidence. Below an even chance, more severe prep items move up; above it,
 * they move down a little. A zero model predicts exactly 0.5, so it changes nothing.
 */
@Component
public class FederatedLiftCalculator {
    public static final double MIN_MULTIPLIER = 0.75;
    public static final double MAX_MULTIPLIER = 1.25;
    public static final double NEUTRAL_CONFIDENCE = 0.5;

    public double predictedConfidence(FlModelInfo model, FlFeatureVector features) {
        List<Double> parameters = model.parameters();
        double[] x = features.toOrderedArray();
        if (parameters == null || parameters.size() != x.length + 1) {
            return NEUTRAL_CONFIDENCE;
        }
        double z = parameters.get(x.length);
        for (int i = 0; i < x.length; i++) {
            z += parameters.get(i) * x[i];
        }
        if (!Double.isFinite(z)) {
            return NEUTRAL_CONFIDENCE;
        }
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public double adjustmentMultiplier(double predictedConfidence, double candidateSeverity) {
        double raw = 1.0 + (NEUTRAL_CONFIDENCE - predictedConfidence) * candidateSeverity;
        if (!Double.isFinite(raw)) {
            return 1.0;
        }
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, raw));
    }
}
