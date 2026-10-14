package com.cohort.api.service.preparation.analytics;

public abstract class AnalyticsMetric<I, V> {
    public static final int DEFAULT_MINIMUM_SAMPLE_SIZE = 5;

    public final AnalyticsResult<V> evaluate(I input) {
        int sampleSize = sampleSize(input);
        V rawValue = compute(input);
        boolean sufficientData = sampleSize >= minimumSampleSize();
        String formatted = format(rawValue, sufficientData);
        return new AnalyticsResult<>(rawValue, formatted, sampleSize, sufficientData);
    }

    protected abstract int sampleSize(I input);

    protected abstract V compute(I input);

    // The sample-size threshold below which {@code sufficientData} is false.
    protected int minimumSampleSize() {
        return DEFAULT_MINIMUM_SAMPLE_SIZE;
    }

    protected String format(V value, boolean sufficientData) {
        return String.valueOf(value);
    }
}
