package com.cohort.api.service.analytics;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * US-45: applications and interviews plotted over time, weekly buckets
 * (Monday-start ISO weeks), as a concrete {@link AnalyticsMetric} subclass.
 * Like {@link FunnelMetric}, this is a set of raw counts rather than a
 * rate, so {@link #minimumSampleSize()} is zero -- FR-67's low-confidence
 * gate protects against a misleadingly precise ratio, not against a
 * user honestly having no activity yet (US-45's own acceptance criterion
 * requires "an empty state for users with no history," which is exactly
 * what {@code sampleSize == 0} with {@code sufficientData == true} lets the
 * frontend render, distinct from a suppressed rate).
 */
public class ActivityTimelineMetric extends AnalyticsMetric<ActivityTimelineMetric.Input, List<ActivityTimelineMetric.WeekBucket>> {

    public record Input(List<LocalDate> applicationDates, List<LocalDate> interviewDates, LocalDate rangeStart, LocalDate rangeEnd) {
    }

    public record WeekBucket(LocalDate weekStart, long applications, long interviews) {
    }

    @Override
    protected int sampleSize(Input input) {
        return input.applicationDates().size() + input.interviewDates().size();
    }

    @Override
    protected List<WeekBucket> compute(Input input) {
        List<WeekBucket> buckets = new ArrayList<>();
        LocalDate cursor = mondayOf(input.rangeStart());
        LocalDate lastWeek = mondayOf(input.rangeEnd());

        while (!cursor.isAfter(lastWeek)) {
            LocalDate weekEndExclusive = cursor.plusWeeks(1);
            long applications = countInWeek(input.applicationDates(), cursor, weekEndExclusive);
            long interviews = countInWeek(input.interviewDates(), cursor, weekEndExclusive);
            buckets.add(new WeekBucket(cursor, applications, interviews));
            cursor = weekEndExclusive;
        }
        return buckets;
    }

    @Override
    protected int minimumSampleSize() {
        return 0;
    }

    private long countInWeek(List<LocalDate> dates, LocalDate weekStartInclusive, LocalDate weekEndExclusive) {
        return dates.stream()
                .filter(date -> !date.isBefore(weekStartInclusive) && date.isBefore(weekEndExclusive))
                .count();
    }

    private LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
