package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
