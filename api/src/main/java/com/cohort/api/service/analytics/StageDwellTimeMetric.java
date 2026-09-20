package com.cohort.api.service.analytics;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * US-48 / §20.7: "median dwell time per stage... completed transitions
 * only... reported with IQR; suppressed below 5 completed observations."
 * A concrete {@link AnalyticsMetric} subclass -- {@link #sampleSize} is the
 * count of <em>completed</em> transitions only (right-censored,
 * still-in-progress applications are excluded per {@link StageDwellCalculator}'s
 * javadoc and never leak into this metric's input), so the inherited,
 * final {@code evaluate()} gates this exactly like every other rate/average
 * in this codebase.
 *
 * <p>PROJECT_PLAN.md does not pin one specific percentile algorithm for
 * "IQR," so this class documents its own, precisely, for reproducibility:
 * Tukey's hinges -- sort the durations, the median is the middle value (or
 * the average of the two middle values for an even count), and the 25th/75th
 * percentiles are the medians of the lower and upper halves respectively
 * (excluding the overall median itself when the count is odd).
 */
public class StageDwellTimeMetric extends AnalyticsMetric<List<Double>, StageDwellTimeMetric.DwellStats> {

    public record DwellStats(double medianDays, double p25Days, double p75Days) {
    }

    @Override
    protected int sampleSize(List<Double> input) {
        return input.size();
    }

    @Override
    protected DwellStats compute(List<Double> input) {
        if (input.isEmpty()) {
            return new DwellStats(0.0, 0.0, 0.0);
        }
        List<Double> sorted = new ArrayList<>(input);
        Collections.sort(sorted);
        int n = sorted.size();

        double median = medianOf(sorted, 0, n);

        boolean odd = n % 2 == 1;
        int lowerEndExclusive = n / 2;
        int upperStart = odd ? n / 2 + 1 : n / 2;

        double p25 = medianOf(sorted, 0, lowerEndExclusive);
        double p75 = medianOf(sorted, upperStart, n);

        return new DwellStats(median, p25, p75);
    }

    private double medianOf(List<Double> sorted, int fromInclusive, int toExclusive) {
        int size = toExclusive - fromInclusive;
        if (size <= 0) {
            return sorted.get(fromInclusive < sorted.size() ? fromInclusive : sorted.size() - 1);
        }
        int mid = fromInclusive + size / 2;
        if (size % 2 == 1) {
            return sorted.get(mid);
        }
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }
}
