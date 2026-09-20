package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * US-46 / §20.7: "for each rejected application, the last stage reached
 * before the rejection row" -- which is exactly {@code Application.currentStage}
 * for a rejected application, per that field's own javadoc ("the
 * furthest/last non-terminal stage reached... frozen once status becomes
 * terminal"). A concrete {@link AnalyticsMetric} subclass so FR-67's
 * sample-size gate applies to the distribution as a whole exactly as it
 * does to every other metric here.
 *
 * <p>Deliberately does <em>not</em> compute the {@code no_response} bucket
 * -- that requires "no activity for 60 days," which depends on the current
 * wall-clock time, a comparison this class has no business making inside a
 * pure, testable formula. {@code AnalyticsService} computes it separately
 * and reports it alongside this metric's result, "never merged into
 * rejections" (DR-02) at either layer.
 */
public class RejectionDistributionMetric extends AnalyticsMetric<List<ApplicationStatus>, Map<ApplicationStatus, Long>> {

    @Override
    protected int sampleSize(List<ApplicationStatus> input) {
        return input.size();
    }

    @Override
    protected Map<ApplicationStatus, Long> compute(List<ApplicationStatus> input) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus lastStage : input) {
            counts.merge(lastStage, 1L, Long::sum);
        }
        return counts;
    }
}
