package com.cohort.api.dto;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;

/** FR-67 / §20.7: "every rate returns {value, numerator, denominator, sufficient_data}." {@link AnalyticsResult} carries value/sampleSize/sufficientData already; numerator is folded back in here since the base class's generic result has no concept of one. */
public record ConversionRateView(double value, String formatted, long numerator, long denominator, boolean sufficientData) {

    public static ConversionRateView from(AnalyticsResult<Double> result, long numerator, long denominator) {
        return new ConversionRateView(result.value(), result.formatted(), numerator, denominator, result.sufficientData());
    }
}
