package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class StageAttainment {
    private static final Set<ApplicationStatus> PIPELINE_STAGES = EnumSet.of(
            ApplicationStatus.SAVED,
            ApplicationStatus.APPLIED,
            ApplicationStatus.OA,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.FINAL_ROUND,
            ApplicationStatus.OFFER);

    private StageAttainment() {
    }

    public static boolean reached(List<ApplicationStatusHistory> history, ApplicationStatus stage) {
        return history.stream()
                .map(ApplicationStatusHistory::getToStatus)
                .filter(PIPELINE_STAGES::contains)
                .anyMatch(reachedStatus -> reachedStatus.ordinal() >= stage.ordinal());
    }

    public static Set<ApplicationStatus> pipelineStages() {
        return PIPELINE_STAGES;
    }
}
