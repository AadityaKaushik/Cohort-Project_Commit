package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException() {
        super("Interview not found.");
    }
}
