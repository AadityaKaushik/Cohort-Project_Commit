package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationSnapshot(
        UUID applicationId,
        ApplicationStatus status,
        ApplicationStatus currentStage,
        Instant stageEnteredAt,
        List<ApplicationStatusHistory> history) {
    public boolean isResolvedAtStage(ApplicationStatus stage) {
        boolean stillOpenAtThisStage = currentStage == stage && !status.isTerminal();
        return StageAttainment.reached(history, stage) && !stillOpenAtThisStage;
    }
}
