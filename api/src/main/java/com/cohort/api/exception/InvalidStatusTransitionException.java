package com.cohort.api.exception;

import com.cohort.api.entity.ApplicationStatus;

/** FR-21: an illegal (from, to) status transition, per ApplicationStatusStateRegistry. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Cannot change status from " + from + " to " + to + ".");
    }
}
