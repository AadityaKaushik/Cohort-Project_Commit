package com.cohort.api.service.analytics;

import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.dto.RejectionDistributionResponse;
import com.cohort.api.dto.TimelineResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Interview;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationSnapshotLoader snapshotLoader;
    private final InterviewRepository interviewRepository;
    private final AnalyticsSummaryCalculator calculator = new AnalyticsSummaryCalculator();
    private final ActivityTimelineMetric activityTimelineMetric = new ActivityTimelineMetric();

    public AnalyticsService(
            ApplicationRepository applicationRepository,
            ApplicationSnapshotLoader snapshotLoader,
            InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.snapshotLoader = snapshotLoader;
        this.interviewRepository = interviewRepository;
    }

    @Transactional(readOnly = true)
    public FunnelResponse funnel(UUID userId) {
        return calculator.funnel(snapshotLoader.load(userId));
    }

    @Transactional(readOnly = true)
    public ConversionRatesResponse conversions(UUID userId) {
        return calculator.conversions(snapshotLoader.load(userId));
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

    @Transactional(readOnly = true)
    public RejectionDistributionResponse rejectionDistribution(UUID userId) {
        return calculator.rejectionDistribution(snapshotLoader.load(userId), Instant.now());
    }

    @Transactional(readOnly = true)
    public DwellTimesResponse dwellTimes(UUID userId) {
        return calculator.dwellTimes(snapshotLoader.load(userId));
    }
}
