package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/** OA: online assessment / screening test stage (employer naming varies, DR-04). */
public final class OaState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.OA;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.FINAL_ROUND,
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED);
    }
}
