package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/** Declined: terminal, absorbing. See {@link RejectedState} for the rationale. */
public final class DeclinedState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.DECLINED;
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
