package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class CompanyNoteNotFoundException extends RuntimeException {

    public CompanyNoteNotFoundException() {
        super("Company note not found.");
    }
}
