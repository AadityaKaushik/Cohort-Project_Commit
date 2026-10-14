package com.cohort.api.dto;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;

public record ConversionRateView(
        double value, String formatted, long numerator, long denominator, long stillOpen, boolean sufficientData) {
    public static ConversionRateView from(
            AnalyticsResult<Double> result, long numerator, long denominator, long stillOpen) {
        return new ConversionRateView(
                result.value(), result.formatted(), numerator, denominator, stillOpen, result.sufficientData());
    }
}
