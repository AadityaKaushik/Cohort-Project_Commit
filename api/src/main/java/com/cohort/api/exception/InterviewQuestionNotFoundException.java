package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class InterviewQuestionNotFoundException extends RuntimeException {

    public InterviewQuestionNotFoundException() {
        super("Interview question not found.");
    }
}
