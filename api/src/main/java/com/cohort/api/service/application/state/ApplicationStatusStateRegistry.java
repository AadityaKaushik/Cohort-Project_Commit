package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

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
