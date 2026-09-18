package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/** Saved: not yet submitted. The earliest stage — nothing to correct back to. */
public final class SavedState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.SAVED;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(
                ApplicationStatus.APPLIED,
                ApplicationStatus.OA,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.FINAL_ROUND,
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of();
    }
}
