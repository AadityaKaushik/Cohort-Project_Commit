package com.cohort.api.exception;

/** Thrown for both a missing resume ID and one belonging to another user — FR-95: always 404, never 403. */
public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException() {
        super("Resume not found.");
    }
}
