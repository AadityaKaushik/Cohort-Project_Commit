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

public class AnalyticsSummaryCalculator {
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
