package com.cohort.api.exception;

/** Thrown for both a missing question ID and one belonging to another user — FR-95: always 404, never 403. */
public class InterviewQuestionNotFoundException extends RuntimeException {

    public InterviewQuestionNotFoundException() {
        super("Interview question not found.");
    }
}
