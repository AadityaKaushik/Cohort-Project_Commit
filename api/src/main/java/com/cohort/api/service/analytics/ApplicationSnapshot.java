package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The minimal, pure slice of one application's data every analytics metric
 * in this package needs: its identity, current status/stage, when it
 * entered that stage, and its full status history (the only source of
 * truth for "stage ever reached" per §20.7/DR-01). Deliberately not the JPA
 * {@code Application} entity itself -- these metrics are plain, testable,
 * golden-file-pinned functions with no persistence dependency, the same
 * discipline {@code MasteryEngine} follows for the same reason.
 */
public record ApplicationSnapshot(
        UUID applicationId,
        ApplicationStatus status,
        ApplicationStatus currentStage,
        Instant stageEnteredAt,
        List<ApplicationStatusHistory> history) {

    /** §20.7: "an application still awaiting a response is neither a success nor a failure." */
    public boolean isResolved() {
        return status.isTerminal();
    }

    /**
     * Whether this application is "resolved" with respect to the S -&gt;
     * next-stage transition, i.e. eligible for a conversion rate's
     * denominator at stage {@code stage} (§20.7, DR-02/DR-05): it reached
     * {@code stage}, and is not still sitting there, non-terminally,
     * waiting on a response.
     */
    public boolean isResolvedAtStage(ApplicationStatus stage) {
        boolean stillOpenAtThisStage = currentStage == stage && !status.isTerminal();
        return StageAttainment.reached(history, stage) && !stillOpenAtThisStage;
    }
}
