package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

public final class AcceptedState implements ApplicationStatusState {
    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.ACCEPTED;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of();
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of();
    }
}
