package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/**
 * Rejected: terminal, absorbing. No transition out — including the
 * correction-note exception, per the diagram's explicit "Terminal →
 * non-terminal is rejected outright" (§17.5).
 */
public final class RejectedState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.REJECTED;
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
