package com.cohort.api.service.analytics;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;

/**
 * US-44 / §20.7's five stage-to-stage conversion rates, all computed by
 * this one concrete {@link AnalyticsMetric} subclass -- each rate is
 * "reached next stage &divide; reached this stage, over resolved
 * applications only" (numerator/denominator pair), so one formula
 * evaluated five times with five different {@link Input}s (built by
 * {@code AnalyticsService}) covers {@code application_to_oa},
 * {@code oa_to_interview}, {@code interview_to_final},
 * {@code final_to_offer}, and {@code overall_success} without five nearly
 * identical classes.
 *
 * <p>{@link #sampleSize} returns the denominator, so FR-67's rule --
 * "rates from fewer than 5 observations are marked low confidence" -- is
 * enforced by the inherited, {@code final} {@link AnalyticsMetric#evaluate}
 * exactly as it is for every other metric in this codebase, never by this
 * class deciding for itself.
 */
public class ConversionRateMetric extends AnalyticsMetric<ConversionRateMetric.Input, Double> {

    /** {@code denominator == 0} means "no resolved observations at all" -- not a division by zero to guard against ad hoc, but a legitimate zero-sample case the gate already handles (sampleSize 0 &lt; 5 -&gt; insufficient). */
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
