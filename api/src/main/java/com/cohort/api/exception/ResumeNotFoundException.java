package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException() {
        super("Resume not found.");
    }
}
