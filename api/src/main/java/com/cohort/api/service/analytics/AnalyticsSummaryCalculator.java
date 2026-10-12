package com.cohort.api.service.analytics;

import com.cohort.api.dto.ConversionRateView;
import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.dto.RejectionDistributionResponse;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The §20.7 analytics formulas applied to an arbitrary set of
 * {@link ApplicationSnapshot}s, with no repository access at all. Extracted
 * from {@link AnalyticsService} (P63) so that the in-app analytics endpoints
 * and the exported report (FR-103: "reusing the exact same formulas and
 * sample-size gating as the in-app analytics") call literally the same code:
 * {@code AnalyticsService} passes a user's whole history, the report passes
 * the date-range-filtered subset. Every number still comes from the
 * {@link com.cohort.api.service.preparation.analytics.AnalyticsMetric}
 * subclasses, so every rate keeps its Template-Method-enforced sample-size gate.
 */
public class AnalyticsSummaryCalculator {

    /** §20.7: "no activity for 60 days" for the no_response bucket; {@code stageEnteredAt} is the last-activity proxy (there is no separate activity log). */
    static final long NO_RESPONSE_THRESHOLD_DAYS = 60;

    private final FunnelMetric funnelMetric = new FunnelMetric();
    private final ConversionRateMetric conversionRateMetric = new ConversionRateMetric();
    private final RejectionDistributionMetric rejectionDistributionMetric = new RejectionDistributionMetric();
    private final StageDwellTimeMetric stageDwellTimeMetric = new StageDwellTimeMetric();

    public FunnelResponse funnel(List<ApplicationSnapshot> snapshots) {
        AnalyticsResult<FunnelMetric.FunnelCounts> result = funnelMetric.evaluate(snapshots);
        FunnelMetric.FunnelCounts counts = result.value();
        return new FunnelResponse(
                counts.applied(), counts.reachedOa(), counts.reachedInterview(),
                counts.reachedFinal(), counts.reachedOffer(), result.sampleSize());
    }

    /** The applied population is the funnel's own definition (status other than saved), so every card reconciles with the funnel counts. */
    public ConversionRatesResponse conversions(List<ApplicationSnapshot> snapshots) {
        long reachedApplied = snapshots.stream().filter(s -> s.status() != ApplicationStatus.SAVED).count();
        long reachedOa = reachedCount(snapshots, ApplicationStatus.OA);
        long reachedInterview = reachedCount(snapshots, ApplicationStatus.INTERVIEW);
        long reachedFinal = reachedCount(snapshots, ApplicationStatus.FINAL_ROUND);
        long reachedOffer = reachedCount(snapshots, ApplicationStatus.OFFER);

        long appliedResolved = snapshots.stream()
                .filter(s -> s.status() != ApplicationStatus.SAVED)
                .filter(s -> !(s.currentStage() == ApplicationStatus.APPLIED && !s.status().isTerminal()))
                .count();
        long oaResolved = resolvedAtStage(snapshots, ApplicationStatus.OA);
        long interviewResolved = resolvedAtStage(snapshots, ApplicationStatus.INTERVIEW);
        long finalResolved = resolvedAtStage(snapshots, ApplicationStatus.FINAL_ROUND);

        return new ConversionRatesResponse(
                rate(reachedOa, appliedResolved, reachedApplied - appliedResolved),
                rate(reachedInterview, oaResolved, reachedOa - oaResolved),
                rate(reachedFinal, interviewResolved, reachedInterview - interviewResolved),
                rate(reachedOffer, finalResolved, reachedFinal - finalResolved),
                rate(reachedOffer, appliedResolved, reachedApplied - appliedResolved));
    }

    public RejectionDistributionResponse rejectionDistribution(List<ApplicationSnapshot> snapshots, Instant now) {
        List<ApplicationStatus> lastStages = snapshots.stream()
                .filter(s -> s.status() == ApplicationStatus.REJECTED)
                .map(ApplicationSnapshot::currentStage)
                .toList();
        AnalyticsResult<Map<ApplicationStatus, Long>> result = rejectionDistributionMetric.evaluate(lastStages);

        Instant noResponseCutoff = now.minus(NO_RESPONSE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        long noResponseCount = snapshots.stream()
                .filter(s -> !s.status().isTerminal() && s.stageEnteredAt().isBefore(noResponseCutoff))
                .count();

        return new RejectionDistributionResponse(result.value(), noResponseCount, result.sampleSize(), result.sufficientData());
    }

    public DwellTimesResponse dwellTimes(List<ApplicationSnapshot> snapshots) {
        StageDwellCalculator.DwellData dwellData = StageDwellCalculator.compute(snapshots);

        Map<ApplicationStatus, DwellTimesResponse.DwellView> byStage = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus stage : StageAttainment.pipelineStages()) {
            List<Double> durations = dwellData.completedDurationDays().getOrDefault(stage, List.of());
            AnalyticsResult<StageDwellTimeMetric.DwellStats> result = stageDwellTimeMetric.evaluate(durations);
            long censoredCount = dwellData.censoredCounts().getOrDefault(stage, 0L);

            byStage.put(stage, new DwellTimesResponse.DwellView(
                    result.value().medianDays(), result.value().p25Days(), result.value().p75Days(),
                    result.sampleSize(), censoredCount, result.sufficientData()));
        }
        return new DwellTimesResponse(byStage);
    }

    private long reachedCount(List<ApplicationSnapshot> snapshots, ApplicationStatus stage) {
        return snapshots.stream().filter(s -> StageAttainment.reached(s.history(), stage)).count();
    }

    private long resolvedAtStage(List<ApplicationSnapshot> snapshots, ApplicationStatus stage) {
        return snapshots.stream().filter(s -> s.isResolvedAtStage(stage)).count();
    }

    private ConversionRateView rate(long numerator, long denominator, long stillOpen) {
        AnalyticsResult<Double> result = conversionRateMetric.evaluate(new ConversionRateMetric.Input(numerator, denominator));
        return ConversionRateView.from(result, numerator, denominator, stillOpen);
    }
}
