package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
