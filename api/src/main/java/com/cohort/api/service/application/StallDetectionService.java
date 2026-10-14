package com.cohort.api.service.application;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.Company;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.service.analytics.ApplicationSnapshot;
import com.cohort.api.service.analytics.ApplicationSnapshotLoader;
import com.cohort.api.service.analytics.StageDwellCalculator;
import com.cohort.api.service.analytics.StageDwellTimeMetric;
import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StallDetectionService {
    static final Map<ApplicationStatus, Double> SYSTEM_DEFAULT_DWELL_DAYS = new EnumMap<>(Map.of(
            ApplicationStatus.SAVED, 30.0,
            ApplicationStatus.APPLIED, 14.0,
            ApplicationStatus.OA, 10.0,
            ApplicationStatus.INTERVIEW, 10.0,
            ApplicationStatus.FINAL_ROUND, 7.0,
            ApplicationStatus.OFFER, 7.0));

    static final double PERSONAL_MEDIAN_MULTIPLIER = 1.5;

    public static final String BASIS_PERSONAL = "personal";
    public static final String BASIS_SYSTEM_DEFAULT = "system_default";

    public record StallEvaluation(boolean stalled, double daysInStage, double thresholdDays, String basis, int personalSampleSize) {
    }

    public record StalledApplication(
            UUID applicationId,
            String companyName,
            String jobTitle,
            ApplicationStatus currentStage,
            StallEvaluation evaluation,
            Instant stageEnteredAt) {
    }

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationSnapshotLoader snapshotLoader;
    private final StageDwellTimeMetric stageDwellTimeMetric = new StageDwellTimeMetric();

    public StallDetectionService(
            ApplicationRepository applicationRepository, CompanyRepository companyRepository, ApplicationSnapshotLoader snapshotLoader) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.snapshotLoader = snapshotLoader;
    }

    public static StallEvaluation evaluate(
            double daysInStage, double personalMedianDays, int personalSampleSize, double systemDefaultDays) {
        boolean sufficientPersonalData = personalSampleSize >= AnalyticsMetric.DEFAULT_MINIMUM_SAMPLE_SIZE;
        double threshold = sufficientPersonalData
                ? Math.max(personalMedianDays * PERSONAL_MEDIAN_MULTIPLIER, systemDefaultDays)
                : systemDefaultDays;
        String basis = sufficientPersonalData ? BASIS_PERSONAL : BASIS_SYSTEM_DEFAULT;
        boolean stalled = daysInStage > threshold;
        return new StallEvaluation(stalled, daysInStage, threshold, basis, personalSampleSize);
    }

    @Transactional(readOnly = true)
    public List<StalledApplication> detectStalled(UUID userId) {
        List<ApplicationSnapshot> snapshots = snapshotLoader.load(userId);
        Map<ApplicationStatus, AnalyticsResult<StageDwellTimeMetric.DwellStats>> personalMedianByStage =
                personalMedianByStage(snapshots);

        List<Application> applications = applicationRepository.findAllByUserId(userId);
        Map<UUID, Company> companiesById = new java.util.HashMap<>();

        Instant now = Instant.now();
        return applications.stream()
                .filter(application -> !application.getStatus().isTerminal())
                .map(application -> toStalledApplicationOrNull(application, personalMedianByStage, companiesById, userId, now))
                .filter(java.util.Objects::nonNull)
                .filter(stalled -> stalled.evaluation().stalled())
                .toList();
    }

    private StalledApplication toStalledApplicationOrNull(
            Application application,
            Map<ApplicationStatus, AnalyticsResult<StageDwellTimeMetric.DwellStats>> personalMedianByStage,
            Map<UUID, Company> companiesById,
            UUID userId,
            Instant now) {
        ApplicationStatus stage = application.getCurrentStage();
        AnalyticsResult<StageDwellTimeMetric.DwellStats> personalMedian = personalMedianByStage.get(stage);
        double daysInStage = daysBetween(application.getStageEnteredAt(), now);
        double systemDefault = SYSTEM_DEFAULT_DWELL_DAYS.getOrDefault(stage, 14.0);

        StallEvaluation evaluation = evaluate(
                daysInStage,
                personalMedian == null ? 0.0 : personalMedian.value().medianDays(),
                personalMedian == null ? 0 : personalMedian.sampleSize(),
                systemDefault);

        Company company = companiesById.computeIfAbsent(
                application.getCompanyId(), id -> companyRepository.findByIdAndUserId(id, userId).orElse(null));
        String companyName = company == null ? "Unknown" : company.getName();

        return new StalledApplication(
                application.getId(), companyName, application.getJobTitle(), stage, evaluation, application.getStageEnteredAt());
    }

    private Map<ApplicationStatus, AnalyticsResult<StageDwellTimeMetric.DwellStats>> personalMedianByStage(
            List<ApplicationSnapshot> snapshots) {
        StageDwellCalculator.DwellData dwellData = StageDwellCalculator.compute(snapshots);
        Map<ApplicationStatus, AnalyticsResult<StageDwellTimeMetric.DwellStats>> result = new EnumMap<>(ApplicationStatus.class);
        for (Map.Entry<ApplicationStatus, List<Double>> entry : dwellData.completedDurationDays().entrySet()) {
            result.put(entry.getKey(), stageDwellTimeMetric.evaluate(entry.getValue()));
        }
        return result;
    }

    private double daysBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMillis() / 86_400_000.0;
    }
}
