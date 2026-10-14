package com.cohort.api.service.analytics;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
