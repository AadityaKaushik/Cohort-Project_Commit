package com.cohort.api.service.fl;

import org.springframework.stereotype.Component;

@Component
public class FederatedLiftCalculator {
    public static final double MIN_MULTIPLIER = 0.75;
    public static final double MAX_MULTIPLIER = 1.25;

    public double adjustmentMultiplier(FlModelInfo model, double candidateSeverity) {
        double meanParameter = model.parameters().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double raw = meanParameter * (candidateSeverity - 0.5);
        if (!Double.isFinite(raw)) {
            return 1.0;
        }
        return clamp(1.0 + raw, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }

    private double clamp(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
