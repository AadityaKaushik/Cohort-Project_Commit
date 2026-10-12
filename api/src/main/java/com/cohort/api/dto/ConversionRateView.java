package com.cohort.api.dto;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;

/**
 * FR-67 / §20.7: "every rate returns {value, numerator, denominator, sufficient_data}." {@link AnalyticsResult}
 * carries value/sampleSize/sufficientData already; numerator is folded back in here since the base class's generic
 * result has no concept of one. {@code stillOpen} counts the applications that reached the source stage and are
 * still waiting there: they are in neither numerator nor denominator, so denominator + stillOpen is the funnel
 * count for the source stage and numerator is the funnel count for the destination stage.
 */
public record ConversionRateView(
        double value, String formatted, long numerator, long denominator, long stillOpen, boolean sufficientData) {

    public static ConversionRateView from(
            AnalyticsResult<Double> result, long numerator, long denominator, long stillOpen) {
        return new ConversionRateView(
                result.value(), result.formatted(), numerator, denominator, stillOpen, result.sufficientData());
    }
}
