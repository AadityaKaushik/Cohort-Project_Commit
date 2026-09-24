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

/**
 * US-18 / §20.7's stall flag: "an application is stalled when
 * {@code days_in_stage > max(user_median_dwell[stage] x 1.5, system_default[stage])},
 * requiring &gt;=5 observations for the personal median; otherwise the
 * system default applies." The personal-median half of that comparison is
 * computed by {@link StageDwellTimeMetric} -- the exact same {@link AnalyticsMetric}
 * Template Method subclass P34 built for US-48's dwell-time medians, reused
 * here rather than reimplemented, so the "&gt;=5 observations" gate is the
 * same {@code final evaluate()} skeleton every other metric in this
 * codebase is bound by, not a bespoke threshold check.
 */
@Service
public class StallDetectionService {

    /**
     * §20.7 doesn't pin numeric system defaults (only that one must exist
     * "when history is insufficient," FR-26) -- these are this codebase's
     * documented choice: roughly how long a candidate would typically wait
     * at each stage before following up is reasonable, front-loaded for
     * early stages where employers are slower, tightening as a candidate
     * gets closer to an offer.
     */
    static final Map<ApplicationStatus, Double> SYSTEM_DEFAULT_DWELL_DAYS = new EnumMap<>(Map.of(
            ApplicationStatus.SAVED, 30.0,
            ApplicationStatus.APPLIED, 14.0,
            ApplicationStatus.OA, 10.0,
            ApplicationStatus.INTERVIEW, 10.0,
            ApplicationStatus.FINAL_ROUND, 7.0,
            ApplicationStatus.OFFER, 7.0));

    /** §20.7: "requiring >= 5 observations for the personal median" -- the same threshold {@link AnalyticsMetric#DEFAULT_MINIMUM_SAMPLE_SIZE} enforces everywhere else. */
    static final double PERSONAL_MEDIAN_MULTIPLIER = 1.5;

    public static final String BASIS_PERSONAL = "personal";
    public static final String BASIS_SYSTEM_DEFAULT = "system_default";

    public record StallEvaluation(boolean stalled, double daysInStage, double thresholdDays, String basis, int personalSampleSize) {
    }

    /**
     * {@code stageEnteredAt} identifies <em>which</em> stay in the current
     * stage is stalled -- P59's {@code StalledApplicationEvent} uses it so
     * one stall is reported once, not once per detection run.
     */
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

    /**
     * The pure decision at the heart of US-18, isolated from any repository
     * so it can be unit-tested directly against every combination of
     * inputs: given how long an application has sat in its current stage,
     * this user's own historical median dwell for that stage (and how many
     * observations that median is based on), and the system default for
     * the stage, decide whether it's stalled and which basis was used.
     */
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

    /**
     * US-18: every one of this user's non-terminal applications that is
     * currently stalled, scoped end to end by {@code userId} (FR-94/95).
     * Personal medians are computed once, across this user's whole history,
     * and reused for every application sitting in that same stage.
     */
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
