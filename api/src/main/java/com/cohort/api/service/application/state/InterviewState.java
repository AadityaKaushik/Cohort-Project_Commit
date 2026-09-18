package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/** Interview: one or more interview rounds in progress. */
public final class InterviewState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.INTERVIEW;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(
                ApplicationStatus.FINAL_ROUND,
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED, ApplicationStatus.OA);
    }
}
