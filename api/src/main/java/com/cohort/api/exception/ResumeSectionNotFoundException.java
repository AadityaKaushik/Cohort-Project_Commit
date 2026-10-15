package com.cohort.api.exception;

/** Missing, or inside a resume owned by another user: 404, never 403. */
public class ResumeSectionNotFoundException extends RuntimeException {

    public ResumeSectionNotFoundException() {
        super("Resume section not found.");
    }
}
