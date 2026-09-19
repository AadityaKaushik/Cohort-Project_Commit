package com.cohort.api.exception;

/** Thrown for both a missing interview ID and one belonging to another user — FR-95: always 404, never 403. */
public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException() {
        super("Interview not found.");
    }
}
