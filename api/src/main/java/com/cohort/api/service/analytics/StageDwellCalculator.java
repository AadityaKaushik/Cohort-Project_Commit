package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * §20.7's "median dwell time per stage" and "days in stage," shared between
 * {@link StageDwellTimeMetric} (US-48, per-stage medians/IQR across a
 * user's whole history) and P36's stall detector (US-18, the same
 * per-stage completed-transition durations, reused for the personal-median
 * half of the stall threshold).
 *
 * <p>For each application, every consecutive pair of history rows
 * {@code (enter, next)} is one <b>completed</b> transition for the stage
 * {@code enter.toStatus} -- the application entered that stage at
 * {@code enter.changedAt} and left it at {@code next.changedAt}. The very
 * last history row is different: if the application's current status is
 * still non-terminal, it is still sitting in that stage right now, so its
 * dwell time is <b>right-censored</b> (§20.7) -- not a completed
 * observation, excluded from the median, counted separately.
 */
public final class StageDwellCalculator {

    private StageDwellCalculator() {
    }

    public record DwellData(Map<ApplicationStatus, List<Double>> completedDurationDays, Map<ApplicationStatus, Long> censoredCounts) {
    }

    public static DwellData compute(List<ApplicationSnapshot> snapshots) {
        Map<ApplicationStatus, List<Double>> completed = new EnumMap<>(ApplicationStatus.class);
        Map<ApplicationStatus, Long> censored = new EnumMap<>(ApplicationStatus.class);

        for (ApplicationSnapshot snapshot : snapshots) {
            List<ApplicationStatusHistory> ordered = snapshot.history().stream()
                    .sorted(Comparator.comparing(ApplicationStatusHistory::getChangedAt))
                    .toList();

            for (int i = 0; i < ordered.size() - 1; i++) {
                ApplicationStatus stage = ordered.get(i).getToStatus();
                if (!stage.isTerminal()) {
                    double days = daysBetween(ordered.get(i).getChangedAt(), ordered.get(i + 1).getChangedAt());
                    completed.computeIfAbsent(stage, s -> new ArrayList<>()).add(days);
                }
            }

            if (!ordered.isEmpty() && !snapshot.status().isTerminal()) {
                ApplicationStatus currentStage = ordered.get(ordered.size() - 1).getToStatus();
                censored.merge(currentStage, 1L, Long::sum);
            }
        }
        return new DwellData(completed, censored);
    }

    private static double daysBetween(java.time.Instant from, java.time.Instant to) {
        return Duration.between(from, to).toMillis() / 86_400_000.0;
    }
}
