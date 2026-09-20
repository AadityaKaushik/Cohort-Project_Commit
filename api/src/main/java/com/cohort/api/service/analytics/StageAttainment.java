package com.cohort.api.service.analytics;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * PROJECT_PLAN.md §20.7's "stage attainment" rule, in one place so every
 * metric in this package (funnel, conversion, rejection distribution) uses
 * exactly the same definition: "An application has *reached* stage S if
 * {@code application_status_history} contains any row with
 * {@code to_status = S} <b>or</b> a status ordered after S." Computed from
 * history, never from current status (DR-01) -- an application currently
 * Rejected that previously reached Interview must still count in the
 * interview stage, or the funnel is meaningless.
 *
 * <p>The six non-terminal statuses are declared in exactly pipeline order
 * in {@link ApplicationStatus} (see its own javadoc / PROJECT_PLAN.md
 * §17.2's "Ordered progression"), so their {@code ordinal()} already *is*
 * the pipeline order -- no separate ordering table is needed. Terminal
 * statuses (Rejected, Withdrawn, Accepted, Declined) are deliberately
 * excluded from this comparison: a transition *into* Offer is always
 * recorded as its own history row before an Accepted/Declined row can
 * exist, so "reached Offer" is already true without treating terminal
 * statuses as if they had a pipeline position of their own.
 */
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

    /** Whether this application's history shows it ever reached {@code stage} or any stage ordered after it. */
    public static boolean reached(List<ApplicationStatusHistory> history, ApplicationStatus stage) {
        return history.stream()
                .map(ApplicationStatusHistory::getToStatus)
                .filter(PIPELINE_STAGES::contains)
                .anyMatch(reachedStatus -> reachedStatus.ordinal() >= stage.ordinal());
    }
}
