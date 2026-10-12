package com.cohort.api.service.report;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.analytics.AnalyticsSummaryCalculator;
import com.cohort.api.service.analytics.ApplicationSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builder pattern (ARCHITECTURE_REVISION_JAVA.md §4.1/§6.2, US-80): assembles
 * a {@link Report} from a user's applications through a fluent chain --
 * {@code .withDateRange(from, to).withApplications().withAnalytics().build()}
 * -- instead of a constructor with a long run of nullable, easily-swapped
 * arguments. Every part is optional except the data and at least one
 * section; {@link #build()} validates the combination once, in one place.
 *
 * <p>The date range filters the applications by {@code date_applied}
 * (inclusive on both ends), and the analytics section is computed over
 * exactly that filtered set by {@link AnalyticsSummaryCalculator} -- the
 * same code the in-app analytics endpoints call (FR-103), so no formula or
 * sample-size gate is reimplemented here.
 *
 * <p>Not a Spring bean and not thread-safe: one builder per report.
 */
public final class ReportBuilder {

    private final String ownerName;
    private final List<ReportApplicationInput> source;
    private final AnalyticsSummaryCalculator calculator = new AnalyticsSummaryCalculator();

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private boolean includeApplications;
    private boolean includeAnalytics;
    private Instant generatedAt;
    private ZoneId zone = ZoneOffset.UTC;
    private String title;

    private ReportBuilder(String ownerName, List<ReportApplicationInput> source) {
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Cohort user" : ownerName;
        this.source = List.copyOf(Objects.requireNonNull(source, "source"));
    }

    /** Starts a report for {@code ownerName} over {@code applications} (all of that one user's applications -- the builder does the filtering). */
    public static ReportBuilder forOwner(String ownerName, List<ReportApplicationInput> applications) {
        return new ReportBuilder(ownerName, applications);
    }

    /** Inclusive on both ends; either bound may be null for an open-ended range. */
    public ReportBuilder withDateRange(LocalDate from, LocalDate to) {
        this.dateFrom = from;
        this.dateTo = to;
        return this;
    }

    public ReportBuilder withApplications() {
        return withApplications(true);
    }

    public ReportBuilder withApplications(boolean include) {
        this.includeApplications = include;
        return this;
    }

    public ReportBuilder withAnalytics() {
        return withAnalytics(true);
    }

    public ReportBuilder withAnalytics(boolean include) {
        this.includeAnalytics = include;
        return this;
    }

    /** Sections implied by a {@link ReportType}; equivalent to the matching {@code with...} calls. */
    public ReportBuilder withSectionsFor(ReportType type) {
        return withApplications(type.includesApplications()).withAnalytics(type.includesAnalytics());
    }

    /** The moment "days in current stage" and the "no response" bucket are measured from, and the zone dates are shown in. Defaults to now, UTC. */
    public ReportBuilder generatedAt(Instant instant, ZoneId zone) {
        this.generatedAt = instant;
        this.zone = zone == null ? ZoneOffset.UTC : zone;
        return this;
    }

    /** Overrides the title otherwise derived from the included sections. */
    public ReportBuilder titled(String title) {
        this.title = title;
        return this;
    }

    public Report build() {
        if (!includeApplications && !includeAnalytics) {
            throw new IllegalStateException("A report needs at least one section: call withApplications() and/or withAnalytics().");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalStateException("The report's date range starts (" + dateFrom + ") after it ends (" + dateTo + ").");
        }
        Instant asOf = generatedAt == null ? Instant.now() : generatedAt;

        List<ReportApplicationInput> inRange = source.stream()
                .filter(this::withinDateRange)
                .sorted(Comparator.comparing(ReportApplicationInput::dateApplied).reversed()
                        .thenComparing(ReportApplicationInput::companyName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ReportApplicationInput::jobTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<Report.ApplicationRow> rows = includeApplications
                ? inRange.stream().map(input -> toRow(input, asOf)).toList()
                : null;
        Report.AnalyticsSummary analytics = includeAnalytics ? analyticsFor(inRange, asOf) : null;

        return new Report(resolveTitle(), ownerName, dateFrom, dateTo, asOf, zone, rows, analytics);
    }

    private boolean withinDateRange(ReportApplicationInput input) {
        LocalDate applied = input.dateApplied();
        return (dateFrom == null || !applied.isBefore(dateFrom)) && (dateTo == null || !applied.isAfter(dateTo));
    }

    private Report.ApplicationRow toRow(ReportApplicationInput input, Instant asOf) {
        ApplicationSnapshot snapshot = input.snapshot();
        Long daysInStage = snapshot.status().isTerminal()
                ? null
                : Math.max(0, Duration.between(snapshot.stageEnteredAt(), asOf).toDays());
        return new Report.ApplicationRow(
                input.companyName(), input.jobTitle(), snapshot.status(), snapshot.currentStage(), input.dateApplied(),
                daysInStage, input.interviewCount(), input.location(), input.workMode(), input.priority(),
                input.source(), input.referral());
    }

    private Report.AnalyticsSummary analyticsFor(List<ReportApplicationInput> inRange, Instant asOf) {
        List<ApplicationSnapshot> snapshots = inRange.stream().map(ReportApplicationInput::snapshot).toList();
        Map<ApplicationStatus, Long> statusBreakdown = new EnumMap<>(ApplicationStatus.class);
        statusBreakdown.putAll(snapshots.stream().collect(Collectors.groupingBy(ApplicationSnapshot::status, Collectors.counting())));
        return new Report.AnalyticsSummary(
                snapshots.size(),
                statusBreakdown,
                calculator.funnel(snapshots),
                calculator.conversions(snapshots),
                calculator.dwellTimes(snapshots),
                calculator.rejectionDistribution(snapshots, asOf));
    }

    private String resolveTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (includeApplications && includeAnalytics) {
            return "Job search progress report";
        }
        return includeApplications ? "Applications report" : "Analytics summary";
    }
}
