package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/**
 * State pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): one implementation per
 * {@link ApplicationStatus}, each declaring its own legal next-states as
 * plain, hardcoded sets. This replaces a transition check that would
 * otherwise be a sprawling if/else (or switch) over every (from, to) pair
 * with a compile-time-known table — adding a new status means adding one
 * new class, not editing a shared conditional that every other status also
 * lives in.
 *
 * <p>Two kinds of legal transition, matching the state machine exactly as
 * drawn in PROJECT_PLAN.md §17.5:
 * <ul>
 *   <li>{@link #allowedTransitions()} — forward progression (skips allowed)
 *       and terminal exits (Rejected/Withdrawn from any non-terminal stage,
 *       Accepted/Declined only from Offer). Always legal.</li>
 *   <li>{@link #correctionTransitions()} — the one documented backward
 *       exception: "any status may move to a previous status if a
 *       correction note is supplied, because users do mis-click." Legal
 *       only when the caller supplies a note. Terminal states return an
 *       empty set here too — the diagram is explicit that terminal states
 *       are absorbing even for corrections ("Terminal → non-terminal is
 *       rejected outright").</li>
 * </ul>
 */
public interface ApplicationStatusState {

    ApplicationStatus status();

    Set<ApplicationStatus> allowedTransitions();

    Set<ApplicationStatus> correctionTransitions();

    /**
     * @param target             the status the caller wants to move to
     * @param hasCorrectionNote  whether the caller supplied a correction note
     * @return whether this transition is legal from this state
     */
    default boolean canTransitionTo(ApplicationStatus target, boolean hasCorrectionNote) {
        return allowedTransitions().contains(target)
                || (hasCorrectionNote && correctionTransitions().contains(target));
    }
}
