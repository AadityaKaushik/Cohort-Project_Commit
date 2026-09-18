package com.cohort.api.entity;

/**
 * The application lifecycle status domain (PROJECT_PLAN.md §17.5). Six
 * non-terminal, ordered stages a candidate progresses through, and four
 * terminal outcomes reachable from any non-terminal stage (or, for Accepted
 * / Declined, only from Offer).
 *
 * <p>This enum intentionally carries no transition logic of its own — which
 * statuses a given status may move to is owned entirely by that status's
 * {@link com.cohort.api.service.application.state.ApplicationStatusState}
 * implementation (State pattern, ARCHITECTURE_REVISION_JAVA.md §4.1). Putting
 * "is this transition legal" logic here instead would just relocate the
 * if/else chain the State pattern exists to avoid.
 */
public enum ApplicationStatus {
    SAVED,
    APPLIED,
    OA,
    INTERVIEW,
    FINAL_ROUND,
    OFFER,
    REJECTED,
    WITHDRAWN,
    ACCEPTED,
    DECLINED
}
