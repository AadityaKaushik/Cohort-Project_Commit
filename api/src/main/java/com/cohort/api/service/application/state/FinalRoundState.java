package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

public final class FinalRoundState implements ApplicationStatusState {
    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.FINAL_ROUND;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of(
                ApplicationStatus.SAVED,
                ApplicationStatus.APPLIED,
                ApplicationStatus.OA,
                ApplicationStatus.INTERVIEW);
    }
}
