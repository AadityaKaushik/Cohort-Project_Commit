package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.List;

public class FunnelMetric extends AnalyticsMetric<List<ApplicationSnapshot>, FunnelMetric.FunnelCounts> {
    public record FunnelCounts(long applied, long reachedOa, long reachedInterview, long reachedFinal, long reachedOffer) {
    }

    @Override
    protected int sampleSize(List<ApplicationSnapshot> input) {
        return input.size();
    }

    @Override
    protected FunnelCounts compute(List<ApplicationSnapshot> input) {
        long applied = input.stream().filter(a -> a.status() != ApplicationStatus.SAVED).count();
        long reachedOa = countReaching(input, ApplicationStatus.OA);
        long reachedInterview = countReaching(input, ApplicationStatus.INTERVIEW);
        long reachedFinal = countReaching(input, ApplicationStatus.FINAL_ROUND);
        long reachedOffer = countReaching(input, ApplicationStatus.OFFER);
        return new FunnelCounts(applied, reachedOa, reachedInterview, reachedFinal, reachedOffer);
    }

    @Override
    protected int minimumSampleSize() {
        return 0;
    }

    private long countReaching(List<ApplicationSnapshot> input, ApplicationStatus stage) {
        return input.stream().filter(a -> StageAttainment.reached(a.history(), stage)).count();
    }
}
