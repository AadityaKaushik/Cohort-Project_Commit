package com.cohort.api.service.analytics;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;

public class ConversionRateMetric extends AnalyticsMetric<ConversionRateMetric.Input, Double> {
    public record Input(long numerator, long denominator) {
    }

    @Override
    protected int sampleSize(Input input) {
        return (int) Math.min(input.denominator(), Integer.MAX_VALUE);
    }

    @Override
    protected Double compute(Input input) {
        if (input.denominator() == 0) {
            return 0.0;
        }
        return input.numerator() / (double) input.denominator();
    }

    @Override
    protected String format(Double value, boolean sufficientData) {
        if (!sufficientData) {
            return "insufficient data";
        }
        return String.format("%.1f%%", value * 100);
    }
}
