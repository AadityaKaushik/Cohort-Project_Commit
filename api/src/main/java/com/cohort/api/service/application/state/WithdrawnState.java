package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/** Withdrawn: terminal, absorbing. See {@link RejectedState} for the rationale. */
public final class WithdrawnState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.WITHDRAWN;
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
