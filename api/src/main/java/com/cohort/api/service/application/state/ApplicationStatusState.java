package com.cohort.api.service.application.state;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Set;

public interface ApplicationStatusState {
    ApplicationStatus status();

    Set<ApplicationStatus> allowedTransitions();

    Set<ApplicationStatus> correctionTransitions();

    default boolean canTransitionTo(ApplicationStatus target, boolean hasCorrectionNote) {
        return allowedTransitions().contains(target)
                || (hasCorrectionNote && correctionTransitions().contains(target));
    }
}
