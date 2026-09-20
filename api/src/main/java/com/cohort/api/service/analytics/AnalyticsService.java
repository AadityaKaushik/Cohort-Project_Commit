package com.cohort.api.service.analytics;

import com.cohort.api.dto.ConversionRateView;
import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.dto.TimelineResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.entity.Interview;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.ApplicationStatusHistoryRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-43..45: assembles the per-user analytics views from real repository
 * data and hands the formula work to the {@link com.cohort.api.service.preparation.analytics.AnalyticsMetric}
 * subclasses in this package ({@link FunnelMetric}, {@link ConversionRateMetric},
 * {@link ActivityTimelineMetric}) -- thin by design (ARCHITECTURE_REVISION_JAVA.md
 * §4.2), like every other service in this codebase: this class's job is
 * fetching and shaping data, never computing a formula itself.
 *
 * <p>Every method takes {@code userId} and scopes every query through it
 * (FR-94/95) -- {@code applicationRepository.findAllByUserId} first, then
 * history rows only for that user's own application IDs.
 */
@Service
public class AnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final InterviewRepository interviewRepository;
    private final FunnelMetric funnelMetric = new FunnelMetric();
    private final ConversionRateMetric conversionRateMetric = new ConversionRateMetric();
    private final ActivityTimelineMetric activityTimelineMetric = new ActivityTimelineMetric();

    public AnalyticsService(
            ApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository historyRepository,
            InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.interviewRepository = interviewRepository;
    }

    @Transactional(readOnly = true)
    public FunnelResponse funnel(UUID userId) {
        List<ApplicationSnapshot> snapshots = loadSnapshots(userId);
        AnalyticsResult<FunnelMetric.FunnelCounts> result = funnelMetric.evaluate(snapshots);
        FunnelMetric.FunnelCounts counts = result.value();
        return new FunnelResponse(
                counts.applied(), counts.reachedOa(), counts.reachedInterview(),
                counts.reachedFinal(), counts.reachedOffer(), result.sampleSize());
    }

    @Transactional(readOnly = true)
    public ConversionRatesResponse conversions(UUID userId) {
        List<ApplicationSnapshot> snapshots = loadSnapshots(userId);

        long appliedResolved = resolvedAtStage(snapshots, ApplicationStatus.APPLIED);
        long reachedOaResolved = resolvedAtStage(snapshots, ApplicationStatus.OA);
        long reachedInterviewResolved = resolvedAtStage(snapshots, ApplicationStatus.INTERVIEW);
        long reachedFinalResolved = resolvedAtStage(snapshots, ApplicationStatus.FINAL_ROUND);

        long reachedOa = reachedCount(snapshots, ApplicationStatus.OA);
        long reachedInterview = reachedCount(snapshots, ApplicationStatus.INTERVIEW);
        long reachedFinal = reachedCount(snapshots, ApplicationStatus.FINAL_ROUND);
        long reachedOffer = reachedCount(snapshots, ApplicationStatus.OFFER);

        return new ConversionRatesResponse(
                rate(reachedOa, appliedResolved),
                rate(reachedInterview, reachedOaResolved),
                rate(reachedFinal, reachedInterviewResolved),
                rate(reachedOffer, reachedFinalResolved),
                rate(reachedOffer, appliedResolved));
    }

    @Transactional(readOnly = true)
    public TimelineResponse timeline(UUID userId, LocalDate from, LocalDate to) {
        List<Application> applications = applicationRepository.findAllByUserId(userId);
        List<Interview> interviews = interviewRepository.findAllByUserId(userId);

        List<LocalDate> applicationDates = applications.stream().map(Application::getDateApplied).toList();
        List<LocalDate> interviewDates = interviews.stream()
                .filter(interview -> interview.getScheduledAt() != null)
                .map(interview -> interview.getScheduledAt().atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .toList();

        ActivityTimelineMetric.Input input = new ActivityTimelineMetric.Input(applicationDates, interviewDates, from, to);
        AnalyticsResult<List<ActivityTimelineMetric.WeekBucket>> result = activityTimelineMetric.evaluate(input);

        List<TimelineResponse.Bucket> buckets = result.value().stream()
                .map(bucket -> new TimelineResponse.Bucket(bucket.weekStart(), bucket.applications(), bucket.interviews()))
                .toList();
        return new TimelineResponse(buckets, result.sampleSize());
    }

    /** Loads every one of this user's applications plus its full status history, scoped end to end by {@code userId}. */
    List<ApplicationSnapshot> loadSnapshots(UUID userId) {
        List<Application> applications = applicationRepository.findAllByUserId(userId);
        List<UUID> applicationIds = applications.stream().map(Application::getId).toList();

        Map<UUID, List<ApplicationStatusHistory>> historyByApplication = applicationIds.isEmpty()
                ? Map.of()
                : historyRepository.findByApplicationIdIn(applicationIds).stream()
                        .collect(Collectors.groupingBy(ApplicationStatusHistory::getApplicationId));

        return applications.stream()
                .map(application -> new ApplicationSnapshot(
                        application.getId(),
                        application.getStatus(),
                        application.getCurrentStage(),
                        historyByApplication.getOrDefault(application.getId(), List.of())))
                .toList();
    }

    private long reachedCount(List<ApplicationSnapshot> snapshots, ApplicationStatus stage) {
        return snapshots.stream().filter(s -> StageAttainment.reached(s.history(), stage)).count();
    }

    private long resolvedAtStage(List<ApplicationSnapshot> snapshots, ApplicationStatus stage) {
        return snapshots.stream().filter(s -> s.isResolvedAtStage(stage)).count();
    }

    private ConversionRateView rate(long numerator, long denominator) {
        AnalyticsResult<Double> result = conversionRateMetric.evaluate(new ConversionRateMetric.Input(numerator, denominator));
        return ConversionRateView.from(result, numerator, denominator);
    }
}
