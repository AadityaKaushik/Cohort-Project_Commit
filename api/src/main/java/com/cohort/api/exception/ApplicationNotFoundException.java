package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("Application not found.");
    }
}
