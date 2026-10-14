package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

public final class AppliedState implements ApplicationStatusState {
    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.APPLIED;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(
                ApplicationStatus.OA,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.FINAL_ROUND,
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of(ApplicationStatus.SAVED);
    }
}
