package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Looks up the {@link ApplicationStatusState} for a given {@link ApplicationStatus}
 * by table lookup (an {@link EnumMap}, populated once from the fixed list of
 * state instances below) rather than a switch or if/else chain over the
 * enum — the whole point of the State pattern here is that adding a new
 * status means adding one new class and one line below, never touching a
 * shared conditional (ARCHITECTURE_REVISION_JAVA.md §4.1).
 */
@Component
public class ApplicationStatusStateRegistry {

    private final Map<ApplicationStatus, ApplicationStatusState> statesByStatus;

    public ApplicationStatusStateRegistry() {
        List<ApplicationStatusState> states = List.of(
                new SavedState(),
                new AppliedState(),
                new OaState(),
                new InterviewState(),
                new FinalRoundState(),
                new OfferState(),
                new RejectedState(),
                new WithdrawnState(),
                new AcceptedState(),
                new DeclinedState());

        Map<ApplicationStatus, ApplicationStatusState> map = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatusState state : states) {
            map.put(state.status(), state);
        }
        // Fails fast at startup, not on the first request, if a status is
        // ever added to the enum without a matching state class.
        if (map.size() != ApplicationStatus.values().length) {
            throw new IllegalStateException(
                    "Every ApplicationStatus must have exactly one ApplicationStatusState registered");
        }
        this.statesByStatus = Map.copyOf(map);
    }

    public ApplicationStatusState stateFor(ApplicationStatus status) {
        return statesByStatus.get(status);
    }

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to, boolean hasCorrectionNote) {
        return stateFor(from).canTransitionTo(to, hasCorrectionNote);
    }
}
