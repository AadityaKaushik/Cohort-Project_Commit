package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

/**
 * Offer: the only status Accepted and Declined can be reached from — the
 * diagram (§17.5) draws those two branching solely off Offer, not off every
 * non-terminal stage the way Rejected/Withdrawn do.
 */
public final class OfferState implements ApplicationStatusState {

    @Override
    public ApplicationStatus status() {
        return ApplicationStatus.OFFER;
    }

    @Override
    public Set<ApplicationStatus> allowedTransitions() {
        return Set.of(
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.DECLINED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN);
    }

    @Override
    public Set<ApplicationStatus> correctionTransitions() {
        return Set.of(
                ApplicationStatus.SAVED,
                ApplicationStatus.APPLIED,
                ApplicationStatus.OA,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.FINAL_ROUND);
    }
}
