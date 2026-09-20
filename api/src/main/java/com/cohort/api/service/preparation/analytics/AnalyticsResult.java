package com.cohort.api.service.preparation.analytics;

/**
 * The one and only output shape an {@link AnalyticsMetric} produces. Every
 * field here is assigned exclusively inside {@link AnalyticsMetric#evaluate}
 * — never by a subclass — which is what makes {@code sufficientData}
 * trustworthy: it always reflects the base class's own sample-size check,
 * regardless of what a subclass's {@code compute}/{@code format} steps
 * return (FR-67).
 */
public record AnalyticsResult<V>(V value, String formatted, int sampleSize, boolean sufficientData) {
}
