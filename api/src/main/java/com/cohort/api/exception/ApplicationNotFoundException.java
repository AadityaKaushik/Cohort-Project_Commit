package com.cohort.api.exception;

/** Thrown for both a missing application ID and one belonging to another user — FR-95: always 404, never 403. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("Application not found.");
    }
}
