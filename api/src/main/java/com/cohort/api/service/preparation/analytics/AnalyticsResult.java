package com.cohort.api.service.preparation.analytics;

public record AnalyticsResult<V>(V value, String formatted, int sampleSize, boolean sufficientData) {
}
