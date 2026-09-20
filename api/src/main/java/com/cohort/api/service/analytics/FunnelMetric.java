package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.List;

/**
 * US-43 / §20.7's funnel counts, as a concrete {@link AnalyticsMetric}
 * subclass (Template Method, ARCHITECTURE_REVISION_JAVA.md §4.1 -- the
 * same base P29 built and P30's {@code MasteryEngine} deliberately does
 * <em>not</em> use, since a raw count isn't a rate FR-67 gates). Every
 * count here is "ever reached," computed via {@link StageAttainment} from
 * each application's full history, never from current status alone
 * (DR-01) -- an application currently Rejected that once reached Interview
 * still counts in the interview stage.
 *
 * <p>Overrides {@link #minimumSampleSize()} to zero: FR-67's low-confidence
 * gate exists for <em>rates</em> (a ratio can be misleadingly precise from
 * a tiny denominator); a raw count is never misleading about its own size
 * -- "1 application reached Offer" is exactly as honest displayed as
 * computed. {@code sufficientData} is therefore always {@code true} here,
 * which is itself an honest signal, not a bypass of the gate (contrast
 * with {@code AnalyticsMetricTest}'s adversarial subclass, which fakes
 * sufficiency while still reporting an accurate sample size -- this class
 * reports an accurate sample size AND declares its own threshold honestly).
 */
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
